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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessShort
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessShort
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessShort
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessShort
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

public class VarHandleTestAccessShort extends VarHandleBaseTest {
    static final short static_final_v = (short)0x0123;

    static short static_v;

    final short final_v = (short)0x0123;

    short v;

    static final short static_final_v2 = (short)0x0123;

    static short static_v2;

    final short final_v2 = (short)0x0123;

    short v2;

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
                    VarHandleTestAccessShort.class, "final_v" + postfix, short.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessShort.class, "v" + postfix, short.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessShort.class, "static_final_v" + postfix, short.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessShort.class, "static_v" + postfix, short.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(short[].class);
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
                VarHandleTestAccessShort.class, "final_v", short.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "v", short.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessShort.class, "static_final_v", short.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessShort.class, "static_v", short.class);

        vhArray = MethodHandles.arrayElementVarHandle(short[].class);
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
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessShort.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(short[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), short.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessShort.class, "final_v", short.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessShort.class, "v", short.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "static_final_v", short.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessShort.class, "static_v", short.class);
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
                                              vhStaticFinalField, VarHandleTestAccessShort::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessShort::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessShort::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessShort::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessShort::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessShort::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessShort::testArrayIndexOutOfBounds,
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




    static void testInstanceFinalField(VarHandleTestAccessShort recv, VarHandle vh) {
        // Plain
        {
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "get short value");
        }


        // Volatile
        {
            short x = (short) vh.getVolatile(recv);
            assertEquals(x, (short)0x0123, "getVolatile short value");
        }

        // Lazy
        {
            short x = (short) vh.getAcquire(recv);
            assertEquals(x, (short)0x0123, "getRelease short value");
        }

        // Opaque
        {
            short x = (short) vh.getOpaque(recv);
            assertEquals(x, (short)0x0123, "getOpaque short value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessShort recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, (short)0x4567);
        });

        checkUOE(() -> {
            vh.setVolatile(recv, (short)0x4567);
        });

        checkUOE(() -> {
            vh.setRelease(recv, (short)0x4567);
        });

        checkUOE(() -> {
            vh.setOpaque(recv, (short)0x4567);
        });



    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "get short value");
        }


        // Volatile
        {
            short x = (short) vh.getVolatile();
            assertEquals(x, (short)0x0123, "getVolatile short value");
        }

        // Lazy
        {
            short x = (short) vh.getAcquire();
            assertEquals(x, (short)0x0123, "getRelease short value");
        }

        // Opaque
        {
            short x = (short) vh.getOpaque();
            assertEquals(x, (short)0x0123, "getOpaque short value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set((short)0x4567);
        });

        checkUOE(() -> {
            vh.setVolatile((short)0x4567);
        });

        checkUOE(() -> {
            vh.setRelease((short)0x4567);
        });

        checkUOE(() -> {
            vh.setOpaque((short)0x4567);
        });



    }


    static void testInstanceField(VarHandleTestAccessShort recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, (short)0x0123);
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "set short value");
        }


        // Volatile
        {
            vh.setVolatile(recv, (short)0x4567);
            short x = (short) vh.getVolatile(recv);
            assertEquals(x, (short)0x4567, "setVolatile short value");
        }

        // Lazy
        {
            vh.setRelease(recv, (short)0x0123);
            short x = (short) vh.getAcquire(recv);
            assertEquals(x, (short)0x0123, "setRelease short value");
        }

        // Opaque
        {
            vh.setOpaque(recv, (short)0x4567);
            short x = (short) vh.getOpaque(recv);
            assertEquals(x, (short)0x4567, "setOpaque short value");
        }

        vh.set(recv, (short)0x0123);

        // Compare
        {
            boolean r = vh.compareAndSet(recv, (short)0x0123, (short)0x4567);
            assertEquals(r, true, "success compareAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "success compareAndSet short value");
        }

        {
            boolean r = vh.compareAndSet(recv, (short)0x0123, (short)0x89AB);
            assertEquals(r, false, "failing compareAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "failing compareAndSet short value");
        }

        {
            short r = (short) vh.compareAndExchange(recv, (short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchange short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "success compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchange(recv, (short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchange short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire(recv, (short)0x0123, (short)0x4567);
            assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire(recv, (short)0x0123, (short)0x89AB);
            assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease(recv, (short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease(recv, (short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, (short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetPlain short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "weakCompareAndSetPlain short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, (short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, (short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, (short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x0123, "weakCompareAndSet short value");
        }

        // Compare set and get
        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndSet(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSet short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "getAndSet short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndSetAcquire(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSetAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "getAndSetAcquire short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndSetRelease(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSetRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)0x4567, "getAndSetRelease short value");
        }

        // get and add, add and get
        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndAdd(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAdd short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAdd short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndAddAcquire(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAddAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAddAcquire short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndAddRelease(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAddReleaseshort");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAddRelease short value");
        }

        // get and bitwise or
        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseOr(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseOr short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOr short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseOrAcquire(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseOrAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOrAcquire short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseOrRelease(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseOrRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOrRelease short value");
        }

        // get and bitwise and
        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseAnd(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseAnd short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAnd short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseAndAcquire(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseAndAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAndAcquire short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseAndRelease(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseAndRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAndRelease short value");
        }

        // get and bitwise xor
        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseXor(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseXor short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXor short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseXorAcquire(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseXorAcquire short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXorAcquire short value");
        }

        {
            vh.set(recv, (short)0x0123);

            short o = (short) vh.getAndBitwiseXorRelease(recv, (short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseXorRelease short");
            short x = (short) vh.get(recv);
            assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXorRelease short value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessShort recv, VarHandle vh) {


    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set((short)0x0123);
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "set short value");
        }


        // Volatile
        {
            vh.setVolatile((short)0x4567);
            short x = (short) vh.getVolatile();
            assertEquals(x, (short)0x4567, "setVolatile short value");
        }

        // Lazy
        {
            vh.setRelease((short)0x0123);
            short x = (short) vh.getAcquire();
            assertEquals(x, (short)0x0123, "setRelease short value");
        }

        // Opaque
        {
            vh.setOpaque((short)0x4567);
            short x = (short) vh.getOpaque();
            assertEquals(x, (short)0x4567, "setOpaque short value");
        }

        vh.set((short)0x0123);

        // Compare
        {
            boolean r = vh.compareAndSet((short)0x0123, (short)0x4567);
            assertEquals(r, true, "success compareAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "success compareAndSet short value");
        }

        {
            boolean r = vh.compareAndSet((short)0x0123, (short)0x89AB);
            assertEquals(r, false, "failing compareAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "failing compareAndSet short value");
        }

        {
            short r = (short) vh.compareAndExchange((short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchange short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "success compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchange((short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchange short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire((short)0x0123, (short)0x4567);
            assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeAcquire((short)0x0123, (short)0x89AB);
            assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease((short)0x4567, (short)0x0123);
            assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
        }

        {
            short r = (short) vh.compareAndExchangeRelease((short)0x4567, (short)0x89AB);
            assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain((short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetPlain short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "weakCompareAndSetPlain short value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire((short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease((short)0x0123, (short)0x4567);
            }
            assertEquals(success, true, "weakCompareAndSetRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet((short)0x4567, (short)0x0123);
            }
            assertEquals(success, true, "weakCompareAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x0123, "weakCompareAndSet short");
        }

        // Compare set and get
        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndSet((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSet short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "getAndSet short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndSetAcquire((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSetAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "getAndSetAcquire short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndSetRelease((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndSetRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)0x4567, "getAndSetRelease short value");
        }

        // get and add, add and get
        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndAdd((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAdd short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAdd short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndAddAcquire((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAddAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAddAcquire short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndAddRelease((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndAddReleaseshort");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAddRelease short value");
        }

        // get and bitwise or
        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseOr((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseOr short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOr short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseOrAcquire((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseOrAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOrAcquire short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseOrRelease((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseOrRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOrRelease short value");
        }

        // get and bitwise and
        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseAnd((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseAnd short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAnd short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseAndAcquire((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseAndAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAndAcquire short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseAndRelease((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseAndRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAndRelease short value");
        }

        // get and bitwise xor
        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseXor((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseXor short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXor short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseXorAcquire((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseXorAcquire short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXorAcquire short value");
        }

        {
            vh.set((short)0x0123);

            short o = (short) vh.getAndBitwiseXorRelease((short)0x4567);
            assertEquals(o, (short)0x0123, "getAndBitwiseXorRelease short");
            short x = (short) vh.get();
            assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXorRelease short value");
        }
    }

    static void testStaticFieldUnsupported(VarHandle vh) {


    }


    static void testArray(VarHandle vh) {
        short[] array = new short[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, (short)0x0123);
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "get short value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, (short)0x4567);
                short x = (short) vh.getVolatile(array, i);
                assertEquals(x, (short)0x4567, "setVolatile short value");
            }

            // Lazy
            {
                vh.setRelease(array, i, (short)0x0123);
                short x = (short) vh.getAcquire(array, i);
                assertEquals(x, (short)0x0123, "setRelease short value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, (short)0x4567);
                short x = (short) vh.getOpaque(array, i);
                assertEquals(x, (short)0x4567, "setOpaque short value");
            }

            vh.set(array, i, (short)0x0123);

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, (short)0x0123, (short)0x4567);
                assertEquals(r, true, "success compareAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "success compareAndSet short value");
            }

            {
                boolean r = vh.compareAndSet(array, i, (short)0x0123, (short)0x89AB);
                assertEquals(r, false, "failing compareAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "failing compareAndSet short value");
            }

            {
                short r = (short) vh.compareAndExchange(array, i, (short)0x4567, (short)0x0123);
                assertEquals(r, (short)0x4567, "success compareAndExchange short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "success compareAndExchange short value");
            }

            {
                short r = (short) vh.compareAndExchange(array, i, (short)0x4567, (short)0x89AB);
                assertEquals(r, (short)0x0123, "failing compareAndExchange short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "failing compareAndExchange short value");
            }

            {
                short r = (short) vh.compareAndExchangeAcquire(array, i, (short)0x0123, (short)0x4567);
                assertEquals(r, (short)0x0123, "success compareAndExchangeAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "success compareAndExchangeAcquire short value");
            }

            {
                short r = (short) vh.compareAndExchangeAcquire(array, i, (short)0x0123, (short)0x89AB);
                assertEquals(r, (short)0x4567, "failing compareAndExchangeAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "failing compareAndExchangeAcquire short value");
            }

            {
                short r = (short) vh.compareAndExchangeRelease(array, i, (short)0x4567, (short)0x0123);
                assertEquals(r, (short)0x4567, "success compareAndExchangeRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "success compareAndExchangeRelease short value");
            }

            {
                short r = (short) vh.compareAndExchangeRelease(array, i, (short)0x4567, (short)0x89AB);
                assertEquals(r, (short)0x0123, "failing compareAndExchangeRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "failing compareAndExchangeRelease short value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, (short)0x0123, (short)0x4567);
                }
                assertEquals(success, true, "weakCompareAndSetPlain short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "weakCompareAndSetPlain short value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, (short)0x4567, (short)0x0123);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "weakCompareAndSetAcquire short");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, (short)0x0123, (short)0x4567);
                }
                assertEquals(success, true, "weakCompareAndSetRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "weakCompareAndSetRelease short");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, (short)0x4567, (short)0x0123);
                }
                assertEquals(success, true, "weakCompareAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x0123, "weakCompareAndSet short");
            }

            // Compare set and get
            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndSet(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndSet short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "getAndSet short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndSetAcquire(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndSetAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "getAndSetAcquire short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndSetRelease(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndSetRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)0x4567, "getAndSetRelease short value");
            }

            // get and add, add and get
            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndAdd(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndAdd short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAdd short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndAddAcquire(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndAddAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAddAcquire short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndAddRelease(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndAddReleaseshort");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 + (short)0x4567), "getAndAddRelease short value");
            }

            // get and bitwise or
            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseOr(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseOr short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOr short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseOrAcquire(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseOrAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOrAcquire short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseOrRelease(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseOrRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 | (short)0x4567), "getAndBitwiseOrRelease short value");
            }

            // get and bitwise and
            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseAnd(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseAnd short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAnd short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseAndAcquire(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseAndAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAndAcquire short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseAndRelease(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseAndRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 & (short)0x4567), "getAndBitwiseAndRelease short value");
            }

            // get and bitwise xor
            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseXor(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseXor short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXor short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseXorAcquire(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseXorAcquire short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXorAcquire short value");
            }

            {
                vh.set(array, i, (short)0x0123);

                short o = (short) vh.getAndBitwiseXorRelease(array, i, (short)0x4567);
                assertEquals(o, (short)0x0123, "getAndBitwiseXorRelease short");
                short x = (short) vh.get(array, i);
                assertEquals(x, (short)((short)0x0123 ^ (short)0x4567), "getAndBitwiseXorRelease short value");
            }
        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        short[] array = new short[10];

        int i = 0;


    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        short[] array = new short[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                short x = (short) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short x = (short) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short x = (short) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short x = (short) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, (short)0x0123, (short)0x4567);
            });

            checkAIOOBE(() -> {
                short r = (short) vh.compareAndExchange(array, ci, (short)0x4567, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short r = (short) vh.compareAndExchangeAcquire(array, ci, (short)0x4567, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short r = (short) vh.compareAndExchangeRelease(array, ci, (short)0x4567, (short)0x0123);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, (short)0x0123, (short)0x4567);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, (short)0x0123, (short)0x4567);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, (short)0x0123, (short)0x4567);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, (short)0x0123, (short)0x4567);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndSet(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndSetAcquire(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndSetRelease(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndAdd(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndAddAcquire(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndAddRelease(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseOr(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseOrAcquire(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseOrRelease(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseAnd(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseAndAcquire(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseAndRelease(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseXor(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseXorAcquire(array, ci, (short)0x0123);
            });

            checkAIOOBE(() -> {
                short o = (short) vh.getAndBitwiseXorRelease(array, ci, (short)0x0123);
            });
        }
    }

}

