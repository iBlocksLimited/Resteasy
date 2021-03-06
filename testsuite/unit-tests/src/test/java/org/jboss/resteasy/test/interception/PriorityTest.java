package org.jboss.resteasy.test.interception;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.jboss.resteasy.core.interception.ClientResponseFilterRegistry;
import org.jboss.resteasy.core.interception.ContainerResponseFilterRegistry;
import org.jboss.resteasy.core.interception.JaxrsInterceptorRegistry;
import org.jboss.resteasy.core.interception.LegacyPrecedence;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.test.interception.resource.FakeHttpServer;
import org.jboss.resteasy.test.interception.resource.PriorityClientRequestFilter1;
import org.jboss.resteasy.test.interception.resource.PriorityClientRequestFilter2;
import org.jboss.resteasy.test.interception.resource.PriorityClientRequestFilter3;
import org.jboss.resteasy.test.interception.resource.PriorityClientResponseFilter1;
import org.jboss.resteasy.test.interception.resource.PriorityClientResponseFilter2;
import org.jboss.resteasy.test.interception.resource.PriorityClientResponseFilter3;
import org.jboss.resteasy.test.interception.resource.PriorityContainerResponseFilter1;
import org.jboss.resteasy.test.interception.resource.PriorityContainerResponseFilter2;
import org.jboss.resteasy.test.interception.resource.PriorityContainerResponseFilter3;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * @tpSubChapter Interception tests
 * @tpChapter Unit tests
 * @tpTestCaseDetails Check functionality of Priority annotation on filter classes. Use more classes with different value in Priority annotation.
 * @tpSince RESTEasy 3.0.16
 */
public class PriorityTest {

   private static final String ERROR_MESSAGE = "RESTEasy uses filter in wrong older";

   @Rule
   public FakeHttpServer fakeHttpServer = new FakeHttpServer();

   /**
    * @tpTestDetails Test for classes implements ContainerResponseFilter.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testPriority() throws Exception {
      ContainerResponseFilterRegistry containerResponseFilterRegistry = new ContainerResponseFilterRegistry(new ResteasyProviderFactory(), new LegacyPrecedence());
      ClientResponseFilterRegistry clientResponseFilterRegistry = new ClientResponseFilterRegistry(new ResteasyProviderFactory());
      JaxrsInterceptorRegistry<ClientRequestFilter> clientRequestFilterRegistry = new JaxrsInterceptorRegistry<ClientRequestFilter>(new ResteasyProviderFactory(), ClientRequestFilter.class);

      containerResponseFilterRegistry.registerClass(PriorityContainerResponseFilter2.class);
      containerResponseFilterRegistry.registerClass(PriorityContainerResponseFilter1.class);
      containerResponseFilterRegistry.registerClass(PriorityContainerResponseFilter3.class);

      ContainerResponseFilter[] containerResponseFilters = containerResponseFilterRegistry.postMatch(null, null);
      assertTrue(ERROR_MESSAGE, containerResponseFilters[0] instanceof PriorityContainerResponseFilter3);
      assertTrue(ERROR_MESSAGE, containerResponseFilters[1] instanceof PriorityContainerResponseFilter2);
      assertTrue(ERROR_MESSAGE, containerResponseFilters[2] instanceof PriorityContainerResponseFilter1);

      clientResponseFilterRegistry.registerClass(PriorityClientResponseFilter3.class);
      clientResponseFilterRegistry.registerClass(PriorityClientResponseFilter1.class);
      clientResponseFilterRegistry.registerClass(PriorityClientResponseFilter2.class);

      ClientResponseFilter[] clientResponseFilters = clientResponseFilterRegistry.postMatch(null, null);
      assertTrue(ERROR_MESSAGE, clientResponseFilters[0] instanceof PriorityClientResponseFilter3);
      assertTrue(ERROR_MESSAGE, clientResponseFilters[1] instanceof PriorityClientResponseFilter2);
      assertTrue(ERROR_MESSAGE, clientResponseFilters[2] instanceof PriorityClientResponseFilter1);

      clientRequestFilterRegistry.registerClass(PriorityClientRequestFilter3.class);
      clientRequestFilterRegistry.registerClass(PriorityClientRequestFilter1.class);
      clientRequestFilterRegistry.registerClass(PriorityClientRequestFilter2.class);

      ClientRequestFilter[] clientRequestFilters = clientRequestFilterRegistry.postMatch(null, null);
      assertTrue(ERROR_MESSAGE, clientRequestFilters[0] instanceof PriorityClientRequestFilter1);
      assertTrue(ERROR_MESSAGE, clientRequestFilters[1] instanceof PriorityClientRequestFilter2);
      assertTrue(ERROR_MESSAGE, clientRequestFilters[2] instanceof PriorityClientRequestFilter3);

   }

   /**
    * @tpTestDetails Test for classes implements ClientRequestFilter.
    * @tpSince RESTEasy 3.0.16
    */
   @Test
   public void testPriorityOverride() {
      JaxrsInterceptorRegistry<ClientRequestFilter> clientRequestFilterRegistry = new JaxrsInterceptorRegistry<ClientRequestFilter>(new ResteasyProviderFactory(), ClientRequestFilter.class);

      clientRequestFilterRegistry.registerClass(PriorityClientRequestFilter3.class, 100);
      clientRequestFilterRegistry.registerClass(PriorityClientRequestFilter1.class, 200);
      clientRequestFilterRegistry.registerClass(PriorityClientRequestFilter2.class, 300);

      ClientRequestFilter[] clientRequestFilters = clientRequestFilterRegistry.postMatch(null, null);
      assertTrue(ERROR_MESSAGE, clientRequestFilters[0] instanceof PriorityClientRequestFilter3);
      assertTrue(ERROR_MESSAGE, clientRequestFilters[1] instanceof PriorityClientRequestFilter1);
      assertTrue(ERROR_MESSAGE, clientRequestFilters[2] instanceof PriorityClientRequestFilter2);
   }

   @Test
   public void testClientRequestFilterPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         StringBuilder result = new StringBuilder();
         webTarget.register(new ClientRequestFilter()
         {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException
            {
               result.append("K");
            }
         }, 1);
         webTarget.register(new ClientRequestFilter()
         {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException
            {
               result.append("O");
            }
         }, 0);
         webTarget.request().get().close();
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testClientResponseFilterPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         StringBuilder result = new StringBuilder();
         webTarget.register(new ClientResponseFilter()
         {
            @Override
            public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
                  throws IOException
            {
               result.append("O");
            }
         }, 1);
         webTarget.register(new ClientResponseFilter()
         {
            @Override
            public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext)
                  throws IOException
            {
               result.append("K");
            }
         }, 0);
         webTarget.request().get().close();
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testReaderInterceptorPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         webTarget.register((ClientResponseFilter) (containerRequestContext, containerResponseContext) -> {
            containerResponseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            containerResponseContext.setEntityStream(new ByteArrayInputStream("hello".getBytes()));
         });
         StringBuilder result = new StringBuilder();
         webTarget.register(new ReaderInterceptor()
         {
            @Override
            public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException
            {
               result.append("K");
               return context.proceed();
            }
         }, 1);
         webTarget.register(new ReaderInterceptor()
         {

            @Override
            public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException
            {
               result.append("O");
               return context.proceed();
            }
         }, 0);
         webTarget.request().get().readEntity(String.class);
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testWriterPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         StringBuilder result = new StringBuilder();
         webTarget.register(new WriterInterceptor()
         {
            @Override
            public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException
            {
               result.append("K");
               context.proceed();
            }
         }, 1);
         webTarget.register(new WriterInterceptor()
         {
            @Override
            public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException
            {
               result.append("O");
               context.proceed();
            }
         }, 0);
         webTarget.request().post(Entity.text("Hello")).close();
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testMessageBodyReaderPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         webTarget.register((ClientResponseFilter) (containerRequestContext, containerResponseContext) -> {
            containerResponseContext.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN);
            containerResponseContext.setEntityStream(new ByteArrayInputStream("hello".getBytes()));
         });
         StringBuilder result = new StringBuilder();
         webTarget.register(new MessageBodyReader<String>()
         {
            @Override
            public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
            {
               result.append("K");
               return false;
            }

            @Override
            public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                  MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                  throws IOException, WebApplicationException
            {
               return null;
            }
         }, 1);
         webTarget.register(new MessageBodyReader<String>()
         {
            @Override
            public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
            {
               result.append("O");
               return false;
            }

            @Override
            public String readFrom(Class<String> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                  MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
                  throws IOException, WebApplicationException
            {
               return null;
            }
         }, 0);
         webTarget.request().get().readEntity(String.class);
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testMessageBodyWriterPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         StringBuilder result = new StringBuilder();
         webTarget.register(new MessageBodyWriter<String>()
         {
            @Override
            public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
            {
               result.append("K");
               return false;
            }

            @Override
            public void writeTo(String t, Class<?> type, Type genericType, Annotation[] annotations,
                  MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                  throws IOException, WebApplicationException
            {
            }
         }, 1);
         webTarget.register(new MessageBodyWriter<String>()
         {
            @Override
            public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
            {
               result.append("O");
               return false;
            }

            @Override
            public void writeTo(String t, Class<?> type, Type genericType, Annotation[] annotations,
                  MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
                  throws IOException, WebApplicationException
            {
            }
         }, 0);
         webTarget.request().post(Entity.text("Hello")).close();
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testContextResolverPriorityOverride()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         WebTarget webTarget = client.target("http://www.test.com");
         webTarget.register(new ClientRequestFilter()
         {
            @Context
            Providers providers;

            @Override
            public void filter(ClientRequestContext requestContext) throws IOException
            {
               providers.getContextResolver(String.class, MediaType.WILDCARD_TYPE).getContext(getClass());
            }
         });
         StringBuilder result = new StringBuilder();
         webTarget.register(new ContextResolver<String>()
         {
            @Override
            public String getContext(Class<?> type)
            {
               result.append("O");
               return null;
            }
         }, 0);
         webTarget.register(new ContextResolver<String>()
         {
            @Override
            public String getContext(Class<?> type)
            {
               result.append("K");
               return null;
            }
         }, 1);
         webTarget.request().get().close();
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }

   @Test
   public void testContextResolverPriorityOverride_2()
   {
      Client client = ClientBuilder.newClient();
      try
      {
         fakeHttpServer.start();

         WebTarget webTarget = client.target("http://" + fakeHttpServer.getHostAndPort());
         webTarget.register(new ClientRequestFilter()
         {
            @Context
            Providers providers;

            @Override
            public void filter(ClientRequestContext requestContext) throws IOException
            {
               providers.getContextResolver(String.class, MediaType.WILDCARD_TYPE).getContext(getClass());
            }
         });
         StringBuilder result = new StringBuilder();
         webTarget.register(new ContextResolver<String>()
         {
            @Override
            public String getContext(Class<?> type)
            {
               result.append("K");
               return null;
            }
         }, 1);
         webTarget.register(new ContextResolver<String>()
         {
            @Override
            public String getContext(Class<?> type)
            {
               result.append("O");
               return null;
            }
         }, 0);
         webTarget.request().get().close();
         Assert.assertEquals("OK", result.toString());
      }
      finally
      {
         client.close();
      }
   }
}
