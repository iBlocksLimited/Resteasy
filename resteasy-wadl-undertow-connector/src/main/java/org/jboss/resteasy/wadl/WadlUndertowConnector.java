package org.jboss.resteasy.wadl;

import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Created by weli on 7/26/16.
 */
public class WadlUndertowConnector {
   public UndertowJaxrsServer deployToServer(UndertowJaxrsServer server, Class<? extends Application> application) {
      ApplicationPath appPath = application.getAnnotation(ApplicationPath.class);
      String path = "/";
      if (appPath != null) path = appPath.value();

      return deployToServer(server, application, path);
   }

   public UndertowJaxrsServer deployToServer(UndertowJaxrsServer server, Class<? extends Application> application, String contextPath) {
      ResteasyDeployment deployment = new ResteasyDeployment();
      deployment.setApplicationClass(application.getName());

      DeploymentInfo di = server.undertowDeployment(deployment);

      di.setClassLoader(application.getClassLoader());
      di.setContextPath(contextPath);
      di.setDeploymentName("Resteasy" + contextPath);
      return server.deploy(di);
   }
}
