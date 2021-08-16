/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng/othervm -Diters=20000 VarHandleTestMethodHandleAccessFloat
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

public class VarHandleTestMethodHandleAccessFloat extends VarHandleBaseTest {
    static final float static_final_v = 1.0f;

    static float static_v;

    final float final_v = 1.0f;

    float v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessFloat.class, "final_v", float.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessFloat.class, "v", float.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessFloat.class, "static_final_v", float.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessFloat.class, "static_v", float.class);

        vhArray = MethodHandles.arrayElementVarHandle(float[].class);
    }


    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        for (VarHandleToMethodHandle f : VarHandleToMethodHandle.values()) {
            cases.add(new MethodHandleAccessTestCase("Instance field",
                                                     vhField, f, hs -> testInstanceField(this, hs)));
            cases.add(new MethodHandleAccessTestCase("Instance field unsupported",
                                                     vhField, f, hs -> testInstanceFieldUnsupported(this, hs),
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Static field",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessFloat::testStaticField));
            cases.add(new MethodHandleAccessTestCase("Static field unsupported",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessFloat::testStaticFieldUnsupported,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodHandleAccessFloat::testArray));
            cases.add(new MethodHandleAccessTestCase("Array unsupported",
                                                     vhArray, f, VarHandleTestMethodHandleAccessFloat::testArrayUnsupported,
                                                     false));
            cases.add(new MethodHandleAccessTestCase("Array index out of bounds",
                                                     vhArray, f, VarHandleTestMethodHandleAccessFloat::testArrayIndexOutOfBounds,
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


    static void testInstanceField(VarHandleTestMethodHandleAccessFloat recv, Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0f);
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "set float value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(recv, 2.0f);
            float x = (float) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(recv);
            assertEquals(x, 2.0f, "setVolatile float value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(recv, 1.0f);
            float x = (float) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(recv);
            assertEquals(x, 1.0f, "setRelease float value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(recv, 2.0f);
            float x = (float) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(recv);
            assertEquals(x, 2.0f, "setOpaque float value");
        }

        hs.get(TestAccessMode.SET).invokeExact(recv, 1.0f);

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, 1.0f, 2.0f);
            assertEquals(r, true, "success compareAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "success compareAndSet float value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, 1.0f, 3.0f);
            assertEquals(r, false, "failing compareAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "failing compareAndSet float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, 2.0f, 1.0f);
            assertEquals(r, 2.0f, "success compareAndExchange float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "success compareAndExchange float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, 2.0f, 3.0f);
            assertEquals(r, 1.0f, "failing compareAndExchange float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "failing compareAndExchange float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, 1.0f, 2.0f);
            assertEquals(r, 1.0f, "success compareAndExchangeAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "success compareAndExchangeAcquire float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, 1.0f, 3.0f);
            assertEquals(r, 2.0f, "failing compareAndExchangeAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "failing compareAndExchangeAcquire float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, 2.0f, 1.0f);
            assertEquals(r, 2.0f, "success compareAndExchangeRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "success compareAndExchangeRelease float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, 2.0f, 3.0f);
            assertEquals(r, 1.0f, "failing compareAndExchangeRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "failing compareAndExchangeRelease float value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(recv, 1.0f, 2.0f);
            }
            assertEquals(success, true, "weakCompareAndSetPlain float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "weakCompareAndSetPlain float value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(recv, 2.0f, 1.0f);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "weakCompareAndSetAcquire float");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(recv, 1.0f, 2.0f);
            }
            assertEquals(success, true, "weakCompareAndSetRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "weakCompareAndSetRelease float");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(recv, 2.0f, 1.0f);
            }
            assertEquals(success, true, "weakCompareAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0f, "weakCompareAndSet float");
        }

        // Compare set and get
        {
            float o = (float) hs.get(TestAccessMode.GET_AND_SET).invokeExact(recv, 2.0f);
            assertEquals(o, 1.0f, "getAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0f, "getAndSet float value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(recv, 2.0f);
            assertEquals(o, 1.0f, "getAndAdd float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAdd float value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(recv, 2.0f);
            assertEquals(o, 1.0f, "getAndAddAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAddAcquire float value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(recv, 2.0f);
            assertEquals(o, 1.0f, "getAndAddRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAddRelease float value");
        }

    }

    static void testInstanceFieldUnsupported(VarHandleTestMethodHandleAccessFloat recv, Handles hs) throws Throwable {


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                float r = (float) hs.get(am).invokeExact(recv, 1.0f);
            });
        }
    }


    static void testStaticField(Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "set float value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(2.0f);
            float x = (float) hs.get(TestAccessMode.GET_VOLATILE).invokeExact();
            assertEquals(x, 2.0f, "setVolatile float value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(1.0f);
            float x = (float) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact();
            assertEquals(x, 1.0f, "setRelease float value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(2.0f);
            float x = (float) hs.get(TestAccessMode.GET_OPAQUE).invokeExact();
            assertEquals(x, 2.0f, "setOpaque float value");
        }

        hs.get(TestAccessMode.SET).invokeExact(1.0f);

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(1.0f, 2.0f);
            assertEquals(r, true, "success compareAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "success compareAndSet float value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(1.0f, 3.0f);
            assertEquals(r, false, "failing compareAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "failing compareAndSet float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(2.0f, 1.0f);
            assertEquals(r, 2.0f, "success compareAndExchange float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "success compareAndExchange float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(2.0f, 3.0f);
            assertEquals(r, 1.0f, "failing compareAndExchange float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "failing compareAndExchange float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(1.0f, 2.0f);
            assertEquals(r, 1.0f, "success compareAndExchangeAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "success compareAndExchangeAcquire float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(1.0f, 3.0f);
            assertEquals(r, 2.0f, "failing compareAndExchangeAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "failing compareAndExchangeAcquire float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(2.0f, 1.0f);
            assertEquals(r, 2.0f, "success compareAndExchangeRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "success compareAndExchangeRelease float value");
        }

        {
            float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(2.0f, 3.0f);
            assertEquals(r, 1.0f, "failing compareAndExchangeRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "failing compareAndExchangeRelease float value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(1.0f, 2.0f);
            }
            assertEquals(success, true, "weakCompareAndSetPlain float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "weakCompareAndSetPlain float value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(2.0f, 1.0f);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "weakCompareAndSetAcquire float");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(1.0f, 2.0f);
            }
            assertEquals(success, true, "weakCompareAndSetRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "weakCompareAndSetRelease float");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(2.0f, 1.0f);
            }
            assertEquals(success, true, "weakCompareAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0f, "weakCompareAndSet float");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_SET).invokeExact(2.0f);
            assertEquals(o, 1.0f, "getAndSet float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "getAndSet float value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(2.0f);
            assertEquals(o, 1.0f, "getAndSetAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "getAndSetAcquire float value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(2.0f);
            assertEquals(o, 1.0f, "getAndSetRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0f, "getAndSetRelease float value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(2.0f);
            assertEquals(o, 1.0f, "getAndAdd float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAdd float value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(2.0f);
            assertEquals(o, 1.0f, "getAndAddAcquire float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAddAcquire float value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(1.0f);

            float o = (float) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(2.0f);
            assertEquals(o, 1.0f, "getAndAddRelease float");
            float x = (float) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (float)(1.0f + 2.0f), "getAndAddRelease float value");
        }

    }

    static void testStaticFieldUnsupported(Handles hs) throws Throwable {


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                float r = (float) hs.get(am).invokeExact(1.0f);
            });
        }
    }


    static void testArray(Handles hs) throws Throwable {
        float[] array = new float[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "get float value");
            }


            // Volatile
            {
                hs.get(TestAccessMode.SET_VOLATILE).invokeExact(array, i, 2.0f);
                float x = (float) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(array, i);
                assertEquals(x, 2.0f, "setVolatile float value");
            }

            // Lazy
            {
                hs.get(TestAccessMode.SET_RELEASE).invokeExact(array, i, 1.0f);
                float x = (float) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(array, i);
                assertEquals(x, 1.0f, "setRelease float value");
            }

            // Opaque
            {
                hs.get(TestAccessMode.SET_OPAQUE).invokeExact(array, i, 2.0f);
                float x = (float) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(array, i);
                assertEquals(x, 2.0f, "setOpaque float value");
            }

            hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

            // Compare
            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, 1.0f, 2.0f);
                assertEquals(r, true, "success compareAndSet float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "success compareAndSet float value");
            }

            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, 1.0f, 3.0f);
                assertEquals(r, false, "failing compareAndSet float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "failing compareAndSet float value");
            }

            {
                float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, 2.0f, 1.0f);
                assertEquals(r, 2.0f, "success compareAndExchange float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "success compareAndExchange float value");
            }

            {
                float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, 2.0f, 3.0f);
                assertEquals(r, 1.0f, "failing compareAndExchange float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "failing compareAndExchange float value");
            }

            {
                float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, 1.0f, 2.0f);
                assertEquals(r, 1.0f, "success compareAndExchangeAcquire float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "success compareAndExchangeAcquire float value");
            }

            {
                float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, 1.0f, 3.0f);
                assertEquals(r, 2.0f, "failing compareAndExchangeAcquire float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "failing compareAndExchangeAcquire float value");
            }

            {
                float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, 2.0f, 1.0f);
                assertEquals(r, 2.0f, "success compareAndExchangeRelease float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "success compareAndExchangeRelease float value");
            }

            {
                float r = (float) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, 2.0f, 3.0f);
                assertEquals(r, 1.0f, "failing compareAndExchangeRelease float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "failing compareAndExchangeRelease float value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(array, i, 1.0f, 2.0f);
                }
                assertEquals(success, true, "weakCompareAndSetPlain float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "weakCompareAndSetPlain float value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(array, i, 2.0f, 1.0f);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "weakCompareAndSetAcquire float");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(array, i, 1.0f, 2.0f);
                }
                assertEquals(success, true, "weakCompareAndSetRelease float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "weakCompareAndSetRelease float");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(array, i, 2.0f, 1.0f);
                }
                assertEquals(success, true, "weakCompareAndSet float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0f, "weakCompareAndSet float");
            }

            // Compare set and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

                float o = (float) hs.get(TestAccessMode.GET_AND_SET).invokeExact(array, i, 2.0f);
                assertEquals(o, 1.0f, "getAndSet float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "getAndSet float value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

                float o = (float) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(array, i, 2.0f);
                assertEquals(o, 1.0f, "getAndSetAcquire float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "getAndSetAcquire float value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

                float o = (float) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(array, i, 2.0f);
                assertEquals(o, 1.0f, "getAndSetRelease float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0f, "getAndSetRelease float value");
            }

            // get and add, add and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

                float o = (float) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(array, i, 2.0f);
                assertEquals(o, 1.0f, "getAndAdd float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (float)(1.0f + 2.0f), "getAndAdd float value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

                float o = (float) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(array, i, 2.0f);
                assertEquals(o, 1.0f, "getAndAddAcquire float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (float)(1.0f + 2.0f), "getAndAddAcquire float value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0f);

                float o = (float) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(array, i, 2.0f);
                assertEquals(o, 1.0f, "getAndAddRelease float");
                float x = (float) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (float)(1.0f + 2.0f), "getAndAddRelease float value");
            }

        }
    }

    static void testArrayUnsupported(Handles hs) throws Throwable {
        float[] array = new float[10];

        final int i = 0;


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                float o = (float) hs.get(am).invokeExact(array, i, 1.0f);
            });
        }
    }

    static void testArrayIndexOutOfBounds(Handles hs) throws Throwable {
        float[] array = new float[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
                checkAIOOBE(am, () -> {
                    float x = (float) hs.get(am).invokeExact(array, ci);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
                checkAIOOBE(am, () -> {
                    hs.get(am).invokeExact(array, ci, 1.0f);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
                checkAIOOBE(am, () -> {
                    boolean r = (boolean) hs.get(am).invokeExact(array, ci, 1.0f, 2.0f);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
                checkAIOOBE(am, () -> {
                    float r = (float) hs.get(am).invokeExact(array, ci, 2.0f, 1.0f);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
                checkAIOOBE(am, () -> {
                    float o = (float) hs.get(am).invokeExact(array, ci, 1.0f);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
                checkAIOOBE(am, () -> {
                    float o = (float) hs.get(am).invokeExact(array, ci, 3.0f);
                });
            }

        }
    }
}

