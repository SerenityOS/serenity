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
 * @run testng/othervm VarHandleTestMethodTypeFloat
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeFloat
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeFloat
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeFloat
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

public class VarHandleTestMethodTypeFloat extends VarHandleBaseTest {
    static final float static_final_v = 1.0f;

    static float static_v = 1.0f;

    final float final_v = 1.0f;

    float v = 1.0f;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeFloat.class, "final_v", float.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeFloat.class, "v", float.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeFloat.class, "static_final_v", float.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeFloat.class, "static_v", float.class);

        vhArray = MethodHandles.arrayElementVarHandle(float[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeFloat::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeFloat::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeFloat::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeFloat::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeFloat recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            float x = (float) vh.get(0);
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
            float x = (float) vh.get();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, 1.0f, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            float x = (float) vh.getVolatile(0);
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
            float x = (float) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, 1.0f, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            float x = (float) vh.getOpaque(0);
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
            float x = (float) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, 1.0f, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            float x = (float) vh.getAcquire(0);
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
            float x = (float) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, 1.0f, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, 1.0f, 1.0f, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.compareAndExchange(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.compareAndExchange(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchange(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchange(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.compareAndExchange(0, 1.0f, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(recv, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchange(recv, 1.0f, 1.0f, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.compareAndExchangeAcquire(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.compareAndExchangeAcquire(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchangeAcquire(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchangeAcquire(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.compareAndExchangeAcquire(0, 1.0f, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchangeAcquire(recv, 1.0f, 1.0f, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.compareAndExchangeRelease(null, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.compareAndExchangeRelease(Void.class, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchangeRelease(recv, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchangeRelease(recv, 1.0f, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.compareAndExchangeRelease(0, 1.0f, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchangeRelease(recv, 1.0f, 1.0f, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAndSet(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAndSet(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.getAndSet(0, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(recv, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSet(recv, 1.0f, Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAndSetAcquire(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAndSetAcquire(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.getAndSetAcquire(0, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(recv, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSetAcquire(recv, 1.0f, Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAndSetRelease(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAndSetRelease(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.getAndSetRelease(0, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(recv, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSetRelease(recv, 1.0f, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAndAdd(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAndAdd(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAdd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.getAndAdd(0, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(recv, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(recv, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAdd(recv, 1.0f, Void.class);
        });

        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAndAddAcquire(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAndAddAcquire(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAddAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.getAndAddAcquire(0, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(recv, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(recv, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAddAcquire(recv, 1.0f, Void.class);
        });

        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.getAndAddRelease(null, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            float x = (float) vh.getAndAddRelease(Void.class, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAddRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            float x = (float) vh.getAndAddRelease(0, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(recv, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(recv, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAddRelease(recv, 1.0f, Void.class);
        });

    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeFloat recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class)).
                    invokeExact((VarHandleTestMethodTypeFloat) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeFloat.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact((VarHandleTestMethodTypeFloat) null, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, float.class)).
                    invokeExact(Void.class, 1.0f);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeFloat.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, float.class)).
                    invokeExact(0, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeFloat.class, float.class, Class.class)).
                    invokeExact(recv, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class, float.class, float.class)).
                    invokeExact((VarHandleTestMethodTypeFloat) null, 1.0f, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, float.class, float.class)).
                    invokeExact(Void.class, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class, Class.class, float.class)).
                    invokeExact(recv, Void.class, 1.0f);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class, float.class, Class.class)).
                    invokeExact(recv, 1.0f, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , float.class, float.class)).
                    invokeExact(0, 1.0f, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class, float.class, float.class, Class.class)).
                    invokeExact(recv, 1.0f, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class, float.class)).
                    invokeExact((VarHandleTestMethodTypeFloat) null, 1.0f, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, float.class, float.class)).
                    invokeExact(Void.class, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // expected reference class
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, Class.class, float.class)).
                    invokeExact(recv, Void.class, 1.0f);
            });
            checkWMTE(() -> { // actual reference class
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class, Class.class)).
                    invokeExact(recv, 1.0f, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class , float.class, float.class)).
                    invokeExact(0, 1.0f, 1.0f);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeFloat.class , float.class, float.class)).
                    invokeExact(recv, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class , float.class, float.class)).
                    invokeExact(recv, 1.0f, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class, float.class, Class.class)).
                    invokeExact(recv, 1.0f, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact((VarHandleTestMethodTypeFloat) null, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, float.class)).
                    invokeExact(Void.class, 1.0f);
            });
            checkWMTE(() -> { // value reference class
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class, float.class)).
                    invokeExact(0, 1.0f);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact(recv, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact(recv, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact(recv, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { // null receiver
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact((VarHandleTestMethodTypeFloat) null, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, float.class)).
                    invokeExact(Void.class, 1.0f);
            });
            checkWMTE(() -> { // value reference class
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class, float.class)).
                    invokeExact(0, 1.0f);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact(recv, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact(recv, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, VarHandleTestMethodTypeFloat.class, float.class)).
                    invokeExact(recv, 1.0f, Void.class);
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
            float x = (float) vh.get(Void.class);
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
            vh.set(1.0f, Void.class);
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
            float x = (float) vh.getVolatile(Void.class);
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
            vh.setVolatile(1.0f, Void.class);
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
            float x = (float) vh.getOpaque(Void.class);
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
            vh.setOpaque(1.0f, Void.class);
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
            float x = (float) vh.getAcquire(Void.class);
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
            vh.setRelease(1.0f, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(1.0f, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(1.0f, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(1.0f, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(1.0f, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(1.0f, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(1.0f, 1.0f, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchange(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchange(1.0f, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchange(1.0f, 1.0f, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchangeAcquire(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchangeAcquire(1.0f, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchangeAcquire(1.0f, 1.0f, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchangeRelease(Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchangeRelease(1.0f, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchangeRelease(1.0f, 1.0f, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSet(1.0f, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSetAcquire(1.0f, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSetRelease(1.0f, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAdd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAdd(1.0f, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAddAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAddAcquire(1.0f, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAddRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAddRelease(1.0f, Void.class);
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
                float x = (float) hs.get(am, methodType(Class.class)).
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
                hs.get(am, methodType(void.class, float.class, Class.class)).
                    invokeExact(1.0f, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, float.class)).
                    invokeExact(Void.class, 1.0f);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float.class, Class.class)).
                    invokeExact(1.0f, Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float.class, float.class, Class.class)).
                    invokeExact(1.0f, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, float.class)).
                    invokeExact(Void.class, 1.0f);
            });
            checkWMTE(() -> { // actual reference class
                float x = (float) hs.get(am, methodType(float.class, float.class, Class.class)).
                    invokeExact(1.0f, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, float.class, float.class)).
                    invokeExact(1.0f, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float.class, float.class)).
                    invokeExact(1.0f, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float.class, float.class, Class.class)).
                    invokeExact(1.0f, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, float.class)).
                    invokeExact(1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float.class)).
                    invokeExact(1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float.class, Class.class)).
                    invokeExact(1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, float.class)).
                    invokeExact(1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float.class)).
                    invokeExact(1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float.class, Class.class)).
                    invokeExact(1.0f, Void.class);
            });
        }

    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        float[] array = new float[10];
        Arrays.fill(array, 1.0f);

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.get(array, Void.class);
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
            float x = (float) vh.get();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, 1.0f, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getVolatile(array, Void.class);
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
            float x = (float) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, 1.0f, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getOpaque(array, Void.class);
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
            float x = (float) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, 1.0f, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAcquire(array, Void.class);
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
            float x = (float) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, 1.0f, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 1.0f, 1.0f, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, 1.0f, 1.0f, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.compareAndExchange(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.compareAndExchange(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchange(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchange(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.compareAndExchange(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.compareAndExchange(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(array, 0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchange(array, 0, 1.0f, 1.0f, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.compareAndExchangeAcquire(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.compareAndExchangeAcquire(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchangeAcquire(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchangeAcquire(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.compareAndExchangeAcquire(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.compareAndExchangeAcquire(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchangeAcquire(array, 0, 1.0f, 1.0f, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            float x = (float) vh.compareAndExchangeRelease(null, 0, 1.0f, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.compareAndExchangeRelease(Void.class, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // expected reference class
            float x = (float) vh.compareAndExchangeRelease(array, 0, Void.class, 1.0f);
        });
        checkWMTE(() -> { // actual reference class
            float x = (float) vh.compareAndExchangeRelease(array, 0, 1.0f, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.compareAndExchangeRelease(0, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.compareAndExchangeRelease(array, Void.class, 1.0f, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, 1.0f, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, 1.0f, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.compareAndExchangeRelease(array, 0, 1.0f, 1.0f, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAndSet(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAndSet(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            float x = (float) vh.getAndSet(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAndSet(array, Void.class, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(array, 0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSet(array, 0, 1.0f, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAndSetAcquire(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAndSetAcquire(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            float x = (float) vh.getAndSetAcquire(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAndSetAcquire(array, Void.class, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSetAcquire(array, 0, 1.0f, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAndSetRelease(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAndSetRelease(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            float x = (float) vh.getAndSetRelease(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAndSetRelease(array, Void.class, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndSetRelease(array, 0, 1.0f, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAndAdd(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAndAdd(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAdd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.getAndAdd(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAndAdd(array, Void.class, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(array, 0, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(array, 0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAdd(array, 0, 1.0f, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAndAddAcquire(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAndAddAcquire(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAddAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.getAndAddAcquire(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAndAddAcquire(array, Void.class, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(array, 0, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(array, 0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAddAcquire(array, 0, 1.0f, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            float x = (float) vh.getAndAddRelease(null, 0, 1.0f);
        });
        checkCCE(() -> { // array reference class
            float x = (float) vh.getAndAddRelease(Void.class, 0, 1.0f);
        });
        checkWMTE(() -> { // value reference class
            float x = (float) vh.getAndAddRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            float x = (float) vh.getAndAddRelease(0, 0, 1.0f);
        });
        checkWMTE(() -> { // index reference class
            float x = (float) vh.getAndAddRelease(array, Void.class, 1.0f);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(array, 0, 1.0f);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(array, 0, 1.0f);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            float x = (float) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            float x = (float) vh.getAndAddRelease(array, 0, 1.0f, Void.class);
        });

    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        float[] array = new float[10];
        Arrays.fill(array, 1.0f);

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class)).
                    invokeExact((float[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, float[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, float[].class, int.class, float.class)).
                    invokeExact((float[]) null, 0, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, float.class)).
                    invokeExact(Void.class, 0, 1.0f);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, float[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, float.class)).
                    invokeExact(0, 0, 1.0f);
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, float[].class, Class.class, float.class)).
                    invokeExact(array, Void.class, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, float[].class, int.class, Class.class)).
                    invokeExact(array, 0, 1.0f, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, float.class, float.class)).
                    invokeExact((float[]) null, 0, 1.0f, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, float.class, float.class)).
                    invokeExact(Void.class, 0, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, Class.class, float.class)).
                    invokeExact(array, 0, Void.class, 1.0f);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, float.class, Class.class)).
                    invokeExact(array, 0, 1.0f, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, float.class, float.class)).
                    invokeExact(0, 0, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float[].class, Class.class, float.class, float.class)).
                    invokeExact(array, Void.class, 1.0f, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, float.class, float.class, Class.class)).
                    invokeExact(array, 0, 1.0f, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class, float.class)).
                    invokeExact((float[]) null, 0, 1.0f, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, int.class, float.class, float.class)).
                    invokeExact(Void.class, 0, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // expected reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, Class.class, float.class)).
                    invokeExact(array, 0, Void.class, 1.0f);
            });
            checkWMTE(() -> { // actual reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class, Class.class)).
                    invokeExact(array, 0, 1.0f, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class, int.class, float.class, float.class)).
                    invokeExact(0, 0, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // index reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, Class.class, float.class, float.class)).
                    invokeExact(array, Void.class, 1.0f, 1.0f);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, float[].class, int.class, float.class, float.class)).
                    invokeExact(array, 0, 1.0f, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, float.class, float.class)).
                    invokeExact(array, 0, 1.0f, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class, float.class, Class.class)).
                    invokeExact(array, 0, 1.0f, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class)).
                    invokeExact((float[]) null, 0, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, int.class, float.class)).
                    invokeExact(Void.class, 0, 1.0f);
            });
            checkWMTE(() -> { // value reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class, int.class, float.class)).
                    invokeExact(0, 0, 1.0f);
            });
            checkWMTE(() -> { // index reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, Class.class, float.class)).
                    invokeExact(array, Void.class, 1.0f);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, float[].class, int.class, float.class)).
                    invokeExact(array, 0, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, float.class)).
                    invokeExact(array, 0, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class, Class.class)).
                    invokeExact(array, 0, 1.0f, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class)).
                    invokeExact((float[]) null, 0, 1.0f);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                float x = (float) hs.get(am, methodType(float.class, Class.class, int.class, float.class)).
                    invokeExact(Void.class, 0, 1.0f);
            });
            checkWMTE(() -> { // value reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                float x = (float) hs.get(am, methodType(float.class, int.class, int.class, float.class)).
                    invokeExact(0, 0, 1.0f);
            });
            checkWMTE(() -> { // index reference class
                float x = (float) hs.get(am, methodType(float.class, float[].class, Class.class, float.class)).
                    invokeExact(array, Void.class, 1.0f);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, float[].class, int.class, float.class)).
                    invokeExact(array, 0, 1.0f);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, float[].class, int.class, float.class)).
                    invokeExact(array, 0, 1.0f);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                float x = (float) hs.get(am, methodType(float.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                float x = (float) hs.get(am, methodType(float.class, float[].class, int.class, float.class, Class.class)).
                    invokeExact(array, 0, 1.0f, Void.class);
            });
        }

    }
}
