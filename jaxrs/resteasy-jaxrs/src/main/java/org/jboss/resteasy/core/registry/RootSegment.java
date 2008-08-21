package org.jboss.resteasy.core.registry;

import org.jboss.resteasy.core.ResourceInvoker;
import org.jboss.resteasy.core.ResourceLocator;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.LoggableFailure;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.jboss.resteasy.util.IsHttpMethod;
import org.jboss.resteasy.util.PathHelper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RootSegment extends Segment
{
   protected Map<String, SimpleSegment> simpleSegments = new HashMap<String, SimpleSegment>();
   protected Map<String, PathParamSegment> resourceExpressions = new HashMap<String, PathParamSegment>();
   protected Map<String, PathParamSegment> locatorExpressions = new HashMap<String, PathParamSegment>();

   @Override
   protected boolean isEmpty()
   {
      return super.isEmpty() && simpleSegments.size() == 0 && resourceExpressions.size() == 0 && locatorExpressions.size() == 0;
   }

   protected void addPath(String[] segments, int index, ResourceInvoker invoker)
   {
      String segment = segments[index];
      Matcher withPathParam = PathHelper.URI_PARAM_PATTERN.matcher(segment);
      if (withPathParam.find())
      {
         String expression = recombineSegments(segments, index);

         if (invoker instanceof ResourceLocator)
         {
            PathParamSegment segmentNode = locatorExpressions.get(expression);
            if (segmentNode != null)
               throw new LoggableFailure("You cannot have 2 locators for same path: " + expression);
            segmentNode = new PathParamSegment(expression);
            segmentNode.locator = (ResourceLocator) invoker;
            locatorExpressions.put(segmentNode.getPathExpression(), segmentNode);
         }
         else
         {
            PathParamSegment segmentNode = resourceExpressions.get(expression);
            if (segmentNode == null)
            {
               segmentNode = new PathParamSegment(expression);
               resourceExpressions.put(segmentNode.getPathExpression(), segmentNode);
            }
            segmentNode.methods.add((ResourceMethod) invoker);
         }
      }
      else
      {
         SimpleSegment segmentNode = simpleSegments.get(segment);
         if (segmentNode == null)
         {
            segmentNode = new SimpleSegment(segment);
            simpleSegments.put(segment, segmentNode);
         }
         if (segments.length > index + 1)
         {
            segmentNode.addPath(segments, index + 1, invoker);
         }
         else
         {
            if (invoker instanceof ResourceLocator)
            {
               segmentNode.locator = (ResourceLocator) invoker;
            }
            else
            {
               segmentNode.methods.add((ResourceMethod) invoker);
            }
         }
      }

   }

   private String recombineSegments(String[] segments, int index)
   {
      String expression = "";
      boolean first = true;
      for (int i = index; i < segments.length; i++)
      {
         if (first)
         {
            first = false;
         }
         else
         {
            expression += "/";
         }
         expression += segments[i];
      }
      return expression;
   }

   protected boolean isLocator(Method method)
   {
      return IsHttpMethod.getHttpMethods(method) == null;
   }


   protected ResourceInvoker removePath(String[] segments, int index, Method method)
   {
      String segment = segments[index];
      Matcher withPathParam = PathHelper.URI_PARAM_PATTERN.matcher(segment);
      if (withPathParam.find())
      {
         String expression = recombineSegments(segments, index);
         if (isLocator(method))
         {
            PathParamSegment rtn = locatorExpressions.remove(expression);
            if (rtn != null) return rtn.locator;
            else return null;
         }
         else
         {
            PathParamSegment node = resourceExpressions.get(expression);
            if (node == null) return null;
            Iterator<ResourceMethod> it = node.methods.iterator();
            try
            {
               while (it.hasNext())
               {
                  ResourceMethod invoker = it.next();
                  if (invoker.getMethod().equals(method))
                  {
                     it.remove();
                     return invoker;
                  }

               }
            }
            finally
            {
               if (node.isEmpty()) resourceExpressions.remove(expression);
            }
         }
      }
      else
      {
         SimpleSegment segmentNode = simpleSegments.get(segment);
         if (segmentNode == null)
         {
            return null;
         }
         if (segments.length > index + 1)
         {
            try
            {
               return segmentNode.removePath(segments, index + 1, method);
            }
            finally
            {
               if (segmentNode.isEmpty()) simpleSegments.remove(segment);
            }
         }
         else
         {
            try
            {
               if (isLocator(method))
               {
                  ResourceLocator loc = segmentNode.locator;
                  segmentNode.locator = null;
                  return loc;
               }
               else
               {
                  Iterator<ResourceMethod> it = segmentNode.methods.iterator();
                  while (it.hasNext())
                  {
                     ResourceMethod invoker = it.next();
                     if (invoker.getMethod().equals(method))
                     {
                        it.remove();
                        return invoker;
                     }

                  }
               }
            }
            finally
            {
               if (segmentNode.isEmpty()) simpleSegments.remove(segment);
            }
         }
      }
      return null;

   }


   public void addPath(String path, ResourceInvoker invoker)
   {
      if (path.startsWith("/")) path = path.substring(1);

      MultivaluedMapImpl<String, String> pathParamExpr = new MultivaluedMapImpl<String, String>();
      StringBuffer newPath = pullPathParamExpressions(path, pathParamExpr);
      path = newPath.toString();
      String[] segments = path.split("/");

      for (int i = 0; i < segments.length; i++)
      {
         segments[i] = putBackPathParamExpressions(segments[i], pathParamExpr);
      }
      addPath(segments, 0, invoker);
   }

   public ResourceInvoker removePath(String path, Method method)
   {
      if (path.startsWith("/")) path = path.substring(1);

      MultivaluedMapImpl<String, String> pathParamExpr = new MultivaluedMapImpl<String, String>();
      StringBuffer newPath = pullPathParamExpressions(path, pathParamExpr);
      path = newPath.toString();
      String[] segments = path.split("/");

      for (int i = 0; i < segments.length; i++)
      {
         segments[i] = putBackPathParamExpressions(segments[i], pathParamExpr);
      }
      return removePath(segments, 0, method);
   }

   protected ResourceInvoker matchChildren(HttpRequest request, String path, int start)
   {
      String simpleSegment = null;
      if (start == path.length())
      {
         simpleSegment = "";
      }
      else
      {
         int endOfSegmentIndex = path.indexOf('/', start);
         if (endOfSegmentIndex > -1) simpleSegment = path.substring(start, endOfSegmentIndex);
         else simpleSegment = path.substring(start);
      }

      Failure lastFailure = null;

      SimpleSegment segment = simpleSegments.get(simpleSegment);
      if (segment != null)
      {
         try
         {
            return segment.matchSimple(request, path, start);
         }
         catch (Failure e)
         {
            lastFailure = e;
         }
      }

      for (PathParamSegment pathParamSegment : resourceExpressions.values())
      {
         try
         {
            return pathParamSegment.matchPattern(request, path, start);
         }
         catch (Failure e)
         {
            lastFailure = e;
         }
      }
      for (PathParamSegment pathParamSegment : locatorExpressions.values())
      {
         try
         {
            return pathParamSegment.matchPattern(request, path, start);
         }
         catch (Failure e)
         {
            lastFailure = e;
         }
      }
      if (lastFailure != null) throw lastFailure;
      throw new Failure("Could not find resource for path: " + path, HttpResponseCodes.SC_NOT_FOUND);
   }


   public ResourceInvoker matchRoot(HttpRequest request)
   {
      int start = 0;
      return matchRoot(request, start);
   }

   public ResourceInvoker matchRoot(HttpRequest request, int start)
   {
      String path = request.getPreprocessedPath();
      if (start < path.length() && path.charAt(start) == '/') start++;
      return matchChildren(request, path, start);
   }


   private static StringBuffer pullPathParamExpressions(String path, MultivaluedMapImpl<String, String> pathParamExpr)
   {
      Matcher matcher = PathHelper.URI_PARAM_WITH_REGEX_PATTERN.matcher(path);
      StringBuffer newPath = new StringBuffer();
      while (matcher.find())
      {
         String name = matcher.group(1);
         String regex = matcher.group(3);
         pathParamExpr.add(name, regex);
         matcher.appendReplacement(newPath, "{$1:x}");
      }
      matcher.appendTail(newPath);
      return newPath;
   }

   private static String putBackPathParamExpressions(String path, MultivaluedMapImpl<String, String> pathParamExpr)
   {
      Matcher matcher = PathHelper.URI_PARAM_WITH_REGEX_PATTERN.matcher(path);
      StringBuffer newPath = new StringBuffer();
      while (matcher.find())
      {
         String name = matcher.group(1);
         String val = pathParamExpr.get(name).remove(0);
         matcher.appendReplacement(newPath, "{$1:" + val + "}");
      }
      matcher.appendTail(newPath);
      return newPath.toString();
   }

}
