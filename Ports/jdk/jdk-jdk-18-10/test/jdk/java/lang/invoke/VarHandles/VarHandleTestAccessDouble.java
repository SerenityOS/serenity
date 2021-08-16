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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessDouble
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessDouble
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessDouble
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessDouble
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

public class VarHandleTestAccessDouble extends VarHandleBaseTest {
    static final double static_final_v = 1.0d;

    static double static_v;

    final double final_v = 1.0d;

    double v;

    static final double static_final_v2 = 1.0d;

    static double static_v2;

    final double final_v2 = 1.0d;

    double v2;

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
                    VarHandleTestAccessDouble.class, "final_v" + postfix, double.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessDouble.class, "v" + postfix, double.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessDouble.class, "static_final_v" + postfix, double.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessDouble.class, "static_v" + postfix, double.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(double[].class);
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
                VarHandleTestAccessDouble.class, "final_v", double.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessDouble.class, "v", double.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessDouble.class, "static_final_v", double.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessDouble.class, "static_v", double.class);

        vhArray = MethodHandles.arrayElementVarHandle(double[].class);
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

        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_OR_RELEASE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_AND_RELEASE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_BITWISE_XOR_RELEASE));
    }


    @DataProvider
    public Object[][] typesProvider() throws Exception {
        List<Object[]> types = new ArrayList<>();
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessDouble.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(double[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), double.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessDouble.class, "final_v", double.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessDouble.class, "v", double.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessDouble.class, "static_final_v", double.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessDouble.class, "static_v", double.class);
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
                                              vhStaticFinalField, VarHandleTestAccessDouble::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessDouble::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessDouble::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessDouble::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessDouble::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessDouble::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessDouble::testArrayIndexOutOfBounds,
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




    static void testInstanceFinalField(VarHandleTestAccessDouble recv, VarHandle vh) {
        // Plain
        {
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "get double value");
        }


        // Volatile
        {
            double x = (double) vh.getVolatile(recv);
            assertEquals(x, 1.0d, "getVolatile double value");
        }

        // Lazy
        {
            double x = (double) vh.getAcquire(recv);
            assertEquals(x, 1.0d, "getRelease double value");
        }

        // Opaque
        {
            double x = (double) vh.getOpaque(recv);
            assertEquals(x, 1.0d, "getOpaque double value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessDouble recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, 2.0d);
        });

        checkUOE(() -> {
            vh.setVolatile(recv, 2.0d);
        });

        checkUOE(() -> {
            vh.setRelease(recv, 2.0d);
        });

        checkUOE(() -> {
            vh.setOpaque(recv, 2.0d);
        });



        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOr(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrAcquire(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrRelease(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAnd(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndAcquire(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndRelease(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXor(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorAcquire(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorRelease(recv, 1.0d);
        });
    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "get double value");
        }


        // Volatile
        {
            double x = (double) vh.getVolatile();
            assertEquals(x, 1.0d, "getVolatile double value");
        }

        // Lazy
        {
            double x = (double) vh.getAcquire();
            assertEquals(x, 1.0d, "getRelease double value");
        }

        // Opaque
        {
            double x = (double) vh.getOpaque();
            assertEquals(x, 1.0d, "getOpaque double value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set(2.0d);
        });

        checkUOE(() -> {
            vh.setVolatile(2.0d);
        });

        checkUOE(() -> {
            vh.setRelease(2.0d);
        });

        checkUOE(() -> {
            vh.setOpaque(2.0d);
        });



        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOr(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrAcquire(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrRelease(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAnd(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndAcquire(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndRelease(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXor(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorAcquire(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorRelease(1.0d);
        });
    }


    static void testInstanceField(VarHandleTestAccessDouble recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, 1.0d);
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "set double value");
        }


        // Volatile
        {
            vh.setVolatile(recv, 2.0d);
            double x = (double) vh.getVolatile(recv);
            assertEquals(x, 2.0d, "setVolatile double value");
        }

        // Lazy
        {
            vh.setRelease(recv, 1.0d);
            double x = (double) vh.getAcquire(recv);
            assertEquals(x, 1.0d, "setRelease double value");
        }

        // Opaque
        {
            vh.setOpaque(recv, 2.0d);
            double x = (double) vh.getOpaque(recv);
            assertEquals(x, 2.0d, "setOpaque double value");
        }

        vh.set(recv, 1.0d);

        // Compare
        {
            boolean r = vh.compareAndSet(recv, 1.0d, 2.0d);
            assertEquals(r, true, "success compareAndSet double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "success compareAndSet double value");
        }

        {
            boolean r = vh.compareAndSet(recv, 1.0d, 3.0d);
            assertEquals(r, false, "failing compareAndSet double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "failing compareAndSet double value");
        }

        {
            double r = (double) vh.compareAndExchange(recv, 2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchange double");
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "success compareAndExchange double value");
        }

        {
            double r = (double) vh.compareAndExchange(recv, 2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchange double");
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "failing compareAndExchange double value");
        }

        {
            double r = (double) vh.compareAndExchangeAcquire(recv, 1.0d, 2.0d);
            assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
        }

        {
            double r = (double) vh.compareAndExchangeAcquire(recv, 1.0d, 3.0d);
            assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
        }

        {
            double r = (double) vh.compareAndExchangeRelease(recv, 2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
        }

        {
            double r = (double) vh.compareAndExchangeRelease(recv, 2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, 1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetPlain double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, 2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire double");
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, 1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetRelease double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, 2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSet double");
            double x = (double) vh.get(recv);
            assertEquals(x, 1.0d, "weakCompareAndSet double value");
        }

        // Compare set and get
        {
            vh.set(recv, 1.0d);

            double o = (double) vh.getAndSet(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndSet double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "getAndSet double value");
        }

        {
            vh.set(recv, 1.0d);

            double o = (double) vh.getAndSetAcquire(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndSetAcquire double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "getAndSetAcquire double value");
        }

        {
            vh.set(recv, 1.0d);

            double o = (double) vh.getAndSetRelease(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndSetRelease double");
            double x = (double) vh.get(recv);
            assertEquals(x, 2.0d, "getAndSetRelease double value");
        }

        // get and add, add and get
        {
            vh.set(recv, 1.0d);

            double o = (double) vh.getAndAdd(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndAdd double");
            double x = (double) vh.get(recv);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double value");
        }

        {
            vh.set(recv, 1.0d);

            double o = (double) vh.getAndAddAcquire(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndAddAcquire double");
            double x = (double) vh.get(recv);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddAcquire double value");
        }

        {
            vh.set(recv, 1.0d);

            double o = (double) vh.getAndAddRelease(recv, 2.0d);
            assertEquals(o, 1.0d, "getAndAddReleasedouble");
            double x = (double) vh.get(recv);
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddRelease double value");
        }

    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessDouble recv, VarHandle vh) {


        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOr(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrAcquire(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrRelease(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAnd(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndAcquire(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndRelease(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXor(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorAcquire(recv, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorRelease(recv, 1.0d);
        });
    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set(1.0d);
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "set double value");
        }


        // Volatile
        {
            vh.setVolatile(2.0d);
            double x = (double) vh.getVolatile();
            assertEquals(x, 2.0d, "setVolatile double value");
        }

        // Lazy
        {
            vh.setRelease(1.0d);
            double x = (double) vh.getAcquire();
            assertEquals(x, 1.0d, "setRelease double value");
        }

        // Opaque
        {
            vh.setOpaque(2.0d);
            double x = (double) vh.getOpaque();
            assertEquals(x, 2.0d, "setOpaque double value");
        }

        vh.set(1.0d);

        // Compare
        {
            boolean r = vh.compareAndSet(1.0d, 2.0d);
            assertEquals(r, true, "success compareAndSet double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "success compareAndSet double value");
        }

        {
            boolean r = vh.compareAndSet(1.0d, 3.0d);
            assertEquals(r, false, "failing compareAndSet double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "failing compareAndSet double value");
        }

        {
            double r = (double) vh.compareAndExchange(2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchange double");
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "success compareAndExchange double value");
        }

        {
            double r = (double) vh.compareAndExchange(2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchange double");
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "failing compareAndExchange double value");
        }

        {
            double r = (double) vh.compareAndExchangeAcquire(1.0d, 2.0d);
            assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
        }

        {
            double r = (double) vh.compareAndExchangeAcquire(1.0d, 3.0d);
            assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
        }

        {
            double r = (double) vh.compareAndExchangeRelease(2.0d, 1.0d);
            assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
        }

        {
            double r = (double) vh.compareAndExchangeRelease(2.0d, 3.0d);
            assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetPlain double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSetAcquire double");
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(1.0d, 2.0d);
            }
            assertEquals(success, true, "weakCompareAndSetRelease double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(2.0d, 1.0d);
            }
            assertEquals(success, true, "weakCompareAndSet double");
            double x = (double) vh.get();
            assertEquals(x, 1.0d, "weakCompareAndSet double");
        }

        // Compare set and get
        {
            vh.set(1.0d);

            double o = (double) vh.getAndSet(2.0d);
            assertEquals(o, 1.0d, "getAndSet double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "getAndSet double value");
        }

        {
            vh.set(1.0d);

            double o = (double) vh.getAndSetAcquire(2.0d);
            assertEquals(o, 1.0d, "getAndSetAcquire double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "getAndSetAcquire double value");
        }

        {
            vh.set(1.0d);

            double o = (double) vh.getAndSetRelease(2.0d);
            assertEquals(o, 1.0d, "getAndSetRelease double");
            double x = (double) vh.get();
            assertEquals(x, 2.0d, "getAndSetRelease double value");
        }

        // get and add, add and get
        {
            vh.set(1.0d);

            double o = (double) vh.getAndAdd(2.0d);
            assertEquals(o, 1.0d, "getAndAdd double");
            double x = (double) vh.get();
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double value");
        }

        {
            vh.set(1.0d);

            double o = (double) vh.getAndAddAcquire(2.0d);
            assertEquals(o, 1.0d, "getAndAddAcquire double");
            double x = (double) vh.get();
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddAcquire double value");
        }

        {
            vh.set(1.0d);

            double o = (double) vh.getAndAddRelease(2.0d);
            assertEquals(o, 1.0d, "getAndAddReleasedouble");
            double x = (double) vh.get();
            assertEquals(x, (double)(1.0d + 2.0d), "getAndAddRelease double value");
        }

    }

    static void testStaticFieldUnsupported(VarHandle vh) {


        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOr(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrAcquire(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrRelease(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAnd(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndAcquire(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndRelease(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXor(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorAcquire(1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorRelease(1.0d);
        });
    }


    static void testArray(VarHandle vh) {
        double[] array = new double[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, 1.0d);
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "get double value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, 2.0d);
                double x = (double) vh.getVolatile(array, i);
                assertEquals(x, 2.0d, "setVolatile double value");
            }

            // Lazy
            {
                vh.setRelease(array, i, 1.0d);
                double x = (double) vh.getAcquire(array, i);
                assertEquals(x, 1.0d, "setRelease double value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, 2.0d);
                double x = (double) vh.getOpaque(array, i);
                assertEquals(x, 2.0d, "setOpaque double value");
            }

            vh.set(array, i, 1.0d);

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, 1.0d, 2.0d);
                assertEquals(r, true, "success compareAndSet double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "success compareAndSet double value");
            }

            {
                boolean r = vh.compareAndSet(array, i, 1.0d, 3.0d);
                assertEquals(r, false, "failing compareAndSet double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "failing compareAndSet double value");
            }

            {
                double r = (double) vh.compareAndExchange(array, i, 2.0d, 1.0d);
                assertEquals(r, 2.0d, "success compareAndExchange double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "success compareAndExchange double value");
            }

            {
                double r = (double) vh.compareAndExchange(array, i, 2.0d, 3.0d);
                assertEquals(r, 1.0d, "failing compareAndExchange double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "failing compareAndExchange double value");
            }

            {
                double r = (double) vh.compareAndExchangeAcquire(array, i, 1.0d, 2.0d);
                assertEquals(r, 1.0d, "success compareAndExchangeAcquire double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "success compareAndExchangeAcquire double value");
            }

            {
                double r = (double) vh.compareAndExchangeAcquire(array, i, 1.0d, 3.0d);
                assertEquals(r, 2.0d, "failing compareAndExchangeAcquire double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "failing compareAndExchangeAcquire double value");
            }

            {
                double r = (double) vh.compareAndExchangeRelease(array, i, 2.0d, 1.0d);
                assertEquals(r, 2.0d, "success compareAndExchangeRelease double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "success compareAndExchangeRelease double value");
            }

            {
                double r = (double) vh.compareAndExchangeRelease(array, i, 2.0d, 3.0d);
                assertEquals(r, 1.0d, "failing compareAndExchangeRelease double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "failing compareAndExchangeRelease double value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, 1.0d, 2.0d);
                }
                assertEquals(success, true, "weakCompareAndSetPlain double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "weakCompareAndSetPlain double value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, 2.0d, 1.0d);
                }
                assertEquals(success, true, "weakCompareAndSetAcquire double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "weakCompareAndSetAcquire double");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, 1.0d, 2.0d);
                }
                assertEquals(success, true, "weakCompareAndSetRelease double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "weakCompareAndSetRelease double");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, 2.0d, 1.0d);
                }
                assertEquals(success, true, "weakCompareAndSet double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 1.0d, "weakCompareAndSet double");
            }

            // Compare set and get
            {
                vh.set(array, i, 1.0d);

                double o = (double) vh.getAndSet(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndSet double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "getAndSet double value");
            }

            {
                vh.set(array, i, 1.0d);

                double o = (double) vh.getAndSetAcquire(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndSetAcquire double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "getAndSetAcquire double value");
            }

            {
                vh.set(array, i, 1.0d);

                double o = (double) vh.getAndSetRelease(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndSetRelease double");
                double x = (double) vh.get(array, i);
                assertEquals(x, 2.0d, "getAndSetRelease double value");
            }

            // get and add, add and get
            {
                vh.set(array, i, 1.0d);

                double o = (double) vh.getAndAdd(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndAdd double");
                double x = (double) vh.get(array, i);
                assertEquals(x, (double)(1.0d + 2.0d), "getAndAdd double value");
            }

            {
                vh.set(array, i, 1.0d);

                double o = (double) vh.getAndAddAcquire(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndAddAcquire double");
                double x = (double) vh.get(array, i);
                assertEquals(x, (double)(1.0d + 2.0d), "getAndAddAcquire double value");
            }

            {
                vh.set(array, i, 1.0d);

                double o = (double) vh.getAndAddRelease(array, i, 2.0d);
                assertEquals(o, 1.0d, "getAndAddReleasedouble");
                double x = (double) vh.get(array, i);
                assertEquals(x, (double)(1.0d + 2.0d), "getAndAddRelease double value");
            }

        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        double[] array = new double[10];

        int i = 0;


        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOr(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrAcquire(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseOrRelease(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAnd(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndAcquire(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseAndRelease(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXor(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorAcquire(array, i, 1.0d);
        });

        checkUOE(() -> {
            double o = (double) vh.getAndBitwiseXorRelease(array, i, 1.0d);
        });
    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        double[] array = new double[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                double x = (double) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double x = (double) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double x = (double) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double x = (double) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, 1.0d, 2.0d);
            });

            checkAIOOBE(() -> {
                double r = (double) vh.compareAndExchange(array, ci, 2.0d, 1.0d);
            });

            checkAIOOBE(() -> {
                double r = (double) vh.compareAndExchangeAcquire(array, ci, 2.0d, 1.0d);
            });

            checkAIOOBE(() -> {
                double r = (double) vh.compareAndExchangeRelease(array, ci, 2.0d, 1.0d);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, 1.0d, 2.0d);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, 1.0d, 2.0d);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, 1.0d, 2.0d);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, 1.0d, 2.0d);
            });

            checkAIOOBE(() -> {
                double o = (double) vh.getAndSet(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double o = (double) vh.getAndSetAcquire(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double o = (double) vh.getAndSetRelease(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double o = (double) vh.getAndAdd(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double o = (double) vh.getAndAddAcquire(array, ci, 1.0d);
            });

            checkAIOOBE(() -> {
                double o = (double) vh.getAndAddRelease(array, ci, 1.0d);
            });

        }
    }

}

