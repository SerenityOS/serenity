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

/*
 * @test
 * @bug 8022053
 * @summary 8022053: javac generates unverifiable initializer for nested subclass of local class
 * @run main UnverifiableInitForNestedLocalClassTest
 */

public class UnverifiableInitForNestedLocalClassTest {

    public static void main(final String[] args) {
        test("test");
    }

    static void test(final String arg) {
        final String inlined = " inlined ";
        class LocalClass {
            String m() {
                return "LocalClass " + arg + inlined;
            }

            class SubClass extends LocalClass {
                @Override
                String m() {
                    return "SubClass " + arg + inlined;
                }
            }

            class SubSubClass extends SubClass {
                @Override
                String m() {
                    return "SubSubClass " + arg + inlined;
                }
            }

            class AnotherLocal {
                class AnotherSub extends LocalClass {
                    @Override
                    String m() {
                        return "AnotherSub " + arg + inlined;
                    }
                }
            }
        }
        System.out.println(new LocalClass().m());
        System.out.println(new LocalClass().new SubClass().m());
        System.out.println(new LocalClass().new SubSubClass().m());
        System.out.println(new LocalClass().new AnotherLocal().new AnotherSub().m());
    }

}
