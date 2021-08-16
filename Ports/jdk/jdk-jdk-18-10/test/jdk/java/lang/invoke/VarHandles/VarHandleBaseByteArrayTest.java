/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;

import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public abstract class VarHandleBaseByteArrayTest extends VarHandleBaseTest {

    enum MemoryMode {
        ALIGNED(0, false), UNALIGNED(0, true),
        BIG_ENDIAN(1, false), LITTLE_ENDIAN(1, true),
        READ_WRITE(2, false), READ_ONLY(2, true),;

        final int bit;
        final int value;

        MemoryMode(int bit, boolean value) {
            this.bit = bit;
            this.value = value ? 1 << bit : 0;
        }

        boolean isSet(int bitSet) {
            return (bitSet & (1 << bit)) == value;
        }

        static int bitSet(MemoryMode... modes) {
            if (modes == null) return 0;

            int set = 0;
            for (MemoryMode m : modes) {
                set = (set & ~(1 << m.bit)) | m.value;
            }
            return set;
        }

        static EnumSet<MemoryMode> enumSet(int bitSet) {
            EnumSet<MemoryMode> es = EnumSet.noneOf(MemoryMode.class);
            for (MemoryMode m : values()) {
                if (m.isSet(bitSet)) {
                    es.add(m);
                }
            }
            return es;
        }
    }

    static class Source<T> {
        final T s;
        final int memoryModes;

        public Source(T s, MemoryMode... modes) {
            this.s = s;
            memoryModes = MemoryMode.bitSet(modes);
        }

        @Override
        public String toString() {
            return s.getClass().getCanonicalName() + " " + MemoryMode.enumSet(memoryModes);
        }
    }

    static abstract class ByteArrayViewSource<T> extends Source<T> {
        public ByteArrayViewSource(T t, MemoryMode... modes) {
            super(t, modes);
        }

        abstract void fill(byte value);

        abstract void fill(byte[] values);
    }

    static class ByteArraySource extends ByteArrayViewSource<byte[]> {
        public ByteArraySource(byte[] bytes, MemoryMode... modes) {
            super(bytes, modes);
        }

        void fill(byte value) {
            Arrays.fill(s, value);
        }

        void fill(byte[] values) {
            for (int i = 0; i < s.length; i++) {
                s[i] = values[i % values.length];
            }
        }
    }

    static class ByteBufferSource extends ByteArrayViewSource<ByteBuffer> {
        public ByteBufferSource(ByteBuffer buffer, MemoryMode... modes) {
            super(buffer, modes);
        }

        void fill(byte value) {
            for (int i = 0; i < s.limit(); i++) {
                s.put(i, value);
            }
        }

        void fill(byte[] values) {
            for (int i = 0; i < s.limit(); i++) {
                s.put(i, values[i % values.length]);
            }
        }

        @Override
        public String toString() {
            return s + " " + MemoryMode.enumSet(memoryModes);
        }
    }

    static class ByteBufferReadOnlySource extends ByteBufferSource {
        final ByteBuffer rwSource;

        public ByteBufferReadOnlySource(ByteBuffer roBuffer, ByteBuffer rwSource, MemoryMode... modes) {
            super(roBuffer, modes);
            this.rwSource = rwSource;
        }

        void fill(byte value) {
            for (int i = 0; i < rwSource.limit(); i++) {
                rwSource.put(i, value);
            }
        }

        void fill(byte[] values) {
            for (int i = 0; i < rwSource.limit(); i++) {
                rwSource.put(i, values[i % values.length]);
            }
        }
    }

    static class VarHandleSource extends Source<VarHandle> {
        VarHandleSource(VarHandle vh, MemoryMode... modes) {
            super(vh, modes);
        }

        boolean matches(ByteArrayViewSource<?> bav) {
            return s.coordinateTypes().get(0).isAssignableFrom(bav.s.getClass());
        }

        @Override
        public String toString() {
            return " VarHandle " + MemoryMode.enumSet(memoryModes);
        }
    }

    static class VarHandleSourceAccessTestCase extends AccessTestCase<VarHandleSource> {
        final ByteArrayViewSource<?> bs;
        final VarHandleSource vhs;

        VarHandleSourceAccessTestCase(String desc, ByteArrayViewSource<?> bs, VarHandleSource vhs, AccessTestAction<VarHandleSource> ata) {
            this(desc, bs, vhs, ata, true);
        }

        VarHandleSourceAccessTestCase(String desc, ByteArrayViewSource<?> bs, VarHandleSource vhs, AccessTestAction<VarHandleSource> ata, boolean loop) {
            super(vhs + " -> " + bs + " " + desc, ata, loop);
            this.bs = bs;
            this.vhs = vhs;
        }

        @Override
        VarHandleSource get() {
            return vhs;
        }
    }


    static double rotateLeft(double i, int distance) {
        return Double.longBitsToDouble(
                Long.rotateLeft(Double.doubleToRawLongBits(i), distance));
    }

    static double rotateRight(double i, int distance) {
        return Double.longBitsToDouble(
                Long.rotateRight(Double.doubleToRawLongBits(i), distance));
    }

    static float rotateLeft(float i, int distance) {
        return Float.intBitsToFloat(
                Integer.rotateLeft(Float.floatToRawIntBits(i), distance));
    }

    static float rotateRight(float i, int distance) {
        return Float.intBitsToFloat(
                Integer.rotateRight(Float.floatToRawIntBits(i), distance));
    }

    static long rotateLeft(long i, int distance) {
        return Long.rotateLeft(i, distance);
    }

    static long rotateRight(long i, int distance) {
        return Long.rotateRight(i, distance);
    }

    static int rotateLeft(int i, int distance) {
        return Integer.rotateLeft(i, distance);
    }

    static int rotateRight(int i, int distance) {
        return Integer.rotateRight(i, distance);
    }

    static short rotateLeft(short i, int distance) {
        int v = (i << 16) | i;
        v = Integer.rotateLeft(v, distance);
        return (short) v;
    }

    static short rotateRight(short i, int distance) {
        int v = (i << 16) | i;
        v = Integer.rotateRight(v, distance);
        return (short) v;
    }

    static char rotateLeft(char i, int distance) {
        int v = (i << 16) | i;
        v = Integer.rotateLeft(v, distance);
        return (char) v;
    }

    static char rotateRight(char i, int distance) {
        int v = (i << 16) | i;
        v = Integer.rotateRight(v, distance);
        return (char) v;
    }

    static final int LENGTH_BYTES = 32;

    byte[] array;

    List<ByteArrayViewSource<?>> bavss;

    List<VarHandleSource> vhss;

    public void setupByteSources() {
        array = new byte[LENGTH_BYTES];

        // Native endianess
        MemoryMode ne = ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN
                        ? MemoryMode.BIG_ENDIAN : MemoryMode.LITTLE_ENDIAN;

        bavss = new ArrayList<>();

        // byte[] source
        ByteArraySource a =
                new ByteArraySource(array,
                                    ne, MemoryMode.READ_WRITE);
        bavss.add(a);


        // Combinations of ByteBuffer sources
        ByteBufferSource hbb =
                new ByteBufferSource(ByteBuffer.wrap(array),
                                     MemoryMode.ALIGNED, ne, MemoryMode.READ_WRITE);
        bavss.add(hbb);
        ByteBufferReadOnlySource hbb_ro =
                new ByteBufferReadOnlySource(hbb.s.asReadOnlyBuffer(), hbb.s,
                                             MemoryMode.ALIGNED, ne, MemoryMode.READ_ONLY);
        bavss.add(hbb_ro);

        ByteBufferSource hbb_offset_aligned =
                new ByteBufferSource(ByteBuffer.wrap(array, array.length / 4, array.length / 2).slice(),
                                     MemoryMode.ALIGNED, ne, MemoryMode.READ_WRITE);
        bavss.add(hbb_offset_aligned);
        ByteBufferReadOnlySource hbb_offset_aligned_ro =
                new ByteBufferReadOnlySource(hbb_offset_aligned.s.asReadOnlyBuffer(), hbb_offset_aligned.s,
                                             MemoryMode.ALIGNED, ne, MemoryMode.READ_ONLY);
        bavss.add(hbb_offset_aligned_ro);

        ByteBufferSource hbb_offset_unaligned =
                new ByteBufferSource(ByteBuffer.wrap(array, array.length / 4 - 1, array.length / 2).slice(),
                                     MemoryMode.UNALIGNED, ne, MemoryMode.READ_WRITE);
        bavss.add(hbb_offset_unaligned);
        ByteBufferReadOnlySource hbb_offset_unaligned_ro =
                new ByteBufferReadOnlySource(hbb_offset_unaligned.s.asReadOnlyBuffer(), hbb_offset_unaligned.s,
                                             MemoryMode.UNALIGNED, ne, MemoryMode.READ_ONLY);
        bavss.add(hbb_offset_unaligned_ro);


        ByteBufferSource dbb =
                new ByteBufferSource(ByteBuffer.allocateDirect(array.length),
                                     MemoryMode.ALIGNED, ne, MemoryMode.READ_WRITE);
        bavss.add(dbb);
        ByteBufferReadOnlySource dbb_ro =
                new ByteBufferReadOnlySource(dbb.s.asReadOnlyBuffer(), dbb.s,
                                             MemoryMode.ALIGNED, ne, MemoryMode.READ_ONLY);
        bavss.add(dbb_ro);

        ByteBufferSource dbb_offset_aligned =
                new ByteBufferSource(dbb.s.slice().position(array.length / 4).limit(array.length / 4 + array.length / 2).slice(),
                                     MemoryMode.ALIGNED, ne, MemoryMode.READ_WRITE);
        bavss.add(dbb_offset_aligned);
        ByteBufferReadOnlySource dbb_offset_aligned_ro =
                new ByteBufferReadOnlySource(dbb_offset_aligned.s.asReadOnlyBuffer(), dbb_offset_aligned.s,
                                             MemoryMode.ALIGNED, ne, MemoryMode.READ_ONLY);
        bavss.add(dbb_offset_aligned_ro);

        ByteBufferSource dbb_offset_unaligned =
                new ByteBufferSource(dbb.s.slice().position(array.length / 4 - 1).limit(array.length / 4 - 1 + array.length / 2).slice(),
                                     MemoryMode.UNALIGNED, ne, MemoryMode.READ_WRITE);
        bavss.add(dbb_offset_unaligned);
        ByteBufferReadOnlySource dbb_offset_unaligned_ro =
                new ByteBufferReadOnlySource(dbb_offset_unaligned.s.asReadOnlyBuffer(), dbb_offset_unaligned.s,
                                             MemoryMode.UNALIGNED, ne, MemoryMode.READ_ONLY);
        bavss.add(dbb_offset_unaligned_ro);
    }

    @BeforeClass
    public void setup() {
        setupByteSources();
        vhss = setupVarHandleSources(true);
    }

    abstract List<VarHandleSource> setupVarHandleSources(boolean same);


    @DataProvider
    public Object[][] varHandlesProvider() throws Exception {
        return vhss.stream().map(cvh -> new Object[]{cvh}).toArray(Object[][]::new);
    }

    @DataProvider
    public Object[][] typesProvider() throws Exception {
        List<java.lang.Class<?>> aepts = Arrays.asList(byte[].class, int.class);
        List<java.lang.Class<?>> bbpts = Arrays.asList(ByteBuffer.class, int.class);

        Function<VarHandle, List<Class<?>>> vhToPts = vh ->
                vh.coordinateTypes().get(0) == byte[].class ? aepts : bbpts;

        return vhss.stream().map(vh -> new Object[]{vh.s, vhToPts.apply(vh.s)}).toArray(Object[][]::new);
    }
}
