'use strict';

angular.module('uploadExampleApp')
    .config(function ($stateProvider) {
        $stateProvider
            .state('uploader', {
                parent: 'site',
                url: '/uploader',
                data: {
                    roles: []
                },
                views: {
                    'content@': {
                        templateUrl: 'scripts/app/uploader/uploader.html',
                        controller: 'UploaderController'
                    }
                },
                resolve: {
                }
            });
    });
