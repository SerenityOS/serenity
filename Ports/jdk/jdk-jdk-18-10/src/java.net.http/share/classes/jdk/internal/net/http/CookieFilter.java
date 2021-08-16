/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.CookieHandler;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.http.HttpHeaders;
import jdk.internal.net.http.common.HttpHeadersBuilder;
import jdk.internal.net.http.common.Log;
import jdk.internal.net.http.common.Utils;

class CookieFilter implements HeaderFilter {

    public CookieFilter() {
    }

    @Override
    public void request(HttpRequestImpl r, MultiExchange<?> e) throws IOException {
        HttpClientImpl client = e.client();
        Optional<CookieHandler> cookieHandlerOpt = client.cookieHandler();
        if (cookieHandlerOpt.isPresent()) {
            CookieHandler cookieHandler = cookieHandlerOpt.get();
            Map<String,List<String>> userheaders = r.getUserHeaders().map();
            Map<String,List<String>> cookies = cookieHandler.get(r.uri(), userheaders);

            // add the returned cookies
            HttpHeadersBuilder systemHeadersBuilder = r.getSystemHeadersBuilder();
            if (cookies.isEmpty()) {
                Log.logTrace("Request: no cookie to add for {0}", r.uri());
            } else {
                Log.logTrace("Request: adding cookies for {0}", r.uri());
            }
            for (Map.Entry<String,List<String>> entry : cookies.entrySet()) {
                final String hdrname = entry.getKey();
                if (!hdrname.equalsIgnoreCase("Cookie")
                        && !hdrname.equalsIgnoreCase("Cookie2"))
                    continue;
                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) continue;
                for (String val : values) {
                    if (Utils.isValidValue(val)) {
                        systemHeadersBuilder.addHeader(hdrname, val);
                    }
                }
            }
        } else {
            Log.logTrace("Request: No cookie manager found for {0}", r.uri());
        }
    }

    @Override
    public HttpRequestImpl response(Response r) throws IOException {
        HttpHeaders hdrs = r.headers();
        HttpRequestImpl request = r.request();
        Exchange<?> e = r.exchange;
        Log.logTrace("Response: processing cookies for {0}", request.uri());
        Optional<CookieHandler> cookieHandlerOpt = e.client().cookieHandler();
        if (cookieHandlerOpt.isPresent()) {
            CookieHandler cookieHandler = cookieHandlerOpt.get();
            Log.logTrace("Response: parsing cookies from {0}", hdrs.map());
            cookieHandler.put(request.uri(), hdrs.map());
        } else {
            Log.logTrace("Response: No cookie manager found for {0}",
                         request.uri());
        }
        return null;
    }
}
