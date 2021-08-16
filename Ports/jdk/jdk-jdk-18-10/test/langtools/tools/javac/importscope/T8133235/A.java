/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8133235
 * @summary Ensuring order of inputs does not affect compilability of the sources
 * @compile A.java B.java C.java D.java
 * @compile A.java B.java D.java C.java
 * @compile A.java C.java B.java D.java
 * @compile A.java C.java D.java B.java
 * @compile A.java D.java B.java C.java
 * @compile A.java D.java C.java B.java
 * @compile D.java A.java B.java C.java
 * @compile D.java A.java C.java B.java
 * @compile D.java B.java A.java C.java
 * @compile D.java B.java C.java A.java
 * @compile D.java C.java B.java A.java
 * @compile D.java C.java A.java B.java
 */
package pkg;

public class A {
  public interface One {
      public interface N2 {
          public class N3 { }
      }

      public class Foo extends D {}
  }
}
