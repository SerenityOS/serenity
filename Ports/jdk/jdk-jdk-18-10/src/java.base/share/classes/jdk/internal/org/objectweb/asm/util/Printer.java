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
package jdk.internal.org.objectweb.asm.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import jdk.internal.org.objectweb.asm.Attribute;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ConstantDynamic;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.TypePath;
import jdk.internal.org.objectweb.asm.TypeReference;

/**
 * An abstract converter from visit events to text.
 *
 * @author Eric Bruneton
 */
public abstract class Printer {

    /** The names of the Java Virtual Machine opcodes. */
    public static final String[] OPCODES = {
        "NOP", // 0 (0x0)
        "ACONST_NULL", // 1 (0x1)
        "ICONST_M1", // 2 (0x2)
        "ICONST_0", // 3 (0x3)
        "ICONST_1", // 4 (0x4)
        "ICONST_2", // 5 (0x5)
        "ICONST_3", // 6 (0x6)
        "ICONST_4", // 7 (0x7)
        "ICONST_5", // 8 (0x8)
        "LCONST_0", // 9 (0x9)
        "LCONST_1", // 10 (0xa)
        "FCONST_0", // 11 (0xb)
        "FCONST_1", // 12 (0xc)
        "FCONST_2", // 13 (0xd)
        "DCONST_0", // 14 (0xe)
        "DCONST_1", // 15 (0xf)
        "BIPUSH", // 16 (0x10)
        "SIPUSH", // 17 (0x11)
        "LDC", // 18 (0x12)
        "LDC_W", // 19 (0x13)
        "LDC2_W", // 20 (0x14)
        "ILOAD", // 21 (0x15)
        "LLOAD", // 22 (0x16)
        "FLOAD", // 23 (0x17)
        "DLOAD", // 24 (0x18)
        "ALOAD", // 25 (0x19)
        "ILOAD_0", // 26 (0x1a)
        "ILOAD_1", // 27 (0x1b)
        "ILOAD_2", // 28 (0x1c)
        "ILOAD_3", // 29 (0x1d)
        "LLOAD_0", // 30 (0x1e)
        "LLOAD_1", // 31 (0x1f)
        "LLOAD_2", // 32 (0x20)
        "LLOAD_3", // 33 (0x21)
        "FLOAD_0", // 34 (0x22)
        "FLOAD_1", // 35 (0x23)
        "FLOAD_2", // 36 (0x24)
        "FLOAD_3", // 37 (0x25)
        "DLOAD_0", // 38 (0x26)
        "DLOAD_1", // 39 (0x27)
        "DLOAD_2", // 40 (0x28)
        "DLOAD_3", // 41 (0x29)
        "ALOAD_0", // 42 (0x2a)
        "ALOAD_1", // 43 (0x2b)
        "ALOAD_2", // 44 (0x2c)
        "ALOAD_3", // 45 (0x2d)
        "IALOAD", // 46 (0x2e)
        "LALOAD", // 47 (0x2f)
        "FALOAD", // 48 (0x30)
        "DALOAD", // 49 (0x31)
        "AALOAD", // 50 (0x32)
        "BALOAD", // 51 (0x33)
        "CALOAD", // 52 (0x34)
        "SALOAD", // 53 (0x35)
        "ISTORE", // 54 (0x36)
        "LSTORE", // 55 (0x37)
        "FSTORE", // 56 (0x38)
        "DSTORE", // 57 (0x39)
        "ASTORE", // 58 (0x3a)
        "ISTORE_0", // 59 (0x3b)
        "ISTORE_1", // 60 (0x3c)
        "ISTORE_2", // 61 (0x3d)
        "ISTORE_3", // 62 (0x3e)
        "LSTORE_0", // 63 (0x3f)
        "LSTORE_1", // 64 (0x40)
        "LSTORE_2", // 65 (0x41)
        "LSTORE_3", // 66 (0x42)
        "FSTORE_0", // 67 (0x43)
        "FSTORE_1", // 68 (0x44)
        "FSTORE_2", // 69 (0x45)
        "FSTORE_3", // 70 (0x46)
        "DSTORE_0", // 71 (0x47)
        "DSTORE_1", // 72 (0x48)
        "DSTORE_2", // 73 (0x49)
        "DSTORE_3", // 74 (0x4a)
        "ASTORE_0", // 75 (0x4b)
        "ASTORE_1", // 76 (0x4c)
        "ASTORE_2", // 77 (0x4d)
        "ASTORE_3", // 78 (0x4e)
        "IASTORE", // 79 (0x4f)
        "LASTORE", // 80 (0x50)
        "FASTORE", // 81 (0x51)
        "DASTORE", // 82 (0x52)
        "AASTORE", // 83 (0x53)
        "BASTORE", // 84 (0x54)
        "CASTORE", // 85 (0x55)
        "SASTORE", // 86 (0x56)
        "POP", // 87 (0x57)
        "POP2", // 88 (0x58)
        "DUP", // 89 (0x59)
        "DUP_X1", // 90 (0x5a)
        "DUP_X2", // 91 (0x5b)
        "DUP2", // 92 (0x5c)
        "DUP2_X1", // 93 (0x5d)
        "DUP2_X2", // 94 (0x5e)
        "SWAP", // 95 (0x5f)
        "IADD", // 96 (0x60)
        "LADD", // 97 (0x61)
        "FADD", // 98 (0x62)
        "DADD", // 99 (0x63)
        "ISUB", // 100 (0x64)
        "LSUB", // 101 (0x65)
        "FSUB", // 102 (0x66)
        "DSUB", // 103 (0x67)
        "IMUL", // 104 (0x68)
        "LMUL", // 105 (0x69)
        "FMUL", // 106 (0x6a)
        "DMUL", // 107 (0x6b)
        "IDIV", // 108 (0x6c)
        "LDIV", // 109 (0x6d)
        "FDIV", // 110 (0x6e)
        "DDIV", // 111 (0x6f)
        "IREM", // 112 (0x70)
        "LREM", // 113 (0x71)
        "FREM", // 114 (0x72)
        "DREM", // 115 (0x73)
        "INEG", // 116 (0x74)
        "LNEG", // 117 (0x75)
        "FNEG", // 118 (0x76)
        "DNEG", // 119 (0x77)
        "ISHL", // 120 (0x78)
        "LSHL", // 121 (0x79)
        "ISHR", // 122 (0x7a)
        "LSHR", // 123 (0x7b)
        "IUSHR", // 124 (0x7c)
        "LUSHR", // 125 (0x7d)
        "IAND", // 126 (0x7e)
        "LAND", // 127 (0x7f)
        "IOR", // 128 (0x80)
        "LOR", // 129 (0x81)
        "IXOR", // 130 (0x82)
        "LXOR", // 131 (0x83)
        "IINC", // 132 (0x84)
        "I2L", // 133 (0x85)
        "I2F", // 134 (0x86)
        "I2D", // 135 (0x87)
        "L2I", // 136 (0x88)
        "L2F", // 137 (0x89)
        "L2D", // 138 (0x8a)
        "F2I", // 139 (0x8b)
        "F2L", // 140 (0x8c)
        "F2D", // 141 (0x8d)
        "D2I", // 142 (0x8e)
        "D2L", // 143 (0x8f)
        "D2F", // 144 (0x90)
        "I2B", // 145 (0x91)
        "I2C", // 146 (0x92)
        "I2S", // 147 (0x93)
        "LCMP", // 148 (0x94)
        "FCMPL", // 149 (0x95)
        "FCMPG", // 150 (0x96)
        "DCMPL", // 151 (0x97)
        "DCMPG", // 152 (0x98)
        "IFEQ", // 153 (0x99)
        "IFNE", // 154 (0x9a)
        "IFLT", // 155 (0x9b)
        "IFGE", // 156 (0x9c)
        "IFGT", // 157 (0x9d)
        "IFLE", // 158 (0x9e)
        "IF_ICMPEQ", // 159 (0x9f)
        "IF_ICMPNE", // 160 (0xa0)
        "IF_ICMPLT", // 161 (0xa1)
        "IF_ICMPGE", // 162 (0xa2)
        "IF_ICMPGT", // 163 (0xa3)
        "IF_ICMPLE", // 164 (0xa4)
        "IF_ACMPEQ", // 165 (0xa5)
        "IF_ACMPNE", // 166 (0xa6)
        "GOTO", // 167 (0xa7)
        "JSR", // 168 (0xa8)
        "RET", // 169 (0xa9)
        "TABLESWITCH", // 170 (0xaa)
        "LOOKUPSWITCH", // 171 (0xab)
        "IRETURN", // 172 (0xac)
        "LRETURN", // 173 (0xad)
        "FRETURN", // 174 (0xae)
        "DRETURN", // 175 (0xaf)
        "ARETURN", // 176 (0xb0)
        "RETURN", // 177 (0xb1)
        "GETSTATIC", // 178 (0xb2)
        "PUTSTATIC", // 179 (0xb3)
        "GETFIELD", // 180 (0xb4)
        "PUTFIELD", // 181 (0xb5)
        "INVOKEVIRTUAL", // 182 (0xb6)
        "INVOKESPECIAL", // 183 (0xb7)
        "INVOKESTATIC", // 184 (0xb8)
        "INVOKEINTERFACE", // 185 (0xb9)
        "INVOKEDYNAMIC", // 186 (0xba)
        "NEW", // 187 (0xbb)
        "NEWARRAY", // 188 (0xbc)
        "ANEWARRAY", // 189 (0xbd)
        "ARRAYLENGTH", // 190 (0xbe)
        "ATHROW", // 191 (0xbf)
        "CHECKCAST", // 192 (0xc0)
        "INSTANCEOF", // 193 (0xc1)
        "MONITORENTER", // 194 (0xc2)
        "MONITOREXIT", // 195 (0xc3)
        "WIDE", // 196 (0xc4)
        "MULTIANEWARRAY", // 197 (0xc5)
        "IFNULL", // 198 (0xc6)
        "IFNONNULL" // 199 (0xc7)
    };

    /**
      * The names of the {@code operand} values of the {@link
      * jdk.internal.org.objectweb.asm.MethodVisitor#visitIntInsn} method when {@code opcode} is {@code NEWARRAY}.
      */
    public static final String[] TYPES = {
        "",
        "",
        "",
        "",
        "T_BOOLEAN",
        "T_CHAR",
        "T_FLOAT",
        "T_DOUBLE",
        "T_BYTE",
        "T_SHORT",
        "T_INT",
        "T_LONG"
    };

    /** The names of the {@code tag} field values for {@link jdk.internal.org.objectweb.asm.Handle}. */
    public static final String[] HANDLE_TAG = {
        "",
        "H_GETFIELD",
        "H_GETSTATIC",
        "H_PUTFIELD",
        "H_PUTSTATIC",
        "H_INVOKEVIRTUAL",
        "H_INVOKESTATIC",
        "H_INVOKESPECIAL",
        "H_NEWINVOKESPECIAL",
        "H_INVOKEINTERFACE"
    };

    /** Message of the UnsupportedOperationException thrown by methods which must be overridden. */
    private static final String UNSUPPORTED_OPERATION = "Must be overridden";

    /**
      * The ASM API version implemented by this class. The value of this field must be one of {@link
      * Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
      */
    protected final int api;

    /** The builder used to build strings in the various visit methods. */
    protected final StringBuilder stringBuilder;

    /**
      * The text to be printed. Since the code of methods is not necessarily visited in sequential
      * order, one method after the other, but can be interlaced (some instructions from method one,
      * then some instructions from method two, then some instructions from method one again...), it is
      * not possible to print the visited instructions directly to a sequential stream. A class is
      * therefore printed in a two steps process: a string tree is constructed during the visit, and
      * printed to a sequential stream at the end of the visit. This string tree is stored in this
      * field, as a string list that can contain other string lists, which can themselves contain other
      * string lists, and so on.
      */
    public final List<Object> text;

    // -----------------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------------

    /**
      * Constructs a new {@link Printer}.
      *
      * @param api the ASM API version implemented by this printer. Must be one of {@link
      *     Opcodes#ASM4}, {@link Opcodes#ASM5}, {@link Opcodes#ASM6} or {@link Opcodes#ASM7}.
      */
    protected Printer(final int api) {
        this.api = api;
        this.stringBuilder = new StringBuilder();
        this.text = new ArrayList<>();
    }

    // -----------------------------------------------------------------------------------------------
    // Classes
    // -----------------------------------------------------------------------------------------------

    /**
      * Class header. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visit}.
      *
      * @param version the class version. The minor version is stored in the 16 most significant bits,
      *     and the major version in the 16 least significant bits.
      * @param access the class's access flags (see {@link Opcodes}). This parameter also indicates if
      *     the class is deprecated.
      * @param name the internal name of the class (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}).
      * @param signature the signature of this class. May be {@literal null} if the class is not a
      *     generic one, and does not extend or implement generic classes or interfaces.
      * @param superName the internal of name of the super class (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}). For interfaces, the super class is {@link
      *     Object}. May be {@literal null}, but only for the {@link Object} class.
      * @param interfaces the internal names of the class's interfaces (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}). May be {@literal null}.
      */
    public abstract void visit(
            int version,
            int access,
            String name,
            String signature,
            String superName,
            String[] interfaces);

    /**
      * Class source. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitSource}.
      *
      * @param source the name of the source file from which the class was compiled. May be {@literal
      *     null}.
      * @param debug additional debug information to compute the correspondence between source and
      *     compiled elements of the class. May be {@literal null}.
      */
    public abstract void visitSource(String source, String debug);

    /**
      * Module. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitModule}.
      *
      * @param name the fully qualified name (using dots) of the module.
      * @param access the module access flags, among {@code ACC_OPEN}, {@code ACC_SYNTHETIC} and {@code
      *     ACC_MANDATED}.
      * @param version the module version, or {@literal null}.
      * @return the printer.
      */
    public Printer visitModule(final String name, final int access, final String version) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Visits the nest host class of the class. A nest is a set of classes of the same package that
      * share access to their private members. One of these classes, called the host, lists the other
      * members of the nest, which in turn should link to the host of their nest. This method must be
      * called only once and only if the visited class is a non-host member of a nest. A class is
      * implicitly its own nest, so it's invalid to call this method with the visited class name as
      * argument.
      *
      * @param nestHost the internal name of the host class of the nest.
      */
    public void visitNestHost(final String nestHost) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Class outer class. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitOuterClass}.
      *
      * @param owner internal name of the enclosing class of the class.
      * @param name the name of the method that contains the class, or {@literal null} if the class is
      *     not enclosed in a method of its enclosing class.
      * @param descriptor the descriptor of the method that contains the class, or {@literal null} if
      *     the class is not enclosed in a method of its enclosing class.
      */
    public abstract void visitOuterClass(String owner, String name, String descriptor);

    /**
      * Class annotation. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitAnnotation}.
      *
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public abstract Printer visitClassAnnotation(String descriptor, boolean visible);

    /**
      * Class type annotation. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitTypeAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link jdk.internal.org.objectweb.asm.TypeReference#CLASS_TYPE_PARAMETER}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#CLASS_EXTENDS}. See {@link
      *     jdk.internal.org.objectweb.asm.TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public Printer visitClassTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Class attribute. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitAttribute}.
      *
      * @param attribute an attribute.
      */
    public abstract void visitClassAttribute(Attribute attribute);

    /**
      * Visits a member of the nest. A nest is a set of classes of the same package that share access
      * to their private members. One of these classes, called the host, lists the other members of the
      * nest, which in turn should link to the host of their nest. This method must be called only if
      * the visited class is the host of a nest. A nest host is implicitly a member of its own nest, so
      * it's invalid to call this method with the visited class name as argument.
      *
      * @param nestMember the internal name of a nest member.
      */
    public void visitNestMember(final String nestMember) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * <b>Experimental, use at your own risk. This method will be renamed when it becomes stable, this
      * will break existing code using it</b>.
      *
      * <p>Visits a permitted subclass. A permitted subtclass is one of the allowed subclasses of the
      * current class. See {@link
      * jdk.internal.org.objectweb.asm.ClassVisitor#visitPermittedSubclassExperimental(String)}.
      *
      * @param permittedSubclass the internal name of a permitted subclass.
      * @deprecated this API is experimental.
      */
    @Deprecated
    public void visitPermittedSubclassExperimental(final String permittedSubclass) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Class inner name. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitInnerClass}.
      *
      * @param name the internal name of an inner class (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}).
      * @param outerName the internal name of the class to which the inner class belongs (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}). May be {@literal null} for not member classes.
      * @param innerName the (simple) name of the inner class inside its enclosing class. May be
      *     {@literal null} for anonymous inner classes.
      * @param access the access flags of the inner class as originally declared in the enclosing
      *     class.
      */
    public abstract void visitInnerClass(String name, String outerName, String innerName, int access);

    /**
      * Visits a record component of the class. See {@link
      * jdk.internal.org.objectweb.asm.ClassVisitor#visitRecordComponent(String, String, String)}.
      *
      * @param name the field's name.
      * @param descriptor the record component descriptor (see {@link Type}).
      * @param signature the record component signature. May be {@literal null} if the record component
      *     type does not use generic types.
      * @return a visitor to visit this record component annotations and attributes, or {@literal null}
      *     if this class visitor is not interested in visiting these annotations and attributes.
      */
    public Printer visitRecordComponent(
            final String name, final String descriptor, final String signature) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Class field. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitField}.
      *
      * @param access the field's access flags (see {@link Opcodes}). This parameter also indicates if
      *     the field is synthetic and/or deprecated.
      * @param name the field's name.
      * @param descriptor the field's descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param signature the field's signature. May be {@literal null} if the field's type does not use
      *     generic types.
      * @param value the field's initial value. This parameter, which may be {@literal null} if the
      *     field does not have an initial value, must be an {@link Integer}, a {@link Float}, a {@link
      *     Long}, a {@link Double} or a {@link String} (for {@code int}, {@code float}, {@code long}
      *     or {@code String} fields respectively). <i>This parameter is only used for static
      *     fields</i>. Its value is ignored for non static fields, which must be initialized through
      *     bytecode instructions in constructors or methods.
      * @return the printer.
      */
    public abstract Printer visitField(
            int access, String name, String descriptor, String signature, Object value);

    /**
      * Class method. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitMethod}.
      *
      * @param access the method's access flags (see {@link Opcodes}). This parameter also indicates if
      *     the method is synthetic and/or deprecated.
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param signature the method's signature. May be {@literal null} if the method parameters,
      *     return type and exceptions do not use generic types.
      * @param exceptions the internal names of the method's exception classes (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}). May be {@literal null}.
      * @return the printer.
      */
    public abstract Printer visitMethod(
            int access, String name, String descriptor, String signature, String[] exceptions);

    /** Class end. See {@link jdk.internal.org.objectweb.asm.ClassVisitor#visitEnd}. */
    public abstract void visitClassEnd();

    // -----------------------------------------------------------------------------------------------
    // Modules
    // -----------------------------------------------------------------------------------------------

    /**
      * Module main class. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitMainClass}.
      *
      * @param mainClass the internal name of the main class of the current module.
      */
    public void visitMainClass(final String mainClass) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Module package. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitPackage}.
      *
      * @param packaze the internal name of a package.
      */
    public void visitPackage(final String packaze) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Module require. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitRequire}.
      *
      * @param module the fully qualified name (using dots) of the dependence.
      * @param access the access flag of the dependence among {@code ACC_TRANSITIVE}, {@code
      *     ACC_STATIC_PHASE}, {@code ACC_SYNTHETIC} and {@code ACC_MANDATED}.
      * @param version the module version at compile time, or {@literal null}.
      */
    public void visitRequire(final String module, final int access, final String version) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Module export. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitExport}.
      *
      * @param packaze the internal name of the exported package.
      * @param access the access flag of the exported package, valid values are among {@code
      *     ACC_SYNTHETIC} and {@code ACC_MANDATED}.
      * @param modules the fully qualified names (using dots) of the modules that can access the public
      *     classes of the exported package, or {@literal null}.
      */
    public void visitExport(final String packaze, final int access, final String... modules) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Module open. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitOpen}.
      *
      * @param packaze the internal name of the opened package.
      * @param access the access flag of the opened package, valid values are among {@code
      *     ACC_SYNTHETIC} and {@code ACC_MANDATED}.
      * @param modules the fully qualified names (using dots) of the modules that can use deep
      *     reflection to the classes of the open package, or {@literal null}.
      */
    public void visitOpen(final String packaze, final int access, final String... modules) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Module use. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitUse}.
      *
      * @param service the internal name of the service.
      */
    public void visitUse(final String service) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Module provide. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitProvide}.
      *
      * @param service the internal name of the service.
      * @param providers the internal names of the implementations of the service (there is at least
      *     one provider).
      */
    public void visitProvide(final String service, final String... providers) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /** Module end. See {@link jdk.internal.org.objectweb.asm.ModuleVisitor#visitEnd}. */
    public void visitModuleEnd() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    // -----------------------------------------------------------------------------------------------
    // Annotations
    // -----------------------------------------------------------------------------------------------

    /**
      * Annotation value. See {@link jdk.internal.org.objectweb.asm.AnnotationVisitor#visit}.
      *
      * @param name the value name.
      * @param value the actual value, whose type must be {@link Byte}, {@link Boolean}, {@link
      *     Character}, {@link Short}, {@link Integer} , {@link Long}, {@link Float}, {@link Double},
      *     {@link String} or {@link jdk.internal.org.objectweb.asm.Type} of {@link jdk.internal.org.objectweb.asm.Type#OBJECT}
      *     or {@link jdk.internal.org.objectweb.asm.Type#ARRAY} sort. This value can also be an array of byte,
      *     boolean, short, char, int, long, float or double values (this is equivalent to using {@link
      *     #visitArray} and visiting each array element in turn, but is more convenient).
      */
    // DontCheck(OverloadMethodsDeclarationOrder): overloads are semantically different.
    public abstract void visit(String name, Object value);

    /**
      * Annotation enum value. See {@link jdk.internal.org.objectweb.asm.AnnotationVisitor#visitEnum}.
      *
      * @param name the value name.
      * @param descriptor the class descriptor of the enumeration class.
      * @param value the actual enumeration value.
      */
    public abstract void visitEnum(String name, String descriptor, String value);

    /**
      * Nested annotation value. See {@link jdk.internal.org.objectweb.asm.AnnotationVisitor#visitAnnotation}.
      *
      * @param name the value name.
      * @param descriptor the class descriptor of the nested annotation class.
      * @return the printer.
      */
    public abstract Printer visitAnnotation(String name, String descriptor);

    /**
      * Annotation array value. See {@link jdk.internal.org.objectweb.asm.AnnotationVisitor#visitArray}.
      *
      * @param name the value name.
      * @return the printer.
      */
    public abstract Printer visitArray(String name);

    /** Annotation end. See {@link jdk.internal.org.objectweb.asm.AnnotationVisitor#visitEnd}. */
    public abstract void visitAnnotationEnd();

    // -----------------------------------------------------------------------------------------------
    // Record components
    // -----------------------------------------------------------------------------------------------

    /**
      * Visits an annotation of the record component. See {@link
      * jdk.internal.org.objectweb.asm.RecordComponentVisitor#visitAnnotation}.
      *
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
      *     interested in visiting this annotation.
      */
    public Printer visitRecordComponentAnnotation(final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Visits an annotation on a type in the record component signature. See {@link
      * jdk.internal.org.objectweb.asm.RecordComponentVisitor#visitTypeAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link TypeReference#CLASS_TYPE_PARAMETER}, {@link
      *     TypeReference#CLASS_TYPE_PARAMETER_BOUND} or {@link TypeReference#CLASS_EXTENDS}. See
      *     {@link TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return a visitor to visit the annotation values, or {@literal null} if this visitor is not
      *     interested in visiting this annotation.
      */
    public Printer visitRecordComponentTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Visits a non standard attribute of the record component. See {@link
      * jdk.internal.org.objectweb.asm.RecordComponentVisitor#visitAttribute}.
      *
      * @param attribute an attribute.
      */
    public void visitRecordComponentAttribute(final Attribute attribute) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Visits the end of the record component. See {@link
      * jdk.internal.org.objectweb.asm.RecordComponentVisitor#visitEnd}. This method, which is the last one to be
      * called, is used to inform the visitor that everything have been visited.
      */
    public void visitRecordComponentEnd() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    // -----------------------------------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------------------------------

    /**
      * Field annotation. See {@link jdk.internal.org.objectweb.asm.FieldVisitor#visitAnnotation}.
      *
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public abstract Printer visitFieldAnnotation(String descriptor, boolean visible);

    /**
      * Field type annotation. See {@link jdk.internal.org.objectweb.asm.FieldVisitor#visitTypeAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link jdk.internal.org.objectweb.asm.TypeReference#FIELD}. See {@link jdk.internal.org.objectweb.asm.TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public Printer visitFieldTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Field attribute. See {@link jdk.internal.org.objectweb.asm.FieldVisitor#visitAttribute}.
      *
      * @param attribute an attribute.
      */
    public abstract void visitFieldAttribute(Attribute attribute);

    /** Field end. See {@link jdk.internal.org.objectweb.asm.FieldVisitor#visitEnd}. */
    public abstract void visitFieldEnd();

    // -----------------------------------------------------------------------------------------------
    // Methods
    // -----------------------------------------------------------------------------------------------

    /**
      * Method parameter. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitParameter(String, int)}.
      *
      * @param name parameter name or {@literal null} if none is provided.
      * @param access the parameter's access flags, only {@code ACC_FINAL}, {@code ACC_SYNTHETIC}
      *     or/and {@code ACC_MANDATED} are allowed (see {@link Opcodes}).
      */
    public void visitParameter(final String name, final int access) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Method default annotation. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitAnnotationDefault}.
      *
      * @return the printer.
      */
    public abstract Printer visitAnnotationDefault();

    /**
      * Method annotation. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitAnnotation}.
      *
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public abstract Printer visitMethodAnnotation(String descriptor, boolean visible);

    /**
      * Method type annotation. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitTypeAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link jdk.internal.org.objectweb.asm.TypeReference#METHOD_TYPE_PARAMETER}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_TYPE_PARAMETER_BOUND}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_RETURN}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_RECEIVER}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_FORMAL_PARAMETER} or {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#THROWS}. See {@link jdk.internal.org.objectweb.asm.TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public Printer visitMethodTypeAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Number of method parameters that can have annotations. See {@link
      * jdk.internal.org.objectweb.asm.MethodVisitor#visitAnnotableParameterCount}.
      *
      * @param parameterCount the number of method parameters than can have annotations. This number
      *     must be less or equal than the number of parameter types in the method descriptor. It can
      *     be strictly less when a method has synthetic parameters and when these parameters are
      *     ignored when computing parameter indices for the purpose of parameter annotations (see
      *     https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18).
      * @param visible {@literal true} to define the number of method parameters that can have
      *     annotations visible at runtime, {@literal false} to define the number of method parameters
      *     that can have annotations invisible at runtime.
      * @return the printer.
      */
    public Printer visitAnnotableParameterCount(final int parameterCount, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Method parameter annotation. See {@link
      * jdk.internal.org.objectweb.asm.MethodVisitor#visitParameterAnnotation}.
      *
      * @param parameter the parameter index. This index must be strictly smaller than the number of
      *     parameters in the method descriptor, and strictly smaller than the parameter count
      *     specified in {@link #visitAnnotableParameterCount}. Important note: <i>a parameter index i
      *     is not required to correspond to the i'th parameter descriptor in the method
      *     descriptor</i>, in particular in case of synthetic parameters (see
      *     https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18).
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public abstract Printer visitParameterAnnotation(
            int parameter, String descriptor, boolean visible);

    /**
      * Method attribute. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitAttribute}.
      *
      * @param attribute an attribute.
      */
    public abstract void visitMethodAttribute(Attribute attribute);

    /** Method start. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitCode}. */
    public abstract void visitCode();

    /**
      * Method stack frame. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitFrame}.
      *
      * @param type the type of this stack map frame. Must be {@link Opcodes#F_NEW} for expanded
      *     frames, or {@link Opcodes#F_FULL}, {@link Opcodes#F_APPEND}, {@link Opcodes#F_CHOP}, {@link
      *     Opcodes#F_SAME} or {@link Opcodes#F_APPEND}, {@link Opcodes#F_SAME1} for compressed frames.
      * @param numLocal the number of local variables in the visited frame.
      * @param local the local variable types in this frame. This array must not be modified. Primitive
      *     types are represented by {@link Opcodes#TOP}, {@link Opcodes#INTEGER}, {@link
      *     Opcodes#FLOAT}, {@link Opcodes#LONG}, {@link Opcodes#DOUBLE}, {@link Opcodes#NULL} or
      *     {@link Opcodes#UNINITIALIZED_THIS} (long and double are represented by a single element).
      *     Reference types are represented by String objects (representing internal names), and
      *     uninitialized types by Label objects (this label designates the NEW instruction that
      *     created this uninitialized value).
      * @param numStack the number of operand stack elements in the visited frame.
      * @param stack the operand stack types in this frame. This array must not be modified. Its
      *     content has the same format as the "local" array.
      */
    public abstract void visitFrame(
            int type, int numLocal, Object[] local, int numStack, Object[] stack);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitInsn}
      *
      * @param opcode the opcode of the instruction to be visited. This opcode is either NOP,
      *     ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5,
      *     LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD,
      *     FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE,
      *     AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2,
      *     SWAP, IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV,
      *     FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR, IUSHR,
      *     LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR, I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I,
      *     D2L, D2F, I2B, I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN, FRETURN,
      *     DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW, MONITORENTER, or MONITOREXIT.
      */
    public abstract void visitInsn(int opcode);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitIntInsn}.
      *
      * @param opcode the opcode of the instruction to be visited. This opcode is either BIPUSH, SIPUSH
      *     or NEWARRAY.
      * @param operand the operand of the instruction to be visited.<br>
      *     When opcode is BIPUSH, operand value should be between Byte.MIN_VALUE and Byte.MAX_VALUE.
      *     <br>
      *     When opcode is SIPUSH, operand value should be between Short.MIN_VALUE and Short.MAX_VALUE.
      *     <br>
      *     When opcode is NEWARRAY, operand value should be one of {@link Opcodes#T_BOOLEAN}, {@link
      *     Opcodes#T_CHAR}, {@link Opcodes#T_FLOAT}, {@link Opcodes#T_DOUBLE}, {@link Opcodes#T_BYTE},
      *     {@link Opcodes#T_SHORT}, {@link Opcodes#T_INT} or {@link Opcodes#T_LONG}.
      */
    public abstract void visitIntInsn(int opcode, int operand);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitVarInsn}.
      *
      * @param opcode the opcode of the local variable instruction to be visited. This opcode is either
      *     ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET.
      * @param var the operand of the instruction to be visited. This operand is the index of a local
      *     variable.
      */
    public abstract void visitVarInsn(int opcode, int var);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitTypeInsn}.
      *
      * @param opcode the opcode of the type instruction to be visited. This opcode is either NEW,
      *     ANEWARRAY, CHECKCAST or INSTANCEOF.
      * @param type the operand of the instruction to be visited. This operand must be the internal
      *     name of an object or array class (see {@link jdk.internal.org.objectweb.asm.Type#getInternalName()}).
      */
    public abstract void visitTypeInsn(int opcode, String type);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitFieldInsn}.
      *
      * @param opcode the opcode of the type instruction to be visited. This opcode is either
      *     GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
      * @param owner the internal name of the field's owner class (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}).
      * @param name the field's name.
      * @param descriptor the field's descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      */
    public abstract void visitFieldInsn(int opcode, String owner, String name, String descriptor);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitMethodInsn}.
      *
      * @param opcode the opcode of the type instruction to be visited. This opcode is either
      *     INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
      * @param owner the internal name of the method's owner class (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @deprecated use {@link #visitMethodInsn(int, String, String, String, boolean)} instead.
      */
    @Deprecated
    public void visitMethodInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
        // This method was abstract before ASM5, and was therefore always overridden (without any
        // call to 'super'). Thus, at this point we necessarily have api >= ASM5, and we must then
        // redirect the method call to the ASM5 visitMethodInsn() method.
        visitMethodInsn(opcode, owner, name, descriptor, opcode == Opcodes.INVOKEINTERFACE);
    }

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitMethodInsn}.
      *
      * @param opcode the opcode of the type instruction to be visited. This opcode is either
      *     INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
      * @param owner the internal name of the method's owner class (see {@link
      *     jdk.internal.org.objectweb.asm.Type#getInternalName()}).
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param isInterface if the method's owner class is an interface.
      */
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitInvokeDynamicInsn}.
      *
      * @param name the method's name.
      * @param descriptor the method's descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param bootstrapMethodHandle the bootstrap method.
      * @param bootstrapMethodArguments the bootstrap method constant arguments. Each argument must be
      *     an {@link Integer}, {@link Float}, {@link Long}, {@link Double}, {@link String}, {@link
      *     jdk.internal.org.objectweb.asm.Type} or {@link Handle} value. This method is allowed to modify the
      *     content of the array so a caller should expect that this array may change.
      */
    public abstract void visitInvokeDynamicInsn(
            String name,
            String descriptor,
            Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments);

    /**
      * Method jump instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitJumpInsn}.
      *
      * @param opcode the opcode of the type instruction to be visited. This opcode is either IFEQ,
      *     IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT,
      *     IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
      * @param label the operand of the instruction to be visited. This operand is a label that
      *     designates the instruction to which the jump instruction may jump.
      */
    public abstract void visitJumpInsn(int opcode, Label label);

    /**
      * Method label. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitLabel}.
      *
      * @param label a {@link Label} object.
      */
    public abstract void visitLabel(Label label);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitLdcInsn}.
      *
      * @param value the constant to be loaded on the stack. This parameter must be a non null {@link
      *     Integer}, a {@link Float}, a {@link Long}, a {@link Double}, a {@link String}, a {@link
      *     Type} of OBJECT or ARRAY sort for {@code .class} constants, for classes whose version is
      *     49, a {@link Type} of METHOD sort for MethodType, a {@link Handle} for MethodHandle
      *     constants, for classes whose version is 51 or a {@link ConstantDynamic} for a constant
      *     dynamic for classes whose version is 55.
      */
    public abstract void visitLdcInsn(Object value);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitIincInsn}.
      *
      * @param var index of the local variable to be incremented.
      * @param increment amount to increment the local variable by.
      */
    public abstract void visitIincInsn(int var, int increment);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitTableSwitchInsn}.
      *
      * @param min the minimum key value.
      * @param max the maximum key value.
      * @param dflt beginning of the default handler block.
      * @param labels beginnings of the handler blocks. {@code labels[i]} is the beginning of the
      *     handler block for the {@code min + i} key.
      */
    public abstract void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitLookupSwitchInsn}.
      *
      * @param dflt beginning of the default handler block.
      * @param keys the values of the keys.
      * @param labels beginnings of the handler blocks. {@code labels[i]} is the beginning of the
      *     handler block for the {@code keys[i]} key.
      */
    public abstract void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels);

    /**
      * Method instruction. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitMultiANewArrayInsn}.
      *
      * @param descriptor an array type descriptor (see {@link jdk.internal.org.objectweb.asm.Type}).
      * @param numDimensions the number of dimensions of the array to allocate.
      */
    public abstract void visitMultiANewArrayInsn(String descriptor, int numDimensions);

    /**
      * Instruction type annotation. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitInsnAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link jdk.internal.org.objectweb.asm.TypeReference#INSTANCEOF}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#NEW}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#CONSTRUCTOR_REFERENCE}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_REFERENCE}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#CAST}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_INVOCATION_TYPE_ARGUMENT}, {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT}, or {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#METHOD_REFERENCE_TYPE_ARGUMENT}. See {@link
      *     jdk.internal.org.objectweb.asm.TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public Printer visitInsnAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Method exception handler. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitTryCatchBlock}.
      *
      * @param start the beginning of the exception handler's scope (inclusive).
      * @param end the end of the exception handler's scope (exclusive).
      * @param handler the beginning of the exception handler's code.
      * @param type the internal name of the type of exceptions handled by the handler, or {@literal
      *     null} to catch any exceptions (for "finally" blocks).
      */
    public abstract void visitTryCatchBlock(Label start, Label end, Label handler, String type);

    /**
      * Try catch block type annotation. See {@link
      * jdk.internal.org.objectweb.asm.MethodVisitor#visitTryCatchAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link jdk.internal.org.objectweb.asm.TypeReference#EXCEPTION_PARAMETER}. See {@link
      *     jdk.internal.org.objectweb.asm.TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public Printer visitTryCatchAnnotation(
            final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Method debug info. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitLocalVariable}.
      *
      * @param name the name of a local variable.
      * @param descriptor the type descriptor of this local variable.
      * @param signature the type signature of this local variable. May be {@literal null} if the local
      *     variable type does not use generic types.
      * @param start the first instruction corresponding to the scope of this local variable
      *     (inclusive).
      * @param end the last instruction corresponding to the scope of this local variable (exclusive).
      * @param index the local variable's index.
      */
    public abstract void visitLocalVariable(
            String name, String descriptor, String signature, Label start, Label end, int index);

    /**
      * Local variable type annotation. See {@link
      * jdk.internal.org.objectweb.asm.MethodVisitor#visitTryCatchAnnotation}.
      *
      * @param typeRef a reference to the annotated type. The sort of this type reference must be
      *     {@link jdk.internal.org.objectweb.asm.TypeReference#LOCAL_VARIABLE} or {@link
      *     jdk.internal.org.objectweb.asm.TypeReference#RESOURCE_VARIABLE}. See {@link
      *     jdk.internal.org.objectweb.asm.TypeReference}.
      * @param typePath the path to the annotated type argument, wildcard bound, array element type, or
      *     static inner type within 'typeRef'. May be {@literal null} if the annotation targets
      *     'typeRef' as a whole.
      * @param start the fist instructions corresponding to the continuous ranges that make the scope
      *     of this local variable (inclusive).
      * @param end the last instructions corresponding to the continuous ranges that make the scope of
      *     this local variable (exclusive). This array must have the same size as the 'start' array.
      * @param index the local variable's index in each range. This array must have the same size as
      *     the 'start' array.
      * @param descriptor the class descriptor of the annotation class.
      * @param visible {@literal true} if the annotation is visible at runtime.
      * @return the printer.
      */
    public Printer visitLocalVariableAnnotation(
            final int typeRef,
            final TypePath typePath,
            final Label[] start,
            final Label[] end,
            final int[] index,
            final String descriptor,
            final boolean visible) {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
    }

    /**
      * Method debug info. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitLineNumber}.
      *
      * @param line a line number. This number refers to the source file from which the class was
      *     compiled.
      * @param start the first instruction corresponding to this line number.
      */
    public abstract void visitLineNumber(int line, Label start);

    /**
      * Method max stack and max locals. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitMaxs}.
      *
      * @param maxStack maximum stack size of the method.
      * @param maxLocals maximum number of local variables for the method.
      */
    public abstract void visitMaxs(int maxStack, int maxLocals);

    /** Method end. See {@link jdk.internal.org.objectweb.asm.MethodVisitor#visitEnd}. */
    public abstract void visitMethodEnd();

    // -----------------------------------------------------------------------------------------------
    // Print and utility methods
    // -----------------------------------------------------------------------------------------------

    /**
      * Returns the text constructed by this visitor.
      *
      * @return the text constructed by this visitor. See {@link #text}.
      */
    public List<Object> getText() {
        return text;
    }

    /**
      * Prints the text constructed by this visitor.
      *
      * @param printWriter the print writer to be used.
      */
    public void print(final PrintWriter printWriter) {
        printList(printWriter, text);
    }

    /**
      * Prints the given string tree.
      *
      * @param printWriter the writer to be used to print the tree.
      * @param list a string tree, i.e., a string list that can contain other string lists, and so on
      *     recursively.
      */
    static void printList(final PrintWriter printWriter, final List<?> list) {
        for (Object o : list) {
            if (o instanceof List) {
                printList(printWriter, (List<?>) o);
            } else {
                printWriter.print(o.toString());
            }
        }
    }

    /**
      * Appends a quoted string to the given string builder.
      *
      * @param stringBuilder the buffer where the string must be added.
      * @param string the string to be added.
      */
    public static void appendString(final StringBuilder stringBuilder, final String string) {
        stringBuilder.append('\"');
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '\n') {
                stringBuilder.append("\\n");
            } else if (c == '\r') {
                stringBuilder.append("\\r");
            } else if (c == '\\') {
                stringBuilder.append("\\\\");
            } else if (c == '"') {
                stringBuilder.append("\\\"");
            } else if (c < 0x20 || c > 0x7f) {
                stringBuilder.append("\\u");
                if (c < 0x10) {
                    stringBuilder.append("000");
                } else if (c < 0x100) {
                    stringBuilder.append("00");
                } else if (c < 0x1000) {
                    stringBuilder.append('0');
                }
                stringBuilder.append(Integer.toString(c, 16));
            } else {
                stringBuilder.append(c);
            }
        }
        stringBuilder.append('\"');
    }

    /**
      * Prints a the given class to the given output.
      *
      * <p>Command line arguments: [-debug] &lt;binary class name or class file name &gt;
      *
      * @param args the command line arguments.
      * @param usage the help message to show when command line arguments are incorrect.
      * @param printer the printer to convert the class into text.
      * @param output where to print the result.
      * @param logger where to log errors.
      * @throws IOException if the class cannot be found, or if an IOException occurs.
      */
    static void main(
            final String[] args,
            final String usage,
            final Printer printer,
            final PrintWriter output,
            final PrintWriter logger)
            throws IOException {
        if (args.length < 1 || args.length > 2 || (args[0].equals("-debug") && args.length != 2)) {
            logger.println(usage);
            return;
        }

        TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, output);

        String className;
        int parsingOptions;
        if (args[0].equals("-debug")) {
            className = args[1];
            parsingOptions = ClassReader.SKIP_DEBUG;
        } else {
            className = args[0];
            parsingOptions = 0;
        }

        if (className.endsWith(".class")
                || className.indexOf('\\') != -1
                || className.indexOf('/') != -1) {
            InputStream inputStream =
                    new FileInputStream(className); // NOPMD(AvoidFileStream): can't fix for 1.5 compatibility
            new ClassReader(inputStream).accept(traceClassVisitor, parsingOptions);
        } else {
            new ClassReader(className).accept(traceClassVisitor, parsingOptions);
        }
    }
}
