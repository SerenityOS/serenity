/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.lang.invoke;

import org.testng.annotations.Test;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;

import static org.testng.Assert.fail;

/**
 * Ensure that the $deserializeLambda$ method is present when it should be, and absent otherwise
 */

@Test(groups = { "serialization-hostile" })
public class DeserializeMethodTest {
    private void assertDeserializeMethod(Class<?> clazz, boolean expectedPresent) {
        try {
            Method m = clazz.getDeclaredMethod("$deserializeLambda$", SerializedLambda.class);
            if (!expectedPresent)
                fail("Unexpected $deserializeLambda$ in " + clazz);
        }
        catch (NoSuchMethodException e) {
            if (expectedPresent)
                fail("Expected to find $deserializeLambda$ in " + clazz);
        }
    }

    static class Empty {}

    public void testEmptyClass() {
        assertDeserializeMethod(Empty.class, false);
    }

    static class Cap1 {
        void foo() {
            Runnable r = (Runnable & Serializable) () -> { };
        }
    }

    public void testCapturingSerLambda() {
        assertDeserializeMethod(Cap1.class, true);
    }

    static class Cap2 {
        void foo() {
            Runnable r = () -> { };
        }
    }

    public void testCapturingNonSerLambda() {
        assertDeserializeMethod(Cap2.class, false);
    }

    interface Marker { }
    static class Cap3 {
        void foo() {
            Runnable r = (Runnable & Marker) () -> { };
        }
    }

    public void testCapturingNonserIntersectionLambda() {
        assertDeserializeMethod(Cap3.class, false);
    }
}
