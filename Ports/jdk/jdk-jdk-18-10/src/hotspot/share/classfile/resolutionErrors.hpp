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

#ifndef SHARE_CLASSFILE_RESOLUTIONERRORS_HPP
#define SHARE_CLASSFILE_RESOLUTIONERRORS_HPP

#include "oops/constantPool.hpp"
#include "utilities/hashtable.hpp"

class ResolutionErrorEntry;

// ResolutionError objects are used to record errors encountered during
// constant pool resolution (JVMS 5.4.3).

// This value is added to the cpCache index of an invokedynamic instruction when
// storing the resolution error resulting from that invokedynamic instruction.
// This prevents issues where the cpCache index is the same as the constant pool
// index of another entry in the table.
const int CPCACHE_INDEX_MANGLE_VALUE = 1000000;

class ResolutionErrorTable : public Hashtable<ConstantPool*, mtClass> {

private:
  void free_entry(ResolutionErrorEntry *entry);

public:
  ResolutionErrorTable(int table_size);

  ResolutionErrorEntry* bucket(int i) {
    return (ResolutionErrorEntry*)Hashtable<ConstantPool*, mtClass>::bucket(i);
  }

  ResolutionErrorEntry** bucket_addr(int i) {
    return (ResolutionErrorEntry**)Hashtable<ConstantPool*, mtClass>::bucket_addr(i);
  }

  void add_entry(int index, ResolutionErrorEntry* new_entry) {
    Hashtable<ConstantPool*, mtClass>::add_entry(index,
      (HashtableEntry<ConstantPool*, mtClass>*)new_entry);
  }

  void add_entry(int index, unsigned int hash,
                 const constantPoolHandle& pool, int which, Symbol* error, Symbol* message,
                 Symbol* cause, Symbol* cause_msg);

  void add_entry(int index, unsigned int hash,
                 const constantPoolHandle& pool, int which, const char* message);

  // find error given the constant pool and constant pool index
  ResolutionErrorEntry* find_entry(int index, unsigned int hash,
                                   const constantPoolHandle& pool, int cp_index);


  unsigned int compute_hash(const constantPoolHandle& pool, int cp_index) {
    return (unsigned int) pool->identity_hash() + cp_index;
  }

  // purges unloaded entries from the table
  void purge_resolution_errors();

  // RedefineClasses support - remove obsolete constant pool entry
  void delete_entry(ConstantPool* c);

  // This function is used to encode an index to differentiate it from a
  // constant pool index.  It assumes it is being called with a cpCache index
  // (that is less than 0).
  static int encode_cpcache_index(int index) {
    assert(index < 0, "Unexpected non-negative cpCache index");
    return index + CPCACHE_INDEX_MANGLE_VALUE;
  }
};


class ResolutionErrorEntry : public HashtableEntry<ConstantPool*, mtClass> {
 private:
  int               _cp_index;
  Symbol*           _error;
  Symbol*           _message;
  Symbol*           _cause;
  Symbol*           _cause_msg;
  const char*       _nest_host_error;

 public:
  ConstantPool*      pool() const               { return literal(); }

  int                cp_index() const           { return _cp_index; }
  void               set_cp_index(int cp_index) { _cp_index = cp_index; }

  Symbol*            error() const              { return _error; }
  void               set_error(Symbol* e);

  Symbol*            message() const            { return _message; }
  void               set_message(Symbol* c);

  Symbol*            cause() const              { return _cause; }
  void               set_cause(Symbol* c);

  Symbol*            cause_msg() const          { return _cause_msg; }
  void               set_cause_msg(Symbol* c);

  const char*        nest_host_error() const    { return _nest_host_error; }
  void               set_nest_host_error(const char* message);

  ResolutionErrorEntry* next() const {
    return (ResolutionErrorEntry*)HashtableEntry<ConstantPool*, mtClass>::next();
  }

  ResolutionErrorEntry** next_addr() {
    return (ResolutionErrorEntry**)HashtableEntry<ConstantPool*, mtClass>::next_addr();
  }
};

#endif // SHARE_CLASSFILE_RESOLUTIONERRORS_HPP
