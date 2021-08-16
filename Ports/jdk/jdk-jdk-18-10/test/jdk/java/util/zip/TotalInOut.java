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

/* @test
 * @bug 7188852
 * @summary Test De/Inflater.getBytesRead/Written()
 * @key randomness
 */

import java.io.*;
import java.util.*;
import java.util.zip.*;


public class TotalInOut {
     static final int BUF_SIZE= 1 * 1024 * 1024;

     static void realMain (String[] args) throws Throwable {
         long dataSize = 128L * 1024L * 1024L;      // 128MB
         if (args.length > 0 && "large".equals(args[0]))
             dataSize = 5L * 1024L * 1024L * 1024L; //  5GB

         Deflater deflater = new Deflater();
         Inflater inflater = new Inflater();

         byte[] dataIn = new byte[BUF_SIZE];
         byte[] dataOut = new byte[BUF_SIZE];
         byte[] tmp = new byte[BUF_SIZE];

         Random r = new Random();
         r.nextBytes(dataIn);
         long bytesReadDef    = 0;
         long bytesWrittenDef = 0;
         long bytesReadInf    = 0;
         long bytesWrittenInf = 0;

         deflater.setInput(dataIn, 0, dataIn.length);
         while (bytesReadDef < dataSize || bytesWrittenInf < dataSize) {
             int len = r.nextInt(BUF_SIZE/2) + BUF_SIZE / 2;
             if (deflater.needsInput()) {
                 bytesReadDef += dataIn.length;
                 check(bytesReadDef == deflater.getBytesRead());
                 deflater.setInput(dataIn, 0, dataIn.length);
             }
             int n = deflater.deflate(tmp, 0, len);
             bytesWrittenDef += n;
             check(bytesWrittenDef == deflater.getBytesWritten());

             inflater.setInput(tmp, 0, n);
             bytesReadInf += n;
             while (!inflater.needsInput()) {
                 bytesWrittenInf += inflater.inflate(dataOut, 0, dataOut.length);
                 check(bytesWrittenInf == inflater.getBytesWritten());
             }
             check(bytesReadInf == inflater.getBytesRead());
         }
     }

     //--------------------- Infrastructure ---------------------------
     static volatile int passed = 0, failed = 0;
     static void pass() {passed++;}
     static void pass(String msg) {System.out.println(msg); passed++;}
     static void fail() {failed++; Thread.dumpStack();}
     static void fail(String msg) {System.out.println(msg); fail();}
     static void unexpected(Throwable t) {failed++; t.printStackTrace();}
     static void unexpected(Throwable t, String msg) {
         System.out.println(msg); failed++; t.printStackTrace();}
     static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}
     static void equal(Object x, Object y) {
          if (x == null ? y == null : x.equals(y)) pass();
          else fail(x + " not equal to " + y);}
     public static void main(String[] args) throws Throwable {
          try {realMain(args);} catch (Throwable t) {unexpected(t);}
          System.out.println("\nPassed = " + passed + " failed = " + failed);
          if (failed > 0) throw new AssertionError("Some tests failed");}
}
