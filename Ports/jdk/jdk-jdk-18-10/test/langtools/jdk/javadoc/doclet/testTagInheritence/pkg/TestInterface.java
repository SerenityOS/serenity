/*
 * Copyright (c) 2001, 2002, Oracle and/or its affiliates. All rights reserved.
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

public interface TestInterface {

  /**
   * Test 11 passes.
   * @param p1 Test 12 passes.
   * @param p2 Test 13 passes.
   * @return a string.
   * @throws java.io.IOException Test 14 passes.
   * @throws java.lang.NullPointerException Test 15 passes.
   * @see java.lang.String
   * @see java.util.Vector
   */
  public String testInterface_method1(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException;

  /**
   * Test 16 passes.
   * @param p1 Test 17 passes.
   * @param p2 Test 18 passes.
   * @return a string.
   * @throws java.io.IOException Test 19 passes.
   * @throws java.lang.NullPointerException Test 20 passes.
   * @see java.lang.String
   * @see java.util.Vector
   */
  public String testInterface_method2(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException;

}
