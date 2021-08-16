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
 * @bug 8231358
 * @compile ../../nio/file/spi/TestProvider.java AddressTest.java
 * @run testng/othervm AddressTest
 */

import java.net.UnixDomainSocketAddress;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.spi.FileSystemProvider;
import java.nio.file.Path;

import org.testng.annotations.Test;

import static org.testng.Assert.assertThrows;

/**
 * Verify that UnixDomainSocketAddress.of(path) throws IAE
 * if given a Path that does not originate from system default
 * file system.
 */
public class AddressTest {

    // Expected exception
    private static final Class<IllegalArgumentException> IAE =
        IllegalArgumentException.class;

    @Test
    public static void runTest() throws Exception {
        TestProvider prov = new TestProvider(FileSystems.getDefault().provider());
        Path path = prov.getPath(URI.create("file:/"));
        assertThrows(IAE, () -> UnixDomainSocketAddress.of(path));
    }
}
