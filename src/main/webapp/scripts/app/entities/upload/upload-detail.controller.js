'use strict';

angular.module('uploadExampleApp')
    .controller('UploadDetailController', function ($scope, $stateParams, Upload) {
        $scope.upload = {};
        $scope.load = function (id) {
            Upload.get({id: id}, function(result) {
              $scope.upload = result;
            });
        };
        $scope.load($stateParams.id);
    });
