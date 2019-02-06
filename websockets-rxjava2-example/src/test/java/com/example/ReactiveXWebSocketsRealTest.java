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
import com.appunite.websocket.rxevent.messages.RxEvent;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ReactiveXWebSocketsRealTest {

    private ReactiveXWebSockets socket;

    @Before
    public void setUp() throws Exception {
        socket = new ReactiveXWebSockets(new OkHttpClient(),
                new Request.Builder()
                        .get()
                        .url("ws://10.10.0.2:8080/ws")
                        .addHeader("Sec-WebSocket-Protocol", "chat")
                        .build());

    }

    @Test
    @Ignore
    public void testName() throws Exception {
        final Disposable disposable = socket.webSocketObservable()
                .subscribeOn(Schedulers.io())
                .doOnNext(new Consumer<RxEvent>() {
                    @Override
                    public void accept(RxEvent rxEvent) {
                        System.out.println("Event: " + rxEvent);
                    }
                })
                .subscribe();
        Thread.sleep(5000);
        disposable.dispose();
        Thread.sleep(5000);
    }

}