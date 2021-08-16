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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessString
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessString
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessString
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessString
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

public class VarHandleTestAccessString extends VarHandleBaseTest {
    static final String static_final_v = "foo";

    static String static_v;

    final String final_v = "foo";

    String v;

    static final String static_final_v2 = "foo";

    static String static_v2;

    final String final_v2 = "foo";

    String v2;

    VarHandle vhFinalField;

    VarHandle vhField;

    VarHandle vhStaticField;

    VarHandle vhStaticFinalField;

    VarHandle vhArray;

    VarHandle vhArrayObject;

    VarHandle[] allocate(boolean same) {
        List<VarHandle> vhs = new ArrayList<>();

        String postfix = same ? "" : "2";
        VarHandle vh;
        try {
            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessString.class, "final_v" + postfix, String.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessString.class, "v" + postfix, String.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessString.class, "static_final_v" + postfix, String.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessString.class, "static_v" + postfix, String.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(String[].class);
            }
            else {
                vh = MethodHandles.arrayElementVarHandle(int[].class);
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
                VarHandleTestAccessString.class, "final_v", String.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessString.class, "v", String.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessString.class, "static_final_v", String.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessString.class, "static_v", String.class);

        vhArray = MethodHandles.arrayElementVarHandle(String[].class);
        vhArrayObject = MethodHandles.arrayElementVarHandle(Object[].class);
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

        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD_ACQUIRE));
        assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.GET_AND_ADD_RELEASE));

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
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessString.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(String[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), String.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessString.class, "final_v", String.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessString.class, "v", String.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessString.class, "static_final_v", String.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessString.class, "static_v", String.class);
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
                                              vhStaticFinalField, VarHandleTestAccessString::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessString::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessString::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessString::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessString::testArray));
        cases.add(new VarHandleAccessTestCase("Array Object[]",
                                              vhArrayObject, VarHandleTestAccessString::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessString::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessString::testArrayIndexOutOfBounds,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array store exception",
                                              vhArrayObject, VarHandleTestAccessString::testArrayStoreException,
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




    static void testInstanceFinalField(VarHandleTestAccessString recv, VarHandle vh) {
        // Plain
        {
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "get String value");
        }


        // Volatile
        {
            String x = (String) vh.getVolatile(recv);
            assertEquals(x, "foo", "getVolatile String value");
        }

        // Lazy
        {
            String x = (String) vh.getAcquire(recv);
            assertEquals(x, "foo", "getRelease String value");
        }

        // Opaque
        {
            String x = (String) vh.getOpaque(recv);
            assertEquals(x, "foo", "getOpaque String value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessString recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, "bar");
        });

        checkUOE(() -> {
            vh.setVolatile(recv, "bar");
        });

        checkUOE(() -> {
            vh.setRelease(recv, "bar");
        });

        checkUOE(() -> {
            vh.setOpaque(recv, "bar");
        });


        checkUOE(() -> {
            String o = (String) vh.getAndAdd(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddRelease(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOr(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrRelease(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAnd(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndRelease(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXor(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorRelease(recv, "foo");
        });
    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            String x = (String) vh.get();
            assertEquals(x, "foo", "get String value");
        }


        // Volatile
        {
            String x = (String) vh.getVolatile();
            assertEquals(x, "foo", "getVolatile String value");
        }

        // Lazy
        {
            String x = (String) vh.getAcquire();
            assertEquals(x, "foo", "getRelease String value");
        }

        // Opaque
        {
            String x = (String) vh.getOpaque();
            assertEquals(x, "foo", "getOpaque String value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set("bar");
        });

        checkUOE(() -> {
            vh.setVolatile("bar");
        });

        checkUOE(() -> {
            vh.setRelease("bar");
        });

        checkUOE(() -> {
            vh.setOpaque("bar");
        });


        checkUOE(() -> {
            String o = (String) vh.getAndAdd("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddRelease("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOr("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrRelease("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAnd("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndRelease("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXor("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorRelease("foo");
        });
    }


    static void testInstanceField(VarHandleTestAccessString recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, "foo");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "set String value");
        }


        // Volatile
        {
            vh.setVolatile(recv, "bar");
            String x = (String) vh.getVolatile(recv);
            assertEquals(x, "bar", "setVolatile String value");
        }

        // Lazy
        {
            vh.setRelease(recv, "foo");
            String x = (String) vh.getAcquire(recv);
            assertEquals(x, "foo", "setRelease String value");
        }

        // Opaque
        {
            vh.setOpaque(recv, "bar");
            String x = (String) vh.getOpaque(recv);
            assertEquals(x, "bar", "setOpaque String value");
        }

        vh.set(recv, "foo");

        // Compare
        {
            boolean r = vh.compareAndSet(recv, "foo", "bar");
            assertEquals(r, true, "success compareAndSet String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "success compareAndSet String value");
        }

        {
            boolean r = vh.compareAndSet(recv, "foo", "baz");
            assertEquals(r, false, "failing compareAndSet String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "failing compareAndSet String value");
        }

        {
            String r = (String) vh.compareAndExchange(recv, "bar", "foo");
            assertEquals(r, "bar", "success compareAndExchange String");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "success compareAndExchange String value");
        }

        {
            String r = (String) vh.compareAndExchange(recv, "bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchange String");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "failing compareAndExchange String value");
        }

        {
            String r = (String) vh.compareAndExchangeAcquire(recv, "foo", "bar");
            assertEquals(r, "foo", "success compareAndExchangeAcquire String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "success compareAndExchangeAcquire String value");
        }

        {
            String r = (String) vh.compareAndExchangeAcquire(recv, "foo", "baz");
            assertEquals(r, "bar", "failing compareAndExchangeAcquire String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "failing compareAndExchangeAcquire String value");
        }

        {
            String r = (String) vh.compareAndExchangeRelease(recv, "bar", "foo");
            assertEquals(r, "bar", "success compareAndExchangeRelease String");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "success compareAndExchangeRelease String value");
        }

        {
            String r = (String) vh.compareAndExchangeRelease(recv, "bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchangeRelease String");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "failing compareAndExchangeRelease String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, "foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetPlain String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "weakCompareAndSetPlain String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, "bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSetAcquire String");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "weakCompareAndSetAcquire String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, "foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetRelease String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "weakCompareAndSetRelease String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, "bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSet String");
            String x = (String) vh.get(recv);
            assertEquals(x, "foo", "weakCompareAndSet String value");
        }

        // Compare set and get
        {
            vh.set(recv, "foo");

            String o = (String) vh.getAndSet(recv, "bar");
            assertEquals(o, "foo", "getAndSet String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "getAndSet String value");
        }

        {
            vh.set(recv, "foo");

            String o = (String) vh.getAndSetAcquire(recv, "bar");
            assertEquals(o, "foo", "getAndSetAcquire String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "getAndSetAcquire String value");
        }

        {
            vh.set(recv, "foo");

            String o = (String) vh.getAndSetRelease(recv, "bar");
            assertEquals(o, "foo", "getAndSetRelease String");
            String x = (String) vh.get(recv);
            assertEquals(x, "bar", "getAndSetRelease String value");
        }


    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessString recv, VarHandle vh) {

        checkUOE(() -> {
            String o = (String) vh.getAndAdd(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddRelease(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOr(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrRelease(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAnd(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndRelease(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXor(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorAcquire(recv, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorRelease(recv, "foo");
        });
    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set("foo");
            String x = (String) vh.get();
            assertEquals(x, "foo", "set String value");
        }


        // Volatile
        {
            vh.setVolatile("bar");
            String x = (String) vh.getVolatile();
            assertEquals(x, "bar", "setVolatile String value");
        }

        // Lazy
        {
            vh.setRelease("foo");
            String x = (String) vh.getAcquire();
            assertEquals(x, "foo", "setRelease String value");
        }

        // Opaque
        {
            vh.setOpaque("bar");
            String x = (String) vh.getOpaque();
            assertEquals(x, "bar", "setOpaque String value");
        }

        vh.set("foo");

        // Compare
        {
            boolean r = vh.compareAndSet("foo", "bar");
            assertEquals(r, true, "success compareAndSet String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "success compareAndSet String value");
        }

        {
            boolean r = vh.compareAndSet("foo", "baz");
            assertEquals(r, false, "failing compareAndSet String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "failing compareAndSet String value");
        }

        {
            String r = (String) vh.compareAndExchange("bar", "foo");
            assertEquals(r, "bar", "success compareAndExchange String");
            String x = (String) vh.get();
            assertEquals(x, "foo", "success compareAndExchange String value");
        }

        {
            String r = (String) vh.compareAndExchange("bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchange String");
            String x = (String) vh.get();
            assertEquals(x, "foo", "failing compareAndExchange String value");
        }

        {
            String r = (String) vh.compareAndExchangeAcquire("foo", "bar");
            assertEquals(r, "foo", "success compareAndExchangeAcquire String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "success compareAndExchangeAcquire String value");
        }

        {
            String r = (String) vh.compareAndExchangeAcquire("foo", "baz");
            assertEquals(r, "bar", "failing compareAndExchangeAcquire String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "failing compareAndExchangeAcquire String value");
        }

        {
            String r = (String) vh.compareAndExchangeRelease("bar", "foo");
            assertEquals(r, "bar", "success compareAndExchangeRelease String");
            String x = (String) vh.get();
            assertEquals(x, "foo", "success compareAndExchangeRelease String value");
        }

        {
            String r = (String) vh.compareAndExchangeRelease("bar", "baz");
            assertEquals(r, "foo", "failing compareAndExchangeRelease String");
            String x = (String) vh.get();
            assertEquals(x, "foo", "failing compareAndExchangeRelease String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain("foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetPlain String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "weakCompareAndSetPlain String value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire("bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSetAcquire String");
            String x = (String) vh.get();
            assertEquals(x, "foo", "weakCompareAndSetAcquire String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease("foo", "bar");
            }
            assertEquals(success, true, "weakCompareAndSetRelease String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "weakCompareAndSetRelease String");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet("bar", "foo");
            }
            assertEquals(success, true, "weakCompareAndSet String");
            String x = (String) vh.get();
            assertEquals(x, "foo", "weakCompareAndSet String");
        }

        // Compare set and get
        {
            vh.set("foo");

            String o = (String) vh.getAndSet("bar");
            assertEquals(o, "foo", "getAndSet String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "getAndSet String value");
        }

        {
            vh.set("foo");

            String o = (String) vh.getAndSetAcquire("bar");
            assertEquals(o, "foo", "getAndSetAcquire String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "getAndSetAcquire String value");
        }

        {
            vh.set("foo");

            String o = (String) vh.getAndSetRelease("bar");
            assertEquals(o, "foo", "getAndSetRelease String");
            String x = (String) vh.get();
            assertEquals(x, "bar", "getAndSetRelease String value");
        }


    }

    static void testStaticFieldUnsupported(VarHandle vh) {

        checkUOE(() -> {
            String o = (String) vh.getAndAdd("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddRelease("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOr("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrRelease("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAnd("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndRelease("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXor("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorAcquire("foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorRelease("foo");
        });
    }


    static void testArray(VarHandle vh) {
        String[] array = new String[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, "foo");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "get String value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, "bar");
                String x = (String) vh.getVolatile(array, i);
                assertEquals(x, "bar", "setVolatile String value");
            }

            // Lazy
            {
                vh.setRelease(array, i, "foo");
                String x = (String) vh.getAcquire(array, i);
                assertEquals(x, "foo", "setRelease String value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, "bar");
                String x = (String) vh.getOpaque(array, i);
                assertEquals(x, "bar", "setOpaque String value");
            }

            vh.set(array, i, "foo");

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, "foo", "bar");
                assertEquals(r, true, "success compareAndSet String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "success compareAndSet String value");
            }

            {
                boolean r = vh.compareAndSet(array, i, "foo", "baz");
                assertEquals(r, false, "failing compareAndSet String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "failing compareAndSet String value");
            }

            {
                String r = (String) vh.compareAndExchange(array, i, "bar", "foo");
                assertEquals(r, "bar", "success compareAndExchange String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "success compareAndExchange String value");
            }

            {
                String r = (String) vh.compareAndExchange(array, i, "bar", "baz");
                assertEquals(r, "foo", "failing compareAndExchange String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "failing compareAndExchange String value");
            }

            {
                String r = (String) vh.compareAndExchangeAcquire(array, i, "foo", "bar");
                assertEquals(r, "foo", "success compareAndExchangeAcquire String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "success compareAndExchangeAcquire String value");
            }

            {
                String r = (String) vh.compareAndExchangeAcquire(array, i, "foo", "baz");
                assertEquals(r, "bar", "failing compareAndExchangeAcquire String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "failing compareAndExchangeAcquire String value");
            }

            {
                String r = (String) vh.compareAndExchangeRelease(array, i, "bar", "foo");
                assertEquals(r, "bar", "success compareAndExchangeRelease String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "success compareAndExchangeRelease String value");
            }

            {
                String r = (String) vh.compareAndExchangeRelease(array, i, "bar", "baz");
                assertEquals(r, "foo", "failing compareAndExchangeRelease String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "failing compareAndExchangeRelease String value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, "foo", "bar");
                }
                assertEquals(success, true, "weakCompareAndSetPlain String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "weakCompareAndSetPlain String value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, "bar", "foo");
                }
                assertEquals(success, true, "weakCompareAndSetAcquire String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "weakCompareAndSetAcquire String");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, "foo", "bar");
                }
                assertEquals(success, true, "weakCompareAndSetRelease String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "weakCompareAndSetRelease String");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, "bar", "foo");
                }
                assertEquals(success, true, "weakCompareAndSet String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "foo", "weakCompareAndSet String");
            }

            // Compare set and get
            {
                vh.set(array, i, "foo");

                String o = (String) vh.getAndSet(array, i, "bar");
                assertEquals(o, "foo", "getAndSet String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "getAndSet String value");
            }

            {
                vh.set(array, i, "foo");

                String o = (String) vh.getAndSetAcquire(array, i, "bar");
                assertEquals(o, "foo", "getAndSetAcquire String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "getAndSetAcquire String value");
            }

            {
                vh.set(array, i, "foo");

                String o = (String) vh.getAndSetRelease(array, i, "bar");
                assertEquals(o, "foo", "getAndSetRelease String");
                String x = (String) vh.get(array, i);
                assertEquals(x, "bar", "getAndSetRelease String value");
            }


        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        String[] array = new String[10];

        int i = 0;

        checkUOE(() -> {
            String o = (String) vh.getAndAdd(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddAcquire(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndAddRelease(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOr(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrAcquire(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseOrRelease(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAnd(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndAcquire(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseAndRelease(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXor(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorAcquire(array, i, "foo");
        });

        checkUOE(() -> {
            String o = (String) vh.getAndBitwiseXorRelease(array, i, "foo");
        });
    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        String[] array = new String[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                String x = (String) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, "foo");
            });

            checkAIOOBE(() -> {
                String x = (String) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, "foo");
            });

            checkAIOOBE(() -> {
                String x = (String) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, "foo");
            });

            checkAIOOBE(() -> {
                String x = (String) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, "foo");
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, "foo", "bar");
            });

            checkAIOOBE(() -> {
                String r = (String) vh.compareAndExchange(array, ci, "bar", "foo");
            });

            checkAIOOBE(() -> {
                String r = (String) vh.compareAndExchangeAcquire(array, ci, "bar", "foo");
            });

            checkAIOOBE(() -> {
                String r = (String) vh.compareAndExchangeRelease(array, ci, "bar", "foo");
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, "foo", "bar");
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, "foo", "bar");
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, "foo", "bar");
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, "foo", "bar");
            });

            checkAIOOBE(() -> {
                String o = (String) vh.getAndSet(array, ci, "foo");
            });

            checkAIOOBE(() -> {
                String o = (String) vh.getAndSetAcquire(array, ci, "foo");
            });

            checkAIOOBE(() -> {
                String o = (String) vh.getAndSetRelease(array, ci, "foo");
            });


        }
    }

    static void testArrayStoreException(VarHandle vh) throws Throwable {
        Object[] array = new String[10];
        Arrays.fill(array, "foo");
        Object value = new Object();

        // Set
        checkASE(() -> {
            vh.set(array, 0, value);
        });

        // SetVolatile
        checkASE(() -> {
            vh.setVolatile(array, 0, value);
        });

        // SetOpaque
        checkASE(() -> {
            vh.setOpaque(array, 0, value);
        });

        // SetRelease
        checkASE(() -> {
            vh.setRelease(array, 0, value);
        });

        // CompareAndSet
        checkASE(() -> { // receiver reference class
            boolean r = vh.compareAndSet(array, 0, "foo", value);
        });

        // WeakCompareAndSet
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetPlain(array, 0, "foo", value);
        });

        // WeakCompareAndSetVolatile
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSet(array, 0, "foo", value);
        });

        // WeakCompareAndSetAcquire
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetAcquire(array, 0, "foo", value);
        });

        // WeakCompareAndSetRelease
        checkASE(() -> { // receiver reference class
            boolean r = vh.weakCompareAndSetRelease(array, 0, "foo", value);
        });

        // CompareAndExchange
        checkASE(() -> { // receiver reference class
            String x = (String) vh.compareAndExchange(array, 0, "foo", value);
        });

        // CompareAndExchangeAcquire
        checkASE(() -> { // receiver reference class
            String x = (String) vh.compareAndExchangeAcquire(array, 0, "foo", value);
        });

        // CompareAndExchangeRelease
        checkASE(() -> { // receiver reference class
            String x = (String) vh.compareAndExchangeRelease(array, 0, "foo", value);
        });

        // GetAndSet
        checkASE(() -> { // receiver reference class
            String x = (String) vh.getAndSet(array, 0, value);
        });

        // GetAndSetAcquire
        checkASE(() -> { // receiver reference class
            String x = (String) vh.getAndSetAcquire(array, 0, value);
        });

        // GetAndSetRelease
        checkASE(() -> { // receiver reference class
            String x = (String) vh.getAndSetRelease(array, 0, value);
        });
    }
}

