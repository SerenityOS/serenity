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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessLong
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessLong
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessLong
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessLong
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

public class VarHandleTestAccessLong extends VarHandleBaseTest {
    static final long static_final_v = 0x0123456789ABCDEFL;

    static long static_v;

    final long final_v = 0x0123456789ABCDEFL;

    long v;

    static final long static_final_v2 = 0x0123456789ABCDEFL;

    static long static_v2;

    final long final_v2 = 0x0123456789ABCDEFL;

    long v2;

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
                    VarHandleTestAccessLong.class, "final_v" + postfix, long.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessLong.class, "v" + postfix, long.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessLong.class, "static_final_v" + postfix, long.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessLong.class, "static_v" + postfix, long.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(long[].class);
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
                VarHandleTestAccessLong.class, "final_v", long.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessLong.class, "v", long.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessLong.class, "static_final_v", long.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessLong.class, "static_v", long.class);

        vhArray = MethodHandles.arrayElementVarHandle(long[].class);
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
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessLong.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(long[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), long.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessLong.class, "final_v", long.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessLong.class, "v", long.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessLong.class, "static_final_v", long.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessLong.class, "static_v", long.class);
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
                                              vhStaticFinalField, VarHandleTestAccessLong::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessLong::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessLong::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessLong::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessLong::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessLong::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessLong::testArrayIndexOutOfBounds,
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




    static void testInstanceFinalField(VarHandleTestAccessLong recv, VarHandle vh) {
        // Plain
        {
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "get long value");
        }


        // Volatile
        {
            long x = (long) vh.getVolatile(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "getVolatile long value");
        }

        // Lazy
        {
            long x = (long) vh.getAcquire(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "getRelease long value");
        }

        // Opaque
        {
            long x = (long) vh.getOpaque(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "getOpaque long value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessLong recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, 0xCAFEBABECAFEBABEL);
        });

        checkUOE(() -> {
            vh.setVolatile(recv, 0xCAFEBABECAFEBABEL);
        });

        checkUOE(() -> {
            vh.setRelease(recv, 0xCAFEBABECAFEBABEL);
        });

        checkUOE(() -> {
            vh.setOpaque(recv, 0xCAFEBABECAFEBABEL);
        });



    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "get long value");
        }


        // Volatile
        {
            long x = (long) vh.getVolatile();
            assertEquals(x, 0x0123456789ABCDEFL, "getVolatile long value");
        }

        // Lazy
        {
            long x = (long) vh.getAcquire();
            assertEquals(x, 0x0123456789ABCDEFL, "getRelease long value");
        }

        // Opaque
        {
            long x = (long) vh.getOpaque();
            assertEquals(x, 0x0123456789ABCDEFL, "getOpaque long value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set(0xCAFEBABECAFEBABEL);
        });

        checkUOE(() -> {
            vh.setVolatile(0xCAFEBABECAFEBABEL);
        });

        checkUOE(() -> {
            vh.setRelease(0xCAFEBABECAFEBABEL);
        });

        checkUOE(() -> {
            vh.setOpaque(0xCAFEBABECAFEBABEL);
        });



    }


    static void testInstanceField(VarHandleTestAccessLong recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, 0x0123456789ABCDEFL);
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "set long value");
        }


        // Volatile
        {
            vh.setVolatile(recv, 0xCAFEBABECAFEBABEL);
            long x = (long) vh.getVolatile(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "setVolatile long value");
        }

        // Lazy
        {
            vh.setRelease(recv, 0x0123456789ABCDEFL);
            long x = (long) vh.getAcquire(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "setRelease long value");
        }

        // Opaque
        {
            vh.setOpaque(recv, 0xCAFEBABECAFEBABEL);
            long x = (long) vh.getOpaque(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "setOpaque long value");
        }

        vh.set(recv, 0x0123456789ABCDEFL);

        // Compare
        {
            boolean r = vh.compareAndSet(recv, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            assertEquals(r, true, "success compareAndSet long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "success compareAndSet long value");
        }

        {
            boolean r = vh.compareAndSet(recv, 0x0123456789ABCDEFL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, false, "failing compareAndSet long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "failing compareAndSet long value");
        }

        {
            long r = (long) vh.compareAndExchange(recv, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            assertEquals(r, 0xCAFEBABECAFEBABEL, "success compareAndExchange long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "success compareAndExchange long value");
        }

        {
            long r = (long) vh.compareAndExchange(recv, 0xCAFEBABECAFEBABEL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, 0x0123456789ABCDEFL, "failing compareAndExchange long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "failing compareAndExchange long value");
        }

        {
            long r = (long) vh.compareAndExchangeAcquire(recv, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            assertEquals(r, 0x0123456789ABCDEFL, "success compareAndExchangeAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "success compareAndExchangeAcquire long value");
        }

        {
            long r = (long) vh.compareAndExchangeAcquire(recv, 0x0123456789ABCDEFL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, 0xCAFEBABECAFEBABEL, "failing compareAndExchangeAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "failing compareAndExchangeAcquire long value");
        }

        {
            long r = (long) vh.compareAndExchangeRelease(recv, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            assertEquals(r, 0xCAFEBABECAFEBABEL, "success compareAndExchangeRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "success compareAndExchangeRelease long value");
        }

        {
            long r = (long) vh.compareAndExchangeRelease(recv, 0xCAFEBABECAFEBABEL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, 0x0123456789ABCDEFL, "failing compareAndExchangeRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "failing compareAndExchangeRelease long value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            }
            assertEquals(success, true, "weakCompareAndSetPlain long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "weakCompareAndSetPlain long value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "weakCompareAndSetAcquire long");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            }
            assertEquals(success, true, "weakCompareAndSetRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "weakCompareAndSetRelease long");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            }
            assertEquals(success, true, "weakCompareAndSet long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0x0123456789ABCDEFL, "weakCompareAndSet long value");
        }

        // Compare set and get
        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndSet(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndSet long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSet long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndSetAcquire(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndSetAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSetAcquire long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndSetRelease(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndSetRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSetRelease long value");
        }

        // get and add, add and get
        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndAdd(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndAdd long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAdd long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndAddAcquire(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndAddAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAddAcquire long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndAddRelease(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndAddReleaselong");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAddRelease long value");
        }

        // get and bitwise or
        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseOr(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOr long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOr long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseOrAcquire(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOrAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOrAcquire long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseOrRelease(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOrRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOrRelease long value");
        }

        // get and bitwise and
        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseAnd(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAnd long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAnd long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseAndAcquire(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAndAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAndAcquire long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseAndRelease(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAndRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAndRelease long value");
        }

        // get and bitwise xor
        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseXor(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXor long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXor long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseXorAcquire(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXorAcquire long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXorAcquire long value");
        }

        {
            vh.set(recv, 0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseXorRelease(recv, 0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXorRelease long");
            long x = (long) vh.get(recv);
            assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXorRelease long value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessLong recv, VarHandle vh) {


    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set(0x0123456789ABCDEFL);
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "set long value");
        }


        // Volatile
        {
            vh.setVolatile(0xCAFEBABECAFEBABEL);
            long x = (long) vh.getVolatile();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "setVolatile long value");
        }

        // Lazy
        {
            vh.setRelease(0x0123456789ABCDEFL);
            long x = (long) vh.getAcquire();
            assertEquals(x, 0x0123456789ABCDEFL, "setRelease long value");
        }

        // Opaque
        {
            vh.setOpaque(0xCAFEBABECAFEBABEL);
            long x = (long) vh.getOpaque();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "setOpaque long value");
        }

        vh.set(0x0123456789ABCDEFL);

        // Compare
        {
            boolean r = vh.compareAndSet(0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            assertEquals(r, true, "success compareAndSet long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "success compareAndSet long value");
        }

        {
            boolean r = vh.compareAndSet(0x0123456789ABCDEFL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, false, "failing compareAndSet long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "failing compareAndSet long value");
        }

        {
            long r = (long) vh.compareAndExchange(0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            assertEquals(r, 0xCAFEBABECAFEBABEL, "success compareAndExchange long");
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "success compareAndExchange long value");
        }

        {
            long r = (long) vh.compareAndExchange(0xCAFEBABECAFEBABEL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, 0x0123456789ABCDEFL, "failing compareAndExchange long");
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "failing compareAndExchange long value");
        }

        {
            long r = (long) vh.compareAndExchangeAcquire(0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            assertEquals(r, 0x0123456789ABCDEFL, "success compareAndExchangeAcquire long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "success compareAndExchangeAcquire long value");
        }

        {
            long r = (long) vh.compareAndExchangeAcquire(0x0123456789ABCDEFL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, 0xCAFEBABECAFEBABEL, "failing compareAndExchangeAcquire long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "failing compareAndExchangeAcquire long value");
        }

        {
            long r = (long) vh.compareAndExchangeRelease(0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            assertEquals(r, 0xCAFEBABECAFEBABEL, "success compareAndExchangeRelease long");
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "success compareAndExchangeRelease long value");
        }

        {
            long r = (long) vh.compareAndExchangeRelease(0xCAFEBABECAFEBABEL, 0xDEADBEEFDEADBEEFL);
            assertEquals(r, 0x0123456789ABCDEFL, "failing compareAndExchangeRelease long");
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "failing compareAndExchangeRelease long value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            }
            assertEquals(success, true, "weakCompareAndSetPlain long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "weakCompareAndSetPlain long value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire long");
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "weakCompareAndSetAcquire long");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            }
            assertEquals(success, true, "weakCompareAndSetRelease long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "weakCompareAndSetRelease long");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            }
            assertEquals(success, true, "weakCompareAndSet long");
            long x = (long) vh.get();
            assertEquals(x, 0x0123456789ABCDEFL, "weakCompareAndSet long");
        }

        // Compare set and get
        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndSet(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndSet long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSet long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndSetAcquire(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndSetAcquire long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSetAcquire long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndSetRelease(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndSetRelease long");
            long x = (long) vh.get();
            assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSetRelease long value");
        }

        // get and add, add and get
        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndAdd(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndAdd long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAdd long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndAddAcquire(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndAddAcquire long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAddAcquire long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndAddRelease(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndAddReleaselong");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAddRelease long value");
        }

        // get and bitwise or
        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseOr(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOr long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOr long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseOrAcquire(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOrAcquire long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOrAcquire long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseOrRelease(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOrRelease long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOrRelease long value");
        }

        // get and bitwise and
        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseAnd(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAnd long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAnd long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseAndAcquire(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAndAcquire long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAndAcquire long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseAndRelease(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAndRelease long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAndRelease long value");
        }

        // get and bitwise xor
        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseXor(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXor long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXor long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseXorAcquire(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXorAcquire long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXorAcquire long value");
        }

        {
            vh.set(0x0123456789ABCDEFL);

            long o = (long) vh.getAndBitwiseXorRelease(0xCAFEBABECAFEBABEL);
            assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXorRelease long");
            long x = (long) vh.get();
            assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXorRelease long value");
        }
    }

    static void testStaticFieldUnsupported(VarHandle vh) {


    }


    static void testArray(VarHandle vh) {
        long[] array = new long[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, 0x0123456789ABCDEFL);
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "get long value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, 0xCAFEBABECAFEBABEL);
                long x = (long) vh.getVolatile(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "setVolatile long value");
            }

            // Lazy
            {
                vh.setRelease(array, i, 0x0123456789ABCDEFL);
                long x = (long) vh.getAcquire(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "setRelease long value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, 0xCAFEBABECAFEBABEL);
                long x = (long) vh.getOpaque(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "setOpaque long value");
            }

            vh.set(array, i, 0x0123456789ABCDEFL);

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
                assertEquals(r, true, "success compareAndSet long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "success compareAndSet long value");
            }

            {
                boolean r = vh.compareAndSet(array, i, 0x0123456789ABCDEFL, 0xDEADBEEFDEADBEEFL);
                assertEquals(r, false, "failing compareAndSet long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "failing compareAndSet long value");
            }

            {
                long r = (long) vh.compareAndExchange(array, i, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
                assertEquals(r, 0xCAFEBABECAFEBABEL, "success compareAndExchange long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "success compareAndExchange long value");
            }

            {
                long r = (long) vh.compareAndExchange(array, i, 0xCAFEBABECAFEBABEL, 0xDEADBEEFDEADBEEFL);
                assertEquals(r, 0x0123456789ABCDEFL, "failing compareAndExchange long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "failing compareAndExchange long value");
            }

            {
                long r = (long) vh.compareAndExchangeAcquire(array, i, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
                assertEquals(r, 0x0123456789ABCDEFL, "success compareAndExchangeAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "success compareAndExchangeAcquire long value");
            }

            {
                long r = (long) vh.compareAndExchangeAcquire(array, i, 0x0123456789ABCDEFL, 0xDEADBEEFDEADBEEFL);
                assertEquals(r, 0xCAFEBABECAFEBABEL, "failing compareAndExchangeAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "failing compareAndExchangeAcquire long value");
            }

            {
                long r = (long) vh.compareAndExchangeRelease(array, i, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
                assertEquals(r, 0xCAFEBABECAFEBABEL, "success compareAndExchangeRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "success compareAndExchangeRelease long value");
            }

            {
                long r = (long) vh.compareAndExchangeRelease(array, i, 0xCAFEBABECAFEBABEL, 0xDEADBEEFDEADBEEFL);
                assertEquals(r, 0x0123456789ABCDEFL, "failing compareAndExchangeRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "failing compareAndExchangeRelease long value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
                }
                assertEquals(success, true, "weakCompareAndSetPlain long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "weakCompareAndSetPlain long value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "weakCompareAndSetAcquire long");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
                }
                assertEquals(success, true, "weakCompareAndSetRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "weakCompareAndSetRelease long");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
                }
                assertEquals(success, true, "weakCompareAndSet long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0x0123456789ABCDEFL, "weakCompareAndSet long");
            }

            // Compare set and get
            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndSet(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndSet long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSet long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndSetAcquire(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndSetAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSetAcquire long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndSetRelease(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndSetRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, 0xCAFEBABECAFEBABEL, "getAndSetRelease long value");
            }

            // get and add, add and get
            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndAdd(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndAdd long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAdd long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndAddAcquire(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndAddAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAddAcquire long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndAddRelease(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndAddReleaselong");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL + 0xCAFEBABECAFEBABEL), "getAndAddRelease long value");
            }

            // get and bitwise or
            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseOr(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOr long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOr long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseOrAcquire(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOrAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOrAcquire long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseOrRelease(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseOrRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL | 0xCAFEBABECAFEBABEL), "getAndBitwiseOrRelease long value");
            }

            // get and bitwise and
            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseAnd(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAnd long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAnd long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseAndAcquire(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAndAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAndAcquire long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseAndRelease(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseAndRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL & 0xCAFEBABECAFEBABEL), "getAndBitwiseAndRelease long value");
            }

            // get and bitwise xor
            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseXor(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXor long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXor long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseXorAcquire(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXorAcquire long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXorAcquire long value");
            }

            {
                vh.set(array, i, 0x0123456789ABCDEFL);

                long o = (long) vh.getAndBitwiseXorRelease(array, i, 0xCAFEBABECAFEBABEL);
                assertEquals(o, 0x0123456789ABCDEFL, "getAndBitwiseXorRelease long");
                long x = (long) vh.get(array, i);
                assertEquals(x, (long)(0x0123456789ABCDEFL ^ 0xCAFEBABECAFEBABEL), "getAndBitwiseXorRelease long value");
            }
        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        long[] array = new long[10];

        int i = 0;


    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        long[] array = new long[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                long x = (long) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long x = (long) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long x = (long) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long x = (long) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            });

            checkAIOOBE(() -> {
                long r = (long) vh.compareAndExchange(array, ci, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long r = (long) vh.compareAndExchangeAcquire(array, ci, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long r = (long) vh.compareAndExchangeRelease(array, ci, 0xCAFEBABECAFEBABEL, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, 0x0123456789ABCDEFL, 0xCAFEBABECAFEBABEL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndSet(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndSetAcquire(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndSetRelease(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndAdd(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndAddAcquire(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndAddRelease(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseOr(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseOrAcquire(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseOrRelease(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseAnd(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseAndAcquire(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseAndRelease(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseXor(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseXorAcquire(array, ci, 0x0123456789ABCDEFL);
            });

            checkAIOOBE(() -> {
                long o = (long) vh.getAndBitwiseXorRelease(array, ci, 0x0123456789ABCDEFL);
            });
        }
    }

}

