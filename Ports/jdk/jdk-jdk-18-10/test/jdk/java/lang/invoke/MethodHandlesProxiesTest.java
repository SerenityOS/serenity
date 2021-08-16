/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206955 8269351
 * @run testng/othervm -ea -esa test.java.lang.invoke.MethodHandlesProxiesTest
 */

package test.java.lang.invoke;

import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;

import static org.testng.Assert.assertEquals;

public class MethodHandlesProxiesTest {

    public interface A {
        default String a() {
            return "A";
        }
    }

    public interface B {
        default String b() {
            return "B";
        }
    }

    public interface C extends A, B {
        String f();

        default String c() {
            return "C";
        }

        default String concat() {
            return a() + b() + c();
        }
    }

    public interface Override extends C {
        String f();

        default String a() {
            return "OA";
        }

        default String b() {
            return "OB";
        }

        default String c() {
            return "OC";
        }
    }

    @Test
    public static void testDefaultMethods() throws Throwable {
        MethodHandle target = MethodHandles.constant(String.class, "F");
        C proxy = MethodHandleProxies.asInterfaceInstance(C.class, target);

        assertEquals(proxy.f(), "F");
        assertEquals(proxy.a(), "A");
        assertEquals(proxy.b(), "B");
        assertEquals(proxy.c(), "C");
        assertEquals(proxy.concat(), "ABC");
    }

    @Test
    public static void testOverriddenDefaultMethods() throws Throwable {
        MethodHandle target = MethodHandles.constant(String.class, "F");
        Override proxy = MethodHandleProxies.asInterfaceInstance(Override.class, target);

        assertEquals(proxy.a(), "OA");
        assertEquals(proxy.b(), "OB");
        assertEquals(proxy.c(), "OC");
    }

    public sealed interface Intf permits NonSealedInterface {
        String m();
    }

    public non-sealed interface NonSealedInterface extends Intf {
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testSealedInterface() {
        MethodHandle target = MethodHandles.constant(String.class, "Sealed");
        MethodHandleProxies.asInterfaceInstance(Intf.class, target);
    }

    @Test
    public void testNonSealedInterface() {
        MethodHandle target = MethodHandles.constant(String.class, "Non-Sealed");
        NonSealedInterface proxy = MethodHandleProxies.asInterfaceInstance(NonSealedInterface.class, target);
        assertEquals(proxy.m(), "Non-Sealed");
    }
}
