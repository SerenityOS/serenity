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
 * @run testng/othervm -Diters=10    -Xint                   VarHandleTestAccessChar
 * @run testng/othervm -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestAccessChar
 * @run testng/othervm -Diters=20000                         VarHandleTestAccessChar
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  VarHandleTestAccessChar
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

public class VarHandleTestAccessChar extends VarHandleBaseTest {
    static final char static_final_v = '\u0123';

    static char static_v;

    final char final_v = '\u0123';

    char v;

    static final char static_final_v2 = '\u0123';

    static char static_v2;

    final char final_v2 = '\u0123';

    char v2;

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
                    VarHandleTestAccessChar.class, "final_v" + postfix, char.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findVarHandle(
                    VarHandleTestAccessChar.class, "v" + postfix, char.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessChar.class, "static_final_v" + postfix, char.class);
            vhs.add(vh);

            vh = MethodHandles.lookup().findStaticVarHandle(
                VarHandleTestAccessChar.class, "static_v" + postfix, char.class);
            vhs.add(vh);

            if (same) {
                vh = MethodHandles.arrayElementVarHandle(char[].class);
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
                VarHandleTestAccessChar.class, "final_v", char.class);

        vhField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessChar.class, "v", char.class);

        vhStaticFinalField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessChar.class, "static_final_v", char.class);

        vhStaticField = MethodHandles.lookup().findStaticVarHandle(
            VarHandleTestAccessChar.class, "static_v", char.class);

        vhArray = MethodHandles.arrayElementVarHandle(char[].class);
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
        types.add(new Object[] {vhField, Arrays.asList(VarHandleTestAccessChar.class)});
        types.add(new Object[] {vhStaticField, Arrays.asList()});
        types.add(new Object[] {vhArray, Arrays.asList(char[].class, int.class)});

        return types.stream().toArray(Object[][]::new);
    }

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<Class<?>> pts) {
        assertEquals(vh.varType(), char.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @Test
    public void testLookupInstanceToStatic() {
        checkIAE("Lookup of static final field to instance final field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessChar.class, "final_v", char.class);
        });

        checkIAE("Lookup of static field to instance field", () -> {
            MethodHandles.lookup().findStaticVarHandle(
                    VarHandleTestAccessChar.class, "v", char.class);
        });
    }

    @Test
    public void testLookupStaticToInstance() {
        checkIAE("Lookup of instance final field to static final field", () -> {
            MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessChar.class, "static_final_v", char.class);
        });

        checkIAE("Lookup of instance field to static field", () -> {
            vhStaticField = MethodHandles.lookup().findVarHandle(
                VarHandleTestAccessChar.class, "static_v", char.class);
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
                                              vhStaticFinalField, VarHandleTestAccessChar::testStaticFinalField));
        cases.add(new VarHandleAccessTestCase("Static final field unsupported",
                                              vhStaticFinalField, VarHandleTestAccessChar::testStaticFinalFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Instance field",
                                              vhField, vh -> testInstanceField(this, vh)));
        cases.add(new VarHandleAccessTestCase("Instance field unsupported",
                                              vhField, vh -> testInstanceFieldUnsupported(this, vh),
                                              false));

        cases.add(new VarHandleAccessTestCase("Static field",
                                              vhStaticField, VarHandleTestAccessChar::testStaticField));
        cases.add(new VarHandleAccessTestCase("Static field unsupported",
                                              vhStaticField, VarHandleTestAccessChar::testStaticFieldUnsupported,
                                              false));

        cases.add(new VarHandleAccessTestCase("Array",
                                              vhArray, VarHandleTestAccessChar::testArray));
        cases.add(new VarHandleAccessTestCase("Array unsupported",
                                              vhArray, VarHandleTestAccessChar::testArrayUnsupported,
                                              false));
        cases.add(new VarHandleAccessTestCase("Array index out of bounds",
                                              vhArray, VarHandleTestAccessChar::testArrayIndexOutOfBounds,
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




    static void testInstanceFinalField(VarHandleTestAccessChar recv, VarHandle vh) {
        // Plain
        {
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "get char value");
        }


        // Volatile
        {
            char x = (char) vh.getVolatile(recv);
            assertEquals(x, '\u0123', "getVolatile char value");
        }

        // Lazy
        {
            char x = (char) vh.getAcquire(recv);
            assertEquals(x, '\u0123', "getRelease char value");
        }

        // Opaque
        {
            char x = (char) vh.getOpaque(recv);
            assertEquals(x, '\u0123', "getOpaque char value");
        }
    }

    static void testInstanceFinalFieldUnsupported(VarHandleTestAccessChar recv, VarHandle vh) {
        checkUOE(() -> {
            vh.set(recv, '\u4567');
        });

        checkUOE(() -> {
            vh.setVolatile(recv, '\u4567');
        });

        checkUOE(() -> {
            vh.setRelease(recv, '\u4567');
        });

        checkUOE(() -> {
            vh.setOpaque(recv, '\u4567');
        });



    }


    static void testStaticFinalField(VarHandle vh) {
        // Plain
        {
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "get char value");
        }


        // Volatile
        {
            char x = (char) vh.getVolatile();
            assertEquals(x, '\u0123', "getVolatile char value");
        }

        // Lazy
        {
            char x = (char) vh.getAcquire();
            assertEquals(x, '\u0123', "getRelease char value");
        }

        // Opaque
        {
            char x = (char) vh.getOpaque();
            assertEquals(x, '\u0123', "getOpaque char value");
        }
    }

    static void testStaticFinalFieldUnsupported(VarHandle vh) {
        checkUOE(() -> {
            vh.set('\u4567');
        });

        checkUOE(() -> {
            vh.setVolatile('\u4567');
        });

        checkUOE(() -> {
            vh.setRelease('\u4567');
        });

        checkUOE(() -> {
            vh.setOpaque('\u4567');
        });



    }


    static void testInstanceField(VarHandleTestAccessChar recv, VarHandle vh) {
        // Plain
        {
            vh.set(recv, '\u0123');
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "set char value");
        }


        // Volatile
        {
            vh.setVolatile(recv, '\u4567');
            char x = (char) vh.getVolatile(recv);
            assertEquals(x, '\u4567', "setVolatile char value");
        }

        // Lazy
        {
            vh.setRelease(recv, '\u0123');
            char x = (char) vh.getAcquire(recv);
            assertEquals(x, '\u0123', "setRelease char value");
        }

        // Opaque
        {
            vh.setOpaque(recv, '\u4567');
            char x = (char) vh.getOpaque(recv);
            assertEquals(x, '\u4567', "setOpaque char value");
        }

        vh.set(recv, '\u0123');

        // Compare
        {
            boolean r = vh.compareAndSet(recv, '\u0123', '\u4567');
            assertEquals(r, true, "success compareAndSet char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "success compareAndSet char value");
        }

        {
            boolean r = vh.compareAndSet(recv, '\u0123', '\u89AB');
            assertEquals(r, false, "failing compareAndSet char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "failing compareAndSet char value");
        }

        {
            char r = (char) vh.compareAndExchange(recv, '\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchange char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "success compareAndExchange char value");
        }

        {
            char r = (char) vh.compareAndExchange(recv, '\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchange char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "failing compareAndExchange char value");
        }

        {
            char r = (char) vh.compareAndExchangeAcquire(recv, '\u0123', '\u4567');
            assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
        }

        {
            char r = (char) vh.compareAndExchangeAcquire(recv, '\u0123', '\u89AB');
            assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
        }

        {
            char r = (char) vh.compareAndExchangeRelease(recv, '\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
        }

        {
            char r = (char) vh.compareAndExchangeRelease(recv, '\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain(recv, '\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetPlain char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire(recv, '\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSetAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease(recv, '\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet(recv, '\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSet char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u0123', "weakCompareAndSet char value");
        }

        // Compare set and get
        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndSet(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndSet char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "getAndSet char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndSetAcquire(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndSetAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "getAndSetAcquire char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndSetRelease(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndSetRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, '\u4567', "getAndSetRelease char value");
        }

        // get and add, add and get
        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndAdd(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndAdd char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndAddAcquire(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndAddAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddAcquire char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndAddRelease(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndAddReleasechar");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddRelease char value");
        }

        // get and bitwise or
        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseOr(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOr char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOr char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseOrAcquire(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrAcquire char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseOrRelease(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrRelease char value");
        }

        // get and bitwise and
        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseAnd(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAnd char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAnd char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseAndAcquire(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndAcquire char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseAndRelease(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndRelease char value");
        }

        // get and bitwise xor
        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseXor(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXor char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXor char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseXorAcquire(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorAcquire char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorAcquire char value");
        }

        {
            vh.set(recv, '\u0123');

            char o = (char) vh.getAndBitwiseXorRelease(recv, '\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorRelease char");
            char x = (char) vh.get(recv);
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorRelease char value");
        }
    }

    static void testInstanceFieldUnsupported(VarHandleTestAccessChar recv, VarHandle vh) {


    }


    static void testStaticField(VarHandle vh) {
        // Plain
        {
            vh.set('\u0123');
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "set char value");
        }


        // Volatile
        {
            vh.setVolatile('\u4567');
            char x = (char) vh.getVolatile();
            assertEquals(x, '\u4567', "setVolatile char value");
        }

        // Lazy
        {
            vh.setRelease('\u0123');
            char x = (char) vh.getAcquire();
            assertEquals(x, '\u0123', "setRelease char value");
        }

        // Opaque
        {
            vh.setOpaque('\u4567');
            char x = (char) vh.getOpaque();
            assertEquals(x, '\u4567', "setOpaque char value");
        }

        vh.set('\u0123');

        // Compare
        {
            boolean r = vh.compareAndSet('\u0123', '\u4567');
            assertEquals(r, true, "success compareAndSet char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "success compareAndSet char value");
        }

        {
            boolean r = vh.compareAndSet('\u0123', '\u89AB');
            assertEquals(r, false, "failing compareAndSet char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "failing compareAndSet char value");
        }

        {
            char r = (char) vh.compareAndExchange('\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchange char");
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "success compareAndExchange char value");
        }

        {
            char r = (char) vh.compareAndExchange('\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchange char");
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "failing compareAndExchange char value");
        }

        {
            char r = (char) vh.compareAndExchangeAcquire('\u0123', '\u4567');
            assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
        }

        {
            char r = (char) vh.compareAndExchangeAcquire('\u0123', '\u89AB');
            assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
        }

        {
            char r = (char) vh.compareAndExchangeRelease('\u4567', '\u0123');
            assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
        }

        {
            char r = (char) vh.compareAndExchangeRelease('\u4567', '\u89AB');
            assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetPlain('\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetPlain char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetAcquire('\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSetAcquire char");
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSetRelease('\u0123', '\u4567');
            }
            assertEquals(success, true, "weakCompareAndSetRelease char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
        }

        {
            boolean success = false;
            for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                success = vh.weakCompareAndSet('\u4567', '\u0123');
            }
            assertEquals(success, true, "weakCompareAndSet char");
            char x = (char) vh.get();
            assertEquals(x, '\u0123', "weakCompareAndSet char");
        }

        // Compare set and get
        {
            vh.set('\u0123');

            char o = (char) vh.getAndSet('\u4567');
            assertEquals(o, '\u0123', "getAndSet char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "getAndSet char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndSetAcquire('\u4567');
            assertEquals(o, '\u0123', "getAndSetAcquire char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "getAndSetAcquire char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndSetRelease('\u4567');
            assertEquals(o, '\u0123', "getAndSetRelease char");
            char x = (char) vh.get();
            assertEquals(x, '\u4567', "getAndSetRelease char value");
        }

        // get and add, add and get
        {
            vh.set('\u0123');

            char o = (char) vh.getAndAdd('\u4567');
            assertEquals(o, '\u0123', "getAndAdd char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndAddAcquire('\u4567');
            assertEquals(o, '\u0123', "getAndAddAcquire char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddAcquire char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndAddRelease('\u4567');
            assertEquals(o, '\u0123', "getAndAddReleasechar");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddRelease char value");
        }

        // get and bitwise or
        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseOr('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOr char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOr char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseOrAcquire('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrAcquire char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrAcquire char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseOrRelease('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseOrRelease char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrRelease char value");
        }

        // get and bitwise and
        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseAnd('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAnd char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAnd char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseAndAcquire('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndAcquire char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndAcquire char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseAndRelease('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseAndRelease char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndRelease char value");
        }

        // get and bitwise xor
        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseXor('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXor char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXor char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseXorAcquire('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorAcquire char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorAcquire char value");
        }

        {
            vh.set('\u0123');

            char o = (char) vh.getAndBitwiseXorRelease('\u4567');
            assertEquals(o, '\u0123', "getAndBitwiseXorRelease char");
            char x = (char) vh.get();
            assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorRelease char value");
        }
    }

    static void testStaticFieldUnsupported(VarHandle vh) {


    }


    static void testArray(VarHandle vh) {
        char[] array = new char[10];

        for (int i = 0; i < array.length; i++) {
            // Plain
            {
                vh.set(array, i, '\u0123');
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "get char value");
            }


            // Volatile
            {
                vh.setVolatile(array, i, '\u4567');
                char x = (char) vh.getVolatile(array, i);
                assertEquals(x, '\u4567', "setVolatile char value");
            }

            // Lazy
            {
                vh.setRelease(array, i, '\u0123');
                char x = (char) vh.getAcquire(array, i);
                assertEquals(x, '\u0123', "setRelease char value");
            }

            // Opaque
            {
                vh.setOpaque(array, i, '\u4567');
                char x = (char) vh.getOpaque(array, i);
                assertEquals(x, '\u4567', "setOpaque char value");
            }

            vh.set(array, i, '\u0123');

            // Compare
            {
                boolean r = vh.compareAndSet(array, i, '\u0123', '\u4567');
                assertEquals(r, true, "success compareAndSet char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "success compareAndSet char value");
            }

            {
                boolean r = vh.compareAndSet(array, i, '\u0123', '\u89AB');
                assertEquals(r, false, "failing compareAndSet char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "failing compareAndSet char value");
            }

            {
                char r = (char) vh.compareAndExchange(array, i, '\u4567', '\u0123');
                assertEquals(r, '\u4567', "success compareAndExchange char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "success compareAndExchange char value");
            }

            {
                char r = (char) vh.compareAndExchange(array, i, '\u4567', '\u89AB');
                assertEquals(r, '\u0123', "failing compareAndExchange char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "failing compareAndExchange char value");
            }

            {
                char r = (char) vh.compareAndExchangeAcquire(array, i, '\u0123', '\u4567');
                assertEquals(r, '\u0123', "success compareAndExchangeAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "success compareAndExchangeAcquire char value");
            }

            {
                char r = (char) vh.compareAndExchangeAcquire(array, i, '\u0123', '\u89AB');
                assertEquals(r, '\u4567', "failing compareAndExchangeAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "failing compareAndExchangeAcquire char value");
            }

            {
                char r = (char) vh.compareAndExchangeRelease(array, i, '\u4567', '\u0123');
                assertEquals(r, '\u4567', "success compareAndExchangeRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "success compareAndExchangeRelease char value");
            }

            {
                char r = (char) vh.compareAndExchangeRelease(array, i, '\u4567', '\u89AB');
                assertEquals(r, '\u0123', "failing compareAndExchangeRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "failing compareAndExchangeRelease char value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetPlain(array, i, '\u0123', '\u4567');
                }
                assertEquals(success, true, "weakCompareAndSetPlain char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "weakCompareAndSetPlain char value");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetAcquire(array, i, '\u4567', '\u0123');
                }
                assertEquals(success, true, "weakCompareAndSetAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "weakCompareAndSetAcquire char");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSetRelease(array, i, '\u0123', '\u4567');
                }
                assertEquals(success, true, "weakCompareAndSetRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "weakCompareAndSetRelease char");
            }

            {
                boolean success = false;
                for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                    success = vh.weakCompareAndSet(array, i, '\u4567', '\u0123');
                }
                assertEquals(success, true, "weakCompareAndSet char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u0123', "weakCompareAndSet char");
            }

            // Compare set and get
            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndSet(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndSet char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "getAndSet char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndSetAcquire(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndSetAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "getAndSetAcquire char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndSetRelease(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndSetRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, '\u4567', "getAndSetRelease char value");
            }

            // get and add, add and get
            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndAdd(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndAdd char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAdd char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndAddAcquire(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndAddAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddAcquire char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndAddRelease(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndAddReleasechar");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' + '\u4567'), "getAndAddRelease char value");
            }

            // get and bitwise or
            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseOr(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseOr char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOr char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseOrAcquire(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseOrAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrAcquire char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseOrRelease(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseOrRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' | '\u4567'), "getAndBitwiseOrRelease char value");
            }

            // get and bitwise and
            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseAnd(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseAnd char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAnd char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseAndAcquire(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseAndAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndAcquire char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseAndRelease(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseAndRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' & '\u4567'), "getAndBitwiseAndRelease char value");
            }

            // get and bitwise xor
            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseXor(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseXor char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXor char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseXorAcquire(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseXorAcquire char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorAcquire char value");
            }

            {
                vh.set(array, i, '\u0123');

                char o = (char) vh.getAndBitwiseXorRelease(array, i, '\u4567');
                assertEquals(o, '\u0123', "getAndBitwiseXorRelease char");
                char x = (char) vh.get(array, i);
                assertEquals(x, (char)('\u0123' ^ '\u4567'), "getAndBitwiseXorRelease char value");
            }
        }
    }

    static void testArrayUnsupported(VarHandle vh) {
        char[] array = new char[10];

        int i = 0;


    }

    static void testArrayIndexOutOfBounds(VarHandle vh) throws Throwable {
        char[] array = new char[10];

        for (int i : new int[]{-1, Integer.MIN_VALUE, 10, 11, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                char x = (char) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char x = (char) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char x = (char) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char x = (char) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, '\u0123', '\u4567');
            });

            checkAIOOBE(() -> {
                char r = (char) vh.compareAndExchange(array, ci, '\u4567', '\u0123');
            });

            checkAIOOBE(() -> {
                char r = (char) vh.compareAndExchangeAcquire(array, ci, '\u4567', '\u0123');
            });

            checkAIOOBE(() -> {
                char r = (char) vh.compareAndExchangeRelease(array, ci, '\u4567', '\u0123');
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, '\u0123', '\u4567');
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, '\u0123', '\u4567');
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, '\u0123', '\u4567');
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, '\u0123', '\u4567');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndSet(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndSetAcquire(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndSetRelease(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndAdd(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndAddAcquire(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndAddRelease(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseOr(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseOrAcquire(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseOrRelease(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseAnd(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseAndAcquire(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseAndRelease(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseXor(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseXorAcquire(array, ci, '\u0123');
            });

            checkAIOOBE(() -> {
                char o = (char) vh.getAndBitwiseXorRelease(array, ci, '\u0123');
            });
        }
    }

}

