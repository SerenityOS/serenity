/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_CODE_OOPRECORDER_HPP
#define SHARE_CODE_OOPRECORDER_HPP

#include "runtime/handles.hpp"
#include "utilities/growableArray.hpp"

// Recording and retrieval of either oop relocations or metadata in compiled code.

class CodeBlob;

template <class T> class ValueRecorder : public StackObj {
 public:
  // A two-way mapping from positive indexes to oop handles.
  // The zero index is reserved for a constant (sharable) null.
  // Indexes may not be negative.

  // Use the given arena to manage storage, if not NULL.
  // By default, uses the current ResourceArea.
  ValueRecorder(Arena* arena = NULL);

  // Generate a new index on which nmethod::oop_addr_at will work.
  // allocate_index and find_index never return the same index,
  // and allocate_index never returns the same index twice.
  // In fact, two successive calls to allocate_index return successive ints.
  int allocate_index(T h) {
    return add_handle(h, false);
  }

  // For a given jobject or Metadata*, this will return the same index
  // repeatedly. The index can later be given to nmethod::oop_at or
  // metadata_at to retrieve the oop.
  // However, the oop must not be changed via nmethod::oop_addr_at.
  int find_index(T h) {
    int index = maybe_find_index(h);
    if (index < 0) {  // previously unallocated
      index = add_handle(h, true);
    }
    return index;
  }

  // returns the size of the generated oop/metadata table, for sizing the
  // CodeBlob. Must be called after all oops are allocated!
  int size();

  // Retrieve the value at a given index.
  T at(int index);

  int count() {
    if (_handles == NULL) return 0;
    // there is always a NULL virtually present as first object
    return _handles->length() + first_index;
  }

  // Helper function; returns false for NULL or Universe::non_oop_word().
  inline bool is_real(T h);

  // copy the generated table to nmethod
  void copy_values_to(nmethod* nm);

  bool is_unused() { return _handles == NULL && !_complete; }
#ifdef ASSERT
  bool is_complete() { return _complete; }
#endif

 private:
  // variant of find_index which does not allocate if not found (yields -1)
  int maybe_find_index(T h);

  // leaky hash table of handle => index, to help detect duplicate insertion
  template <class X> class IndexCache : public ResourceObj {
    // This class is only used by the ValueRecorder class.
    friend class ValueRecorder;
    enum {
      _log_cache_size = 9,
      _cache_size = (1<<_log_cache_size),
      // Index entries are ints.  The LSBit is a collision indicator.
      _collision_bit_shift = 0,
      _collision_bit = 1,
      _index_shift = _collision_bit_shift+1
    };
    int _cache[_cache_size];
    static juint cache_index(X handle) {
      juint ci = (int) (intptr_t) handle;
      ci ^= ci >> (BitsPerByte*2);
      ci += ci >> (BitsPerByte*1);
      return ci & (_cache_size-1);
    }
    int* cache_location(X handle) {
      return &_cache[ cache_index(handle) ];
    }
    static bool cache_location_collision(int* cloc) {
      return ((*cloc) & _collision_bit) != 0;
    }
    static int cache_location_index(int* cloc) {
      return (*cloc) >> _index_shift;
    }
    static void set_cache_location_index(int* cloc, int index) {
      int cval0 = (*cloc);
      int cval1 = (index << _index_shift);
      if (cval0 != 0 && cval1 != cval0)  cval1 += _collision_bit;
      (*cloc) = cval1;
    }
    IndexCache();
  };

  void maybe_initialize();
  int add_handle(T h, bool make_findable);

  enum { null_index = 0, first_index = 1, index_cache_threshold = 20 };

  GrowableArray<T>*        _handles;  // ordered list (first is always NULL)
  GrowableArray<int>*       _no_finds; // all unfindable indexes; usually empty
  IndexCache<T>*           _indexes;  // map: handle -> its probable index
  Arena*                    _arena;
  bool                      _complete;

#ifdef ASSERT
  static int _find_index_calls, _hit_indexes, _missed_indexes;
#endif
};

class OopRecorder;

class ObjectLookup : public ResourceObj {
 private:
  class ObjectEntry {
   private:
    jobject _value;
    int     _index;

   public:
    ObjectEntry(jobject value, int index) : _value(value), _index(index) {}
    ObjectEntry() : _value(NULL), _index(0) {}
    oop oop_value() const;
    int index() { return _index; }
  };

  GrowableArray<ObjectEntry> _values;
  unsigned int _gc_count;

  // Utility sort functions
  static int sort_by_address(oop a, oop b);
  static int sort_by_address(ObjectEntry* a, ObjectEntry* b);
  static int sort_oop_by_address(oop const& a, ObjectEntry const& b);

 public:
  ObjectLookup();

  // Resort list if a GC has occurred since the last sort
  void maybe_resort();
  int find_index(jobject object, OopRecorder* oop_recorder);
};

class OopRecorder : public ResourceObj {
 private:
  ValueRecorder<jobject>      _oops;
  ValueRecorder<Metadata*>    _metadata;
  ObjectLookup*               _object_lookup;
 public:
  OopRecorder(Arena* arena = NULL, bool deduplicate = false): _oops(arena), _metadata(arena) {
    if (deduplicate) {
      _object_lookup = new ObjectLookup();
    } else {
      _object_lookup = NULL;
    }
  }

  int allocate_oop_index(jobject h) {
    return _oops.allocate_index(h);
  }
  virtual int find_index(jobject h) {
    return _object_lookup != NULL ? _object_lookup->find_index(h, this) : _oops.find_index(h);
  }
  jobject oop_at(int index) {
    return _oops.at(index);
  }
  int oop_size() {
    return _oops.size();
  }
  int oop_count() {
    return _oops.count();
  }
  inline bool is_real(jobject h);

  int allocate_metadata_index(Metadata* oop) {
    return _metadata.allocate_index(oop);
  }
  virtual int find_index(Metadata* h) {
    return _metadata.find_index(h);
  }
  Metadata* metadata_at(int index) {
    return _metadata.at(index);
  }
  int metadata_size() {
    return _metadata.size();
  }
  int metadata_count() {
    return _metadata.count();
  }
  inline bool is_real(Metadata* h);

  bool is_unused() {
    return _oops.is_unused() && _metadata.is_unused();
  }

  void freeze() {
    _oops.size();
    _metadata.size();
  }

  void copy_values_to(nmethod* nm) {
    if (!_oops.is_unused()) {
      _oops.copy_values_to(nm);
    }
    if (!_metadata.is_unused()) {
      _metadata.copy_values_to(nm);
    }
  }

#ifdef ASSERT
  bool is_complete() {
    assert(_oops.is_complete() == _metadata.is_complete(), "must agree");
    return _oops.is_complete();
  }
#endif
};


#endif // SHARE_CODE_OOPRECORDER_HPP
