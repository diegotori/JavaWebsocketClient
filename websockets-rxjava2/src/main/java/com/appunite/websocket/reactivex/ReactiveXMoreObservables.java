package com.appunite.websocket.reactivex;

import com.appunite.websocket.rxevent.object.ObjectSerializer;
import com.appunite.websocket.rxevent.object.ObjectWebSocketSender;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventMessage;
import com.appunite.websocket.reactivex.object.ReactiveXObjectWebSockets;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import okhttp3.WebSocket;

/**
 * Created by son_g on 9/17/2017.
 */

public final class ReactiveXMoreObservables {
    public static final Logger logger = Logger.getLogger("ReactiveXWebSockets");

    private ReactiveXMoreObservables() {
        throw new UnsupportedOperationException();
    }

    /**
     * Enqueue message to send
     *
     * @param sender connection event that is used to send message
     * @param message message to send
     * @return Single that returns true if message was enqueued
     * @see #sendObjectMessage(ObjectWebSocketSender, Object)
     */
    @Nonnull
    public static Single<Boolean> sendMessage(final @Nonnull WebSocket sender, final @Nonnull String message) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                logger.log(Level.FINE, "sendStringMessage: {0}", message);
                return sender.send(message);
            }
        });
    }

    /**
     * Send object
     * <p>
     * Object is parsed via {@link ObjectSerializer} given by
     * {@link ReactiveXObjectWebSockets#ReactiveXObjectWebSockets(ReactiveXWebSockets, ObjectSerializer)}
     *
     * @param sender connection event that is used to send message
     * @param message message to serialize and sent
     * @return Single that returns true if message was enqueued or ObjectParseException if couldn't
     * serialize
     * @see #sendMessage(WebSocket, String)
     */
    @Nonnull
    public static Single<Boolean> sendObjectMessage(final @Nonnull ObjectWebSocketSender sender, final @Nonnull Object message) {
        return Single.fromCallable(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                logger.log(Level.FINE, "sendStringMessage: {0}", message);
                return sender.sendObjectMessage(message);
            }
        });
    }

    /**
     * Transform one observable to observable of given type filtering by a type
     *
     * @param clazz type of message that you would like get
     * @param <T> type of message that you would like get
     * @return Observable that returns given type of message
     */
    @Nonnull
    public static <T> ObservableTransformer<RxObjectEventMessage, T> filterAndMap(@Nonnull
    final Class<T> clazz) {
        return new ObservableTransformer<RxObjectEventMessage, T>() {
            @Override
            public ObservableSource<T> apply(
                    @NonNull Observable<RxObjectEventMessage> upstream) {
                return upstream
                        .filter(new Predicate<RxObjectEventMessage>() {
                            @Override
                            public boolean test(
                                    @NonNull RxObjectEventMessage o)
                                    throws Exception {
                                return o != null && clazz.isInstance(o.message());
                            }
                        })
                        .map(new Function<RxObjectEventMessage, T>() {
                            @Override
                            public T apply(
                                    @NonNull RxObjectEventMessage o)
                                    throws Exception {
                                return o.message();
                            }
                        });
            }
        };
    }
}
