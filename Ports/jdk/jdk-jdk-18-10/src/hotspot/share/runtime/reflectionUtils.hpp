/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_REFLECTIONUTILS_HPP
#define SHARE_RUNTIME_REFLECTIONUTILS_HPP

#include "memory/allocation.hpp"
#include "oops/instanceKlass.hpp"
#include "oops/objArrayOop.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/handles.hpp"
#include "runtime/reflection.hpp"
#include "utilities/accessFlags.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/growableArray.hpp"

// A KlassStream is an abstract stream for streaming over self, superclasses
// and (super)interfaces. Streaming is done in reverse order (subclasses first,
// interfaces last).
//
//    for (KlassStream st(k, false, false, false); !st.eos(); st.next()) {
//      Klass* k = st.klass();
//      ...
//    }

class KlassStream {
 protected:
  InstanceKlass*      _klass;           // current klass/interface iterated over
  InstanceKlass*      _base_klass;      // initial klass/interface to iterate over
  Array<InstanceKlass*>*_interfaces;    // transitive interfaces for initial class
  int                 _interface_index; // current interface being processed
  bool                _local_only;      // process initial class/interface only
  bool                _classes_only;    // process classes only (no interfaces)
  bool                _walk_defaults;   // process default methods
  bool                _base_class_search_defaults; // time to process default methods
  bool                _defaults_checked; // already checked for default methods
  int                 _index;

  virtual int length() = 0;

 public:
  // constructor
  KlassStream(InstanceKlass* klass, bool local_only, bool classes_only, bool walk_defaults);

  // testing
  bool eos();

  // iterating
  virtual void next() = 0;

  // accessors
  InstanceKlass* klass() const      { return _klass; }
  int index() const                 { return _index; }
  bool base_class_search_defaults() const { return _base_class_search_defaults; }
  void base_class_search_defaults(bool b) { _base_class_search_defaults = b; }
};


// A MethodStream streams over all methods in a class, superclasses and (super)interfaces.
// Streaming is done in reverse order (subclasses first, methods in reverse order)
// Usage:
//
//    for (MethodStream st(k, false, false); !st.eos(); st.next()) {
//      Method* m = st.method();
//      ...
//    }

class MethodStream : public KlassStream {
 private:
  int length()                    { return methods()->length(); }
  Array<Method*>* methods() {
    if (base_class_search_defaults()) {
      base_class_search_defaults(false);
      return _klass->default_methods();
    } else {
      return _klass->methods();
    }
  }
 public:
  MethodStream(InstanceKlass* klass, bool local_only, bool classes_only)
    : KlassStream(klass, local_only, classes_only, true) {
    _index = length();
    next();
  }

  void next() { _index--; }
  Method* method() { return methods()->at(index()); }
};


// A FieldStream streams over all fields in a class, superclasses and (super)interfaces.
// Streaming is done in reverse order (subclasses first, fields in reverse order)
// Usage:
//
//    for (FieldStream st(k, false, false); !st.eos(); st.next()) {
//      Symbol* field_name = st.name();
//      ...
//    }


class FieldStream : public KlassStream {
 private:
  int length() { return _klass->java_fields_count(); }

  fieldDescriptor _fd_buf;

 public:
  FieldStream(InstanceKlass* klass, bool local_only, bool classes_only)
    : KlassStream(klass, local_only, classes_only, false) {
    _index = length();
    next();
  }

  void next() { _index -= 1; }

  // Accessors for current field
  AccessFlags access_flags() const {
    AccessFlags flags;
    flags.set_flags(_klass->field_access_flags(_index));
    return flags;
  }
  Symbol* name() const {
    return _klass->field_name(_index);
  }
  Symbol* signature() const {
    return _klass->field_signature(_index);
  }
  // missing: initval()
  int offset() const {
    return _klass->field_offset( index() );
  }
  // bridge to a heavier API:
  fieldDescriptor& field_descriptor() const {
    fieldDescriptor& field = const_cast<fieldDescriptor&>(_fd_buf);
    field.reinitialize(_klass, _index);
    return field;
  }
};

class FilteredField : public CHeapObj<mtInternal>  {
 private:
  Klass* _klass;
  int    _field_offset;

 public:
  FilteredField(Klass* klass, int field_offset) {
    _klass = klass;
    _field_offset = field_offset;
  }
  Klass* klass() { return _klass; }
  int  field_offset() { return _field_offset; }
};

class FilteredFieldsMap : AllStatic {
 private:
  static GrowableArray<FilteredField *> *_filtered_fields;
 public:
  static void initialize();
  static bool is_filtered_field(Klass* klass, int field_offset) {
    for (int i=0; i < _filtered_fields->length(); i++) {
      if (klass == _filtered_fields->at(i)->klass() &&
        field_offset == _filtered_fields->at(i)->field_offset()) {
        return true;
      }
    }
    return false;
  }
  static int  filtered_fields_count(Klass* klass, bool local_only) {
    int nflds = 0;
    for (int i=0; i < _filtered_fields->length(); i++) {
      if (local_only && klass == _filtered_fields->at(i)->klass()) {
        nflds++;
      } else if (klass->is_subtype_of(_filtered_fields->at(i)->klass())) {
        nflds++;
      }
    }
    return nflds;
  }
};


// A FilteredFieldStream streams over all fields in a class, superclasses and
// (super)interfaces. Streaming is done in reverse order (subclasses first,
// fields in reverse order)
//
// Usage:
//
//    for (FilteredFieldStream st(k, false, false); !st.eos(); st.next()) {
//      Symbol* field_name = st.name();
//      ...
//    }

class FilteredFieldStream : public FieldStream {
 private:
  int  _filtered_fields_count;
  bool has_filtered_field() { return (_filtered_fields_count > 0); }

 public:
  FilteredFieldStream(InstanceKlass* klass, bool local_only, bool classes_only)
    : FieldStream(klass, local_only, classes_only) {
    _filtered_fields_count = FilteredFieldsMap::filtered_fields_count(klass, local_only);
  }
  int field_count();
  void next() {
    _index -= 1;
    if (has_filtered_field()) {
      while (_index >=0 && FilteredFieldsMap::is_filtered_field((Klass*)_klass, offset())) {
        _index -= 1;
      }
    }
  }
};

#endif // SHARE_RUNTIME_REFLECTIONUTILS_HPP
