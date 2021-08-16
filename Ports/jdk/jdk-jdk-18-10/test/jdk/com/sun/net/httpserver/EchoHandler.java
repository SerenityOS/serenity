/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Implements a basic static EchoHandler for an HTTP server
 */
public class EchoHandler implements HttpHandler {
    public void handle (HttpExchange t)
        throws IOException
    {
        InputStream is = t.getRequestBody();
        Headers map = t.getRequestHeaders();
        String fixedrequest = map.getFirst ("XFixed");

        // return the number of bytes received (no echo)
        String summary = map.getFirst ("XSummary");
        OutputStream os = t.getResponseBody();
        byte[] in;
        in = is.readAllBytes();
        if (summary != null) {
            in = Integer.toString(in.length).getBytes(StandardCharsets.UTF_8);
        }
        if (fixedrequest != null) {
            t.sendResponseHeaders(200, in.length == 0 ? -1 : in.length);
        } else {
            t.sendResponseHeaders(200, 0);
        }
        os.write(in);
        close(t, os);
        close(t, is);
    }

    protected void close(OutputStream os) throws IOException {
        os.close();
    }
    protected void close(InputStream is) throws IOException {
        is.close();
    }
    protected void close(HttpExchange t, OutputStream os) throws IOException {
        close(os);
    }
    protected void close(HttpExchange t, InputStream is) throws IOException {
        close(is);
    }
}
