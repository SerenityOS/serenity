
/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARED_CDS_DUMPTIMESHAREDCLASSINFO_HPP
#define SHARED_CDS_DUMPTIMESHAREDCLASSINFO_HPP
#include "cds/archiveBuilder.hpp"
#include "cds/archiveUtils.hpp"
#include "cds/metaspaceShared.hpp"
#include "classfile/compactHashtable.hpp"
#include "memory/metaspaceClosure.hpp"
#include "oops/instanceKlass.hpp"
#include "prims/jvmtiExport.hpp"
#include "utilities/growableArray.hpp"

class Method;
class Symbol;

class DumpTimeClassInfo: public CHeapObj<mtClass> {
  bool                         _excluded;
  bool                         _is_early_klass;
  bool                         _has_checked_exclusion;
public:
  struct DTLoaderConstraint {
    Symbol* _name;
    char _loader_type1;
    char _loader_type2;
    DTLoaderConstraint(Symbol* name, char l1, char l2) : _name(name), _loader_type1(l1), _loader_type2(l2) {
      _name->increment_refcount();
    }
    DTLoaderConstraint() : _name(NULL), _loader_type1('0'), _loader_type2('0') {}
    bool equals(const DTLoaderConstraint& t) {
      return t._name == _name &&
             ((t._loader_type1 == _loader_type1 && t._loader_type2 == _loader_type2) ||
              (t._loader_type2 == _loader_type1 && t._loader_type1 == _loader_type2));
    }
  };

  struct DTVerifierConstraint {
    Symbol* _name;
    Symbol* _from_name;
    DTVerifierConstraint() : _name(NULL), _from_name(NULL) {}
    DTVerifierConstraint(Symbol* n, Symbol* fn) : _name(n), _from_name(fn) {
      _name->increment_refcount();
      _from_name->increment_refcount();
    }
  };

  InstanceKlass*               _klass;
  InstanceKlass*               _nest_host;
  bool                         _failed_verification;
  bool                         _is_archived_lambda_proxy;
  int                          _id;
  int                          _clsfile_size;
  int                          _clsfile_crc32;
  GrowableArray<DTVerifierConstraint>* _verifier_constraints;
  GrowableArray<char>*                 _verifier_constraint_flags;
  GrowableArray<DTLoaderConstraint>*   _loader_constraints;

  DumpTimeClassInfo() {
    _klass = NULL;
    _nest_host = NULL;
    _failed_verification = false;
    _is_archived_lambda_proxy = false;
    _has_checked_exclusion = false;
    _id = -1;
    _clsfile_size = -1;
    _clsfile_crc32 = -1;
    _excluded = false;
    _is_early_klass = JvmtiExport::is_early_phase();
    _verifier_constraints = NULL;
    _verifier_constraint_flags = NULL;
    _loader_constraints = NULL;
  }

  void add_verification_constraint(InstanceKlass* k, Symbol* name,
         Symbol* from_name, bool from_field_is_protected, bool from_is_array, bool from_is_object);
  void record_linking_constraint(Symbol* name, Handle loader1, Handle loader2);

  bool is_builtin();

  int num_verifier_constraints() {
    if (_verifier_constraint_flags != NULL) {
      return _verifier_constraint_flags->length();
    } else {
      return 0;
    }
  }

  int num_loader_constraints() {
    if (_loader_constraints != NULL) {
      return _loader_constraints->length();
    } else {
      return 0;
    }
  }

  void metaspace_pointers_do(MetaspaceClosure* it) {
    it->push(&_klass);
    it->push(&_nest_host);
    if (_verifier_constraints != NULL) {
      for (int i = 0; i < _verifier_constraints->length(); i++) {
        DTVerifierConstraint* cons = _verifier_constraints->adr_at(i);
        it->push(&cons->_name);
        it->push(&cons->_from_name);
      }
    }
    if (_loader_constraints != NULL) {
      for (int i = 0; i < _loader_constraints->length(); i++) {
        DTLoaderConstraint* lc = _loader_constraints->adr_at(i);
        it->push(&lc->_name);
      }
    }
  }

  bool is_excluded() {
    // _klass may become NULL due to DynamicArchiveBuilder::set_to_null
    return _excluded || _failed_verification || _klass == NULL;
  }

  // Was this class loaded while JvmtiExport::is_early_phase()==true
  bool is_early_klass() {
    return _is_early_klass;
  }

  // simple accessors
  void set_excluded()                               { _excluded = true; }
  bool has_checked_exclusion() const                { return _has_checked_exclusion; }
  void set_has_checked_exclusion()                  { _has_checked_exclusion = true; }
  bool failed_verification() const                  { return _failed_verification; }
  void set_failed_verification()                    { _failed_verification = true; }
  InstanceKlass* nest_host() const                  { return _nest_host; }
  void set_nest_host(InstanceKlass* nest_host)      { _nest_host = nest_host; }
  DumpTimeClassInfo clone();
};


inline unsigned DumpTimeSharedClassTable_hash(InstanceKlass* const& k) {
  if (DumpSharedSpaces) {
    // Deterministic archive contents
    uintx delta = k->name() - MetaspaceShared::symbol_rs_base();
    return primitive_hash<uintx>(delta);
  } else {
    // Deterministic archive is not possible because classes can be loaded
    // in multiple threads.
    return primitive_hash<InstanceKlass*>(k);
  }
}

class DumpTimeSharedClassTable: public ResourceHashtable<
  InstanceKlass*,
  DumpTimeClassInfo,
  15889, // prime number
  ResourceObj::C_HEAP,
  mtClassShared,
  &DumpTimeSharedClassTable_hash>
{
  int _builtin_count;
  int _unregistered_count;
public:
  DumpTimeSharedClassTable() {
    _builtin_count = 0;
    _unregistered_count = 0;
  }
  DumpTimeClassInfo* find_or_allocate_info_for(InstanceKlass* k, bool dump_in_progress);
  void inc_builtin_count()      { _builtin_count++; }
  void inc_unregistered_count() { _unregistered_count++; }
  void update_counts();
  int count_of(bool is_builtin) const {
    if (is_builtin) {
      return _builtin_count;
    } else {
      return _unregistered_count;
    }
  }
};

#endif // SHARED_CDS_DUMPTIMESHAREDCLASSINFO_HPP
