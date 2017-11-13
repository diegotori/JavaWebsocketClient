package com.example;

import com.appunite.websocket.reactivex.object.ReactiveXObjectWebSockets;
import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.reactivestreams.Publisher;

/**
 * Created by son_g on 11/12/2017.
 */

public class ReactiveSocketConnectionImpl implements ReactiveSocketConnection {
    @Nonnull
    private final ReactiveXObjectWebSockets sockets;
    @Nonnull
    private final Scheduler scheduler;

    public ReactiveSocketConnectionImpl(@Nonnull ReactiveXObjectWebSockets sockets, @Nonnull Scheduler scheduler) {
        this.sockets = sockets;
        this.scheduler = scheduler;
    }

    @Nonnull
    @Override
    public Flowable<RxObjectEvent> connection() {
        return sockets.webSocketObservable()
                .retryWhen(repeatDuration(1, TimeUnit.SECONDS));
    }

    @Nonnull
    private Function<Flowable<? extends Throwable>, Flowable<?>> repeatDuration(final long
            delay,
            @Nonnull final TimeUnit timeUnit) {
        return new Function<Flowable<? extends Throwable>, Flowable<?>>() {
            @Override
            public Flowable<?> apply(Flowable<? extends Throwable> attempts) throws Exception {
                return attempts
                        .flatMap(new Function<Throwable, Publisher<?>>() {
                            @Override
                            public Publisher<?> apply(Throwable throwable) throws Exception {
                                return Flowable.timer(delay, timeUnit, scheduler);
                            }
                        });
            }
        };
    }
}
