/*
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.asm;

/** <P> Register is an abstraction over machine registers. </P>

    <P> FIXME: should align constants with underlying VM code </P> */

public abstract class Register extends ImmediateOrRegister {
  /** Corresponds to the machine register code. -1 stands for invalid
      register (initial value). */
  protected int number;

  public Register() {
    number = -1;
  }

  public Register(int number) {
    this.number = number;
  }

  /** Must be overridden by subclass to indicate number of available
      registers on this platform */
  public abstract int getNumberOfRegisters();

  public boolean isValid() {
    return ((0 <= number) && (number <= getNumberOfRegisters()));
  }

  public int getNumber() {
    return number;
  }

  public boolean equals(Object x) {
    if (x == null) {
      return false;
    }

    if (!getClass().equals(x.getClass())) {
      return false;
    }

    Register reg = (Register) x;

    return (reg.getNumber() == getNumber());
  }

  public int hashCode() {
    return number;
  }

  public boolean isRegister() {
    return true;
  }

  public abstract boolean isStackPointer();
  public abstract boolean isFramePointer();
  public abstract boolean isFloat();
}
