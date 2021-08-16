/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @run testng TestMemoryAccessStatics
 */

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class TestMemoryAccessStatics {

    static class Accessor<X> {

        interface SegmentGetter<X> {
            X get(MemorySegment segment);
        }

        interface SegmentSetter<X> {
            void set(MemorySegment segment, X o);
        }

        interface BufferGetter<X> {
            X get(ByteBuffer segment);
        }

        interface BufferSetter<X> {
            void set(ByteBuffer buffer, X o);
        }

        final X value;
        final SegmentGetter<X> segmentGetter;
        final SegmentSetter<X> segmentSetter;
        final BufferGetter<X> bufferGetter;
        final BufferSetter<X> bufferSetter;

        Accessor(X value,
                 SegmentGetter<X> segmentGetter, SegmentSetter<X> segmentSetter,
                 BufferGetter<X> bufferGetter, BufferSetter<X> bufferSetter) {
            this.value = value;
            this.segmentGetter = segmentGetter;
            this.segmentSetter = segmentSetter;
            this.bufferGetter = bufferGetter;
            this.bufferSetter = bufferSetter;
        }

        void test() {
            MemorySegment segment = MemorySegment.ofArray(new byte[32]);
            ByteBuffer buffer = segment.asByteBuffer();
            segmentSetter.set(segment, value);
            assertEquals(bufferGetter.get(buffer), value);
            bufferSetter.set(buffer, value);
            assertEquals(value, segmentGetter.get(segment));
        }

        <Z> Accessor<Z> of(Z value,
                           SegmentGetter<Z> segmentGetter, SegmentSetter<Z> segmentSetter,
                           BufferGetter<Z> bufferGetter, BufferSetter<Z> bufferSetter) {
            return new Accessor<>(value, segmentGetter, segmentSetter, bufferGetter, bufferSetter);
        }
    }

    @Test(dataProvider = "accessors")
    public void testMemoryAccess(String testName, Accessor<?> accessor) {
        accessor.test();
    }

    static final ByteOrder BE = ByteOrder.BIG_ENDIAN;
    static final ByteOrder LE = ByteOrder.LITTLE_ENDIAN;
    static final ByteOrder NE = ByteOrder.nativeOrder();

    @DataProvider(name = "accessors")
    static Object[][] accessors() {
        return new Object[][]{

                {"byte", new Accessor<>((byte) 42,
                        MemoryAccess::getByte, MemoryAccess::setByte,
                        (bb) -> bb.get(0), (bb, v) -> bb.put(0, v))
                },
                {"char", new Accessor<>((char) 42,
                        MemoryAccess::getChar, MemoryAccess::setChar,
                        (bb) -> bb.order(NE).getChar(0), (bb, v) -> bb.order(NE).putChar(0, v))
                },
                {"char/LE", new Accessor<>((char) 42,
                        s -> MemoryAccess.getChar(s, LE), (s, x) -> MemoryAccess.setChar(s, LE, x),
                        (bb) -> bb.order(LE).getChar(0), (bb, v) -> bb.order(LE).putChar(0, v))
                },
                {"char/BE", new Accessor<>((char) 42,
                        s -> MemoryAccess.getChar(s, BE), (s, x) -> MemoryAccess.setChar(s, BE, x),
                        (bb) -> bb.order(BE).getChar(0), (bb, v) -> bb.order(BE).putChar(0, v))
                },
                {"short", new Accessor<>((short) 42,
                        MemoryAccess::getShort, MemoryAccess::setShort,
                        (bb) -> bb.order(NE).getShort(0), (bb, v) -> bb.order(NE).putShort(0, v))
                },
                {"short/LE", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShort(s, LE), (s, x) -> MemoryAccess.setShort(s, LE, x),
                        (bb) -> bb.order(LE).getShort(0), (bb, v) -> bb.order(LE).putShort(0, v))
                },
                {"short/BE", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShort(s, BE), (s, x) -> MemoryAccess.setShort(s, BE, x),
                        (bb) -> bb.order(BE).getShort(0), (bb, v) -> bb.order(BE).putShort(0, v))
                },
                {"int", new Accessor<>(42,
                        MemoryAccess::getInt, MemoryAccess::setInt,
                        (bb) -> bb.order(NE).getInt(0), (bb, v) -> bb.order(NE).putInt(0, v))
                },
                {"int/LE", new Accessor<>(42,
                        s -> MemoryAccess.getInt(s, LE), (s, x) -> MemoryAccess.setInt(s, LE, x),
                        (bb) -> bb.order(LE).getInt(0), (bb, v) -> bb.order(LE).putInt(0, v))
                },
                {"int/BE", new Accessor<>(42,
                        s -> MemoryAccess.getInt(s, BE), (s, x) -> MemoryAccess.setInt(s, BE, x),
                        (bb) -> bb.order(BE).getInt(0), (bb, v) -> bb.order(BE).putInt(0, v))
                },
                // float, no offset
                {"float", new Accessor<>(42f,
                        MemoryAccess::getFloat, MemoryAccess::setFloat,
                        (bb) -> bb.order(NE).getFloat(0), (bb, v) -> bb.order(NE).putFloat(0, v))
                },
                {"float/LE", new Accessor<>(42f,
                        s -> MemoryAccess.getFloat(s, LE), (s, x) -> MemoryAccess.setFloat(s, LE, x),
                        (bb) -> bb.order(LE).getFloat(0), (bb, v) -> bb.order(LE).putFloat(0, v))
                },
                {"float/BE", new Accessor<>(42f,
                        s -> MemoryAccess.getFloat(s, BE), (s, x) -> MemoryAccess.setFloat(s, BE, x),
                        (bb) -> bb.order(BE).getFloat(0), (bb, v) -> bb.order(BE).putFloat(0, v))
                },
                // double, no offset
                {"double", new Accessor<>(42d,
                        MemoryAccess::getDouble, MemoryAccess::setDouble,
                        (bb) -> bb.order(NE).getDouble(0), (bb, v) -> bb.order(NE).putDouble(0, v))
                },
                {"double/LE", new Accessor<>(42d,
                        s -> MemoryAccess.getDouble(s, LE), (s, x) -> MemoryAccess.setDouble(s, LE, x),
                        (bb) -> bb.order(LE).getDouble(0), (bb, v) -> bb.order(LE).putDouble(0, v))
                },
                {"double/BE", new Accessor<>(42d,
                        s -> MemoryAccess.getDouble(s, BE), (s, x) -> MemoryAccess.setDouble(s, BE, x),
                        (bb) -> bb.order(BE).getDouble(0), (bb, v) -> bb.order(BE).putDouble(0, v))
                },


                // byte, offset
                {"byte/offset", new Accessor<>((byte) 42,
                        s -> MemoryAccess.getByteAtOffset(s, 4), (s, x) -> MemoryAccess.setByteAtOffset(s, 4, x),
                        (bb) -> bb.get(4), (bb, v) -> bb.put(4, v))
                },
                // char, offset
                {"char/offset", new Accessor<>((char) 42,
                        s -> MemoryAccess.getCharAtOffset(s, 4), (s, x) -> MemoryAccess.setCharAtOffset(s, 4, x),
                        (bb) -> bb.order(NE).getChar(4), (bb, v) -> bb.order(NE).putChar(4, v))
                },
                {"char/offset/LE", new Accessor<>((char) 42,
                        s -> MemoryAccess.getCharAtOffset(s, 4, LE), (s, x) -> MemoryAccess.setCharAtOffset(s, 4, LE, x),
                        (bb) -> bb.order(LE).getChar(4), (bb, v) -> bb.order(LE).putChar(4, v))
                },
                {"char/offset/BE", new Accessor<>((char) 42,
                        s -> MemoryAccess.getCharAtOffset(s, 4, BE), (s, x) -> MemoryAccess.setCharAtOffset(s, 4, BE, x),
                        (bb) -> bb.order(BE).getChar(4), (bb, v) -> bb.order(BE).putChar(4, v))
                },
                // short, offset
                {"short/offset", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShortAtOffset(s, 4), (s, x) -> MemoryAccess.setShortAtOffset(s, 4, x),
                        (bb) -> bb.order(NE).getShort(4), (bb, v) -> bb.order(NE).putShort(4, v))
                },
                {"short/offset/LE", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShortAtOffset(s, 4, LE), (s, x) -> MemoryAccess.setShortAtOffset(s, 4, LE, x),
                        (bb) -> bb.order(LE).getShort(4), (bb, v) -> bb.order(LE).putShort(4, v))
                },
                {"short/offset/BE", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShortAtOffset(s, 4, BE), (s, x) -> MemoryAccess.setShortAtOffset(s, 4, BE, x),
                        (bb) -> bb.order(BE).getShort(4), (bb, v) -> bb.order(BE).putShort(4, v))
                },
                // int, offset
                {"int/offset", new Accessor<>(42,
                        s -> MemoryAccess.getIntAtOffset(s, 4), (s, x) -> MemoryAccess.setIntAtOffset(s, 4, x),
                        (bb) -> bb.order(NE).getInt(4), (bb, v) -> bb.order(NE).putInt(4, v))
                },
                {"int/offset/LE", new Accessor<>(42,
                        s -> MemoryAccess.getIntAtOffset(s, 4, LE), (s, x) -> MemoryAccess.setIntAtOffset(s, 4, LE, x),
                        (bb) -> bb.order(LE).getInt(4), (bb, v) -> bb.order(LE).putInt(4, v))
                },
                {"int/offset/BE", new Accessor<>(42,
                        s -> MemoryAccess.getIntAtOffset(s, 4, BE), (s, x) -> MemoryAccess.setIntAtOffset(s, 4, BE, x),
                        (bb) -> bb.order(BE).getInt(4), (bb, v) -> bb.order(BE).putInt(4, v))
                },
                // float, offset
                {"float/offset", new Accessor<>(42f,
                        s -> MemoryAccess.getFloatAtOffset(s, 4), (s, x) -> MemoryAccess.setFloatAtOffset(s, 4, x),
                        (bb) -> bb.order(NE).getFloat(4), (bb, v) -> bb.order(NE).putFloat(4, v))
                },
                {"float/offset/LE", new Accessor<>(42f,
                        s -> MemoryAccess.getFloatAtOffset(s, 4, LE), (s, x) -> MemoryAccess.setFloatAtOffset(s, 4, LE, x),
                        (bb) -> bb.order(LE).getFloat(4), (bb, v) -> bb.order(LE).putFloat(4, v))
                },
                {"float/offset/BE", new Accessor<>(42f,
                        s -> MemoryAccess.getFloatAtOffset(s, 4, BE), (s, x) -> MemoryAccess.setFloatAtOffset(s, 4, BE, x),
                        (bb) -> bb.order(BE).getFloat(4), (bb, v) -> bb.order(BE).putFloat(4, v))
                },
                // double, offset
                {"double/offset", new Accessor<>(42d,
                        s -> MemoryAccess.getDoubleAtOffset(s, 4), (s, x) -> MemoryAccess.setDoubleAtOffset(s, 4, x),
                        (bb) -> bb.order(NE).getDouble(4), (bb, v) -> bb.order(NE).putDouble(4, v))
                },
                {"double/offset/LE", new Accessor<>(42d,
                        s -> MemoryAccess.getDoubleAtOffset(s, 4, LE), (s, x) -> MemoryAccess.setDoubleAtOffset(s, 4, LE, x),
                        (bb) -> bb.order(LE).getDouble(4), (bb, v) -> bb.order(LE).putDouble(4, v))
                },
                {"double/offset/BE", new Accessor<>(42d,
                        s -> MemoryAccess.getDoubleAtOffset(s, 4, BE), (s, x) -> MemoryAccess.setDoubleAtOffset(s, 4, BE, x),
                        (bb) -> bb.order(BE).getDouble(4), (bb, v) -> bb.order(BE).putDouble(4, v))
                },


                // char, index
                {"char/index", new Accessor<>((char) 42,
                        s -> MemoryAccess.getCharAtIndex(s, 2), (s, x) -> MemoryAccess.setCharAtIndex(s, 2, x),
                        (bb) -> bb.order(NE).asCharBuffer().get(2), (bb, v) -> bb.order(NE).asCharBuffer().put(2, v))
                },
                {"char/index/LE", new Accessor<>((char) 42,
                        s -> MemoryAccess.getCharAtIndex(s, 2, LE), (s, x) -> MemoryAccess.setCharAtIndex(s, 2, LE, x),
                        (bb) -> bb.order(LE).asCharBuffer().get(2), (bb, v) -> bb.order(LE).asCharBuffer().put(2, v))
                },
                {"char/index/BE", new Accessor<>((char) 42,
                        s -> MemoryAccess.getCharAtIndex(s, 2, BE), (s, x) -> MemoryAccess.setCharAtIndex(s, 2, BE, x),
                        (bb) -> bb.order(BE).asCharBuffer().get(2), (bb, v) -> bb.order(BE).asCharBuffer().put(2, v))
                },
                // short, index
                {"short/index", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShortAtIndex(s, 2), (s, x) -> MemoryAccess.setShortAtIndex(s, 2, x),
                        (bb) -> bb.order(NE).asShortBuffer().get(2), (bb, v) -> bb.order(NE).asShortBuffer().put(2, v))
                },
                {"short/index/LE", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShortAtIndex(s, 2, LE), (s, x) -> MemoryAccess.setShortAtIndex(s, 2, LE, x),
                        (bb) -> bb.order(LE).asShortBuffer().get(2), (bb, v) -> bb.order(LE).asShortBuffer().put(2, v))
                },
                {"short/index/BE", new Accessor<>((short) 42,
                        s -> MemoryAccess.getShortAtIndex(s, 2, BE), (s, x) -> MemoryAccess.setShortAtIndex(s, 2, BE, x),
                        (bb) -> bb.order(BE).asShortBuffer().get(2), (bb, v) -> bb.order(BE).asShortBuffer().put(2, v))
                },
                {"int/index", new Accessor<>(42,
                        s -> MemoryAccess.getIntAtIndex(s, 2), (s, x) -> MemoryAccess.setIntAtIndex(s, 2, x),
                        (bb) -> bb.order(NE).asIntBuffer().get(2), (bb, v) -> bb.order(NE).asIntBuffer().put(2, v))
                },
                {"int/index/LE", new Accessor<>(42,
                        s -> MemoryAccess.getIntAtIndex(s, 2, LE), (s, x) -> MemoryAccess.setIntAtIndex(s, 2, LE, x),
                        (bb) -> bb.order(LE).asIntBuffer().get(2), (bb, v) -> bb.order(LE).asIntBuffer().put(2, v))
                },
                {"int/index/BE", new Accessor<>(42,
                        s -> MemoryAccess.getIntAtIndex(s, 2, BE), (s, x) -> MemoryAccess.setIntAtIndex(s, 2, BE, x),
                        (bb) -> bb.order(BE).asIntBuffer().get(2), (bb, v) -> bb.order(BE).asIntBuffer().put(2, v))
                },
                {"float/index", new Accessor<>(42f,
                        s -> MemoryAccess.getFloatAtIndex(s, 2), (s, x) -> MemoryAccess.setFloatAtIndex(s, 2, x),
                        (bb) -> bb.order(NE).asFloatBuffer().get(2), (bb, v) -> bb.order(NE).asFloatBuffer().put(2, v))
                },
                {"float/index/LE", new Accessor<>(42f,
                        s -> MemoryAccess.getFloatAtIndex(s, 2, LE), (s, x) -> MemoryAccess.setFloatAtIndex(s, 2, LE, x),
                        (bb) -> bb.order(LE).asFloatBuffer().get(2), (bb, v) -> bb.order(LE).asFloatBuffer().put(2, v))
                },
                {"float/index/BE", new Accessor<>(42f,
                        s -> MemoryAccess.getFloatAtIndex(s, 2, BE), (s, x) -> MemoryAccess.setFloatAtIndex(s, 2, BE, x),
                        (bb) -> bb.order(BE).asFloatBuffer().get(2), (bb, v) -> bb.order(BE).asFloatBuffer().put(2, v))
                },
                {"double/index", new Accessor<>(42d,
                        s -> MemoryAccess.getDoubleAtIndex(s, 2), (s, x) -> MemoryAccess.setDoubleAtIndex(s, 2, x),
                        (bb) -> bb.order(NE).asDoubleBuffer().get(2), (bb, v) -> bb.order(NE).asDoubleBuffer().put(2, v))
                },
                {"double/index/LE", new Accessor<>(42d,
                        s -> MemoryAccess.getDoubleAtIndex(s, 2, LE), (s, x) -> MemoryAccess.setDoubleAtIndex(s, 2, LE, x),
                        (bb) -> bb.order(LE).asDoubleBuffer().get(2), (bb, v) -> bb.order(LE).asDoubleBuffer().put(2, v))
                },
                {"double/index/BE", new Accessor<>(42d,
                        s -> MemoryAccess.getDoubleAtIndex(s, 2, BE), (s, x) -> MemoryAccess.setDoubleAtIndex(s, 2, BE, x),
                        (bb) -> bb.order(BE).asDoubleBuffer().get(2), (bb, v) -> bb.order(BE).asDoubleBuffer().put(2, v))
                },

                { "address", new Accessor<>(MemoryAddress.ofLong(42),
                        MemoryAccess::getAddress, MemoryAccess::setAddress,
                        (bb) -> {
                            ByteBuffer nb = bb.order(NE);
                            long addr = MemoryLayouts.ADDRESS.byteSize() == 8 ?
                                    nb.getLong(0) : nb.getInt(0);
                            return MemoryAddress.ofLong(addr);
                        },
                        (bb, v) -> {
                            ByteBuffer nb = bb.order(NE);
                            if (MemoryLayouts.ADDRESS.byteSize() == 8) {
                                nb.putLong(0, v.toRawLongValue());
                            } else {
                                nb.putInt(0, (int)v.toRawLongValue());
                            }
                        })
                },
                { "address/offset", new Accessor<>(MemoryAddress.ofLong(42),
                        s -> MemoryAccess.getAddressAtOffset(s, 4), (s, x) -> MemoryAccess.setAddressAtOffset(s, 4, x),
                        (bb) -> {
                            ByteBuffer nb = bb.order(NE);
                            long addr = MemoryLayouts.ADDRESS.byteSize() == 8 ?
                                    nb.getLong(4) : nb.getInt(4);
                            return MemoryAddress.ofLong(addr);
                        },
                        (bb, v) -> {
                            ByteBuffer nb = bb.order(NE);
                            if (MemoryLayouts.ADDRESS.byteSize() == 8) {
                                nb.putLong(4, v.toRawLongValue());
                            } else {
                                nb.putInt(4, (int)v.toRawLongValue());
                            }
                        })
                },
                { "address/index", new Accessor<>(MemoryAddress.ofLong(42),
                        s -> MemoryAccess.getAddressAtIndex(s, 2), (s, x) -> MemoryAccess.setAddressAtIndex(s, 2, x),
                        (bb) -> {
                            ByteBuffer nb = bb.order(NE);
                            long addr = MemoryLayouts.ADDRESS.byteSize() == 8 ?
                                    nb.asLongBuffer().get(2) : nb.asIntBuffer().get(2);
                            return MemoryAddress.ofLong(addr);
                        },
                        (bb, v) -> {
                            ByteBuffer nb = bb.order(NE);
                            if (MemoryLayouts.ADDRESS.byteSize() == 8) {
                                nb.asLongBuffer().put(2, v.toRawLongValue());
                            } else {
                                nb.asIntBuffer().put(2, (int)v.toRawLongValue());
                            }
                        })
                },
        };
    }
}
