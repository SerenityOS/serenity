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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessInt
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessInt
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessInt
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessInt
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

public class VarHandleTestAccessInt extends VarHandleBaseTest {
    static final int static_final_v = 0x01234567;

    static int static_v;

    final int final_v = 0x01234567;

    int v;

    static final int static_final_v2 = 0x01234567;

    static int static_v2;

    final int final_v2 = 0x01234567;

    int v2;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;


    VarHandle[] allocate(boolean same) {
        List<VarHandle> vhs = new ArrayList<>();

        String postfix = same ? "" : "2";
        VarHandle vh;
        try {
            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessInt.class, "final_v" + postfix, int.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessInt.class, "v" + postfix, int.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessInt.class, "static_final_v" + postfix, int.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessInt.class, "static_v" + postfix, int.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(int[].class);
            }
            else {
                vh = MethodHandles.arrayElementVarHandle(String[].class);
            }
            vhs.add(vh);
        } catch (Exception e) {
            throw new InternalError(e);
        }
        return vhs.toArray(new VarHandle[0]);
    }

    @BeforeClass
    public void setup() throws Exception {
        vhFinalField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessInt.class, "final_v", int.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessInt.class, "v", int.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessInt.class, "static_final_v", int.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessInt.class, "static_v", int.class);

        vhArray = MethodHandles.arrayElementVarHandle(int[].class);
    }


    @DataProvider
    public Object[][] varHandlesProvider() throws Exception {
        List<VarHandle> vhs = new ArrayList<>();
        vhs.add(vhField);
        vhs.add(vhStaticField);
        vhs.add(vhArray);

        return vhs.stream().map(tc -> new Object[]{tc}).toArray(Object[][]::new);
    }

    @Test
    public void testEquals() {
        VarHandle[] vhs1 = allocate(true);
        VarHandle[] vhs2 = allocate(true);

        for (int i = 0; i < vhs1.length; i++) {
            for (int j = 0; j < vhs1.length; j++) {
                if (i != j) {
                    assertNotEquals(vhs1[i], vhs1[j]);
                    assertNotEquals(vhs1[i], vhs2[j]);
                }
            }
        }

        VarHandle[] vhs3 = allocate(false);
        for (int i = 0; i < vhs1.length; i++) {
            assertNotEquals(vhs1[i], vhs3[i]);
        }
    }

    @Test(dataProvider = "varHandlesProvider")
    public void testIsAccessModeSupported(VarHandle vh) {
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_VOLATILE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_OPAQUE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET_OPAQUE));

        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.COMPARE_AND_EXCHANGE_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_PLAIN));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.WEAK_COMPARE_AND_SET_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_SET_RELEASE));

        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD_RELEASE));

        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND_RELEASE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR_ACQUIRE));
        assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR_RELEASE));
    }


    @DataProvider
    public Object[][] typesProvider() throws Exception {
        List<Object[]> types = new ArrayList<>();
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessInt.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(int[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), int.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessInt.class, "final_v", int.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessInt.class, "v", int.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessInt.class, "static_final_v", int.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessInt.class, "static_v", int.class);
        });
    }


    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        cases.add(new VarHandleAccessTestCase("Instance final field",
                                              vhFinalField, vh -> testInstanceFinalField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance final field unsupported",
                                              vhFinalField, vh -> testInstanceFinalFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static final field",
                                              vhStaticFinalField, VarHandleTestAccessInt::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessInt::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessInt::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessInt::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessInt::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessInt::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessInt::testArrayIndexOutOfBounds,
                                              false));
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




    static void testInstanceFinalField(VarHandleTestAccessInt recv, VarHandle vh) {
        // Plain
        {
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "get int value");
        }


        // Volatile
        {
            int x = (int) vh.getVolatile(recv);
            assertEquals(x, 0x01234567, "getVolatile int value");
        }

        // Lazy
        {
            int x = (int) vh.getAcquire(recv);
            assertEquals(x, 0x01234567, "getRelease int value");
        }

        // Opaque
        {
            int x = (int) vh.getOpaque(recv);
            assertEquals(x, 0x01234567, "getOpaque int value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessInt recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, 0x89ABCDEF);
        });

        checkUOE(() -> {
            vh.setVolatile(recv, 0x89ABCDEF);
        });

        checkUOE(() -> {
            vh.setRelease(recv, 0x89ABCDEF);
        });

        checkUOE(() -> {
            vh.setOpaque(recv, 0x89ABCDEF);
        });



    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "get int value");
        }


        // Volatile
        {
            int x = (int) vh.getVolatile();
            assertEquals(x, 0x01234567, "getVolatile int value");
        }

        // Lazy
        {
            int x = (int) vh.getAcquire();
            assertEquals(x, 0x01234567, "getRelease int value");
        }

        // Opaque
        {
            int x = (int) vh.getOpaque();
            assertEquals(x, 0x01234567, "getOpaque int value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set(0x89ABCDEF);
        });

        checkUOE(() -> {
            vh.setVolatile(0x89ABCDEF);
        });

        checkUOE(() -> {
            vh.setRelease(0x89ABCDEF);
        });

        checkUOE(() -> {
            vh.setOpaque(0x89ABCDEF);
        });



    }


    static void testInstanceField(VarHandleTestAccessInt recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, 0x01234567);
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "set int value");
        }


        // Volatile
        {
            vh.setVolatile(recv, 0x89ABCDEF);
            int x = (int) vh.getVolatile(recv);
            assertEquals(x, 0x89ABCDEF, "setVolatile int value");
        }

        // Lazy
        {
            vh.setRelease(recv, 0x01234567);
            int x = (int) vh.getAcquire(recv);
            assertEquals(x, 0x01234567, "setRelease int value");
        }

        // Opaque
        {
            vh.setOpaque(recv, 0x89ABCDEF);
            int x = (int) vh.getOpaque(recv);
            assertEquals(x, 0x89ABCDEF, "setOpaque int value");
        }

        vh.set(recv, 0x01234567);

        // Compare
        {
            boolean r = vh.compareAndSet(recv, 0x01234567, 0x89ABCDEF);
            assertEquals(r, true, "success compareAndSet int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "success compareAndSet int value");
        }

        {
            boolean r = vh.compareAndSet(recv, 0x01234567, 0xCAFEBABE);
            assertEquals(r, false, "failing compareAndSet int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "failing compareAndSet int value");
        }

        {
            int r = (int) vh.compareAndExchange(recv, 0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchange int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "success compareAndExchange int value");
        }

        {
            int r = (int) vh.compareAndExchange(recv, 0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchange int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "failing compareAndExchange int value");
        }

        {
            int r = (int) vh.compareAndExchangeAcquire(recv, 0x01234567, 0x89ABCDEF);
            assertEquals(r, 0x01234567, "success compareAndExchangeAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "success compareAndExchangeAcquire int value");
        }

        {
            int r = (int) vh.compareAndExchangeAcquire(recv, 0x01234567, 0xCAFEBABE);
            assertEquals(r, 0x89ABCDEF, "failing compareAndExchangeAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "failing compareAndExchangeAcquire int value");
        }

        {
            int r = (int) vh.compareAndExchangeRelease(recv, 0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchangeRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "success compareAndExchangeRelease int value");
        }

        {
            int r = (int) vh.compareAndExchangeRelease(recv, 0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchangeRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "failing compareAndExchangeRelease int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, 0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetPlain int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetPlain int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, 0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "weakCompareAndSetAcquire int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, 0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetRelease int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, 0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSet int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x01234567, "weakCompareAndSet int value");
        }

        // Compare set and get
        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndSet(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSet int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "getAndSet int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndSetAcquire(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSetAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "getAndSetAcquire int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndSetRelease(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSetRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, 0x89ABCDEF, "getAndSetRelease int value");
        }

        // get and add, add and get
        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndAdd(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAdd int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndAddAcquire(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddAcquire int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndAddRelease(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddReleaseint");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddRelease int value");
        }

        // get and bitwise or
        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseOr(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOr int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOr int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseOrAcquire(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrAcquire int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseOrRelease(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrRelease int value");
        }

        // get and bitwise and
        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseAnd(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAnd int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAnd int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseAndAcquire(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndAcquire int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseAndRelease(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndRelease int value");
        }

        // get and bitwise xor
        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseXor(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXor int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXor int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseXorAcquire(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorAcquire int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorAcquire int value");
        }

        {
            vh.set(recv, 0x01234567);

            int o = (int) vh.getAndBitwiseXorRelease(recv, 0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorRelease int");
            int x = (int) vh.get(recv);
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorRelease int value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessInt recv, VarHandle vh) {


    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set(0x01234567);
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "set int value");
        }


        // Volatile
        {
            vh.setVolatile(0x89ABCDEF);
            int x = (int) vh.getVolatile();
            assertEquals(x, 0x89ABCDEF, "setVolatile int value");
        }

        // Lazy
        {
            vh.setRelease(0x01234567);
            int x = (int) vh.getAcquire();
            assertEquals(x, 0x01234567, "setRelease int value");
        }

        // Opaque
        {
            vh.setOpaque(0x89ABCDEF);
            int x = (int) vh.getOpaque();
            assertEquals(x, 0x89ABCDEF, "setOpaque int value");
        }

        vh.set(0x01234567);

        // Compare
        {
            boolean r = vh.compareAndSet(0x01234567, 0x89ABCDEF);
            assertEquals(r, true, "success compareAndSet int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "success compareAndSet int value");
        }

        {
            boolean r = vh.compareAndSet(0x01234567, 0xCAFEBABE);
            assertEquals(r, false, "failing compareAndSet int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "failing compareAndSet int value");
        }

        {
            int r = (int) vh.compareAndExchange(0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchange int");
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "success compareAndExchange int value");
        }

        {
            int r = (int) vh.compareAndExchange(0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchange int");
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "failing compareAndExchange int value");
        }

        {
            int r = (int) vh.compareAndExchangeAcquire(0x01234567, 0x89ABCDEF);
            assertEquals(r, 0x01234567, "success compareAndExchangeAcquire int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "success compareAndExchangeAcquire int value");
        }

        {
            int r = (int) vh.compareAndExchangeAcquire(0x01234567, 0xCAFEBABE);
            assertEquals(r, 0x89ABCDEF, "failing compareAndExchangeAcquire int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "failing compareAndExchangeAcquire int value");
        }

        {
            int r = (int) vh.compareAndExchangeRelease(0x89ABCDEF, 0x01234567);
            assertEquals(r, 0x89ABCDEF, "success compareAndExchangeRelease int");
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "success compareAndExchangeRelease int value");
        }

        {
            int r = (int) vh.compareAndExchangeRelease(0x89ABCDEF, 0xCAFEBABE);
            assertEquals(r, 0x01234567, "failing compareAndExchangeRelease int");
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "failing compareAndExchangeRelease int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetPlain int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetPlain int value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire int");
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "weakCompareAndSetAcquire int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(0x01234567, 0x89ABCDEF);
            }
            assertEquals(success, true, "weakCompareAndSetRelease int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "weakCompareAndSetRelease int");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(0x89ABCDEF, 0x01234567);
            }
            assertEquals(success, true, "weakCompareAndSet int");
            int x = (int) vh.get();
            assertEquals(x, 0x01234567, "weakCompareAndSet int");
        }

        // Compare set and get
        {
            vh.set(0x01234567);

            int o = (int) vh.getAndSet(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSet int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "getAndSet int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndSetAcquire(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSetAcquire int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "getAndSetAcquire int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndSetRelease(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndSetRelease int");
            int x = (int) vh.get();
            assertEquals(x, 0x89ABCDEF, "getAndSetRelease int value");
        }

        // get and add, add and get
        {
            vh.set(0x01234567);

            int o = (int) vh.getAndAdd(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAdd int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndAddAcquire(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddAcquire int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddAcquire int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndAddRelease(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndAddReleaseint");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddRelease int value");
        }

        // get and bitwise or
        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseOr(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOr int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOr int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseOrAcquire(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrAcquire int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrAcquire int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseOrRelease(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseOrRelease int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrRelease int value");
        }

        // get and bitwise and
        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseAnd(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAnd int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAnd int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseAndAcquire(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndAcquire int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndAcquire int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseAndRelease(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseAndRelease int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndRelease int value");
        }

        // get and bitwise xor
        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseXor(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXor int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXor int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseXorAcquire(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorAcquire int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorAcquire int value");
        }

        {
            vh.set(0x01234567);

            int o = (int) vh.getAndBitwiseXorRelease(0x89ABCDEF);
            assertEquals(o, 0x01234567, "getAndBitwiseXorRelease int");
            int x = (int) vh.get();
            assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorRelease int value");
        }
    }

    static void testStaticFieldUnsupported(VarHandle vh) {


    }


    static void testArray(VarHandle vh) {
        int[] array = new int[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, 0x01234567);
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "get int value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, 0x89ABCDEF);
                int x = (int) vh.getVolatile(array, i);
                assertEquals(x, 0x89ABCDEF, "setVolatile int value");
            }

            // Lazy
            {
                vh.setRelease(array, i, 0x01234567);
                int x = (int) vh.getAcquire(array, i);
                assertEquals(x, 0x01234567, "setRelease int value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, 0x89ABCDEF);
                int x = (int) vh.getOpaque(array, i);
                assertEquals(x, 0x89ABCDEF, "setOpaque int value");
            }

            vh.set(array, i, 0x01234567);

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, 0x01234567, 0x89ABCDEF);
                assertEquals(r, true, "success compareAndSet int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "success compareAndSet int value");
            }

            {
                boolean r = vh.compareAndSet(array, i, 0x01234567, 0xCAFEBABE);
                assertEquals(r, false, "failing compareAndSet int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "failing compareAndSet int value");
            }

            {
                int r = (int) vh.compareAndExchange(array, i, 0x89ABCDEF, 0x01234567);
                assertEquals(r, 0x89ABCDEF, "success compareAndExchange int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "success compareAndExchange int value");
            }

            {
                int r = (int) vh.compareAndExchange(array, i, 0x89ABCDEF, 0xCAFEBABE);
                assertEquals(r, 0x01234567, "failing compareAndExchange int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "failing compareAndExchange int value");
            }

            {
                int r = (int) vh.compareAndExchangeAcquire(array, i, 0x01234567, 0x89ABCDEF);
                assertEquals(r, 0x01234567, "success compareAndExchangeAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "success compareAndExchangeAcquire int value");
            }

            {
                int r = (int) vh.compareAndExchangeAcquire(array, i, 0x01234567, 0xCAFEBABE);
                assertEquals(r, 0x89ABCDEF, "failing compareAndExchangeAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "failing compareAndExchangeAcquire int value");
            }

            {
                int r = (int) vh.compareAndExchangeRelease(array, i, 0x89ABCDEF, 0x01234567);
                assertEquals(r, 0x89ABCDEF, "success compareAndExchangeRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "success compareAndExchangeRelease int value");
            }

            {
                int r = (int) vh.compareAndExchangeRelease(array, i, 0x89ABCDEF, 0xCAFEBABE);
                assertEquals(r, 0x01234567, "failing compareAndExchangeRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "failing compareAndExchangeRelease int value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, 0x01234567, 0x89ABCDEF);
                }
                assertEquals(success, true, "weakCompareAndSetPlain int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "weakCompareAndSetPlain int value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, 0x89ABCDEF, 0x01234567);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "weakCompareAndSetAcquire int");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, 0x01234567, 0x89ABCDEF);
                }
                assertEquals(success, true, "weakCompareAndSetRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "weakCompareAndSetRelease int");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, 0x89ABCDEF, 0x01234567);
                }
                assertEquals(success, true, "weakCompareAndSet int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x01234567, "weakCompareAndSet int");
            }

            // Compare set and get
            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndSet(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndSet int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "getAndSet int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndSetAcquire(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndSetAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "getAndSetAcquire int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndSetRelease(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndSetRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, 0x89ABCDEF, "getAndSetRelease int value");
            }

            // get and add, add and get
            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndAdd(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndAdd int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAdd int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndAddAcquire(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndAddAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddAcquire int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndAddRelease(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndAddReleaseint");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 + 0x89ABCDEF), "getAndAddRelease int value");
            }

            // get and bitwise or
            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseOr(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseOr int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOr int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseOrAcquire(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseOrAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrAcquire int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseOrRelease(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseOrRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 | 0x89ABCDEF), "getAndBitwiseOrRelease int value");
            }

            // get and bitwise and
            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseAnd(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseAnd int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAnd int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseAndAcquire(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseAndAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndAcquire int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseAndRelease(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseAndRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 & 0x89ABCDEF), "getAndBitwiseAndRelease int value");
            }

            // get and bitwise xor
            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseXor(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseXor int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXor int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseXorAcquire(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseXorAcquire int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorAcquire int value");
            }

            {
                vh.set(array, i, 0x01234567);

                int o = (int) vh.getAndBitwiseXorRelease(array, i, 0x89ABCDEF);
                assertEquals(o, 0x01234567, "getAndBitwiseXorRelease int");
                int x = (int) vh.get(array, i);
                assertEquals(x, (int)(0x01234567 ^ 0x89ABCDEF), "getAndBitwiseXorRelease int value");
            }
        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        int[] array = new int[10];

        int i = 0;


    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        int[] array = new int[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                int x = (int) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int x = (int) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int x = (int) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int x = (int) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, 0x01234567, 0x89ABCDEF);
            });

            checkAIOOBE(() -> {
                int r = (int) vh.compareAndExchange(array, ci, 0x89ABCDEF, 0x01234567);
            });

            checkAIOOBE(() -> {
                int r = (int) vh.compareAndExchangeAcquire(array, ci, 0x89ABCDEF, 0x01234567);
            });

            checkAIOOBE(() -> {
                int r = (int) vh.compareAndExchangeRelease(array, ci, 0x89ABCDEF, 0x01234567);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, 0x01234567, 0x89ABCDEF);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, 0x01234567, 0x89ABCDEF);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, 0x01234567, 0x89ABCDEF);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, 0x01234567, 0x89ABCDEF);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndSet(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndSetAcquire(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndSetRelease(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndAdd(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndAddAcquire(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndAddRelease(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseOr(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseOrAcquire(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseOrRelease(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseAnd(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseAndAcquire(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseAndRelease(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseXor(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseXorAcquire(array, ci, 0x01234567);
            });

            checkAIOOBE(() -> {
                int o = (int) vh.getAndBitwiseXorRelease(array, ci, 0x01234567);
            });
        }
    }

}

