/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.invoke.util.Wrapper;

import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.invoke.LambdaForm.*;
import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.MethodHandleImpl.Intrinsic;
import static java.lang.invoke.MethodHandleImpl.NF_loop;
import static java.lang.invoke.MethodHandleImpl.makeIntrinsic;

/** Transforms on LFs.
 *  A lambda-form editor can derive new LFs from its base LF.
 *  The editor can cache derived LFs, which simplifies the reuse of their underlying bytecodes.
 *  To support this caching, a LF has an optional pointer to its editor.
 */
class LambdaFormEditor {
    final LambdaForm lambdaForm;

    private LambdaFormEditor(LambdaForm lambdaForm) {
        this.lambdaForm = lambdaForm;
    }

    // Factory method.
    static LambdaFormEditor lambdaFormEditor(LambdaForm lambdaForm) {
        // TO DO:  Consider placing intern logic here, to cut down on duplication.
        // lambdaForm = findPreexistingEquivalent(lambdaForm)

        // Always use uncustomized version for editing.
        // It helps caching and customized LambdaForms reuse transformCache field to keep a link to uncustomized version.
        return new LambdaFormEditor(lambdaForm.uncustomize());
    }

    // Transform types
    // maybe add more for guard with test, catch exception, pointwise type conversions
    private static final byte
            BIND_ARG = 1,
            ADD_ARG = 2,
            DUP_ARG = 3,
            SPREAD_ARGS = 4,
            FILTER_ARG = 5,
            FILTER_RETURN = 6,
            FILTER_RETURN_TO_ZERO = 7,
            COLLECT_ARGS = 8,
            COLLECT_ARGS_TO_VOID = 9,
            COLLECT_ARGS_TO_ARRAY = 10,
            FOLD_ARGS = 11,
            FOLD_ARGS_TO_VOID = 12,
            PERMUTE_ARGS = 13,
            LOCAL_TYPES = 14,
            FOLD_SELECT_ARGS = 15,
            FOLD_SELECT_ARGS_TO_VOID = 16,
            FILTER_SELECT_ARGS = 17,
            REPEAT_FILTER_ARGS = 18;

    /**
     * A description of a cached transform, possibly associated with the result of the transform.
     * The logical content is a sequence of byte values, starting with a kind value.
     * The sequence is unterminated, ending with an indefinite number of zero bytes.
     * Sequences that are simple (short enough and with small enough values) pack into a 64-bit long.
     *
     * Tightly coupled with the TransformKey class, which is used to lookup existing
     * Transforms.
     */
    private static final class Transform extends SoftReference<LambdaForm> {
        final long packedBytes;
        final byte[] fullBytes;

        private Transform(long packedBytes, byte[] fullBytes, LambdaForm result) {
            super(result);
            this.packedBytes = packedBytes;
            this.fullBytes = fullBytes;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TransformKey) {
                return equals((TransformKey) obj);
            }
            return obj instanceof Transform && equals((Transform)obj);
        }

        private boolean equals(TransformKey that) {
            return this.packedBytes == that.packedBytes && Arrays.equals(this.fullBytes, that.fullBytes);
        }

        private boolean equals(Transform that) {
            return this.packedBytes == that.packedBytes && Arrays.equals(this.fullBytes, that.fullBytes);
        }

        @Override
        public int hashCode() {
            if (packedBytes != 0) {
                assert(fullBytes == null);
                return Long.hashCode(packedBytes);
            }
            return Arrays.hashCode(fullBytes);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append(new TransformKey(packedBytes, fullBytes).toString());
            LambdaForm result = get();
            if (result != null) {
                buf.append(" result=");
                buf.append(result);
            }
            return buf.toString();
        }
    }

    /**
     * Used as a lookup key to find existing Transforms
     */
    private static final class TransformKey {
        final long packedBytes;
        final byte[] fullBytes;

        private TransformKey(long packedBytes) {
            this.packedBytes = packedBytes;
            this.fullBytes = null;
        }

        private TransformKey(byte[] fullBytes) {
            this.fullBytes = fullBytes;
            this.packedBytes = 0;
        }

        private TransformKey(long packedBytes, byte[] fullBytes) {
            this.fullBytes = fullBytes;
            this.packedBytes = packedBytes;
        }

        private static byte bval(int b) {
            assert((b & 0xFF) == b);  // incoming value must fit in *unsigned* byte
            return (byte)b;
        }
        static TransformKey of(byte k, int b1) {
            byte b0 = bval(k);
            if (inRange(b0 | b1))
                return new TransformKey(packedBytes(b0, b1));
            else
                return new TransformKey(fullBytes(b0, b1));
        }
        static TransformKey of(byte b0, int b1, int b2) {
            if (inRange(b0 | b1 | b2))
                return new TransformKey(packedBytes(b0, b1, b2));
            else
                return new TransformKey(fullBytes(b0, b1, b2));
        }
        static TransformKey of(byte b0, int b1, int b2, int b3) {
            if (inRange(b0 | b1 | b2 | b3))
                return new TransformKey(packedBytes(b0, b1, b2, b3));
            else
                return new TransformKey(fullBytes(b0, b1, b2, b3));
        }
        private static final byte[] NO_BYTES = {};
        static TransformKey of(byte kind, int... b123) {
            return ofBothArrays(kind, b123, NO_BYTES);
        }

        static TransformKey of(byte kind, int b1, int[] b23456) {
            byte[] fullBytes = new byte[b23456.length + 2];
            fullBytes[0] = kind;
            fullBytes[1] = bval(b1);
            for (int i = 0; i < b23456.length; i++) {
                fullBytes[i + 2] = TransformKey.bval(b23456[i]);
            }
            long packedBytes = packedBytes(fullBytes);
            if (packedBytes != 0)
                return new TransformKey(packedBytes);
            else
                return new TransformKey(fullBytes);
        }

        static TransformKey of(byte kind, int b1, int b2, byte[] b345) {
            return ofBothArrays(kind, new int[]{ b1, b2 }, b345);
        }
        private static TransformKey ofBothArrays(byte kind, int[] b123, byte[] b456) {
            byte[] fullBytes = new byte[1 + b123.length + b456.length];
            int i = 0;
            fullBytes[i++] = bval(kind);
            for (int bv : b123) {
                fullBytes[i++] = bval(bv);
            }
            for (byte bv : b456) {
                fullBytes[i++] = bv;
            }
            long packedBytes = packedBytes(fullBytes);
            if (packedBytes != 0)
                return new TransformKey(packedBytes);
            else
                return new TransformKey(fullBytes);
        }

        private static final boolean STRESS_TEST = false; // turn on to disable most packing
        private static final int
                PACKED_BYTE_SIZE = (STRESS_TEST ? 2 : 4),
                PACKED_BYTE_MASK = (1 << PACKED_BYTE_SIZE) - 1,
                PACKED_BYTE_MAX_LENGTH = (STRESS_TEST ? 3 : 64 / PACKED_BYTE_SIZE);

        private static long packedBytes(byte[] bytes) {
            if (!inRange(bytes[0]) || bytes.length > PACKED_BYTE_MAX_LENGTH)
                return 0;
            long pb = 0;
            int bitset = 0;
            for (int i = 0; i < bytes.length; i++) {
                int b = bytes[i] & 0xFF;
                bitset |= b;
                pb |= (long)b << (i * PACKED_BYTE_SIZE);
            }
            if (!inRange(bitset))
                return 0;
            return pb;
        }
        private static long packedBytes(int b0, int b1) {
            assert(inRange(b0 | b1));
            return (  (b0 << 0*PACKED_BYTE_SIZE)
                    | (b1 << 1*PACKED_BYTE_SIZE));
        }
        private static long packedBytes(int b0, int b1, int b2) {
            assert(inRange(b0 | b1 | b2));
            return (  (b0 << 0*PACKED_BYTE_SIZE)
                    | (b1 << 1*PACKED_BYTE_SIZE)
                    | (b2 << 2*PACKED_BYTE_SIZE));
        }
        private static long packedBytes(int b0, int b1, int b2, int b3) {
            assert(inRange(b0 | b1 | b2 | b3));
            return (  (b0 << 0*PACKED_BYTE_SIZE)
                    | (b1 << 1*PACKED_BYTE_SIZE)
                    | (b2 << 2*PACKED_BYTE_SIZE)
                    | (b3 << 3*PACKED_BYTE_SIZE));
        }
        private static boolean inRange(int bitset) {
            assert((bitset & 0xFF) == bitset);  // incoming values must fit in *unsigned* byte
            return ((bitset & ~PACKED_BYTE_MASK) == 0);
        }
        private static byte[] fullBytes(int... byteValues) {
            byte[] bytes = new byte[byteValues.length];
            int i = 0;
            for (int bv : byteValues) {
                bytes[i++] = bval(bv);
            }
            assert(packedBytes(bytes) == 0);
            return bytes;
        }

        Transform withResult(LambdaForm result) {
            return new Transform(this.packedBytes, this.fullBytes, result);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            long bits = packedBytes;
            if (bits != 0) {
                buf.append("(");
                while (bits != 0) {
                    buf.append(bits & PACKED_BYTE_MASK);
                    bits >>>= PACKED_BYTE_SIZE;
                    if (bits != 0)  buf.append(",");
                }
                buf.append(")");
            }
            if (fullBytes != null) {
                buf.append("unpacked");
                buf.append(Arrays.toString(fullBytes));
            }
            return buf.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TransformKey) {
                return equals((TransformKey) obj);
            }
            return obj instanceof Transform && equals((Transform)obj);
        }

        private boolean equals(TransformKey that) {
            return this.packedBytes == that.packedBytes && Arrays.equals(this.fullBytes, that.fullBytes);
        }

        private boolean equals(Transform that) {
            return this.packedBytes == that.packedBytes && Arrays.equals(this.fullBytes, that.fullBytes);
        }

        @Override
        public int hashCode() {
            if (packedBytes != 0) {
                assert(fullBytes == null);
                return Long.hashCode(packedBytes);
            }
            return Arrays.hashCode(fullBytes);
        }
    }

    /** Find a previously cached transform equivalent to the given one, and return its result. */
    private LambdaForm getInCache(TransformKey key) {
        // The transformCache is one of null, Transform, Transform[], or ConcurrentHashMap.
        Object c = lambdaForm.transformCache;
        Transform k = null;
        if (c instanceof ConcurrentHashMap) {
            @SuppressWarnings("unchecked")
            ConcurrentHashMap<Transform,Transform> m = (ConcurrentHashMap<Transform,Transform>) c;
            k = m.get(key);
        } else if (c == null) {
            return null;
        } else if (c instanceof Transform t) {
            // one-element cache avoids overhead of an array
            if (t.equals(key))  k = t;
        } else {
            Transform[] ta = (Transform[])c;
            for (int i = 0; i < ta.length; i++) {
                Transform t = ta[i];
                if (t == null)  break;
                if (t.equals(key)) { k = t; break; }
            }
        }
        assert(k == null || key.equals(k));
        return (k != null) ? k.get() : null;
    }

    /** Arbitrary but reasonable limits on Transform[] size for cache. */
    private static final int MIN_CACHE_ARRAY_SIZE = 4, MAX_CACHE_ARRAY_SIZE = 16;

    /** Cache a transform with its result, and return that result.
     *  But if an equivalent transform has already been cached, return its result instead.
     */
    private LambdaForm putInCache(TransformKey key, LambdaForm form) {
        Transform transform = key.withResult(form);
        for (int pass = 0; ; pass++) {
            Object c = lambdaForm.transformCache;
            if (c instanceof ConcurrentHashMap) {
                @SuppressWarnings("unchecked")
                ConcurrentHashMap<Transform,Transform> m = (ConcurrentHashMap<Transform,Transform>) c;
                Transform k = m.putIfAbsent(transform, transform);
                if (k == null) return form;
                LambdaForm result = k.get();
                if (result != null) {
                    return result;
                } else {
                    if (m.replace(transform, k, transform)) {
                        return form;
                    } else {
                        continue;
                    }
                }
            }
            assert(pass == 0);
            synchronized (lambdaForm) {
                c = lambdaForm.transformCache;
                if (c instanceof ConcurrentHashMap)
                    continue;
                if (c == null) {
                    lambdaForm.transformCache = transform;
                    return form;
                }
                Transform[] ta;
                if (c instanceof Transform k) {
                    if (k.equals(key)) {
                        LambdaForm result = k.get();
                        if (result == null) {
                            lambdaForm.transformCache = transform;
                            return form;
                        } else {
                            return result;
                        }
                    } else if (k.get() == null) { // overwrite stale entry
                        lambdaForm.transformCache = transform;
                        return form;
                    }
                    // expand one-element cache to small array
                    ta = new Transform[MIN_CACHE_ARRAY_SIZE];
                    ta[0] = k;
                    lambdaForm.transformCache = ta;
                } else {
                    // it is already expanded
                    ta = (Transform[])c;
                }
                int len = ta.length;
                int stale = -1;
                int i;
                for (i = 0; i < len; i++) {
                    Transform k = ta[i];
                    if (k == null) {
                        break;
                    }
                    if (k.equals(transform)) {
                        LambdaForm result = k.get();
                        if (result == null) {
                            ta[i] = transform;
                            return form;
                        } else {
                            return result;
                        }
                    } else if (stale < 0 && k.get() == null) {
                        stale = i; // remember 1st stale entry index
                    }
                }
                if (i < len || stale >= 0) {
                    // just fall through to cache update
                } else if (len < MAX_CACHE_ARRAY_SIZE) {
                    len = Math.min(len * 2, MAX_CACHE_ARRAY_SIZE);
                    ta = Arrays.copyOf(ta, len);
                    lambdaForm.transformCache = ta;
                } else {
                    ConcurrentHashMap<Transform, Transform> m = new ConcurrentHashMap<>(MAX_CACHE_ARRAY_SIZE * 2);
                    for (Transform k : ta) {
                        m.put(k, k);
                    }
                    lambdaForm.transformCache = m;
                    // The second iteration will update for this query, concurrently.
                    continue;
                }
                int idx = (stale >= 0) ? stale : i;
                ta[idx] = transform;
                return form;
            }
        }
    }

    private LambdaFormBuffer buffer() {
        return new LambdaFormBuffer(lambdaForm);
    }

    /// Editing methods for method handles.  These need to have fast paths.

    private BoundMethodHandle.SpeciesData oldSpeciesData() {
        return BoundMethodHandle.speciesDataFor(lambdaForm);
    }

    private BoundMethodHandle.SpeciesData newSpeciesData(BasicType type) {
        return oldSpeciesData().extendWith((byte) type.ordinal());
    }

    BoundMethodHandle bindArgumentL(BoundMethodHandle mh, int pos, Object value) {
        assert(mh.speciesData() == oldSpeciesData());
        BasicType bt = L_TYPE;
        MethodType type2 = bindArgumentType(mh, pos, bt);
        LambdaForm form2 = bindArgumentForm(1+pos);
        return mh.copyWithExtendL(type2, form2, value);
    }
    BoundMethodHandle bindArgumentI(BoundMethodHandle mh, int pos, int value) {
        assert(mh.speciesData() == oldSpeciesData());
        BasicType bt = I_TYPE;
        MethodType type2 = bindArgumentType(mh, pos, bt);
        LambdaForm form2 = bindArgumentForm(1+pos);
        return mh.copyWithExtendI(type2, form2, value);
    }

    BoundMethodHandle bindArgumentJ(BoundMethodHandle mh, int pos, long value) {
        assert(mh.speciesData() == oldSpeciesData());
        BasicType bt = J_TYPE;
        MethodType type2 = bindArgumentType(mh, pos, bt);
        LambdaForm form2 = bindArgumentForm(1+pos);
        return mh.copyWithExtendJ(type2, form2, value);
    }

    BoundMethodHandle bindArgumentF(BoundMethodHandle mh, int pos, float value) {
        assert(mh.speciesData() == oldSpeciesData());
        BasicType bt = F_TYPE;
        MethodType type2 = bindArgumentType(mh, pos, bt);
        LambdaForm form2 = bindArgumentForm(1+pos);
        return mh.copyWithExtendF(type2, form2, value);
    }

    BoundMethodHandle bindArgumentD(BoundMethodHandle mh, int pos, double value) {
        assert(mh.speciesData() == oldSpeciesData());
        BasicType bt = D_TYPE;
        MethodType type2 = bindArgumentType(mh, pos, bt);
        LambdaForm form2 = bindArgumentForm(1+pos);
        return mh.copyWithExtendD(type2, form2, value);
    }

    private MethodType bindArgumentType(BoundMethodHandle mh, int pos, BasicType bt) {
        assert(mh.form.uncustomize() == lambdaForm);
        assert(mh.form.names[1+pos].type == bt);
        assert(BasicType.basicType(mh.type().parameterType(pos)) == bt);
        return mh.type().dropParameterTypes(pos, pos+1);
    }

    /// Editing methods for lambda forms.
    // Each editing method can (potentially) cache the edited LF so that it can be reused later.

    LambdaForm bindArgumentForm(int pos) {
        TransformKey key = TransformKey.of(BIND_ARG, pos);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.parameterConstraint(0) == newSpeciesData(lambdaForm.parameterType(pos)));
            return form;
        }
        LambdaFormBuffer buf = buffer();
        buf.startEdit();

        BoundMethodHandle.SpeciesData oldData = oldSpeciesData();
        BoundMethodHandle.SpeciesData newData = newSpeciesData(lambdaForm.parameterType(pos));
        Name oldBaseAddress = lambdaForm.parameter(0);  // BMH holding the values
        Name newBaseAddress;
        NamedFunction getter = newData.getterFunction(oldData.fieldCount());

        if (pos != 0) {
            // The newly created LF will run with a different BMH.
            // Switch over any pre-existing BMH field references to the new BMH class.
            buf.replaceFunctions(oldData.getterFunctions(), newData.getterFunctions(), oldBaseAddress);
            newBaseAddress = oldBaseAddress.withConstraint(newData);
            buf.renameParameter(0, newBaseAddress);
            buf.replaceParameterByNewExpression(pos, new Name(getter, newBaseAddress));
        } else {
            // cannot bind the MH arg itself, unless oldData is empty
            assert(oldData == BoundMethodHandle.SPECIALIZER.topSpecies());
            newBaseAddress = new Name(L_TYPE).withConstraint(newData);
            buf.replaceParameterByNewExpression(0, new Name(getter, newBaseAddress));
            buf.insertParameter(0, newBaseAddress);
        }

        form = buf.endEdit();
        return putInCache(key, form);
    }

    LambdaForm addArgumentForm(int pos, BasicType type) {
        TransformKey key = TransformKey.of(ADD_ARG, pos, type.ordinal());
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity+1);
            assert(form.parameterType(pos) == type);
            return form;
        }
        LambdaFormBuffer buf = buffer();
        buf.startEdit();

        buf.insertParameter(pos, new Name(type));

        form = buf.endEdit();
        return putInCache(key, form);
    }

    LambdaForm dupArgumentForm(int srcPos, int dstPos) {
        TransformKey key = TransformKey.of(DUP_ARG, srcPos, dstPos);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity-1);
            return form;
        }
        LambdaFormBuffer buf = buffer();
        buf.startEdit();

        assert(lambdaForm.parameter(srcPos).constraint == null);
        assert(lambdaForm.parameter(dstPos).constraint == null);
        buf.replaceParameterByCopy(dstPos, srcPos);

        form = buf.endEdit();
        return putInCache(key, form);
    }

    LambdaForm spreadArgumentsForm(int pos, Class<?> arrayType, int arrayLength) {
        Class<?> elementType = arrayType.getComponentType();
        Class<?> erasedArrayType = arrayType;
        if (!elementType.isPrimitive())
            erasedArrayType = Object[].class;
        BasicType bt = basicType(elementType);
        int elementTypeKey = bt.ordinal();
        if (bt.basicTypeClass() != elementType) {
            if (elementType.isPrimitive()) {
                elementTypeKey = TYPE_LIMIT + Wrapper.forPrimitiveType(elementType).ordinal();
            }
        }
        TransformKey key = TransformKey.of(SPREAD_ARGS, pos, elementTypeKey, arrayLength);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity - arrayLength + 1);
            return form;
        }
        LambdaFormBuffer buf = buffer();
        buf.startEdit();

        assert(pos <= MethodType.MAX_JVM_ARITY);
        assert(pos + arrayLength <= lambdaForm.arity);
        assert(pos > 0);  // cannot spread the MH arg itself

        Name spreadParam = new Name(L_TYPE);
        Name checkSpread = new Name(MethodHandleImpl.getFunction(MethodHandleImpl.NF_checkSpreadArgument),
                spreadParam, arrayLength);

        // insert the new expressions
        int exprPos = lambdaForm.arity();
        buf.insertExpression(exprPos++, checkSpread);
        // adjust the arguments
        MethodHandle aload = MethodHandles.arrayElementGetter(erasedArrayType);
        for (int i = 0; i < arrayLength; i++) {
            Name loadArgument = new Name(new NamedFunction(makeIntrinsic(aload, Intrinsic.ARRAY_LOAD)), spreadParam, i);
            buf.insertExpression(exprPos + i, loadArgument);
            buf.replaceParameterByCopy(pos + i, exprPos + i);
        }
        buf.insertParameter(pos, spreadParam);

        form = buf.endEdit();
        return putInCache(key, form);
    }

    LambdaForm collectArgumentsForm(int pos, MethodType collectorType) {
        int collectorArity = collectorType.parameterCount();
        boolean dropResult = (collectorType.returnType() == void.class);
        if (collectorArity == 1 && !dropResult) {
            return filterArgumentForm(pos, basicType(collectorType.parameterType(0)));
        }
        byte[] newTypes = BasicType.basicTypesOrd(collectorType.ptypes());
        byte kind = (dropResult ? COLLECT_ARGS_TO_VOID : COLLECT_ARGS);
        if (dropResult && collectorArity == 0)  pos = 1;  // pure side effect
        TransformKey key = TransformKey.of(kind, pos, collectorArity, newTypes);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity - (dropResult ? 0 : 1) + collectorArity);
            return form;
        }
        form = makeArgumentCombinationForm(pos, collectorType, false, dropResult);
        return putInCache(key, form);
    }

    LambdaForm filterArgumentForm(int pos, BasicType newType) {
        TransformKey key = TransformKey.of(FILTER_ARG, pos, newType.ordinal());
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity);
            assert(form.parameterType(pos) == newType);
            return form;
        }

        BasicType oldType = lambdaForm.parameterType(pos);
        MethodType filterType = MethodType.methodType(oldType.basicTypeClass(),
                newType.basicTypeClass());
        form = makeArgumentCombinationForm(pos, filterType, false, false);
        return putInCache(key, form);
    }

    /**
     * This creates a LF that will repeatedly invoke some unary filter function
     * at each of the given positions. This allows fewer LFs and BMH species
     * classes to be generated in typical cases compared to building up the form
     * by reapplying of {@code filterArgumentForm(int,BasicType)}, and should do
     * no worse in the worst case.
     */
    LambdaForm filterRepeatedArgumentForm(BasicType newType, int... argPositions) {
        assert (argPositions.length > 1);
        TransformKey key = TransformKey.of(REPEAT_FILTER_ARGS, newType.ordinal(), argPositions);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity &&
                    formParametersMatch(form, newType, argPositions));
            return form;
        }
        BasicType oldType = lambdaForm.parameterType(argPositions[0]);
        MethodType filterType = MethodType.methodType(oldType.basicTypeClass(),
                newType.basicTypeClass());
        form = makeRepeatedFilterForm(filterType, argPositions);
        assert (formParametersMatch(form, newType, argPositions));
        return putInCache(key, form);
    }

    private boolean formParametersMatch(LambdaForm form, BasicType newType, int... argPositions) {
        for (int i : argPositions) {
            if (form.parameterType(i) != newType) {
                return false;
            }
        }
        return true;
    }

    private LambdaForm makeRepeatedFilterForm(MethodType combinerType, int... positions) {
        assert (combinerType.parameterCount() == 1 &&
                combinerType == combinerType.basicType() &&
                combinerType.returnType() != void.class);
        LambdaFormBuffer buf = buffer();
        buf.startEdit();

        BoundMethodHandle.SpeciesData oldData = oldSpeciesData();
        BoundMethodHandle.SpeciesData newData = newSpeciesData(L_TYPE);

        // The newly created LF will run with a different BMH.
        // Switch over any pre-existing BMH field references to the new BMH class.
        Name oldBaseAddress = lambdaForm.parameter(0);  // BMH holding the values
        buf.replaceFunctions(oldData.getterFunctions(), newData.getterFunctions(), oldBaseAddress);
        Name newBaseAddress = oldBaseAddress.withConstraint(newData);
        buf.renameParameter(0, newBaseAddress);

        // Insert the new expressions at the end
        int exprPos = lambdaForm.arity();
        Name getCombiner = new Name(newData.getterFunction(oldData.fieldCount()), newBaseAddress);
        buf.insertExpression(exprPos++, getCombiner);

        // After inserting expressions, we insert parameters in order
        // from lowest to highest, simplifying the calculation of where parameters
        // and expressions are
        var newParameters = new TreeMap<Name, Integer>(new Comparator<>() {
            public int compare(Name n1, Name n2) {
                return n1.index - n2.index;
            }
        });

        // Insert combiner expressions in reverse order so that the invocation of
        // the resulting form will invoke the combiners in left-to-right order
        for (int i = positions.length - 1; i >= 0; --i) {
            int pos = positions[i];
            assert (pos > 0 && pos <= MethodType.MAX_JVM_ARITY && pos < lambdaForm.arity);

            Name newParameter = new Name(pos, basicType(combinerType.parameterType(0)));
            Object[] combinerArgs = {getCombiner, newParameter};

            Name callCombiner = new Name(combinerType, combinerArgs);
            buf.insertExpression(exprPos++, callCombiner);
            newParameters.put(newParameter, exprPos);
        }

        // Mix in new parameters from left to right in the buffer (this doesn't change
        // execution order
        int offset = 0;
        for (var entry : newParameters.entrySet()) {
            Name newParameter = entry.getKey();
            int from = entry.getValue();
            buf.insertParameter(newParameter.index() + 1 + offset, newParameter);
            buf.replaceParameterByCopy(newParameter.index() + offset, from + offset);
            offset++;
        }
        return buf.endEdit();
    }


    private LambdaForm makeArgumentCombinationForm(int pos,
                                                   MethodType combinerType,
                                                   boolean keepArguments, boolean dropResult) {
        LambdaFormBuffer buf = buffer();
        buf.startEdit();
        int combinerArity = combinerType.parameterCount();
        int resultArity = (dropResult ? 0 : 1);

        assert(pos <= MethodType.MAX_JVM_ARITY);
        assert(pos + resultArity + (keepArguments ? combinerArity : 0) <= lambdaForm.arity);
        assert(pos > 0);  // cannot filter the MH arg itself
        assert(combinerType == combinerType.basicType());
        assert(combinerType.returnType() != void.class || dropResult);

        BoundMethodHandle.SpeciesData oldData = oldSpeciesData();
        BoundMethodHandle.SpeciesData newData = newSpeciesData(L_TYPE);

        // The newly created LF will run with a different BMH.
        // Switch over any pre-existing BMH field references to the new BMH class.
        Name oldBaseAddress = lambdaForm.parameter(0);  // BMH holding the values
        buf.replaceFunctions(oldData.getterFunctions(), newData.getterFunctions(), oldBaseAddress);
        Name newBaseAddress = oldBaseAddress.withConstraint(newData);
        buf.renameParameter(0, newBaseAddress);

        Name getCombiner = new Name(newData.getterFunction(oldData.fieldCount()), newBaseAddress);
        Object[] combinerArgs = new Object[1 + combinerArity];
        combinerArgs[0] = getCombiner;
        Name[] newParams;
        if (keepArguments) {
            newParams = new Name[0];
            System.arraycopy(lambdaForm.names, pos + resultArity,
                             combinerArgs, 1, combinerArity);
        } else {
            newParams = new Name[combinerArity];
            for (int i = 0; i < newParams.length; i++) {
                newParams[i] = new Name(pos + i, basicType(combinerType.parameterType(i)));
            }
            System.arraycopy(newParams, 0,
                             combinerArgs, 1, combinerArity);
        }
        Name callCombiner = new Name(combinerType, combinerArgs);

        // insert the two new expressions
        int exprPos = lambdaForm.arity();
        buf.insertExpression(exprPos+0, getCombiner);
        buf.insertExpression(exprPos+1, callCombiner);

        // insert new arguments, if needed
        int argPos = pos + resultArity;  // skip result parameter
        for (Name newParam : newParams) {
            buf.insertParameter(argPos++, newParam);
        }
        assert(buf.lastIndexOf(callCombiner) == exprPos+1+newParams.length);
        if (!dropResult) {
            buf.replaceParameterByCopy(pos, exprPos+1+newParams.length);
        }

        return buf.endEdit();
    }

    private LambdaForm makeArgumentCombinationForm(int pos,
                                                   MethodType combinerType,
                                                   int[] argPositions,
                                                   boolean keepArguments,
                                                   boolean dropResult) {
        LambdaFormBuffer buf = buffer();
        buf.startEdit();
        int combinerArity = combinerType.parameterCount();
        assert(combinerArity == argPositions.length);

        int resultArity = (dropResult ? 0 : 1);

        assert(pos <= lambdaForm.arity);
        assert(pos > 0);  // cannot filter the MH arg itself
        assert(combinerType == combinerType.basicType());
        assert(combinerType.returnType() != void.class || dropResult);

        BoundMethodHandle.SpeciesData oldData = oldSpeciesData();
        BoundMethodHandle.SpeciesData newData = newSpeciesData(L_TYPE);

        // The newly created LF will run with a different BMH.
        // Switch over any pre-existing BMH field references to the new BMH class.
        Name oldBaseAddress = lambdaForm.parameter(0);  // BMH holding the values
        buf.replaceFunctions(oldData.getterFunctions(), newData.getterFunctions(), oldBaseAddress);
        Name newBaseAddress = oldBaseAddress.withConstraint(newData);
        buf.renameParameter(0, newBaseAddress);

        Name getCombiner = new Name(newData.getterFunction(oldData.fieldCount()), newBaseAddress);
        Object[] combinerArgs = new Object[1 + combinerArity];
        combinerArgs[0] = getCombiner;
        Name newParam = null;
        if (keepArguments) {
            for (int i = 0; i < combinerArity; i++) {
                combinerArgs[i + 1] = lambdaForm.parameter(1 + argPositions[i]);
                assert (basicType(combinerType.parameterType(i)) == lambdaForm.parameterType(1 + argPositions[i]));
            }
        } else {
            newParam = new Name(pos, BasicType.basicType(combinerType.returnType()));
            for (int i = 0; i < combinerArity; i++) {
                int argPos = 1 + argPositions[i];
                if (argPos == pos) {
                    combinerArgs[i + 1] = newParam;
                } else {
                    combinerArgs[i + 1] = lambdaForm.parameter(argPos);
                }
                assert (basicType(combinerType.parameterType(i)) == lambdaForm.parameterType(1 + argPositions[i]));
            }
        }
        Name callCombiner = new Name(combinerType, combinerArgs);

        // insert the two new expressions
        int exprPos = lambdaForm.arity();
        buf.insertExpression(exprPos+0, getCombiner);
        buf.insertExpression(exprPos+1, callCombiner);

        // insert new arguments, if needed
        int argPos = pos + resultArity;  // skip result parameter
        if (newParam != null) {
            buf.insertParameter(argPos++, newParam);
            exprPos++;
        }
        assert(buf.lastIndexOf(callCombiner) == exprPos+1);
        if (!dropResult) {
            buf.replaceParameterByCopy(pos, exprPos+1);
        }

        return buf.endEdit();
    }

    LambdaForm filterReturnForm(BasicType newType, boolean constantZero) {
        byte kind = (constantZero ? FILTER_RETURN_TO_ZERO : FILTER_RETURN);
        TransformKey key = TransformKey.of(kind, newType.ordinal());
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity);
            assert(form.returnType() == newType);
            return form;
        }
        LambdaFormBuffer buf = buffer();
        buf.startEdit();

        int insPos = lambdaForm.names.length;
        Name callFilter;
        if (constantZero) {
            // Synthesize a constant zero value for the given type.
            if (newType == V_TYPE)
                callFilter = null;
            else
                callFilter = new Name(constantZero(newType));
        } else {
            BoundMethodHandle.SpeciesData oldData = oldSpeciesData();
            BoundMethodHandle.SpeciesData newData = newSpeciesData(L_TYPE);

            // The newly created LF will run with a different BMH.
            // Switch over any pre-existing BMH field references to the new BMH class.
            Name oldBaseAddress = lambdaForm.parameter(0);  // BMH holding the values
            buf.replaceFunctions(oldData.getterFunctions(), newData.getterFunctions(), oldBaseAddress);
            Name newBaseAddress = oldBaseAddress.withConstraint(newData);
            buf.renameParameter(0, newBaseAddress);

            Name getFilter = new Name(newData.getterFunction(oldData.fieldCount()), newBaseAddress);
            buf.insertExpression(insPos++, getFilter);
            BasicType oldType = lambdaForm.returnType();
            if (oldType == V_TYPE) {
                MethodType filterType = MethodType.methodType(newType.basicTypeClass());
                callFilter = new Name(filterType, getFilter);
            } else {
                MethodType filterType = MethodType.methodType(newType.basicTypeClass(), oldType.basicTypeClass());
                callFilter = new Name(filterType, getFilter, lambdaForm.names[lambdaForm.result]);
            }
        }

        if (callFilter != null)
            buf.insertExpression(insPos++, callFilter);
        buf.setResult(callFilter);

        form = buf.endEdit();
        return putInCache(key, form);
    }

    LambdaForm collectReturnValueForm(MethodType combinerType) {
        LambdaFormBuffer buf = buffer();
        buf.startEdit();
        int combinerArity = combinerType.parameterCount();
        int argPos = lambdaForm.arity();
        int exprPos = lambdaForm.names.length;

        BoundMethodHandle.SpeciesData oldData = oldSpeciesData();
        BoundMethodHandle.SpeciesData newData = newSpeciesData(L_TYPE);

        // The newly created LF will run with a different BMH.
        // Switch over any pre-existing BMH field references to the new BMH class.
        Name oldBaseAddress = lambdaForm.parameter(0);  // BMH holding the values
        buf.replaceFunctions(oldData.getterFunctions(), newData.getterFunctions(), oldBaseAddress);
        Name newBaseAddress = oldBaseAddress.withConstraint(newData);
        buf.renameParameter(0, newBaseAddress);

        // Now we set up the call to the filter
        Name getCombiner = new Name(newData.getterFunction(oldData.fieldCount()), newBaseAddress);

        Object[] combinerArgs = new Object[combinerArity + 1];
        combinerArgs[0] = getCombiner; // first (synthetic) argument should be the MH that acts as a target of the invoke

        // set up additional adapter parameters (in case the combiner is not a unary function)
        Name[] newParams = new Name[combinerArity - 1]; // last combiner parameter is the return adapter
        for (int i = 0; i < newParams.length; i++) {
            newParams[i] = new Name(argPos + i, basicType(combinerType.parameterType(i)));
        }

        // set up remaining filter parameters to point to the corresponding adapter parameters (see above)
        System.arraycopy(newParams, 0,
                combinerArgs, 1, combinerArity - 1);

        // the last filter argument is set to point at the result of the target method handle
        combinerArgs[combinerArity] = buf.name(lambdaForm.names.length - 1);
        Name callCombiner = new Name(combinerType, combinerArgs);

        // insert the two new expressions
        buf.insertExpression(exprPos, getCombiner);
        buf.insertExpression(exprPos + 1, callCombiner);

        // insert additional arguments
        int insPos = argPos;
        for (Name newParam : newParams) {
            buf.insertParameter(insPos++, newParam);
        }

        buf.setResult(callCombiner);
        return buf.endEdit();
    }

    LambdaForm foldArgumentsForm(int foldPos, boolean dropResult, MethodType combinerType) {
        int combinerArity = combinerType.parameterCount();
        byte kind = (dropResult ? FOLD_ARGS_TO_VOID : FOLD_ARGS);
        TransformKey key = TransformKey.of(kind, foldPos, combinerArity);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity - (kind == FOLD_ARGS ? 1 : 0));
            return form;
        }
        form = makeArgumentCombinationForm(foldPos, combinerType, true, dropResult);
        return putInCache(key, form);
    }

    LambdaForm foldArgumentsForm(int foldPos, boolean dropResult, MethodType combinerType, int ... argPositions) {
        byte kind = (dropResult ? FOLD_SELECT_ARGS_TO_VOID : FOLD_SELECT_ARGS);
        TransformKey key = TransformKey.of(kind, foldPos, argPositions);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity - (kind == FOLD_SELECT_ARGS ? 1 : 0));
            return form;
        }
        form = makeArgumentCombinationForm(foldPos, combinerType, argPositions, true, dropResult);
        return putInCache(key, form);
    }

    LambdaForm filterArgumentsForm(int filterPos, MethodType combinerType, int ... argPositions) {
        TransformKey key = TransformKey.of(FILTER_SELECT_ARGS, filterPos, argPositions);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == lambdaForm.arity);
            return form;
        }
        form = makeArgumentCombinationForm(filterPos, combinerType, argPositions, false, false);
        return putInCache(key, form);
    }

    LambdaForm permuteArgumentsForm(int skip, int[] reorder) {
        assert(skip == 1);  // skip only the leading MH argument, names[0]
        int length = lambdaForm.names.length;
        int outArgs = reorder.length;
        int inTypes = 0;
        boolean nullPerm = true;
        for (int i = 0; i < reorder.length; i++) {
            int inArg = reorder[i];
            if (inArg != i)  nullPerm = false;
            inTypes = Math.max(inTypes, inArg+1);
        }
        assert(skip + reorder.length == lambdaForm.arity);
        if (nullPerm)  return lambdaForm;  // do not bother to cache
        TransformKey key = TransformKey.of(PERMUTE_ARGS, reorder);
        LambdaForm form = getInCache(key);
        if (form != null) {
            assert(form.arity == skip+inTypes) : form;
            return form;
        }

        BasicType[] types = new BasicType[inTypes];
        for (int i = 0; i < outArgs; i++) {
            int inArg = reorder[i];
            types[inArg] = lambdaForm.names[skip + i].type;
        }
        assert (skip + outArgs == lambdaForm.arity);
        assert (permutedTypesMatch(reorder, types, lambdaForm.names, skip));
        int pos = 0;
        while (pos < outArgs && reorder[pos] == pos) {
            pos += 1;
        }
        Name[] names2 = new Name[length - outArgs + inTypes];
        System.arraycopy(lambdaForm.names, 0, names2, 0, skip + pos);
        int bodyLength = length - lambdaForm.arity;
        System.arraycopy(lambdaForm.names, skip + outArgs, names2, skip + inTypes, bodyLength);
        int arity2 = names2.length - bodyLength;
        int result2 = lambdaForm.result;
        if (result2 >= skip) {
            if (result2 < skip + outArgs) {
                result2 = reorder[result2 - skip] + skip;
            } else {
                result2 = result2 - outArgs + inTypes;
            }
        }
        for (int j = pos; j < outArgs; j++) {
            Name n = lambdaForm.names[skip + j];
            int i = reorder[j];
            Name n2 = names2[skip + i];
            if (n2 == null) {
                names2[skip + i] = n2 = new Name(types[i]);
            } else {
                assert (n2.type == types[i]);
            }
            for (int k = arity2; k < names2.length; k++) {
                names2[k] = names2[k].replaceName(n, n2);
            }
        }
        for (int i = skip + pos; i < arity2; i++) {
            if (names2[i] == null) {
                names2[i] = argument(i, types[i - skip]);
            }
        }
        for (int j = lambdaForm.arity; j < lambdaForm.names.length; j++) {
            int i = j - lambdaForm.arity + arity2;
            Name n = lambdaForm.names[j];
            Name n2 = names2[i];
            if (n != n2) {
                for (int k = i + 1; k < names2.length; k++) {
                    names2[k] = names2[k].replaceName(n, n2);
                }
            }
        }

        form = new LambdaForm(arity2, names2, result2);
        return putInCache(key, form);
    }

    LambdaForm noteLoopLocalTypesForm(int pos, BasicType[] localTypes) {
        assert(lambdaForm.isLoop(pos));
        int[] desc = BasicType.basicTypeOrds(localTypes);
        desc = Arrays.copyOf(desc, desc.length + 1);
        desc[desc.length - 1] = pos;
        TransformKey key = TransformKey.of(LOCAL_TYPES, desc);
        LambdaForm form = getInCache(key);
        if (form != null) {
            return form;
        }

        // replace the null entry in the MHImpl.loop invocation with localTypes
        Name invokeLoop = lambdaForm.names[pos + 1];
        assert(invokeLoop.function.equals(MethodHandleImpl.getFunction(NF_loop)));
        Object[] args = Arrays.copyOf(invokeLoop.arguments, invokeLoop.arguments.length);
        assert(args[0] == null);
        args[0] = localTypes;

        LambdaFormBuffer buf = buffer();
        buf.startEdit();
        buf.changeName(pos + 1, new Name(MethodHandleImpl.getFunction(NF_loop), args));
        form = buf.endEdit();

        return putInCache(key, form);
    }

    static boolean permutedTypesMatch(int[] reorder, BasicType[] types, Name[] names, int skip) {
        for (int i = 0; i < reorder.length; i++) {
            assert (names[skip + i].isParam());
            assert (names[skip + i].type == types[reorder[i]]);
        }
        return true;
    }
}
