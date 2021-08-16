/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.net.http;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import jdk.internal.net.http.common.Logger;
import jdk.internal.net.http.common.Utils;

public class ResponseTimerEvent extends TimeoutEvent {
    private static final Logger debug =
            Utils.getDebugLogger("ResponseTimerEvent"::toString, Utils.DEBUG);

    private final MultiExchange<?> multiExchange;

    static ResponseTimerEvent of(MultiExchange<?> exchange) {
        return new ResponseTimerEvent(exchange);
    }

    private ResponseTimerEvent(MultiExchange<?> multiExchange) {
        super(multiExchange.exchange.request.timeout().get());
        this.multiExchange = multiExchange;
    }

    @Override
    public void handle() {
        if (debug.on()) {
            debug.log("Cancelling MultiExchange due to timeout for request %s",
                      multiExchange.exchange.request);
        }
        HttpTimeoutException t = null;

        // more specific, "request timed out", message when connected
        Exchange<?> exchange = multiExchange.getExchange();
        if (exchange != null) {
            ExchangeImpl<?> exchangeImpl = exchange.exchImpl;
            if (exchangeImpl != null) {
                if (exchangeImpl.connection().connected()) {
                    t = new HttpTimeoutException("request timed out");
                }
            }
        }
        if (t == null) {
            t = new HttpConnectTimeoutException("HTTP connect timed out");
            t.initCause(new ConnectException("HTTP connect timed out"));
        }
        multiExchange.cancel(t);
    }

    @Override
    public String toString() {
        return "ResponseTimerEvent[" + super.toString() + "]";
    }
}
