/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4937962
 * @summary ProxySelector.connectFailed and .select never throw IllegalArgumentException
 */
import java.net.*;
import java.util.List;
import java.io.IOException;

public class NullArguments {
    public static void main(String[] args) {
        ProxySelector ps = ProxySelector.getDefault();
        List p = null;
        boolean ok = false;
        if (ps != null) {
            try {
                p = ps.select(null);
            } catch (IllegalArgumentException iae) {
                System.out.println("OK");
                ok = true;
            }
            if (!ok)
                throw new RuntimeException("Expected IllegalArgumentException!");
            URI uri = null;
            try {
                uri = new URI("http://java.sun.com");
            } catch (java.net.URISyntaxException use) {
                // can't happen
            }
            SocketAddress sa = new InetSocketAddress("localhost", 80);
            IOException ioe = new IOException("dummy IOE");
            ok = false;
            try {
                ps.connectFailed(uri, sa, null);
            } catch (IllegalArgumentException iae) {
                System.out.println("OK");
                ok = true;
            }
            if (!ok)
                throw new RuntimeException("Expected IllegalArgumentException!");
            ok = false;
            try {
                ps.connectFailed(uri, null, ioe);
            } catch (IllegalArgumentException iae) {
                System.out.println("OK");
                ok = true;
            }
            if (!ok)
                throw new RuntimeException("Expected IllegalArgumentException!");
            ok = false;
            try {
                ps.connectFailed(null, sa, ioe);
            } catch (IllegalArgumentException iae) {
                System.out.println("OK");
                ok = true;
            }
            if (!ok)
                throw new RuntimeException("Expected IllegalArgumentException!");
        }
    }
}
