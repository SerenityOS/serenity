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

/*
 * @test
 * @bug 8174399
 * @summary LambdaMetafactory should be able to handle inherited methods as 'implMethod'
 */
import java.lang.ReflectiveOperationException;
import java.lang.invoke.*;

public class InheritedMethodTest {

    public static MethodType mt(Class<?> ret, Class<?>... params) { return MethodType.methodType(ret, params); }

    public interface StringFactory {
        String get();
    }

    public interface I {
        String iString();
    }

    public interface J extends I {}

    public static abstract class C implements I {}

    public static class D extends C implements J {
        public String toString() { return "a"; }
        public String iString() { return "b"; }
    }

    private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

    public static void main(String... args) throws Throwable {
        test(lookup.findVirtual(C.class, "toString", mt(String.class)), "a");
        test(lookup.findVirtual(C.class, "iString", mt(String.class)), "b");
        test(lookup.findVirtual(J.class, "toString", mt(String.class)), "a");
        test(lookup.findVirtual(J.class, "iString", mt(String.class)), "b");
        test(lookup.findVirtual(I.class, "toString", mt(String.class)), "a");
        test(lookup.findVirtual(I.class, "iString", mt(String.class)), "b");
    }

    static void test(MethodHandle implMethod, String expected) throws Throwable {
        testMetafactory(implMethod, expected);
        testAltMetafactory(implMethod, expected);
    }

    static void testMetafactory(MethodHandle implMethod, String expected) throws Throwable {
        CallSite cs = LambdaMetafactory.metafactory(lookup, "get", mt(StringFactory.class, D.class), mt(String.class),
                                                    implMethod, mt(String.class));
        StringFactory factory = (StringFactory) cs.dynamicInvoker().invokeExact(new D());
        String actual = factory.get();
        if (!expected.equals(actual)) throw new AssertionError("Unexpected result: " + actual);
    }

    static void testAltMetafactory(MethodHandle implMethod, String expected) throws Throwable {
        CallSite cs = LambdaMetafactory.altMetafactory(lookup, "get", mt(StringFactory.class, D.class), mt(String.class),
                                                       implMethod, mt(String.class), LambdaMetafactory.FLAG_SERIALIZABLE);
        StringFactory factory = (StringFactory) cs.dynamicInvoker().invokeExact(new D());
        String actual = factory.get();
        if (!expected.equals(actual)) throw new AssertionError("Unexpected result: " + actual);
    }

}
