/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4283413 4171233
 * @summary Make sure that the mark() and reset() methods behave as expected.
 */

import java.io.*;


public class MarkReset {
    public static void main(String args[]) throws Exception {

        boolean expOccurred = false;
        byte data[] = {90, 91, 92, 93, 94, 95, 96, 97};
        PushbackInputStream in =
            new PushbackInputStream(new ByteArrayInputStream(data));
        in.mark(-5);
        in.mark(6);
        in.mark(Integer.MAX_VALUE);
        try {
            in.reset();
            throw new RuntimeException("Expected exception not thrown");
        } catch (IOException e) {
            // Correct result
        }
        in.read();
        in.read();
        try {
            in.reset();
            throw new RuntimeException("Expected exception not thrown");
        } catch (IOException e) {
            // Correct result
        }
    }
}
