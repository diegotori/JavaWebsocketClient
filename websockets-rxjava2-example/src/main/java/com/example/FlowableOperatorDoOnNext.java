package com.example;

import io.reactivex.FlowableOperator;
import io.reactivex.Observer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Created by son_g on 11/12/2017.
 */

public class FlowableOperatorDoOnNext<T> implements FlowableOperator<T, T> {
    private final Observer<? super T> doOnNextObserver;

    public FlowableOperatorDoOnNext(Observer<? super T> doOnNextObserver) {
        this.doOnNextObserver = doOnNextObserver;
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> observer) throws Exception {
        return new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {}

            @Override
            public void onNext(T t) {
                doOnNextObserver.onNext(t);
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onComplete() {}
        };
    }
}
