/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.org.apache.bcel.internal.classfile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.FilterReader;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.util.ByteSequence;

/**
 * Utility functions that do not really belong to any class in particular.
 *
 * @LastModified: June 2021
 */
// @since 6.0 methods are no longer final
public abstract class Utility {

    private static int unwrap( final ThreadLocal<Integer> tl ) {
        return tl.get();
    }

    private static void wrap( final ThreadLocal<Integer> tl, final int value ) {
        tl.set(value);
    }

    /* How many chars have been consumed
     * during parsing in typeSignatureToString().
     * Read by methodSignatureToString().
     * Set by side effect, but only internally.
     */
    private static ThreadLocal<Integer> consumed_chars = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    /* The `WIDE' instruction is used in the
     * byte code to allow 16-bit wide indices
     * for local variables. This opcode
     * precedes an `ILOAD', e.g.. The opcode
     * immediately following takes an extra
     * byte which is combined with the
     * following byte to form a
     * 16-bit value.
     */
    private static boolean wide = false;


    /**
     * Convert bit field of flags into string such as `static final'.
     *
     * @param  access_flags Access flags
     * @return String representation of flags
     */
    public static String accessToString( final int access_flags ) {
        return accessToString(access_flags, false);
    }


    /**
     * Convert bit field of flags into string such as `static final'.
     *
     * Special case: Classes compiled with new compilers and with the
     * `ACC_SUPER' flag would be said to be "synchronized". This is
     * because SUN used the same value for the flags `ACC_SUPER' and
     * `ACC_SYNCHRONIZED'.
     *
     * @param  access_flags Access flags
     * @param  for_class access flags are for class qualifiers ?
     * @return String representation of flags
     */
    public static String accessToString( final int access_flags, final boolean for_class ) {
        final StringBuilder buf = new StringBuilder();
        int p = 0;
        for (int i = 0; p < Const.MAX_ACC_FLAG_I; i++) { // Loop through known flags
            p = pow2(i);
            if ((access_flags & p) != 0) {
                /* Special case: Classes compiled with new compilers and with the
                 * `ACC_SUPER' flag would be said to be "synchronized". This is
                 * because SUN used the same value for the flags `ACC_SUPER' and
                 * `ACC_SYNCHRONIZED'.
                 */
                if (for_class && ((p == Const.ACC_SUPER) || (p == Const.ACC_INTERFACE))) {
                    continue;
                }
                buf.append(Const.getAccessName(i)).append(" ");
            }
        }
        return buf.toString().trim();
    }


    /**
     * @param access_flags the class flags
     *
     * @return "class" or "interface", depending on the ACC_INTERFACE flag
     */
    public static String classOrInterface( final int access_flags ) {
        return ((access_flags & Const.ACC_INTERFACE) != 0) ? "interface" : "class";
    }


    /**
     * Disassemble a byte array of JVM byte codes starting from code line
     * `index' and return the disassembled string representation. Decode only
     * `num' opcodes (including their operands), use -1 if you want to
     * decompile everything.
     *
     * @param  code byte code array
     * @param  constant_pool Array of constants
     * @param  index offset in `code' array
     * <EM>(number of opcodes, not bytes!)</EM>
     * @param  length number of opcodes to decompile, -1 for all
     * @param  verbose be verbose, e.g. print constant pool index
     * @return String representation of byte codes
     */
    public static String codeToString( final byte[] code, final ConstantPool constant_pool, final int index,
            final int length, final boolean verbose ) {
        final StringBuilder buf = new StringBuilder(code.length * 20); // Should be sufficient // CHECKSTYLE IGNORE MagicNumber
        try (ByteSequence stream = new ByteSequence(code)) {
            for (int i = 0; i < index; i++) {
                codeToString(stream, constant_pool, verbose);
            }
            for (int i = 0; stream.available() > 0; i++) {
                if ((length < 0) || (i < length)) {
                    final String indices = fillup(stream.getIndex() + ":", 6, true, ' ');
                    buf.append(indices).append(codeToString(stream, constant_pool, verbose)).append('\n');
                }
            }
        } catch (final IOException e) {
            throw new ClassFormatException("Byte code error: " + buf.toString(), e);
        }
        return buf.toString();
    }


    public static String codeToString( final byte[] code, final ConstantPool constant_pool, final int index, final int length ) {
        return codeToString(code, constant_pool, index, length, true);
    }


    /**
     * Disassemble a stream of byte codes and return the
     * string representation.
     *
     * @param  bytes stream of bytes
     * @param  constant_pool Array of constants
     * @param  verbose be verbose, e.g. print constant pool index
     * @return String representation of byte code
     *
     * @throws IOException if a failure from reading from the bytes argument occurs
     */
    @SuppressWarnings("fallthrough") // by design for case Const.INSTANCEOF
    public static String codeToString(final ByteSequence bytes, final ConstantPool constant_pool,
            final boolean verbose) throws IOException {
        final short opcode = (short) bytes.readUnsignedByte();
        int default_offset = 0;
        int low;
        int high;
        int npairs;
        int index;
        int vindex;
        int constant;
        int[] match;
        int[] jump_table;
        int no_pad_bytes = 0;
        int offset;
        final StringBuilder buf = new StringBuilder(Const.getOpcodeName(opcode));
        /* Special case: Skip (0-3) padding bytes, i.e., the
         * following bytes are 4-byte-aligned
         */
        if ((opcode == Const.TABLESWITCH) || (opcode == Const.LOOKUPSWITCH)) {
            final int remainder = bytes.getIndex() % 4;
            no_pad_bytes = (remainder == 0) ? 0 : 4 - remainder;
            for (int i = 0; i < no_pad_bytes; i++) {
                byte b;
                if ((b = bytes.readByte()) != 0) {
                    System.err.println("Warning: Padding byte != 0 in "
                            + Const.getOpcodeName(opcode) + ":" + b);
                }
            }
            // Both cases have a field default_offset in common
            default_offset = bytes.readInt();
        }
        switch (opcode) {
            /* Table switch has variable length arguments.
             */
            case Const.TABLESWITCH:
                low = bytes.readInt();
                high = bytes.readInt();
                offset = bytes.getIndex() - 12 - no_pad_bytes - 1;
                default_offset += offset;
                buf.append("\tdefault = ").append(default_offset).append(", low = ").append(low)
                        .append(", high = ").append(high).append("(");
                jump_table = new int[high - low + 1];
                for (int i = 0; i < jump_table.length; i++) {
                    jump_table[i] = offset + bytes.readInt();
                    buf.append(jump_table[i]);
                    if (i < jump_table.length - 1) {
                        buf.append(", ");
                    }
                }
                buf.append(")");
                break;
            /* Lookup switch has variable length arguments.
             */
            case Const.LOOKUPSWITCH: {
                npairs = bytes.readInt();
                offset = bytes.getIndex() - 8 - no_pad_bytes - 1;
                match = new int[npairs];
                jump_table = new int[npairs];
                default_offset += offset;
                buf.append("\tdefault = ").append(default_offset).append(", npairs = ").append(
                        npairs).append(" (");
                for (int i = 0; i < npairs; i++) {
                    match[i] = bytes.readInt();
                    jump_table[i] = offset + bytes.readInt();
                    buf.append("(").append(match[i]).append(", ").append(jump_table[i]).append(")");
                    if (i < npairs - 1) {
                        buf.append(", ");
                    }
                }
                buf.append(")");
            }
                break;
            /* Two address bytes + offset from start of byte stream form the
             * jump target
             */
            case Const.GOTO:
            case Const.IFEQ:
            case Const.IFGE:
            case Const.IFGT:
            case Const.IFLE:
            case Const.IFLT:
            case Const.JSR:
            case Const.IFNE:
            case Const.IFNONNULL:
            case Const.IFNULL:
            case Const.IF_ACMPEQ:
            case Const.IF_ACMPNE:
            case Const.IF_ICMPEQ:
            case Const.IF_ICMPGE:
            case Const.IF_ICMPGT:
            case Const.IF_ICMPLE:
            case Const.IF_ICMPLT:
            case Const.IF_ICMPNE:
                buf.append("\t\t#").append((bytes.getIndex() - 1) + bytes.readShort());
                break;
            /* 32-bit wide jumps
             */
            case Const.GOTO_W:
            case Const.JSR_W:
                buf.append("\t\t#").append((bytes.getIndex() - 1) + bytes.readInt());
                break;
            /* Index byte references local variable (register)
             */
            case Const.ALOAD:
            case Const.ASTORE:
            case Const.DLOAD:
            case Const.DSTORE:
            case Const.FLOAD:
            case Const.FSTORE:
            case Const.ILOAD:
            case Const.ISTORE:
            case Const.LLOAD:
            case Const.LSTORE:
            case Const.RET:
                if (wide) {
                    vindex = bytes.readUnsignedShort();
                    wide = false; // Clear flag
                } else {
                    vindex = bytes.readUnsignedByte();
                }
                buf.append("\t\t%").append(vindex);
                break;
            /*
             * Remember wide byte which is used to form a 16-bit address in the
             * following instruction. Relies on that the method is called again with
             * the following opcode.
             */
            case Const.WIDE:
                wide = true;
                buf.append("\t(wide)");
                break;
            /* Array of basic type.
             */
            case Const.NEWARRAY:
                buf.append("\t\t<").append(Const.getTypeName(bytes.readByte())).append(">");
                break;
            /* Access object/class fields.
             */
            case Const.GETFIELD:
            case Const.GETSTATIC:
            case Const.PUTFIELD:
            case Const.PUTSTATIC:
                index = bytes.readUnsignedShort();
                buf.append("\t\t").append(
                        constant_pool.constantToString(index, Const.CONSTANT_Fieldref)).append(
                        verbose ? " (" + index + ")" : "");
                break;
            /* Operands are references to classes in constant pool
             */
            case Const.NEW:
            case Const.CHECKCAST:
                buf.append("\t");
                //$FALL-THROUGH$
            case Const.INSTANCEOF:
                index = bytes.readUnsignedShort();
                buf.append("\t<").append(
                        constant_pool.constantToString(index, Const.CONSTANT_Class))
                        .append(">").append(verbose ? " (" + index + ")" : "");
                break;
            /* Operands are references to methods in constant pool
             */
            case Const.INVOKESPECIAL:
            case Const.INVOKESTATIC:
                index = bytes.readUnsignedShort();
                final Constant c = constant_pool.getConstant(index);
                // With Java8 operand may be either a CONSTANT_Methodref
                // or a CONSTANT_InterfaceMethodref.   (markro)
                buf.append("\t").append(
                        constant_pool.constantToString(index, c.getTag()))
                        .append(verbose ? " (" + index + ")" : "");
                break;
            case Const.INVOKEVIRTUAL:
                index = bytes.readUnsignedShort();
                buf.append("\t").append(
                        constant_pool.constantToString(index, Const.CONSTANT_Methodref))
                        .append(verbose ? " (" + index + ")" : "");
                break;
            case Const.INVOKEINTERFACE:
                index = bytes.readUnsignedShort();
                final int nargs = bytes.readUnsignedByte(); // historical, redundant
                buf.append("\t").append(
                        constant_pool
                                .constantToString(index, Const.CONSTANT_InterfaceMethodref))
                        .append(verbose ? " (" + index + ")\t" : "").append(nargs).append("\t")
                        .append(bytes.readUnsignedByte()); // Last byte is a reserved space
                break;
            case Const.INVOKEDYNAMIC:
                index = bytes.readUnsignedShort();
                buf.append("\t").append(
                        constant_pool
                                .constantToString(index, Const.CONSTANT_InvokeDynamic))
                        .append(verbose ? " (" + index + ")\t" : "")
                        .append(bytes.readUnsignedByte())  // Thrid byte is a reserved space
                        .append(bytes.readUnsignedByte()); // Last byte is a reserved space
                break;
            /* Operands are references to items in constant pool
             */
            case Const.LDC_W:
            case Const.LDC2_W:
                index = bytes.readUnsignedShort();
                buf.append("\t\t").append(
                        constant_pool.constantToString(index, constant_pool.getConstant(index)
                                .getTag())).append(verbose ? " (" + index + ")" : "");
                break;
            case Const.LDC:
                index = bytes.readUnsignedByte();
                buf.append("\t\t").append(
                        constant_pool.constantToString(index, constant_pool.getConstant(index)
                                .getTag())).append(verbose ? " (" + index + ")" : "");
                break;
            /* Array of references.
             */
            case Const.ANEWARRAY:
                index = bytes.readUnsignedShort();
                buf.append("\t\t<").append(
                        compactClassName(constant_pool.getConstantString(index,
                                Const.CONSTANT_Class), false)).append(">").append(
                        verbose ? " (" + index + ")" : "");
                break;
            /* Multidimensional array of references.
             */
            case Const.MULTIANEWARRAY: {
                index = bytes.readUnsignedShort();
                final int dimensions = bytes.readUnsignedByte();
                buf.append("\t<").append(
                        compactClassName(constant_pool.getConstantString(index,
                                Const.CONSTANT_Class), false)).append(">\t").append(dimensions)
                        .append(verbose ? " (" + index + ")" : "");
            }
                break;
            /* Increment local variable.
             */
            case Const.IINC:
                if (wide) {
                    vindex = bytes.readUnsignedShort();
                    constant = bytes.readShort();
                    wide = false;
                } else {
                    vindex = bytes.readUnsignedByte();
                    constant = bytes.readByte();
                }
                buf.append("\t\t%").append(vindex).append("\t").append(constant);
                break;
            default:
                if (Const.getNoOfOperands(opcode) > 0) {
                    for (int i = 0; i < Const.getOperandTypeCount(opcode); i++) {
                        buf.append("\t\t");
                        switch (Const.getOperandType(opcode, i)) {
                            case Const.T_BYTE:
                                buf.append(bytes.readByte());
                                break;
                            case Const.T_SHORT:
                                buf.append(bytes.readShort());
                                break;
                            case Const.T_INT:
                                buf.append(bytes.readInt());
                                break;
                            default: // Never reached
                                throw new IllegalStateException("Unreachable default case reached!");
                        }
                    }
                }
        }
        return buf.toString();
    }


    public static String codeToString( final ByteSequence bytes, final ConstantPool constant_pool )
            throws IOException {
        return codeToString(bytes, constant_pool, true);
    }


    /**
     * Shorten long class names, <em>java/lang/String</em> becomes
     * <em>String</em>.
     *
     * @param str The long class name
     * @return Compacted class name
     */
    public static String compactClassName( final String str ) {
        return compactClassName(str, true);
    }


    /**
     * Shorten long class names, <em>java/lang/String</em> becomes
     * <em>java.lang.String</em>,
     * e.g.. If <em>chopit</em> is <em>true</em> the prefix <em>java.lang</em>
     * is also removed.
     *
     * @param str The long class name
     * @param chopit flag that determines whether chopping is executed or not
     * @return Compacted class name
     */
    public static String compactClassName( final String str, final boolean chopit ) {
        return compactClassName(str, "java.lang.", chopit);
    }


    /**
     * Shorten long class name <em>str</em>, i.e., chop off the <em>prefix</em>,
     * if the
     * class name starts with this string and the flag <em>chopit</em> is true.
     * Slashes <em>/</em> are converted to dots <em>.</em>.
     *
     * @param str The long class name
     * @param prefix The prefix the get rid off
     * @param chopit flag that determines whether chopping is executed or not
     * @return Compacted class name
     */
    public static String compactClassName( String str, final String prefix, final boolean chopit ) {
        final int len = prefix.length();
        str = str.replace('/', '.'); // Is `/' on all systems, even DOS
        if (chopit) {
            // If string starts with `prefix' and contains no further dots
            if (str.startsWith(prefix) && (str.substring(len).indexOf('.') == -1)) {
                str = str.substring(len);
            }
        }
        return str;
    }


    /**
     * @return `flag' with bit `i' set to 1
     */
    public static int setBit( final int flag, final int i ) {
        return flag | pow2(i);
    }


    /**
     * @return `flag' with bit `i' set to 0
     */
    public static int clearBit( final int flag, final int i ) {
        final int bit = pow2(i);
        return (flag & bit) == 0 ? flag : flag ^ bit;
    }


    /**
     * @return true, if bit `i' in `flag' is set
     */
    public static boolean isSet( final int flag, final int i ) {
        return (flag & pow2(i)) != 0;
    }


    /**
     * Converts string containing the method return and argument types
     * to a byte code method signature.
     *
     * @param  ret Return type of method
     * @param  argv Types of method arguments
     * @return Byte code representation of method signature
     *
     * @throws ClassFormatException if the signature is for Void
     */
    public static String methodTypeToSignature( final String ret, final String[] argv )
            throws ClassFormatException {
        final StringBuilder buf = new StringBuilder("(");
        String str;
        if (argv != null) {
            for (final String element : argv) {
                str = getSignature(element);
                if (str.endsWith("V")) {
                    throw new ClassFormatException("Invalid type: " + element);
                }
                buf.append(str);
            }
        }
        str = getSignature(ret);
        buf.append(")").append(str);
        return buf.toString();
    }


    /**
     * Converts argument list portion of method signature to string with all class names compacted.
     *
     * @param  signature    Method signature
     * @return String Array of argument types
     * @throws ClassFormatException
     */
    public static String[] methodSignatureArgumentTypes( final String signature )
            throws ClassFormatException {
        return methodSignatureArgumentTypes(signature, true);
    }


    /**
     * Converts argument list portion of method signature to string.
     *
     * @param  signature    Method signature
     * @param  chopit flag that determines whether chopping is executed or not
     * @return String Array of argument types
     * @throws ClassFormatException
     */
    public static String[] methodSignatureArgumentTypes( final String signature, final boolean chopit )
            throws ClassFormatException {
        final List<String> vec = new ArrayList<>();
        int index;
        try {
            // Skip any type arguments to read argument declarations between `(' and `)'
            index = signature.indexOf('(') + 1;
            if (index <= 0) {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            while (signature.charAt(index) != ')') {
                vec.add(typeSignatureToString(signature.substring(index), chopit));
                //corrected concurrent private static field acess
                index += unwrap(consumed_chars); // update position
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
        return vec.toArray(new String[vec.size()]);
    }


    /**
     * Converts return type portion of method signature to string with all class names compacted.
     *
     * @param  signature    Method signature
     * @return String representation of method return type
     * @throws ClassFormatException
     */
    public static String methodSignatureReturnType( final String signature ) throws ClassFormatException {
        return methodSignatureReturnType(signature, true);
    }


    /**
     * Converts return type portion of method signature to string.
     *
     * @param  signature    Method signature
     * @param  chopit flag that determines whether chopping is executed or not
     * @return String representation of method return type
     * @throws ClassFormatException
     */
    public static String methodSignatureReturnType( final String signature, final boolean chopit ) throws ClassFormatException {
        int index;
        String type;
        try {
            // Read return type after `)'
            index = signature.lastIndexOf(')') + 1;
            if (index <= 0) {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            type = typeSignatureToString(signature.substring(index), chopit);
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
        return type;
    }


    /**
     * Converts method signature to string with all class names compacted.
     *
     * @param  signature to convert
     * @param  name of method
     * @param  access flags of method
     * @return Human readable signature
     */
    public static String methodSignatureToString( final String signature, final String name, final String access ) {
        return methodSignatureToString(signature, name, access, true);
    }


    /**
     * Converts method signature to string.
     *
     * @param  signature to convert
     * @param  name of method
     * @param  access flags of method
     * @param  chopit flag that determines whether chopping is executed or not
     * @return Human readable signature
     */
    public static String methodSignatureToString( final String signature, final String name, final String access, final boolean chopit ) {
        return methodSignatureToString(signature, name, access, chopit, null);
    }


    /**
     * This method converts a method signature string into a Java type declaration like
     * `void main(String[])' and throws a `ClassFormatException' when the parsed
     * type is invalid.
     *
     * @param  signature    Method signature
     * @param  name         Method name
     * @param  access       Method access rights
     * @param  chopit flag that determines whether chopping is executed or not
     * @param  vars the LocalVariableTable for the method
     * @return Java type declaration
     * @throws ClassFormatException
     */
    public static String methodSignatureToString( final String signature, final String name,
            final String access, final boolean chopit, final LocalVariableTable vars ) throws ClassFormatException {
        final StringBuilder buf = new StringBuilder("(");
        String type;
        int index;
        int var_index = access.contains("static") ? 0 : 1;
        try {
            // Skip any type arguments to read argument declarations between `(' and `)'
            index = signature.indexOf('(') + 1;
            if (index <= 0) {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            while (signature.charAt(index) != ')') {
                final String param_type = typeSignatureToString(signature.substring(index), chopit);
                buf.append(param_type);
                if (vars != null) {
                    final LocalVariable l = vars.getLocalVariable(var_index, 0);
                    if (l != null) {
                        buf.append(" ").append(l.getName());
                    }
                } else {
                    buf.append(" arg").append(var_index);
                }
                if ("double".equals(param_type) || "long".equals(param_type)) {
                    var_index += 2;
                } else {
                    var_index++;
                }
                buf.append(", ");
                //corrected concurrent private static field acess
                index += unwrap(consumed_chars); // update position
            }
            index++; // update position
            // Read return type after `)'
            type = typeSignatureToString(signature.substring(index), chopit);
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
        // ignore any throws information in the signature
        if (buf.length() > 1) {
            buf.setLength(buf.length() - 2);
        }
        buf.append(")");
        return access + ((access.length() > 0) ? " " : "") + // May be an empty string
                type + " " + name + buf.toString();
    }


    private static int pow2( final int n ) {
        return 1 << n;
    }


    /**
     * Replace all occurrences of <em>old</em> in <em>str</em> with <em>new</em>.
     *
     * @param str String to permute
     * @param old String to be replaced
     * @param new_ Replacement string
     * @return new String object
     */
    public static String replace( String str, final String old, final String new_ ) {
        int index;
        int old_index;
        try {
            if (str.contains(old)) { // `old' found in str
                final StringBuilder buf = new StringBuilder();
                old_index = 0; // String start offset
                // While we have something to replace
                while ((index = str.indexOf(old, old_index)) != -1) {
                    buf.append(str.substring(old_index, index)); // append prefix
                    buf.append(new_); // append replacement
                    old_index = index + old.length(); // Skip `old'.length chars
                }
                buf.append(str.substring(old_index)); // append rest of string
                str = buf.toString();
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should not occur
            System.err.println(e);
        }
        return str;
    }


    /**
     * WARNING:
     *
     * There is some nomenclature confusion through much of the BCEL code base with
     * respect to the terms Descriptor and Signature.  For the offical definitions see:
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3">
     * Descriptors in The Java Virtual Machine Specification</a>
     *
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.9.1">
     * Signatures in The Java Virtual Machine Specification</a>
     *
     * In brief, a descriptor is a string representing the type of a field or method.
     * Signatures are similar, but more complex.  Signatures are used to encode declarations
     * written in the Java programming language that use types outside the type system of the
     * Java Virtual Machine.  They are used to describe the type of any class, interface,
     * constructor, method or field whose declaration uses type variables or parameterized types.
     *
     * To parse a descriptor, call typeSignatureToString.
     * To parse a signature, call signatureToString.
     *
     * Note that if the signature string is a single, non-generic item, the call to
     * signatureToString reduces to a call to typeSignatureToString.
     * Also note, that if you only wish to parse the first item in a longer signature
     * string, you should call typeSignatureToString directly.
     */


    /**
     * Converts a signature to a string with all class names compacted.
     * Class, Method and Type signatures are supported.
     * Enum and Interface signatures are not supported.
     *
     * @param  signature signature to convert
     * @return String containg human readable signature
     */
    public static String signatureToString( final String signature ) {
        return signatureToString(signature, true);
    }


    /**
     * Converts a signature to a string.
     * Class, Method and Type signatures are supported.
     * Enum and Interface signatures are not supported.
     *
     * @param  signature signature to convert
     * @param  chopit flag that determines whether chopping is executed or not
     * @return String containg human readable signature
     */
    public static String signatureToString( final String signature, final boolean chopit ) {
        String type = "";
        String typeParams = "";
        int index = 0;
        if (signature.charAt(0) == '<') {
            // we have type paramters
            typeParams = typeParamTypesToString(signature, chopit);
            index += unwrap(consumed_chars); // update position
        }
        if (signature.charAt(index) == '(') {
            // We have a Method signature.
            // add types of arguments
            type = typeParams + typeSignaturesToString(signature.substring(index), chopit, ')');
            index += unwrap(consumed_chars); // update position
            // add return type
            type = type + typeSignatureToString(signature.substring(index), chopit);
            index += unwrap(consumed_chars); // update position
            // ignore any throws information in the signature
            return type;
        }
        // Could be Class or Type...
        type = typeSignatureToString(signature.substring(index), chopit);
        index += unwrap(consumed_chars); // update position
        if ((typeParams.length() == 0) && (index == signature.length())) {
            // We have a Type signature.
            return type;
        }
        // We have a Class signature.
        final StringBuilder typeClass = new StringBuilder(typeParams);
        typeClass.append(" extends ");
        typeClass.append(type);
        if (index < signature.length()) {
            typeClass.append(" implements ");
            typeClass.append(typeSignatureToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        while (index < signature.length()) {
            typeClass.append(", ");
            typeClass.append(typeSignatureToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        return typeClass.toString();
    }


    /**
     * Converts a type parameter list signature to a string.
     *
     * @param  signature signature to convert
     * @param  chopit flag that determines whether chopping is executed or not
     * @return String containg human readable signature
     */
    private static String typeParamTypesToString( final String signature, final boolean chopit ) {
        // The first character is guranteed to be '<'
        final StringBuilder typeParams = new StringBuilder("<");
        int index = 1;  // skip the '<'
        // get the first TypeParameter
        typeParams.append(typeParamTypeToString(signature.substring(index), chopit));
        index += unwrap(consumed_chars); // update position
        // are there more TypeParameters?
        while (signature.charAt(index) != '>') {
            typeParams.append(", ");
            typeParams.append(typeParamTypeToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        wrap(consumed_chars, index + 1); // account for the '>' char
        return typeParams.append(">").toString();
    }


    /**
     * Converts a type parameter signature to a string.
     *
     * @param  signature signature to convert
     * @param  chopit flag that determines whether chopping is executed or not
     * @return String containg human readable signature
     */
    private static String typeParamTypeToString( final String signature, final boolean chopit ) {
        int index = signature.indexOf(':');
        if (index <= 0) {
            throw new ClassFormatException("Invalid type parameter signature: " + signature);
        }
        // get the TypeParameter identifier
        final StringBuilder typeParam = new StringBuilder(signature.substring(0, index));
        index++;  // account for the ':'
        if (signature.charAt(index) != ':') {
            // we have a class bound
            typeParam.append(" extends ");
            typeParam.append(typeSignatureToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        // look for interface bounds
        while (signature.charAt(index) == ':') {
            index++;  // skip over the ':'
            typeParam.append(" & ");
            typeParam.append(typeSignatureToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        wrap(consumed_chars, index);
        return typeParam.toString();
    }


    /**
     * Converts a list of type signatures to a string.
     *
     * @param  signature signature to convert
     * @param  chopit flag that determines whether chopping is executed or not
     * @param  term character indicating the end of the list
     * @return String containg human readable signature
     */
    private static String typeSignaturesToString( final String signature, final boolean chopit, final char term ) {
        // The first character will be an 'open' that matches the 'close' contained in term.
        final StringBuilder typeList = new StringBuilder(signature.substring(0, 1));
        int index = 1;  // skip the 'open' character
        // get the first Type in the list
        if (signature.charAt(index) != term) {
            typeList.append(typeSignatureToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        // are there more types in the list?
        while (signature.charAt(index) != term) {
            typeList.append(", ");
            typeList.append(typeSignatureToString(signature.substring(index), chopit));
            index += unwrap(consumed_chars); // update position
        }
        wrap(consumed_chars, index + 1); // account for the term char
        return typeList.append(term).toString();
    }


    /**
     *
     * This method converts a type signature string into a Java type declaration such as
     * `String[]' and throws a `ClassFormatException' when the parsed type is invalid.
     *
     * @param  signature type signature
     * @param  chopit flag that determines whether chopping is executed or not
     * @return string containing human readable type signature
     * @throws ClassFormatException
     * @since 6.4.0
     */
    public static String typeSignatureToString( final String signature, final boolean chopit ) throws ClassFormatException {
        //corrected concurrent private static field acess
        wrap(consumed_chars, 1); // This is the default, read just one char like `B'
        try {
            switch (signature.charAt(0)) {
                case 'B':
                    return "byte";
                case 'C':
                    return "char";
                case 'D':
                    return "double";
                case 'F':
                    return "float";
                case 'I':
                    return "int";
                case 'J':
                    return "long";
                case 'T': { // TypeVariableSignature
                    final int index = signature.indexOf(';'); // Look for closing `;'
                    if (index < 0) {
                        throw new ClassFormatException("Invalid type variable signature: " + signature);
                    }
                    //corrected concurrent private static field acess
                    wrap(consumed_chars, index + 1); // "Tblabla;" `T' and `;' are removed
                    return compactClassName(signature.substring(1, index), chopit);
                }
                case 'L': { // Full class name
                    // should this be a while loop? can there be more than
                    // one generic clause?  (markro)
                    int fromIndex = signature.indexOf('<'); // generic type?
                    if (fromIndex < 0) {
                        fromIndex = 0;
                    } else {
                        fromIndex = signature.indexOf('>', fromIndex);
                        if (fromIndex < 0) {
                            throw new ClassFormatException("Invalid signature: " + signature);
                        }
                    }
                    final int index = signature.indexOf(';', fromIndex); // Look for closing `;'
                    if (index < 0) {
                        throw new ClassFormatException("Invalid signature: " + signature);
                    }

                    // check to see if there are any TypeArguments
                    final int bracketIndex = signature.substring(0, index).indexOf('<');
                    if (bracketIndex < 0) {
                        // just a class identifier
                        wrap(consumed_chars, index + 1); // "Lblabla;" `L' and `;' are removed
                        return compactClassName(signature.substring(1, index), chopit);
                    }
                    // but make sure we are not looking past the end of the current item
                    fromIndex = signature.indexOf(';');
                    if (fromIndex < 0) {
                        throw new ClassFormatException("Invalid signature: " + signature);
                    }
                    if (fromIndex < bracketIndex) {
                        // just a class identifier
                        wrap(consumed_chars, fromIndex + 1); // "Lblabla;" `L' and `;' are removed
                        return compactClassName(signature.substring(1, fromIndex), chopit);
                    }

                    // we have TypeArguments; build up partial result
                    // as we recurse for each TypeArgument
                    final StringBuilder type = new StringBuilder(compactClassName(signature.substring(1, bracketIndex), chopit)).append("<");
                    int consumed_chars = bracketIndex + 1; // Shadows global var

                    // check for wildcards
                    if (signature.charAt(consumed_chars) == '+') {
                        type.append("? extends ");
                        consumed_chars++;
                    } else if (signature.charAt(consumed_chars) == '-') {
                        type.append("? super ");
                        consumed_chars++;
                    }

                    // get the first TypeArgument
                    if (signature.charAt(consumed_chars) == '*') {
                        type.append("?");
                        consumed_chars++;
                    } else {
                        type.append(typeSignatureToString(signature.substring(consumed_chars), chopit));
                        // update our consumed count by the number of characters the for type argument
                        consumed_chars = unwrap(Utility.consumed_chars) + consumed_chars;
                        wrap(Utility.consumed_chars, consumed_chars);
                    }

                    // are there more TypeArguments?
                    while (signature.charAt(consumed_chars) != '>') {
                        type.append(", ");
                        // check for wildcards
                        if (signature.charAt(consumed_chars) == '+') {
                            type.append("? extends ");
                            consumed_chars++;
                        } else if (signature.charAt(consumed_chars) == '-') {
                            type.append("? super ");
                            consumed_chars++;
                        }
                        if (signature.charAt(consumed_chars) == '*') {
                            type.append("?");
                            consumed_chars++;
                        } else {
                            type.append(typeSignatureToString(signature.substring(consumed_chars), chopit));
                            // update our consumed count by the number of characters the for type argument
                            consumed_chars = unwrap(Utility.consumed_chars) + consumed_chars;
                            wrap(Utility.consumed_chars, consumed_chars);
                        }
                    }

                    // process the closing ">"
                    consumed_chars++;
                    type.append(">");

                    if (signature.charAt(consumed_chars) == '.') {
                        // we have a ClassTypeSignatureSuffix
                        type.append(".");
                        // convert SimpleClassTypeSignature to fake ClassTypeSignature
                        // and then recurse to parse it
                        type.append(typeSignatureToString("L" + signature.substring(consumed_chars+1), chopit));
                        // update our consumed count by the number of characters the for type argument
                        // note that this count includes the "L" we added, but that is ok
                        // as it accounts for the "." we didn't consume
                        consumed_chars = unwrap(Utility.consumed_chars) + consumed_chars;
                        wrap(Utility.consumed_chars, consumed_chars);
                        return type.toString();
                    }
                    if (signature.charAt(consumed_chars) != ';') {
                        throw new ClassFormatException("Invalid signature: " + signature);
                    }
                    wrap(Utility.consumed_chars, consumed_chars + 1); // remove final ";"
                    return type.toString();
                }
                case 'S':
                    return "short";
                case 'Z':
                    return "boolean";
                case '[': { // Array declaration
                    int n;
                    StringBuilder brackets;
                    String type;
                    int consumed_chars; // Shadows global var
                    brackets = new StringBuilder(); // Accumulate []'s
                    // Count opening brackets and look for optional size argument
                    for (n = 0; signature.charAt(n) == '['; n++) {
                        brackets.append("[]");
                    }
                    consumed_chars = n; // Remember value
                    // The rest of the string denotes a `<field_type>'
                    type = typeSignatureToString(signature.substring(n), chopit);
                    //corrected concurrent private static field acess
                    //Utility.consumed_chars += consumed_chars; is replaced by:
                    final int _temp = unwrap(Utility.consumed_chars) + consumed_chars;
                    wrap(Utility.consumed_chars, _temp);
                    return type + brackets.toString();
                }
                case 'V':
                    return "void";
                default:
                    throw new ClassFormatException("Invalid signature: `" + signature + "'");
            }
        } catch (final StringIndexOutOfBoundsException e) { // Should never occur
            throw new ClassFormatException("Invalid signature: " + signature, e);
        }
    }


    /** Parse Java type such as "char", or "java.lang.String[]" and return the
     * signature in byte code format, e.g. "C" or "[Ljava/lang/String;" respectively.
     *
     * @param  type Java type
     * @return byte code signature
     */
    public static String getSignature( String type ) {
        final StringBuilder buf = new StringBuilder();
        final char[] chars = type.toCharArray();
        boolean char_found = false;
        boolean delim = false;
        int index = -1;
        loop: for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                case '\t':
                case '\n':
                case '\r':
                case '\f':
                    if (char_found) {
                        delim = true;
                    }
                    break;
                case '[':
                    if (!char_found) {
                        throw new IllegalArgumentException("Illegal type: " + type);
                    }
                    index = i;
                    break loop;
                default:
                    char_found = true;
                    if (!delim) {
                        buf.append(chars[i]);
                    }
            }
        }
        int brackets = 0;
        if (index > 0) {
            brackets = countBrackets(type.substring(index));
        }
        type = buf.toString();
        buf.setLength(0);
        for (int i = 0; i < brackets; i++) {
            buf.append('[');
        }
        boolean found = false;
        for (int i = Const.T_BOOLEAN; (i <= Const.T_VOID) && !found; i++) {
            if (Const.getTypeName(i).equals(type)) {
                found = true;
                buf.append(Const.getShortTypeName(i));
            }
        }
        if (!found) {
            buf.append('L').append(type.replace('.', '/')).append(';');
        }
        return buf.toString();
    }


    private static int countBrackets( final String brackets ) {
        final char[] chars = brackets.toCharArray();
        int count = 0;
        boolean open = false;
        for (final char c : chars) {
            switch (c) {
                case '[':
                    if (open) {
                        throw new IllegalArgumentException("Illegally nested brackets:" + brackets);
                    }
                    open = true;
                    break;
                case ']':
                    if (!open) {
                        throw new IllegalArgumentException("Illegally nested brackets:" + brackets);
                    }
                    open = false;
                    count++;
                    break;
                default:
                    // Don't care
                    break;
            }
        }
        if (open) {
            throw new IllegalArgumentException("Illegally nested brackets:" + brackets);
        }
        return count;
    }


    /**
     * Return type of method signature as a byte value as defined in <em>Constants</em>
     *
     * @param  signature in format described above
     * @return type of method signature
     * @see    Const
     *
     * @throws ClassFormatException if signature is not a method signature
     */
    public static byte typeOfMethodSignature( final String signature ) throws ClassFormatException {
        int index;
        try {
            if (signature.charAt(0) != '(') {
                throw new ClassFormatException("Invalid method signature: " + signature);
            }
            index = signature.lastIndexOf(')') + 1;
            return typeOfSignature(signature.substring(index));
        } catch (final StringIndexOutOfBoundsException e) {
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
    }


    /**
     * Return type of signature as a byte value as defined in <em>Constants</em>
     *
     * @param  signature in format described above
     * @return type of signature
     * @see    Const
     *
     * @throws ClassFormatException if signature isn't a known type
     */
    public static byte typeOfSignature( final String signature ) throws ClassFormatException {
        try {
            switch (signature.charAt(0)) {
                case 'B':
                    return Const.T_BYTE;
                case 'C':
                    return Const.T_CHAR;
                case 'D':
                    return Const.T_DOUBLE;
                case 'F':
                    return Const.T_FLOAT;
                case 'I':
                    return Const.T_INT;
                case 'J':
                    return Const.T_LONG;
                case 'L':
                case 'T':
                    return Const.T_REFERENCE;
                case '[':
                    return Const.T_ARRAY;
                case 'V':
                    return Const.T_VOID;
                case 'Z':
                    return Const.T_BOOLEAN;
                case 'S':
                    return Const.T_SHORT;
                case '!':
                case '+':
                case '*':
                    return typeOfSignature(signature.substring(1));
                default:
                    throw new ClassFormatException("Invalid method signature: " + signature);
            }
        } catch (final StringIndexOutOfBoundsException e) {
            throw new ClassFormatException("Invalid method signature: " + signature, e);
        }
    }


    /** Map opcode names to opcode numbers. E.g., return Constants.ALOAD for "aload"
     */
    public static short searchOpcode( String name ) {
        name = name.toLowerCase(Locale.ENGLISH);
        for (short i = 0; i < Const.OPCODE_NAMES_LENGTH; i++) {
            if (Const.getOpcodeName(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Convert (signed) byte to (unsigned) short value, i.e., all negative
     * values become positive.
     */
    private static short byteToShort( final byte b ) {
        return (b < 0) ? (short) (256 + b) : (short) b;
    }


    /** Convert bytes into hexadecimal string
     *
     * @param bytes an array of bytes to convert to hexadecimal
     *
     * @return bytes as hexadecimal string, e.g. 00 fa 12 ...
     */
    public static String toHexString( final byte[] bytes ) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            final short b = byteToShort(bytes[i]);
            final String hex = Integer.toHexString(b);
            if (b < 0x10) {
                buf.append('0');
            }
            buf.append(hex);
            if (i < bytes.length - 1) {
                buf.append(' ');
            }
        }
        return buf.toString();
    }


    /**
     * Return a string for an integer justified left or right and filled up with
     * `fill' characters if necessary.
     *
     * @param i integer to format
     * @param length length of desired string
     * @param left_justify format left or right
     * @param fill fill character
     * @return formatted int
     */
    public static String format( final int i, final int length, final boolean left_justify, final char fill ) {
        return fillup(Integer.toString(i), length, left_justify, fill);
    }


    /**
     * Fillup char with up to length characters with char `fill' and justify it left or right.
     *
     * @param str string to format
     * @param length length of desired string
     * @param left_justify format left or right
     * @param fill fill character
     * @return formatted string
     */
    public static String fillup( final String str, final int length, final boolean left_justify, final char fill ) {
        final int len = length - str.length();
        final char[] buf = new char[(len < 0) ? 0 : len];
        for (int j = 0; j < buf.length; j++) {
            buf[j] = fill;
        }
        if (left_justify) {
            return str + new String(buf);
        }
        return new String(buf) + str;
    }


    static boolean equals( final byte[] a, final byte[] b ) {
        int size;
        if ((size = a.length) != b.length) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }


    public static void printArray( final PrintStream out, final Object[] obj ) {
        out.println(printArray(obj, true));
    }


    public static void printArray( final PrintWriter out, final Object[] obj ) {
        out.println(printArray(obj, true));
    }


    public static String printArray( final Object[] obj ) {
        return printArray(obj, true);
    }


    public static String printArray( final Object[] obj, final boolean braces ) {
        return printArray(obj, braces, false);
    }


    public static String printArray( final Object[] obj, final boolean braces, final boolean quote ) {
        if (obj == null) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        if (braces) {
            buf.append('{');
        }
        for (int i = 0; i < obj.length; i++) {
            if (obj[i] != null) {
                buf.append(quote ? "\"" : "").append(obj[i]).append(quote ? "\"" : "");
            } else {
                buf.append("null");
            }
            if (i < obj.length - 1) {
                buf.append(", ");
            }
        }
        if (braces) {
            buf.append('}');
        }
        return buf.toString();
    }


    /**
     * @param ch the character to test if it's part of an identifier
     *
     * @return true, if character is one of (a, ... z, A, ... Z, 0, ... 9, _)
     */
    public static boolean isJavaIdentifierPart( final char ch ) {
        return ((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))
                || ((ch >= '0') && (ch <= '9')) || (ch == '_');
    }


    /**
     * Encode byte array it into Java identifier string, i.e., a string
     * that only contains the following characters: (a, ... z, A, ... Z,
     * 0, ... 9, _, $).  The encoding algorithm itself is not too
     * clever: if the current byte's ASCII value already is a valid Java
     * identifier part, leave it as it is. Otherwise it writes the
     * escape character($) followed by:
     *
     * <ul>
     *   <li> the ASCII value as a hexadecimal string, if the value is not in the range 200..247</li>
     *   <li>a Java identifier char not used in a lowercase hexadecimal string, if the value is in the range 200..247</li>
     * </ul>
     *
     * <p>This operation inflates the original byte array by roughly 40-50%</p>
     *
     * @param bytes the byte array to convert
     * @param compress use gzip to minimize string
     *
     * @throws IOException if there's a gzip exception
     */
    public static String encode(byte[] bytes, final boolean compress) throws IOException {
        if (compress) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    GZIPOutputStream gos = new GZIPOutputStream(baos)) {
                gos.write(bytes, 0, bytes.length);
                gos.finish();
                bytes = baos.toByteArray();
            }
        }
        final CharArrayWriter caw = new CharArrayWriter();
        try (JavaWriter jw = new JavaWriter(caw)) {
            for (final byte b : bytes) {
                final int in = b & 0x000000ff; // Normalize to unsigned
                jw.write(in);
            }
        }
        return caw.toString();
    }


    /**
     * Decode a string back to a byte array.
     *
     * @param s the string to convert
     * @param uncompress use gzip to uncompress the stream of bytes
     *
     * @throws IOException if there's a gzip exception
     */
    public static byte[] decode(final String s, final boolean uncompress) throws IOException {
        byte[] bytes;
        try (JavaReader jr = new JavaReader(new CharArrayReader(s.toCharArray()));
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            int ch;
            while ((ch = jr.read()) >= 0) {
                bos.write(ch);
            }
            bytes = bos.toByteArray();
        }
        if (uncompress) {
            final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
            final byte[] tmp = new byte[bytes.length * 3]; // Rough estimate
            int count = 0;
            int b;
            while ((b = gis.read()) >= 0) {
                tmp[count++] = (byte) b;
            }
            bytes = new byte[count];
            System.arraycopy(tmp, 0, bytes, 0, count);
        }
        return bytes;
    }

    // A-Z, g-z, _, $
    private static final int FREE_CHARS = 48;
    private static int[] CHAR_MAP = new int[FREE_CHARS];
    private static int[] MAP_CHAR = new int[256]; // Reverse map
    private static final char ESCAPE_CHAR = '$';
    static {
        int j = 0;
        for (int i = 'A'; i <= 'Z'; i++) {
            CHAR_MAP[j] = i;
            MAP_CHAR[i] = j;
            j++;
        }
        for (int i = 'g'; i <= 'z'; i++) {
            CHAR_MAP[j] = i;
            MAP_CHAR[i] = j;
            j++;
        }
        CHAR_MAP[j] = '$';
        MAP_CHAR['$'] = j;
        j++;
        CHAR_MAP[j] = '_';
        MAP_CHAR['_'] = j;
    }

    /**
     * Decode characters into bytes.
     * Used by <a href="Utility.html#decode(java.lang.String, boolean)">decode()</a>
     */
    private static class JavaReader extends FilterReader {

        public JavaReader(final Reader in) {
            super(in);
        }


        @Override
        public int read() throws IOException {
            final int b = in.read();
            if (b != ESCAPE_CHAR) {
                return b;
            }
            final int i = in.read();
            if (i < 0) {
                return -1;
            }
            if (((i >= '0') && (i <= '9')) || ((i >= 'a') && (i <= 'f'))) { // Normal escape
                final int j = in.read();
                if (j < 0) {
                    return -1;
                }
                final char[] tmp = {
                        (char) i, (char) j
                };
                final int s = Integer.parseInt(new String(tmp), 16);
                return s;
            }
            return MAP_CHAR[i];
        }


        @Override
        public int read( final char[] cbuf, final int off, final int len ) throws IOException {
            for (int i = 0; i < len; i++) {
                cbuf[off + i] = (char) read();
            }
            return len;
        }
    }

    /**
     * Encode bytes into valid java identifier characters.
     * Used by <a href="Utility.html#encode(byte[], boolean)">encode()</a>
     */
    private static class JavaWriter extends FilterWriter {

        public JavaWriter(final Writer out) {
            super(out);
        }


        @Override
        public void write( final int b ) throws IOException {
            if (isJavaIdentifierPart((char) b) && (b != ESCAPE_CHAR)) {
                out.write(b);
            } else {
                out.write(ESCAPE_CHAR); // Escape character
                // Special escape
                if (b >= 0 && b < FREE_CHARS) {
                    out.write(CHAR_MAP[b]);
                } else { // Normal escape
                    final char[] tmp = Integer.toHexString(b).toCharArray();
                    if (tmp.length == 1) {
                        out.write('0');
                        out.write(tmp[0]);
                    } else {
                        out.write(tmp[0]);
                        out.write(tmp[1]);
                    }
                }
            }
        }


        @Override
        public void write( final char[] cbuf, final int off, final int len ) throws IOException {
            for (int i = 0; i < len; i++) {
                write(cbuf[off + i]);
            }
        }


        @Override
        public void write( final String str, final int off, final int len ) throws IOException {
            write(str.toCharArray(), off, len);
        }
    }


    /**
     * Escape all occurences of newline chars '\n', quotes \", etc.
     */
    public static String convertString( final String label ) {
        final char[] ch = label.toCharArray();
        final StringBuilder buf = new StringBuilder();
        for (final char element : ch) {
            switch (element) {
                case '\n':
                    buf.append("\\n");
                    break;
                case '\r':
                    buf.append("\\r");
                    break;
                case '\"':
                    buf.append("\\\"");
                    break;
                case '\'':
                    buf.append("\\'");
                    break;
                case '\\':
                    buf.append("\\\\");
                    break;
                default:
                    buf.append(element);
                    break;
            }
        }
        return buf.toString();
    }

}
