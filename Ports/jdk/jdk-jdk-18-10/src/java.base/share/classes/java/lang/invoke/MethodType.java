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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.MethodTypeDesc;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import jdk.internal.vm.annotation.Stable;
import sun.invoke.util.BytecodeDescriptor;
import sun.invoke.util.VerifyType;
import sun.invoke.util.Wrapper;
import sun.security.util.SecurityConstants;

import static java.lang.invoke.MethodHandleStatics.UNSAFE;
import static java.lang.invoke.MethodHandleStatics.newIllegalArgumentException;
import static java.lang.invoke.MethodType.fromDescriptor;

/**
 * A method type represents the arguments and return type accepted and
 * returned by a method handle, or the arguments and return type passed
 * and expected  by a method handle caller.  Method types must be properly
 * matched between a method handle and all its callers,
 * and the JVM's operations enforce this matching at, specifically
 * during calls to {@link MethodHandle#invokeExact MethodHandle.invokeExact}
 * and {@link MethodHandle#invoke MethodHandle.invoke}, and during execution
 * of {@code invokedynamic} instructions.
 * <p>
 * The structure is a return type accompanied by any number of parameter types.
 * The types (primitive, {@code void}, and reference) are represented by {@link Class} objects.
 * (For ease of exposition, we treat {@code void} as if it were a type.
 * In fact, it denotes the absence of a return type.)
 * <p>
 * All instances of {@code MethodType} are immutable.
 * Two instances are completely interchangeable if they compare equal.
 * Equality depends on pairwise correspondence of the return and parameter types and on nothing else.
 * <p>
 * This type can be created only by factory methods.
 * All factory methods may cache values, though caching is not guaranteed.
 * Some factory methods are static, while others are virtual methods which
 * modify precursor method types, e.g., by changing a selected parameter.
 * <p>
 * Factory methods which operate on groups of parameter types
 * are systematically presented in two versions, so that both Java arrays and
 * Java lists can be used to work with groups of parameter types.
 * The query methods {@code parameterArray} and {@code parameterList}
 * also provide a choice between arrays and lists.
 * <p>
 * {@code MethodType} objects are sometimes derived from bytecode instructions
 * such as {@code invokedynamic}, specifically from the type descriptor strings associated
 * with the instructions in a class file's constant pool.
 * <p>
 * Like classes and strings, method types can also be represented directly
 * in a class file's constant pool as constants.
 * A method type may be loaded by an {@code ldc} instruction which refers
 * to a suitable {@code CONSTANT_MethodType} constant pool entry.
 * The entry refers to a {@code CONSTANT_Utf8} spelling for the descriptor string.
 * (For full details on method type constants, see sections {@jvms
 * 4.4.8} and {@jvms 5.4.3.5} of the Java Virtual Machine
 * Specification.)
 * <p>
 * When the JVM materializes a {@code MethodType} from a descriptor string,
 * all classes named in the descriptor must be accessible, and will be loaded.
 * (But the classes need not be initialized, as is the case with a {@code CONSTANT_Class}.)
 * This loading may occur at any time before the {@code MethodType} object is first derived.
 * <p>
 * <b><a id="descriptor">Nominal Descriptors</a></b>
 * <p>
 * A {@code MethodType} can be described in {@linkplain MethodTypeDesc nominal form}
 * if and only if all of the parameter types and return type can be described
 * with a {@link Class#describeConstable() nominal descriptor} represented by
 * {@link ClassDesc}.  If a method type can be described nominally, then:
 * <ul>
 * <li>The method type has a {@link MethodTypeDesc nominal descriptor}
 *     returned by {@link #describeConstable() MethodType::describeConstable}.</li>
 * <li>The descriptor string returned by
 *     {@link #descriptorString() MethodType::descriptorString} or
 *     {@link #toMethodDescriptorString() MethodType::toMethodDescriptorString}
 *     for the method type is a method descriptor (JVMS {@jvms 4.3.3}).</li>
 * </ul>
 * <p>
 * If any of the parameter types or return type cannot be described
 * nominally, i.e. {@link Class#describeConstable() Class::describeConstable}
 * returns an empty optional for that type,
 * then the method type cannot be described nominally:
 * <ul>
 * <li>The method type has no {@link MethodTypeDesc nominal descriptor} and
 *     {@link #describeConstable() MethodType::describeConstable} returns
 *     an empty optional.</li>
 * <li>The descriptor string returned by
 *     {@link #descriptorString() MethodType::descriptorString} or
 *     {@link #toMethodDescriptorString() MethodType::toMethodDescriptorString}
 *     for the method type is not a type descriptor.</li>
 * </ul>
 *
 * @author John Rose, JSR 292 EG
 * @since 1.7
 */
public final
class MethodType
        implements Constable,
                   TypeDescriptor.OfMethod<Class<?>, MethodType>,
                   java.io.Serializable {
    @java.io.Serial
    private static final long serialVersionUID = 292L;  // {rtype, {ptype...}}

    // The rtype and ptypes fields define the structural identity of the method type:
    private final @Stable Class<?>   rtype;
    private final @Stable Class<?>[] ptypes;

    // The remaining fields are caches of various sorts:
    private @Stable MethodTypeForm form; // erased form, plus cached data about primitives
    private @Stable Object wrapAlt;  // alternative wrapped/unwrapped version and
                                     // private communication for readObject and readResolve
    private @Stable Invokers invokers;   // cache of handy higher-order adapters
    private @Stable String methodDescriptor;  // cache for toMethodDescriptorString

    /**
     * Constructor that performs no copying or validation.
     * Should only be called from the factory method makeImpl
     */
    private MethodType(Class<?> rtype, Class<?>[] ptypes) {
        this.rtype = rtype;
        this.ptypes = ptypes;
    }

    /*trusted*/ MethodTypeForm form() { return form; }
    /*trusted*/ Class<?> rtype() { return rtype; }
    /*trusted*/ Class<?>[] ptypes() { return ptypes; }

    void setForm(MethodTypeForm f) { form = f; }

    /** This number, mandated by the JVM spec as 255,
     *  is the maximum number of <em>slots</em>
     *  that any Java method can receive in its argument list.
     *  It limits both JVM signatures and method type objects.
     *  The longest possible invocation will look like
     *  {@code staticMethod(arg1, arg2, ..., arg255)} or
     *  {@code x.virtualMethod(arg1, arg2, ..., arg254)}.
     */
    /*non-public*/
    static final int MAX_JVM_ARITY = 255;  // this is mandated by the JVM spec.

    /** This number is the maximum arity of a method handle, 254.
     *  It is derived from the absolute JVM-imposed arity by subtracting one,
     *  which is the slot occupied by the method handle itself at the
     *  beginning of the argument list used to invoke the method handle.
     *  The longest possible invocation will look like
     *  {@code mh.invoke(arg1, arg2, ..., arg254)}.
     */
    // Issue:  Should we allow MH.invokeWithArguments to go to the full 255?
    /*non-public*/
    static final int MAX_MH_ARITY = MAX_JVM_ARITY-1;  // deduct one for mh receiver

    /** This number is the maximum arity of a method handle invoker, 253.
     *  It is derived from the absolute JVM-imposed arity by subtracting two,
     *  which are the slots occupied by invoke method handle, and the
     *  target method handle, which are both at the beginning of the argument
     *  list used to invoke the target method handle.
     *  The longest possible invocation will look like
     *  {@code invokermh.invoke(targetmh, arg1, arg2, ..., arg253)}.
     */
    /*non-public*/
    static final int MAX_MH_INVOKER_ARITY = MAX_MH_ARITY-1;  // deduct one more for invoker

    /** Return number of extra slots (count of long/double args). */
    private static int checkPtypes(Class<?>[] ptypes) {
        int slots = 0;
        for (Class<?> ptype : ptypes) {
            Objects.requireNonNull(ptype);
            if (ptype == void.class)
                throw newIllegalArgumentException("parameter type cannot be void");
            if (ptype == double.class || ptype == long.class) {
                slots++;
            }
        }
        checkSlotCount(ptypes.length + slots);
        return slots;
    }

    static {
        // MAX_JVM_ARITY must be power of 2 minus 1 for following code trick to work:
        assert((MAX_JVM_ARITY & (MAX_JVM_ARITY+1)) == 0);
    }
    static void checkSlotCount(int count) {
        if ((count & MAX_JVM_ARITY) != count)
            throw newIllegalArgumentException("bad parameter count "+count);
    }
    private static IndexOutOfBoundsException newIndexOutOfBoundsException(Object num) {
        if (num instanceof Integer)  num = "bad index: "+num;
        return new IndexOutOfBoundsException(num.toString());
    }

    static final ConcurrentWeakInternSet<MethodType> internTable = new ConcurrentWeakInternSet<>();

    static final Class<?>[] NO_PTYPES = {};

    /**
     * Finds or creates an instance of the given method type.
     * @param rtype  the return type
     * @param ptypes the parameter types
     * @return a method type with the given components
     * @throws NullPointerException if {@code rtype} or {@code ptypes} or any element of {@code ptypes} is null
     * @throws IllegalArgumentException if any element of {@code ptypes} is {@code void.class}
     */
    public static MethodType methodType(Class<?> rtype, Class<?>[] ptypes) {
        return makeImpl(rtype, ptypes, false);
    }

    /**
     * Finds or creates a method type with the given components.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param rtype  the return type
     * @param ptypes the parameter types
     * @return a method type with the given components
     * @throws NullPointerException if {@code rtype} or {@code ptypes} or any element of {@code ptypes} is null
     * @throws IllegalArgumentException if any element of {@code ptypes} is {@code void.class}
     */
    public static MethodType methodType(Class<?> rtype, List<Class<?>> ptypes) {
        boolean notrust = false;  // random List impl. could return evil ptypes array
        return makeImpl(rtype, listToArray(ptypes), notrust);
    }

    private static Class<?>[] listToArray(List<Class<?>> ptypes) {
        // sanity check the size before the toArray call, since size might be huge
        checkSlotCount(ptypes.size());
        return ptypes.toArray(NO_PTYPES);
    }

    /**
     * Finds or creates a method type with the given components.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * The leading parameter type is prepended to the remaining array.
     * @param rtype  the return type
     * @param ptype0 the first parameter type
     * @param ptypes the remaining parameter types
     * @return a method type with the given components
     * @throws NullPointerException if {@code rtype} or {@code ptype0} or {@code ptypes} or any element of {@code ptypes} is null
     * @throws IllegalArgumentException if {@code ptype0} or {@code ptypes} or any element of {@code ptypes} is {@code void.class}
     */
    public static MethodType methodType(Class<?> rtype, Class<?> ptype0, Class<?>... ptypes) {
        Class<?>[] ptypes1 = new Class<?>[1+ptypes.length];
        ptypes1[0] = ptype0;
        System.arraycopy(ptypes, 0, ptypes1, 1, ptypes.length);
        return makeImpl(rtype, ptypes1, true);
    }

    /**
     * Finds or creates a method type with the given components.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * The resulting method has no parameter types.
     * @param rtype  the return type
     * @return a method type with the given return value
     * @throws NullPointerException if {@code rtype} is null
     */
    public static MethodType methodType(Class<?> rtype) {
        return makeImpl(rtype, NO_PTYPES, true);
    }

    /**
     * Finds or creates a method type with the given components.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * The resulting method has the single given parameter type.
     * @param rtype  the return type
     * @param ptype0 the parameter type
     * @return a method type with the given return value and parameter type
     * @throws NullPointerException if {@code rtype} or {@code ptype0} is null
     * @throws IllegalArgumentException if {@code ptype0} is {@code void.class}
     */
    public static MethodType methodType(Class<?> rtype, Class<?> ptype0) {
        return makeImpl(rtype, new Class<?>[]{ ptype0 }, true);
    }

    /**
     * Finds or creates a method type with the given components.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * The resulting method has the same parameter types as {@code ptypes},
     * and the specified return type.
     * @param rtype  the return type
     * @param ptypes the method type which supplies the parameter types
     * @return a method type with the given components
     * @throws NullPointerException if {@code rtype} or {@code ptypes} is null
     */
    public static MethodType methodType(Class<?> rtype, MethodType ptypes) {
        return makeImpl(rtype, ptypes.ptypes, true);
    }

    /**
     * Sole factory method to find or create an interned method type. Will perform
     * input validation on behalf of factory methods
     *
     * @param rtype desired return type
     * @param ptypes desired parameter types
     * @param trusted whether the ptypes can be used without cloning
     * @throws NullPointerException if {@code rtype} or {@code ptypes} or any element of {@code ptypes} is null
     * @throws IllegalArgumentException if any element of {@code ptypes} is {@code void.class}
     * @return the unique method type of the desired structure
     */
    /*trusted*/
    static MethodType makeImpl(Class<?> rtype, Class<?>[] ptypes, boolean trusted) {
        if (ptypes.length == 0) {
            ptypes = NO_PTYPES; trusted = true;
        }
        MethodType primordialMT = new MethodType(rtype, ptypes);
        MethodType mt = internTable.get(primordialMT);
        if (mt != null)
            return mt;

        // promote the object to the Real Thing, and reprobe
        Objects.requireNonNull(rtype);
        if (trusted) {
            MethodType.checkPtypes(ptypes);
            mt = primordialMT;
        } else {
            // Make defensive copy then validate
            ptypes = Arrays.copyOf(ptypes, ptypes.length);
            MethodType.checkPtypes(ptypes);
            mt = new MethodType(rtype, ptypes);
        }
        mt.form = MethodTypeForm.findForm(mt);
        return internTable.add(mt);
    }
    private static final @Stable MethodType[] objectOnlyTypes = new MethodType[20];

    /**
     * Finds or creates a method type whose components are {@code Object} with an optional trailing {@code Object[]} array.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * All parameters and the return type will be {@code Object},
     * except the final array parameter if any, which will be {@code Object[]}.
     * @param objectArgCount number of parameters (excluding the final array parameter if any)
     * @param finalArray whether there will be a trailing array parameter, of type {@code Object[]}
     * @return a generally applicable method type, for all calls of the given fixed argument count and a collected array of further arguments
     * @throws IllegalArgumentException if {@code objectArgCount} is negative or greater than 255 (or 254, if {@code finalArray} is true)
     * @see #genericMethodType(int)
     */
    public static MethodType genericMethodType(int objectArgCount, boolean finalArray) {
        MethodType mt;
        checkSlotCount(objectArgCount);
        int ivarargs = (!finalArray ? 0 : 1);
        int ootIndex = objectArgCount*2 + ivarargs;
        if (ootIndex < objectOnlyTypes.length) {
            mt = objectOnlyTypes[ootIndex];
            if (mt != null)  return mt;
        }
        Class<?>[] ptypes = new Class<?>[objectArgCount + ivarargs];
        Arrays.fill(ptypes, Object.class);
        if (ivarargs != 0)  ptypes[objectArgCount] = Object[].class;
        mt = makeImpl(Object.class, ptypes, true);
        if (ootIndex < objectOnlyTypes.length) {
            objectOnlyTypes[ootIndex] = mt;     // cache it here also!
        }
        return mt;
    }

    /**
     * Finds or creates a method type whose components are all {@code Object}.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * All parameters and the return type will be Object.
     * @param objectArgCount number of parameters
     * @return a generally applicable method type, for all calls of the given argument count
     * @throws IllegalArgumentException if {@code objectArgCount} is negative or greater than 255
     * @see #genericMethodType(int, boolean)
     */
    public static MethodType genericMethodType(int objectArgCount) {
        return genericMethodType(objectArgCount, false);
    }

    /**
     * Finds or creates a method type with a single different parameter type.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param num    the index (zero-based) of the parameter type to change
     * @param nptype a new parameter type to replace the old one with
     * @return the same type, except with the selected parameter changed
     * @throws IndexOutOfBoundsException if {@code num} is not a valid index into {@code parameterArray()}
     * @throws IllegalArgumentException if {@code nptype} is {@code void.class}
     * @throws NullPointerException if {@code nptype} is null
     */
    public MethodType changeParameterType(int num, Class<?> nptype) {
        if (parameterType(num) == nptype)  return this;
        Class<?>[] nptypes = ptypes.clone();
        nptypes[num] = nptype;
        return makeImpl(rtype, nptypes, true);
    }

    /**
     * Finds or creates a method type with additional parameter types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param num    the position (zero-based) of the inserted parameter type(s)
     * @param ptypesToInsert zero or more new parameter types to insert into the parameter list
     * @return the same type, except with the selected parameter(s) inserted
     * @throws IndexOutOfBoundsException if {@code num} is negative or greater than {@code parameterCount()}
     * @throws IllegalArgumentException if any element of {@code ptypesToInsert} is {@code void.class}
     *                                  or if the resulting method type would have more than 255 parameter slots
     * @throws NullPointerException if {@code ptypesToInsert} or any of its elements is null
     */
    public MethodType insertParameterTypes(int num, Class<?>... ptypesToInsert) {
        int len = ptypes.length;
        if (num < 0 || num > len)
            throw newIndexOutOfBoundsException(num);
        int ins = checkPtypes(ptypesToInsert);
        checkSlotCount(parameterSlotCount() + ptypesToInsert.length + ins);
        int ilen = ptypesToInsert.length;
        if (ilen == 0)  return this;
        Class<?>[] nptypes = new Class<?>[len + ilen];
        if (num > 0) {
            System.arraycopy(ptypes, 0, nptypes, 0, num);
        }
        System.arraycopy(ptypesToInsert, 0, nptypes, num, ilen);
        if (num < len) {
            System.arraycopy(ptypes, num, nptypes, num+ilen, len-num);
        }
        return makeImpl(rtype, nptypes, true);
    }

    /**
     * Finds or creates a method type with additional parameter types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param ptypesToInsert zero or more new parameter types to insert after the end of the parameter list
     * @return the same type, except with the selected parameter(s) appended
     * @throws IllegalArgumentException if any element of {@code ptypesToInsert} is {@code void.class}
     *                                  or if the resulting method type would have more than 255 parameter slots
     * @throws NullPointerException if {@code ptypesToInsert} or any of its elements is null
     */
    public MethodType appendParameterTypes(Class<?>... ptypesToInsert) {
        return insertParameterTypes(parameterCount(), ptypesToInsert);
    }

    /**
     * Finds or creates a method type with additional parameter types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param num    the position (zero-based) of the inserted parameter type(s)
     * @param ptypesToInsert zero or more new parameter types to insert into the parameter list
     * @return the same type, except with the selected parameter(s) inserted
     * @throws IndexOutOfBoundsException if {@code num} is negative or greater than {@code parameterCount()}
     * @throws IllegalArgumentException if any element of {@code ptypesToInsert} is {@code void.class}
     *                                  or if the resulting method type would have more than 255 parameter slots
     * @throws NullPointerException if {@code ptypesToInsert} or any of its elements is null
     */
    public MethodType insertParameterTypes(int num, List<Class<?>> ptypesToInsert) {
        return insertParameterTypes(num, listToArray(ptypesToInsert));
    }

    /**
     * Finds or creates a method type with additional parameter types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param ptypesToInsert zero or more new parameter types to insert after the end of the parameter list
     * @return the same type, except with the selected parameter(s) appended
     * @throws IllegalArgumentException if any element of {@code ptypesToInsert} is {@code void.class}
     *                                  or if the resulting method type would have more than 255 parameter slots
     * @throws NullPointerException if {@code ptypesToInsert} or any of its elements is null
     */
    public MethodType appendParameterTypes(List<Class<?>> ptypesToInsert) {
        return insertParameterTypes(parameterCount(), ptypesToInsert);
    }

    /**
     * Finds or creates a method type with modified parameter types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param start  the position (zero-based) of the first replaced parameter type(s)
     * @param end    the position (zero-based) after the last replaced parameter type(s)
     * @param ptypesToInsert zero or more new parameter types to insert into the parameter list
     * @return the same type, except with the selected parameter(s) replaced
     * @throws IndexOutOfBoundsException if {@code start} is negative or greater than {@code parameterCount()}
     *                                  or if {@code end} is negative or greater than {@code parameterCount()}
     *                                  or if {@code start} is greater than {@code end}
     * @throws IllegalArgumentException if any element of {@code ptypesToInsert} is {@code void.class}
     *                                  or if the resulting method type would have more than 255 parameter slots
     * @throws NullPointerException if {@code ptypesToInsert} or any of its elements is null
     */
    /*non-public*/
    MethodType replaceParameterTypes(int start, int end, Class<?>... ptypesToInsert) {
        if (start == end)
            return insertParameterTypes(start, ptypesToInsert);
        int len = ptypes.length;
        if (!(0 <= start && start <= end && end <= len))
            throw newIndexOutOfBoundsException("start="+start+" end="+end);
        int ilen = ptypesToInsert.length;
        if (ilen == 0)
            return dropParameterTypes(start, end);
        return dropParameterTypes(start, end).insertParameterTypes(start, ptypesToInsert);
    }

    /** Replace the last arrayLength parameter types with the component type of arrayType.
     * @param arrayType any array type
     * @param pos position at which to spread
     * @param arrayLength the number of parameter types to change
     * @return the resulting type
     */
    /*non-public*/
    MethodType asSpreaderType(Class<?> arrayType, int pos, int arrayLength) {
        assert(parameterCount() >= arrayLength);
        int spreadPos = pos;
        if (arrayLength == 0)  return this;  // nothing to change
        if (arrayType == Object[].class) {
            if (isGeneric())  return this;  // nothing to change
            if (spreadPos == 0) {
                // no leading arguments to preserve; go generic
                MethodType res = genericMethodType(arrayLength);
                if (rtype != Object.class) {
                    res = res.changeReturnType(rtype);
                }
                return res;
            }
        }
        Class<?> elemType = arrayType.getComponentType();
        assert(elemType != null);
        for (int i = spreadPos; i < spreadPos + arrayLength; i++) {
            if (ptypes[i] != elemType) {
                Class<?>[] fixedPtypes = ptypes.clone();
                Arrays.fill(fixedPtypes, i, spreadPos + arrayLength, elemType);
                return methodType(rtype, fixedPtypes);
            }
        }
        return this;  // arguments check out; no change
    }

    /** Return the leading parameter type, which must exist and be a reference.
     *  @return the leading parameter type, after error checks
     */
    /*non-public*/
    Class<?> leadingReferenceParameter() {
        Class<?> ptype;
        if (ptypes.length == 0 ||
            (ptype = ptypes[0]).isPrimitive())
            throw newIllegalArgumentException("no leading reference parameter");
        return ptype;
    }

    /** Delete the last parameter type and replace it with arrayLength copies of the component type of arrayType.
     * @param arrayType any array type
     * @param pos position at which to insert parameters
     * @param arrayLength the number of parameter types to insert
     * @return the resulting type
     */
    /*non-public*/
    MethodType asCollectorType(Class<?> arrayType, int pos, int arrayLength) {
        assert(parameterCount() >= 1);
        assert(pos < ptypes.length);
        assert(ptypes[pos].isAssignableFrom(arrayType));
        MethodType res;
        if (arrayType == Object[].class) {
            res = genericMethodType(arrayLength);
            if (rtype != Object.class) {
                res = res.changeReturnType(rtype);
            }
        } else {
            Class<?> elemType = arrayType.getComponentType();
            assert(elemType != null);
            res = methodType(rtype, Collections.nCopies(arrayLength, elemType));
        }
        if (ptypes.length == 1) {
            return res;
        } else {
            // insert after (if need be), then before
            if (pos < ptypes.length - 1) {
                res = res.insertParameterTypes(arrayLength, Arrays.copyOfRange(ptypes, pos + 1, ptypes.length));
            }
            return res.insertParameterTypes(0, Arrays.copyOf(ptypes, pos));
        }
    }

    /**
     * Finds or creates a method type with some parameter types omitted.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param start  the index (zero-based) of the first parameter type to remove
     * @param end    the index (greater than {@code start}) of the first parameter type after not to remove
     * @return the same type, except with the selected parameter(s) removed
     * @throws IndexOutOfBoundsException if {@code start} is negative or greater than {@code parameterCount()}
     *                                  or if {@code end} is negative or greater than {@code parameterCount()}
     *                                  or if {@code start} is greater than {@code end}
     */
    public MethodType dropParameterTypes(int start, int end) {
        int len = ptypes.length;
        if (!(0 <= start && start <= end && end <= len))
            throw newIndexOutOfBoundsException("start="+start+" end="+end);
        if (start == end)  return this;
        Class<?>[] nptypes;
        if (start == 0) {
            if (end == len) {
                // drop all parameters
                nptypes = NO_PTYPES;
            } else {
                // drop initial parameter(s)
                nptypes = Arrays.copyOfRange(ptypes, end, len);
            }
        } else {
            if (end == len) {
                // drop trailing parameter(s)
                nptypes = Arrays.copyOfRange(ptypes, 0, start);
            } else {
                int tail = len - end;
                nptypes = Arrays.copyOfRange(ptypes, 0, start + tail);
                System.arraycopy(ptypes, end, nptypes, start, tail);
            }
        }
        return makeImpl(rtype, nptypes, true);
    }

    /**
     * Finds or creates a method type with a different return type.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * @param nrtype a return parameter type to replace the old one with
     * @return the same type, except with the return type change
     * @throws NullPointerException if {@code nrtype} is null
     */
    public MethodType changeReturnType(Class<?> nrtype) {
        if (returnType() == nrtype)  return this;
        return makeImpl(nrtype, ptypes, true);
    }

    /**
     * Reports if this type contains a primitive argument or return value.
     * The return type {@code void} counts as a primitive.
     * @return true if any of the types are primitives
     */
    public boolean hasPrimitives() {
        return form.hasPrimitives();
    }

    /**
     * Reports if this type contains a wrapper argument or return value.
     * Wrappers are types which box primitive values, such as {@link Integer}.
     * The reference type {@code java.lang.Void} counts as a wrapper,
     * if it occurs as a return type.
     * @return true if any of the types are wrappers
     */
    public boolean hasWrappers() {
        return unwrap() != this;
    }

    /**
     * Erases all reference types to {@code Object}.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * All primitive types (including {@code void}) will remain unchanged.
     * @return a version of the original type with all reference types replaced
     */
    public MethodType erase() {
        return form.erasedType();
    }

    /**
     * Erases all reference types to {@code Object}, and all subword types to {@code int}.
     * This is the reduced type polymorphism used by private methods
     * such as {@link MethodHandle#invokeBasic invokeBasic}.
     * @return a version of the original type with all reference and subword types replaced
     */
    /*non-public*/
    MethodType basicType() {
        return form.basicType();
    }

    private static final @Stable Class<?>[] METHOD_HANDLE_ARRAY
            = new Class<?>[] { MethodHandle.class };

    /**
     * @return a version of the original type with MethodHandle prepended as the first argument
     */
    /*non-public*/
    MethodType invokerType() {
        return insertParameterTypes(0, METHOD_HANDLE_ARRAY);
    }

    /**
     * Converts all types, both reference and primitive, to {@code Object}.
     * Convenience method for {@link #genericMethodType(int) genericMethodType}.
     * The expression {@code type.wrap().erase()} produces the same value
     * as {@code type.generic()}.
     * @return a version of the original type with all types replaced
     */
    public MethodType generic() {
        return genericMethodType(parameterCount());
    }

    /*non-public*/
    boolean isGeneric() {
        return this == erase() && !hasPrimitives();
    }

    /**
     * Converts all primitive types to their corresponding wrapper types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * All reference types (including wrapper types) will remain unchanged.
     * A {@code void} return type is changed to the type {@code java.lang.Void}.
     * The expression {@code type.wrap().erase()} produces the same value
     * as {@code type.generic()}.
     * @return a version of the original type with all primitive types replaced
     */
    public MethodType wrap() {
        return hasPrimitives() ? wrapWithPrims(this) : this;
    }

    /**
     * Converts all wrapper types to their corresponding primitive types.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * All primitive types (including {@code void}) will remain unchanged.
     * A return type of {@code java.lang.Void} is changed to {@code void}.
     * @return a version of the original type with all wrapper types replaced
     */
    public MethodType unwrap() {
        MethodType noprims = !hasPrimitives() ? this : wrapWithPrims(this);
        return unwrapWithNoPrims(noprims);
    }

    private static MethodType wrapWithPrims(MethodType pt) {
        assert(pt.hasPrimitives());
        MethodType wt = (MethodType)pt.wrapAlt;
        if (wt == null) {
            // fill in lazily
            wt = MethodTypeForm.canonicalize(pt, MethodTypeForm.WRAP);
            assert(wt != null);
            pt.wrapAlt = wt;
        }
        return wt;
    }

    private static MethodType unwrapWithNoPrims(MethodType wt) {
        assert(!wt.hasPrimitives());
        MethodType uwt = (MethodType)wt.wrapAlt;
        if (uwt == null) {
            // fill in lazily
            uwt = MethodTypeForm.canonicalize(wt, MethodTypeForm.UNWRAP);
            if (uwt == null)
                uwt = wt;    // type has no wrappers or prims at all
            wt.wrapAlt = uwt;
        }
        return uwt;
    }

    /**
     * Returns the parameter type at the specified index, within this method type.
     * @param num the index (zero-based) of the desired parameter type
     * @return the selected parameter type
     * @throws IndexOutOfBoundsException if {@code num} is not a valid index into {@code parameterArray()}
     */
    public Class<?> parameterType(int num) {
        return ptypes[num];
    }
    /**
     * Returns the number of parameter types in this method type.
     * @return the number of parameter types
     */
    public int parameterCount() {
        return ptypes.length;
    }
    /**
     * Returns the return type of this method type.
     * @return the return type
     */
    public Class<?> returnType() {
        return rtype;
    }

    /**
     * Presents the parameter types as a list (a convenience method).
     * The list will be immutable.
     * @return the parameter types (as an immutable list)
     */
    public List<Class<?>> parameterList() {
        return Collections.unmodifiableList(Arrays.asList(ptypes.clone()));
    }

    /**
     * Returns the last parameter type of this method type.
     * If this type has no parameters, the sentinel value
     * {@code void.class} is returned instead.
     * @apiNote
     * <p>
     * The sentinel value is chosen so that reflective queries can be
     * made directly against the result value.
     * The sentinel value cannot be confused with a real parameter,
     * since {@code void} is never acceptable as a parameter type.
     * For variable arity invocation modes, the expression
     * {@link Class#getComponentType lastParameterType().getComponentType()}
     * is useful to query the type of the "varargs" parameter.
     * @return the last parameter type if any, else {@code void.class}
     * @since 10
     */
    public Class<?> lastParameterType() {
        int len = ptypes.length;
        return len == 0 ? void.class : ptypes[len-1];
    }

    /**
     * Presents the parameter types as an array (a convenience method).
     * Changes to the array will not result in changes to the type.
     * @return the parameter types (as a fresh copy if necessary)
     */
    public Class<?>[] parameterArray() {
        return ptypes.clone();
    }

    /**
     * Compares the specified object with this type for equality.
     * That is, it returns {@code true} if and only if the specified object
     * is also a method type with exactly the same parameters and return type.
     * @param x object to compare
     * @see Object#equals(Object)
     */
    // This implementation may also return true if x is a WeakEntry containing
    // a method type that is equal to this. This is an internal implementation
    // detail to allow for faster method type lookups.
    // See ConcurrentWeakInternSet.WeakEntry#equals(Object)
    @Override
    public boolean equals(Object x) {
        if (this == x) {
            return true;
        }
        if (x instanceof MethodType) {
            return equals((MethodType)x);
        }
        if (x instanceof ConcurrentWeakInternSet.WeakEntry) {
            Object o = ((ConcurrentWeakInternSet.WeakEntry)x).get();
            if (o instanceof MethodType) {
                return equals((MethodType)o);
            }
        }
        return false;
    }

    private boolean equals(MethodType that) {
        return this.rtype == that.rtype
            && Arrays.equals(this.ptypes, that.ptypes);
    }

    /**
     * Returns the hash code value for this method type.
     * It is defined to be the same as the hashcode of a List
     * whose elements are the return type followed by the
     * parameter types.
     * @return the hash code value for this method type
     * @see Object#hashCode()
     * @see #equals(Object)
     * @see List#hashCode()
     */
    @Override
    public int hashCode() {
        int hashCode = 31 + rtype.hashCode();
        for (Class<?> ptype : ptypes)
            hashCode = 31 * hashCode + ptype.hashCode();
        return hashCode;
    }

    /**
     * Returns a string representation of the method type,
     * of the form {@code "(PT0,PT1...)RT"}.
     * The string representation of a method type is a
     * parenthesis enclosed, comma separated list of type names,
     * followed immediately by the return type.
     * <p>
     * Each type is represented by its
     * {@link java.lang.Class#getSimpleName simple name}.
     */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(",", "(",
                ")" + rtype.getSimpleName());
        for (int i = 0; i < ptypes.length; i++) {
            sj.add(ptypes[i].getSimpleName());
        }
        return sj.toString();
    }

    /** True if my parameter list is effectively identical to the given full list,
     *  after skipping the given number of my own initial parameters.
     *  In other words, after disregarding {@code skipPos} parameters,
     *  my remaining parameter list is no longer than the {@code fullList}, and
     *  is equal to the same-length initial sublist of {@code fullList}.
     */
    /*non-public*/
    boolean effectivelyIdenticalParameters(int skipPos, List<Class<?>> fullList) {
        int myLen = ptypes.length, fullLen = fullList.size();
        if (skipPos > myLen || myLen - skipPos > fullLen)
            return false;
        List<Class<?>> myList = Arrays.asList(ptypes);
        if (skipPos != 0) {
            myList = myList.subList(skipPos, myLen);
            myLen -= skipPos;
        }
        if (fullLen == myLen)
            return myList.equals(fullList);
        else
            return myList.equals(fullList.subList(0, myLen));
    }

    /** True if the old return type can always be viewed (w/o casting) under new return type,
     *  and the new parameters can be viewed (w/o casting) under the old parameter types.
     */
    /*non-public*/
    boolean isViewableAs(MethodType newType, boolean keepInterfaces) {
        if (!VerifyType.isNullConversion(returnType(), newType.returnType(), keepInterfaces))
            return false;
        if (form == newType.form && form.erasedType == this)
            return true;  // my reference parameters are all Object
        if (ptypes == newType.ptypes)
            return true;
        int argc = parameterCount();
        if (argc != newType.parameterCount())
            return false;
        for (int i = 0; i < argc; i++) {
            if (!VerifyType.isNullConversion(newType.parameterType(i), parameterType(i), keepInterfaces))
                return false;
        }
        return true;
    }
    /*non-public*/
    boolean isConvertibleTo(MethodType newType) {
        MethodTypeForm oldForm = this.form();
        MethodTypeForm newForm = newType.form();
        if (oldForm == newForm)
            // same parameter count, same primitive/object mix
            return true;
        if (!canConvert(returnType(), newType.returnType()))
            return false;
        Class<?>[] srcTypes = newType.ptypes;
        Class<?>[] dstTypes = ptypes;
        if (srcTypes == dstTypes)
            return true;
        int argc;
        if ((argc = srcTypes.length) != dstTypes.length)
            return false;
        if (argc <= 1) {
            if (argc == 1 && !canConvert(srcTypes[0], dstTypes[0]))
                return false;
            return true;
        }
        if ((!oldForm.hasPrimitives() && oldForm.erasedType == this) ||
            (!newForm.hasPrimitives() && newForm.erasedType == newType)) {
            // Somewhat complicated test to avoid a loop of 2 or more trips.
            // If either type has only Object parameters, we know we can convert.
            assert(canConvertParameters(srcTypes, dstTypes));
            return true;
        }
        return canConvertParameters(srcTypes, dstTypes);
    }

    /** Returns true if MHs.explicitCastArguments produces the same result as MH.asType.
     *  If the type conversion is impossible for either, the result should be false.
     */
    /*non-public*/
    boolean explicitCastEquivalentToAsType(MethodType newType) {
        if (this == newType)  return true;
        if (!explicitCastEquivalentToAsType(rtype, newType.rtype)) {
            return false;
        }
        Class<?>[] srcTypes = newType.ptypes;
        Class<?>[] dstTypes = ptypes;
        if (dstTypes == srcTypes) {
            return true;
        }
        assert(dstTypes.length == srcTypes.length);
        for (int i = 0; i < dstTypes.length; i++) {
            if (!explicitCastEquivalentToAsType(srcTypes[i], dstTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /** Reports true if the src can be converted to the dst, by both asType and MHs.eCE,
     *  and with the same effect.
     *  MHs.eCA has the following "upgrades" to MH.asType:
     *  1. interfaces are unchecked (that is, treated as if aliased to Object)
     *     Therefore, {@code Object->CharSequence} is possible in both cases but has different semantics
     *  2. the full matrix of primitive-to-primitive conversions is supported
     *     Narrowing like {@code long->byte} and basic-typing like {@code boolean->int}
     *     are not supported by asType, but anything supported by asType is equivalent
     *     with MHs.eCE.
     *  3a. unboxing conversions can be followed by the full matrix of primitive conversions
     *  3b. unboxing of null is permitted (creates a zero primitive value)
     * Other than interfaces, reference-to-reference conversions are the same.
     * Boxing primitives to references is the same for both operators.
     */
    private static boolean explicitCastEquivalentToAsType(Class<?> src, Class<?> dst) {
        if (src == dst || dst == Object.class || dst == void.class)  return true;
        if (src.isPrimitive()) {
            // Could be a prim/prim conversion, where casting is a strict superset.
            // Or a boxing conversion, which is always to an exact wrapper class.
            return canConvert(src, dst);
        } else if (dst.isPrimitive()) {
            // Unboxing behavior is different between MHs.eCA & MH.asType (see 3b).
            return false;
        } else {
            // R->R always works, but we have to avoid a check-cast to an interface.
            return !dst.isInterface() || dst.isAssignableFrom(src);
        }
    }

    private boolean canConvertParameters(Class<?>[] srcTypes, Class<?>[] dstTypes) {
        for (int i = 0; i < srcTypes.length; i++) {
            if (!canConvert(srcTypes[i], dstTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /*non-public*/
    static boolean canConvert(Class<?> src, Class<?> dst) {
        // short-circuit a few cases:
        if (src == dst || src == Object.class || dst == Object.class)  return true;
        // the remainder of this logic is documented in MethodHandle.asType
        if (src.isPrimitive()) {
            // can force void to an explicit null, a la reflect.Method.invoke
            // can also force void to a primitive zero, by analogy
            if (src == void.class)  return true;  //or !dst.isPrimitive()?
            Wrapper sw = Wrapper.forPrimitiveType(src);
            if (dst.isPrimitive()) {
                // P->P must widen
                return Wrapper.forPrimitiveType(dst).isConvertibleFrom(sw);
            } else {
                // P->R must box and widen
                return dst.isAssignableFrom(sw.wrapperType());
            }
        } else if (dst.isPrimitive()) {
            // any value can be dropped
            if (dst == void.class)  return true;
            Wrapper dw = Wrapper.forPrimitiveType(dst);
            // R->P must be able to unbox (from a dynamically chosen type) and widen
            // For example:
            //   Byte/Number/Comparable/Object -> dw:Byte -> byte.
            //   Character/Comparable/Object -> dw:Character -> char
            //   Boolean/Comparable/Object -> dw:Boolean -> boolean
            // This means that dw must be cast-compatible with src.
            if (src.isAssignableFrom(dw.wrapperType())) {
                return true;
            }
            // The above does not work if the source reference is strongly typed
            // to a wrapper whose primitive must be widened.  For example:
            //   Byte -> unbox:byte -> short/int/long/float/double
            //   Character -> unbox:char -> int/long/float/double
            if (Wrapper.isWrapperType(src) &&
                dw.isConvertibleFrom(Wrapper.forWrapperType(src))) {
                // can unbox from src and then widen to dst
                return true;
            }
            // We have already covered cases which arise due to runtime unboxing
            // of a reference type which covers several wrapper types:
            //   Object -> cast:Integer -> unbox:int -> long/float/double
            //   Serializable -> cast:Byte -> unbox:byte -> byte/short/int/long/float/double
            // An marginal case is Number -> dw:Character -> char, which would be OK if there were a
            // subclass of Number which wraps a value that can convert to char.
            // Since there is none, we don't need an extra check here to cover char or boolean.
            return false;
        } else {
            // R->R always works, since null is always valid dynamically
            return true;
        }
    }

    /// Queries which have to do with the bytecode architecture

    /** Reports the number of JVM stack slots required to invoke a method
     * of this type.  Note that (for historical reasons) the JVM requires
     * a second stack slot to pass long and double arguments.
     * So this method returns {@link #parameterCount() parameterCount} plus the
     * number of long and double parameters (if any).
     * <p>
     * This method is included for the benefit of applications that must
     * generate bytecodes that process method handles and invokedynamic.
     * @return the number of JVM stack slots for this type's parameters
     */
    /*non-public*/
    int parameterSlotCount() {
        return form.parameterSlotCount();
    }

    /*non-public*/
    Invokers invokers() {
        Invokers inv = invokers;
        if (inv != null)  return inv;
        invokers = inv = new Invokers(this);
        return inv;
    }

    /**
     * Finds or creates an instance of a method type, given the spelling of its bytecode descriptor.
     * Convenience method for {@link #methodType(java.lang.Class, java.lang.Class[]) methodType}.
     * Any class or interface name embedded in the descriptor string will be
     * resolved by the given loader (or if it is null, on the system class loader).
     * <p>
     * Note that it is possible to encounter method types which cannot be
     * constructed by this method, because their component types are
     * not all reachable from a common class loader.
     * <p>
     * This method is included for the benefit of applications that must
     * generate bytecodes that process method handles and {@code invokedynamic}.
     * @param descriptor a bytecode-level type descriptor string "(T...)T"
     * @param loader the class loader in which to look up the types
     * @return a method type matching the bytecode-level type descriptor
     * @throws NullPointerException if the string is null
     * @throws IllegalArgumentException if the string is not well-formed
     * @throws TypeNotPresentException if a named type cannot be found
     * @throws SecurityException if the security manager is present and
     *         {@code loader} is {@code null} and the caller does not have the
     *         {@link RuntimePermission}{@code ("getClassLoader")}
     */
    public static MethodType fromMethodDescriptorString(String descriptor, ClassLoader loader)
        throws IllegalArgumentException, TypeNotPresentException
    {
        if (loader == null) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);
            }
        }
        return fromDescriptor(descriptor,
                              (loader == null) ? ClassLoader.getSystemClassLoader() : loader);
    }

    /**
     * Same as {@link #fromMethodDescriptorString(String, ClassLoader)}, but
     * {@code null} ClassLoader means the bootstrap loader is used here.
     * <p>
     * IMPORTANT: This method is preferable for JDK internal use as it more
     * correctly interprets {@code null} ClassLoader than
     * {@link #fromMethodDescriptorString(String, ClassLoader)}.
     * Use of this method also avoids early initialization issues when system
     * ClassLoader is not initialized yet.
     */
    static MethodType fromDescriptor(String descriptor, ClassLoader loader)
        throws IllegalArgumentException, TypeNotPresentException
    {
        if (!descriptor.startsWith("(") ||  // also generates NPE if needed
            descriptor.indexOf(')') < 0 ||
            descriptor.indexOf('.') >= 0)
            throw newIllegalArgumentException("not a method descriptor: "+descriptor);
        List<Class<?>> types = BytecodeDescriptor.parseMethod(descriptor, loader);
        Class<?> rtype = types.remove(types.size() - 1);
        Class<?>[] ptypes = listToArray(types);
        return makeImpl(rtype, ptypes, true);
    }

    /**
     * Returns a descriptor string for the method type.  This method
     * is equivalent to calling {@link #descriptorString() MethodType::descriptorString}.
     *
     * <p>
     * Note that this is not a strict inverse of {@link #fromMethodDescriptorString fromMethodDescriptorString}.
     * Two distinct classes which share a common name but have different class loaders
     * will appear identical when viewed within descriptor strings.
     * <p>
     * This method is included for the benefit of applications that must
     * generate bytecodes that process method handles and {@code invokedynamic}.
     * {@link #fromMethodDescriptorString(java.lang.String, java.lang.ClassLoader) fromMethodDescriptorString},
     * because the latter requires a suitable class loader argument.
     * @return the descriptor string for this method type
     * @jvms 4.3.3 Method Descriptors
     * @see <a href="#descriptor">Nominal Descriptor for {@code MethodType}</a>
     */
    public String toMethodDescriptorString() {
        String desc = methodDescriptor;
        if (desc == null) {
            desc = BytecodeDescriptor.unparseMethod(this.rtype, this.ptypes);
            methodDescriptor = desc;
        }
        return desc;
    }

    /**
     * Returns a descriptor string for this method type.
     *
     * <p>
     * If this method type can be <a href="#descriptor">described nominally</a>,
     * then the result is a method type descriptor (JVMS {@jvms 4.3.3}).
     * {@link MethodTypeDesc MethodTypeDesc} for this method type
     * can be produced by calling {@link MethodTypeDesc#ofDescriptor(String)
     * MethodTypeDesc::ofDescriptor} with the result descriptor string.
     * <p>
     * If this method type cannot be <a href="#descriptor">described nominally</a>
     * and the result is a string of the form:
     * <blockquote>{@code "(<parameter-descriptors>)<return-descriptor>"}</blockquote>
     * where {@code <parameter-descriptors>} is the concatenation of the
     * {@linkplain Class#descriptorString() descriptor string} of all
     * of the parameter types and the {@linkplain Class#descriptorString() descriptor string}
     * of the return type. No {@link java.lang.constant.MethodTypeDesc MethodTypeDesc}
     * can be produced from the result string.
     *
     * @return the descriptor string for this method type
     * @since 12
     * @jvms 4.3.3 Method Descriptors
     * @see <a href="#descriptor">Nominal Descriptor for {@code MethodType}</a>
     */
    @Override
    public String descriptorString() {
        return toMethodDescriptorString();
    }

    /*non-public*/
    static String toFieldDescriptorString(Class<?> cls) {
        return BytecodeDescriptor.unparse(cls);
    }

    /**
     * Returns a nominal descriptor for this instance, if one can be
     * constructed, or an empty {@link Optional} if one cannot be.
     *
     * @return An {@link Optional} containing the resulting nominal descriptor,
     * or an empty {@link Optional} if one cannot be constructed.
     * @since 12
     * @see <a href="#descriptor">Nominal Descriptor for {@code MethodType}</a>
     */
    @Override
    public Optional<MethodTypeDesc> describeConstable() {
        try {
            return Optional.of(MethodTypeDesc.of(returnType().describeConstable().orElseThrow(),
                                                 Stream.of(parameterArray())
                                                      .map(p -> p.describeConstable().orElseThrow())
                                                      .toArray(ClassDesc[]::new)));
        }
        catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /// Serialization.

    /**
     * There are no serializable fields for {@code MethodType}.
     */
    @java.io.Serial
    private static final java.io.ObjectStreamField[] serialPersistentFields = { };

    /**
     * Save the {@code MethodType} instance to a stream.
     *
     * @serialData
     * For portability, the serialized format does not refer to named fields.
     * Instead, the return type and parameter type arrays are written directly
     * from the {@code writeObject} method, using two calls to {@code s.writeObject}
     * as follows:
     * <blockquote><pre>{@code
s.writeObject(this.returnType());
s.writeObject(this.parameterArray());
     * }</pre></blockquote>
     * <p>
     * The deserialized field values are checked as if they were
     * provided to the factory method {@link #methodType(Class,Class[]) methodType}.
     * For example, null values, or {@code void} parameter types,
     * will lead to exceptions during deserialization.
     * @param s the stream to write the object to
     * @throws java.io.IOException if there is a problem writing the object
     */
    @java.io.Serial
    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
        s.defaultWriteObject();  // requires serialPersistentFields to be an empty array
        s.writeObject(returnType());
        s.writeObject(parameterArray());
    }

    /**
     * Reconstitute the {@code MethodType} instance from a stream (that is,
     * deserialize it).
     * This instance is a scratch object with bogus final fields.
     * It provides the parameters to the factory method called by
     * {@link #readResolve readResolve}.
     * After that call it is discarded.
     * @param s the stream to read the object from
     * @throws java.io.IOException if there is a problem reading the object
     * @throws ClassNotFoundException if one of the component classes cannot be resolved
     * @see #readResolve
     * @see #writeObject
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        // Assign defaults in case this object escapes
        UNSAFE.putReference(this, OffsetHolder.rtypeOffset, void.class);
        UNSAFE.putReference(this, OffsetHolder.ptypesOffset, NO_PTYPES);

        s.defaultReadObject();  // requires serialPersistentFields to be an empty array

        Class<?>   returnType     = (Class<?>)   s.readObject();
        Class<?>[] parameterArray = (Class<?>[]) s.readObject();

        // Verify all operands, and make sure ptypes is unshared
        // Cache the new MethodType for readResolve
        wrapAlt = new MethodType[]{MethodType.methodType(returnType, parameterArray)};
    }

    // Support for resetting final fields while deserializing. Implement Holder
    // pattern to make the rarely needed offset calculation lazy.
    private static class OffsetHolder {
        static final long rtypeOffset
                = UNSAFE.objectFieldOffset(MethodType.class, "rtype");

        static final long ptypesOffset
                = UNSAFE.objectFieldOffset(MethodType.class, "ptypes");
    }

    /**
     * Resolves and initializes a {@code MethodType} object
     * after serialization.
     * @return the fully initialized {@code MethodType} object
     */
    @java.io.Serial
    private Object readResolve() {
        // Do not use a trusted path for deserialization:
        //    return makeImpl(rtype, ptypes, true);
        // Verify all operands, and make sure ptypes is unshared:
        // Return a new validated MethodType for the rtype and ptypes passed from readObject.
        MethodType mt = ((MethodType[])wrapAlt)[0];
        wrapAlt = null;
        return mt;
    }

    /**
     * Simple implementation of weak concurrent intern set.
     *
     * @param <T> interned type
     */
    private static class ConcurrentWeakInternSet<T> {

        private final ConcurrentMap<WeakEntry<T>, WeakEntry<T>> map;
        private final ReferenceQueue<T> stale;

        public ConcurrentWeakInternSet() {
            this.map = new ConcurrentHashMap<>(512);
            this.stale = new ReferenceQueue<>();
        }

        /**
         * Get the existing interned element.
         * This method returns null if no element is interned.
         *
         * @param elem element to look up
         * @return the interned element
         */
        public T get(T elem) {
            if (elem == null) throw new NullPointerException();
            expungeStaleElements();

            WeakEntry<T> value = map.get(elem);
            if (value != null) {
                T res = value.get();
                if (res != null) {
                    return res;
                }
            }
            return null;
        }

        /**
         * Interns the element.
         * Always returns non-null element, matching the one in the intern set.
         * Under the race against another add(), it can return <i>different</i>
         * element, if another thread beats us to interning it.
         *
         * @param elem element to add
         * @return element that was actually added
         */
        public T add(T elem) {
            if (elem == null) throw new NullPointerException();

            // Playing double race here, and so spinloop is required.
            // First race is with two concurrent updaters.
            // Second race is with GC purging weak ref under our feet.
            // Hopefully, we almost always end up with a single pass.
            T interned;
            WeakEntry<T> e = new WeakEntry<>(elem, stale);
            do {
                expungeStaleElements();
                WeakEntry<T> exist = map.putIfAbsent(e, e);
                interned = (exist == null) ? elem : exist.get();
            } while (interned == null);
            return interned;
        }

        private void expungeStaleElements() {
            Reference<? extends T> reference;
            while ((reference = stale.poll()) != null) {
                map.remove(reference);
            }
        }

        private static class WeakEntry<T> extends WeakReference<T> {

            public final int hashcode;

            public WeakEntry(T key, ReferenceQueue<T> queue) {
                super(key, queue);
                hashcode = key.hashCode();
            }

            /**
             * This implementation returns {@code true} if {@code obj} is another
             * {@code WeakEntry} whose referent is equal to this referent, or
             * if {@code obj} is equal to the referent of this. This allows
             * lookups to be made without wrapping in a {@code WeakEntry}.
             *
             * @param obj the object to compare
             * @return true if {@code obj} is equal to this or the referent of this
             * @see MethodType#equals(Object)
             * @see Object#equals(Object)
             */
            @Override
            public boolean equals(Object obj) {
                Object mine = get();
                if (obj instanceof WeakEntry) {
                    Object that = ((WeakEntry) obj).get();
                    return (that == null || mine == null) ? (this == obj) : mine.equals(that);
                }
                return (mine == null) ? (obj == null) : mine.equals(obj);
            }

            @Override
            public int hashCode() {
                return hashcode;
            }

        }
    }

}
