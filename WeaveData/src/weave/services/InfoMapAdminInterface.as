package weave.services
{
		import flash.net.FileReference;
		import flash.utils.ByteArray;
		import flash.utils.Dictionary;
		
		import mx.controls.Alert;
		import mx.rpc.Fault;
		import mx.rpc.events.FaultEvent;
		import mx.rpc.events.ResultEvent;
		import mx.utils.StringUtil;
		
		import weave.api.objectWasDisposed;
		import weave.api.services.IAsyncService;
		import weave.core.CallbackCollection;
		import weave.services.AMF3Servlet;
		import weave.services.AsyncInvocationQueue;
		import weave.services.DelayedAsyncInvocation;
		
		public class InfoMapAdminInterface
		{
			
			private static var _thisInstance:InfoMapAdminInterface = null;
			public static function get instance():InfoMapAdminInterface
			{
				if (_thisInstance == null)
					_thisInstance = new InfoMapAdminInterface("/InfoMapServices");
				return _thisInstance;
			}
			
			public static const messageLog:Array = new Array();
			public static const messageLogCallbacks:CallbackCollection = new CallbackCollection();
			public static function messageDisplay(messageTitle:String, message:String, showPopup:Boolean):void 
			{
				// for errors, both a popupbox and addition in the Log takes place
				// for successes, only addition in Log takes place
				if (showPopup)
					Alert.show(message,messageTitle);
				
				// always add the message to the log
				if (messageTitle == null)
					messageLog.push(message);
				else
					messageLog.push(messageTitle + ": " + message);
				
				messageLogCallbacks.triggerCallbacks();
			}
			
			public function InfoMapAdminInterface(url:String)
			{
				service = new AMF3Servlet(url + "/AdminService");
				queue = new AsyncInvocationQueue();
				getRssFeeds();
			}
			
			private var queue:AsyncInvocationQueue;
			private var service:AMF3Servlet;
			
			private function generateQueryAndAddToQueue(methodName:String, parameters:Array):DelayedAsyncInvocation
			{
				var query:DelayedAsyncInvocation = new DelayedAsyncInvocation(service, methodName, parameters);
				// we want to use a queue so the admin functions will execute in the correct order.
				queue.addToQueue(query);
				// automatically display FaultEvent error messages as alert boxes
				query.addAsyncResponder(null, alertFault, query);
				return query;
			}
			// this function displays a String response from a server in an Alert box.
			private function alertResult(event:ResultEvent, token:Object = null):void
			{
				messageDisplay(null,String(event.result),false);
			}
			// this function displays an error message from a FaultEvent in an Alert box.
			private function alertFault(event:FaultEvent, token:Object = null):void
			{
				var query:DelayedAsyncInvocation = token as DelayedAsyncInvocation;
				
				var paramDebugStr:String = '';
				if (query.parameters.length > 0)
					paramDebugStr = '"' + query.parameters.join('", "') + '"';
				trace(StringUtil.substitute(
					"Received error on {0}({1}):\n\t{2}",
					query.methodName,
					paramDebugStr,
					event.fault.faultString
				));
				
				//Alert.show(event.fault.faultString, event.fault.name);
				var msg:String = event.fault.faultString;
				if (msg == "ioError")
					msg = "Received no response from the servlet.\nHas the WAR file been deployed correctly?\nExpected servlet URL: "+ service.servletURL;
				messageDisplay(event.fault.name, msg, true);
			}
			
			[Bindable]
			public var rssFeeds:Array = [];
			public function getRssFeeds():void
			{
				rssFeeds = []
				generateQueryAndAddToQueue("getRssFeeds",[]).addAsyncResponder(handleGetRssFeeds);
				function handleGetRssFeeds(event:ResultEvent,token:Object=null):void
				{
					rssFeeds = event.result as Array || [];
				}
			}
			
			
			public function addRssFeed(title:String,url:String):void
			{
				generateQueryAndAddToQueue("addRssFeed",[title,url]).addAsyncResponder(handler);
				function handler(event:ResultEvent, token:Object=null):void
				{
					Alert.show(event.result.toString());
					getRssFeeds();
				}
			}
			
			public function deleteRssFeed(url:String):void
			{
				generateQueryAndAddToQueue("deleteRssFeed",[url]).addAsyncResponder(handler);
				function handler(event:ResultEvent, token:Object=null):void
				{
					Alert.show(event.result.toString());
					getRssFeeds();
				}
			}
			
			public function deleteRssFeedWithTitle(title:String):void
			{
				generateQueryAndAddToQueue("deleteRssFeedWithTitle",[title]).addAsyncResponder(handler);
				function handler(event:ResultEvent, token:Object=null):void
				{
					Alert.show(event.result.toString());
					getRssFeeds();
				}
			}
			
			public function renameFile(filePath:String,newFileName:String,overwrite:Boolean=false):void
			{
				generateQueryAndAddToQueue("renameFile",[filePath,newFileName,overwrite]).addAsyncResponder(handler);
				function handler(event:ResultEvent, token:Object=null):void
				{
					Alert.show(event.result.toString());
				}
			}
			
			public function searchInDocuments(entities:Array, docs:Array):DelayedAsyncInvocation
			{
				var query:DelayedAsyncInvocation = generateQueryAndAddToQueue("searchInDocuments",[entities,docs]);
				return query;
			}
			
			public function addDocumentToSolr(username:String,file:FileReference):void
			{
				generateQueryAndAddToQueue("addTextDocument",[username,file.name,file.data]).addAsyncResponder(resultHandler,errorHandler);
				function resultHandler(event:ResultEvent, token:Object = null):void
				{
//					Alert.show(event.result.toString());
				}
				function errorHandler(event:FaultEvent, token:Object = null):void
				{
					Alert.show(event.fault.toString());
				}
			}
			
			public function getQueryResults(queryURL:String,filterQuery:String,sortField:String,rows:int):DelayedAsyncInvocation
			{
				var query:DelayedAsyncInvocation = generateQueryAndAddToQueue("getQueryResults",[queryURL,filterQuery,sortField,rows]);
				return query;
			}
			
			public function getNumberOfMatchedDocuments(queryURL:String,filterQuery:String):DelayedAsyncInvocation
			{
				var query:DelayedAsyncInvocation = generateQueryAndAddToQueue("getNumberOfMatchedDocuments",[queryURL,filterQuery]);
				return query;
			}
			
			public function testDropBox():DelayedAsyncInvocation
			{
				var q:DelayedAsyncInvocation = generateQueryAndAddToQueue("testDropbox",[]);
				return q;
			}
			
			public function testAddFileToDropBox():DelayedAsyncInvocation
			{
				var q:DelayedAsyncInvocation = generateQueryAndAddToQueue("testAddFileToDropBox",[]);
				return q;
			}
		}
}
