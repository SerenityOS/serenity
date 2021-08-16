/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4416605
 * @summary Incorrect order for initializers in nested class
 * @author gafter
 *
 * @compile Closure5.java
 * @run main Closure5
 */

// note that this test case was derived from 4466029, because it
// also checks other features of -target 1.4 simultaneously.
class A {
  int i = 12;
  abstract class B {
    { foo(); }
    abstract void foo();
  }
}
public class Closure5 extends A {
  int i;
  public static void main(String[] args) {
    new Closure5().new D();
  }
  class D extends B {
    int i;
    void foo() {
      if (Closure5.super.i != 12) throw new Error("4416605");
    }
  }
}
