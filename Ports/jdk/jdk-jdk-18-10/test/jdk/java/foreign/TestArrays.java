/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

/*
 * @test
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestArrays
 */

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemoryLayout.PathElement;
import jdk.incubator.foreign.MemoryLayouts;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SequenceLayout;

import java.lang.invoke.VarHandle;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.testng.annotations.*;

import static org.testng.Assert.*;

public class TestArrays {

    static SequenceLayout bytes = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_BYTE
    );

    static SequenceLayout chars = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_CHAR
    );

    static SequenceLayout shorts = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_SHORT
    );

    static SequenceLayout ints = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_INT
    );

    static SequenceLayout floats = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_FLOAT
    );

    static SequenceLayout longs = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_LONG
    );

    static SequenceLayout doubles = MemoryLayout.sequenceLayout(100,
            MemoryLayouts.JAVA_DOUBLE
    );

    static VarHandle byteHandle = bytes.varHandle(byte.class, PathElement.sequenceElement());
    static VarHandle charHandle = chars.varHandle(char.class, PathElement.sequenceElement());
    static VarHandle shortHandle = shorts.varHandle(short.class, PathElement.sequenceElement());
    static VarHandle intHandle = ints.varHandle(int.class, PathElement.sequenceElement());
    static VarHandle floatHandle = floats.varHandle(float.class, PathElement.sequenceElement());
    static VarHandle longHandle = longs.varHandle(long.class, PathElement.sequenceElement());
    static VarHandle doubleHandle = doubles.varHandle(double.class, PathElement.sequenceElement());

    static void initBytes(MemorySegment base, SequenceLayout seq, BiConsumer<MemorySegment, Long> handleSetter) {
        for (long i = 0; i < seq.elementCount().getAsLong() ; i++) {
            handleSetter.accept(base, i);
        }
    }

    static void checkBytes(MemorySegment base, SequenceLayout layout, Function<MemorySegment, Object> arrayFactory, BiFunction<MemorySegment, Long, Object> handleGetter) {
        int nelems = (int)layout.elementCount().getAsLong();
        Object arr = arrayFactory.apply(base);
        for (int i = 0; i < nelems; i++) {
            Object found = handleGetter.apply(base, (long) i);
            Object expected = java.lang.reflect.Array.get(arr, i);
            assertEquals(expected, found);
        }
    }

    @Test(dataProvider = "arrays")
    public void testArrays(Consumer<MemorySegment> init, Consumer<MemorySegment> checker, MemoryLayout layout) {
        MemorySegment segment = MemorySegment.allocateNative(layout, ResourceScope.newImplicitScope());
        init.accept(segment);
        assertFalse(segment.isReadOnly());
        checker.accept(segment);
    }

    @Test(dataProvider = "elemLayouts",
            expectedExceptions = IllegalStateException.class)
    public void testTooBigForArray(MemoryLayout layout, Function<MemorySegment, Object> arrayFactory) {
        MemoryLayout seq = MemoryLayout.sequenceLayout((Integer.MAX_VALUE * layout.byteSize()) + 1, layout);
        //do not really allocate here, as it's way too much memory
        MemorySegment segment = MemoryAddress.NULL.asSegment(seq.byteSize(), ResourceScope.globalScope());
        arrayFactory.apply(segment);
    }

    @Test(dataProvider = "elemLayouts",
            expectedExceptions = IllegalStateException.class)
    public void testBadSize(MemoryLayout layout, Function<MemorySegment, Object> arrayFactory) {
        if (layout.byteSize() == 1) throw new IllegalStateException(); //make it fail
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment segment = MemorySegment.allocateNative(layout.byteSize() + 1, layout.byteSize(), scope);
            arrayFactory.apply(segment);
        }
    }

    @Test(dataProvider = "elemLayouts",
            expectedExceptions = IllegalStateException.class)
    public void testArrayFromClosedSegment(MemoryLayout layout, Function<MemorySegment, Object> arrayFactory) {
        MemorySegment segment = MemorySegment.allocateNative(layout, ResourceScope.newConfinedScope());
        segment.scope().close();
        arrayFactory.apply(segment);
    }

    @DataProvider(name = "arrays")
    public Object[][] nativeAccessOps() {
        Consumer<MemorySegment> byteInitializer =
                (base) -> initBytes(base, bytes, (addr, pos) -> byteHandle.set(addr, pos, (byte)(long)pos));
        Consumer<MemorySegment> charInitializer =
                (base) -> initBytes(base, chars, (addr, pos) -> charHandle.set(addr, pos, (char)(long)pos));
        Consumer<MemorySegment> shortInitializer =
                (base) -> initBytes(base, shorts, (addr, pos) -> shortHandle.set(addr, pos, (short)(long)pos));
        Consumer<MemorySegment> intInitializer =
                (base) -> initBytes(base, ints, (addr, pos) -> intHandle.set(addr, pos, (int)(long)pos));
        Consumer<MemorySegment> floatInitializer =
                (base) -> initBytes(base, floats, (addr, pos) -> floatHandle.set(addr, pos, (float)(long)pos));
        Consumer<MemorySegment> longInitializer =
                (base) -> initBytes(base, longs, (addr, pos) -> longHandle.set(addr, pos, (long)pos));
        Consumer<MemorySegment> doubleInitializer =
                (base) -> initBytes(base, doubles, (addr, pos) -> doubleHandle.set(addr, pos, (double)(long)pos));

        Consumer<MemorySegment> byteChecker =
                (base) -> checkBytes(base, bytes, MemorySegment::toByteArray, (addr, pos) -> (byte)byteHandle.get(addr, pos));
        Consumer<MemorySegment> shortChecker =
                (base) -> checkBytes(base, shorts, MemorySegment::toShortArray, (addr, pos) -> (short)shortHandle.get(addr, pos));
        Consumer<MemorySegment> charChecker =
                (base) -> checkBytes(base, chars, MemorySegment::toCharArray, (addr, pos) -> (char)charHandle.get(addr, pos));
        Consumer<MemorySegment> intChecker =
                (base) -> checkBytes(base, ints, MemorySegment::toIntArray, (addr, pos) -> (int)intHandle.get(addr, pos));
        Consumer<MemorySegment> floatChecker =
                (base) -> checkBytes(base, floats, MemorySegment::toFloatArray, (addr, pos) -> (float)floatHandle.get(addr, pos));
        Consumer<MemorySegment> longChecker =
                (base) -> checkBytes(base, longs, MemorySegment::toLongArray, (addr, pos) -> (long)longHandle.get(addr, pos));
        Consumer<MemorySegment> doubleChecker =
                (base) -> checkBytes(base, doubles, MemorySegment::toDoubleArray, (addr, pos) -> (double)doubleHandle.get(addr, pos));

        return new Object[][]{
                {byteInitializer, byteChecker, bytes},
                {charInitializer, charChecker, chars},
                {shortInitializer, shortChecker, shorts},
                {intInitializer, intChecker, ints},
                {floatInitializer, floatChecker, floats},
                {longInitializer, longChecker, longs},
                {doubleInitializer, doubleChecker, doubles}
        };
    }

    @DataProvider(name = "elemLayouts")
    public Object[][] elemLayouts() {
        return new Object[][] {
                { MemoryLayouts.JAVA_BYTE, (Function<MemorySegment, Object>) MemorySegment::toByteArray },
                { MemoryLayouts.JAVA_SHORT, (Function<MemorySegment, Object>) MemorySegment::toShortArray },
                { MemoryLayouts.JAVA_CHAR, (Function<MemorySegment, Object>) MemorySegment::toCharArray },
                { MemoryLayouts.JAVA_INT, (Function<MemorySegment, Object>) MemorySegment::toIntArray },
                { MemoryLayouts.JAVA_FLOAT, (Function<MemorySegment, Object>) MemorySegment::toFloatArray },
                { MemoryLayouts.JAVA_LONG, (Function<MemorySegment, Object>) MemorySegment::toLongArray },
                { MemoryLayouts.JAVA_DOUBLE, (Function<MemorySegment, Object>) MemorySegment::toDoubleArray }
        };
    }
}
