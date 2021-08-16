/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8058199
 * @summary Code generation problem with javac skipping a checkcast instruction
 */
public class T8058199 {

    final static String SYNTHETIC_CAST_TYPE = "[Ljava.lang.String;";

    @SuppressWarnings("unchecked")
    <Z> Z[] makeArr(Z z) { return (Z[])new Object[1]; }

    <U> void check(U u) { }

    void testMethod() {
        test(() -> check(makeArr("")));
    }

    void testNewDiamond() {
        class Check<X> {
            Check(X x) { }
        }
        test(()-> new Check<>(makeArr("")));
    }

    void testNewGeneric() {
        class Check {
            <Z> Check(Z z) { }
        }
        test(()-> new Check(makeArr("")));
    }

    private void test(Runnable r) {
        try {
            r.run();
            throw new AssertionError("Missing synthetic cast");
        } catch (ClassCastException cce) {
            if (!cce.getMessage().contains(SYNTHETIC_CAST_TYPE)) {
                throw new AssertionError("Bad type in synthetic cast", cce);
            }
        }
    }

    public static void main(String[] args) {
        T8058199 test = new T8058199();
        test.testMethod();
        test.testNewDiamond();
        test.testNewGeneric();
    }
}
