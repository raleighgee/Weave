/*
    Weave (Web-based Analysis and Visualization Environment)
    Copyright (C) 2008-2011 University of Massachusetts Lowell

    This file is a part of Weave.

    Weave is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License, Version 3,
    as published by the Free Software Foundation.

    Weave is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Weave.  If not, see <http://www.gnu.org/licenses/>.
*/

package weave.data.DataSources
{	
	import flash.net.URLLoaderDataFormat;
	import flash.net.URLRequest;
	import flash.utils.ByteArray;
	
	import mx.rpc.events.FaultEvent;
	import mx.rpc.events.ResultEvent;
	
	import org.vanrijkom.dbf.DbfField;
	import org.vanrijkom.dbf.DbfHeader;
	import org.vanrijkom.dbf.DbfRecord;
	import org.vanrijkom.dbf.DbfTools;
	
	import weave.api.WeaveAPI;
	import weave.api.data.ColumnMetadata;
	import weave.api.data.DataTypes;
	import weave.api.data.IAttributeColumn;
	import weave.api.data.IDataSource;
	import weave.api.data.IQualifiedKey;
	import weave.api.data.IWeaveTreeNode;
	import weave.api.detectLinkableObjectChange;
	import weave.api.disposeObject;
	import weave.api.getCallbackCollection;
	import weave.api.linkableObjectIsBusy;
	import weave.api.newLinkableChild;
	import weave.api.objectWasDisposed;
	import weave.api.registerLinkableChild;
	import weave.api.reportError;
	import weave.compiler.StandardLib;
	import weave.core.LinkableString;
	import weave.core.SessionManager;
	import weave.data.AttributeColumns.DateColumn;
	import weave.data.AttributeColumns.GeometryColumn;
	import weave.data.AttributeColumns.NumberColumn;
	import weave.data.AttributeColumns.ProxyColumn;
	import weave.data.AttributeColumns.StringColumn;
	import weave.primitives.GeneralizedGeometry;
	import weave.utils.ShpFileReader;

	/**
	 * @author adufilie
	 */
	public class DBFDataSource extends AbstractDataSource
	{
		WeaveAPI.registerImplementation(IDataSource, DBFDataSource, "SHP/DBF files");
		
		public function DBFDataSource()
		{
			(WeaveAPI.SessionManager as SessionManager).excludeLinkableChildFromSessionState(this, _attributeHierarchy);
		}
		
		override protected function get initializationComplete():Boolean
		{
			// make sure everything is ready before column requests get handled.
			return super.initializationComplete
				&& !linkableObjectIsBusy(this)
				&& (!shpfile || shpfile.geomsReady);
		}
		
		override protected function uninitialize():void
		{
			super.uninitialize();
			if (detectLinkableObjectChange(uninitialize, dbfUrl))
			{
				dbfData = null;
				dbfHeader = null;
			}
			if (detectLinkableObjectChange(uninitialize, shpUrl))
			{
				if (shpfile)
					disposeObject(shpfile)
				shpfile = null;
			}
		}
		
		override protected function initialize():void
		{
			if (detectLinkableObjectChange(initialize, dbfUrl) && dbfUrl.value)
				WeaveAPI.URLRequestUtils.getURL(this, new URLRequest(dbfUrl.value), handleDBFDownload, handleDBFDownloadError, dbfUrl.value, URLLoaderDataFormat.BINARY);
			if (detectLinkableObjectChange(initialize, shpUrl) && shpUrl.value)
				WeaveAPI.URLRequestUtils.getURL(this, new URLRequest(shpUrl.value), handleShpDownload, handleShpDownloadError, shpUrl.value, URLLoaderDataFormat.BINARY);
			
			// recalculate all columns previously requested because data may have changed.
			refreshAllProxyColumns();
			
			super.initialize();
		}
		
		public const keyType:LinkableString = newLinkableChild(this, LinkableString);
		public const keyColName:LinkableString = newLinkableChild(this, LinkableString);
		public const dbfUrl:LinkableString = newLinkableChild(this, LinkableString);
		public const shpUrl:LinkableString = newLinkableChild(this, LinkableString);
		public const projection:LinkableString = newLinkableChild(this, LinkableString);
		
		private var dbfData:ByteArray = null;
		private var dbfHeader:DbfHeader = null;
		private var shpfile:ShpFileReader = null;
		
		public static const DBF_COLUMN_NAME:String = 'name';
		public static const THE_GEOM_COLUMN:String = 'the_geom';
		
		/**
		 * Called when the DBF file is downloaded from the URL
		 */
		private function handleDBFDownload(event:ResultEvent, url:String):void
		{
			// ignore outdated results
			if (objectWasDisposed(this) || url != dbfUrl.value)
				return;
			
			dbfData = ByteArray(event.result);
			if (dbfData.length == 0)
			{
				dbfData = null;
				reportError("Zero-byte DBF: " + dbfUrl.value);
			}
			else
			{
				try
				{
					dbfData.position = 0;
					dbfHeader = new DbfHeader(dbfData);
				}
				catch (e:Error)
				{
					dbfData = null;
					reportError(e);
				}
			}
			getCallbackCollection(this).triggerCallbacks();
		}
		
		override protected function requestHierarchyFromSource(subtreeNode:XML = null):void
		{
			// do nothing
		}

		/**
		 * Gets the root node of the attribute hierarchy.
		 */
		override public function getHierarchyRoot():IWeaveTreeNode
		{
			if (_attributeHierarchy.value === null)
			{
				if (!(_rootNode is DBFColumnNode))
					_rootNode = new DBFColumnNode(this);
				return _rootNode;
			}
			else
			{
				return super.getHierarchyRoot();
			}
		}
		
		override protected function generateHierarchyNode(metadata:Object):IWeaveTreeNode
		{
			if (!metadata)
				return null;
			
			var root:DBFColumnNode = getHierarchyRoot() as DBFColumnNode;
			if (!root)
				return super.generateHierarchyNode(metadata);
			
			if (metadata.hasOwnProperty(DBF_COLUMN_NAME))
				return new DBFColumnNode(this, metadata[DBF_COLUMN_NAME]);
			
			return null;
		}
		
		/**
		 * Called when the Shp file is downloaded from the URL
		 */
		private function handleShpDownload(event:ResultEvent, url:String):void
		{
			// ignore outdated results
			if (objectWasDisposed(this) || url != shpUrl.value)
				return;
			
			debug('shp download complete',shpUrl.value);
			
			if (shpfile)
			{
				disposeObject(shpfile);
				shpfile = null;
			}
			var bytes:ByteArray = ByteArray(event.result);
			if (bytes.length == 0)
			{
				reportError("Zero-byte ShapeFile: " + shpUrl.value);
			}
			else
			{
				try
				{
					bytes.position = 0;
					shpfile = registerLinkableChild(this, new ShpFileReader(bytes));
				}
				catch (e:Error)
				{
					reportError(e);
				}
			}
			getCallbackCollection(this).triggerCallbacks();
		}

		/**
		 * Called when the DBF file fails to download from the URL
		 */
		private function handleDBFDownloadError(event:FaultEvent, url:String):void
		{
			if (objectWasDisposed(this))
				return;
			
			// ignore outdated results
			if (url != dbfUrl.value)
				return;
			
			reportError(event);
			getCallbackCollection(this).triggerCallbacks();
		}

		/**
		 * Called when the DBF file fails to download from the URL
		 */
		private function handleShpDownloadError(event:FaultEvent, url:String):void
		{
			if (objectWasDisposed(this))
				return;
			
			// ignore outdated results
			if (url != shpUrl.value)
				return;
			
			reportError(event);
			getCallbackCollection(this).triggerCallbacks();
		}

		/**
		 * @inheritDoc
		 */
		override protected function requestColumnFromSource(proxyColumn:ProxyColumn):void
		{
			var metadata:Object = proxyColumn.getProxyMetadata();
			
			// get column name from proxy metadata
			var columnName:String = metadata[DBF_COLUMN_NAME];
			
			// override proxy metadata
			metadata = getColumnMetadata(columnName);
			proxyColumn.setMetadata(metadata);

			var keysVector:Vector.<IQualifiedKey> = Vector.<IQualifiedKey>(WeaveAPI.QKeyManager.getQKeys(getKeyType(), getColumnValues(keyColName.value)));
			var data:Array = getColumnValues(columnName);

			var newColumn:IAttributeColumn;
			var dataType:String = metadata[ColumnMetadata.DATA_TYPE];
			if (dataType == DataTypes.GEOMETRY)
			{
				newColumn = new GeometryColumn();
				(newColumn as GeometryColumn).setGeometries(keysVector, Vector.<GeneralizedGeometry>(data));
			}
			else if (dataType == DataTypes.DATE)
			{
				newColumn = new DateColumn(metadata);
				(newColumn as DateColumn).setRecords(keysVector, Vector.<String>(data));
			}
			else if (dataType == DataTypes.NUMBER)
			{
				newColumn = new NumberColumn(metadata);
				data.forEach(function(str:String, i:int, a:Array):Number { return StandardLib.asNumber(str); });
				(newColumn as NumberColumn).setRecords(keysVector, Vector.<Number>(data));
			}
			else // string
			{
				newColumn = new StringColumn(metadata);
				(newColumn as StringColumn).setRecords(keysVector, Vector.<String>(data));
			}

			proxyColumn.setInternalColumn(newColumn);
		}
		
		public function getKeyType():String
		{
			return keyType.value || WeaveAPI.globalHashMap.getName(this);
		}
		public function getColumnNames():Array
		{
			var names:Array = [];
			if (shpfile)
				names.push(THE_GEOM_COLUMN);
			if (dbfHeader)
				for (var i:int = 0; i < dbfHeader.fields.length; i++)
					names.push((dbfHeader.fields[i] as DbfField).name);
			return names;
		}
		public function getColumnMetadata(columnName:String):Object
		{
			var meta:Object = {};
			meta[DBF_COLUMN_NAME] = columnName;
			meta[ColumnMetadata.TITLE] = columnName;
			meta[ColumnMetadata.KEY_TYPE] = getKeyType();
			meta[ColumnMetadata.PROJECTION] = projection.value;
			if (columnName == THE_GEOM_COLUMN)
			{
				meta[ColumnMetadata.DATA_TYPE] = DataTypes.GEOMETRY;
			}
			else
			{
				for each (var field:DbfField in dbfHeader.fields)
				{
					if (field.name == columnName)
					{
						var typeChar:String = String.fromCharCode(field.type);
						var dataType:String = FIELD_TYPE_LOOKUP[typeChar];
						if (dataType)
							meta[ColumnMetadata.DATA_TYPE] = dataType;
						if (dataType == DataTypes.DATE)
							meta[ColumnMetadata.DATE_FORMAT] = "YYYYMMDD";
						break;
					}
				}
			}
			return meta;
		}
		private function getColumnValues(columnName:String):Array
		{
			var values:Array = [];
			if (columnName == THE_GEOM_COLUMN)
				return shpfile ? shpfile.geoms : [];
			
			if (!dbfHeader)
				return values;
			
			var record:DbfRecord = null;
			for (var i:int = 0; i < dbfHeader.recordCount; i++)
			{ 
				if (columnName)
				{
					record = DbfTools.getRecord(dbfData, dbfHeader, i);
					values.push( record.values[columnName] );
				}
				else
					values.push(String(i + 1));
			}
			return values;
		}
		
		private static const FIELD_TYPE_LOOKUP:Object = {
			"C": DataTypes.STRING, // Char - ASCII
			"D": DataTypes.DATE, // Date - 8 Ascii digits (0..9) in the YYYYMMDD format
			"F": DataTypes.NUMBER, // Numeric - Ascii digits (-.0123456789) variable position of floating point
			"N": DataTypes.NUMBER, // Numeric - Ascii digits (-.0123456789) fixed position/no floating point
			"2": DataTypes.NUMBER, // short int -- binary int
			"4": DataTypes.NUMBER, // long int - binary int
			"8": DataTypes.NUMBER, // double - binary signed double IEEE
			"L": "boolean", // Logical - Ascii chars (YyNnTtFf space ?)
			"M": null, // Memo - 10 digits representing the start block position in .dbt file, or 10 spaces if no entry in memo
			"B": null, // Binary - binary data in .dbt, structure like M
			"G": null, // General - OLE objects, structure like M
			"P": null, // Picture - binary data in .ftp, structure like M
			"I": null,
			"0": null,
			"@": null,
			"+": null
		};
	}
}

import weave.api.WeaveAPI;
import weave.api.data.IColumnReference;
import weave.api.data.IDataSource;
import weave.api.data.IWeaveTreeNode;
import weave.data.DataSources.DBFDataSource;

internal class DBFColumnNode implements IWeaveTreeNode, IColumnReference
{
	private var source:DBFDataSource;
	public var columnName:String;
	internal var children:Array;
	public function DBFColumnNode(source:DBFDataSource = null, columnName:String = null)
	{
		this.source = source;
		this.columnName = columnName;
	}
	public function equals(other:IWeaveTreeNode):Boolean
	{
		var that:DBFColumnNode = other as DBFColumnNode;
		return !!that
			&& this.source == that.source
			&& this.columnName == that.columnName;
	}
	public function getLabel():String
	{
		if (!columnName)
			return WeaveAPI.globalHashMap.getName(source);
		return columnName;
	}
	public function isBranch():Boolean { return !columnName; }
	public function hasChildBranches():Boolean { return false; }
	public function getChildren():Array
	{
		if (columnName)
			return null;
		
		if (!children)
			children = [];
		var names:Array = source.getColumnNames();
		for (var i:int = 0; i < names.length; i++)
		{
			if (children[i])
				(children[i] as DBFColumnNode).columnName = names[i];
			else
				children[i] = new DBFColumnNode(source, names[i]);
		}
		children.length = names.length;
		return children;
	}
	
	public function getDataSource():IDataSource { return source; }
	public function getColumnMetadata():Object { return source.getColumnMetadata(columnName); }
}
