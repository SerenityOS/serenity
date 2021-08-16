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

/**
 * @test
 * @bug 8245194
 * @run testng NullTest
 */

import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.*;
import java.nio.file.Path;
import org.testng.annotations.Test;
import static org.testng.Assert.assertThrows;

/**
 * Check for NPE
 */
public class NullTest {

    // Expected exception
    private static final Class<NullPointerException> NPE =
        NullPointerException.class;

    @Test
    public static void runTest() throws Exception {
        assertThrows(NPE, () -> SocketChannel.open((ProtocolFamily)null));
        assertThrows(NPE, () -> SocketChannel.open((SocketAddress)null));
        assertThrows(NPE, () -> ServerSocketChannel.open((ProtocolFamily)null));
        assertThrows(NPE, () -> UnixDomainSocketAddress.of((Path)null));
        assertThrows(NPE, () -> UnixDomainSocketAddress.of((String)null));
    }

}
