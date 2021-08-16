/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8205612
 * @run testng CallWithInterruptSet
 * @summary Test invoking Files methods with the interrupt status set
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CallWithInterruptSet {

    @Test
    public void testReadAllBytes() throws Exception {
        Path file = mkfile(100);
        Thread.currentThread().interrupt();
        try {
            byte[] bytes = Files.readAllBytes(file);
            assertTrue(bytes.length == 100);
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
    }

    @Test
    public void testInputStream() throws IOException {
        Path file = mkfile(100);
        Thread.currentThread().interrupt();
        try (InputStream in = Files.newInputStream(file)) {
            int n = in.read(new byte[10]);
            assertTrue(n > 0);
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
    }

    @Test
    public void testOutputStream() throws Exception {
        Path file = mkfile();
        try (OutputStream out = Files.newOutputStream(file)) {
            Thread.currentThread().interrupt();
            out.write(new byte[10]);
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
        assertTrue(Files.size(file) == 10);
    }

    @Test
    public void testReadString() throws Exception {
        Path file = mkfile();
        Files.writeString(file, "hello");
        Thread.currentThread().interrupt();
        try {
            String msg = Files.readString(file);
            assertEquals(msg, "hello");
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
    }

    @Test
    public void testWriteString() throws Exception {
        Path file = mkfile();
        Thread.currentThread().interrupt();
        try {
            Files.writeString(file, "hello");
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
        String msg = Files.readString(file);
        assertEquals(msg, "hello");
    }

    @Test
    public void testBufferedReader() throws Exception {
        Path file = mkfile();
        Files.writeString(file, "hello");
        Thread.currentThread().interrupt();
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String msg = reader.readLine();
            assertEquals(msg, "hello");
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
    }

    @Test
    public void testBufferedWriter() throws Exception {
        Path file = mkfile();
        Thread.currentThread().interrupt();
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("hello");
        } finally {
            assertTrue(Thread.interrupted());  // clear interrupt
        }
        String msg = Files.readString(file);
        assertEquals(msg, "hello");
    }

    private Path mkfile() throws IOException {
        return Files.createTempFile(Path.of("."), "tmp", "tmp");
    }

    private Path mkfile(int size) throws IOException {
        return Files.write(mkfile(), new byte[size]);
    }

}
