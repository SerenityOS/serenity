/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

// Create a class to load inside the loader instance, that will load
// A through a constant pool reference.
// Class A extends B extends A
class CP1 {
  void foo() throws Exception {
      ThreadPrint.println("CP1.foo()");
      try {
          Class<?> a = A.class;
          Object obj = a.getConstructor().newInstance();
          throw new RuntimeException("Should throw CCE here");
      } catch (Throwable e) {
          ThreadPrint.println("Exception is caught: " + e);
          if (!(e instanceof java.lang.ClassCircularityError)) {
              throw new RuntimeException("Unexpected exception");
          }
      }
  }
}

// This class has a constant pool reference to B which will also get CCE, but
// starting at B.
class CP2 {
  void foo() throws Exception {
      ThreadPrint.println("CP2.foo()");
      try {
          Class<?> a = B.class;
          Object obj = a.getConstructor().newInstance();
          throw new RuntimeException("Should throw CCE here");
      } catch (Throwable e) {
          ThreadPrint.println("Exception is caught: " + e);
          if (!(e instanceof java.lang.ClassCircularityError)) {
              throw new RuntimeException("Unexpected exception");
          }
      }
  }
}

public class ClassInLoader {
  private static boolean first = true;
  public ClassInLoader() throws Exception {
    ThreadPrint.println("ClassInLoader");
    if (first) {
        first = false;
        CP1 c1 = new CP1();
        c1.foo();
    } else {
        CP2 c2 = new CP2();
        c2.foo();
    }
  }
}
