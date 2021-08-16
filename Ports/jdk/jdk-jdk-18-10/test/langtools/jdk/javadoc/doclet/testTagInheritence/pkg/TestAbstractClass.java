/*
 * Copyright (c) 2001, 2003, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.util.zip.*;

public abstract class TestAbstractClass extends TestSuperSuperClass
    implements TestInterfaceForAbstractClass {

  /**
   * Test 1 passes.
   * @param p1 Test 2 passes.
   * @param p2 Test 3 passes.
   * @return a string.
   * @throws java.io.IOException Test 4 passes.
   * @throws java.lang.NullPointerException Test 5 passes.
   * @see java.lang.String
   * @see java.util.Vector
   */
  public String testAbstractClass_method1(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException {
      return null;
  }

  /**
   * Test 6 passes.
   * @param p1 Test 7 passes.
   * @param p2 Test 8 passes.
   * @return a string.
   * @throws java.io.IOException Test 9 passes.
   * @throws java.lang.NullPointerException Test 10 passes
   * @see java.lang.String
   * @see java.util.Vector
   */
  public String testAbstractClass_method2(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException {
      return null;
  }

    /**
     * Test 31 passes.
     * @param <Q> Test 33 passes.
     * @param x2 Test 35 passes.
     * @throws java.io.IOException Test 37 passes.
     * @throws java.util.zip.ZipException Test 39 passes.
     */
    public <P,Q> String testSuperSuperMethod2(int x1, int x2) {
        return null;
    }
}
