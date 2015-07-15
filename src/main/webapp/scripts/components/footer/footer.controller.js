'use strict';

angular.module('uploadExampleApp').controller('FooterController', function ($scope, $state, Auth, Principal, CloudService) {
	$scope.isAuthenticated = Principal.isAuthenticated;
	$scope.isInRole = Principal.isInRole;
	$scope.$state = $state;
	
	$scope.cloudinfo = CloudService.cloudinfo().get();
});
