/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8034275
 * @summary [JDK 8u40] Test interface initialization order
 * @run main TestInterfaceOrder
 */

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class TestInterfaceOrder {
  static List<Class<?>> cInitOrder = new ArrayList<>();

  public static void main(java.lang.String[] args) {
    //Trigger initialization
    C c = new C();

    List<Class<?>> expectedCInitOrder = Arrays.asList(I.class, J.class, A.class, K.class, B.class, L.class, C.class);
    if (!cInitOrder.equals(expectedCInitOrder)) {
      throw new RuntimeException(String.format("Class initialization order %s not equal to expected order %s", cInitOrder, expectedCInitOrder));
    }
  }

  interface I {
    boolean v = TestInterfaceOrder.out(I.class);
   default void i() {}
  }

  interface J extends I {
    boolean v = TestInterfaceOrder.out(J.class);
    default void j() {}
  }

  static class A implements J {
    static boolean v = TestInterfaceOrder.out(A.class);
  }

  interface K extends I {
    boolean v = TestInterfaceOrder.out(K.class);
    default void k() {}
  }

  static class B extends A implements K {
    static boolean v = TestInterfaceOrder.out(B.class);
  }

  interface L  {
    boolean v = TestInterfaceOrder.out(L.class);
    default void l() {}
  }

  static class C extends B implements L {
    static boolean v = TestInterfaceOrder.out(C.class);
  }


   static boolean out(Class c) {
       System.out.println("#: initializing " + c.getName());
       cInitOrder.add(c);
       return true;
   }

}
