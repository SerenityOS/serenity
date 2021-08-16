/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4157921
 * @summary Test subclasses of BasicPermission to make sure different
 * subclasses don't equal or imply each other.
 */

import java.security.Permission;
import java.security.BasicPermission;

class A extends BasicPermission {
    public A(String name) { super(name); }
}

class B extends BasicPermission {
    public B(String name) { super(name); }
}

public class EqualsImplies {

    public static void main(String[]args) throws Exception {

      Permission p1 = new A("foo");
      Permission p2 = new B("foo");

      if (p1.implies(p2) || p2.implies(p1) || p1.equals(p2)) {
          throw new Exception("Test failed");
      }

      // make sure permissions imply and equal themselves
      if (! (p1.implies(p1) && p1.equals(p1))) {
          throw new Exception("Test failed");
      }

    }
}
