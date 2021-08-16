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
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @modules java.base/jdk.internal.ref
 *          jdk.incubator.foreign
 * @run testng/othervm
 *     --enable-native-access=ALL-UNNAMED
 *     TestNulls
 */

import jdk.incubator.foreign.*;
import jdk.internal.ref.CleanerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.NoInjection;
import org.testng.annotations.Test;

import java.lang.constant.Constable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;
import static org.testng.Assert.fail;

/**
 * This test makes sure that public API classes (listed in {@link TestNulls#CLASSES}) throws NPEs whenever
 * nulls are provided. The test looks at all the public methods in all the listed classes, and injects
 * values automatically. If an API takes a reference, the test will try to inject nulls. For APIs taking
 * either reference arrays, or collections, the framework will also generate additional <em>replacements</em>
 * (e.g. other than just replacing the array, or collection with null), such as an array or collection
 * with null elements. The test can be customized by adding/removing classes to the {@link #CLASSES} array,
 * by adding/removing default mappings for standard carrier types (see {@link #DEFAULT_VALUES} or by
 * adding/removing custom replacements (see {@link #REPLACEMENT_VALUES}).
 */
public class TestNulls {

    static final Class<?>[] CLASSES = new Class<?>[] {
            MemorySegment.class,
            MemoryAddress.class,
            MemoryLayout.class,
            MemoryLayout.PathElement.class,
            SequenceLayout.class,
            ValueLayout.class,
            GroupLayout.class,
            Addressable.class,
            SymbolLookup.class,
            MemoryAccess.class,
            MemoryLayouts.class,
            MemoryHandles.class,
            CLinker.class,
            CLinker.VaList.class,
            CLinker.VaList.Builder.class,
            FunctionDescriptor.class,
            SegmentAllocator.class,
            ResourceScope.class
    };

    static final Set<String> EXCLUDE_LIST = Set.of(
            "jdk.incubator.foreign.MemoryLayout/withAttribute(java.lang.String,java.lang.constant.Constable)/1/0",
            "jdk.incubator.foreign.MemoryAddress/asSegment(long,java.lang.Runnable,java.lang.Object)/1/0",
            "jdk.incubator.foreign.MemoryAddress/asSegment(long,java.lang.Runnable,java.lang.Object)/2/0",
            "jdk.incubator.foreign.MemoryAddress/asSegment(long,java.lang.Runnable,jdk.incubator.foreign.ResourceScope)/1/0",
            "jdk.incubator.foreign.SequenceLayout/withAttribute(java.lang.String,java.lang.constant.Constable)/1/0",
            "jdk.incubator.foreign.ValueLayout/withAttribute(java.lang.String,java.lang.constant.Constable)/1/0",
            "jdk.incubator.foreign.GroupLayout/withAttribute(java.lang.String,java.lang.constant.Constable)/1/0",
            "jdk.incubator.foreign.MemoryHandles/insertCoordinates(java.lang.invoke.VarHandle,int,java.lang.Object[])/2/1",
            "jdk.incubator.foreign.FunctionDescriptor/withAttribute(java.lang.String,java.lang.constant.Constable)/1/0"
    );

    static final Set<String> OBJECT_METHODS = Stream.of(Object.class.getMethods())
            .map(Method::getName)
            .collect(Collectors.toSet());

    static final Map<Class<?>, Object> DEFAULT_VALUES = new HashMap<>();

    static <Z> void addDefaultMapping(Class<Z> carrier, Z value) {
        DEFAULT_VALUES.put(carrier, value);
    }

    static {
        addDefaultMapping(char.class, (char)0);
        addDefaultMapping(byte.class, (byte)0);
        addDefaultMapping(short.class, (short)0);
        addDefaultMapping(int.class, 0);
        addDefaultMapping(float.class, 0f);
        addDefaultMapping(long.class, 0L);
        addDefaultMapping(double.class, 0d);
        addDefaultMapping(boolean.class, true);
        addDefaultMapping(ByteOrder.class, ByteOrder.nativeOrder());
        addDefaultMapping(Thread.class, Thread.currentThread());
        addDefaultMapping(Cleaner.class, CleanerFactory.cleaner());
        addDefaultMapping(ByteBuffer.class, ByteBuffer.wrap(new byte[10]));
        addDefaultMapping(Path.class, Path.of("nonExistent"));
        addDefaultMapping(FileChannel.MapMode.class, FileChannel.MapMode.PRIVATE);
        addDefaultMapping(UnaryOperator.class, UnaryOperator.identity());
        addDefaultMapping(String.class, "Hello!");
        addDefaultMapping(Constable.class, "Hello!");
        addDefaultMapping(Class.class, String.class);
        addDefaultMapping(Runnable.class, () -> {});
        addDefaultMapping(Object.class, new Object());
        addDefaultMapping(VarHandle.class, MemoryHandles.varHandle(int.class, ByteOrder.nativeOrder()));
        addDefaultMapping(MethodHandle.class, MethodHandles.identity(int.class));
        addDefaultMapping(List.class, List.of());
        addDefaultMapping(Charset.class, Charset.defaultCharset());
        addDefaultMapping(Consumer.class, x -> {});
        addDefaultMapping(MethodType.class, MethodType.methodType(void.class));
        addDefaultMapping(MemoryAddress.class, MemoryAddress.ofLong(1));
        addDefaultMapping(Addressable.class, MemoryAddress.ofLong(1));
        addDefaultMapping(MemoryLayout.class, MemoryLayouts.JAVA_INT);
        addDefaultMapping(ValueLayout.class, MemoryLayouts.JAVA_INT);
        addDefaultMapping(GroupLayout.class, MemoryLayout.structLayout(MemoryLayouts.JAVA_INT));
        addDefaultMapping(SequenceLayout.class, MemoryLayout.sequenceLayout(MemoryLayouts.JAVA_INT));
        addDefaultMapping(MemorySegment.class, MemorySegment.ofArray(new byte[10]));
        addDefaultMapping(FunctionDescriptor.class, FunctionDescriptor.ofVoid());
        addDefaultMapping(CLinker.class, CLinker.getInstance());
        addDefaultMapping(CLinker.VaList.class, VaListHelper.vaList);
        addDefaultMapping(CLinker.VaList.Builder.class, VaListHelper.vaListBuilder);
        addDefaultMapping(ResourceScope.class, ResourceScope.newImplicitScope());
        addDefaultMapping(SegmentAllocator.class, (size, align) -> null);
        addDefaultMapping(Supplier.class, () -> null);
        addDefaultMapping(ResourceScope.Handle.class, ResourceScope.globalScope().acquire());
        addDefaultMapping(ClassLoader.class, TestNulls.class.getClassLoader());
        addDefaultMapping(SymbolLookup.class, CLinker.systemLookup());
    }

    static class VaListHelper {
        static final CLinker.VaList vaList;
        static final CLinker.VaList.Builder vaListBuilder;

        static {
            AtomicReference<CLinker.VaList.Builder> builderRef = new AtomicReference<>();
            vaList = CLinker.VaList.make(b -> {
                builderRef.set(b);
                b.vargFromLong(CLinker.C_LONG_LONG, 42L);
            }, ResourceScope.newImplicitScope());
            vaListBuilder = builderRef.get();
        }
    }

    static final Map<Class<?>, Object[]> REPLACEMENT_VALUES = new HashMap<>();

    @SafeVarargs
    static <Z> void addReplacements(Class<Z> carrier, Z... value) {
        REPLACEMENT_VALUES.put(carrier, value);
    }

    static {
        addReplacements(Collection.class, null, Stream.of(new Object[] { null }).collect(Collectors.toList()));
        addReplacements(List.class, null, Stream.of(new Object[] { null }).collect(Collectors.toList()));
        addReplacements(Set.class, null, Stream.of(new Object[] { null }).collect(Collectors.toSet()));
    }

    @Test(dataProvider = "cases")
    public void testNulls(String testName, @NoInjection Method meth, Object receiver, Object[] args) {
        try {
            meth.invoke(receiver, args);
            fail("Method invocation completed normally");
        } catch (InvocationTargetException ex) {
            Class<?> cause = ex.getCause().getClass();
            assertEquals(cause, NullPointerException.class, "got " + cause.getName() + " - expected NullPointerException");
        } catch (Throwable ex) {
            fail("Unexpected exception: " + ex);
        }
    }

    @DataProvider(name = "cases")
    static Iterator<Object[]> cases() {
        List<Object[]> cases = new ArrayList<>();
        for (Class<?> clazz : CLASSES) {
            for (Method m : clazz.getMethods()) {
                if (OBJECT_METHODS.contains(m.getName())) continue;
                boolean isStatic = (m.getModifiers() & Modifier.STATIC) != 0;
                List<Integer> refIndices = new ArrayList<>();
                for (int i = 0; i < m.getParameterCount(); i++) {
                    Class<?> param = m.getParameterTypes()[i];
                    if (!param.isPrimitive()) {
                        refIndices.add(i);
                    }
                }
                for (int i : refIndices) {
                    Object[] replacements = replacements(m.getParameterTypes()[i]);
                    for (int r = 0 ; r < replacements.length ; r++) {
                        String testName = clazz.getName() + "/" + shortSig(m) + "/" + i + "/" + r;
                        if (EXCLUDE_LIST.contains(testName)) continue;
                        Object[] args = new Object[m.getParameterCount()];
                        for (int j = 0; j < args.length; j++) {
                            args[j] = defaultValue(m.getParameterTypes()[j]);
                        }
                        args[i] = replacements[r];
                        Object receiver = isStatic ? null : defaultValue(clazz);
                        cases.add(new Object[]{testName, m, receiver, args});
                    }
                }
            }
        }
        return cases.iterator();
    };

    static String shortSig(Method m) {
        StringJoiner sj = new StringJoiner(",", m.getName() + "(", ")");
        for (Class<?> parameterType : m.getParameterTypes()) {
            sj.add(parameterType.getTypeName());
        }
        return sj.toString();
    }

    static Object defaultValue(Class<?> carrier) {
        if (carrier.isArray()) {
            return Array.newInstance(carrier.componentType(), 0);
        }
        Object value = DEFAULT_VALUES.get(carrier);
        if (value == null) {
            throw new UnsupportedOperationException(carrier.getName());
        }
        return value;
    }

    static Object[] replacements(Class<?> carrier) {
        if (carrier.isArray() && !carrier.getComponentType().isPrimitive()) {
            Object arr = Array.newInstance(carrier.componentType(), 1);
            Array.set(arr, 0, null);
            return new Object[] { null, arr };
        }
        return REPLACEMENT_VALUES.getOrDefault(carrier, new Object[] { null });
    }
}
