/*
 * Copyright (c) 2007, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.net.URI;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.ConstantValue_attribute;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.Exceptions_attribute;
import com.sun.tools.classfile.Field;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.Module_attribute;
import com.sun.tools.classfile.Signature;
import com.sun.tools.classfile.Signature_attribute;
import com.sun.tools.classfile.SourceFile_attribute;
import com.sun.tools.classfile.Type;
import com.sun.tools.classfile.Type.ArrayType;
import com.sun.tools.classfile.Type.ClassSigType;
import com.sun.tools.classfile.Type.ClassType;
import com.sun.tools.classfile.Type.MethodType;
import com.sun.tools.classfile.Type.SimpleType;
import com.sun.tools.classfile.Type.TypeParamType;
import com.sun.tools.classfile.Type.WildcardType;

import static com.sun.tools.classfile.AccessFlags.*;
import static com.sun.tools.classfile.ConstantPool.CONSTANT_Module;
import static com.sun.tools.classfile.ConstantPool.CONSTANT_Package;

/*
 *  The main javap class to write the contents of a class file as text.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassWriter extends BasicWriter {
    static ClassWriter instance(Context context) {
        ClassWriter instance = context.get(ClassWriter.class);
        if (instance == null)
            instance = new ClassWriter(context);
        return instance;
    }

    protected ClassWriter(Context context) {
        super(context);
        context.put(ClassWriter.class, this);
        options = Options.instance(context);
        attrWriter = AttributeWriter.instance(context);
        codeWriter = CodeWriter.instance(context);
        constantWriter = ConstantWriter.instance(context);
    }

    void setDigest(String name, byte[] digest) {
        this.digestName = name;
        this.digest = digest;
    }

    void setFile(URI uri) {
        this.uri = uri;
    }

    void setFileSize(int size) {
        this.size = size;
    }

    void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    protected ClassFile getClassFile() {
        return classFile;
    }

    protected void setClassFile(ClassFile cf) {
        classFile = cf;
        constant_pool = classFile.constant_pool;
    }

    protected Method getMethod() {
        return method;
    }

    protected void setMethod(Method m) {
        method = m;
    }

    public void write(ClassFile cf) {
        setClassFile(cf);

        if (options.sysInfo || options.verbose) {
            if (uri != null) {
                if (uri.getScheme().equals("file"))
                    println("Classfile " + uri.getPath());
                else
                    println("Classfile " + uri);
            }
            indent(+1);
            if (lastModified != -1) {
                Date lm = new Date(lastModified);
                DateFormat df = DateFormat.getDateInstance();
                if (size > 0) {
                    println("Last modified " + df.format(lm) + "; size " + size + " bytes");
                } else {
                    println("Last modified " + df.format(lm));
                }
            } else if (size > 0) {
                println("Size " + size + " bytes");
            }
            if (digestName != null && digest != null) {
                StringBuilder sb = new StringBuilder();
                for (byte b: digest)
                    sb.append(String.format("%02x", b));
                println(digestName + " checksum " + sb);
            }
        }

        Attribute sfa = cf.getAttribute(Attribute.SourceFile);
        if (sfa instanceof SourceFile_attribute) {
            println("Compiled from \"" + getSourceFile((SourceFile_attribute) sfa) + "\"");
        }

        if (options.sysInfo || options.verbose) {
            indent(-1);
        }

        AccessFlags flags = cf.access_flags;
        writeModifiers(flags.getClassModifiers());

        if (classFile.access_flags.is(AccessFlags.ACC_MODULE)) {
            Attribute attr = classFile.attributes.get(Attribute.Module);
            if (attr instanceof Module_attribute) {
                Module_attribute modAttr = (Module_attribute) attr;
                String name;
                try {
                    // FIXME: compatibility code
                    if (constant_pool.get(modAttr.module_name).getTag() == CONSTANT_Module) {
                        name = getJavaName(constant_pool.getModuleInfo(modAttr.module_name).getName());
                    } else {
                        name = getJavaName(constant_pool.getUTF8Value(modAttr.module_name));
                    }
                } catch (ConstantPoolException e) {
                    name = report(e);
                }
                if ((modAttr.module_flags & Module_attribute.ACC_OPEN) != 0) {
                    print("open ");
                }
                print("module ");
                print(name);
                if (modAttr.module_version_index != 0) {
                    print("@");
                    print(getUTF8Value(modAttr.module_version_index));
                }
            } else {
                // fallback for malformed class files
                print("class ");
                print(getJavaName(classFile));
            }
        } else {
            if (classFile.isClass())
                print("class ");
            else if (classFile.isInterface())
                print("interface ");

            print(getJavaName(classFile));
        }

        Signature_attribute sigAttr = getSignature(cf.attributes);
        if (sigAttr == null) {
            // use info from class file header
            if (classFile.isClass() && classFile.super_class != 0 ) {
                String sn = getJavaSuperclassName(cf);
                if (!sn.equals("java.lang.Object")) {
                    print(" extends ");
                    print(sn);
                }
            }
            for (int i = 0; i < classFile.interfaces.length; i++) {
                print(i == 0 ? (classFile.isClass() ? " implements " : " extends ") : ",");
                print(getJavaInterfaceName(classFile, i));
            }
        } else {
            try {
                Type t = sigAttr.getParsedSignature().getType(constant_pool);
                JavaTypePrinter p = new JavaTypePrinter(classFile.isInterface());
                // The signature parser cannot disambiguate between a
                // FieldType and a ClassSignatureType that only contains a superclass type.
                if (t instanceof Type.ClassSigType) {
                    print(p.print(t));
                } else if (options.verbose || !t.isObject()) {
                    print(" extends ");
                    print(p.print(t));
                }
            } catch (ConstantPoolException e) {
                print(report(e));
            } catch (IllegalStateException e) {
                report("Invalid value for Signature attribute: " + e.getMessage());
            }
        }

        if (options.verbose) {
            println();
            indent(+1);
            println("minor version: " + cf.minor_version);
            println("major version: " + cf.major_version);
            writeList(String.format("flags: (0x%04x) ", flags.flags), flags.getClassFlags(), "\n");
            print("this_class: #" + cf.this_class);
            if (cf.this_class != 0) {
                tab();
                print("// " + constantWriter.stringValue(cf.this_class));
            }
            println();
            print("super_class: #" + cf.super_class);
            if (cf.super_class != 0) {
                tab();
                print("// " + constantWriter.stringValue(cf.super_class));
            }
            println();
            print("interfaces: " + cf.interfaces.length);
            print(", fields: " + cf.fields.length);
            print(", methods: " + cf.methods.length);
            println(", attributes: " + cf.attributes.attrs.length);
            indent(-1);
            constantWriter.writeConstantPool();
        } else {
            print(" ");
        }

        println("{");
        indent(+1);
        if (flags.is(AccessFlags.ACC_MODULE) && !options.verbose) {
            writeDirectives();
        }
        writeFields();
        writeMethods();
        indent(-1);
        println("}");

        if (options.verbose) {
            attrWriter.write(cf, cf.attributes, constant_pool);
        }
    }
    // where
        class JavaTypePrinter implements Type.Visitor<StringBuilder,StringBuilder> {
            boolean isInterface;

            JavaTypePrinter(boolean isInterface) {
                this.isInterface = isInterface;
            }

            String print(Type t) {
                return t.accept(this, new StringBuilder()).toString();
            }

            String printTypeArgs(List<? extends TypeParamType> typeParamTypes) {
                StringBuilder builder = new StringBuilder();
                appendIfNotEmpty(builder, "<", typeParamTypes, "> ");
                return builder.toString();
            }

            @Override
            public StringBuilder visitSimpleType(SimpleType type, StringBuilder sb) {
                sb.append(getJavaName(type.name));
                return sb;
            }

            @Override
            public StringBuilder visitArrayType(ArrayType type, StringBuilder sb) {
                append(sb, type.elemType);
                sb.append("[]");
                return sb;
            }

            @Override
            public StringBuilder visitMethodType(MethodType type, StringBuilder sb) {
                appendIfNotEmpty(sb, "<", type.typeParamTypes, "> ");
                append(sb, type.returnType);
                append(sb, " (", type.paramTypes, ")");
                appendIfNotEmpty(sb, " throws ", type.throwsTypes, "");
                return sb;
            }

            @Override
            public StringBuilder visitClassSigType(ClassSigType type, StringBuilder sb) {
                appendIfNotEmpty(sb, "<", type.typeParamTypes, ">");
                if (isInterface) {
                    appendIfNotEmpty(sb, " extends ", type.superinterfaceTypes, "");
                } else {
                    if (type.superclassType != null
                            && (options.verbose || !type.superclassType.isObject())) {
                        sb.append(" extends ");
                        append(sb, type.superclassType);
                    }
                    appendIfNotEmpty(sb, " implements ", type.superinterfaceTypes, "");
                }
                return sb;
            }

            @Override
            public StringBuilder visitClassType(ClassType type, StringBuilder sb) {
                if (type.outerType != null) {
                    append(sb, type.outerType);
                    sb.append(".");
                }
                sb.append(getJavaName(type.name));
                appendIfNotEmpty(sb, "<", type.typeArgs, ">");
                return sb;
            }

            @Override
            public StringBuilder visitTypeParamType(TypeParamType type, StringBuilder sb) {
                sb.append(type.name);
                String sep = " extends ";
                if (type.classBound != null
                        && (options.verbose || !type.classBound.isObject())) {
                    sb.append(sep);
                    append(sb, type.classBound);
                    sep = " & ";
                }
                if (type.interfaceBounds != null) {
                    for (Type bound: type.interfaceBounds) {
                        sb.append(sep);
                        append(sb, bound);
                        sep = " & ";
                    }
                }
                return sb;
            }

            @Override
            public StringBuilder visitWildcardType(WildcardType type, StringBuilder sb) {
                switch (type.kind) {
                    case UNBOUNDED:
                        sb.append("?");
                        break;
                    case EXTENDS:
                        sb.append("? extends ");
                        append(sb, type.boundType);
                        break;
                    case SUPER:
                        sb.append("? super ");
                        append(sb, type.boundType);
                        break;
                    default:
                        throw new AssertionError();
                }
                return sb;
            }

            private void append(StringBuilder sb, Type t) {
                t.accept(this, sb);
            }

            private void append(StringBuilder sb, String prefix, List<? extends Type> list, String suffix) {
                sb.append(prefix);
                String sep = "";
                for (Type t: list) {
                    sb.append(sep);
                    append(sb, t);
                    sep = ", ";
                }
                sb.append(suffix);
            }

            private void appendIfNotEmpty(StringBuilder sb, String prefix, List<? extends Type> list, String suffix) {
                if (!isEmpty(list))
                    append(sb, prefix, list, suffix);
            }

            private boolean isEmpty(List<? extends Type> list) {
                return (list == null || list.isEmpty());
            }
        }

    protected void writeFields() {
        for (Field f: classFile.fields) {
            writeField(f);
        }
    }

    protected void writeField(Field f) {
        if (!options.checkAccess(f.access_flags))
            return;

        AccessFlags flags = f.access_flags;
        writeModifiers(flags.getFieldModifiers());
        Signature_attribute sigAttr = getSignature(f.attributes);
        if (sigAttr == null)
            print(getJavaFieldType(f.descriptor));
        else {
            try {
                Type t = sigAttr.getParsedSignature().getType(constant_pool);
                print(getJavaName(t.toString()));
            } catch (ConstantPoolException e) {
                // report error?
                // fall back on non-generic descriptor
                print(getJavaFieldType(f.descriptor));
            }
        }
        print(" ");
        print(getFieldName(f));
        if (options.showConstants) {
            Attribute a = f.attributes.get(Attribute.ConstantValue);
            if (a instanceof ConstantValue_attribute) {
                print(" = ");
                ConstantValue_attribute cv = (ConstantValue_attribute) a;
                print(getConstantValue(f.descriptor, cv.constantvalue_index));
            }
        }
        print(";");
        println();

        indent(+1);

        boolean showBlank = false;

        if (options.showDescriptors)
            println("descriptor: " + getValue(f.descriptor));

        if (options.verbose)
            writeList(String.format("flags: (0x%04x) ", flags.flags), flags.getFieldFlags(), "\n");

        if (options.showAllAttrs) {
            for (Attribute attr: f.attributes)
                attrWriter.write(f, attr, constant_pool);
            showBlank = true;
        }

        indent(-1);

        if (showBlank || options.showDisassembled || options.showLineAndLocalVariableTables)
            println();
    }

    protected void writeMethods() {
        for (Method m: classFile.methods)
            writeMethod(m);
        setPendingNewline(false);
    }

    private static final int DEFAULT_ALLOWED_MAJOR_VERSION = 52;
    private static final int DEFAULT_ALLOWED_MINOR_VERSION = 0;

    protected void writeMethod(Method m) {
        if (!options.checkAccess(m.access_flags))
            return;

        method = m;

        AccessFlags flags = m.access_flags;

        Descriptor d;
        Type.MethodType methodType;
        List<? extends Type> methodExceptions;

        Signature_attribute sigAttr = getSignature(m.attributes);
        if (sigAttr == null) {
            d = m.descriptor;
            methodType = null;
            methodExceptions = null;
        } else {
            Signature methodSig = sigAttr.getParsedSignature();
            d = methodSig;
            try {
                methodType = (Type.MethodType) methodSig.getType(constant_pool);
                methodExceptions = methodType.throwsTypes;
                if (methodExceptions != null && methodExceptions.isEmpty())
                    methodExceptions = null;
            } catch (ConstantPoolException | IllegalStateException e) {
                // report error?
                // fall back on standard descriptor
                methodType = null;
                methodExceptions = null;
            }
        }

        Set<String> modifiers = flags.getMethodModifiers();

        String name = getName(m);
        if (classFile.isInterface() &&
                (!flags.is(AccessFlags.ACC_ABSTRACT)) && !name.equals("<clinit>")) {
            if (classFile.major_version > DEFAULT_ALLOWED_MAJOR_VERSION ||
                    (classFile.major_version == DEFAULT_ALLOWED_MAJOR_VERSION && classFile.minor_version >= DEFAULT_ALLOWED_MINOR_VERSION)) {
                if (!flags.is(AccessFlags.ACC_STATIC | AccessFlags.ACC_PRIVATE)) {
                    modifiers.add("default");
                }
            }
        }

        writeModifiers(modifiers);
        if (methodType != null) {
            print(new JavaTypePrinter(false).printTypeArgs(methodType.typeParamTypes));
        }
        switch (name) {
            case "<init>":
                print(getJavaName(classFile));
                print(getJavaParameterTypes(d, flags));
                break;
            case "<clinit>":
                print("{}");
                break;
            default:
                print(getJavaReturnType(d));
                print(" ");
                print(name);
                print(getJavaParameterTypes(d, flags));
                break;
        }

        Attribute e_attr = m.attributes.get(Attribute.Exceptions);
        if (e_attr != null) { // if there are generic exceptions, there must be erased exceptions
            if (e_attr instanceof Exceptions_attribute) {
                Exceptions_attribute exceptions = (Exceptions_attribute) e_attr;
                print(" throws ");
                if (methodExceptions != null) { // use generic list if available
                    writeList("", methodExceptions, "");
                } else {
                    for (int i = 0; i < exceptions.number_of_exceptions; i++) {
                        if (i > 0)
                            print(", ");
                        print(getJavaException(exceptions, i));
                    }
                }
            } else {
                report("Unexpected or invalid value for Exceptions attribute");
            }
        }

        println(";");

        indent(+1);

        if (options.showDescriptors) {
            println("descriptor: " + getValue(m.descriptor));
        }

        if (options.verbose) {
            writeList(String.format("flags: (0x%04x) ", flags.flags), flags.getMethodFlags(), "\n");
        }

        Code_attribute code = null;
        Attribute c_attr = m.attributes.get(Attribute.Code);
        if (c_attr != null) {
            if (c_attr instanceof Code_attribute)
                code = (Code_attribute) c_attr;
            else
                report("Unexpected or invalid value for Code attribute");
        }

        if (options.showAllAttrs) {
            Attribute[] attrs = m.attributes.attrs;
            for (Attribute attr: attrs)
                attrWriter.write(m, attr, constant_pool);
        } else if (code != null) {
            if (options.showDisassembled) {
                println("Code:");
                codeWriter.writeInstrs(code);
                codeWriter.writeExceptionTable(code);
            }

            if (options.showLineAndLocalVariableTables) {
                attrWriter.write(code, code.attributes.get(Attribute.LineNumberTable), constant_pool);
                attrWriter.write(code, code.attributes.get(Attribute.LocalVariableTable), constant_pool);
            }
        }

        indent(-1);

        // set pendingNewline to write a newline before the next method (if any)
        // if a separator is desired
        setPendingNewline(
                options.showDisassembled ||
                options.showAllAttrs ||
                options.showDescriptors ||
                options.showLineAndLocalVariableTables ||
                options.verbose);
    }

    void writeModifiers(Collection<String> items) {
        for (Object item: items) {
            print(item);
            print(" ");
        }
    }

    void writeDirectives() {
        Attribute attr = classFile.attributes.get(Attribute.Module);
        if (!(attr instanceof Module_attribute))
            return;

        Module_attribute m = (Module_attribute) attr;
        for (Module_attribute.RequiresEntry entry: m.requires) {
            print("requires");
            if ((entry.requires_flags & Module_attribute.ACC_STATIC_PHASE) != 0)
                print(" static");
            if ((entry.requires_flags & Module_attribute.ACC_TRANSITIVE) != 0)
                print(" transitive");
            print(" ");
            String mname;
            try {
                mname = getModuleName(entry.requires_index);
            } catch (ConstantPoolException e) {
                mname = report(e);
            }
            print(mname);
            println(";");
        }

        for (Module_attribute.ExportsEntry entry: m.exports) {
            print("exports");
            print(" ");
            String pname;
            try {
                pname = getPackageName(entry.exports_index).replace('/', '.');
            } catch (ConstantPoolException e) {
                pname = report(e);
            }
            print(pname);
            boolean first = true;
            for (int i: entry.exports_to_index) {
                String mname;
                try {
                    mname = getModuleName(i);
                } catch (ConstantPoolException e) {
                    mname = report(e);
                }
                if (first) {
                    println(" to");
                    indent(+1);
                    first = false;
                } else {
                    println(",");
                }
                print(mname);
            }
            println(";");
            if (!first)
                indent(-1);
        }

        for (Module_attribute.OpensEntry entry: m.opens) {
            print("opens");
            print(" ");
            String pname;
            try {
                pname = getPackageName(entry.opens_index).replace('/', '.');
            } catch (ConstantPoolException e) {
                pname = report(e);
            }
            print(pname);
            boolean first = true;
            for (int i: entry.opens_to_index) {
                String mname;
                try {
                    mname = getModuleName(i);
                } catch (ConstantPoolException e) {
                    mname = report(e);
                }
                if (first) {
                    println(" to");
                    indent(+1);
                    first = false;
                } else {
                    println(",");
                }
                print(mname);
            }
            println(";");
            if (!first)
                indent(-1);
        }

        for (int entry: m.uses_index) {
            print("uses ");
            print(getClassName(entry).replace('/', '.'));
            println(";");
        }

        for (Module_attribute.ProvidesEntry entry: m.provides) {
            print("provides  ");
            print(getClassName(entry.provides_index).replace('/', '.'));
            boolean first = true;
            for (int i: entry.with_index) {
                if (first) {
                    println(" with");
                    indent(+1);
                    first = false;
                } else {
                    println(",");
                }
                print(getClassName(i).replace('/', '.'));
            }
            println(";");
            if (!first)
                indent(-1);
        }
    }

    String getModuleName(int index) throws ConstantPoolException {
        if (constant_pool.get(index).getTag() == CONSTANT_Module) {
            return constant_pool.getModuleInfo(index).getName();
        } else {
            return constant_pool.getUTF8Value(index);
        }
    }

    String getPackageName(int index) throws ConstantPoolException {
        if (constant_pool.get(index).getTag() == CONSTANT_Package) {
            return constant_pool.getPackageInfo(index).getName();
        } else {
            return constant_pool.getUTF8Value(index);
        }
    }

    String getUTF8Value(int index) {
        try {
            return classFile.constant_pool.getUTF8Value(index);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getClassName(int index) {
        try {
            return classFile.constant_pool.getClassInfo(index).getName();
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    void writeList(String prefix, Collection<?> items, String suffix) {
        print(prefix);
        String sep = "";
        for (Object item: items) {
            print(sep);
            print(item);
            sep = ", ";
        }
        print(suffix);
    }

    void writeListIfNotEmpty(String prefix, List<?> items, String suffix) {
        if (items != null && items.size() > 0)
            writeList(prefix, items, suffix);
    }

    Signature_attribute getSignature(Attributes attributes) {
        return (Signature_attribute) attributes.get(Attribute.Signature);
    }

    String adjustVarargs(AccessFlags flags, String params) {
        if (flags.is(ACC_VARARGS)) {
            int i = params.lastIndexOf("[]");
            if (i > 0)
                return params.substring(0, i) + "..." + params.substring(i+2);
        }

        return params;
    }

    String getJavaName(ClassFile cf) {
        try {
            return getJavaName(cf.getName());
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getJavaSuperclassName(ClassFile cf) {
        try {
            return getJavaName(cf.getSuperclassName());
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getJavaInterfaceName(ClassFile cf, int index) {
        try {
            return getJavaName(cf.getInterfaceName(index));
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getJavaFieldType(Descriptor d) {
        try {
            return getJavaName(d.getFieldType(constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        } catch (InvalidDescriptor e) {
            return report(e);
        }
    }

    String getJavaReturnType(Descriptor d) {
        try {
            return getJavaName(d.getReturnType(constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        } catch (InvalidDescriptor e) {
            return report(e);
        }
    }

    String getJavaParameterTypes(Descriptor d, AccessFlags flags) {
        try {
            return getJavaName(adjustVarargs(flags, d.getParameterTypes(constant_pool)));
        } catch (ConstantPoolException e) {
            return report(e);
        } catch (InvalidDescriptor e) {
            return report(e);
        }
    }

    String getJavaException(Exceptions_attribute attr, int index) {
        try {
            return getJavaName(attr.getException(index, constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getValue(Descriptor d) {
        try {
            return d.getValue(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getFieldName(Field f) {
        try {
            return f.getName(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    String getName(Method m) {
        try {
            return m.getName(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    static String getJavaName(String name) {
        return name.replace('/', '.');
    }

    String getSourceFile(SourceFile_attribute attr) {
        try {
            return attr.getSourceFile(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    /**
     * Get the value of an entry in the constant pool as a Java constant.
     * Characters and booleans are represented by CONSTANT_Intgere entries.
     * Character and string values are processed to escape characters outside
     * the basic printable ASCII set.
     * @param d the descriptor, giving the expected type of the constant
     * @param index the index of the value in the constant pool
     * @return a printable string containing the value of the constant.
     */
    String getConstantValue(Descriptor d, int index) {
        try {
            ConstantPool.CPInfo cpInfo = constant_pool.get(index);

            switch (cpInfo.getTag()) {
                case ConstantPool.CONSTANT_Integer: {
                    ConstantPool.CONSTANT_Integer_info info =
                            (ConstantPool.CONSTANT_Integer_info) cpInfo;
                    String t = d.getValue(constant_pool);
                    switch (t) {
                        case "C":
                            // character
                            return getConstantCharValue((char) info.value);
                        case "Z":
                            // boolean
                            return String.valueOf(info.value == 1);
                        default:
                            // other: assume integer
                            return String.valueOf(info.value);
                    }
                }

                case ConstantPool.CONSTANT_String: {
                    ConstantPool.CONSTANT_String_info info =
                            (ConstantPool.CONSTANT_String_info) cpInfo;
                    return getConstantStringValue(info.getString());
                }

                default:
                    return constantWriter.stringValue(cpInfo);
            }
        } catch (ConstantPoolException e) {
            return "#" + index;
        }
    }

    private String getConstantCharValue(char c) {
        StringBuilder sb = new StringBuilder();
        sb.append('\'');
        sb.append(esc(c, '\''));
        sb.append('\'');
        return sb.toString();
    }

    private String getConstantStringValue(String s) {
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        for (int i = 0; i < s.length(); i++) {
            sb.append(esc(s.charAt(i), '"'));
        }
        sb.append("\"");
        return sb.toString();
    }

    private String esc(char c, char quote) {
        if (32 <= c && c <= 126 && c != quote && c != '\\')
            return String.valueOf(c);
        else switch (c) {
            case '\b': return "\\b";
            case '\n': return "\\n";
            case '\t': return "\\t";
            case '\f': return "\\f";
            case '\r': return "\\r";
            case '\\': return "\\\\";
            case '\'': return "\\'";
            case '\"': return "\\\"";
            default:   return String.format("\\u%04x", (int) c);
        }
    }

    private final Options options;
    private final AttributeWriter attrWriter;
    private final CodeWriter codeWriter;
    private final ConstantWriter constantWriter;
    private ClassFile classFile;
    private URI uri;
    private long lastModified;
    private String digestName;
    private byte[] digest;
    private int size;
    private ConstantPool constant_pool;
    private Method method;
}
