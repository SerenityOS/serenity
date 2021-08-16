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

import com.sun.org.apache.bcel.internal.Const;
import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.org.apache.bcel.internal.classfile.ConstantClass;
import com.sun.org.apache.bcel.internal.classfile.ConstantFieldref;
import com.sun.org.apache.bcel.internal.classfile.ConstantInterfaceMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantMethodref;
import com.sun.org.apache.bcel.internal.classfile.ConstantNameAndType;
import com.sun.org.apache.bcel.internal.classfile.ConstantPool;
import com.sun.org.apache.bcel.internal.classfile.ConstantString;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.classfile.Utility;

/**
 * Convert constant pool into HTML file.
 *
 *
 */
final class ConstantHTML {

    private final String className; // name of current class
    private final String classPackage; // name of package
    private final ConstantPool constantPool; // reference to constant pool
    private final PrintWriter file; // file to write to
    private final String[] constantRef; // String to return for cp[i]
    private final Constant[] constants; // The constants in the cp
    private final Method[] methods;


    ConstantHTML(final String dir, final String class_name, final String class_package, final Method[] methods,
            final ConstantPool constant_pool) throws IOException {
        this.className = class_name;
        this.classPackage = class_package;
        this.constantPool = constant_pool;
        this.methods = methods;
        constants = constant_pool.getConstantPool();
        file = new PrintWriter(new FileOutputStream(dir + class_name + "_cp.html"));
        constantRef = new String[constants.length];
        constantRef[0] = "&lt;unknown&gt;";
        file.println("<HTML><BODY BGCOLOR=\"#C0C0C0\"><TABLE BORDER=0>");
        // Loop through constants, constants[0] is reserved
        for (int i = 1; i < constants.length; i++) {
            if (i % 2 == 0) {
                file.print("<TR BGCOLOR=\"#C0C0C0\"><TD>");
            } else {
                file.print("<TR BGCOLOR=\"#A0A0A0\"><TD>");
            }
            if (constants[i] != null) {
                writeConstant(i);
            }
            file.print("</TD></TR>\n");
        }
        file.println("</TABLE></BODY></HTML>");
        file.close();
    }


    String referenceConstant( final int index ) {
        return constantRef[index];
    }


    private void writeConstant( final int index ) {
        final byte tag = constants[index].getTag();
        int class_index;
        int name_index;
        String ref;
        // The header is always the same
        file.println("<H4> <A NAME=cp" + index + ">" + index + "</A> " + Const.getConstantName(tag)
                + "</H4>");
        /* For every constant type get the needed parameters and print them appropiately
         */
        switch (tag) {
            case Const.CONSTANT_InterfaceMethodref:
            case Const.CONSTANT_Methodref:
                // Get class_index and name_and_type_index, depending on type
                if (tag == Const.CONSTANT_Methodref) {
                    final ConstantMethodref c = (ConstantMethodref) constantPool.getConstant(index,
                            Const.CONSTANT_Methodref);
                    class_index = c.getClassIndex();
                    name_index = c.getNameAndTypeIndex();
                } else {
                    final ConstantInterfaceMethodref c1 = (ConstantInterfaceMethodref) constantPool
                            .getConstant(index, Const.CONSTANT_InterfaceMethodref);
                    class_index = c1.getClassIndex();
                    name_index = c1.getNameAndTypeIndex();
                }
                // Get method name and its class
                final String method_name = constantPool.constantToString(name_index,
                        Const.CONSTANT_NameAndType);
                final String html_method_name = Class2HTML.toHTML(method_name);
                // Partially compacted class name, i.e., / -> .
                final String method_class = constantPool.constantToString(class_index, Const.CONSTANT_Class);
                String short_method_class = Utility.compactClassName(method_class); // I.e., remove java.lang.
                short_method_class = Utility.compactClassName(short_method_class, classPackage
                        + ".", true); // Remove class package prefix
                // Get method signature
                final ConstantNameAndType c2 = (ConstantNameAndType) constantPool.getConstant(
                        name_index, Const.CONSTANT_NameAndType);
                final String signature = constantPool.constantToString(c2.getSignatureIndex(),
                        Const.CONSTANT_Utf8);
                // Get array of strings containing the argument types
                final String[] args = Utility.methodSignatureArgumentTypes(signature, false);
                // Get return type string
                final String type = Utility.methodSignatureReturnType(signature, false);
                final String ret_type = Class2HTML.referenceType(type);
                final StringBuilder buf = new StringBuilder("(");
                for (int i = 0; i < args.length; i++) {
                    buf.append(Class2HTML.referenceType(args[i]));
                    if (i < args.length - 1) {
                        buf.append(",&nbsp;");
                    }
                }
                buf.append(")");
                final String arg_types = buf.toString();
                if (method_class.equals(className)) {
                    ref = "<A HREF=\"" + className + "_code.html#method"
                            + getMethodNumber(method_name + signature) + "\" TARGET=Code>"
                            + html_method_name + "</A>";
                } else {
                    ref = "<A HREF=\"" + method_class + ".html" + "\" TARGET=_top>"
                            + short_method_class + "</A>." + html_method_name;
                }
                constantRef[index] = ret_type + "&nbsp;<A HREF=\"" + className + "_cp.html#cp"
                        + class_index + "\" TARGET=Constants>" + short_method_class
                        + "</A>.<A HREF=\"" + className + "_cp.html#cp" + index
                        + "\" TARGET=ConstantPool>" + html_method_name + "</A>&nbsp;" + arg_types;
                file.println("<P><TT>" + ret_type + "&nbsp;" + ref + arg_types
                        + "&nbsp;</TT>\n<UL>" + "<LI><A HREF=\"#cp" + class_index
                        + "\">Class index(" + class_index + ")</A>\n" + "<LI><A HREF=\"#cp"
                        + name_index + "\">NameAndType index(" + name_index + ")</A></UL>");
                break;
            case Const.CONSTANT_Fieldref:
                // Get class_index and name_and_type_index
                final ConstantFieldref c3 = (ConstantFieldref) constantPool.getConstant(index,
                        Const.CONSTANT_Fieldref);
                class_index = c3.getClassIndex();
                name_index = c3.getNameAndTypeIndex();
                // Get method name and its class (compacted)
                final String field_class = constantPool.constantToString(class_index, Const.CONSTANT_Class);
                String short_field_class = Utility.compactClassName(field_class); // I.e., remove java.lang.
                short_field_class = Utility.compactClassName(short_field_class,
                        classPackage + ".", true); // Remove class package prefix
                final String field_name = constantPool
                        .constantToString(name_index, Const.CONSTANT_NameAndType);
                if (field_class.equals(className)) {
                    ref = "<A HREF=\"" + field_class + "_methods.html#field" + field_name
                            + "\" TARGET=Methods>" + field_name + "</A>";
                } else {
                    ref = "<A HREF=\"" + field_class + ".html\" TARGET=_top>" + short_field_class
                            + "</A>." + field_name + "\n";
                }
                constantRef[index] = "<A HREF=\"" + className + "_cp.html#cp" + class_index
                        + "\" TARGET=Constants>" + short_field_class + "</A>.<A HREF=\""
                        + className + "_cp.html#cp" + index + "\" TARGET=ConstantPool>"
                        + field_name + "</A>";
                file.println("<P><TT>" + ref + "</TT><BR>\n" + "<UL>" + "<LI><A HREF=\"#cp"
                        + class_index + "\">Class(" + class_index + ")</A><BR>\n"
                        + "<LI><A HREF=\"#cp" + name_index + "\">NameAndType(" + name_index
                        + ")</A></UL>");
                break;
            case Const.CONSTANT_Class:
                final ConstantClass c4 = (ConstantClass) constantPool.getConstant(index, Const.CONSTANT_Class);
                name_index = c4.getNameIndex();
                final String class_name2 = constantPool.constantToString(index, tag); // / -> .
                String short_class_name = Utility.compactClassName(class_name2); // I.e., remove java.lang.
                short_class_name = Utility.compactClassName(short_class_name, classPackage + ".",
                        true); // Remove class package prefix
                ref = "<A HREF=\"" + class_name2 + ".html\" TARGET=_top>" + short_class_name
                        + "</A>";
                constantRef[index] = "<A HREF=\"" + className + "_cp.html#cp" + index
                        + "\" TARGET=ConstantPool>" + short_class_name + "</A>";
                file.println("<P><TT>" + ref + "</TT><UL>" + "<LI><A HREF=\"#cp" + name_index
                        + "\">Name index(" + name_index + ")</A></UL>\n");
                break;
            case Const.CONSTANT_String:
                final ConstantString c5 = (ConstantString) constantPool.getConstant(index,
                        Const.CONSTANT_String);
                name_index = c5.getStringIndex();
                final String str = Class2HTML.toHTML(constantPool.constantToString(index, tag));
                file.println("<P><TT>" + str + "</TT><UL>" + "<LI><A HREF=\"#cp" + name_index
                        + "\">Name index(" + name_index + ")</A></UL>\n");
                break;
            case Const.CONSTANT_NameAndType:
                final ConstantNameAndType c6 = (ConstantNameAndType) constantPool.getConstant(index,
                        Const.CONSTANT_NameAndType);
                name_index = c6.getNameIndex();
                final int signature_index = c6.getSignatureIndex();
                file.println("<P><TT>"
                        + Class2HTML.toHTML(constantPool.constantToString(index, tag))
                        + "</TT><UL>" + "<LI><A HREF=\"#cp" + name_index + "\">Name index("
                        + name_index + ")</A>\n" + "<LI><A HREF=\"#cp" + signature_index
                        + "\">Signature index(" + signature_index + ")</A></UL>\n");
                break;
            default:
                file.println("<P><TT>" + Class2HTML.toHTML(constantPool.constantToString(index, tag)) + "</TT>\n");
        } // switch
    }


    private int getMethodNumber( final String str ) {
        for (int i = 0; i < methods.length; i++) {
            final String cmp = methods[i].getName() + methods[i].getSignature();
            if (cmp.equals(str)) {
                return i;
            }
        }
        return -1;
    }
}
