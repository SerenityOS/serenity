/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm;

/**
 * The JVM opcodes, access flags and array type codes. This interface does not define all the JVM
 * opcodes because some opcodes are automatically handled. For example, the xLOAD and xSTORE opcodes
 * are automatically replaced by xLOAD_n and xSTORE_n opcodes when possible. The xLOAD_n and
 * xSTORE_n opcodes are therefore not defined in this interface. Likewise for LDC, automatically
 * replaced by LDC_W or LDC2_W when necessary, WIDE, GOTO_W and JSR_W.
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-6.html">JVMS 6</a>
 * @author Eric Bruneton
 * @author Eugene Kuleshov
 */
// DontCheck(InterfaceIsType): can't be fixed (for backward binary compatibility).
public interface Opcodes {

    // ASM API versions.

    int ASM4 = 4 << 16 | 0 << 8;
    int ASM5 = 5 << 16 | 0 << 8;
    int ASM6 = 6 << 16 | 0 << 8;
    int ASM7 = 7 << 16 | 0 << 8;
    int ASM8 = 8 << 16 | 0 << 8;

    /**
      * <i>Experimental, use at your own risk. This field will be renamed when it becomes stable, this
      * will break existing code using it. Only code compiled with --enable-preview can use this.</i>
      *
      * @deprecated This API is experimental.
      */
    @Deprecated int ASM9_EXPERIMENTAL = 1 << 24 | 9 << 16 | 0 << 8;

    /*
      * Internal flags used to redirect calls to deprecated methods. For instance, if a visitOldStuff
      * method in API_OLD is deprecated and replaced with visitNewStuff in API_NEW, then the
      * redirection should be done as follows:
      *
      * <pre>
      * public class StuffVisitor {
      *   ...
      *
      *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
      *     // SOURCE_DEPRECATED means "a call from a deprecated method using the old 'api' value".
      *     visitNewStuf(arg | (api &#60; API_NEW ? SOURCE_DEPRECATED : 0), ...);
      *   }
      *
      *   public void visitNewStuff(int argAndSource, ...) {
      *     if (api &#60; API_NEW &#38;&#38; (argAndSource &#38; SOURCE_DEPRECATED) == 0) {
      *       visitOldStuff(argAndSource, ...);
      *     } else {
      *       int arg = argAndSource &#38; ~SOURCE_MASK;
      *       [ do stuff ]
      *     }
      *   }
      * }
      * </pre>
      *
      * <p>If 'api' is equal to API_NEW, there are two cases:
      *
      * <ul>
      *   <li>call visitNewStuff: the redirection test is skipped and 'do stuff' is executed directly.
      *   <li>call visitOldSuff: the source is not set to SOURCE_DEPRECATED before calling
      *       visitNewStuff, but the redirection test is skipped anyway in visitNewStuff, which
      *       directly executes 'do stuff'.
      * </ul>
      *
      * <p>If 'api' is equal to API_OLD, there are two cases:
      *
      * <ul>
      *   <li>call visitOldSuff: the source is set to SOURCE_DEPRECATED before calling visitNewStuff.
      *       Because of this visitNewStuff does not redirect back to visitOldStuff, and instead
      *       executes 'do stuff'.
      *   <li>call visitNewStuff: the call is redirected to visitOldStuff because the source is 0.
      *       visitOldStuff now sets the source to SOURCE_DEPRECATED and calls visitNewStuff back. This
      *       time visitNewStuff does not redirect the call, and instead executes 'do stuff'.
      * </ul>
      *
      * <h1>User subclasses</h1>
      *
      * <p>If a user subclass overrides one of these methods, there are only two cases: either 'api' is
      * API_OLD and visitOldStuff is overridden (and visitNewStuff is not), or 'api' is API_NEW or
      * more, and visitNewStuff is overridden (and visitOldStuff is not). Any other case is a user
      * programming error.
      *
      * <p>If 'api' is equal to API_NEW, the class hierarchy is equivalent to
      *
      * <pre>
      * public class StuffVisitor {
      *   &#64;Deprecated public void visitOldStuff(int arg, ...) { visitNewStuf(arg, ...); }
      *   public void visitNewStuff(int arg, ...) { [ do stuff ] }
      * }
      * class UserStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitNewStuff(int arg, ...) {
      *     super.visitNewStuff(int arg, ...); // optional
      *     [ do user stuff ]
      *   }
      * }
      * </pre>
      *
      * <p>It is then obvious that whether visitNewStuff or visitOldStuff is called, 'do stuff' and 'do
      * user stuff' will be executed, in this order.
      *
      * <p>If 'api' is equal to API_OLD, the class hierarchy is equivalent to
      *
      * <pre>
      * public class StuffVisitor {
      *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
      *     visitNewStuf(arg | SOURCE_DEPRECATED, ...);
      *   }
      *   public void visitNewStuff(int argAndSource...) {
      *     if ((argAndSource & SOURCE_DEPRECATED) == 0) {
      *       visitOldStuff(argAndSource, ...);
      *     } else {
      *       int arg = argAndSource &#38; ~SOURCE_MASK;
      *       [ do stuff ]
      *     }
      *   }
      * }
      * class UserStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitOldStuff(int arg, ...) {
      *     super.visitOldStuff(int arg, ...); // optional
      *     [ do user stuff ]
      *   }
      * }
      * </pre>
      *
      * <p>and there are two cases:
      *
      * <ul>
      *   <li>call visitOldSuff: in the call to super.visitOldStuff, the source is set to
      *       SOURCE_DEPRECATED and visitNewStuff is called. Here 'do stuff' is run because the source
      *       was previously set to SOURCE_DEPRECATED, and execution eventually returns to
      *       UserStuffVisitor.visitOldStuff, where 'do user stuff' is run.
      *   <li>call visitNewStuff: the call is redirected to UserStuffVisitor.visitOldStuff because the
      *       source is 0. Execution continues as in the previous case, resulting in 'do stuff' and 'do
      *       user stuff' being executed, in this order.
      * </ul>
      *
      * <h1>ASM subclasses</h1>
      *
      * <p>In ASM packages, subclasses of StuffVisitor can typically be sub classed again by the user,
      * and can be used with API_OLD or API_NEW. Because of this, if such a subclass must override
      * visitNewStuff, it must do so in the following way (and must not override visitOldStuff):
      *
      * <pre>
      * public class AsmStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitNewStuff(int argAndSource, ...) {
      *     if (api &#60; API_NEW &#38;&#38; (argAndSource &#38; SOURCE_DEPRECATED) == 0) {
      *       super.visitNewStuff(argAndSource, ...);
      *       return;
      *     }
      *     super.visitNewStuff(argAndSource, ...); // optional
      *     int arg = argAndSource &#38; ~SOURCE_MASK;
      *     [ do other stuff ]
      *   }
      * }
      * </pre>
      *
      * <p>If a user class extends this with 'api' equal to API_NEW, the class hierarchy is equivalent
      * to
      *
      * <pre>
      * public class StuffVisitor {
      *   &#64;Deprecated public void visitOldStuff(int arg, ...) { visitNewStuf(arg, ...); }
      *   public void visitNewStuff(int arg, ...) { [ do stuff ] }
      * }
      * public class AsmStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitNewStuff(int arg, ...) {
      *     super.visitNewStuff(arg, ...);
      *     [ do other stuff ]
      *   }
      * }
      * class UserStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitNewStuff(int arg, ...) {
      *     super.visitNewStuff(int arg, ...);
      *     [ do user stuff ]
      *   }
      * }
      * </pre>
      *
      * <p>It is then obvious that whether visitNewStuff or visitOldStuff is called, 'do stuff', 'do
      * other stuff' and 'do user stuff' will be executed, in this order. If, on the other hand, a user
      * class extends AsmStuffVisitor with 'api' equal to API_OLD, the class hierarchy is equivalent to
      *
      * <pre>
      * public class StuffVisitor {
      *   &#64;Deprecated public void visitOldStuff(int arg, ...) {
      *     visitNewStuf(arg | SOURCE_DEPRECATED, ...);
      *   }
      *   public void visitNewStuff(int argAndSource, ...) {
      *     if ((argAndSource & SOURCE_DEPRECATED) == 0) {
      *       visitOldStuff(argAndSource, ...);
      *     } else {
      *       int arg = argAndSource &#38; ~SOURCE_MASK;
      *       [ do stuff ]
      *     }
      *   }
      * }
      * public class AsmStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitNewStuff(int argAndSource, ...) {
      *     if ((argAndSource &#38; SOURCE_DEPRECATED) == 0) {
      *       super.visitNewStuff(argAndSource, ...);
      *       return;
      *     }
      *     super.visitNewStuff(argAndSource, ...); // optional
      *     int arg = argAndSource &#38; ~SOURCE_MASK;
      *     [ do other stuff ]
      *   }
      * }
      * class UserStuffVisitor extends StuffVisitor {
      *   &#64;Override public void visitOldStuff(int arg, ...) {
      *     super.visitOldStuff(arg, ...);
      *     [ do user stuff ]
      *   }
      * }
      * </pre>
      *
      * <p>and, here again, whether visitNewStuff or visitOldStuff is called, 'do stuff', 'do other
      * stuff' and 'do user stuff' will be executed, in this order (exercise left to the reader).
      *
      * <h1>Notes</h1>
      *
      * <ul>
      *   <li>the SOURCE_DEPRECATED flag is set only if 'api' is API_OLD, just before calling
      *       visitNewStuff. By hypothesis, this method is not overridden by the user. Therefore, user
      *       classes can never see this flag. Only ASM subclasses must take care of extracting the
      *       actual argument value by clearing the source flags.
      *   <li>because the SOURCE_DEPRECATED flag is immediately cleared in the caller, the caller can
      *       call visitOldStuff or visitNewStuff (in 'do stuff' and 'do user stuff') on a delegate
      *       visitor without any risks (breaking the redirection logic, "leaking" the flag, etc).
      *   <li>all the scenarios discussed above are unit tested in MethodVisitorTest.
      * </ul>
      */

    int SOURCE_DEPRECATED = 0x100;
    int SOURCE_MASK = SOURCE_DEPRECATED;

    // Java ClassFile versions (the minor version is stored in the 16 most significant bits, and the
    // major version in the 16 least significant bits).

    int V1_1 = 3 << 16 | 45;
    int V1_2 = 0 << 16 | 46;
    int V1_3 = 0 << 16 | 47;
    int V1_4 = 0 << 16 | 48;
    int V1_5 = 0 << 16 | 49;
    int V1_6 = 0 << 16 | 50;
    int V1_7 = 0 << 16 | 51;
    int V1_8 = 0 << 16 | 52;
    int V9 = 0 << 16 | 53;
    int V10 = 0 << 16 | 54;
    int V11 = 0 << 16 | 55;
    int V12 = 0 << 16 | 56;
    int V13 = 0 << 16 | 57;
    int V14 = 0 << 16 | 58;
    int V15 = 0 << 16 | 59;
    int V16 = 0 << 16 | 60;
    int V17 = 0 << 16 | 61;
    int V18 = 0 << 16 | 62;

    /**
      * Version flag indicating that the class is using 'preview' features.
      *
      * <p>{@code version & V_PREVIEW == V_PREVIEW} tests if a version is flagged with {@code
      * V_PREVIEW}.
      */
    int V_PREVIEW = 0xFFFF0000;

    // Access flags values, defined in
    // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1-200-E.1
    // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5-200-A.1
    // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.6-200-A.1
    // - https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25

    int ACC_PUBLIC = 0x0001; // class, field, method
    int ACC_PRIVATE = 0x0002; // class, field, method
    int ACC_PROTECTED = 0x0004; // class, field, method
    int ACC_STATIC = 0x0008; // field, method
    int ACC_FINAL = 0x0010; // class, field, method, parameter
    int ACC_SUPER = 0x0020; // class
    int ACC_SYNCHRONIZED = 0x0020; // method
    int ACC_OPEN = 0x0020; // module
    int ACC_TRANSITIVE = 0x0020; // module requires
    int ACC_VOLATILE = 0x0040; // field
    int ACC_BRIDGE = 0x0040; // method
    int ACC_STATIC_PHASE = 0x0040; // module requires
    int ACC_VARARGS = 0x0080; // method
    int ACC_TRANSIENT = 0x0080; // field
    int ACC_NATIVE = 0x0100; // method
    int ACC_INTERFACE = 0x0200; // class
    int ACC_ABSTRACT = 0x0400; // class, method
    int ACC_STRICT = 0x0800; // method
    int ACC_SYNTHETIC = 0x1000; // class, field, method, parameter, module *
    int ACC_ANNOTATION = 0x2000; // class
    int ACC_ENUM = 0x4000; // class(?) field inner
    int ACC_MANDATED = 0x8000; // field, method, parameter, module, module *
    int ACC_MODULE = 0x8000; // class

    // ASM specific access flags.
    // WARNING: the 16 least significant bits must NOT be used, to avoid conflicts with standard
    // access flags, and also to make sure that these flags are automatically filtered out when
    // written in class files (because access flags are stored using 16 bits only).

    int ACC_RECORD = 0x10000; // class
    int ACC_DEPRECATED = 0x20000; // class, field, method

    // Possible values for the type operand of the NEWARRAY instruction.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html#jvms-6.5.newarray.

    int T_BOOLEAN = 4;
    int T_CHAR = 5;
    int T_FLOAT = 6;
    int T_DOUBLE = 7;
    int T_BYTE = 8;
    int T_SHORT = 9;
    int T_INT = 10;
    int T_LONG = 11;

    // Possible values for the reference_kind field of CONSTANT_MethodHandle_info structures.
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.8.

    int H_GETFIELD = 1;
    int H_GETSTATIC = 2;
    int H_PUTFIELD = 3;
    int H_PUTSTATIC = 4;
    int H_INVOKEVIRTUAL = 5;
    int H_INVOKESTATIC = 6;
    int H_INVOKESPECIAL = 7;
    int H_NEWINVOKESPECIAL = 8;
    int H_INVOKEINTERFACE = 9;

    // ASM specific stack map frame types, used in {@link ClassVisitor#visitFrame}.

    /** An expanded frame. See {@link ClassReader#EXPAND_FRAMES}. */
    int F_NEW = -1;

    /** A compressed frame with complete frame data. */
    int F_FULL = 0;

    /**
      * A compressed frame where locals are the same as the locals in the previous frame, except that
      * additional 1-3 locals are defined, and with an empty stack.
      */
    int F_APPEND = 1;

    /**
      * A compressed frame where locals are the same as the locals in the previous frame, except that
      * the last 1-3 locals are absent and with an empty stack.
      */
    int F_CHOP = 2;

    /**
      * A compressed frame with exactly the same locals as the previous frame and with an empty stack.
      */
    int F_SAME = 3;

    /**
      * A compressed frame with exactly the same locals as the previous frame and with a single value
      * on the stack.
      */
    int F_SAME1 = 4;

    // Standard stack map frame element types, used in {@link ClassVisitor#visitFrame}.

    Integer TOP = Frame.ITEM_TOP;
    Integer INTEGER = Frame.ITEM_INTEGER;
    Integer FLOAT = Frame.ITEM_FLOAT;
    Integer DOUBLE = Frame.ITEM_DOUBLE;
    Integer LONG = Frame.ITEM_LONG;
    Integer NULL = Frame.ITEM_NULL;
    Integer UNINITIALIZED_THIS = Frame.ITEM_UNINITIALIZED_THIS;

    // The JVM opcode values (with the MethodVisitor method name used to visit them in comment, and
    // where '-' means 'same method name as on the previous line').
    // See https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-6.html.

    int NOP = 0; // visitInsn
    int ACONST_NULL = 1; // -
    int ICONST_M1 = 2; // -
    int ICONST_0 = 3; // -
    int ICONST_1 = 4; // -
    int ICONST_2 = 5; // -
    int ICONST_3 = 6; // -
    int ICONST_4 = 7; // -
    int ICONST_5 = 8; // -
    int LCONST_0 = 9; // -
    int LCONST_1 = 10; // -
    int FCONST_0 = 11; // -
    int FCONST_1 = 12; // -
    int FCONST_2 = 13; // -
    int DCONST_0 = 14; // -
    int DCONST_1 = 15; // -
    int BIPUSH = 16; // visitIntInsn
    int SIPUSH = 17; // -
    int LDC = 18; // visitLdcInsn
    int ILOAD = 21; // visitVarInsn
    int LLOAD = 22; // -
    int FLOAD = 23; // -
    int DLOAD = 24; // -
    int ALOAD = 25; // -
    int IALOAD = 46; // visitInsn
    int LALOAD = 47; // -
    int FALOAD = 48; // -
    int DALOAD = 49; // -
    int AALOAD = 50; // -
    int BALOAD = 51; // -
    int CALOAD = 52; // -
    int SALOAD = 53; // -
    int ISTORE = 54; // visitVarInsn
    int LSTORE = 55; // -
    int FSTORE = 56; // -
    int DSTORE = 57; // -
    int ASTORE = 58; // -
    int IASTORE = 79; // visitInsn
    int LASTORE = 80; // -
    int FASTORE = 81; // -
    int DASTORE = 82; // -
    int AASTORE = 83; // -
    int BASTORE = 84; // -
    int CASTORE = 85; // -
    int SASTORE = 86; // -
    int POP = 87; // -
    int POP2 = 88; // -
    int DUP = 89; // -
    int DUP_X1 = 90; // -
    int DUP_X2 = 91; // -
    int DUP2 = 92; // -
    int DUP2_X1 = 93; // -
    int DUP2_X2 = 94; // -
    int SWAP = 95; // -
    int IADD = 96; // -
    int LADD = 97; // -
    int FADD = 98; // -
    int DADD = 99; // -
    int ISUB = 100; // -
    int LSUB = 101; // -
    int FSUB = 102; // -
    int DSUB = 103; // -
    int IMUL = 104; // -
    int LMUL = 105; // -
    int FMUL = 106; // -
    int DMUL = 107; // -
    int IDIV = 108; // -
    int LDIV = 109; // -
    int FDIV = 110; // -
    int DDIV = 111; // -
    int IREM = 112; // -
    int LREM = 113; // -
    int FREM = 114; // -
    int DREM = 115; // -
    int INEG = 116; // -
    int LNEG = 117; // -
    int FNEG = 118; // -
    int DNEG = 119; // -
    int ISHL = 120; // -
    int LSHL = 121; // -
    int ISHR = 122; // -
    int LSHR = 123; // -
    int IUSHR = 124; // -
    int LUSHR = 125; // -
    int IAND = 126; // -
    int LAND = 127; // -
    int IOR = 128; // -
    int LOR = 129; // -
    int IXOR = 130; // -
    int LXOR = 131; // -
    int IINC = 132; // visitIincInsn
    int I2L = 133; // visitInsn
    int I2F = 134; // -
    int I2D = 135; // -
    int L2I = 136; // -
    int L2F = 137; // -
    int L2D = 138; // -
    int F2I = 139; // -
    int F2L = 140; // -
    int F2D = 141; // -
    int D2I = 142; // -
    int D2L = 143; // -
    int D2F = 144; // -
    int I2B = 145; // -
    int I2C = 146; // -
    int I2S = 147; // -
    int LCMP = 148; // -
    int FCMPL = 149; // -
    int FCMPG = 150; // -
    int DCMPL = 151; // -
    int DCMPG = 152; // -
    int IFEQ = 153; // visitJumpInsn
    int IFNE = 154; // -
    int IFLT = 155; // -
    int IFGE = 156; // -
    int IFGT = 157; // -
    int IFLE = 158; // -
    int IF_ICMPEQ = 159; // -
    int IF_ICMPNE = 160; // -
    int IF_ICMPLT = 161; // -
    int IF_ICMPGE = 162; // -
    int IF_ICMPGT = 163; // -
    int IF_ICMPLE = 164; // -
    int IF_ACMPEQ = 165; // -
    int IF_ACMPNE = 166; // -
    int GOTO = 167; // -
    int JSR = 168; // -
    int RET = 169; // visitVarInsn
    int TABLESWITCH = 170; // visiTableSwitchInsn
    int LOOKUPSWITCH = 171; // visitLookupSwitch
    int IRETURN = 172; // visitInsn
    int LRETURN = 173; // -
    int FRETURN = 174; // -
    int DRETURN = 175; // -
    int ARETURN = 176; // -
    int RETURN = 177; // -
    int GETSTATIC = 178; // visitFieldInsn
    int PUTSTATIC = 179; // -
    int GETFIELD = 180; // -
    int PUTFIELD = 181; // -
    int INVOKEVIRTUAL = 182; // visitMethodInsn
    int INVOKESPECIAL = 183; // -
    int INVOKESTATIC = 184; // -
    int INVOKEINTERFACE = 185; // -
    int INVOKEDYNAMIC = 186; // visitInvokeDynamicInsn
    int NEW = 187; // visitTypeInsn
    int NEWARRAY = 188; // visitIntInsn
    int ANEWARRAY = 189; // visitTypeInsn
    int ARRAYLENGTH = 190; // visitInsn
    int ATHROW = 191; // -
    int CHECKCAST = 192; // visitTypeInsn
    int INSTANCEOF = 193; // -
    int MONITORENTER = 194; // visitInsn
    int MONITOREXIT = 195; // -
    int MULTIANEWARRAY = 197; // visitMultiANewArrayInsn
    int IFNULL = 198; // visitJumpInsn
    int IFNONNULL = 199; // -
}
