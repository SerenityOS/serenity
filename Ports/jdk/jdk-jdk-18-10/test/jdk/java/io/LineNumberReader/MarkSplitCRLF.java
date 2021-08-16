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
 * @bug 8218280
 * @summary Make sure marking a line feed within a CRLF sequence works correctly
 * @run testng MarkSplitCRLF
 */

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.io.StringReader;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MarkSplitCRLF {
    @Test
    public static void testSpecifiedBufferSize() throws IOException {
        final String string = "foo\r\nbar";
        try (Reader reader =
            new LineNumberReader(new StringReader(string), 5)) {
            reader.read();  // 'f'
            reader.read();  // 'o'
            reader.read();  // 'o'
            reader.read();  // '\r' -> '\n'
            reader.mark(1); // mark position of '\n'
            reader.read();  // 'b'
            reader.reset(); // reset to '\n' position
            assertEquals(reader.read(), 'b');
            assertEquals(reader.read(), 'a');
            assertEquals(reader.read(), 'r');
        }
    }

    @Test
    public static void testCRNotFollowedByLF() throws IOException {
        final String string = "foo\rbar";
        try (Reader reader =
            new LineNumberReader(new StringReader(string), 5)) {
            reader.read();  // 'f'
            reader.read();  // 'o'
            reader.read();  // 'o'
            reader.read();  // '\r'
            reader.mark(1); // mark position of next character
            reader.read();  // 'b'
            reader.reset(); // reset to position after '\r'
            assertEquals(reader.read(), 'b');
            assertEquals(reader.read(), 'a');
            assertEquals(reader.read(), 'r');
        }
    }

    @Test
    public static void testDefaultBufferSize() throws IOException {
        StringBuilder sb = new StringBuilder(8195);
        for (int i = 0; i < 8190; i++) {
            char c = (char)i;
            sb.append(c);
        }
        sb.append('\r');
        sb.append('\n');
        sb.append('X');
        sb.append('Y');
        sb.append('Z');
        final String string = sb.toString();
        try (Reader reader = new LineNumberReader(new StringReader(string))) {
            for (int i = 0; i < 8191; i++) reader.read();
            reader.mark(1);
            reader.read();
            reader.reset();
            assertEquals(reader.read(), 'X');
            assertEquals(reader.read(), 'Y');
            assertEquals(reader.read(), 'Z');
        }
    }
}
