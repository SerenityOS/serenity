/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.invoke.util;

import java.lang.invoke.MethodType;
import sun.invoke.empty.Empty;

/**
 * This class centralizes information about the JVM verifier
 * and its requirements about type correctness.
 * @author jrose
 */
public class VerifyType {

    private VerifyType() { }  // cannot instantiate

    /**
     * True if a value can be stacked as the source type and unstacked as the
     * destination type, without violating the JVM's type consistency.
     * <p>
     * If both types are references, we apply the verifier's subclass check
     * (or subtyping, if keepInterfaces).
     * If the src type is a type guaranteed to be null (Void) it can be converted
     * to any other reference type.
     * <p>
     * If both types are primitives, we apply the verifier's primitive conversions.
     * These do not include Java conversions such as long to double, since those
     * require computation and (in general) stack depth changes.
     * But very simple 32-bit viewing changes, such as byte to int,
     * are null conversions, because they do not require any computation.
     * These conversions are from any type to a wider type up to 32 bits,
     * as long as the conversion is not signed to unsigned (byte to char).
     * <p>
     * The primitive type 'void' does not interconvert with any other type,
     * even though it is legal to drop any type from the stack and "return void".
     * The stack effects, though are different between void and any other type,
     * so it is safer to report a non-trivial conversion.
     *
     * @param src the type of a stacked value
     * @param dst the type by which we'd like to treat it
     * @param keepInterfaces if false, we treat any interface as if it were Object
     * @return whether the retyping can be done without motion or reformatting
     */
    public static boolean isNullConversion(Class<?> src, Class<?> dst, boolean keepInterfaces) {
        if (src == dst)            return true;
        // Verifier allows any interface to be treated as Object:
        if (!keepInterfaces) {
            if (dst.isInterface())  dst = Object.class;
            if (src.isInterface())  src = Object.class;
            if (src == dst)         return true;  // check again
        }
        if (isNullType(src))       return !dst.isPrimitive();
        if (!src.isPrimitive())    return dst.isAssignableFrom(src);
        if (!dst.isPrimitive())    return false;
        // Verifier allows an int to carry byte, short, char, or even boolean:
        Wrapper sw = Wrapper.forPrimitiveType(src);
        if (dst == int.class)      return sw.isSubwordOrInt();
        Wrapper dw = Wrapper.forPrimitiveType(dst);
        if (!sw.isSubwordOrInt())  return false;
        if (!dw.isSubwordOrInt())  return false;
        if (!dw.isSigned() && sw.isSigned())  return false;
        return dw.bitWidth() > sw.bitWidth();
    }

    /**
     * Specialization of isNullConversion to reference types.
     * @param src the type of a stacked value
     * @param dst the reference type by which we'd like to treat it
     * @return whether the retyping can be done without a cast
     */
    public static boolean isNullReferenceConversion(Class<?> src, Class<?> dst) {
        assert(!dst.isPrimitive());
        if (dst.isInterface())  return true;   // verifier allows this
        if (isNullType(src))    return true;
        return dst.isAssignableFrom(src);
    }

    /**
     * Is the given type java.lang.Null or an equivalent null-only type?
     */
    public static boolean isNullType(Class<?> type) {
        // Any reference statically typed as Void is guaranteed to be null.
        // Therefore, it can be safely treated as a value of any
        // other type that admits null, i.e., a reference type.
        if (type == Void.class)  return true;
        // Locally known null-only class:
        if (type == Empty.class)  return true;
        return false;
    }

    /**
     * True if a method handle can receive a call under a slightly different
     * method type, without moving or reformatting any stack elements.
     *
     * @param call the type of call being made
     * @param recv the type of the method handle receiving the call
     * @return whether the retyping can be done without motion or reformatting
     */
    public static boolean isNullConversion(MethodType call, MethodType recv, boolean keepInterfaces) {
        if (call == recv)  return true;
        int len = call.parameterCount();
        if (len != recv.parameterCount())  return false;
        for (int i = 0; i < len; i++)
            if (!isNullConversion(call.parameterType(i), recv.parameterType(i), keepInterfaces))
                return false;
        return isNullConversion(recv.returnType(), call.returnType(), keepInterfaces);
    }

    /**
     * Determine if the JVM verifier allows a value of type call to be
     * passed to a formal parameter (or return variable) of type recv.
     * Returns 1 if the verifier allows the types to match without conversion.
     * Returns -1 if the types can be made to match by a JVM-supported adapter.
     * Cases supported are:
     * <ul><li>checkcast
     * </li><li>conversion between any two integral types (but not floats)
     * </li><li>unboxing from a wrapper to its corresponding primitive type
     * </li><li>conversion in either direction between float and double
     * </li></ul>
     * (Autoboxing is not supported here; it must be done via Java code.)
     * Returns 0 otherwise.
     */
    public static int canPassUnchecked(Class<?> src, Class<?> dst) {
        if (src == dst)
            return 1;

        if (dst.isPrimitive()) {
            if (dst == void.class)
                // Return anything to a caller expecting void.
                // This is a property of the implementation, which links
                // return values via a register rather than via a stack push.
                // This makes it possible to ignore cleanly.
                return 1;
            if (src == void.class)
                return 0;  // void-to-something?
            if (!src.isPrimitive())
                // Cannot pass a reference to any primitive type (exc. void).
                return 0;
            Wrapper sw = Wrapper.forPrimitiveType(src);
            Wrapper dw = Wrapper.forPrimitiveType(dst);
            if (sw.isSubwordOrInt() && dw.isSubwordOrInt()) {
                if (sw.bitWidth() >= dw.bitWidth())
                    return -1;   // truncation may be required
                if (!dw.isSigned() && sw.isSigned())
                    return -1;   // sign elimination may be required
                return 1;
            }
            if (src == float.class || dst == float.class) {
                if (src == double.class || dst == double.class)
                    return -1;   // floating conversion may be required
                else
                    return 0;    // other primitive conversions NYI
            } else {
                // all fixed-point conversions are supported
                return 0;
            }
        } else if (src.isPrimitive()) {
            // Cannot pass a primitive to any reference type.
            // (Maybe allow null.class?)
            return 0;
        }

        // Handle reference types in the rest of the block:

        // The verifier treats interfaces exactly like Object.
        if (isNullReferenceConversion(src, dst))
            // pass any reference to object or an arb. interface
            return 1;
        // else it's a definite "maybe" (cast is required)
        return -1;
    }

    public static boolean isSpreadArgType(Class<?> spreadArg) {
        return spreadArg.isArray();
    }
    public static Class<?> spreadArgElementType(Class<?> spreadArg, int i) {
        return spreadArg.getComponentType();
    }
}
