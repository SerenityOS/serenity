/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;
import sun.net.spi.DefaultProxySelector;

import java.net.ProxySelector;
import java.net.URI;

/**
 * @test
 * @bug 6563286 6797318 8177648
 * @summary Tests sun.net.spi.DefaultProxySelector#select(URI)
 * @run testng DefaultProxySelectorTest
 * @modules java.base/sun.net.spi:+open
 */
public class DefaultProxySelectorTest {

    /**
     * Tests that {@link DefaultProxySelector#select(URI)} throws
     * {@link IllegalArgumentException} when passed {@code null}
     */
    @Test
    public void testIllegalArgForNull() {
        final ProxySelector selector = new DefaultProxySelector();
        try {
            selector.select(null);
            Assert.fail("select() was expected to fail for null URI");
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }

    /**
     * Tests that {@link DefaultProxySelector} throws a {@link IllegalArgumentException}
     * for URIs that don't have host information
     *
     * @throws Exception
     */
    @Test
    public void testIllegalArgForNoHost() throws Exception {
        final ProxySelector selector = new DefaultProxySelector();
        assertFailsWithIAE(selector, new URI("http", "/test", null));
        assertFailsWithIAE(selector, new URI("https", "/test2", null));
        assertFailsWithIAE(selector, new URI("ftp", "/test3", null));
    }


    /**
     * Tests that {@link DefaultProxySelector} throws a {@link IllegalArgumentException}
     * for URIs that don't have protocol/scheme information
     *
     * @throws Exception
     */
    @Test
    public void testIllegalArgForNoScheme() throws Exception {
        final ProxySelector selector = new DefaultProxySelector();
        assertFailsWithIAE(selector, new URI(null, "/test", null));
    }

    private static void assertFailsWithIAE(final ProxySelector selector, final URI uri) {
        try {
            selector.select(uri);
            Assert.fail("select() was expected to fail for URI " + uri);
        } catch (IllegalArgumentException iae) {
            // expected
        }
    }
}
