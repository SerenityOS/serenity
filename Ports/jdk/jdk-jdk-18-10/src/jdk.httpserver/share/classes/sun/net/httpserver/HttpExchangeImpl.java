/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.httpserver;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.*;
import com.sun.net.httpserver.*;
import com.sun.net.httpserver.spi.*;

class HttpExchangeImpl extends HttpExchange {

    ExchangeImpl impl;

    HttpExchangeImpl (ExchangeImpl impl) {
        this.impl = impl;
    }

    public Headers getRequestHeaders () {
        return impl.getRequestHeaders();
    }

    public Headers getResponseHeaders () {
        return impl.getResponseHeaders();
    }

    public URI getRequestURI () {
        return impl.getRequestURI();
    }

    public String getRequestMethod (){
        return impl.getRequestMethod();
    }

    public HttpContextImpl getHttpContext (){
        return impl.getHttpContext();
    }

    public void close () {
        impl.close();
    }

    public InputStream getRequestBody () {
        return impl.getRequestBody();
    }

    public int getResponseCode () {
        return impl.getResponseCode();
    }

    public OutputStream getResponseBody () {
        return impl.getResponseBody();
    }


    public void sendResponseHeaders (int rCode, long contentLen)
    throws IOException
    {
        impl.sendResponseHeaders (rCode, contentLen);
    }

    public InetSocketAddress getRemoteAddress (){
        return impl.getRemoteAddress();
    }

    public InetSocketAddress getLocalAddress (){
        return impl.getLocalAddress();
    }

    public String getProtocol (){
        return impl.getProtocol();
    }

    public Object getAttribute (String name) {
        return impl.getAttribute (name);
    }

    public void setAttribute (String name, Object value) {
        impl.setAttribute (name, value);
    }

    public void setStreams (InputStream i, OutputStream o) {
        impl.setStreams (i, o);
    }

    public HttpPrincipal getPrincipal () {
        return impl.getPrincipal();
    }

    ExchangeImpl getExchangeImpl () {
        return impl;
    }
}
