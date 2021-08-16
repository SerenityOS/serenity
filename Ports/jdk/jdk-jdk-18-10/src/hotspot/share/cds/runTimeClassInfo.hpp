
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

#ifndef SHARED_CDS_SHAREDCLASSINFO_HPP
#define SHARED_CDS_SHAREDCLASSINFO_HPP
#include "classfile/compactHashtable.hpp"
#include "classfile/javaClasses.hpp"
#include "classfile/systemDictionaryShared.hpp"
#include "cds/archiveBuilder.hpp"
#include "cds/archiveUtils.hpp"
#include "cds/metaspaceShared.hpp"
#include "memory/metaspaceClosure.hpp"
#include "oops/instanceKlass.hpp"
#include "prims/jvmtiExport.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/resourceHash.hpp"

class Method;
class Symbol;

class RunTimeClassInfo {
public:
  struct CrcInfo {
    int _clsfile_size;
    int _clsfile_crc32;
  };

  // This is different than  DumpTimeClassInfo::DTVerifierConstraint. We use
  // u4 instead of Symbol* to save space on 64-bit CPU.
  struct RTVerifierConstraint {
    u4 _name;
    u4 _from_name;
    Symbol* name() { return (Symbol*)(SharedBaseAddress + _name);}
    Symbol* from_name() { return (Symbol*)(SharedBaseAddress + _from_name); }
  };

  struct RTLoaderConstraint {
    u4   _name;
    char _loader_type1;
    char _loader_type2;
    Symbol* constraint_name() {
      return (Symbol*)(SharedBaseAddress + _name);
    }
  };

  InstanceKlass* _klass;
  int _num_verifier_constraints;
  int _num_loader_constraints;

  // optional CrcInfo              _crc;  (only for UNREGISTERED classes)
  // optional InstanceKlass*       _nest_host
  // optional RTLoaderConstraint   _loader_constraint_types[_num_loader_constraints]
  // optional RTVerifierConstraint _verifier_constraints[_num_verifier_constraints]
  // optional char                 _verifier_constraint_flags[_num_verifier_constraints]

private:
  static size_t header_size_size() {
    return sizeof(RunTimeClassInfo);
  }
  static size_t verifier_constraints_size(int num_verifier_constraints) {
    return sizeof(RTVerifierConstraint) * num_verifier_constraints;
  }
  static size_t verifier_constraint_flags_size(int num_verifier_constraints) {
    return sizeof(char) * num_verifier_constraints;
  }
  static size_t loader_constraints_size(int num_loader_constraints) {
    return sizeof(RTLoaderConstraint) * num_loader_constraints;
  }
  static size_t nest_host_size(InstanceKlass* klass) {
    if (klass->is_hidden()) {
      return sizeof(InstanceKlass*);
    } else {
      return 0;
    }
  }

  static size_t crc_size(InstanceKlass* klass);
public:
  static size_t byte_size(InstanceKlass* klass, int num_verifier_constraints, int num_loader_constraints) {
    return header_size_size() +
           crc_size(klass) +
           nest_host_size(klass) +
           loader_constraints_size(num_loader_constraints) +
           verifier_constraints_size(num_verifier_constraints) +
           verifier_constraint_flags_size(num_verifier_constraints);
  }

private:
  size_t crc_offset() const {
    return header_size_size();
  }

  size_t nest_host_offset() const {
      return crc_offset() + crc_size(_klass);
  }

  size_t loader_constraints_offset() const  {
    return nest_host_offset() + nest_host_size(_klass);
  }
  size_t verifier_constraints_offset() const {
    return loader_constraints_offset() + loader_constraints_size(_num_loader_constraints);
  }
  size_t verifier_constraint_flags_offset() const {
    return verifier_constraints_offset() + verifier_constraints_size(_num_verifier_constraints);
  }

  void check_verifier_constraint_offset(int i) const {
    assert(0 <= i && i < _num_verifier_constraints, "sanity");
  }

  void check_loader_constraint_offset(int i) const {
    assert(0 <= i && i < _num_loader_constraints, "sanity");
  }

public:
  CrcInfo* crc() const {
    assert(crc_size(_klass) > 0, "must be");
    return (CrcInfo*)(address(this) + crc_offset());
  }
  RTVerifierConstraint* verifier_constraints() {
    assert(_num_verifier_constraints > 0, "sanity");
    return (RTVerifierConstraint*)(address(this) + verifier_constraints_offset());
  }
  RTVerifierConstraint* verifier_constraint_at(int i) {
    check_verifier_constraint_offset(i);
    return verifier_constraints() + i;
  }

  char* verifier_constraint_flags() {
    assert(_num_verifier_constraints > 0, "sanity");
    return (char*)(address(this) + verifier_constraint_flags_offset());
  }

  InstanceKlass** nest_host_addr() {
    assert(_klass->is_hidden(), "sanity");
    return (InstanceKlass**)(address(this) + nest_host_offset());
  }
  InstanceKlass* nest_host() {
    return *nest_host_addr();
  }
  void set_nest_host(InstanceKlass* k) {
    *nest_host_addr() = k;
    ArchivePtrMarker::mark_pointer((address*)nest_host_addr());
  }

  RTLoaderConstraint* loader_constraints() {
    assert(_num_loader_constraints > 0, "sanity");
    return (RTLoaderConstraint*)(address(this) + loader_constraints_offset());
  }

  RTLoaderConstraint* loader_constraint_at(int i) {
    check_loader_constraint_offset(i);
    return loader_constraints() + i;
  }

  void init(DumpTimeClassInfo& info);

  bool matches(int clsfile_size, int clsfile_crc32) const {
    return crc()->_clsfile_size  == clsfile_size &&
           crc()->_clsfile_crc32 == clsfile_crc32;
  }

  char verifier_constraint_flag(int i) {
    check_verifier_constraint_offset(i);
    return verifier_constraint_flags()[i];
  }

private:
  // ArchiveBuilder::make_shallow_copy() has reserved a pointer immediately
  // before archived InstanceKlasses. We can use this slot to do a quick
  // lookup of InstanceKlass* -> RunTimeClassInfo* without
  // building a new hashtable.
  //
  //  info_pointer_addr(klass) --> 0x0100   RunTimeClassInfo*
  //  InstanceKlass* klass     --> 0x0108   <C++ vtbl>
  //                               0x0110   fields from Klass ...
  static RunTimeClassInfo** info_pointer_addr(InstanceKlass* klass) {
    return &((RunTimeClassInfo**)klass)[-1];
  }

public:
  static RunTimeClassInfo* get_for(InstanceKlass* klass) {
    assert(klass->is_shared(), "don't call for non-shared class");
    return *info_pointer_addr(klass);
  }
  static void set_for(InstanceKlass* klass, RunTimeClassInfo* record) {
    assert(ArchiveBuilder::current()->is_in_buffer_space(klass), "must be");
    assert(ArchiveBuilder::current()->is_in_buffer_space(record), "must be");
    *info_pointer_addr(klass) = record;
    ArchivePtrMarker::mark_pointer(info_pointer_addr(klass));
  }

  // Used by RunTimeSharedDictionary to implement OffsetCompactHashtable::EQUALS
  static inline bool EQUALS(
       const RunTimeClassInfo* value, Symbol* key, int len_unused) {
    return (value->_klass->name() == key);
  }
};

class RunTimeSharedDictionary : public OffsetCompactHashtable<
  Symbol*,
  const RunTimeClassInfo*,
  RunTimeClassInfo::EQUALS> {};
#endif // SHARED_CDS_SHAREDCLASSINFO_HPP
