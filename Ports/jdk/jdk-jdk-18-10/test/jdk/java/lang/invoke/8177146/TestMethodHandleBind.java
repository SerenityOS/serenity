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

/* @test
 * @bug 8177146
 * @run testng/othervm TestMethodHandleBind
 */

import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static java.lang.invoke.MethodHandles.lookup;

import static org.testng.Assert.*;

public class TestMethodHandleBind extends pkg.A {
    static class B extends TestMethodHandleBind {}

    @Test
    public void testInstanceOfCallerClass() throws Throwable {
        MethodHandle bound = lookup().bind(new TestMethodHandleBind() , "m1", MethodType.methodType(String.class));
        String x = (String)bound.invoke();
        assertEquals(x, this.getClass().getSimpleName());
    }

    @Test
    public void testInstanceOfCallerSubclass() throws Throwable {
        MethodHandle bound = lookup().bind(new B() , "m1", MethodType.methodType(String.class));
        // MethodHandle bound = lookup().findVirtual(B.class,  "m1", MethodType.methodType(String.class)).bindTo(new B());
        String x = (String)bound.invoke();
        assertEquals(x, "B");
    }

    @Test
    public void testInstanceOfReceiverClass() throws Throwable {
        try {
            MethodHandle bound = lookup().bind(new pkg.A() , "m1", MethodType.methodType(String.class));
            bound.invoke();
            fail("IllegalAccessException expected");
        } catch (IllegalAccessException e) {
        }
    }

    @Test
    public void testPublicMethod() throws Throwable {
        MethodHandle bound = lookup().bind(new pkg.A() , "m2", MethodType.methodType(String.class));
        String x = (String)bound.invoke();
        assertEquals(x, "A");
    }

    @Test
    public void testPublicMethod2() throws Throwable {
        MethodHandle bound = lookup().bind(new TestMethodHandleBind(), "m2", MethodType.methodType(String.class));
        String x = (String)bound.invoke();
        assertEquals(x, this.getClass().getSimpleName());
    }

    @Test
    public void testInstanceOfCallerClassVarargs() throws Throwable {
        MethodHandle bound = lookup().bind(new TestMethodHandleBind() , "m3", MethodType.methodType(String.class, String[].class));
        String x = (String)bound.invoke("a", "b", "c");
        assertEquals(x, this.getClass().getSimpleName() + "abc");
    }

    @Test
    public void testInstanceOfReceiverClassVarargs() throws Throwable {
        try {
            MethodHandle bound = lookup().bind(new pkg.A(), "m3", MethodType.methodType(String.class, String[].class));
            bound.invoke();
            fail("IllegalAccessException expected");
        } catch (IllegalAccessException e) {
        }
    }
}
