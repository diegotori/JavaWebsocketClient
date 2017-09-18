package com.appunite.websocket.reactivex;

import com.appunite.websocket.rxevent.ServerHttpError;
import com.appunite.websocket.rxevent.ServerRequestedCloseException;
import com.appunite.websocket.rxevent.messages.RxEvent;
import com.appunite.websocket.rxevent.messages.RxEventBinaryMessage;
import com.appunite.websocket.rxevent.messages.RxEventConnected;
import com.appunite.websocket.rxevent.messages.RxEventDisconnected;
import com.appunite.websocket.rxevent.messages.RxEventStringMessage;
import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import javax.annotation.Nonnull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Created by Diego on 6/27/2017.
 */

public class ReactiveXWebSockets {
    @Nonnull
    private final OkHttpClient client;
    @Nonnull
    private final Request request;

    /**
     * Create instance of {@link ReactiveXWebSockets}
     *
     * @param client {@link OkHttpClient} instance
     * @param request request to connect to websocket
     */
    public ReactiveXWebSockets(@Nonnull OkHttpClient client, @Nonnull Request request) {
        this.client = client;
        this.request = request;
    }

    /**
     * Returns Flowable that connected to a websocket and returns {@link RxObjectEvent}'s
     *
     * @return Flowable that connects to websocket
     */
    @Nonnull
    public Flowable<RxEvent> webSocketObservable() {
        return Flowable.create(new FlowableOnSubscribe<RxEvent>() {
            @Override
            public void subscribe(@NonNull final FlowableEmitter<RxEvent> emitter)
                    throws Exception {
                final WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
                    @Override
                    public void onOpen(WebSocket webSocket, Response response) {
                        emitter.onNext(new RxEventConnected(webSocket));
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, String text) {
                        emitter.onNext(new RxEventStringMessage(webSocket, text));
                    }

                    @Override
                    public void onMessage(WebSocket webSocket, ByteString bytes) {
                        emitter.onNext(new RxEventBinaryMessage(webSocket, bytes.toByteArray()));
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        super.onClosing(webSocket, code, reason);
                        final ServerRequestedCloseException exception =
                                new ServerRequestedCloseException(code, reason);
                        emitter.onNext(new RxEventDisconnected(exception));
                        emitter.onError(exception);
                    }

                    @Override
                    public void onClosed(WebSocket webSocket, int code, String reason) {
                        final ServerRequestedCloseException exception =
                                new ServerRequestedCloseException(code, reason);
                        emitter.onNext(new RxEventDisconnected(exception));
                        emitter.onError(exception);
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                        if (response != null) {
                            final ServerHttpError exception = new ServerHttpError(response);
                            emitter.onNext(new RxEventDisconnected(exception));
                            emitter.onError(exception);
                        } else {
                            emitter.onNext(new RxEventDisconnected(t));
                            emitter.onError(t);
                        }
                    }
                });
                emitter.setDisposable(new Disposable() {
                    boolean disposed = false;

                    @Override
                    public void dispose() {
                        final int code = 1000;
                        final String reason = "Just disconnect";
                        disposed = webSocket.close(code, reason);
                        final ServerRequestedCloseException exception =
                                new ServerRequestedCloseException(code, reason);
                        emitter.onNext(new RxEventDisconnected(exception));
                        emitter.onComplete();
                    }

                    @Override
                    public boolean isDisposed() {
                        return disposed;
                    }
                });
            }
        }, BackpressureStrategy.BUFFER);
    }
}
