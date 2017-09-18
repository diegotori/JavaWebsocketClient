/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.appunite.websocket.reactivex.object;

import com.appunite.websocket.reactivex.ReactiveXWebSockets;
import com.appunite.websocket.rxevent.messages.RxEvent;
import com.appunite.websocket.rxevent.messages.RxEventBinaryMessage;
import com.appunite.websocket.rxevent.messages.RxEventConnected;
import com.appunite.websocket.rxevent.messages.RxEventDisconnected;
import com.appunite.websocket.rxevent.messages.RxEventStringMessage;
import com.appunite.websocket.rxevent.object.ObjectParseException;
import com.appunite.websocket.rxevent.object.ObjectWebSocketSender;
import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventDisconnected;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventMessage;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventWrongBinaryMessageFormat;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventWrongStringMessageFormat;
import com.appunite.websocket.rxevent.object.ObjectSerializer;
import io.reactivex.Flowable;
import io.reactivex.FlowableOperator;
import io.reactivex.annotations.NonNull;
import io.reactivex.subscribers.DisposableSubscriber;
import javax.annotation.Nonnull;
import okhttp3.WebSocket;
import okio.ByteString;
import org.reactivestreams.Subscriber;

/**
 * This class allows to retrieve json messages from websocket
 */
public class ReactiveXObjectWebSockets {
    @Nonnull
    private final ReactiveXWebSockets rxWebSockets;
    @Nonnull
    private final ObjectSerializer objectSerializer;

    /**
     * Creates {@link ReactiveXObjectWebSockets}
     * @param rxWebSockets socket that is used to connect to server
     * @param objectSerializer that is used to parse messages
     */
    public ReactiveXObjectWebSockets(@Nonnull ReactiveXWebSockets rxWebSockets, @Nonnull ObjectSerializer
            objectSerializer) {
        this.rxWebSockets = rxWebSockets;
        this.objectSerializer = objectSerializer;
    }

    /**
     * Returns observable that connected to a websocket and returns {@link RxObjectEvent}s
     *
     * @return Observable that connects to websocket
     * @see ReactiveXWebSockets#webSocketObservable()
     */
    @Nonnull
    public Flowable<RxObjectEvent> webSocketObservable() {
        return rxWebSockets.webSocketObservable()
                .lift(new FlowableOperator<RxObjectEvent, RxEvent>() {
                    @Override
                    public Subscriber<? super RxEvent> apply(
                            @NonNull final Subscriber<? super RxObjectEvent> observer) throws
                            Exception {
                        return new DisposableSubscriber<RxEvent>() {
                            @Override
                            public void onNext(RxEvent rxEvent) {
                                if (rxEvent instanceof RxEventConnected) {
                                    observer.onNext(new RxObjectEventConnected(jsonSocketSender((
                                            (RxEventConnected) rxEvent).sender())));
                                } else if (rxEvent instanceof RxEventDisconnected) {
                                    observer.onNext(new RxObjectEventDisconnected(((RxEventDisconnected)
                                            rxEvent).exception()));
                                } else if (rxEvent instanceof RxEventStringMessage) {
                                    final RxEventStringMessage stringMessage = (RxEventStringMessage) rxEvent;
                                    observer.onNext(parseMessage(stringMessage));
                                } else if (rxEvent instanceof RxEventBinaryMessage) {
                                    final RxEventBinaryMessage binaryMessage = (RxEventBinaryMessage) rxEvent;
                                    observer.onNext(parseMessage(binaryMessage));
                                } else {
                                    throw new RuntimeException("Unknown message type");
                                }
                            }

                            @Override
                            public void onError(Throwable t) {
                                observer.onError(t);
                            }

                            @Override
                            public void onComplete() {
                                observer.onComplete();
                            }

                            private RxObjectEvent parseMessage(RxEventStringMessage stringMessage) {
                                final String message = stringMessage.message();
                                final Object object;
                                try {
                                    object = objectSerializer.serialize(message);
                                } catch (ObjectParseException e) {
                                    return new RxObjectEventWrongStringMessageFormat(jsonSocketSender(stringMessage.sender()), message, e);
                                }
                                return new RxObjectEventMessage(jsonSocketSender(stringMessage
                                        .sender()), object);
                            }

                            private RxObjectEvent parseMessage(RxEventBinaryMessage binaryMessage) {
                                final byte[] message = binaryMessage.message();
                                final Object object;
                                try {
                                    object = objectSerializer.serialize(message);
                                } catch (ObjectParseException e) {
                                    return new RxObjectEventWrongBinaryMessageFormat(jsonSocketSender(binaryMessage.sender()), message, e);
                                }
                                return new RxObjectEventMessage(jsonSocketSender(binaryMessage.sender()), object);
                            }
                        };
                    }
                });
    }

    @Nonnull
    private ObjectWebSocketSender jsonSocketSender(@Nonnull final WebSocket sender) {
        return new ObjectWebSocketSender() {
            @Override
            public boolean sendObjectMessage(@Nonnull Object message) throws ObjectParseException {
                if (objectSerializer.isBinary(message)) {
                    return sender.send(ByteString.of(objectSerializer.deserializeBinary(message)));
                } else {
                    return sender.send(objectSerializer.deserializeString(message));
                }
            }
        };
    }
}
