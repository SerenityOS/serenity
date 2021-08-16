/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import sun.invoke.util.BytecodeDescriptor;
import sun.invoke.util.Wrapper;
import static sun.invoke.util.Wrapper.*;

class TypeConvertingMethodAdapter extends MethodVisitor {

    TypeConvertingMethodAdapter(MethodVisitor mv) {
        super(Opcodes.ASM7, mv);
    }

    private static final int NUM_WRAPPERS = Wrapper.COUNT;

    private static final String NAME_OBJECT = "java/lang/Object";
    private static final String WRAPPER_PREFIX = "Ljava/lang/";

    // Same for all primitives; name of the boxing method
    private static final String NAME_BOX_METHOD = "valueOf";

    // Table of opcodes for widening primitive conversions; NOP = no conversion
    private static final int[][] wideningOpcodes = new int[NUM_WRAPPERS][NUM_WRAPPERS];

    private static final Wrapper[] FROM_WRAPPER_NAME = new Wrapper[16];

    // Table of wrappers for primitives, indexed by ASM type sorts
    private static final Wrapper[] FROM_TYPE_SORT = new Wrapper[12];

    static {
        for (Wrapper w : Wrapper.values()) {
            if (w.basicTypeChar() != 'L') {
                int wi = hashWrapperName(w.wrapperSimpleName());
                assert (FROM_WRAPPER_NAME[wi] == null);
                FROM_WRAPPER_NAME[wi] = w;
            }
        }

        // wideningOpcodes[][] will be NOP-initialized by default
        assert(Opcodes.NOP == 0);

        initWidening(LONG,   Opcodes.I2L, BYTE, SHORT, INT, CHAR);
        initWidening(LONG,   Opcodes.F2L, FLOAT);
        initWidening(FLOAT,  Opcodes.I2F, BYTE, SHORT, INT, CHAR);
        initWidening(FLOAT,  Opcodes.L2F, LONG);
        initWidening(DOUBLE, Opcodes.I2D, BYTE, SHORT, INT, CHAR);
        initWidening(DOUBLE, Opcodes.F2D, FLOAT);
        initWidening(DOUBLE, Opcodes.L2D, LONG);

        FROM_TYPE_SORT[Type.BYTE] = Wrapper.BYTE;
        FROM_TYPE_SORT[Type.SHORT] = Wrapper.SHORT;
        FROM_TYPE_SORT[Type.INT] = Wrapper.INT;
        FROM_TYPE_SORT[Type.LONG] = Wrapper.LONG;
        FROM_TYPE_SORT[Type.CHAR] = Wrapper.CHAR;
        FROM_TYPE_SORT[Type.FLOAT] = Wrapper.FLOAT;
        FROM_TYPE_SORT[Type.DOUBLE] = Wrapper.DOUBLE;
        FROM_TYPE_SORT[Type.BOOLEAN] = Wrapper.BOOLEAN;
    }

    private static void initWidening(Wrapper to, int opcode, Wrapper... from) {
        for (Wrapper f : from) {
            wideningOpcodes[f.ordinal()][to.ordinal()] = opcode;
        }
    }

    /**
     * Class name to Wrapper hash, derived from Wrapper.hashWrap()
     * @param xn
     * @return The hash code 0-15
     */
    private static int hashWrapperName(String xn) {
        if (xn.length() < 3) {
            return 0;
        }
        return (3 * xn.charAt(1) + xn.charAt(2)) % 16;
    }

    private Wrapper wrapperOrNullFromDescriptor(String desc) {
        if (!desc.startsWith(WRAPPER_PREFIX)) {
            // Not a class type (array or method), so not a boxed type
            // or not in the right package
            return null;
        }
        // Pare it down to the simple class name
        String cname = desc.substring(WRAPPER_PREFIX.length(), desc.length() - 1);
        // Hash to a Wrapper
        Wrapper w = FROM_WRAPPER_NAME[hashWrapperName(cname)];
        if (w == null || w.wrapperSimpleName().equals(cname)) {
            return w;
        } else {
            return null;
        }
    }

    private static String wrapperName(Wrapper w) {
        return "java/lang/" + w.wrapperSimpleName();
    }

    private static String unboxMethod(Wrapper w) {
        return w.primitiveSimpleName() + "Value";
    }

    private static String boxingDescriptor(Wrapper w) {
        return "(" + w.basicTypeChar() + ")L" + wrapperName(w) + ";";
    }

    private static String unboxingDescriptor(Wrapper w) {
        return "()" + w.basicTypeChar();
    }

    void boxIfTypePrimitive(Type t) {
        Wrapper w = FROM_TYPE_SORT[t.getSort()];
        if (w != null) {
            box(w);
        }
    }

    void widen(Wrapper ws, Wrapper wt) {
        if (ws != wt) {
            int opcode = wideningOpcodes[ws.ordinal()][wt.ordinal()];
            if (opcode != Opcodes.NOP) {
                visitInsn(opcode);
            }
        }
    }

    void box(Wrapper w) {
        visitMethodInsn(Opcodes.INVOKESTATIC,
                wrapperName(w),
                NAME_BOX_METHOD,
                boxingDescriptor(w), false);
    }

    /**
     * Convert types by unboxing. The source type is known to be a primitive wrapper.
     * @param sname A primitive wrapper corresponding to wrapped reference source type
     * @param wt A primitive wrapper being converted to
     */
    void unbox(String sname, Wrapper wt) {
        visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                sname,
                unboxMethod(wt),
                unboxingDescriptor(wt), false);
    }

    private String descriptorToName(String desc) {
        int last = desc.length() - 1;
        if (desc.charAt(0) == 'L' && desc.charAt(last) == ';') {
            // In descriptor form
            return desc.substring(1, last);
        } else {
            // Already in internal name form
            return desc;
        }
    }

    void cast(String ds, String dt) {
        String ns = descriptorToName(ds);
        String nt = descriptorToName(dt);
        if (!nt.equals(ns) && !nt.equals(NAME_OBJECT)) {
            visitTypeInsn(Opcodes.CHECKCAST, nt);
        }
    }

    private Wrapper toWrapper(String desc) {
        char first = desc.charAt(0);
        if (first == '[' || first == '(') {
            first = 'L';
        }
        return Wrapper.forBasicType(first);
    }

    /**
     * Convert an argument of type 'arg' to be passed to 'target' assuring that it is 'functional'.
     * Insert the needed conversion instructions in the method code.
     * @param arg
     * @param target
     * @param functional
     */
    void convertType(Class<?> arg, Class<?> target, Class<?> functional) {
        if (arg.equals(target) && arg.equals(functional)) {
            return;
        }
        if (arg == Void.TYPE || target == Void.TYPE) {
            return;
        }
        if (arg.isPrimitive()) {
            Wrapper wArg = Wrapper.forPrimitiveType(arg);
            if (target.isPrimitive()) {
                // Both primitives: widening
                widen(wArg, Wrapper.forPrimitiveType(target));
            } else {
                // Primitive argument to reference target
                String dTarget = BytecodeDescriptor.unparse(target);
                Wrapper wPrimTarget = wrapperOrNullFromDescriptor(dTarget);
                if (wPrimTarget != null) {
                    // The target is a boxed primitive type, widen to get there before boxing
                    widen(wArg, wPrimTarget);
                    box(wPrimTarget);
                } else {
                    // Otherwise, box and cast
                    box(wArg);
                    cast(wrapperName(wArg), dTarget);
                }
            }
        } else {
            String dArg = BytecodeDescriptor.unparse(arg);
            String dSrc;
            if (functional.isPrimitive()) {
                dSrc = dArg;
            } else {
                // Cast to convert to possibly more specific type, and generate CCE for invalid arg
                dSrc = BytecodeDescriptor.unparse(functional);
                cast(dArg, dSrc);
            }
            String dTarget = BytecodeDescriptor.unparse(target);
            if (target.isPrimitive()) {
                Wrapper wTarget = toWrapper(dTarget);
                // Reference argument to primitive target
                Wrapper wps = wrapperOrNullFromDescriptor(dSrc);
                if (wps != null) {
                    if (wps.isSigned() || wps.isFloating()) {
                        // Boxed number to primitive
                        unbox(wrapperName(wps), wTarget);
                    } else {
                        // Character or Boolean
                        unbox(wrapperName(wps), wps);
                        widen(wps, wTarget);
                    }
                } else {
                    // Source type is reference type, but not boxed type,
                    // assume it is super type of target type
                    String intermediate;
                    if (wTarget.isSigned() || wTarget.isFloating()) {
                        // Boxed number to primitive
                        intermediate = "java/lang/Number";
                    } else {
                        // Character or Boolean
                        intermediate = wrapperName(wTarget);
                    }
                    cast(dSrc, intermediate);
                    unbox(intermediate, wTarget);
                }
            } else {
                // Both reference types: just case to target type
                cast(dSrc, dTarget);
            }
        }
    }

    /**
     * The following method is copied from
     * org.objectweb.asm.commons.InstructionAdapter. Part of ASM: a very small
     * and fast Java bytecode manipulation framework.
     * Copyright (c) 2000-2005 INRIA, France Telecom All rights reserved.
     */
    void iconst(final int cst) {
        if (cst >= -1 && cst <= 5) {
            mv.visitInsn(Opcodes.ICONST_0 + cst);
        } else if (cst >= Byte.MIN_VALUE && cst <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, cst);
        } else if (cst >= Short.MIN_VALUE && cst <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, cst);
        } else {
            mv.visitLdcInsn(cst);
        }
    }
}
