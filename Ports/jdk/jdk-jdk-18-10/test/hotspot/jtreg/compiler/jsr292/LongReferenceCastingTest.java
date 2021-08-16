/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148752
 * @summary Test correct casting of MH arguments during inlining.
 *
 * @run main compiler.jsr292.LongReferenceCastingTest
 */

package compiler.jsr292;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class LongReferenceCastingTest {
    static final String MY_STRING = "myString";
    static final MethodHandle MH;

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodType mt = MethodType.methodType(String.class, long.class, Object.class, String.class);
            MH = lookup.findVirtual(LongReferenceCastingTest.class, "myMethod", mt);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public String myMethod(long l, Object o, String s) {
        // The long argument occupies two stack slots, causing C2 to treat it as
        // two arguments and casting the fist one two long and the second one to Object.
        // As a result, Object o is casted to String and the o.toString() call is
        // inlined as String::toString(). We fail at runtime because 'o' is not a String.
        return o.toString();
    }

    public String toString() {
        return MY_STRING;
    }

    public static void main(String[] args) throws Exception {
        LongReferenceCastingTest test = new LongReferenceCastingTest();
        try {
            for (int i = 0; i < 20_000; ++i) {
                if (!test.invoke().equals(MY_STRING)) {
                    throw new RuntimeException("Invalid string");
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Test failed", t);
        }
    }

    public String invoke() throws Throwable {
        return (String) MH.invokeExact(this, 0L, (Object)this, MY_STRING);
    }
}
