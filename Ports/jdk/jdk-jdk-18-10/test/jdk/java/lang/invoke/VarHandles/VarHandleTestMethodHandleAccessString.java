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
 * @run testng/othervm -Diters=20000 VarHandleTestMethodHandleAccessString
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

public class VarHandleTestMethodHandleAccessString extends VarHandleBaseTest {
    static final String static_final_v = "foo";

    static String static_v;

    final String final_v = "foo";

    String v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessString.class, "final_v", String.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessString.class, "v", String.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessString.class, "static_final_v", String.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessString.class, "static_v", String.class);

        vhArray = MethodHandles.arrayElementVarHandle(String[].class);
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
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessString::testStaticField));
            cases.add(new MethodHandleAccessTestCase("Static field unsupported",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessString::testStaticFieldUnsupported,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodHandleAccessString::testArray));
            cases.add(new MethodHandleAccessTestCase("Array unsupported",
                                                     vhArray, f, VarHandleTestMethodHandleAccessString::testArrayUnsupported,
                                                     false));
            cases.add(new MethodHandleAccessTestCase("Array index out of bounds",
                                                     vhArray, f, VarHandleTestMethodHandleAccessString::testArrayIndexOutOfBounds,
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


    static void testInstanceField(VarHandleTestMethodHandleAccessString recv, Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, "foo");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "set String value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(recv, "bar");
            String x = (String) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(recv);
            assertEquals(x, "bar", "setVolatile String value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(recv, "foo");
            String x = (String) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(recv);
            assertEquals(x, "foo", "setRelease String value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(recv, "bar");
            String x = (String) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(recv);
            assertEquals(x, "bar", "setOpaque String value");
        }

        hs.get(TestAccessMode.SET).invokeExact(recv, "foo");

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, "foo", "bar");
            assertEquals(r, true, "success compareAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "success compareAndSet String value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, "foo", "baz");
            assertEquals(r, false, "failing compareAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "failing compareAndSet String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, "bar", "foo");
            assertEquals(r, "bar", "success compareAndExchange String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "success compareAndExchange String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, "bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchange String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "failing compareAndExchange String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, "foo", "bar");
            assertEquals(r, "foo", "success compareAndExchangeAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "success compareAndExchangeAcquire String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, "foo", "baz");
            assertEquals(r, "bar", "failing compareAndExchangeAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "failing compareAndExchangeAcquire String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, "bar", "foo");
            assertEquals(r, "bar", "success compareAndExchangeRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "success compareAndExchangeRelease String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, "bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchangeRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "failing compareAndExchangeRelease String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(recv, "foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetPlain String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "weakCompareAndSetPlain String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(recv, "bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSetAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "weakCompareAndSetAcquire String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(recv, "foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "weakCompareAndSetRelease String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(recv, "bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "foo", "weakCompareAndSet String");
        }

        // Compare set and get
        {
            String o = (String) hs.get(TestAccessMode.GET_AND_SET).invokeExact(recv, "bar");
            assertEquals(o, "foo", "getAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, "bar", "getAndSet String value");
        }


    }

    static void testInstanceFieldUnsupported(VarHandleTestMethodHandleAccessString recv, Handles hs) throws Throwable {

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                String r = (String) hs.get(am).invokeExact(recv, "foo");
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                String r = (String) hs.get(am).invokeExact(recv, "foo");
            });
        }
    }


    static void testStaticField(Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact("foo");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "set String value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact("bar");
            String x = (String) hs.get(TestAccessMode.GET_VOLATILE).invokeExact();
            assertEquals(x, "bar", "setVolatile String value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact("foo");
            String x = (String) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact();
            assertEquals(x, "foo", "setRelease String value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact("bar");
            String x = (String) hs.get(TestAccessMode.GET_OPAQUE).invokeExact();
            assertEquals(x, "bar", "setOpaque String value");
        }

        hs.get(TestAccessMode.SET).invokeExact("foo");

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact("foo", "bar");
            assertEquals(r, true, "success compareAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "success compareAndSet String value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact("foo", "baz");
            assertEquals(r, false, "failing compareAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "failing compareAndSet String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact("bar", "foo");
            assertEquals(r, "bar", "success compareAndExchange String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "success compareAndExchange String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact("bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchange String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "failing compareAndExchange String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact("foo", "bar");
            assertEquals(r, "foo", "success compareAndExchangeAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "success compareAndExchangeAcquire String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact("foo", "baz");
            assertEquals(r, "bar", "failing compareAndExchangeAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "failing compareAndExchangeAcquire String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact("bar", "foo");
            assertEquals(r, "bar", "success compareAndExchangeRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "success compareAndExchangeRelease String value");
        }

        {
            String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact("bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchangeRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "failing compareAndExchangeRelease String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact("foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetPlain String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "weakCompareAndSetPlain String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact("bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSetAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "weakCompareAndSetAcquire String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact("foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "weakCompareAndSetRelease String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact("bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "foo", "weakCompareAndSet String");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact("foo");

            String o = (String) hs.get(TestAccessMode.GET_AND_SET).invokeExact("bar");
            assertEquals(o, "foo", "getAndSet String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "getAndSet String value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact("foo");

            String o = (String) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact("bar");
            assertEquals(o, "foo", "getAndSetAcquire String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "getAndSetAcquire String value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact("foo");

            String o = (String) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact("bar");
            assertEquals(o, "foo", "getAndSetRelease String");
            String x = (String) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, "bar", "getAndSetRelease String value");
        }


    }

    static void testStaticFieldUnsupported(Handles hs) throws Throwable {

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                String r = (String) hs.get(am).invokeExact("foo");
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                String r = (String) hs.get(am).invokeExact("foo");
            });
        }
    }


    static void testArray(Handles hs) throws Throwable {
        String[] array = new String[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, "foo");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "get String value");
            }


            // Volatile
            {
                hs.get(TestAccessMode.SET_VOLATILE).invokeExact(array, i, "bar");
                String x = (String) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(array, i);
                assertEquals(x, "bar", "setVolatile String value");
            }

            // Lazy
            {
                hs.get(TestAccessMode.SET_RELEASE).invokeExact(array, i, "foo");
                String x = (String) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(array, i);
                assertEquals(x, "foo", "setRelease String value");
            }

            // Opaque
            {
                hs.get(TestAccessMode.SET_OPAQUE).invokeExact(array, i, "bar");
                String x = (String) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(array, i);
                assertEquals(x, "bar", "setOpaque String value");
            }

            hs.get(TestAccessMode.SET).invokeExact(array, i, "foo");

            // Compare
            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, "foo", "bar");
                assertEquals(r, true, "success compareAndSet String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "success compareAndSet String value");
            }

            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, "foo", "baz");
                assertEquals(r, false, "failing compareAndSet String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "failing compareAndSet String value");
            }

            {
                String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, "bar", "foo");
                assertEquals(r, "bar", "success compareAndExchange String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "success compareAndExchange String value");
            }

            {
                String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, "bar", "baz");
                assertEquals(r, "foo", "failing compareAndExchange String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "failing compareAndExchange String value");
            }

            {
                String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, "foo", "bar");
                assertEquals(r, "foo", "success compareAndExchangeAcquire String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "success compareAndExchangeAcquire String value");
            }

            {
                String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, "foo", "baz");
                assertEquals(r, "bar", "failing compareAndExchangeAcquire String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "failing compareAndExchangeAcquire String value");
            }

            {
                String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, "bar", "foo");
                assertEquals(r, "bar", "success compareAndExchangeRelease String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "success compareAndExchangeRelease String value");
            }

            {
                String r = (String) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, "bar", "baz");
                assertEquals(r, "foo", "failing compareAndExchangeRelease String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "failing compareAndExchangeRelease String value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(array, i, "foo", "bar");
                }
                assertEquals(success, true, "weakCompareAndSetPlain String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "weakCompareAndSetPlain String value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(array, i, "bar", "foo");
                }
                assertEquals(success, true, "weakCompareAndSetAcquire String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "weakCompareAndSetAcquire String");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(array, i, "foo", "bar");
                }
                assertEquals(success, true, "weakCompareAndSetRelease String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "weakCompareAndSetRelease String");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(array, i, "bar", "foo");
                }
                assertEquals(success, true, "weakCompareAndSet String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "foo", "weakCompareAndSet String");
            }

            // Compare set and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, "foo");

                String o = (String) hs.get(TestAccessMode.GET_AND_SET).invokeExact(array, i, "bar");
                assertEquals(o, "foo", "getAndSet String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "getAndSet String value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, "foo");

                String o = (String) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(array, i, "bar");
                assertEquals(o, "foo", "getAndSetAcquire String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "getAndSetAcquire String value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, "foo");

                String o = (String) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(array, i, "bar");
                assertEquals(o, "foo", "getAndSetRelease String");
                String x = (String) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, "bar", "getAndSetRelease String value");
            }


        }
    }

    static void testArrayUnsupported(Handles hs) throws Throwable {
        String[] array = new String[10];

        final int i = 0;

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
            checkUOE(am, () -> {
                String o = (String) hs.get(am).invokeExact(array, i, "foo");
            });
        }

        for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
            checkUOE(am, () -> {
                String o = (String) hs.get(am).invokeExact(array, i, "foo");
            });
        }
    }

    static void testArrayIndexOutOfBounds(Handles hs) throws Throwable {
        String[] array = new String[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
                checkAIOOBE(am, () -> {
                    String x = (String) hs.get(am).invokeExact(array, ci);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
                checkAIOOBE(am, () -> {
                    hs.get(am).invokeExact(array, ci, "foo");
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
                checkAIOOBE(am, () -> {
                    boolean r = (boolean) hs.get(am).invokeExact(array, ci, "foo", "bar");
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
                checkAIOOBE(am, () -> {
                    String r = (String) hs.get(am).invokeExact(array, ci, "bar", "foo");
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
                checkAIOOBE(am, () -> {
                    String o = (String) hs.get(am).invokeExact(array, ci, "foo");
                });
            }


        }
    }
}

