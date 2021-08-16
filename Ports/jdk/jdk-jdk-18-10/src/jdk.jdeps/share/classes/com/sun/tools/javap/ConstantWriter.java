/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javap;

import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;

import static com.sun.tools.classfile.ConstantPool.*;

/*
 *  Write a constant pool entry.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ConstantWriter extends BasicWriter {
    public static ConstantWriter instance(Context context) {
        ConstantWriter instance = context.get(ConstantWriter.class);
        if (instance == null)
            instance = new ConstantWriter(context);
        return instance;
    }

    protected ConstantWriter(Context context) {
        super(context);
        context.put(ConstantWriter.class, this);
        classWriter = ClassWriter.instance(context);
        options = Options.instance(context);
    }

    protected void writeConstantPool() {
        ConstantPool constant_pool = classWriter.getClassFile().constant_pool;
        writeConstantPool(constant_pool);
    }

    protected void writeConstantPool(ConstantPool constant_pool) {
        ConstantPool.Visitor<Integer, Void> v = new ConstantPool.Visitor<>() {
            public Integer visitClass(CONSTANT_Class_info info, Void p) {
                print("#" + info.name_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitDouble(CONSTANT_Double_info info, Void p) {
                println(stringValue(info));
                return 2;
            }

            public Integer visitFieldref(CONSTANT_Fieldref_info info, Void p) {
                print("#" + info.class_index + ".#" + info.name_and_type_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitFloat(CONSTANT_Float_info info, Void p) {
                println(stringValue(info));
                return 1;
            }

            public Integer visitInteger(CONSTANT_Integer_info info, Void p) {
                println(stringValue(info));
                return 1;
            }

            public Integer visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, Void p) {
                print("#" + info.class_index + ".#" + info.name_and_type_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, Void p) {
                print("#" + info.bootstrap_method_attr_index + ":#" + info.name_and_type_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitDynamicConstant(CONSTANT_Dynamic_info info, Void p) {
                print("#" + info.bootstrap_method_attr_index + ":#" + info.name_and_type_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitLong(CONSTANT_Long_info info, Void p) {
                println(stringValue(info));
                return 2;
            }

            public Integer visitMethodref(CONSTANT_Methodref_info info, Void p) {
                print("#" + info.class_index + ".#" + info.name_and_type_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitMethodHandle(CONSTANT_MethodHandle_info info, Void p) {
                print(info.reference_kind.tag + ":#" + info.reference_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitMethodType(CONSTANT_MethodType_info info, Void p) {
                print("#" + info.descriptor_index);
                tab();
                println("//  " + stringValue(info));
                return 1;
            }

            public Integer visitModule(CONSTANT_Module_info info, Void p) {
                print("#" + info.name_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitNameAndType(CONSTANT_NameAndType_info info, Void p) {
                print("#" + info.name_index + ":#" + info.type_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitPackage(CONSTANT_Package_info info, Void p) {
                print("#" + info.name_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitString(CONSTANT_String_info info, Void p) {
                print("#" + info.string_index);
                tab();
                println("// " + stringValue(info));
                return 1;
            }

            public Integer visitUtf8(CONSTANT_Utf8_info info, Void p) {
                println(stringValue(info));
                return 1;
            }

        };
        println("Constant pool:");
        indent(+1);
        int width = String.valueOf(constant_pool.size()).length() + 1;
        int cpx = 1;
        while (cpx < constant_pool.size()) {
            print(String.format("%" + width + "s", ("#" + cpx)));
            try {
                CPInfo cpInfo = constant_pool.get(cpx);
                print(String.format(" = %-18s ", cpTagName(cpInfo)));
                cpx += cpInfo.accept(v, null);
            } catch (ConstantPool.InvalidIndex ex) {
                // should not happen
            }
        }
        indent(-1);
    }

    protected void write(int cpx) {
        ClassFile classFile = classWriter.getClassFile();
        if (cpx == 0) {
            print("#0");
            return;
        }

        CPInfo cpInfo;
        try {
            cpInfo = classFile.constant_pool.get(cpx);
        } catch (ConstantPoolException e) {
            print("#" + cpx);
            return;
        }

        int tag = cpInfo.getTag();
        switch (tag) {
            case CONSTANT_Methodref:
            case CONSTANT_InterfaceMethodref:
            case CONSTANT_Fieldref:
                // simplify references within this class
                CPRefInfo ref = (CPRefInfo) cpInfo;
                try {
                    if (ref.class_index == classFile.this_class)
                         cpInfo = classFile.constant_pool.get(ref.name_and_type_index);
                } catch (ConstantPool.InvalidIndex e) {
                    // ignore, for now
                }
        }
        print(tagName(tag) + " " + stringValue(cpInfo));
    }

    String cpTagName(CPInfo cpInfo) {
        String n = cpInfo.getClass().getSimpleName();
        return n.replace("CONSTANT_", "").replace("_info", "");
    }

    String tagName(int tag) {
        switch (tag) {
            case CONSTANT_Utf8:
                return "Utf8";
            case CONSTANT_Integer:
                return "int";
            case CONSTANT_Float:
                return "float";
            case CONSTANT_Long:
                return "long";
            case CONSTANT_Double:
                return "double";
            case CONSTANT_Class:
                return "class";
            case CONSTANT_String:
                return "String";
            case CONSTANT_Fieldref:
                return "Field";
            case CONSTANT_MethodHandle:
                return "MethodHandle";
            case CONSTANT_MethodType:
                return "MethodType";
            case CONSTANT_Methodref:
                return "Method";
            case CONSTANT_InterfaceMethodref:
                return "InterfaceMethod";
            case CONSTANT_InvokeDynamic:
                return "InvokeDynamic";
            case CONSTANT_Dynamic:
                return "Dynamic";
            case CONSTANT_NameAndType:
                return "NameAndType";
            default:
                return "(unknown tag " + tag + ")";
        }
    }

    String booleanValue(int constant_pool_index) {
        ClassFile classFile = classWriter.getClassFile();
        try {
            CPInfo info = classFile.constant_pool.get(constant_pool_index);
            if (info instanceof CONSTANT_Integer_info) {
                int value = ((CONSTANT_Integer_info) info).value;
               switch (value) {
                   case 0: return "false";
                   case 1: return "true";
               }
            }
            return "#" + constant_pool_index;
        } catch (ConstantPool.InvalidIndex e) {
            return report(e);
        }
    }

    String charValue(int constant_pool_index) {
        ClassFile classFile = classWriter.getClassFile();
        try {
            CPInfo info = classFile.constant_pool.get(constant_pool_index);
            if (info instanceof CONSTANT_Integer_info) {
                int value = ((CONSTANT_Integer_info) info).value;
                return String.valueOf((char) value);
            } else {
                return "#" + constant_pool_index;
            }
        } catch (ConstantPool.InvalidIndex e) {
            return report(e);
        }
    }

    String stringValue(int constant_pool_index) {
        ClassFile classFile = classWriter.getClassFile();
        try {
            return stringValue(classFile.constant_pool.get(constant_pool_index));
        } catch (ConstantPool.InvalidIndex e) {
            return report(e);
        }
    }

    String stringValue(CPInfo cpInfo) {
        return stringValueVisitor.visit(cpInfo);
    }

    StringValueVisitor stringValueVisitor = new StringValueVisitor();

    private class StringValueVisitor implements ConstantPool.Visitor<String, Void> {
        public String visit(CPInfo info) {
            return info.accept(this, null);
        }

        public String visitClass(CONSTANT_Class_info info, Void p) {
            return getCheckedName(info);
        }

        String getCheckedName(CONSTANT_Class_info info) {
            try {
                return checkName(info.getName());
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitDouble(CONSTANT_Double_info info, Void p) {
            return info.value + "d";
        }

        public String visitFieldref(CONSTANT_Fieldref_info info, Void p) {
            return visitRef(info, p);
        }

        public String visitFloat(CONSTANT_Float_info info, Void p) {
            return info.value + "f";
        }

        public String visitInteger(CONSTANT_Integer_info info, Void p) {
            return String.valueOf(info.value);
        }

        public String visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, Void p) {
            return visitRef(info, p);
        }

        public String visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, Void p) {
            try {
                String callee = stringValue(info.getNameAndTypeInfo());
                return "#" + info.bootstrap_method_attr_index + ":" + callee;
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitDynamicConstant(CONSTANT_Dynamic_info info, Void p) {
            try {
                String callee = stringValue(info.getNameAndTypeInfo());
                return "#" + info.bootstrap_method_attr_index + ":" + callee;
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitLong(CONSTANT_Long_info info, Void p) {
            return info.value + "l";
        }

        public String visitModule(CONSTANT_Module_info info, Void p) {
            try {
                return checkName(info.getName());
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitNameAndType(CONSTANT_NameAndType_info info, Void p) {
            return getCheckedName(info) + ":" + getType(info);
        }

        String getCheckedName(CONSTANT_NameAndType_info info) {
            try {
                return checkName(info.getName());
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitPackage(CONSTANT_Package_info info, Void p) {
            try {
                return checkName(info.getName());
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        String getType(CONSTANT_NameAndType_info info) {
            try {
                return info.getType();
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitMethodHandle(CONSTANT_MethodHandle_info info, Void p) {
            try {
                return info.reference_kind + " " + stringValue(info.getCPRefInfo());
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitMethodType(CONSTANT_MethodType_info info, Void p) {
            try {
                return info.getType();
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitMethodref(CONSTANT_Methodref_info info, Void p) {
            return visitRef(info, p);
        }

        public String visitString(CONSTANT_String_info info, Void p) {
            try {
                ClassFile classFile = classWriter.getClassFile();
                int string_index = info.string_index;
                return stringValue(classFile.constant_pool.getUTF8Info(string_index));
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }

        public String visitUtf8(CONSTANT_Utf8_info info, Void p) {
            String s = info.value;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\t':
                        sb.append('\\').append('t');
                        break;
                    case '\n':
                        sb.append('\\').append('n');
                        break;
                    case '\r':
                        sb.append('\\').append('r');
                        break;
                    case '\b':
                        sb.append('\\').append('b');
                        break;
                    case '\f':
                        sb.append('\\').append('f');
                        break;
                    case '\"':
                        sb.append('\\').append('\"');
                        break;
                    case '\'':
                        sb.append('\\').append('\'');
                        break;
                    case '\\':
                        sb.append('\\').append('\\');
                        break;
                    default:
                        if (Character.isISOControl(c)) {
                            sb.append(String.format("\\u%04x", (int) c));
                            break;
                        }
                        sb.append(c);
                }
            }
            return sb.toString();
        }

        String visitRef(CPRefInfo info, Void p) {
            String cn = getCheckedClassName(info);
            String nat;
            try {
                nat = stringValue(info.getNameAndTypeInfo());
            } catch (ConstantPoolException e) {
                nat = report(e);
            }
            return cn + "." + nat;
        }

        String getCheckedClassName(CPRefInfo info) {
            try {
                return checkName(info.getClassName());
            } catch (ConstantPoolException e) {
                return report(e);
            }
        }
    }

    /* If name is a valid binary name, return it; otherwise quote it. */
    private static String checkName(String name) {
        if (name == null)
            return "null";

        int len = name.length();
        if (len == 0)
            return "\"\"";

        int cc = '/';
        int cp;
        for (int k = 0; k < len; k += Character.charCount(cp)) {
            cp = name.codePointAt(k);
            if ((cc == '/' && !Character.isJavaIdentifierStart(cp))
                    || (cp != '/' && !Character.isJavaIdentifierPart(cp))) {
                return "\"" + addEscapes(name) + "\"";
            }
            cc = cp;
        }

        return name;
    }

    /* If name requires escapes, put them in, so it can be a string body. */
    private static String addEscapes(String name) {
        String esc = "\\\"\n\t";
        String rep = "\\\"nt";
        StringBuilder buf = null;
        int nextk = 0;
        int len = name.length();
        for (int k = 0; k < len; k++) {
            char cp = name.charAt(k);
            int n = esc.indexOf(cp);
            if (n >= 0) {
                if (buf == null)
                    buf = new StringBuilder(len * 2);
                if (nextk < k)
                    buf.append(name, nextk, k);
                buf.append('\\');
                buf.append(rep.charAt(n));
                nextk = k+1;
            }
        }
        if (buf == null)
            return name;
        if (nextk < len)
            buf.append(name, nextk, len);
        return buf.toString();
    }

    private final ClassWriter classWriter;
    private final Options options;
}
