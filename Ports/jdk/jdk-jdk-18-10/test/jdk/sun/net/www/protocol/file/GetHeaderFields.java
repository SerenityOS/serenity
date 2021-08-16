/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8241389
 * @summary URLConnection::getHeaderFields returns result inconsistent with getHeaderField/Key for FileURLConnection, FtpURLConnection
 */

import java.net.*;
import java.io.*;
import java.util.*;

public class GetHeaderFields {

    public static void main(String[] args) {
        HashMap<String,List<String>> map1 = new HashMap<>();
        Map<String,List<String>> headers = null;

        try {
            File f = File.createTempFile("test", null);
            f.deleteOnExit();
            String s = f.getAbsolutePath();
            s = s.startsWith("/") ? s : "/" + s;
            URL url = new URL("file://localhost"+s);
            URLConnection urlc = url.openConnection();
            InputStream in = urlc.getInputStream();
            headers = urlc.getHeaderFields();
            Set<String> keys = headers.keySet();

            int i = 0;
            while (true) {
                String key = urlc.getHeaderFieldKey(i);
                if (key == null)
                    break;
                String val = urlc.getHeaderField(i);
                putValue(map1,key,val);
                i++;
            }
            System.out.println("headers = " + headers.toString());
            System.out.println("map1 = " + map1.toString());
            in.close();
        } catch (java.io.FileNotFoundException fnfe) {
            throw new RuntimeException("failed to recognize localhost");
        } catch (Exception ex) {
            throw new RuntimeException("Unexpected exception: " + ex);
        }
        if (!map1.equals(headers)) {
            throw new RuntimeException("headers != map1");
        }

        // check connection to non-existent resource returns
        // an empty map

        URLConnection urlc = null;

        try {
            URL url = new URL("file:///dev/non/existing/ABCD/123/foobar/sqisjjlk");
            urlc = url.openConnection();
            urlc.getInputStream();
        } catch (IOException e) {}

        headers = urlc.getHeaderFields();
        if (headers == null) {
            throw new RuntimeException("expected empty map");
        }
        if (!headers.isEmpty()) {
            throw new RuntimeException("expected empty map");
        }

    }

    static void putValue(HashMap<String,List<String>> map, String key, String val) {
        List<String> l = map.get(key);
        if (l == null) {
            l = new LinkedList<String>();
            map.put(key, l);
        }
        l.add(val);
    }
}
