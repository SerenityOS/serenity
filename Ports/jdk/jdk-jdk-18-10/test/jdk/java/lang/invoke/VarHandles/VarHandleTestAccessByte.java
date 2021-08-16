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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessByte
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessByte
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessByte
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessByte
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

public class VarHandleTestAccessByte extends VarHandleBaseTest {
    static final byte static_final_v = (byte)0x01;

    static byte static_v;

    final byte final_v = (byte)0x01;

    byte v;

    static final byte static_final_v2 = (byte)0x01;

    static byte static_v2;

    final byte final_v2 = (byte)0x01;

    byte v2;

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
                    VarHandleTestAccessByte.class, "final_v" + postfix, byte.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessByte.class, "v" + postfix, byte.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessByte.class, "static_final_v" + postfix, byte.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessByte.class, "static_v" + postfix, byte.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(byte[].class);
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
                VarHandleTestAccessByte.class, "final_v", byte.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessByte.class, "v", byte.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessByte.class, "static_final_v", byte.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessByte.class, "static_v", byte.class);

        vhArray = MethodHandles.arrayElementVarHandle(byte[].class);
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
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessByte.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(byte[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), byte.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessByte.class, "final_v", byte.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessByte.class, "v", byte.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessByte.class, "static_final_v", byte.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessByte.class, "static_v", byte.class);
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
                                              vhStaticFinalField, VarHandleTestAccessByte::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessByte::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessByte::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessByte::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessByte::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessByte::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessByte::testArrayIndexOutOfBounds,
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




    static void testInstanceFinalField(VarHandleTestAccessByte recv, VarHandle vh) {
        // Plain
        {
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "get byte value");
        }


        // Volatile
        {
            byte x = (byte) vh.getVolatile(recv);
            assertEquals(x, (byte)0x01, "getVolatile byte value");
        }

        // Lazy
        {
            byte x = (byte) vh.getAcquire(recv);
            assertEquals(x, (byte)0x01, "getRelease byte value");
        }

        // Opaque
        {
            byte x = (byte) vh.getOpaque(recv);
            assertEquals(x, (byte)0x01, "getOpaque byte value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessByte recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, (byte)0x23);
        });

        checkUOE(() -> {
            vh.setVolatile(recv, (byte)0x23);
        });

        checkUOE(() -> {
            vh.setRelease(recv, (byte)0x23);
        });

        checkUOE(() -> {
            vh.setOpaque(recv, (byte)0x23);
        });



    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "get byte value");
        }


        // Volatile
        {
            byte x = (byte) vh.getVolatile();
            assertEquals(x, (byte)0x01, "getVolatile byte value");
        }

        // Lazy
        {
            byte x = (byte) vh.getAcquire();
            assertEquals(x, (byte)0x01, "getRelease byte value");
        }

        // Opaque
        {
            byte x = (byte) vh.getOpaque();
            assertEquals(x, (byte)0x01, "getOpaque byte value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set((byte)0x23);
        });

        checkUOE(() -> {
            vh.setVolatile((byte)0x23);
        });

        checkUOE(() -> {
            vh.setRelease((byte)0x23);
        });

        checkUOE(() -> {
            vh.setOpaque((byte)0x23);
        });



    }


    static void testInstanceField(VarHandleTestAccessByte recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, (byte)0x01);
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "set byte value");
        }


        // Volatile
        {
            vh.setVolatile(recv, (byte)0x23);
            byte x = (byte) vh.getVolatile(recv);
            assertEquals(x, (byte)0x23, "setVolatile byte value");
        }

        // Lazy
        {
            vh.setRelease(recv, (byte)0x01);
            byte x = (byte) vh.getAcquire(recv);
            assertEquals(x, (byte)0x01, "setRelease byte value");
        }

        // Opaque
        {
            vh.setOpaque(recv, (byte)0x23);
            byte x = (byte) vh.getOpaque(recv);
            assertEquals(x, (byte)0x23, "setOpaque byte value");
        }

        vh.set(recv, (byte)0x01);

        // Compare
        {
            boolean r = vh.compareAndSet(recv, (byte)0x01, (byte)0x23);
            assertEquals(r, true, "success compareAndSet byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "success compareAndSet byte value");
        }

        {
            boolean r = vh.compareAndSet(recv, (byte)0x01, (byte)0x45);
            assertEquals(r, false, "failing compareAndSet byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "failing compareAndSet byte value");
        }

        {
            byte r = (byte) vh.compareAndExchange(recv, (byte)0x23, (byte)0x01);
            assertEquals(r, (byte)0x23, "success compareAndExchange byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "success compareAndExchange byte value");
        }

        {
            byte r = (byte) vh.compareAndExchange(recv, (byte)0x23, (byte)0x45);
            assertEquals(r, (byte)0x01, "failing compareAndExchange byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "failing compareAndExchange byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeAcquire(recv, (byte)0x01, (byte)0x23);
            assertEquals(r, (byte)0x01, "success compareAndExchangeAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "success compareAndExchangeAcquire byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeAcquire(recv, (byte)0x01, (byte)0x45);
            assertEquals(r, (byte)0x23, "failing compareAndExchangeAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "failing compareAndExchangeAcquire byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeRelease(recv, (byte)0x23, (byte)0x01);
            assertEquals(r, (byte)0x23, "success compareAndExchangeRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "success compareAndExchangeRelease byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeRelease(recv, (byte)0x23, (byte)0x45);
            assertEquals(r, (byte)0x01, "failing compareAndExchangeRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "failing compareAndExchangeRelease byte value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, (byte)0x01, (byte)0x23);
            }
            assertEquals(success, true, "weakCompareAndSetPlain byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "weakCompareAndSetPlain byte value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, (byte)0x23, (byte)0x01);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "weakCompareAndSetAcquire byte");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, (byte)0x01, (byte)0x23);
            }
            assertEquals(success, true, "weakCompareAndSetRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "weakCompareAndSetRelease byte");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, (byte)0x23, (byte)0x01);
            }
            assertEquals(success, true, "weakCompareAndSet byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x01, "weakCompareAndSet byte value");
        }

        // Compare set and get
        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndSet(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndSet byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "getAndSet byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndSetAcquire(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndSetAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "getAndSetAcquire byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndSetRelease(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndSetRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)0x23, "getAndSetRelease byte value");
        }

        // get and add, add and get
        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndAdd(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndAdd byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAdd byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndAddAcquire(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndAddAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAddAcquire byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndAddRelease(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndAddReleasebyte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAddRelease byte value");
        }

        // get and bitwise or
        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseOr(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseOr byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOr byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseOrAcquire(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseOrAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOrAcquire byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseOrRelease(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseOrRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOrRelease byte value");
        }

        // get and bitwise and
        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseAnd(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseAnd byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAnd byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseAndAcquire(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseAndAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAndAcquire byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseAndRelease(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseAndRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAndRelease byte value");
        }

        // get and bitwise xor
        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseXor(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseXor byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXor byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseXorAcquire(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseXorAcquire byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXorAcquire byte value");
        }

        {
            vh.set(recv, (byte)0x01);

            byte o = (byte) vh.getAndBitwiseXorRelease(recv, (byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseXorRelease byte");
            byte x = (byte) vh.get(recv);
            assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXorRelease byte value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessByte recv, VarHandle vh) {


    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set((byte)0x01);
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "set byte value");
        }


        // Volatile
        {
            vh.setVolatile((byte)0x23);
            byte x = (byte) vh.getVolatile();
            assertEquals(x, (byte)0x23, "setVolatile byte value");
        }

        // Lazy
        {
            vh.setRelease((byte)0x01);
            byte x = (byte) vh.getAcquire();
            assertEquals(x, (byte)0x01, "setRelease byte value");
        }

        // Opaque
        {
            vh.setOpaque((byte)0x23);
            byte x = (byte) vh.getOpaque();
            assertEquals(x, (byte)0x23, "setOpaque byte value");
        }

        vh.set((byte)0x01);

        // Compare
        {
            boolean r = vh.compareAndSet((byte)0x01, (byte)0x23);
            assertEquals(r, true, "success compareAndSet byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "success compareAndSet byte value");
        }

        {
            boolean r = vh.compareAndSet((byte)0x01, (byte)0x45);
            assertEquals(r, false, "failing compareAndSet byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "failing compareAndSet byte value");
        }

        {
            byte r = (byte) vh.compareAndExchange((byte)0x23, (byte)0x01);
            assertEquals(r, (byte)0x23, "success compareAndExchange byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "success compareAndExchange byte value");
        }

        {
            byte r = (byte) vh.compareAndExchange((byte)0x23, (byte)0x45);
            assertEquals(r, (byte)0x01, "failing compareAndExchange byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "failing compareAndExchange byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeAcquire((byte)0x01, (byte)0x23);
            assertEquals(r, (byte)0x01, "success compareAndExchangeAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "success compareAndExchangeAcquire byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeAcquire((byte)0x01, (byte)0x45);
            assertEquals(r, (byte)0x23, "failing compareAndExchangeAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "failing compareAndExchangeAcquire byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeRelease((byte)0x23, (byte)0x01);
            assertEquals(r, (byte)0x23, "success compareAndExchangeRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "success compareAndExchangeRelease byte value");
        }

        {
            byte r = (byte) vh.compareAndExchangeRelease((byte)0x23, (byte)0x45);
            assertEquals(r, (byte)0x01, "failing compareAndExchangeRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "failing compareAndExchangeRelease byte value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain((byte)0x01, (byte)0x23);
            }
            assertEquals(success, true, "weakCompareAndSetPlain byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "weakCompareAndSetPlain byte value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire((byte)0x23, (byte)0x01);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "weakCompareAndSetAcquire byte");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease((byte)0x01, (byte)0x23);
            }
            assertEquals(success, true, "weakCompareAndSetRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "weakCompareAndSetRelease byte");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet((byte)0x23, (byte)0x01);
            }
            assertEquals(success, true, "weakCompareAndSet byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x01, "weakCompareAndSet byte");
        }

        // Compare set and get
        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndSet((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndSet byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "getAndSet byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndSetAcquire((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndSetAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "getAndSetAcquire byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndSetRelease((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndSetRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)0x23, "getAndSetRelease byte value");
        }

        // get and add, add and get
        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndAdd((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndAdd byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAdd byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndAddAcquire((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndAddAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAddAcquire byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndAddRelease((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndAddReleasebyte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAddRelease byte value");
        }

        // get and bitwise or
        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseOr((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseOr byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOr byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseOrAcquire((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseOrAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOrAcquire byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseOrRelease((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseOrRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOrRelease byte value");
        }

        // get and bitwise and
        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseAnd((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseAnd byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAnd byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseAndAcquire((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseAndAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAndAcquire byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseAndRelease((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseAndRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAndRelease byte value");
        }

        // get and bitwise xor
        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseXor((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseXor byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXor byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseXorAcquire((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseXorAcquire byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXorAcquire byte value");
        }

        {
            vh.set((byte)0x01);

            byte o = (byte) vh.getAndBitwiseXorRelease((byte)0x23);
            assertEquals(o, (byte)0x01, "getAndBitwiseXorRelease byte");
            byte x = (byte) vh.get();
            assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXorRelease byte value");
        }
    }

    static void testStaticFieldUnsupported(VarHandle vh) {


    }


    static void testArray(VarHandle vh) {
        byte[] array = new byte[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, (byte)0x01);
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "get byte value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, (byte)0x23);
                byte x = (byte) vh.getVolatile(array, i);
                assertEquals(x, (byte)0x23, "setVolatile byte value");
            }

            // Lazy
            {
                vh.setRelease(array, i, (byte)0x01);
                byte x = (byte) vh.getAcquire(array, i);
                assertEquals(x, (byte)0x01, "setRelease byte value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, (byte)0x23);
                byte x = (byte) vh.getOpaque(array, i);
                assertEquals(x, (byte)0x23, "setOpaque byte value");
            }

            vh.set(array, i, (byte)0x01);

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, (byte)0x01, (byte)0x23);
                assertEquals(r, true, "success compareAndSet byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "success compareAndSet byte value");
            }

            {
                boolean r = vh.compareAndSet(array, i, (byte)0x01, (byte)0x45);
                assertEquals(r, false, "failing compareAndSet byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "failing compareAndSet byte value");
            }

            {
                byte r = (byte) vh.compareAndExchange(array, i, (byte)0x23, (byte)0x01);
                assertEquals(r, (byte)0x23, "success compareAndExchange byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "success compareAndExchange byte value");
            }

            {
                byte r = (byte) vh.compareAndExchange(array, i, (byte)0x23, (byte)0x45);
                assertEquals(r, (byte)0x01, "failing compareAndExchange byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "failing compareAndExchange byte value");
            }

            {
                byte r = (byte) vh.compareAndExchangeAcquire(array, i, (byte)0x01, (byte)0x23);
                assertEquals(r, (byte)0x01, "success compareAndExchangeAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "success compareAndExchangeAcquire byte value");
            }

            {
                byte r = (byte) vh.compareAndExchangeAcquire(array, i, (byte)0x01, (byte)0x45);
                assertEquals(r, (byte)0x23, "failing compareAndExchangeAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "failing compareAndExchangeAcquire byte value");
            }

            {
                byte r = (byte) vh.compareAndExchangeRelease(array, i, (byte)0x23, (byte)0x01);
                assertEquals(r, (byte)0x23, "success compareAndExchangeRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "success compareAndExchangeRelease byte value");
            }

            {
                byte r = (byte) vh.compareAndExchangeRelease(array, i, (byte)0x23, (byte)0x45);
                assertEquals(r, (byte)0x01, "failing compareAndExchangeRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "failing compareAndExchangeRelease byte value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, (byte)0x01, (byte)0x23);
                }
                assertEquals(success, true, "weakCompareAndSetPlain byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "weakCompareAndSetPlain byte value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, (byte)0x23, (byte)0x01);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "weakCompareAndSetAcquire byte");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, (byte)0x01, (byte)0x23);
                }
                assertEquals(success, true, "weakCompareAndSetRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "weakCompareAndSetRelease byte");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, (byte)0x23, (byte)0x01);
                }
                assertEquals(success, true, "weakCompareAndSet byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x01, "weakCompareAndSet byte");
            }

            // Compare set and get
            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndSet(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndSet byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "getAndSet byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndSetAcquire(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndSetAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "getAndSetAcquire byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndSetRelease(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndSetRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)0x23, "getAndSetRelease byte value");
            }

            // get and add, add and get
            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndAdd(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndAdd byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAdd byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndAddAcquire(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndAddAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAddAcquire byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndAddRelease(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndAddReleasebyte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 + (byte)0x23), "getAndAddRelease byte value");
            }

            // get and bitwise or
            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseOr(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseOr byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOr byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseOrAcquire(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseOrAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOrAcquire byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseOrRelease(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseOrRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 | (byte)0x23), "getAndBitwiseOrRelease byte value");
            }

            // get and bitwise and
            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseAnd(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseAnd byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAnd byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseAndAcquire(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseAndAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAndAcquire byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseAndRelease(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseAndRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 & (byte)0x23), "getAndBitwiseAndRelease byte value");
            }

            // get and bitwise xor
            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseXor(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseXor byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXor byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseXorAcquire(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseXorAcquire byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXorAcquire byte value");
            }

            {
                vh.set(array, i, (byte)0x01);

                byte o = (byte) vh.getAndBitwiseXorRelease(array, i, (byte)0x23);
                assertEquals(o, (byte)0x01, "getAndBitwiseXorRelease byte");
                byte x = (byte) vh.get(array, i);
                assertEquals(x, (byte)((byte)0x01 ^ (byte)0x23), "getAndBitwiseXorRelease byte value");
            }
        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        byte[] array = new byte[10];

        int i = 0;


    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        byte[] array = new byte[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                byte x = (byte) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte x = (byte) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte x = (byte) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte x = (byte) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, (byte)0x01, (byte)0x23);
            });

            checkAIOOBE(() -> {
                byte r = (byte) vh.compareAndExchange(array, ci, (byte)0x23, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte r = (byte) vh.compareAndExchangeAcquire(array, ci, (byte)0x23, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte r = (byte) vh.compareAndExchangeRelease(array, ci, (byte)0x23, (byte)0x01);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, (byte)0x01, (byte)0x23);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, (byte)0x01, (byte)0x23);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, (byte)0x01, (byte)0x23);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, (byte)0x01, (byte)0x23);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndSet(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndSetAcquire(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndSetRelease(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndAdd(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndAddAcquire(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndAddRelease(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseOr(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseOrAcquire(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseOrRelease(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseAnd(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseAndAcquire(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseAndRelease(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseXor(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseXorAcquire(array, ci, (byte)0x01);
            });

            checkAIOOBE(() -> {
                byte o = (byte) vh.getAndBitwiseXorRelease(array, ci, (byte)0x01);
            });
        }
    }

}

