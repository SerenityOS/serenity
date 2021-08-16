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
 * @run testng/othervm VarHandleTestMethodTypeDouble
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeDouble
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeDouble
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeDouble
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

public class VarHandleTestMethodTypeDouble extends VarHandleBaseTest {
    static final double static_final_v = 1.0d;

    static double static_v = 1.0d;

    final double final_v = 1.0d;

    double v = 1.0d;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeDouble.class, "final_v", double.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeDouble.class, "v", double.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeDouble.class, "static_final_v", double.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeDouble.class, "static_v", double.class);

        vhArray = MethodHandles.arrayElementVarHandle(double[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeDouble::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeDouble::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeDouble::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeDouble::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeDouble recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            double x = (double) vh.get(0);
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
            double x = (double) vh.get();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, 1.0d, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            double x = (double) vh.getVolatile(0);
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
            double x = (double) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, 1.0d, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            double x = (double) vh.getOpaque(0);
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
            double x = (double) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, 1.0d, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            double x = (double) vh.getAcquire(0);
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
            double x = (double) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, 1.0d, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, 1.0d, 1.0d, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.compareAndExchange(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.compareAndExchange(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchange(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchange(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.compareAndExchange(0, 1.0d, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(recv, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchange(recv, 1.0d, 1.0d, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.compareAndExchangeAcquire(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.compareAndExchangeAcquire(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchangeAcquire(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchangeAcquire(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.compareAndExchangeAcquire(0, 1.0d, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchangeAcquire(recv, 1.0d, 1.0d, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.compareAndExchangeRelease(null, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.compareAndExchangeRelease(Void.class, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchangeRelease(recv, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchangeRelease(recv, 1.0d, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.compareAndExchangeRelease(0, 1.0d, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchangeRelease(recv, 1.0d, 1.0d, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAndSet(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAndSet(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.getAndSet(0, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(recv, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSet(recv, 1.0d, Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAndSetAcquire(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAndSetAcquire(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.getAndSetAcquire(0, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(recv, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSetAcquire(recv, 1.0d, Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAndSetRelease(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAndSetRelease(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.getAndSetRelease(0, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(recv, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSetRelease(recv, 1.0d, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAndAdd(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAndAdd(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAdd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.getAndAdd(0, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(recv, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(recv, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAdd(recv, 1.0d, Void.class);
        });

        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAndAddAcquire(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAndAddAcquire(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAddAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.getAndAddAcquire(0, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(recv, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(recv, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAddAcquire(recv, 1.0d, Void.class);
        });

        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.getAndAddRelease(null, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            double x = (double) vh.getAndAddRelease(Void.class, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAddRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            double x = (double) vh.getAndAddRelease(0, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(recv, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(recv, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAddRelease(recv, 1.0d, Void.class);
        });

    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeDouble recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class)).
                    invokeExact((VarHandleTestMethodTypeDouble) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeDouble.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact((VarHandleTestMethodTypeDouble) null, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, double.class)).
                    invokeExact(Void.class, 1.0d);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeDouble.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, double.class)).
                    invokeExact(0, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeDouble.class, double.class, Class.class)).
                    invokeExact(recv, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class, double.class, double.class)).
                    invokeExact((VarHandleTestMethodTypeDouble) null, 1.0d, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, double.class, double.class)).
                    invokeExact(Void.class, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class, Class.class, double.class)).
                    invokeExact(recv, Void.class, 1.0d);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class, double.class, Class.class)).
                    invokeExact(recv, 1.0d, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , double.class, double.class)).
                    invokeExact(0, 1.0d, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class, double.class, double.class, Class.class)).
                    invokeExact(recv, 1.0d, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class, double.class)).
                    invokeExact((VarHandleTestMethodTypeDouble) null, 1.0d, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, double.class, double.class)).
                    invokeExact(Void.class, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // expected reference class
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, Class.class, double.class)).
                    invokeExact(recv, Void.class, 1.0d);
            });
            checkWMTE(() -> { // actual reference class
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class, Class.class)).
                    invokeExact(recv, 1.0d, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class , double.class, double.class)).
                    invokeExact(0, 1.0d, 1.0d);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeDouble.class , double.class, double.class)).
                    invokeExact(recv, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class , double.class, double.class)).
                    invokeExact(recv, 1.0d, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class, double.class, Class.class)).
                    invokeExact(recv, 1.0d, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact((VarHandleTestMethodTypeDouble) null, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, double.class)).
                    invokeExact(Void.class, 1.0d);
            });
            checkWMTE(() -> { // value reference class
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class, double.class)).
                    invokeExact(0, 1.0d);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact(recv, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact(recv, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact(recv, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { // null receiver
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact((VarHandleTestMethodTypeDouble) null, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, double.class)).
                    invokeExact(Void.class, 1.0d);
            });
            checkWMTE(() -> { // value reference class
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class, double.class)).
                    invokeExact(0, 1.0d);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact(recv, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact(recv, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, VarHandleTestMethodTypeDouble.class, double.class)).
                    invokeExact(recv, 1.0d, Void.class);
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
            double x = (double) vh.get(Void.class);
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
            vh.set(1.0d, Void.class);
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
            double x = (double) vh.getVolatile(Void.class);
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
            vh.setVolatile(1.0d, Void.class);
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
            double x = (double) vh.getOpaque(Void.class);
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
            vh.setOpaque(1.0d, Void.class);
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
            double x = (double) vh.getAcquire(Void.class);
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
            vh.setRelease(1.0d, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(1.0d, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(1.0d, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(1.0d, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(1.0d, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(1.0d, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(1.0d, 1.0d, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchange(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchange(1.0d, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchange(1.0d, 1.0d, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchangeAcquire(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchangeAcquire(1.0d, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchangeAcquire(1.0d, 1.0d, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchangeRelease(Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchangeRelease(1.0d, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchangeRelease(1.0d, 1.0d, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSet(1.0d, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSetAcquire(1.0d, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSetRelease(1.0d, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAdd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAdd(1.0d, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAddAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAddAcquire(1.0d, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAddRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAddRelease(1.0d, Void.class);
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
                double x = (double) hs.get(am, methodType(Class.class)).
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
                hs.get(am, methodType(void.class, double.class, Class.class)).
                    invokeExact(1.0d, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, double.class)).
                    invokeExact(Void.class, 1.0d);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double.class, Class.class)).
                    invokeExact(1.0d, Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double.class, double.class, Class.class)).
                    invokeExact(1.0d, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, double.class)).
                    invokeExact(Void.class, 1.0d);
            });
            checkWMTE(() -> { // actual reference class
                double x = (double) hs.get(am, methodType(double.class, double.class, Class.class)).
                    invokeExact(1.0d, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, double.class, double.class)).
                    invokeExact(1.0d, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double.class, double.class)).
                    invokeExact(1.0d, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double.class, double.class, Class.class)).
                    invokeExact(1.0d, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, double.class)).
                    invokeExact(1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double.class)).
                    invokeExact(1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double.class, Class.class)).
                    invokeExact(1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, double.class)).
                    invokeExact(1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double.class)).
                    invokeExact(1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double.class, Class.class)).
                    invokeExact(1.0d, Void.class);
            });
        }

    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        double[] array = new double[10];
        Arrays.fill(array, 1.0d);

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.get(array, Void.class);
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
            double x = (double) vh.get();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, 1.0d, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getVolatile(array, Void.class);
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
            double x = (double) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, 1.0d, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getOpaque(array, Void.class);
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
            double x = (double) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, 1.0d, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAcquire(array, Void.class);
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
            double x = (double) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, 1.0d, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, 1.0d, 1.0d, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, 1.0d, 1.0d, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.compareAndExchange(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.compareAndExchange(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchange(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchange(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.compareAndExchange(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.compareAndExchange(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(array, 0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchange(array, 0, 1.0d, 1.0d, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.compareAndExchangeAcquire(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.compareAndExchangeAcquire(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchangeAcquire(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchangeAcquire(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.compareAndExchangeAcquire(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.compareAndExchangeAcquire(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchangeAcquire(array, 0, 1.0d, 1.0d, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            double x = (double) vh.compareAndExchangeRelease(null, 0, 1.0d, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.compareAndExchangeRelease(Void.class, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // expected reference class
            double x = (double) vh.compareAndExchangeRelease(array, 0, Void.class, 1.0d);
        });
        checkWMTE(() -> { // actual reference class
            double x = (double) vh.compareAndExchangeRelease(array, 0, 1.0d, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.compareAndExchangeRelease(0, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.compareAndExchangeRelease(array, Void.class, 1.0d, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, 1.0d, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, 1.0d, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.compareAndExchangeRelease(array, 0, 1.0d, 1.0d, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAndSet(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAndSet(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            double x = (double) vh.getAndSet(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAndSet(array, Void.class, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(array, 0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSet(array, 0, 1.0d, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAndSetAcquire(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAndSetAcquire(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            double x = (double) vh.getAndSetAcquire(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAndSetAcquire(array, Void.class, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSetAcquire(array, 0, 1.0d, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAndSetRelease(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAndSetRelease(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            double x = (double) vh.getAndSetRelease(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAndSetRelease(array, Void.class, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndSetRelease(array, 0, 1.0d, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAndAdd(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAndAdd(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAdd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.getAndAdd(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAndAdd(array, Void.class, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(array, 0, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(array, 0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAdd(array, 0, 1.0d, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAndAddAcquire(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAndAddAcquire(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAddAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.getAndAddAcquire(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAndAddAcquire(array, Void.class, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(array, 0, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(array, 0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAddAcquire(array, 0, 1.0d, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            double x = (double) vh.getAndAddRelease(null, 0, 1.0d);
        });
        checkCCE(() -> { // array reference class
            double x = (double) vh.getAndAddRelease(Void.class, 0, 1.0d);
        });
        checkWMTE(() -> { // value reference class
            double x = (double) vh.getAndAddRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            double x = (double) vh.getAndAddRelease(0, 0, 1.0d);
        });
        checkWMTE(() -> { // index reference class
            double x = (double) vh.getAndAddRelease(array, Void.class, 1.0d);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(array, 0, 1.0d);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(array, 0, 1.0d);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            double x = (double) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            double x = (double) vh.getAndAddRelease(array, 0, 1.0d, Void.class);
        });

    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        double[] array = new double[10];
        Arrays.fill(array, 1.0d);

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class)).
                    invokeExact((double[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, double[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, double[].class, int.class, double.class)).
                    invokeExact((double[]) null, 0, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, double.class)).
                    invokeExact(Void.class, 0, 1.0d);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, double[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, double.class)).
                    invokeExact(0, 0, 1.0d);
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, double[].class, Class.class, double.class)).
                    invokeExact(array, Void.class, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, double[].class, int.class, Class.class)).
                    invokeExact(array, 0, 1.0d, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, double.class, double.class)).
                    invokeExact((double[]) null, 0, 1.0d, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, double.class, double.class)).
                    invokeExact(Void.class, 0, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, Class.class, double.class)).
                    invokeExact(array, 0, Void.class, 1.0d);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, double.class, Class.class)).
                    invokeExact(array, 0, 1.0d, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, double.class, double.class)).
                    invokeExact(0, 0, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double[].class, Class.class, double.class, double.class)).
                    invokeExact(array, Void.class, 1.0d, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, double.class, double.class, Class.class)).
                    invokeExact(array, 0, 1.0d, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class, double.class)).
                    invokeExact((double[]) null, 0, 1.0d, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, int.class, double.class, double.class)).
                    invokeExact(Void.class, 0, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // expected reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, Class.class, double.class)).
                    invokeExact(array, 0, Void.class, 1.0d);
            });
            checkWMTE(() -> { // actual reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class, Class.class)).
                    invokeExact(array, 0, 1.0d, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class, int.class, double.class, double.class)).
                    invokeExact(0, 0, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // index reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, Class.class, double.class, double.class)).
                    invokeExact(array, Void.class, 1.0d, 1.0d);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, double[].class, int.class, double.class, double.class)).
                    invokeExact(array, 0, 1.0d, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, double.class, double.class)).
                    invokeExact(array, 0, 1.0d, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class, double.class, Class.class)).
                    invokeExact(array, 0, 1.0d, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class)).
                    invokeExact((double[]) null, 0, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, int.class, double.class)).
                    invokeExact(Void.class, 0, 1.0d);
            });
            checkWMTE(() -> { // value reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class, int.class, double.class)).
                    invokeExact(0, 0, 1.0d);
            });
            checkWMTE(() -> { // index reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, Class.class, double.class)).
                    invokeExact(array, Void.class, 1.0d);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, double[].class, int.class, double.class)).
                    invokeExact(array, 0, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, double.class)).
                    invokeExact(array, 0, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class, Class.class)).
                    invokeExact(array, 0, 1.0d, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class)).
                    invokeExact((double[]) null, 0, 1.0d);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                double x = (double) hs.get(am, methodType(double.class, Class.class, int.class, double.class)).
                    invokeExact(Void.class, 0, 1.0d);
            });
            checkWMTE(() -> { // value reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                double x = (double) hs.get(am, methodType(double.class, int.class, int.class, double.class)).
                    invokeExact(0, 0, 1.0d);
            });
            checkWMTE(() -> { // index reference class
                double x = (double) hs.get(am, methodType(double.class, double[].class, Class.class, double.class)).
                    invokeExact(array, Void.class, 1.0d);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, double[].class, int.class, double.class)).
                    invokeExact(array, 0, 1.0d);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, double[].class, int.class, double.class)).
                    invokeExact(array, 0, 1.0d);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                double x = (double) hs.get(am, methodType(double.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                double x = (double) hs.get(am, methodType(double.class, double[].class, int.class, double.class, Class.class)).
                    invokeExact(array, 0, 1.0d, Void.class);
            });
        }

    }
}
