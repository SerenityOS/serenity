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
 * @bug 8254692
 * @summary Basic test for java.nio.channels.spi.SelectorProvider.java default implementation
 * @run testng TestDefaultImplementation
 */

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

public class TestDefaultImplementation {
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;
    static final Class<NullPointerException> NPE = NullPointerException.class;
    static final ProtocolFamily BAD_PF = () -> "BAD_PROTOCOL_FAMILY";

    @Test
    public void testSelectorProvider() throws IOException {
        CustomSelectorProviderImpl customSpi = new CustomSelectorProviderImpl();

        assertNull(customSpi.inheritedChannel());

        assertThrows(NPE, () -> customSpi.openSocketChannel(null));
        assertThrows(NPE, () -> customSpi.openServerSocketChannel(null));

        assertThrows(UOE, () -> customSpi.openSocketChannel(BAD_PF));
        assertThrows(UOE, () -> customSpi.openServerSocketChannel(BAD_PF));
    }

    // A concrete subclass of SelectorProvider, in order to test the
    // default java.nio.channels.spi.SelectorProvider implementation
    static class CustomSelectorProviderImpl extends SelectorProvider {
        @Override public DatagramChannel openDatagramChannel() { return null; }
        @Override public DatagramChannel openDatagramChannel(ProtocolFamily family) { return null; }
        @Override public Pipe openPipe() { return null; }
        @Override public AbstractSelector openSelector() { return null; }
        @Override public ServerSocketChannel openServerSocketChannel() { return null; }
        @Override public SocketChannel openSocketChannel() { return null; }
    }
}