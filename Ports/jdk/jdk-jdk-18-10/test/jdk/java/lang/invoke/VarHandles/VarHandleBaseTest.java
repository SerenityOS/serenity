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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.invoke.WrongMethodTypeException;
import java.lang.reflect.Method;
import java.nio.ReadOnlyBufferException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.testng.Assert.*;

abstract class VarHandleBaseTest {
    static final int ITERS = Integer.getInteger("iters", 1);
    static final int WEAK_ATTEMPTS = Integer.getInteger("weakAttempts", 10);

    interface ThrowingRunnable {
        void run() throws Throwable;
    }

    static void checkUOE(ThrowingRunnable r) {
        checkWithThrowable(UnsupportedOperationException.class, null, r);
    }

    static void checkUOE(Object message, ThrowingRunnable r) {
        checkWithThrowable(UnsupportedOperationException.class, message, r);
    }

    static void checkROBE(ThrowingRunnable r) {
        checkWithThrowable(ReadOnlyBufferException.class, null, r);
    }

    static void checkROBE(Object message, ThrowingRunnable r) {
        checkWithThrowable(ReadOnlyBufferException.class, message, r);
    }

    static void checkIOOBE(ThrowingRunnable r) {
        checkWithThrowable(IndexOutOfBoundsException.class, null, r);
    }

    static void checkIOOBE(Object message, ThrowingRunnable r) {
        checkWithThrowable(IndexOutOfBoundsException.class, message, r);
    }

    static void checkAIOOBE(ThrowingRunnable r) {
        checkWithThrowable(ArrayIndexOutOfBoundsException.class, null, r);
    }

    static void checkAIOOBE(Object message, ThrowingRunnable r) {
        checkWithThrowable(ArrayIndexOutOfBoundsException.class, message, r);
    }

    static void checkASE(ThrowingRunnable r) {
        checkWithThrowable(ArrayStoreException.class, null, r);
    }

    static void checkASE(Object message, ThrowingRunnable r) {
        checkWithThrowable(ArrayStoreException.class, message, r);
    }

    static void checkISE(ThrowingRunnable r) {
        checkWithThrowable(IllegalStateException.class, null, r);
    }

    static void checkISE(Object message, ThrowingRunnable r) {
        checkWithThrowable(IllegalStateException.class, message, r);
    }

    static void checkIAE(ThrowingRunnable r) {
        checkWithThrowable(IllegalAccessException.class, null, r);
    }

    static void checkIAE(Object message, ThrowingRunnable r) {
        checkWithThrowable(IllegalAccessException.class, message, r);
    }

    static void checkWMTE(ThrowingRunnable r) {
        checkWithThrowable(WrongMethodTypeException.class, null, r);
    }

    static void checkWMTE(Object message, ThrowingRunnable r) {
        checkWithThrowable(WrongMethodTypeException.class, message, r);
    }

    static void checkCCE(ThrowingRunnable r) {
        checkWithThrowable(ClassCastException.class, null, r);
    }

    static void checkCCE(Object message, ThrowingRunnable r) {
        checkWithThrowable(ClassCastException.class, message, r);
    }

    static void checkNPE(ThrowingRunnable r) {
        checkWithThrowable(NullPointerException.class, null, r);
    }

    static void checkNPE(Object message, ThrowingRunnable r) {
        checkWithThrowable(NullPointerException.class, message, r);
    }

    static void checkWithThrowable(Class<? extends Throwable> re,
                                   Object message,
                                   ThrowingRunnable r) {
        Throwable _e = null;
        try {
            r.run();
        }
        catch (Throwable e) {
            _e = e;
        }
        message = message == null ? "" : message + ". ";
        assertNotNull(_e, String.format("%sNo throwable thrown. Expected %s", message, re));
        assertTrue(re.isInstance(_e), String.format("%sIncorrect throwable thrown, %s. Expected %s", message, _e, re));
    }


    enum TestAccessType {
        GET,
        SET,
        COMPARE_AND_SET,
        COMPARE_AND_EXCHANGE,
        GET_AND_SET,
        GET_AND_ADD,
        GET_AND_BITWISE;
    }

    enum TestAccessMode {
        GET(TestAccessType.GET),
        SET(TestAccessType.SET),
        GET_VOLATILE(TestAccessType.GET),
        SET_VOLATILE(TestAccessType.SET),
        GET_ACQUIRE(TestAccessType.GET),
        SET_RELEASE(TestAccessType.SET),
        GET_OPAQUE(TestAccessType.GET),
        SET_OPAQUE(TestAccessType.SET),
        COMPARE_AND_SET(TestAccessType.COMPARE_AND_SET),
        COMPARE_AND_EXCHANGE(TestAccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_ACQUIRE(TestAccessType.COMPARE_AND_EXCHANGE),
        COMPARE_AND_EXCHANGE_RELEASE(TestAccessType.COMPARE_AND_EXCHANGE),
        WEAK_COMPARE_AND_SET_PLAIN(TestAccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET(TestAccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET_ACQUIRE(TestAccessType.COMPARE_AND_SET),
        WEAK_COMPARE_AND_SET_RELEASE(TestAccessType.COMPARE_AND_SET),
        GET_AND_SET(TestAccessType.GET_AND_SET),
        GET_AND_SET_ACQUIRE(TestAccessType.GET_AND_SET),
        GET_AND_SET_RELEASE(TestAccessType.GET_AND_SET),
        GET_AND_ADD(TestAccessType.GET_AND_ADD),
        GET_AND_ADD_ACQUIRE(TestAccessType.GET_AND_ADD),
        GET_AND_ADD_RELEASE(TestAccessType.GET_AND_ADD),
        GET_AND_BITWISE_OR(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_OR_ACQUIRE(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_OR_RELEASE(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_AND(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_AND_ACQUIRE(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_AND_RELEASE(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_XOR(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_XOR_ACQUIRE(TestAccessType.GET_AND_BITWISE),
        GET_AND_BITWISE_XOR_RELEASE(TestAccessType.GET_AND_BITWISE),
        ;

        final TestAccessType at;
        final boolean isPolyMorphicInReturnType;
        final Class<?> returnType;

        TestAccessMode(TestAccessType at) {
            this.at = at;

            try {
                VarHandle.AccessMode vh_am = toAccessMode();
                Method m = VarHandle.class.getMethod(vh_am.methodName(), Object[].class);
                this.returnType = m.getReturnType();
                isPolyMorphicInReturnType = returnType != Object.class;
            }
            catch (Exception e) {
                throw new Error(e);
            }
        }

        boolean isOfType(TestAccessType at) {
            return this.at == at;
        }

        VarHandle.AccessMode toAccessMode() {
            return VarHandle.AccessMode.valueOf(name());
        }
    }

    static List<TestAccessMode> testAccessModes() {
        return Stream.of(TestAccessMode.values()).collect(toList());
    }

    static List<TestAccessMode> testAccessModesOfType(TestAccessType... ats) {
        Stream<TestAccessMode> s = Stream.of(TestAccessMode.values());
        return s.filter(e -> Stream.of(ats).anyMatch(e::isOfType))
                .collect(toList());
    }

    static List<VarHandle.AccessMode> accessModes() {
        return Stream.of(VarHandle.AccessMode.values()).collect(toList());
    }

    static List<VarHandle.AccessMode> accessModesOfType(TestAccessType... ats) {
        Stream<TestAccessMode> s = Stream.of(TestAccessMode.values());
        return s.filter(e -> Stream.of(ats).anyMatch(e::isOfType))
                .map(TestAccessMode::toAccessMode)
                .collect(toList());
    }

    static MethodHandle toMethodHandle(VarHandle vh, TestAccessMode tam, MethodType mt) {
        return vh.toMethodHandle(tam.toAccessMode());
    }

    static MethodHandle findVirtual(VarHandle vh, TestAccessMode tam, MethodType mt) {
        MethodHandle mh;
        try {
            mh = MethodHandles.publicLookup().
                    findVirtual(VarHandle.class,
                                tam.toAccessMode().methodName(),
                                mt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return bind(vh, mh, mt);
    }

    static MethodHandle varHandleInvoker(VarHandle vh, TestAccessMode tam, MethodType mt) {
        MethodHandle mh = MethodHandles.varHandleInvoker(
                tam.toAccessMode(),
                mt);

        return bind(vh, mh, mt);
    }

    static MethodHandle varHandleExactInvoker(VarHandle vh, TestAccessMode tam, MethodType mt) {
        MethodHandle mh = MethodHandles.varHandleExactInvoker(
                tam.toAccessMode(),
                mt);

        return bind(vh, mh, mt);
    }

    private static MethodHandle bind(VarHandle vh, MethodHandle mh, MethodType emt) {
        assertEquals(mh.type(), emt.insertParameterTypes(0, VarHandle.class),
                     "MethodHandle type differs from access mode type");

        MethodHandleInfo info = MethodHandles.lookup().revealDirect(mh);
        assertEquals(info.getMethodType(), emt,
                     "MethodHandleInfo method type differs from access mode type");

        return mh.bindTo(vh);
    }

    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    enum VarHandleToMethodHandle {
        VAR_HANDLE_TO_METHOD_HANDLE(
                "VarHandle.toMethodHandle",
                true,
                VarHandleBaseTest::toMethodHandle),
        METHOD_HANDLES_LOOKUP_FIND_VIRTUAL(
                "Lookup.findVirtual",
                false,
                VarHandleBaseTest::findVirtual),
        METHOD_HANDLES_VAR_HANDLE_INVOKER(
                "MethodHandles.varHandleInvoker",
                false,
                VarHandleBaseTest::varHandleInvoker),
        METHOD_HANDLES_VAR_HANDLE_EXACT_INVOKER(
                "MethodHandles.varHandleExactInvoker",
                true,
                VarHandleBaseTest::varHandleExactInvoker);

        final String desc;
        final boolean isExact;
        final TriFunction<VarHandle, TestAccessMode, MethodType, MethodHandle> f;

        VarHandleToMethodHandle(String desc, boolean isExact,
                                TriFunction<VarHandle, TestAccessMode, MethodType, MethodHandle> f) {
            this.desc = desc;
            this.f = f;
            this.isExact = isExact;
        }

        MethodHandle apply(VarHandle vh, TestAccessMode am, MethodType mt) {
            return f.apply(vh, am, mt);
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    static class Handles {
        static class AccessModeAndType {
            final TestAccessMode tam;
            final MethodType t;

            public AccessModeAndType(TestAccessMode tam, MethodType t) {
                this.tam = tam;
                this.t = t;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                AccessModeAndType x = (AccessModeAndType) o;

                if (tam != x.tam) return false;
                if (t != null ? !t.equals(x.t) : x.t != null) return false;

                return true;
            }

            @Override
            public int hashCode() {
                int result = tam != null ? tam.hashCode() : 0;
                result = 31 * result + (t != null ? t.hashCode() : 0);
                return result;
            }
        }

        final VarHandle vh;
        final VarHandleToMethodHandle f;
        final EnumMap<TestAccessMode, MethodType> amToType;
        final Map<AccessModeAndType, MethodHandle> amToHandle;

        Handles(VarHandle vh, VarHandleToMethodHandle f) throws Exception {
            this.vh = vh;
            this.f = f;
            this.amToHandle = new HashMap<>();

            amToType = new EnumMap<>(TestAccessMode.class);
            for (TestAccessMode am : testAccessModes()) {
                amToType.put(am, vh.accessModeType(am.toAccessMode()));
            }
        }

        MethodHandle get(TestAccessMode am) {
            return get(am, amToType.get(am));
        }

        MethodHandle get(TestAccessMode am, MethodType mt) {
            AccessModeAndType amt = new AccessModeAndType(am, mt);
            return amToHandle.computeIfAbsent(
                    amt, k -> f.apply(vh, am, mt));
        }

        Class<? extends Throwable> getWMTEOOrOther(Class<? extends Throwable> c) {
            return f.isExact ? WrongMethodTypeException.class : c;
        }

        void checkWMTEOrCCE(ThrowingRunnable r) {
            checkWithThrowable(getWMTEOOrOther(ClassCastException.class), null, r);
        }

    }

    interface AccessTestAction<T> {
        void action(T t) throws Throwable;
    }

    static abstract class AccessTestCase<T> {
        final String desc;
        final AccessTestAction<T> ata;
        final boolean loop;

        AccessTestCase(String desc, AccessTestAction<T> ata, boolean loop) {
            this.desc = desc;
            this.ata = ata;
            this.loop = loop;
        }

        boolean requiresLoop() {
            return loop;
        }

        abstract T get() throws Exception;

        void testAccess(T t) throws Throwable {
            ata.action(t);
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    static class VarHandleAccessTestCase extends AccessTestCase<VarHandle> {
        final VarHandle vh;

        VarHandleAccessTestCase(String desc, VarHandle vh, AccessTestAction<VarHandle> ata) {
            this(desc, vh, ata, true);
        }

        VarHandleAccessTestCase(String desc, VarHandle vh, AccessTestAction<VarHandle> ata, boolean loop) {
            super("VarHandle -> " + desc, ata, loop);
            this.vh = vh;
        }

        @Override
        VarHandle get() {
            return vh;
        }
    }

    static class MethodHandleAccessTestCase extends AccessTestCase<Handles> {
        final VarHandle vh;
        final VarHandleToMethodHandle f;

        MethodHandleAccessTestCase(String desc, VarHandle vh, VarHandleToMethodHandle f, AccessTestAction<Handles> ata) {
            this(desc, vh, f, ata, true);
        }

        MethodHandleAccessTestCase(String desc, VarHandle vh, VarHandleToMethodHandle f, AccessTestAction<Handles> ata, boolean loop) {
            super("VarHandle -> " + f.toString() + " -> " + desc, ata, loop);
            this.vh = vh;
            this.f = f;
        }

        @Override
        Handles get() throws Exception {
            return new Handles(vh, f);
        }
    }

    static void testTypes(VarHandle vh) {
        List<Class<?>> pts = vh.coordinateTypes();

        for (TestAccessMode accessMode : testAccessModes()) {
            MethodType amt = vh.accessModeType(accessMode.toAccessMode());

            assertEquals(amt.parameterList().subList(0, pts.size()), pts);
        }

        for (TestAccessMode testAccessMode : testAccessModesOfType(TestAccessType.GET)) {
            MethodType mt = vh.accessModeType(testAccessMode.toAccessMode());
            assertEquals(mt.returnType(), vh.varType());
            assertEquals(mt.parameterList(), pts);
        }

        for (TestAccessMode testAccessMode : testAccessModesOfType(TestAccessType.SET)) {
            MethodType mt = vh.accessModeType(testAccessMode.toAccessMode());
            assertEquals(mt.returnType(), void.class);
            assertEquals(mt.parameterType(mt.parameterCount() - 1), vh.varType());
        }

        for (TestAccessMode testAccessMode : testAccessModesOfType(TestAccessType.COMPARE_AND_SET)) {
            MethodType mt = vh.accessModeType(testAccessMode.toAccessMode());
            assertEquals(mt.returnType(), boolean.class);
            assertEquals(mt.parameterType(mt.parameterCount() - 1), vh.varType());
            assertEquals(mt.parameterType(mt.parameterCount() - 2), vh.varType());
        }

        for (TestAccessMode testAccessMode : testAccessModesOfType(TestAccessType.COMPARE_AND_EXCHANGE)) {
            MethodType mt = vh.accessModeType(testAccessMode.toAccessMode());
            assertEquals(mt.returnType(), vh.varType());
            assertEquals(mt.parameterType(mt.parameterCount() - 1), vh.varType());
            assertEquals(mt.parameterType(mt.parameterCount() - 2), vh.varType());
        }

        for (TestAccessMode testAccessMode : testAccessModesOfType(TestAccessType.GET_AND_SET, TestAccessType.GET_AND_ADD)) {
            MethodType mt = vh.accessModeType(testAccessMode.toAccessMode());
            assertEquals(mt.returnType(), vh.varType());
            assertEquals(mt.parameterType(mt.parameterCount() - 1), vh.varType());
        }
    }
}
