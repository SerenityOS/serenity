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

import java.lang.*;
import java.io.*;

public class TestTagInheritence extends TestAbstractClass implements TestInterface{

  //This method below tests tag inheritence from a class.


  public String testAbstractClass_method1(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException {
      return null;
  }

  /**
   * This method tests @inheritDoc with a class.  Here is the inherited comment:<br>
   * {@inheritDoc}
   * @param p1 {@inheritDoc}
   * @param p2 {@inheritDoc}
   * @return {@inheritDoc}
   * @throws java.io.IOException {@inheritDoc}
   * @throws java.lang.NullPointerException {@inheritDoc}
   */
  public String testAbstractClass_method2(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException {
      return null;
  }

  public String testInterface_method1(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException
  {
      return null;
  }

  /**
   * This method tests @inheritDoc with an inteface.  Here is the inherited comment:<br>
   * {@inheritDoc}
   * @param p1 {@inheritDoc}
   * @param p2 {@inheritDoc}
   * @return {@inheritDoc}
   * @throws java.io.IOException {@inheritDoc}
   * @throws java.lang.NullPointerException {@inheritDoc}
   */
  public String testInterface_method2(int p1, int p2) throws java.io.IOException,
java.lang.NullPointerException {
      return null;
  }

    /**
     * Here is the documentation that I should inherit from
     * the interface implemented by my superclass.
     * {@inheritDoc}
     */
    public void methodInTestInterfaceForAbstractClass() {}

    /**
     * {@inheritDoc}
     */
    public void testSuperSuperMethod() {}

    /**
     * Test inheritDoc warning message:
     * {@inheritDoc}
     */
    public void testBadInheritDocTag () {}

    /**
     * {@inheritDoc}
     * @param <X> {@inheritDoc}
     * @param <Y> {@inheritDoc}
     * @param p1 {@inheritDoc}
     * @param p2 {@inheritDoc}
     * @return {@inheritDoc}
     * @throws java.io.IOException {@inheritDoc}
     * @throws java.lang.NullPointerException {@inheritDoc}
     */
    public <X,Y> String testSuperSuperMethod(int p1, int p2) {
        return null;
    }

    public <X,Y> String testSuperSuperMethod2(int p1, int p2)
    throws java.io.IOException, java.lang.NullPointerException {
        return null;
    }

}
