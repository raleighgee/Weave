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

package weave.visualization.plotters
{
	import flash.display.Bitmap;
	import flash.display.BitmapData;
	import flash.display.Graphics;
	import flash.display.LineScaleMode;
	import flash.geom.ColorTransform;
	import flash.geom.Matrix;
	import flash.geom.Point;
	import flash.geom.Rectangle;
	import flash.utils.Dictionary;
	import flash.utils.getTimer;
	
	import weave.Weave;
	import weave.api.WeaveAPI;
	import weave.api.core.IDisposableObject;
	import weave.api.core.ILinkableHashMap;
	import weave.api.data.ColumnMetadata;
	import weave.api.data.IAttributeColumn;
	import weave.api.data.IColumnWrapper;
	import weave.api.data.IQualifiedKey;
	import weave.api.getLinkableDescendants;
	import weave.api.linkSessionState;
	import weave.api.newLinkableChild;
	import weave.api.primitives.IBounds2D;
	import weave.api.registerLinkableChild;
	import weave.api.setSessionState;
	import weave.api.ui.IPlotTask;
	import weave.api.ui.IPlotter;
	import weave.api.ui.IPlotterWithGeometries;
	import weave.core.LinkableBoolean;
	import weave.core.LinkableHashMap;
	import weave.core.LinkableNumber;
	import weave.core.LinkableString;
	import weave.data.AttributeColumns.ImageColumn;
	import weave.data.AttributeColumns.ReprojectedGeometryColumn;
	import weave.data.AttributeColumns.StreamedGeometryColumn;
	import weave.primitives.Bounds2D;
	import weave.primitives.GeneralizedGeometry;
	import weave.primitives.GeometryType;
	import weave.primitives.Range;
	import weave.utils.BitmapText;
	import weave.utils.GeometryTileDescriptor;
	import weave.utils.PlotterUtils;
	import weave.visualization.layers.PlotTask;
	import weave.visualization.plotters.styles.ExtendedFillStyle;
	import weave.visualization.plotters.styles.ExtendedLineStyle;
	
	/**
	 * GeometryPlotter
	 * 
	 * @author adufilie
	 */
	public class GeometryPlotter extends AbstractPlotter implements IPlotterWithGeometries, IDisposableObject
	{
		WeaveAPI.registerImplementation(IPlotter, GeometryPlotter, "Geometries");
		
		public function GeometryPlotter()
		{
			registerLinkableChild(this, Demo.settings);
			
			// initialize default line & fill styles
			line.scaleMode.defaultValue.setSessionState(LineScaleMode.NONE);
			fill.color.internalDynamicColumn.globalName = Weave.DEFAULT_COLOR_COLUMN;

			line.weight.addImmediateCallback(this, disposeCachedBitmaps);
			
			linkSessionState(StreamedGeometryColumn.geometryMinimumScreenArea, pixellation);

			setSingleKeySource(geometryColumn);
			
			// not every change to the geometries changes the keys
			geometryColumn.removeCallback(_filteredKeySet.triggerCallbacks);
			geometryColumn.boundingBoxCallbacks.addImmediateCallback(this, _filteredKeySet.triggerCallbacks);
			
			geometryColumn.boundingBoxCallbacks.addImmediateCallback(this, spatialCallbacks.triggerCallbacks); // bounding box should trigger spatial
			registerSpatialProperty(_filteredKeySet.keyFilter); // subset should trigger spatial callbacks
		}
		
		public function get streamedGeometryColumn():StreamedGeometryColumn
		{
			return getLinkableDescendants(geometryColumn, StreamedGeometryColumn)[0];
		}
		
		/** Array of GeometryTileDescriptor */
		public function get metaTiles():Array
		{
			var sgc:StreamedGeometryColumn = streamedGeometryColumn;
			return sgc ? sgc.requiredMetadataTiles : [];
		}
		/** Array of GeometryTileDescriptor */
		public function get geomTiles():Array
		{
			var sgc:StreamedGeometryColumn = streamedGeometryColumn;
			return sgc ? sgc.requiredGeometryTiles : [];
		}
		
		public const symbolPlotters:ILinkableHashMap = registerLinkableChild(this, new LinkableHashMap(IPlotter));

		/**
		 * This is the reprojected geometry column to draw.
		 */
		public const geometryColumn:ReprojectedGeometryColumn = newLinkableChild(this, ReprojectedGeometryColumn);
		
		/**
		 *  This is the default URL path for images, when using images in place of points.
		 */
		public const pointDataImageColumn:ImageColumn = newLinkableChild(this, ImageColumn);
		public const useFixedImageSize:LinkableBoolean = registerLinkableChild(this, new LinkableBoolean(false));
		
		[Embed(source="/weave/resources/images/missing.png")]
		private static var _missingImageClass:Class;
		private static const _missingImage:BitmapData = Bitmap(new _missingImageClass()).bitmapData;
		
		/**
		 * This is the line style used to draw the lines of the geometries.
		 */
		public const line:ExtendedLineStyle = newLinkableChild(this, ExtendedLineStyle, invalidateCachedBitmaps);
		/**
		 * This is the fill style used to fill the geometries.
		 */
		public const fill:ExtendedFillStyle = newLinkableChild(this, ExtendedFillStyle, invalidateCachedBitmaps);

		/**
		 * This is the size of the points drawn when the geometry represents point data.
		 **/
		public const iconSize:LinkableNumber = registerLinkableChild(this, new LinkableNumber(10, validateIconSize), disposeCachedBitmaps);
		private function validateIconSize(value:Number):Boolean { return 0.2 <= value && value <= 1024; };

		override public function getDataBoundsFromRecordKey(recordKey:IQualifiedKey, output:Array):void
		{
			var geoms:Array = null;
			var column:IAttributeColumn = geometryColumn; 
			var notGeoms:Boolean = false;
			
			// the column value may contain a single geom or an array of geoms
			var value:* = column.getValueFromKey(recordKey);
			if (value is Array)
			{
				geoms = value; // array of geoms
				//Need to ensure that it is an array of geoms
				for( var j:int = 0; j < geoms.length; j++)
					if( !(geoms[j] is GeneralizedGeometry) )
					{
						notGeoms = true;
						break;
					}
			}
			else if (value is GeneralizedGeometry)
				geoms = [value as GeneralizedGeometry]; // single geom -- create array

			var i:int = 0;
			if( !notGeoms )
				if (geoms != null)
					for each (var geom:GeneralizedGeometry in geoms)
						output[i++] = geom.bounds;
			output.length = i;
		}
		
		public function getGeometriesFromRecordKey(recordKey:IQualifiedKey, minImportance:Number = 0, bounds:IBounds2D = null):Array
		{
			var value:* = geometryColumn.getValueFromKey(recordKey);
			var geoms:Array = null;
			var notGeoms:Boolean = false;
			
			if (value is Array)
			{
				geoms = value;
				//Need to ensure that it is an array of geoms
				for( var j:int = 0; j < geoms.length; j++)
					if( !(geoms[j] is GeneralizedGeometry) )
					{
						notGeoms = true;
						break;
					}
			}
			else if (value is GeneralizedGeometry)
				geoms = [ value as GeneralizedGeometry ];
			
			var results:Array = [];
			if( !notGeoms )
				if (geoms != null)
					for each (var geom:GeneralizedGeometry in geoms)
						results.push(geom);
			
			return results;
		}
		
		public function getBackgroundGeometries():Array
		{
			return [];
		}
		
		/**
		 * This function returns a Bounds2D object set to the data bounds associated with the background.
		 * @param outputDataBounds A Bounds2D object to store the result in.
		 */
		override public function getBackgroundDataBounds(output:IBounds2D):void
		{
			// try to find an internal StreamedGeometryColumn
			var column:IAttributeColumn = geometryColumn;
			while (!(column is StreamedGeometryColumn) && column is IColumnWrapper)
				column = (column as IColumnWrapper).getInternalColumn();
			
			// if the internal geometry column is a streamed column, request the required detail
			var streamedColumn:StreamedGeometryColumn = column as StreamedGeometryColumn;
			if (streamedColumn)
				output.copyFrom(streamedColumn.collectiveBounds);
			else
				output.reset(); // undefined
		}
		
		private var _debugSimplifyDataBounds:IBounds2D;
		private var _debugSimplifyScreenBounds:IBounds2D;

		/**
		 * This function calculates the importance of a pixel.
		 */
		protected function getDataAreaPerPixel(dataBounds:IBounds2D, screenBounds:IBounds2D):Number
		{
			// get minimum importance value required to display the shape at this zoom level
//			var dw:Number = dataBounds.getWidth();
//			var dh:Number = dataBounds.getHeight();
//			var sw:Number = screenBounds.getWidth();
//			var sh:Number = screenBounds.getHeight();
//			return Math.min((dw*dw)/(sw*sw), (dh*dh)/(sh*sh));
			return dataBounds.getArea() / screenBounds.getArea();
		}
		
		private var colorToBitmapMap:Dictionary = new Dictionary(); // color -> BitmapData
		private var colorToBitmapValidFlagMap:Dictionary = new Dictionary(); // color -> valid flag

		// this calls dispose() on all cached bitmaps and removes references to them.
		private function disposeCachedBitmaps():void
		{
			var disposed:Boolean = false;
			for each (var bitmapData:BitmapData in colorToBitmapMap)
			{
				bitmapData.dispose();
				disposed = true;
			}
			if (disposed)
				colorToBitmapMap = new Dictionary();
			invalidateCachedBitmaps();
			
			var weight:Number = line.weight.getValueFromKey(null, Number);
			pointOffset = (Math.ceil(iconSize.value) + weight) / 2;
			circleBitmapSize = Math.ceil(pointOffset * 2 + 1);
			circleBitmapDataRectangle.width = circleBitmapSize;
			circleBitmapDataRectangle.height = circleBitmapSize;
		}
		// this invalidates all cached bitmap graphics
		private function invalidateCachedBitmaps():void
		{
			for (var k:* in colorToBitmapValidFlagMap)
			{
				colorToBitmapValidFlagMap = new Dictionary();
				return;
			}
		}
		
		private var circleBitmapSize:int = 0;
		private var circleBitmapDataRectangle:Rectangle = new Rectangle(0,0,0,0);
		
		// this is the offset used to draw a circle onto a cached BitmapData
		private var pointOffset:Number;
		
		// this function returns the BitmapData associated with the given key
		private function drawCircle(destination:BitmapData, color:Number, x:Number, y:Number):void
		{
			var bitmapData:BitmapData = colorToBitmapMap[color] as BitmapData;
			if (!bitmapData)
			{
				// create bitmap
				try
				{
					bitmapData = new BitmapData(circleBitmapSize, circleBitmapSize);
				}
				catch (e:Error)
				{
					return; // do nothing if this fails
				}
				colorToBitmapMap[color] = bitmapData;
			}
			if (colorToBitmapValidFlagMap[color] == undefined)
			{
				// draw graphics on cached bitmap
				var g:Graphics = tempShape.graphics;
				g.clear();
				if (isNaN(color))
				{
					if (fill.enableMissingDataGradient.value)
						fill.beginFillStyle(null, g);
				}
				else if (fill.enabled.defaultValue.value)
				{
					g.beginFill(color, fill.alpha.getValueFromKey(null, Number));
				}
				line.beginLineStyle(null, g);
				g.drawCircle(pointOffset, pointOffset, iconSize.value / 2);
				g.endFill();
				PlotterUtils.clearBitmapData(bitmapData);
				bitmapData.draw(tempShape);
				g.clear(); // clear tempShape now so these graphics don't get used anywhere else by mistake
				
				colorToBitmapValidFlagMap[color] = true;
			}
			// copy bitmap graphics
			tempPoint.x = Math.round(x - pointOffset);
			tempPoint.y = Math.round(y - pointOffset);
			destination.copyPixels(bitmapData, circleBitmapDataRectangle, tempPoint, null, null, true);
		}
		
		public var debug:Boolean = false;
		public var debugGridSkip:Boolean = false;
		private var keepTrack:Boolean = false;
		public var totalVertices:int = 0;
		
		public const pixellation:LinkableNumber = registerLinkableChild(this, new LinkableNumber(1));
		
		private const _destinationToPlotTaskMap:Dictionary = new Dictionary(true);
		
		private const _singleGeom:Array = []; // reusable array for holding one item
		
		private const RECORD_INDEX:String = 'recordIndex';
		private const MIN_IMPORTANCE:String = 'minImportance';
		private const D_PROGRESS:String = 'd_progress';
		private const D_ASYNCSTATE:String = 'd_asyncState';
		override public function drawPlotAsyncIteration(task:IPlotTask):Number
		{
			var simplifyDataBounds:IBounds2D = task.dataBounds;
			var simplifyScreenBounds:IBounds2D = task.screenBounds;
			if (Demo.settings.lock_geomFiltering.value)
			{
				if (!_debugSimplifyDataBounds)
				{
					_debugSimplifyDataBounds = new Bounds2D();
					_debugSimplifyDataBounds.copyFrom(task.dataBounds);
					_debugSimplifyScreenBounds = new Bounds2D();
					_debugSimplifyScreenBounds.copyFrom(task.screenBounds);
				}
				simplifyDataBounds = _debugSimplifyDataBounds;
				simplifyScreenBounds = _debugSimplifyScreenBounds;
			}
			
			keepTrack = debug && (task['taskType'] == 0);
			if (task.iteration == 0)
			{
				if (!Demo.settings.lock_geomFiltering.value)
					_debugSimplifyDataBounds = _debugSimplifyScreenBounds = null;
				
				if (keepTrack)
					totalVertices = 0;
				task.asyncState[RECORD_INDEX] = 0;
				task.asyncState[MIN_IMPORTANCE] = getDataAreaPerPixel(simplifyDataBounds, simplifyScreenBounds) * pixellation.value;
				task.asyncState[D_PROGRESS] = new Dictionary(true);
				task.asyncState[D_ASYNCSTATE] = new Dictionary(true);
			}
			
			if (debugGridSkip)
				simplifyDataBounds = null;

			var pass:int;
			var graphics:Graphics = tempShape.graphics;
			var drawImages:Boolean = pointDataImageColumn.getInternalColumn() != null;
			var recordIndex:Number = task.asyncState[RECORD_INDEX];
			var minImportance:Number = task.asyncState[MIN_IMPORTANCE];
			var d_progress:Dictionary = task.asyncState[D_PROGRESS];
			var d_asyncState:Dictionary = task.asyncState[D_ASYNCSTATE];
			var progress:Number = 1; // set to 1 in case loop is not entered
			while (recordIndex < task.recordKeys.length)
			{
				var recordKey:IQualifiedKey = task.recordKeys[recordIndex] as IQualifiedKey;
				var geoms:Array = null;
				var value:* = geometryColumn.getValueFromKey(recordKey);
				if (value is Array)
					geoms = value;
				else if (value is GeneralizedGeometry)
				{
					geoms = _singleGeom;
					_singleGeom[0] = value;
				}
				
				for (pass = 0; pass < 2; pass++)
				{
					if (pass == 1 && !drawImages)
						break;
					
					if (geoms && geoms.length > 0)
					{
						var styleSet:Boolean = false;
						
						// draw the geom
						for (var i:int = 0; i < geoms.length; i++)
						{
							var geom:GeneralizedGeometry = geoms[i] as GeneralizedGeometry;
							if (geom)
							{
								// skip shapes that are considered unimportant at this zoom level
								if (geom.geomType == GeometryType.POLYGON && geom.bounds.getArea() < minImportance)
									continue;
								if (pass == 0)
								{
									if (!styleSet)
									{
										graphics.clear();
										fill.beginFillStyle(recordKey, graphics);
										line.beginLineStyle(recordKey, graphics);
										styleSet = true;
									}
									drawMultiPartShape(recordKey, geom, geom.getSimplifiedGeometry(minImportance, simplifyDataBounds), task.dataBounds, task.screenBounds, graphics, task.buffer);
								}
								else
								{
									drawImage(recordKey, geom.bounds.getXCenter(), geom.bounds.getYCenter(), task.dataBounds, task.screenBounds, graphics, task.buffer);
								}
							}
						}
						if (pass == 0 && styleSet)
						{
							graphics.endFill();
							task.buffer.draw(tempShape);
						}
					}
				}
				
				// this progress value will be less than 1
				progress = recordIndex / task.recordKeys.length;
				task.asyncState[RECORD_INDEX] = ++recordIndex;
				
				if (keepTrack)
					continue;
				
				// avoid doing too little or too much work per iteration 
				if (getTimer() > task.iterationStopTime)
					break; // not done yet
			}
			
			if (keepTrack)
				trace('totalVertices',totalVertices);
			
			// hack for symbol plotters
			var symbolPlottersArray:Array = symbolPlotters.getObjects();
			var ourAsyncState:Object = task.asyncState;
			for each (var plotter:IPlotter in symbolPlottersArray)
			{
				if (task.iteration == 0)
				{
					d_asyncState[plotter] = {};
					d_progress[plotter] = 0;
				}
				if (d_progress[plotter] != 1)
				{
					task.asyncState = d_asyncState[plotter];
					d_progress[plotter] = plotter.drawPlotAsyncIteration(task);
				}
				progress += d_progress[plotter];
			}
			task.asyncState = ourAsyncState;
			
			progress = progress / (1 + symbolPlottersArray.length);
			
			///////////////////////
			// demo rendering code
			
			// only render on the subset task
			if (PlotTask(task).taskType != PlotTask.TASK_TYPE_SUBSET)
				return progress;
			
			var locked:Boolean = Demo.settings.lock_tileFiltering.value || Demo.settings.lock_geomFiltering.value;
			if (!locked)
			{
				// save for next time
				lockedTileMinImportance = minImportance;
				lockedTileDataBounds.copyFrom(task.dataBounds);
			}
			
			var showDemoBounds:Boolean = Demo.settings.show_pointBounds.value || Demo.settings.show_queryBounds.value;
			if (progress == 1 && showDemoBounds)
			{
				var borderAlpha:Number = Demo.settings.alpha_border.value;
				var queryAlpha:Number = Demo.settings.alpha_queryBounds.value;
				var pointAlpha:Number = Demo.settings.alpha_pointBounds.value;
				var queryColor:Number = Demo.settings.color_queryBounds.value;
				var pointColor:Number = Demo.settings.color_pointBounds.value;
				
				// draw tile borders
				var singleTileID:Number = Demo.settings.show_singleTileID.value;
				var tiles:Array = geomTiles;
				for each (pass in [1,2])
				{
					for each (var tile:GeometryTileDescriptor in tiles)
					{
						if (isFinite(singleTileID) && tile.tileID != singleTileID)
							continue;
						if (Demo.settings.filterWithinImportanceRange.value && !tile.importanceRange.contains(lockedTileMinImportance))
							continue;
						
						graphics.clear();
						graphics.lineStyle(0,0,0);
						
						pointBounds.copyFrom(tile.pointBounds);
						task.dataBounds.projectCoordsTo(pointBounds, task.screenBounds);
						
						if (pass == 1 && Demo.settings.show_queryBounds.value)
						{
							// outer rectangle
							queryBounds.copyFrom(tile.queryBounds);
							task.dataBounds.projectCoordsTo(queryBounds, task.screenBounds);
							graphics.beginFill(queryColor, queryAlpha);
							drawBounds(graphics, queryBounds);
							drawBounds(graphics, pointBounds); // hole
							graphics.endFill();
							
							task.buffer.draw(tempShape);
						}
						
						if (pass == 2)
						{
							if (Demo.settings.show_pointBounds.value)
							{
								// inner rectangle
								graphics.beginFill(pointColor, pointAlpha);
								drawBounds(graphics, pointBounds);
								graphics.endFill();
							}
							
							// inner border (always shown if either inner or outer rectangle is drawn)
							graphics.lineStyle(1, pointColor, borderAlpha);
							drawBounds(graphics, pointBounds);
						
							task.buffer.draw(tempShape);
							
							// draw tileID
							bitmapText.textFormat.color = pointColor;
							bitmapText.text = '' + tile.tileID;
							bitmapText.x = pointBounds.getXNumericMin();
							bitmapText.y = pointBounds.getYNumericMin();
							bitmapText.width = pointBounds.getXCoverage();
							bitmapText.height = pointBounds.getYCoverage();
							colorTransform.alphaMultiplier = borderAlpha;
							bitmapText.draw(task.buffer, null, colorTransform);
						}
					}
				}
			}
			
			if (locked)
			{
				var lockColor:Number = Demo.settings.color_lockedBounds.value;
				graphics.clear();
				graphics.lineStyle(2, lockColor, 1);
				queryBounds.copyFrom(lockedTileDataBounds);
				task.dataBounds.projectCoordsTo(queryBounds, task.screenBounds);
				drawBounds(graphics, queryBounds)
				task.buffer.draw(tempShape);
			}
			
			///////////////////
			
			return progress;
		}
		private function drawBounds(graphics:Graphics, bounds:Bounds2D):void
		{
			var r:Rectangle = tempRectangle;
			bounds.getRectangle(r);
			graphics.drawRect(r.x, r.y, r.width, r.height);
		}
		
		private var lockedTileMinImportance:Number = NaN;
		private const lockedTileDataBounds:Bounds2D = new Bounds2D();
		
		private static const colorTransform:ColorTransform = new ColorTransform();
		private static const bitmapText:BitmapText = new BitmapText();
		private static const tempPoint:Point = new Point(); // reusable object
		private static const tempRectangle:Rectangle = new Rectangle(); // reusable object
		private static const pointBounds:Bounds2D = new Bounds2D(); // reusable object
		private static const queryBounds:Bounds2D = new Bounds2D(); // reusable object
		private static const tempMatrix:Matrix = new Matrix(); // reusable object

		/**
		 * This function draws a list of GeneralizedGeometry objects
		 * @param geomParts A 2-dimensional Array or Vector of objects, each having x and y properties.
		 */
		private function drawMultiPartShape(key:IQualifiedKey, geom:GeneralizedGeometry, geomParts:Object, dataBounds:IBounds2D, screenBounds:IBounds2D, graphics:Graphics, bitmapData:BitmapData):void
		{
			var geomType:String = geom.geomType;
			for (var i:int = 0; i < geomParts.length; i++)
				drawShape(key, geomParts[i], geomType, dataBounds, screenBounds, graphics, bitmapData);
		}
		/**
		 * This function draws a single geometry.
		 * @param points An Array or Vector of objects, each having x and y properties.
		 */
		private function drawShape(key:IQualifiedKey, points:Object, geomType:String, dataBounds:IBounds2D, screenBounds:IBounds2D, outputGraphics:Graphics, outputBitmapData:BitmapData):void
		{
			if (points.length == 0)
				return;

			var currentNode:Object;

			if (geomType == GeometryType.POINT)
			{
				for each (currentNode in points)
				{
					tempPoint.x = currentNode.x;
					tempPoint.y = currentNode.y;
					dataBounds.projectPointTo(tempPoint, screenBounds);
					// round coordinates for faster & more consistent rendering
					tempPoint.x = Math.round(tempPoint.x);
					tempPoint.y = Math.round(tempPoint.y);
					drawCircle(outputBitmapData, fill.color.getValueFromKey(key, Number), tempPoint.x, tempPoint.y);
				}
				return;
			}

			// prevent moveTo/lineTo from drawing a filled polygon if the shape type is line
			if (geomType == GeometryType.LINE)
				outputGraphics.endFill();

			var numPoints:int = points.length;
			var firstX:Number, firstY:Number;
			for (var vIndex:int = 0; vIndex < numPoints; vIndex++)
			{
				currentNode = points[vIndex];
				tempPoint.x = currentNode.x;
				tempPoint.y = currentNode.y;
				dataBounds.projectPointTo(tempPoint, screenBounds);
				var x:Number = tempPoint.x,
					y:Number = tempPoint.y;
				
				if (debug)
				{
					if (keepTrack)
						totalVertices++;
					x=int(x),y=int(y);
					outputGraphics.moveTo(x-1,y);
					outputGraphics.lineTo(x+1,y);
					outputGraphics.moveTo(x,y-1);
					outputGraphics.lineTo(x,y+1);
					outputGraphics.moveTo(x, y);
					continue;
				}
				
				if (vIndex == 0)
				{
					firstX = x;
					firstY = y;
					outputGraphics.moveTo(x, y);
					continue;
				}
				outputGraphics.lineTo(x, y);
			}
			
			if (!debug)
				if (geomType == GeometryType.POLYGON)
					outputGraphics.lineTo(firstX, firstY);
		}
		
		private function drawImage(key:IQualifiedKey, dataX:Number, dataY:Number, dataBounds:IBounds2D, screenBounds:IBounds2D, outputGraphics:Graphics, outputBitmapData:BitmapData):void
		{
			tempPoint.x = dataX;
			tempPoint.y = dataY;
			dataBounds.projectPointTo(tempPoint, screenBounds);
			// round coordinates for faster & more consistent rendering
			tempPoint.x = Math.round(tempPoint.x);
			tempPoint.y = Math.round(tempPoint.y);
			
			var bitmapData:BitmapData = pointDataImageColumn.getValueFromKey(key) || _missingImage;
			var w:Number = bitmapData.width;
			var h:Number = bitmapData.height;
			tempMatrix.identity();
			if (useFixedImageSize.value)
			{
				var maxSize:Number = Math.max(w, h);
				var scale:Number = iconSize.value / maxSize;
				tempMatrix.scale(scale, scale);
				tempMatrix.translate(Math.round(Math.abs(maxSize - w) / 2), Math.round(Math.abs(maxSize - h) / 2));
				w = h = iconSize.value;
			}
			tempMatrix.translate(Math.round(tempPoint.x - w / 2), Math.round(tempPoint.y - h / 2));
			outputBitmapData.draw(bitmapData, tempMatrix, null, null, null, true);
		}
		
		public function dispose():void
		{
			disposeCachedBitmaps();
		}

		// backwards compatibility 0.9.6
		[Deprecated(replacement="line")] public function set lineStyle(value:Object):void
		{
			try {
				setSessionState(line, value[0].sessionState);
			} catch (e:Error) { }
		}
		[Deprecated(replacement="fill")] public function set fillStyle(value:Object):void
		{
			try {
				setSessionState(fill, value[0].sessionState);
			} catch (e:Error) { }
		}
		[Deprecated(replacement="geometryColumn")] public function set geometry(value:Object):void
		{
			setSessionState(geometryColumn.internalDynamicColumn, value);
		}
		// backwards compatibility May 2012
		[Deprecated(replacement="iconSize")] public function set pointShapeSize(value:Number):void { iconSize.value = value * 2; }
	}
}
