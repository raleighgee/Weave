angular.module('aws.directives.scrollto', [])
	.directive('scrollTo', function() {
    return {
      link: function(scope, element, attrs) {
        var value = attrs.scrollTo;
        element.click(function() {
          scope.$apply(function() {
            var selector = "[scroll-bookmark='"+ value +"']";
            var element = $(selector);
            if(element.length)
              window.scrollTo(0, element[0].offsetTop +800);  // Don't want the top to be the exact element, -100 will go to the top for a little bit more
          });
        });
      }
    };
});