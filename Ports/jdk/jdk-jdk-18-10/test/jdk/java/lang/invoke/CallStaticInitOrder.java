/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @summary static initializer invocation order
 *
 * @build indify.Indify
 * @compile CallStaticInitOrder.java
 * @run main/othervm
 *      indify.Indify
 *      --expand-properties --classpath ${test.classes}
 *      --java test.java.lang.invoke.CallStaticInitOrder
 */

package test.java.lang.invoke;

import java.io.*;

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class CallStaticInitOrder {
    private static int TICK;
    private static synchronized int tick(String event) {
        int n = ++TICK;
        System.out.println("event #"+n+" = "+event);
        return n;
    }

    static int Init1Tick;
    private static class Init1 {
        static { Init1Tick = tick("foo -> Init1.<clinit>"); }
        static int foo() { return Init1Tick; }
    }

    static int Init2Tick;
    private static class Init2 {
        static { Init2Tick = tick("bar -> Init2.<clinit>"); }
        static int bar() { return Init2Tick; }
    }

    static int Init3Tick;
    private static class Init3 {
        static { Init3Tick = tick("baz -> Init3.<clinit>"); }
        static int baz() { return Init3Tick; }
    }

    static int Init4Tick;
    private static class Init4 {
        static { Init4Tick = tick("bat -> Init4.<clinit>"); }
        static int bat() { return Init4Tick; }
    }

    static int Init5Tick;
    private static class Init5 {
        static { Init5Tick = tick("read bang -> Init5.<clinit>"); }
        static int bang = Init5Tick;
    }

    static int Init6Tick;
    private static class Init6 {
        static { Init6Tick = tick("write pong -> Init6.<clinit>"); }
        static int pong;
    }

    private static final MutableCallSite CONSTANT_CS_baz;
    private static MethodHandle MH_foo() throws ReflectiveOperationException {
        return lookup().findStatic(Init1.class, "foo", methodType(int.class));
    }
    private static final MethodHandle CONSTANT_MH_bar;
    private static MethodHandle MH_baz() throws ReflectiveOperationException {
        return lookup().findStatic(Init3.class, "baz", methodType(int.class));
    }
    private static final MethodHandle CONSTANT_MH_bat;
    private static final MethodHandle CONSTANT_MH_bangGetter;
    private static final MethodHandle CONSTANT_MH_pongSetter;
    static {
        try {
            int t1 = tick("CallStaticInitOrder.<clinit>");
            {
                CONSTANT_CS_baz = new MutableCallSite(methodType(int.class));
                // MH_foo() := lookup().findStatic(Init1.class, "foo", methodType(int.class));
                CONSTANT_MH_bar = lookup().findStatic(Init2.class, "bar", methodType(int.class));
                // MH_baz() := lookup().findStatic(Init3.class, "baz", methodType(int.class));
                CONSTANT_MH_bat = lookup().unreflect(Init4.class.getDeclaredMethod("bat"));
                CONSTANT_MH_bangGetter = lookup().findStaticGetter(Init5.class, "bang", int.class);
                MethodHandle pongSetter = lookup().findStaticSetter(Init6.class, "pong", int.class);
                MethodHandle tickGetter = lookup().findStaticGetter(CallStaticInitOrder.class, "Init6Tick", int.class);
                CONSTANT_MH_pongSetter = filterReturnValue(insertArguments(pongSetter, 0, -99), tickGetter);
            }
            int t2 = tick("CallStaticInitOrder.<clinit> done");
            assertEquals(t1+1, t2);  // no ticks in between
        } catch (Exception ex) {
            throw new InternalError(ex.toString());
        }
    }

    public static void main(String... av) throws Throwable {
        testInit();
        if (LAST_LOSER != null)  throw LAST_LOSER;
    }

    private static Throwable LAST_LOSER;

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            Throwable loser = new AssertionError("expected: " + expected + ", actual: " + actual);
            if (LAST_LOSER != null)
                LAST_LOSER.printStackTrace(System.out);
            LAST_LOSER = loser;
        }
    }

    private static void testInit() throws Throwable {
        System.out.println("runFoo = "+runFoo());
        System.out.println("runBar = "+runBar());
        try {
            runBaz();
        } catch (IllegalStateException ex) {
            tick("runBaz throw/catch");
        }
        CONSTANT_CS_baz.setTarget(MH_baz());
        System.out.println("runBaz = "+runBaz());
        System.out.println("runBat = "+runBat());
        System.out.println("runBang = "+runBang());
        System.out.println("runPong = "+runPong());
    }

    private static int runFoo() throws Throwable {
        assertEquals(Init1Tick, 0);  // Init1 not initialized yet
        int t1 = tick("runFoo");
        int t2 = (int) INDY_foo().invokeExact();
        int t3 = tick("runFoo done");
        assertEquals(Init1Tick, t2);  // when Init1 was initialized
        assertEquals(t1+2, t3);  // exactly two ticks in between
        assertEquals(t1+1, t2);  // init happened inside
        return t2;
    }
    private static MethodHandle INDY_foo() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm().invoke(lookup(), "foo", methodType(int.class))).dynamicInvoker();
    }

    private static int runBar() throws Throwable {
        assertEquals(Init2Tick, 0);  // Init2 not initialized yet
        int t1 = tick("runBar");
        int t2 = (int) INDY_bar().invokeExact();
        int t3 = tick("runBar done");
        assertEquals(Init2Tick, t2);  // when Init2 was initialized
        assertEquals(t1+2, t3);  // exactly two ticks in between
        assertEquals(t1+1, t2);  // init happened inside
        return t2;
    }
    private static MethodHandle INDY_bar() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm().invoke(lookup(), "bar", methodType(int.class))).dynamicInvoker();
    }

    private static int runBaz() throws Throwable {
        assertEquals(Init3Tick, 0);  // Init3 not initialized yet
        int t1 = tick("runBaz");
        int t2 = (int) INDY_baz().invokeExact();
        int t3 = tick("runBaz done");
        assertEquals(Init3Tick, t2);  // when Init3 was initialized
        assertEquals(t1+2, t3);  // exactly two ticks in between
        assertEquals(t1+1, t2);  // init happened inside
        return t2;
    }
    private static MethodHandle INDY_baz() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm().invoke(lookup(), "baz", methodType(int.class))).dynamicInvoker();
    }

    private static int runBat() throws Throwable {
        assertEquals(Init4Tick, 0);  // Init4 not initialized yet
        int t1 = tick("runBat");
        int t2 = (int) INDY_bat().invokeExact();
        int t3 = tick("runBat done");
        assertEquals(Init4Tick, t2);  // when Init4 was initialized
        assertEquals(t1+2, t3);  // exactly two ticks in between
        assertEquals(t1+1, t2);  // init happened inside
        return t2;
    }
    private static MethodHandle INDY_bat() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm().invoke(lookup(), "bat", methodType(int.class))).dynamicInvoker();
    }

    private static int runBang() throws Throwable {
        assertEquals(Init5Tick, 0);  // Init5 not initialized yet
        int t1 = tick("runBang");
        int t2 = (int) INDY_bang().invokeExact();
        int t3 = tick("runBang done");
        assertEquals(Init5Tick, t2);  // when Init5 was initialized
        assertEquals(t1+2, t3);  // exactly two ticks in between
        assertEquals(t1+1, t2);  // init happened inside
        return t2;
    }
    private static MethodHandle INDY_bang() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm().invoke(lookup(), "bang", methodType(int.class))).dynamicInvoker();
    }

    private static int runPong() throws Throwable {
        assertEquals(Init6Tick, 0);  // Init6 not initialized yet
        int t1 = tick("runPong");
        int t2 = (int) INDY_pong().invokeExact();
        int t3 = tick("runPong done");
        assertEquals(Init6Tick, t2);  // when Init6 was initialized
        assertEquals(t1+2, t3);  // exactly two ticks in between
        assertEquals(t1+1, t2);  // init happened inside
        return t2;
    }
    private static MethodHandle INDY_pong() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm().invoke(lookup(), "pong", methodType(int.class))).dynamicInvoker();
    }

    private static CallSite bsm(Lookup caller, String name, MethodType type) throws ReflectiveOperationException {
        System.out.println("bsm "+name+type);
        CallSite res;
        switch (name) {
        case "foo":
            res = new ConstantCallSite(MH_foo()); break;
        case "bar":
            res = new ConstantCallSite(CONSTANT_MH_bar); break;
        case "baz":
            res = CONSTANT_CS_baz; break;
        case "bat":
            res = new ConstantCallSite(CONSTANT_MH_bat); break;
        case "bang":
            res = new ConstantCallSite(CONSTANT_MH_bangGetter); break;
        case "pong":
            res = new ConstantCallSite(CONSTANT_MH_pongSetter); break;
        default:
            res = null;
        }
        if (res == null || !res.type().equals(type)) {
            throw new AssertionError(String.valueOf(res));
        }
        return res;
    }
    private static MethodHandle MH_bsm() throws ReflectiveOperationException {
        shouldNotCallThis();
        return lookup().findStatic(lookup().lookupClass(), "bsm",
                                   methodType(CallSite.class, Lookup.class, String.class, MethodType.class));
    }
    private static void shouldNotCallThis() {
        // if this gets called, the transformation has not taken place
        throw new AssertionError("this code should be statically transformed away by Indify");
    }
}
