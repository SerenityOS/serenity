/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import javax.net.ssl.SSLSession;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.common.HttpHeadersBuilder;

/**
 * A supplier of Http2TestExchanges. If the default Http2TestExchange impl is
 * not sufficient, then a supplier may be set on an Http2TestServer through its
 * {@link Http2TestServer#setExchangeSupplier(Http2TestExchangeSupplier)}.
 *
 * Useful for testing scenarios where non-standard or specific server behaviour
 * is required, either direct control over the frames sent, "bad" behaviour, or
 * something else.
 */
public interface Http2TestExchangeSupplier {

    Http2TestExchange get(int streamid,
                          String method,
                          HttpHeaders reqheaders,
                          HttpHeadersBuilder rspheadersBuilder,
                          URI uri,
                          InputStream is,
                          SSLSession sslSession,
                          BodyOutputStream os,
                          Http2TestServerConnection conn,
                          boolean pushAllowed);

    static Http2TestExchangeSupplier ofDefault() {
        return Http2TestExchangeImpl::new;
    }
}
