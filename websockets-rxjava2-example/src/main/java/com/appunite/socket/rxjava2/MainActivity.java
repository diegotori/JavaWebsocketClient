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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.appunite.socket.R;
import com.appunite.websocket.reactivex.ReactiveXWebSockets;
import com.appunite.websocket.reactivex.object.ReactiveXObjectWebSockets;
import com.appunite.websocket.rx.object.GsonObjectSerializer;
import com.example.ReactiveSocket;
import com.example.ReactiveSocketConnection;
import com.example.ReactiveSocketConnectionImpl;
import com.example.model.Message;
import com.example.model.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jakewharton.rxbinding2.view.RxView;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends FragmentActivity {

	private static final String RETENTION_FRAGMENT_TAG = "retention_fragment_tag";

	private CompositeDisposable disposables;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final MainPresenter presenter = getRetentionFragment(savedInstanceState).presenter();

		setContentView(R.layout.main_activity);
		final RecyclerView recyclerView = findViewById(R.id.main_activity_recycler_view);
		final LinearLayoutManager layout = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
		recyclerView.setLayoutManager(layout);
		final MainAdapter adapter = new MainAdapter();
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(new DefaultItemAnimator());

		final BehaviorSubject<Boolean> isLastSubject = BehaviorSubject.create();
		recyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
			boolean manualScrolling = false;

			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
				if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
					manualScrolling = true;
				}
				if (manualScrolling && newState == RecyclerView.SCROLL_STATE_IDLE) {
					manualScrolling = false;

					final int lastVisibleItemPosition = layout.findLastVisibleItemPosition();
					final int previousItemsCount = adapter.getItemCount();
					final boolean isLast = previousItemsCount - 1 == lastVisibleItemPosition;
					isLastSubject.onNext(isLast);
				}
			}
		});

		isLastSubject.subscribe(presenter.lastItemInViewObserver());
		disposables = new CompositeDisposable(
				presenter.itemsWithScrollObservable()
						.subscribe(new Consumer<MainPresenter.ItemsWithScroll>() {
							@Override
							public void accept(final MainPresenter.ItemsWithScroll itemsWithScroll) {
								adapter.accept(itemsWithScroll.items());
								if (itemsWithScroll.shouldScroll()) {
									recyclerView.post(new Runnable() {
										@Override
										public void run() {
											recyclerView.smoothScrollToPosition(itemsWithScroll.scrollToPosition());
										}
									});
								}
							}
						}),
				presenter.connectButtonEnabledObservable()
					.subscribe(RxView.enabled(findViewById(R.id.main_activity_connect_button))),
				presenter.disconnectButtonEnabledObservable()
						.subscribe(RxView.enabled(findViewById(R.id.macin_activity_disconnect_button))),
				presenter.sendButtonEnabledObservable()
						.subscribe(RxView.enabled(findViewById(R.id.main_activity_send_button))));
		RxView.clicks(findViewById(R.id.main_activity_connect_button))
				.subscribe(presenter.connectClickObserver());
		RxView.clicks(findViewById(R.id.macin_activity_disconnect_button))
				.subscribe(presenter.disconnectClickObserver());
		RxView.clicks(findViewById(R.id.main_activity_send_button))
				.subscribe(presenter.sendClickObserver());
	}

	private RetentionFragment getRetentionFragment(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			final RetentionFragment retentionFragment = new RetentionFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.add(retentionFragment, RETENTION_FRAGMENT_TAG)
					.commit();
			return retentionFragment;
		} else {
			return (RetentionFragment) getSupportFragmentManager()
					.findFragmentByTag(RETENTION_FRAGMENT_TAG);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (disposables != null && !disposables.isDisposed()) {
			disposables.dispose();
		}
	}

	public static class RetentionFragment extends Fragment {

		private final MainPresenter presenter;

		public RetentionFragment() {
			final Gson gson = new GsonBuilder()
					.registerTypeAdapter(Message.class, new Message.Deserializer())
					.registerTypeAdapter(MessageType.class, new MessageType.SerializerDeserializer())
					.create();

			final OkHttpClient okHttpClient = new OkHttpClient();
			final ReactiveXWebSockets webSockets = new ReactiveXWebSockets(okHttpClient, new Request.Builder()
					.get()
					.url("ws://coreos2.appunite.net:8080/ws")
					.addHeader("Sec-WebSocket-Protocol", "chat")
					.build());
			final GsonObjectSerializer serializer = new GsonObjectSerializer(gson, Message.class);
			final ReactiveXObjectWebSockets jsonWebSockets = new ReactiveXObjectWebSockets(webSockets, serializer);
			final ReactiveSocketConnection socketConnection = new ReactiveSocketConnectionImpl(jsonWebSockets, Schedulers.io());
			presenter = new MainPresenter(new ReactiveSocket(socketConnection, Schedulers.io()), Schedulers.io(), AndroidSchedulers.mainThread());
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			presenter.dispose();
		}

		public MainPresenter presenter() {
			return presenter;
		}
	}

}
