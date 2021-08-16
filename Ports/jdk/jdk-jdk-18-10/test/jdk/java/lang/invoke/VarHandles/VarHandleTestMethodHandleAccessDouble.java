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
 * @run testng/othervm -Diters=20000 VarHandleTestMethodHandleAccessDouble
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

public class VarHandleTestMethodHandleAccessDouble extends VarHandleBaseTest {
    static final double static_final_v = 1.0d;

    static double static_v;

    final double final_v = 1.0d;

    double v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessDouble.class, "final_v", double.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessDouble.class, "v", double.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessDouble.class, "static_final_v", double.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessDouble.class, "static_v", double.class);

        vhArray = MethodHandles.arrayElementVarHandle(double[].class);
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
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessDouble::testStaticField));
            cases.add(new MethodHandleAccessTestCase("Static field unsupported",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessDouble::testStaticFieldUnsupported,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodHandleAccessDouble::testArray));
            cases.add(new MethodHandleAccessTestCase("Array unsupported",
                                                     vhArray, f, VarHandleTestMethodHandleAccessDouble::testArrayUnsupported,
                                                     false));
            cases.add(new MethodHandleAccessTestCase("Array index out of bounds",
                                                     vhArray, f, VarHandleTestMethodHandleAccessDouble::testArrayIndexOutOfBounds,
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


    static void testInstanceField(VarHandleTestMethodHandleAccessDouble recv, Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0d);
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "set double value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(recv, 2.0d);
            double x = (double) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(recv);
            assertEquals(x, 2.0d, "setVolatile double value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(recv, 1.0d);
            double x = (double) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(recv);
            assertEquals(x, 1.0d, "setRelease double value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(recv, 2.0d);
            double x = (double) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(recv);
            assertEquals(x, 2.0d, "setOpaque double value");
        }

        hs.get(TestAccessMode.SET).invokeExact(recv, 1.0d);

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, 1.0d, 2.0d);
            assertEquals(r, true, "success compareAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "success compareAndSet double value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, 1.0d, 3.0d);
            assertEquals(r, false, "failing compareAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "failing compareAndSet double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, 2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchange double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "success compareAndExchange double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, 2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchange double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "failing compareAndExchange double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, 1.0d, 2.0d);
            assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, 1.0d, 3.0d);
            assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, 2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, 2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(recv, 1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetPlain double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(recv, 2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(recv, 1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(recv, 2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 1.0d, "weakCompareAndSet double");
        }

        // Compare set and get
        {
            double o = (double) hs.get(TestAccessMode.GET_AND_SET).invokeExact(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 2.0d, "getAndSet double value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndAdd double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndAddAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddAcquire double value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndAddRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddRelease double value");
        }

    }

    static void testInstanceFieldUnsupported(VarHandleTestMethodHandleAccessDouble recv, Handles hs) throws Throwable {


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                double r = (double) hs.get(am).invokeExact(recv, 1.0d);
            });
        }
    }


    static void testStaticField(Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "set double value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(2.0d);
            double x = (double) hs.get(TestAccessMode.GET_VOLATILE).invokeExact();
            assertEquals(x, 2.0d, "setVolatile double value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(1.0d);
            double x = (double) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact();
            assertEquals(x, 1.0d, "setRelease double value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(2.0d);
            double x = (double) hs.get(TestAccessMode.GET_OPAQUE).invokeExact();
            assertEquals(x, 2.0d, "setOpaque double value");
        }

        hs.get(TestAccessMode.SET).invokeExact(1.0d);

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(1.0d, 2.0d);
            assertEquals(r, true, "success compareAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "success compareAndSet double value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(1.0d, 3.0d);
            assertEquals(r, false, "failing compareAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "failing compareAndSet double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchange double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "success compareAndExchange double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchange double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "failing compareAndExchange double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(1.0d, 2.0d);
            assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(1.0d, 3.0d);
            assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
        }

        {
            double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetPlain double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 1.0d, "weakCompareAndSet double");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_SET).invokeExact(2.0d);
            assertEquals(o, 1.0d, "getAndSet double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "getAndSet double value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(2.0d);
            assertEquals(o, 1.0d, "getAndSetAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "getAndSetAcquire double value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(2.0d);
            assertEquals(o, 1.0d, "getAndSetRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 2.0d, "getAndSetRelease double value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(2.0d);
            assertEquals(o, 1.0d, "getAndAdd double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(2.0d);
            assertEquals(o, 1.0d, "getAndAddAcquire double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddAcquire double value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(1.0d);

            double o = (double) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(2.0d);
            assertEquals(o, 1.0d, "getAndAddRelease double");
            double x = (double) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddRelease double value");
        }

    }

    static void testStaticFieldUnsupported(Handles hs) throws Throwable {


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                double r = (double) hs.get(am).invokeExact(1.0d);
            });
        }
    }


    static void testArray(Handles hs) throws Throwable {
        double[] array = new double[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "get double value");
            }


            // Volatile
            {
                hs.get(TestAccessMode.SET_VOLATILE).invokeExact(array, i, 2.0d);
                double x = (double) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(array, i);
                assertEquals(x, 2.0d, "setVolatile double value");
            }

            // Lazy
            {
                hs.get(TestAccessMode.SET_RELEASE).invokeExact(array, i, 1.0d);
                double x = (double) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(array, i);
                assertEquals(x, 1.0d, "setRelease double value");
            }

            // Opaque
            {
                hs.get(TestAccessMode.SET_OPAQUE).invokeExact(array, i, 2.0d);
                double x = (double) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(array, i);
                assertEquals(x, 2.0d, "setOpaque double value");
            }

            hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

            // Compare
            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, 1.0d, 2.0d);
                assertEquals(r, true, "success compareAndSet double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "success compareAndSet double value");
            }

            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, 1.0d, 3.0d);
                assertEquals(r, false, "failing compareAndSet double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "failing compareAndSet double value");
            }

            {
                double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, 2.0d, 1.0d);
                assertEquals(r, 2.0d, "success compareAndExchange double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "success compareAndExchange double value");
            }

            {
                double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, 2.0d, 3.0d);
                assertEquals(r, 1.0d, "failing compareAndExchange double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "failing compareAndExchange double value");
            }

            {
                double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, 1.0d, 2.0d);
                assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
            }

            {
                double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, 1.0d, 3.0d);
                assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
            }

            {
                double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, 2.0d, 1.0d);
                assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
            }

            {
                double r = (double) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, 2.0d, 3.0d);
                assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(array, i, 1.0d, 2.0d);
                }
                assertEquals(success, true, "weakCompareAndSetPlain double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(array, i, 2.0d, 1.0d);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(array, i, 1.0d, 2.0d);
                }
                assertEquals(success, true, "weakCompareAndSetRelease double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(array, i, 2.0d, 1.0d);
                }
                assertEquals(success, true, "weakCompareAndSet double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 1.0d, "weakCompareAndSet double");
            }

            // Compare set and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

                double o = (double) hs.get(TestAccessMode.GET_AND_SET).invokeExact(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndSet double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "getAndSet double value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

                double o = (double) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndSetAcquire double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "getAndSetAcquire double value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

                double o = (double) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndSetRelease double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 2.0d, "getAndSetRelease double value");
            }

            // get and add, add and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

                double o = (double) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndAdd double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

                double o = (double) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndAddAcquire double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (double)(1.0d + 2.0d), "getAndAddAcquire double value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 1.0d);

                double o = (double) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndAddRelease double");
                double x = (double) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (double)(1.0d + 2.0d), "getAndAddRelease double value");
            }

        }
    }

    static void testArrayUnsupported(Handles hs) throws Throwable {
        double[] array = new double[10];

        final int i = 0;


        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                double o = (double) hs.get(am).invokeExact(array, i, 1.0d);
            });
        }
    }

    static void testArrayIndexOutOfBounds(Handles hs) throws Throwable {
        double[] array = new double[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
                checkAIOOBE(am, () -> {
                    double x = (double) hs.get(am).invokeExact(array, ci);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
                checkAIOOBE(am, () -> {
                    hs.get(am).invokeExact(array, ci, 1.0d);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
                checkAIOOBE(am, () -> {
                    boolean r = (boolean) hs.get(am).invokeExact(array, ci, 1.0d, 2.0d);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
                checkAIOOBE(am, () -> {
                    double r = (double) hs.get(am).invokeExact(array, ci, 2.0d, 1.0d);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
                checkAIOOBE(am, () -> {
                    double o = (double) hs.get(am).invokeExact(array, ci, 1.0d);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
                checkAIOOBE(am, () -> {
                    double o = (double) hs.get(am).invokeExact(array, ci, 3.0d);
                });
            }

        }
    }
}

