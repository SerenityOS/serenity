/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4047816
 * @summary certain inheritance patterns involving methods defined in
 *          Object used to cause spurious error messages.
 * @author turnidge
 *
 * @compile CloneableProblem.java
 */

interface A extends Cloneable
{
  public Object clone() throws CloneNotSupportedException;
}
interface B extends A
{ }
interface C extends A
{ }
interface D extends B, C
{ }

public class CloneableProblem implements D
{
  private int i;
  public CloneableProblem(int i)
  {
    this.i = i;
  }
  public Object clone()
  {
    CloneableProblem theCloneableProblem = null;
    try
      {
        theCloneableProblem = (CloneableProblem) super.clone();
        theCloneableProblem.i = i;
      }
    catch (CloneNotSupportedException cnse)
      { }
    return theCloneableProblem;
  }
  public static void main(String argv[])
  {
    try
      {
        A a0 = new CloneableProblem(0);
        A a1 = (A) a0.clone();
        B b0 = new CloneableProblem(0);
        B b1 = (B) b0.clone();
        C c0 = new CloneableProblem(0);
        C c1 = (C) c0.clone();
        D d0 = new CloneableProblem(0);
        D d1 = (D) d0.clone();
      }
    catch (CloneNotSupportedException cnse)
      { }
      }
}
