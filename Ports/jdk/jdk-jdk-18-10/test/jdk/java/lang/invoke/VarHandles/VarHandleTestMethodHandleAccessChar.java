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
 * @run testng/othervm -Diters=20000 VarHandleTestMethodHandleAccessChar
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

public class VarHandleTestMethodHandleAccessChar extends VarHandleBaseTest {
    static final char static_final_v = '\u0123';

    static char static_v;

    final char final_v = '\u0123';

    char v;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessChar.class, "final_v", char.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestMethodHandleAccessChar.class, "v", char.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessChar.class, "static_final_v", char.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestMethodHandleAccessChar.class, "static_v", char.class);

        vhArray = MethodHandles.arrayElementVarHandle(char[].class);
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
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessChar::testStaticField));
            cases.add(new MethodHandleAccessTestCase("Static field unsupported",
                                                     vhStaticField, f, VarHandleTestMethodHandleAccessChar::testStaticFieldUnsupported,
                                                     false));

            cases.add(new MethodHandleAccessTestCase("Array",
                                                     vhArray, f, VarHandleTestMethodHandleAccessChar::testArray));
            cases.add(new MethodHandleAccessTestCase("Array unsupported",
                                                     vhArray, f, VarHandleTestMethodHandleAccessChar::testArrayUnsupported,
                                                     false));
            cases.add(new MethodHandleAccessTestCase("Array index out of bounds",
                                                     vhArray, f, VarHandleTestMethodHandleAccessChar::testArrayIndexOutOfBounds,
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


    static void testInstanceField(VarHandleTestMethodHandleAccessChar recv, Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "set char value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact(recv, '\u4567');
            char x = (char) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(recv);
            assertEquals(x, '\u4567', "setVolatile char value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact(recv, '\u0123');
            char x = (char) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(recv);
            assertEquals(x, '\u0123', "setRelease char value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact(recv, '\u4567');
            char x = (char) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(recv);
            assertEquals(x, '\u4567', "setOpaque char value");
        }

        hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, '\u0123', '\u4567');
            assertEquals(r, true, "success compareAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "success compareAndSet char value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(recv, '\u0123', '\u89AB');
            assertEquals(r, false, "failing compareAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "failing compareAndSet char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, '\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchange char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "success compareAndExchange char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(recv, '\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchange char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "failing compareAndExchange char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, '\u0123', '\u4567');
            assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(recv, '\u0123', '\u89AB');
            assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, '\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(recv, '\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(recv, '\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetPlain char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(recv, '\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSetAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(recv, '\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(recv, '\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u0123', "weakCompareAndSet char");
        }

        // Compare set and get
        {
            char o = (char) hs.get(TestAccessMode.GET_AND_SET).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, '\u4567', "getAndSet char value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndAdd char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndAddAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndAddRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddRelease char value");
        }

        // get and bitwise or
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOr char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOr char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR_ACQUIRE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR_RELEASE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrRelease char value");
        }

        // get and bitwise and
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAnd char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAnd char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND_ACQUIRE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND_RELEASE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndRelease char value");
        }

        // get and bitwise xor
        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXor char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXor char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_ACQUIRE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(recv, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_RELEASE).invokeExact(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(recv);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorRelease char value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestMethodHandleAccessChar recv, Handles hs) throws Throwable {


    }


    static void testStaticField(Handles hs) throws Throwable {
        // Plain
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "set char value");
        }


        // Volatile
        {
            hs.get(TestAccessMode.SET_VOLATILE).invokeExact('\u4567');
            char x = (char) hs.get(TestAccessMode.GET_VOLATILE).invokeExact();
            assertEquals(x, '\u4567', "setVolatile char value");
        }

        // Lazy
        {
            hs.get(TestAccessMode.SET_RELEASE).invokeExact('\u0123');
            char x = (char) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact();
            assertEquals(x, '\u0123', "setRelease char value");
        }

        // Opaque
        {
            hs.get(TestAccessMode.SET_OPAQUE).invokeExact('\u4567');
            char x = (char) hs.get(TestAccessMode.GET_OPAQUE).invokeExact();
            assertEquals(x, '\u4567', "setOpaque char value");
        }

        hs.get(TestAccessMode.SET).invokeExact('\u0123');

        // Compare
        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact('\u0123', '\u4567');
            assertEquals(r, true, "success compareAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "success compareAndSet char value");
        }

        {
            boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact('\u0123', '\u89AB');
            assertEquals(r, false, "failing compareAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "failing compareAndSet char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact('\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchange char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "success compareAndExchange char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact('\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchange char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "failing compareAndExchange char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact('\u0123', '\u4567');
            assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact('\u0123', '\u89AB');
            assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact('\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
        }

        {
            char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact('\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact('\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetPlain char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact('\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSetAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact('\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact('\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u0123', "weakCompareAndSet char");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_SET).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndSet char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "getAndSet char value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndSetAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "getAndSetAcquire char value");
        }

        // Compare set and get
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndSetRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, '\u4567', "getAndSetRelease char value");
        }

        // get and add, add and get
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_ADD).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndAdd char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndAddAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndAddRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddRelease char value");
        }

        // get and bitwise or
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOr char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOr char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR_ACQUIRE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR_RELEASE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrRelease char value");
        }

        // get and bitwise and
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAnd char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAnd char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND_ACQUIRE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND_RELEASE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndRelease char value");
        }

        // get and bitwise xor
        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXor char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXor char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_ACQUIRE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact('\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_RELEASE).invokeExact('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact();
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorRelease char value");
        }
    }

    static void testStaticFieldUnsupported(Handles hs) throws Throwable {


    }


    static void testArray(Handles hs) throws Throwable {
        char[] array = new char[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "get char value");
            }


            // Volatile
            {
                hs.get(TestAccessMode.SET_VOLATILE).invokeExact(array, i, '\u4567');
                char x = (char) hs.get(TestAccessMode.GET_VOLATILE).invokeExact(array, i);
                assertEquals(x, '\u4567', "setVolatile char value");
            }

            // Lazy
            {
                hs.get(TestAccessMode.SET_RELEASE).invokeExact(array, i, '\u0123');
                char x = (char) hs.get(TestAccessMode.GET_ACQUIRE).invokeExact(array, i);
                assertEquals(x, '\u0123', "setRelease char value");
            }

            // Opaque
            {
                hs.get(TestAccessMode.SET_OPAQUE).invokeExact(array, i, '\u4567');
                char x = (char) hs.get(TestAccessMode.GET_OPAQUE).invokeExact(array, i);
                assertEquals(x, '\u4567', "setOpaque char value");
            }

            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            // Compare
            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, '\u0123', '\u4567');
                assertEquals(r, true, "success compareAndSet char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "success compareAndSet char value");
            }

            {
                boolean r = (boolean) hs.get(TestAccessMode.COMPARE_AND_SET).invokeExact(array, i, '\u0123', '\u89AB');
                assertEquals(r, false, "failing compareAndSet char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "failing compareAndSet char value");
            }

            {
                char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, '\u4567', '\u0123');
                assertEquals(r, '\u4567', "success compareAndExchange char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "success compareAndExchange char value");
            }

            {
                char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE).invokeExact(array, i, '\u4567', '\u89AB');
                assertEquals(r, '\u0123', "failing compareAndExchange char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "failing compareAndExchange char value");
            }

            {
                char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, '\u0123', '\u4567');
                assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
            }

            {
                char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_ACQUIRE).invokeExact(array, i, '\u0123', '\u89AB');
                assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
            }

            {
                char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, '\u4567', '\u0123');
                assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
            }

            {
                char r = (char) hs.get(TestAccessMode.COMPARE_AND_EXCHANGE_RELEASE).invokeExact(array, i, '\u4567', '\u89AB');
                assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_PLAIN).invokeExact(array, i, '\u0123', '\u4567');
                }
                assertEquals(success, true, "weakCompareAndSetPlain char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_ACQUIRE).invokeExact(array, i, '\u4567', '\u0123');
                }
                assertEquals(success, true, "weakCompareAndSetAcquire char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET_RELEASE).invokeExact(array, i, '\u0123', '\u4567');
                }
                assertEquals(success, true, "weakCompareAndSetRelease char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = (boolean) hs.get(TestAccessMode.WEAK_COMPARE_AND_SET).invokeExact(array, i, '\u4567', '\u0123');
                }
                assertEquals(success, true, "weakCompareAndSet char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u0123', "weakCompareAndSet char");
            }

            // Compare set and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

                char o = (char) hs.get(TestAccessMode.GET_AND_SET).invokeExact(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndSet char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "getAndSet char value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

                char o = (char) hs.get(TestAccessMode.GET_AND_SET_ACQUIRE).invokeExact(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndSetAcquire char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "getAndSetAcquire char value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

                char o = (char) hs.get(TestAccessMode.GET_AND_SET_RELEASE).invokeExact(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndSetRelease char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, '\u4567', "getAndSetRelease char value");
            }

            // get and add, add and get
            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

                char o = (char) hs.get(TestAccessMode.GET_AND_ADD).invokeExact(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndAdd char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

                char o = (char) hs.get(TestAccessMode.GET_AND_ADD_ACQUIRE).invokeExact(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndAddAcquire char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddAcquire char value");
            }

            {
                hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

                char o = (char) hs.get(TestAccessMode.GET_AND_ADD_RELEASE).invokeExact(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndAddRelease char");
                char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
                assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddRelease char value");
            }

        // get and bitwise or
        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOr char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOr char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR_ACQUIRE).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_OR_RELEASE).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrRelease char value");
        }

        // get and bitwise and
        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAnd char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAnd char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND_ACQUIRE).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_AND_RELEASE).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndRelease char value");
        }

        // get and bitwise xor
        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXor char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXor char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_ACQUIRE).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorAcquire char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorAcquire char value");
        }

        {
            hs.get(TestAccessMode.SET).invokeExact(array, i, '\u0123');

            char o = (char) hs.get(TestAccessMode.GET_AND_BITWISE_XOR_RELEASE).invokeExact(array, i, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorRelease char");
            char x = (char) hs.get(TestAccessMode.GET).invokeExact(array, i);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorRelease char value");
        }
        }
    }

    static void testArrayUnsupported(Handles hs) throws Throwable {
        char[] array = new char[10];

        final int i = 0;


    }

    static void testArrayIndexOutOfBounds(Handles hs) throws Throwable {
        char[] array = new char[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET)) {
                checkAIOOBE(am, () -> {
                    char x = (char) hs.get(am).invokeExact(array, ci);
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.SET)) {
                checkAIOOBE(am, () -> {
                    hs.get(am).invokeExact(array, ci, '\u0123');
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
                checkAIOOBE(am, () -> {
                    boolean r = (boolean) hs.get(am).invokeExact(array, ci, '\u0123', '\u4567');
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
                checkAIOOBE(am, () -> {
                    char r = (char) hs.get(am).invokeExact(array, ci, '\u4567', '\u0123');
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_SET)) {
                checkAIOOBE(am, () -> {
                    char o = (char) hs.get(am).invokeExact(array, ci, '\u0123');
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_ADD)) {
                checkAIOOBE(am, () -> {
                    char o = (char) hs.get(am).invokeExact(array, ci, '\u89AB');
                });
            }

            for (TestAccessMode am : testAccessModesOfType(TestAccessType.GET_AND_BITWISE)) {
                checkAIOOBE(am, () -> {
                    char o = (char) hs.get(am).invokeExact(array, ci, '\u89AB');
                });
            }
        }
    }
}

