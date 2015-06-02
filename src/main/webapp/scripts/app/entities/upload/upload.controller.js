'use strict';

angular.module('uploadExampleApp')
    .controller('UploadController', function ($scope, Upload) {
        $scope.uploads = [];
        $scope.loadAll = function() {
            Upload.query(function(result) {
               $scope.uploads = result;
            });
        };
        $scope.loadAll();

        $scope.showUpdate = function (id) {
            Upload.get({id: id}, function(result) {
                $scope.upload = result;
                $('#saveUploadModal').modal('show');
            });
        };

        $scope.save = function () {
            if ($scope.upload.id != null) {
                Upload.update($scope.upload,
                    function () {
                        $scope.refresh();
                    });
            } else {
                Upload.save($scope.upload,
                    function () {
                        $scope.refresh();
                    });
            }
        };

        $scope.delete = function (id) {
            Upload.get({id: id}, function(result) {
                $scope.upload = result;
                $('#deleteUploadConfirmation').modal('show');
            });
        };

        $scope.confirmDelete = function (id) {
            Upload.delete({id: id},
                function () {
                    $scope.loadAll();
                    $('#deleteUploadConfirmation').modal('hide');
                    $scope.clear();
                });
        };

        $scope.refresh = function () {
            $scope.loadAll();
            $('#saveUploadModal').modal('hide');
            $scope.clear();
        };

        $scope.clear = function () {
            $scope.upload = {originalName: null, uploadedAt: null, md5sum: null, uploadComplete: null, totalChunks: null, totalSize: null, completedAt: null, id: null};
            $scope.editForm.$setPristine();
            $scope.editForm.$setUntouched();
        };
    });
