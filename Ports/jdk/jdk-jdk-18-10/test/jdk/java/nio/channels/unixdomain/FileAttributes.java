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

/**
 * @test
 * @bug 8252971
 * @library /test/lib
 * @run testng FileAttributes
 */

import java.io.IOException;
import java.io.File;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import org.testng.annotations.Test;
import org.testng.SkipException;

import static java.net.StandardProtocolFamily.UNIX;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

/**
 */
public class FileAttributes {

    @Test
    public static void test() throws Exception {
        checkSupported();
        Path path = null;
        try (var chan = SocketChannel.open(UNIX)) {
            path = Path.of("foo.sock");
            var addr = UnixDomainSocketAddress.of(path);

            chan.bind(addr);

            // Check file exists

            File f = path.toFile();
            assertTrue(f.exists(), "File.exists failed");

            assertTrue(Files.exists(path), "Files.exists failed");

            // Check basic attributes
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

            assertFalse(attrs.isDirectory(), "file is not a directory");
            assertTrue(attrs.isOther(), "file is other");
            assertFalse(attrs.isRegularFile(), "file is not a regular file");
            assertFalse(attrs.isSymbolicLink(), "file is not a symbolic link");

            // Check can't copy
            final Path src = path;
            final Path dest = Path.of("bar.sock");
            assertThrows(IOException.class, () -> Files.copy(src, dest));

            // Check deletion
            assertTrue(f.delete(), "File.delete failed");
        } finally {
            Files.deleteIfExists(path);
        }
    }

    static void checkSupported() {
        try {
            SocketChannel.open(UNIX).close();
        } catch (UnsupportedOperationException e) {
            throw new SkipException("Unix domain channels not supported");
        } catch (Exception e) {
            // continue test to see what problem is
        }
    }
}
