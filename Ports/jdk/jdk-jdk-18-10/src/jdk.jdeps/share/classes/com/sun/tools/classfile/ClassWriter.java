/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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


package com.sun.tools.classfile;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.sun.tools.classfile.Annotation.*;
import static com.sun.tools.classfile.ConstantPool.*;
import static com.sun.tools.classfile.StackMapTable_attribute.*;
import static com.sun.tools.classfile.StackMapTable_attribute.verification_type_info.*;

/**
 * Write a ClassFile data structure to a file or stream.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class ClassWriter {
    public ClassWriter() {
        attributeWriter = new AttributeWriter();
        constantPoolWriter = new ConstantPoolWriter();
        out = new ClassOutputStream();
    }

    /**
     * Write a ClassFile data structure to a file.
     * @param classFile the classfile object to be written
     * @param f the file
     * @throws IOException if an error occurs while writing the file
     */
    public void write(ClassFile classFile, File f) throws IOException {
        try (FileOutputStream f_out = new FileOutputStream(f)) {
            write(classFile, f_out);
        }
    }

    /**
     * Write a ClassFile data structure to a stream.
     * @param classFile the classfile object to be written
     * @param s the stream
     * @throws IOException if an error occurs while writing the file
     */
    public void write(ClassFile classFile, OutputStream s) throws IOException {
        this.classFile = classFile;
        out.reset();
        write();
        out.writeTo(s);
    }

    protected void write() throws IOException {
        writeHeader();
        writeConstantPool();
        writeAccessFlags(classFile.access_flags);
        writeClassInfo();
        writeFields();
        writeMethods();
        writeAttributes(classFile.attributes);
    }

    protected void writeHeader() {
        out.writeInt(classFile.magic);
        out.writeShort(classFile.minor_version);
        out.writeShort(classFile.major_version);
    }

    protected void writeAccessFlags(AccessFlags flags) {
        out.writeShort(flags.flags);
    }

    protected void writeAttributes(Attributes attributes) {
        int size = attributes.size();
        out.writeShort(size);
        for (Attribute attr: attributes)
            attributeWriter.write(attr, out);
    }

    protected void writeClassInfo() {
        out.writeShort(classFile.this_class);
        out.writeShort(classFile.super_class);
        int[] interfaces = classFile.interfaces;
        out.writeShort(interfaces.length);
        for (int i: interfaces)
            out.writeShort(i);
    }

    protected void writeDescriptor(Descriptor d) {
        out.writeShort(d.index);
    }

    protected void writeConstantPool() {
        ConstantPool pool = classFile.constant_pool;
        int size = pool.size();
        out.writeShort(size);
        for (CPInfo cpInfo: pool.entries())
            constantPoolWriter.write(cpInfo, out);
    }

    protected void writeFields() throws IOException {
        Field[] fields = classFile.fields;
        out.writeShort(fields.length);
        for (Field f: fields)
            writeField(f);
    }

    protected void writeField(Field f) throws IOException {
        writeAccessFlags(f.access_flags);
        out.writeShort(f.name_index);
        writeDescriptor(f.descriptor);
        writeAttributes(f.attributes);
    }

    protected void writeMethods() throws IOException {
        Method[] methods = classFile.methods;
        out.writeShort(methods.length);
        for (Method m: methods) {
            writeMethod(m);
        }
    }

    protected void writeMethod(Method m) throws IOException {
        writeAccessFlags(m.access_flags);
        out.writeShort(m.name_index);
        writeDescriptor(m.descriptor);
        writeAttributes(m.attributes);
    }

    protected ClassFile classFile;
    protected ClassOutputStream out;
    protected AttributeWriter attributeWriter;
    protected ConstantPoolWriter constantPoolWriter;

    /**
     * Subtype of ByteArrayOutputStream with the convenience methods of
     * a DataOutputStream. Since ByteArrayOutputStream does not throw
     * IOException, there are no exceptions from the additional
     * convenience methods either,
     */
    protected static class ClassOutputStream extends ByteArrayOutputStream {
        public ClassOutputStream() {
            d = new DataOutputStream(this);
        }

        public void writeByte(int value) {
            try {
                d.writeByte(value);
            } catch (IOException ignore) {
            }
        }

        public void writeShort(int value) {
            try {
                d.writeShort(value);
            } catch (IOException ignore) {
            }
        }

        public void writeInt(int value) {
            try {
                d.writeInt(value);
            } catch (IOException ignore) {
            }
        }

        public void writeLong(long value) {
            try {
                d.writeLong(value);
            } catch (IOException ignore) {
            }
        }

        public void writeFloat(float value) {
            try {
                d.writeFloat(value);
            } catch (IOException ignore) {
            }
        }

        public void writeDouble(double value) {
            try {
                d.writeDouble(value);
            } catch (IOException ignore) {
            }
        }

        public void writeUTF(String value) {
            try {
                d.writeUTF(value);
            } catch (IOException ignore) {
            }
        }

        public void writeTo(ClassOutputStream s) {
            try {
                super.writeTo(s);
            } catch (IOException ignore) {
            }
        }

        private final DataOutputStream d;
    }

    /**
     * Writer for the entries in the constant pool.
     */
    protected static class ConstantPoolWriter
           implements ConstantPool.Visitor<Integer,ClassOutputStream> {
        protected int write(CPInfo info, ClassOutputStream out) {
            out.writeByte(info.getTag());
            return info.accept(this, out);
        }

        @Override
        public Integer visitClass(CONSTANT_Class_info info, ClassOutputStream out) {
            out.writeShort(info.name_index);
            return 1;
        }

        @Override
        public Integer visitDouble(CONSTANT_Double_info info, ClassOutputStream out) {
            out.writeDouble(info.value);
            return 2;
        }

        @Override
        public Integer visitFieldref(CONSTANT_Fieldref_info info, ClassOutputStream out) {
            writeRef(info, out);
            return 1;
        }

        @Override
        public Integer visitFloat(CONSTANT_Float_info info, ClassOutputStream out) {
            out.writeFloat(info.value);
            return 1;
        }

        @Override
        public Integer visitInteger(CONSTANT_Integer_info info, ClassOutputStream out) {
            out.writeInt(info.value);
            return 1;
        }

        @Override
        public Integer visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, ClassOutputStream out) {
            writeRef(info, out);
            return 1;
        }

        @Override
        public Integer visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, ClassOutputStream out) {
            out.writeShort(info.bootstrap_method_attr_index);
            out.writeShort(info.name_and_type_index);
            return 1;
        }

        public Integer visitDynamicConstant(CONSTANT_Dynamic_info info, ClassOutputStream out) {
            out.writeShort(info.bootstrap_method_attr_index);
            out.writeShort(info.name_and_type_index);
            return 1;
        }

        @Override
        public Integer visitLong(CONSTANT_Long_info info, ClassOutputStream out) {
            out.writeLong(info.value);
            return 2;
        }

        @Override
        public Integer visitMethodHandle(CONSTANT_MethodHandle_info info, ClassOutputStream out) {
            out.writeByte(info.reference_kind.tag);
            out.writeShort(info.reference_index);
            return 1;
        }

        @Override
        public Integer visitMethodType(CONSTANT_MethodType_info info, ClassOutputStream out) {
            out.writeShort(info.descriptor_index);
            return 1;
        }

        @Override
        public Integer visitMethodref(CONSTANT_Methodref_info info, ClassOutputStream out) {
            return writeRef(info, out);
        }

        @Override
        public Integer visitModule(CONSTANT_Module_info info, ClassOutputStream out) {
            out.writeShort(info.name_index);
            return 1;
        }

        @Override
        public Integer visitNameAndType(CONSTANT_NameAndType_info info, ClassOutputStream out) {
            out.writeShort(info.name_index);
            out.writeShort(info.type_index);
            return 1;
        }

        @Override
        public Integer visitPackage(CONSTANT_Package_info info, ClassOutputStream out) {
            out.writeShort(info.name_index);
            return 1;
        }

        @Override
        public Integer visitString(CONSTANT_String_info info, ClassOutputStream out) {
            out.writeShort(info.string_index);
            return 1;
        }

        @Override
        public Integer visitUtf8(CONSTANT_Utf8_info info, ClassOutputStream out) {
            out.writeUTF(info.value);
            return 1;
        }

        protected Integer writeRef(CPRefInfo info, ClassOutputStream out) {
            out.writeShort(info.class_index);
            out.writeShort(info.name_and_type_index);
            return 1;
        }
    }

    /**
     * Writer for the different types of attribute.
     */
    protected static class AttributeWriter implements Attribute.Visitor<Void,ClassOutputStream> {
        public void write(Attributes attributes, ClassOutputStream out) {
            int size = attributes.size();
            out.writeShort(size);
            for (Attribute a: attributes)
                write(a, out);
        }

        public void write(Attribute attr, ClassOutputStream out) {
            out.writeShort(attr.attribute_name_index);
            ClassOutputStream nestedOut = new ClassOutputStream();
            attr.accept(this, nestedOut);
            out.writeInt(nestedOut.size());
            nestedOut.writeTo(out);
        }

        protected AnnotationWriter annotationWriter = new AnnotationWriter();

        @Override
        public Void visitDefault(DefaultAttribute attr, ClassOutputStream out) {
            out.write(attr.info, 0, attr.info.length);
            return null;
        }

        @Override
        public Void visitAnnotationDefault(AnnotationDefault_attribute attr, ClassOutputStream out) {
            annotationWriter.write(attr.default_value, out);
            return null;
        }

        @Override
        public Void visitBootstrapMethods(BootstrapMethods_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.bootstrap_method_specifiers.length);
            for (BootstrapMethods_attribute.BootstrapMethodSpecifier bsm : attr.bootstrap_method_specifiers) {
                out.writeShort(bsm.bootstrap_method_ref);
                int bsm_args_count = bsm.bootstrap_arguments.length;
                out.writeShort(bsm_args_count);
                for (int i : bsm.bootstrap_arguments) {
                    out.writeShort(i);
                }
            }
            return null;
        }

        @Override
        public Void visitCharacterRangeTable(CharacterRangeTable_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.character_range_table.length);
            for (CharacterRangeTable_attribute.Entry e: attr.character_range_table)
                writeCharacterRangeTableEntry(e, out);
            return null;
        }

        protected void writeCharacterRangeTableEntry(CharacterRangeTable_attribute.Entry entry, ClassOutputStream out) {
            out.writeShort(entry.start_pc);
            out.writeShort(entry.end_pc);
            out.writeInt(entry.character_range_start);
            out.writeInt(entry.character_range_end);
            out.writeShort(entry.flags);
        }

        @Override
        public Void visitCode(Code_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.max_stack);
            out.writeShort(attr.max_locals);
            out.writeInt(attr.code.length);
            out.write(attr.code, 0, attr.code.length);
            out.writeShort(attr.exception_table.length);
            for (Code_attribute.Exception_data e: attr.exception_table)
                writeExceptionTableEntry(e, out);
            new AttributeWriter().write(attr.attributes, out);
            return null;
        }

        protected void writeExceptionTableEntry(Code_attribute.Exception_data exception_data, ClassOutputStream out) {
            out.writeShort(exception_data.start_pc);
            out.writeShort(exception_data.end_pc);
            out.writeShort(exception_data.handler_pc);
            out.writeShort(exception_data.catch_type);
        }

        @Override
        public Void visitCompilationID(CompilationID_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.compilationID_index);
            return null;
        }

        @Override
        public Void visitConstantValue(ConstantValue_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.constantvalue_index);
            return null;
        }

        @Override
        public Void visitDeprecated(Deprecated_attribute attr, ClassOutputStream out) {
            return null;
        }

        @Override
        public Void visitEnclosingMethod(EnclosingMethod_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.class_index);
            out.writeShort(attr.method_index);
            return null;
        }

        @Override
        public Void visitExceptions(Exceptions_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.exception_index_table.length);
            for (int i: attr.exception_index_table)
                out.writeShort(i);
            return null;
        }

        @Override
        public Void visitInnerClasses(InnerClasses_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.classes.length);
            for (InnerClasses_attribute.Info info: attr.classes)
                writeInnerClassesInfo(info, out);
            return null;
        }

        protected void writeInnerClassesInfo(InnerClasses_attribute.Info info, ClassOutputStream out) {
            out.writeShort(info.inner_class_info_index);
            out.writeShort(info.outer_class_info_index);
            out.writeShort(info.inner_name_index);
            writeAccessFlags(info.inner_class_access_flags, out);
        }

        @Override
        public Void visitLineNumberTable(LineNumberTable_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.line_number_table.length);
            for (LineNumberTable_attribute.Entry e: attr.line_number_table)
                writeLineNumberTableEntry(e, out);
            return null;
        }

        protected void writeLineNumberTableEntry(LineNumberTable_attribute.Entry entry, ClassOutputStream out) {
            out.writeShort(entry.start_pc);
            out.writeShort(entry.line_number);
        }

        @Override
        public Void visitLocalVariableTable(LocalVariableTable_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.local_variable_table.length);
            for (LocalVariableTable_attribute.Entry e: attr.local_variable_table)
                writeLocalVariableTableEntry(e, out);
            return null;
        }

        protected void writeLocalVariableTableEntry(LocalVariableTable_attribute.Entry entry, ClassOutputStream out) {
            out.writeShort(entry.start_pc);
            out.writeShort(entry.length);
            out.writeShort(entry.name_index);
            out.writeShort(entry.descriptor_index);
            out.writeShort(entry.index);
        }

        @Override
        public Void visitLocalVariableTypeTable(LocalVariableTypeTable_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.local_variable_table.length);
            for (LocalVariableTypeTable_attribute.Entry e: attr.local_variable_table)
                writeLocalVariableTypeTableEntry(e, out);
            return null;
        }

        protected void writeLocalVariableTypeTableEntry(LocalVariableTypeTable_attribute.Entry entry, ClassOutputStream out) {
            out.writeShort(entry.start_pc);
            out.writeShort(entry.length);
            out.writeShort(entry.name_index);
            out.writeShort(entry.signature_index);
            out.writeShort(entry.index);
        }

        @Override
        public Void visitNestHost(NestHost_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.top_index);
            return null;
        }

        @Override
        public Void visitMethodParameters(MethodParameters_attribute attr, ClassOutputStream out) {
            out.writeByte(attr.method_parameter_table.length);
            for (MethodParameters_attribute.Entry e : attr.method_parameter_table) {
                out.writeShort(e.name_index);
                out.writeShort(e.flags);
            }
            return null;
        }

        @Override
        public Void visitModule(Module_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.module_name);
            out.writeShort(attr.module_flags);
            out.writeShort(attr.module_version_index);

            out.writeShort(attr.requires.length);
            for (Module_attribute.RequiresEntry e: attr.requires) {
                out.writeShort(e.requires_index);
                out.writeShort(e.requires_flags);
                out.writeShort(e.requires_version_index);
            }

            out.writeShort(attr.exports.length);
            for (Module_attribute.ExportsEntry e: attr.exports) {
                out.writeShort(e.exports_index);
                out.writeShort(e.exports_flags);
                out.writeShort(e.exports_to_index.length);
                for (int index: e.exports_to_index)
                    out.writeShort(index);
            }

            out.writeShort(attr.opens.length);
            for (Module_attribute.OpensEntry e: attr.opens) {
                out.writeShort(e.opens_index);
                out.writeShort(e.opens_flags);
                out.writeShort(e.opens_to_index.length);
                for (int index: e.opens_to_index)
                    out.writeShort(index);
            }

            out.writeShort(attr.uses_index.length);
            for (int index: attr.uses_index) {
                out.writeShort(index);
            }

            out.writeShort(attr.provides.length);
            for (Module_attribute.ProvidesEntry e: attr.provides) {
                out.writeShort(e.provides_index);
                out.writeShort(e.with_count);
                for (int with : e.with_index) {
                    out.writeShort(with);
                }
            }

            return null;
        }

        @Override
        public Void visitModuleHashes(ModuleHashes_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.algorithm_index);
            out.writeShort(attr.hashes_table.length);
            for (ModuleHashes_attribute.Entry e: attr.hashes_table) {
                out.writeShort(e.module_name_index);
                out.writeShort(e.hash.length);
                for (byte b: e.hash) {
                    out.writeByte(b);
                }
            }
            return null;
        }

        @Override
        public Void visitModuleMainClass(ModuleMainClass_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.main_class_index);
            return null;
        }

        @Override
        public Void visitModulePackages(ModulePackages_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.packages_count);
            for (int i: attr.packages_index)
                out.writeShort(i);
            return null;
        }

        @Override
        public Void visitModuleResolution(ModuleResolution_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.resolution_flags);
            return null;
        }

        @Override
        public Void visitModuleTarget(ModuleTarget_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.target_platform_index);
            return null;
        }

        @Override
        public Void visitNestMembers(NestMembers_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.members_indexes.length);
            for (int i : attr.members_indexes) {
                out.writeShort(i);
            }
            return null;
        }

        @Override
        public Void visitRecord(Record_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.component_count);
            for (Record_attribute.ComponentInfo info: attr.component_info_arr) {
                out.writeShort(info.name_index);
                out.writeShort(info.descriptor.index);
                int size = info.attributes.size();
                out.writeShort(size);
                for (Attribute componentAttr: info.attributes)
                    write(componentAttr, out);
            }
            return null;
        }

        @Override
        public Void visitRuntimeInvisibleAnnotations(RuntimeInvisibleAnnotations_attribute attr, ClassOutputStream out) {
            annotationWriter.write(attr.annotations, out);
            return null;
        }

        @Override
        public Void visitRuntimeInvisibleParameterAnnotations(RuntimeInvisibleParameterAnnotations_attribute attr, ClassOutputStream out) {
            out.writeByte(attr.parameter_annotations.length);
            for (Annotation[] annos: attr.parameter_annotations)
                annotationWriter.write(annos, out);
            return null;
        }

        @Override
        public Void visitRuntimeInvisibleTypeAnnotations(RuntimeInvisibleTypeAnnotations_attribute attr, ClassOutputStream out) {
            annotationWriter.write(attr.annotations, out);
            return null;
        }

        @Override
        public Void visitRuntimeVisibleAnnotations(RuntimeVisibleAnnotations_attribute attr, ClassOutputStream out) {
            annotationWriter.write(attr.annotations, out);
            return null;
        }

        @Override
        public Void visitRuntimeVisibleParameterAnnotations(RuntimeVisibleParameterAnnotations_attribute attr, ClassOutputStream out) {
            out.writeByte(attr.parameter_annotations.length);
            for (Annotation[] annos: attr.parameter_annotations)
                annotationWriter.write(annos, out);
            return null;
        }

        @Override
        public Void visitRuntimeVisibleTypeAnnotations(RuntimeVisibleTypeAnnotations_attribute attr, ClassOutputStream out) {
            annotationWriter.write(attr.annotations, out);
            return null;
        }

        @Override
        public Void visitPermittedSubclasses(PermittedSubclasses_attribute attr, ClassOutputStream out) {
            int n = attr.subtypes.length;
            out.writeShort(n);
            for (int i = 0 ; i < n ; i++) {
                out.writeShort(attr.subtypes[i]);
            }
            return null;
        }

        @Override
        public Void visitSignature(Signature_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.signature_index);
            return null;
        }

        @Override
        public Void visitSourceDebugExtension(SourceDebugExtension_attribute attr, ClassOutputStream out) {
            out.write(attr.debug_extension, 0, attr.debug_extension.length);
            return null;
        }

        @Override
        public Void visitSourceFile(SourceFile_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.sourcefile_index);
            return null;
        }

        @Override
        public Void visitSourceID(SourceID_attribute attr, ClassOutputStream out) {
            out.writeShort(attr.sourceID_index);
            return null;
        }

        @Override
        public Void visitStackMap(StackMap_attribute attr, ClassOutputStream out) {
            if (stackMapWriter == null)
                stackMapWriter = new StackMapTableWriter();

            out.writeShort(attr.entries.length);
            for (stack_map_frame f: attr.entries)
                stackMapWriter.write(f, out);
            return null;
        }

        @Override
        public Void visitStackMapTable(StackMapTable_attribute attr, ClassOutputStream out) {
            if (stackMapWriter == null)
                stackMapWriter = new StackMapTableWriter();

            out.writeShort(attr.entries.length);
            for (stack_map_frame f: attr.entries)
                stackMapWriter.write(f, out);
            return null;
        }

        @Override
        public Void visitSynthetic(Synthetic_attribute attr, ClassOutputStream out) {
            return null;
        }

        protected void writeAccessFlags(AccessFlags flags, ClassOutputStream out) {
            out.writeShort(flags.flags);
        }

        protected StackMapTableWriter stackMapWriter;
    }

    /**
     * Writer for the frames of StackMap and StackMapTable attributes.
     */
    protected static class StackMapTableWriter
            implements stack_map_frame.Visitor<Void,ClassOutputStream> {

        public void write(stack_map_frame frame, ClassOutputStream out) {
            out.write(frame.frame_type);
            frame.accept(this, out);
        }

        @Override
        public Void visit_same_frame(same_frame frame, ClassOutputStream p) {
            return null;
        }

        @Override
        public Void visit_same_locals_1_stack_item_frame(same_locals_1_stack_item_frame frame, ClassOutputStream out) {
            writeVerificationTypeInfo(frame.stack[0], out);
            return null;
        }

        @Override
        public Void visit_same_locals_1_stack_item_frame_extended(same_locals_1_stack_item_frame_extended frame, ClassOutputStream out) {
            out.writeShort(frame.offset_delta);
            writeVerificationTypeInfo(frame.stack[0], out);
            return null;
        }

        @Override
        public Void visit_chop_frame(chop_frame frame, ClassOutputStream out) {
            out.writeShort(frame.offset_delta);
            return null;
        }

        @Override
        public Void visit_same_frame_extended(same_frame_extended frame, ClassOutputStream out) {
            out.writeShort(frame.offset_delta);
            return null;
        }

        @Override
        public Void visit_append_frame(append_frame frame, ClassOutputStream out) {
            out.writeShort(frame.offset_delta);
            for (verification_type_info l: frame.locals)
                writeVerificationTypeInfo(l, out);
            return null;
        }

        @Override
        public Void visit_full_frame(full_frame frame, ClassOutputStream out) {
            out.writeShort(frame.offset_delta);
            out.writeShort(frame.locals.length);
            for (verification_type_info l: frame.locals)
                writeVerificationTypeInfo(l, out);
            out.writeShort(frame.stack.length);
            for (verification_type_info s: frame.stack)
                writeVerificationTypeInfo(s, out);
            return null;
        }

        protected void writeVerificationTypeInfo(verification_type_info info,
                ClassOutputStream out)  {
            out.write(info.tag);
            switch (info.tag) {
            case ITEM_Top:
            case ITEM_Integer:
            case ITEM_Float:
            case ITEM_Long:
            case ITEM_Double:
            case ITEM_Null:
            case ITEM_UninitializedThis:
                break;

            case ITEM_Object:
                Object_variable_info o = (Object_variable_info) info;
                out.writeShort(o.cpool_index);
                break;

            case ITEM_Uninitialized:
                Uninitialized_variable_info u = (Uninitialized_variable_info) info;
                out.writeShort(u.offset);
                break;

            default:
                throw new Error();
            }
        }
    }

    /**
     * Writer for annotations and the values they contain.
     */
    protected static class AnnotationWriter
            implements Annotation.element_value.Visitor<Void,ClassOutputStream> {
        public void write(Annotation[] annos, ClassOutputStream out) {
            out.writeShort(annos.length);
            for (Annotation anno: annos)
                write(anno, out);
        }

        public void write(TypeAnnotation[] annos, ClassOutputStream out) {
            out.writeShort(annos.length);
            for (TypeAnnotation anno: annos)
                write(anno, out);
        }

        public void write(Annotation anno, ClassOutputStream out) {
            out.writeShort(anno.type_index);
            out.writeShort(anno.element_value_pairs.length);
            for (element_value_pair p: anno.element_value_pairs)
                write(p, out);
        }

        public void write(TypeAnnotation anno, ClassOutputStream out) {
            write(anno.position, out);
            write(anno.annotation, out);
        }

        public void write(element_value_pair pair, ClassOutputStream out) {
            out.writeShort(pair.element_name_index);
            write(pair.value, out);
        }

        public void write(element_value ev, ClassOutputStream out) {
            out.writeByte(ev.tag);
            ev.accept(this, out);
        }

        @Override
        public Void visitPrimitive(Primitive_element_value ev, ClassOutputStream out) {
            out.writeShort(ev.const_value_index);
            return null;
        }

        @Override
        public Void visitEnum(Enum_element_value ev, ClassOutputStream out) {
            out.writeShort(ev.type_name_index);
            out.writeShort(ev.const_name_index);
            return null;
        }

        @Override
        public Void visitClass(Class_element_value ev, ClassOutputStream out) {
            out.writeShort(ev.class_info_index);
            return null;
        }

        @Override
        public Void visitAnnotation(Annotation_element_value ev, ClassOutputStream out) {
            write(ev.annotation_value, out);
            return null;
        }

        @Override
        public Void visitArray(Array_element_value ev, ClassOutputStream out) {
            out.writeShort(ev.num_values);
            for (element_value v: ev.values)
                write(v, out);
            return null;
        }

        // TODO: Move this to TypeAnnotation to be closer with similar logic?
        private void write(TypeAnnotation.Position p, ClassOutputStream out) {
            out.writeByte(p.type.targetTypeValue());
            switch (p.type) {
            // instanceof
            case INSTANCEOF:
            // new expression
            case NEW:
            // constructor/method reference receiver
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE:
                out.writeShort(p.offset);
                break;
            // local variable
            case LOCAL_VARIABLE:
            // resource variable
            case RESOURCE_VARIABLE:
                int table_length = p.lvarOffset.length;
                out.writeShort(table_length);
                for (int i = 0; i < table_length; ++i) {
                    out.writeShort(1);  // for table length
                    out.writeShort(p.lvarOffset[i]);
                    out.writeShort(p.lvarLength[i]);
                    out.writeShort(p.lvarIndex[i]);
                }
                break;
            // exception parameter
            case EXCEPTION_PARAMETER:
                out.writeShort(p.exception_index);
                break;
            // method receiver
            case METHOD_RECEIVER:
                // Do nothing
                break;
            // type parameters
            case CLASS_TYPE_PARAMETER:
            case METHOD_TYPE_PARAMETER:
                out.writeByte(p.parameter_index);
                break;
            // type parameters bounds
            case CLASS_TYPE_PARAMETER_BOUND:
            case METHOD_TYPE_PARAMETER_BOUND:
                out.writeByte(p.parameter_index);
                out.writeByte(p.bound_index);
                break;
            // class extends or implements clause
            case CLASS_EXTENDS:
                out.writeShort(p.type_index);
                break;
            // throws
            case THROWS:
                out.writeShort(p.type_index);
                break;
            // method parameter
            case METHOD_FORMAL_PARAMETER:
                out.writeByte(p.parameter_index);
                break;
            // type cast
            case CAST:
            // method/constructor/reference type argument
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
                out.writeShort(p.offset);
                out.writeByte(p.type_index);
                break;
            // We don't need to worry about these
            case METHOD_RETURN:
            case FIELD:
                break;
            case UNKNOWN:
                throw new AssertionError("ClassWriter: UNKNOWN target type should never occur!");
            default:
                throw new AssertionError("ClassWriter: Unknown target type for position: " + p);
            }

            { // Append location data for generics/arrays.
                // TODO: check for overrun?
                out.writeByte((byte)p.location.size());
                for (int i : TypeAnnotation.Position.getBinaryFromTypePath(p.location))
                    out.writeByte((byte)i);
            }
        }
    }
}
