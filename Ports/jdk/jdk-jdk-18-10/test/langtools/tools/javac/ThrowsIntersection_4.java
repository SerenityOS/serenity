/*
 * Copyright (c) 2000, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4042259
 * @summary Check that a class can inherit multiple methods with conflicting throws clauses.
 * @author maddox
 *
 * @run compile/fail ThrowsIntersection_4.java
 */

package ThrowsIntersection_4;

// Note:  This is the test that actually failed for 4042259.  The others are for completeness.

class Ex1 extends Exception {}
class Ex2 extends Exception {}

interface a {
  int m1() throws Ex1;
}

interface b {
  int m1() throws Ex2;
}

// Should fail regardless of order in which superinterfaces appear.
abstract class c implements a, b {}
//abstract class c implements b, a {}

class d extends c  {
  public int m1() throws Ex1 {
    return 1;
  }
}
