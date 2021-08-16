/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

#include "precompiled.hpp"
#include "classfile/symbolTable.hpp"
#include "interpreter/bytecodeStream.hpp"
#include "memory/universe.hpp"
#include "oops/fieldStreams.inline.hpp"
#include "oops/recordComponent.hpp"
#include "prims/jvmtiClassFileReconstituter.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/signature.hpp"
#include "utilities/bytes.hpp"

// FIXME: add Deprecated attribute
// FIXME: fix Synthetic attribute
// FIXME: per Serguei, add error return handling for ConstantPool::copy_cpool_bytes()

JvmtiConstantPoolReconstituter::JvmtiConstantPoolReconstituter(InstanceKlass* ik) {
  set_error(JVMTI_ERROR_NONE);
  _ik = ik;
  _cpool = constantPoolHandle(Thread::current(), ik->constants());
  _symmap = new SymbolHashMap();
  _classmap = new SymbolHashMap();
  _cpool_size = _cpool->hash_entries_to(_symmap, _classmap);
  if (_cpool_size == 0) {
    set_error(JVMTI_ERROR_OUT_OF_MEMORY);
  } else if (_cpool_size < 0) {
    set_error(JVMTI_ERROR_INTERNAL);
  }
}

// Write the field information portion of ClassFile structure
// JVMSpec|     u2 fields_count;
// JVMSpec|     field_info fields[fields_count];
void JvmtiClassFileReconstituter::write_field_infos() {
  HandleMark hm(thread());
  Array<AnnotationArray*>* fields_anno = ik()->fields_annotations();
  Array<AnnotationArray*>* fields_type_anno = ik()->fields_type_annotations();

  // Compute the real number of Java fields
  int java_fields = ik()->java_fields_count();

  write_u2(java_fields);
  for (JavaFieldStream fs(ik()); !fs.done(); fs.next()) {
    AccessFlags access_flags = fs.access_flags();
    int name_index = fs.name_index();
    int signature_index = fs.signature_index();
    int initial_value_index = fs.initval_index();
    guarantee(name_index != 0 && signature_index != 0, "bad constant pool index for field");
    // int offset = ik()->field_offset( index );
    int generic_signature_index = fs.generic_signature_index();
    AnnotationArray* anno = fields_anno == NULL ? NULL : fields_anno->at(fs.index());
    AnnotationArray* type_anno = fields_type_anno == NULL ? NULL : fields_type_anno->at(fs.index());

    // JVMSpec|   field_info {
    // JVMSpec|         u2 access_flags;
    // JVMSpec|         u2 name_index;
    // JVMSpec|         u2 descriptor_index;
    // JVMSpec|         u2 attributes_count;
    // JVMSpec|         attribute_info attributes[attributes_count];
    // JVMSpec|   }

    write_u2(access_flags.as_int() & JVM_RECOGNIZED_FIELD_MODIFIERS);
    write_u2(name_index);
    write_u2(signature_index);
    int attr_count = 0;
    if (initial_value_index != 0) {
      ++attr_count;
    }
    if (access_flags.is_synthetic()) {
      // ++attr_count;
    }
    if (generic_signature_index != 0) {
      ++attr_count;
    }
    if (anno != NULL) {
      ++attr_count;     // has RuntimeVisibleAnnotations attribute
    }
    if (type_anno != NULL) {
      ++attr_count;     // has RuntimeVisibleTypeAnnotations attribute
    }

    write_u2(attr_count);

    if (initial_value_index != 0) {
      write_attribute_name_index("ConstantValue");
      write_u4(2); //length always 2
      write_u2(initial_value_index);
    }
    if (access_flags.is_synthetic()) {
      // write_synthetic_attribute();
    }
    if (generic_signature_index != 0) {
      write_signature_attribute(generic_signature_index);
    }
    if (anno != NULL) {
      write_annotations_attribute("RuntimeVisibleAnnotations", anno);
    }
    if (type_anno != NULL) {
      write_annotations_attribute("RuntimeVisibleTypeAnnotations", type_anno);
    }
  }
}

// Write Code attribute
// JVMSpec|   Code_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 max_stack;
// JVMSpec|     u2 max_locals;
// JVMSpec|     u4 code_length;
// JVMSpec|     u1 code[code_length];
// JVMSpec|     u2 exception_table_length;
// JVMSpec|     {       u2 start_pc;
// JVMSpec|             u2 end_pc;
// JVMSpec|             u2  handler_pc;
// JVMSpec|             u2  catch_type;
// JVMSpec|     }       exception_table[exception_table_length];
// JVMSpec|     u2 attributes_count;
// JVMSpec|     attribute_info attributes[attributes_count];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_code_attribute(const methodHandle& method) {
  ConstMethod* const_method = method->constMethod();
  u2 line_num_cnt = 0;
  int stackmap_len = 0;
  int local_variable_table_length = 0;
  int local_variable_type_table_length = 0;

  // compute number and length of attributes
  int attr_count = 0;
  int attr_size = 0;
  if (const_method->has_linenumber_table()) {
    line_num_cnt = line_number_table_entries(method);
    if (line_num_cnt != 0) {
      ++attr_count;
      // Compute the complete size of the line number table attribute:
      //      LineNumberTable_attribute {
      //        u2 attribute_name_index;
      //        u4 attribute_length;
      //        u2 line_number_table_length;
      //        {  u2 start_pc;
      //           u2 line_number;
      //        } line_number_table[line_number_table_length];
      //      }
      attr_size += 2 + 4 + 2 + line_num_cnt * (2 + 2);
    }
  }
  if (method->has_stackmap_table()) {
    stackmap_len = method->stackmap_data()->length();
    if (stackmap_len != 0) {
      ++attr_count;
      // Compute the  size of the stack map table attribute (VM stores raw):
      //      StackMapTable_attribute {
      //        u2 attribute_name_index;
      //        u4 attribute_length;
      //        u2 number_of_entries;
      //        stack_map_frame_entries[number_of_entries];
      //      }
      attr_size += 2 + 4 + stackmap_len;
    }
  }
  if (method->has_localvariable_table()) {
    local_variable_table_length = method->localvariable_table_length();
    if (local_variable_table_length != 0) {
      ++attr_count;
      // Compute the size of the local variable table attribute (VM stores raw):
      // LocalVariableTable_attribute {
      //   u2 attribute_name_index;
      //   u4 attribute_length;
      //   u2 local_variable_table_length;
      //   {
      //     u2 start_pc;
      //     u2 length;
      //     u2 name_index;
      //     u2 descriptor_index;
      //     u2 index;
      //   }
      attr_size += 2 + 4 + 2 + local_variable_table_length * (2 + 2 + 2 + 2 + 2);

      // Local variables with generic signatures must have LVTT entries
      LocalVariableTableElement *elem = method->localvariable_table_start();
      for (int idx = 0; idx < local_variable_table_length; idx++) {
        if (elem[idx].signature_cp_index != 0) {
          local_variable_type_table_length++;
        }
      }

      if (local_variable_type_table_length != 0) {
        ++attr_count;
        // Compute the size of the local variable type table attribute (VM stores raw):
        // LocalVariableTypeTable_attribute {
        //   u2 attribute_name_index;
        //   u4 attribute_length;
        //   u2 local_variable_type_table_length;
        //   {
        //     u2 start_pc;
        //     u2 length;
        //     u2 name_index;
        //     u2 signature_index;
        //     u2 index;
        //   }
        attr_size += 2 + 4 + 2 + local_variable_type_table_length * (2 + 2 + 2 + 2 + 2);
      }
    }
  }

  ExceptionTable exception_table(method());
  int exception_table_length = exception_table.length();
  int code_size = const_method->code_size();
  int size =
    2+2+4 +                                // max_stack, max_locals, code_length
    code_size +                            // code
    2 +                                    // exception_table_length
    (2+2+2+2) * exception_table_length +   // exception_table
    2 +                                    // attributes_count
    attr_size;                             // attributes

  write_attribute_name_index("Code");
  write_u4(size);
  write_u2(method->verifier_max_stack());
  write_u2(method->max_locals());
  write_u4(code_size);
  copy_bytecodes(method, (unsigned char*)writeable_address(code_size));
  write_u2(exception_table_length);
  for (int index = 0; index < exception_table_length; index++) {
    write_u2(exception_table.start_pc(index));
    write_u2(exception_table.end_pc(index));
    write_u2(exception_table.handler_pc(index));
    write_u2(exception_table.catch_type_index(index));
  }
  write_u2(attr_count);
  if (line_num_cnt != 0) {
    write_line_number_table_attribute(method, line_num_cnt);
  }
  if (stackmap_len != 0) {
    write_stackmap_table_attribute(method, stackmap_len);
  }
  if (local_variable_table_length != 0) {
    write_local_variable_table_attribute(method, local_variable_table_length);
  }
  if (local_variable_type_table_length != 0) {
    write_local_variable_type_table_attribute(method, local_variable_type_table_length);
  }
}

// Write Exceptions attribute
// JVMSpec|   Exceptions_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 number_of_exceptions;
// JVMSpec|     u2 exception_index_table[number_of_exceptions];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_exceptions_attribute(ConstMethod* const_method) {
  CheckedExceptionElement* checked_exceptions = const_method->checked_exceptions_start();
  int checked_exceptions_length = const_method->checked_exceptions_length();
  int size =
    2 +                                    // number_of_exceptions
    2 * checked_exceptions_length;         // exception_index_table

  write_attribute_name_index("Exceptions");
  write_u4(size);
  write_u2(checked_exceptions_length);
  for (int index = 0; index < checked_exceptions_length; index++) {
    write_u2(checked_exceptions[index].class_cp_index);
  }
}

// Write SourceFile attribute
// JVMSpec|   SourceFile_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 sourcefile_index;
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_source_file_attribute() {
  assert(ik()->source_file_name() != NULL, "caller must check");

  write_attribute_name_index("SourceFile");
  write_u4(2);  // always length 2
  write_u2(symbol_to_cpool_index(ik()->source_file_name()));
}

// Write SourceDebugExtension attribute
// JSR45|   SourceDebugExtension_attribute {
// JSR45|       u2 attribute_name_index;
// JSR45|       u4 attribute_length;
// JSR45|       u1 debug_extension[attribute_length];
// JSR45|   }
void JvmtiClassFileReconstituter::write_source_debug_extension_attribute() {
  assert(ik()->source_debug_extension() != NULL, "caller must check");

  write_attribute_name_index("SourceDebugExtension");
  int len = (int)strlen(ik()->source_debug_extension());
  write_u4(len);
  u1* ext = (u1*)ik()->source_debug_extension();
  for (int i=0; i<len; i++) {
    write_u1(ext[i]);
  }
}

// Write (generic) Signature attribute
// JVMSpec|   Signature_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 signature_index;
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_signature_attribute(u2 generic_signature_index) {
  write_attribute_name_index("Signature");
  write_u4(2);  // always length 2
  write_u2(generic_signature_index);
}

// Compute the number of entries in the InnerClasses attribute
u2 JvmtiClassFileReconstituter::inner_classes_attribute_length() {
  InnerClassesIterator iter(ik());
  return iter.length();
}

// Write an annotation attribute.  The VM stores them in raw form, so all we need
// to do is add the attrubute name and fill in the length.
// JSR202|   *Annotations_attribute {
// JSR202|     u2 attribute_name_index;
// JSR202|     u4 attribute_length;
// JSR202|     ...
// JSR202|   }
void JvmtiClassFileReconstituter::write_annotations_attribute(const char* attr_name,
                                                              AnnotationArray* annos) {
  u4 length = annos->length();
  write_attribute_name_index(attr_name);
  write_u4(length);
  memcpy(writeable_address(length), annos->adr_at(0), length);
}

//  BootstrapMethods_attribute {
//    u2 attribute_name_index;
//    u4 attribute_length;
//    u2 num_bootstrap_methods;
//    {   u2 bootstrap_method_ref;
//        u2 num_bootstrap_arguments;
//        u2 bootstrap_arguments[num_bootstrap_arguments];
//    } bootstrap_methods[num_bootstrap_methods];
//  }
void JvmtiClassFileReconstituter::write_bootstrapmethod_attribute() {
  Array<u2>* operands = cpool()->operands();
  write_attribute_name_index("BootstrapMethods");
  int num_bootstrap_methods = ConstantPool::operand_array_length(operands);

  // calculate length of attribute
  int length = sizeof(u2); // num_bootstrap_methods
  for (int n = 0; n < num_bootstrap_methods; n++) {
    u2 num_bootstrap_arguments = cpool()->operand_argument_count_at(n);
    length += sizeof(u2); // bootstrap_method_ref
    length += sizeof(u2); // num_bootstrap_arguments
    length += sizeof(u2) * num_bootstrap_arguments; // bootstrap_arguments[num_bootstrap_arguments]
  }
  write_u4(length);

  // write attribute
  write_u2(num_bootstrap_methods);
  for (int n = 0; n < num_bootstrap_methods; n++) {
    u2 bootstrap_method_ref = cpool()->operand_bootstrap_method_ref_index_at(n);
    u2 num_bootstrap_arguments = cpool()->operand_argument_count_at(n);
    write_u2(bootstrap_method_ref);
    write_u2(num_bootstrap_arguments);
    for (int arg = 0; arg < num_bootstrap_arguments; arg++) {
      u2 bootstrap_argument = cpool()->operand_argument_index_at(n, arg);
      write_u2(bootstrap_argument);
    }
  }
}

//  NestHost_attribute {
//    u2 attribute_name_index;
//    u4 attribute_length;
//    u2 host_class_index;
//  }
void JvmtiClassFileReconstituter::write_nest_host_attribute() {
  int length = sizeof(u2);
  int host_class_index = ik()->nest_host_index();

  write_attribute_name_index("NestHost");
  write_u4(length);
  write_u2(host_class_index);
}

//  NestMembers_attribute {
//    u2 attribute_name_index;
//    u4 attribute_length;
//    u2 number_of_classes;
//    u2 classes[number_of_classes];
//  }
void JvmtiClassFileReconstituter::write_nest_members_attribute() {
  Array<u2>* nest_members = ik()->nest_members();
  int number_of_classes = nest_members->length();
  int length = sizeof(u2) * (1 + number_of_classes);

  write_attribute_name_index("NestMembers");
  write_u4(length);
  write_u2(number_of_classes);
  for (int i = 0; i < number_of_classes; i++) {
    u2 class_cp_index = nest_members->at(i);
    write_u2(class_cp_index);
  }
}

//  PermittedSubclasses {
//    u2 attribute_name_index;
//    u4 attribute_length;
//    u2 number_of_classes;
//    u2 classes[number_of_classes];
//  }
void JvmtiClassFileReconstituter::write_permitted_subclasses_attribute() {
  Array<u2>* permitted_subclasses = ik()->permitted_subclasses();
  int number_of_classes = permitted_subclasses->length();
  int length = sizeof(u2) * (1 + number_of_classes); // '1 +' is for number_of_classes field

  write_attribute_name_index("PermittedSubclasses");
  write_u4(length);
  write_u2(number_of_classes);
  for (int i = 0; i < number_of_classes; i++) {
    u2 class_cp_index = permitted_subclasses->at(i);
    write_u2(class_cp_index);
  }
}

//  Record {
//    u2 attribute_name_index;
//    u4 attribute_length;
//    u2 components_count;
//    component_info components[components_count];
//  }
//  component_info {
//    u2 name_index;
//    u2 descriptor_index
//    u2 attributes_count;
//    attribute_info_attributes[attributes_count];
//  }
void JvmtiClassFileReconstituter::write_record_attribute() {
  Array<RecordComponent*>* components = ik()->record_components();
  int number_of_components = components->length();

  // Each component has a u2 for name, descr, attribute count
  int length = sizeof(u2) + (sizeof(u2) * 3 * number_of_components);
  for (int x = 0; x < number_of_components; x++) {
    RecordComponent* component = components->at(x);
    if (component->generic_signature_index() != 0) {
      length += 8; // Signature attribute size
      assert(component->attributes_count() > 0, "Bad component attributes count");
    }
    if (component->annotations() != NULL) {
      length += 6 + component->annotations()->length();
    }
    if (component->type_annotations() != NULL) {
      length += 6 + component->type_annotations()->length();
    }
  }

  write_attribute_name_index("Record");
  write_u4(length);
  write_u2(number_of_components);
  for (int i = 0; i < number_of_components; i++) {
    RecordComponent* component = components->at(i);
    write_u2(component->name_index());
    write_u2(component->descriptor_index());
    write_u2(component->attributes_count());
    if (component->generic_signature_index() != 0) {
      write_signature_attribute(component->generic_signature_index());
    }
    if (component->annotations() != NULL) {
      write_annotations_attribute("RuntimeVisibleAnnotations", component->annotations());
    }
    if (component->type_annotations() != NULL) {
      write_annotations_attribute("RuntimeVisibleTypeAnnotations", component->type_annotations());
    }
  }
}

// Write InnerClasses attribute
// JVMSpec|   InnerClasses_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 number_of_classes;
// JVMSpec|     {  u2 inner_class_info_index;
// JVMSpec|        u2 outer_class_info_index;
// JVMSpec|        u2 inner_name_index;
// JVMSpec|        u2 inner_class_access_flags;
// JVMSpec|     } classes[number_of_classes];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_inner_classes_attribute(int length) {
  InnerClassesIterator iter(ik());
  guarantee(iter.length() != 0 && iter.length() == length,
            "caller must check");
  u2 entry_count = length / InstanceKlass::inner_class_next_offset;
  u4 size = 2 + entry_count * (2+2+2+2);

  write_attribute_name_index("InnerClasses");
  write_u4(size);
  write_u2(entry_count);
  for (; !iter.done(); iter.next()) {
    write_u2(iter.inner_class_info_index());
    write_u2(iter.outer_class_info_index());
    write_u2(iter.inner_name_index());
    write_u2(iter.inner_access_flags());
  }
}

// Write Synthetic attribute
// JVMSpec|   Synthetic_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_synthetic_attribute() {
  write_attribute_name_index("Synthetic");
  write_u4(0); //length always zero
}

// Compute size of LineNumberTable
u2 JvmtiClassFileReconstituter::line_number_table_entries(const methodHandle& method) {
  // The line number table is compressed so we don't know how big it is until decompressed.
  // Decompression is really fast so we just do it twice.
  u2 num_entries = 0;
  CompressedLineNumberReadStream stream(method->compressed_linenumber_table());
  while (stream.read_pair()) {
    num_entries++;
  }
  return num_entries;
}

// Write LineNumberTable attribute
// JVMSpec|   LineNumberTable_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 line_number_table_length;
// JVMSpec|     {  u2 start_pc;
// JVMSpec|        u2 line_number;
// JVMSpec|     } line_number_table[line_number_table_length];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_line_number_table_attribute(const methodHandle& method,
                                                                    u2 num_entries) {

  write_attribute_name_index("LineNumberTable");
  write_u4(2 + num_entries * (2 + 2));
  write_u2(num_entries);

  CompressedLineNumberReadStream stream(method->compressed_linenumber_table());
  while (stream.read_pair()) {
    write_u2(stream.bci());
    write_u2(stream.line());
  }
}

// Write LocalVariableTable attribute
// JVMSpec|   LocalVariableTable_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 local_variable_table_length;
// JVMSpec|     {  u2 start_pc;
// JVMSpec|       u2 length;
// JVMSpec|       u2 name_index;
// JVMSpec|       u2 descriptor_index;
// JVMSpec|       u2 index;
// JVMSpec|     } local_variable_table[local_variable_table_length];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_local_variable_table_attribute(const methodHandle& method, u2 num_entries) {
    write_attribute_name_index("LocalVariableTable");
    write_u4(2 + num_entries * (2 + 2 + 2 + 2 + 2));
    write_u2(num_entries);

    assert(method->localvariable_table_length() == num_entries, "just checking");

    LocalVariableTableElement *elem = method->localvariable_table_start();
    for (int j=0; j<method->localvariable_table_length(); j++) {
      write_u2(elem->start_bci);
      write_u2(elem->length);
      write_u2(elem->name_cp_index);
      write_u2(elem->descriptor_cp_index);
      write_u2(elem->slot);
      elem++;
    }
}

// Write LocalVariableTypeTable attribute
// JVMSpec|   LocalVariableTypeTable_attribute {
// JVMSpec|     u2 attribute_name_index;
// JVMSpec|     u4 attribute_length;
// JVMSpec|     u2 local_variable_type_table_length;
// JVMSpec|     { u2 start_pc;
// JVMSpec|       u2 length;
// JVMSpec|       u2 name_index;
// JVMSpec|       u2 signature_index;
// JVMSpec|       u2 index;
// JVMSpec|     } local_variable_type_table[local_variable_type_table_length];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_local_variable_type_table_attribute(const methodHandle& method, u2 num_entries) {
    write_attribute_name_index("LocalVariableTypeTable");
    write_u4(2 + num_entries * (2 + 2 + 2 + 2 + 2));
    write_u2(num_entries);

    LocalVariableTableElement *elem = method->localvariable_table_start();
    for (int j=0; j<method->localvariable_table_length(); j++) {
      if (elem->signature_cp_index > 0) {
        // Local variable has a generic signature - write LVTT attribute entry
        write_u2(elem->start_bci);
        write_u2(elem->length);
        write_u2(elem->name_cp_index);
        write_u2(elem->signature_cp_index);
        write_u2(elem->slot);
        num_entries--;
      }
      elem++;
    }
    assert(num_entries == 0, "just checking");
}

// Write stack map table attribute
// JSR-202|   StackMapTable_attribute {
// JSR-202|     u2 attribute_name_index;
// JSR-202|     u4 attribute_length;
// JSR-202|     u2 number_of_entries;
// JSR-202|     stack_map_frame_entries[number_of_entries];
// JSR-202|   }
void JvmtiClassFileReconstituter::write_stackmap_table_attribute(const methodHandle& method,
                                                                 int stackmap_len) {

  write_attribute_name_index("StackMapTable");
  write_u4(stackmap_len);
  memcpy(
    writeable_address(stackmap_len),
    (void*)(method->stackmap_data()->adr_at(0)),
    stackmap_len);
}

// Write one method_info structure
// JVMSpec|   method_info {
// JVMSpec|     u2 access_flags;
// JVMSpec|     u2 name_index;
// JVMSpec|     u2 descriptor_index;
// JVMSpec|     u2 attributes_count;
// JVMSpec|     attribute_info attributes[attributes_count];
// JVMSpec|   }
void JvmtiClassFileReconstituter::write_method_info(const methodHandle& method) {
  AccessFlags access_flags = method->access_flags();
  ConstMethod* const_method = method->constMethod();
  u2 generic_signature_index = const_method->generic_signature_index();
  AnnotationArray* anno = method->annotations();
  AnnotationArray* param_anno = method->parameter_annotations();
  AnnotationArray* default_anno = method->annotation_default();
  AnnotationArray* type_anno = method->type_annotations();

  // skip generated default interface methods
  if (method->is_overpass()) {
    return;
  }

  write_u2(access_flags.get_flags() & JVM_RECOGNIZED_METHOD_MODIFIERS);
  write_u2(const_method->name_index());
  write_u2(const_method->signature_index());

  // write attributes in the same order javac does, so we can test with byte for
  // byte comparison
  int attr_count = 0;
  if (const_method->code_size() != 0) {
    ++attr_count;     // has Code attribute
  }
  if (const_method->has_checked_exceptions()) {
    ++attr_count;     // has Exceptions attribute
  }
  if (default_anno != NULL) {
    ++attr_count;     // has AnnotationDefault attribute
  }
  // Deprecated attribute would go here
  if (access_flags.is_synthetic()) { // FIXME
    // ++attr_count;
  }
  if (generic_signature_index != 0) {
    ++attr_count;
  }
  if (anno != NULL) {
    ++attr_count;     // has RuntimeVisibleAnnotations attribute
  }
  if (param_anno != NULL) {
    ++attr_count;     // has RuntimeVisibleParameterAnnotations attribute
  }
  if (type_anno != NULL) {
    ++attr_count;     // has RuntimeVisibleTypeAnnotations attribute
  }

  write_u2(attr_count);
  if (const_method->code_size() > 0) {
    write_code_attribute(method);
  }
  if (const_method->has_checked_exceptions()) {
    write_exceptions_attribute(const_method);
  }
  if (default_anno != NULL) {
    write_annotations_attribute("AnnotationDefault", default_anno);
  }
  // Deprecated attribute would go here
  if (access_flags.is_synthetic()) {
    // write_synthetic_attribute();
  }
  if (generic_signature_index != 0) {
    write_signature_attribute(generic_signature_index);
  }
  if (anno != NULL) {
    write_annotations_attribute("RuntimeVisibleAnnotations", anno);
  }
  if (param_anno != NULL) {
    write_annotations_attribute("RuntimeVisibleParameterAnnotations", param_anno);
  }
  if (type_anno != NULL) {
    write_annotations_attribute("RuntimeVisibleTypeAnnotations", type_anno);
  }
}

// Write the class attributes portion of ClassFile structure
// JVMSpec|     u2 attributes_count;
// JVMSpec|     attribute_info attributes[attributes_count];
void JvmtiClassFileReconstituter::write_class_attributes() {
  u2 inner_classes_length = inner_classes_attribute_length();
  Symbol* generic_signature = ik()->generic_signature();
  AnnotationArray* anno = ik()->class_annotations();
  AnnotationArray* type_anno = ik()->class_type_annotations();

  int attr_count = 0;
  if (generic_signature != NULL) {
    ++attr_count;
  }
  if (ik()->source_file_name() != NULL) {
    ++attr_count;
  }
  if (ik()->source_debug_extension() != NULL) {
    ++attr_count;
  }
  if (inner_classes_length > 0) {
    ++attr_count;
  }
  if (anno != NULL) {
    ++attr_count;     // has RuntimeVisibleAnnotations attribute
  }
  if (type_anno != NULL) {
    ++attr_count;     // has RuntimeVisibleTypeAnnotations attribute
  }
  if (cpool()->operands() != NULL) {
    ++attr_count;
  }
  if (ik()->nest_host_index() != 0) {
    ++attr_count;
  }
  if (ik()->nest_members() != Universe::the_empty_short_array()) {
    ++attr_count;
  }
  if (ik()->permitted_subclasses() != Universe::the_empty_short_array()) {
    ++attr_count;
  }
  if (ik()->record_components() != NULL) {
    ++attr_count;
  }

  write_u2(attr_count);

  if (generic_signature != NULL) {
    write_signature_attribute(symbol_to_cpool_index(generic_signature));
  }
  if (ik()->source_file_name() != NULL) {
    write_source_file_attribute();
  }
  if (ik()->source_debug_extension() != NULL) {
    write_source_debug_extension_attribute();
  }
  if (anno != NULL) {
    write_annotations_attribute("RuntimeVisibleAnnotations", anno);
  }
  if (type_anno != NULL) {
    write_annotations_attribute("RuntimeVisibleTypeAnnotations", type_anno);
  }
  if (ik()->nest_host_index() != 0) {
    write_nest_host_attribute();
  }
  if (ik()->nest_members() != Universe::the_empty_short_array()) {
    write_nest_members_attribute();
  }
  if (ik()->permitted_subclasses() != Universe::the_empty_short_array()) {
    write_permitted_subclasses_attribute();
  }
  if (ik()->record_components() != NULL) {
    write_record_attribute();
  }
  if (cpool()->operands() != NULL) {
    write_bootstrapmethod_attribute();
  }
  if (inner_classes_length > 0) {
    write_inner_classes_attribute(inner_classes_length);
  }
}

// Write the method information portion of ClassFile structure
// JVMSpec|     u2 methods_count;
// JVMSpec|     method_info methods[methods_count];
void JvmtiClassFileReconstituter::write_method_infos() {
  HandleMark hm(thread());
  Array<Method*>* methods = ik()->methods();
  int num_methods = methods->length();
  int num_overpass = 0;

  // count the generated default interface methods
  // these will not be re-created by write_method_info
  // and should not be included in the total count
  for (int index = 0; index < num_methods; index++) {
    Method* method = methods->at(index);
    if (method->is_overpass()) {
      num_overpass++;
    }
  }

  write_u2(num_methods - num_overpass);
  if (JvmtiExport::can_maintain_original_method_order()) {
    int index;
    int original_index;
    intArray method_order(num_methods, num_methods, 0);

    // invert the method order mapping
    for (index = 0; index < num_methods; index++) {
      original_index = ik()->method_ordering()->at(index);
      assert(original_index >= 0 && original_index < num_methods,
             "invalid original method index");
      method_order.at_put(original_index, index);
    }

    // write in original order
    for (original_index = 0; original_index < num_methods; original_index++) {
      index = method_order.at(original_index);
      methodHandle method(thread(), methods->at(index));
      write_method_info(method);
    }
  } else {
    // method order not preserved just dump the method infos
    for (int index = 0; index < num_methods; index++) {
      methodHandle method(thread(), methods->at(index));
      write_method_info(method);
    }
  }
}

void JvmtiClassFileReconstituter::write_class_file_format() {
  ReallocMark();

  // JVMSpec|   ClassFile {
  // JVMSpec|           u4 magic;
  write_u4(0xCAFEBABE);

  // JVMSpec|           u2 minor_version;
  // JVMSpec|           u2 major_version;
  write_u2(ik()->minor_version());
  u2 major = ik()->major_version();
  write_u2(major);

  // JVMSpec|           u2 constant_pool_count;
  // JVMSpec|           cp_info constant_pool[constant_pool_count-1];
  write_u2(cpool()->length());
  copy_cpool_bytes(writeable_address(cpool_size()));

  // JVMSpec|           u2 access_flags;
  write_u2(ik()->access_flags().get_flags() & JVM_RECOGNIZED_CLASS_MODIFIERS);

  // JVMSpec|           u2 this_class;
  // JVMSpec|           u2 super_class;
  write_u2(class_symbol_to_cpool_index(ik()->name()));
  Klass* super_class = ik()->super();
  write_u2(super_class == NULL? 0 :  // zero for java.lang.Object
                class_symbol_to_cpool_index(super_class->name()));

  // JVMSpec|           u2 interfaces_count;
  // JVMSpec|           u2 interfaces[interfaces_count];
  Array<InstanceKlass*>* interfaces =  ik()->local_interfaces();
  int num_interfaces = interfaces->length();
  write_u2(num_interfaces);
  for (int index = 0; index < num_interfaces; index++) {
    HandleMark hm(thread());
    InstanceKlass* iik = interfaces->at(index);
    write_u2(class_symbol_to_cpool_index(iik->name()));
  }

  // JVMSpec|           u2 fields_count;
  // JVMSpec|           field_info fields[fields_count];
  write_field_infos();

  // JVMSpec|           u2 methods_count;
  // JVMSpec|           method_info methods[methods_count];
  write_method_infos();

  // JVMSpec|           u2 attributes_count;
  // JVMSpec|           attribute_info attributes[attributes_count];
  // JVMSpec|   } /* end ClassFile 8?
  write_class_attributes();
}

address JvmtiClassFileReconstituter::writeable_address(size_t size) {
  size_t used_size = _buffer_ptr - _buffer;
  if (size + used_size >= _buffer_size) {
    // compute the new buffer size: must be at least twice as big as before
    // plus whatever new is being used; then convert to nice clean block boundary
    size_t new_buffer_size = (size + _buffer_size*2 + 1) / initial_buffer_size
                                                         * initial_buffer_size;

    // VM goes belly-up if the memory isn't available, so cannot do OOM processing
    _buffer = REALLOC_RESOURCE_ARRAY(u1, _buffer, _buffer_size, new_buffer_size);
    _buffer_size = new_buffer_size;
    _buffer_ptr = _buffer + used_size;
  }
  u1* ret_ptr = _buffer_ptr;
  _buffer_ptr += size;
  return ret_ptr;
}

void JvmtiClassFileReconstituter::write_attribute_name_index(const char* name) {
  TempNewSymbol sym = SymbolTable::probe(name, (int)strlen(name));
  assert(sym != NULL, "attribute name symbol not found");
  u2 attr_name_index = symbol_to_cpool_index(sym);
  assert(attr_name_index != 0, "attribute name symbol not in constant pool");
  write_u2(attr_name_index);
}

void JvmtiClassFileReconstituter::write_u1(u1 x) {
  *writeable_address(1) = x;
}

void JvmtiClassFileReconstituter::write_u2(u2 x) {
  Bytes::put_Java_u2(writeable_address(2), x);
}

void JvmtiClassFileReconstituter::write_u4(u4 x) {
  Bytes::put_Java_u4(writeable_address(4), x);
}

void JvmtiClassFileReconstituter::write_u8(u8 x) {
  Bytes::put_Java_u8(writeable_address(8), x);
}

void JvmtiClassFileReconstituter::copy_bytecodes(const methodHandle& mh,
                                                 unsigned char* bytecodes) {
  // use a BytecodeStream to iterate over the bytecodes. JVM/fast bytecodes
  // and the breakpoint bytecode are converted to their original bytecodes.

  BytecodeStream bs(mh);

  unsigned char* p = bytecodes;
  Bytecodes::Code code;
  bool is_rewritten = mh->method_holder()->is_rewritten();

  while ((code = bs.next()) >= 0) {
    assert(Bytecodes::is_java_code(code), "sanity check");
    assert(code != Bytecodes::_breakpoint, "sanity check");

    // length of bytecode (mnemonic + operands)
    address bcp = bs.bcp();
    int     len = bs.instruction_size();
    assert(len > 0, "length must be > 0");

    // copy the bytecodes
    *p = (unsigned char) (bs.is_wide()? Bytecodes::_wide : code);
    if (len > 1) {
      memcpy(p+1, bcp+1, len-1);
    }

    // During linking the get/put and invoke instructions are rewritten
    // with an index into the constant pool cache. The original constant
    // pool index must be returned to caller.  Rewrite the index.
    if (is_rewritten && len > 1) {
      bool is_wide = false;
      switch (code) {
      case Bytecodes::_getstatic       :  // fall through
      case Bytecodes::_putstatic       :  // fall through
      case Bytecodes::_getfield        :  // fall through
      case Bytecodes::_putfield        :  // fall through
      case Bytecodes::_invokevirtual   :  // fall through
      case Bytecodes::_invokespecial   :  // fall through
      case Bytecodes::_invokestatic    :  // fall through
      case Bytecodes::_invokedynamic   :  // fall through
      case Bytecodes::_invokeinterface : {
        assert(len == 3 ||
               (code == Bytecodes::_invokeinterface && len == 5) ||
               (code == Bytecodes::_invokedynamic   && len == 5),
               "sanity check");

        int cpci = Bytes::get_native_u2(bcp+1);
        bool is_invokedynamic = (code == Bytecodes::_invokedynamic);
        ConstantPoolCacheEntry* entry;
        if (is_invokedynamic) {
          cpci = Bytes::get_native_u4(bcp+1);
          entry = mh->constants()->invokedynamic_cp_cache_entry_at(cpci);
        } else {
        // cache cannot be pre-fetched since some classes won't have it yet
          entry = mh->constants()->cache()->entry_at(cpci);
        }
        int i = entry->constant_pool_index();
        assert(i < mh->constants()->length(), "sanity check");
        Bytes::put_Java_u2((address)(p+1), (u2)i);     // java byte ordering
        if (is_invokedynamic)  *(p+3) = *(p+4) = 0;
        break;
      }
      case Bytecodes::_ldc_w:
        is_wide = true; // fall through
      case Bytecodes::_ldc: {
        if (bs.raw_code() == Bytecodes::_fast_aldc || bs.raw_code() == Bytecodes::_fast_aldc_w) {
          int cpci = is_wide ? Bytes::get_native_u2(bcp+1) : (u1)(*(bcp+1));
          int i = mh->constants()->object_to_cp_index(cpci);
          assert(i < mh->constants()->length(), "sanity check");
          if (is_wide) {
            Bytes::put_Java_u2((address)(p+1), (u2)i);     // java byte ordering
          } else {
            *(p+1) = (u1)i;
          }
        }
        break;
        }
      default:
        break;
      }
    }

    p += len;
  }
}
