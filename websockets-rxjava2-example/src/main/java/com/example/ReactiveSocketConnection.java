package com.example;

import com.appunite.websocket.rxevent.object.messages.RxObjectEvent;
import io.reactivex.Flowable;
import javax.annotation.Nonnull;

/**
 * Created by son_g on 11/12/2017.
 */

public interface ReactiveSocketConnection {
    @Nonnull
    Flowable<RxObjectEvent> connection();
}
