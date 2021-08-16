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
 * @run testng/othervm VarHandleTestMethodTypeBoolean
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeBoolean
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeBoolean
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeBoolean
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

public class VarHandleTestMethodTypeBoolean extends VarHandleBaseTest {
    static final boolean static_final_v = true;

    static boolean static_v = true;

    final boolean final_v = true;

    boolean v = true;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeBoolean.class, "final_v", boolean.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeBoolean.class, "v", boolean.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeBoolean.class, "static_final_v", boolean.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeBoolean.class, "static_v", boolean.class);

        vhArray = MethodHandles.arrayElementVarHandle(boolean[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeBoolean::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeBoolean::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeBoolean::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeBoolean::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeBoolean recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean x = (boolean) vh.get(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.get(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.get();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, true);
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, true, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean x = (boolean) vh.getVolatile(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getVolatile(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, true);
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, true, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean x = (boolean) vh.getOpaque(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getOpaque(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, true);
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, true, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean x = (boolean) vh.getAcquire(0);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAcquire(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, true);
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, true, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, true, true, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, true, true, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, true, true, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, true, true, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, true, true, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.compareAndExchange(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.compareAndExchange(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchange(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchange(recv, true, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.compareAndExchange(0, true, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchange(recv, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchange(recv, true, true, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.compareAndExchangeAcquire(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, true, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(0, true, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchangeAcquire(recv, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, true, true, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.compareAndExchangeRelease(null, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(Void.class, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, true, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(0, true, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchangeRelease(recv, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, true, true, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndSet(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndSet(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndSet(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSet(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSet(recv, true, Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndSetAcquire(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndSetAcquire(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndSetAcquire(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSetAcquire(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSetAcquire(recv, true, Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndSetRelease(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndSetRelease(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndSetRelease(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSetRelease(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSetRelease(recv, true, Void.class);
        });


        // GetAndBitwiseOr
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseOr(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseOr(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOr(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOr(recv, true, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOrAcquire(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(recv, true, Void.class);
        });


        // GetAndBitwiseOrRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseOrRelease(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseOr(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOr(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOr(recv, true, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseAnd(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAnd(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, true, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAndAcquire(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(recv, true, Void.class);
        });


        // GetAndBitwiseAndRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseAndRelease(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAnd(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, true, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseXor(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseXor(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXor(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXor(recv, true, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXorAcquire(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(recv, true, Void.class);
        });


        // GetAndBitwiseXorRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.getAndBitwiseXorRelease(null, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean x = (boolean) vh.getAndBitwiseXor(Void.class, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(0, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(recv, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXor(recv, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXor(recv, true, Void.class);
        });
    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeBoolean recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class)).
                    invokeExact((VarHandleTestMethodTypeBoolean) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeBoolean.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeBoolean.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact((VarHandleTestMethodTypeBoolean) null, true);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, boolean.class)).
                    invokeExact(Void.class, true);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeBoolean.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, boolean.class)).
                    invokeExact(0, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeBoolean.class, boolean.class, Class.class)).
                    invokeExact(recv, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class, boolean.class)).
                    invokeExact((VarHandleTestMethodTypeBoolean) null, true, true);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, boolean.class, boolean.class)).
                    invokeExact(Void.class, true, true);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, Class.class, boolean.class)).
                    invokeExact(recv, Void.class, true);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class, Class.class)).
                    invokeExact(recv, true, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , boolean.class, boolean.class)).
                    invokeExact(0, true, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class, boolean.class, Class.class)).
                    invokeExact(recv, true, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class, boolean.class)).
                    invokeExact((VarHandleTestMethodTypeBoolean) null, true, true);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, boolean.class, boolean.class)).
                    invokeExact(Void.class, true, true);
            });
            checkWMTE(() -> { // expected reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, Class.class, boolean.class)).
                    invokeExact(recv, Void.class, true);
            });
            checkWMTE(() -> { // actual reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class, Class.class)).
                    invokeExact(recv, true, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class , boolean.class, boolean.class)).
                    invokeExact(0, true, true);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeBoolean.class , boolean.class, boolean.class)).
                    invokeExact(recv, true, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeBoolean.class , boolean.class, boolean.class)).
                    invokeExact(recv, true, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class, boolean.class, Class.class)).
                    invokeExact(recv, true, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact((VarHandleTestMethodTypeBoolean) null, true);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, boolean.class)).
                    invokeExact(Void.class, true);
            });
            checkWMTE(() -> { // value reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, boolean.class)).
                    invokeExact(0, true);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact(recv, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact(recv, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact(recv, true, Void.class);
            });
        }


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { // null receiver
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact((VarHandleTestMethodTypeBoolean) null, true);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, boolean.class)).
                    invokeExact(Void.class, true);
            });
            checkWMTE(() -> { // value reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, boolean.class)).
                    invokeExact(0, true);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact(recv, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact(recv, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeBoolean.class, boolean.class)).
                    invokeExact(recv, true, Void.class);
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
            int x = (int) vh.get();
        });
        // Incorrect arity
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.get(Void.class);
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
            vh.set(true, Void.class);
        });


        // GetVolatile
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getVolatile(Void.class);
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
            vh.setVolatile(true, Void.class);
        });


        // GetOpaque
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getOpaque(Void.class);
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
            vh.setOpaque(true, Void.class);
        });


        // GetAcquire
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAcquire(Void.class);
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
            vh.setRelease(true, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(true, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(true, true, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(true, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(true, true, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(true, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(true, true, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(true, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(true, true, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(true, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(true, true, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchange(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchange(true, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchange(true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchange(true, true, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(true, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchangeAcquire(true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchangeAcquire(true, true, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(true, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchangeRelease(true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchangeRelease(true, true, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSet(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSet(true, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSetAcquire(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSetAcquire(true, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSetRelease(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSetRelease(true, Void.class);
        });


        // GetAndBitwiseOr
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOr(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOr(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOr(true, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOrAcquire(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(true, Void.class);
        });


        // GetAndBitwiseOrReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrRelease(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOrRelease(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOrRelease(true, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAnd(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAnd(true, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAndAcquire(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(true, Void.class);
        });


        // GetAndBitwiseAndReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndRelease(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAndRelease(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAndRelease(true, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXor(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXor(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXor(true, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXorAcquire(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(true, Void.class);
        });


        // GetAndBitwiseXorReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorRelease(true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXorRelease(true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXorRelease(true, Void.class);
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
                int x = (int) hs.get(am, methodType(int.class)).
                    invokeExact();
            });
            // Incorrect arity
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(Class.class)).
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
                hs.get(am, methodType(void.class, boolean.class, Class.class)).
                    invokeExact(true, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, boolean.class)).
                    invokeExact(Void.class, true);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean.class, Class.class)).
                    invokeExact(true, Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean.class, boolean.class, Class.class)).
                    invokeExact(true, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, boolean.class)).
                    invokeExact(Void.class, true);
            });
            checkWMTE(() -> { // actual reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean.class, Class.class)).
                    invokeExact(true, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, boolean.class, boolean.class)).
                    invokeExact(true, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean.class, boolean.class)).
                    invokeExact(true, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean.class, boolean.class, Class.class)).
                    invokeExact(true, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, boolean.class)).
                    invokeExact(true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean.class)).
                    invokeExact(true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean.class, Class.class)).
                    invokeExact(true, Void.class);
            });
        }


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, boolean.class)).
                    invokeExact(true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean.class)).
                    invokeExact(true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean.class, Class.class)).
                    invokeExact(true, Void.class);
            });
        }
    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        boolean[] array = new boolean[10];
        Arrays.fill(array, true);

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.get(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.get(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.get();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, true, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getVolatile(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getVolatile(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, true, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getOpaque(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getOpaque(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, true, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAcquire(array, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAcquire(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, true, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, true, true, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, true, true, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, true, true, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, true, true, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, true, true);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, true, true, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.compareAndExchange(null, 0, true, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.compareAndExchange(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchange(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchange(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.compareAndExchange(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.compareAndExchange(array, Void.class, true, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchange(array, 0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchange(array, 0, true, true, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.compareAndExchangeAcquire(null, 0, true, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, Void.class, true, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchangeAcquire(array, 0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, true, true, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean x = (boolean) vh.compareAndExchangeRelease(null, 0, true, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(Void.class, 0, true, true);
        });
        checkWMTE(() -> { // expected reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, Void.class, true);
        });
        checkWMTE(() -> { // actual reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, true, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(0, 0, true, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, Void.class, true, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, true, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.compareAndExchangeRelease(array, 0, true, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, true, true, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndSet(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndSet(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            boolean x = (boolean) vh.getAndSet(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndSet(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSet(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSet(array, 0, true, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndSetAcquire(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndSetAcquire(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            boolean x = (boolean) vh.getAndSetAcquire(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndSetAcquire(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSetAcquire(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, true, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndSetRelease(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndSetRelease(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            boolean x = (boolean) vh.getAndSetRelease(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndSetRelease(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndSetRelease(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndSetRelease(array, 0, true, Void.class);
        });


        // GetAndBitwiseOr
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseOr(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseOr(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOr(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseOr(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOr(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOr(array, 0, true, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOrAcquire(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, 0, true, Void.class);
        });


        // GetAndBitwiseOrRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseOrRelease(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrRelease(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseOrRelease(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, 0, true, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseAnd(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseAnd(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAnd(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAnd(array, 0, true, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAndAcquire(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, 0, true, Void.class);
        });


        // GetAndBitwiseAndRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseAndRelease(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndRelease(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseAndRelease(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, 0, true, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseXor(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseXor(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXor(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseXor(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXor(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXor(array, 0, true, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXorAcquire(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, 0, true, Void.class);
        });


        // GetAndBitwiseXorRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            boolean x = (boolean) vh.getAndBitwiseXorRelease(null, 0, true);
        });
        checkCCE(() -> { // array reference class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(Void.class, 0, true);
        });
        checkWMTE(() -> { // value reference class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(0, 0, true);
        });
        checkWMTE(() -> { // index reference class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, Void.class, true);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorRelease(array, 0, true);
        });
        checkWMTE(() -> { // primitive class
            int x = (int) vh.getAndBitwiseXorRelease(array, 0, true);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean x = (boolean) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { // >
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, 0, true, Void.class);
        });
    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        boolean[] array = new boolean[10];
        Arrays.fill(array, true);

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class)).
                    invokeExact((boolean[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, boolean[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, boolean[].class, int.class, boolean.class)).
                    invokeExact((boolean[]) null, 0, true);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, boolean.class)).
                    invokeExact(Void.class, 0, true);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, boolean[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, boolean.class)).
                    invokeExact(0, 0, true);
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, boolean[].class, Class.class, boolean.class)).
                    invokeExact(array, Void.class, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, boolean[].class, int.class, Class.class)).
                    invokeExact(array, 0, true, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, boolean.class)).
                    invokeExact((boolean[]) null, 0, true, true);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, boolean.class, boolean.class)).
                    invokeExact(Void.class, 0, true, true);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, Class.class, boolean.class)).
                    invokeExact(array, 0, Void.class, true);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, Class.class)).
                    invokeExact(array, 0, true, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, boolean.class, boolean.class)).
                    invokeExact(0, 0, true, true);
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, Class.class, boolean.class, boolean.class)).
                    invokeExact(array, Void.class, true, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, boolean.class, Class.class)).
                    invokeExact(array, 0, true, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, boolean.class)).
                    invokeExact((boolean[]) null, 0, true, true);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, boolean.class, boolean.class)).
                    invokeExact(Void.class, 0, true, true);
            });
            checkWMTE(() -> { // expected reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, Class.class, boolean.class)).
                    invokeExact(array, 0, Void.class, true);
            });
            checkWMTE(() -> { // actual reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, Class.class)).
                    invokeExact(array, 0, true, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, boolean.class, boolean.class)).
                    invokeExact(0, 0, true, true);
            });
            checkWMTE(() -> { // index reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, Class.class, boolean.class, boolean.class)).
                    invokeExact(array, Void.class, true, true);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, boolean[].class, int.class, boolean.class, boolean.class)).
                    invokeExact(array, 0, true, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean[].class, int.class, boolean.class, boolean.class)).
                    invokeExact(array, 0, true, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, boolean.class, Class.class)).
                    invokeExact(array, 0, true, true, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class)).
                    invokeExact((boolean[]) null, 0, true);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, boolean.class)).
                    invokeExact(Void.class, 0, true);
            });
            checkWMTE(() -> { // value reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, boolean.class)).
                    invokeExact(0, 0, true);
            });
            checkWMTE(() -> { // index reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, Class.class, boolean.class)).
                    invokeExact(array, Void.class, true);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, boolean[].class, int.class, boolean.class)).
                    invokeExact(array, 0, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean[].class, int.class, boolean.class)).
                    invokeExact(array, 0, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, Class.class)).
                    invokeExact(array, 0, true, Void.class);
            });
        }


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class)).
                    invokeExact((boolean[]) null, 0, true);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, boolean.class)).
                    invokeExact(Void.class, 0, true);
            });
            checkWMTE(() -> { // value reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, boolean.class)).
                    invokeExact(0, 0, true);
            });
            checkWMTE(() -> { // index reference class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, Class.class, boolean.class)).
                    invokeExact(array, Void.class, true);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, boolean[].class, int.class, boolean.class)).
                    invokeExact(array, 0, true);
            });
            checkWMTE(() -> { // primitive class
                int x = (int) hs.get(am, methodType(int.class, boolean[].class, int.class, boolean.class)).
                    invokeExact(array, 0, true);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean x = (boolean) hs.get(am, methodType(boolean.class, boolean[].class, int.class, boolean.class, Class.class)).
                    invokeExact(array, 0, true, Void.class);
            });
        }
    }
}
