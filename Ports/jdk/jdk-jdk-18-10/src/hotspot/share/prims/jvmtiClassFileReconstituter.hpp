/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_PRIMS_JVMTICLASSFILERECONSTITUTER_HPP
#define SHARE_PRIMS_JVMTICLASSFILERECONSTITUTER_HPP

#include "jvmtifiles/jvmtiEnv.hpp"


class JvmtiConstantPoolReconstituter : public StackObj {
 private:
  int                  _cpool_size;
  SymbolHashMap*       _symmap;
  SymbolHashMap*       _classmap;
  constantPoolHandle   _cpool;
  InstanceKlass*       _ik;
  jvmtiError           _err;

 protected:
  InstanceKlass*  ik()           { return _ik; };
  constantPoolHandle   cpool()   { return _cpool; };

  u2 symbol_to_cpool_index(Symbol* sym) {
    return _symmap->symbol_to_value(sym);
  }

  u2 class_symbol_to_cpool_index(Symbol* sym) {
    return _classmap->symbol_to_value(sym);
  }

 public:
  // Calls to this constructor must be proceeded by a ResourceMark
  // and a HandleMark
  JvmtiConstantPoolReconstituter(InstanceKlass* ik);

  ~JvmtiConstantPoolReconstituter() {
    if (_symmap != NULL) {
      delete _symmap;
      _symmap = NULL;
    }
    if (_classmap != NULL) {
      delete _classmap;
      _classmap = NULL;
    }
  }


  void       set_error(jvmtiError err)    { _err = err; }
  jvmtiError get_error()                  { return _err; }

  int cpool_size()                        { return _cpool_size; }

  void copy_cpool_bytes(unsigned char *cpool_bytes) {
    if (cpool_bytes == NULL) {
      assert(cpool_bytes != NULL, "cpool_bytes pointer must not be NULL");
      return;
    }
    cpool()->copy_cpool_bytes(cpool_size(), _symmap, cpool_bytes);
  }
};


class JvmtiClassFileReconstituter : public JvmtiConstantPoolReconstituter {
 private:
  size_t               _buffer_size;
  u1*                  _buffer;
  u1*                  _buffer_ptr;
  Thread*              _thread;

  enum {
    // initial size should be power of two
    initial_buffer_size = 1024
  };

  inline Thread* thread() { return _thread; }

  void write_class_file_format();
  void write_field_infos();
  void write_method_infos();
  void write_method_info(const methodHandle& method);
  void write_code_attribute(const methodHandle& method);
  void write_exceptions_attribute(ConstMethod* const_method);
  void write_synthetic_attribute();
  void write_class_attributes();
  void write_source_file_attribute();
  void write_source_debug_extension_attribute();
  u2 line_number_table_entries(const methodHandle& method);
  void write_line_number_table_attribute(const methodHandle& method, u2 num_entries);
  void write_local_variable_table_attribute(const methodHandle& method, u2 num_entries);
  void write_local_variable_type_table_attribute(const methodHandle& method, u2 num_entries);
  void write_stackmap_table_attribute(const methodHandle& method, int stackmap_table_len);
  u2 inner_classes_attribute_length();
  void write_inner_classes_attribute(int length);
  void write_signature_attribute(u2 generic_signaure_index);
  void write_attribute_name_index(const char* name);
  void write_annotations_attribute(const char* attr_name, AnnotationArray* annos);
  void write_bootstrapmethod_attribute();
  void write_nest_host_attribute();
  void write_nest_members_attribute();
  void write_permitted_subclasses_attribute();
  void write_record_attribute();

  address writeable_address(size_t size);
  void write_u1(u1 x);
  void write_u2(u2 x);
  void write_u4(u4 x);
  void write_u8(u8 x);

 public:
  // Calls to this constructor must be proceeded by a ResourceMark
  // and a HandleMark
  JvmtiClassFileReconstituter(InstanceKlass* ik) :
                                      JvmtiConstantPoolReconstituter(ik) {
    _buffer_size = initial_buffer_size;
    _buffer = _buffer_ptr = NEW_RESOURCE_ARRAY(u1, _buffer_size);
    _thread = Thread::current();
    write_class_file_format();
  };

  size_t class_file_size()    { return _buffer_ptr - _buffer; }

  u1* class_file_bytes()      { return _buffer; }

  static void copy_bytecodes(const methodHandle& method, unsigned char* bytecodes);
};

#endif // SHARE_PRIMS_JVMTICLASSFILERECONSTITUTER_HPP
