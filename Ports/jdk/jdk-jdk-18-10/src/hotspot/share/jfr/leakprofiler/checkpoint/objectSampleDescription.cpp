/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm_io.h"
#include "classfile/javaClasses.inline.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmClasses.hpp"
#include "classfile/vmSymbols.hpp"
#include "jfr/leakprofiler/checkpoint/objectSampleDescription.hpp"
#include "jfr/recorder/checkpoint/jfrCheckpointWriter.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/fieldDescriptor.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/ostream.hpp"

static Symbol* symbol_size = NULL;

ObjectDescriptionBuilder::ObjectDescriptionBuilder() {
  reset();
}

void ObjectDescriptionBuilder::write_int(jint value) {
  char buf[20];
  jio_snprintf(buf, sizeof(buf), "%d", value);
  write_text(buf);
}

void ObjectDescriptionBuilder::write_text(const char* text) {
  if (_index == sizeof(_buffer) - 2) {
    return;
  }
  while (*text != '\0' && _index < sizeof(_buffer) - 2) {
    _buffer[_index] = *text;
    _index++;
    text++;
  }
  assert(_index < sizeof(_buffer) - 1, "index should not exceed buffer size");
  // add ellipsis if we reached end
  if (_index == sizeof(_buffer) - 2) {
    _buffer[_index-3] = '.';
    _buffer[_index-2] = '.';
    _buffer[_index-1] = '.';
  }
  // terminate string
  _buffer[_index] = '\0';
}

void ObjectDescriptionBuilder::reset() {
  _index = 0;
  _buffer[0] = '\0';
}

void ObjectDescriptionBuilder::print_description(outputStream* out) {
  out->print("%s", (const char*)_buffer);
}

const char* ObjectDescriptionBuilder::description() {
  if (_buffer[0] == '\0') {
    return NULL;
  }
  const size_t len = strlen(_buffer);
  char* copy = NEW_RESOURCE_ARRAY(char, len + 1);
  assert(copy != NULL, "invariant");
  strncpy(copy, _buffer, len + 1);
  return copy;
}

ObjectSampleDescription::ObjectSampleDescription(oop object) :
  _object(object) {
}

void ObjectSampleDescription::ensure_initialized() {
  if (symbol_size == NULL) {
    symbol_size = SymbolTable::new_permanent_symbol("size");
  }
}

void ObjectSampleDescription::print_description(outputStream* out) {
  write_object_to_buffer();
  _description.print_description(out);
}

const char* ObjectSampleDescription::description() {
  write_object_to_buffer();
  return _description.description();
}

void ObjectSampleDescription::write_text(const char* text) {
  _description.write_text(text);
}

void ObjectSampleDescription::write_int(jint value) {
  _description.write_int(value);
}

void ObjectSampleDescription::write_object_to_buffer() {
  ensure_initialized();
  _description.reset();
  write_object_details();
}

void ObjectSampleDescription::write_object_details() {
  Klass* klass = _object->klass();
  Symbol* class_name = klass->name();
  jint size;

  if (_object->is_a(vmClasses::Class_klass())) {
    write_class_name();
    return;
  }

  if (_object->is_a(vmClasses::Thread_klass())) {
    write_thread_name();
    return;
  }

  if (_object->is_a(vmClasses::ThreadGroup_klass())) {
    write_thread_group_name();
    return;
  }

  if (read_int_size(&size)) {
    write_size(size);
    return;
  }
}

void ObjectSampleDescription::write_class_name() {
  assert(_object->is_a(vmClasses::Class_klass()), "invariant");
  const Klass* const k = java_lang_Class::as_Klass(_object);
  if (k == NULL) {
    // might represent a primitive
    const Klass* const ak = java_lang_Class::array_klass_acquire(_object);
    // If ak is NULL, this is most likely a mirror associated with a
    // jvmti redefine/retransform scratch klass. We can't get any additional
    // information from it.
    if (ak != NULL) {
      write_text(type2name(java_lang_Class::primitive_type(_object)));
    }
    return;
  }

  if (k->is_instance_klass()) {
    const InstanceKlass* ik = InstanceKlass::cast(k);
    if (ik->is_hidden()) {
      return;
    }
    const Symbol* name = ik->name();
    if (name != NULL) {
      write_text("Class Name: ");
      write_text(name->as_klass_external_name());
    }
  }
}

void ObjectSampleDescription::write_thread_group_name() {
  assert(_object->is_a(vmClasses::ThreadGroup_klass()), "invariant");
  const char* tg_name = java_lang_ThreadGroup::name(_object);
  if (tg_name != NULL) {
    write_text("Thread Group: ");
    write_text(tg_name);
  }
}

void ObjectSampleDescription::write_thread_name() {
  assert(_object->is_a(vmClasses::Thread_klass()), "invariant");
  oop name = java_lang_Thread::name(_object);
  if (name != NULL) {
    char* p = java_lang_String::as_utf8_string(name);
    if (p != NULL) {
      write_text("Thread Name: ");
      write_text(p);
    }
  }
}

void ObjectSampleDescription::write_size(jint size) {
  if (size >= 0) {
    write_text("Size: ");
    write_int(size);
  }
}

bool ObjectSampleDescription::read_int_size(jint* result_size) {
  fieldDescriptor fd;
  Klass* klass = _object->klass();
  if (klass->is_instance_klass()) {
    InstanceKlass* ik = InstanceKlass::cast(klass);
    if (ik->find_field(symbol_size, vmSymbols::int_signature(), false, &fd) != NULL) {
       jint size = _object->int_field(fd.offset());
       *result_size = size;
       return true;
    }
  }
  return false;
}
