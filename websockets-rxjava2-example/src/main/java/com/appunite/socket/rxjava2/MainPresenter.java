/*
 * Copyright (C) 2015 Jacek Marchwicki <jacek.marchwicki@gmail.com>
 * Copyright (C) 2019 Gavriel Fleischer <flocsy@gmail.com>
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

package com.appunite.socket.rxjava2;

import android.util.Pair;

import com.appunite.detector.SimpleDetector;
import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventConn;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventDisconnected;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventMessage;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventWrongMessageFormat;
import com.appunite.websocket.rxevent.object.messages.RxObjectEventWrongStringMessageFormat;
import com.example.ReactiveSocket;
import com.example.model.DataMessage;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class MainPresenter {

    private final BehaviorSubject<ImmutableList<AdapterItem>> items;
    private final Observable<Boolean> connected;
    private final BehaviorSubject<Boolean> requestConnection = BehaviorSubject.create();
    private final PublishSubject<Object> connectClick = PublishSubject.create();
    private final PublishSubject<Object> disconnectClick = PublishSubject.create();
    private final PublishSubject<Object> sendClick = PublishSubject.create();
    private final BehaviorSubject<Boolean> lastItemInView = BehaviorSubject.create();
    private final PublishSubject<AdapterItem> addItem = PublishSubject.create();

    private Disposable disposable;

    public MainPresenter(@Nonnull final ReactiveSocket socket,
                         @Nonnull final Scheduler networkScheduler,
                         @Nonnull final Scheduler uiScheduler) {
        items = BehaviorSubject.create();

        Observable.merge(connectClick.map(funcTrue()), disconnectClick.map(funcFalse()))
                .startWith(false)
                .subscribe(requestConnection);

        sendClick
                .flatMap(flatMapClicksToSendMessageAndResult(socket))
                .map(mapDataMessageOrErrorToPair())
                .map(mapPairToNewAdapterItem())
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)
                .subscribe(addItem);

        disposable = requestConnection
                .subscribe(new Consumer<Boolean>() {

                    private Disposable subscribe;

                    @Override
                    public void accept(Boolean requestConnection) {
                        if (requestConnection) {
                            if (subscribe == null) {
                                subscribe = socket
                                        .connection()
                                        .subscribeOn(networkScheduler)
                                        .observeOn(uiScheduler)
                                        .subscribe();
                            }
                        } else {
                            if (subscribe != null && !subscribe.isDisposed()) {
                                subscribe.dispose();
                                subscribe = null;
                            }
                        }
                    }
                });

        requestConnection
                .map(mapConnectingStatusToString())
                .map(mapStringToNewAdapterItem())
                .subscribe(addItem);

        addItem
                .scan(ImmutableList.<AdapterItem>of(), new BiFunction<ImmutableList<AdapterItem>, AdapterItem, ImmutableList<AdapterItem>>() {
                    @Override
                    public ImmutableList<AdapterItem> apply(ImmutableList<AdapterItem> adapterItems, AdapterItem adapterItem) {
                        return ImmutableList.<AdapterItem>builder().addAll(adapterItems).add(adapterItem).build();
                    }
                })
                .subscribe(items);

        socket.events()
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler)
                .lift(liftRxJsonEventToPairMessage())
                .map(mapPairToNewAdapterItem())
                .subscribe(addItem);

        connected = socket.connectedAndRegistered()
                .map(new Function<RxObjectEventConn, Boolean>() {
                    @Override
                    public Boolean apply(RxObjectEventConn rxJsonEventConn) {
                        return rxJsonEventConn != null;
                    }
                })
                .distinctUntilChanged()
                .subscribeOn(networkScheduler)
                .observeOn(uiScheduler);

        connected
                .map(mapConnectedStatusToString())
                .map(mapStringToNewAdapterItem())
                .subscribe(addItem);
    }

    public void dispose() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    @Nonnull
    public Observable<ItemsWithScroll> itemsWithScrollObservable() {
        return Observable.combineLatest(items, lastItemInView, new BiFunction<ImmutableList<AdapterItem>, Boolean, ItemsWithScroll>() {
            @Override
            public ItemsWithScroll apply(ImmutableList<AdapterItem> adapterItems, Boolean isLastItemInList) {
                final int lastItemPosition = adapterItems.size() - 1;
                final boolean shouldScroll = isLastItemInList && lastItemPosition >= 0;
                return new ItemsWithScroll(adapterItems, shouldScroll, lastItemPosition);
            }
        });
    }

    public Observer<Boolean> lastItemInViewObserver() {
        return lastItemInView;
    }

    private Function<Boolean, String> mapConnectedStatusToString() {
        return new Function<Boolean, String>() {
            @Override
            public String apply(Boolean connected) {
                return connected ? "connected" : "disconnected";
            }
        };
    }

    private ObservableOperator<Pair<String, String>, RxObjectEvent> liftRxJsonEventToPairMessage() {
        return new ObservableOperator<Pair<String, String>, RxObjectEvent>() {
            @Override
            public Observer<? super RxObjectEvent> apply(final Observer<? super Pair<String, String>> observer) throws Exception {
                return new Observer<RxObjectEvent>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        observer.onSubscribe(d);
                    }

                    @Override
                    public void onComplete() {
                        observer.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(RxObjectEvent rxObjectEvent) {
                        if (rxObjectEvent instanceof RxObjectEventMessage) {
                            observer.onNext(new Pair<>("message", ((RxObjectEventMessage) rxObjectEvent).message().toString()));
                        } else if (rxObjectEvent instanceof RxObjectEventWrongMessageFormat) {
                            final RxObjectEventWrongStringMessageFormat wrongMessageFormat = (RxObjectEventWrongStringMessageFormat) rxObjectEvent;
                            //noinspection ThrowableResultOfMethodCallIgnored
                            observer.onNext(new Pair<>("could not parse message", wrongMessageFormat.message()
                                    + ", " + wrongMessageFormat.exception().toString()));
                        } else if (rxObjectEvent instanceof RxObjectEventDisconnected) {
                            //noinspection ThrowableResultOfMethodCallIgnored
                            final Throwable exception = ((RxObjectEventDisconnected) rxObjectEvent).exception();
                            if (!(exception instanceof InterruptedException)) {
                                observer.onNext(new Pair<>("error", exception.toString()));
                            }
                        }
                    }
                };
            }
        };
    }

    private Function<Boolean, String> mapConnectingStatusToString() {
        return new Function<Boolean, String>() {
            @Override
            public String apply(Boolean aBoolean) {
                return aBoolean ? "connecting" : "disconnecting";
            }
        };
    }

    private Function<Object, Observable<DataMessageOrError>> flatMapClicksToSendMessageAndResult(@Nonnull final ReactiveSocket socket) {
        return new Function<Object, Observable<DataMessageOrError>>() {
            @Override
            public Observable<DataMessageOrError> apply(Object o) {
                addItem.onNext(newItem("sending...", null));
                return socket
                        .sendMessageOnceWhenConnected(new Function<String, Observable<Object>>() {
                            @Override
                            public Observable<Object> apply(String id) {
                                return Observable.<Object>just(new DataMessage(id, "krowa"));
                            }
                        })
                        .map(new Function<DataMessage, DataMessageOrError>() {
                            @Override
                            public DataMessageOrError apply(DataMessage dataMessage) {
                                return new DataMessageOrError(dataMessage, null);
                            }
                        })
                        .onErrorResumeNext(new Function<Throwable, Observable<DataMessageOrError>>() {
                            @Override
                            public Observable<DataMessageOrError> apply(Throwable throwable) {
                                return Observable.just(new DataMessageOrError(null, throwable));
                            }
                        });
            }
        };
    }

    private Function<DataMessageOrError, Pair<String, String>> mapDataMessageOrErrorToPair() {
        return new Function<DataMessageOrError, Pair<String, String>>() {
            @Override
            public Pair<String, String> apply(DataMessageOrError dataMessageOrError) {
                if (dataMessageOrError.error != null) {
                    return new Pair<>("sending error", dataMessageOrError.error.toString());
                } else {
                    return new Pair<>("sending response", dataMessageOrError.message.toString());
                }
            }
        };
    }


    @Nonnull
    private Function<? super Object, Boolean> funcTrue() {
        return new Function<Object, Boolean>() {
            @Override
            public Boolean apply(Object o) {
                return true;
            }
        };
    }

    @Nonnull
    private Function<? super Object, Boolean> funcFalse() {
        return new Function<Object, Boolean>() {
            @Override
            public Boolean apply(Object o) {
                return false;
            }
        };
    }

    @Nonnull
    private Function<String, AdapterItem> mapStringToNewAdapterItem() {
        return new Function<String, AdapterItem>() {
            @Override
            public AdapterItem apply(String s) {
                return newItem(s, null);
            }
        };
    }

    @Nonnull
    private Function<Pair<String, String>, AdapterItem> mapPairToNewAdapterItem() {
        return new Function<Pair<String, String>, AdapterItem>() {
            @Override
            public AdapterItem apply(Pair<String, String> s) {
                return newItem(s.first, s.second);
            }
        };
    }

    private final Object idLock = new Object();
    private long id = 0;

    @Nonnull
    private String newId() {
        synchronized (idLock) {
            final long id = this.id;
            this.id += 1;
            return String.valueOf(id);
        }
    }

    @Nonnull
    private AdapterItem newItem(@Nonnull String message, @Nullable String details) {

        return new AdapterItem(newId(), System.currentTimeMillis(), message, details);
    }

    @Nonnull
    public Observer<Object> connectClickObserver() {
        return connectClick;
    }

    @Nonnull
    public Observer<Object> disconnectClickObserver() {
        return disconnectClick;
    }

    @Nonnull
    public Observer<Object> sendClickObserver() {
        return sendClick;
    }

    @Nonnull
    public Observable<Boolean> connectButtonEnabledObservable() {
        return requestConnection.map(not());
    }

    @Nonnull
    public Observable<Boolean> disconnectButtonEnabledObservable() {
        return requestConnection;
    }

    @Nonnull
    public Observable<Boolean> sendButtonEnabledObservable() {
        return connected;
    }

    @Nonnull
    private Function<Boolean, Boolean> not() {
        return new Function<Boolean, Boolean>() {
            @Override
            public Boolean apply(Boolean aBoolean) {
                return !aBoolean;
            }
        };
    }

    public class AdapterItem implements SimpleDetector.Detectable<AdapterItem> {

        @Nonnull
        private final String id;
        private final long publishTime;
        @Nullable
        private final String text;
        @Nullable
        private final String details;

        public AdapterItem(@Nonnull String id,
                           long publishTime,
                           @Nullable String text,
                           @Nullable String details) {
            this.id = id;
            this.publishTime = publishTime;
            this.text = text;
            this.details = details;
        }

        @Nonnull
        public String id() {
            return id;
        }

        @Nullable
        public String text() {
            return text;
        }

        @Nullable
        public String details() {
            return details;
        }

        public long publishTime() {
            return publishTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AdapterItem)) return false;

            final AdapterItem that = (AdapterItem) o;

            return id.equals(that.id)
                    && !(text != null ? !text.equals(that.text) : that.text != null)
                    && publishTime == that.publishTime
                    && !(details != null ? !details.equals(that.details) : that.details != null);
        }

        @Override
        public int hashCode() {
            int result = id.hashCode();
            result = 31 * result + (text != null ? text.hashCode() : 0);
            result = 31 * result + (details != null ? details.hashCode() : 0);
            result = 31 * result + (int)publishTime;
            return result;
        }

        @Override
        public boolean matches(@Nonnull AdapterItem item) {
            return id.equals(item.id);
        }

        @Override
        public boolean same(@Nonnull AdapterItem item) {
            return equals(item);
        }

        @Nonnull
        public Consumer<Object> clickObserver() {
            return new Consumer<Object>() {
                @Override
                public void accept(Object o) throws Exception {

                }
            };
        }
    }

    public static class ItemsWithScroll {
        private final ImmutableList<AdapterItem> items;
        private final boolean shouldScroll;
        private final int scrollToPosition;

        public ItemsWithScroll(ImmutableList<AdapterItem> items, boolean shouldScroll, int scrollToPosition) {
            this.items = items;
            this.shouldScroll = shouldScroll;
            this.scrollToPosition = scrollToPosition;
        }

        public ImmutableList<AdapterItem> items() {
            return items;
        }

        public boolean shouldScroll() {
            return shouldScroll;
        }

        public int scrollToPosition() {
            return scrollToPosition;
        }
    }

    static class DataMessageOrError {
        private final DataMessage message;
        private final Throwable error;

        public DataMessageOrError(DataMessage message, Throwable error) {
            this.message = message;
            this.error = error;
        }
    }
}
