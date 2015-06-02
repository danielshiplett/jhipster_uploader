'use strict';

angular.module('uploadExampleApp')
    .controller('LogoutController', function (Auth) {
        Auth.logout();
    });
