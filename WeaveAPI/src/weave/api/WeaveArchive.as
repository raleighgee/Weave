/* ***** BEGIN LICENSE BLOCK *****
 *
 * This file is part of the Weave API.
 *
 * The Initial Developer of the Weave API is the Institute for Visualization
 * and Perception Research at the University of Massachusetts Lowell.
 * Portions created by the Initial Developer are Copyright (C) 2008-2012
 * the Initial Developer. All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * ***** END LICENSE BLOCK ***** */

package weave.api
{
	import flash.utils.ByteArray;
	import flash.utils.getQualifiedClassName;
	import flash.utils.getTimer;
	
	import nochump.util.zip.ZipEntry;
	import nochump.util.zip.ZipFile;
	import nochump.util.zip.ZipOutput;
	
	import weave.utils.OrderedHashMap;
	import weave.zip.CModule;
	import weave.zip.readZip;

	/**
	 * This is an interface for reading and writing data in the Weave file format.
	 * 
	 * @author adufilie
	 */
	public class WeaveArchive
	{
		/**
		 * @param input A Weave file to decode.
		 */
		public function WeaveArchive(input:ByteArray = null)
		{
			if (input)
				_readArchive(input);
		}
		
		/**
		 * This is a dynamic object containing all the files (ByteArray objects) in the archive.
		 * The property names used in this object must be valid filenames or serialize() will fail.
		 */
		public const files:OrderedHashMap = new OrderedHashMap();
		
		/**
		 * This is a dynamic object containing all the amf objects stored in the archive.
		 * The property names used in this object must be valid filenames or serialize() will fail.
		 */
		public const objects:OrderedHashMap = new OrderedHashMap();
		
		private static const FOLDER_AMF:String = "weave-amf"; // folder used for amf-encoded objects
		private static const FOLDER_FILES:String = "weave-files"; // folder used for raw files
		
		/**
		 * @private
		 */		
		private function _readArchive(fileData:ByteArray):void
		{
			
			var t:int = getTimer();
			trace('?', weave.zip.readZip(fileData));
			trace('done', getTimer() - t);
			fileData.position = 0;
			var zip:ZipFile = new ZipFile(fileData);
			for (var i:int = 0; i < zip.entries.length; i++)
			{
				var entry:ZipEntry = zip.entries[i];
				var path:Array = entry.name.split('/');
				if (path[0] == FOLDER_FILES)
					files[path[1]] = zip.getInput(entry);
				if (path[0] == FOLDER_AMF)
					objects[path[1]] = zip.getInput(entry).readObject();
			}
		}
		
		/**
		 * @private
		 */		
		private function _addZipEntry(zipOut:ZipOutput, fileName:String, fileData:ByteArray):void
		{
			var ze:ZipEntry = new ZipEntry(fileName);
			zipOut.putNextEntry(ze);
			zipOut.write(fileData);
			zipOut.closeEntry();
		}
		
		/**
		 * This function will create a ByteArray containing the objects that have been specified with setObject().
		 * @param contentType A String describing the type of content contained in the objects.
		 * @return A ByteArray in the Weave file format.
		 */
		public function serialize():ByteArray
		{
			var i:int;
			var name:String;
			var zipOut:ZipOutput = new ZipOutput();
			for (name in files)
			{
				_addZipEntry(zipOut, FOLDER_FILES + '/' + name, files[name]);
			}
			for (name in objects)
			{
				var amf:ByteArray = new ByteArray();
				amf.writeObject(objects[name]);
				_addZipEntry(zipOut, FOLDER_AMF + '/' + name, amf);
			}
			zipOut.comment = getQualifiedClassName(this);
			zipOut.finish();
			
			return zipOut.byteArray;
		}
	}
}
