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


/* @test
   @bug 4027717
   @summary Check for correct implementation of RandomAccessFile.skipBytes
   */


import java.io.*;

public class SkipBytes{


    /*
     * doTest attempts to skip num_to_skip bytes in raf starting from position 0.
     * It also does a read after the skip to check if EOF has been reached
     * correctly or incorrectly.
     */


    private static void doTest(RandomAccessFile raf, int start, int num_to_skip)
        throws Exception
    {

        raf.seek(start);

        long cur_ptr = raf.getFilePointer();
        int length = (int) raf.length();
        System.err.println("\nCurrent pointer = " + cur_ptr + " length = " +
                           length + " num_to_skip = " + num_to_skip);

        //Do the Skip test
        int num_skipped = raf.skipBytes(num_to_skip);
        System.err.println("After skipBytes -- no. skipped = " + num_skipped);

        // if num_to_skip is negative do the negative skip test
        if (num_to_skip <= 0) {
            if (num_skipped != 0){
                System.err.println("Negative Skip Test Failed");
                throw new RuntimeException("Negative Skip Test Failed");
            }
            else {
                System.err.println("Negative Skip Test Succeeded");
            }
        }

        cur_ptr = raf.getFilePointer();
        System.err.println("Current pointer = " + cur_ptr);

        // Check if skip has gone beyond EOF.
        if (cur_ptr > length) {
            System.err.println("Past EOF Skip Test Failed");
            throw new RuntimeException("Past EOF Skip Test Failed");
        }
        else {
            System.err.println("Past EOF Skip Test Succeeded");
        }

        // do read test
        int byte_read = raf.read();
        if ( (cur_ptr == length) &&
             (byte_read != -1) ) {
            System.err.println("byte_read = " + byte_read +
                               " Read Test Failed ......");
            throw new RuntimeException("Read Test Failed");
        }
        else {
            System.err.println("byte_read = " + byte_read +
                               " Read Test Succeeded");
        }

    }

    public static void main(String[] args) throws Exception {

        RandomAccessFile raf = new RandomAccessFile("input.txt" , "rw");
        try {
            int length = (int)raf.length();

            doTest(raf , 0 , 2*length);
            doTest(raf , 0 , length);
            doTest(raf , 0 , length/2);
            doTest(raf , length/2 , -2);
            doTest(raf , length , 0);
            doTest(raf , 0 , -1);
        } finally{
            raf.close();
        }

    }

}
