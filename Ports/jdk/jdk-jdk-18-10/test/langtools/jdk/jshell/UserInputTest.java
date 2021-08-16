/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8131023 8167461
 * @summary Verify that the user's code can read System.in
 * @build KullaTesting TestingInputStream
 * @run testng UserInputTest
 * @key intermittent
 */

import java.io.IOException;
import java.io.InputStream;

import org.testng.annotations.Test;

@Test
public class UserInputTest extends KullaTesting {

    public void testReadInput() {
        setInput("AB\n");
        assertEval("System.in.read()", "65");
        setInput("CD\n");
        assertEval("System.in.read()", "67");
    }

    public void testScanner() {
        assertEval("import java.util.Scanner;");
        assertEval("Scanner s = new Scanner(System.in);");
        setInput("12\n");
        assertEval("s.nextInt();", "12");
    }

    public void testClose() {
        setInput(new InputStream() {
            private final byte[] data = new byte[] {0, 1, 2};
            private int cursor;
            @Override public int read() throws IOException {
                if (cursor < data.length) {
                    return data[cursor++];
                } else {
                    return -1;
                }
            }
        });
        assertEval("int read;", "0");
        assertEval("System.in.read();", "0");
        assertEval("System.in.read();", "1");
        assertEval("System.in.read();", "2");
        assertEval("System.in.read();", "-1");
        assertEval("System.in.read();", "-1");
        assertEval("System.in.read();", "-1");
    }

    public void testException() {
        setInput(new InputStream() {
            private final int[] data = new int[] {0, 1, -2, 2};
            private int cursor;
            @Override public int read() throws IOException {
                if (cursor < data.length) {
                    int d = data[cursor++];
                    if (d == (-2)) {
                        throw new IOException("Crashed");
                    }
                    return d;
                } else {
                    return -1;
                }
            }
        });
        assertEval("int read;", "0");
        assertEval("System.in.read();", "0");
        assertEval("System.in.read();", "1");
        assertEval("java.io.IOException e;");
        assertEval("try { System.in.read(); } catch (java.io.IOException exc) { e = exc; }");
        assertEval("e", "java.io.IOException: Crashed");
        assertEval("System.in.read();", "2");
        assertEval("System.in.read();", "-1");
    }
}
