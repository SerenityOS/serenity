/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "interpreter/interpreter.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/metaspaceClosure.hpp"
#include "memory/resourceArea.hpp"
#include "oops/constMethod.hpp"
#include "oops/method.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "utilities/align.hpp"

// Static initialization
const u2 ConstMethod::MAX_IDNUM   = 0xFFFE;
const u2 ConstMethod::UNSET_IDNUM = 0xFFFF;

ConstMethod* ConstMethod::allocate(ClassLoaderData* loader_data,
                                   int byte_code_size,
                                   InlineTableSizes* sizes,
                                   MethodType method_type,
                                   TRAPS) {
  int size = ConstMethod::size(byte_code_size, sizes);
  return new (loader_data, size, MetaspaceObj::ConstMethodType, THREAD) ConstMethod(
      byte_code_size, sizes, method_type, size);
}

ConstMethod::ConstMethod(int byte_code_size,
                         InlineTableSizes* sizes,
                         MethodType method_type,
                         int size) {

  NoSafepointVerifier no_safepoint;
  init_fingerprint();
  set_constants(NULL);
  set_stackmap_data(NULL);
  set_code_size(byte_code_size);
  set_constMethod_size(size);
  set_inlined_tables_length(sizes); // sets _flags
  set_method_type(method_type);
  assert(this->size() == size, "wrong size for object");
  set_name_index(0);
  set_signature_index(0);
  set_constants(NULL);
  set_max_stack(0);
  set_max_locals(0);
  set_method_idnum(0);
  set_size_of_parameters(0);
  set_result_type((BasicType)0);
}

// Accessor that copies to metadata.
void ConstMethod::copy_stackmap_data(ClassLoaderData* loader_data,
                                     u1* sd, int length, TRAPS) {
  _stackmap_data = MetadataFactory::new_array<u1>(loader_data, length, CHECK);
  memcpy((void*)_stackmap_data->adr_at(0), (void*)sd, length);
}

// Deallocate metadata fields associated with ConstMethod*
void ConstMethod::deallocate_contents(ClassLoaderData* loader_data) {
  if (stackmap_data() != NULL) {
    MetadataFactory::free_array<u1>(loader_data, stackmap_data());
  }
  set_stackmap_data(NULL);

  // deallocate annotation arrays
  if (has_method_annotations())
    MetadataFactory::free_array<u1>(loader_data, method_annotations());
  if (has_parameter_annotations())
    MetadataFactory::free_array<u1>(loader_data, parameter_annotations());
  if (has_type_annotations())
    MetadataFactory::free_array<u1>(loader_data, type_annotations());
  if (has_default_annotations())
    MetadataFactory::free_array<u1>(loader_data, default_annotations());
}

// How big must this constMethodObject be?

int ConstMethod::size(int code_size,
                      InlineTableSizes* sizes) {
  int extra_bytes = code_size;
  if (sizes->compressed_linenumber_size() > 0) {
    extra_bytes += sizes->compressed_linenumber_size();
  }
  if (sizes->checked_exceptions_length() > 0) {
    extra_bytes += sizeof(u2);
    extra_bytes += sizes->checked_exceptions_length() * sizeof(CheckedExceptionElement);
  }
  if (sizes->localvariable_table_length() > 0) {
    extra_bytes += sizeof(u2);
    extra_bytes +=
              sizes->localvariable_table_length() * sizeof(LocalVariableTableElement);
  }
  if (sizes->exception_table_length() > 0) {
    extra_bytes += sizeof(u2);
    extra_bytes += sizes->exception_table_length() * sizeof(ExceptionTableElement);
  }
  if (sizes->generic_signature_index() != 0) {
    extra_bytes += sizeof(u2);
  }
  // This has to be a less-than-or-equal check, because we might be
  // storing information from a zero-length MethodParameters
  // attribute.  We have to store these, because in some cases, they
  // cause the reflection API to throw a MalformedParametersException.
  if (sizes->method_parameters_length() >= 0) {
    extra_bytes += sizeof(u2);
    extra_bytes += sizes->method_parameters_length() * sizeof(MethodParametersElement);
  }

  // Align sizes up to a word.
  extra_bytes = align_up(extra_bytes, BytesPerWord);

  // One pointer per annotation array
  if (sizes->method_annotations_length() > 0) {
    extra_bytes += sizeof(AnnotationArray*);
  }
  if (sizes->parameter_annotations_length() > 0) {
    extra_bytes += sizeof(AnnotationArray*);
  }
  if (sizes->type_annotations_length() > 0) {
    extra_bytes += sizeof(AnnotationArray*);
  }
  if (sizes->default_annotations_length() > 0) {
    extra_bytes += sizeof(AnnotationArray*);
  }

  int extra_words = align_up(extra_bytes, BytesPerWord) / BytesPerWord;
  assert(extra_words == extra_bytes/BytesPerWord, "should already be aligned");
  return align_metadata_size(header_size() + extra_words);
}

Method* ConstMethod::method() const {
    return _constants->pool_holder()->method_with_idnum(_method_idnum);
  }

// linenumber table - note that length is unknown until decompression,
// see class CompressedLineNumberReadStream.

u_char* ConstMethod::compressed_linenumber_table() const {
  // Located immediately following the bytecodes.
  assert(has_linenumber_table(), "called only if table is present");
  return code_end();
}

// Last short in ConstMethod* before annotations
u2* ConstMethod::last_u2_element() const {
  int offset = 0;
  if (has_method_annotations()) offset++;
  if (has_parameter_annotations()) offset++;
  if (has_type_annotations()) offset++;
  if (has_default_annotations()) offset++;
  return (u2*)((AnnotationArray**)constMethod_end() - offset) - 1;
}

u2* ConstMethod::generic_signature_index_addr() const {
  // Located at the end of the constMethod.
  assert(has_generic_signature(), "called only if generic signature exists");
  return last_u2_element();
}

u2* ConstMethod::method_parameters_length_addr() const {
  assert(has_method_parameters(), "called only if table is present");
  return has_generic_signature() ? (last_u2_element() - 1) :
                                    last_u2_element();
}

u2* ConstMethod::checked_exceptions_length_addr() const {
  // Located immediately before the generic signature index.
  assert(has_checked_exceptions(), "called only if table is present");
  if(has_method_parameters()) {
    // If method parameters present, locate immediately before them.
    return (u2*)method_parameters_start() - 1;
  } else {
    // Else, the exception table is at the end of the constMethod.
    return has_generic_signature() ? (last_u2_element() - 1) :
                                     last_u2_element();
  }
}

u2* ConstMethod::exception_table_length_addr() const {
  assert(has_exception_handler(), "called only if table is present");
  if (has_checked_exceptions()) {
    // If checked_exception present, locate immediately before them.
    return (u2*) checked_exceptions_start() - 1;
  } else {
    if(has_method_parameters()) {
      // If method parameters present, locate immediately before them.
      return (u2*)method_parameters_start() - 1;
    } else {
      // Else, the exception table is at the end of the constMethod.
      return has_generic_signature() ? (last_u2_element() - 1) :
                                        last_u2_element();
    }
  }
}

u2* ConstMethod::localvariable_table_length_addr() const {
  assert(has_localvariable_table(), "called only if table is present");
  if (has_exception_handler()) {
    // If exception_table present, locate immediately before them.
    return (u2*) exception_table_start() - 1;
  } else {
    if (has_checked_exceptions()) {
      // If checked_exception present, locate immediately before them.
      return (u2*) checked_exceptions_start() - 1;
    } else {
      if(has_method_parameters()) {
        // If method parameters present, locate immediately before them.
        return (u2*)method_parameters_start() - 1;
      } else {
        // Else, the exception table is at the end of the constMethod.
      return has_generic_signature() ? (last_u2_element() - 1) :
                                        last_u2_element();
      }
    }
  }
}

// Update the flags to indicate the presence of these optional fields.
void ConstMethod::set_inlined_tables_length(InlineTableSizes* sizes) {
  _flags = 0;
  if (sizes->compressed_linenumber_size() > 0)
    _flags |= _has_linenumber_table;
  if (sizes->generic_signature_index() != 0)
    _flags |= _has_generic_signature;
  if (sizes->method_parameters_length() >= 0)
    _flags |= _has_method_parameters;
  if (sizes->checked_exceptions_length() > 0)
    _flags |= _has_checked_exceptions;
  if (sizes->exception_table_length() > 0)
    _flags |= _has_exception_table;
  if (sizes->localvariable_table_length() > 0)
    _flags |= _has_localvariable_table;

  // annotations, they are all pointer sized embedded objects so don't have
  // a length embedded also.
  if (sizes->method_annotations_length() > 0)
    _flags |= _has_method_annotations;
  if (sizes->parameter_annotations_length() > 0)
    _flags |= _has_parameter_annotations;
  if (sizes->type_annotations_length() > 0)
    _flags |= _has_type_annotations;
  if (sizes->default_annotations_length() > 0)
    _flags |= _has_default_annotations;

  // This code is extremely brittle and should possibly be revised.
  // The *_length_addr functions walk backwards through the
  // constMethod data, using each of the length indexes ahead of them,
  // as well as the flags variable.  Therefore, the indexes must be
  // initialized in reverse order, or else they will compute the wrong
  // offsets.  Moving the initialization of _flags into a separate
  // block solves *half* of the problem, but the following part will
  // still break if the order is not exactly right.
  //
  // Also, the servicability agent needs to be informed anytime
  // anything is added here.  It might be advisable to have some sort
  // of indication of this inline.
  if (sizes->generic_signature_index() != 0)
    *(generic_signature_index_addr()) = sizes->generic_signature_index();
  // New data should probably go here.
  if (sizes->method_parameters_length() >= 0)
    *(method_parameters_length_addr()) = sizes->method_parameters_length();
  if (sizes->checked_exceptions_length() > 0)
    *(checked_exceptions_length_addr()) = sizes->checked_exceptions_length();
  if (sizes->exception_table_length() > 0)
    *(exception_table_length_addr()) = sizes->exception_table_length();
  if (sizes->localvariable_table_length() > 0)
    *(localvariable_table_length_addr()) = sizes->localvariable_table_length();
}

int ConstMethod::method_parameters_length() const {
  return has_method_parameters() ? *(method_parameters_length_addr()) : -1;
}

MethodParametersElement* ConstMethod::method_parameters_start() const {
  u2* addr = method_parameters_length_addr();
  u2 length = *addr;
  addr -= length * sizeof(MethodParametersElement) / sizeof(u2);
  return (MethodParametersElement*) addr;
}


int ConstMethod::checked_exceptions_length() const {
  return has_checked_exceptions() ? *(checked_exceptions_length_addr()) : 0;
}


CheckedExceptionElement* ConstMethod::checked_exceptions_start() const {
  u2* addr = checked_exceptions_length_addr();
  u2 length = *addr;
  assert(length > 0, "should only be called if table is present");
  addr -= length * sizeof(CheckedExceptionElement) / sizeof(u2);
  return (CheckedExceptionElement*) addr;
}


int ConstMethod::localvariable_table_length() const {
  return has_localvariable_table() ? *(localvariable_table_length_addr()) : 0;
}


LocalVariableTableElement* ConstMethod::localvariable_table_start() const {
  u2* addr = localvariable_table_length_addr();
  u2 length = *addr;
  assert(length > 0, "should only be called if table is present");
  addr -= length * sizeof(LocalVariableTableElement) / sizeof(u2);
  return (LocalVariableTableElement*) addr;
}

int ConstMethod::exception_table_length() const {
  return has_exception_handler() ? *(exception_table_length_addr()) : 0;
}

ExceptionTableElement* ConstMethod::exception_table_start() const {
  u2* addr = exception_table_length_addr();
  u2 length = *addr;
  assert(length > 0, "should only be called if table is present");
  addr -= length * sizeof(ExceptionTableElement) / sizeof(u2);
  return (ExceptionTableElement*)addr;
}

AnnotationArray** ConstMethod::method_annotations_addr() const {
  assert(has_method_annotations(), "should only be called if method annotations are present");
  return (AnnotationArray**)constMethod_end() - 1;
}

AnnotationArray** ConstMethod::parameter_annotations_addr() const {
  assert(has_parameter_annotations(), "should only be called if method parameter annotations are present");
  int offset = 1;
  if (has_method_annotations()) offset++;
  return (AnnotationArray**)constMethod_end() - offset;
}

AnnotationArray** ConstMethod::type_annotations_addr() const {
  assert(has_type_annotations(), "should only be called if method type annotations are present");
  int offset = 1;
  if (has_method_annotations()) offset++;
  if (has_parameter_annotations()) offset++;
  return (AnnotationArray**)constMethod_end() - offset;
}

AnnotationArray** ConstMethod::default_annotations_addr() const {
  assert(has_default_annotations(), "should only be called if method default annotations are present");
  int offset = 1;
  if (has_method_annotations()) offset++;
  if (has_parameter_annotations()) offset++;
  if (has_type_annotations()) offset++;
  return (AnnotationArray**)constMethod_end() - offset;
}

Array<u1>* copy_annotations(ClassLoaderData* loader_data, AnnotationArray* from, TRAPS) {
  int length = from->length();
  Array<u1>* a = MetadataFactory::new_array<u1>(loader_data, length, 0, CHECK_NULL);
  memcpy((void*)a->adr_at(0), (void*)from->adr_at(0), length);
  return a;
}

// copy annotations from 'cm' to 'this'
// Must make copy because these are deallocated with their constMethod, if redefined.
void ConstMethod::copy_annotations_from(ClassLoaderData* loader_data, ConstMethod* cm, TRAPS) {
  Array<u1>* a;
  if (cm->has_method_annotations()) {
    assert(has_method_annotations(), "should be allocated already");
    a = copy_annotations(loader_data, cm->method_annotations(), CHECK);
    set_method_annotations(a);
  }
  if (cm->has_parameter_annotations()) {
    assert(has_parameter_annotations(), "should be allocated already");
    a = copy_annotations(loader_data, cm->parameter_annotations(), CHECK);
    set_parameter_annotations(a);
  }
  if (cm->has_type_annotations()) {
    assert(has_type_annotations(), "should be allocated already");
    a = copy_annotations(loader_data, cm->type_annotations(), CHECK);
    set_type_annotations(a);
  }
  if (cm->has_default_annotations()) {
    assert(has_default_annotations(), "should be allocated already");
    a = copy_annotations(loader_data, cm->default_annotations(), CHECK);
    set_default_annotations(a);
  }
}

void ConstMethod::metaspace_pointers_do(MetaspaceClosure* it) {
  log_trace(cds)("Iter(ConstMethod): %p", this);

  if (!method()->method_holder()->is_rewritten()) {
    it->push(&_constants, MetaspaceClosure::_writable);
  } else {
    it->push(&_constants);
  }
  it->push(&_stackmap_data);
  if (has_method_annotations()) {
    it->push(method_annotations_addr());
  }
  if (has_parameter_annotations()) {
      it->push(parameter_annotations_addr());
  }
  if (has_type_annotations()) {
      it->push(type_annotations_addr());
  }
  if (has_default_annotations()) {
      it->push(default_annotations_addr());
  }
}

// Printing

void ConstMethod::print_on(outputStream* st) const {
  ResourceMark rm;
  st->print_cr("%s", internal_name());
  Method* m = method();
  st->print(" - method:       " INTPTR_FORMAT " ", p2i((address)m));
  if (m != NULL) {
    m->print_value_on(st);
  }
  st->cr();
  if (has_stackmap_table()) {
    st->print(" - stackmap data:       ");
    stackmap_data()->print_value_on(st);
    st->cr();
  }
}

// Short version of printing ConstMethod* - just print the name of the
// method it belongs to.
void ConstMethod::print_value_on(outputStream* st) const {
  st->print(" const part of method " );
  Method* m = method();
  if (m != NULL) {
    m->print_value_on(st);
  } else {
    st->print("NULL");
  }
}

// Verification

void ConstMethod::verify_on(outputStream* st) {
  // Verification can occur during oop construction before the method or
  // other fields have been initialized.
  guarantee(method() != NULL && method()->is_method(), "should be method");

  address m_end = (address)((intptr_t) this + size());
  address compressed_table_start = code_end();
  guarantee(compressed_table_start <= m_end, "invalid method layout");
  address compressed_table_end = compressed_table_start;
  // Verify line number table
  if (has_linenumber_table()) {
    CompressedLineNumberReadStream stream(compressed_linenumber_table());
    while (stream.read_pair()) {
      guarantee(stream.bci() >= 0 && stream.bci() <= code_size(), "invalid bci in line number table");
    }
    compressed_table_end += stream.position();
  }
  guarantee(compressed_table_end <= m_end, "invalid method layout");
  // Verify checked exceptions, exception table and local variable tables
  if (has_method_parameters()) {
    u2* addr = method_parameters_length_addr();
    guarantee(*addr > 0 && (address) addr >= compressed_table_end && (address) addr < m_end, "invalid method layout");
  }
  if (has_checked_exceptions()) {
    u2* addr = checked_exceptions_length_addr();
    guarantee(*addr > 0 && (address) addr >= compressed_table_end && (address) addr < m_end, "invalid method layout");
  }
  if (has_exception_handler()) {
    u2* addr = exception_table_length_addr();
     guarantee(*addr > 0 && (address) addr >= compressed_table_end && (address) addr < m_end, "invalid method layout");
  }
  if (has_localvariable_table()) {
    u2* addr = localvariable_table_length_addr();
    guarantee(*addr > 0 && (address) addr >= compressed_table_end && (address) addr < m_end, "invalid method layout");
  }
  // Check compressed_table_end relative to uncompressed_table_start
  u2* uncompressed_table_start;
  if (has_localvariable_table()) {
    uncompressed_table_start = (u2*) localvariable_table_start();
  } else if (has_exception_handler()) {
    uncompressed_table_start = (u2*) exception_table_start();
  } else if (has_checked_exceptions()) {
      uncompressed_table_start = (u2*) checked_exceptions_start();
  } else if (has_method_parameters()) {
      uncompressed_table_start = (u2*) method_parameters_start();
  } else {
      uncompressed_table_start = (u2*) m_end;
  }
  int gap = (intptr_t) uncompressed_table_start - (intptr_t) compressed_table_end;
  int max_gap = align_metadata_size(1)*BytesPerWord;
  guarantee(gap >= 0 && gap < max_gap, "invalid method layout");
}
