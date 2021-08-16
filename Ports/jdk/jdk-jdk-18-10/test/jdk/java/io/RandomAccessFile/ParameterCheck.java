/*
 * Copyright (c) 1997, 2010, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 4030253 4030278 4030243
  @summary Test for correct parameter checking in read(byte[], int, int),
  readFully(byte[], int, int) and write(byte[], int, int) of RandomAccessFile
  */

import java.io.*;

public class ParameterCheck {

    static int off[] = {-1, -1,  0, 0, 33, 33, 0, 32,
                        32, 4, 1, 0, -1, Integer.MAX_VALUE, 1};
    static int len[] = {-1,  0, -1, 33, 0, 4, 32,
                        0, 4, 16, 31, 0, Integer.MAX_VALUE,
                        Integer.MAX_VALUE, Integer.MAX_VALUE};
    static boolean results[] = { false,  false,  false, false, false, false,
                                 true, true, false, true, true, true, false,
                                 false, false };
    static int numBad = 0;

    private static void doTest(String method) throws Exception {
        File fn = new File("x.ParameterCheck");
        RandomAccessFile raf = null;

        try {
            byte b[] = new byte[32];
            int numCases = off.length;
            int[] got = new int[numCases];
            int numGood = 0;
            FileOutputStream fout = new FileOutputStream(fn);
            for (int i = 0; i < 32; i++) {
                fout.write(i);
            }
            fout.close();
            raf =  new RandomAccessFile(fn , "rw");

            System.err.println("-----------------------------" +
                               "-----------------------------");
            System.err.println("\nRandomAccessFile." + method +
                               "\nTotal test cases = " + (off.length+1));
            System.err.println("-----------------------------" +
                               "-----------------------------");
            for(int i = 0; i < numCases; i++) {
                try {
                    if (method.equals("readFully")) {
                        raf.readFully(b , off[i] , len[i]);
                    }
                    if (method.equals("read")) {
                        raf.read(b , off[i] , len[i]);
                    }
                    if (method.equals("write")) {
                        raf.write(b , off[i] , len[i]);
                    }
                    raf.seek(0);
                } catch(IndexOutOfBoundsException aiobe) {
                    if (results[i]) {
                        printErr(method , numGood,
                                 i, "java.lang.IndexOutOfBoundsException");
                    } else {
                        numGood++;
                    }
                    continue;
                } catch(OutOfMemoryError ome) {
                    printErr(method, numGood,
                             i, "java.lang.OutOfMemoryError");
                    continue;
                }

                if (results[i]) {
                    numGood++;
                }
                else {
                    printErr(method, numGood,
                             i, "No java.lang.IndexOutOfBoundsException");
                }

            }

            raf.seek(0);
            boolean thrown = false;
            try {
                if (method.equals("readFully")) {
                    raf.readFully(null, 1, 2);
                }
                if (method.equals("read")) {
                    raf.read(null, 1, 2);
                }
                if (method.equals("write")) {
                    raf.write(null, 1, 2);
                }

            } catch(NullPointerException npe) {
                numGood++;
                thrown = true;
            }
            if (!thrown) {
                printErr(method, numGood, -1,
                         "no NullPointerException for null b");
            }

            System.err.println("\nTotal passed = " + numGood);
            System.err.println("-----------------------------" +
                               "-----------------------------");
        } finally {
            if (raf != null)
                raf.close();
            fn.delete();
        }

    }

    private static void printErr(String method, int numGood,
                                 int i, String expStr) {
        numBad++;
        System.err.println("\nNumber passed so far = " + numGood +
                           "\nUnexpected " + expStr);
        if ( i < 0 ) {
            System.err.println("for case : b = null");
        } else {
            System.err.println("for case : b.length = " + 32 +
                               " off = " + off[i] +
                               " len = " + len[i]);
        }
    }

    public static void main(String argv[]) throws Exception{
        doTest("read");
        doTest("readFully");
        doTest("write");

        if (numBad > 0) {
            throw new RuntimeException("Failed " + numBad + " tests");
        }
    }
}
