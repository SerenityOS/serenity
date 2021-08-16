/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

public class CpAttr1 {
  public static void main(String args[]) {
    System.out.println("2"); CpAttr2.doit(); // Only the version of this class defined in CpAttr2.java will not throw exception.
    System.out.println("3"); CpAttr3.doit(); // Only the version of this class defined in CpAttr3.java will not throw exception.
    System.out.println("4"); CpAttr4.doit(); // Only the version of this class defined in CpAttr4.java will not throw exception.
    System.out.println("5"); CpAttr5.doit(); // Only the version of this class defined in CpAttr5.java will not throw exception.
    System.out.println("Test passed");
  }
}

class CpAttr2 { static void doit() {throw new RuntimeException("");} }
class CpAttr3 { static void doit() {throw new RuntimeException("");} }
class CpAttr4 { static void doit() {throw new RuntimeException("");} }
class CpAttr5 { static void doit() {throw new RuntimeException("");} }
