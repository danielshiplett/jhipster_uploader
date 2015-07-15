'use strict';

angular.module('uploadExampleApp')
.factory('CloudService', function ($resource) {
	return {
		cloudinfo: function() {
			return $resource('api/cloudinfo');
		}
	}
});
