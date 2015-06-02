'use strict';

angular.module('uploadExampleApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('upload', {
                parent: 'entity',
                url: '/upload',
                data: {
                    roles: ['ROLE_USER'],
                    pageTitle: 'uploadExampleApp.upload.home.title'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/upload/uploads.html',
                        controller: 'UploadController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('upload');
                        return $translate.refresh();
                    }]
                }
            })
            .state('uploadDetail', {
                parent: 'entity',
                url: '/upload/:id',
                data: {
                    roles: ['ROLE_USER'],
                    pageTitle: 'uploadExampleApp.upload.detail.title'
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/entities/upload/upload-detail.html',
                        controller: 'UploadDetailController'
                    }
                },
                resolve: {
                    translatePartialLoader: ['$translate', '$translatePartialLoader', function ($translate, $translatePartialLoader) {
                        $translatePartialLoader.addPart('upload');
                        return $translate.refresh();
                    }]
                }
            });
    });
