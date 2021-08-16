/*
 * Copyright 2012 Skip Balk.  All Rights Reserved.
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
 * @bug 7179138
 * @summary Incorrect result with String concatenation optimization
 *
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:-TieredCompilation
 *      compiler.c2.Test7179138_1
 *
 * @author Skip Balk
 */

package compiler.c2;

public class Test7179138_1 {
    public static void main(String[] args) throws Exception {
        System.out.println("Java Version: " + System.getProperty("java.vm.version"));
        long[] durations = new long[60];
        for (int i = 0; i < 100000; i++) {
            // this empty for-loop is required to reproduce this bug
            for (long duration : durations) {
                // do nothing
            }
            {
                String s = "test";
                int len = s.length();

                s = new StringBuilder(String.valueOf(s)).append(s).toString();
                len = len + len;

                s = new StringBuilder(String.valueOf(s)).append(s).toString();
                len = len + len;

                s = new StringBuilder(String.valueOf(s)).append(s).toString();
                len = len + len;

                if (s.length() != len) {
                    System.out.println("Failed at iteration: " + i);
                    System.out.println("Length mismatch: " + s.length() + " <> " + len);
                    System.out.println("Expected: \"" + "test" + "test" + "test" + "test" + "test" + "test" + "test" + "test" + "\"");
                    System.out.println("Actual:   \"" + s + "\"");
                    System.exit(97);
                }
            }
        }
    }
}

