package com.example;

import com.appunite.websocket.reactivex.ReactiveXMoreObservables;
import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventConn;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventDisconnected;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventMessage;
import com.example.model.DataMessage;
import com.example.model.PingMessage;
import com.example.model.RegisterMessage;
import com.example.model.RegisteredMessage;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Created by son_g on 11/12/2017.
 */

public class ReactiveSocket {

    public static final Logger LOGGER = Logger.getLogger("Rx");

    private final Observable<RxObjectEvent> events;
    private final Flowable<Object> connection;
    private final BehaviorSubject<RxObjectEventConn> connectedAndRegistered;
    @Nonnull
    private final Scheduler scheduler;

    public ReactiveSocket(@Nonnull ReactiveSocketConnection socketConnection, @Nonnull Scheduler
            scheduler) {
        this.scheduler = scheduler;
        final PublishSubject<RxObjectEvent> events = PublishSubject.create();
        connection = socketConnection.connection()
                .lift(new FlowableOperatorDoOnNext<>(events))
                .lift(ReactiveMoreObservables.ignoreNext())
                .compose(ReactiveMoreObservables.behaviorRefCount());
        this.events = events;

        final Observable<RxObjectEventMessage> registeredMessage = events
                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventMessage.class))
                .filter(new FilterRegisterMessage());

//        final Observable<RxObjectEventDisconnected> disconnectedMessage = events
//                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventDisconnected.class));

        connectedAndRegistered = BehaviorSubject.create();
//        disconnectedMessage
        events
//                .map(new Function<RxObjectEventDisconnected, RxObjectEventConn>() {
//                    @Override
//                    public RxObjectEventConn apply(RxObjectEventDisconnected rxEventDisconnected) {
//                        //TODO: This shouldn't return null!!!
////                        return null;
////                        return new RxObjectEvent() {
////                            @Override
////                            public String toString() {
////                                return null;
////                            }
////                        };
//                    }
//                })
//                .filter(new Predicate<RxObjectEvent>() {
//                    @Override
//                    public boolean test(RxObjectEvent rxObjectEvent) throws Exception {
//                        return rxObjectEvent instanceof RxObjectEventConnected;
//                    }
//                })
//                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventDisconnected.class));
                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventConnected.class))
//                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventConn.class))
                .cast(RxObjectEventConn.class)
                .mergeWith(registeredMessage)
//                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventConnected.class))
                .subscribe(connectedAndRegistered);
//        registeredMessage.subscribe(connectedAndRegistered);

        // Register on connected
        final Observable<RxObjectEventConnected> connectedMessage = events
                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventConnected.class))
                .lift(ReactiveLoggingObservables.<RxObjectEventConnected>loggingLift(LOGGER,
                        "ConnectedEvent"));

        connectedMessage
                .flatMap(new Function<RxObjectEventConnected, Observable<?>>() {
                    @Override
                    public Observable<?> apply(RxObjectEventConnected rxEventConn) {
                        return ReactiveXMoreObservables.sendObjectMessage(rxEventConn.sender(),
                                new RegisterMessage("asdf")
                        ).toObservable();
                    }
                })
                .lift(ReactiveLoggingObservables.loggingOnlyErrorLift(LOGGER, "SendRegisterEvent"))
                .onErrorReturn(ReactiveMoreObservables.throwableToIgnoreError())
                .subscribe();

        // Log events
        LOGGER.setLevel(Level.ALL);
        ReactiveXMoreObservables.logger.setLevel(Level.ALL);
        events.subscribe(ReactiveLoggingObservables.logging(LOGGER, "Events"));
        connectedAndRegistered
                .subscribe(ReactiveLoggingObservables.logging(LOGGER, "ConnectedAndRegistered"));
    }

    public Observable<RxObjectEvent> events() {
        return events;
    }

    public Observable<RxObjectEventConn> connectedAndRegistered() {
        return connectedAndRegistered;
    }

    public Flowable<Object> connection() {
        return connection;
    }

    public void sendPingWhenConnected() {
        Observable.combineLatest(
                Observable.interval(5, TimeUnit.SECONDS, scheduler),
                connectedAndRegistered,
                new BiFunction<Long, RxObjectEventConn, RxObjectEventConn>() {
                    @Override
                    public RxObjectEventConn apply(Long aLong, RxObjectEventConn rxEventConn) {
                        return rxEventConn;
                    }
                })
                .compose(isConnected())
                .flatMap(new Function<RxObjectEventConn, Observable<?>>() {
                    @Override
                    public Observable<?> apply(RxObjectEventConn rxEventConn)
                            throws Exception {
                        return ReactiveXMoreObservables.sendObjectMessage(rxEventConn.sender(),
                                new PingMessage
                                        ("send_only_when_connected"))
                                .toObservable();
                    }
                })
                .subscribe();
    }

    public void sendPingEvery5seconds() {
        Observable.interval(5, TimeUnit.SECONDS, scheduler)
                .flatMap(new Function<Long, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Long aLong) throws Exception {
                        return connectedAndRegistered
                                .compose(isConnected())
                                .firstElement()
                                .flatMap(new Function<RxObjectEventConn, MaybeSource<Boolean>>() {
                                    @Override
                                    public MaybeSource<Boolean> apply(RxObjectEventConn rxEventConn)
                                            throws Exception {
                                        return ReactiveXMoreObservables.sendObjectMessage(
                                                rxEventConn.sender(),
                                                new PingMessage("be_sure_to_send"))
                                                .toMaybe();
                                    }
                                }).toObservable();
                    }
                })
                .subscribe();
    }

    private final Object lock = new Object();
    private int counter = 0;
    @Nonnull
    public Observable<String> nextId() {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                final int current;
                synchronized (lock) {
                    current = counter;
                    counter += 1;
                }
                emitter.onNext(String.valueOf(current));
                emitter.onComplete();
            }
        });
    }

    @Nonnull
    public Observable<DataMessage> sendMessageOnceWhenConnected(final Function<String,
            Observable<Object>> createMessage) {
        return connectedAndRegistered
                .compose(isConnected())
                .firstElement()
                .flatMap(new Function<RxObjectEventConn, Maybe<DataMessage>>() {
                    @Override
                    public Maybe<DataMessage> apply(RxObjectEventConn rxEventConn)
                            throws Exception {
                        return requestData(rxEventConn, createMessage).firstElement();
                    }
                }).toObservable();
    }

    @Nonnull
    private Observable<DataMessage> requestData(final RxObjectEventConn rxEventConn,
            final Function<String, Observable<Object>> createMessage) {
        return nextId()
                .flatMap(new Function<String, Observable<DataMessage>>() {
                    @Override
                    public Observable<DataMessage> apply(final String messageId) throws Exception {
                        final Observable<Object> sendMessageObservable =
                                createMessage.apply(messageId)
                                .flatMap(new Function<Object, Observable<?>>() {
                                    @Override
                                    public Observable<?> apply(Object s) throws Exception {
                                        return ReactiveXMoreObservables
                                                .sendObjectMessage(rxEventConn.sender(), s)
                                                .toObservable();
                                    }
                                });

                        final Observable<DataMessage> waitForResponseObservable = events
                                .compose(ReactiveMoreObservables.filterAndMap(RxObjectEventMessage
                                        .class))
                                .compose(ReactiveMoreObservables.filterAndMap(DataMessage.class))
                                .filter(new Predicate<DataMessage>() {
                                    @Override
                                    public boolean test(DataMessage dataMessage) throws Exception {
                                        return dataMessage.id().equals(messageId);
                                    }
                                })
                                .firstElement()
                                .toObservable()
                                .timeout(5, TimeUnit.SECONDS, scheduler);
                        return Observable.combineLatest(waitForResponseObservable,
                                sendMessageObservable,
                                new BiFunction<DataMessage, Object, DataMessage>() {
                                    @Override
                                    public DataMessage apply(DataMessage dataMessage, Object o)
                                            throws Exception {
                                        return dataMessage;
                                    }
                                });
                    }
                });
    }

    @Nonnull
    static ObservableTransformer<RxObjectEventConn, RxObjectEventConn> isConnected() {
        return new ObservableTransformer<RxObjectEventConn, RxObjectEventConn>() {
            @Override
            public Observable<RxObjectEventConn> apply(Observable<RxObjectEventConn>
                    rxEventConnObservable) {
                return rxEventConnObservable.filter(new Predicate<RxObjectEventConn>() {
                    @Override
                    public boolean test(RxObjectEventConn rxEventConn) throws Exception {
                        return rxEventConn != null;
                    }
                });
            }
        };
    }

    static class FilterRegisterMessage implements Predicate<RxObjectEventMessage> {
        @Override
        public boolean test(RxObjectEventMessage rxEvent) throws Exception {
            return rxEvent.message() instanceof RegisteredMessage;
        }
    }
}
