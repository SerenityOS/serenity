/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package gc;

/*
 * @test TestStackOverflow
 * @bug 4396719
 * @summary Test verifies only that VM doesn't crash but throw expected Error.
 * @run main/othervm gc.TestStackOverflow
 */

public class TestStackOverflow {
    final static int LOOP_LENGTH = 1000000;
    final static int LOGGING_STEP = 10000;

    public static void main(String args[]) {
        Object object = null;

        for (int i = 0; i < LOOP_LENGTH; i++) {

            // Check progress
            if (i % LOGGING_STEP == 0) {
                System.out.println(i);
            }
            try {
                Object array[] = {object, object, object, object, object};
                object = array;
            } catch (OutOfMemoryError e) {
                object = null;
                System.out.println("Caught OutOfMemoryError.");
                return;
            } catch (StackOverflowError e) {
                object = null;
                System.out.println("Caught StackOverflowError.");
                return;
            }
        }
    }
}

