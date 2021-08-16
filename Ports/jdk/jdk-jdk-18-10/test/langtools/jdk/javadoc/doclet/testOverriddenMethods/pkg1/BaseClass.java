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

public class BaseClass {

  /*************************************************
   * A public method that can be overridden.
   *
   */
  public void publicMethod() {}


  /*************************************************
   * A package private method that can only
   * be overridden by sub classes in the same package.
   *
   */
  void packagePrivateMethod() {}

  /*************************************************
   * A private that cannot be overridden.
   *
   */
  private void privateMethod() {}

  /**
   * These comments will be copied to the overridden method.
   */
  public void overriddenMethodWithDocsToCopy() {}

  /**
   * @deprecated func1 deprecated
   */
  @Deprecated
  public void func1() {}

  /**
   * @deprecated func2 deprecated
   */
  @Deprecated
  public void func2() {}

  /**
   * @deprecated func3 deprecated
   */
  @Deprecated
  public void func3() {}

}
