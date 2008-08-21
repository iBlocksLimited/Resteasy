/*
 * JBoss, the OpenSource J2EE webOS
 * 
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.resteasy.plugins.providers.jaxb;

import org.jboss.resteasy.core.ExceptionAdapter;
import org.jboss.resteasy.spi.LoggableFailure;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * <p>
 * A JAXB entity provider that handles classes without {@link XmlRootElement}
 * annotation. Classes which have been generated by XJC will most likely not
 * contain this annotation, In order for these classes to marshalled, they
 * must be wrapped within a {@link JAXBElement} instance. This is typically
 * accomplished by invoking a method on the class which serves as the
 * {@link XmlRegistry} and is named ObjectFactory.
 * </p>
 * <p>
 * This provider is selected when the class is annotated with an {@link XmlType} annotation
 * and <strong>not</strong> an {@link XmlRootElement} annotation.
 * </p>
 * <p>
 * This provider simplifies this task by attempting to locate the {@link XmlRegistry}
 * for the target class. By default, a JAXB implementation will create a class called
 * ObjectFactory and is located in the same package as the target class. When this
 * class is located, it will contain a "create" method that takes the object instance
 * as a parameter. For example, of the target type is called "Contact", then the
 * ObjectFactory class will have a method:</p>
 * <code>
 * public JAXBElement<Contact> createContact(Contact value);
 * </code>
 *
 * @author <a href="ryan@damnhandy.com">Ryan J. McDonough</a>
 * @version $Revision:$
 */
@Provider
@Produces(
        {"text/xml", "application/xml"})
@Consumes(
        {"text/xml", "application/xml"})
public class JAXBXmlTypeProvider extends AbstractJAXBProvider<Object>
{

   protected static final String OBJECT_FACTORY_NAME = ".ObjectFactory";

   /**
    *
    */
   @Override
   public void writeTo(Object t,
                       Class<?> type,
                       Type genericType,
                       Annotation[] annotations,
                       MediaType mediaType,
                       MultivaluedMap<String, Object> httpHeaders,
                       OutputStream entityStream) throws IOException
   {
      JAXBElement<?> result = wrapInJAXBElement(t, type);
      super.writeTo(result, type, genericType, annotations, mediaType, httpHeaders, entityStream);
   }

   /**
    *
    */
   @Override
   protected boolean isReadWritable(Class<?> type, Type genericType, Annotation[] annotations)
   {
      return (!type.isAnnotationPresent(XmlRootElement.class)
              && type.isAnnotationPresent(XmlType.class));
   }

   /**
    * Attempts to locate {@link XmlRegistry} for the XML type. Usually,
    * a class named ObjectFactory is located in the same package as the
    * type we're trying to marshall. This method simply locates this
    * class and instantiates it if found.
    *
    * @param t
    * @param type
    * @return
    */
   private Object findObjectFactory(Object t, Class<?> type)
   {
      try
      {
         StringBuilder b = new StringBuilder(type.getPackage().getName());
         b.append(OBJECT_FACTORY_NAME);
         Class<?> factoryClass = Class.forName(b.toString());
         if (factoryClass.isAnnotationPresent(XmlRegistry.class))
         {
            Object factory = factoryClass.newInstance();
            return factory;
         }
         else
         {
            throw new LoggableFailure("A valid XmlRegistry could not be located.");
         }
      }
      catch (ClassNotFoundException e)
      {
         throw new ExceptionAdapter(e);
      }
      catch (InstantiationException e)
      {
         throw new ExceptionAdapter(e);
      }
      catch (IllegalAccessException e)
      {
         throw new ExceptionAdapter(e);
      }

   }

   /**
    * If this object is managed by an XmlRegistry, this method will
    * invoke the registry and wrap the object in a JAXBElement so
    * that it can be marshalled.
    *
    * @param t
    * @param type
    * @return
    */
   protected JAXBElement<?> wrapInJAXBElement(Object t, Class<?> type)
   {
      try
      {
         Object factory = findObjectFactory(t, type);
         Method[] method = factory.getClass().getDeclaredMethods();
         for (int i = 0; i < method.length; i++)
         {
            Method current = method[i];
            if (current.getParameterTypes().length == 1
                    && current.getParameterTypes()[0].equals(type)
                    && current.getName().startsWith("create"))
            {
               Object result = current.invoke(factory, new Object[]{t});
               return JAXBElement.class.cast(result);
            }
            else
            {
               throw new LoggableFailure(String.format("The method create%s() "
                       + "was not found in the object Factory!", type));
            }
         }
      }
      catch (IllegalArgumentException e)
      {
         throw new ExceptionAdapter(e);
      }
      catch (IllegalAccessException e)
      {
         throw new ExceptionAdapter(e);
      }
      catch (InvocationTargetException e)
      {
         throw new ExceptionAdapter(e);
      }
      return null;
   }
}
