package org.jboss.resteasy.client.exception;

/**
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 * Copyright Jul 28, 2012
 */
public class ResteasyConnectionClosedException extends ResteasyIOException
{
   private static final long serialVersionUID = -5711578608757689465L;

   public ResteasyConnectionClosedException()
   {
   }

   public ResteasyConnectionClosedException(final String message)
   {
      super(message);
   }

   public ResteasyConnectionClosedException(final String message, final Throwable cause)
   {
      super(message, cause);
   }

   public ResteasyConnectionClosedException(final Throwable cause)
   {
      super(cause);
   }
}
