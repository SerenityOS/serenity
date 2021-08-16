/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared.data.method.body;

import vm.runtime.defmeth.shared.data.method.param.Param;
import jdk.internal.org.objectweb.asm.Opcodes;
import vm.runtime.defmeth.shared.data.Visitor;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.Interface;

/**
 * Represents arbitrary method call (invoke*).
 */
public class CallMethod implements MethodBody {

    /** Invocation byte code instruction */
    public static enum Invoke {
        VIRTUAL(   Opcodes.INVOKEVIRTUAL,   "INVOKEVIRTUAL",   5),
        INTERFACE( Opcodes.INVOKEINTERFACE, "INVOKEINTERFACE", 9),
        SPECIAL(   Opcodes.INVOKESPECIAL,   "INVOKESPECIAL",   7),
        STATIC(    Opcodes.INVOKESTATIC,    "INVOKESTATIC",    6);

        private final int opcode;
        private final String name;

        // Kind  Description           Interpretation
        //    1   REF_getField,         getfield C.f:T
        //    2   REF_getStatic         getstatic C.f:T
        //    3   REF_putField          putfield C.f:T
        //    4   REF_putStatic         putstatic C.f:T
        //    5   REF_invokeVirtual     invokevirtual C.m:(A*)T
        //    6   REF_invokeStatic      invokestatic C.m:(A*)T
        //    7   REF_invokeSpecial     invokespecial C.m:(A*)T
        //    8   REF_newInvokeSpecial  new C; dup; invokespecial C.<init>:(A*)void
        //    9   REF_invokeInterface   invokeinterface C.m:(A*)T
        private final int tag;

        Invoke(int opcode, String name, int tag) {
            this.opcode = opcode;
            this.name = name;
            this.tag = tag;
        }

        public int opcode() {
            return opcode;
        }

        @Override
        public String toString() {
            return name;
        }

        public int tag() {
            return tag;
        }
    }

    /** At callsite direct visitMethodInsn() to issue a CONSTANT_Methodref,
        a CONSTANT_InterfaceMethodref or let it be determined at the
        callsite if the callee is an instance of an Interface */
    public static enum IndexbyteOp {
        METHODREF,
        INTERFACEMETHODREF,
        CALLSITE
    }

    /** Invoke instruction which should be used for the call */
    final Invoke invokeInsn;

    /** Static receiver class */
    final Clazz staticClass;

    /** Dynamic receiver class */
    final Clazz receiverClass;

    /** Name of the method to be invoked*/
    final String methodName;

    /** Descriptor of the method to be invoked */
    final String methodDesc;

    /** Parameter values */
    final Param[] params;

    /** Name of method's return type */
    final String returnType;

    /** Should return value be popped off the stack after the call */
    final boolean popReturnValue;

    /** Indexbyte operand to generate at call site */
    final IndexbyteOp generateIndexbyteOp;

    public CallMethod(Invoke invokeInsn, Clazz staticClass, Clazz receiverClass,
                      String methodName, String methodDesc, Param[] params,
                      String returnType, boolean popReturnValue,
                      IndexbyteOp generateIndexbyteOp) {
        this.invokeInsn = invokeInsn;
        this.staticClass = staticClass;
        this.receiverClass = receiverClass;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.params = params;
        this.returnType = returnType;
        this.popReturnValue = popReturnValue;
        this.generateIndexbyteOp = generateIndexbyteOp;
    }

    public boolean popReturnValue() {
        return popReturnValue;
    }

    public IndexbyteOp generateIndexbyteOp() {
        return generateIndexbyteOp;
    }

    public Invoke invokeInsn() {
        return invokeInsn;
    }

    public Clazz staticClass() {
        return staticClass;
    }

    public Clazz receiverClass() {
        return receiverClass;
    }

    public String methodName() {
        return methodName;
    }

    public String methodDesc() {
        return methodDesc;
    }

    public Param[] params() {
        return params;
    }

    public String returnType() {
        return returnType;
    }

    public boolean isInterface() {
        return generateIndexbyteOp() == IndexbyteOp.METHODREF ?
                     false :
                     (generateIndexbyteOp() == IndexbyteOp.INTERFACEMETHODREF ?
                         true :
                         staticClass() instanceof Interface);
    }

    public boolean isConstructorCall() {
        return invokeInsn() == Invoke.SPECIAL &&
               methodName().equals("<init>") &&
               methodDesc().equals("()V");
    }

    @Override
    public void visit(Visitor v) {
        v.visitCallMethod(this);
    }
}
