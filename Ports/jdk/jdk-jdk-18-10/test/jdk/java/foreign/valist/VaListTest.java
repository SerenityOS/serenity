/*
 *  Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library ../
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @modules jdk.incubator.foreign/jdk.internal.foreign
 *          jdk.incubator.foreign/jdk.internal.foreign.abi
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.aarch64.linux
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.aarch64.macos
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.x64.windows
 *          jdk.incubator.foreign/jdk.internal.foreign.abi.x64.sysv
 * @run testng/othervm --enable-native-access=ALL-UNNAMED VaListTest
 */

import jdk.incubator.foreign.*;
import jdk.incubator.foreign.CLinker.VaList;
import jdk.internal.foreign.abi.aarch64.linux.LinuxAArch64Linker;
import jdk.internal.foreign.abi.aarch64.macos.MacOsAArch64Linker;
import jdk.internal.foreign.abi.x64.sysv.SysVx64Linker;
import jdk.internal.foreign.abi.x64.windows.Windowsx64Linker;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static jdk.incubator.foreign.CLinker.C_DOUBLE;
import static jdk.incubator.foreign.CLinker.C_FLOAT;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG;
import static jdk.incubator.foreign.CLinker.C_LONG_LONG;
import static jdk.incubator.foreign.CLinker.C_POINTER;
import static jdk.incubator.foreign.CLinker.C_VA_LIST;
import static jdk.incubator.foreign.MemoryLayout.PathElement.groupElement;
import static jdk.incubator.foreign.MemoryLayouts.JAVA_INT;
import static jdk.internal.foreign.PlatformLayouts.*;
import static org.testng.Assert.*;

public class VaListTest extends NativeTestHelper {

    private static final CLinker abi = CLinker.getInstance();
    static {
        System.loadLibrary("VaList");
    }

    static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    private static final MethodHandle MH_sumInts = link("sumInts",
            MethodType.methodType(int.class, int.class, VaList.class),
            FunctionDescriptor.of(C_INT, C_INT, C_VA_LIST));
    private static final MethodHandle MH_sumDoubles = link("sumDoubles",
            MethodType.methodType(double.class, int.class, VaList.class),
            FunctionDescriptor.of(C_DOUBLE, C_INT, C_VA_LIST));
    private static final MethodHandle MH_getInt = link("getInt",
            MethodType.methodType(int.class, VaList.class),
            FunctionDescriptor.of(C_INT, C_VA_LIST));
    private static final MethodHandle MH_sumStruct = link("sumStruct",
            MethodType.methodType(int.class, VaList.class),
            FunctionDescriptor.of(C_INT, C_VA_LIST));
    private static final MethodHandle MH_sumBigStruct = link("sumBigStruct",
            MethodType.methodType(long.class, VaList.class),
            FunctionDescriptor.of(C_LONG_LONG, C_VA_LIST));
    private static final MethodHandle MH_sumHugeStruct = link("sumHugeStruct",
            MethodType.methodType(long.class, VaList.class),
            FunctionDescriptor.of(C_LONG_LONG, C_VA_LIST));
    private static final MethodHandle MH_sumFloatStruct = link("sumFloatStruct",
            MethodType.methodType(float.class, VaList.class),
            FunctionDescriptor.of(C_FLOAT, C_VA_LIST));
    private static final MethodHandle MH_sumStack = link("sumStack",
            MethodType.methodType(void.class, MemoryAddress.class, MemoryAddress.class, VaList.class),
            FunctionDescriptor.ofVoid(C_POINTER, C_POINTER, C_VA_LIST));

    private static MethodHandle link(String symbol, MethodType mt, FunctionDescriptor fd) {
        return abi.downcallHandle(LOOKUP.lookup(symbol).get(), mt, fd);
    }

    private static MethodHandle linkVaListCB(String symbol) {
        return link(symbol,
                MethodType.methodType(void.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(C_POINTER));

    }

    private static final Function<Consumer<VaList.Builder>, VaList> winVaListFactory
            = actions -> Windowsx64Linker.newVaList(actions, ResourceScope.newConfinedScope());
    private static final Function<Consumer<VaList.Builder>, VaList> sysvVaListFactory
            = actions -> SysVx64Linker.newVaList(actions, ResourceScope.newConfinedScope());
    private static final Function<Consumer<VaList.Builder>, VaList> linuxAArch64VaListFactory
            = actions -> LinuxAArch64Linker.newVaList(actions, ResourceScope.newConfinedScope());
    private static final Function<Consumer<VaList.Builder>, VaList> macAArch64VaListFactory
            = actions -> MacOsAArch64Linker.newVaList(actions, ResourceScope.newConfinedScope());
    private static final Function<Consumer<VaList.Builder>, VaList> platformVaListFactory
            = (builder) -> VaList.make(builder, ResourceScope.newConfinedScope());

    private static final BiFunction<Consumer<VaList.Builder>, NativeScope, VaList> winVaListScopedFactory
            = (builder, scope) -> Windowsx64Linker.newVaList(builder, scope.scope());
    private static final BiFunction<Consumer<VaList.Builder>, NativeScope, VaList> sysvVaListScopedFactory
            = (builder, scope) -> SysVx64Linker.newVaList(builder, scope.scope());
    private static final BiFunction<Consumer<VaList.Builder>, NativeScope, VaList> linuxAArch64VaListScopedFactory
            = (builder, scope) -> LinuxAArch64Linker.newVaList(builder, scope.scope());
    private static final BiFunction<Consumer<VaList.Builder>, NativeScope, VaList> macAArch64VaListScopedFactory
            = (builder, scope) -> MacOsAArch64Linker.newVaList(builder, scope.scope());
    private static final BiFunction<Consumer<VaList.Builder>, NativeScope, VaList> platformVaListScopedFactory
            = (builder, scope) -> VaList.make(builder, scope.scope());

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] sumInts() {
        Function<MemoryLayout, BiFunction<Integer, VaList, Integer>> sumIntsJavaFact = layout ->
                (num, list) -> IntStream.generate(() -> list.vargAsInt(layout)).limit(num).sum();
        BiFunction<Integer, VaList, Integer> sumIntsNative
                = MethodHandleProxies.asInterfaceInstance(BiFunction.class, MH_sumInts);
        return new Object[][]{
                { winVaListFactory,          sumIntsJavaFact.apply(Win64.C_INT),   Win64.C_INT   },
                { sysvVaListFactory,         sumIntsJavaFact.apply(SysV.C_INT),    SysV.C_INT    },
                { linuxAArch64VaListFactory, sumIntsJavaFact.apply(AArch64.C_INT), AArch64.C_INT },
                { macAArch64VaListFactory,   sumIntsJavaFact.apply(AArch64.C_INT), AArch64.C_INT },
                { platformVaListFactory,     sumIntsNative,                        C_INT         },
        };
    }

    @Test(dataProvider = "sumInts")
    public void testIntSum(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                           BiFunction<Integer, VaList, Integer> sumInts,
                           ValueLayout intLayout) {
        VaList vaList = vaListFactory.apply(b ->
            b.vargFromInt(intLayout, 10)
                    .vargFromInt(intLayout, 15)
                    .vargFromInt(intLayout, 20));
        int x = sumInts.apply(3, vaList);
        assertEquals(x, 45);
        vaList.scope().close();
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] sumDoubles() {
        Function<MemoryLayout, BiFunction<Integer, VaList, Double>> sumDoublesJavaFact  = layout ->
                (num, list) -> DoubleStream.generate(() -> list.vargAsDouble(layout)).limit(num).sum();
        BiFunction<Integer, VaList, Double> sumDoublesNative
                = MethodHandleProxies.asInterfaceInstance(BiFunction.class, MH_sumDoubles);
        return new Object[][]{
                { winVaListFactory,          sumDoublesJavaFact.apply(Win64.C_DOUBLE),   Win64.C_DOUBLE   },
                { sysvVaListFactory,         sumDoublesJavaFact.apply(SysV.C_DOUBLE),    SysV.C_DOUBLE    },
                { linuxAArch64VaListFactory, sumDoublesJavaFact.apply(AArch64.C_DOUBLE), AArch64.C_DOUBLE },
                { macAArch64VaListFactory,   sumDoublesJavaFact.apply(AArch64.C_DOUBLE), AArch64.C_DOUBLE },
                { platformVaListFactory,     sumDoublesNative,                           C_DOUBLE         },
        };
    }

    @Test(dataProvider = "sumDoubles")
    public void testDoubleSum(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                              BiFunction<Integer, VaList, Double> sumDoubles,
                              ValueLayout doubleLayout) {
        VaList vaList = vaListFactory.apply(b ->
            b.vargFromDouble(doubleLayout, 3.0D)
                    .vargFromDouble(doubleLayout, 4.0D)
                    .vargFromDouble(doubleLayout, 5.0D));
        double x = sumDoubles.apply(3, vaList);
        assertEquals(x, 12.0D);
        vaList.scope().close();
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] pointers() {
        Function<MemoryLayout, Function<VaList, Integer>> getIntJavaFact = layout ->
                list -> {
                    MemoryAddress ma = list.vargAsAddress(layout);
                    return MemoryAccess.getIntAtOffset(MemorySegment.globalNativeSegment(), ma.toRawLongValue());
                };
        Function<VaList, Integer> getIntNative = MethodHandleProxies.asInterfaceInstance(Function.class, MH_getInt);
        return new Object[][]{
                { winVaListFactory,          getIntJavaFact.apply(Win64.C_POINTER),   Win64.C_POINTER   },
                { sysvVaListFactory,         getIntJavaFact.apply(SysV.C_POINTER),    SysV.C_POINTER    },
                { linuxAArch64VaListFactory, getIntJavaFact.apply(AArch64.C_POINTER), AArch64.C_POINTER },
                { macAArch64VaListFactory,   getIntJavaFact.apply(AArch64.C_POINTER), AArch64.C_POINTER },
                { platformVaListFactory,     getIntNative,                            C_POINTER         },
        };
    }

    @Test(dataProvider = "pointers")
    public void testVaListMemoryAddress(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                                        Function<VaList, Integer> getFromPointer,
                                        ValueLayout pointerLayout) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment msInt = MemorySegment.allocateNative(JAVA_INT, scope);
            MemoryAccess.setInt(msInt, 10);
            VaList vaList = vaListFactory.apply(b -> b.vargFromAddress(pointerLayout, msInt.address()));
            int x = getFromPointer.apply(vaList);
            assertEquals(x, 10);
            vaList.scope().close();
        }
    }

    interface TriFunction<S, T, U, R> {
        R apply(S s, T t, U u);
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] structs() {
        TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Integer>> sumStructJavaFact
                = (pointLayout, VH_Point_x, VH_Point_y) ->
                list -> {
                    MemorySegment struct = list.vargAsSegment(pointLayout, ResourceScope.newImplicitScope());
                    int x = (int) VH_Point_x.get(struct);
                    int y = (int) VH_Point_y.get(struct);
                    return x + y;
                };

        TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Integer>> sumStructNativeFact
                = (pointLayout, VH_Point_x, VH_Point_y) ->
                MethodHandleProxies.asInterfaceInstance(Function.class, MH_sumStruct);

        TriFunction<Function<Consumer<VaList.Builder>, VaList>, MemoryLayout,
                TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Integer>>, Object[]> argsFact
                = (vaListFact, intLayout, sumStructFact) -> {
            GroupLayout pointLayout =  MemoryLayout.structLayout(
                    intLayout.withName("x"),
                    intLayout.withName("y")
            );
            VarHandle VH_Point_x = pointLayout.varHandle(int.class, groupElement("x"));
            VarHandle VH_Point_y = pointLayout.varHandle(int.class, groupElement("y"));
            return new Object[] { vaListFact, sumStructFact.apply(pointLayout, VH_Point_x, VH_Point_y),
                    pointLayout, VH_Point_x, VH_Point_y  };
        };
        return new Object[][]{
                argsFact.apply(winVaListFactory,          Win64.C_INT,   sumStructJavaFact),
                argsFact.apply(sysvVaListFactory,         SysV.C_INT,    sumStructJavaFact),
                argsFact.apply(linuxAArch64VaListFactory, AArch64.C_INT, sumStructJavaFact),
                argsFact.apply(macAArch64VaListFactory,   AArch64.C_INT, sumStructJavaFact),
                argsFact.apply(platformVaListFactory,     C_INT,         sumStructNativeFact),
        };
    }

    @Test(dataProvider = "structs")
    public void testStruct(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                           Function<VaList, Integer> sumStruct,
                           GroupLayout Point_LAYOUT, VarHandle VH_Point_x, VarHandle VH_Point_y) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment struct = MemorySegment.allocateNative(Point_LAYOUT, scope);
            VH_Point_x.set(struct, 5);
            VH_Point_y.set(struct, 10);

            VaList vaList = vaListFactory.apply(b -> b.vargFromSegment(Point_LAYOUT, struct));
            int sum = sumStruct.apply(vaList);
            assertEquals(sum, 15);
            vaList.scope().close();
        }
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] bigStructs() {
        TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Long>> sumStructJavaFact
                = (BigPoint_LAYOUT, VH_BigPoint_x, VH_BigPoint_y) ->
                list -> {
                    MemorySegment struct = list.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    long x = (long) VH_BigPoint_x.get(struct);
                    long y = (long) VH_BigPoint_y.get(struct);
                    return x + y;
                };

        TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Long>> sumStructNativeFact
                = (pointLayout, VH_BigPoint_x, VH_BigPoint_y) ->
                MethodHandleProxies.asInterfaceInstance(Function.class, MH_sumBigStruct);

        TriFunction<Function<Consumer<VaList.Builder>, VaList>, MemoryLayout,
                TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Long>>, Object[]> argsFact
                = (vaListFact, longLongLayout, sumBigStructFact) -> {
            GroupLayout BigPoint_LAYOUT =  MemoryLayout.structLayout(
                    longLongLayout.withName("x"),
                    longLongLayout.withName("y")
            );
            VarHandle VH_BigPoint_x = BigPoint_LAYOUT.varHandle(long.class, groupElement("x"));
            VarHandle VH_BigPoint_y = BigPoint_LAYOUT.varHandle(long.class, groupElement("y"));
            return new Object[] { vaListFact, sumBigStructFact.apply(BigPoint_LAYOUT, VH_BigPoint_x, VH_BigPoint_y),
                    BigPoint_LAYOUT, VH_BigPoint_x, VH_BigPoint_y  };
        };
        return new Object[][]{
                argsFact.apply(winVaListFactory,          Win64.C_LONG_LONG,   sumStructJavaFact),
                argsFact.apply(sysvVaListFactory,         SysV.C_LONG_LONG,    sumStructJavaFact),
                argsFact.apply(linuxAArch64VaListFactory, AArch64.C_LONG_LONG, sumStructJavaFact),
                argsFact.apply(macAArch64VaListFactory,   AArch64.C_LONG_LONG, sumStructJavaFact),
                argsFact.apply(platformVaListFactory,     C_LONG_LONG,         sumStructNativeFact),
        };
    }

    @Test(dataProvider = "bigStructs")
    public void testBigStruct(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                              Function<VaList, Long> sumBigStruct,
                              GroupLayout BigPoint_LAYOUT, VarHandle VH_BigPoint_x, VarHandle VH_BigPoint_y) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment struct = MemorySegment.allocateNative(BigPoint_LAYOUT, scope);
            VH_BigPoint_x.set(struct, 5);
            VH_BigPoint_y.set(struct, 10);

            VaList vaList = vaListFactory.apply(b -> b.vargFromSegment(BigPoint_LAYOUT, struct));
            long sum = sumBigStruct.apply(vaList);
            assertEquals(sum, 15);
            vaList.scope().close();
        }
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] floatStructs() {
        TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Float>> sumStructJavaFact
                = (FloatPoint_LAYOUT, VH_FloatPoint_x, VH_FloatPoint_y) ->
                list -> {
                    MemorySegment struct = list.vargAsSegment(FloatPoint_LAYOUT, ResourceScope.newImplicitScope());
                    float x = (float) VH_FloatPoint_x.get(struct);
                    float y = (float) VH_FloatPoint_y.get(struct);
                    return x + y;
                };

        TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Float>> sumStructNativeFact
                = (pointLayout, VH_FloatPoint_x, VH_FloatPoint_y) ->
                MethodHandleProxies.asInterfaceInstance(Function.class, MH_sumFloatStruct);

        TriFunction<Function<Consumer<VaList.Builder>, VaList>, MemoryLayout,
                TriFunction<MemoryLayout, VarHandle, VarHandle, Function<VaList, Float>>, Object[]> argsFact
                = (vaListFact, floatLayout, sumFloatStructFact) -> {
            GroupLayout FloatPoint_LAYOUT = MemoryLayout.structLayout(
                    floatLayout.withName("x"),
                    floatLayout.withName("y")
            );
            VarHandle VH_FloatPoint_x = FloatPoint_LAYOUT.varHandle(float.class, groupElement("x"));
            VarHandle VH_FloatPoint_y = FloatPoint_LAYOUT.varHandle(float.class, groupElement("y"));
            return new Object[] { vaListFact, sumFloatStructFact.apply(FloatPoint_LAYOUT, VH_FloatPoint_x, VH_FloatPoint_y),
                    FloatPoint_LAYOUT, VH_FloatPoint_x, VH_FloatPoint_y  };
        };
        return new Object[][]{
                argsFact.apply(winVaListFactory,          Win64.C_FLOAT,   sumStructJavaFact),
                argsFact.apply(sysvVaListFactory,         SysV.C_FLOAT,    sumStructJavaFact),
                argsFact.apply(linuxAArch64VaListFactory, AArch64.C_FLOAT, sumStructJavaFact),
                argsFact.apply(macAArch64VaListFactory,   AArch64.C_FLOAT, sumStructJavaFact),
                argsFact.apply(platformVaListFactory,     C_FLOAT,         sumStructNativeFact),
        };
    }

    @Test(dataProvider = "floatStructs")
    public void testFloatStruct(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                                Function<VaList, Float> sumFloatStruct,
                                GroupLayout FloatPoint_LAYOUT,
                                VarHandle VH_FloatPoint_x, VarHandle VH_FloatPoint_y) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment struct = MemorySegment.allocateNative(FloatPoint_LAYOUT, scope);
            VH_FloatPoint_x.set(struct, 1.234f);
            VH_FloatPoint_y.set(struct, 3.142f);

            VaList vaList = vaListFactory.apply(b -> b.vargFromSegment(FloatPoint_LAYOUT, struct));
            float sum = sumFloatStruct.apply(vaList);
            assertEquals(sum, 4.376f, 0.00001f);
            vaList.scope().close();
        }
    }

    interface QuadFunc<T0, T1, T2, T3, R> {
        R apply(T0 t0, T1 t1, T2 t2, T3 t3);
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] hugeStructs() {
        QuadFunc<MemoryLayout, VarHandle, VarHandle, VarHandle, Function<VaList, Long>> sumStructJavaFact
                = (HugePoint_LAYOUT, VH_HugePoint_x, VH_HugePoint_y, VH_HugePoint_z) ->
                list -> {
                    MemorySegment struct = list.vargAsSegment(HugePoint_LAYOUT, ResourceScope.newImplicitScope());
                    long x = (long) VH_HugePoint_x.get(struct);
                    long y = (long) VH_HugePoint_y.get(struct);
                    long z = (long) VH_HugePoint_z.get(struct);
                    return x + y + z;
                };

        QuadFunc<MemoryLayout, VarHandle, VarHandle, VarHandle, Function<VaList, Long>> sumStructNativeFact
                = (pointLayout, VH_HugePoint_x, VH_HugePoint_y, VH_HugePoint_z) ->
                MethodHandleProxies.asInterfaceInstance(Function.class, MH_sumHugeStruct);

        TriFunction<Function<Consumer<VaList.Builder>, VaList>, MemoryLayout,
                QuadFunc<MemoryLayout, VarHandle, VarHandle, VarHandle, Function<VaList, Long>>, Object[]> argsFact
                = (vaListFact, longLongLayout, sumBigStructFact) -> {
            GroupLayout HugePoint_LAYOUT = MemoryLayout.structLayout(
                    longLongLayout.withName("x"),
                    longLongLayout.withName("y"),
                    longLongLayout.withName("z")
            );
            VarHandle VH_HugePoint_x = HugePoint_LAYOUT.varHandle(long.class, groupElement("x"));
            VarHandle VH_HugePoint_y = HugePoint_LAYOUT.varHandle(long.class, groupElement("y"));
            VarHandle VH_HugePoint_z = HugePoint_LAYOUT.varHandle(long.class, groupElement("z"));
            return new Object[] { vaListFact,
                    sumBigStructFact.apply(HugePoint_LAYOUT, VH_HugePoint_x, VH_HugePoint_y, VH_HugePoint_z),
                    HugePoint_LAYOUT, VH_HugePoint_x, VH_HugePoint_y, VH_HugePoint_z  };
        };
        return new Object[][]{
                argsFact.apply(winVaListFactory,          Win64.C_LONG_LONG,   sumStructJavaFact),
                argsFact.apply(sysvVaListFactory,         SysV.C_LONG_LONG,    sumStructJavaFact),
                argsFact.apply(linuxAArch64VaListFactory, AArch64.C_LONG_LONG, sumStructJavaFact),
                argsFact.apply(macAArch64VaListFactory,   AArch64.C_LONG_LONG, sumStructJavaFact),
                argsFact.apply(platformVaListFactory,     C_LONG_LONG,         sumStructNativeFact),
        };
    }

    @Test(dataProvider = "hugeStructs")
    public void testHugeStruct(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                               Function<VaList, Long> sumHugeStruct,
                               GroupLayout HugePoint_LAYOUT,
                               VarHandle VH_HugePoint_x, VarHandle VH_HugePoint_y, VarHandle VH_HugePoint_z) {
        // On AArch64 a struct needs to be larger than 16 bytes to be
        // passed by reference.
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment struct = MemorySegment.allocateNative(HugePoint_LAYOUT, scope);
            VH_HugePoint_x.set(struct, 1);
            VH_HugePoint_y.set(struct, 2);
            VH_HugePoint_z.set(struct, 3);

            VaList vaList = vaListFactory.apply(b -> b.vargFromSegment(HugePoint_LAYOUT, struct));
            long sum = sumHugeStruct.apply(vaList);
            assertEquals(sum, 6);
            vaList.scope().close();
        }
    }

    public interface SumStackFunc {
        void invoke(MemorySegment longSum, MemorySegment doubleSum, VaList list);
    }

    @DataProvider
    public static Object[][] sumStack() {
        BiFunction<MemoryLayout, MemoryLayout, SumStackFunc> sumStackJavaFact = (longLayout, doubleLayout) ->
                (longSum, doubleSum, list) -> {
                    long lSum = 0L;
                    for (int i = 0; i < 16; i++) {
                        lSum += list.vargAsLong(longLayout);
                    }
                    MemoryAccess.setLong(longSum, lSum);
                    double dSum = 0D;
                    for (int i = 0; i < 16; i++) {
                        dSum += list.vargAsDouble(doubleLayout);
                    }
                    MemoryAccess.setDouble(doubleSum, dSum);
                };
        SumStackFunc sumStackNative = (longSum, doubleSum, list) -> {
            try {
                MH_sumStack.invokeExact(longSum.address(), doubleSum.address(), list);
            } catch (Throwable ex) {
                throw new AssertionError(ex);
            }
        };
        return new Object[][]{
                { winVaListFactory,           sumStackJavaFact.apply(Win64.C_LONG_LONG, Win64.C_DOUBLE),     Win64.C_LONG_LONG,   Win64.C_DOUBLE   },
                { sysvVaListFactory,          sumStackJavaFact.apply(SysV.C_LONG_LONG, SysV.C_DOUBLE),       SysV.C_LONG_LONG,    SysV.C_DOUBLE    },
                { linuxAArch64VaListFactory,  sumStackJavaFact.apply(AArch64.C_LONG_LONG, AArch64.C_DOUBLE), AArch64.C_LONG_LONG, AArch64.C_DOUBLE },
                { macAArch64VaListFactory,    sumStackJavaFact.apply(AArch64.C_LONG_LONG, AArch64.C_DOUBLE), AArch64.C_LONG_LONG, AArch64.C_DOUBLE },
                { platformVaListFactory,      sumStackNative,                                                C_LONG_LONG,         C_DOUBLE         },
        };
    }

    @Test(dataProvider = "sumStack")
    public void testStack(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                          SumStackFunc sumStack,
                          ValueLayout longLayout,
                          ValueLayout doubleLayout) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment longSum = MemorySegment.allocateNative(longLayout, scope);
            MemorySegment doubleSum = MemorySegment.allocateNative(doubleLayout, scope);
            MemoryAccess.setLong(longSum, 0L);
            MemoryAccess.setDouble(doubleSum, 0D);

            VaList list = vaListFactory.apply(b -> {
                for (long l = 1; l <= 16L; l++) {
                    b.vargFromLong(longLayout, l);
                }
                for (double d = 1; d <= 16D; d++) {
                    b.vargFromDouble(doubleLayout, d);
                }
            });

            try {
                sumStack.invoke(longSum, doubleSum, list);
            } finally {
                list.scope().close();
            }

            long lSum = MemoryAccess.getLong(longSum);
            double dSum = MemoryAccess.getDouble(doubleSum);

            assertEquals(lSum, 136L);
            assertEquals(dSum, 136D);
        }
    }

    @Test(dataProvider = "upcalls")
    public void testUpcall(MethodHandle target, MethodHandle callback) throws Throwable {
        FunctionDescriptor desc = FunctionDescriptor.ofVoid(C_VA_LIST);
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemoryAddress stub = abi.upcallStub(callback, desc, scope);
            target.invokeExact(stub.address());
        }
    }

    @DataProvider
    public Object[][] emptyVaLists() {
        return new Object[][] {
                { Windowsx64Linker.emptyVaList()           },
                { winVaListFactory.apply(b -> {})          },
                { SysVx64Linker.emptyVaList()              },
                { sysvVaListFactory.apply(b -> {})         },
                { LinuxAArch64Linker.emptyVaList()         },
                { linuxAArch64VaListFactory.apply(b -> {}) },
                { MacOsAArch64Linker.emptyVaList()         },
                { macAArch64VaListFactory.apply(b -> {})   },
        };
    }

    @Test(expectedExceptions = UnsupportedOperationException.class,
            expectedExceptionsMessageRegExp = ".*Scope cannot be closed.*",
            dataProvider = "emptyVaLists")
    public void testEmptyNotCloseable(VaList emptyList) {
        emptyList.scope().close();
    }

    @DataProvider
    @SuppressWarnings("unchecked")
    public static Object[][] sumIntsScoped() {
        Function<MemoryLayout, BiFunction<Integer, VaList, Integer>> sumIntsJavaFact = layout ->
                (num, list) -> IntStream.generate(() -> list.vargAsInt(layout)).limit(num).sum();
        BiFunction<Integer, VaList, Integer> sumIntsNative
                = MethodHandleProxies.asInterfaceInstance(BiFunction.class, MH_sumInts);
        return new Object[][]{
                { winVaListScopedFactory,          sumIntsJavaFact.apply(Win64.C_INT),   Win64.C_INT   },
                { sysvVaListScopedFactory,         sumIntsJavaFact.apply(SysV.C_INT),    SysV.C_INT    },
                { linuxAArch64VaListScopedFactory, sumIntsJavaFact.apply(AArch64.C_INT), AArch64.C_INT },
                { macAArch64VaListScopedFactory,   sumIntsJavaFact.apply(AArch64.C_INT), AArch64.C_INT },
                { platformVaListScopedFactory,     sumIntsNative,                        C_INT         },
        };
    }

    @Test(dataProvider = "sumIntsScoped")
    public void testScopedVaList(BiFunction<Consumer<VaList.Builder>, NativeScope, VaList> vaListFactory,
                                 BiFunction<Integer, VaList, Integer> sumInts,
                                 ValueLayout intLayout) {
        VaList listLeaked;
        try (NativeScope scope = new NativeScope()) {
            VaList list = vaListFactory.apply(b -> b.vargFromInt(intLayout, 4)
                            .vargFromInt(intLayout, 8),
                    scope);
            int x = sumInts.apply(2, list);
            assertEquals(x, 12);
            listLeaked = list;
        }
        assertFalse(listLeaked.scope().isAlive());
    }

    @Test(dataProvider = "structs")
    public void testScopeMSRead(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                                Function<VaList, Integer> sumStruct, // ignored
                                GroupLayout Point_LAYOUT, VarHandle VH_Point_x, VarHandle VH_Point_y) {
        MemorySegment pointOut;
        try (NativeScope scope = new NativeScope()) {
            try (ResourceScope innerScope = ResourceScope.newConfinedScope()) {
                MemorySegment pointIn = MemorySegment.allocateNative(Point_LAYOUT, innerScope);
                VH_Point_x.set(pointIn, 3);
                VH_Point_y.set(pointIn, 6);
                VaList list = vaListFactory.apply(b -> b.vargFromSegment(Point_LAYOUT, pointIn));
                pointOut = list.vargAsSegment(Point_LAYOUT, scope);
                assertEquals((int) VH_Point_x.get(pointOut), 3);
                assertEquals((int) VH_Point_y.get(pointOut), 6);
                list.scope().close();
                assertTrue(pointOut.scope().isAlive()); // after VaList freed
            }
            assertTrue(pointOut.scope().isAlive()); // after inner scope freed
        }
        assertFalse(pointOut.scope().isAlive()); // after outer scope freed
    }

    @DataProvider
    public Object[][] copy() {
        return new Object[][] {
                { winVaListFactory,          Win64.C_INT   },
                { sysvVaListFactory,         SysV.C_INT    },
                { linuxAArch64VaListFactory, AArch64.C_INT },
                { macAArch64VaListFactory,   AArch64.C_INT },
        };
    }

    @Test(dataProvider = "copy")
    public void testCopy(Function<Consumer<VaList.Builder>, VaList> vaListFactory, ValueLayout intLayout) {
        VaList list = vaListFactory.apply(b -> b.vargFromInt(intLayout, 4)
                .vargFromInt(intLayout, 8));
        VaList  copy = list.copy();
        assertEquals(copy.vargAsInt(intLayout), 4);
        assertEquals(copy.vargAsInt(intLayout), 8);

//        try { // this logic only works on Windows!
//            int x = copy.vargAsInt(intLayout);
//            fail();
//        } catch (IndexOutOfBoundsException ex) {
//            // ok - we exhausted the list
//        }

        assertEquals(list.vargAsInt(intLayout), 4);
        assertEquals(list.vargAsInt(intLayout), 8);
        list.scope().close();
    }

    @Test(dataProvider = "copy",
            expectedExceptions = IllegalStateException.class)
    public void testCopyUnusableAfterOriginalClosed(Function<Consumer<VaList.Builder>, VaList> vaListFactory,
                                                    ValueLayout intLayout) {
        VaList list = vaListFactory.apply(b -> b.vargFromInt(intLayout, 4)
                .vargFromInt(intLayout, 8));
        VaList copy = list.copy();
        list.scope().close();

        copy.vargAsInt(intLayout); // should throw
    }

    @DataProvider
    public static Object[][] upcalls() {
        GroupLayout BigPoint_LAYOUT = MemoryLayout.structLayout(
                C_LONG_LONG.withName("x"),
                C_LONG_LONG.withName("y")
        );
        VarHandle VH_BigPoint_x = BigPoint_LAYOUT.varHandle(long.class, groupElement("x"));
        VarHandle VH_BigPoint_y = BigPoint_LAYOUT.varHandle(long.class, groupElement("y"));
        GroupLayout Point_LAYOUT = MemoryLayout.structLayout(
                C_INT.withName("x"),
                C_INT.withName("y")
        );
        VarHandle VH_Point_x = Point_LAYOUT.varHandle(int.class, groupElement("x"));
        VarHandle VH_Point_y = Point_LAYOUT.varHandle(int.class, groupElement("y"));
        GroupLayout FloatPoint_LAYOUT = MemoryLayout.structLayout(
                C_FLOAT.withName("x"),
                C_FLOAT.withName("y")
        );
        VarHandle VH_FloatPoint_x = FloatPoint_LAYOUT.varHandle(float.class, groupElement("x"));
        VarHandle VH_FloatPoint_y = FloatPoint_LAYOUT.varHandle(float.class, groupElement("y"));
        GroupLayout HugePoint_LAYOUT = MemoryLayout.structLayout(
                C_LONG_LONG.withName("x"),
                C_LONG_LONG.withName("y"),
                C_LONG_LONG.withName("z")
        );
        VarHandle VH_HugePoint_x = HugePoint_LAYOUT.varHandle(long.class, groupElement("x"));
        VarHandle VH_HugePoint_y = HugePoint_LAYOUT.varHandle(long.class, groupElement("y"));
        VarHandle VH_HugePoint_z = HugePoint_LAYOUT.varHandle(long.class, groupElement("z"));

        return new Object[][]{
                { linkVaListCB("upcallBigStruct"), VaListConsumer.mh(vaList -> {
                    MemorySegment struct = vaList.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_BigPoint_x.get(struct), 8);
                    assertEquals((long) VH_BigPoint_y.get(struct), 16);
                })},
                { linkVaListCB("upcallBigStruct"), VaListConsumer.mh(vaList -> {
                    VaList copy = vaList.copy();
                    MemorySegment struct = vaList.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_BigPoint_x.get(struct), 8);
                    assertEquals((long) VH_BigPoint_y.get(struct), 16);

                    VH_BigPoint_x.set(struct, 0);
                    VH_BigPoint_y.set(struct, 0);

                    // should be independent
                    struct = copy.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_BigPoint_x.get(struct), 8);
                    assertEquals((long) VH_BigPoint_y.get(struct), 16);
                })},
                { linkVaListCB("upcallBigStructPlusScalar"), VaListConsumer.mh(vaList -> {
                    MemorySegment struct = vaList.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_BigPoint_x.get(struct), 8);
                    assertEquals((long) VH_BigPoint_y.get(struct), 16);

                    assertEquals(vaList.vargAsLong(C_LONG_LONG), 42);
                })},
                { linkVaListCB("upcallBigStructPlusScalar"), VaListConsumer.mh(vaList -> {
                    vaList.skip(BigPoint_LAYOUT);
                    assertEquals(vaList.vargAsLong(C_LONG_LONG), 42);
                })},
                { linkVaListCB("upcallStruct"), VaListConsumer.mh(vaList -> {
                    MemorySegment struct = vaList.vargAsSegment(Point_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((int) VH_Point_x.get(struct), 5);
                    assertEquals((int) VH_Point_y.get(struct), 10);
                })},
                { linkVaListCB("upcallHugeStruct"), VaListConsumer.mh(vaList -> {
                    MemorySegment struct = vaList.vargAsSegment(HugePoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_HugePoint_x.get(struct), 1);
                    assertEquals((long) VH_HugePoint_y.get(struct), 2);
                    assertEquals((long) VH_HugePoint_z.get(struct), 3);
                })},
                { linkVaListCB("upcallFloatStruct"), VaListConsumer.mh(vaList -> {
                    MemorySegment struct = vaList.vargAsSegment(FloatPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((float) VH_FloatPoint_x.get(struct), 1.0f);
                    assertEquals((float) VH_FloatPoint_y.get(struct), 2.0f);
                })},
                { linkVaListCB("upcallMemoryAddress"), VaListConsumer.mh(vaList -> {
                    MemoryAddress intPtr = vaList.vargAsAddress(C_POINTER);
                    MemorySegment ms = intPtr.asSegment(C_INT.byteSize(), ResourceScope.globalScope());
                    int x = MemoryAccess.getInt(ms);
                    assertEquals(x, 10);
                })},
                { linkVaListCB("upcallDoubles"), VaListConsumer.mh(vaList -> {
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 3.0);
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 4.0);
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 5.0);
                })},
                { linkVaListCB("upcallInts"), VaListConsumer.mh(vaList -> {
                    assertEquals(vaList.vargAsInt(C_INT), 10);
                    assertEquals(vaList.vargAsInt(C_INT), 15);
                    assertEquals(vaList.vargAsInt(C_INT), 20);
                })},
                { linkVaListCB("upcallStack"), VaListConsumer.mh(vaList -> {
                    // skip all registers
                    for (long l = 1; l <= 16; l++) {
                        assertEquals(vaList.vargAsLong(C_LONG_LONG), l);
                    }
                    for (double d = 1; d <= 16; d++) {
                        assertEquals(vaList.vargAsDouble(C_DOUBLE), d);
                    }

                    // test some arbitrary values on the stack
                    assertEquals((byte) vaList.vargAsInt(C_INT), (byte) 1);
                    assertEquals((char) vaList.vargAsInt(C_INT), 'a');
                    assertEquals((short) vaList.vargAsInt(C_INT), (short) 3);
                    assertEquals(vaList.vargAsInt(C_INT), 4);
                    assertEquals(vaList.vargAsLong(C_LONG_LONG), 5L);
                    assertEquals((float) vaList.vargAsDouble(C_DOUBLE), 6.0F);
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 7.0D);
                    assertEquals((byte) vaList.vargAsInt(C_INT), (byte) 8);
                    assertEquals((char) vaList.vargAsInt(C_INT), 'b');
                    assertEquals((short) vaList.vargAsInt(C_INT), (short) 10);
                    assertEquals(vaList.vargAsInt(C_INT), 11);
                    assertEquals(vaList.vargAsLong(C_LONG_LONG), 12L);
                    assertEquals((float) vaList.vargAsDouble(C_DOUBLE), 13.0F);
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 14.0D);

                    MemorySegment point = vaList.vargAsSegment(Point_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((int) VH_Point_x.get(point), 5);
                    assertEquals((int) VH_Point_y.get(point), 10);

                    VaList copy = vaList.copy();
                    MemorySegment bigPoint = vaList.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_BigPoint_x.get(bigPoint), 15);
                    assertEquals((long) VH_BigPoint_y.get(bigPoint), 20);

                    VH_BigPoint_x.set(bigPoint, 0);
                    VH_BigPoint_y.set(bigPoint, 0);

                    // should be independent
                    MemorySegment struct = copy.vargAsSegment(BigPoint_LAYOUT, ResourceScope.newImplicitScope());
                    assertEquals((long) VH_BigPoint_x.get(struct), 15);
                    assertEquals((long) VH_BigPoint_y.get(struct), 20);
                })},
                // test skip
                { linkVaListCB("upcallStack"), VaListConsumer.mh(vaList -> {
                    vaList.skip(C_LONG_LONG, C_LONG_LONG, C_LONG_LONG, C_LONG_LONG);
                    assertEquals(vaList.vargAsLong(C_LONG_LONG), 5L);
                    vaList.skip(C_LONG_LONG, C_LONG_LONG, C_LONG_LONG, C_LONG_LONG);
                    assertEquals(vaList.vargAsLong(C_LONG_LONG), 10L);
                    vaList.skip(C_LONG_LONG, C_LONG_LONG, C_LONG_LONG, C_LONG_LONG, C_LONG_LONG, C_LONG_LONG);
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 1.0D);
                    vaList.skip(C_DOUBLE, C_DOUBLE, C_DOUBLE, C_DOUBLE);
                    assertEquals(vaList.vargAsDouble(C_DOUBLE), 6.0D);
                })},
        };
    }

    interface VaListConsumer {
        void accept(VaList list);

        static MethodHandle mh(VaListConsumer instance) {
            try {
                return MethodHandles.lookup().findVirtual(VaListConsumer.class, "accept",
                        MethodType.methodType(void.class, VaList.class)).bindTo(instance);
            } catch (ReflectiveOperationException e) {
                throw new InternalError(e);
            }
        }
    }
}
