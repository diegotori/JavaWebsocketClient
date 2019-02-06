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

package com.example;

import com.appunite.websocket.reactivex.ReactiveXWebSockets;
import com.appunite.websocket.reactivex.object.ReactiveXObjectWebSockets;
import com.appunite.websocket.rx.object.GsonObjectSerializer;
import com.example.model.DataMessage;
import com.example.model.Message;
import com.example.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.logging.Logger;

public class ReactiveSocketRealTest {

    private ReactiveSocket socket;

    @Before
    public void setUp() throws Exception {
        final Gson gson = new GsonBuilder()
                .registerTypeAdapter(Message.class, new Message.Deserializer())
                .registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
                .create();

        final ReactiveXWebSockets webSockets = new ReactiveXWebSockets(new OkHttpClient(),
                new Request.Builder()
                        .get()
                        .url("ws://10.10.0.2:8080/ws")
                        .addHeader("Sec-WebSocket-Protocol", "chat")
                        .build());
        final ReactiveXObjectWebSockets jsonWebSockets = new ReactiveXObjectWebSockets(webSockets, new GsonObjectSerializer(gson, Message.class));
        final ReactiveSocketConnection socketConnection = new ReactiveSocketConnectionImpl(jsonWebSockets, Schedulers.computation());
        socket = new ReactiveSocket(socketConnection, Schedulers.computation());

    }

    @Test
    @Ignore
    public void testName() throws Exception {
        socket.sendMessageOnceWhenConnected(new Function<String, Observable<Object>>() {
            @Override
            public Observable<Object> apply(String messageId) {
                return Observable.<Object>just(new DataMessage(messageId, "some message"));
            }
        })
                .subscribe(ReactiveLoggingObservables.logging(Logger.getLogger("Rx"), "SendMessage"));

        socket.sendPingWhenConnected();
        socket.sendPingEvery5seconds();
        final Disposable disposable = socket.connection()
                .subscribeOn(Schedulers.io())
                .subscribe();
        Thread.sleep(10000);
        disposable.dispose();
        Thread.sleep(10000);
    }

}