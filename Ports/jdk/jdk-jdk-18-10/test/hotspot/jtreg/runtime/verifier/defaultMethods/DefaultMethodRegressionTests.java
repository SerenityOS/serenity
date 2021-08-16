/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003639
 * @summary defaultMethod resolution and verification
 * @run main DefaultMethodRegressionTests
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This set of classes/interfaces (K/I/C) is specially designed to expose a
 * bug in the JVM where it did not find some overloaded methods in some
 * specific situations. (fixed by hotspot changeset ffb9316fd9ed).
 */
interface K {
    int bbb(Long l);
}

interface I extends K {
    default void aaa() {}
    default void aab() {}
    default void aac() {}

    default int bbb(Integer i) { return 22; }
    default int bbb(Float f) { return 33; }
    default int bbb(Long l) { return 44; }
    default int bbb(Double d) { return 55; }
    default int bbb(String s) { return 66; }

    default void caa() {}
    default void cab() {}
    default void cac() {}
}

class C implements I {}

public class DefaultMethodRegressionTests {
    public static void main(String... args) {
        new DefaultMethodRegressionTests().run(args);
    }
    void run(String... args) {
        testLostOverloadedMethod();
        System.out.println("testLostOverloadedMethod: OK");
        testInferenceVerifier();
        System.out.println("testInferenceVerifier: OK");
    }
    void testLostOverloadedMethod() {
        C c = new C();
        assertEquals(c.bbb(Integer.valueOf(1)), 22);
        assertEquals(c.bbb(Float.valueOf(1.1F)), 33);
        assertEquals(c.bbb(Long.valueOf(1L)), 44);
        assertEquals(c.bbb(Double.valueOf(0.01)), 55);
        assertEquals(c.bbb(new String("")), 66);
    }
    // Test to ensure that the inference verifier accepts older classfiles
    // with classes that implement interfaces with defaults.
    void testInferenceVerifier() {
        // interface I { int m() default { return 99; } }
        byte I_bytes[] = {
            (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe, 0x00, 0x00, 0x00, 0x34,
            0x00, 0x08, 0x07, 0x00, 0x06, 0x07, 0x00, 0x07,
            0x01, 0x00, 0x03, 0x66, 0x6f, 0x6f, 0x01, 0x00,
            0x03, 0x28, 0x29, 0x49, 0x01, 0x00, 0x04, 0x43,
            0x6f, 0x64, 0x65, 0x01, 0x00, 0x01, 0x49, 0x01,
            0x00, 0x10, 0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c,
            0x61, 0x6e, 0x67, 0x2f, 0x4f, 0x62, 0x6a, 0x65,
            0x63, 0x74, 0x06, 0x00, 0x00, 0x01, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x01,
            0x00, 0x03, 0x00, 0x04, 0x00, 0x01, 0x00, 0x05,
            0x00, 0x00, 0x00, 0x0f, 0x00, 0x01, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x03, 0x10, 0x63, (byte)0xac, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00
        };
        // public class C implements I {}  /* -target 1.5 */
        byte C_bytes[] = {
            (byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe, 0x00, 0x00, 0x00, 0x31,
            0x00, 0x0c, 0x0a, 0x00, 0x03, 0x00, 0x08, 0x07,
            0x00, 0x09, 0x07, 0x00, 0x0a, 0x07, 0x00, 0x0b,
            0x01, 0x00, 0x06, 0x3c, 0x69, 0x6e, 0x69, 0x74,
            0x3e, 0x01, 0x00, 0x03, 0x28, 0x29, 0x56, 0x01,
            0x00, 0x04, 0x43, 0x6f, 0x64, 0x65, 0x0c, 0x00,
            0x05, 0x00, 0x06, 0x01, 0x00, 0x01, 0x43, 0x01,
            0x00, 0x10, 0x6a, 0x61, 0x76, 0x61, 0x2f, 0x6c,
            0x61, 0x6e, 0x67, 0x2f, 0x4f, 0x62, 0x6a, 0x65,
            0x63, 0x74, 0x01, 0x00, 0x01, 0x49, 0x00, 0x21,
            0x00, 0x02, 0x00, 0x03, 0x00, 0x01, 0x00, 0x04,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x05,
            0x00, 0x06, 0x00, 0x01, 0x00, 0x07, 0x00, 0x00,
            0x00, 0x11, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x05, 0x2a, (byte)0xb7, 0x00, 0x01, (byte)0xb1, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00
        };

        ClassLoader cl = new ClassLoader() {
            protected Class<?> findClass(String name) {
                if (name.equals("I")) {
                    return defineClass("I", I_bytes, 0, I_bytes.length);
                } else if (name.equals("C")) {
                    return defineClass("C", C_bytes, 0, C_bytes.length);
                } else {
                    return null;
                }
            }
        };
        try {
            Class.forName("C", true, cl);
        } catch (Exception e) {
            // unmodified verifier will throw VerifyError
            throw new RuntimeException(e);
        }
    }
    void assertEquals(Object o1, Object o2) {
        System.out.print("Expected: " + o1);
        System.out.println(", Obtained: " + o2);
        if (!o1.equals(o2)) {
            throw new RuntimeException("got unexpected values");
        }
    }
}
