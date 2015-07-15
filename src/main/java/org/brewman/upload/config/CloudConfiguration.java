package org.brewman.upload.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Determine's some configuration parameters based on what environment variables
 * are available.
 * 
 * NOTE: This does not currently configure anything. It just logs some
 * information at startup.
 * 
 * NOTE: This configuration isn't tied to the CLOUD profile. It will run all the
 * time.
 * 
 * @author danielshiplett
 */
@Configuration
public class CloudConfiguration implements EnvironmentAware {

    private final Logger LOG = LoggerFactory
            .getLogger(CloudConfiguration.class);

    @Value("${tutum.service.hostname:#{null}}")
    private String tutumServiceHostname;

    @Value("${tutum.container.hostname:#{null}}")
    private String tutumContainerHostname;

    private String serviceName = "local";
    private String containerName = "local";

    private boolean isTutum = false;

    @Override
    public void setEnvironment(Environment environment) {

        String tmp = environment.getProperty("tutum.service.hostname");

        if (tmp != null && !tmp.isEmpty()) {
            LOG.info("tmp: {}", tmp);
        } else {
            LOG.info("tmp: null");
        }

        LOG.info("tutumServiceHostname: {}", tutumServiceHostname);
        LOG.info("tutumContainerHostname: {}", tutumContainerHostname);

        if (tutumServiceHostname != null && !tutumServiceHostname.isEmpty()) {
            isTutum = true;
            serviceName = tutumServiceHostname;
            containerName = tutumContainerHostname;
        }
    }

    public boolean isTutum() {
        return isTutum;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
