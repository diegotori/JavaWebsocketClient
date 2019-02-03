package com.example;

import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import javax.annotation.Nonnull;

import io.reactivex.Flowable;
import io.reactivex.FlowableOperator;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

/**
 * Created by son_g on 11/12/2017.
 */

public class ReactiveMoreObservables {
    @Nonnull
    public static <T> FlowableTransformer<? super T, ? extends T> behaviorRefCount() {
        return new FlowableTransformer<T, T>() {
            @Override
            public Publisher<T> apply(Flowable<T> upstream) {
                return upstream.publish().refCount();
            }
        };
    }

    @Nonnull
    public static <T> ObservableTransformer<Object, T> filterAndMap(@Nonnull final Class<T> clazz) {
        return new ObservableTransformer<Object, T>() {
            @Override
            public Observable<T> apply(Observable<Object> observable) {
                return observable
                        .filter(new Predicate<Object>() {
                            @Override
                            public boolean test(Object o) throws Exception {
                                return o != null && clazz.isInstance(o);
                            }
                        })
                        .map(new Function<Object, T>() {
                            @Override
                            public T apply(Object o) {
                                //noinspection unchecked
                                return (T) o;
                            }
                        });
            }
        };
    }

    @Nonnull
    public static Function<Throwable, Object> throwableToIgnoreError() {
        return new Function<Throwable, Object>() {
            @Override
            public Object apply(Throwable throwable) {
                return new Object();
            }
        };
    }

    public static FlowableOperator<Object, RxObjectEvent> ignoreNext() {
        return new FlowableOperator<Object, RxObjectEvent>() {
            @Override
            public Subscriber<? super RxObjectEvent> apply(final Subscriber<? super Object> observer)
                    throws Exception {
                return new Subscriber<RxObjectEvent>() {
                    @Override
                    public void onSubscribe(Subscription observer) {

                    }

                    @Override
                    public void onNext(RxObjectEvent rxObjectEvent) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }
                };
            }
        };


    }
}
