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
 * @bug 8270286
 * @summary Test for HttpServerProvider::loadProviderFromProperty
 * @run testng/othervm
 *      -Dcom.sun.net.httpserver.HttpServerProvider=HttpServerProviderTest$ProviderP
 *      HttpServerProviderTest
 * @run testng/othervm
 *      -Dcom.sun.net.httpserver.HttpServerProvider=HttpServerProviderTest$ProviderPNPC
 *      HttpServerProviderTest
 * @run testng/othervm
 *      -Dcom.sun.net.httpserver.HttpServerProvider=HttpServerProviderTest$ProviderNP
 *      HttpServerProviderTest
 * @run testng/othervm
 *      -Dcom.sun.net.httpserver.HttpServerProvider=HttpServerProviderTest$ProviderT
 *      HttpServerProviderTest
 * @run testng/othervm
 *      -Dcom.sun.net.httpserver.HttpServerProvider=DoesNotExist
 *      HttpServerProviderTest
 */

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.ServiceConfigurationError;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.spi.HttpServerProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.expectThrows;

public class HttpServerProviderTest {
    public final static String PROPERTY_KEY = "com.sun.net.httpserver.HttpServerProvider";

    @Test
    public void test() throws Exception {
        var provider = System.getProperty(PROPERTY_KEY);
        switch (provider) {
            case "HttpServerProviderTest$ProviderP" -> testPublic();
            case "HttpServerProviderTest$ProviderPNPC" -> testPublicNonPublicConstructor();
            case "HttpServerProviderTest$ProviderNP" -> testNonPublic();
            case "HttpServerProviderTest$ProviderT" -> testThrowingConstructor();
            default -> testBadData();
        }
    }

    private void testPublic() throws Exception {
        var n = ProviderP.class.getName();
        assertEquals(System.getProperty(PROPERTY_KEY), n);

        var p = HttpServerProvider.provider();
        assertNull(p.createHttpServer(null, 0));
        assertNull(p.createHttpsServer(null, 0));
    }

    private void testPublicNonPublicConstructor() {
        var n = ProviderPNPC.class.getName();
        assertEquals(System.getProperty(PROPERTY_KEY), n);

        var e = expectThrows(ServiceConfigurationError.class, HttpServerProvider::provider);
        assertEquals(e.getClass(), ServiceConfigurationError.class);
        assertEquals(e.getCause().getClass(), IllegalAccessException.class);
    }

    private void testNonPublic() {
        var n = ProviderNP.class.getName();
        assertEquals(System.getProperty(PROPERTY_KEY), n);

        var e = expectThrows(ServiceConfigurationError.class, HttpServerProvider::provider);
        assertEquals(e.getClass(), ServiceConfigurationError.class);
        assertEquals(e.getCause().getClass(), IllegalAccessException.class);
    }

    private void testThrowingConstructor() {
        var cn = ProviderT.class.getName();
        assertEquals(System.getProperty(PROPERTY_KEY), cn);

        var e = expectThrows(ServiceConfigurationError.class, HttpServerProvider::provider);
        assertEquals(e.getClass(), ServiceConfigurationError.class);
        assertEquals(e.getCause().getClass(), InvocationTargetException.class);
        assertEquals(e.getCause().getCause().getMessage(), "throwing constructor");
    }

    private void testBadData() {
        var cn = "DoesNotExist";
        assertEquals(System.getProperty(PROPERTY_KEY), cn);

        var e = expectThrows(ServiceConfigurationError.class, HttpServerProvider::provider);
        assertEquals(e.getClass(), ServiceConfigurationError.class);
        assertEquals(e.getCause().getClass(), ClassNotFoundException.class);
    }

    /**
     * Test provider that is public (P)
     */
    public static class ProviderP extends HttpServerProvider {
        public ProviderP() { super(); }
        @Override
        public HttpServer createHttpServer(InetSocketAddress addr, int backlog) { return null; }
        @Override
        public HttpsServer createHttpsServer(InetSocketAddress addr, int backlog) { return null; }
    }

    /**
     * Test provider that is public with a non-public constructor (PNPC)
     */
    public static class ProviderPNPC extends HttpServerProvider {
        /*package-private*/ ProviderPNPC() { super(); }
        @Override
        public HttpServer createHttpServer(InetSocketAddress addr, int backlog) { return null; }
        @Override
        public HttpsServer createHttpsServer(InetSocketAddress addr, int backlog) { return null; }
    }

    /**
     * Test provider that is not public (NP)
     */
    /*package-private*/ static class ProviderNP extends HttpServerProvider {
        /*package-private*/ ProviderNP() { super(); }
        @Override
        public HttpServer createHttpServer(InetSocketAddress addr, int backlog) { return null; }
        @Override
        public HttpsServer createHttpsServer(InetSocketAddress addr, int backlog) { return null; }
    }

    /**
     * Test provider with a constructor that throws
     */
    public static class ProviderT extends HttpServerProvider {
        public ProviderT() { throw new AssertionError("throwing constructor"); }
        @Override
        public HttpServer createHttpServer(InetSocketAddress addr, int backlog) { return null; }
        @Override
        public HttpsServer createHttpsServer(InetSocketAddress addr, int backlog) { return null; }
    }
}
