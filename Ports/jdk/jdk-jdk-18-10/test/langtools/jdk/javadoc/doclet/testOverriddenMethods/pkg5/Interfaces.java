/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package pkg5;

public class Interfaces {
     public interface A {

         /** field f in A */
         public int f = 0;

         public static String QUOTE = "Winter is coming";

         /** a documented static method */
         public static void msd() {}

         /* An undocumented static method */
         public static void msn() {}

         /** A property in parent */
         DoubleProperty rate = null;
         public void setRate(double l);
         public double getRate();
         public DoubleProperty rateProperty();
         // A support class
         public interface DoubleProperty {}

         /** AA in A */
         public interface AA {}

         /** m0 in A */
         public void m0();

         /** m1 in A */
         public void m1();

         /** m2 in A */
         public void m2();

         /** m3 in A */
         public void m3();
     }

     public interface B extends A {
         // No comment
         public void m0();

         /** m1 in B */
         public void m1();

         /** {@inheritDoc} */
         public void m2();

         /** @throws Exception e */
         public void m3() throws Exception;

         /** n in B */
         public void n();
     }

     public interface C extends A, B  {
         /** m in C */
         public void m();

         /** o in C */
         public void o();
     }

    /**
     * The child of all children.
     *
     * Start of links <p>
     * {@link m0},
     * {@link m1},
     * {@link m2},
     * {@link m3},
     * {@link m},
     * {@link n},
     * {@link o},
     * End of links
     *
     * @see #m0()
     * @see #m1()
     * @see #m2()
     * @see #m3()
     * @see #m
     * @see #n
     * @see #o
     */
    public interface D extends A, B, C {
         /** m in D */
         public void m();

         /** n in D */
         public void n();

         // no comment
         public void o();
     }
 }
