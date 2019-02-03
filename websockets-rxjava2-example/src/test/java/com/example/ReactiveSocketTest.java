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

import com.appunite.websocket.rxevent.object.ObjectParseException;
import com.appunite.websocket.rxevent.object.ObjectWebSocketSender;
import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventConnected;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventMessage;
import com.example.model.DataMessage;
import com.example.model.PingMessage;
import com.example.model.RegisterMessage;
import com.example.model.RegisteredMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReactiveSocketTest {

    @Mock
    ReactiveSocketConnection socketConnection;
    @Mock
//    Observer<Object> observer;
    Consumer<Object> observer;
//    private final TestObserver<Object> observer = new TestObserver<>();
    @Mock
    ObjectWebSocketSender sender;
    @Mock
    Observer<DataMessage> dataObserver;

    private ReactiveSocket socket;

//    private final TestScheduler testScheduler = Schedulers.test();
    private final TestScheduler testScheduler = new TestScheduler();
//    private final TestSubject<RxObjectEvent> connection = TestSubject.create(testScheduler);
//    private final Subject<RxObjectEvent> connection = Subject.create(testScheduler);
//    private final Flowable<RxObjectEvent> connection = Flowable.create(testScheduler);
//    private final Observable<RxObjectEvent> connection = Observable.create(testScheduler);
    private Emitter<RxObjectEvent> connection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        FlowableOnSubscribe<RxObjectEvent> source = new FlowableOnSubscribe<RxObjectEvent>() {
            @Override
            public void subscribe(FlowableEmitter e) throws Exception {
                connection = e;
            }
        };
        Flowable<RxObjectEvent> connection = Flowable.create(source, BackpressureStrategy.LATEST);
        when(socketConnection.connection()).thenReturn(connection);
        socket = new ReactiveSocket(socketConnection, testScheduler);
    }

    @Test
    public void testConnection_registerIsSent() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        try {
            connection.onNext(new RxObjectEventConnected(sender));
            testScheduler.triggerActions();
            verify(sender).sendObjectMessage(new RegisterMessage("asdf"));
        } finally {
            disposable.dispose();
        }
    }

    private void register() throws IOException, InterruptedException, ObjectParseException {
        connection.onNext(new RxObjectEventConnected(sender));
        testScheduler.triggerActions();
        verify(sender).sendObjectMessage(new RegisterMessage("asdf"));
        connection.onNext(new RxObjectEventMessage(sender, new RegisteredMessage()));
        testScheduler.triggerActions();
    }

    @Test
    public void testWhenNoResponse_throwError() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        socket.sendMessageOnceWhenConnected(
                new Function<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();

            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onError(any(Throwable.class));
        } finally {
            disposable.dispose();
        }
    }

    @Test
    public void testWhenResponseOnDifferentMessage_throwError() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        socket.sendMessageOnceWhenConnected(
                new Function<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();

            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));

            connection.onNext(new RxObjectEventMessage(sender, new DataMessage("100", "asdf")));
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onError(any(Throwable.class));
        } finally {
            disposable.dispose();
        }
    }

    @Test
    public void testWhenResponse_messageSuccess() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        socket.sendMessageOnceWhenConnected(
                new Function<String, Observable<Object>>() {
                    @Override
                    public Observable<Object> apply(String id) {
                        return Observable.<Object>just(new DataMessage(id, "krowa"));
                    }
                })
                .subscribe(dataObserver);
        try {
            register();
            verify(sender).sendObjectMessage(new DataMessage("0", "krowa"));

            connection.onNext(new RxObjectEventMessage(sender, new DataMessage("0", "asdf")));
            testScheduler.advanceTimeBy(10, TimeUnit.SECONDS);

            verify(dataObserver).onNext(new DataMessage("0", "asdf"));
            verify(dataObserver).onComplete();
        } finally {
            disposable.dispose();
        }
    }

    @Test
    public void testAfterConnection_registerSuccess() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        try {
            register();
        } finally {
            disposable.dispose();
        }
    }

    @Test
    public void testConnectionSuccessAfterAWhile_registerSuccess() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();
        } finally {
            disposable.dispose();
        }
    }

    @Test
    public void testWhenTimePassBeforeConnection_sendAllPings() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        socket.sendPingEvery5seconds();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();

            verify(sender, times(6)).sendObjectMessage(new PingMessage("be_sure_to_send"));
        } finally {
            disposable.dispose();
        }
    }

    @Test
    public void testWhenTimePassBeforeConnection_sendPingOnlyOnce() throws Exception {
        final Disposable disposable = socket.connection().subscribe(observer);
        socket.sendPingWhenConnected();
        try {
            testScheduler.advanceTimeBy(30, TimeUnit.SECONDS);
            register();

            verify(sender).sendObjectMessage(new PingMessage("send_only_when_connected"));
        } finally {
            disposable.dispose();
        }
    }
}