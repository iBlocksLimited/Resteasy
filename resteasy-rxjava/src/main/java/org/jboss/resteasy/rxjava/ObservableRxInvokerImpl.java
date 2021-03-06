package org.jboss.resteasy.rxjava;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.SyncInvoker;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEventSource;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocationBuilder;
import org.jboss.resteasy.plugins.providers.sse.InboundSseEventImpl;
import org.jboss.resteasy.plugins.providers.sse.client.SseEventSourceImpl;
import org.jboss.resteasy.plugins.providers.sse.client.SseEventSourceImpl.SourceBuilder;
import org.jboss.resteasy.rxjava.i18n.Messages;

import rx.Observable;
import rx.Subscriber;

/**
 *
 *   "RxJava 1.x is now officially end-of-life (EOL). No further developments,
 *    bugfixes, enhancements, javadoc changes or maintenance will be provided by
 *    this project after version 1.3.8." - From https://github.com/ReactiveX/RxJava/releases
 *
 *    Please upgrade to resteasy-rxjava2 and RxJava 2.x.
 * @deprecated
 */
@Deprecated
public class ObservableRxInvokerImpl implements ObservableRxInvoker
{
   private static Object monitor = new Object();
   private ClientInvocationBuilder syncInvoker;
   private ScheduledExecutorService executorService;

   public ObservableRxInvokerImpl(final SyncInvoker syncInvoker, final ExecutorService executorService)
   {
      if (!(syncInvoker instanceof ClientInvocationBuilder))
      {
         throw new ProcessingException(Messages.MESSAGES.expectedClientInvocationBuilder(syncInvoker.getClass().getName()));
      }
      this.syncInvoker = (ClientInvocationBuilder) syncInvoker;
      if (executorService instanceof ScheduledExecutorService)
      {
         this.executorService = (ScheduledExecutorService) executorService;
      }
   }

   @Override
   public Observable<?> get()
   {
      return eventSourceToObservable(getEventSource(), String.class, "GET", null, getAccept());
   }

   @Override
   public <R> Observable<R> get(Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "GET", null, getAccept());
   }

   @Override
   public <R> Observable<R> get(GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "GET", null, getAccept());
   }

   @Override
   public Observable<?> put(Entity<?> entity)
   {
      return eventSourceToObservable(getEventSource(), String.class, "PUT", entity, getAccept());
   }

   @Override
   public <R> Observable<?> put(Entity<?> entity, Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "PUT", entity, getAccept());
   }

   @Override
   public <R> Observable<?> put(Entity<?> entity, GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "PUT", entity, getAccept());
   }

   @Override
   public Observable<?> post(Entity<?> entity)
   {
      return eventSourceToObservable(getEventSource(), String.class, "POST", entity, getAccept());
   }

   @Override
   public <R> Observable<?> post(Entity<?> entity, Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "POST", entity, getAccept());
   }

   @Override
   public <R> Observable<?> post(Entity<?> entity, GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "POST", entity, getAccept());
   }

   @Override
   public Observable<?> delete()
   {
      return eventSourceToObservable(getEventSource(), String.class, "DELETE", null, getAccept());
   }

   @Override
   public <R> Observable<?> delete(Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "DELETE", null, getAccept());
   }

   @Override
   public <R> Observable<?> delete(GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "DELETE", null, getAccept());
   }

   @Override
   public Observable<?> head()
   {
      return eventSourceToObservable(getEventSource(), String.class, "HEAD", null, getAccept());
   }

   @Override
   public Observable<?> options()
   {
      return eventSourceToObservable(getEventSource(), String.class, "OPTIONS", null, getAccept());
   }

   @Override
   public <R> Observable<?> options(Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "OPTIONS", null, getAccept());
   }

   @Override
   public <R> Observable<?> options(GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "OPTIONS", null, getAccept());
   }

   @Override
   public Observable<?> trace()
   {
      return eventSourceToObservable(getEventSource(), String.class, "TRACE", null, getAccept());
   }

   @Override
   public <R> Observable<?> trace(Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "TRACE", null, getAccept());
   }

   @Override
   public <R> Observable<?> trace(GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, "TRACE", null, getAccept());
   }

   @Override
   public Observable<?> method(String name)
   {
      return eventSourceToObservable(getEventSource(), String.class, name, null, getAccept());
   }

   @Override
   public <R> Observable<?> method(String name, Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, name, null, getAccept());
   }

   @Override
   public <R> Observable<?> method(String name, GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, name, null, getAccept());
   }

   @Override
   public Observable<?> method(String name, Entity<?> entity)
   {
      return eventSourceToObservable(getEventSource(), String.class, name, entity, getAccept());
   }

   @Override
   public <R> Observable<?> method(String name, Entity<?> entity, Class<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, name, entity, getAccept());
   }

   @Override
   public <R> Observable<?> method(String name, Entity<?> entity, GenericType<R> responseType)
   {
      return eventSourceToObservable(getEventSource(), responseType, name, entity, getAccept());
   }

   ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

   private <T> Observable<T> eventSourceToObservable(SseEventSourceImpl sseEventSource, Class<T> clazz, String verb, Entity<?> entity, MediaType[] mediaTypes)
   {
      Observable<T> observable = Observable.create(
         new Observable.OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> sub) {
               sseEventSource.register(
                  (InboundSseEvent e) -> {T t = e.readData(clazz, ((InboundSseEventImpl) e).getMediaType()); sub.onNext(t);},
                  (Throwable t) -> sub.onError(t),
                  () -> sub.onCompleted());
               synchronized (monitor)
               {
                  if (!sseEventSource.isOpen())
                  {
                     sseEventSource.open(null, verb, entity, mediaTypes);
                  }
               }
            }
         });
      return observable;
   }

   private <T> Observable<T> eventSourceToObservable(SseEventSourceImpl sseEventSource, GenericType<T> type, String verb, Entity<?> entity, MediaType[] mediaTypes)
   {
      Observable<T> observable = Observable.create(
            new Observable.OnSubscribe<T>() {
               @Override
               public void call(Subscriber<? super T> sub) {
                  sseEventSource.register(
                     (InboundSseEvent e) -> {T t = e.readData(type, ((InboundSseEventImpl) e).getMediaType()); sub.onNext(t);},
                     (Throwable t) -> sub.onError(t),
                     () -> sub.onCompleted());
                  synchronized (monitor)
                  {
                     if (!sseEventSource.isOpen())
                     {
                        sseEventSource.open(null, verb, entity, mediaTypes);
                     }
                  }
               }
            });
      return observable;
   }

   private SseEventSourceImpl getEventSource()
   {
      SourceBuilder builder = (SourceBuilder) SseEventSource.target(syncInvoker.getTarget());
      if (executorService != null)
      {
         builder.executor(executorService);
      }
      SseEventSourceImpl sseEventSource = (SseEventSourceImpl) builder.build();
      sseEventSource.setAlwaysReconnect(false);
      return sseEventSource;
   }

   private MediaType[] getAccept()
   {
      if (syncInvoker instanceof ClientInvocationBuilder)
      {
         ClientInvocationBuilder builder = (ClientInvocationBuilder) syncInvoker;
         List<MediaType> accept = builder.getHeaders().getAcceptableMediaTypes();
         return accept.toArray(new MediaType[accept.size()]);
      }
      else
      {
         return null;
      }
   }
}
