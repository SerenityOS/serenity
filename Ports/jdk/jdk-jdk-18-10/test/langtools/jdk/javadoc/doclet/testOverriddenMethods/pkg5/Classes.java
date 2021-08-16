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

public class Classes {

   public static class GP {
      /** m0 in grand parent */
      public void m0() {}

      /** m7 in grand parent */
      public void m7() {}
   }

    public static class P<K, V> extends GP {

       /** A nested class in parent */
       public class PN<K, V>{}

       /** A property in parent */
       private DoubleProperty rate;
       public final void setRate(double l){}
       public final double getRate(){return 1;}
       public DoubleProperty rateProperty() {return null;}

        /** A ctor in parent */
       public P() {}

       /**
        * A ctor in parent.
        * @param s string
        */
       public P(String s) {}

       /** field0 in parent */
       public int field0;

       /** field1 in parent */
       public int field1;


       // m0 in parent
       public void m0() {}

       /** m1 in parent */
       public void m1() {}

       /** m2 in parent */
       public void m2() {}

       /** m3 in parent */
       public void m3() {}

       /** m4 in parent
           @param k a key
           @param v a value
        */
       public void m4(K k, V v) {}

       // No comment
       public void m5() {}

       // No comment
       public void m6() {}

        /** {@inheritDoc} */
        public void m7() {}

    }

    public static class C extends P {

       public C(String s) {}

       public int field1;

       /** A modified method */
       public void m1() {}

       /** {@inheritDoc} */
       public void m2() {}

       // No comment method
       public void m3() {}

       public void m4(String k, String v) {}

       // Do something else than the parent
       public void m5() {}

       /** A test of links to the methods in this class. <p>
        * {@link m0},
        * {@link m1},
        * {@link m2},
        * {@link m3},
        * {@link m4},
        * {@link m5},
        * {@link m6},
        * {@link m7},
        * End of links
        *
        * @see #m0()
        * @see #m1()
        * @see #m2()
        * @see #m3()
        * @see #m4(String k, String v)
        * @see #m5()
        * @see #m6()
        * @see #m7()
        */
       public void m6() {}

        /** m7 in Child. */
        public void m7() {}
    }

    /** Tickle this {@link TestEnum#doSomething()} */
    public class DoubleProperty {}
}
