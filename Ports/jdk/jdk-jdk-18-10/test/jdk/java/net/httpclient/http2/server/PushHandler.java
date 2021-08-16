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

import java.io.*;
import java.net.*;
import java.net.http.HttpHeaders;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

public class PushHandler implements Http2Handler {

    static final BiPredicate<String,String> ACCEPT_ALL = (x, y) -> true;

    final Path tempFile;
    final int loops;
    final long file_size;

    public PushHandler(Path file, int loops) throws Exception {
        tempFile = file;
        this.loops = loops;
        this.file_size = Files.size(file);
    }

    int invocation = 0;

    public void handle(Http2TestExchange ee) {
        try {
            System.err.println ("Server: handle " + ee);
            invocation++;

            if (ee.serverPushAllowed()) {
                URI requestURI = ee.getRequestURI();
                for (int i=0; i<loops; i++) {
                    InputStream is = new FileInputStream(tempFile.toFile());
                    URI u = requestURI.resolve("/x/y/z/" + Integer.toString(i));
                    HttpHeaders h = HttpHeaders.of(Map.of("X-foo", List.of("bar")),
                                                   ACCEPT_ALL);
                    ee.serverPush(u, h, is);
                }
                System.err.println ("Server: sent all pushes");
            }
            ee.sendResponseHeaders(200, file_size);
            OutputStream os = ee.getResponseBody();
            InputStream iis = new FileInputStream(tempFile.toFile());
            iis.transferTo(os);
            os.close();
            iis.close();
        } catch (Exception ex) {
            System.err.println ("Server: exception " + ex);
        }
    }
}
