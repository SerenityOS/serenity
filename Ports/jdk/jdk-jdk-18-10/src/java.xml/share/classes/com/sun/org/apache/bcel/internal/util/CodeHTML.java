/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
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

package com.sun.org.apache.bcel.internal.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.BitSet;

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.Attribute;
import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.CodeException;
import com.sun.org.apache.bcel.internal.classfile.ConstantFieldref;
import com.sun.org.apache.bcel.internal.classfile.ConstantInterfaceMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantInvokeDynamic;
import com.sun.org.apache.bcel.internal.classfile.ConstantMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantNameAndType;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.LocalVariable;
import com.sun.org.apache.bcel.internal.classfile.LocalVariableTable;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Utility;

/**
 * Convert code into HTML file.
 *
 *
 */
final class CodeHTML {

    private final String className; // name of current class
//    private Method[] methods; // Methods to print
    private final PrintWriter file; // file to write to
    private BitSet gotoSet;
    private final ConstantPool constantPool;
    private final ConstantHTML constantHtml;
    private static boolean wide = false;


    CodeHTML(final String dir, final String class_name, final Method[] methods, final ConstantPool constant_pool,
            final ConstantHTML constant_html) throws IOException {
        this.className = class_name;
//        this.methods = methods;
        this.constantPool = constant_pool;
        this.constantHtml = constant_html;
        file = new PrintWriter(new FileOutputStream(dir + class_name + "_code.html"));
        file.println("<HTML><BODY BGCOLOR=\"#C0C0C0\">");
        for (int i = 0; i < methods.length; i++) {
            writeMethod(methods[i], i);
        }
        file.println("</BODY></HTML>");
        file.close();
    }


    /**
     * Disassemble a stream of byte codes and return the
     * string representation.
     *
     * @param  stream data input stream
     * @return String representation of byte code
     */
    private String codeToHTML( final ByteSequence bytes, final int method_number ) throws IOException {
        final short opcode = (short) bytes.readUnsignedByte();
        String name;
        String signature;
        int default_offset = 0;
        int low;
        int high;
        int index;
        int class_index;
        int vindex;
        int constant;
        int[] jump_table;
        int no_pad_bytes = 0;
        int offset;
        final StringBuilder buf = new StringBuilder(256); // CHECKSTYLE IGNORE MagicNumber
        buf.append("<TT>").append(Const.getOpcodeName(opcode)).append("</TT></TD><TD>");
        /* Special case: Skip (0-3) padding bytes, i.e., the
         * following bytes are 4-byte-aligned
         */
        if ((opcode == Const.TABLESWITCH) || (opcode == Const.LOOKUPSWITCH)) {
            final int remainder = bytes.getIndex() % 4;
            no_pad_bytes = (remainder == 0) ? 0 : 4 - remainder;
            for (int i = 0; i < no_pad_bytes; i++) {
                bytes.readByte();
            }
            // Both cases have a field default_offset in common
            default_offset = bytes.readInt();
        }
        switch (opcode) {
            case Const.TABLESWITCH:
                low = bytes.readInt();
                high = bytes.readInt();
                offset = bytes.getIndex() - 12 - no_pad_bytes - 1;
                default_offset += offset;
                buf.append("<TABLE BORDER=1><TR>");
                // Print switch indices in first row (and default)
                jump_table = new int[high - low + 1];
                for (int i = 0; i < jump_table.length; i++) {
                    jump_table[i] = offset + bytes.readInt();
                    buf.append("<TH>").append(low + i).append("</TH>");
                }
                buf.append("<TH>default</TH></TR>\n<TR>");
                // Print target and default indices in second row
            for (final int element : jump_table) {
                buf.append("<TD><A HREF=\"#code").append(method_number).append("@").append(
                        element).append("\">").append(element).append("</A></TD>");
            }
                buf.append("<TD><A HREF=\"#code").append(method_number).append("@").append(
                        default_offset).append("\">").append(default_offset).append(
                        "</A></TD></TR>\n</TABLE>\n");
                break;
            /* Lookup switch has variable length arguments.
             */
            case Const.LOOKUPSWITCH:
                final int npairs = bytes.readInt();
                offset = bytes.getIndex() - 8 - no_pad_bytes - 1;
                jump_table = new int[npairs];
                default_offset += offset;
                buf.append("<TABLE BORDER=1><TR>");
                // Print switch indices in first row (and default)
                for (int i = 0; i < npairs; i++) {
                    final int match = bytes.readInt();
                    jump_table[i] = offset + bytes.readInt();
                    buf.append("<TH>").append(match).append("</TH>");
                }
                buf.append("<TH>default</TH></TR>\n<TR>");
                // Print target and default indices in second row
                for (int i = 0; i < npairs; i++) {
                    buf.append("<TD><A HREF=\"#code").append(method_number).append("@").append(
                            jump_table[i]).append("\">").append(jump_table[i]).append("</A></TD>");
                }
                buf.append("<TD><A HREF=\"#code").append(method_number).append("@").append(
                        default_offset).append("\">").append(default_offset).append(
                        "</A></TD></TR>\n</TABLE>\n");
                break;
            /* Two address bytes + offset from start of byte stream form the
             * jump target.
             */
            case Const.GOTO:
            case Const.IFEQ:
            case Const.IFGE:
            case Const.IFGT:
            case Const.IFLE:
            case Const.IFLT:
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
            case Const.JSR:
                index = bytes.getIndex() + bytes.readShort() - 1;
                buf.append("<A HREF=\"#code").append(method_number).append("@").append(index)
                        .append("\">").append(index).append("</A>");
                break;
            /* Same for 32-bit wide jumps
             */
            case Const.GOTO_W:
            case Const.JSR_W:
                final int windex = bytes.getIndex() + bytes.readInt() - 1;
                buf.append("<A HREF=\"#code").append(method_number).append("@").append(windex)
                        .append("\">").append(windex).append("</A>");
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
                    vindex = bytes.readShort();
                    wide = false; // Clear flag
                } else {
                    vindex = bytes.readUnsignedByte();
                }
                buf.append("%").append(vindex);
                break;
            /*
             * Remember wide byte which is used to form a 16-bit address in the
             * following instruction. Relies on that the method is called again with
             * the following opcode.
             */
            case Const.WIDE:
                wide = true;
                buf.append("(wide)");
                break;
            /* Array of basic type.
             */
            case Const.NEWARRAY:
                buf.append("<FONT COLOR=\"#00FF00\">").append(Const.getTypeName(bytes.readByte())).append(
                        "</FONT>");
                break;
            /* Access object/class fields.
             */
            case Const.GETFIELD:
            case Const.GETSTATIC:
            case Const.PUTFIELD:
            case Const.PUTSTATIC:
                index = bytes.readShort();
                final ConstantFieldref c1 = (ConstantFieldref) constantPool.getConstant(index,
                        Const.CONSTANT_Fieldref);
                class_index = c1.getClassIndex();
                name = constantPool.getConstantString(class_index, Const.CONSTANT_Class);
                name = Utility.compactClassName(name, false);
                index = c1.getNameAndTypeIndex();
                final String field_name = constantPool.constantToString(index, Const.CONSTANT_NameAndType);
                if (name.equals(className)) { // Local field
                    buf.append("<A HREF=\"").append(className).append("_methods.html#field")
                            .append(field_name).append("\" TARGET=Methods>").append(field_name)
                            .append("</A>\n");
                } else {
                    buf.append(constantHtml.referenceConstant(class_index)).append(".").append(
                            field_name);
                }
                break;
            /* Operands are references to classes in constant pool
             */
            case Const.CHECKCAST:
            case Const.INSTANCEOF:
            case Const.NEW:
                index = bytes.readShort();
                buf.append(constantHtml.referenceConstant(index));
                break;
            /* Operands are references to methods in constant pool
             */
            case Const.INVOKESPECIAL:
            case Const.INVOKESTATIC:
            case Const.INVOKEVIRTUAL:
            case Const.INVOKEINTERFACE:
            case Const.INVOKEDYNAMIC:
                final int m_index = bytes.readShort();
                String str;
                if (opcode == Const.INVOKEINTERFACE) { // Special treatment needed
                    bytes.readUnsignedByte(); // Redundant
                    bytes.readUnsignedByte(); // Reserved
//                    int nargs = bytes.readUnsignedByte(); // Redundant
//                    int reserved = bytes.readUnsignedByte(); // Reserved
                    final ConstantInterfaceMethodref c = (ConstantInterfaceMethodref) constantPool
                            .getConstant(m_index, Const.CONSTANT_InterfaceMethodref);
                    class_index = c.getClassIndex();
                    index = c.getNameAndTypeIndex();
                    name = Class2HTML.referenceClass(class_index);
                } else if (opcode == Const.INVOKEDYNAMIC) { // Special treatment needed
                    bytes.readUnsignedByte(); // Reserved
                    bytes.readUnsignedByte(); // Reserved
                    final ConstantInvokeDynamic c = (ConstantInvokeDynamic) constantPool
                            .getConstant(m_index, Const.CONSTANT_InvokeDynamic);
                    index = c.getNameAndTypeIndex();
                    name = "#" + c.getBootstrapMethodAttrIndex();
                } else {
                    // UNDONE: Java8 now allows INVOKESPECIAL and INVOKESTATIC to
                    // reference EITHER a Methodref OR an InterfaceMethodref.
                    // Not sure if that affects this code or not.  (markro)
                    final ConstantMethodref c = (ConstantMethodref) constantPool.getConstant(m_index,
                            Const.CONSTANT_Methodref);
                    class_index = c.getClassIndex();
                    index = c.getNameAndTypeIndex();
                name = Class2HTML.referenceClass(class_index);
                }
                str = Class2HTML.toHTML(constantPool.constantToString(constantPool.getConstant(
                        index, Const.CONSTANT_NameAndType)));
                // Get signature, i.e., types
                final ConstantNameAndType c2 = (ConstantNameAndType) constantPool.getConstant(index,
                        Const.CONSTANT_NameAndType);
                signature = constantPool.constantToString(c2.getSignatureIndex(), Const.CONSTANT_Utf8);
                final String[] args = Utility.methodSignatureArgumentTypes(signature, false);
                final String type = Utility.methodSignatureReturnType(signature, false);
                buf.append(name).append(".<A HREF=\"").append(className).append("_cp.html#cp")
                        .append(m_index).append("\" TARGET=ConstantPool>").append(str).append(
                                "</A>").append("(");
                // List arguments
                for (int i = 0; i < args.length; i++) {
                    buf.append(Class2HTML.referenceType(args[i]));
                    if (i < args.length - 1) {
                        buf.append(", ");
                    }
                }
                // Attach return type
                buf.append("):").append(Class2HTML.referenceType(type));
                break;
            /* Operands are references to items in constant pool
             */
            case Const.LDC_W:
            case Const.LDC2_W:
                index = bytes.readShort();
                buf.append("<A HREF=\"").append(className).append("_cp.html#cp").append(index)
                        .append("\" TARGET=\"ConstantPool\">").append(
                                Class2HTML.toHTML(constantPool.constantToString(index,
                                        constantPool.getConstant(index).getTag()))).append("</a>");
                break;
            case Const.LDC:
                index = bytes.readUnsignedByte();
                buf.append("<A HREF=\"").append(className).append("_cp.html#cp").append(index)
                        .append("\" TARGET=\"ConstantPool\">").append(
                                Class2HTML.toHTML(constantPool.constantToString(index,
                                        constantPool.getConstant(index).getTag()))).append("</a>");
                break;
            /* Array of references.
             */
            case Const.ANEWARRAY:
                index = bytes.readShort();
                buf.append(constantHtml.referenceConstant(index));
                break;
            /* Multidimensional array of references.
             */
            case Const.MULTIANEWARRAY:
                index = bytes.readShort();
                final int dimensions = bytes.readByte();
                buf.append(constantHtml.referenceConstant(index)).append(":").append(dimensions)
                        .append("-dimensional");
                break;
            /* Increment local variable.
             */
            case Const.IINC:
                if (wide) {
                    vindex = bytes.readShort();
                    constant = bytes.readShort();
                    wide = false;
                } else {
                    vindex = bytes.readUnsignedByte();
                    constant = bytes.readByte();
                }
                buf.append("%").append(vindex).append(" ").append(constant);
                break;
            default:
                if (Const.getNoOfOperands(opcode) > 0) {
                    for (int i = 0; i < Const.getOperandTypeCount(opcode); i++) {
                        switch (Const.getOperandType(opcode, i)) {
                            case Const.T_BYTE:
                                buf.append(bytes.readUnsignedByte());
                                break;
                            case Const.T_SHORT: // Either branch or index
                                buf.append(bytes.readShort());
                                break;
                            case Const.T_INT:
                                buf.append(bytes.readInt());
                                break;
                            default: // Never reached
                                throw new IllegalStateException(
                                        "Unreachable default case reached! " +
                                                Const.getOperandType(opcode, i));
                        }
                        buf.append("&nbsp;");
                    }
                }
        }
        buf.append("</TD>");
        return buf.toString();
    }


    /**
     * Find all target addresses in code, so that they can be marked
     * with &lt;A NAME = ...&gt;. Target addresses are kept in an BitSet object.
     */
    private void findGotos( final ByteSequence bytes, final Code code ) throws IOException {
        int index;
        gotoSet = new BitSet(bytes.available());
        int opcode;
        /* First get Code attribute from method and the exceptions handled
         * (try .. catch) in this method. We only need the line number here.
         */
        if (code != null) {
            final CodeException[] ce = code.getExceptionTable();
            for (final CodeException cex : ce) {
                gotoSet.set(cex.getStartPC());
                gotoSet.set(cex.getEndPC());
                gotoSet.set(cex.getHandlerPC());
            }
            // Look for local variables and their range
            final Attribute[] attributes = code.getAttributes();
            for (final Attribute attribute : attributes) {
                if (attribute.getTag() == Const.ATTR_LOCAL_VARIABLE_TABLE) {
                    final LocalVariable[] vars = ((LocalVariableTable) attribute)
                            .getLocalVariableTable();
                    for (final LocalVariable var : vars) {
                        final int start = var.getStartPC();
                        final int end = start + var.getLength();
                        gotoSet.set(start);
                        gotoSet.set(end);
                    }
                    break;
                }
            }
        }
        // Get target addresses from GOTO, JSR, TABLESWITCH, etc.
        for (; bytes.available() > 0;) {
            opcode = bytes.readUnsignedByte();
            //System.out.println(getOpcodeName(opcode));
            switch (opcode) {
                case Const.TABLESWITCH:
                case Const.LOOKUPSWITCH:
                    //bytes.readByte(); // Skip already read byte
                    final int remainder = bytes.getIndex() % 4;
                    final int no_pad_bytes = (remainder == 0) ? 0 : 4 - remainder;
                    int default_offset;
                    int offset;
                    for (int j = 0; j < no_pad_bytes; j++) {
                        bytes.readByte();
                    }
                    // Both cases have a field default_offset in common
                    default_offset = bytes.readInt();
                    if (opcode == Const.TABLESWITCH) {
                        final int low = bytes.readInt();
                        final int high = bytes.readInt();
                        offset = bytes.getIndex() - 12 - no_pad_bytes - 1;
                        default_offset += offset;
                        gotoSet.set(default_offset);
                        for (int j = 0; j < (high - low + 1); j++) {
                            index = offset + bytes.readInt();
                            gotoSet.set(index);
                        }
                    } else { // LOOKUPSWITCH
                        final int npairs = bytes.readInt();
                        offset = bytes.getIndex() - 8 - no_pad_bytes - 1;
                        default_offset += offset;
                        gotoSet.set(default_offset);
                        for (int j = 0; j < npairs; j++) {
//                            int match = bytes.readInt();
                            bytes.readInt();
                            index = offset + bytes.readInt();
                            gotoSet.set(index);
                        }
                    }
                    break;
                case Const.GOTO:
                case Const.IFEQ:
                case Const.IFGE:
                case Const.IFGT:
                case Const.IFLE:
                case Const.IFLT:
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
                case Const.JSR:
                    //bytes.readByte(); // Skip already read byte
                    index = bytes.getIndex() + bytes.readShort() - 1;
                    gotoSet.set(index);
                    break;
                case Const.GOTO_W:
                case Const.JSR_W:
                    //bytes.readByte(); // Skip already read byte
                    index = bytes.getIndex() + bytes.readInt() - 1;
                    gotoSet.set(index);
                    break;
                default:
                    bytes.unreadByte();
                    codeToHTML(bytes, 0); // Ignore output
            }
        }
    }


    /**
     * Write a single method with the byte code associated with it.
     */
    private void writeMethod( final Method method, final int method_number ) throws IOException {
        // Get raw signature
        final String signature = method.getSignature();
        // Get array of strings containing the argument types
        final String[] args = Utility.methodSignatureArgumentTypes(signature, false);
        // Get return type string
        final String type = Utility.methodSignatureReturnType(signature, false);
        // Get method name
        final String name = method.getName();
        final String html_name = Class2HTML.toHTML(name);
        // Get method's access flags
        String access = Utility.accessToString(method.getAccessFlags());
        access = Utility.replace(access, " ", "&nbsp;");
        // Get the method's attributes, the Code Attribute in particular
        final Attribute[] attributes = method.getAttributes();
        file.print("<P><B><FONT COLOR=\"#FF0000\">" + access + "</FONT>&nbsp;" + "<A NAME=method"
                + method_number + ">" + Class2HTML.referenceType(type) + "</A>&nbsp<A HREF=\""
                + className + "_methods.html#method" + method_number + "\" TARGET=Methods>"
                + html_name + "</A>(");
        for (int i = 0; i < args.length; i++) {
            file.print(Class2HTML.referenceType(args[i]));
            if (i < args.length - 1) {
                file.print(",&nbsp;");
            }
        }
        file.println(")</B></P>");
        Code c = null;
        byte[] code = null;
        if (attributes.length > 0) {
            file.print("<H4>Attributes</H4><UL>\n");
            for (int i = 0; i < attributes.length; i++) {
                byte tag = attributes[i].getTag();
                if (tag != Const.ATTR_UNKNOWN) {
                    file.print("<LI><A HREF=\"" + className + "_attributes.html#method"
                            + method_number + "@" + i + "\" TARGET=Attributes>"
                            + Const.getAttributeName(tag) + "</A></LI>\n");
                } else {
                    file.print("<LI>" + attributes[i] + "</LI>");
                }
                if (tag == Const.ATTR_CODE) {
                    c = (Code) attributes[i];
                    final Attribute[] attributes2 = c.getAttributes();
                    code = c.getCode();
                    file.print("<UL>");
                    for (int j = 0; j < attributes2.length; j++) {
                        tag = attributes2[j].getTag();
                        file.print("<LI><A HREF=\"" + className + "_attributes.html#" + "method"
                                + method_number + "@" + i + "@" + j + "\" TARGET=Attributes>"
                                + Const.getAttributeName(tag) + "</A></LI>\n");
                    }
                    file.print("</UL>");
                }
            }
            file.println("</UL>");
        }
        if (code != null) { // No code, an abstract method, e.g.
            //System.out.println(name + "\n" + Utility.codeToString(code, constantPool, 0, -1));
            // Print the byte code
            try (ByteSequence stream = new ByteSequence(code)) {
                stream.mark(stream.available());
                findGotos(stream, c);
                stream.reset();
                file.println("<TABLE BORDER=0><TR><TH ALIGN=LEFT>Byte<BR>offset</TH>"
                        + "<TH ALIGN=LEFT>Instruction</TH><TH ALIGN=LEFT>Argument</TH>");
                for (; stream.available() > 0;) {
                    final int offset = stream.getIndex();
                    final String str = codeToHTML(stream, method_number);
                    String anchor = "";
                    /*
                     * Set an anchor mark if this line is targetted by a goto, jsr, etc. Defining an anchor for every
                     * line is very inefficient!
                     */
                    if (gotoSet.get(offset)) {
                        anchor = "<A NAME=code" + method_number + "@" + offset + "></A>";
                    }
                    String anchor2;
                    if (stream.getIndex() == code.length) {
                        anchor2 = "<A NAME=code" + method_number + "@" + code.length + ">" + offset + "</A>";
                    } else {
                        anchor2 = "" + offset;
                    }
                    file.println("<TR VALIGN=TOP><TD>" + anchor2 + "</TD><TD>" + anchor + str + "</TR>");
                }
            }
            // Mark last line, may be targetted from Attributes window
            file.println("<TR><TD> </A></TD></TR>");
            file.println("</TABLE>");
        }
    }
}
