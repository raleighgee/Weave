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
	import weave.api.WeaveAPI;
	import weave.api.data.ColumnMetadata;
	import weave.api.data.DataTypes;
	import weave.api.data.IDataSource;
	import weave.api.data.IWeaveTreeNode;
	import weave.api.detectLinkableObjectChange;
	import weave.api.disposeObject;
	import weave.api.getCallbackCollection;
	import weave.api.newLinkableChild;
	import weave.api.registerLinkableChild;
	import weave.api.reportError;
	import weave.compiler.Compiler;
	import weave.compiler.StandardLib;
	import weave.core.LinkableBoolean;
	import weave.core.LinkableNumber;
	import weave.core.LinkableString;
	import weave.core.SessionManager;
	import weave.data.AttributeColumns.ProxyColumn;
	import weave.services.JsonCache;
	
	/**
	 * 
	 * @author adufilie
	 */
	public class SODADataSource extends AbstractDataSource
	{
		//WeaveAPI.registerImplementation(IDataSource, SODADataSource, "Socrata Open Data Portal");
		
		public function SODADataSource()
		{
			(WeaveAPI.SessionManager as SessionManager).unregisterLinkableChild(this, _attributeHierarchy);
		}
		
		public const url:LinkableString = registerLinkableChild(this, new LinkableString());
		public const showViews:LinkableBoolean = registerLinkableChild(this, new LinkableBoolean(true));
		public const showGroups:LinkableBoolean = registerLinkableChild(this, new LinkableBoolean(true));
		public const showTags:LinkableBoolean = registerLinkableChild(this, new LinkableBoolean(true));
		
		private function validateApiVersion(value:Number):Boolean { return [1, 2, 3].indexOf(value) >= 0; }
		
		/**
		 * This gets called when callbacks are triggered.
		 */		
		override protected function initialize():void
		{
			// TODO handle url change
			
			super.initialize();
		}
		
		override public function refreshHierarchy():void
		{
			getCallbackCollection(this).delayCallbacks();
			jsonCache.clearCache();
			for (var url:String in _dataSourceCache)
			{
				var ds:IDataSource = _dataSourceCache[url];
				disposeObject(ds);
			}
			_dataSourceCache = {};
			super.refreshHierarchy();
			getCallbackCollection(this).resumeCallbacks();
		}
		
		/**
		 * Gets the root node of the attribute hierarchy.
		 */
		override public function getHierarchyRoot():IWeaveTreeNode
		{
			if (!(_rootNode is SODANode))
				_rootNode = new SODANode(this);
			return _rootNode;
		}
		
		override protected function generateHierarchyNode(metadata:Object):IWeaveTreeNode
		{
			if (!metadata)
				return null;

			var id:String = metadata[PARAMS_SODA_ID];
			
			var ds:IDataSource = getChildDataSource(id);
			if (!ds)
				return null;
			
			var internalNode:IWeaveTreeNode = ds.findHierarchyNode(metadata);
			if (!internalNode)
				return null;
			
			var node:SODANode = new SODANode(this);
			node.action = SODANode.GET_COLUMN;
			node.id = metadata[PARAMS_SODA_ID];
			node.internalNode = internalNode
			return node;
		}
		
		/**
		 * @inheritDoc
		 */
		override protected function requestColumnFromSource(proxyColumn:ProxyColumn):void
		{
			var metadata:Object = proxyColumn.getProxyMetadata();
			var dataSource:IDataSource = getChildDataSource(metadata[PARAMS_SODA_ID]);
			if (dataSource)
				proxyColumn.setInternalColumn(dataSource.getAttributeColumn(metadata));
			else
				proxyColumn.setInternalColumn(ProxyColumn.undefinedColumn);
		}
		
		public static const PARAMS_SODA_ID:String = 'soda_id';
		
		public const jsonCache:JsonCache = newLinkableChild(this, JsonCache);
		
		public function getViewsURL():String
		{
			// get base url
			var url:String = this.url.value || '';
			var i:int = url.lastIndexOf('/api');
			if (i >= 0)
				url = url.substr(0, i);
			if (url.charAt(url.length - 1) != '/')
				url += '/';
			url += 'api/views';
			return url;
		}
		
		private static const UNCATEGORIZED:String = lang("Uncategorized");
		private var _cachedViews:Array;
		private var _cachedCategories:Array;
		private var _cachedTags:Array;
		private var _categoryLookup:Object;
		private var _tagLookup:Object;
		/**
		 * Array of views
		 */
		public function getViews():Array
		{
			var views:Array = jsonCache.getJsonObject(getViewsURL()) as Array;
			if (_cachedViews != views)
			{
				_cachedViews = views;
				_cachedCategories = [];
				_cachedTags = [];
				_categoryLookup = {};
				_tagLookup = {};
				for (var view:Object in views)
				{
					// handle category
					var cat:String = view.category || UNCATEGORIZED;
					var array:Array = _categoryLookup[cat];
					if (!array)
					{
						_categoryLookup[cat] = array = [];
						_cachedCategories.push(cat);
					}
					array.push(view);
					
					// handle tags
					for each (var tag:String in view.tags)
					{
						array = _tagLookup[tag];
						if (!array)
						{
							_tagLookup[tag] = array = [];
							_cachedTags.push(cat);
						}
						array.push(view);
					}
				}
				
				StandardLib.sort(_cachedCategories);
				StandardLib.sort(_cachedTags);
			}
			return views;
		}
		public function getCategories():Array
		{
			getViews();
			return _cachedCategories;
		}
		public function getTags():Array
		{
			getViews();
			return _cachedTags;
		}
		/**
		 * category -> Array of views with that category
		 */
		public function getCategoryLookup():Object
		{
			getViews();
			return _categoryLookup;
		}
		/**
		 * tag -> Array of views with that tag
		 */
		public function getTagLookup():Object
		{
			getViews();
			return _tagLookup;
		}
		
		/**
		 * @private
		 */
		public function getChildDataSource(id:String):IDataSource
		{
			if (!id)
				return null;
			
			var url:String = getViewsURL() + '/' + id + '/rows.json';
			var result:Object = jsonCache.getJsonObject(url);
			try
			{
				if (result && result.meta && result.data)
				{
					var ds:CSVDataSource = _dataSourceCache[url];
					if (!ds)
					{
						var metadata:Array = [];
						var headerRow:Array = [];
						(result.meta.view.columns as Array).forEach(function(input:Object, i:int, a:Array):void {
							var output:Object = metadata[i] = {};
							output[CSVDataSource.METADATA_COLUMN_NAME] = input['fieldName'];
							headerRow[i] = output[ColumnMetadata.TITLE] = input['name'];
							var dataType:String = input['dataTypeName'];
							if (dataType == 'text')
							{
								dataType = DataTypes.STRING;
							}
							if (dataType == 'percent')
							{
								dataType = DataTypes.STRING;
								output[ColumnMetadata.NUMBER] = "asNumber(replace(string,'%',''))";
							}
							if (dataType == 'calendar_date')
							{
								dataType = DataTypes.DATE;
								//output[ColumnMetadata.DATE_FORMAT] = 'YYYY-MM-DDTHH:NN:SS';
							}
						});
						ds = new CSVDataSource();
						ds.keyType.value = url;
						ds.metadata.setSessionState(metadata);
						ds.csvData.setSessionState(headerRow.concat(result.data));
						
						_dataSourceCache[url] = registerLinkableChild(this, ds);
					}
					return ds;
				}
			}
			catch (e:Error)
			{
				reportError(e, "Unexpected JSON format: " + url);
			}
			return null;
		}
		
		/**
		 * url -> IDataSource
		 */
		private var _dataSourceCache:Object = {};
	}
}

import flash.events.Event;
import flash.external.ExternalInterface;
import flash.net.URLRequest;
import flash.net.URLRequestHeader;
import flash.net.URLRequestMethod;
import flash.net.URLVariables;

import mx.rpc.events.FaultEvent;
import mx.rpc.events.ResultEvent;
import mx.utils.ObjectUtil;
import mx.utils.URLUtil;

import weave.api.WeaveAPI;
import weave.api.data.IColumnReference;
import weave.api.data.IDataSource;
import weave.api.data.IWeaveTreeNode;
import weave.api.data.IWeaveTreeNodeWithPathFinding;
import weave.api.detectLinkableObjectChange;
import weave.api.reportError;
import weave.compiler.Compiler;
import weave.compiler.StandardLib;
import weave.core.ClassUtils;
import weave.data.DataSources.SODADataSource;
import weave.services.URLRequestUtils;
import weave.utils.VectorUtils;

internal class SODANode implements IWeaveTreeNode, IColumnReference, IWeaveTreeNodeWithPathFinding
{
	public static const VIEW_LIST:String = 'view_list';
	public static const CATEGORY_LIST:String = 'category_list';
	public static const CATEGORY_SHOW:String = 'category_show';
	public static const TAG_LIST:String = 'tag_list';
	public static const TAG_SHOW:String = 'tag_show';
	
	public static const GET_DATASOURCE:String = 'get_datasource';
	public static const GET_COLUMN:String = 'get_column';
	
	private var source:SODADataSource;
	/**
	 * The metadata associated with the node
	 */
	public var metadata:Object;
	/**
	 * The action associated with this node
	 */
	public var action:String;
	/**
	 * Identifies which thing this node corresponds to
	 */
	public var id:String;
	
	public var internalNode:IWeaveTreeNode;
	
	public function SODANode(source:SODADataSource)
	{
		this.source = source;
	}
	
	public function equals(other:IWeaveTreeNode):Boolean
	{
		var that:SODANode = other as SODANode;
		if (!that)
			return false;
		
		if (this.internalNode && that.internalNode)
			return this.source && that.source
				&& this.internalNode.equals(that.internalNode);
		
		return !this.internalNode == !that.internalNode
			&& this.source == that.source
			&& this.action == that.action
			&& ObjectUtil.compare(this.id, that.id) == 0;
	}
	public function getLabel():String
	{
		if (internalNode)
			return internalNode.getLabel();
		
		if (!action)
			return WeaveAPI.globalHashMap.getName(source);
		
		if (action == VIEW_LIST)
			return lang("Views");
		if (action == CATEGORY_LIST)
			return lang("Categories");
		if (action == TAG_LIST)
			return lang("Tags");
		
		if (action == CATEGORY_SHOW || action == TAG_SHOW)
			return id;
		
		if (action == GET_DATASOURCE)
			return metadata['name'] || id;
		
		return null;
	}
	public function isBranch():Boolean
	{
		if (internalNode)
			return internalNode.isBranch();
		
		return true;
	}
	public function hasChildBranches():Boolean
	{
		if (internalNode)
			return internalNode.hasChildBranches();
		
		if (action == GET_DATASOURCE)
			return false;
		
		return getChildren().length > 0;
	}
	
	private var _childNodes:Array = [];
	
	/**
	 * @param input The input metadata items for generating child nodes
	 * @param childAction The action property of the child nodes
	 * @param updater A function like function(node:SODANode, item:Object):void which receives the child node and its corresponding input metadata item.
	 * @return The updated _childNodes Array. 
	 */
	private function updateChildren(input:Array, updater:Function = null, nodeType:Class = null):Array
	{
		if (!nodeType)
			nodeType = SODANode;
		var outputIndex:int = 0;
		for each (var item:Object in input)
		{
			var node:SODANode = _childNodes[outputIndex];
			if (!node || Object(node).constructor != nodeType)
				_childNodes[outputIndex] = node = new nodeType(source);
			
			updater(node, item);
			
			outputIndex++;
		}
		_childNodes.length = outputIndex;
		return _childNodes;
	}
	
	public function getChildren():Array
	{
		if (internalNode)
			return internalNode.getChildren();
		
		var list:Array;
		if (!action)
		{
			list = [];
			if (source.showViews.value)
				list.push(VIEW_LIST);
			if (source.showGroups.value)
				list.push(CATEGORY_LIST);
			if (source.showTags.value)
				list.push(TAG_LIST);
			return updateChildren(list, function(node:SODANode, action:String):void {
				node.action = action;
				node.id = null;
				node.metadata = null;
			});
		}
		
		if (action == CATEGORY_LIST)
			return updateChildren(source.getCategories(), function(node:SODANode, category:String):void {
				node.action = CATEGORY_SHOW;
				node.id = category;
				node.metadata = null;
			});
		if (action == TAG_LIST)
			return updateChildren(source.getTags(), function(node:SODANode, tag:String):void {
				node.action = TAG_SHOW;
				node.id = tag;
				node.metadata = null;
			});
		
		if (action == VIEW_LIST)
			list = source.getViews();
		if (action == CATEGORY_SHOW)
			list = source.getCategoryLookup()[id];
		if (action == TAG_SHOW)
			list = source.getTagLookup()[id];
		if (list)
			return updateChildren(list, function(node:SODANode, view:Object):void {
				node.action = GET_DATASOURCE;
				node.id = view['id'];
				node.metadata = view;
			});
		
		if (action == GET_DATASOURCE)
		{
			var ds:IDataSource = source.getChildDataSource(id);
			if (ds)
			{
				var root:IWeaveTreeNode = ds.getHierarchyRoot();
				return updateChildren(root.getChildren(), function(node:SODANode, otherNode:IWeaveTreeNode):void {
					node.action = GET_COLUMN;
					node.internalNode = otherNode;
					node.id = id; // copy from parent
					node.metadata = null;
				});
			}
		}
		
		_childNodes.length = 0;
		return _childNodes;
	}
	
	public function getDataSource():IDataSource
	{
		return source;
	}
	public function getColumnMetadata():Object
	{
		if (internalNode is IColumnReference)
		{
			var meta:Object = (internalNode as IColumnReference).getColumnMetadata();
			meta[SODADataSource.PARAMS_SODA_ID] = id;
			return meta;
		}
		return null;
	}
	
	public function findPathToNode(descendant:IWeaveTreeNode):Array
	{
		if (!descendant)
			return null;
		if (equals(descendant))
			return [this];
		
		// search cached children only
		for each (var child:SODANode in _childNodes)
		{
			var path:Array = child.findPathToNode(descendant);
			if (path)
			{
				path.unshift(this);
				return path;
			}
		}
		return null;
	}
}
