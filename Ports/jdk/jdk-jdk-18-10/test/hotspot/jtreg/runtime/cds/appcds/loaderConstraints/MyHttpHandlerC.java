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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

public class MyHttpHandlerC implements HttpHandler {
    public MyHttpHandlerC() {}

    // This class overrides handle(), but its loader (MyClassLoader) resolves the same
    // HttpExchange as the platfom loader (whose handle() method is overidden), so when
    // MyHttpHandlerC loaded it should be able to link
    public void handle(HttpExchange exchange) {
        throw new RuntimeException("MyHttpHandlerB.test() must not be able to invoke this method");
    }

    static public void test(HttpHandler handler) throws IOException {
        // This method call must fail to link.
        handler.handle(null);
    }
}

