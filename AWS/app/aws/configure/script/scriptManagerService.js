angular.module('aws.configure.script')
  .service("scriptManagerService", ['$q', '$rootScope', function($q, scope) {
     
	  var scriptManagementURL = '/WeaveAnalystServices/ScriptManagementServlet';
	  this.dataObject = {
        scriptName: ""
      };
      var that = this;
      var refreshNeeded = true;
      
      this.getListOfScripts = function() {
        if(!refreshNeeded && this.dataObject.listOfScripts){
          return this.dataObject.listOfScripts;
        }
        var deferred = $q.defer();
        aws.queryService(scriptManagementURL, 'getListOfScripts', {}, function(result) {
          that.dataObject.listOfScripts = result;
          scope.$apply(function() {
            deferred.resolve(result);
          });
        });
        refreshNeeded = false;
        return deferred.promise;
      };
      
      this.refreshScriptInfo = function(scriptName){
        if(scriptName == this.dataObject.scriptName){
          return;
        }
        this.dataObject.scriptName = scriptName;
        this.getScriptMetadata();
        this.getScript();
      };
      
      this.uploadNewScript = function(file){
        var deferred = $q.defer();
        aws.queryService(scriptManagementURL, 'uploadNewScript', {
        															scriptName : file.filename, 
        															content : file.contents
        														  },
        														function(result){
          console.log(result); // currently just string returned from servlet
          scope.$safeApply(function() { deferred.resolve(result); });
        });
        refreshNeeded = true;
        return deferred.promise;
      };
      
      
      /**
       * This function wraps the async aws getListOfScripts function into
       * an angular defer/promise So that the UI asynchronously wait for
       * the data to be available...
       */
       this.getScriptMetadata = function(){
        var deferred = $q.defer();
        aws.queryService(scriptManagementURL, 'getScriptMetadata', { scriptName : this.dataObject.scriptName }, function(result) {
          that.dataObject.scriptMetadata = result;
          console.log(result);
          scope.$safeApply(function() { deferred.resolve(result); });
        });
        return deferred.promise;
      };
      
      this.getScript = function(){
        var deferred = $q.defer();
        aws.queryService(scriptManagementURL, 'getScript', { scriptName : this.dataObject.scriptName }, function(result){
          that.dataObject.scriptContent = result;
          scope.$safeApply(function(){deferred.resolve(result);});
        });
        return deferred.promise;
      };
      
      this.saveChangedMetadata = function(metadata){
      
    	  var deferred = $q.defer();
        
    	  this.dataObject.scriptMetadata = metadata;
        
    	  aws.queryService(scriptManagementURL, 'saveMetadata', { scriptName : this.dataObject.scriptName , metadata : this.dataObject.scriptMetadata }, function(result){
          
    		  scope.$safeApply(function(result){
            
    			  if(result.error){
    				  deferred.reject();
    			  } else{
    				  deferred.resolve(true);
    			  }
    		  });

    	  });
        
    	  return deferred.promise;
      
      };
    }
  ]);