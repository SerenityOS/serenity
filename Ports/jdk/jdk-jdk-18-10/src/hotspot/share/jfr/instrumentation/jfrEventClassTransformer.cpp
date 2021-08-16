/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm.h"
#include "classfile/classFileParser.hpp"
#include "classfile/classFileStream.hpp"
#include "classfile/classLoadInfo.hpp"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/moduleEntry.hpp"
#include "classfile/modules.hpp"
#include "classfile/stackMapTable.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/systemDictionary.hpp"
#include "classfile/verificationType.hpp"
#include "interpreter/bytecodes.hpp"
#include "jfr/instrumentation/jfrEventClassTransformer.hpp"
#include "jfr/jfr.hpp"
#include "jfr/jni/jfrJavaSupport.hpp"
#include "jfr/jni/jfrUpcalls.hpp"
#include "jfr/recorder/checkpoint/types/traceid/jfrTraceId.inline.hpp"
#include "jfr/support/jfrJdkJfrEvent.hpp"
#include "jfr/utilities/jfrBigEndian.hpp"
#include "jfr/writers/jfrBigEndianWriter.hpp"
#include "logging/log.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/array.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/method.hpp"
#include "prims/jvmtiRedefineClasses.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/os.hpp"
#include "runtime/thread.inline.hpp"
#include "utilities/exceptions.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

static const u2 number_of_new_methods = 5;
static const u2 number_of_new_fields = 3;
static const int extra_stream_bytes = 0x280;
static const u2 invalid_cp_index = 0;

static const char* utf8_constants[] = {
  "Code",         // 0
  "J",            // 1
  "commit",       // 2
  "eventHandler", // 3
  "duration",     // 4
  "begin",        // 5
  "()V",          // 6
  "isEnabled",    // 7
  "()Z",          // 8
  "end",          // 9
  "shouldCommit", // 10
  "startTime",    // 11 // LAST_REQUIRED_UTF8
  "Ljdk/jfr/internal/handlers/EventHandler;", // 12
  "Ljava/lang/Object;", // 13
  "<clinit>",     // 14
  "jdk/jfr/FlightRecorder", // 15
  "register",     // 16
  "(Ljava/lang/Class;)V", // 17
  "StackMapTable", // 18
  "Exceptions", // 19
  "LineNumberTable", // 20
  "LocalVariableTable", // 21
  "LocalVariableTypeTable", // 22
  "RuntimeVisibleAnnotation", // 23
};

enum utf8_req_symbols {
  UTF8_REQ_Code,
  UTF8_REQ_J_FIELD_DESC,
  UTF8_REQ_commit,
  UTF8_REQ_eventHandler,
  UTF8_REQ_duration,
  UTF8_REQ_begin,
  UTF8_REQ_EMPTY_VOID_METHOD_DESC,
  UTF8_REQ_isEnabled,
  UTF8_REQ_EMPTY_BOOLEAN_METHOD_DESC,
  UTF8_REQ_end,
  UTF8_REQ_shouldCommit,
  UTF8_REQ_startTime,
  NOF_UTF8_REQ_SYMBOLS
};

enum utf8_opt_symbols {
  UTF8_OPT_eventHandler_FIELD_DESC = NOF_UTF8_REQ_SYMBOLS,
  UTF8_OPT_LjavaLangObject,
  UTF8_OPT_clinit,
  UTF8_OPT_FlightRecorder,
  UTF8_OPT_register,
  UTF8_OPT_CLASS_VOID_METHOD_DESC,
  UTF8_OPT_StackMapTable,
  UTF8_OPT_Exceptions,
  UTF8_OPT_LineNumberTable,
  UTF8_OPT_LocalVariableTable,
  UTF8_OPT_LocalVariableTypeTable,
  UTF8_OPT_RuntimeVisibleAnnotation,
  NOF_UTF8_SYMBOLS
};

static u1 empty_void_method_code_attribute[] = {
  0x0,
  0x0,
  0x0,
  0xd, // attribute len
  0x0,
  0x0, // max stack
  0x0,
  0x1, // max locals
  0x0,
  0x0,
  0x0,
  0x1, // code length
  Bytecodes::_return,
  0x0,
  0x0, // ex table len
  0x0,
  0x0  // attributes_count
};

static u1 boolean_method_code_attribute[] = {
  0x0,
  0x0,
  0x0,
  0xe,
  0x0,
  0x1, // max stack
  0x0,
  0x1, // max locals
  0x0,
  0x0,
  0x0,
  0x2,
  Bytecodes::_iconst_0,
  Bytecodes::_ireturn,
  0x0,
  0x0, // ex table len
  0x0,
  0x0, // attributes_count
};

// annotation processing support

enum {  // initial annotation layout
  atype_off = 0,      // utf8 such as 'Ljava/lang/annotation/Retention;'
  count_off = 2,      // u2   such as 1 (one value)
  member_off = 4,     // utf8 such as 'value'
  tag_off = 6,        // u1   such as 'c' (type) or 'e' (enum)
  e_tag_val = 'e',
  e_type_off = 7,   // utf8 such as 'Ljava/lang/annotation/RetentionPolicy;'
  e_con_off = 9,    // utf8 payload, such as 'SOURCE', 'CLASS', 'RUNTIME'
  e_size = 11,     // end of 'e' annotation
  c_tag_val = 'c',    // payload is type
  c_con_off = 7,    // utf8 payload, such as 'I'
  c_size = 9,       // end of 'c' annotation
  s_tag_val = 's',    // payload is String
  s_con_off = 7,    // utf8 payload, such as 'Ljava/lang/String;'
  s_size = 9,
  min_size = 6        // smallest possible size (zero members)
};

static int skip_annotation_value(const address, int, int); // fwd decl

// Skip an annotation.  Return >=limit if there is any problem.
static int next_annotation_index(const address buffer, int limit, int index) {
  assert(buffer != NULL, "invariant");
  index += 2;  // skip atype
  if ((index += 2) >= limit) {
    return limit;
  }
  int nof_members = JfrBigEndian::read<u2>(buffer + index - 2);
  while (--nof_members >= 0 && index < limit) {
    index += 2; // skip member
    index = skip_annotation_value(buffer, limit, index);
  }
  return index;
}

// Skip an annotation value.  Return >=limit if there is any problem.
static int skip_annotation_value(const address buffer, int limit, int index) {
  assert(buffer != NULL, "invariant");
  // value := switch (tag:u1) {
  //   case B, C, I, S, Z, D, F, J, c: con:u2;
  //   case e: e_class:u2 e_name:u2;
  //   case s: s_con:u2;
  //   case [: do(nval:u2) {value};
  //   case @: annotation;
  //   case s: s_con:u2;
  // }
  if ((index += 1) >= limit) {
    return limit;
  }
  const u1 tag = buffer[index - 1];
  switch (tag) {
    case 'B':
    case 'C':
    case 'I':
    case 'S':
    case 'Z':
    case 'D':
    case 'F':
    case 'J':
    case 'c':
    case 's':
      index += 2;  // skip con or s_con
      break;
    case 'e':
      index += 4;  // skip e_class, e_name
      break;
    case '[':
      {
        if ((index += 2) >= limit) {
          return limit;
        }
        int nof_values = JfrBigEndian::read<u2>(buffer + index - 2);
        while (--nof_values >= 0 && index < limit) {
          index = skip_annotation_value(buffer, limit, index);
        }
      }
      break;
    case '@':
      index = next_annotation_index(buffer, limit, index);
      break;
    default:
      return limit;  //  bad tag byte
  }
  return index;
}

static const u2 number_of_elements_offset = (u2)2;
static const u2 element_name_offset = (u2)(number_of_elements_offset + 2);
static const u2 element_name_size = (u2)2;
static const u2 value_type_relative_offset = (u2)2;
static const u2 value_relative_offset = (u2)(value_type_relative_offset + 1);

// see JVMS - 4.7.16. The RuntimeVisibleAnnotations Attribute

class AnnotationElementIterator : public StackObj {
 private:
  const InstanceKlass* _ik;
  const address _buffer;
  const u2 _limit; // length of annotation
  mutable u2 _current; // element
  mutable u2 _next; // element
  u2 value_index() const {
    return JfrBigEndian::read<u2>(_buffer + _current + value_relative_offset);
  }

 public:
  AnnotationElementIterator(const InstanceKlass* ik, address buffer, u2 limit) : _ik(ik),
                                                                                 _buffer(buffer),
                                                                                 _limit(limit),
                                                                                 _current(element_name_offset),
                                                                                 _next(element_name_offset) {
    assert(_buffer != NULL, "invariant");
    assert(_next == element_name_offset, "invariant");
    assert(_current == element_name_offset, "invariant");
  }

  bool has_next() const {
    return _next < _limit;
  }

  void move_to_next() const {
    assert(has_next(), "invariant");
    _current = _next;
    if (_next < _limit) {
      _next = skip_annotation_value(_buffer, _limit, _next + element_name_size);
    }
    assert(_next <= _limit, "invariant");
    assert(_current <= _limit, "invariant");
  }

  u2 number_of_elements() const {
    return JfrBigEndian::read<u2>(_buffer + number_of_elements_offset);
  }

  const Symbol* name() const {
    assert(_current < _next, "invariant");
    return _ik->constants()->symbol_at(JfrBigEndian::read<u2>(_buffer + _current));
  }

  char value_type() const {
    return JfrBigEndian::read<u1>(_buffer + _current + value_type_relative_offset);
  }

  jint read_int() const {
    return _ik->constants()->int_at(value_index());
  }

  bool read_bool() const {
    return read_int() != 0;
  }
};

class AnnotationIterator : public StackObj {
 private:
  const InstanceKlass* _ik;
  // ensure _limit field is declared before _buffer
  u2 _limit; // length of annotations array
  const address _buffer;
  mutable u2 _current; // annotation
  mutable u2 _next; // annotation

 public:
  AnnotationIterator(const InstanceKlass* ik, AnnotationArray* ar) : _ik(ik),
                                                                     _limit(ar != NULL ? ar->length() : 0),
                                                                     _buffer(_limit > 2 ? ar->adr_at(2) : NULL),
                                                                     _current(0),
                                                                     _next(0) {
    if (_buffer != NULL) {
      _limit -= 2; // subtract sizeof(u2) number of annotations field
    }
  }
  bool has_next() const {
    return _next < _limit;
  }

  void move_to_next() const {
    assert(has_next(), "invariant");
    _current = _next;
    if (_next < _limit) {
      _next = next_annotation_index(_buffer, _limit, _next);
    }
    assert(_next <= _limit, "invariant");
    assert(_current <= _limit, "invariant");
  }
  const AnnotationElementIterator elements() const {
    assert(_current < _next, "invariant");
    return AnnotationElementIterator(_ik, _buffer + _current, _next - _current);
  }
  const Symbol* type() const {
    assert(_buffer != NULL, "invariant");
    assert(_current < _limit, "invariant");
    return _ik->constants()->symbol_at(JfrBigEndian::read<u2>(_buffer + _current));
  }
};

static const char value_name[] = "value";
static bool has_annotation(const InstanceKlass* ik, const Symbol* annotation_type, bool& value) {
  assert(annotation_type != NULL, "invariant");
  AnnotationArray* class_annotations = ik->class_annotations();
  if (class_annotations == NULL) {
    return false;
  }

  const AnnotationIterator annotation_iterator(ik, class_annotations);
  while (annotation_iterator.has_next()) {
    annotation_iterator.move_to_next();
    if (annotation_iterator.type() == annotation_type) {
      // target annotation found
      static const Symbol* value_symbol =
        SymbolTable::probe(value_name, sizeof value_name - 1);
      assert(value_symbol != NULL, "invariant");
      const AnnotationElementIterator element_iterator = annotation_iterator.elements();
      while (element_iterator.has_next()) {
        element_iterator.move_to_next();
        if (value_symbol == element_iterator.name()) {
          // "value" element
          assert('Z' == element_iterator.value_type(), "invariant");
          value = element_iterator.read_bool();
          return true;
        }
      }
    }
  }
  return false;
}

// Evaluate to the value of the first found Symbol* annotation type.
// Searching moves upwards in the klass hierarchy in order to support
// inherited annotations in addition to the ability to override.
static bool annotation_value(const InstanceKlass* ik, const Symbol* annotation_type, bool& value) {
  assert(ik != NULL, "invariant");
  assert(annotation_type != NULL, "invariant");
  assert(JdkJfrEvent::is_a(ik), "invariant");
  if (has_annotation(ik, annotation_type, value)) {
    return true;
  }
  InstanceKlass* const super = InstanceKlass::cast(ik->super());
  return super != NULL && JdkJfrEvent::is_a(super) ? annotation_value(super, annotation_type, value) : false;
}

static const char jdk_jfr_module_name[] = "jdk.jfr";

static bool java_base_can_read_jdk_jfr() {
  static bool can_read = false;
  if (can_read) {
    return true;
  }
  static Symbol* jdk_jfr_module_symbol = NULL;
  if (jdk_jfr_module_symbol == NULL) {
    jdk_jfr_module_symbol = SymbolTable::probe(jdk_jfr_module_name, sizeof jdk_jfr_module_name - 1);
    if (jdk_jfr_module_symbol == NULL) {
      return false;
    }
  }
  assert(jdk_jfr_module_symbol != NULL, "invariant");
  ModuleEntryTable* const table = Modules::get_module_entry_table(Handle());
  assert(table != NULL, "invariant");
  const ModuleEntry* const java_base_module = table->javabase_moduleEntry();
  if (java_base_module == NULL) {
    return false;
  }
  assert(java_base_module != NULL, "invariant");
  ModuleEntry* const jdk_jfr_module = table->lookup_only(jdk_jfr_module_symbol);
  if (jdk_jfr_module == NULL) {
    return false;
  }
  assert(jdk_jfr_module != NULL, "invariant");
  if (java_base_module->can_read(jdk_jfr_module)) {
    can_read = true;
  }
  return can_read;
}

static const char registered_constant[] = "Ljdk/jfr/Registered;";

// Evaluate to the value of the first found "Ljdk/jfr/Registered;" annotation.
// Searching moves upwards in the klass hierarchy in order to support
// inherited annotations in addition to the ability to override.
static bool should_register_klass(const InstanceKlass* ik, bool& untypedEventHandler) {
  assert(ik != NULL, "invariant");
  assert(JdkJfrEvent::is_a(ik), "invariant");
  assert(!untypedEventHandler, "invariant");
  static const Symbol* registered_symbol = NULL;
  if (registered_symbol == NULL) {
    registered_symbol = SymbolTable::probe(registered_constant, sizeof registered_constant - 1);
    if (registered_symbol == NULL) {
      untypedEventHandler = true;
      return false;
    }
  }
  assert(registered_symbol != NULL, "invariant");
  bool value = false; // to be set by annotation_value
  untypedEventHandler = !(annotation_value(ik, registered_symbol, value) || java_base_can_read_jdk_jfr());
  return value;
}

/*
 * Map an utf8 constant back to its CONSTANT_UTF8_INFO
 */
static u2 utf8_info_index(const InstanceKlass* ik, const Symbol* const target, TRAPS) {
  assert(target != NULL, "invariant");
  const ConstantPool* cp = ik->constants();
  const int cp_len = cp->length();
  for (u2 index = 1; index < cp_len; ++index) {
    const constantTag tag = cp->tag_at(index);
    if (tag.is_utf8()) {
      const Symbol* const utf8_sym = cp->symbol_at(index);
      assert(utf8_sym != NULL, "invariant");
      if (utf8_sym == target) {
        return index;
      }
    }
  }
  // not in constant pool
  return invalid_cp_index;
}

#ifdef ASSERT
static bool is_index_within_range(u2 index, u2 orig_cp_len, u2 new_cp_entries_len) {
  return index > 0 && index < orig_cp_len + new_cp_entries_len;
}
#endif

static u2 add_utf8_info(JfrBigEndianWriter& writer, const char* utf8_constant, u2 orig_cp_len, u2& new_cp_entries_len) {
  assert(utf8_constant != NULL, "invariant");
  writer.write<u1>(JVM_CONSTANT_Utf8);
  writer.write_utf8_u2_len(utf8_constant);
  assert(writer.is_valid(), "invariant");
  // return index for the added utf8 info
  return orig_cp_len + new_cp_entries_len++;
}

static u2 add_method_ref_info(JfrBigEndianWriter& writer,
                              u2 cls_name_index,
                              u2 method_index,
                              u2 desc_index,
                              u2 orig_cp_len,
                              u2& number_of_new_constants,
                              TRAPS) {
  assert(cls_name_index != invalid_cp_index, "invariant");
  assert(method_index != invalid_cp_index, "invariant");
  assert(desc_index != invalid_cp_index, "invariant");
  assert(is_index_within_range(cls_name_index, orig_cp_len, number_of_new_constants), "invariant");
  assert(is_index_within_range(method_index, orig_cp_len, number_of_new_constants), "invariant");
  assert(is_index_within_range(desc_index, orig_cp_len, number_of_new_constants), "invariant");
  writer.write<u1>(JVM_CONSTANT_Class);
  writer.write<u2>(cls_name_index);
  const u2 cls_entry_index = orig_cp_len + number_of_new_constants;
  ++number_of_new_constants;
  writer.write<u1>(JVM_CONSTANT_NameAndType);
  writer.write<u2>(method_index);
  writer.write<u2>(desc_index);
  const u2 nat_entry_index = orig_cp_len + number_of_new_constants;
  ++number_of_new_constants;
  writer.write<u1>(JVM_CONSTANT_Methodref);
  writer.write<u2>(cls_entry_index);
  writer.write<u2>(nat_entry_index);
  // post-increment number_of_new_constants
  // value returned is the index to the added method_ref
  return orig_cp_len + number_of_new_constants++;
}

static u2 add_flr_register_method_constants(JfrBigEndianWriter& writer,
                                            const u2* utf8_indexes,
                                            u2 orig_cp_len,
                                            u2& number_of_new_constants,
                                            TRAPS) {
  assert(utf8_indexes != NULL, "invariant");
  return add_method_ref_info(writer,
                             utf8_indexes[UTF8_OPT_FlightRecorder],
                             utf8_indexes[UTF8_OPT_register],
                             utf8_indexes[UTF8_OPT_CLASS_VOID_METHOD_DESC],
                             orig_cp_len,
                             number_of_new_constants,
                             THREAD);
}

/*
 * field_info {
 *   u2             access_flags;
 *   u2             name_index;
 *   u2             descriptor_index;
 *   u2             attributes_count;
 *   attribute_info attributes[attributes_count];
 * }
 */
static jlong add_field_info(JfrBigEndianWriter& writer, u2 name_index, u2 desc_index, bool is_static = false) {
  assert(name_index != invalid_cp_index, "invariant");
  assert(desc_index != invalid_cp_index, "invariant");
  DEBUG_ONLY(const jlong start_offset = writer.current_offset();)
  writer.write<u2>(JVM_ACC_SYNTHETIC | JVM_ACC_PRIVATE | (is_static ? JVM_ACC_STATIC : JVM_ACC_TRANSIENT)); // flags
  writer.write(name_index);
  writer.write(desc_index);
  writer.write((u2)0x0); // attributes_count
  assert(writer.is_valid(), "invariant");
  DEBUG_ONLY(assert(start_offset + 8 == writer.current_offset(), "invariant");)
  return writer.current_offset();
}

static u2 add_field_infos(JfrBigEndianWriter& writer, const u2* utf8_indexes, bool untypedEventHandler) {
  assert(utf8_indexes != NULL, "invariant");
  add_field_info(writer,
                 utf8_indexes[UTF8_REQ_eventHandler],
                 untypedEventHandler ? utf8_indexes[UTF8_OPT_LjavaLangObject] : utf8_indexes[UTF8_OPT_eventHandler_FIELD_DESC],
                 true); // static

  add_field_info(writer,
                 utf8_indexes[UTF8_REQ_startTime],
                 utf8_indexes[UTF8_REQ_J_FIELD_DESC]);

  add_field_info(writer,
                 utf8_indexes[UTF8_REQ_duration],
                 utf8_indexes[UTF8_REQ_J_FIELD_DESC]);

  return number_of_new_fields;
}

/*
 * method_info {
 *  u2             access_flags;
 *  u2             name_index;
 *  u2             descriptor_index;
 *  u2             attributes_count;
 *  attribute_info attributes[attributes_count];
 * }
 *
 * Code_attribute {
 *   u2 attribute_name_index;
 *   u4 attribute_length;
 *   u2 max_stack;
 *   u2 max_locals;
 *   u4 code_length;
 *   u1 code[code_length];
 *   u2 exception_table_length;
 *   {   u2 start_pc;
 *       u2 end_pc;
 *       u2 handler_pc;
 *       u2 catch_type;
 *   } exception_table[exception_table_length];
 *   u2 attributes_count;
 *   attribute_info attributes[attributes_count];
 * }
 */

static jlong add_method_info(JfrBigEndianWriter& writer,
                             u2 name_index,
                             u2 desc_index,
                             u2 code_index,
                             const u1* const code,
                             const size_t code_len) {
  assert(name_index > 0, "invariant");
  assert(desc_index > 0, "invariant");
  assert(code_index > 0, "invariant");
  DEBUG_ONLY(const jlong start_offset = writer.current_offset();)
  writer.write<u2>(JVM_ACC_SYNTHETIC | JVM_ACC_PUBLIC); // flags
  writer.write(name_index);
  writer.write(desc_index);
  writer.write<u2>(0x1); // attributes_count ; 1 for "Code" attribute
  assert(writer.is_valid(), "invariant");
  DEBUG_ONLY(assert(start_offset + 8 == writer.current_offset(), "invariant");)
  // Code attribute
  writer.write(code_index); // "Code"
  writer.write_bytes(code, code_len);
  DEBUG_ONLY(assert((start_offset + 8 + 2 + (jlong)code_len) == writer.current_offset(), "invariant");)
  return writer.current_offset();
}

/*
 * On return, the passed stream will be positioned
 * just after the constant pool section in the classfile
 * and the cp length is returned.
 *
 * Stream should come in at the start position.
 */
static u2 position_stream_after_cp(const ClassFileStream* stream) {
  assert(stream != NULL, "invariant");
  assert(stream->current_offset() == 0, "invariant");
  stream->skip_u4_fast(2);  // 8 bytes skipped
  const u2 cp_len = stream->get_u2_fast();
  assert(cp_len > 0, "invariant");
  // now spin the stream position to just after the constant pool
  for (u2 index = 1; index < cp_len; ++index) {
    const u1 tag = stream->get_u1_fast(); // cp tag
    switch (tag) {
      case JVM_CONSTANT_Class:
      case JVM_CONSTANT_String: {
        stream->skip_u2_fast(1); // skip 2 bytes
        continue;
      }
      case JVM_CONSTANT_Fieldref:
      case JVM_CONSTANT_Methodref:
      case JVM_CONSTANT_InterfaceMethodref:
      case JVM_CONSTANT_Integer:
      case JVM_CONSTANT_Float:
      case JVM_CONSTANT_NameAndType:
      case JVM_CONSTANT_InvokeDynamic: {
        stream->skip_u4_fast(1); // skip 4 bytes
        continue;
      }
      case JVM_CONSTANT_Long:
      case JVM_CONSTANT_Double: {
        stream->skip_u4_fast(2); // skip 8 bytes
        // Skip entry following eigth-byte constant, see JVM book p. 98
        ++index;
        continue;
      }
      case JVM_CONSTANT_Utf8: {
        u2 utf8_length = stream->get_u2_fast();
        stream->skip_u1_fast(utf8_length); // skip 2 + len bytes
        continue;
      }
      case JVM_CONSTANT_MethodHandle:
      case JVM_CONSTANT_MethodType: {
        if (tag == JVM_CONSTANT_MethodHandle) {
          stream->skip_u1_fast(1);
          stream->skip_u2_fast(1); // skip 3 bytes
        }
        else if (tag == JVM_CONSTANT_MethodType) {
          stream->skip_u2_fast(1); // skip 3 bytes
        }
      }
      continue;
      case JVM_CONSTANT_Dynamic:
        stream->skip_u2_fast(1);
        stream->skip_u2_fast(1);
      continue;
      default:
        assert(false, "error in skip logic!");
        break;
    } // end switch(tag)
  }
  return cp_len;
}

/*
* On return, the passed stream will be positioned
* just after the fields section in the classfile
* and the number of fields will be returned.
*
* Stream should come in positioned just before fields_count
*/
static u2 position_stream_after_fields(const ClassFileStream* stream) {
  assert(stream != NULL, "invariant");
  assert(stream->current_offset() > 0, "invariant");
  // fields len
  const u2 orig_fields_len = stream->get_u2_fast();
  // fields
  for (u2 i = 0; i < orig_fields_len; ++i) {
    stream->skip_u2_fast(3);
    const u2 attrib_info_len = stream->get_u2_fast();
    for (u2 j = 0; j < attrib_info_len; ++j) {
      stream->skip_u2_fast(1);
      const u4 attrib_len = stream->get_u4_fast();
      stream->skip_u1_fast(attrib_len);
    }
  }
  return orig_fields_len;
}

/*
* On return, the passed stream will be positioned
* just after the methods section in the classfile
* and the number of methods will be returned.
*
* Stream should come in positioned just before methods_count
*/
static u2 position_stream_after_methods(JfrBigEndianWriter& writer,
                                        const ClassFileStream* stream,
                                        const u2* utf8_indexes,
                                        bool register_klass,
                                        const Method* clinit_method,
                                        u4& orig_method_len_offset) {
  assert(stream != NULL, "invariant");
  assert(stream->current_offset() > 0, "invariant");
  assert(utf8_indexes != NULL, "invariant");
  // We will come back to this location when we
  // know how many methods there will be.
  writer.reserve(sizeof(u2));
  const u2 orig_methods_len = stream->get_u2_fast();
  // Move copy position past original method_count
  // in order to not copy the original count
  orig_method_len_offset += sizeof(u2);
  for (u2 i = 0; i < orig_methods_len; ++i) {
    const u4 method_offset = stream->current_offset();
    stream->skip_u2_fast(1); // Access Flags
    const u2 name_index = stream->get_u2_fast(); // Name index
    stream->skip_u2_fast(1); // Descriptor index
    const u2 attributes_count = stream->get_u2_fast();
    for (u2 j = 0; j < attributes_count; ++j) {
      stream->skip_u2_fast(1);
      const u4 attrib_len = stream->get_u4_fast();
      stream->skip_u1_fast(attrib_len);
    }
    if (clinit_method != NULL && name_index == clinit_method->name_index()) {
      // The method just parsed is an existing <clinit> method.
      // If the class has the @Registered(false) annotation, i.e. marking a class
      // for opting out from automatic registration, then we do not need to do anything.
      if (!register_klass) {
        continue;
      }
      // Automatic registration with the jfr system is acccomplished
      // by pre-pending code to the <clinit> method of the class.
      // We will need to re-create a new <clinit> in a later step.
      // For now, ensure that this method is excluded from the methods
      // being copied.
      writer.write_bytes(stream->buffer() + orig_method_len_offset,
                         method_offset - orig_method_len_offset);
      assert(writer.is_valid(), "invariant");

      // Update copy position to skip copy of <clinit> method
      orig_method_len_offset = stream->current_offset();
    }
  }
  return orig_methods_len;
}

static u2 add_method_infos(JfrBigEndianWriter& writer, const u2* utf8_indexes) {
  assert(utf8_indexes != NULL, "invariant");
  add_method_info(writer,
                  utf8_indexes[UTF8_REQ_begin],
                  utf8_indexes[UTF8_REQ_EMPTY_VOID_METHOD_DESC],
                  utf8_indexes[UTF8_REQ_Code],
                  empty_void_method_code_attribute,
                  sizeof(empty_void_method_code_attribute));

  assert(writer.is_valid(), "invariant");

  add_method_info(writer,
                  utf8_indexes[UTF8_REQ_end],
                  utf8_indexes[UTF8_REQ_EMPTY_VOID_METHOD_DESC],
                  utf8_indexes[UTF8_REQ_Code],
                  empty_void_method_code_attribute,
                  sizeof(empty_void_method_code_attribute));

  assert(writer.is_valid(), "invariant");

  add_method_info(writer,
                  utf8_indexes[UTF8_REQ_commit],
                  utf8_indexes[UTF8_REQ_EMPTY_VOID_METHOD_DESC],
                  utf8_indexes[UTF8_REQ_Code],
                  empty_void_method_code_attribute,
                  sizeof(empty_void_method_code_attribute));

  assert(writer.is_valid(), "invariant");

  add_method_info(writer,
                  utf8_indexes[UTF8_REQ_isEnabled],
                  utf8_indexes[UTF8_REQ_EMPTY_BOOLEAN_METHOD_DESC],
                  utf8_indexes[UTF8_REQ_Code],
                  boolean_method_code_attribute,
                  sizeof(boolean_method_code_attribute));

  assert(writer.is_valid(), "invariant");

  add_method_info(writer,
                  utf8_indexes[UTF8_REQ_shouldCommit],
                  utf8_indexes[UTF8_REQ_EMPTY_BOOLEAN_METHOD_DESC],
                  utf8_indexes[UTF8_REQ_Code],
                  boolean_method_code_attribute,
                  sizeof(boolean_method_code_attribute));
  assert(writer.is_valid(), "invariant");
  return number_of_new_methods;
}

static void adjust_exception_table(JfrBigEndianWriter& writer, u2 bci_adjustment_offset, const Method* method, TRAPS) {
  const u2 ex_table_length = method != NULL ? (u2)method->exception_table_length() : 0;
  writer.write<u2>(ex_table_length); // Exception table length
  if (ex_table_length > 0) {
    assert(method != NULL, "invariant");
    const ExceptionTableElement* const ex_elements = method->exception_table_start();
    for (int i = 0; i < ex_table_length; ++i) {
      assert(ex_elements != NULL, "invariant");
      writer.write<u2>(ex_elements[i].start_pc + bci_adjustment_offset);
      writer.write<u2>(ex_elements[i].end_pc + bci_adjustment_offset);
      writer.write<u2>(ex_elements[i].handler_pc + bci_adjustment_offset);
      writer.write<u2>(ex_elements[i].catch_type_index); // no adjustment
    }
  }
}

enum StackMapFrameTypes {
  SAME_FRAME_BEGIN = 0,
  SAME_FRAME_END = 63,
  SAME_LOCALS_1_STACK_ITEM_FRAME_BEGIN = 64,
  SAME_LOCALS_1_STACK_ITEM_FRAME_END = 127,
  SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED = 247,
  CHOP_FRAME_BEGIN = 248,
  CHOP_FRAME_END = 250,
  SAME_FRAME_EXTENDED = 251,
  APPEND_FRAME_BEGIN = 252,
  APPEND_FRAME_END = 254,
  FULL_FRAME = 255
};

static void adjust_stack_map(JfrBigEndianWriter& writer,
                             Array<u1>* stack_map,
                             const u2* utf8_indexes,
                             u2 bci_adjustment_offset,
                             TRAPS) {
  assert(stack_map != NULL, "invariant");
  assert(utf8_indexes != NULL, "invariant");
  writer.write<u2>(utf8_indexes[UTF8_OPT_StackMapTable]);
  const jlong stack_map_attrib_len_offset = writer.current_offset();
  writer.reserve(sizeof(u4));
  StackMapStream stream(stack_map);
  const u2 stack_map_entries = stream.get_u2(THREAD);
  // number of entries
  writer.write<u2>(stack_map_entries); // new stack map entry added
  const u1 frame_type = stream.get_u1(THREAD);
  // SAME_FRAME and SAME_LOCALS_1_STACK_ITEM_FRAME encode
  // their offset_delta into the actual frame type itself.
  // If such a frame type is the first frame, then we transform
  // it to a SAME_FRAME_EXTENDED or a SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED frame.
  // This is done in order to not overflow frame types accidentally
  // when adjusting the offset_delta. In changing the frame types,
  // we can work with an explicit u2 offset_delta field (like the other frame types)
  if (frame_type <= SAME_FRAME_END) {
    writer.write<u1>(SAME_FRAME_EXTENDED);
    writer.write<u2>(frame_type + bci_adjustment_offset);
  } else if (frame_type >= SAME_LOCALS_1_STACK_ITEM_FRAME_BEGIN &&
             frame_type <= SAME_LOCALS_1_STACK_ITEM_FRAME_END) {
    writer.write<u1>(SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED);
    writer.write<u2>((frame_type - SAME_LOCALS_1_STACK_ITEM_FRAME_BEGIN) + bci_adjustment_offset);
  } else if (frame_type >= SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
      // SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED to FULL_FRAME
      // has a u2 offset_delta field
      writer.write<u1>(frame_type);
      writer.write<u2>(stream.get_u2(THREAD) + bci_adjustment_offset);
  } else {
    assert(false, "stackMapFrame type is invalid");
  }

  while (!stream.at_end()) {
    writer.write<u1>(stream.get_u1(THREAD));
  }

  u4 stack_map_attrib_len = writer.current_offset() - stack_map_attrib_len_offset;
  // the stack_map_table_attributes_length value is exclusive
  stack_map_attrib_len -= sizeof(u4);
  writer.write_at_offset(stack_map_attrib_len, stack_map_attrib_len_offset);
}

static void adjust_line_number_table(JfrBigEndianWriter& writer,
                                     const u2* utf8_indexes,
                                     u4 bci_adjustement_offset,
                                     const Method* method,
                                     TRAPS) {
  assert(utf8_indexes != NULL, "invariant");
  assert(method != NULL, "invariant");
  assert(method->has_linenumber_table(), "invariant");
  writer.write(utf8_indexes[UTF8_OPT_LineNumberTable]);
  const jlong lnt_attributes_length_offset = writer.current_offset();
  writer.reserve(sizeof(u4));
  const jlong lnt_attributes_entries_offset = writer.current_offset();
  writer.reserve(sizeof(u2));
  u1* lnt = method->compressed_linenumber_table();
  CompressedLineNumberReadStream lnt_stream(lnt);
  u2 line_number_table_entries = 0;
  while (lnt_stream.read_pair()) {
    ++line_number_table_entries;
    const u2 bci = (u2)lnt_stream.bci();
    writer.write<u2>(bci + (u2)bci_adjustement_offset);
    writer.write<u2>((u2)lnt_stream.line());
  }
  writer.write_at_offset(line_number_table_entries, lnt_attributes_entries_offset);
  u4 lnt_table_attributes_len = writer.current_offset() - lnt_attributes_length_offset;
  // the line_number_table_attributes_length value is exclusive
  lnt_table_attributes_len -= sizeof(u4);
  writer.write_at_offset(lnt_table_attributes_len, lnt_attributes_length_offset);
}

// returns the number of lvtt entries
static u2 adjust_local_variable_table(JfrBigEndianWriter& writer,
                                      const u2* utf8_indexes,
                                      u2 bci_adjustment_offset,
                                      const Method* method,
                                      TRAPS) {
  assert(utf8_indexes != NULL, "invariant");
  assert(method != NULL, "invariant");
  assert(method->has_localvariable_table(), "invariant");
  writer.write<u2>(utf8_indexes[UTF8_OPT_LocalVariableTable]);
  const jlong lvt_attributes_length_offset = writer.current_offset();
  writer.reserve(sizeof(u4));
  const int lvt_len = method->localvariable_table_length();
  writer.write<u2>((u2)lvt_len);
  const LocalVariableTableElement* table = method->localvariable_table_start();
  assert(table != NULL, "invariant");
  u2 num_lvtt_entries = 0;
  for (int i = 0; i < lvt_len; ++i) {
    writer.write<u2>(table[i].start_bci + bci_adjustment_offset);
    writer.write<u2>(table[i].length);
    writer.write<u2>(table[i].name_cp_index);
    writer.write<u2>(table[i].descriptor_cp_index);
    writer.write<u2>(table[i].slot);
    if (table[i].signature_cp_index > 0) {
      ++num_lvtt_entries;
    }
  }
  u4 lvt_table_attributes_len = writer.current_offset() - lvt_attributes_length_offset;
  // the lvt_table_attributes_length value is exclusive
  lvt_table_attributes_len -= sizeof(u4);
  writer.write_at_offset(lvt_table_attributes_len, lvt_attributes_length_offset);
  return num_lvtt_entries;
}

static void adjust_local_variable_type_table(JfrBigEndianWriter& writer,
                                            const u2* utf8_indexes,
                                            u2 bci_adjustment_offset,
                                            u2 num_lvtt_entries,
                                            const Method* method,
                                            TRAPS) {
  assert(num_lvtt_entries > 0, "invariant");
  writer.write<u2>(utf8_indexes[UTF8_OPT_LocalVariableTypeTable]);
  const jlong lvtt_attributes_length_offset = writer.current_offset();
  writer.reserve(sizeof(u4));
  writer.write<u2>(num_lvtt_entries);
  const LocalVariableTableElement* table = method->localvariable_table_start();
  assert(table != NULL, "invariant");
  const int lvt_len = method->localvariable_table_length();
  for (int i = 0; i < lvt_len; ++i) {
    if (table[i].signature_cp_index > 0) {
      writer.write<u2>(table[i].start_bci + bci_adjustment_offset);
      writer.write<u2>(table[i].length);
      writer.write<u2>(table[i].name_cp_index);
      writer.write<u2>(table[i].signature_cp_index);
      writer.write<u2>(table[i].slot);
    }
  }
  u4 lvtt_table_attributes_len = writer.current_offset() - lvtt_attributes_length_offset;
  // the lvtt_table_attributes_length value is exclusive
  lvtt_table_attributes_len -= sizeof(u4);
  writer.write_at_offset(lvtt_table_attributes_len, lvtt_attributes_length_offset);
}

static void adjust_code_attributes(JfrBigEndianWriter& writer,
                                   const u2* utf8_indexes,
                                   u2 bci_adjustment_offset,
                                   const Method* clinit_method,
                                   TRAPS) {
  // "Code" attributes
  assert(utf8_indexes != NULL, "invariant");
  const jlong code_attributes_offset = writer.current_offset();
  writer.reserve(sizeof(u2));
  u2 number_of_code_attributes = 0;
  if (clinit_method != NULL) {
    Array<u1>* stack_map = clinit_method->stackmap_data();
    if (stack_map != NULL) {
      ++number_of_code_attributes;
      adjust_stack_map(writer, stack_map, utf8_indexes, bci_adjustment_offset, THREAD);
      assert(writer.is_valid(), "invariant");
    }
    if (clinit_method != NULL && clinit_method->has_linenumber_table()) {
      ++number_of_code_attributes;
      adjust_line_number_table(writer, utf8_indexes, bci_adjustment_offset, clinit_method, THREAD);
      assert(writer.is_valid(), "invariant");
    }
    if (clinit_method != NULL && clinit_method->has_localvariable_table()) {
      ++number_of_code_attributes;
      const u2 num_of_lvtt_entries = adjust_local_variable_table(writer, utf8_indexes, bci_adjustment_offset, clinit_method, THREAD);
      assert(writer.is_valid(), "invariant");
      if (num_of_lvtt_entries > 0) {
        ++number_of_code_attributes;
        adjust_local_variable_type_table(writer, utf8_indexes, bci_adjustment_offset, num_of_lvtt_entries, clinit_method, THREAD);
        assert(writer.is_valid(), "invariant");
      }
    }
  }

  // Store the number of code_attributes
  writer.write_at_offset(number_of_code_attributes, code_attributes_offset);
}

static jlong insert_clinit_method(const InstanceKlass* ik,
                                  const ClassFileParser& parser,
                                  JfrBigEndianWriter& writer,
                                  u2 orig_constant_pool_len,
                                  const u2* utf8_indexes,
                                  const u2 register_method_ref_index,
                                  const Method* clinit_method,
                                  TRAPS) {
  assert(utf8_indexes != NULL, "invariant");
  // The injected code length is always this value.
  // This is to ensure that padding can be done
  // where needed and to simplify size calculations.
  static const u2 injected_code_length = 8;
  const u2 name_index = utf8_indexes[UTF8_OPT_clinit];
  assert(name_index != invalid_cp_index, "invariant");
  const u2 desc_index = utf8_indexes[UTF8_REQ_EMPTY_VOID_METHOD_DESC];
  const u2 max_stack = MAX2(clinit_method != NULL ? clinit_method->verifier_max_stack() : 1, 1);
  const u2 max_locals = MAX2(clinit_method != NULL ? clinit_method->max_locals() : 0, 0);
  const u2 orig_bytecodes_length = clinit_method != NULL ? (u2)clinit_method->code_size() : 0;
  const address orig_bytecodes = clinit_method != NULL ? clinit_method->code_base() : NULL;
  const u2 new_code_length = injected_code_length + orig_bytecodes_length;
  DEBUG_ONLY(const jlong start_offset = writer.current_offset();)
  writer.write<u2>(JVM_ACC_STATIC); // flags
  writer.write<u2>(name_index);
  writer.write<u2>(desc_index);
  writer.write<u2>((u2)0x1); // attributes_count // "Code"
  assert(writer.is_valid(), "invariant");
  DEBUG_ONLY(assert(start_offset + 8 == writer.current_offset(), "invariant");)
  // "Code" attribute
  writer.write<u2>(utf8_indexes[UTF8_REQ_Code]); // "Code"
  const jlong code_attribute_length_offset = writer.current_offset();
  writer.reserve(sizeof(u4));
  writer.write<u2>(max_stack); // max stack
  writer.write<u2>(max_locals); // max locals
  writer.write<u4>((u4)new_code_length); // code length

  /* BEGIN CLINIT CODE */

  // Note the use of ldc_w here instead of ldc.
  // This is to handle all values of "this_class_index"
  writer.write<u1>((u1)Bytecodes::_ldc_w);
  writer.write<u2>((u2)parser.this_class_index()); // load constant "this class"
  writer.write<u1>((u1)Bytecodes::_invokestatic);
  // invoke "FlightRecorder.register(Ljava/lang/Class;")
  writer.write<u2>(register_method_ref_index);
  if (clinit_method == NULL) {
    writer.write<u1>((u1)Bytecodes::_nop);
    writer.write<u1>((u1)Bytecodes::_return);
  } else {
    // If we are pre-pending to original code,
    // do padding to minimize disruption to the original.
    // It might have dependencies on 4-byte boundaries
    // i.e. lookupswitch and tableswitch instructions
    writer.write<u1>((u1)Bytecodes::_nop);
    writer.write<u1>((u1)Bytecodes::_nop);
    // insert original clinit code
    writer.write_bytes(orig_bytecodes, orig_bytecodes_length);
  }

  /* END CLINIT CODE */

  assert(writer.is_valid(), "invariant");
  adjust_exception_table(writer, injected_code_length, clinit_method, THREAD);
  assert(writer.is_valid(), "invariant");
  adjust_code_attributes(writer, utf8_indexes, injected_code_length, clinit_method, THREAD);
  assert(writer.is_valid(), "invariant");
  u4 code_attribute_len = writer.current_offset() - code_attribute_length_offset;
  // the code_attribute_length value is exclusive
  code_attribute_len -= sizeof(u4);
  writer.write_at_offset(code_attribute_len, code_attribute_length_offset);
  return writer.current_offset();
}

// Caller needs ResourceMark
static ClassFileStream* create_new_bytes_for_event_klass(const InstanceKlass* ik, const ClassFileParser& parser, TRAPS) {
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(THREAD));
  static const u2 public_final_flag_mask = JVM_ACC_PUBLIC | JVM_ACC_FINAL;
  const ClassFileStream* const orig_stream = parser.clone_stream();
  const int orig_stream_length = orig_stream->length();
  // allocate an identically sized buffer
  u1* const new_buffer = NEW_RESOURCE_ARRAY_IN_THREAD_RETURN_NULL(THREAD, u1, orig_stream_length);
  if (new_buffer == NULL) {
    return NULL;
  }
  assert(new_buffer != NULL, "invariant");
  // memcpy the entire [B
  memcpy(new_buffer, orig_stream->buffer(), orig_stream_length);
  const u2 orig_cp_len = position_stream_after_cp(orig_stream);
  assert(orig_cp_len > 0, "invariant");
  assert(orig_stream->current_offset() > 0, "invariant");
  orig_stream->skip_u2_fast(3); // access_flags, this_class_index, super_class_index
  const u2 iface_len = orig_stream->get_u2_fast();
  orig_stream->skip_u2_fast(iface_len);
  // fields len
  const u2 orig_fields_len = orig_stream->get_u2_fast();
  // fields
  for (u2 i = 0; i < orig_fields_len; ++i) {
    orig_stream->skip_u2_fast(3);
    const u2 attrib_info_len = orig_stream->get_u2_fast();
    for (u2 j = 0; j < attrib_info_len; ++j) {
      orig_stream->skip_u2_fast(1);
      const u4 attrib_len = orig_stream->get_u4_fast();
      orig_stream->skip_u1_fast(attrib_len);
    }
  }
  // methods
  const u2 orig_methods_len = orig_stream->get_u2_fast();
  for (u2 i = 0; i < orig_methods_len; ++i) {
    const u4 access_flag_offset = orig_stream->current_offset();
    const u2 flags = orig_stream->get_u2_fast();
    // Rewrite JVM_ACC_FINAL -> JVM_ACC_PUBLIC
    if (public_final_flag_mask == flags) {
      JfrBigEndianWriter accessflagsrewriter(new_buffer + access_flag_offset, sizeof(u2));
      accessflagsrewriter.write<u2>(JVM_ACC_PUBLIC);
      assert(accessflagsrewriter.is_valid(), "invariant");
    }
    orig_stream->skip_u2_fast(2);
    const u2 attributes_count = orig_stream->get_u2_fast();
    for (u2 j = 0; j < attributes_count; ++j) {
      orig_stream->skip_u2_fast(1);
      const u4 attrib_len = orig_stream->get_u4_fast();
      orig_stream->skip_u1_fast(attrib_len);
    }
  }
  return new ClassFileStream(new_buffer, orig_stream_length, NULL, ClassFileStream::verify);
}

// Attempt to locate an existing UTF8_INFO mapping the utf8_constant.
// If no UTF8_INFO exists, add (append) a new one to the constant pool.
static u2 find_or_add_utf8_info(JfrBigEndianWriter& writer,
                                const InstanceKlass* ik,
                                const char* const utf8_constant,
                                u2 orig_cp_len,
                                u2& added_cp_entries,
                                TRAPS) {
  assert(utf8_constant != NULL, "invariant");
  TempNewSymbol utf8_sym = SymbolTable::new_symbol(utf8_constant);
  // lookup existing
  const int utf8_orig_idx = utf8_info_index(ik, utf8_sym, THREAD);
  if (utf8_orig_idx != invalid_cp_index) {
    // existing constant pool entry found
    return utf8_orig_idx;
  }
  // no existing match, need to add a new utf8 cp entry
  assert(invalid_cp_index == utf8_orig_idx, "invariant");
  // add / append new
  return add_utf8_info(writer, utf8_constant, orig_cp_len, added_cp_entries);
}

/*
 * This routine will resolve the required utf8_constants array
 * to their constant pool indexes (mapping to their UTF8_INFO's)
 * Only if a constant is actually needed and does not already exist
 * will it be added.
 *
 * The passed in indexes array will be populated with the resolved indexes.
 * The number of newly added constant pool entries is returned.
 */
static u2 resolve_utf8_indexes(JfrBigEndianWriter& writer,
                               const InstanceKlass* ik,
                               u2* const utf8_indexes,
                               u2 orig_cp_len,
                               const Method* clinit_method,
                               bool register_klass,
                               bool untypedEventHandler,
                               TRAPS) {
  assert(utf8_indexes != NULL, "invariant");
  u2 added_cp_entries = 0;
  // resolve all required symbols
  for (u2 index = 0; index < NOF_UTF8_REQ_SYMBOLS; ++index) {
    utf8_indexes[index] = find_or_add_utf8_info(writer, ik, utf8_constants[index], orig_cp_len, added_cp_entries, THREAD);
  }

  // resolve optional constants
  utf8_indexes[UTF8_OPT_eventHandler_FIELD_DESC] = untypedEventHandler ? invalid_cp_index :
    find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_eventHandler_FIELD_DESC], orig_cp_len, added_cp_entries, THREAD);

  utf8_indexes[UTF8_OPT_LjavaLangObject] = untypedEventHandler ?
    find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_LjavaLangObject], orig_cp_len, added_cp_entries, THREAD) : invalid_cp_index;

  if (register_klass) {
    utf8_indexes[UTF8_OPT_clinit] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_clinit], orig_cp_len, added_cp_entries, THREAD);
    utf8_indexes[UTF8_OPT_FlightRecorder] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_FlightRecorder], orig_cp_len, added_cp_entries, THREAD);
    utf8_indexes[UTF8_OPT_register] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_register], orig_cp_len, added_cp_entries, THREAD);
    utf8_indexes[UTF8_OPT_CLASS_VOID_METHOD_DESC] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_CLASS_VOID_METHOD_DESC], orig_cp_len, added_cp_entries, THREAD);
  } else {
    utf8_indexes[UTF8_OPT_clinit] = invalid_cp_index;
    utf8_indexes[UTF8_OPT_FlightRecorder] = invalid_cp_index;
    utf8_indexes[UTF8_OPT_register] = invalid_cp_index;
    utf8_indexes[UTF8_OPT_CLASS_VOID_METHOD_DESC] = invalid_cp_index;
  }

  if (clinit_method != NULL && clinit_method->has_stackmap_table()) {
    utf8_indexes[UTF8_OPT_StackMapTable] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_StackMapTable], orig_cp_len, added_cp_entries, THREAD);
  } else {
    utf8_indexes[UTF8_OPT_StackMapTable] = invalid_cp_index;
  }

  if (clinit_method != NULL && clinit_method->has_linenumber_table()) {
    utf8_indexes[UTF8_OPT_LineNumberTable] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_LineNumberTable], orig_cp_len, added_cp_entries, THREAD);
  } else {
    utf8_indexes[UTF8_OPT_LineNumberTable] = invalid_cp_index;
  }

  if (clinit_method != NULL && clinit_method->has_localvariable_table()) {
    utf8_indexes[UTF8_OPT_LocalVariableTable] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_LocalVariableTable], orig_cp_len, added_cp_entries, THREAD);
    utf8_indexes[UTF8_OPT_LocalVariableTypeTable] =
      find_or_add_utf8_info(writer, ik, utf8_constants[UTF8_OPT_LocalVariableTypeTable], orig_cp_len, added_cp_entries, THREAD);
  } else {
    utf8_indexes[UTF8_OPT_LocalVariableTable] = invalid_cp_index;
    utf8_indexes[UTF8_OPT_LocalVariableTypeTable] = invalid_cp_index;
  }

  return added_cp_entries;
}

static u1* new_bytes_for_lazy_instrumentation(const InstanceKlass* ik,
                                              const ClassFileParser& parser,
                                              jint& size_of_new_bytes,
                                              TRAPS) {
  assert(ik != NULL, "invariant");
  // If the class already has a clinit method
  // we need to take that into account
  const Method* clinit_method = ik->class_initializer();
  bool untypedEventHandler = false;
  const bool register_klass = should_register_klass(ik, untypedEventHandler);
  const ClassFileStream* const orig_stream = parser.clone_stream();
  const int orig_stream_size = orig_stream->length();
  assert(orig_stream->current_offset() == 0, "invariant");
  const u2 orig_cp_len = position_stream_after_cp(orig_stream);
  assert(orig_cp_len > 0, "invariant");
  assert(orig_stream->current_offset() > 0, "invariant");
  // Dimension and allocate a working byte buffer
  // to be used in building up a modified class [B.
  const jint new_buffer_size = extra_stream_bytes + orig_stream_size;
  u1* const new_buffer = NEW_RESOURCE_ARRAY_IN_THREAD_RETURN_NULL(THREAD, u1, new_buffer_size);
  if (new_buffer == NULL) {
    log_error(jfr, system) ("Thread local allocation (native) for " SIZE_FORMAT
      " bytes failed in JfrClassAdapter::on_klass_creation", (size_t)new_buffer_size);
    return NULL;
  }
  assert(new_buffer != NULL, "invariant");
  // [B wrapped in a big endian writer
  JfrBigEndianWriter writer(new_buffer, new_buffer_size);
  assert(writer.current_offset() == 0, "invariant");
  const u4 orig_access_flag_offset = orig_stream->current_offset();
  // Copy original stream from the beginning up to AccessFlags
  // This means the original constant pool contents are copied unmodified
  writer.write_bytes(orig_stream->buffer(), orig_access_flag_offset);
  assert(writer.is_valid(), "invariant");
  assert(writer.current_offset() == (intptr_t)orig_access_flag_offset, "invariant"); // same positions
  // Our writer now sits just after the last original constant pool entry.
  // I.e. we are in a good position to append new constant pool entries
  // This array will contain the resolved indexes
  // in order to reference UTF8_INFO's needed
  u2 utf8_indexes[NOF_UTF8_SYMBOLS];
  // Resolve_utf8_indexes will be conservative in attempting to
  // locate an existing UTF8_INFO; it will only append constants
  // that is absolutely required
  u2 number_of_new_constants =
    resolve_utf8_indexes(writer, ik, utf8_indexes, orig_cp_len, clinit_method, register_klass, untypedEventHandler, THREAD);
  // UTF8_INFO entries now added to the constant pool
  // In order to invoke a method we would need additional
  // constants, JVM_CONSTANT_Class, JVM_CONSTANT_NameAndType
  // and JVM_CONSTANT_Methodref.
  const u2 flr_register_method_ref_index =
    register_klass ?
      add_flr_register_method_constants(writer,
                                        utf8_indexes,
                                        orig_cp_len,
                                        number_of_new_constants,
                                        THREAD) :  invalid_cp_index;

  // New constant pool entries added and all UTF8_INFO indexes resolved
  // Now update the class file constant_pool_count with an updated count
  writer.write_at_offset<u2>(orig_cp_len + number_of_new_constants, 8);
  assert(writer.is_valid(), "invariant");
  orig_stream->skip_u2_fast(3); // access_flags, this_class_index, super_class_index
  const u2 iface_len = orig_stream->get_u2_fast(); // interfaces
  orig_stream->skip_u2_fast(iface_len);
  const u4 orig_fields_len_offset = orig_stream->current_offset();
  // Copy from AccessFlags up to and including interfaces
  writer.write_bytes(orig_stream->buffer() + orig_access_flag_offset,
                     orig_fields_len_offset - orig_access_flag_offset);
  assert(writer.is_valid(), "invariant");
  const jlong new_fields_len_offset = writer.current_offset();
  const u2 orig_fields_len = position_stream_after_fields(orig_stream);
  u4 orig_method_len_offset = orig_stream->current_offset();
  // Copy up to and including fields
  writer.write_bytes(orig_stream->buffer() + orig_fields_len_offset, orig_method_len_offset - orig_fields_len_offset);
  assert(writer.is_valid(), "invariant");
  // We are sitting just after the original number of field_infos
  // so this is a position where we can add (append) new field_infos
  const u2 number_of_new_fields = add_field_infos(writer, utf8_indexes, untypedEventHandler);
  assert(writer.is_valid(), "invariant");
  const jlong new_method_len_offset = writer.current_offset();
  // Additional field_infos added, update classfile fields_count
  writer.write_at_offset<u2>(orig_fields_len + number_of_new_fields, new_fields_len_offset);
  assert(writer.is_valid(), "invariant");
  // Our current location is now at classfile methods_count
  const u2 orig_methods_len = position_stream_after_methods(writer,
                                                            orig_stream,
                                                            utf8_indexes,
                                                            register_klass,
                                                            clinit_method,
                                                            orig_method_len_offset);
  const u4 orig_attributes_count_offset = orig_stream->current_offset();
  // Copy existing methods
  writer.write_bytes(orig_stream->buffer() + orig_method_len_offset, orig_attributes_count_offset - orig_method_len_offset);
  assert(writer.is_valid(), "invariant");
  // We are sitting just after the original number of method_infos
  // so this is a position where we can add (append) new method_infos
  u2 number_of_new_methods = add_method_infos(writer, utf8_indexes);

  // We have just added the new methods.
  //
  // What about the state of <clinit>?
  // We would need to do:
  // 1. Nothing (@Registered(false) annotation)
  // 2. Build up a new <clinit> - and if the original class already contains a <clinit>,
  //                              merging will be neccessary.
  //
  if (register_klass) {
    insert_clinit_method(ik, parser, writer, orig_cp_len, utf8_indexes, flr_register_method_ref_index, clinit_method, THREAD);
  }
  number_of_new_methods += clinit_method != NULL ? 0 : register_klass ? 1 : 0;
  // Update classfile methods_count
  writer.write_at_offset<u2>(orig_methods_len + number_of_new_methods, new_method_len_offset);
  assert(writer.is_valid(), "invariant");
  // Copy last remaining bytes
  writer.write_bytes(orig_stream->buffer() + orig_attributes_count_offset, orig_stream_size - orig_attributes_count_offset);
  assert(writer.is_valid(), "invariant");
  assert(writer.current_offset() > orig_stream->length(), "invariant");
  size_of_new_bytes = (jint)writer.current_offset();
  return new_buffer;
}

static void log_pending_exception(oop throwable) {
  assert(throwable != NULL, "invariant");
  oop msg = java_lang_Throwable::message(throwable);
  if (msg != NULL) {
    char* text = java_lang_String::as_utf8_string(msg);
    if (text != NULL) {
      log_error(jfr, system) ("%s", text);
    }
  }
}

static bool should_force_instrumentation() {
  return !JfrOptionSet::allow_event_retransforms() || JfrEventClassTransformer::is_force_instrumentation();
}

static ClassFileStream* create_new_bytes_for_subklass(const InstanceKlass* ik, const ClassFileParser& parser, JavaThread* t) {
  assert(JdkJfrEvent::is_a(ik), "invariant");
  DEBUG_ONLY(JfrJavaSupport::check_java_thread_in_vm(t));
  jint size_of_new_bytes = 0;
  const u1* new_bytes = new_bytes_for_lazy_instrumentation(ik, parser, size_of_new_bytes, t);
  if (new_bytes == NULL) {
    return NULL;
  }
  assert(new_bytes != NULL, "invariant");
  assert(size_of_new_bytes > 0, "invariant");

  bool force_instrumentation = should_force_instrumentation();
  if (Jfr::is_recording() || force_instrumentation) {
    jint size_instrumented_data = 0;
    unsigned char* instrumented_data = NULL;
    const jclass super = (jclass)JNIHandles::make_local(ik->super()->java_mirror());
    JfrUpcalls::new_bytes_eager_instrumentation(JfrTraceId::load_raw(ik),
                                                force_instrumentation,
                                                super,
                                                size_of_new_bytes,
                                                new_bytes,
                                                &size_instrumented_data,
                                                &instrumented_data,
                                                t);
    if (t->has_pending_exception()) {
      log_pending_exception(t->pending_exception());
      t->clear_pending_exception();
      return NULL;
    }
    assert(instrumented_data != NULL, "invariant");
    assert(size_instrumented_data > 0, "invariant");
    return new ClassFileStream(instrumented_data, size_instrumented_data, NULL, ClassFileStream::verify);
  }
  return new ClassFileStream(new_bytes, size_of_new_bytes, NULL, ClassFileStream::verify);
}

static bool cache_bytes(InstanceKlass* ik, ClassFileStream* new_stream, InstanceKlass* new_ik, TRAPS) {
  assert(ik != NULL, "invariant");
  assert(new_ik != NULL, "invariant");
  assert(new_ik->name() != NULL, "invariant");
  assert(new_stream != NULL, "invariant");
  assert(!HAS_PENDING_EXCEPTION, "invariant");
  static const bool can_retransform = JfrOptionSet::allow_retransforms();
  if (!can_retransform) {
    return true;
  }
  const jint stream_len = new_stream->length();
  JvmtiCachedClassFileData* p =
    (JvmtiCachedClassFileData*)NEW_C_HEAP_ARRAY_RETURN_NULL(u1, offset_of(JvmtiCachedClassFileData, data) + stream_len, mtInternal);
  if (p == NULL) {
    log_error(jfr, system)("Allocation using C_HEAP_ARRAY for " SIZE_FORMAT
      " bytes failed in JfrClassAdapter::on_klass_creation", (size_t)offset_of(JvmtiCachedClassFileData, data) + stream_len);
    return false;
  }
  p->length = stream_len;
  memcpy(p->data, new_stream->buffer(), stream_len);
  new_ik->set_cached_class_file(p);
  JvmtiCachedClassFileData* const cached_class_data = ik->get_cached_class_file();
  if (cached_class_data != NULL) {
    os::free(cached_class_data);
    ik->set_cached_class_file(NULL);
  }
  return true;
}

static InstanceKlass* create_new_instance_klass(InstanceKlass* ik, ClassFileStream* stream, TRAPS) {
  assert(stream != NULL, "invariant");
  ResourceMark rm(THREAD);
  ClassLoaderData* const cld = ik->class_loader_data();
  Handle pd(THREAD, ik->protection_domain());
  Symbol* const class_name = ik->name();
  const char* const klass_name = class_name != NULL ? class_name->as_C_string() : "";
  ClassLoadInfo cl_info(pd);
  ClassFileParser new_parser(stream,
                             class_name,
                             cld,
                             &cl_info,
                             ClassFileParser::INTERNAL, // internal visibility
                             THREAD);
  if (HAS_PENDING_EXCEPTION) {
    log_pending_exception(PENDING_EXCEPTION);
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  }
  const ClassInstanceInfo* cl_inst_info = cl_info.class_hidden_info_ptr();
  InstanceKlass* const new_ik = new_parser.create_instance_klass(false, *cl_inst_info, THREAD);
  if (HAS_PENDING_EXCEPTION) {
    log_pending_exception(PENDING_EXCEPTION);
    CLEAR_PENDING_EXCEPTION;
    return NULL;
  }
  assert(new_ik != NULL, "invariant");
  assert(new_ik->name() != NULL, "invariant");
  assert(strncmp(ik->name()->as_C_string(), new_ik->name()->as_C_string(), strlen(ik->name()->as_C_string())) == 0, "invariant");
  return cache_bytes(ik, stream, new_ik, THREAD) ? new_ik : NULL;
}

static void rewrite_klass_pointer(InstanceKlass*& ik, InstanceKlass* new_ik, ClassFileParser& parser, TRAPS) {
  assert(ik != NULL, "invariant");
  assert(new_ik != NULL, "invariant");
  assert(new_ik->name() != NULL, "invariant");
  assert(JdkJfrEvent::is(new_ik) || JdkJfrEvent::is_subklass(new_ik), "invariant");
  assert(!HAS_PENDING_EXCEPTION, "invariant");
  // assign original InstanceKlass* back onto "its" parser object for proper destruction
  parser.set_klass_to_deallocate(ik);
  // now rewrite original pointer to newly created InstanceKlass
  ik = new_ik;
}

static bool is_retransforming(const InstanceKlass* ik, TRAPS) {
  assert(ik != NULL, "invariant");
  assert(JdkJfrEvent::is_a(ik), "invariant");
  Symbol* const name = ik->name();
  assert(name != NULL, "invariant");
  Handle class_loader(THREAD, ik->class_loader());
  Handle protection_domain(THREAD, ik->protection_domain());
  return SystemDictionary::find_instance_klass(name, class_loader, protection_domain) != NULL;
}

// target for JFR_ON_KLASS_CREATION hook
void JfrEventClassTransformer::on_klass_creation(InstanceKlass*& ik, ClassFileParser& parser, TRAPS) {
  assert(ik != NULL, "invariant");
  if (JdkJfrEvent::is(ik)) {
    ResourceMark rm(THREAD);
    HandleMark hm(THREAD);
    ClassFileStream* new_stream = create_new_bytes_for_event_klass(ik, parser, THREAD);
    if (new_stream == NULL) {
      log_error(jfr, system)("JfrClassAdapter: unable to create ClassFileStream");
      return;
    }
    assert(new_stream != NULL, "invariant");
    InstanceKlass* new_ik = create_new_instance_klass(ik, new_stream, THREAD);
    if (new_ik == NULL) {
      log_error(jfr, system)("JfrClassAdapter: unable to create InstanceKlass");
      return;
    }
    assert(new_ik != NULL, "invariant");
    // We now need to explicitly tag the replaced klass as the jdk.jfr.Event klass
    assert(!JdkJfrEvent::is(new_ik), "invariant");
    JdkJfrEvent::tag_as(new_ik);
    assert(JdkJfrEvent::is(new_ik), "invariant");
    rewrite_klass_pointer(ik, new_ik, parser, THREAD);
    return;
  }
  assert(JdkJfrEvent::is_subklass(ik), "invariant");
  if (ik->is_abstract() || is_retransforming(ik, THREAD)) {
    // abstract and scratch classes are not instrumented
    return;
  }
  ResourceMark rm(THREAD);
  HandleMark hm(THREAD);
  ClassFileStream* const new_stream = create_new_bytes_for_subklass(ik, parser, THREAD);
  if (NULL == new_stream) {
    log_error(jfr, system)("JfrClassAdapter: unable to create ClassFileStream");
    return;
  }
  assert(new_stream != NULL, "invariant");
  InstanceKlass* new_ik = create_new_instance_klass(ik, new_stream, THREAD);
  if (new_ik == NULL) {
    log_error(jfr, system)("JfrClassAdapter: unable to create InstanceKlass");
    return;
  }
  assert(new_ik != NULL, "invariant");
  // would have been tagged already as a subklass during the normal process of traceid assignment
  assert(JdkJfrEvent::is_subklass(new_ik), "invariant");
  traceid id = ik->trace_id();
  ik->set_trace_id(0);
  new_ik->set_trace_id(id);
  rewrite_klass_pointer(ik, new_ik, parser, THREAD);
}

static bool _force_instrumentation = false;
void JfrEventClassTransformer::set_force_instrumentation(bool force_instrumentation) {
  _force_instrumentation = force_instrumentation;
}

bool JfrEventClassTransformer::is_force_instrumentation() {
  return _force_instrumentation;
}
