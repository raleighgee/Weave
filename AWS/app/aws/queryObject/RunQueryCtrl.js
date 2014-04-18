var queryHandler = undefined;
/**
 * RunQueryCtrl. This controller manages the run of queries.
 */

QueryObject.controller("RunQueryCtrl_2", function($scope, queryService) {
			
		$scope.runQuery = function(){
			queryHandler = new aws.QueryHandler(queryService.queryObject);
          
            // doesn't work to remove weave instance -> $scope.weaveInstancePanel = "";
            // Probably need to put a broadcast event here? to tell weave instance panel to die.

			queryHandler.runQuery();
		};
		
		
		$scope.updateVisualizations = function(){
			if(queryHandler) {
				queryHandler.updateVisualizations(queryService.queryObject);
			}
		};
		
		$scope.clearWeave = function(){
			if (queryHandler != undefined) {
				queryHandler.clearWeave();
			}
		};
});

QueryObject.controller("ErrorLogCtrl", function($scope) {
	
	
	$scope.alerts;

    $scope.$watch(function() {
		return aws.errorReport;
	}, function() {
		if(aws.errorReport == "") {
		
		} else {
			if($scope.alerts != undefined) {
				$scope.alerts.unshift({ type : 'danger', msg: aws.errorReport});
			} else {
				$scope.alerts = [{ type : 'danger', msg: aws.errorReport}];
			}
		}
		aws.errorReport = ""; // we do this so next time around the message is guaranteed to be different
	});
});