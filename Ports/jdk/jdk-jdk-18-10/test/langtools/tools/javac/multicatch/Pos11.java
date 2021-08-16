/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8013163
 * @author  sogoel
 * @summary Test multiple nested multi-catch blocks with Exception hierarchies
 * @run main Pos11
 */

/*
 * For this test, exception hierarchy used:
 *
 *           Throwable
 *             /   \
 *      Exception  Error
 *       |    |      |
 *       A    B      D
 *            |
 *            C
 *            |
 *            E
 * As an exception is thrown within a nested try-catch block, outer catch blocks
 * will catch an exception or its child exceptions, so the same exception can
 * be caught and rethrown multiple times.
 */

public class Pos11 {

    public static String results = "";
    public static String sExpected = "-AB:A-AB:B-CD:C-AB:C-CD:D-Throwable:D-CD:E" +
            "-AB:E-Exception:Exception-Throwable:Exception";

    enum TestExceptions {
        A("A"),
        B("B"),
        C("C"),
        D("D"),
        E("E"),
        U("U");

        String exType;
        TestExceptions(String type) {
            this.exType = type;
        }
    }

    public static void main(String... args) {
        Pos11 pos11 = new Pos11();
        for(TestExceptions t : TestExceptions.values()) {
            pos11.rethrower(t.exType);
        }
        if (results.compareTo(sExpected) != 0)
            throw new RuntimeException("FAIL: final strings did not match:\n"
                    + results + "!=\n" + sExpected);
        System.out.println("PASS");
    }

    void rethrower(String T) {
        try { /* try1 */
            try { /* try2 */
                try { /* try3 */
                    try { /* try4 */
                        switch (T) {
                        case "A":
                            throw new A();
                        case "B":
                            throw new B();
                        case "C":
                            throw new C();
                        case "D":
                            throw new D();
                        case "E":
                            throw new E();
                        default:
                            throw new Exception(
                                    new Throwable());
                        }
                    } catch ( final C|D cd) {
                        results=results.concat("-CD:" + cd.getClass().getSimpleName());
                        throw cd;
                    }
                } catch (final A|B ab) {
                    results=results.concat("-AB:" + ab.getClass().getSimpleName());
                }
            } catch (final Exception e ) {
                results=results.concat("-Exception:" + e.getClass().getSimpleName());
                throw e;
            }
        } catch (Throwable t) {
            results=results.concat("-Throwable:" + t.getClass().getSimpleName());
        }
    }

    // Test Exception
    static class A extends Exception {}

    // Test Exception
    static class B extends Exception {}

    // Test Exception
    static class C extends B {}

    // Not a descendant of Exception
    static class D extends Error {}

    // Test Exception
    static class E extends C {}

}

