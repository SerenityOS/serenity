/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156486
 * @run testng/othervm VarHandleTestMethodTypeLong
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeLong
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeLong
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeLong
 */

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.*;

import static java.lang.invoke.MethodType.*;

public class VarHandleTestMethodTypeLong extends VarHandleBaseTest {
    static final long static_final_v = 0x0123456789ABCDEFL;

    static long static_v = 0x0123456789ABCDEFL;

    final long final_v = 0x0123456789ABCDEFL;

    long v = 0x0123456789ABCDEFL;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeLong.class, "final_v", long.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeLong.class, "v", long.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeLong.class, "static_final_v", long.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeLong.class, "static_v", long.class);

        vhArray = MethodHandles.arrayElementVarHandle(long[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeLong::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeLong::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeLong::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeLong::testArrayWrongMethodType,
                                                     false));
        }
        // Work around issue with jtreg summary reporting which truncates
        // the String result of Object.toString to 30 characters, hence
        // the first dummy argument
        return cases.stream().map(tc -> new Object[]{tc.toString(), tc}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "accessTestCaseProvider")
    public <T> void testAccess(String desc, AccessTestCase<T> atc) throws Throwable {
        T t = atc.get();
        int iters = atc.requiresLoop() ? ITERS : 1;
        for (int c = 0; c < iters; c++) {
            atc.testAccess(t);
        }
    }


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeLong recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            long x = (long) vh.get(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.get();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            long x = (long) vh.getVolatile(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            long x = (long) vh.getOpaque(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            long x = (long) vh.getAcquire(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.compareAndExchange(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.compareAndExchange(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchange(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchange(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.compareAndExchange(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchange(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.compareAndExchangeAcquire(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.compareAndExchangeAcquire(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchangeAcquire(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchangeAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.compareAndExchangeAcquire(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchangeAcquire(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.compareAndExchangeRelease(null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.compareAndExchangeRelease(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchangeRelease(recv, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchangeRelease(recv, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.compareAndExchangeRelease(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchangeRelease(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndSet(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndSet(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndSet(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSet(recv, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndSetAcquire(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndSetAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndSetAcquire(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSetAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndSetRelease(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndSetRelease(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndSetRelease(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSetRelease(recv, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndAdd(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndAdd(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAdd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndAdd(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAdd(recv, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndAddAcquire(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndAddAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAddAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndAddAcquire(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAddAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndAddRelease(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndAddRelease(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAddRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndAddRelease(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAddRelease(recv, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndBitwiseOr
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseOr(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseOr(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseOr(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOr(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseOrAcquire(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseOrAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOrAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseOrAcquire(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOrAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseOrRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseOrRelease(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseOr(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseOr(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOr(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseAnd(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseAnd(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseAnd(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAnd(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseAndAcquire(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseAndAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAndAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseAndAcquire(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAndAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAndRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseAndRelease(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseAnd(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseAnd(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAnd(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseXor(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseXor(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseXor(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXor(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseXorAcquire(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseXorAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXorAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseXorAcquire(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXorAcquire(recv, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXorRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.getAndBitwiseXorRelease(null, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            long x = (long) vh.getAndBitwiseXor(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            long x = (long) vh.getAndBitwiseXor(0, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(recv, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(recv, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXor(recv, 0x0123456789ABCDEFL, Void.class);
        });
    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeLong recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeLong.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeLong.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, long.class)).
                    invokeExact(0, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeLong.class, long.class, Class.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, long.class, long.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, long.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, Class.class, long.class)).
                    invokeExact(recv, Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, long.class, Class.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , long.class, long.class)).
                    invokeExact(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, long.class, long.class, Class.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class, long.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, long.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // expected reference class
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, Class.class, long.class)).
                    invokeExact(recv, Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // actual reference class
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class, Class.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class , long.class, long.class)).
                    invokeExact(0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeLong.class , long.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class , long.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class, long.class, Class.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, long.class)).
                    invokeExact(0, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { // null receiver
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, long.class)).
                    invokeExact(0, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { // null receiver
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact((VarHandleTestMethodTypeLong) null, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, long.class)).
                    invokeExact(0, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, VarHandleTestMethodTypeLong.class, long.class)).
                    invokeExact(recv, 0x0123456789ABCDEFL, Void.class);
            });
        }
    }


    static void testStaticFieldWrongMethodType(VarHandle vh) throws Throwable {
        // Get
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.get();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get();
        });
        // Incorrect arity
        checkWMTE(() -> { // >
            long x = (long) vh.get(Void.class);
        });


        // Set
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            vh.set(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(0x0123456789ABCDEFL, Void.class);
        });


        // GetVolatile
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getVolatile(Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            vh.setVolatile(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(0x0123456789ABCDEFL, Void.class);
        });


        // GetOpaque
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getOpaque(Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            vh.setOpaque(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(0x0123456789ABCDEFL, Void.class);
        });


        // GetAcquire
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAcquire(Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            vh.setRelease(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchange(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchange(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchange(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchangeAcquire(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchangeAcquire(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchangeAcquire(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchangeRelease(Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchangeRelease(0x0123456789ABCDEFL, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchangeRelease(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSet(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSetAcquire(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSetRelease(0x0123456789ABCDEFL, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAdd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAdd(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAddAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAddAcquire(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAddRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAddRelease(0x0123456789ABCDEFL, Void.class);
        });

        // GetAndBitwiseOr
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOr(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOr(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOrAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOrAcquire(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseOrReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOrRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrRelease(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOrRelease(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAnd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAnd(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAndAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAndAcquire(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAndReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAndRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndRelease(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAndRelease(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXor(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXor(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXorAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXorAcquire(0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXorReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXorRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorRelease(0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXorRelease(0x0123456789ABCDEFL, Void.class);
        });
    }

    static void testStaticFieldWrongMethodType(Handles hs) throws Throwable {
        int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            // Incorrect arity
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(Class.class)).
                    invokeExact(Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, long.class)).
                    invokeExact(Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // actual reference class
                long x = (long) hs.get(am, methodType(long.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long.class)).
                    invokeExact(0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long.class, Class.class)).
                    invokeExact(0x0123456789ABCDEFL, Void.class);
            });
        }
    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        long[] array = new long[10];
        Arrays.fill(array, 0x0123456789ABCDEFL);

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.get(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.get();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getVolatile(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getOpaque(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAcquire(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.compareAndExchange(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.compareAndExchange(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchange(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchange(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.compareAndExchange(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.compareAndExchange(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchange(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.compareAndExchangeAcquire(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.compareAndExchangeAcquire(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchangeAcquire(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchangeAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.compareAndExchangeAcquire(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.compareAndExchangeAcquire(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchangeAcquire(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            long x = (long) vh.compareAndExchangeRelease(null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.compareAndExchangeRelease(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // expected reference class
            long x = (long) vh.compareAndExchangeRelease(array, 0, Void.class, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // actual reference class
            long x = (long) vh.compareAndExchangeRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.compareAndExchangeRelease(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.compareAndExchangeRelease(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.compareAndExchangeRelease(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndSet(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndSet(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            long x = (long) vh.getAndSet(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndSet(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSet(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndSetAcquire(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndSetAcquire(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            long x = (long) vh.getAndSetAcquire(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndSetAcquire(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSetAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndSetRelease(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndSetRelease(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            long x = (long) vh.getAndSetRelease(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndSetRelease(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndSetRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndAdd(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndAdd(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAdd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndAdd(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndAdd(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAdd(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndAddAcquire(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndAddAcquire(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAddAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndAddAcquire(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndAddAcquire(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAddAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndAddRelease(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndAddRelease(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndAddRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndAddRelease(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndAddRelease(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndAddRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });

        // GetAndBitwiseOr
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseOr(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseOr(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOr(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseOr(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseOr(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOr(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseOrAcquire(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseOrAcquire(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOrAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseOrAcquire(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseOrAcquire(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOrAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseOrRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseOrRelease(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseOrRelease(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseOrRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseOrRelease(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseOrRelease(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrRelease(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseOrRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseAnd(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseAnd(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAnd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseAnd(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseAnd(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAnd(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseAndAcquire(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseAndAcquire(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAndAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseAndAcquire(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseAndAcquire(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAndAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseAndRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseAndRelease(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseAndRelease(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseAndRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseAndRelease(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseAndRelease(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndRelease(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseAndRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseXor(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseXor(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXor(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseXor(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseXor(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXor(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseXorAcquire(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseXorAcquire(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXorAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseXorAcquire(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseXorAcquire(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXorAcquire(array, 0, 0x0123456789ABCDEFL, Void.class);
        });


        // GetAndBitwiseXorRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            long x = (long) vh.getAndBitwiseXorRelease(null, 0, 0x0123456789ABCDEFL);
        });
        checkCCE(() -> { // array reference class
            long x = (long) vh.getAndBitwiseXorRelease(Void.class, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // value reference class
            long x = (long) vh.getAndBitwiseXorRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            long x = (long) vh.getAndBitwiseXorRelease(0, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // index reference class
            long x = (long) vh.getAndBitwiseXorRelease(array, Void.class, 0x0123456789ABCDEFL);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorRelease(array, 0, 0x0123456789ABCDEFL);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, 0, 0x0123456789ABCDEFL);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            long x = (long) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { // >
            long x = (long) vh.getAndBitwiseXorRelease(array, 0, 0x0123456789ABCDEFL, Void.class);
        });
    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        long[] array = new long[10];
        Arrays.fill(array, 0x0123456789ABCDEFL);

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class)).
                    invokeExact((long[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, long[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, long[].class, int.class, long.class)).
                    invokeExact((long[]) null, 0, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, long.class)).
                    invokeExact(Void.class, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, long[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, long.class)).
                    invokeExact(0, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, long[].class, Class.class, long.class)).
                    invokeExact(array, Void.class, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, long[].class, int.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class, long.class)).
                    invokeExact((long[]) null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, long.class, long.class)).
                    invokeExact(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, Class.class, long.class)).
                    invokeExact(array, 0, Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, long.class, long.class)).
                    invokeExact(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long[].class, Class.class, long.class, long.class)).
                    invokeExact(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class, long.class)).
                    invokeExact((long[]) null, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, int.class, long.class, long.class)).
                    invokeExact(Void.class, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // expected reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, Class.class, long.class)).
                    invokeExact(array, 0, Void.class, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // actual reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, int.class, long.class, long.class)).
                    invokeExact(0, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // index reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, Class.class, long.class, long.class)).
                    invokeExact(array, Void.class, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long[].class, int.class, long.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class)).
                    invokeExact((long[]) null, 0, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, int.class, long.class)).
                    invokeExact(Void.class, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, int.class, long.class)).
                    invokeExact(0, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // index reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, Class.class, long.class)).
                    invokeExact(array, Void.class, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long[].class, int.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class)).
                    invokeExact((long[]) null, 0, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, int.class, long.class)).
                    invokeExact(Void.class, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, int.class, long.class)).
                    invokeExact(0, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // index reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, Class.class, long.class)).
                    invokeExact(array, Void.class, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long[].class, int.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class)).
                    invokeExact((long[]) null, 0, 0x0123456789ABCDEFL);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                long x = (long) hs.get(am, methodType(long.class, Class.class, int.class, long.class)).
                    invokeExact(Void.class, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // value reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                long x = (long) hs.get(am, methodType(long.class, int.class, int.class, long.class)).
                    invokeExact(0, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // index reference class
                long x = (long) hs.get(am, methodType(long.class, long[].class, Class.class, long.class)).
                    invokeExact(array, Void.class, 0x0123456789ABCDEFL);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, long[].class, int.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, long[].class, int.class, long.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                long x = (long) hs.get(am, methodType(long.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                long x = (long) hs.get(am, methodType(long.class, long[].class, int.class, long.class, Class.class)).
                    invokeExact(array, 0, 0x0123456789ABCDEFL, Void.class);
            });
        }
    }
}
