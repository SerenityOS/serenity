/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166363
 * @summary Method with reordered type parameter bounds compiles with Override annotation but does not actually override superclass method.
 * @run main ReorderedBoundsTest
 */

import java.io.Serializable;

public class ReorderedBoundsTest {
  public static class Parent {
    public <T extends Appendable & Serializable> String printClassName(T t) {
      return "Parent";
    }
  }

  public static class OrderedChild extends Parent {
    @Override
    public <T extends Appendable & Serializable> String printClassName(T t) {
      return "OrderedChild";
    }
  }

  public static class ReorderedChild extends Parent {
    @Override
    public <T extends Serializable & Appendable> String printClassName(T t) {
      return "ReorderedChild";
    }
  }

  public static void main(String[] args) {

    String s;
    Parent p = new OrderedChild();
    if (!(s = p.printClassName(new StringBuilder())).equals("OrderedChild"))
        throw new AssertionError("Bad output: " + s);

    p = new ReorderedChild();
    if (!(s = p.printClassName(new StringBuilder())).equals("ReorderedChild"))
        throw new AssertionError("Bad output: " + s);
  }
}