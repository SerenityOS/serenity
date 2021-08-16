/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test that private Lookup works for both nestmate-enabled classes
 * and legacy classes
 * @compile TestPrivateLookup.java
 * @run main TestPrivateLookup
 * @compile -source 10 -target 10 TestPrivateLookup.java
 * @run main TestPrivateLookup noNestmates
 */

// Need the explicit first @compile above so that jtreg forces recompilation
// with latest version. Otherwise compile-on-demand sees the JDK 10 version
// and assumes it is up to date and then runs the test using that version -
// which fails.

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class TestPrivateLookup {

    static boolean compiledForNestmates;

    static class C {
        static class D {
            private void m() { }
        }

        static void test() throws Throwable {
            MethodType M_T = MethodType.methodType(void.class);
            // Direct lookup from C
            Lookup l = lookup();
            try {
                MethodHandle mh = l.findVirtual(D.class, "m", M_T);
                if (compiledForNestmates) {
                    System.out.println("Lookup of D.m from C succeeded as expected with nestmates");
                }
                else {
                    throw new Error("Unexpected success when not compiled for nestmates!");
                }
            }
            catch (IllegalAccessException iae) {
                if (!compiledForNestmates) {
                    System.out.println("Lookup of D.m from C failed as expected without nestmates");
                }
                else {
                    throw new Error("Unexpected failure with nestmates", iae);
                }
            }
            // switch lookup class to D
            l = l.in(D.class);
            try {
                MethodHandle mh = l.findVirtual(D.class, "m", M_T);
                System.out.println("Lookup of D.m from D succeeded as expected" +
                                   " with" + (compiledForNestmates ? "" : "out") +
                                   " nestmates");
            }
            catch (IllegalAccessException iae) {
                throw new Error("Lookup of D.m from D failed", iae);
            }
        }
    }

    public static void main(String[] args) throws Throwable {

        // If there's no nesthost attribute A.getNestHost() == A
        compiledForNestmates = C.D.class.getNestHost() == TestPrivateLookup.class;
        // sanity check
        boolean expectingNestmates = args.length == 0;

        if (compiledForNestmates && !expectingNestmates) {
            throw new Error("Test is being run incorrectly: " +
                            "nestmates are being used but not expected");
        }
        if (expectingNestmates && !compiledForNestmates) {
            throw new Error("Test is being run incorrectly: " +
                            "nestmates are expected but not being used");
        }
        System.out.println("Testing with" + (expectingNestmates ? "" : "out") +
                           " nestmates");

        C.test();
    }
}
