/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.perf.PerfCounter;
import jdk.internal.vm.annotation.DontInline;
import jdk.internal.vm.annotation.Hidden;
import jdk.internal.vm.annotation.Stable;
import sun.invoke.util.Wrapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import static java.lang.invoke.LambdaForm.BasicType.*;
import static java.lang.invoke.MethodHandleNatives.Constants.*;
import static java.lang.invoke.MethodHandleStatics.*;

/**
 * The symbolic, non-executable form of a method handle's invocation semantics.
 * It consists of a series of names.
 * The first N (N=arity) names are parameters,
 * while any remaining names are temporary values.
 * Each temporary specifies the application of a function to some arguments.
 * The functions are method handles, while the arguments are mixes of
 * constant values and local names.
 * The result of the lambda is defined as one of the names, often the last one.
 * <p>
 * Here is an approximate grammar:
 * <blockquote><pre>{@code
 * LambdaForm = "(" ArgName* ")=>{" TempName* Result "}"
 * ArgName = "a" N ":" T
 * TempName = "t" N ":" T "=" Function "(" Argument* ");"
 * Function = ConstantValue
 * Argument = NameRef | ConstantValue
 * Result = NameRef | "void"
 * NameRef = "a" N | "t" N
 * N = (any whole number)
 * T = "L" | "I" | "J" | "F" | "D" | "V"
 * }</pre></blockquote>
 * Names are numbered consecutively from left to right starting at zero.
 * (The letters are merely a taste of syntax sugar.)
 * Thus, the first temporary (if any) is always numbered N (where N=arity).
 * Every occurrence of a name reference in an argument list must refer to
 * a name previously defined within the same lambda.
 * A lambda has a void result if and only if its result index is -1.
 * If a temporary has the type "V", it cannot be the subject of a NameRef,
 * even though possesses a number.
 * Note that all reference types are erased to "L", which stands for {@code Object}.
 * All subword types (boolean, byte, short, char) are erased to "I" which is {@code int}.
 * The other types stand for the usual primitive types.
 * <p>
 * Function invocation closely follows the static rules of the Java verifier.
 * Arguments and return values must exactly match when their "Name" types are
 * considered.
 * Conversions are allowed only if they do not change the erased type.
 * <ul>
 * <li>L = Object: casts are used freely to convert into and out of reference types
 * <li>I = int: subword types are forcibly narrowed when passed as arguments (see {@code explicitCastArguments})
 * <li>J = long: no implicit conversions
 * <li>F = float: no implicit conversions
 * <li>D = double: no implicit conversions
 * <li>V = void: a function result may be void if and only if its Name is of type "V"
 * </ul>
 * Although implicit conversions are not allowed, explicit ones can easily be
 * encoded by using temporary expressions which call type-transformed identity functions.
 * <p>
 * Examples:
 * <blockquote><pre>{@code
 * (a0:J)=>{ a0 }
 *     == identity(long)
 * (a0:I)=>{ t1:V = System.out#println(a0); void }
 *     == System.out#println(int)
 * (a0:L)=>{ t1:V = System.out#println(a0); a0 }
 *     == identity, with printing side-effect
 * (a0:L, a1:L)=>{ t2:L = BoundMethodHandle#argument(a0);
 *                 t3:L = BoundMethodHandle#target(a0);
 *                 t4:L = MethodHandle#invoke(t3, t2, a1); t4 }
 *     == general invoker for unary insertArgument combination
 * (a0:L, a1:L)=>{ t2:L = FilterMethodHandle#filter(a0);
 *                 t3:L = MethodHandle#invoke(t2, a1);
 *                 t4:L = FilterMethodHandle#target(a0);
 *                 t5:L = MethodHandle#invoke(t4, t3); t5 }
 *     == general invoker for unary filterArgument combination
 * (a0:L, a1:L)=>{ ...(same as previous example)...
 *                 t5:L = MethodHandle#invoke(t4, t3, a1); t5 }
 *     == general invoker for unary/unary foldArgument combination
 * (a0:L, a1:I)=>{ t2:I = identity(long).asType((int)->long)(a1); t2 }
 *     == invoker for identity method handle which performs i2l
 * (a0:L, a1:L)=>{ t2:L = BoundMethodHandle#argument(a0);
 *                 t3:L = Class#cast(t2,a1); t3 }
 *     == invoker for identity method handle which performs cast
 * }</pre></blockquote>
 * <p>
 * @author John Rose, JSR 292 EG
 */
class LambdaForm {
    final int arity;
    final int result;
    final boolean forceInline;
    final MethodHandle customized;
    @Stable final Name[] names;
    final Kind kind;
    MemberName vmentry;   // low-level behavior, or null if not yet prepared
    private boolean isCompiled;

    // Either a LambdaForm cache (managed by LambdaFormEditor) or a link to uncustomized version (for customized LF)
    volatile Object transformCache;

    public static final int VOID_RESULT = -1, LAST_RESULT = -2;

    enum BasicType {
        L_TYPE('L', Object.class, Wrapper.OBJECT),  // all reference types
        I_TYPE('I', int.class,    Wrapper.INT),
        J_TYPE('J', long.class,   Wrapper.LONG),
        F_TYPE('F', float.class,  Wrapper.FLOAT),
        D_TYPE('D', double.class, Wrapper.DOUBLE),  // all primitive types
        V_TYPE('V', void.class,   Wrapper.VOID);    // not valid in all contexts

        static final @Stable BasicType[] ALL_TYPES = BasicType.values();
        static final @Stable BasicType[] ARG_TYPES = Arrays.copyOf(ALL_TYPES, ALL_TYPES.length-1);

        static final int ARG_TYPE_LIMIT = ARG_TYPES.length;
        static final int TYPE_LIMIT = ALL_TYPES.length;

        // Derived int constants, which (unlike the enums) can be constant folded.
        // We can remove them when JDK-8161245 is fixed.
        static final byte
                L_TYPE_NUM = (byte) L_TYPE.ordinal(),
                I_TYPE_NUM = (byte) I_TYPE.ordinal(),
                J_TYPE_NUM = (byte) J_TYPE.ordinal(),
                F_TYPE_NUM = (byte) F_TYPE.ordinal(),
                D_TYPE_NUM = (byte) D_TYPE.ordinal(),
                V_TYPE_NUM = (byte) V_TYPE.ordinal();

        final char btChar;
        final Class<?> btClass;
        final Wrapper btWrapper;

        private BasicType(char btChar, Class<?> btClass, Wrapper wrapper) {
            this.btChar = btChar;
            this.btClass = btClass;
            this.btWrapper = wrapper;
        }

        char basicTypeChar() {
            return btChar;
        }
        Class<?> basicTypeClass() {
            return btClass;
        }
        Wrapper basicTypeWrapper() {
            return btWrapper;
        }
        int basicTypeSlots() {
            return btWrapper.stackSlots();
        }

        static BasicType basicType(byte type) {
            return ALL_TYPES[type];
        }
        static BasicType basicType(char type) {
            return switch (type) {
                case 'L' -> L_TYPE;
                case 'I' -> I_TYPE;
                case 'J' -> J_TYPE;
                case 'F' -> F_TYPE;
                case 'D' -> D_TYPE;
                case 'V' -> V_TYPE;
                // all subword types are represented as ints
                case 'Z', 'B', 'S', 'C' -> I_TYPE;
                default -> throw newInternalError("Unknown type char: '" + type + "'");
            };
        }
        static BasicType basicType(Wrapper type) {
            char c = type.basicTypeChar();
            return basicType(c);
        }
        static BasicType basicType(Class<?> type) {
            if (!type.isPrimitive())  return L_TYPE;
            return basicType(Wrapper.forPrimitiveType(type));
        }
        static int[] basicTypeOrds(BasicType[] types) {
            if (types == null) {
                return null;
            }
            int[] a = new int[types.length];
            for(int i = 0; i < types.length; ++i) {
                a[i] = types[i].ordinal();
            }
            return a;
        }

        static char basicTypeChar(Class<?> type) {
            return basicType(type).btChar;
        }

        static byte[] basicTypesOrd(Class<?>[] types) {
            byte[] ords = new byte[types.length];
            for (int i = 0; i < ords.length; i++) {
                ords[i] = (byte)basicType(types[i]).ordinal();
            }
            return ords;
        }

        static boolean isBasicTypeChar(char c) {
            return "LIJFDV".indexOf(c) >= 0;
        }
        static boolean isArgBasicTypeChar(char c) {
            return "LIJFD".indexOf(c) >= 0;
        }

        static { assert(checkBasicType()); }
        private static boolean checkBasicType() {
            for (int i = 0; i < ARG_TYPE_LIMIT; i++) {
                assert ARG_TYPES[i].ordinal() == i;
                assert ARG_TYPES[i] == ALL_TYPES[i];
            }
            for (int i = 0; i < TYPE_LIMIT; i++) {
                assert ALL_TYPES[i].ordinal() == i;
            }
            assert ALL_TYPES[TYPE_LIMIT - 1] == V_TYPE;
            assert !Arrays.asList(ARG_TYPES).contains(V_TYPE);
            return true;
        }
    }

    enum Kind {
        GENERIC("invoke"),
        ZERO("zero"),
        IDENTITY("identity"),
        BOUND_REINVOKER("BMH.reinvoke", "reinvoke"),
        REINVOKER("MH.reinvoke", "reinvoke"),
        DELEGATE("MH.delegate", "delegate"),
        EXACT_LINKER("MH.invokeExact_MT", "invokeExact_MT"),
        EXACT_INVOKER("MH.exactInvoker", "exactInvoker"),
        GENERIC_LINKER("MH.invoke_MT", "invoke_MT"),
        GENERIC_INVOKER("MH.invoker", "invoker"),
        LINK_TO_TARGET_METHOD("linkToTargetMethod"),
        LINK_TO_CALL_SITE("linkToCallSite"),
        DIRECT_INVOKE_VIRTUAL("DMH.invokeVirtual", "invokeVirtual"),
        DIRECT_INVOKE_SPECIAL("DMH.invokeSpecial", "invokeSpecial"),
        DIRECT_INVOKE_SPECIAL_IFC("DMH.invokeSpecialIFC", "invokeSpecialIFC"),
        DIRECT_INVOKE_STATIC("DMH.invokeStatic", "invokeStatic"),
        DIRECT_NEW_INVOKE_SPECIAL("DMH.newInvokeSpecial", "newInvokeSpecial"),
        DIRECT_INVOKE_INTERFACE("DMH.invokeInterface", "invokeInterface"),
        DIRECT_INVOKE_STATIC_INIT("DMH.invokeStaticInit", "invokeStaticInit"),
        GET_REFERENCE("getReference"),
        PUT_REFERENCE("putReference"),
        GET_REFERENCE_VOLATILE("getReferenceVolatile"),
        PUT_REFERENCE_VOLATILE("putReferenceVolatile"),
        GET_INT("getInt"),
        PUT_INT("putInt"),
        GET_INT_VOLATILE("getIntVolatile"),
        PUT_INT_VOLATILE("putIntVolatile"),
        GET_BOOLEAN("getBoolean"),
        PUT_BOOLEAN("putBoolean"),
        GET_BOOLEAN_VOLATILE("getBooleanVolatile"),
        PUT_BOOLEAN_VOLATILE("putBooleanVolatile"),
        GET_BYTE("getByte"),
        PUT_BYTE("putByte"),
        GET_BYTE_VOLATILE("getByteVolatile"),
        PUT_BYTE_VOLATILE("putByteVolatile"),
        GET_CHAR("getChar"),
        PUT_CHAR("putChar"),
        GET_CHAR_VOLATILE("getCharVolatile"),
        PUT_CHAR_VOLATILE("putCharVolatile"),
        GET_SHORT("getShort"),
        PUT_SHORT("putShort"),
        GET_SHORT_VOLATILE("getShortVolatile"),
        PUT_SHORT_VOLATILE("putShortVolatile"),
        GET_LONG("getLong"),
        PUT_LONG("putLong"),
        GET_LONG_VOLATILE("getLongVolatile"),
        PUT_LONG_VOLATILE("putLongVolatile"),
        GET_FLOAT("getFloat"),
        PUT_FLOAT("putFloat"),
        GET_FLOAT_VOLATILE("getFloatVolatile"),
        PUT_FLOAT_VOLATILE("putFloatVolatile"),
        GET_DOUBLE("getDouble"),
        PUT_DOUBLE("putDouble"),
        GET_DOUBLE_VOLATILE("getDoubleVolatile"),
        PUT_DOUBLE_VOLATILE("putDoubleVolatile"),
        TRY_FINALLY("tryFinally"),
        TABLE_SWITCH("tableSwitch"),
        COLLECT("collect"),
        COLLECTOR("collector"),
        CONVERT("convert"),
        SPREAD("spread"),
        LOOP("loop"),
        FIELD("field"),
        GUARD("guard"),
        GUARD_WITH_CATCH("guardWithCatch"),
        VARHANDLE_EXACT_INVOKER("VH.exactInvoker"),
        VARHANDLE_INVOKER("VH.invoker", "invoker"),
        VARHANDLE_LINKER("VH.invoke_MT", "invoke_MT");

        final String defaultLambdaName;
        final String methodName;

        private Kind(String defaultLambdaName) {
            this(defaultLambdaName, defaultLambdaName);
        }

        private Kind(String defaultLambdaName, String methodName) {
            this.defaultLambdaName = defaultLambdaName;
            this.methodName = methodName;
        }
    }

    LambdaForm(int arity, Name[] names, int result) {
        this(arity, names, result, /*forceInline=*/true, /*customized=*/null, Kind.GENERIC);
    }
    LambdaForm(int arity, Name[] names, int result, Kind kind) {
        this(arity, names, result, /*forceInline=*/true, /*customized=*/null, kind);
    }
    LambdaForm(int arity, Name[] names, int result, boolean forceInline, MethodHandle customized) {
        this(arity, names, result, forceInline, customized, Kind.GENERIC);
    }
    LambdaForm(int arity, Name[] names, int result, boolean forceInline, MethodHandle customized, Kind kind) {
        assert(namesOK(arity, names));
        this.arity = arity;
        this.result = fixResult(result, names);
        this.names = names.clone();
        this.forceInline = forceInline;
        this.customized = customized;
        this.kind = kind;
        int maxOutArity = normalize();
        if (maxOutArity > MethodType.MAX_MH_INVOKER_ARITY) {
            // Cannot use LF interpreter on very high arity expressions.
            assert(maxOutArity <= MethodType.MAX_JVM_ARITY);
            compileToBytecode();
        }
    }
    LambdaForm(int arity, Name[] names) {
        this(arity, names, LAST_RESULT, /*forceInline=*/true, /*customized=*/null, Kind.GENERIC);
    }
    LambdaForm(int arity, Name[] names, Kind kind) {
        this(arity, names, LAST_RESULT, /*forceInline=*/true, /*customized=*/null, kind);
    }
    LambdaForm(int arity, Name[] names, boolean forceInline, Kind kind) {
        this(arity, names, LAST_RESULT, forceInline, /*customized=*/null, kind);
    }

    private static Name[] buildNames(Name[] formals, Name[] temps, Name result) {
        int arity = formals.length;
        int length = arity + temps.length + (result == null ? 0 : 1);
        Name[] names = Arrays.copyOf(formals, length);
        System.arraycopy(temps, 0, names, arity, temps.length);
        if (result != null)
            names[length - 1] = result;
        return names;
    }

    private LambdaForm(MethodType mt) {
        // Make a blank lambda form, which returns a constant zero or null.
        // It is used as a template for managing the invocation of similar forms that are non-empty.
        // Called only from getPreparedForm.
        this.arity = mt.parameterCount();
        this.result = (mt.returnType() == void.class || mt.returnType() == Void.class) ? -1 : arity;
        this.names = buildEmptyNames(arity, mt, result == -1);
        this.forceInline = true;
        this.customized = null;
        this.kind = Kind.ZERO;
        assert(nameRefsAreLegal());
        assert(isEmpty());
        String sig = null;
        assert(isValidSignature(sig = basicTypeSignature()));
        assert(sig.equals(basicTypeSignature())) : sig + " != " + basicTypeSignature();
    }

    private static Name[] buildEmptyNames(int arity, MethodType mt, boolean isVoid) {
        Name[] names = arguments(isVoid ? 0 : 1, mt);
        if (!isVoid) {
            Name zero = new Name(constantZero(basicType(mt.returnType())));
            names[arity] = zero.newIndex(arity);
        }
        return names;
    }

    private static int fixResult(int result, Name[] names) {
        if (result == LAST_RESULT)
            result = names.length - 1;  // might still be void
        if (result >= 0 && names[result].type == V_TYPE)
            result = VOID_RESULT;
        return result;
    }

    static boolean debugNames() {
        return DEBUG_NAME_COUNTERS != null;
    }

    static void associateWithDebugName(LambdaForm form, String name) {
        assert (debugNames());
        synchronized (DEBUG_NAMES) {
            DEBUG_NAMES.put(form, name);
        }
    }

    String lambdaName() {
        if (DEBUG_NAMES != null) {
            synchronized (DEBUG_NAMES) {
                String name = DEBUG_NAMES.get(this);
                if (name == null) {
                    name = generateDebugName();
                }
                return name;
            }
        }
        return kind.defaultLambdaName;
    }

    private String generateDebugName() {
        assert (debugNames());
        String debugNameStem = kind.defaultLambdaName;
        Integer ctr = DEBUG_NAME_COUNTERS.getOrDefault(debugNameStem, 0);
        DEBUG_NAME_COUNTERS.put(debugNameStem, ctr + 1);
        StringBuilder buf = new StringBuilder(debugNameStem);
        int leadingZero = buf.length();
        buf.append((int) ctr);
        for (int i = buf.length() - leadingZero; i < 3; i++) {
            buf.insert(leadingZero, '0');
        }
        buf.append('_');
        buf.append(basicTypeSignature());
        String name = buf.toString();
        associateWithDebugName(this, name);
        return name;
    }

    private static boolean namesOK(int arity, Name[] names) {
        for (int i = 0; i < names.length; i++) {
            Name n = names[i];
            assert(n != null) : "n is null";
            if (i < arity)
                assert( n.isParam()) : n + " is not param at " + i;
            else
                assert(!n.isParam()) : n + " is param at " + i;
        }
        return true;
    }

    /** Customize LambdaForm for a particular MethodHandle */
    LambdaForm customize(MethodHandle mh) {
        if (customized == mh) {
            return this;
        }
        LambdaForm customForm = new LambdaForm(arity, names, result, forceInline, mh, kind);
        if (COMPILE_THRESHOLD >= 0 && isCompiled) {
            // If shared LambdaForm has been compiled, compile customized version as well.
            customForm.compileToBytecode();
        }
        customForm.transformCache = this; // LambdaFormEditor should always use uncustomized form.
        return customForm;
    }

    /** Get uncustomized flavor of the LambdaForm */
    LambdaForm uncustomize() {
        if (customized == null) {
            return this;
        }
        assert(transformCache != null); // Customized LambdaForm should always has a link to uncustomized version.
        LambdaForm uncustomizedForm = (LambdaForm)transformCache;
        if (COMPILE_THRESHOLD >= 0 && isCompiled) {
            // If customized LambdaForm has been compiled, compile uncustomized version as well.
            uncustomizedForm.compileToBytecode();
        }
        return uncustomizedForm;
    }

    /** Renumber and/or replace params so that they are interned and canonically numbered.
     *  @return maximum argument list length among the names (since we have to pass over them anyway)
     */
    private int normalize() {
        Name[] oldNames = null;
        int maxOutArity = 0;
        int changesStart = 0;
        for (int i = 0; i < names.length; i++) {
            Name n = names[i];
            if (!n.initIndex(i)) {
                if (oldNames == null) {
                    oldNames = names.clone();
                    changesStart = i;
                }
                names[i] = n.cloneWithIndex(i);
            }
            if (n.arguments != null && maxOutArity < n.arguments.length)
                maxOutArity = n.arguments.length;
        }
        if (oldNames != null) {
            int startFixing = arity;
            if (startFixing <= changesStart)
                startFixing = changesStart+1;
            for (int i = startFixing; i < names.length; i++) {
                Name fixed = names[i].replaceNames(oldNames, names, changesStart, i);
                names[i] = fixed.newIndex(i);
            }
        }
        assert(nameRefsAreLegal());
        int maxInterned = Math.min(arity, INTERNED_ARGUMENT_LIMIT);
        boolean needIntern = false;
        for (int i = 0; i < maxInterned; i++) {
            Name n = names[i], n2 = internArgument(n);
            if (n != n2) {
                names[i] = n2;
                needIntern = true;
            }
        }
        if (needIntern) {
            for (int i = arity; i < names.length; i++) {
                names[i].internArguments();
            }
        }
        assert(nameRefsAreLegal());
        return maxOutArity;
    }

    /**
     * Check that all embedded Name references are localizable to this lambda,
     * and are properly ordered after their corresponding definitions.
     * <p>
     * Note that a Name can be local to multiple lambdas, as long as
     * it possesses the same index in each use site.
     * This allows Name references to be freely reused to construct
     * fresh lambdas, without confusion.
     */
    boolean nameRefsAreLegal() {
        assert(arity >= 0 && arity <= names.length);
        assert(result >= -1 && result < names.length);
        // Do all names possess an index consistent with their local definition order?
        for (int i = 0; i < arity; i++) {
            Name n = names[i];
            assert(n.index() == i) : Arrays.asList(n.index(), i);
            assert(n.isParam());
        }
        // Also, do all local name references
        for (int i = arity; i < names.length; i++) {
            Name n = names[i];
            assert(n.index() == i);
            for (Object arg : n.arguments) {
                if (arg instanceof Name n2) {
                    int i2 = n2.index;
                    assert(0 <= i2 && i2 < names.length) : n.debugString() + ": 0 <= i2 && i2 < names.length: 0 <= " + i2 + " < " + names.length;
                    assert(names[i2] == n2) : Arrays.asList("-1-", i, "-2-", n.debugString(), "-3-", i2, "-4-", n2.debugString(), "-5-", names[i2].debugString(), "-6-", this);
                    assert(i2 < i);  // ref must come after def!
                }
            }
        }
        return true;
    }

    /** Invoke this form on the given arguments. */
    // final Object invoke(Object... args) throws Throwable {
    //     // NYI: fit this into the fast path?
    //     return interpretWithArguments(args);
    // }

    /** Report the return type. */
    BasicType returnType() {
        if (result < 0)  return V_TYPE;
        Name n = names[result];
        return n.type;
    }

    /** Report the N-th argument type. */
    BasicType parameterType(int n) {
        return parameter(n).type;
    }

    /** Report the N-th argument name. */
    Name parameter(int n) {
        Name param = names[n];
        assert(n < arity && param.isParam());
        return param;
    }

    /** Report the N-th argument type constraint. */
    Object parameterConstraint(int n) {
        return parameter(n).constraint;
    }

    /** Report the arity. */
    int arity() {
        return arity;
    }

    /** Report the number of expressions (non-parameter names). */
    int expressionCount() {
        return names.length - arity;
    }

    /** Return the method type corresponding to my basic type signature. */
    MethodType methodType() {
        Class<?>[] ptypes = new Class<?>[arity];
        for (int i = 0; i < arity; ++i) {
            ptypes[i] = parameterType(i).btClass;
        }
        return MethodType.makeImpl(returnType().btClass, ptypes, true);
    }

    /** Return ABC_Z, where the ABC are parameter type characters, and Z is the return type character. */
    final String basicTypeSignature() {
        StringBuilder buf = new StringBuilder(arity() + 3);
        for (int i = 0, a = arity(); i < a; i++)
            buf.append(parameterType(i).basicTypeChar());
        return buf.append('_').append(returnType().basicTypeChar()).toString();
    }
    static int signatureArity(String sig) {
        assert(isValidSignature(sig));
        return sig.indexOf('_');
    }
    static boolean isValidSignature(String sig) {
        int arity = sig.indexOf('_');
        if (arity < 0)  return false;  // must be of the form *_*
        int siglen = sig.length();
        if (siglen != arity + 2)  return false;  // *_X
        for (int i = 0; i < siglen; i++) {
            if (i == arity)  continue;  // skip '_'
            char c = sig.charAt(i);
            if (c == 'V')
                return (i == siglen - 1 && arity == siglen - 2);
            if (!isArgBasicTypeChar(c))  return false; // must be [LIJFD]
        }
        return true;  // [LIJFD]*_[LIJFDV]
    }

    /**
     * Check if i-th name is a call to MethodHandleImpl.selectAlternative.
     */
    boolean isSelectAlternative(int pos) {
        // selectAlternative idiom:
        //   t_{n}:L=MethodHandleImpl.selectAlternative(...)
        //   t_{n+1}:?=MethodHandle.invokeBasic(t_{n}, ...)
        if (pos+1 >= names.length)  return false;
        Name name0 = names[pos];
        Name name1 = names[pos+1];
        return name0.refersTo(MethodHandleImpl.class, "selectAlternative") &&
                name1.isInvokeBasic() &&
                name1.lastUseIndex(name0) == 0 && // t_{n+1}:?=MethodHandle.invokeBasic(t_{n}, ...)
                lastUseIndex(name0) == pos+1;     // t_{n} is local: used only in t_{n+1}
    }

    private boolean isMatchingIdiom(int pos, String idiomName, int nArgs) {
        if (pos+2 >= names.length)  return false;
        Name name0 = names[pos];
        Name name1 = names[pos+1];
        Name name2 = names[pos+2];
        return name1.refersTo(MethodHandleImpl.class, idiomName) &&
                name0.isInvokeBasic() &&
                name2.isInvokeBasic() &&
                name1.lastUseIndex(name0) == nArgs && // t_{n+1}:L=MethodHandleImpl.<invoker>(<args>, t_{n});
                lastUseIndex(name0) == pos+1 &&       // t_{n} is local: used only in t_{n+1}
                name2.lastUseIndex(name1) == 1 &&     // t_{n+2}:?=MethodHandle.invokeBasic(*, t_{n+1})
                lastUseIndex(name1) == pos+2;         // t_{n+1} is local: used only in t_{n+2}
    }

    /**
     * Check if i-th name is a start of GuardWithCatch idiom.
     */
    boolean isGuardWithCatch(int pos) {
        // GuardWithCatch idiom:
        //   t_{n}:L=MethodHandle.invokeBasic(...)
        //   t_{n+1}:L=MethodHandleImpl.guardWithCatch(*, *, *, t_{n});
        //   t_{n+2}:?=MethodHandle.invokeBasic(*, t_{n+1})
        return isMatchingIdiom(pos, "guardWithCatch", 3);
    }

    /**
     * Check if i-th name is a start of the tryFinally idiom.
     */
    boolean isTryFinally(int pos) {
        // tryFinally idiom:
        //   t_{n}:L=MethodHandle.invokeBasic(...)
        //   t_{n+1}:L=MethodHandleImpl.tryFinally(*, *, t_{n})
        //   t_{n+2}:?=MethodHandle.invokeBasic(*, t_{n+1})
        return isMatchingIdiom(pos, "tryFinally", 2);
    }

    /**
     * Check if i-th name is a start of the tableSwitch idiom.
     */
    boolean isTableSwitch(int pos) {
        // tableSwitch idiom:
        //   t_{n}:L=MethodHandle.invokeBasic(...)     // args
        //   t_{n+1}:L=MethodHandleImpl.tableSwitch(*, *, *, t_{n})
        //   t_{n+2}:?=MethodHandle.invokeBasic(*, t_{n+1})
        if (pos + 2 >= names.length)  return false;

        final int POS_COLLECT_ARGS = pos;
        final int POS_TABLE_SWITCH = pos + 1;
        final int POS_UNBOX_RESULT = pos + 2;

        Name collectArgs = names[POS_COLLECT_ARGS];
        Name tableSwitch = names[POS_TABLE_SWITCH];
        Name unboxResult = names[POS_UNBOX_RESULT];
        return tableSwitch.refersTo(MethodHandleImpl.class, "tableSwitch") &&
                collectArgs.isInvokeBasic() &&
                unboxResult.isInvokeBasic() &&
                tableSwitch.lastUseIndex(collectArgs) == 3 &&     // t_{n+1}:L=MethodHandleImpl.<invoker>(*, *, *, t_{n});
                lastUseIndex(collectArgs) == POS_TABLE_SWITCH &&  // t_{n} is local: used only in t_{n+1}
                unboxResult.lastUseIndex(tableSwitch) == 1 &&     // t_{n+2}:?=MethodHandle.invokeBasic(*, t_{n+1})
                lastUseIndex(tableSwitch) == POS_UNBOX_RESULT;    // t_{n+1} is local: used only in t_{n+2}
    }

    /**
     * Check if i-th name is a start of the loop idiom.
     */
    boolean isLoop(int pos) {
        // loop idiom:
        //   t_{n}:L=MethodHandle.invokeBasic(...)
        //   t_{n+1}:L=MethodHandleImpl.loop(types, *, t_{n})
        //   t_{n+2}:?=MethodHandle.invokeBasic(*, t_{n+1})
        return isMatchingIdiom(pos, "loop", 2);
    }

    /*
     * Code generation issues:
     *
     * Compiled LFs should be reusable in general.
     * The biggest issue is how to decide when to pull a name into
     * the bytecode, versus loading a reified form from the MH data.
     *
     * For example, an asType wrapper may require execution of a cast
     * after a call to a MH.  The target type of the cast can be placed
     * as a constant in the LF itself.  This will force the cast type
     * to be compiled into the bytecodes and native code for the MH.
     * Or, the target type of the cast can be erased in the LF, and
     * loaded from the MH data.  (Later on, if the MH as a whole is
     * inlined, the data will flow into the inlined instance of the LF,
     * as a constant, and the end result will be an optimal cast.)
     *
     * This erasure of cast types can be done with any use of
     * reference types.  It can also be done with whole method
     * handles.  Erasing a method handle might leave behind
     * LF code that executes correctly for any MH of a given
     * type, and load the required MH from the enclosing MH's data.
     * Or, the erasure might even erase the expected MT.
     *
     * Also, for direct MHs, the MemberName of the target
     * could be erased, and loaded from the containing direct MH.
     * As a simple case, a LF for all int-valued non-static
     * field getters would perform a cast on its input argument
     * (to non-constant base type derived from the MemberName)
     * and load an integer value from the input object
     * (at a non-constant offset also derived from the MemberName).
     * Such MN-erased LFs would be inlinable back to optimized
     * code, whenever a constant enclosing DMH is available
     * to supply a constant MN from its data.
     *
     * The main problem here is to keep LFs reasonably generic,
     * while ensuring that hot spots will inline good instances.
     * "Reasonably generic" means that we don't end up with
     * repeated versions of bytecode or machine code that do
     * not differ in their optimized form.  Repeated versions
     * of machine would have the undesirable overheads of
     * (a) redundant compilation work and (b) extra I$ pressure.
     * To control repeated versions, we need to be ready to
     * erase details from LFs and move them into MH data,
     * whenever those details are not relevant to significant
     * optimization.  "Significant" means optimization of
     * code that is actually hot.
     *
     * Achieving this may require dynamic splitting of MHs, by replacing
     * a generic LF with a more specialized one, on the same MH,
     * if (a) the MH is frequently executed and (b) the MH cannot
     * be inlined into a containing caller, such as an invokedynamic.
     *
     * Compiled LFs that are no longer used should be GC-able.
     * If they contain non-BCP references, they should be properly
     * interlinked with the class loader(s) that their embedded types
     * depend on.  This probably means that reusable compiled LFs
     * will be tabulated (indexed) on relevant class loaders,
     * or else that the tables that cache them will have weak links.
     */

    /**
     * Make this LF directly executable, as part of a MethodHandle.
     * Invariant:  Every MH which is invoked must prepare its LF
     * before invocation.
     * (In principle, the JVM could do this very lazily,
     * as a sort of pre-invocation linkage step.)
     */
    public void prepare() {
        if (COMPILE_THRESHOLD == 0 && !forceInterpretation() && !isCompiled) {
            compileToBytecode();
        }
        if (this.vmentry != null) {
            // already prepared (e.g., a primitive DMH invoker form)
            return;
        }
        MethodType mtype = methodType();
        LambdaForm prep = mtype.form().cachedLambdaForm(MethodTypeForm.LF_INTERPRET);
        if (prep == null) {
            assert (isValidSignature(basicTypeSignature()));
            prep = new LambdaForm(mtype);
            prep.vmentry = InvokerBytecodeGenerator.generateLambdaFormInterpreterEntryPoint(mtype);
            prep = mtype.form().setCachedLambdaForm(MethodTypeForm.LF_INTERPRET, prep);
        }
        this.vmentry = prep.vmentry;
        // TO DO: Maybe add invokeGeneric, invokeWithArguments
    }

    private static @Stable PerfCounter LF_FAILED;

    private static PerfCounter failedCompilationCounter() {
        if (LF_FAILED == null) {
            LF_FAILED = PerfCounter.newPerfCounter("java.lang.invoke.failedLambdaFormCompilations");
        }
        return LF_FAILED;
    }

    /** Generate optimizable bytecode for this form. */
    void compileToBytecode() {
        if (forceInterpretation()) {
            return; // this should not be compiled
        }
        if (vmentry != null && isCompiled) {
            return;  // already compiled somehow
        }

        // Obtain the invoker MethodType outside of the following try block.
        // This ensures that an IllegalArgumentException is directly thrown if the
        // type would have 256 or more parameters
        MethodType invokerType = methodType();
        assert(vmentry == null || vmentry.getMethodType().basicType().equals(invokerType));
        try {
            vmentry = InvokerBytecodeGenerator.generateCustomizedCode(this, invokerType);
            if (TRACE_INTERPRETER)
                traceInterpreter("compileToBytecode", this);
            isCompiled = true;
        } catch (InvokerBytecodeGenerator.BytecodeGenerationException bge) {
            // bytecode generation failed - mark this LambdaForm as to be run in interpretation mode only
            invocationCounter = -1;
            failedCompilationCounter().increment();
            if (LOG_LF_COMPILATION_FAILURE) {
                System.out.println("LambdaForm compilation failed: " + this);
                bge.printStackTrace(System.out);
            }
        } catch (Error e) {
            // Pass through any error
            throw e;
        } catch (Exception e) {
            // Wrap any exception
            throw newInternalError(this.toString(), e);
        }
    }

    // The next few routines are called only from assert expressions
    // They verify that the built-in invokers process the correct raw data types.
    private static boolean argumentTypesMatch(String sig, Object[] av) {
        int arity = signatureArity(sig);
        assert(av.length == arity) : "av.length == arity: av.length=" + av.length + ", arity=" + arity;
        assert(av[0] instanceof MethodHandle) : "av[0] not instance of MethodHandle: " + av[0];
        MethodHandle mh = (MethodHandle) av[0];
        MethodType mt = mh.type();
        assert(mt.parameterCount() == arity-1);
        for (int i = 0; i < av.length; i++) {
            Class<?> pt = (i == 0 ? MethodHandle.class : mt.parameterType(i-1));
            assert(valueMatches(basicType(sig.charAt(i)), pt, av[i]));
        }
        return true;
    }
    private static boolean valueMatches(BasicType tc, Class<?> type, Object x) {
        // The following line is needed because (...)void method handles can use non-void invokers
        if (type == void.class)  tc = V_TYPE;   // can drop any kind of value
        assert tc == basicType(type) : tc + " == basicType(" + type + ")=" + basicType(type);
        switch (tc) {
        case I_TYPE: assert checkInt(type, x)   : "checkInt(" + type + "," + x +")";   break;
        case J_TYPE: assert x instanceof Long   : "instanceof Long: " + x;             break;
        case F_TYPE: assert x instanceof Float  : "instanceof Float: " + x;            break;
        case D_TYPE: assert x instanceof Double : "instanceof Double: " + x;           break;
        case L_TYPE: assert checkRef(type, x)   : "checkRef(" + type + "," + x + ")";  break;
        case V_TYPE: break;  // allow anything here; will be dropped
        default:  assert(false);
        }
        return true;
    }
    private static boolean checkInt(Class<?> type, Object x) {
        assert(x instanceof Integer);
        if (type == int.class)  return true;
        Wrapper w = Wrapper.forBasicType(type);
        assert(w.isSubwordOrInt());
        Object x1 = Wrapper.INT.wrap(w.wrap(x));
        return x.equals(x1);
    }
    private static boolean checkRef(Class<?> type, Object x) {
        assert(!type.isPrimitive());
        if (x == null)  return true;
        if (type.isInterface())  return true;
        return type.isInstance(x);
    }

    /** If the invocation count hits the threshold we spin bytecodes and call that subsequently. */
    private static final int COMPILE_THRESHOLD;
    static {
        COMPILE_THRESHOLD = Math.max(-1, MethodHandleStatics.COMPILE_THRESHOLD);
    }
    private int invocationCounter = 0; // a value of -1 indicates LambdaForm interpretation mode forever

    private boolean forceInterpretation() {
        return invocationCounter == -1;
    }

    @Hidden
    @DontInline
    /** Interpretively invoke this form on the given arguments. */
    Object interpretWithArguments(Object... argumentValues) throws Throwable {
        if (TRACE_INTERPRETER)
            return interpretWithArgumentsTracing(argumentValues);
        checkInvocationCounter();
        assert(arityCheck(argumentValues));
        Object[] values = Arrays.copyOf(argumentValues, names.length);
        for (int i = argumentValues.length; i < values.length; i++) {
            values[i] = interpretName(names[i], values);
        }
        Object rv = (result < 0) ? null : values[result];
        assert(resultCheck(argumentValues, rv));
        return rv;
    }

    @Hidden
    @DontInline
    /** Evaluate a single Name within this form, applying its function to its arguments. */
    Object interpretName(Name name, Object[] values) throws Throwable {
        if (TRACE_INTERPRETER)
            traceInterpreter("| interpretName", name.debugString(), (Object[]) null);
        Object[] arguments = Arrays.copyOf(name.arguments, name.arguments.length, Object[].class);
        for (int i = 0; i < arguments.length; i++) {
            Object a = arguments[i];
            if (a instanceof Name) {
                int i2 = ((Name)a).index();
                assert(names[i2] == a);
                a = values[i2];
                arguments[i] = a;
            }
        }
        return name.function.invokeWithArguments(arguments);
    }

    private void checkInvocationCounter() {
        if (COMPILE_THRESHOLD != 0 &&
            !forceInterpretation() && invocationCounter < COMPILE_THRESHOLD) {
            invocationCounter++;  // benign race
            if (invocationCounter >= COMPILE_THRESHOLD) {
                // Replace vmentry with a bytecode version of this LF.
                compileToBytecode();
            }
        }
    }
    Object interpretWithArgumentsTracing(Object... argumentValues) throws Throwable {
        traceInterpreter("[ interpretWithArguments", this, argumentValues);
        if (!forceInterpretation() && invocationCounter < COMPILE_THRESHOLD) {
            int ctr = invocationCounter++;  // benign race
            traceInterpreter("| invocationCounter", ctr);
            if (invocationCounter >= COMPILE_THRESHOLD) {
                compileToBytecode();
            }
        }
        Object rval;
        try {
            assert(arityCheck(argumentValues));
            Object[] values = Arrays.copyOf(argumentValues, names.length);
            for (int i = argumentValues.length; i < values.length; i++) {
                values[i] = interpretName(names[i], values);
            }
            rval = (result < 0) ? null : values[result];
        } catch (Throwable ex) {
            traceInterpreter("] throw =>", ex);
            throw ex;
        }
        traceInterpreter("] return =>", rval);
        return rval;
    }

    static void traceInterpreter(String event, Object obj, Object... args) {
        if (TRACE_INTERPRETER) {
            System.out.println("LFI: "+event+" "+(obj != null ? obj : "")+(args != null && args.length != 0 ? Arrays.asList(args) : ""));
        }
    }
    static void traceInterpreter(String event, Object obj) {
        traceInterpreter(event, obj, (Object[])null);
    }
    private boolean arityCheck(Object[] argumentValues) {
        assert(argumentValues.length == arity) : arity+"!="+Arrays.asList(argumentValues)+".length";
        // also check that the leading (receiver) argument is somehow bound to this LF:
        assert(argumentValues[0] instanceof MethodHandle) : "not MH: " + argumentValues[0];
        MethodHandle mh = (MethodHandle) argumentValues[0];
        assert(mh.internalForm() == this);
        // note:  argument #0 could also be an interface wrapper, in the future
        argumentTypesMatch(basicTypeSignature(), argumentValues);
        return true;
    }
    private boolean resultCheck(Object[] argumentValues, Object result) {
        MethodHandle mh = (MethodHandle) argumentValues[0];
        MethodType mt = mh.type();
        assert(valueMatches(returnType(), mt.returnType(), result));
        return true;
    }

    private boolean isEmpty() {
        if (result < 0)
            return (names.length == arity);
        else if (result == arity && names.length == arity + 1)
            return names[arity].isConstantZero();
        else
            return false;
    }

    public String toString() {
        String lambdaName = lambdaName();
        StringBuilder buf = new StringBuilder(lambdaName + "=Lambda(");
        for (int i = 0; i < names.length; i++) {
            if (i == arity)  buf.append(")=>{");
            Name n = names[i];
            if (i >= arity)  buf.append("\n    ");
            buf.append(n.paramString());
            if (i < arity) {
                if (i+1 < arity)  buf.append(",");
                continue;
            }
            buf.append("=").append(n.exprString());
            buf.append(";");
        }
        if (arity == names.length)  buf.append(")=>{");
        buf.append(result < 0 ? "void" : names[result]).append("}");
        if (TRACE_INTERPRETER) {
            // Extra verbosity:
            buf.append(":").append(basicTypeSignature());
            buf.append("/").append(vmentry);
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LambdaForm && equals((LambdaForm)obj);
    }
    public boolean equals(LambdaForm that) {
        if (this.result != that.result)  return false;
        return Arrays.equals(this.names, that.names);
    }
    public int hashCode() {
        return result + 31 * Arrays.hashCode(names);
    }
    LambdaFormEditor editor() {
        return LambdaFormEditor.lambdaFormEditor(this);
    }

    boolean contains(Name name) {
        int pos = name.index();
        if (pos >= 0) {
            return pos < names.length && name.equals(names[pos]);
        }
        for (int i = arity; i < names.length; i++) {
            if (name.equals(names[i]))
                return true;
        }
        return false;
    }

    static class NamedFunction {
        final MemberName member;
        private @Stable MethodHandle resolvedHandle;
        @Stable MethodHandle invoker;

        NamedFunction(MethodHandle resolvedHandle) {
            this(resolvedHandle.internalMemberName(), resolvedHandle);
        }
        NamedFunction(MemberName member, MethodHandle resolvedHandle) {
            this.member = member;
            this.resolvedHandle = resolvedHandle;
             // The following assert is almost always correct, but will fail for corner cases, such as PrivateInvokeTest.
             //assert(!isInvokeBasic(member));
        }
        NamedFunction(MethodType basicInvokerType) {
            assert(basicInvokerType == basicInvokerType.basicType()) : basicInvokerType;
            if (basicInvokerType.parameterSlotCount() < MethodType.MAX_MH_INVOKER_ARITY) {
                this.resolvedHandle = basicInvokerType.invokers().basicInvoker();
                this.member = resolvedHandle.internalMemberName();
            } else {
                // necessary to pass BigArityTest
                this.member = Invokers.invokeBasicMethod(basicInvokerType);
            }
            assert(isInvokeBasic(member));
        }

        private static boolean isInvokeBasic(MemberName member) {
            return member != null &&
                   member.getDeclaringClass() == MethodHandle.class &&
                  "invokeBasic".equals(member.getName());
        }

        // The next 2 constructors are used to break circular dependencies on MH.invokeStatic, etc.
        // Any LambdaForm containing such a member is not interpretable.
        // This is OK, since all such LFs are prepared with special primitive vmentry points.
        // And even without the resolvedHandle, the name can still be compiled and optimized.
        NamedFunction(Method method) {
            this(new MemberName(method));
        }
        NamedFunction(MemberName member) {
            this(member, null);
        }

        MethodHandle resolvedHandle() {
            if (resolvedHandle == null)  resolve();
            return resolvedHandle;
        }

        synchronized void resolve() {
            if (resolvedHandle == null) {
                resolvedHandle = DirectMethodHandle.make(member);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null) return false;
            return (other instanceof NamedFunction that)
                    && this.member != null
                    && this.member.equals(that.member);
        }

        @Override
        public int hashCode() {
            if (member != null)
                return member.hashCode();
            return super.hashCode();
        }

        static final MethodType INVOKER_METHOD_TYPE =
            MethodType.methodType(Object.class, MethodHandle.class, Object[].class);

        private static MethodHandle computeInvoker(MethodTypeForm typeForm) {
            typeForm = typeForm.basicType().form();  // normalize to basic type
            MethodHandle mh = typeForm.cachedMethodHandle(MethodTypeForm.MH_NF_INV);
            if (mh != null)  return mh;
            MemberName invoker = InvokerBytecodeGenerator.generateNamedFunctionInvoker(typeForm);  // this could take a while
            mh = DirectMethodHandle.make(invoker);
            MethodHandle mh2 = typeForm.cachedMethodHandle(MethodTypeForm.MH_NF_INV);
            if (mh2 != null)  return mh2;  // benign race
            if (!mh.type().equals(INVOKER_METHOD_TYPE))
                throw newInternalError(mh.debugString());
            return typeForm.setCachedMethodHandle(MethodTypeForm.MH_NF_INV, mh);
        }

        @Hidden
        Object invokeWithArguments(Object... arguments) throws Throwable {
            // If we have a cached invoker, call it right away.
            // NOTE: The invoker always returns a reference value.
            if (TRACE_INTERPRETER)  return invokeWithArgumentsTracing(arguments);
            return invoker().invokeBasic(resolvedHandle(), arguments);
        }

        @Hidden
        Object invokeWithArgumentsTracing(Object[] arguments) throws Throwable {
            Object rval;
            try {
                traceInterpreter("[ call", this, arguments);
                if (invoker == null) {
                    traceInterpreter("| getInvoker", this);
                    invoker();
                }
                // resolvedHandle might be uninitialized, ok for tracing
                if (resolvedHandle == null) {
                    traceInterpreter("| resolve", this);
                    resolvedHandle();
                }
                rval = invoker().invokeBasic(resolvedHandle(), arguments);
            } catch (Throwable ex) {
                traceInterpreter("] throw =>", ex);
                throw ex;
            }
            traceInterpreter("] return =>", rval);
            return rval;
        }

        private MethodHandle invoker() {
            if (invoker != null)  return invoker;
            // Get an invoker and cache it.
            return invoker = computeInvoker(methodType().form());
        }

        MethodType methodType() {
            if (resolvedHandle != null)
                return resolvedHandle.type();
            else
                // only for certain internal LFs during bootstrapping
                return member.getInvocationType();
        }

        MemberName member() {
            assert(assertMemberIsConsistent());
            return member;
        }

        // Called only from assert.
        private boolean assertMemberIsConsistent() {
            if (resolvedHandle instanceof DirectMethodHandle) {
                MemberName m = resolvedHandle.internalMemberName();
                assert(m.equals(member));
            }
            return true;
        }

        Class<?> memberDeclaringClassOrNull() {
            return (member == null) ? null : member.getDeclaringClass();
        }

        BasicType returnType() {
            return basicType(methodType().returnType());
        }

        BasicType parameterType(int n) {
            return basicType(methodType().parameterType(n));
        }

        int arity() {
            return methodType().parameterCount();
        }

        public String toString() {
            if (member == null)  return String.valueOf(resolvedHandle);
            return member.getDeclaringClass().getSimpleName()+"."+member.getName();
        }

        public boolean isIdentity() {
            return this.equals(identity(returnType()));
        }

        public boolean isConstantZero() {
            return this.equals(constantZero(returnType()));
        }

        public MethodHandleImpl.Intrinsic intrinsicName() {
            return resolvedHandle != null
                ? resolvedHandle.intrinsicName()
                : MethodHandleImpl.Intrinsic.NONE;
        }

        public Object intrinsicData() {
            return resolvedHandle != null
                ? resolvedHandle.intrinsicData()
                : null;
        }
    }

    public static String basicTypeSignature(MethodType type) {
        int params = type.parameterCount();
        char[] sig = new char[params + 2];
        int sigp = 0;
        while (sigp < params) {
            sig[sigp] = basicTypeChar(type.parameterType(sigp++));
        }
        sig[sigp++] = '_';
        sig[sigp++] = basicTypeChar(type.returnType());
        assert(sigp == sig.length);
        return String.valueOf(sig);
    }

    /** Hack to make signatures more readable when they show up in method names.
     * Signature should start with a sequence of uppercase ASCII letters.
     * Runs of three or more are replaced by a single letter plus a decimal repeat count.
     * A tail of anything other than uppercase ASCII is passed through unchanged.
     * @param signature sequence of uppercase ASCII letters with possible repetitions
     * @return same sequence, with repetitions counted by decimal numerals
     */
    public static String shortenSignature(String signature) {
        final int NO_CHAR = -1, MIN_RUN = 3;
        int c0, c1 = NO_CHAR, c1reps = 0;
        StringBuilder buf = null;
        int len = signature.length();
        if (len < MIN_RUN)  return signature;
        for (int i = 0; i <= len; i++) {
            if (c1 != NO_CHAR && !('A' <= c1 && c1 <= 'Z')) {
                // wrong kind of char; bail out here
                if (buf != null) {
                    buf.append(signature, i - c1reps, len);
                }
                break;
            }
            // shift in the next char:
            c0 = c1; c1 = (i == len ? NO_CHAR : signature.charAt(i));
            if (c1 == c0) { ++c1reps; continue; }
            // shift in the next count:
            int c0reps = c1reps; c1reps = 1;
            // end of a  character run
            if (c0reps < MIN_RUN) {
                if (buf != null) {
                    while (--c0reps >= 0)
                        buf.append((char)c0);
                }
                continue;
            }
            // found three or more in a row
            if (buf == null)
                buf = new StringBuilder().append(signature, 0, i - c0reps);
            buf.append((char)c0).append(c0reps);
        }
        return (buf == null) ? signature : buf.toString();
    }

    static final class Name {
        final BasicType type;
        @Stable short index;
        final NamedFunction function;
        final Object constraint;  // additional type information, if not null
        @Stable final Object[] arguments;

        private Name(int index, BasicType type, NamedFunction function, Object[] arguments) {
            this.index = (short)index;
            this.type = type;
            this.function = function;
            this.arguments = arguments;
            this.constraint = null;
            assert(this.index == index);
        }
        private Name(Name that, Object constraint) {
            this.index = that.index;
            this.type = that.type;
            this.function = that.function;
            this.arguments = that.arguments;
            this.constraint = constraint;
            assert(constraint == null || isParam());  // only params have constraints
            assert(constraint == null || constraint instanceof ClassSpecializer.SpeciesData || constraint instanceof Class);
        }
        Name(MethodHandle function, Object... arguments) {
            this(new NamedFunction(function), arguments);
        }
        Name(MethodType functionType, Object... arguments) {
            this(new NamedFunction(functionType), arguments);
            assert(arguments[0] instanceof Name && ((Name)arguments[0]).type == L_TYPE);
        }
        Name(MemberName function, Object... arguments) {
            this(new NamedFunction(function), arguments);
        }
        Name(NamedFunction function, Object... arguments) {
            this(-1, function.returnType(), function, arguments = Arrays.copyOf(arguments, arguments.length, Object[].class));
            assert(typesMatch(function, arguments));
        }
        /** Create a raw parameter of the given type, with an expected index. */
        Name(int index, BasicType type) {
            this(index, type, null, null);
        }
        /** Create a raw parameter of the given type. */
        Name(BasicType type) { this(-1, type); }

        BasicType type() { return type; }
        int index() { return index; }
        boolean initIndex(int i) {
            if (index != i) {
                if (index != -1)  return false;
                index = (short)i;
            }
            return true;
        }
        char typeChar() {
            return type.btChar;
        }

        Name newIndex(int i) {
            if (initIndex(i))  return this;
            return cloneWithIndex(i);
        }
        Name cloneWithIndex(int i) {
            Object[] newArguments = (arguments == null) ? null : arguments.clone();
            return new Name(i, type, function, newArguments).withConstraint(constraint);
        }
        Name withConstraint(Object constraint) {
            if (constraint == this.constraint)  return this;
            return new Name(this, constraint);
        }
        Name replaceName(Name oldName, Name newName) {  // FIXME: use replaceNames uniformly
            if (oldName == newName)  return this;
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            Object[] arguments = this.arguments;
            if (arguments == null)  return this;
            boolean replaced = false;
            for (int j = 0; j < arguments.length; j++) {
                if (arguments[j] == oldName) {
                    if (!replaced) {
                        replaced = true;
                        arguments = arguments.clone();
                    }
                    arguments[j] = newName;
                }
            }
            if (!replaced)  return this;
            return new Name(function, arguments);
        }
        /** In the arguments of this Name, replace oldNames[i] pairwise by newNames[i].
         *  Limit such replacements to {@code start<=i<end}.  Return possibly changed self.
         */
        Name replaceNames(Name[] oldNames, Name[] newNames, int start, int end) {
            if (start >= end)  return this;
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            Object[] arguments = this.arguments;
            boolean replaced = false;
        eachArg:
            for (int j = 0; j < arguments.length; j++) {
                if (arguments[j] instanceof Name n) {
                    int check = n.index;
                    // harmless check to see if the thing is already in newNames:
                    if (check >= 0 && check < newNames.length && n == newNames[check])
                        continue eachArg;
                    // n might not have the correct index: n != oldNames[n.index].
                    for (int i = start; i < end; i++) {
                        if (n == oldNames[i]) {
                            if (n == newNames[i])
                                continue eachArg;
                            if (!replaced) {
                                replaced = true;
                                arguments = arguments.clone();
                            }
                            arguments[j] = newNames[i];
                            continue eachArg;
                        }
                    }
                }
            }
            if (!replaced)  return this;
            return new Name(function, arguments);
        }
        void internArguments() {
            @SuppressWarnings("LocalVariableHidesMemberVariable")
            Object[] arguments = this.arguments;
            for (int j = 0; j < arguments.length; j++) {
                if (arguments[j] instanceof Name n) {
                    if (n.isParam() && n.index < INTERNED_ARGUMENT_LIMIT)
                        arguments[j] = internArgument(n);
                }
            }
        }
        boolean isParam() {
            return function == null;
        }
        boolean isConstantZero() {
            return !isParam() && arguments.length == 0 && function.isConstantZero();
        }

        boolean refersTo(Class<?> declaringClass, String methodName) {
            return function != null &&
                    function.member() != null && function.member().refersTo(declaringClass, methodName);
        }

        /**
         * Check if MemberName is a call to MethodHandle.invokeBasic.
         */
        boolean isInvokeBasic() {
            if (function == null)
                return false;
            if (arguments.length < 1)
                return false;  // must have MH argument
            MemberName member = function.member();
            return member != null && member.refersTo(MethodHandle.class, "invokeBasic") &&
                    !member.isPublic() && !member.isStatic();
        }

        /**
         * Check if MemberName is a call to MethodHandle.linkToStatic, etc.
         */
        boolean isLinkerMethodInvoke() {
            if (function == null)
                return false;
            if (arguments.length < 1)
                return false;  // must have MH argument
            MemberName member = function.member();
            return member != null &&
                    member.getDeclaringClass() == MethodHandle.class &&
                    !member.isPublic() && member.isStatic() &&
                    member.getName().startsWith("linkTo");
        }

        public String toString() {
            return (isParam()?"a":"t")+(index >= 0 ? index : System.identityHashCode(this))+":"+typeChar();
        }
        public String debugString() {
            String s = paramString();
            return (function == null) ? s : s + "=" + exprString();
        }
        public String paramString() {
            String s = toString();
            Object c = constraint;
            if (c == null)
                return s;
            if (c instanceof Class)  c = ((Class<?>)c).getSimpleName();
            return s + "/" + c;
        }
        public String exprString() {
            if (function == null)  return toString();
            StringBuilder buf = new StringBuilder(function.toString());
            buf.append("(");
            String cma = "";
            for (Object a : arguments) {
                buf.append(cma); cma = ",";
                if (a instanceof Name || a instanceof Integer)
                    buf.append(a);
                else
                    buf.append("(").append(a).append(")");
            }
            buf.append(")");
            return buf.toString();
        }

        private boolean typesMatch(NamedFunction function, Object ... arguments) {
            assert(arguments.length == function.arity()) : "arity mismatch: arguments.length=" + arguments.length + " == function.arity()=" + function.arity() + " in " + debugString();
            for (int i = 0; i < arguments.length; i++) {
                assert (typesMatch(function.parameterType(i), arguments[i])) : "types don't match: function.parameterType(" + i + ")=" + function.parameterType(i) + ", arguments[" + i + "]=" + arguments[i] + " in " + debugString();
            }
            return true;
        }

        private static boolean typesMatch(BasicType parameterType, Object object) {
            if (object instanceof Name) {
                return ((Name)object).type == parameterType;
            }
            switch (parameterType) {
                case I_TYPE:  return object instanceof Integer;
                case J_TYPE:  return object instanceof Long;
                case F_TYPE:  return object instanceof Float;
                case D_TYPE:  return object instanceof Double;
            }
            assert(parameterType == L_TYPE);
            return true;
        }

        /** Return the index of the last occurrence of n in the argument array.
         *  Return -1 if the name is not used.
         */
        int lastUseIndex(Name n) {
            if (arguments == null)  return -1;
            for (int i = arguments.length; --i >= 0; ) {
                if (arguments[i] == n)  return i;
            }
            return -1;
        }

        /** Return the number of occurrences of n in the argument array.
         *  Return 0 if the name is not used.
         */
        int useCount(Name n) {
            int count = 0;
            if (arguments != null) {
                for (Object argument : arguments) {
                    if (argument == n) {
                        count++;
                    }
                }
            }
            return count;
        }

        public boolean equals(Name that) {
            if (this == that)  return true;
            if (isParam())
                // each parameter is a unique atom
                return false;  // this != that
            return
                //this.index == that.index &&
                this.type == that.type &&
                this.function.equals(that.function) &&
                Arrays.equals(this.arguments, that.arguments);
        }
        @Override
        public boolean equals(Object x) {
            return x instanceof Name && equals((Name)x);
        }
        @Override
        public int hashCode() {
            if (isParam())
                return index | (type.ordinal() << 8);
            return function.hashCode() ^ Arrays.hashCode(arguments);
        }
    }

    /** Return the index of the last name which contains n as an argument.
     *  Return -1 if the name is not used.  Return names.length if it is the return value.
     */
    int lastUseIndex(Name n) {
        int ni = n.index, nmax = names.length;
        assert(names[ni] == n);
        if (result == ni)  return nmax;  // live all the way beyond the end
        for (int i = nmax; --i > ni; ) {
            if (names[i].lastUseIndex(n) >= 0)
                return i;
        }
        return -1;
    }

    /** Return the number of times n is used as an argument or return value. */
    int useCount(Name n) {
        int count = (result == n.index) ? 1 : 0;
        int i = Math.max(n.index + 1, arity);
        while (i < names.length) {
            count += names[i++].useCount(n);
        }
        return count;
    }

    static Name argument(int which, BasicType type) {
        if (which >= INTERNED_ARGUMENT_LIMIT)
            return new Name(which, type);
        return INTERNED_ARGUMENTS[type.ordinal()][which];
    }
    static Name internArgument(Name n) {
        assert(n.isParam()) : "not param: " + n;
        assert(n.index < INTERNED_ARGUMENT_LIMIT);
        if (n.constraint != null)  return n;
        return argument(n.index, n.type);
    }
    static Name[] arguments(int extra, MethodType types) {
        int length = types.parameterCount();
        Name[] names = new Name[length + extra];
        for (int i = 0; i < length; i++)
            names[i] = argument(i, basicType(types.parameterType(i)));
        return names;
    }
    static final int INTERNED_ARGUMENT_LIMIT = 10;
    private static final Name[][] INTERNED_ARGUMENTS
            = new Name[ARG_TYPE_LIMIT][INTERNED_ARGUMENT_LIMIT];
    static {
        for (BasicType type : BasicType.ARG_TYPES) {
            int ord = type.ordinal();
            for (int i = 0; i < INTERNED_ARGUMENTS[ord].length; i++) {
                INTERNED_ARGUMENTS[ord][i] = new Name(i, type);
            }
        }
    }

    private static final MemberName.Factory IMPL_NAMES = MemberName.getFactory();

    static LambdaForm identityForm(BasicType type) {
        int ord = type.ordinal();
        LambdaForm form = LF_identity[ord];
        if (form != null) {
            return form;
        }
        createFormsFor(type);
        return LF_identity[ord];
    }

    static LambdaForm zeroForm(BasicType type) {
        int ord = type.ordinal();
        LambdaForm form = LF_zero[ord];
        if (form != null) {
            return form;
        }
        createFormsFor(type);
        return LF_zero[ord];
    }

    static NamedFunction identity(BasicType type) {
        int ord = type.ordinal();
        NamedFunction function = NF_identity[ord];
        if (function != null) {
            return function;
        }
        createFormsFor(type);
        return NF_identity[ord];
    }

    static NamedFunction constantZero(BasicType type) {
        int ord = type.ordinal();
        NamedFunction function = NF_zero[ord];
        if (function != null) {
            return function;
        }
        createFormsFor(type);
        return NF_zero[ord];
    }

    private static final @Stable LambdaForm[] LF_identity = new LambdaForm[TYPE_LIMIT];
    private static final @Stable LambdaForm[] LF_zero = new LambdaForm[TYPE_LIMIT];
    private static final @Stable NamedFunction[] NF_identity = new NamedFunction[TYPE_LIMIT];
    private static final @Stable NamedFunction[] NF_zero = new NamedFunction[TYPE_LIMIT];

    private static final Object createFormsLock = new Object();
    private static void createFormsFor(BasicType type) {
        // Avoid racy initialization during bootstrap
        UNSAFE.ensureClassInitialized(BoundMethodHandle.class);
        synchronized (createFormsLock) {
            final int ord = type.ordinal();
            LambdaForm idForm = LF_identity[ord];
            if (idForm != null) {
                return;
            }
            char btChar = type.basicTypeChar();
            boolean isVoid = (type == V_TYPE);
            Class<?> btClass = type.btClass;
            MethodType zeType = MethodType.methodType(btClass);
            MethodType idType = (isVoid) ? zeType : MethodType.methodType(btClass, btClass);

            // Look up symbolic names.  It might not be necessary to have these,
            // but if we need to emit direct references to bytecodes, it helps.
            // Zero is built from a call to an identity function with a constant zero input.
            MemberName idMem = new MemberName(LambdaForm.class, "identity_"+btChar, idType, REF_invokeStatic);
            MemberName zeMem = null;
            try {
                idMem = IMPL_NAMES.resolveOrFail(REF_invokeStatic, idMem, null, LM_TRUSTED, NoSuchMethodException.class);
                if (!isVoid) {
                    zeMem = new MemberName(LambdaForm.class, "zero_"+btChar, zeType, REF_invokeStatic);
                    zeMem = IMPL_NAMES.resolveOrFail(REF_invokeStatic, zeMem, null, LM_TRUSTED, NoSuchMethodException.class);
                }
            } catch (IllegalAccessException|NoSuchMethodException ex) {
                throw newInternalError(ex);
            }

            NamedFunction idFun;
            LambdaForm zeForm;
            NamedFunction zeFun;

            // Create the LFs and NamedFunctions. Precompiling LFs to byte code is needed to break circular
            // bootstrap dependency on this method in case we're interpreting LFs
            if (isVoid) {
                Name[] idNames = new Name[] { argument(0, L_TYPE) };
                idForm = new LambdaForm(1, idNames, VOID_RESULT, Kind.IDENTITY);
                idForm.compileToBytecode();
                idFun = new NamedFunction(idMem, SimpleMethodHandle.make(idMem.getInvocationType(), idForm));

                zeForm = idForm;
                zeFun = idFun;
            } else {
                Name[] idNames = new Name[] { argument(0, L_TYPE), argument(1, type) };
                idForm = new LambdaForm(2, idNames, 1, Kind.IDENTITY);
                idForm.compileToBytecode();
                idFun = new NamedFunction(idMem, MethodHandleImpl.makeIntrinsic(SimpleMethodHandle.make(idMem.getInvocationType(), idForm),
                            MethodHandleImpl.Intrinsic.IDENTITY));

                Object zeValue = Wrapper.forBasicType(btChar).zero();
                Name[] zeNames = new Name[] { argument(0, L_TYPE), new Name(idFun, zeValue) };
                zeForm = new LambdaForm(1, zeNames, 1, Kind.ZERO);
                zeForm.compileToBytecode();
                zeFun = new NamedFunction(zeMem, MethodHandleImpl.makeIntrinsic(SimpleMethodHandle.make(zeMem.getInvocationType(), zeForm),
                        MethodHandleImpl.Intrinsic.ZERO));
            }

            LF_zero[ord] = zeForm;
            NF_zero[ord] = zeFun;
            LF_identity[ord] = idForm;
            NF_identity[ord] = idFun;

            assert(idFun.isIdentity());
            assert(zeFun.isConstantZero());
            assert(new Name(zeFun).isConstantZero());
        }
    }

    // Avoid appealing to ValueConversions at bootstrap time:
    private static int identity_I(int x) { return x; }
    private static long identity_J(long x) { return x; }
    private static float identity_F(float x) { return x; }
    private static double identity_D(double x) { return x; }
    private static Object identity_L(Object x) { return x; }
    private static void identity_V() { return; }
    private static int zero_I() { return 0; }
    private static long zero_J() { return 0; }
    private static float zero_F() { return 0; }
    private static double zero_D() { return 0; }
    private static Object zero_L() { return null; }

    /**
     * Internal marker for byte-compiled LambdaForms.
     */
    /*non-public*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Compiled {
    }

    private static final HashMap<String,Integer> DEBUG_NAME_COUNTERS;
    private static final HashMap<LambdaForm,String> DEBUG_NAMES;
    static {
        if (debugEnabled()) {
            DEBUG_NAME_COUNTERS = new HashMap<>();
            DEBUG_NAMES = new HashMap<>();
        } else {
            DEBUG_NAME_COUNTERS = null;
            DEBUG_NAMES = null;
        }
    }

    static {
        // The Holder class will contain pre-generated forms resolved
        // using MemberName.getFactory(). However, that doesn't initialize the
        // class, which subtly breaks inlining etc. By forcing
        // initialization of the Holder class we avoid these issues.
        UNSAFE.ensureClassInitialized(Holder.class);
    }

    /* Placeholder class for zero and identity forms generated ahead of time */
    final class Holder {}

    // The following hack is necessary in order to suppress TRACE_INTERPRETER
    // during execution of the static initializes of this class.
    // Turning on TRACE_INTERPRETER too early will cause
    // stack overflows and other misbehavior during attempts to trace events
    // that occur during LambdaForm.<clinit>.
    // Therefore, do not move this line higher in this file, and do not remove.
    private static final boolean TRACE_INTERPRETER = MethodHandleStatics.TRACE_INTERPRETER;
}
