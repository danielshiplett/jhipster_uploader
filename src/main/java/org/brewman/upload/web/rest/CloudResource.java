package org.brewman.upload.web.rest;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;

import org.brewman.upload.config.CloudConfiguration;
import org.brewman.upload.security.AuthoritiesConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CloudResource {

    @Inject
    private CloudConfiguration cloudConfiguration;

    private class CloudInfo {
        private String serviceName;
        private String containerName;

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }
    }

    @RequestMapping(value = "/cloudinfo", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @RolesAllowed(AuthoritiesConstants.ADMIN)
    public ResponseEntity<CloudInfo> getCloudInfo() {
        CloudInfo ci = new CloudInfo();
        ci.setServiceName(cloudConfiguration.getServiceName());
        ci.setContainerName(cloudConfiguration.getContainerName());

        return new ResponseEntity<CloudInfo>(ci, HttpStatus.OK);
    }
}
