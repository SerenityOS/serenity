/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.net.httpserver.*;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.util.concurrent.*;
import javax.net.ssl.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import jdk.test.lib.net.SimpleSSLContext;
import static java.net.http.HttpRequest.*;
import static java.net.http.HttpResponse.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpEchoHandler implements HttpHandler {
    static final Path CWD = Paths.get(".");

    public HttpEchoHandler() {}

    @Override
    public void handle(HttpExchange t)
            throws IOException {
        try {
            System.err.println("EchoHandler received request to " + t.getRequestURI());
            InputStream is = t.getRequestBody();
            Headers map = t.getRequestHeaders();
            Headers map1 = t.getResponseHeaders();
            map1.add("X-Hello", "world");
            map1.add("X-Bye", "universe");
            String fixedrequest = map.getFirst("XFixed");
            File outfile = Files.createTempFile(CWD, "foo", "bar").toFile();
            FileOutputStream fos = new FileOutputStream(outfile);
            int count = (int) is.transferTo(fos);
            is.close();
            fos.close();
            InputStream is1 = new FileInputStream(outfile);
            OutputStream os = null;
            // return the number of bytes received (no echo)
            String summary = map.getFirst("XSummary");
            if (fixedrequest != null && summary == null) {
                t.sendResponseHeaders(200, count);
                os = t.getResponseBody();
                is1.transferTo(os);
            } else {
                t.sendResponseHeaders(200, 0);
                os = t.getResponseBody();
                is1.transferTo(os);

                if (summary != null) {
                    String s = Integer.toString(count);
                    os.write(s.getBytes());
                }
            }
            outfile.delete();
            os.close();
            is1.close();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }
}
