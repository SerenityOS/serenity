/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test id=with_SerialGC
 * @requires vm.gc.Serial
 * @bug 8198540
 * @summary Test TypeConverterFactory is not leaking class loaders (Serial GC)
 * @run main/othervm -XX:+UseSerialGC TypeConverterFactoryRetentionTests
 */

/*
 * @test id=with_ParallelGC
 * @requires vm.gc.Parallel
 * @bug 8198540
 * @summary Test TypeConverterFactory is not leaking class loaders (Parallel GC)
 * @run main/othervm -XX:+UseParallelGC TypeConverterFactoryRetentionTests
 */

/*
 * @test id=with_G1GC
 * @requires vm.gc.G1
 * @bug 8198540
 * @summary Test TypeConverterFactory is not leaking class loaders (G1 GC)
 * @run main/othervm -XX:+UseG1GC TypeConverterFactoryRetentionTests
 */

/*
 * @test id=with_ZGC
 * @requires vm.gc.Z
 * @bug 8198540
 * @summary Test TypeConverterFactory is not leaking class loaders (Z GC)
 * @run main/othervm -XX:+UseZGC TypeConverterFactoryRetentionTests
 */

/*
 * @test id=with_ShenandoahGC
 * @requires vm.gc.Shenandoah
 * @bug 8198540
 * @summary Test TypeConverterFactory is not leaking class loaders (Shenandoah GC)
 * @run main/othervm -XX:+UseShenandoahGC TypeConverterFactoryRetentionTests
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.GuardingTypeConverterFactory;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * Tests that jdk.dynalink.TypeConverterFactory doesn't prevent class loaders
 * from getting garbage collected.
 */
public class TypeConverterFactoryRetentionTests {
    // With explicit GC calls succeeds in 1-2 iterations depending on GC used.
    // 1000 should be a safe upper limit after which we can consider it failed.
    private static final int MAX_ITERATIONS = 1000;

    private static class TestLinker implements GuardingDynamicLinker, GuardingTypeConverterFactory {
        public GuardedInvocation getGuardedInvocation(LinkRequest linkRequest, LinkerServices linkerServices) {
            // We're only testing convertToType
            throw new UnsupportedOperationException();
        }

        public GuardedInvocation convertToType(Class<?> sourceType, Class<?> targetType, Supplier<MethodHandles.Lookup> lookupSupplier) {
            // Never meant to be invoked, just a dummy MH that conforms to the expected type.
            MethodHandle result = MethodHandles.empty(MethodType.methodType(targetType, sourceType));
            return new GuardedInvocation(result);
        }
    }

    private static class TestClassLoader extends ClassLoader {
        private final String name;

        TestClassLoader(ClassLoader parent, String name) {
            super(parent);
            this.name = name;
            if (this.name.length() != 1) {
                throw new IllegalArgumentException();
            }
        }

        @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (this.name.equals(name)) {
                // Base64-encoding of simplest "public final class X { private X() {} }"
                // with Java 8 class format. The single-character name is in position 63.
                byte[] bytes = Base64.getDecoder().decode(
                    "yv66vgAAADQACgoAAgADBwAEDAAFAAYBABBqYXZhL2xhbmcvT2Jq" +
                    "ZWN0AQAGPGluaXQ+AQADKClWBwAIAQABWAEABENvZGUAMQAHAAIA" +
                    "AAAAAAEAAgAFAAYAAQAJAAAAEQABAAEAAAAFKrcAAbEAAAAAAAA=");
                assert bytes[63] == 'X';
                // rename
                bytes[63] = (byte)name.charAt(0);
                return defineClass(name, bytes, 0, bytes.length);
            }
            throw new ClassNotFoundException();
        }
    }

    public static void main(String[] args) throws Exception {
        testSystemLoaderToOtherLoader();
        testParentToChildLoader();
        testUnrelatedLoaders();
    }

    private static final LinkerServices createLinkerServices() {
        DynamicLinkerFactory f = new DynamicLinkerFactory();
        f.setFallbackLinkers();
        f.setPrioritizedLinker(new TestLinker());
        return f.createLinker().getLinkerServices();
    }

    /**
     * Creates converters between a system class and user-loaded classes,
     * tests that the user-loaded classes and their loader can be GCd
     * (system classes won't pin them.)
     */
    private static void testSystemLoaderToOtherLoader() throws ClassNotFoundException {
        testFromOneClassToClassLoader(double.class);
    }

    /**
     * Creates converters between a user-loaded classes belonging to class
     * loaders in parent-child relationship, tests that the classes loaded by
     * child loaders can be GCd (parent won't pin them.)
     */
    private static void testParentToChildLoader() throws ClassNotFoundException {
        TestClassLoader parent = new TestClassLoader(null, "Y");
        Class<?> y = Class.forName("Y", true, parent);

        testFromOneClassToClassLoader(y);
    }

    private static void testFromOneClassToClassLoader(Class<?> y) throws ClassNotFoundException {
        ReferenceQueue<ClassLoader> refQueue = new ReferenceQueue<>();
        List<PhantomReference<ClassLoader>> refs = new ArrayList<>();

        LinkerServices linkerServices = createLinkerServices();

        for (int count = 0; count < MAX_ITERATIONS; count++) {
            TestClassLoader cl = new TestClassLoader(y.getClassLoader(), "X");
            Class<?> x = Class.forName("X", true, cl);
            assert x.getClassLoader() == cl;
            linkerServices.getTypeConverter(y, x);
            linkerServices.getTypeConverter(x, y);
            refs.add(new PhantomReference<>(cl, refQueue));
            System.gc();
            if (refQueue.poll() != null) {
                return;
            }
        }
        // No class loaders became phantom reachable before deadline.
        throw new AssertionError("Should have GCd a class loader by now");
    }

    /**
     * Creates converters between a user-loaded classes belonging to unrelated
     * class loaders, tests that the classes and the loaders can be GCd
     * (neither side will pin the other side.)
     */
    private static void testUnrelatedLoaders() throws ClassNotFoundException {
        ReferenceQueue<ClassLoader> refQueue1 = new ReferenceQueue<>();
        ReferenceQueue<ClassLoader> refQueue2 = new ReferenceQueue<>();
        List<PhantomReference<ClassLoader>> refs = new ArrayList<>();
        boolean gc1 = false;
        boolean gc2 = false;

        LinkerServices linkerServices = createLinkerServices();

        for (int count = 0; count < MAX_ITERATIONS; count++) {
            TestClassLoader cl1 = new TestClassLoader(null, "X");
            Class<?> x = Class.forName("X", true, cl1);
            assert x.getClassLoader() == cl1;
            TestClassLoader cl2 = new TestClassLoader(null, "Y");
            Class<?> y = Class.forName("Y", true, cl2);
            assert y.getClassLoader() == cl2;
            linkerServices.getTypeConverter(y, x);
            linkerServices.getTypeConverter(x, y);
            refs.add(new PhantomReference<>(cl1, refQueue1));
            refs.add(new PhantomReference<>(cl2, refQueue2));
            System.gc();
            if (refQueue1.poll() != null) {
                gc1 = true;
            }
            if (refQueue2.poll() != null) {
                gc2 = true;
            }
            if (gc1 && gc2) {
                return;
            }
        }
        // No class loaders from both sides became phantom reachable before deadline.
        throw new AssertionError("Should have GCd a class loader from both queues by now");
    }
}
