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
 * @run testng/othervm VarHandleTestMethodTypeByte
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeByte
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeByte
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeByte
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

public class VarHandleTestMethodTypeByte extends VarHandleBaseTest {
    static final byte static_final_v = (byte)0x01;

    static byte static_v = (byte)0x01;

    final byte final_v = (byte)0x01;

    byte v = (byte)0x01;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeByte.class, "final_v", byte.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeByte.class, "v", byte.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeByte.class, "static_final_v", byte.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeByte.class, "static_v", byte.class);

        vhArray = MethodHandles.arrayElementVarHandle(byte[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeByte::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeByte::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeByte::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeByte::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeByte recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            byte x = (byte) vh.get(0);
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
            byte x = (byte) vh.get();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, (byte)0x01, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            byte x = (byte) vh.getVolatile(0);
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
            byte x = (byte) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, (byte)0x01, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            byte x = (byte) vh.getOpaque(0);
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
            byte x = (byte) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, (byte)0x01, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            byte x = (byte) vh.getAcquire(0);
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
            byte x = (byte) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, (byte)0x01, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.compareAndExchange(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.compareAndExchange(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchange(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchange(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.compareAndExchange(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(recv, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchange(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.compareAndExchangeAcquire(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.compareAndExchangeAcquire(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchangeAcquire(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchangeAcquire(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.compareAndExchangeAcquire(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchangeAcquire(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.compareAndExchangeRelease(null, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.compareAndExchangeRelease(Void.class, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchangeRelease(recv, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchangeRelease(recv, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.compareAndExchangeRelease(0, (byte)0x01, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchangeRelease(recv, (byte)0x01, (byte)0x01, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndSet(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndSet(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndSet(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSet(recv, (byte)0x01, Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndSetAcquire(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndSetAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndSetAcquire(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSetAcquire(recv, (byte)0x01, Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndSetRelease(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndSetRelease(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndSetRelease(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSetRelease(recv, (byte)0x01, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndAdd(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndAdd(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAdd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndAdd(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAdd(recv, (byte)0x01, Void.class);
        });

        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndAddAcquire(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndAddAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAddAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndAddAcquire(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAddAcquire(recv, (byte)0x01, Void.class);
        });

        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndAddRelease(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndAddRelease(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAddRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndAddRelease(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAddRelease(recv, (byte)0x01, Void.class);
        });

        // GetAndBitwiseOr
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseOr(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseOr(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseOr(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOr(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseOrAcquire(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseOrAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOrAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseOrAcquire(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOrAcquire(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseOrRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseOrRelease(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseOr(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOr(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseOr(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOr(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseAnd(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseAnd(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseAnd(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAnd(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseAndAcquire(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseAndAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAndAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseAndAcquire(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAndAcquire(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseAndRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseAndRelease(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseAnd(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAnd(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseAnd(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAnd(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseXor(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseXor(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseXor(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXor(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseXorAcquire(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseXorAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXorAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseXorAcquire(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXorAcquire(recv, (byte)0x01, Void.class);
        });


        // GetAndBitwiseXorRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.getAndBitwiseXorRelease(null, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            byte x = (byte) vh.getAndBitwiseXor(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXor(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            byte x = (byte) vh.getAndBitwiseXor(0, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(recv, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(recv, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXor(recv, (byte)0x01, Void.class);
        });
    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeByte recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeByte.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeByte.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, byte.class)).
                    invokeExact(0, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeByte.class, byte.class, Class.class)).
                    invokeExact(recv, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, byte.class, byte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null, (byte)0x01, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, byte.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, Class.class, byte.class)).
                    invokeExact(recv, Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, byte.class, Class.class)).
                    invokeExact(recv, (byte)0x01, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , byte.class, byte.class)).
                    invokeExact(0, (byte)0x01, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, byte.class, byte.class, Class.class)).
                    invokeExact(recv, (byte)0x01, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class, byte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null, (byte)0x01, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, byte.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // expected reference class
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, Class.class, byte.class)).
                    invokeExact(recv, Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // actual reference class
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class, Class.class)).
                    invokeExact(recv, (byte)0x01, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class , byte.class, byte.class)).
                    invokeExact(0, (byte)0x01, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeByte.class , byte.class, byte.class)).
                    invokeExact(recv, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class , byte.class, byte.class)).
                    invokeExact(recv, (byte)0x01, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class, byte.class, Class.class)).
                    invokeExact(recv, (byte)0x01, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, byte.class)).
                    invokeExact(0, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkNPE(() -> { // null receiver
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, byte.class)).
                    invokeExact(0, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkNPE(() -> { // null receiver
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact((VarHandleTestMethodTypeByte) null, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, byte.class)).
                    invokeExact(0, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, VarHandleTestMethodTypeByte.class, byte.class)).
                    invokeExact(recv, (byte)0x01, Void.class);
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
            byte x = (byte) vh.get(Void.class);
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
            vh.set((byte)0x01, Void.class);
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
            byte x = (byte) vh.getVolatile(Void.class);
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
            vh.setVolatile((byte)0x01, Void.class);
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
            byte x = (byte) vh.getOpaque(Void.class);
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
            vh.setOpaque((byte)0x01, Void.class);
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
            byte x = (byte) vh.getAcquire(Void.class);
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
            vh.setRelease((byte)0x01, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet((byte)0x01, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet((byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain((byte)0x01, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain((byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet((byte)0x01, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet((byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire((byte)0x01, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire((byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease((byte)0x01, Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease((byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchange(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchange((byte)0x01, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange((byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange((byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchange((byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchangeAcquire(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchangeAcquire((byte)0x01, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire((byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire((byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchangeAcquire((byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchangeRelease(Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchangeRelease((byte)0x01, Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease((byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease((byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchangeRelease((byte)0x01, (byte)0x01, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSet((byte)0x01, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSetAcquire((byte)0x01, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSetRelease((byte)0x01, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAdd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAdd((byte)0x01, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAddAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAddAcquire((byte)0x01, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAddRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAddRelease((byte)0x01, Void.class);
        });

        // GetAndBitwiseOr
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOr(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOr((byte)0x01, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOrAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOrAcquire((byte)0x01, Void.class);
        });


        // GetAndBitwiseOrReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOrRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrRelease((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrRelease((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOrRelease((byte)0x01, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAnd(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAnd((byte)0x01, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAndAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAndAcquire((byte)0x01, Void.class);
        });


        // GetAndBitwiseAndReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAndRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndRelease((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndRelease((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAndRelease((byte)0x01, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXor(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXor((byte)0x01, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXorAcquire(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXorAcquire((byte)0x01, Void.class);
        });


        // GetAndBitwiseXorReleaseRelease
        // Incorrect argument types
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXorRelease(Void.class);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorRelease((byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorRelease((byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXorRelease((byte)0x01, Void.class);
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
                byte x = (byte) hs.get(am, methodType(Class.class)).
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
                hs.get(am, methodType(void.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkWMTE(() -> { // expected reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, byte.class)).
                    invokeExact(Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // actual reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte.class, byte.class)).
                    invokeExact((byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte.class, byte.class)).
                    invokeExact((byte)0x01, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte.class)).
                    invokeExact((byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte.class)).
                    invokeExact((byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte.class)).
                    invokeExact((byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte.class)).
                    invokeExact((byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            // Incorrect argument types
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte.class)).
                    invokeExact((byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte.class)).
                    invokeExact((byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte.class, Class.class)).
                    invokeExact((byte)0x01, Void.class);
            });
        }
    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        byte[] array = new byte[10];
        Arrays.fill(array, (byte)0x01);

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.get(array, Void.class);
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
            byte x = (byte) vh.get();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, (byte)0x01, Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getVolatile(array, Void.class);
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
            byte x = (byte) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, (byte)0x01, Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getOpaque(array, Void.class);
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
            byte x = (byte) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, (byte)0x01, Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAcquire(array, Void.class);
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
            byte x = (byte) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, (byte)0x01, Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.compareAndExchange(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.compareAndExchange(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchange(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchange(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.compareAndExchange(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.compareAndExchange(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(array, 0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchange(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.compareAndExchangeAcquire(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.compareAndExchangeAcquire(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchangeAcquire(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchangeAcquire(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.compareAndExchangeAcquire(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.compareAndExchangeAcquire(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchangeAcquire(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            byte x = (byte) vh.compareAndExchangeRelease(null, 0, (byte)0x01, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.compareAndExchangeRelease(Void.class, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // expected reference class
            byte x = (byte) vh.compareAndExchangeRelease(array, 0, Void.class, (byte)0x01);
        });
        checkWMTE(() -> { // actual reference class
            byte x = (byte) vh.compareAndExchangeRelease(array, 0, (byte)0x01, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.compareAndExchangeRelease(0, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.compareAndExchangeRelease(array, Void.class, (byte)0x01, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, (byte)0x01, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, (byte)0x01, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.compareAndExchangeRelease(array, 0, (byte)0x01, (byte)0x01, Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndSet(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndSet(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            byte x = (byte) vh.getAndSet(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndSet(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSet(array, 0, (byte)0x01, Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndSetAcquire(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndSetAcquire(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            byte x = (byte) vh.getAndSetAcquire(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndSetAcquire(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSetAcquire(array, 0, (byte)0x01, Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndSetRelease(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndSetRelease(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            byte x = (byte) vh.getAndSetRelease(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndSetRelease(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndSetRelease(array, 0, (byte)0x01, Void.class);
        });

        // GetAndAdd
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndAdd(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndAdd(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAdd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndAdd(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndAdd(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAdd(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAdd(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAdd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAdd(array, 0, (byte)0x01, Void.class);
        });


        // GetAndAddAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndAddAcquire(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndAddAcquire(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAddAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndAddAcquire(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndAddAcquire(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddAcquire(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddAcquire(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAddAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAddAcquire(array, 0, (byte)0x01, Void.class);
        });


        // GetAndAddRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndAddRelease(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndAddRelease(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndAddRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndAddRelease(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndAddRelease(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndAddRelease(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndAddRelease(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndAddRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndAddRelease(array, 0, (byte)0x01, Void.class);
        });

        // GetAndBitwiseOr
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseOr(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseOr(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOr(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseOr(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseOr(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOr(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOr(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOr();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOr(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseOrAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseOrAcquire(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseOrAcquire(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOrAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseOrAcquire(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseOrAcquire(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrAcquire(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrAcquire(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOrAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOrAcquire(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseOrRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseOrRelease(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseOrRelease(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseOrRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseOrRelease(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseOrRelease(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseOrRelease(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseOrRelease(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseOrRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseOrRelease(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseAnd
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseAnd(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseAnd(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAnd(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseAnd(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseAnd(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAnd(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAnd(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAnd();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAnd(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseAndAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseAndAcquire(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseAndAcquire(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAndAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseAndAcquire(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseAndAcquire(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndAcquire(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndAcquire(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAndAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAndAcquire(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseAndRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseAndRelease(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseAndRelease(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseAndRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseAndRelease(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseAndRelease(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseAndRelease(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseAndRelease(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseAndRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseAndRelease(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseXor
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseXor(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseXor(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXor(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseXor(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseXor(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXor(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXor(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXor();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXor(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseXorAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseXorAcquire(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseXorAcquire(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXorAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseXorAcquire(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseXorAcquire(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorAcquire(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorAcquire(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXorAcquire();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXorAcquire(array, 0, (byte)0x01, Void.class);
        });


        // GetAndBitwiseXorRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            byte x = (byte) vh.getAndBitwiseXorRelease(null, 0, (byte)0x01);
        });
        checkCCE(() -> { // array reference class
            byte x = (byte) vh.getAndBitwiseXorRelease(Void.class, 0, (byte)0x01);
        });
        checkWMTE(() -> { // value reference class
            byte x = (byte) vh.getAndBitwiseXorRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // array primitive class
            byte x = (byte) vh.getAndBitwiseXorRelease(0, 0, (byte)0x01);
        });
        checkWMTE(() -> { // index reference class
            byte x = (byte) vh.getAndBitwiseXorRelease(array, Void.class, (byte)0x01);
        });
        // Incorrect return type
        checkWMTE(() -> { // reference class
            Void r = (Void) vh.getAndBitwiseXorRelease(array, 0, (byte)0x01);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndBitwiseXorRelease(array, 0, (byte)0x01);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            byte x = (byte) vh.getAndBitwiseXorRelease();
        });
        checkWMTE(() -> { // >
            byte x = (byte) vh.getAndBitwiseXorRelease(array, 0, (byte)0x01, Void.class);
        });
    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        byte[] array = new byte[10];
        Arrays.fill(array, (byte)0x01);

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class)).
                    invokeExact((byte[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, byte[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, byte[].class, int.class, byte.class)).
                    invokeExact((byte[]) null, 0, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, byte.class)).
                    invokeExact(Void.class, 0, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                hs.get(am, methodType(void.class, byte[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, byte.class)).
                    invokeExact(0, 0, (byte)0x01);
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, byte[].class, Class.class, byte.class)).
                    invokeExact(array, Void.class, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, byte[].class, int.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class, byte.class)).
                    invokeExact((byte[]) null, 0, (byte)0x01, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, byte.class, byte.class)).
                    invokeExact(Void.class, 0, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, Class.class, byte.class)).
                    invokeExact(array, 0, Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, byte.class, byte.class)).
                    invokeExact(0, 0, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte[].class, Class.class, byte.class, byte.class)).
                    invokeExact(array, Void.class, (byte)0x01, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class, byte.class)).
                    invokeExact((byte[]) null, 0, (byte)0x01, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, int.class, byte.class, byte.class)).
                    invokeExact(Void.class, 0, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // expected reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, Class.class, byte.class)).
                    invokeExact(array, 0, Void.class, (byte)0x01);
            });
            checkWMTE(() -> { // actual reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, int.class, byte.class, byte.class)).
                    invokeExact(0, 0, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // index reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, Class.class, byte.class, byte.class)).
                    invokeExact(array, Void.class, (byte)0x01, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte[].class, int.class, byte.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class)).
                    invokeExact((byte[]) null, 0, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, int.class, byte.class)).
                    invokeExact(Void.class, 0, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, int.class, byte.class)).
                    invokeExact(0, 0, (byte)0x01);
            });
            checkWMTE(() -> { // index reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, Class.class, byte.class)).
                    invokeExact(array, Void.class, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte[].class, int.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class)).
                    invokeExact((byte[]) null, 0, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, int.class, byte.class)).
                    invokeExact(Void.class, 0, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, int.class, byte.class)).
                    invokeExact(0, 0, (byte)0x01);
            });
            checkWMTE(() -> { // index reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, Class.class, byte.class)).
                    invokeExact(array, Void.class, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte[].class, int.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class)).
                    invokeExact((byte[]) null, 0, (byte)0x01);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                byte x = (byte) hs.get(am, methodType(byte.class, Class.class, int.class, byte.class)).
                    invokeExact(Void.class, 0, (byte)0x01);
            });
            checkWMTE(() -> { // value reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                byte x = (byte) hs.get(am, methodType(byte.class, int.class, int.class, byte.class)).
                    invokeExact(0, 0, (byte)0x01);
            });
            checkWMTE(() -> { // index reference class
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, Class.class, byte.class)).
                    invokeExact(array, Void.class, (byte)0x01);
            });
            // Incorrect return type
            checkWMTE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, byte[].class, int.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, byte[].class, int.class, byte.class)).
                    invokeExact(array, 0, (byte)0x01);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                byte x = (byte) hs.get(am, methodType(byte.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                byte x = (byte) hs.get(am, methodType(byte.class, byte[].class, int.class, byte.class, Class.class)).
                    invokeExact(array, 0, (byte)0x01, Void.class);
            });
        }
    }
}
