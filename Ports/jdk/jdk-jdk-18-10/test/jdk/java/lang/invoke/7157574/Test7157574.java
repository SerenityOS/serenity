/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
7157574 method handles returned by reflective lookup API sometimes have wrong receiver type

When an inherited non-static field or method is looked up in a class C using Lookup.findVirtual(C...), etc., the JSR 292 API, the first argument of the resulting method handle must be the receiver ('this'), and must be the requested class (or more specific, in the case of findSpecial or a lookup of a protected method).

But currently, if a supertype T defines the looked-up method or field and C inherits it, the returned method handle might have the more specific initial type T.

The relevant javadoc (and 292 spec.) is as follows:
    * The formal parameter {@code this} stands for the self-reference of type {@code C};
    * if it is present, it is always the leading argument to the method handle invocation.
    * (In the case of some {@code protected} members, {@code this} may be
    * restricted in type to the lookup class; see below.)

Because of this bug, all of the assertions fail in the following example:
*/

/* @test
 * @bug 7157574
 * @summary method handles returned by reflective lookup API sometimes have wrong receiver type
 *
 * @run main Test7157574
 */

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;
public class Test7157574 {
    interface Intf { void ig1(); void ig2(); void ig3(); void ig4(); void m1(); }
    abstract static class Super implements Intf { public abstract void m2(); public int f2; }
    abstract static class Sub extends Super { }
    public static void main(String... av) throws Throwable {
        MethodHandle m1 = lookup().findVirtual(Sub.class, "m1", methodType(void.class));
        System.out.println(m1);
        MethodHandle m2 = lookup().findVirtual(Sub.class, "m2", methodType(void.class));
        System.out.println(m2);
        MethodHandle f2 = lookup().findGetter(Sub.class, "f2", int.class);
        System.out.println(f2);
        MethodHandle f2s = lookup().findSetter(Sub.class, "f2", int.class);
        System.out.println(f2s);
        MethodHandle chc = lookup().findVirtual(Sub.class,  "hashCode", methodType(int.class));
        System.out.println(chc);
        MethodHandle ihc = lookup().findVirtual(Intf.class, "hashCode", methodType(int.class));
        System.out.println(ihc);
        assertEquals(Sub.class, m1.type().parameterType(0));
        assertEquals(Sub.class, m2.type().parameterType(0));
        assertEquals(Sub.class, f2.type().parameterType(0));
        assertEquals(Sub.class, f2s.type().parameterType(0));
        assertEquals(Sub.class, chc.type().parameterType(0));
        assertEquals(Intf.class, ihc.type().parameterType(0));
        // test the MHs on a concrete version of Sub
        class C extends Sub {
            public void m1() { this.f2 = -1; }
            public void m2() { this.f2 = -2; }
            // Pack the vtable of Intf with leading junk:
            private void ig() { throw new RuntimeException(); }
            public void ig1() { ig(); }
            public void ig2() { ig(); }
            public void ig3() { ig(); }
            public void ig4() { ig(); }
        }
        testConcrete(new C(), m1, m2, f2, f2s, chc, ihc);
    }
    private static void testConcrete(Sub s,
                                     MethodHandle m1, MethodHandle m2,
                                     MethodHandle f2, MethodHandle f2s,
                                     MethodHandle chc, MethodHandle ihc
                                     ) throws Throwable {
        s.f2 = 0;
        m1.invokeExact(s);
        assertEquals(-1, s.f2);
        m2.invokeExact(s);
        assertEquals(-2, s.f2);
        s.f2 = 2;
        assertEquals(2, (int) f2.invokeExact(s));
        f2s.invokeExact(s, 0);
        assertEquals(0, s.f2);
        assertEquals(s.hashCode(), (int) chc.invokeExact(s));
        assertEquals(s.hashCode(), (int) ihc.invokeExact((Intf)s));
    }

    private static void assertEquals(Object expect, Object observe) {
        if (java.util.Objects.equals(expect, observe))  return;
        String msg = ("expected "+expect+" but observed "+observe);
        System.out.println("FAILED: "+msg);
        throw new AssertionError(msg);
    }
}
