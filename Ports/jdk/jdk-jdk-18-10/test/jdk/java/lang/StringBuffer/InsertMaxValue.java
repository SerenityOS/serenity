/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4231107
 * @summary Test Insert method parameter checking
 */

public class InsertMaxValue {

   public static void main (String argv[]) throws Exception {
       StringBuffer sb = new StringBuffer("");
       StringBuffer sb1 = new StringBuffer("Some test StringBuffer");

       try {
           sb.insert(0, new char[5], 1, Integer.MAX_VALUE);
           throw new RuntimeException("Exception expected");
       } catch (StringIndexOutOfBoundsException sobe) {
           // Test passed
       } catch (OutOfMemoryError oome) {
           throw new RuntimeException("Wrong exception thrown.");
       }

       try {
           sb1.insert(2, new char[25], 5, Integer.MAX_VALUE);
           throw new RuntimeException("Exception expected");
       } catch (StringIndexOutOfBoundsException sobe) {
           // Test passed
       } catch (ArrayIndexOutOfBoundsException aioe) {
           throw new RuntimeException("Wrong exception thrown.");
       }
   }
}
