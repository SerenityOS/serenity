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
 * @run testng/othervm VarHandleTestMethodTypeString
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=true -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeString
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=false VarHandleTestMethodTypeString
 * @run testng/othervm -Djava.lang.invoke.VarHandle.VAR_HANDLE_GUARDS=false -Djava.lang.invoke.VarHandle.VAR_HANDLE_IDENTITY_ADAPT=true VarHandleTestMethodTypeString
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

public class VarHandleTestMethodTypeString extends VarHandleBaseTest {
    static final String static_final_v = "foo";

    static String static_v = "foo";

    final String final_v = "foo";

    String v = "foo";

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeString.class, "final_v", String.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodTypeString.class, "v", String.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeString.class, "static_final_v", String.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodTypeString.class, "static_v", String.class);

        vhArray = MethodHandles.arrayElementVarHandle(String[].class);
    }

    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceFieldWrongMethodType(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestMethodTypeString::testStaticFieldWrongMethodType,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestMethodTypeString::testArrayWrongMethodType,
                                              false));

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceFieldWrongMethodType(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodTypeString::testStaticFieldWrongMethodType,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodTypeString::testArrayWrongMethodType,
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


    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeString recv, VarHandle vh) throws Throwable {
        // Get
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.get(null);
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.get(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            String x = (String) vh.get(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.get(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.get();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.get(recv, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.set(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            vh.set(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.set(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(recv, "foo", Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.getVolatile(null);
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.getVolatile(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            String x = (String) vh.getVolatile(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getVolatile(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getVolatile(recv, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setVolatile(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            vh.setVolatile(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.setVolatile(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(recv, "foo", Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.getOpaque(null);
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.getOpaque(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            String x = (String) vh.getOpaque(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getOpaque(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getOpaque(recv, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setOpaque(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            vh.setOpaque(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.setOpaque(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(recv, "foo", Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.getAcquire(null);
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.getAcquire(Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            String x = (String) vh.getAcquire(0);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getAcquire(recv);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire(recv);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAcquire(recv, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            vh.setRelease(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            vh.setRelease(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.setRelease(recv, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(recv, "foo", Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.compareAndSet(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.compareAndSet(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(recv, "foo", "foo", Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(recv, "foo", "foo", Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(recv, "foo", "foo", Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(recv, "foo", "foo", Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(recv, "foo", "foo", Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.compareAndExchange(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.compareAndExchange(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchange(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchange(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            String x = (String) vh.compareAndExchange(0, "foo", "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(recv, "foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(recv, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchange(recv, "foo", "foo", Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.compareAndExchangeAcquire(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.compareAndExchangeAcquire(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchangeAcquire(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchangeAcquire(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            String x = (String) vh.compareAndExchangeAcquire(0, "foo", "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(recv, "foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(recv, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchangeAcquire(recv, "foo", "foo", Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.compareAndExchangeRelease(null, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.compareAndExchangeRelease(Void.class, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchangeRelease(recv, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchangeRelease(recv, "foo", Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            String x = (String) vh.compareAndExchangeRelease(0, "foo", "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(recv, "foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(recv, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchangeRelease(recv, "foo", "foo", Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.getAndSet(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.getAndSet(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSet(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            String x = (String) vh.getAndSet(0, "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSet(recv, "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(recv, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSet(recv, "foo", Void.class);
        });

        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.getAndSetAcquire(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.getAndSetAcquire(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSetAcquire(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            String x = (String) vh.getAndSetAcquire(0, "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(recv, "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(recv, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSetAcquire(recv, "foo", Void.class);
        });

        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.getAndSetRelease(null, "foo");
        });
        checkCCE(() -> { // receiver reference class
            String x = (String) vh.getAndSetRelease(Void.class, "foo");
        });
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSetRelease(recv, Void.class);
        });
        checkWMTE(() -> { // reciever primitive class
            String x = (String) vh.getAndSetRelease(0, "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(recv, "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(recv, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSetRelease(recv, "foo", Void.class);
        });


    }

    static void testInstanceFieldWrongMethodType(VarHandleTestMethodTypeString recv, Handles hs) throws Throwable {
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class)).
                    invokeExact((VarHandleTestMethodTypeString) null);
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class)).
                    invokeExact(Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                String x = (String) hs.get(am, methodType(String.class, int.class)).
                    invokeExact(0);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeString.class)).
                    invokeExact(recv);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class)).
                    invokeExact(recv);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                hs.get(am, methodType(void.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeString.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, String.class)).
                    invokeExact(0, "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, VarHandleTestMethodTypeString.class, String.class, Class.class)).
                    invokeExact(recv, "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, String.class, String.class)).
                    invokeExact(Void.class, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, Class.class, String.class)).
                    invokeExact(recv, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class, Class.class)).
                    invokeExact(recv, "foo", Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class , String.class, String.class)).
                    invokeExact(0, "foo", "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class, String.class, Class.class)).
                    invokeExact(recv, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            checkNPE(() -> { // null receiver
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class, String.class, String.class)).
                    invokeExact(Void.class, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, Class.class, String.class)).
                    invokeExact(recv, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class, Class.class)).
                    invokeExact(recv, "foo", Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                String x = (String) hs.get(am, methodType(String.class, int.class , String.class, String.class)).
                    invokeExact(0, "foo", "foo");
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeString.class , String.class, String.class)).
                    invokeExact(recv, "foo", "foo");
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class , String.class, String.class)).
                    invokeExact(recv, "foo", "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class, String.class, Class.class)).
                    invokeExact(recv, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            checkNPE(() -> { // null receiver
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact((VarHandleTestMethodTypeString) null, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, Class.class)).
                    invokeExact(recv, Void.class);
            });
            checkWMTE(() -> { // reciever primitive class
                String x = (String) hs.get(am, methodType(String.class, int.class, String.class)).
                    invokeExact(0, "foo");
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact(recv, "foo");
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact(recv, "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, VarHandleTestMethodTypeString.class, String.class)).
                    invokeExact(recv, "foo", Void.class);
            });
        }


    }


    static void testStaticFieldWrongMethodType(VarHandle vh) throws Throwable {
        // Get
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.get();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get();
        });
        // Incorrect arity
        checkWMTE(() -> { // >
            String x = (String) vh.get(Void.class);
        });


        // Set
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.set(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set("foo", Void.class);
        });


        // GetVolatile
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getVolatile();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getVolatile(Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.setVolatile(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile("foo", Void.class);
        });


        // GetOpaque
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getOpaque();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getOpaque(Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.setOpaque(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque("foo", Void.class);
        });


        // GetAcquire
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getAcquire();
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAcquire(Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            vh.setRelease(Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease("foo", Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.compareAndSet(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.compareAndSet("foo", Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet("foo", "foo", Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain("foo", Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain("foo", "foo", Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet("foo", Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet("foo", "foo", Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire("foo", Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire("foo", "foo", Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease("foo", Void.class);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease("foo", "foo", Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchange(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchange("foo", Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange("foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange("foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchange("foo", "foo", Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchangeAcquire(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchangeAcquire("foo", Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire("foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire("foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchangeAcquire("foo", "foo", Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchangeRelease(Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchangeRelease("foo", Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease("foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease("foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchangeRelease("foo", "foo", Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSet(Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSet("foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet("foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSet("foo", Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSetAcquire(Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire("foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire("foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSetAcquire("foo", Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSetRelease(Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease("foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease("foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSetRelease("foo", Void.class);
        });


    }

    static void testStaticFieldWrongMethodType(Handles hs) throws Throwable {
        int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            // Incorrect arity
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(Class.class)).
                    invokeExact(Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            hs.checkWMTEOrCCE(() -> { // value reference class
                hs.get(am, methodType(void.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            hs.checkWMTEOrCCE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String.class, String.class, Class.class)).
                    invokeExact("foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            hs.checkWMTEOrCCE(() -> { // expected reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class, String.class)).
                    invokeExact(Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                String x = (String) hs.get(am, methodType(String.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, String.class, String.class)).
                    invokeExact("foo", "foo");
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String.class, String.class)).
                    invokeExact("foo", "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, String.class, String.class, Class.class)).
                    invokeExact("foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            hs.checkWMTEOrCCE(() -> { // value reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class)).
                    invokeExact(Void.class);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, String.class)).
                    invokeExact("foo");
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String.class)).
                    invokeExact("foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, String.class, Class.class)).
                    invokeExact("foo", Void.class);
            });
        }


    }


    static void testArrayWrongMethodType(VarHandle vh) throws Throwable {
        String[] array = new String[10];
        Arrays.fill(array, "foo");

        // Get
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.get(null, 0);
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.get(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.get(0, 0);
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.get(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.get(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.get(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.get();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.get(array, 0, Void.class);
        });


        // Set
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.set(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            vh.set(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.set(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.set(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            vh.set(array, Void.class, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.set();
        });
        checkWMTE(() -> { // >
            vh.set(array, 0, "foo", Void.class);
        });


        // GetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.getVolatile(null, 0);
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.getVolatile(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.getVolatile(0, 0);
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.getVolatile(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getVolatile(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getVolatile(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getVolatile();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getVolatile(array, 0, Void.class);
        });


        // SetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setVolatile(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            vh.setVolatile(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.setVolatile(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setVolatile(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            vh.setVolatile(array, Void.class, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setVolatile();
        });
        checkWMTE(() -> { // >
            vh.setVolatile(array, 0, "foo", Void.class);
        });


        // GetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.getOpaque(null, 0);
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.getOpaque(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.getOpaque(0, 0);
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.getOpaque(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getOpaque(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getOpaque(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getOpaque();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getOpaque(array, 0, Void.class);
        });


        // SetOpaque
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setOpaque(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            vh.setOpaque(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.setOpaque(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setOpaque(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            vh.setOpaque(array, Void.class, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setOpaque();
        });
        checkWMTE(() -> { // >
            vh.setOpaque(array, 0, "foo", Void.class);
        });


        // GetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.getAcquire(null, 0);
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.getAcquire(Void.class, 0);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.getAcquire(0, 0);
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.getAcquire(array, Void.class);
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void x = (Void) vh.getAcquire(array, 0);
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAcquire(array, 0);
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAcquire(array, 0, Void.class);
        });


        // SetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            vh.setRelease(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            vh.setRelease(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            vh.setRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            vh.setRelease(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            vh.setRelease(array, Void.class, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            vh.setRelease();
        });
        checkWMTE(() -> { // >
            vh.setRelease(array, 0, "foo", Void.class);
        });


        // CompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.compareAndSet(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.compareAndSet(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.compareAndSet(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.compareAndSet(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.compareAndSet(array, Void.class, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.compareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.compareAndSet(array, 0, "foo", "foo", Void.class);
        });


        // WeakCompareAndSet
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetPlain(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetPlain(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetPlain(array, Void.class, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetPlain();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetPlain(array, 0, "foo", "foo", Void.class);
        });


        // WeakCompareAndSetVolatile
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSet(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSet(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSet(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSet(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSet(array, Void.class, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSet();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSet(array, 0, "foo", "foo", Void.class);
        });


        // WeakCompareAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetAcquire(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetAcquire(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetAcquire(array, Void.class, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetAcquire();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetAcquire(array, 0, "foo", "foo", Void.class);
        });


        // WeakCompareAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            boolean r = vh.weakCompareAndSetRelease(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // receiver primitive class
            boolean r = vh.weakCompareAndSetRelease(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            boolean r = vh.weakCompareAndSetRelease(array, Void.class, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            boolean r = vh.weakCompareAndSetRelease();
        });
        checkWMTE(() -> { // >
            boolean r = vh.weakCompareAndSetRelease(array, 0, "foo", "foo", Void.class);
        });


        // CompareAndExchange
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.compareAndExchange(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.compareAndExchange(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchange(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchange(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.compareAndExchange(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.compareAndExchange(array, Void.class, "foo", "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchange(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchange(array, 0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchange();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchange(array, 0, "foo", "foo", Void.class);
        });


        // CompareAndExchangeAcquire
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.compareAndExchangeAcquire(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.compareAndExchangeAcquire(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchangeAcquire(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchangeAcquire(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.compareAndExchangeAcquire(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.compareAndExchangeAcquire(array, Void.class, "foo", "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeAcquire(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeAcquire(array, 0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchangeAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchangeAcquire(array, 0, "foo", "foo", Void.class);
        });


        // CompareAndExchangeRelease
        // Incorrect argument types
        checkNPE(() -> { // null receiver
            String x = (String) vh.compareAndExchangeRelease(null, 0, "foo", "foo");
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.compareAndExchangeRelease(Void.class, 0, "foo", "foo");
        });
        checkCCE(() -> { // expected reference class
            String x = (String) vh.compareAndExchangeRelease(array, 0, Void.class, "foo");
        });
        checkCCE(() -> { // actual reference class
            String x = (String) vh.compareAndExchangeRelease(array, 0, "foo", Void.class);
        });
        checkWMTE(() -> { // array primitive class
            String x = (String) vh.compareAndExchangeRelease(0, 0, "foo", "foo");
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.compareAndExchangeRelease(array, Void.class, "foo", "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.compareAndExchangeRelease(array, 0, "foo", "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.compareAndExchangeRelease(array, 0, "foo", "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.compareAndExchangeRelease();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.compareAndExchangeRelease(array, 0, "foo", "foo", Void.class);
        });


        // GetAndSet
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.getAndSet(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.getAndSet(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSet(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            String x = (String) vh.getAndSet(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.getAndSet(array, Void.class, "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSet(array, 0, "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSet(array, 0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSet();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSet(array, 0, "foo", Void.class);
        });


        // GetAndSetAcquire
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.getAndSetAcquire(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.getAndSetAcquire(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSetAcquire(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            String x = (String) vh.getAndSetAcquire(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.getAndSetAcquire(array, Void.class, "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetAcquire(array, 0, "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetAcquire(array, 0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSetAcquire();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSetAcquire(array, 0, "foo", Void.class);
        });


        // GetAndSetRelease
        // Incorrect argument types
        checkNPE(() -> { // null array
            String x = (String) vh.getAndSetRelease(null, 0, "foo");
        });
        checkCCE(() -> { // array reference class
            String x = (String) vh.getAndSetRelease(Void.class, 0, "foo");
        });
        checkCCE(() -> { // value reference class
            String x = (String) vh.getAndSetRelease(array, 0, Void.class);
        });
        checkWMTE(() -> { // reciarrayever primitive class
            String x = (String) vh.getAndSetRelease(0, 0, "foo");
        });
        checkWMTE(() -> { // index reference class
            String x = (String) vh.getAndSetRelease(array, Void.class, "foo");
        });
        // Incorrect return type
        checkCCE(() -> { // reference class
            Void r = (Void) vh.getAndSetRelease(array, 0, "foo");
        });
        checkWMTE(() -> { // primitive class
            boolean x = (boolean) vh.getAndSetRelease(array, 0, "foo");
        });
        // Incorrect arity
        checkWMTE(() -> { // 0
            String x = (String) vh.getAndSetRelease();
        });
        checkWMTE(() -> { // >
            String x = (String) vh.getAndSetRelease(array, 0, "foo", Void.class);
        });


    }

    static void testArrayWrongMethodType(Handles hs) throws Throwable {
        String[] array = new String[10];
        Arrays.fill(array, "foo");

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class)).
                    invokeExact((String[]) null, 0);
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class, int.class)).
                    invokeExact(Void.class, 0);
            });
            checkWMTE(() -> { // array primitive class
                String x = (String) hs.get(am, methodType(String.class, int.class, int.class)).
                    invokeExact(0, 0);
            });
            checkWMTE(() -> { // index reference class
                String x = (String) hs.get(am, methodType(String.class, String[].class, Class.class)).
                    invokeExact(array, Void.class);
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void x = (Void) hs.get(am, methodType(Void.class, String[].class, int.class)).
                    invokeExact(array, 0);
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class)).
                    invokeExact(array, 0);
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                hs.get(am, methodType(void.class, String[].class, int.class, String.class)).
                    invokeExact((String[]) null, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                hs.get(am, methodType(void.class, Class.class, int.class, String.class)).
                    invokeExact(Void.class, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                hs.get(am, methodType(void.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                hs.get(am, methodType(void.class, int.class, int.class, String.class)).
                    invokeExact(0, 0, "foo");
            });
            checkWMTE(() -> { // index reference class
                hs.get(am, methodType(void.class, String[].class, Class.class, String.class)).
                    invokeExact(array, Void.class, "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                hs.get(am, methodType(void.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                hs.get(am, methodType(void.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
        }
        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, String.class)).
                    invokeExact((String[]) null, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // receiver reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, Class.class, int.class, String.class, String.class)).
                    invokeExact(Void.class, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, Class.class, String.class)).
                    invokeExact(array, 0, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
            checkWMTE(() -> { // receiver primitive class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, int.class, int.class, String.class, String.class)).
                    invokeExact(0, 0, "foo", "foo");
            });
            checkWMTE(() -> { // index reference class
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, Class.class, String.class, String.class)).
                    invokeExact(array, Void.class, "foo", "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                boolean r = (boolean) hs.get(am, methodType(boolean.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                boolean r = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            // Incorrect argument types
            checkNPE(() -> { // null receiver
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, String.class)).
                    invokeExact((String[]) null, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class, int.class, String.class, String.class)).
                    invokeExact(Void.class, 0, "foo", "foo");
            });
            hs.checkWMTEOrCCE(() -> { // expected reference class
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, Class.class, String.class)).
                    invokeExact(array, 0, Void.class, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // actual reference class
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
            checkWMTE(() -> { // array primitive class
                String x = (String) hs.get(am, methodType(String.class, int.class, int.class, String.class, String.class)).
                    invokeExact(0, 0, "foo", "foo");
            });
            checkWMTE(() -> { // index reference class
                String x = (String) hs.get(am, methodType(String.class, String[].class, Class.class, String.class, String.class)).
                    invokeExact(array, Void.class, "foo", "foo");
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, String[].class, int.class, String.class, String.class)).
                    invokeExact(array, 0, "foo", "foo");
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class, String.class)).
                    invokeExact(array, 0, "foo", "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", "foo", Void.class);
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
            // Incorrect argument types
            checkNPE(() -> { // null array
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class)).
                    invokeExact((String[]) null, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // array reference class
                String x = (String) hs.get(am, methodType(String.class, Class.class, int.class, String.class)).
                    invokeExact(Void.class, 0, "foo");
            });
            hs.checkWMTEOrCCE(() -> { // value reference class
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, Class.class)).
                    invokeExact(array, 0, Void.class);
            });
            checkWMTE(() -> { // array primitive class
                String x = (String) hs.get(am, methodType(String.class, int.class, int.class, String.class)).
                    invokeExact(0, 0, "foo");
            });
            checkWMTE(() -> { // index reference class
                String x = (String) hs.get(am, methodType(String.class, String[].class, Class.class, String.class)).
                    invokeExact(array, Void.class, "foo");
            });
            // Incorrect return type
            hs.checkWMTEOrCCE(() -> { // reference class
                Void r = (Void) hs.get(am, methodType(Void.class, String[].class, int.class, String.class)).
                    invokeExact(array, 0, "foo");
            });
            checkWMTE(() -> { // primitive class
                boolean x = (boolean) hs.get(am, methodType(boolean.class, String[].class, int.class, String.class)).
                    invokeExact(array, 0, "foo");
            });
            // Incorrect arity
            checkWMTE(() -> { // 0
                String x = (String) hs.get(am, methodType(String.class)).
                    invokeExact();
            });
            checkWMTE(() -> { // >
                String x = (String) hs.get(am, methodType(String.class, String[].class, int.class, String.class, Class.class)).
                    invokeExact(array, 0, "foo", Void.class);
            });
        }


    }
}
