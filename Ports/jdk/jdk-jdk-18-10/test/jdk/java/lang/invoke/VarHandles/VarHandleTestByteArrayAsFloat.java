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
 * @bug 8154556
 * @run testng/othervm/timeout=360 -Diters=20000 -XX:TieredStopAtLevel=1 VarHandleTestByteArrayAsFloat
 * @run testng/othervm/timeout=360 -Diters=20000                         VarHandleTestByteArrayAsFloat
 * @run testng/othervm/timeout=360 -Diters=20000 -XX:-TieredCompilation  VarHandleTestByteArrayAsFloat
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.testng.Assert.*;

public class VarHandleTestByteArrayAsFloat extends VarHandleBaseByteArrayTest {
    static final int SIZE = Float.BYTES;

    static final float VALUE_1 = 0x01020304;

    static final float VALUE_2 = 0x11121314;

    static final float VALUE_3 = 0xFFFEFDFC;


    @Override
    public List<VarHandleSource> setupVarHandleSources(boolean same) {
        // Combinations of VarHandle byte[] or ByteBuffer
        List<VarHandleSource> vhss = new ArrayList<>();
        for (MemoryMode endianess : List.of(MemoryMode.BIG_ENDIAN, MemoryMode.LITTLE_ENDIAN)) {

            ByteOrder bo = endianess == MemoryMode.BIG_ENDIAN
                    ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;

            Class<?> arrayType;
            if (same) {
                arrayType = float[].class;
            }
            else {
                arrayType = int[].class;
            }
            VarHandleSource aeh = new VarHandleSource(
                    MethodHandles.byteArrayViewVarHandle(arrayType, bo),
                    endianess, MemoryMode.READ_WRITE);
            vhss.add(aeh);

            VarHandleSource bbh = new VarHandleSource(
                    MethodHandles.byteBufferViewVarHandle(arrayType, bo),
                    endianess, MemoryMode.READ_WRITE);
            vhss.add(bbh);
        }
        return vhss;
    }

    @Test
    public void testEquals() {
        VarHandle[] vhs1 = setupVarHandleSources(true).stream().
            map(vhs -> vhs.s).toArray(VarHandle[]::new);
        VarHandle[] vhs2 = setupVarHandleSources(true).stream().
            map(vhs -> vhs.s).toArray(VarHandle[]::new);

        for (int i = 0; i < vhs1.length; i++) {
            for (int j = 0; j < vhs1.length; j++) {
                if (i != j) {
                    assertNotEquals(vhs1[i], vhs1[j]);
                    assertNotEquals(vhs1[i], vhs2[j]);
                }
            }
        }

        VarHandle[] vhs3 = setupVarHandleSources(false).stream().
            map(vhs -> vhs.s).toArray(VarHandle[]::new);
        for (int i = 0; i < vhs1.length; i++) {
            assertNotEquals(vhs1[i], vhs3[i]);
        }
    }

    @Test(dataProvider = "varHandlesProvider")
    public void testIsAccessModeSupported(VarHandleSource vhs) {
        VarHandle vh = vhs.s;

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

    @Test(dataProvider = "typesProvider")
    public void testTypes(VarHandle vh, List<java.lang.Class<?>> pts) {
        assertEquals(vh.varType(), float.class);

        assertEquals(vh.coordinateTypes(), pts);

        testTypes(vh);
    }


    @DataProvider
    public Object[][] accessTestCaseProvider() throws Exception {
        List<AccessTestCase<?>> cases = new ArrayList<>();

        for (ByteArrayViewSource<?> bav : bavss) {
            for (VarHandleSource vh : vhss) {
                if (vh.matches(bav)) {
                    if (bav instanceof ByteArraySource) {
                        ByteArraySource bas = (ByteArraySource) bav;

                        cases.add(new VarHandleSourceAccessTestCase(
                                "read write", bav, vh, h -> testArrayReadWrite(bas, h),
                                true));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "null array", bav, vh, h -> testArrayNPE(bas, h),
                                false));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "unsupported", bav, vh, h -> testArrayUnsupported(bas, h),
                                false));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "index out of bounds", bav, vh, h -> testArrayIndexOutOfBounds(bas, h),
                                false));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "misaligned access", bav, vh, h -> testArrayMisalignedAccess(bas, h),
                                false));
                    }
                    else {
                        ByteBufferSource bbs = (ByteBufferSource) bav;

                        if (MemoryMode.READ_WRITE.isSet(bav.memoryModes)) {
                            cases.add(new VarHandleSourceAccessTestCase(
                                    "read write", bav, vh, h -> testArrayReadWrite(bbs, h),
                                    true));
                        }
                        else {
                            cases.add(new VarHandleSourceAccessTestCase(
                                    "read only", bav, vh, h -> testArrayReadOnly(bbs, h),
                                    true));
                        }

                        cases.add(new VarHandleSourceAccessTestCase(
                                "null buffer", bav, vh, h -> testArrayNPE(bbs, h),
                                false));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "unsupported", bav, vh, h -> testArrayUnsupported(bbs, h),
                                false));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "index out of bounds", bav, vh, h -> testArrayIndexOutOfBounds(bbs, h),
                                false));
                        cases.add(new VarHandleSourceAccessTestCase(
                                "misaligned access", bav, vh, h -> testArrayMisalignedAccess(bbs, h),
                                false));
                    }
                }
            }
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


    static void testArrayNPE(ByteArraySource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        byte[] array = null;
        int ci = 1;

        checkNPE(() -> {
            float x = (float) vh.get(array, ci);
        });

        checkNPE(() -> {
            vh.set(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            float x = (float) vh.getVolatile(array, ci);
        });

        checkNPE(() -> {
            float x = (float) vh.getAcquire(array, ci);
        });

        checkNPE(() -> {
            float x = (float) vh.getOpaque(array, ci);
        });

        checkNPE(() -> {
            vh.setVolatile(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            vh.setRelease(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            vh.setOpaque(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
        });

        checkNPE(() -> {
            float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
        });

        checkNPE(() -> {
            float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            float o = (float) vh.getAndSet(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
        });


    }

    static void testArrayNPE(ByteBufferSource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        ByteBuffer array = null;
        int ci = 1;

        checkNPE(() -> {
            float x = (float) vh.get(array, ci);
        });

        checkNPE(() -> {
            vh.set(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            float x = (float) vh.getVolatile(array, ci);
        });

        checkNPE(() -> {
            float x = (float) vh.getAcquire(array, ci);
        });

        checkNPE(() -> {
            float x = (float) vh.getOpaque(array, ci);
        });

        checkNPE(() -> {
            vh.setVolatile(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            vh.setRelease(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            vh.setOpaque(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
        });

        checkNPE(() -> {
            float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
        });

        checkNPE(() -> {
            float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
        });

        checkNPE(() -> {
            float o = (float) vh.getAndSet(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
        });

        checkNPE(() -> {
            float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
        });


    }

    static void testArrayUnsupported(ByteArraySource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        byte[] array = bs.s;
        int ci = 1;


        checkUOE(() -> {
            float o = (float) vh.getAndAdd(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndAddAcquire(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndAddRelease(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseOr(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseOrAcquire(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseOrRelease(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseAnd(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseAndAcquire(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseAndRelease(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseXor(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseXorAcquire(array, ci, VALUE_1);
        });

        checkUOE(() -> {
            float o = (float) vh.getAndBitwiseXorRelease(array, ci, VALUE_1);
        });
    }

    static void testArrayUnsupported(ByteBufferSource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        ByteBuffer array = bs.s;
        int ci = 0;
        boolean readOnly = MemoryMode.READ_ONLY.isSet(bs.memoryModes);

        if (readOnly) {
            checkROBE(() -> {
                vh.set(array, ci, VALUE_1);
            });
        }

        if (readOnly) {
            checkROBE(() -> {
                vh.setVolatile(array, ci, VALUE_1);
            });

            checkROBE(() -> {
                vh.setRelease(array, ci, VALUE_1);
            });

            checkROBE(() -> {
                vh.setOpaque(array, ci, VALUE_1);
            });

            checkROBE(() -> {
                boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
            });

            checkROBE(() -> {
                float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
            });

            checkROBE(() -> {
                float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
            });

            checkROBE(() -> {
                float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
            });

            checkROBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
            });

            checkROBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
            });

            checkROBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
            });

            checkROBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
            });

            checkROBE(() -> {
                float o = (float) vh.getAndSet(array, ci, VALUE_1);
            });

            checkROBE(() -> {
                float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
            });

            checkROBE(() -> {
                float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
            });


            checkUOE(() -> {
                float o = (float) vh.getAndAdd(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndAddAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndAddRelease(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseOr(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseOrAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseOrRelease(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseAnd(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseAndAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseAndRelease(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseXor(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseXorAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseXorRelease(array, ci, VALUE_1);
            });
        }
        else {
            checkUOE(() -> {
                float o = (float) vh.getAndAdd(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndAddAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndAddRelease(array, ci, VALUE_1);
            });
            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseOr(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseOrAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseOrRelease(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseAnd(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseAndAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseAndRelease(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseXor(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseXorAcquire(array, ci, VALUE_1);
            });

            checkUOE(() -> {
                float o = (float) vh.getAndBitwiseXorRelease(array, ci, VALUE_1);
            });
        }
    }


    static void testArrayIndexOutOfBounds(ByteArraySource bs, VarHandleSource vhs) throws Throwable {
        VarHandle vh = vhs.s;
        byte[] array = bs.s;

        int length = array.length - SIZE + 1;
        for (int i : new int[]{-1, Integer.MIN_VALUE, length, length + 1, Integer.MAX_VALUE}) {
            final int ci = i;

            checkAIOOBE(() -> {
                float x = (float) vh.get(array, ci);
            });

            checkAIOOBE(() -> {
                vh.set(array, ci, VALUE_1);
            });

            checkAIOOBE(() -> {
                float x = (float) vh.getVolatile(array, ci);
            });

            checkAIOOBE(() -> {
                float x = (float) vh.getAcquire(array, ci);
            });

            checkAIOOBE(() -> {
                float x = (float) vh.getOpaque(array, ci);
            });

            checkAIOOBE(() -> {
                vh.setVolatile(array, ci, VALUE_1);
            });

            checkAIOOBE(() -> {
                vh.setRelease(array, ci, VALUE_1);
            });

            checkAIOOBE(() -> {
                vh.setOpaque(array, ci, VALUE_1);
            });

            checkAIOOBE(() -> {
                boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
            });

            checkAIOOBE(() -> {
                float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
            });

            checkAIOOBE(() -> {
                float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
            });

            checkAIOOBE(() -> {
                float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
            });

            checkAIOOBE(() -> {
                boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
            });

            checkAIOOBE(() -> {
                float o = (float) vh.getAndSet(array, ci, VALUE_1);
            });

            checkAIOOBE(() -> {
                float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
            });

            checkAIOOBE(() -> {
                float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
            });



        }
    }

    static void testArrayIndexOutOfBounds(ByteBufferSource bs, VarHandleSource vhs) throws Throwable {
        VarHandle vh = vhs.s;
        ByteBuffer array = bs.s;

        boolean readOnly = MemoryMode.READ_ONLY.isSet(bs.memoryModes);

        int length = array.limit() - SIZE + 1;
        for (int i : new int[]{-1, Integer.MIN_VALUE, length, length + 1, Integer.MAX_VALUE}) {
            final int ci = i;

            checkIOOBE(() -> {
                float x = (float) vh.get(array, ci);
            });

            if (!readOnly) {
                checkIOOBE(() -> {
                    vh.set(array, ci, VALUE_1);
                });
            }

            checkIOOBE(() -> {
                float x = (float) vh.getVolatile(array, ci);
            });

            checkIOOBE(() -> {
                float x = (float) vh.getAcquire(array, ci);
            });

            checkIOOBE(() -> {
                float x = (float) vh.getOpaque(array, ci);
            });

            if (!readOnly) {
                checkIOOBE(() -> {
                    vh.setVolatile(array, ci, VALUE_1);
                });

                checkIOOBE(() -> {
                    vh.setRelease(array, ci, VALUE_1);
                });

                checkIOOBE(() -> {
                    vh.setOpaque(array, ci, VALUE_1);
                });

                checkIOOBE(() -> {
                    boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
                });

                checkIOOBE(() -> {
                    float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
                });

                checkIOOBE(() -> {
                    float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
                });

                checkIOOBE(() -> {
                    float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
                });

                checkIOOBE(() -> {
                    boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
                });

                checkIOOBE(() -> {
                    boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
                });

                checkIOOBE(() -> {
                    boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
                });

                checkIOOBE(() -> {
                    boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
                });

                checkIOOBE(() -> {
                    float o = (float) vh.getAndSet(array, ci, VALUE_1);
                });

                checkIOOBE(() -> {
                    float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
                });

                checkIOOBE(() -> {
                    float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
                });


            }
        }
    }

    static void testArrayMisalignedAccess(ByteArraySource bs, VarHandleSource vhs) throws Throwable {
        VarHandle vh = vhs.s;
        byte[] array = bs.s;

        int misalignmentAtZero = ByteBuffer.wrap(array).alignmentOffset(0, SIZE);

        int length = array.length - SIZE + 1;
        for (int i = 0; i < length; i++) {
            boolean iAligned = ((i + misalignmentAtZero) & (SIZE - 1)) == 0;
            final int ci = i;

            if (!iAligned) {
                checkISE(() -> {
                    float x = (float) vh.getVolatile(array, ci);
                });

                checkISE(() -> {
                    float x = (float) vh.getAcquire(array, ci);
                });

                checkISE(() -> {
                    float x = (float) vh.getOpaque(array, ci);
                });

                checkISE(() -> {
                    vh.setVolatile(array, ci, VALUE_1);
                });

                checkISE(() -> {
                    vh.setRelease(array, ci, VALUE_1);
                });

                checkISE(() -> {
                    vh.setOpaque(array, ci, VALUE_1);
                });

                checkISE(() -> {
                    boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
                });

                checkISE(() -> {
                    float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
                });

                checkISE(() -> {
                    float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
                });

                checkISE(() -> {
                    float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
                });

                checkISE(() -> {
                    boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
                });

                checkISE(() -> {
                    boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
                });

                checkISE(() -> {
                    boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
                });

                checkISE(() -> {
                    boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
                });

                checkISE(() -> {
                    float o = (float) vh.getAndSet(array, ci, VALUE_1);
                });

                checkISE(() -> {
                    float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
                });

                checkISE(() -> {
                    float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
                });


            }
        }
    }

    static void testArrayMisalignedAccess(ByteBufferSource bs, VarHandleSource vhs) throws Throwable {
        VarHandle vh = vhs.s;
        ByteBuffer array = bs.s;

        boolean readOnly = MemoryMode.READ_ONLY.isSet(bs.memoryModes);
        int misalignmentAtZero = array.alignmentOffset(0, SIZE);

        int length = array.limit() - SIZE + 1;
        for (int i = 0; i < length; i++) {
            boolean iAligned = ((i + misalignmentAtZero) & (SIZE - 1)) == 0;
            final int ci = i;

            if (!iAligned) {
                checkISE(() -> {
                    float x = (float) vh.getVolatile(array, ci);
                });

                checkISE(() -> {
                    float x = (float) vh.getAcquire(array, ci);
                });

                checkISE(() -> {
                    float x = (float) vh.getOpaque(array, ci);
                });

                if (!readOnly) {
                    checkISE(() -> {
                        vh.setVolatile(array, ci, VALUE_1);
                    });

                    checkISE(() -> {
                        vh.setRelease(array, ci, VALUE_1);
                    });

                    checkISE(() -> {
                        vh.setOpaque(array, ci, VALUE_1);
                    });

                    checkISE(() -> {
                        boolean r = vh.compareAndSet(array, ci, VALUE_1, VALUE_2);
                    });

                    checkISE(() -> {
                        float r = (float) vh.compareAndExchange(array, ci, VALUE_2, VALUE_1);
                    });

                    checkISE(() -> {
                        float r = (float) vh.compareAndExchangeAcquire(array, ci, VALUE_2, VALUE_1);
                    });

                    checkISE(() -> {
                        float r = (float) vh.compareAndExchangeRelease(array, ci, VALUE_2, VALUE_1);
                    });

                    checkISE(() -> {
                        boolean r = vh.weakCompareAndSetPlain(array, ci, VALUE_1, VALUE_2);
                    });

                    checkISE(() -> {
                        boolean r = vh.weakCompareAndSet(array, ci, VALUE_1, VALUE_2);
                    });

                    checkISE(() -> {
                        boolean r = vh.weakCompareAndSetAcquire(array, ci, VALUE_1, VALUE_2);
                    });

                    checkISE(() -> {
                        boolean r = vh.weakCompareAndSetRelease(array, ci, VALUE_1, VALUE_2);
                    });

                    checkISE(() -> {
                        float o = (float) vh.getAndSet(array, ci, VALUE_1);
                    });

                    checkISE(() -> {
                        float o = (float) vh.getAndSetAcquire(array, ci, VALUE_1);
                    });

                    checkISE(() -> {
                        float o = (float) vh.getAndSetRelease(array, ci, VALUE_1);
                    });


                }
            }
        }
    }

    static void testArrayReadWrite(ByteArraySource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        byte[] array = bs.s;

        int misalignmentAtZero = ByteBuffer.wrap(array).alignmentOffset(0, SIZE);

        bs.fill((byte) 0xff);
        int length = array.length - SIZE + 1;
        for (int i = 0; i < length; i++) {
            boolean iAligned = ((i + misalignmentAtZero) & (SIZE - 1)) == 0;

            // Plain
            {
                vh.set(array, i, VALUE_1);
                float x = (float) vh.get(array, i);
                assertEquals(x, VALUE_1, "get float value");
            }


            if (iAligned) {
                // Volatile
                {
                    vh.setVolatile(array, i, VALUE_2);
                    float x = (float) vh.getVolatile(array, i);
                    assertEquals(x, VALUE_2, "setVolatile float value");
                }

                // Lazy
                {
                    vh.setRelease(array, i, VALUE_1);
                    float x = (float) vh.getAcquire(array, i);
                    assertEquals(x, VALUE_1, "setRelease float value");
                }

                // Opaque
                {
                    vh.setOpaque(array, i, VALUE_2);
                    float x = (float) vh.getOpaque(array, i);
                    assertEquals(x, VALUE_2, "setOpaque float value");
                }

                vh.set(array, i, VALUE_1);

                // Compare
                {
                    boolean r = vh.compareAndSet(array, i, VALUE_1, VALUE_2);
                    assertEquals(r, true, "success compareAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "success compareAndSet float value");
                }

                {
                    boolean r = vh.compareAndSet(array, i, VALUE_1, VALUE_3);
                    assertEquals(r, false, "failing compareAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "failing compareAndSet float value");
                }

                {
                    float r = (float) vh.compareAndExchange(array, i, VALUE_2, VALUE_1);
                    assertEquals(r, VALUE_2, "success compareAndExchange float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "success compareAndExchange float value");
                }

                {
                    float r = (float) vh.compareAndExchange(array, i, VALUE_2, VALUE_3);
                    assertEquals(r, VALUE_1, "failing compareAndExchange float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "failing compareAndExchange float value");
                }

                {
                    float r = (float) vh.compareAndExchangeAcquire(array, i, VALUE_1, VALUE_2);
                    assertEquals(r, VALUE_1, "success compareAndExchangeAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "success compareAndExchangeAcquire float value");
                }

                {
                    float r = (float) vh.compareAndExchangeAcquire(array, i, VALUE_1, VALUE_3);
                    assertEquals(r, VALUE_2, "failing compareAndExchangeAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "failing compareAndExchangeAcquire float value");
                }

                {
                    float r = (float) vh.compareAndExchangeRelease(array, i, VALUE_2, VALUE_1);
                    assertEquals(r, VALUE_2, "success compareAndExchangeRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "success compareAndExchangeRelease float value");
                }

                {
                    float r = (float) vh.compareAndExchangeRelease(array, i, VALUE_2, VALUE_3);
                    assertEquals(r, VALUE_1, "failing compareAndExchangeRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "failing compareAndExchangeRelease float value");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSetPlain(array, i, VALUE_1, VALUE_2);
                    }
                    assertEquals(success, true, "weakCompareAndSetPlain float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "weakCompareAndSetPlain float value");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSetAcquire(array, i, VALUE_2, VALUE_1);
                    }
                    assertEquals(success, true, "weakCompareAndSetAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "weakCompareAndSetAcquire float");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSetRelease(array, i, VALUE_1, VALUE_2);
                    }
                    assertEquals(success, true, "weakCompareAndSetRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "weakCompareAndSetRelease float");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSet(array, i, VALUE_2, VALUE_1);
                    }
                    assertEquals(success, true, "weakCompareAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "weakCompareAndSet float");
                }

                // Compare set and get
                {
                    vh.set(array, i, VALUE_1);

                    float o = (float) vh.getAndSet(array, i, VALUE_2);
                    assertEquals(o, VALUE_1, "getAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "getAndSet float value");
                }

                {
                    vh.set(array, i, VALUE_1);

                    float o = (float) vh.getAndSetAcquire(array, i, VALUE_2);
                    assertEquals(o, VALUE_1, "getAndSetAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "getAndSetAcquire float value");
                }

                {
                    vh.set(array, i, VALUE_1);

                    float o = (float) vh.getAndSetRelease(array, i, VALUE_2);
                    assertEquals(o, VALUE_1, "getAndSetRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "getAndSetRelease float value");
                }


            }
        }
    }


    static void testArrayReadWrite(ByteBufferSource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        ByteBuffer array = bs.s;

        int misalignmentAtZero = array.alignmentOffset(0, SIZE);

        bs.fill((byte) 0xff);
        int length = array.limit() - SIZE + 1;
        for (int i = 0; i < length; i++) {
            boolean iAligned = ((i + misalignmentAtZero) & (SIZE - 1)) == 0;

            // Plain
            {
                vh.set(array, i, VALUE_1);
                float x = (float) vh.get(array, i);
                assertEquals(x, VALUE_1, "get float value");
            }

            if (iAligned) {
                // Volatile
                {
                    vh.setVolatile(array, i, VALUE_2);
                    float x = (float) vh.getVolatile(array, i);
                    assertEquals(x, VALUE_2, "setVolatile float value");
                }

                // Lazy
                {
                    vh.setRelease(array, i, VALUE_1);
                    float x = (float) vh.getAcquire(array, i);
                    assertEquals(x, VALUE_1, "setRelease float value");
                }

                // Opaque
                {
                    vh.setOpaque(array, i, VALUE_2);
                    float x = (float) vh.getOpaque(array, i);
                    assertEquals(x, VALUE_2, "setOpaque float value");
                }

                vh.set(array, i, VALUE_1);

                // Compare
                {
                    boolean r = vh.compareAndSet(array, i, VALUE_1, VALUE_2);
                    assertEquals(r, true, "success compareAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "success compareAndSet float value");
                }

                {
                    boolean r = vh.compareAndSet(array, i, VALUE_1, VALUE_3);
                    assertEquals(r, false, "failing compareAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "failing compareAndSet float value");
                }

                {
                    float r = (float) vh.compareAndExchange(array, i, VALUE_2, VALUE_1);
                    assertEquals(r, VALUE_2, "success compareAndExchange float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "success compareAndExchange float value");
                }

                {
                    float r = (float) vh.compareAndExchange(array, i, VALUE_2, VALUE_3);
                    assertEquals(r, VALUE_1, "failing compareAndExchange float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "failing compareAndExchange float value");
                }

                {
                    float r = (float) vh.compareAndExchangeAcquire(array, i, VALUE_1, VALUE_2);
                    assertEquals(r, VALUE_1, "success compareAndExchangeAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "success compareAndExchangeAcquire float value");
                }

                {
                    float r = (float) vh.compareAndExchangeAcquire(array, i, VALUE_1, VALUE_3);
                    assertEquals(r, VALUE_2, "failing compareAndExchangeAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "failing compareAndExchangeAcquire float value");
                }

                {
                    float r = (float) vh.compareAndExchangeRelease(array, i, VALUE_2, VALUE_1);
                    assertEquals(r, VALUE_2, "success compareAndExchangeRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "success compareAndExchangeRelease float value");
                }

                {
                    float r = (float) vh.compareAndExchangeRelease(array, i, VALUE_2, VALUE_3);
                    assertEquals(r, VALUE_1, "failing compareAndExchangeRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "failing compareAndExchangeRelease float value");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSetPlain(array, i, VALUE_1, VALUE_2);
                    }
                    assertEquals(success, true, "weakCompareAndSetPlain float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "weakCompareAndSetPlain float value");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSetAcquire(array, i, VALUE_2, VALUE_1);
                    }
                    assertEquals(success, true, "weakCompareAndSetAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "weakCompareAndSetAcquire float");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSetRelease(array, i, VALUE_1, VALUE_2);
                    }
                    assertEquals(success, true, "weakCompareAndSetRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "weakCompareAndSetRelease float");
                }

                {
                    boolean success = false;
                    for (int c = 0; c < WEAK_ATTEMPTS && !success; c++) {
                        success = vh.weakCompareAndSet(array, i, VALUE_2, VALUE_1);
                    }
                    assertEquals(success, true, "weakCompareAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_1, "weakCompareAndSet float");
                }

                // Compare set and get
                {
                    vh.set(array, i, VALUE_1);

                    float o = (float) vh.getAndSet(array, i, VALUE_2);
                    assertEquals(o, VALUE_1, "getAndSet float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "getAndSet float value");
                }

                {
                    vh.set(array, i, VALUE_1);

                    float o = (float) vh.getAndSetAcquire(array, i, VALUE_2);
                    assertEquals(o, VALUE_1, "getAndSetAcquire float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "getAndSetAcquire float value");
                }

                {
                    vh.set(array, i, VALUE_1);

                    float o = (float) vh.getAndSetRelease(array, i, VALUE_2);
                    assertEquals(o, VALUE_1, "getAndSetRelease float");
                    float x = (float) vh.get(array, i);
                    assertEquals(x, VALUE_2, "getAndSetRelease float value");
                }


            }
        }
    }

    static void testArrayReadOnly(ByteBufferSource bs, VarHandleSource vhs) {
        VarHandle vh = vhs.s;
        ByteBuffer array = bs.s;

        int misalignmentAtZero = array.alignmentOffset(0, SIZE);

        ByteBuffer bb = ByteBuffer.allocate(SIZE);
        bb.order(MemoryMode.BIG_ENDIAN.isSet(vhs.memoryModes) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        bs.fill(bb.putFloat(0, VALUE_2).array());

        int length = array.limit() - SIZE + 1;
        for (int i = 0; i < length; i++) {
            boolean iAligned = ((i + misalignmentAtZero) & (SIZE - 1)) == 0;

            float v = MemoryMode.BIG_ENDIAN.isSet(vhs.memoryModes)
                    ? rotateLeft(VALUE_2, (i % SIZE) << 3)
                    : rotateRight(VALUE_2, (i % SIZE) << 3);
            // Plain
            {
                float x = (float) vh.get(array, i);
                assertEquals(x, v, "get float value");
            }

            if (iAligned) {
                // Volatile
                {
                    float x = (float) vh.getVolatile(array, i);
                    assertEquals(x, v, "getVolatile float value");
                }

                // Lazy
                {
                    float x = (float) vh.getAcquire(array, i);
                    assertEquals(x, v, "getRelease float value");
                }

                // Opaque
                {
                    float x = (float) vh.getOpaque(array, i);
                    assertEquals(x, v, "getOpaque float value");
                }
            }
        }
    }

}

