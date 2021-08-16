/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005419
 * @summary Improve intrinsics code performance on x86 by using AVX2
 *
 * @run main/othervm -Xbatch -Xmx128m compiler.intrinsics.Test8005419
 */

package compiler.intrinsics;

public class Test8005419 {
    public static int SIZE = 64;

    public static void main(String[] args) {
        char[] a = new char[SIZE];
        char[] b = new char[SIZE];

        for (int i = 16; i < SIZE; i++) {
          a[i] = (char)i;
          b[i] = (char)i;
        }
        String s1 = new String(a);
        String s2 = new String(b);

        // Warm up
        boolean failed = false;
        int result = 0;
        for (int i = 0; i < 10000; i++) {
          result += test(s1, s2);
        }
        for (int i = 0; i < 10000; i++) {
          result += test(s1, s2);
        }
        for (int i = 0; i < 10000; i++) {
          result += test(s1, s2);
        }
        if (result != 0) failed = true;

        System.out.println("Start testing");
        // Compare same string
        result = test(s1, s1);
        if (result != 0) {
          failed = true;
          System.out.println("Failed same: result = " + result + ", expected 0");
        }
        // Compare equal strings
        for (int i = 1; i <= SIZE; i++) {
          s1 = new String(a, 0, i);
          s2 = new String(b, 0, i);
          result = test(s1, s2);
          if (result != 0) {
            failed = true;
            System.out.println("Failed equals s1[" + i + "], s2[" + i + "]: result = " + result + ", expected 0");
          }
        }
        // Compare equal strings but different sizes
        for (int i = 1; i <= SIZE; i++) {
          s1 = new String(a, 0, i);
          for (int j = 1; j <= SIZE; j++) {
            s2 = new String(b, 0, j);
            result = test(s1, s2);
            if (result != (i-j)) {
              failed = true;
              System.out.println("Failed diff size s1[" + i + "], s2[" + j + "]: result = " + result + ", expected " + (i-j));
            }
          }
        }
        // Compare strings with one char different and different sizes
        for (int i = 1; i <= SIZE; i++) {
          s1 = new String(a, 0, i);
          for (int j = 0; j < i; j++) {
            b[j] -= 3; // change char
            s2 = new String(b, 0, i);
            result = test(s1, s2);
            int chdiff = a[j] - b[j];
            if (result != chdiff) {
              failed = true;
              System.out.println("Failed diff char s1[" + i + "], s2[" + i + "]: result = " + result + ", expected " + chdiff);
            }
            result = test(s2, s1);
            chdiff = b[j] - a[j];
            if (result != chdiff) {
              failed = true;
              System.out.println("Failed diff char s2[" + i + "], s1[" + i + "]: result = " + result + ", expected " + chdiff);
            }
            b[j] += 3; // restore
          }
        }
        if (failed) {
          System.out.println("FAILED");
          System.exit(97);
        }
        System.out.println("PASSED");
    }

    private static int test(String str1, String str2) {
        return str1.compareTo(str2);
    }
}
