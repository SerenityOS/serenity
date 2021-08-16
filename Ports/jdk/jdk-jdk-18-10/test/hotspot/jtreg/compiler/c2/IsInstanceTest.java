/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6894807
 * @summary No ClassCastException for HashAttributeSet constructors if run with -Xcomp
 *
 * @run main compiler.c2.IsInstanceTest
*/

package compiler.c2;

public class IsInstanceTest {

    public static void main(String[] args) {
        BaseInterface baseInterfaceImpl = new BaseInterfaceImpl();
        for (int i = 0; i < 100000; i++) {
            if (isInstanceOf(baseInterfaceImpl, ExtendedInterface.class)) {
                throw new AssertionError("Failed at index:" + i);
            }
        }
        System.out.println("Done!");
    }

    public static boolean isInstanceOf(BaseInterface baseInterfaceImpl, Class... baseInterfaceClasses) {
        for (Class baseInterfaceClass : baseInterfaceClasses) {
            if (baseInterfaceClass.isInstance(baseInterfaceImpl)) {
                return true;
            }
        }
        return false;
    }

    private interface BaseInterface {
    }

    private interface ExtendedInterface extends BaseInterface {
    }

    private static class BaseInterfaceImpl implements BaseInterface {
    }
}
