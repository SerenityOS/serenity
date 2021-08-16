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

/*
 * Note: verifier_test_tmp.jar will be processed by ../AppCDSVerifierTest.java to
 * create verifier_test.jar, which contains invalid versions of UnverifiableBase
 * and UnverifiableIntf.
 */

public class VerifierTest0 {
  public static void main(String args[]) {
    boolean good = true;
    good &= mustBeInvalid("VerifierTestA");
    good &= mustBeInvalid("VerifierTestB");
    good &= mustBeInvalid("VerifierTestC");
    good &= mustBeInvalid("VerifierTestD");
    good &= mustBeInvalid("VerifierTestE");
    if (!good) {
      System.out.println("VerifierTest0 failed");
      System.exit(1);
    }
  }

  /** @return false means error */
  static boolean mustBeInvalid(String className) {
    System.out.println("Testing: " + className);
    try {
      Class.forName(className);
      System.out.println("ERROR: class " + className + " was loaded unexpectedly.");
      return false;
    } catch (Throwable t) {
      System.out.println("Expected exception:");
      t.printStackTrace();
      return true;
    }
  }
}

class UnverifiableBase {
  static final VerifierTest0 x = new VerifierTest0(); // <- this static initializer will be made unverifiable by type mismatch
}

interface UnverifiableIntf {
  static final VerifierTest0 x = new VerifierTest0(); // <- this static initializer will be made unverifiable by type mismatch
}

interface UnverifiableIntfSub extends UnverifiableIntf {}

class VerifierTestA extends    UnverifiableBase {}
class VerifierTestB extends    VerifierTestA {}
class VerifierTestC implements UnverifiableIntf {}
class VerifierTestD extends    VerifierTestC {}
class VerifierTestE implements UnverifiableIntfSub {}

