/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8269692
 * @summary HttpContext::createContext should throw IllegalArgumentException
 *          if context already exists
 * @run testng/othervm HttpContextTest
 */

import java.io.IOException;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

public class HttpContextTest {

    static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;

    @Test
    public static void test() throws IOException {
        final var server = HttpServer.create(null, 0);
        final var path = "/foo/";

        assertThrows(IAE, () -> server.removeContext(path));
        HttpContext context = server.createContext(path);
        assertEquals(context.getPath(), path);
        assertThrows(IAE, () -> server.createContext(path));
        assertThrows(IAE, () -> server.createContext(path, new Handler()));

        context.setHandler(new Handler());
        assertThrows(IAE, () -> server.createContext(path));
        assertThrows(IAE, () -> server.createContext(path, new Handler()));
        server.removeContext(context);
        assertThrows(IAE, () -> server.removeContext(path));

        context = server.createContext(path, new Handler());
        assertEquals(context.getPath(), path);
        assertThrows(IAE, () -> server.createContext(path));
        assertThrows(IAE, () -> server.createContext(path, new Handler()));
        server.removeContext(path);
        assertThrows(IAE, () -> server.removeContext(path));
    }

    /**
     * Confirms that it is possible to create a subcontext, a context whose path
     * shares the prefix of an existing context.
     */
    @Test
    public static void testSubcontext() throws IOException {
        final var server = HttpServer.create(null, 0);
        server.createContext("/foo/bar/");
        server.createContext("/foo/");

        server.createContext("/foo");
        server.createContext("/foo/bar");
    }

    /**
     * A no-op handler
     */
    static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException { }
    }
}
