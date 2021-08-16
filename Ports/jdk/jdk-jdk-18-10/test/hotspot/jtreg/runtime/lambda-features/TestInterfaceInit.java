/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8098557
 * @summary [JDK 8u40] Test interface init: only for interfaces declaring default methods, when subclass inits
 * @run main TestInterfaceInit
 */
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

public class TestInterfaceInit {

   static List<Class<?>> cInitOrder = new ArrayList<>();

   // Declares a default method and initializes
   interface I {
       boolean v = TestInterfaceInit.out(I.class);
        default void ix() {}
   }

   // Declares a default method and initializes
   interface J extends I {
       boolean v = TestInterfaceInit.out(J.class);
       default void jx() {}
   }
   // No default method, has an abstract method, does not initialize
   interface JN extends J {
       boolean v = TestInterfaceInit.out(JN.class);
       public abstract void jnx();
   }

   // Declares a default method and initializes
   interface K extends I {
       boolean v = TestInterfaceInit.out(K.class);
        default void kx() {}
   }

   // No default method, has a static method, does not initialize
   interface KN extends K {
       boolean v = TestInterfaceInit.out(KN.class);
       static void knx() {}
   }

   interface L extends JN, KN {
       boolean v = TestInterfaceInit.out(L.class);
        default void lx() {}
   }

   static class ChildClass implements JN, KN {
       boolean v = TestInterfaceInit.out(ChildClass.class);
       public void jnx() {}
   }

   public static void main(String[] args) {
       // Trigger initialization
       boolean v = L.v;

       List<Class<?>> expectedCInitOrder = Arrays.asList(L.class);
       if (!cInitOrder.equals(expectedCInitOrder)) {
         throw new RuntimeException(String.format("Class initialization array %s not equal to expected array %s", cInitOrder, expectedCInitOrder));
       }

       ChildClass myC = new ChildClass();
       boolean w = myC.v;

       expectedCInitOrder = Arrays.asList(L.class,I.class,J.class,K.class,ChildClass.class);
       if (!cInitOrder.equals(expectedCInitOrder)) {
         throw new RuntimeException(String.format("Class initialization array %s not equal to expected array %s", cInitOrder, expectedCInitOrder));
       }

   }

   static boolean out(Class c) {
       System.out.println("#: initializing " + c.getName());
       cInitOrder.add(c);
       return true;
   }

}
