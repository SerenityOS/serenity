/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package pkg1;

public class SubClass extends BaseClass {

  /*************************************************
   * This method should override the same public
   * method in the base class.
   *
   */
  public void publicMethod() {}


  /*************************************************
   * This method should override the same package
   * private method in the base class because they
   * are in the same package.
   */
  public void packagePrivateMethod() {}

  /*************************************************
   * This method should not override anything because
   * the same method in the base class is private.
   *
   */
  public void privateMethod() {}

  public void overriddenMethodWithDocsToCopy() {}

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public void func1() {}

  /**
   * deprecated with comments
   */
  @Deprecated
  public void func2() {}

  /**
   * {@inheritDoc}
   */
  public void func3() {}

}
