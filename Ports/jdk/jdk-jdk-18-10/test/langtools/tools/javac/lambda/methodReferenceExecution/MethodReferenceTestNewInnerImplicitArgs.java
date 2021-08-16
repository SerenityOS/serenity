/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8011591
 * @summary BootstrapMethodError when capturing constructor ref to local classes
 * @run testng MethodReferenceTestNewInnerImplicitArgs
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Test the case that a constructor has implicit parameters added to
 * access local variables and that this constructor is used in a
 * method reference.
 * @author Robert Field
 */

@Test
public class MethodReferenceTestNewInnerImplicitArgs {


    static class S {
        String b;
        S(String s, String s2) { b = s + s2; }
    }

    interface I {
        S m();
    }

    interface I2 {
        S m(int i, int j);
    }

    public static void testConstructorReferenceImplicitParameters() {
        String title = "Hey";
        String a2 = "!!!";
        class MS extends S {
            MS() {
                super(title, a2);
            }
        }

        I result = MS::new;
        assertEquals(result.m().b, "Hey!!!");

        class MS2 extends S {
            MS2(int x, int y) {
                super(title+x, a2+y);
            }
        }

        I2 result2 = MS2::new;
        assertEquals(result2.m(8, 4).b, "Hey8!!!4");
    }
}
