package org.jboss.resteasy.test.exception;

import java.lang.reflect.ReflectPermission;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.NotSupportedException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.test.exception.resource.ClosedResponseHandlingPleaseMapExceptionMapper;
import org.jboss.resteasy.test.exception.resource.ClosedResponseHandlingResource;
import org.jboss.resteasy.utils.PermissionUtil;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @tpSubChapter Resteasy-client
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.6.3
 * @tpTestCaseDetails Regression test for RESTEASY-1142
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @author <a href="jonas.zeiger@talpidae.net">Jonas Zeiger</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClosedResponseHandlingTest {

   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = TestUtil.prepareArchive(ClosedResponseHandlingTest.class.getSimpleName());
      war.addClass(ClosedResponseHandlingTest.class);
      war.addPackage(ClosedResponseHandlingResource.class.getPackage());
      war.addClass(PortProviderUtil.class);
      war.addAsManifestResource(PermissionUtil.createPermissionsXmlAsset(
           new ReflectPermission("suppressAccessChecks")
      ), "permissions.xml");

      return TestUtil.finishContainerPrepare(war, null, ClosedResponseHandlingResource.class,
            ClosedResponseHandlingPleaseMapExceptionMapper.class);
   }

   /**
    * @tpTestDetails RESTEasy client errors that result in a closed Response are correctly handled.
    * @tpPassCrit A NotAcceptableException is returned
    * @tpSince RESTEasy 3.6.3
    */
   @Test(expected = NotAcceptableException.class)
   public void testNotAcceptable() {
      new ResteasyClientBuilder().build().target(generateURL("/testNotAcceptable")).request().get(String.class);
   }

   /**
    * @tpTestDetails Closed Response instances should be handled correctly with full tracing enabled.
    * @tpPassCrit A NotSupportedException is returned
    * @tpSince RESTEasy 3.6.3
    */
   @Test(expected = NotSupportedException.class)
   public void testNotSupportedTraced() {
      new ResteasyClientBuilder().build().target(generateURL("/testNotSupportedTraced")).request().get(String.class);
   }

   private static String generateURL(String path) {
      return PortProviderUtil.generateURL(path, ClosedResponseHandlingTest.class.getSimpleName());
   }
}
