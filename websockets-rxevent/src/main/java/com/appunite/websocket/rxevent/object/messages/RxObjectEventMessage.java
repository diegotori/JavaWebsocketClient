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

package com.appunite.websocket.rxevent.object.messages;

import com.appunite.websocket.rxevent.messages.RxEventStringMessage;
import com.appunite.websocket.rxevent.object.ObjectParseException;
import com.appunite.websocket.rxevent.object.ObjectWebSocketSender;
import javax.annotation.Nonnull;

/**
 * Event indicating that data returned by server was parsed
 *
 * If {@link ObjectParseException} occur than {@link RxObjectEventWrongMessageFormat} event
 * will be served
 *
 * @see RxEventStringMessage
 */
public class RxObjectEventMessage extends RxObjectEventConn {
    @Nonnull
    private final Object message;

    public RxObjectEventMessage(@Nonnull ObjectWebSocketSender sender, @Nonnull Object message) {
        super(sender);
        this.message = message;
    }

    /**
     * Served parse message
     * @param <T> Class type of message
     * @return a message that was returned
     *
     * @throws ClassCastException when wrong try to get wrong type of message
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public <T> T message() throws ClassCastException {
        return (T) message;
    }

    @Override
    public String toString() {
        return "RxJsonEventMessage{" +
                "message='" + message + '\'' +
                '}';
    }
}
