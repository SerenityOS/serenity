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
 * @run testng/othervm -Diters=20000 VarHandleTestMethodHandleAccessInt
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

public class VarHandleTestMethodHandleAccessInt extends VarHandleBaseTest {
    static final int static_final_v = 0x01234567;

    static int static_v;

    final int final_v = 0x01234567;

    int v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessInt.class, "final_v", int.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessInt.class, "v", int.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessInt.class, "static_final_v", int.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessInt.class, "static_v", int.class);

        vhArray = MethodHandles.arrayElementVarHandle(int[].class);
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
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessInt::testStaticField));
            cases.add(new MethodHandleAccessTestCase("Static field unsupported",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessInt::testStaticFieldUnsupported,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodHandleAccessInt::testArray));
            cases.add(new MethodHandleAccessTestCase("Array unsupported",
                                                     vhArray, f, VarHandleTestMethodHandleAccessInt::testArrayUnsupported,
                                                     false));
            cases.add(new MethodHandleAccessTestCase("Array index out of bounds",
                                                     vhArray, f, VarHandleTestMethodHandleAccessInt::testArrayIndexOutOfBounds,
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


    static void testInstanceField(VarHandleTestMethodHandleAccessInt recv, Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "set int value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(recv, 0x89ABCDEF);
            int x = (int) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "setVolatile int value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(recv, 0x01234567);
            int x = (int) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(recv);
            assertEquals(x, 0x01234567, "setRelease int value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(recv, 0x89ABCDEF);
            int x = (int) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "setOpaque int value");
        }

        hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, 0x01234567, 0x89ABCDEF);
            assertEquals(r, true, "success compareAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "success compareAndSet int value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, 0x01234567, 0xCAFEBABE);
            assertEquals(r, false, "failing compareAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "failing compareAndSet int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, 0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchange int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "success compareAndExchange int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, 0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchange int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "failing compareAndExchange int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, 0x01234567, 0x89ABCDEF);
            assertEquals(r, 0x01234567, "success compareAndExchangeAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "success compareAndExchangeAcquire int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, 0x01234567, 0xCAFEBABE);
            assertEquals(r, 0x89ABCDEF, "failing compareAndExchangeAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "failing compareAndExchangeAcquire int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, 0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchangeRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "success compareAndExchangeRelease int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, 0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchangeRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "failing compareAndExchangeRelease int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(recv, 0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetPlain int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetPlain int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(recv, 0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "weakCompareAndSetAcquire int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(recv, 0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetRelease int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(recv, 0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x01234567, "weakCompareAndSet int");
        }

        // Compare set and get
        {
            int o = (int) hs.get(TestAccessMode.GET_AND_SET).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, 0x89ABCDEF, "getAndSet int value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAdd int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddRelease int value");
        }

        // get and bitwise or
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOr int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOr int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR_ACQUIRE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR_RELEASE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrRelease int value");
        }

        // get and bitwise and
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAnd int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAnd int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND_ACQUIRE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND_RELEASE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndRelease int value");
        }

        // get and bitwise xor
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXor int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXor int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_ACQUIRE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_RELEASE).invokeExact(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorRelease int value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestMethodHandleAccessInt recv, Handles hs) throws Throwable {


    }


    static void testStaticField(Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "set int value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(0x89ABCDEF);
            int x = (int) hs.get(TestAccessMode.GET_VOLATILE).invokeExact();
            assertEquals(x, 0x89ABCDEF, "setVolatile int value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(0x01234567);
            int x = (int) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact();
            assertEquals(x, 0x01234567, "setRelease int value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(0x89ABCDEF);
            int x = (int) hs.get(TestAccessMode.GET_OPAQUE).invokeExact();
            assertEquals(x, 0x89ABCDEF, "setOpaque int value");
        }

        hs.get(TestAccessMode.SET).invokeExact(0x01234567);

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(0x01234567, 0x89ABCDEF);
            assertEquals(r, true, "success compareAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "success compareAndSet int value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(0x01234567, 0xCAFEBABE);
            assertEquals(r, false, "failing compareAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "failing compareAndSet int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchange int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "success compareAndExchange int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchange int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "failing compareAndExchange int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(0x01234567, 0x89ABCDEF);
            assertEquals(r, 0x01234567, "success compareAndExchangeAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "success compareAndExchangeAcquire int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(0x01234567, 0xCAFEBABE);
            assertEquals(r, 0x89ABCDEF, "failing compareAndExchangeAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "failing compareAndExchangeAcquire int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchangeRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "success compareAndExchangeRelease int value");
        }

        {
            int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchangeRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "failing compareAndExchangeRelease int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetPlain int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetPlain int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "weakCompareAndSetAcquire int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetRelease int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x01234567, "weakCompareAndSet int");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_SET).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSet int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "getAndSet int value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSetAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "getAndSetAcquire int value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSetRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, 0x89ABCDEF, "getAndSetRelease int value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAdd int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddRelease int value");
        }

        // get and bitwise or
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOr int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOr int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR_ACQUIRE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR_RELEASE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrRelease int value");
        }

        // get and bitwise and
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAnd int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAnd int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND_ACQUIRE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND_RELEASE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndRelease int value");
        }

        // get and bitwise xor
        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXor int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXor int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_ACQUIRE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_RELEASE).invokeExact(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorRelease int value");
        }
    }

    static void testStaticFieldUnsupported(Handles hs) throws Throwable {


    }


    static void testArray(Handles hs) throws Throwable {
        int[] array = new int[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "get int value");
            }


            // Volatile
            {
                hs.get(TestAccessMode.SET_VOLATILE).invokeExact(array, i, 0x89ABCDEF);
                int x = (int) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "setVolatile int value");
            }

            // Lazy
            {
                hs.get(TestAccessMode.SET_RELEASE).invokeExact(array, i, 0x01234567);
                int x = (int) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(array, i);
                assertEquals(x, 0x01234567, "setRelease int value");
            }

            // Opaque
            {
                hs.get(TestAccessMode.SET_OPAQUE).invokeExact(array, i, 0x89ABCDEF);
                int x = (int) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "setOpaque int value");
            }

            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            // Compare
            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, 0x01234567, 0x89ABCDEF);
                assertEquals(r, true, "success compareAndSet int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "success compareAndSet int value");
            }

            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, 0x01234567, 0xCAFEBABE);
                assertEquals(r, false, "failing compareAndSet int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "failing compareAndSet int value");
            }

            {
                int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, 0x89ABCDEF, 0x01234567);
                assertEquals(r, 0x89ABCDEF, "success compareAndExchange int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "success compareAndExchange int value");
            }

            {
                int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, 0x89ABCDEF, 0xCAFEBABE);
                assertEquals(r, 0x01234567, "failing compareAndExchange int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "failing compareAndExchange int value");
            }

            {
                int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, 0x01234567, 0x89ABCDEF);
                assertEquals(r, 0x01234567, "success compareAndExchangeAcquire int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "success compareAndExchangeAcquire int value");
            }

            {
                int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, 0x01234567, 0xCAFEBABE);
                assertEquals(r, 0x89ABCDEF, "failing compareAndExchangeAcquire int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "failing compareAndExchangeAcquire int value");
            }

            {
                int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, 0x89ABCDEF, 0x01234567);
                assertEquals(r, 0x89ABCDEF, "success compareAndExchangeRelease int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "success compareAndExchangeRelease int value");
            }

            {
                int r = (int) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, 0x89ABCDEF, 0xCAFEBABE);
                assertEquals(r, 0x01234567, "failing compareAndExchangeRelease int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "failing compareAndExchangeRelease int value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(array, i, 0x01234567, 0x89ABCDEF);
                }
                assertEquals(success, true, "weakCompareAndSetPlain int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "weakCompareAndSetPlain int value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(array, i, 0x89ABCDEF, 0x01234567);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "weakCompareAndSetAcquire int");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(array, i, 0x01234567, 0x89ABCDEF);
                }
                assertEquals(success, true, "weakCompareAndSetRelease int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "weakCompareAndSetRelease int");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(array, i, 0x89ABCDEF, 0x01234567);
                }
                assertEquals(success, true, "weakCompareAndSet int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x01234567, "weakCompareAndSet int");
            }

            // Compare set and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

                int o = (int) hs.get(TestAccessMode.GET_AND_SET).invokeExact(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndSet int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "getAndSet int value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

                int o = (int) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndSetAcquire int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "getAndSetAcquire int value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

                int o = (int) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndSetRelease int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, 0x89ABCDEF, "getAndSetRelease int value");
            }

            // get and add, add and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

                int o = (int) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndAdd int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

                int o = (int) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndAddAcquire int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddAcquire int value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

                int o = (int) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndAddRelease int");
                int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddRelease int value");
            }

        // get and bitwise or
        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOr int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOr int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR_ACQUIRE).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_OR_RELEASE).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrRelease int value");
        }

        // get and bitwise and
        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAnd int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAnd int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND_ACQUIRE).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_AND_RELEASE).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndRelease int value");
        }

        // get and bitwise xor
        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXor int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXor int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_ACQUIRE).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorAcquire int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorAcquire int value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, 0x01234567);

            int o = (int) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_RELEASE).invokeExact(array, i, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorRelease int");
            int x = (int) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorRelease int value");
        }
        }
    }

    static void testArrayUnsupported(Handles hs) throws Throwable {
        int[] array = new int[10];

        final int i = 0;


    }

    static void testArrayIndexOutOfBounds(Handles hs) throws Throwable {
        int[] array = new int[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
                checkAIOOBE(am, () -> {
                    int x = (int) hs.get(am).invokeExact(array, ci);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
                checkAIOOBE(am, () -> {
                    hs.get(am).invokeExact(array, ci, 0x01234567);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
                checkAIOOBE(am, () -> {
                    boolean r = (boolean) hs.get(am).invokeExact(array, ci, 0x01234567, 0x89ABCDEF);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
                checkAIOOBE(am, () -> {
                    int r = (int) hs.get(am).invokeExact(array, ci, 0x89ABCDEF, 0x01234567);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
                checkAIOOBE(am, () -> {
                    int o = (int) hs.get(am).invokeExact(array, ci, 0x01234567);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
                checkAIOOBE(am, () -> {
                    int o = (int) hs.get(am).invokeExact(array, ci, 0xCAFEBABE);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
                checkAIOOBE(am, () -> {
                    int o = (int) hs.get(am).invokeExact(array, ci, 0xCAFEBABE);
                });
            }
        }
    }
}

