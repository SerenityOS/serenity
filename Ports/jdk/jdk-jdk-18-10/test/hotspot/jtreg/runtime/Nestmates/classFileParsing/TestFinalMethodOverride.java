/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171
 * @summary Test nestmate checks are no longer used when check_final_method_override is executed during parsing
 * @run main TestFinalMethodOverride
 */

// The check_final_method_override function in ClassfileParser uses an
// accessability check to see if the subclass method overrides a same-named
// superclass method. This would result in a nestmate access check if the
// super class method is private, which in turn could lead to classloading
// and possibly exceptions and cause havoc in the classfile parsing process.
// To fix that we added a check for whether the super class method is private,
// and if so, we skip the override check as by definition you can't override
// a private method.
//
// This test simply sets up the case where a public subclass method has the
// same signature as a private superclass method - the case we now skip when
// doing check_final_method_override. The test should trivially complete
// normally.

public class TestFinalMethodOverride {

  public static class Super {
    private final void theMethod() {}
  }

  public static class Inner extends Super {
    // define our own theMethod
    public void theMethod() {}
  }

  public static void main(String[] args) {
    Inner i = new Inner();
  }
}

