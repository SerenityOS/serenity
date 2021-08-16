/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6885584
 * @summary A particular class structure causes large allocation spike for jit
 *
 * @run main/othervm -Xbatch compiler.c2.Test6885584
 */

package compiler.c2;

public class Test6885584 {
   static private int i1;
   static private int i2;
   static private int i3;

    static int limit = Integer.MAX_VALUE - 8;

   public static void main(String args[]) {
       // Run long enough to trigger an OSR
       for(int j = 200000; j != 0; j--) {
       }

       // This must reference a field
       i1 = i2;

       // The resource leak is roughly proportional to this initial value
       for(int k = Integer.MAX_VALUE - 1; k != 0; k--) {
           // Make sure the body does some work
           if(i2 > i3)i1 = k;
           if (k <= limit) break;
       }
   }

}
