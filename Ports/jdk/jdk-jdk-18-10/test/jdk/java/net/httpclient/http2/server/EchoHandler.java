/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.internal.net.http.common.HttpHeadersImpl;

public class EchoHandler implements Http2Handler {
    static final Path CWD = Paths.get(".");

    public EchoHandler() {}

    @Override
    public void handle(Http2TestExchange t)
            throws IOException {
        try {
            System.err.println("EchoHandler received request to " + t.getRequestURI());
            InputStream is = t.getRequestBody();
            HttpHeadersImpl map = t.getRequestHeaders();
            HttpHeadersImpl map1 = t.getResponseHeaders();
            map1.addHeader("X-Hello", "world");
            map1.addHeader("X-Bye", "universe");
            String fixedrequest = map.firstValue("XFixed").orElse(null);
            File outfile = Files.createTempFile(CWD, "foo", "bar").toFile();
            //System.err.println ("QQQ = " + outfile.toString());
            FileOutputStream fos = new FileOutputStream(outfile);
            int count = (int) is.transferTo(fos);
            System.err.printf("EchoHandler read %d bytes\n", count);
            is.close();
            fos.close();
            InputStream is1 = new FileInputStream(outfile);
            OutputStream os = null;
            // return the number of bytes received (no echo)
            String summary = map.firstValue("XSummary").orElse(null);
            if (fixedrequest != null && summary == null) {
                t.sendResponseHeaders(200, count);
                os = t.getResponseBody();
                int count1 = (int)is1.transferTo(os);
                System.err.printf("EchoHandler wrote %d bytes\n", count1);
            } else {
                t.sendResponseHeaders(200, 0);
                os = t.getResponseBody();
                int count1 = (int)is1.transferTo(os);
                System.err.printf("EchoHandler wrote %d bytes\n", count1);

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
