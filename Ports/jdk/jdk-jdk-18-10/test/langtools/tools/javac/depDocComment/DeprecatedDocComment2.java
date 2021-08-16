/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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

// WARNING: this file much be compiled first before DeprecatedDocCommentTest.java
// in test/tools/javac/ is compiled. This is because the compiler *does not*
// print deprecation warnings for a file currently being compiled.

// If this file fails to compile, then the test has failed.  The test does not
// need to be run.

//package depDocComment ;

public class DeprecatedDocComment2 {

  public static void main(String argv[]) {

    // should just skip over this one
    System.out.println("Hello World");

    /* and this one too */
    System.out.println("Hello World");

  }

    /**
     * @deprecated The compiler should print out deprecation warning for this
   * function
   */
    public static void deprecatedTest1() {
    System.out.println("1");
  }

  /*
   * @deprecated The compiler should not print deprecation warning since this
   * is not a legal docComment
   */
  public static void deprecatedTest2() {
    System.out.println("1");
  }

  /*
   * @deprecated Nor this one */
  public static void deprecatedTest3() {
    System.out.println("1");
  }

  /* @deprecated Nor this */
  public static void deprecatedTest4() {
    System.out.println("1");
  }

  /** @deprecated But it should for this */
  public static void deprecatedTest5() {
    System.out.println("1");
  }

  /**@deprecated But it should for this*/
  public static void deprecatedTest6() {
    System.out.println("1");
  }

    /*
     @deprecated But not for this
     */
    public static void deprecatedTest7() {
        System.out.println("1");
    }



    /**
     * not at the beginning of line @deprecated But not for this
     */
    public static void deprecatedTest8() {
        System.out.println("1");
    }

}
