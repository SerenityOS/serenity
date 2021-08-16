/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package java.lang.invoke;

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.invoke.NativeEntryPoint;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Hidden;
import jdk.internal.vm.annotation.Stable;
import sun.invoke.empty.Empty;
import sun.invoke.util.ValueConversions;
import sun.invoke.util.VerifyType;
import sun.invoke.util.Wrapper;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.invoke.LambdaForm.*;
import static java.lang.invoke.MethodHandleStatics.*;
import static java.lang.invoke.MethodHandles.Lookup.IMPL_LOOKUP;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

/**
 * Trusted implementation code for MethodHandle.
 * @author jrose
 */
/*non-public*/
abstract class MethodHandleImpl {

    /// Factory methods to create method handles:

    static MethodHandle makeArrayElementAccessor(Class<?> arrayClass, ArrayAccess access) {
        if (arrayClass == Object[].class) {
            return ArrayAccess.objectAccessor(access);
        }
        if (!arrayClass.isArray())
            throw newIllegalArgumentException("not an array: "+arrayClass);
        MethodHandle[] cache = ArrayAccessor.TYPED_ACCESSORS.get(arrayClass);
        int cacheIndex = ArrayAccess.cacheIndex(access);
        MethodHandle mh = cache[cacheIndex];
        if (mh != null)  return mh;
        mh = ArrayAccessor.getAccessor(arrayClass, access);
        MethodType correctType = ArrayAccessor.correctType(arrayClass, access);
        if (mh.type() != correctType) {
            assert(mh.type().parameterType(0) == Object[].class);
            /* if access == SET */ assert(access != ArrayAccess.SET || mh.type().parameterType(2) == Object.class);
            /* if access == GET */ assert(access != ArrayAccess.GET ||
                    (mh.type().returnType() == Object.class &&
                     correctType.parameterType(0).getComponentType() == correctType.returnType()));
            // safe to view non-strictly, because element type follows from array type
            mh = mh.viewAsType(correctType, false);
        }
        mh = makeIntrinsic(mh, ArrayAccess.intrinsic(access));
        // Atomically update accessor cache.
        synchronized(cache) {
            if (cache[cacheIndex] == null) {
                cache[cacheIndex] = mh;
            } else {
                // Throw away newly constructed accessor and use cached version.
                mh = cache[cacheIndex];
            }
        }
        return mh;
    }

    enum ArrayAccess {
        GET, SET, LENGTH;

        // As ArrayAccess and ArrayAccessor have a circular dependency, the ArrayAccess properties cannot be stored in
        // final fields.

        static String opName(ArrayAccess a) {
            return switch (a) {
                case GET    -> "getElement";
                case SET    -> "setElement";
                case LENGTH -> "length";
                default -> throw unmatchedArrayAccess(a);
            };
        }

        static MethodHandle objectAccessor(ArrayAccess a) {
            return switch (a) {
                case GET    -> ArrayAccessor.OBJECT_ARRAY_GETTER;
                case SET    -> ArrayAccessor.OBJECT_ARRAY_SETTER;
                case LENGTH -> ArrayAccessor.OBJECT_ARRAY_LENGTH;
                default -> throw unmatchedArrayAccess(a);
            };
        }

        static int cacheIndex(ArrayAccess a) {
            return switch (a) {
                case GET    -> ArrayAccessor.GETTER_INDEX;
                case SET    -> ArrayAccessor.SETTER_INDEX;
                case LENGTH -> ArrayAccessor.LENGTH_INDEX;
                default -> throw unmatchedArrayAccess(a);
            };
        }

        static Intrinsic intrinsic(ArrayAccess a) {
            return switch (a) {
                case GET    -> Intrinsic.ARRAY_LOAD;
                case SET    -> Intrinsic.ARRAY_STORE;
                case LENGTH -> Intrinsic.ARRAY_LENGTH;
                default -> throw unmatchedArrayAccess(a);
            };
        }
    }

    static InternalError unmatchedArrayAccess(ArrayAccess a) {
        return newInternalError("should not reach here (unmatched ArrayAccess: " + a + ")");
    }

    static final class ArrayAccessor {
        /// Support for array element and length access
        static final int GETTER_INDEX = 0, SETTER_INDEX = 1, LENGTH_INDEX = 2, INDEX_LIMIT = 3;
        static final ClassValue<MethodHandle[]> TYPED_ACCESSORS
                = new ClassValue<MethodHandle[]>() {
                    @Override
                    protected MethodHandle[] computeValue(Class<?> type) {
                        return new MethodHandle[INDEX_LIMIT];
                    }
                };
        static final MethodHandle OBJECT_ARRAY_GETTER, OBJECT_ARRAY_SETTER, OBJECT_ARRAY_LENGTH;
        static {
            MethodHandle[] cache = TYPED_ACCESSORS.get(Object[].class);
            cache[GETTER_INDEX] = OBJECT_ARRAY_GETTER = makeIntrinsic(getAccessor(Object[].class, ArrayAccess.GET),    Intrinsic.ARRAY_LOAD);
            cache[SETTER_INDEX] = OBJECT_ARRAY_SETTER = makeIntrinsic(getAccessor(Object[].class, ArrayAccess.SET),    Intrinsic.ARRAY_STORE);
            cache[LENGTH_INDEX] = OBJECT_ARRAY_LENGTH = makeIntrinsic(getAccessor(Object[].class, ArrayAccess.LENGTH), Intrinsic.ARRAY_LENGTH);

            assert(InvokerBytecodeGenerator.isStaticallyInvocable(ArrayAccessor.OBJECT_ARRAY_GETTER.internalMemberName()));
            assert(InvokerBytecodeGenerator.isStaticallyInvocable(ArrayAccessor.OBJECT_ARRAY_SETTER.internalMemberName()));
            assert(InvokerBytecodeGenerator.isStaticallyInvocable(ArrayAccessor.OBJECT_ARRAY_LENGTH.internalMemberName()));
        }

        static int     getElementI(int[]     a, int i)            { return              a[i]; }
        static long    getElementJ(long[]    a, int i)            { return              a[i]; }
        static float   getElementF(float[]   a, int i)            { return              a[i]; }
        static double  getElementD(double[]  a, int i)            { return              a[i]; }
        static boolean getElementZ(boolean[] a, int i)            { return              a[i]; }
        static byte    getElementB(byte[]    a, int i)            { return              a[i]; }
        static short   getElementS(short[]   a, int i)            { return              a[i]; }
        static char    getElementC(char[]    a, int i)            { return              a[i]; }
        static Object  getElementL(Object[]  a, int i)            { return              a[i]; }

        static void    setElementI(int[]     a, int i, int     x) {              a[i] = x; }
        static void    setElementJ(long[]    a, int i, long    x) {              a[i] = x; }
        static void    setElementF(float[]   a, int i, float   x) {              a[i] = x; }
        static void    setElementD(double[]  a, int i, double  x) {              a[i] = x; }
        static void    setElementZ(boolean[] a, int i, boolean x) {              a[i] = x; }
        static void    setElementB(byte[]    a, int i, byte    x) {              a[i] = x; }
        static void    setElementS(short[]   a, int i, short   x) {              a[i] = x; }
        static void    setElementC(char[]    a, int i, char    x) {              a[i] = x; }
        static void    setElementL(Object[]  a, int i, Object  x) {              a[i] = x; }

        static int     lengthI(int[]     a)                       { return a.length; }
        static int     lengthJ(long[]    a)                       { return a.length; }
        static int     lengthF(float[]   a)                       { return a.length; }
        static int     lengthD(double[]  a)                       { return a.length; }
        static int     lengthZ(boolean[] a)                       { return a.length; }
        static int     lengthB(byte[]    a)                       { return a.length; }
        static int     lengthS(short[]   a)                       { return a.length; }
        static int     lengthC(char[]    a)                       { return a.length; }
        static int     lengthL(Object[]  a)                       { return a.length; }

        static String name(Class<?> arrayClass, ArrayAccess access) {
            Class<?> elemClass = arrayClass.getComponentType();
            if (elemClass == null)  throw newIllegalArgumentException("not an array", arrayClass);
            return ArrayAccess.opName(access) + Wrapper.basicTypeChar(elemClass);
        }
        static MethodType type(Class<?> arrayClass, ArrayAccess access) {
            Class<?> elemClass = arrayClass.getComponentType();
            Class<?> arrayArgClass = arrayClass;
            if (!elemClass.isPrimitive()) {
                arrayArgClass = Object[].class;
                elemClass = Object.class;
            }
            return switch (access) {
                case GET    -> MethodType.methodType(elemClass, arrayArgClass, int.class);
                case SET    -> MethodType.methodType(void.class, arrayArgClass, int.class, elemClass);
                case LENGTH -> MethodType.methodType(int.class, arrayArgClass);
                default -> throw unmatchedArrayAccess(access);
            };
        }
        static MethodType correctType(Class<?> arrayClass, ArrayAccess access) {
            Class<?> elemClass = arrayClass.getComponentType();
            return switch (access) {
                case GET    -> MethodType.methodType(elemClass, arrayClass, int.class);
                case SET    -> MethodType.methodType(void.class, arrayClass, int.class, elemClass);
                case LENGTH -> MethodType.methodType(int.class, arrayClass);
                default -> throw unmatchedArrayAccess(access);
            };
        }
        static MethodHandle getAccessor(Class<?> arrayClass, ArrayAccess access) {
            String     name = name(arrayClass, access);
            MethodType type = type(arrayClass, access);
            try {
                return IMPL_LOOKUP.findStatic(ArrayAccessor.class, name, type);
            } catch (ReflectiveOperationException ex) {
                throw uncaughtException(ex);
            }
        }
    }

    /**
     * Create a JVM-level adapter method handle to conform the given method
     * handle to the similar newType, using only pairwise argument conversions.
     * For each argument, convert incoming argument to the exact type needed.
     * The argument conversions allowed are casting, boxing and unboxing,
     * integral widening or narrowing, and floating point widening or narrowing.
     * @param srcType required call type
     * @param target original method handle
     * @param strict if true, only asType conversions are allowed; if false, explicitCastArguments conversions allowed
     * @param monobox if true, unboxing conversions are assumed to be exactly typed (Integer to int only, not long or double)
     * @return an adapter to the original handle with the desired new type,
     *          or the original target if the types are already identical
     *          or null if the adaptation cannot be made
     */
    static MethodHandle makePairwiseConvert(MethodHandle target, MethodType srcType,
                                            boolean strict, boolean monobox) {
        MethodType dstType = target.type();
        if (srcType == dstType)
            return target;
        return makePairwiseConvertByEditor(target, srcType, strict, monobox);
    }

    private static int countNonNull(Object[] array) {
        int count = 0;
        if (array != null) {
            for (Object x : array) {
                if (x != null) ++count;
            }
        }
        return count;
    }

    static MethodHandle makePairwiseConvertByEditor(MethodHandle target, MethodType srcType,
                                                    boolean strict, boolean monobox) {
        // In method types arguments start at index 0, while the LF
        // editor have the MH receiver at position 0 - adjust appropriately.
        final int MH_RECEIVER_OFFSET = 1;
        Object[] convSpecs = computeValueConversions(srcType, target.type(), strict, monobox);
        int convCount = countNonNull(convSpecs);
        if (convCount == 0)
            return target.viewAsType(srcType, strict);
        MethodType basicSrcType = srcType.basicType();
        MethodType midType = target.type().basicType();
        BoundMethodHandle mh = target.rebind();

        // Match each unique conversion to the positions at which it is to be applied
        var convSpecMap = new HashMap<Object, int[]>(((4 * convCount) / 3) + 1);
        for (int i = 0; i < convSpecs.length - MH_RECEIVER_OFFSET; i++) {
            Object convSpec = convSpecs[i];
            if (convSpec == null) continue;
            int[] positions = convSpecMap.get(convSpec);
            if (positions == null) {
                positions = new int[] { i + MH_RECEIVER_OFFSET };
            } else {
                positions = Arrays.copyOf(positions, positions.length + 1);
                positions[positions.length - 1] = i + MH_RECEIVER_OFFSET;
            }
            convSpecMap.put(convSpec, positions);
        }
        for (var entry : convSpecMap.entrySet()) {
            Object convSpec = entry.getKey();

            MethodHandle fn;
            if (convSpec instanceof Class) {
                fn = getConstantHandle(MH_cast).bindTo(convSpec);
            } else {
                fn = (MethodHandle) convSpec;
            }
            int[] positions = entry.getValue();
            Class<?> newType = basicSrcType.parameterType(positions[0] - MH_RECEIVER_OFFSET);
            BasicType newBasicType = BasicType.basicType(newType);
            convCount -= positions.length;
            if (convCount == 0) {
                midType = srcType;
            } else {
                Class<?>[] ptypes = midType.ptypes().clone();
                for (int pos : positions) {
                    ptypes[pos - 1] = newType;
                }
                midType = MethodType.makeImpl(midType.rtype(), ptypes, true);
            }
            LambdaForm form2;
            if (positions.length > 1) {
                form2 = mh.editor().filterRepeatedArgumentForm(newBasicType, positions);
            } else {
                form2 = mh.editor().filterArgumentForm(positions[0], newBasicType);
            }
            mh = mh.copyWithExtendL(midType, form2, fn);
        }
        Object convSpec = convSpecs[convSpecs.length - 1];
        if (convSpec != null) {
            MethodHandle fn;
            if (convSpec instanceof Class) {
                if (convSpec == void.class)
                    fn = null;
                else
                    fn = getConstantHandle(MH_cast).bindTo(convSpec);
            } else {
                fn = (MethodHandle) convSpec;
            }
            Class<?> newType = basicSrcType.returnType();
            assert(--convCount == 0);
            midType = srcType;
            if (fn != null) {
                mh = mh.rebind();  // rebind if too complex
                LambdaForm form2 = mh.editor().filterReturnForm(BasicType.basicType(newType), false);
                mh = mh.copyWithExtendL(midType, form2, fn);
            } else {
                LambdaForm form2 = mh.editor().filterReturnForm(BasicType.basicType(newType), true);
                mh = mh.copyWith(midType, form2);
            }
        }
        assert(convCount == 0);
        assert(mh.type().equals(srcType));
        return mh;
    }

    static Object[] computeValueConversions(MethodType srcType, MethodType dstType,
                                            boolean strict, boolean monobox) {
        final int INARG_COUNT = srcType.parameterCount();
        Object[] convSpecs = null;
        for (int i = 0; i <= INARG_COUNT; i++) {
            boolean isRet = (i == INARG_COUNT);
            Class<?> src = isRet ? dstType.returnType() : srcType.parameterType(i);
            Class<?> dst = isRet ? srcType.returnType() : dstType.parameterType(i);
            if (!VerifyType.isNullConversion(src, dst, /*keepInterfaces=*/ strict)) {
                if (convSpecs == null) {
                    convSpecs = new Object[INARG_COUNT + 1];
                }
                convSpecs[i] = valueConversion(src, dst, strict, monobox);
            }
        }
        return convSpecs;
    }
    static MethodHandle makePairwiseConvert(MethodHandle target, MethodType srcType,
                                            boolean strict) {
        return makePairwiseConvert(target, srcType, strict, /*monobox=*/ false);
    }

    /**
     * Find a conversion function from the given source to the given destination.
     * This conversion function will be used as a LF NamedFunction.
     * Return a Class object if a simple cast is needed.
     * Return void.class if void is involved.
     */
    static Object valueConversion(Class<?> src, Class<?> dst, boolean strict, boolean monobox) {
        assert(!VerifyType.isNullConversion(src, dst, /*keepInterfaces=*/ strict));  // caller responsibility
        if (dst == void.class)
            return dst;
        MethodHandle fn;
        if (src.isPrimitive()) {
            if (src == void.class) {
                return void.class;  // caller must recognize this specially
            } else if (dst.isPrimitive()) {
                // Examples: int->byte, byte->int, boolean->int (!strict)
                fn = ValueConversions.convertPrimitive(src, dst);
            } else {
                // Examples: int->Integer, boolean->Object, float->Number
                Wrapper wsrc = Wrapper.forPrimitiveType(src);
                fn = ValueConversions.boxExact(wsrc);
                assert(fn.type().parameterType(0) == wsrc.primitiveType());
                assert(fn.type().returnType() == wsrc.wrapperType());
                if (!VerifyType.isNullConversion(wsrc.wrapperType(), dst, strict)) {
                    // Corner case, such as int->Long, which will probably fail.
                    MethodType mt = MethodType.methodType(dst, src);
                    if (strict)
                        fn = fn.asType(mt);
                    else
                        fn = MethodHandleImpl.makePairwiseConvert(fn, mt, /*strict=*/ false);
                }
            }
        } else if (dst.isPrimitive()) {
            Wrapper wdst = Wrapper.forPrimitiveType(dst);
            if (monobox || src == wdst.wrapperType()) {
                // Use a strongly-typed unboxer, if possible.
                fn = ValueConversions.unboxExact(wdst, strict);
            } else {
                // Examples:  Object->int, Number->int, Comparable->int, Byte->int
                // must include additional conversions
                // src must be examined at runtime, to detect Byte, Character, etc.
                fn = (strict
                        ? ValueConversions.unboxWiden(wdst)
                        : ValueConversions.unboxCast(wdst));
            }
        } else {
            // Simple reference conversion.
            // Note:  Do not check for a class hierarchy relation
            // between src and dst.  In all cases a 'null' argument
            // will pass the cast conversion.
            return dst;
        }
        assert(fn.type().parameterCount() <= 1) : "pc"+Arrays.asList(src.getSimpleName(), dst.getSimpleName(), fn);
        return fn;
    }

    static MethodHandle makeVarargsCollector(MethodHandle target, Class<?> arrayType) {
        MethodType type = target.type();
        int last = type.parameterCount() - 1;
        if (type.parameterType(last) != arrayType)
            target = target.asType(type.changeParameterType(last, arrayType));
        target = target.asFixedArity();  // make sure this attribute is turned off
        return new AsVarargsCollector(target, arrayType);
    }

    private static final class AsVarargsCollector extends DelegatingMethodHandle {
        private final MethodHandle target;
        private final Class<?> arrayType;
        private @Stable MethodHandle asCollectorCache;

        AsVarargsCollector(MethodHandle target, Class<?> arrayType) {
            this(target.type(), target, arrayType);
        }
        AsVarargsCollector(MethodType type, MethodHandle target, Class<?> arrayType) {
            super(type, target);
            this.target = target;
            this.arrayType = arrayType;
        }

        @Override
        public boolean isVarargsCollector() {
            return true;
        }

        @Override
        protected MethodHandle getTarget() {
            return target;
        }

        @Override
        public MethodHandle asFixedArity() {
            return target;
        }

        @Override
        MethodHandle setVarargs(MemberName member) {
            if (member.isVarargs())  return this;
            return asFixedArity();
        }

        @Override
        public MethodHandle withVarargs(boolean makeVarargs) {
            if (makeVarargs)  return this;
            return asFixedArity();
        }

        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            MethodType type = this.type();
            int collectArg = type.parameterCount() - 1;
            int newArity = newType.parameterCount();
            if (newArity == collectArg+1 &&
                type.parameterType(collectArg).isAssignableFrom(newType.parameterType(collectArg))) {
                // if arity and trailing parameter are compatible, do normal thing
                return asTypeCache = asFixedArity().asType(newType);
            }
            // check cache
            MethodHandle acc = asCollectorCache;
            if (acc != null && acc.type().parameterCount() == newArity)
                return asTypeCache = acc.asType(newType);
            // build and cache a collector
            int arrayLength = newArity - collectArg;
            MethodHandle collector;
            try {
                collector = asFixedArity().asCollector(arrayType, arrayLength);
                assert(collector.type().parameterCount() == newArity) : "newArity="+newArity+" but collector="+collector;
            } catch (IllegalArgumentException ex) {
                throw new WrongMethodTypeException("cannot build collector", ex);
            }
            asCollectorCache = collector;
            return asTypeCache = collector.asType(newType);
        }

        @Override
        boolean viewAsTypeChecks(MethodType newType, boolean strict) {
            super.viewAsTypeChecks(newType, true);
            if (strict) return true;
            // extra assertion for non-strict checks:
            assert (type().lastParameterType().getComponentType()
                    .isAssignableFrom(
                            newType.lastParameterType().getComponentType()))
                    : Arrays.asList(this, newType);
            return true;
        }

        @Override
        public Object invokeWithArguments(Object... arguments) throws Throwable {
            MethodType type = this.type();
            int argc;
            final int MAX_SAFE = 127;  // 127 longs require 254 slots, which is safe to spread
            if (arguments == null
                    || (argc = arguments.length) <= MAX_SAFE
                    || argc < type.parameterCount()) {
                return super.invokeWithArguments(arguments);
            }

            // a jumbo invocation requires more explicit reboxing of the trailing arguments
            int uncollected = type.parameterCount() - 1;
            Class<?> elemType = arrayType.getComponentType();
            int collected = argc - uncollected;
            Object collArgs = (elemType == Object.class)
                ? new Object[collected] : Array.newInstance(elemType, collected);
            if (!elemType.isPrimitive()) {
                // simple cast:  just do some casting
                try {
                    System.arraycopy(arguments, uncollected, collArgs, 0, collected);
                } catch (ArrayStoreException ex) {
                    return super.invokeWithArguments(arguments);
                }
            } else {
                // corner case of flat array requires reflection (or specialized copy loop)
                MethodHandle arraySetter = MethodHandles.arrayElementSetter(arrayType);
                try {
                    for (int i = 0; i < collected; i++) {
                        arraySetter.invoke(collArgs, i, arguments[uncollected + i]);
                    }
                } catch (WrongMethodTypeException|ClassCastException ex) {
                    return super.invokeWithArguments(arguments);
                }
            }

            // chop the jumbo list down to size and call in non-varargs mode
            Object[] newArgs = new Object[uncollected + 1];
            System.arraycopy(arguments, 0, newArgs, 0, uncollected);
            newArgs[uncollected] = collArgs;
            return asFixedArity().invokeWithArguments(newArgs);
        }
    }

    static void checkSpreadArgument(Object av, int n) {
        if (av == null && n == 0) {
            return;
        } else if (av == null) {
            throw new NullPointerException("null array reference");
        } else if (av instanceof Object[]) {
            int len = ((Object[])av).length;
            if (len == n)  return;
        } else {
            int len = java.lang.reflect.Array.getLength(av);
            if (len == n)  return;
        }
        // fall through to error:
        throw newIllegalArgumentException("array is not of length "+n);
    }

    @Hidden
    static MethodHandle selectAlternative(boolean testResult, MethodHandle target, MethodHandle fallback) {
        if (testResult) {
            return target;
        } else {
            return fallback;
        }
    }

    // Intrinsified by C2. Counters are used during parsing to calculate branch frequencies.
    @Hidden
    @jdk.internal.vm.annotation.IntrinsicCandidate
    static boolean profileBoolean(boolean result, int[] counters) {
        // Profile is int[2] where [0] and [1] correspond to false and true occurrences respectively.
        int idx = result ? 1 : 0;
        try {
            counters[idx] = Math.addExact(counters[idx], 1);
        } catch (ArithmeticException e) {
            // Avoid continuous overflow by halving the problematic count.
            counters[idx] = counters[idx] / 2;
        }
        return result;
    }

    // Intrinsified by C2. Returns true if obj is a compile-time constant.
    @Hidden
    @jdk.internal.vm.annotation.IntrinsicCandidate
    static boolean isCompileConstant(Object obj) {
        return false;
    }

    static MethodHandle makeGuardWithTest(MethodHandle test,
                                   MethodHandle target,
                                   MethodHandle fallback) {
        MethodType type = target.type();
        assert(test.type().equals(type.changeReturnType(boolean.class)) && fallback.type().equals(type));
        MethodType basicType = type.basicType();
        LambdaForm form = makeGuardWithTestForm(basicType);
        BoundMethodHandle mh;
        try {
            if (PROFILE_GWT) {
                int[] counts = new int[2];
                mh = (BoundMethodHandle)
                        BoundMethodHandle.speciesData_LLLL().factory().invokeBasic(type, form,
                                (Object) test, (Object) profile(target), (Object) profile(fallback), counts);
            } else {
                mh = (BoundMethodHandle)
                        BoundMethodHandle.speciesData_LLL().factory().invokeBasic(type, form,
                                (Object) test, (Object) profile(target), (Object) profile(fallback));
            }
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }


    static MethodHandle profile(MethodHandle target) {
        if (DONT_INLINE_THRESHOLD >= 0) {
            return makeBlockInliningWrapper(target);
        } else {
            return target;
        }
    }

    /**
     * Block inlining during JIT-compilation of a target method handle if it hasn't been invoked enough times.
     * Corresponding LambdaForm has @DontInline when compiled into bytecode.
     */
    static MethodHandle makeBlockInliningWrapper(MethodHandle target) {
        LambdaForm lform;
        if (DONT_INLINE_THRESHOLD > 0) {
            lform = Makers.PRODUCE_BLOCK_INLINING_FORM.apply(target);
        } else {
            lform = Makers.PRODUCE_REINVOKER_FORM.apply(target);
        }
        return new CountingWrapper(target, lform,
                Makers.PRODUCE_BLOCK_INLINING_FORM, Makers.PRODUCE_REINVOKER_FORM,
                                   DONT_INLINE_THRESHOLD);
    }

    private static final class Makers {
        /** Constructs reinvoker lambda form which block inlining during JIT-compilation for a particular method handle */
        static final Function<MethodHandle, LambdaForm> PRODUCE_BLOCK_INLINING_FORM = new Function<MethodHandle, LambdaForm>() {
            @Override
            public LambdaForm apply(MethodHandle target) {
                return DelegatingMethodHandle.makeReinvokerForm(target,
                                   MethodTypeForm.LF_DELEGATE_BLOCK_INLINING, CountingWrapper.class, false,
                                   DelegatingMethodHandle.NF_getTarget, CountingWrapper.NF_maybeStopCounting);
            }
        };

        /** Constructs simple reinvoker lambda form for a particular method handle */
        static final Function<MethodHandle, LambdaForm> PRODUCE_REINVOKER_FORM = new Function<MethodHandle, LambdaForm>() {
            @Override
            public LambdaForm apply(MethodHandle target) {
                return DelegatingMethodHandle.makeReinvokerForm(target,
                        MethodTypeForm.LF_DELEGATE, DelegatingMethodHandle.class, DelegatingMethodHandle.NF_getTarget);
            }
        };

        /** Maker of type-polymorphic varargs */
        static final ClassValue<MethodHandle[]> TYPED_COLLECTORS = new ClassValue<MethodHandle[]>() {
            @Override
            protected MethodHandle[] computeValue(Class<?> type) {
                return new MethodHandle[MAX_JVM_ARITY + 1];
            }
        };
    }

    /**
     * Counting method handle. It has 2 states: counting and non-counting.
     * It is in counting state for the first n invocations and then transitions to non-counting state.
     * Behavior in counting and non-counting states is determined by lambda forms produced by
     * countingFormProducer & nonCountingFormProducer respectively.
     */
    static class CountingWrapper extends DelegatingMethodHandle {
        private final MethodHandle target;
        private int count;
        private Function<MethodHandle, LambdaForm> countingFormProducer;
        private Function<MethodHandle, LambdaForm> nonCountingFormProducer;
        private volatile boolean isCounting;

        private CountingWrapper(MethodHandle target, LambdaForm lform,
                                Function<MethodHandle, LambdaForm> countingFromProducer,
                                Function<MethodHandle, LambdaForm> nonCountingFormProducer,
                                int count) {
            super(target.type(), lform);
            this.target = target;
            this.count = count;
            this.countingFormProducer = countingFromProducer;
            this.nonCountingFormProducer = nonCountingFormProducer;
            this.isCounting = (count > 0);
        }

        @Hidden
        @Override
        protected MethodHandle getTarget() {
            return target;
        }

        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            MethodHandle newTarget = target.asType(newType);
            MethodHandle wrapper;
            if (isCounting) {
                LambdaForm lform;
                lform = countingFormProducer.apply(newTarget);
                wrapper = new CountingWrapper(newTarget, lform, countingFormProducer, nonCountingFormProducer, DONT_INLINE_THRESHOLD);
            } else {
                wrapper = newTarget; // no need for a counting wrapper anymore
            }
            return (asTypeCache = wrapper);
        }

        boolean countDown() {
            int c = count;
            target.maybeCustomize(); // customize if counting happens for too long
            if (c <= 1) {
                // Try to limit number of updates. MethodHandle.updateForm() doesn't guarantee LF update visibility.
                if (isCounting) {
                    isCounting = false;
                    return true;
                } else {
                    return false;
                }
            } else {
                count = c - 1;
                return false;
            }
        }

        @Hidden
        static void maybeStopCounting(Object o1) {
             final CountingWrapper wrapper = (CountingWrapper) o1;
             if (wrapper.countDown()) {
                 // Reached invocation threshold. Replace counting behavior with a non-counting one.
                 wrapper.updateForm(new Function<>() {
                     public LambdaForm apply(LambdaForm oldForm) {
                         LambdaForm lform = wrapper.nonCountingFormProducer.apply(wrapper.target);
                         lform.compileToBytecode(); // speed up warmup by avoiding LF interpretation again after transition
                         return lform;
                     }});
             }
        }

        static final NamedFunction NF_maybeStopCounting;
        static {
            Class<?> THIS_CLASS = CountingWrapper.class;
            try {
                NF_maybeStopCounting = new NamedFunction(THIS_CLASS.getDeclaredMethod("maybeStopCounting", Object.class));
            } catch (ReflectiveOperationException ex) {
                throw newInternalError(ex);
            }
        }
    }

    static LambdaForm makeGuardWithTestForm(MethodType basicType) {
        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_GWT);
        if (lform != null)  return lform;
        final int THIS_MH      = 0;  // the BMH_LLL
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + basicType.parameterCount();
        int nameCursor = ARG_LIMIT;
        final int GET_TEST     = nameCursor++;
        final int GET_TARGET   = nameCursor++;
        final int GET_FALLBACK = nameCursor++;
        final int GET_COUNTERS = PROFILE_GWT ? nameCursor++ : -1;
        final int CALL_TEST    = nameCursor++;
        final int PROFILE      = (GET_COUNTERS != -1) ? nameCursor++ : -1;
        final int TEST         = nameCursor-1; // previous statement: either PROFILE or CALL_TEST
        final int SELECT_ALT   = nameCursor++;
        final int CALL_TARGET  = nameCursor++;
        assert(CALL_TARGET == SELECT_ALT+1);  // must be true to trigger IBG.emitSelectAlternative

        MethodType lambdaType = basicType.invokerType();
        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data =
                (GET_COUNTERS != -1) ? BoundMethodHandle.speciesData_LLLL()
                                     : BoundMethodHandle.speciesData_LLL();
        names[THIS_MH] = names[THIS_MH].withConstraint(data);
        names[GET_TEST]     = new Name(data.getterFunction(0), names[THIS_MH]);
        names[GET_TARGET]   = new Name(data.getterFunction(1), names[THIS_MH]);
        names[GET_FALLBACK] = new Name(data.getterFunction(2), names[THIS_MH]);
        if (GET_COUNTERS != -1) {
            names[GET_COUNTERS] = new Name(data.getterFunction(3), names[THIS_MH]);
        }
        Object[] invokeArgs = Arrays.copyOfRange(names, 0, ARG_LIMIT, Object[].class);

        // call test
        MethodType testType = basicType.changeReturnType(boolean.class).basicType();
        invokeArgs[0] = names[GET_TEST];
        names[CALL_TEST] = new Name(testType, invokeArgs);

        // profile branch
        if (PROFILE != -1) {
            names[PROFILE] = new Name(getFunction(NF_profileBoolean), names[CALL_TEST], names[GET_COUNTERS]);
        }
        // call selectAlternative
        names[SELECT_ALT] = new Name(new NamedFunction(
                makeIntrinsic(getConstantHandle(MH_selectAlternative), Intrinsic.SELECT_ALTERNATIVE)),
                names[TEST], names[GET_TARGET], names[GET_FALLBACK]);

        // call target or fallback
        invokeArgs[0] = names[SELECT_ALT];
        names[CALL_TARGET] = new Name(basicType, invokeArgs);

        lform = new LambdaForm(lambdaType.parameterCount(), names, /*forceInline=*/true, Kind.GUARD);

        return basicType.form().setCachedLambdaForm(MethodTypeForm.LF_GWT, lform);
    }

    /**
     * The LambdaForm shape for catchException combinator is the following:
     * <blockquote><pre>{@code
     *  guardWithCatch=Lambda(a0:L,a1:L,a2:L)=>{
     *    t3:L=BoundMethodHandle$Species_LLLLL.argL0(a0:L);
     *    t4:L=BoundMethodHandle$Species_LLLLL.argL1(a0:L);
     *    t5:L=BoundMethodHandle$Species_LLLLL.argL2(a0:L);
     *    t6:L=BoundMethodHandle$Species_LLLLL.argL3(a0:L);
     *    t7:L=BoundMethodHandle$Species_LLLLL.argL4(a0:L);
     *    t8:L=MethodHandle.invokeBasic(t6:L,a1:L,a2:L);
     *    t9:L=MethodHandleImpl.guardWithCatch(t3:L,t4:L,t5:L,t8:L);
     *   t10:I=MethodHandle.invokeBasic(t7:L,t9:L);t10:I}
     * }</pre></blockquote>
     *
     * argL0 and argL2 are target and catcher method handles. argL1 is exception class.
     * argL3 and argL4 are auxiliary method handles: argL3 boxes arguments and wraps them into Object[]
     * (ValueConversions.array()) and argL4 unboxes result if necessary (ValueConversions.unbox()).
     *
     * Having t8 and t10 passed outside and not hardcoded into a lambda form allows to share lambda forms
     * among catchException combinators with the same basic type.
     */
    private static LambdaForm makeGuardWithCatchForm(MethodType basicType) {
        MethodType lambdaType = basicType.invokerType();

        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_GWC);
        if (lform != null) {
            return lform;
        }
        final int THIS_MH      = 0;  // the BMH_LLLLL
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + basicType.parameterCount();

        int nameCursor = ARG_LIMIT;
        final int GET_TARGET       = nameCursor++;
        final int GET_CLASS        = nameCursor++;
        final int GET_CATCHER      = nameCursor++;
        final int GET_COLLECT_ARGS = nameCursor++;
        final int GET_UNBOX_RESULT = nameCursor++;
        final int BOXED_ARGS       = nameCursor++;
        final int TRY_CATCH        = nameCursor++;
        final int UNBOX_RESULT     = nameCursor++;

        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLLL();
        names[THIS_MH]          = names[THIS_MH].withConstraint(data);
        names[GET_TARGET]       = new Name(data.getterFunction(0), names[THIS_MH]);
        names[GET_CLASS]        = new Name(data.getterFunction(1), names[THIS_MH]);
        names[GET_CATCHER]      = new Name(data.getterFunction(2), names[THIS_MH]);
        names[GET_COLLECT_ARGS] = new Name(data.getterFunction(3), names[THIS_MH]);
        names[GET_UNBOX_RESULT] = new Name(data.getterFunction(4), names[THIS_MH]);

        // FIXME: rework argument boxing/result unboxing logic for LF interpretation

        // t_{i}:L=MethodHandle.invokeBasic(collectArgs:L,a1:L,...);
        MethodType collectArgsType = basicType.changeReturnType(Object.class);
        MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
        Object[] args = new Object[invokeBasic.type().parameterCount()];
        args[0] = names[GET_COLLECT_ARGS];
        System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT-ARG_BASE);
        names[BOXED_ARGS] = new Name(new NamedFunction(makeIntrinsic(invokeBasic, Intrinsic.GUARD_WITH_CATCH)), args);

        // t_{i+1}:L=MethodHandleImpl.guardWithCatch(target:L,exType:L,catcher:L,t_{i}:L);
        Object[] gwcArgs = new Object[] {names[GET_TARGET], names[GET_CLASS], names[GET_CATCHER], names[BOXED_ARGS]};
        names[TRY_CATCH] = new Name(getFunction(NF_guardWithCatch), gwcArgs);

        // t_{i+2}:I=MethodHandle.invokeBasic(unbox:L,t_{i+1}:L);
        MethodHandle invokeBasicUnbox = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
        Object[] unboxArgs  = new Object[] {names[GET_UNBOX_RESULT], names[TRY_CATCH]};
        names[UNBOX_RESULT] = new Name(invokeBasicUnbox, unboxArgs);

        lform = new LambdaForm(lambdaType.parameterCount(), names, Kind.GUARD_WITH_CATCH);

        return basicType.form().setCachedLambdaForm(MethodTypeForm.LF_GWC, lform);
    }

    static MethodHandle makeGuardWithCatch(MethodHandle target,
                                    Class<? extends Throwable> exType,
                                    MethodHandle catcher) {
        MethodType type = target.type();
        LambdaForm form = makeGuardWithCatchForm(type.basicType());

        // Prepare auxiliary method handles used during LambdaForm interpretation.
        // Box arguments and wrap them into Object[]: ValueConversions.array().
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);
        MethodHandle unboxResult = unboxResultHandle(type.returnType());

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLLL();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.factory().invokeBasic(type, form, (Object) target, (Object) exType,
                    (Object) catcher, (Object) collectArgs, (Object) unboxResult);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    /**
     * Intrinsified during LambdaForm compilation
     * (see {@link InvokerBytecodeGenerator#emitGuardWithCatch emitGuardWithCatch}).
     */
    @Hidden
    static Object guardWithCatch(MethodHandle target, Class<? extends Throwable> exType, MethodHandle catcher,
                                 Object... av) throws Throwable {
        // Use asFixedArity() to avoid unnecessary boxing of last argument for VarargsCollector case.
        try {
            return target.asFixedArity().invokeWithArguments(av);
        } catch (Throwable t) {
            if (!exType.isInstance(t)) throw t;
            return catcher.asFixedArity().invokeWithArguments(prepend(av, t));
        }
    }

    /** Prepend elements to an array. */
    @Hidden
    private static Object[] prepend(Object[] array, Object... elems) {
        int nArray = array.length;
        int nElems = elems.length;
        Object[] newArray = new Object[nArray + nElems];
        System.arraycopy(elems, 0, newArray, 0, nElems);
        System.arraycopy(array, 0, newArray, nElems, nArray);
        return newArray;
    }

    static MethodHandle throwException(MethodType type) {
        assert(Throwable.class.isAssignableFrom(type.parameterType(0)));
        int arity = type.parameterCount();
        if (arity > 1) {
            MethodHandle mh = throwException(type.dropParameterTypes(1, arity));
            mh = MethodHandles.dropArguments(mh, 1, Arrays.copyOfRange(type.parameterArray(), 1, arity));
            return mh;
        }
        return makePairwiseConvert(getFunction(NF_throwException).resolvedHandle(), type, false, true);
    }

    static <T extends Throwable> Empty throwException(T t) throws T { throw t; }

    static MethodHandle[] FAKE_METHOD_HANDLE_INVOKE = new MethodHandle[2];
    static MethodHandle fakeMethodHandleInvoke(MemberName method) {
        assert(method.isMethodHandleInvoke());
        int idx = switch (method.getName()) {
            case "invoke"      -> 0;
            case "invokeExact" -> 1;
            default -> throw new InternalError(method.getName());
        };
        MethodHandle mh = FAKE_METHOD_HANDLE_INVOKE[idx];
        if (mh != null)  return mh;
        MethodType type = MethodType.methodType(Object.class, UnsupportedOperationException.class,
                                                MethodHandle.class, Object[].class);
        mh = throwException(type);
        mh = mh.bindTo(new UnsupportedOperationException("cannot reflectively invoke MethodHandle"));
        if (!method.getInvocationType().equals(mh.type()))
            throw new InternalError(method.toString());
        mh = mh.withInternalMemberName(method, false);
        mh = mh.withVarargs(true);
        assert(method.isVarargs());
        FAKE_METHOD_HANDLE_INVOKE[idx] = mh;
        return mh;
    }
    static MethodHandle fakeVarHandleInvoke(MemberName method) {
        // TODO caching, is it necessary?
        MethodType type = MethodType.methodType(method.getReturnType(), UnsupportedOperationException.class,
                                                VarHandle.class, Object[].class);
        MethodHandle mh = throwException(type);
        mh = mh.bindTo(new UnsupportedOperationException("cannot reflectively invoke VarHandle"));
        if (!method.getInvocationType().equals(mh.type()))
            throw new InternalError(method.toString());
        mh = mh.withInternalMemberName(method, false);
        mh = mh.asVarargsCollector(Object[].class);
        assert(method.isVarargs());
        return mh;
    }

    /**
     * Create an alias for the method handle which, when called,
     * appears to be called from the same class loader and protection domain
     * as hostClass.
     * This is an expensive no-op unless the method which is called
     * is sensitive to its caller.  A small number of system methods
     * are in this category, including Class.forName and Method.invoke.
     */
    static MethodHandle bindCaller(MethodHandle mh, Class<?> hostClass) {
        return BindCaller.bindCaller(mh, hostClass);
    }

    // Put the whole mess into its own nested class.
    // That way we can lazily load the code and set up the constants.
    private static class BindCaller {
        private static MethodType INVOKER_MT = MethodType.methodType(Object.class, MethodHandle.class, Object[].class);

        static MethodHandle bindCaller(MethodHandle mh, Class<?> hostClass) {
            // Code in the boot layer should now be careful while creating method handles or
            // functional interface instances created from method references to @CallerSensitive  methods,
            // it needs to be ensured the handles or interface instances are kept safe and are not passed
            // from the boot layer to untrusted code.
            if (hostClass == null
                ||    (hostClass.isArray() ||
                       hostClass.isPrimitive() ||
                       hostClass.getName().startsWith("java.lang.invoke."))) {
                throw new InternalError();  // does not happen, and should not anyway
            }
            // For simplicity, convert mh to a varargs-like method.
            MethodHandle vamh = prepareForInvoker(mh);
            // Cache the result of makeInjectedInvoker once per argument class.
            MethodHandle bccInvoker = CV_makeInjectedInvoker.get(hostClass);
            return restoreToType(bccInvoker.bindTo(vamh), mh, hostClass);
        }

        private static MethodHandle makeInjectedInvoker(Class<?> targetClass) {
            try {
                /*
                 * The invoker class defined to the same class loader as the lookup class
                 * but in an unnamed package so that the class bytes can be cached and
                 * reused for any @CSM.
                 *
                 * @CSM must be public and exported if called by any module.
                 */
                String name = targetClass.getName() + "$$InjectedInvoker";
                if (targetClass.isHidden()) {
                    // use the original class name
                    name = name.replace('/', '_');
                }
                Class<?> invokerClass = new Lookup(targetClass)
                        .makeHiddenClassDefiner(name, INJECTED_INVOKER_TEMPLATE)
                        .defineClass(true);
                assert checkInjectedInvoker(targetClass, invokerClass);
                return IMPL_LOOKUP.findStatic(invokerClass, "invoke_V", INVOKER_MT);
            } catch (ReflectiveOperationException ex) {
                throw uncaughtException(ex);
            }
        }

        private static ClassValue<MethodHandle> CV_makeInjectedInvoker = new ClassValue<MethodHandle>() {
            @Override protected MethodHandle computeValue(Class<?> hostClass) {
                return makeInjectedInvoker(hostClass);
            }
        };

        // Adapt mh so that it can be called directly from an injected invoker:
        private static MethodHandle prepareForInvoker(MethodHandle mh) {
            mh = mh.asFixedArity();
            MethodType mt = mh.type();
            int arity = mt.parameterCount();
            MethodHandle vamh = mh.asType(mt.generic());
            vamh.internalForm().compileToBytecode();  // eliminate LFI stack frames
            vamh = vamh.asSpreader(Object[].class, arity);
            vamh.internalForm().compileToBytecode();  // eliminate LFI stack frames
            return vamh;
        }

        // Undo the adapter effect of prepareForInvoker:
        private static MethodHandle restoreToType(MethodHandle vamh,
                                                  MethodHandle original,
                                                  Class<?> hostClass) {
            MethodType type = original.type();
            MethodHandle mh = vamh.asCollector(Object[].class, type.parameterCount());
            MemberName member = original.internalMemberName();
            mh = mh.asType(type);
            mh = new WrappedMember(mh, type, member, original.isInvokeSpecial(), hostClass);
            return mh;
        }

        private static boolean checkInjectedInvoker(Class<?> hostClass, Class<?> invokerClass) {
            assert (hostClass.getClassLoader() == invokerClass.getClassLoader()) : hostClass.getName()+" (CL)";
            try {
                assert (hostClass.getProtectionDomain() == invokerClass.getProtectionDomain()) : hostClass.getName()+" (PD)";
            } catch (SecurityException ex) {
                // Self-check was blocked by security manager. This is OK.
            }
            try {
                // Test the invoker to ensure that it really injects into the right place.
                MethodHandle invoker = IMPL_LOOKUP.findStatic(invokerClass, "invoke_V", INVOKER_MT);
                MethodHandle vamh = prepareForInvoker(MH_checkCallerClass);
                return (boolean)invoker.invoke(vamh, new Object[]{ invokerClass });
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }

        private static final MethodHandle MH_checkCallerClass;
        static {
            final Class<?> THIS_CLASS = BindCaller.class;
            assert(checkCallerClass(THIS_CLASS));
            try {
                MH_checkCallerClass = IMPL_LOOKUP
                    .findStatic(THIS_CLASS, "checkCallerClass",
                                MethodType.methodType(boolean.class, Class.class));
                assert((boolean) MH_checkCallerClass.invokeExact(THIS_CLASS));
            } catch (Throwable ex) {
                throw new InternalError(ex);
            }
        }

        @CallerSensitive
        @ForceInline // to ensure Reflection.getCallerClass optimization
        private static boolean checkCallerClass(Class<?> expected) {
            // This method is called via MH_checkCallerClass and so it's correct to ask for the immediate caller here.
            Class<?> actual = Reflection.getCallerClass();
            if (actual != expected)
                throw new InternalError("found " + actual.getName() + ", expected " + expected.getName());
            return true;
        }

        private static final byte[] INJECTED_INVOKER_TEMPLATE = generateInvokerTemplate();

        /** Produces byte code for a class that is used as an injected invoker. */
        private static byte[] generateInvokerTemplate() {
            ClassWriter cw = new ClassWriter(0);

            // private static class InjectedInvoker {
            //     @Hidden
            //     static Object invoke_V(MethodHandle vamh, Object[] args) throws Throwable {
            //        return vamh.invokeExact(args);
            //     }
            // }
            cw.visit(CLASSFILE_VERSION, ACC_PRIVATE | ACC_SUPER, "InjectedInvoker", null, "java/lang/Object", null);

            MethodVisitor mv = cw.visitMethod(ACC_STATIC, "invoke_V",
                          "(Ljava/lang/invoke/MethodHandle;[Ljava/lang/Object;)Ljava/lang/Object;",
                          null, null);

            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact",
                               "([Ljava/lang/Object;)Ljava/lang/Object;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();

            cw.visitEnd();
            return cw.toByteArray();
        }
    }

    /** This subclass allows a wrapped method handle to be re-associated with an arbitrary member name. */
    private static final class WrappedMember extends DelegatingMethodHandle {
        private final MethodHandle target;
        private final MemberName member;
        private final Class<?> callerClass;
        private final boolean isInvokeSpecial;

        private WrappedMember(MethodHandle target, MethodType type,
                              MemberName member, boolean isInvokeSpecial,
                              Class<?> callerClass) {
            super(type, target);
            this.target = target;
            this.member = member;
            this.callerClass = callerClass;
            this.isInvokeSpecial = isInvokeSpecial;
        }

        @Override
        MemberName internalMemberName() {
            return member;
        }
        @Override
        Class<?> internalCallerClass() {
            return callerClass;
        }
        @Override
        boolean isInvokeSpecial() {
            return isInvokeSpecial;
        }
        @Override
        protected MethodHandle getTarget() {
            return target;
        }
        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            // This MH is an alias for target, except for the MemberName
            // Drop the MemberName if there is any conversion.
            return asTypeCache = target.asType(newType);
        }
    }

    static MethodHandle makeWrappedMember(MethodHandle target, MemberName member, boolean isInvokeSpecial) {
        if (member.equals(target.internalMemberName()) && isInvokeSpecial == target.isInvokeSpecial())
            return target;
        return new WrappedMember(target, target.type(), member, isInvokeSpecial, null);
    }

    /** Intrinsic IDs */
    /*non-public*/
    enum Intrinsic {
        SELECT_ALTERNATIVE,
        GUARD_WITH_CATCH,
        TRY_FINALLY,
        TABLE_SWITCH,
        LOOP,
        ARRAY_LOAD,
        ARRAY_STORE,
        ARRAY_LENGTH,
        IDENTITY,
        ZERO,
        NONE // no intrinsic associated
    }

    /** Mark arbitrary method handle as intrinsic.
     * InvokerBytecodeGenerator uses this info to produce more efficient bytecode shape. */
    static final class IntrinsicMethodHandle extends DelegatingMethodHandle {
        private final MethodHandle target;
        private final Intrinsic intrinsicName;
        private final Object intrinsicData;

        IntrinsicMethodHandle(MethodHandle target, Intrinsic intrinsicName) {
           this(target, intrinsicName, null);
        }

        IntrinsicMethodHandle(MethodHandle target, Intrinsic intrinsicName, Object intrinsicData) {
            super(target.type(), target);
            this.target = target;
            this.intrinsicName = intrinsicName;
            this.intrinsicData = intrinsicData;
        }

        @Override
        protected MethodHandle getTarget() {
            return target;
        }

        @Override
        Intrinsic intrinsicName() {
            return intrinsicName;
        }

        @Override
        Object intrinsicData() {
            return intrinsicData;
        }

        @Override
        public MethodHandle asTypeUncached(MethodType newType) {
            // This MH is an alias for target, except for the intrinsic name
            // Drop the name if there is any conversion.
            return asTypeCache = target.asType(newType);
        }

        @Override
        String internalProperties() {
            return super.internalProperties() +
                    "\n& Intrinsic="+intrinsicName;
        }

        @Override
        public MethodHandle asCollector(Class<?> arrayType, int arrayLength) {
            if (intrinsicName == Intrinsic.IDENTITY) {
                MethodType resultType = type().asCollectorType(arrayType, type().parameterCount() - 1, arrayLength);
                MethodHandle newArray = MethodHandleImpl.varargsArray(arrayType, arrayLength);
                return newArray.asType(resultType);
            }
            return super.asCollector(arrayType, arrayLength);
        }
    }

    static MethodHandle makeIntrinsic(MethodHandle target, Intrinsic intrinsicName) {
        return makeIntrinsic(target, intrinsicName, null);
    }

    static MethodHandle makeIntrinsic(MethodHandle target, Intrinsic intrinsicName, Object intrinsicData) {
        if (intrinsicName == target.intrinsicName())
            return target;
        return new IntrinsicMethodHandle(target, intrinsicName, intrinsicData);
    }

    static MethodHandle makeIntrinsic(MethodType type, LambdaForm form, Intrinsic intrinsicName) {
        return new IntrinsicMethodHandle(SimpleMethodHandle.make(type, form), intrinsicName);
    }

    private static final @Stable MethodHandle[] ARRAYS = new MethodHandle[MAX_ARITY + 1];

    /** Return a method handle that takes the indicated number of Object
     *  arguments and returns an Object array of them, as if for varargs.
     */
    static MethodHandle varargsArray(int nargs) {
        MethodHandle mh = ARRAYS[nargs];
        if (mh != null) {
            return mh;
        }
        mh = makeCollector(Object[].class, nargs);
        assert(assertCorrectArity(mh, nargs));
        return ARRAYS[nargs] = mh;
    }

    /** Return a method handle that takes the indicated number of
     *  typed arguments and returns an array of them.
     *  The type argument is the array type.
     */
    static MethodHandle varargsArray(Class<?> arrayType, int nargs) {
        Class<?> elemType = arrayType.getComponentType();
        if (elemType == null)  throw new IllegalArgumentException("not an array: "+arrayType);
        if (nargs >= MAX_JVM_ARITY/2 - 1) {
            int slots = nargs;
            final int MAX_ARRAY_SLOTS = MAX_JVM_ARITY - 1;  // 1 for receiver MH
            if (slots <= MAX_ARRAY_SLOTS && elemType.isPrimitive())
                slots *= Wrapper.forPrimitiveType(elemType).stackSlots();
            if (slots > MAX_ARRAY_SLOTS)
                throw new IllegalArgumentException("too many arguments: "+arrayType.getSimpleName()+", length "+nargs);
        }
        if (elemType == Object.class)
            return varargsArray(nargs);
        // other cases:  primitive arrays, subtypes of Object[]
        MethodHandle cache[] = Makers.TYPED_COLLECTORS.get(elemType);
        MethodHandle mh = nargs < cache.length ? cache[nargs] : null;
        if (mh != null)  return mh;
        mh = makeCollector(arrayType, nargs);
        assert(assertCorrectArity(mh, nargs));
        if (nargs < cache.length)
            cache[nargs] = mh;
        return mh;
    }

    private static boolean assertCorrectArity(MethodHandle mh, int arity) {
        assert(mh.type().parameterCount() == arity) : "arity != "+arity+": "+mh;
        return true;
    }

    static final int MAX_JVM_ARITY = 255;  // limit imposed by the JVM

    /*non-public*/
    static void assertSame(Object mh1, Object mh2) {
        if (mh1 != mh2) {
            String msg = String.format("mh1 != mh2: mh1 = %s (form: %s); mh2 = %s (form: %s)",
                    mh1, ((MethodHandle)mh1).form,
                    mh2, ((MethodHandle)mh2).form);
            throw newInternalError(msg);
        }
    }

    // Local constant functions:

    /* non-public */
    static final byte NF_checkSpreadArgument = 0,
            NF_guardWithCatch = 1,
            NF_throwException = 2,
            NF_tryFinally = 3,
            NF_loop = 4,
            NF_profileBoolean = 5,
            NF_tableSwitch = 6,
            NF_LIMIT = 7;

    private static final @Stable NamedFunction[] NFS = new NamedFunction[NF_LIMIT];

    static NamedFunction getFunction(byte func) {
        NamedFunction nf = NFS[func];
        if (nf != null) {
            return nf;
        }
        return NFS[func] = createFunction(func);
    }

    private static NamedFunction createFunction(byte func) {
        try {
            return switch (func) {
                case NF_checkSpreadArgument -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("checkSpreadArgument", Object.class, int.class));
                case NF_guardWithCatch      -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("guardWithCatch", MethodHandle.class, Class.class,
                                                   MethodHandle.class, Object[].class));
                case NF_tryFinally          -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("tryFinally", MethodHandle.class, MethodHandle.class, Object[].class));
                case NF_loop                -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("loop", BasicType[].class, LoopClauses.class, Object[].class));
                case NF_throwException      -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("throwException", Throwable.class));
                case NF_profileBoolean      -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("profileBoolean", boolean.class, int[].class));
                case NF_tableSwitch         -> new NamedFunction(MethodHandleImpl.class
                                                .getDeclaredMethod("tableSwitch", int.class, MethodHandle.class, CasesHolder.class, Object[].class));
                default -> throw new InternalError("Undefined function: " + func);
            };
        } catch (ReflectiveOperationException ex) {
            throw newInternalError(ex);
        }
    }

    static {
        SharedSecrets.setJavaLangInvokeAccess(new JavaLangInvokeAccess() {
            @Override
            public Object newMemberName() {
                return new MemberName();
            }

            @Override
            public String getName(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.getName();
            }
            @Override
            public Class<?> getDeclaringClass(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.getDeclaringClass();
            }

            @Override
            public MethodType getMethodType(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.getMethodType();
            }

            @Override
            public String getMethodDescriptor(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.getMethodDescriptor();
            }

            @Override
            public boolean isNative(Object mname) {
                MemberName memberName = (MemberName)mname;
                return memberName.isNative();
            }

            @Override
            public Map<String, byte[]> generateHolderClasses(Stream<String> traces) {
                return GenerateJLIClassesHelper.generateHolderClasses(traces);
            }

            @Override
            public void ensureCustomized(MethodHandle mh) {
                mh.customize();
            }

            @Override
            public VarHandle memoryAccessVarHandle(Class<?> carrier, boolean skipAlignmentMaskCheck, long alignmentMask,
                                                   ByteOrder order) {
                return VarHandles.makeMemoryAddressViewHandle(carrier, skipAlignmentMaskCheck, alignmentMask, order);
            }

            @Override
            public MethodHandle nativeMethodHandle(NativeEntryPoint nep, MethodHandle fallback) {
                return NativeMethodHandle.make(nep, fallback);
            }

            @Override
            public VarHandle filterValue(VarHandle target, MethodHandle filterToTarget, MethodHandle filterFromTarget) {
                return VarHandles.filterValue(target, filterToTarget, filterFromTarget);
            }

            @Override
            public VarHandle filterCoordinates(VarHandle target, int pos, MethodHandle... filters) {
                return VarHandles.filterCoordinates(target, pos, filters);
            }

            @Override
            public VarHandle dropCoordinates(VarHandle target, int pos, Class<?>... valueTypes) {
                return VarHandles.dropCoordinates(target, pos, valueTypes);
            }

            @Override
            public VarHandle permuteCoordinates(VarHandle target, List<Class<?>> newCoordinates, int... reorder) {
                return VarHandles.permuteCoordinates(target, newCoordinates, reorder);
            }

            @Override
            public VarHandle collectCoordinates(VarHandle target, int pos, MethodHandle filter) {
                return VarHandles.collectCoordinates(target, pos, filter);
            }

            @Override
            public VarHandle insertCoordinates(VarHandle target, int pos, Object... values) {
                return VarHandles.insertCoordinates(target, pos, values);
            }
        });
    }

    /** Result unboxing: ValueConversions.unbox() OR ValueConversions.identity() OR ValueConversions.ignore(). */
    private static MethodHandle unboxResultHandle(Class<?> returnType) {
        if (returnType.isPrimitive()) {
            if (returnType == void.class) {
                return ValueConversions.ignore();
            } else {
                Wrapper w = Wrapper.forPrimitiveType(returnType);
                return ValueConversions.unboxExact(w);
            }
        } else {
            return MethodHandles.identity(Object.class);
        }
    }

    /**
     * Assembles a loop method handle from the given handles and type information.
     *
     * @param tloop the return type of the loop.
     * @param targs types of the arguments to be passed to the loop.
     * @param init sanitized array of initializers for loop-local variables.
     * @param step sanitited array of loop bodies.
     * @param pred sanitized array of predicates.
     * @param fini sanitized array of loop finalizers.
     *
     * @return a handle that, when invoked, will execute the loop.
     */
    static MethodHandle makeLoop(Class<?> tloop, List<Class<?>> targs, List<MethodHandle> init, List<MethodHandle> step,
                                 List<MethodHandle> pred, List<MethodHandle> fini) {
        MethodType type = MethodType.methodType(tloop, targs);
        BasicType[] initClauseTypes =
                init.stream().map(h -> h.type().returnType()).map(BasicType::basicType).toArray(BasicType[]::new);
        LambdaForm form = makeLoopForm(type.basicType(), initClauseTypes);

        // Prepare auxiliary method handles used during LambdaForm interpretation.
        // Box arguments and wrap them into Object[]: ValueConversions.array().
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);
        MethodHandle unboxResult = unboxResultHandle(tloop);

        LoopClauses clauseData =
                new LoopClauses(new MethodHandle[][]{toArray(init), toArray(step), toArray(pred), toArray(fini)});
        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLL();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.factory().invokeBasic(type, form, (Object) clauseData,
                    (Object) collectArgs, (Object) unboxResult);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    private static MethodHandle[] toArray(List<MethodHandle> l) {
        return l.toArray(new MethodHandle[0]);
    }

    /**
     * Loops introduce some complexity as they can have additional local state. Hence, LambdaForms for loops are
     * generated from a template. The LambdaForm template shape for the loop combinator is as follows (assuming one
     * reference parameter passed in {@code a1}, and a reference return type, with the return value represented by
     * {@code t12}):
     * <blockquote><pre>{@code
     *  loop=Lambda(a0:L,a1:L)=>{
     *    t2:L=BoundMethodHandle$Species_L3.argL0(a0:L);    // LoopClauses holding init, step, pred, fini handles
     *    t3:L=BoundMethodHandle$Species_L3.argL1(a0:L);    // helper handle to box the arguments into an Object[]
     *    t4:L=BoundMethodHandle$Species_L3.argL2(a0:L);    // helper handle to unbox the result
     *    t5:L=MethodHandle.invokeBasic(t3:L,a1:L);         // box the arguments into an Object[]
     *    t6:L=MethodHandleImpl.loop(null,t2:L,t3:L);       // call the loop executor
     *    t7:L=MethodHandle.invokeBasic(t4:L,t6:L);t7:L}    // unbox the result; return the result
     * }</pre></blockquote>
     * <p>
     * {@code argL0} is a LoopClauses instance holding, in a 2-dimensional array, the init, step, pred, and fini method
     * handles. {@code argL1} and {@code argL2} are auxiliary method handles: {@code argL1} boxes arguments and wraps
     * them into {@code Object[]} ({@code ValueConversions.array()}), and {@code argL2} unboxes the result if necessary
     * ({@code ValueConversions.unbox()}).
     * <p>
     * Having {@code t3} and {@code t4} passed in via a BMH and not hardcoded in the lambda form allows to share lambda
     * forms among loop combinators with the same basic type.
     * <p>
     * The above template is instantiated by using the {@link LambdaFormEditor} to replace the {@code null} argument to
     * the {@code loop} invocation with the {@code BasicType} array describing the loop clause types. This argument is
     * ignored in the loop invoker, but will be extracted and used in {@linkplain InvokerBytecodeGenerator#emitLoop(int)
     * bytecode generation}.
     */
    private static LambdaForm makeLoopForm(MethodType basicType, BasicType[] localVarTypes) {
        MethodType lambdaType = basicType.invokerType();

        final int THIS_MH = 0;  // the BMH_LLL
        final int ARG_BASE = 1; // start of incoming arguments
        final int ARG_LIMIT = ARG_BASE + basicType.parameterCount();

        int nameCursor = ARG_LIMIT;
        final int GET_CLAUSE_DATA = nameCursor++;
        final int GET_COLLECT_ARGS = nameCursor++;
        final int GET_UNBOX_RESULT = nameCursor++;
        final int BOXED_ARGS = nameCursor++;
        final int LOOP = nameCursor++;
        final int UNBOX_RESULT = nameCursor++;

        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_LOOP);
        if (lform == null) {
            Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

            BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLL();
            names[THIS_MH] = names[THIS_MH].withConstraint(data);
            names[GET_CLAUSE_DATA] = new Name(data.getterFunction(0), names[THIS_MH]);
            names[GET_COLLECT_ARGS] = new Name(data.getterFunction(1), names[THIS_MH]);
            names[GET_UNBOX_RESULT] = new Name(data.getterFunction(2), names[THIS_MH]);

            // t_{i}:L=MethodHandle.invokeBasic(collectArgs:L,a1:L,...);
            MethodType collectArgsType = basicType.changeReturnType(Object.class);
            MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
            Object[] args = new Object[invokeBasic.type().parameterCount()];
            args[0] = names[GET_COLLECT_ARGS];
            System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT - ARG_BASE);
            names[BOXED_ARGS] = new Name(new NamedFunction(makeIntrinsic(invokeBasic, Intrinsic.LOOP)), args);

            // t_{i+1}:L=MethodHandleImpl.loop(localTypes:L,clauses:L,t_{i}:L);
            Object[] lArgs =
                    new Object[]{null, // placeholder for BasicType[] localTypes - will be added by LambdaFormEditor
                            names[GET_CLAUSE_DATA], names[BOXED_ARGS]};
            names[LOOP] = new Name(getFunction(NF_loop), lArgs);

            // t_{i+2}:I=MethodHandle.invokeBasic(unbox:L,t_{i+1}:L);
            MethodHandle invokeBasicUnbox = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
            Object[] unboxArgs = new Object[]{names[GET_UNBOX_RESULT], names[LOOP]};
            names[UNBOX_RESULT] = new Name(invokeBasicUnbox, unboxArgs);

            lform = basicType.form().setCachedLambdaForm(MethodTypeForm.LF_LOOP,
                    new LambdaForm(lambdaType.parameterCount(), names, Kind.LOOP));
        }

        // BOXED_ARGS is the index into the names array where the loop idiom starts
        return lform.editor().noteLoopLocalTypesForm(BOXED_ARGS, localVarTypes);
    }

    static class LoopClauses {
        @Stable final MethodHandle[][] clauses;
        LoopClauses(MethodHandle[][] clauses) {
            assert clauses.length == 4;
            this.clauses = clauses;
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("LoopClauses -- ");
            for (int i = 0; i < 4; ++i) {
                if (i > 0) {
                    sb.append("       ");
                }
                sb.append('<').append(i).append(">: ");
                MethodHandle[] hs = clauses[i];
                for (int j = 0; j < hs.length; ++j) {
                    if (j > 0) {
                        sb.append("          ");
                    }
                    sb.append('*').append(j).append(": ").append(hs[j]).append('\n');
                }
            }
            sb.append(" --\n");
            return sb.toString();
        }
    }

    /**
     * Intrinsified during LambdaForm compilation
     * (see {@link InvokerBytecodeGenerator#emitLoop(int)}).
     */
    @Hidden
    static Object loop(BasicType[] localTypes, LoopClauses clauseData, Object... av) throws Throwable {
        final MethodHandle[] init = clauseData.clauses[0];
        final MethodHandle[] step = clauseData.clauses[1];
        final MethodHandle[] pred = clauseData.clauses[2];
        final MethodHandle[] fini = clauseData.clauses[3];
        int varSize = (int) Stream.of(init).filter(h -> h.type().returnType() != void.class).count();
        int nArgs = init[0].type().parameterCount();
        Object[] varsAndArgs = new Object[varSize + nArgs];
        for (int i = 0, v = 0; i < init.length; ++i) {
            MethodHandle ih = init[i];
            if (ih.type().returnType() == void.class) {
                ih.invokeWithArguments(av);
            } else {
                varsAndArgs[v++] = ih.invokeWithArguments(av);
            }
        }
        System.arraycopy(av, 0, varsAndArgs, varSize, nArgs);
        final int nSteps = step.length;
        for (; ; ) {
            for (int i = 0, v = 0; i < nSteps; ++i) {
                MethodHandle p = pred[i];
                MethodHandle s = step[i];
                MethodHandle f = fini[i];
                if (s.type().returnType() == void.class) {
                    s.invokeWithArguments(varsAndArgs);
                } else {
                    varsAndArgs[v++] = s.invokeWithArguments(varsAndArgs);
                }
                if (!(boolean) p.invokeWithArguments(varsAndArgs)) {
                    return f.invokeWithArguments(varsAndArgs);
                }
            }
        }
    }

    /**
     * This method is bound as the predicate in {@linkplain MethodHandles#countedLoop(MethodHandle, MethodHandle,
     * MethodHandle) counting loops}.
     *
     * @param limit the upper bound of the parameter, statically bound at loop creation time.
     * @param counter the counter parameter, passed in during loop execution.
     *
     * @return whether the counter has reached the limit.
     */
    static boolean countedLoopPredicate(int limit, int counter) {
        return counter < limit;
    }

    /**
     * This method is bound as the step function in {@linkplain MethodHandles#countedLoop(MethodHandle, MethodHandle,
     * MethodHandle) counting loops} to increment the counter.
     *
     * @param limit the upper bound of the loop counter (ignored).
     * @param counter the loop counter.
     *
     * @return the loop counter incremented by 1.
     */
    static int countedLoopStep(int limit, int counter) {
        return counter + 1;
    }

    /**
     * This is bound to initialize the loop-local iterator in {@linkplain MethodHandles#iteratedLoop iterating loops}.
     *
     * @param it the {@link Iterable} over which the loop iterates.
     *
     * @return an {@link Iterator} over the argument's elements.
     */
    static Iterator<?> initIterator(Iterable<?> it) {
        return it.iterator();
    }

    /**
     * This method is bound as the predicate in {@linkplain MethodHandles#iteratedLoop iterating loops}.
     *
     * @param it the iterator to be checked.
     *
     * @return {@code true} iff there are more elements to iterate over.
     */
    static boolean iteratePredicate(Iterator<?> it) {
        return it.hasNext();
    }

    /**
     * This method is bound as the step for retrieving the current value from the iterator in {@linkplain
     * MethodHandles#iteratedLoop iterating loops}.
     *
     * @param it the iterator.
     *
     * @return the next element from the iterator.
     */
    static Object iterateNext(Iterator<?> it) {
        return it.next();
    }

    /**
     * Makes a {@code try-finally} handle that conforms to the type constraints.
     *
     * @param target the target to execute in a {@code try-finally} block.
     * @param cleanup the cleanup to execute in the {@code finally} block.
     * @param rtype the result type of the entire construct.
     * @param argTypes the types of the arguments.
     *
     * @return a handle on the constructed {@code try-finally} block.
     */
    static MethodHandle makeTryFinally(MethodHandle target, MethodHandle cleanup, Class<?> rtype, List<Class<?>> argTypes) {
        MethodType type = MethodType.methodType(rtype, argTypes);
        LambdaForm form = makeTryFinallyForm(type.basicType());

        // Prepare auxiliary method handles used during LambdaForm interpretation.
        // Box arguments and wrap them into Object[]: ValueConversions.array().
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);
        MethodHandle unboxResult = unboxResultHandle(rtype);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLL();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.factory().invokeBasic(type, form, (Object) target, (Object) cleanup,
                    (Object) collectArgs, (Object) unboxResult);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    /**
     * The LambdaForm shape for the tryFinally combinator is as follows (assuming one reference parameter passed in
     * {@code a1}, and a reference return type, with the return value represented by {@code t8}):
     * <blockquote><pre>{@code
     *  tryFinally=Lambda(a0:L,a1:L)=>{
     *    t2:L=BoundMethodHandle$Species_LLLL.argL0(a0:L);  // target method handle
     *    t3:L=BoundMethodHandle$Species_LLLL.argL1(a0:L);  // cleanup method handle
     *    t4:L=BoundMethodHandle$Species_LLLL.argL2(a0:L);  // helper handle to box the arguments into an Object[]
     *    t5:L=BoundMethodHandle$Species_LLLL.argL3(a0:L);  // helper handle to unbox the result
     *    t6:L=MethodHandle.invokeBasic(t4:L,a1:L);         // box the arguments into an Object[]
     *    t7:L=MethodHandleImpl.tryFinally(t2:L,t3:L,t6:L); // call the tryFinally executor
     *    t8:L=MethodHandle.invokeBasic(t5:L,t7:L);t8:L}    // unbox the result; return the result
     * }</pre></blockquote>
     * <p>
     * {@code argL0} and {@code argL1} are the target and cleanup method handles.
     * {@code argL2} and {@code argL3} are auxiliary method handles: {@code argL2} boxes arguments and wraps them into
     * {@code Object[]} ({@code ValueConversions.array()}), and {@code argL3} unboxes the result if necessary
     * ({@code ValueConversions.unbox()}).
     * <p>
     * Having {@code t4} and {@code t5} passed in via a BMH and not hardcoded in the lambda form allows to share lambda
     * forms among tryFinally combinators with the same basic type.
     */
    private static LambdaForm makeTryFinallyForm(MethodType basicType) {
        MethodType lambdaType = basicType.invokerType();

        LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_TF);
        if (lform != null) {
            return lform;
        }
        final int THIS_MH      = 0;  // the BMH_LLLL
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + basicType.parameterCount();

        int nameCursor = ARG_LIMIT;
        final int GET_TARGET       = nameCursor++;
        final int GET_CLEANUP      = nameCursor++;
        final int GET_COLLECT_ARGS = nameCursor++;
        final int GET_UNBOX_RESULT = nameCursor++;
        final int BOXED_ARGS       = nameCursor++;
        final int TRY_FINALLY      = nameCursor++;
        final int UNBOX_RESULT     = nameCursor++;

        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLL();
        names[THIS_MH]          = names[THIS_MH].withConstraint(data);
        names[GET_TARGET]       = new Name(data.getterFunction(0), names[THIS_MH]);
        names[GET_CLEANUP]      = new Name(data.getterFunction(1), names[THIS_MH]);
        names[GET_COLLECT_ARGS] = new Name(data.getterFunction(2), names[THIS_MH]);
        names[GET_UNBOX_RESULT] = new Name(data.getterFunction(3), names[THIS_MH]);

        // t_{i}:L=MethodHandle.invokeBasic(collectArgs:L,a1:L,...);
        MethodType collectArgsType = basicType.changeReturnType(Object.class);
        MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
        Object[] args = new Object[invokeBasic.type().parameterCount()];
        args[0] = names[GET_COLLECT_ARGS];
        System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT-ARG_BASE);
        names[BOXED_ARGS] = new Name(new NamedFunction(makeIntrinsic(invokeBasic, Intrinsic.TRY_FINALLY)), args);

        // t_{i+1}:L=MethodHandleImpl.tryFinally(target:L,exType:L,catcher:L,t_{i}:L);
        Object[] tfArgs = new Object[] {names[GET_TARGET], names[GET_CLEANUP], names[BOXED_ARGS]};
        names[TRY_FINALLY] = new Name(getFunction(NF_tryFinally), tfArgs);

        // t_{i+2}:I=MethodHandle.invokeBasic(unbox:L,t_{i+1}:L);
        MethodHandle invokeBasicUnbox = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
        Object[] unboxArgs  = new Object[] {names[GET_UNBOX_RESULT], names[TRY_FINALLY]};
        names[UNBOX_RESULT] = new Name(invokeBasicUnbox, unboxArgs);

        lform = new LambdaForm(lambdaType.parameterCount(), names, Kind.TRY_FINALLY);

        return basicType.form().setCachedLambdaForm(MethodTypeForm.LF_TF, lform);
    }

    /**
     * Intrinsified during LambdaForm compilation
     * (see {@link InvokerBytecodeGenerator#emitTryFinally emitTryFinally}).
     */
    @Hidden
    static Object tryFinally(MethodHandle target, MethodHandle cleanup, Object... av) throws Throwable {
        Throwable t = null;
        Object r = null;
        try {
            r = target.invokeWithArguments(av);
        } catch (Throwable thrown) {
            t = thrown;
            throw t;
        } finally {
            Object[] args = target.type().returnType() == void.class ? prepend(av, t) : prepend(av, t, r);
            r = cleanup.invokeWithArguments(args);
        }
        return r;
    }

    // see varargsArray method for chaching/package-private version of this
    private static MethodHandle makeCollector(Class<?> arrayType, int parameterCount) {
        MethodType type = MethodType.methodType(arrayType, Collections.nCopies(parameterCount, arrayType.componentType()));
        MethodHandle newArray = MethodHandles.arrayConstructor(arrayType);

        LambdaForm form = makeCollectorForm(type.basicType(), arrayType);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_L();
        BoundMethodHandle mh;
        try {
            mh = (BoundMethodHandle) data.factory().invokeBasic(type, form, (Object) newArray);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    private static LambdaForm makeCollectorForm(MethodType basicType, Class<?> arrayType) {
        MethodType lambdaType = basicType.invokerType();
        int parameterCount = basicType.parameterCount();

        // Only share the lambda form for empty arrays and reference types.
        // Sharing based on the basic type alone doesn't work because
        // we need a separate lambda form for byte/short/char/int which
        // are all erased to int otherwise.
        // Other caching for primitive types happens at the MethodHandle level (see varargsArray).
        boolean isReferenceType = !arrayType.componentType().isPrimitive();
        boolean isSharedLambdaForm = parameterCount == 0 || isReferenceType;
        if (isSharedLambdaForm) {
            LambdaForm lform = basicType.form().cachedLambdaForm(MethodTypeForm.LF_COLLECTOR);
            if (lform != null) {
                return lform;
            }
        }

        // use erased accessor for reference types
        MethodHandle storeFunc = isReferenceType
                ? ArrayAccessor.OBJECT_ARRAY_SETTER
                : makeArrayElementAccessor(arrayType, ArrayAccess.SET);

        final int THIS_MH      = 0;  // the BMH_L
        final int ARG_BASE     = 1;  // start of incoming arguments
        final int ARG_LIMIT    = ARG_BASE + parameterCount;

        int nameCursor = ARG_LIMIT;
        final int GET_NEW_ARRAY       = nameCursor++;
        final int CALL_NEW_ARRAY      = nameCursor++;
        final int STORE_ELEMENT_BASE  = nameCursor;
        final int STORE_ELEMENT_LIMIT = STORE_ELEMENT_BASE + parameterCount;
        nameCursor = STORE_ELEMENT_LIMIT;

        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_L();
        names[THIS_MH]          = names[THIS_MH].withConstraint(data);
        names[GET_NEW_ARRAY]    = new Name(data.getterFunction(0), names[THIS_MH]);

        MethodHandle invokeBasic = MethodHandles.basicInvoker(MethodType.methodType(Object.class, int.class));
        names[CALL_NEW_ARRAY] = new Name(new NamedFunction(invokeBasic), names[GET_NEW_ARRAY], parameterCount);
        for (int storeIndex = 0,
             storeNameCursor = STORE_ELEMENT_BASE,
             argCursor = ARG_BASE;
             storeNameCursor < STORE_ELEMENT_LIMIT;
             storeIndex++, storeNameCursor++, argCursor++){

            names[storeNameCursor] = new Name(new NamedFunction(makeIntrinsic(storeFunc, Intrinsic.ARRAY_STORE)),
                    names[CALL_NEW_ARRAY], storeIndex, names[argCursor]);
        }

        LambdaForm lform = new LambdaForm(lambdaType.parameterCount(), names, CALL_NEW_ARRAY, Kind.COLLECTOR);
        if (isSharedLambdaForm) {
            lform = basicType.form().setCachedLambdaForm(MethodTypeForm.LF_COLLECTOR, lform);
        }
        return lform;
    }

    // use a wrapper because we need this array to be @Stable
    static class CasesHolder {
        @Stable
        final MethodHandle[] cases;

        public CasesHolder(MethodHandle[] cases) {
            this.cases = cases;
        }
    }

    static MethodHandle makeTableSwitch(MethodType type, MethodHandle defaultCase, MethodHandle[] caseActions) {
        MethodType varargsType = type.changeReturnType(Object[].class);
        MethodHandle collectArgs = varargsArray(type.parameterCount()).asType(varargsType);

        MethodHandle unboxResult = unboxResultHandle(type.returnType());

        BoundMethodHandle.SpeciesData data = BoundMethodHandle.speciesData_LLLL();
        LambdaForm form = makeTableSwitchForm(type.basicType(), data, caseActions.length);
        BoundMethodHandle mh;
        CasesHolder caseHolder =  new CasesHolder(caseActions);
        try {
            mh = (BoundMethodHandle) data.factory().invokeBasic(type, form, (Object) defaultCase, (Object) collectArgs,
                                                                (Object) unboxResult, (Object) caseHolder);
        } catch (Throwable ex) {
            throw uncaughtException(ex);
        }
        assert(mh.type() == type);
        return mh;
    }

    private static class TableSwitchCacheKey {
        private static final Map<TableSwitchCacheKey, LambdaForm> CACHE = new ConcurrentHashMap<>();

        private final MethodType basicType;
        private final int numberOfCases;

        public TableSwitchCacheKey(MethodType basicType, int numberOfCases) {
            this.basicType = basicType;
            this.numberOfCases = numberOfCases;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TableSwitchCacheKey that = (TableSwitchCacheKey) o;
            return numberOfCases == that.numberOfCases && Objects.equals(basicType, that.basicType);
        }
        @Override
        public int hashCode() {
            return Objects.hash(basicType, numberOfCases);
        }
    }

    private static LambdaForm makeTableSwitchForm(MethodType basicType, BoundMethodHandle.SpeciesData data,
                                                  int numCases) {
        MethodType lambdaType = basicType.invokerType();

        // We need to cache based on the basic type X number of cases,
        // since the number of cases is used when generating bytecode.
        // This also means that we can't use the cache in MethodTypeForm,
        // which only uses the basic type as a key.
        TableSwitchCacheKey key = new TableSwitchCacheKey(basicType, numCases);
        LambdaForm lform = TableSwitchCacheKey.CACHE.get(key);
        if (lform != null) {
            return lform;
        }

        final int THIS_MH       = 0;
        final int ARG_BASE      = 1;  // start of incoming arguments
        final int ARG_LIMIT     = ARG_BASE + basicType.parameterCount();
        final int ARG_SWITCH_ON = ARG_BASE;
        assert ARG_SWITCH_ON < ARG_LIMIT;

        int nameCursor = ARG_LIMIT;
        final int GET_COLLECT_ARGS  = nameCursor++;
        final int GET_DEFAULT_CASE  = nameCursor++;
        final int GET_UNBOX_RESULT  = nameCursor++;
        final int GET_CASES         = nameCursor++;
        final int BOXED_ARGS        = nameCursor++;
        final int TABLE_SWITCH      = nameCursor++;
        final int UNBOXED_RESULT    = nameCursor++;

        int fieldCursor = 0;
        final int FIELD_DEFAULT_CASE  = fieldCursor++;
        final int FIELD_COLLECT_ARGS  = fieldCursor++;
        final int FIELD_UNBOX_RESULT  = fieldCursor++;
        final int FIELD_CASES         = fieldCursor++;

        Name[] names = arguments(nameCursor - ARG_LIMIT, lambdaType);

        names[THIS_MH] = names[THIS_MH].withConstraint(data);
        names[GET_DEFAULT_CASE] = new Name(data.getterFunction(FIELD_DEFAULT_CASE), names[THIS_MH]);
        names[GET_COLLECT_ARGS]  = new Name(data.getterFunction(FIELD_COLLECT_ARGS), names[THIS_MH]);
        names[GET_UNBOX_RESULT]  = new Name(data.getterFunction(FIELD_UNBOX_RESULT), names[THIS_MH]);
        names[GET_CASES] = new Name(data.getterFunction(FIELD_CASES), names[THIS_MH]);

        {
            MethodType collectArgsType = basicType.changeReturnType(Object.class);
            MethodHandle invokeBasic = MethodHandles.basicInvoker(collectArgsType);
            Object[] args = new Object[invokeBasic.type().parameterCount()];
            args[0] = names[GET_COLLECT_ARGS];
            System.arraycopy(names, ARG_BASE, args, 1, ARG_LIMIT - ARG_BASE);
            names[BOXED_ARGS] = new Name(new NamedFunction(makeIntrinsic(invokeBasic, Intrinsic.TABLE_SWITCH, numCases)), args);
        }

        {
            Object[] tfArgs = new Object[]{
                names[ARG_SWITCH_ON], names[GET_DEFAULT_CASE], names[GET_CASES], names[BOXED_ARGS]};
            names[TABLE_SWITCH] = new Name(getFunction(NF_tableSwitch), tfArgs);
        }

        {
            MethodHandle invokeBasic = MethodHandles.basicInvoker(MethodType.methodType(basicType.rtype(), Object.class));
            Object[] unboxArgs = new Object[]{names[GET_UNBOX_RESULT], names[TABLE_SWITCH]};
            names[UNBOXED_RESULT] = new Name(invokeBasic, unboxArgs);
        }

        lform = new LambdaForm(lambdaType.parameterCount(), names, Kind.TABLE_SWITCH);
        LambdaForm prev = TableSwitchCacheKey.CACHE.putIfAbsent(key, lform);
        return prev != null ? prev : lform;
    }

    @Hidden
    static Object tableSwitch(int input, MethodHandle defaultCase, CasesHolder holder, Object[] args) throws Throwable {
        MethodHandle[] caseActions = holder.cases;
        MethodHandle selectedCase;
        if (input < 0 || input >= caseActions.length) {
            selectedCase = defaultCase;
        } else {
            selectedCase = caseActions[input];
        }
        return selectedCase.invokeWithArguments(args);
    }

    // Indexes into constant method handles:
    static final int
            MH_cast                  = 0,
            MH_selectAlternative     = 1,
            MH_countedLoopPred       = 2,
            MH_countedLoopStep       = 3,
            MH_initIterator          = 4,
            MH_iteratePred           = 5,
            MH_iterateNext           = 6,
            MH_Array_newInstance     = 7,
            MH_LIMIT                 = 8;

    static MethodHandle getConstantHandle(int idx) {
        MethodHandle handle = HANDLES[idx];
        if (handle != null) {
            return handle;
        }
        return setCachedHandle(idx, makeConstantHandle(idx));
    }

    private static synchronized MethodHandle setCachedHandle(int idx, final MethodHandle method) {
        // Simulate a CAS, to avoid racy duplication of results.
        MethodHandle prev = HANDLES[idx];
        if (prev != null) {
            return prev;
        }
        HANDLES[idx] = method;
        return method;
    }

    // Local constant method handles:
    private static final @Stable MethodHandle[] HANDLES = new MethodHandle[MH_LIMIT];

    private static MethodHandle makeConstantHandle(int idx) {
        try {
            switch (idx) {
                case MH_cast:
                    return IMPL_LOOKUP.findVirtual(Class.class, "cast",
                            MethodType.methodType(Object.class, Object.class));
                case MH_selectAlternative:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "selectAlternative",
                            MethodType.methodType(MethodHandle.class, boolean.class, MethodHandle.class, MethodHandle.class));
                case MH_countedLoopPred:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "countedLoopPredicate",
                            MethodType.methodType(boolean.class, int.class, int.class));
                case MH_countedLoopStep:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "countedLoopStep",
                            MethodType.methodType(int.class, int.class, int.class));
                case MH_initIterator:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "initIterator",
                            MethodType.methodType(Iterator.class, Iterable.class));
                case MH_iteratePred:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "iteratePredicate",
                            MethodType.methodType(boolean.class, Iterator.class));
                case MH_iterateNext:
                    return IMPL_LOOKUP.findStatic(MethodHandleImpl.class, "iterateNext",
                            MethodType.methodType(Object.class, Iterator.class));
                case MH_Array_newInstance:
                    return IMPL_LOOKUP.findStatic(Array.class, "newInstance",
                            MethodType.methodType(Object.class, Class.class, int.class));
            }
        } catch (ReflectiveOperationException ex) {
            throw newInternalError(ex);
        }
        throw newInternalError("Unknown function index: " + idx);
    }
}
