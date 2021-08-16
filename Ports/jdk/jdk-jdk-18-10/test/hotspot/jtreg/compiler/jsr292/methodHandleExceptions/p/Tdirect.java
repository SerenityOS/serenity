/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package p;

/**
 * Invokes I.m directly using invokeInterface bytecodes.
 */
public class Tdirect {
     public static int test(I i) {
         int accum = 0;
         for (int j = 0; j < 100000; j++) {
             accum += i.m();
         }
        return accum;
    }

     public static int test(I ii, byte b, char c, short s, int i, long l,
             Object o1, Object o2, Object o3, Object o4, Object o5, Object o6) {
         int accum = 0;
         for (int j = 0; j < 100000; j++) {
           accum += ii.m(b,c,s,i,l,o1,o2,o3,o4,o5,o6);
         }
         return accum;
     }
}
