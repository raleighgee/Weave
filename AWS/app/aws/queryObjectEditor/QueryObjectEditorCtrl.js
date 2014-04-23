/**
 * This controller controls the queryObject (json ) Editor
 */
angular.module('aws.queryObjectEditor', [])
.controller("QueryObjectEditorCtrl", function($scope, queryService){
	
	var user= null;
	var projectDescription = "";
	$scope.editedJson = null;
	$scope.tempEditor;
	$scope.$watch('username', function(){
		user = $scope.username;
	});
	
	$scope.$watch('username', function(){
		if(!(angular.isUndefined($scope.username)))
		user = $scope.username;
	});
	
//	$scope.$watch(function(){
//		return $scope.currentqueryselected;
//	}, function(){
//		$scope.currentjson = $scope.currentqueryselected;
//		
//	});
	
//	$scope.$watch(function(){
//		return $scope.currentJson;
//	}, function(){
//		$scope.currentQuerySelected = $scope.currentJson;
//		
//	});
	
	$scope.$watch(function(){
		return $scope.editedJson;
	}, function(){
		
		if($scope.editedJson != null)
		$scope.currentJson = $scope.editedJson;
		console.log("watching edited", $scope.currentJson);
		
	});

	$scope.exportToJSONFile = function() {
		console.log("exporting");
		var blob = new Blob([ JSON.stringify($scope.$scope.editedJson) ], {
			type : "text/plain;charset=utf-8"
		});
		saveAs(blob, "QueryObject.json");
	};
	/*****************************BUTTON CONTROLS******************************/
	
	$scope.saveToProject = function(){
		var queryObjectContent = [];
		var queryObjectTitle = [];
		
		console.log("currentJson", $scope.currentJson);
		
		$scope.editedJson = $scope.tempEditor.get();
		console.log("editedJson", $scope.editedJson);
		
		var singlequeryObjectTitle = $scope.editedJson.title;
		queryObjectTitle.push(singlequeryObjectTitle);
		
		
		var singlequeryObjectContent = JSON.stringify($scope.editedJson);//the actual json
		queryObjectContent.push(singlequeryObjectContent);
		
		if(angular.isUndefined(user))
			user = "Awesome User";
		
		if(!(angular.isUndefined($scope.editedJson.projectDescription)))
			projectDescription = $scope.editedJson.projectDescription;
		else
			projectDescription = "This project has no description";
		
		console.log("user", user);
		console.log("projectDescription", projectDescription);
		var projectName = $scope.currentProjectSelected;
//		console.log("sendingProject",$scope.currentProjectSelected);
//		var singlequeryObjectTitle = $scope.currentQuerySelected.title;
//		queryObjectTitle.push(singlequeryObjectTitle);
//		console.log("sendingTitle",$scope.currentQuerySelected.title);
//		var singlequeryObjectContent = JSON.stringify($scope.currentQuerySelected);//the actual json
//		queryObjectContent.push(singlequeryObjectContent);
//		console.log("sendingContent", $scope.currentQuerySelected);
//		var userName = "USerNameFOO";
//		
		//calling the service
		
		queryService.insertQueryObjectToProject(user, projectName, projectDescription, queryObjectTitle, queryObjectContent);
	};
});