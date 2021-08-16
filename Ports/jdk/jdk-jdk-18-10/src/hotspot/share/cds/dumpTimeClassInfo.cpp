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

#include "precompiled.hpp"
#include "cds/archiveBuilder.hpp"
#include "cds/dumpTimeClassInfo.hpp"
#include "classfile/classLoader.hpp"
#include "classfile/classLoaderData.inline.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "memory/resourceArea.hpp"

DumpTimeClassInfo DumpTimeClassInfo::clone() {
  DumpTimeClassInfo clone;
  clone._klass = _klass;
  clone._nest_host = _nest_host;
  clone._failed_verification = _failed_verification;
  clone._is_archived_lambda_proxy = _is_archived_lambda_proxy;
  clone._has_checked_exclusion = _has_checked_exclusion;
  clone._id = _id;
  clone._clsfile_size = _clsfile_size;
  clone._clsfile_crc32 = _clsfile_crc32;
  clone._excluded = _excluded;
  clone._is_early_klass = _is_early_klass;
  clone._verifier_constraints = NULL;
  clone._verifier_constraint_flags = NULL;
  clone._loader_constraints = NULL;
  int clone_num_verifier_constraints = num_verifier_constraints();
  if (clone_num_verifier_constraints > 0) {
    clone._verifier_constraints = new (ResourceObj::C_HEAP, mtClass) GrowableArray<DTVerifierConstraint>(clone_num_verifier_constraints, mtClass);
    clone._verifier_constraint_flags = new (ResourceObj::C_HEAP, mtClass) GrowableArray<char>(clone_num_verifier_constraints, mtClass);
    for (int i = 0; i < clone_num_verifier_constraints; i++) {
      clone._verifier_constraints->append(_verifier_constraints->at(i));
      clone._verifier_constraint_flags->append(_verifier_constraint_flags->at(i));
    }
  }
  int clone_num_loader_constraints = num_loader_constraints();
  if (clone_num_loader_constraints > 0) {
    clone._loader_constraints = new (ResourceObj::C_HEAP, mtClass) GrowableArray<DTLoaderConstraint>(clone_num_loader_constraints, mtClass);
    for (int i = 0; i < clone_num_loader_constraints; i++) {
      clone._loader_constraints->append(_loader_constraints->at(i));
    }
  }
  return clone;
}

void DumpTimeClassInfo::add_verification_constraint(InstanceKlass* k, Symbol* name,
         Symbol* from_name, bool from_field_is_protected, bool from_is_array, bool from_is_object) {
  if (_verifier_constraints == NULL) {
    _verifier_constraints = new (ResourceObj::C_HEAP, mtClass) GrowableArray<DTVerifierConstraint>(4, mtClass);
  }
  if (_verifier_constraint_flags == NULL) {
    _verifier_constraint_flags = new (ResourceObj::C_HEAP, mtClass) GrowableArray<char>(4, mtClass);
  }
  GrowableArray<DTVerifierConstraint>* vc_array = _verifier_constraints;
  for (int i = 0; i < vc_array->length(); i++) {
    DTVerifierConstraint* p = vc_array->adr_at(i);
    if (name == p->_name && from_name == p->_from_name) {
      return;
    }
  }
  DTVerifierConstraint cons(name, from_name);
  vc_array->append(cons);

  GrowableArray<char>* vcflags_array = _verifier_constraint_flags;
  char c = 0;
  c |= from_field_is_protected ? SystemDictionaryShared::FROM_FIELD_IS_PROTECTED : 0;
  c |= from_is_array           ? SystemDictionaryShared::FROM_IS_ARRAY           : 0;
  c |= from_is_object          ? SystemDictionaryShared::FROM_IS_OBJECT          : 0;
  vcflags_array->append(c);

  if (log_is_enabled(Trace, cds, verification)) {
    ResourceMark rm;
    log_trace(cds, verification)("add_verification_constraint: %s: %s must be subclass of %s [0x%x] array len %d flags len %d",
                                 k->external_name(), from_name->as_klass_external_name(),
                                 name->as_klass_external_name(), c, vc_array->length(), vcflags_array->length());
  }
}

static char get_loader_type_by(oop  loader) {
  assert(SystemDictionary::is_builtin_class_loader(loader), "Must be built-in loader");
  if (SystemDictionary::is_boot_class_loader(loader)) {
    return (char)ClassLoader::BOOT_LOADER;
  } else if (SystemDictionary::is_platform_class_loader(loader)) {
    return (char)ClassLoader::PLATFORM_LOADER;
  } else {
    assert(SystemDictionary::is_system_class_loader(loader), "Class loader mismatch");
    return (char)ClassLoader::APP_LOADER;
  }
}

void DumpTimeClassInfo::record_linking_constraint(Symbol* name, Handle loader1, Handle loader2) {
  assert(loader1 != loader2, "sanity");
  LogTarget(Info, class, loader, constraints) log;
  if (_loader_constraints == NULL) {
    _loader_constraints = new (ResourceObj::C_HEAP, mtClass) GrowableArray<DTLoaderConstraint>(4, mtClass);
  }
  char lt1 = get_loader_type_by(loader1());
  char lt2 = get_loader_type_by(loader2());
  DTLoaderConstraint lc(name, lt1, lt2);
  for (int i = 0; i < _loader_constraints->length(); i++) {
    DTLoaderConstraint dt = _loader_constraints->at(i);
    if (lc.equals(dt)) {
      if (log.is_enabled()) {
        ResourceMark rm;
        // Use loader[0]/loader[1] to be consistent with the logs in loaderConstraints.cpp
        log.print("[CDS record loader constraint for class: %s constraint_name: %s loader[0]: %s loader[1]: %s already added]",
                  _klass->external_name(), name->as_C_string(),
                  ClassLoaderData::class_loader_data(loader1())->loader_name_and_id(),
                  ClassLoaderData::class_loader_data(loader2())->loader_name_and_id());
      }
      return;
    }
  }
  _loader_constraints->append(lc);
  if (log.is_enabled()) {
    ResourceMark rm;
    // Use loader[0]/loader[1] to be consistent with the logs in loaderConstraints.cpp
    log.print("[CDS record loader constraint for class: %s constraint_name: %s loader[0]: %s loader[1]: %s total %d]",
              _klass->external_name(), name->as_C_string(),
              ClassLoaderData::class_loader_data(loader1())->loader_name_and_id(),
              ClassLoaderData::class_loader_data(loader2())->loader_name_and_id(),
              _loader_constraints->length());
  }
}

bool DumpTimeClassInfo::is_builtin() {
  return SystemDictionaryShared::is_builtin(_klass);
}

DumpTimeClassInfo* DumpTimeSharedClassTable::find_or_allocate_info_for(InstanceKlass* k, bool dump_in_progress) {
  bool created = false;
  DumpTimeClassInfo* p;
  if (!dump_in_progress) {
    p = put_if_absent(k, &created);
  } else {
    p = get(k);
  }
  if (created) {
    assert(!SystemDictionaryShared::no_class_loading_should_happen(),
           "no new classes can be loaded while dumping archive");
    p->_klass = k;
  } else {
    if (!dump_in_progress) {
      assert(p->_klass == k, "Sanity");
    }
  }
  return p;
}

class CountClassByCategory : StackObj {
  DumpTimeSharedClassTable* _table;
public:
  CountClassByCategory(DumpTimeSharedClassTable* table) : _table(table) {}
  bool do_entry(InstanceKlass* k, DumpTimeClassInfo& info) {
    if (!info.is_excluded()) {
      if (info.is_builtin()) {
        _table->inc_builtin_count();
      } else {
        _table->inc_unregistered_count();
      }
    }
    return true; // keep on iterating
  }
};

void DumpTimeSharedClassTable::update_counts() {
  _builtin_count = 0;
  _unregistered_count = 0;
  CountClassByCategory counter(this);
  iterate(&counter);
}
