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

/* @test
 * @bug 8227609
 * @summary Test of InputStream and OutputStream created by java.nio.file.Files
 * @library ..
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ClosedChannelException;
import java.nio.file.*;
import static java.nio.file.Files.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

public class InputStreamTest {

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            testSkip(dir);
        } finally {
            TestUtil.removeAll(dir);
        }
    }

    /**
     * Tests Files.newInputStream(Path).skip().
     */
    static void testSkip(Path tmpdir) throws IOException {
        Path file = createFile(tmpdir.resolve("foo"));
        try (OutputStream out = Files.newOutputStream(file)) {
            final int size = 512;
            byte[] blah = new byte[size];
            for (int i = 0; i < size; i++) {
                blah[i] = (byte)(i % 128);
            }
            out.write(blah);
            out.close();

            try (InputStream in = Files.newInputStream(file)) {
                assertTrue(in.available() == size);
                assertTrue(in.skip(size/4) == size/4); // 0.25
                assertTrue(in.available() == 3*size/4);

                int b = in.read();
                assertTrue(b == blah[size/4]);
                assertTrue(in.available() == 3*size/4 - 1);
                assertTrue(in.skip(-1) == -1); // 0.25
                assertTrue(in.available() == 3*size/4);

                assertTrue(in.skip(-size/2) == -size/4); // 0
                assertTrue(in.available() == size);

                assertTrue(in.skip(5*size/4) == size); // 1.0
                assertTrue(in.available() == 0);

                assertTrue(in.skip(-3*size/4) == -3*size/4); // 0.25
                assertTrue(in.available() == 3*size/4);

                byte[] buf = new byte[16];
                in.read(buf, 2, 12);
                assertTrue(Arrays.equals(buf, 2, 14,
                    blah, size/4, size/4 + 12));
                assertTrue(in.skip(-12) == -12); // 0.25

                assertTrue(in.skip(3*size/4) == 3*size/4); // 1.0
                assertTrue(in.available() == 0);

                assertTrue(in.skip(-size/2) == -size/2); // 0.5
                assertTrue(in.available() == size/2);

                assertTrue(in.skip(-size) == -size/2); // 0
                assertTrue(in.available() == size);

                assertTrue(in.skip(size/2) == size/2); // 0.5
                assertTrue(in.available() == size/2);

                assertTrue(in.skip(Long.MIN_VALUE) == -size/2); // 0
                assertTrue(in.available() == size);

                assertTrue(in.skip(size/2) == size/2); // 0.5
                assertTrue(in.available() == size/2);

                assertTrue(in.skip(Long.MAX_VALUE - size/4) == size/2);
                assertTrue(in.available() == 0);

                in.close();
                try {
                    in.skip(1);
                    throw new RuntimeException("skip() did not fail");
                } catch (IOException ioe) {
                    if (!(ioe instanceof ClosedChannelException)) {
                        throw new RuntimeException
                            ("IOException is not a ClosedChannelException");
                    }
                }
            }
        }
    }

    static void assertTrue(boolean okay) {
        if (!okay)
            throw new RuntimeException("Assertion Failed");
    }
}
