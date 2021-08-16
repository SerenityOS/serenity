/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/moduleEntry.hpp"
#include "classfile/packageEntry.hpp"
#include "classfile/symbolTable.hpp"
#include "classfile/vmSymbols.hpp"
#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "memory/metadataFactory.hpp"
#include "memory/resourceArea.hpp"
#include "memory/universe.hpp"
#include "oops/arrayKlass.inline.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/klass.inline.hpp"
#include "oops/objArrayKlass.hpp"
#include "oops/oop.inline.hpp"
#include "oops/typeArrayKlass.inline.hpp"
#include "oops/typeArrayOop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "utilities/macros.hpp"

TypeArrayKlass* TypeArrayKlass::create_klass(BasicType type,
                                      const char* name_str, TRAPS) {
  Symbol* sym = NULL;
  if (name_str != NULL) {
    sym = SymbolTable::new_permanent_symbol(name_str);
  }

  ClassLoaderData* null_loader_data = ClassLoaderData::the_null_class_loader_data();

  TypeArrayKlass* ak = TypeArrayKlass::allocate(null_loader_data, type, sym, CHECK_NULL);

  // Call complete_create_array_klass after all instance variables have been initialized.
  complete_create_array_klass(ak, ak->super(), ModuleEntryTable::javabase_moduleEntry(), CHECK_NULL);

  // Add all classes to our internal class loader list here,
  // including classes in the bootstrap (NULL) class loader.
  // Do this step after creating the mirror so that if the
  // mirror creation fails, loaded_classes_do() doesn't find
  // an array class without a mirror.
  null_loader_data->add_class(ak);
  JFR_ONLY(ASSIGN_PRIMITIVE_CLASS_ID(ak);)
  return ak;
}

TypeArrayKlass* TypeArrayKlass::allocate(ClassLoaderData* loader_data, BasicType type, Symbol* name, TRAPS) {
  assert(TypeArrayKlass::header_size() <= InstanceKlass::header_size(),
      "array klasses must be same size as InstanceKlass");

  int size = ArrayKlass::static_size(TypeArrayKlass::header_size());

  return new (loader_data, size, THREAD) TypeArrayKlass(type, name);
}

TypeArrayKlass::TypeArrayKlass(BasicType type, Symbol* name) : ArrayKlass(name, ID) {
  set_layout_helper(array_layout_helper(type));
  assert(is_array_klass(), "sanity");
  assert(is_typeArray_klass(), "sanity");

  set_max_length(arrayOopDesc::max_array_length(type));
  assert(size() >= TypeArrayKlass::header_size(), "bad size");

  set_class_loader_data(ClassLoaderData::the_null_class_loader_data());
}

typeArrayOop TypeArrayKlass::allocate_common(int length, bool do_zero, TRAPS) {
  assert(log2_element_size() >= 0, "bad scale");
  check_array_allocation_length(length, max_length(), CHECK_NULL);
  size_t size = typeArrayOopDesc::object_size(layout_helper(), length);
  return (typeArrayOop)Universe::heap()->array_allocate(this, (int)size, length,
                                                        do_zero, CHECK_NULL);
}

oop TypeArrayKlass::multi_allocate(int rank, jint* last_size, TRAPS) {
  // For typeArrays this is only called for the last dimension
  assert(rank == 1, "just checking");
  int length = *last_size;
  return allocate(length, THREAD);
}


void TypeArrayKlass::copy_array(arrayOop s, int src_pos, arrayOop d, int dst_pos, int length, TRAPS) {
  assert(s->is_typeArray(), "must be type array");

  // Check destination type.
  if (!d->is_typeArray()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    if (d->is_objArray()) {
      ss.print("arraycopy: type mismatch: can not copy %s[] into object array[]",
               type2name_tab[ArrayKlass::cast(s->klass())->element_type()]);
    } else {
      ss.print("arraycopy: destination type %s is not an array", d->klass()->external_name());
    }
    THROW_MSG(vmSymbols::java_lang_ArrayStoreException(), ss.as_string());
  }
  if (element_type() != TypeArrayKlass::cast(d->klass())->element_type()) {
    ResourceMark rm(THREAD);
    stringStream ss;
    ss.print("arraycopy: type mismatch: can not copy %s[] into %s[]",
             type2name_tab[ArrayKlass::cast(s->klass())->element_type()],
             type2name_tab[ArrayKlass::cast(d->klass())->element_type()]);
    THROW_MSG(vmSymbols::java_lang_ArrayStoreException(), ss.as_string());
  }

  // Check if all offsets and lengths are non negative.
  if (src_pos < 0 || dst_pos < 0 || length < 0) {
    // Pass specific exception reason.
    ResourceMark rm(THREAD);
    stringStream ss;
    if (src_pos < 0) {
      ss.print("arraycopy: source index %d out of bounds for %s[%d]",
               src_pos, type2name_tab[ArrayKlass::cast(s->klass())->element_type()], s->length());
    } else if (dst_pos < 0) {
      ss.print("arraycopy: destination index %d out of bounds for %s[%d]",
               dst_pos, type2name_tab[ArrayKlass::cast(d->klass())->element_type()], d->length());
    } else {
      ss.print("arraycopy: length %d is negative", length);
    }
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }
  // Check if the ranges are valid
  if ((((unsigned int) length + (unsigned int) src_pos) > (unsigned int) s->length()) ||
      (((unsigned int) length + (unsigned int) dst_pos) > (unsigned int) d->length())) {
    // Pass specific exception reason.
    ResourceMark rm(THREAD);
    stringStream ss;
    if (((unsigned int) length + (unsigned int) src_pos) > (unsigned int) s->length()) {
      ss.print("arraycopy: last source index %u out of bounds for %s[%d]",
               (unsigned int) length + (unsigned int) src_pos,
               type2name_tab[ArrayKlass::cast(s->klass())->element_type()], s->length());
    } else {
      ss.print("arraycopy: last destination index %u out of bounds for %s[%d]",
               (unsigned int) length + (unsigned int) dst_pos,
               type2name_tab[ArrayKlass::cast(d->klass())->element_type()], d->length());
    }
    THROW_MSG(vmSymbols::java_lang_ArrayIndexOutOfBoundsException(), ss.as_string());
  }
  // Check zero copy
  if (length == 0)
    return;

  // This is an attempt to make the copy_array fast.
  int l2es = log2_element_size();
  size_t src_offset = arrayOopDesc::base_offset_in_bytes(element_type()) + ((size_t)src_pos << l2es);
  size_t dst_offset = arrayOopDesc::base_offset_in_bytes(element_type()) + ((size_t)dst_pos << l2es);
  ArrayAccess<ARRAYCOPY_ATOMIC>::arraycopy<void>(s, src_offset, d, dst_offset, (size_t)length << l2es);
}

// create a klass of array holding typeArrays
Klass* TypeArrayKlass::array_klass(int n, TRAPS) {
  int dim = dimension();
  assert(dim <= n, "check order of chain");
    if (dim == n)
      return this;

  // lock-free read needs acquire semantics
  if (higher_dimension_acquire() == NULL) {

    ResourceMark rm;
    JavaThread *jt = THREAD;
    {
      // Atomic create higher dimension and link into list
      MutexLocker mu(THREAD, MultiArray_lock);

      if (higher_dimension() == NULL) {
        Klass* oak = ObjArrayKlass::allocate_objArray_klass(
              class_loader_data(), dim + 1, this, CHECK_NULL);
        ObjArrayKlass* h_ak = ObjArrayKlass::cast(oak);
        h_ak->set_lower_dimension(this);
        // use 'release' to pair with lock-free load
        release_set_higher_dimension(h_ak);
        assert(h_ak->is_objArray_klass(), "incorrect initialization of ObjArrayKlass");
      }
    }
  }

  ObjArrayKlass* h_ak = ObjArrayKlass::cast(higher_dimension());
  THREAD->check_possible_safepoint();
  return h_ak->array_klass(n, THREAD);
}

// return existing klass of array holding typeArrays
Klass* TypeArrayKlass::array_klass_or_null(int n) {
  int dim = dimension();
  assert(dim <= n, "check order of chain");
    if (dim == n)
      return this;

  // lock-free read needs acquire semantics
  if (higher_dimension_acquire() == NULL) {
    return NULL;
  }

  ObjArrayKlass* h_ak = ObjArrayKlass::cast(higher_dimension());
  return h_ak->array_klass_or_null(n);
}

Klass* TypeArrayKlass::array_klass(TRAPS) {
  return array_klass(dimension() +  1, THREAD);
}

Klass* TypeArrayKlass::array_klass_or_null() {
  return array_klass_or_null(dimension() +  1);
}

int TypeArrayKlass::oop_size(oop obj) const {
  assert(obj->is_typeArray(),"must be a type array");
  typeArrayOop t = typeArrayOop(obj);
  return t->object_size(this);
}

void TypeArrayKlass::initialize(TRAPS) {
  // Nothing to do. Having this function is handy since objArrayKlasses can be
  // initialized by calling initialize on their bottom_klass, see ObjArrayKlass::initialize
}

const char* TypeArrayKlass::external_name(BasicType type) {
  switch (type) {
    case T_BOOLEAN: return "[Z";
    case T_CHAR:    return "[C";
    case T_FLOAT:   return "[F";
    case T_DOUBLE:  return "[D";
    case T_BYTE:    return "[B";
    case T_SHORT:   return "[S";
    case T_INT:     return "[I";
    case T_LONG:    return "[J";
    default: ShouldNotReachHere();
  }
  return NULL;
}


// Printing

void TypeArrayKlass::print_on(outputStream* st) const {
#ifndef PRODUCT
  assert(is_klass(), "must be klass");
  print_value_on(st);
  Klass::print_on(st);
#endif //PRODUCT
}

void TypeArrayKlass::print_value_on(outputStream* st) const {
  assert(is_klass(), "must be klass");
  st->print("{type array ");
  BasicType bt = element_type();
  if (bt == T_BOOLEAN) {
    st->print("bool");
  } else {
    st->print("%s", type2name_tab[bt]);
  }
  st->print("}");
}

#ifndef PRODUCT

static void print_boolean_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    st->print_cr(" - %3d: %s", index, (ta->bool_at(index) == 0) ? "false" : "true");
  }
}


static void print_char_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    jchar c = ta->char_at(index);
    st->print_cr(" - %3d: %x %c", index, c, isprint(c) ? c : ' ');
  }
}


static void print_float_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    st->print_cr(" - %3d: %g", index, ta->float_at(index));
  }
}


static void print_double_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    st->print_cr(" - %3d: %g", index, ta->double_at(index));
  }
}


static void print_byte_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    jbyte c = ta->byte_at(index);
    st->print_cr(" - %3d: %x %c", index, c, isprint(c) ? c : ' ');
  }
}


static void print_short_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    int v = ta->ushort_at(index);
    st->print_cr(" - %3d: 0x%x\t %d", index, v, v);
  }
}


static void print_int_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    jint v = ta->int_at(index);
    st->print_cr(" - %3d: 0x%x %d", index, v, v);
  }
}


static void print_long_array(typeArrayOop ta, int print_len, outputStream* st) {
  for (int index = 0; index < print_len; index++) {
    jlong v = ta->long_at(index);
    st->print_cr(" - %3d: 0x%x 0x%x", index, high(v), low(v));
  }
}


void TypeArrayKlass::oop_print_on(oop obj, outputStream* st) {
  ArrayKlass::oop_print_on(obj, st);
  typeArrayOop ta = typeArrayOop(obj);
  int print_len = MIN2((intx) ta->length(), MaxElementPrintSize);
  switch (element_type()) {
    case T_BOOLEAN: print_boolean_array(ta, print_len, st); break;
    case T_CHAR:    print_char_array(ta, print_len, st);    break;
    case T_FLOAT:   print_float_array(ta, print_len, st);   break;
    case T_DOUBLE:  print_double_array(ta, print_len, st);  break;
    case T_BYTE:    print_byte_array(ta, print_len, st);    break;
    case T_SHORT:   print_short_array(ta, print_len, st);   break;
    case T_INT:     print_int_array(ta, print_len, st);     break;
    case T_LONG:    print_long_array(ta, print_len, st);    break;
    default: ShouldNotReachHere();
  }
  int remaining = ta->length() - print_len;
  if (remaining > 0) {
    st->print_cr(" - <%d more elements, increase MaxElementPrintSize to print>", remaining);
  }
}

#endif // PRODUCT

const char* TypeArrayKlass::internal_name() const {
  return Klass::external_name();
}

// A TypeArrayKlass is an array of a primitive type, its defining module is java.base
ModuleEntry* TypeArrayKlass::module() const {
  return ModuleEntryTable::javabase_moduleEntry();
}

PackageEntry* TypeArrayKlass::package() const {
  return NULL;
}
