/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_INTERPRETER_OOPMAPCACHE_HPP
#define SHARE_INTERPRETER_OOPMAPCACHE_HPP

#include "oops/generateOopMap.hpp"
#include "runtime/mutex.hpp"

// A Cache for storing (method, bci) -> oopMap.
// The memory management system uses the cache when locating object
// references in an interpreted frame.
//
// OopMapCache's are allocated lazily per InstanceKlass.

// The oopMap (InterpreterOopMap) is stored as a bit mask. If the
// bit_mask can fit into two words it is stored in
// the _bit_mask array, otherwise it is allocated on the heap.
// For OopMapCacheEntry the bit_mask is allocated in the C heap
// because these entries persist between garbage collections.
// For InterpreterOopMap the bit_mask is allocated in
// a resource area for better performance.  InterpreterOopMap
// should only be created and deleted during same garbage collection.
//
// If ENABBLE_ZAP_DEAD_LOCALS is defined, two bits are used
// per entry instead of one. In all cases,
// the first bit is set to indicate oops as opposed to other
// values. If the second bit is available,
// it is set for dead values. We get the following encoding:
//
// 00 live value
// 01 live oop
// 10 dead value
// 11 <unused>                                   (we cannot distinguish between dead oops or values with the current oop map generator)


class OffsetClosure  {
 public:
  virtual void offset_do(int offset) = 0;
};

class OopMapCacheEntry;

class InterpreterOopMap: ResourceObj {
  friend class OopMapCache;

 public:
  enum {
    N                = 4,                // the number of words reserved
                                         // for inlined mask storage
    small_mask_limit = N * BitsPerWord,  // the maximum number of bits
                                         // available for small masks,
                                         // small_mask_limit can be set to 0
                                         // for testing bit_mask allocation

    bits_per_entry   = 2,
    dead_bit_number  = 1,
    oop_bit_number   = 0
  };

 private:
  Method*        _method;         // the method for which the mask is valid
  unsigned short _bci;            // the bci    for which the mask is valid
  int            _mask_size;      // the mask size in bits
  int            _expression_stack_size; // the size of the expression stack in slots

 protected:
  intptr_t       _bit_mask[N];    // the bit mask if
                                  // mask_size <= small_mask_limit,
                                  // ptr to bit mask otherwise
                                  // "protected" so that sub classes can
                                  // access it without using trickery in
                                  // methd bit_mask().
#ifdef ASSERT
  bool _resource_allocate_bit_mask;
#endif

  // access methods
  Method*        method() const                  { return _method; }
  void           set_method(Method* v)           { _method = v; }
  int            bci() const                     { return _bci; }
  void           set_bci(int v)                  { _bci = v; }
  int            mask_size() const               { return _mask_size; }
  void           set_mask_size(int v)            { _mask_size = v; }
  // Test bit mask size and return either the in-line bit mask or allocated
  // bit mask.
  uintptr_t*  bit_mask() const                   { return (uintptr_t*)(mask_size() <= small_mask_limit ? (intptr_t)_bit_mask : _bit_mask[0]); }

  // return the word size of_bit_mask.  mask_size() <= 4 * MAX_USHORT
  size_t mask_word_size() const {
    return (mask_size() + BitsPerWord - 1) / BitsPerWord;
  }

  uintptr_t entry_at(int offset) const           { int i = offset * bits_per_entry; return bit_mask()[i / BitsPerWord] >> (i % BitsPerWord); }

  void set_expression_stack_size(int sz)         { _expression_stack_size = sz; }

  // Lookup
  bool match(const methodHandle& method, int bci) const { return _method == method() && _bci == bci; }
  bool is_empty() const;

  // Initialization
  void initialize();

 public:
  InterpreterOopMap();
  ~InterpreterOopMap();

  // Copy the OopMapCacheEntry in parameter "from" into this
  // InterpreterOopMap.  If the _bit_mask[0] in "from" points to
  // allocated space (i.e., the bit mask was to large to hold
  // in-line), allocate the space from a Resource area.
  void resource_copy(OopMapCacheEntry* from);

  void iterate_oop(OffsetClosure* oop_closure) const;
  void print() const;

  int number_of_entries() const                  { return mask_size() / bits_per_entry; }
  bool is_dead(int offset) const                 { return (entry_at(offset) & (1 << dead_bit_number)) != 0; }
  bool is_oop (int offset) const                 { return (entry_at(offset) & (1 << oop_bit_number )) != 0; }

  int expression_stack_size() const              { return _expression_stack_size; }

};

class OopMapCache : public CHeapObj<mtClass> {
 static OopMapCacheEntry* volatile _old_entries;
 private:
  enum { _size        = 32,     // Use fixed size for now
         _probe_depth = 3       // probe depth in case of collisions
  };

  OopMapCacheEntry* volatile * _array;

  unsigned int hash_value_for(const methodHandle& method, int bci) const;
  OopMapCacheEntry* entry_at(int i) const;
  bool put_at(int i, OopMapCacheEntry* entry, OopMapCacheEntry* old);

  static void enqueue_for_cleanup(OopMapCacheEntry* entry);

  void flush();

 public:
  OopMapCache();
  ~OopMapCache();                                // free up memory

  // flush cache entry is occupied by an obsolete method
  void flush_obsolete_entries();

  // Returns the oopMap for (method, bci) in parameter "entry".
  // Returns false if an oop map was not found.
  void lookup(const methodHandle& method, int bci, InterpreterOopMap* entry);

  // Compute an oop map without updating the cache or grabbing any locks (for debugging)
  static void compute_one_oop_map(const methodHandle& method, int bci, InterpreterOopMap* entry);
  static void cleanup_old_entries();
};

#endif // SHARE_INTERPRETER_OOPMAPCACHE_HPP
