'use strict';

angular.module('uploadExampleApp')
    .factory('Register', function ($resource) {
        return $resource('api/register', {}, {
        });
    });


