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

import java.util.Collection;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.AnnotationDefault_attribute;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.Attributes;
import com.sun.tools.classfile.BootstrapMethods_attribute;
import com.sun.tools.classfile.CharacterRangeTable_attribute;
import com.sun.tools.classfile.CharacterRangeTable_attribute.Entry;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.CompilationID_attribute;
import com.sun.tools.classfile.ConstantPool;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Class_info;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.ConstantValue_attribute;
import com.sun.tools.classfile.DefaultAttribute;
import com.sun.tools.classfile.Deprecated_attribute;
import com.sun.tools.classfile.Descriptor;
import com.sun.tools.classfile.Descriptor.InvalidDescriptor;
import com.sun.tools.classfile.EnclosingMethod_attribute;
import com.sun.tools.classfile.Exceptions_attribute;
import com.sun.tools.classfile.InnerClasses_attribute;
import com.sun.tools.classfile.InnerClasses_attribute.Info;
import com.sun.tools.classfile.LineNumberTable_attribute;
import com.sun.tools.classfile.LocalVariableTable_attribute;
import com.sun.tools.classfile.LocalVariableTypeTable_attribute;
import com.sun.tools.classfile.MethodParameters_attribute;
import com.sun.tools.classfile.Module_attribute;
import com.sun.tools.classfile.ModuleHashes_attribute;
import com.sun.tools.classfile.ModuleMainClass_attribute;
import com.sun.tools.classfile.ModulePackages_attribute;
import com.sun.tools.classfile.ModuleResolution_attribute;
import com.sun.tools.classfile.ModuleTarget_attribute;
import com.sun.tools.classfile.NestHost_attribute;
import com.sun.tools.classfile.NestMembers_attribute;
import com.sun.tools.classfile.Record_attribute;
import com.sun.tools.classfile.RuntimeInvisibleAnnotations_attribute;
import com.sun.tools.classfile.RuntimeInvisibleParameterAnnotations_attribute;
import com.sun.tools.classfile.RuntimeInvisibleTypeAnnotations_attribute;
import com.sun.tools.classfile.RuntimeParameterAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleParameterAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleTypeAnnotations_attribute;
import com.sun.tools.classfile.PermittedSubclasses_attribute;
import com.sun.tools.classfile.Signature_attribute;
import com.sun.tools.classfile.SourceDebugExtension_attribute;
import com.sun.tools.classfile.SourceFile_attribute;
import com.sun.tools.classfile.SourceID_attribute;
import com.sun.tools.classfile.StackMapTable_attribute;
import com.sun.tools.classfile.StackMap_attribute;
import com.sun.tools.classfile.Synthetic_attribute;
import com.sun.tools.classfile.Type;

import static com.sun.tools.classfile.AccessFlags.*;

import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.StringUtils;

/*
 *  A writer for writing Attributes as text.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class AttributeWriter extends BasicWriter
        implements Attribute.Visitor<Void,Void>
{
    public static AttributeWriter instance(Context context) {
        AttributeWriter instance = context.get(AttributeWriter.class);
        if (instance == null)
            instance = new AttributeWriter(context);
        return instance;
    }

    protected AttributeWriter(Context context) {
        super(context);
        context.put(AttributeWriter.class, this);
        annotationWriter = AnnotationWriter.instance(context);
        codeWriter = CodeWriter.instance(context);
        constantWriter = ConstantWriter.instance(context);
        options = Options.instance(context);
    }

    public void write(Object owner, Attribute attr, ConstantPool constant_pool) {
        if (attr != null) {
            Assert.checkNonNull(constant_pool);
            Assert.checkNonNull(owner);
            this.constant_pool = constant_pool;
            this.owner = owner;
            attr.accept(this, null);
        }
    }

    public void write(Object owner, Attributes attrs, ConstantPool constant_pool) {
        if (attrs != null) {
            Assert.checkNonNull(constant_pool);
            Assert.checkNonNull(owner);
            this.constant_pool = constant_pool;
            this.owner = owner;
            for (Attribute attr: attrs)
                attr.accept(this, null);
        }
    }

    @Override
    public Void visitDefault(DefaultAttribute attr, Void ignore) {
        byte[] data = attr.info;
        int i = 0;
        int j = 0;
        print("  ");
        try {
            print(attr.getName(constant_pool));
        } catch (ConstantPoolException e) {
            report(e);
            print("attribute name = #" + attr.attribute_name_index);
        }
        print(": ");
        print("length = 0x" + toHex(attr.info.length));
        if (attr.reason != null) {
            print(" (" + attr.reason + ")");
        }
        println();

        print("   ");

        while (i < data.length) {
            print(toHex(data[i], 2));

            j++;
            if (j == 16) {
                println();
                print("   ");
                j = 0;
            } else {
                print(" ");
            }
            i++;
        }
        println();
        return null;
    }

    @Override
    public Void visitAnnotationDefault(AnnotationDefault_attribute attr, Void ignore) {
        println("AnnotationDefault:");
        indent(+1);
        print("default_value: ");
        annotationWriter.write(attr.default_value);
        indent(-1);
        println();
        return null;
    }

    @Override
    public Void visitBootstrapMethods(BootstrapMethods_attribute attr, Void p) {
        println(Attribute.BootstrapMethods + ":");
        for (int i = 0; i < attr.bootstrap_method_specifiers.length ; i++) {
            BootstrapMethods_attribute.BootstrapMethodSpecifier bsm = attr.bootstrap_method_specifiers[i];
            indent(+1);
            print(i + ": #" + bsm.bootstrap_method_ref + " ");
            println(constantWriter.stringValue(bsm.bootstrap_method_ref));
            indent(+1);
            println("Method arguments:");
            indent(+1);
            for (int j = 0; j < bsm.bootstrap_arguments.length; j++) {
                print("#" + bsm.bootstrap_arguments[j] + " ");
                println(constantWriter.stringValue(bsm.bootstrap_arguments[j]));
            }
            indent(-3);
        }
        return null;
    }

    @Override
    public Void visitCharacterRangeTable(CharacterRangeTable_attribute attr, Void ignore) {
        println("CharacterRangeTable:");
        indent(+1);
        for (Entry e : attr.character_range_table) {
            print(String.format("    %2d, %2d, %6x, %6x, %4x",
                    e.start_pc, e.end_pc,
                    e.character_range_start, e.character_range_end,
                    e.flags));
            tab();
            print(String.format("// %2d, %2d, %4d:%02d, %4d:%02d",
                    e.start_pc, e.end_pc,
                    (e.character_range_start >> 10), (e.character_range_start & 0x3ff),
                    (e.character_range_end >> 10), (e.character_range_end & 0x3ff)));
            if ((e.flags & CharacterRangeTable_attribute.CRT_STATEMENT) != 0)
                print(", statement");
            if ((e.flags & CharacterRangeTable_attribute.CRT_BLOCK) != 0)
                print(", block");
            if ((e.flags & CharacterRangeTable_attribute.CRT_ASSIGNMENT) != 0)
                print(", assignment");
            if ((e.flags & CharacterRangeTable_attribute.CRT_FLOW_CONTROLLER) != 0)
                print(", flow-controller");
            if ((e.flags & CharacterRangeTable_attribute.CRT_FLOW_TARGET) != 0)
                print(", flow-target");
            if ((e.flags & CharacterRangeTable_attribute.CRT_INVOKE) != 0)
                print(", invoke");
            if ((e.flags & CharacterRangeTable_attribute.CRT_CREATE) != 0)
                print(", create");
            if ((e.flags & CharacterRangeTable_attribute.CRT_BRANCH_TRUE) != 0)
                print(", branch-true");
            if ((e.flags & CharacterRangeTable_attribute.CRT_BRANCH_FALSE) != 0)
                print(", branch-false");
            println();
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitCode(Code_attribute attr, Void ignore) {
        codeWriter.write(attr, constant_pool);
        return null;
    }

    @Override
    public Void visitCompilationID(CompilationID_attribute attr, Void ignore) {
        constantWriter.write(attr.compilationID_index);
        return null;
    }

    @Override
    public Void visitConstantValue(ConstantValue_attribute attr, Void ignore) {
        print("ConstantValue: ");
        constantWriter.write(attr.constantvalue_index);
        println();
        return null;
    }

    @Override
    public Void visitDeprecated(Deprecated_attribute attr, Void ignore) {
        println("Deprecated: true");
        return null;
    }

    @Override
    public Void visitEnclosingMethod(EnclosingMethod_attribute attr, Void ignore) {
        print("EnclosingMethod: #" + attr.class_index + ".#" + attr.method_index);
        tab();
        print("// " + getJavaClassName(attr));
        if (attr.method_index != 0)
            print("." + getMethodName(attr));
        println();
        return null;
    }

    private String getJavaClassName(EnclosingMethod_attribute a) {
        try {
            return getJavaName(a.getClassName(constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    private String getMethodName(EnclosingMethod_attribute a) {
        try {
            return a.getMethodName(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    @Override
    public Void visitExceptions(Exceptions_attribute attr, Void ignore) {
        println("Exceptions:");
        indent(+1);
        print("throws ");
        for (int i = 0; i < attr.number_of_exceptions; i++) {
            if (i > 0)
                print(", ");
            print(getJavaException(attr, i));
        }
        println();
        indent(-1);
        return null;
    }

    private String getJavaException(Exceptions_attribute attr, int index) {
        try {
            return getJavaName(attr.getException(index, constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }


    @Override
    public Void visitInnerClasses(InnerClasses_attribute attr, Void ignore) {
        boolean first = true;
        for (Info info : attr.classes) {
            //access
            AccessFlags access_flags = info.inner_class_access_flags;
            if (options.checkAccess(access_flags)) {
                if (first) {
                    writeInnerClassHeader();
                    first = false;
                }
                for (String name: access_flags.getInnerClassModifiers())
                    print(name + " ");
                if (info.inner_name_index != 0) {
                    print("#" + info.inner_name_index + "= ");
                }
                print("#" + info.inner_class_info_index);
                if (info.outer_class_info_index != 0) {
                    print(" of #" + info.outer_class_info_index);
                }
                print(";");
                tab();
                print("// ");
                if (info.inner_name_index != 0) {
                    print(getInnerName(constant_pool, info) + "=");
                }
                constantWriter.write(info.inner_class_info_index);
                if (info.outer_class_info_index != 0) {
                    print(" of ");
                    constantWriter.write(info.outer_class_info_index);
                }
                println();
            }
        }
        if (!first)
            indent(-1);
        return null;
    }

    String getInnerName(ConstantPool constant_pool, InnerClasses_attribute.Info info) {
        try {
            return info.getInnerName(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    private void writeInnerClassHeader() {
        println("InnerClasses:");
        indent(+1);
    }

    @Override
    public Void visitLineNumberTable(LineNumberTable_attribute attr, Void ignore) {
        println("LineNumberTable:");
        indent(+1);
        for (LineNumberTable_attribute.Entry entry: attr.line_number_table) {
            println("line " + entry.line_number + ": " + entry.start_pc);
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitLocalVariableTable(LocalVariableTable_attribute attr, Void ignore) {
        println("LocalVariableTable:");
        indent(+1);
        println("Start  Length  Slot  Name   Signature");
        for (LocalVariableTable_attribute.Entry entry : attr.local_variable_table) {
            println(String.format("%5d %7d %5d %5s   %s",
                    entry.start_pc, entry.length, entry.index,
                    constantWriter.stringValue(entry.name_index),
                    constantWriter.stringValue(entry.descriptor_index)));
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitLocalVariableTypeTable(LocalVariableTypeTable_attribute attr, Void ignore) {
        println("LocalVariableTypeTable:");
        indent(+1);
        println("Start  Length  Slot  Name   Signature");
        for (LocalVariableTypeTable_attribute.Entry entry : attr.local_variable_table) {
            println(String.format("%5d %7d %5d %5s   %s",
                    entry.start_pc, entry.length, entry.index,
                    constantWriter.stringValue(entry.name_index),
                    constantWriter.stringValue(entry.signature_index)));
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitNestHost(NestHost_attribute attr, Void aVoid) {
        print("NestHost: ");
        constantWriter.write(attr.top_index);
        println();
        return null;
    }

    private String getJavaClassName(ModuleMainClass_attribute a) {
        try {
            return getJavaName(a.getMainClassName(constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    private static final String format = "%-31s%s";

    @Override
    public Void visitMethodParameters(MethodParameters_attribute attr,
                                      Void ignore) {
        final String header = String.format(format, "Name", "Flags");
        println("MethodParameters:");
        indent(+1);
        println(header);
        for (MethodParameters_attribute.Entry entry :
                 attr.method_parameter_table) {
            String namestr =
                entry.name_index != 0 ?
                constantWriter.stringValue(entry.name_index) : "<no name>";
            String flagstr =
                (0 != (entry.flags & ACC_FINAL) ? "final " : "") +
                (0 != (entry.flags & ACC_MANDATED) ? "mandated " : "") +
                (0 != (entry.flags & ACC_SYNTHETIC) ? "synthetic" : "");
            println(String.format(format, namestr, flagstr));
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitModule(Module_attribute attr, Void ignore) {
        println("Module:");
        indent(+1);

        print("#" + attr.module_name);
        print(",");
        print(String.format("%x", attr.module_flags));
        tab();
        print("// " + constantWriter.stringValue(attr.module_name));
        if ((attr.module_flags & Module_attribute.ACC_OPEN) != 0)
            print(" ACC_OPEN");
        if ((attr.module_flags & Module_attribute.ACC_MANDATED) != 0)
            print(" ACC_MANDATED");
        if ((attr.module_flags & Module_attribute.ACC_SYNTHETIC) != 0)
            print(" ACC_SYNTHETIC");
        println();
        print("#" + attr.module_version_index);
        if (attr.module_version_index != 0) {
            tab();
            print("// " + constantWriter.stringValue(attr.module_version_index));
        }
        println();

        printRequiresTable(attr);
        printExportsTable(attr);
        printOpensTable(attr);
        printUsesTable(attr);
        printProvidesTable(attr);
        indent(-1);
        return null;
    }

    protected void printRequiresTable(Module_attribute attr) {
        Module_attribute.RequiresEntry[] entries = attr.requires;
        print(entries.length);
        tab();
        println("// " + "requires");
        indent(+1);
        for (Module_attribute.RequiresEntry e: entries) {
            print("#" + e.requires_index + "," + String.format("%x", e.requires_flags));
            tab();
            print("// " + constantWriter.stringValue(e.requires_index));
            if ((e.requires_flags & Module_attribute.ACC_TRANSITIVE) != 0)
                print(" ACC_TRANSITIVE");
            if ((e.requires_flags & Module_attribute.ACC_STATIC_PHASE) != 0)
                print(" ACC_STATIC_PHASE");
            if ((e.requires_flags & Module_attribute.ACC_SYNTHETIC) != 0)
                print(" ACC_SYNTHETIC");
            if ((e.requires_flags & Module_attribute.ACC_MANDATED) != 0)
                print(" ACC_MANDATED");
            println();
            print("#" + e.requires_version_index);
            if (e.requires_version_index != 0) {
                tab();
                print("// " + constantWriter.stringValue(e.requires_version_index));
            }
            println();
        }
        indent(-1);
    }

    protected void printExportsTable(Module_attribute attr) {
        Module_attribute.ExportsEntry[] entries = attr.exports;
        print(entries.length);
        tab();
        println("// exports");
        indent(+1);
        for (Module_attribute.ExportsEntry e: entries) {
            printExportOpenEntry(e.exports_index, e.exports_flags, e.exports_to_index);
        }
        indent(-1);
    }

    protected void printOpensTable(Module_attribute attr) {
        Module_attribute.OpensEntry[] entries = attr.opens;
        print(entries.length);
        tab();
        println("// opens");
        indent(+1);
        for (Module_attribute.OpensEntry e: entries) {
            printExportOpenEntry(e.opens_index, e.opens_flags, e.opens_to_index);
        }
        indent(-1);
    }

    protected void printExportOpenEntry(int index, int flags, int[] to_index) {
        print("#" + index + "," + String.format("%x", flags));
        tab();
        print("// ");
        print(constantWriter.stringValue(index));
        if ((flags & Module_attribute.ACC_MANDATED) != 0)
            print(" ACC_MANDATED");
        if ((flags & Module_attribute.ACC_SYNTHETIC) != 0)
            print(" ACC_SYNTHETIC");
        if (to_index.length == 0) {
            println();
        } else {
            println(" to ... " + to_index.length);
            indent(+1);
            for (int to: to_index) {
                print("#" + to);
                tab();
                println("// ... to " + constantWriter.stringValue(to));
            }
            indent(-1);
        }
    }

    protected void printUsesTable(Module_attribute attr) {
        int[] entries = attr.uses_index;
        print(entries.length);
        tab();
        println("// " + "uses");
        indent(+1);
        for (int e: entries) {
            print("#" + e);
            tab();
            println("// " + constantWriter.stringValue(e));
        }
        indent(-1);
    }

    protected void printProvidesTable(Module_attribute attr) {
        Module_attribute.ProvidesEntry[] entries = attr.provides;
        print(entries.length);
        tab();
        println("// " + "provides");
        indent(+1);
        for (Module_attribute.ProvidesEntry e: entries) {
            print("#" + e.provides_index);
            tab();
            print("// ");
            print(constantWriter.stringValue(e.provides_index));
            println(" with ... " + e.with_count);
            indent(+1);
            for (int with : e.with_index) {
                print("#" + with);
                tab();
                println("// ... with " + constantWriter.stringValue(with));
            }
            indent(-1);
        }
        indent(-1);
    }

    @Override
    public Void visitModuleHashes(ModuleHashes_attribute attr, Void ignore) {
        println("ModuleHashes:");
        indent(+1);
        print("algorithm: #" + attr.algorithm_index);
        tab();
        println("// " + getAlgorithm(attr));
        print(attr.hashes_table_length);
        tab();
        println("// hashes");
        for (ModuleHashes_attribute.Entry e : attr.hashes_table) {
            print("#" + e.module_name_index);
            tab();
            println("// " + getModuleName(e));
            println("hash_length: " + e.hash.length);
            println("hash: [" + toHex(e.hash) + "]");
        }
        indent(-1);
        return null;
    }

    private String getAlgorithm(ModuleHashes_attribute attr) {
        try {
            return constant_pool.getUTF8Value(attr.algorithm_index);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    private String getModuleName(ModuleHashes_attribute.Entry entry) {
        try {
            int utf8Index = constant_pool.getModuleInfo(entry.module_name_index).name_index;
            return constant_pool.getUTF8Value(utf8Index);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    @Override
    public Void visitModuleMainClass(ModuleMainClass_attribute attr, Void ignore) {
        print("ModuleMainClass: #" + attr.main_class_index);
        tab();
        print("// " + getJavaClassName(attr));
        println();
        return null;
    }

    @Override
    public Void visitModulePackages(ModulePackages_attribute attr, Void ignore) {
        println("ModulePackages: ");
        indent(+1);
        for (int i = 0; i < attr.packages_count; i++) {
            print("#" + attr.packages_index[i]);
            tab();
            println("// " + getJavaPackage(attr, i));
        }
        indent(-1);
        return null;
    }

    private String getJavaPackage(ModulePackages_attribute attr, int index) {
        try {
            return getJavaName(attr.getPackage(index, constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    @Override
    public Void visitModuleResolution(ModuleResolution_attribute attr, Void ignore) {
        println("ModuleResolution:");
        indent(+1);
        print(String.format("%x", attr.resolution_flags));
        tab();
        print("// ");
        int flags = attr.resolution_flags;
        if ((flags & ModuleResolution_attribute.DO_NOT_RESOLVE_BY_DEFAULT) != 0)
            print(" DO_NOT_RESOLVE_BY_DEFAULT");
        if ((flags & ModuleResolution_attribute.WARN_DEPRECATED) != 0)
            print(" WARN_DEPRECATED");
        if ((flags & ModuleResolution_attribute.WARN_DEPRECATED_FOR_REMOVAL) != 0)
            print(" WARN_DEPRECATED_FOR_REMOVAL");
        if ((flags & ModuleResolution_attribute.WARN_INCUBATING) != 0)
            print(" WARN_INCUBATING");
        println();
        indent(-1);
        return null;
    }

    @Override
    public Void visitModuleTarget(ModuleTarget_attribute attr, Void ignore) {
        println("ModuleTarget:");
        indent(+1);
        print("target_platform: #" + attr.target_platform_index);
        if (attr.target_platform_index != 0) {
            tab();
            print("// " + getTargetPlatform(attr));
        }
        println();
        indent(-1);
        return null;
    }

    private String getTargetPlatform(ModuleTarget_attribute attr) {
        try {
            return constant_pool.getUTF8Value(attr.target_platform_index);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    @Override
    public Void visitNestMembers(NestMembers_attribute attr, Void aVoid) {
        println("NestMembers:");
        indent(+1);
        try {
            CONSTANT_Class_info[] children = attr.getChildren(constant_pool);
            for (int i = 0; i < attr.members_indexes.length; i++) {
                println(constantWriter.stringValue(children[i]));
            }
            indent(-1);
        } catch (ConstantPoolException ex) {
            throw new AssertionError(ex);
        }
        return null;
    }

    @Override
    public Void visitRecord(Record_attribute attr, Void p) {
        println("Record:");
        indent(+1);
        for (Record_attribute.ComponentInfo componentInfo : attr.component_info_arr) {
            Signature_attribute sigAttr = (Signature_attribute) componentInfo.attributes.get(Attribute.Signature);

            if (sigAttr == null)
                print(getJavaFieldType(componentInfo.descriptor));
            else {
                try {
                    Type t = sigAttr.getParsedSignature().getType(constant_pool);
                    print(getJavaName(t.toString()));
                } catch (ConstantPoolException e) {
                    // report error?
                    // fall back on non-generic descriptor
                    print(getJavaFieldType(componentInfo.descriptor));
                }
            }

            print(" ");
            try {
                print(componentInfo.getName(constant_pool));
            } catch (ConstantPoolException e) {
                report(e);
                return null;
            }
            print(";");
            println();
            indent(+1);
            if (options.showDescriptors) {
                println("descriptor: " + getValue(componentInfo.descriptor));
            }
            if (options.showAllAttrs) {
                for (Attribute componentAttr: componentInfo.attributes)
                    write(componentInfo, componentAttr, constant_pool);
                println();
            }
            indent(-1);
        }
        indent(-1);
        return null;
    }

    String getValue(Descriptor d) {
        try {
            return d.getValue(constant_pool);
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

    String getJavaFieldType(Descriptor d) {
        try {
            return getJavaName(d.getFieldType(constant_pool));
        } catch (ConstantPoolException e) {
            return report(e);
        } catch (InvalidDescriptor e) {
            return report(e);
        }
    }

    void writeModifiers(Collection<String> items) {
        for (Object item: items) {
            print(item);
            print(" ");
        }
    }

    @Override
    public Void visitRuntimeVisibleAnnotations(RuntimeVisibleAnnotations_attribute attr, Void ignore) {
        println("RuntimeVisibleAnnotations:");
        indent(+1);
        for (int i = 0; i < attr.annotations.length; i++) {
            print(i + ": ");
            annotationWriter.write(attr.annotations[i]);
            println();
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitRuntimeInvisibleAnnotations(RuntimeInvisibleAnnotations_attribute attr, Void ignore) {
        println("RuntimeInvisibleAnnotations:");
        indent(+1);
        for (int i = 0; i < attr.annotations.length; i++) {
            print(i + ": ");
            annotationWriter.write(attr.annotations[i]);
            println();
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitRuntimeVisibleTypeAnnotations(RuntimeVisibleTypeAnnotations_attribute attr, Void ignore) {
        println("RuntimeVisibleTypeAnnotations:");
        indent(+1);
        for (int i = 0; i < attr.annotations.length; i++) {
            print(i + ": ");
            annotationWriter.write(attr.annotations[i]);
            println();
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitRuntimeInvisibleTypeAnnotations(RuntimeInvisibleTypeAnnotations_attribute attr, Void ignore) {
        println("RuntimeInvisibleTypeAnnotations:");
        indent(+1);
        for (int i = 0; i < attr.annotations.length; i++) {
            print(i + ": ");
            annotationWriter.write(attr.annotations[i]);
            println();
        }
        indent(-1);
        return null;
    }

    private void visitParameterAnnotations(String message, RuntimeParameterAnnotations_attribute attr) {
        println(message);
        indent(+1);
        for (int param = 0; param < attr.parameter_annotations.length; param++) {
            println("parameter " + param + ": ");
            indent(+1);
            for (int i = 0; i < attr.parameter_annotations[param].length; i++) {
                print(i + ": ");
                annotationWriter.write(attr.parameter_annotations[param][i]);
                println();
            }
            indent(-1);
        }
        indent(-1);
    }

    @Override
    public Void visitRuntimeVisibleParameterAnnotations(RuntimeVisibleParameterAnnotations_attribute attr, Void ignore) {
        visitParameterAnnotations("RuntimeVisibleParameterAnnotations:", (RuntimeParameterAnnotations_attribute) attr);
        return null;
    }

    @Override
    public Void visitRuntimeInvisibleParameterAnnotations(RuntimeInvisibleParameterAnnotations_attribute attr, Void ignore) {
        visitParameterAnnotations("RuntimeInvisibleParameterAnnotations:", (RuntimeParameterAnnotations_attribute) attr);
        return null;
    }

    @Override
    public Void visitPermittedSubclasses(PermittedSubclasses_attribute attr, Void ignore) {
        println("PermittedSubclasses:");
        indent(+1);
        try {
            CONSTANT_Class_info[] subtypes = attr.getSubtypes(constant_pool);
            for (int i = 0; i < subtypes.length; i++) {
                println(constantWriter.stringValue(subtypes[i]));
            }
            indent(-1);
        } catch (ConstantPoolException ex) {
            throw new AssertionError(ex);
        }
        return null;
    }

    @Override
    public Void visitSignature(Signature_attribute attr, Void ignore) {
        print("Signature: #" + attr.signature_index);
        tab();
        println("// " + getSignature(attr));
        return null;
    }

    String getSignature(Signature_attribute info) {
        try {
            return info.getSignature(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    @Override
    public Void visitSourceDebugExtension(SourceDebugExtension_attribute attr, Void ignore) {
        println("SourceDebugExtension:");
        indent(+1);
        for (String s: attr.getValue().split("[\r\n]+")) {
            println(s);
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitSourceFile(SourceFile_attribute attr, Void ignore) {
        println("SourceFile: \"" + getSourceFile(attr) + "\"");
        return null;
    }

    private String getSourceFile(SourceFile_attribute attr) {
        try {
            return attr.getSourceFile(constant_pool);
        } catch (ConstantPoolException e) {
            return report(e);
        }
    }

    @Override
    public Void visitSourceID(SourceID_attribute attr, Void ignore) {
        constantWriter.write(attr.sourceID_index);
        return null;
    }

    @Override
    public Void visitStackMap(StackMap_attribute attr, Void ignore) {
        println("StackMap: number_of_entries = " + attr.number_of_entries);
        indent(+1);
        StackMapTableWriter w = new StackMapTableWriter();
        for (StackMapTable_attribute.stack_map_frame entry : attr.entries) {
            w.write(entry);
        }
        indent(-1);
        return null;
    }

    @Override
    public Void visitStackMapTable(StackMapTable_attribute attr, Void ignore) {
        println("StackMapTable: number_of_entries = " + attr.number_of_entries);
        indent(+1);
        StackMapTableWriter w = new StackMapTableWriter();
        for (StackMapTable_attribute.stack_map_frame entry : attr.entries) {
            w.write(entry);
        }
        indent(-1);
        return null;
    }

    class StackMapTableWriter // also handles CLDC StackMap attributes
            implements StackMapTable_attribute.stack_map_frame.Visitor<Void,Void> {
        public void write(StackMapTable_attribute.stack_map_frame frame) {
            frame.accept(this, null);
        }

        @Override
        public Void visit_same_frame(StackMapTable_attribute.same_frame frame, Void p) {
            printHeader(frame, "/* same */");
            return null;
        }

        @Override
        public Void visit_same_locals_1_stack_item_frame(StackMapTable_attribute.same_locals_1_stack_item_frame frame, Void p) {
            printHeader(frame, "/* same_locals_1_stack_item */");
            indent(+1);
            printMap("stack", frame.stack);
            indent(-1);
            return null;
        }

        @Override
        public Void visit_same_locals_1_stack_item_frame_extended(StackMapTable_attribute.same_locals_1_stack_item_frame_extended frame, Void p) {
            printHeader(frame, "/* same_locals_1_stack_item_frame_extended */");
            indent(+1);
            println("offset_delta = " + frame.offset_delta);
            printMap("stack", frame.stack);
            indent(-1);
            return null;
        }

        @Override
        public Void visit_chop_frame(StackMapTable_attribute.chop_frame frame, Void p) {
            printHeader(frame, "/* chop */");
            indent(+1);
            println("offset_delta = " + frame.offset_delta);
            indent(-1);
            return null;
        }

        @Override
        public Void visit_same_frame_extended(StackMapTable_attribute.same_frame_extended frame, Void p) {
            printHeader(frame, "/* same_frame_extended */");
            indent(+1);
            println("offset_delta = " + frame.offset_delta);
            indent(-1);
            return null;
        }

        @Override
        public Void visit_append_frame(StackMapTable_attribute.append_frame frame, Void p) {
            printHeader(frame, "/* append */");
            indent(+1);
            println("offset_delta = " + frame.offset_delta);
            printMap("locals", frame.locals);
            indent(-1);
            return null;
        }

        @Override
        public Void visit_full_frame(StackMapTable_attribute.full_frame frame, Void p) {
            if (frame instanceof StackMap_attribute.stack_map_frame) {
                printHeader(frame, "offset = " + frame.offset_delta);
                indent(+1);
            } else {
                printHeader(frame, "/* full_frame */");
                indent(+1);
                println("offset_delta = " + frame.offset_delta);
            }
            printMap("locals", frame.locals);
            printMap("stack", frame.stack);
            indent(-1);
            return null;
        }

        void printHeader(StackMapTable_attribute.stack_map_frame frame, String extra) {
            print("frame_type = " + frame.frame_type + " ");
            println(extra);
        }

        void printMap(String name, StackMapTable_attribute.verification_type_info[] map) {
            print(name + " = [");
            for (int i = 0; i < map.length; i++) {
                StackMapTable_attribute.verification_type_info info = map[i];
                int tag = info.tag;
                switch (tag) {
                    case StackMapTable_attribute.verification_type_info.ITEM_Object:
                        print(" ");
                        constantWriter.write(((StackMapTable_attribute.Object_variable_info) info).cpool_index);
                        break;
                    case StackMapTable_attribute.verification_type_info.ITEM_Uninitialized:
                        print(" " + mapTypeName(tag));
                        print(" " + ((StackMapTable_attribute.Uninitialized_variable_info) info).offset);
                        break;
                    default:
                        print(" " + mapTypeName(tag));
                }
                print(i == (map.length - 1) ? " " : ",");
            }
            println("]");
        }

        String mapTypeName(int tag) {
            switch (tag) {
            case StackMapTable_attribute.verification_type_info.ITEM_Top:
                return "top";

            case StackMapTable_attribute.verification_type_info.ITEM_Integer:
                return "int";

            case StackMapTable_attribute.verification_type_info.ITEM_Float:
                return "float";

            case StackMapTable_attribute.verification_type_info.ITEM_Long:
                return "long";

            case StackMapTable_attribute.verification_type_info.ITEM_Double:
                return "double";

            case StackMapTable_attribute.verification_type_info.ITEM_Null:
                return "null";

            case StackMapTable_attribute.verification_type_info.ITEM_UninitializedThis:
                return "this";

            case StackMapTable_attribute.verification_type_info.ITEM_Object:
                return "CP";

            case StackMapTable_attribute.verification_type_info.ITEM_Uninitialized:
                return "uninitialized";

            default:
                report("unrecognized verification_type_info tag: " + tag);
                return "[tag:" + tag + "]";
            }
        }
    }

    @Override
    public Void visitSynthetic(Synthetic_attribute attr, Void ignore) {
        println("Synthetic: true");
        return null;
    }

    static String getJavaName(String name) {
        return name.replace('/', '.');
    }

    String toHex(byte b, int w) {
        return toHex(b & 0xff, w);
    }

    static String toHex(int i) {
        return StringUtils.toUpperCase(Integer.toString(i, 16));
    }

    static String toHex(int i, int w) {
        String s = StringUtils.toUpperCase(Integer.toHexString(i));
        while (s.length() < w)
            s = "0" + s;
        return StringUtils.toUpperCase(s);
    }

    static String toHex(byte[] ba) {
        StringBuilder sb = new StringBuilder(ba.length);
        for (byte b: ba) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private final AnnotationWriter annotationWriter;
    private final CodeWriter codeWriter;
    private final ConstantWriter constantWriter;
    private final Options options;

    private ConstantPool constant_pool;
    private Object owner;
}
