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

#ifndef SHARE_OOPS_OOP_INLINE_HPP
#define SHARE_OOPS_OOP_INLINE_HPP

#include "oops/oop.hpp"

#include "memory/universe.hpp"
#include "oops/access.inline.hpp"
#include "oops/arrayKlass.hpp"
#include "oops/arrayOop.hpp"
#include "oops/compressedOops.inline.hpp"
#include "oops/markWord.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/macros.hpp"
#include "utilities/globalDefinitions.hpp"

// Implementation of all inlined member functions defined in oop.hpp
// We need a separate file to avoid circular references

markWord oopDesc::mark() const {
  uintptr_t v = HeapAccess<MO_RELAXED>::load_at(as_oop(), mark_offset_in_bytes());
  return markWord(v);
}

markWord oopDesc::mark_acquire() const {
  uintptr_t v = HeapAccess<MO_ACQUIRE>::load_at(as_oop(), mark_offset_in_bytes());
  return markWord(v);
}

markWord* oopDesc::mark_addr() const {
  return (markWord*) &_mark;
}

void oopDesc::set_mark(markWord m) {
  HeapAccess<MO_RELAXED>::store_at(as_oop(), mark_offset_in_bytes(), m.value());
}

void oopDesc::set_mark(HeapWord* mem, markWord m) {
  *(markWord*)(((char*)mem) + mark_offset_in_bytes()) = m;
}

void oopDesc::release_set_mark(markWord m) {
  HeapAccess<MO_RELEASE>::store_at(as_oop(), mark_offset_in_bytes(), m.value());
}

markWord oopDesc::cas_set_mark(markWord new_mark, markWord old_mark) {
  uintptr_t v = HeapAccess<>::atomic_cmpxchg_at(as_oop(), mark_offset_in_bytes(), old_mark.value(), new_mark.value());
  return markWord(v);
}

markWord oopDesc::cas_set_mark(markWord new_mark, markWord old_mark, atomic_memory_order order) {
  return Atomic::cmpxchg(&_mark, old_mark, new_mark, order);
}

void oopDesc::init_mark() {
  set_mark(markWord::prototype());
}

Klass* oopDesc::klass() const {
  if (UseCompressedClassPointers) {
    return CompressedKlassPointers::decode_not_null(_metadata._compressed_klass);
  } else {
    return _metadata._klass;
  }
}

Klass* oopDesc::klass_or_null() const {
  if (UseCompressedClassPointers) {
    return CompressedKlassPointers::decode(_metadata._compressed_klass);
  } else {
    return _metadata._klass;
  }
}

Klass* oopDesc::klass_or_null_acquire() const {
  if (UseCompressedClassPointers) {
    narrowKlass nklass = Atomic::load_acquire(&_metadata._compressed_klass);
    return CompressedKlassPointers::decode(nklass);
  } else {
    return Atomic::load_acquire(&_metadata._klass);
  }
}

void oopDesc::set_klass(Klass* k) {
  assert(Universe::is_bootstrapping() || (k != NULL && k->is_klass()), "incorrect Klass");
  if (UseCompressedClassPointers) {
    _metadata._compressed_klass = CompressedKlassPointers::encode_not_null(k);
  } else {
    _metadata._klass = k;
  }
}

void oopDesc::release_set_klass(HeapWord* mem, Klass* k) {
  assert(Universe::is_bootstrapping() || (k != NULL && k->is_klass()), "incorrect Klass");
  char* raw_mem = ((char*)mem + klass_offset_in_bytes());
  if (UseCompressedClassPointers) {
    Atomic::release_store((narrowKlass*)raw_mem,
                          CompressedKlassPointers::encode_not_null(k));
  } else {
    Atomic::release_store((Klass**)raw_mem, k);
  }
}

int oopDesc::klass_gap() const {
  return *(int*)(((intptr_t)this) + klass_gap_offset_in_bytes());
}

void oopDesc::set_klass_gap(HeapWord* mem, int v) {
  if (UseCompressedClassPointers) {
    *(int*)(((char*)mem) + klass_gap_offset_in_bytes()) = v;
  }
}

void oopDesc::set_klass_gap(int v) {
  set_klass_gap((HeapWord*)this, v);
}

bool oopDesc::is_a(Klass* k) const {
  return klass()->is_subtype_of(k);
}

int oopDesc::size()  {
  return size_given_klass(klass());
}

int oopDesc::size_given_klass(Klass* klass)  {
  int lh = klass->layout_helper();
  int s;

  // lh is now a value computed at class initialization that may hint
  // at the size.  For instances, this is positive and equal to the
  // size.  For arrays, this is negative and provides log2 of the
  // array element size.  For other oops, it is zero and thus requires
  // a virtual call.
  //
  // We go to all this trouble because the size computation is at the
  // heart of phase 2 of mark-compaction, and called for every object,
  // alive or dead.  So the speed here is equal in importance to the
  // speed of allocation.

  if (lh > Klass::_lh_neutral_value) {
    if (!Klass::layout_helper_needs_slow_path(lh)) {
      s = lh >> LogHeapWordSize;  // deliver size scaled by wordSize
    } else {
      s = klass->oop_size(this);
    }
  } else if (lh <= Klass::_lh_neutral_value) {
    // The most common case is instances; fall through if so.
    if (lh < Klass::_lh_neutral_value) {
      // Second most common case is arrays.  We have to fetch the
      // length of the array, shift (multiply) it appropriately,
      // up to wordSize, add the header, and align to object size.
      size_t size_in_bytes;
      size_t array_length = (size_t) ((arrayOop)this)->length();
      size_in_bytes = array_length << Klass::layout_helper_log2_element_size(lh);
      size_in_bytes += Klass::layout_helper_header_size(lh);

      // This code could be simplified, but by keeping array_header_in_bytes
      // in units of bytes and doing it this way we can round up just once,
      // skipping the intermediate round to HeapWordSize.
      s = (int)(align_up(size_in_bytes, MinObjAlignmentInBytes) / HeapWordSize);

      // UseParallelGC and UseG1GC can change the length field
      // of an "old copy" of an object array in the young gen so it indicates
      // the grey portion of an already copied array. This will cause the first
      // disjunct below to fail if the two comparands are computed across such
      // a concurrent change.
      assert((s == klass->oop_size(this)) ||
             (Universe::is_gc_active() && is_objArray() && is_forwarded() && (get_UseParallelGC() || get_UseG1GC())),
             "wrong array object size");
    } else {
      // Must be zero, so bite the bullet and take the virtual call.
      s = klass->oop_size(this);
    }
  }

  assert(s > 0, "Oop size must be greater than zero, not %d", s);
  assert(is_object_aligned(s), "Oop size is not properly aligned: %d", s);
  return s;
}

bool oopDesc::is_instance()  const { return klass()->is_instance_klass();  }
bool oopDesc::is_array()     const { return klass()->is_array_klass();     }
bool oopDesc::is_objArray()  const { return klass()->is_objArray_klass();  }
bool oopDesc::is_typeArray() const { return klass()->is_typeArray_klass(); }

void*    oopDesc::field_addr(int offset)     const { return reinterpret_cast<void*>(cast_from_oop<intptr_t>(as_oop()) + offset); }

template <class T>
T*       oopDesc::obj_field_addr(int offset) const { return (T*) field_addr(offset); }

template <typename T>
size_t   oopDesc::field_offset(T* p) const { return pointer_delta((void*)p, (void*)this, 1); }

template <DecoratorSet decorators>
inline oop  oopDesc::obj_field_access(int offset) const             { return HeapAccess<decorators>::oop_load_at(as_oop(), offset); }
inline oop  oopDesc::obj_field(int offset) const                    { return HeapAccess<>::oop_load_at(as_oop(), offset);  }

inline void oopDesc::obj_field_put(int offset, oop value)           { HeapAccess<>::oop_store_at(as_oop(), offset, value); }

inline jbyte oopDesc::byte_field(int offset) const                  { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void  oopDesc::byte_field_put(int offset, jbyte value)       { HeapAccess<>::store_at(as_oop(), offset, value); }

inline jchar oopDesc::char_field(int offset) const                  { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void  oopDesc::char_field_put(int offset, jchar value)       { HeapAccess<>::store_at(as_oop(), offset, value); }

inline jboolean oopDesc::bool_field(int offset) const               { return HeapAccess<>::load_at(as_oop(), offset); }
inline void     oopDesc::bool_field_put(int offset, jboolean value) { HeapAccess<>::store_at(as_oop(), offset, jboolean(value & 1)); }
inline jboolean oopDesc::bool_field_volatile(int offset) const      { return HeapAccess<MO_SEQ_CST>::load_at(as_oop(), offset); }
inline void     oopDesc::bool_field_put_volatile(int offset, jboolean value) { HeapAccess<MO_SEQ_CST>::store_at(as_oop(), offset, jboolean(value & 1)); }
inline jshort oopDesc::short_field(int offset) const                { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void   oopDesc::short_field_put(int offset, jshort value)    { HeapAccess<>::store_at(as_oop(), offset, value); }

inline jint oopDesc::int_field(int offset) const                    { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void oopDesc::int_field_put(int offset, jint value)          { HeapAccess<>::store_at(as_oop(), offset, value); }

inline jlong oopDesc::long_field(int offset) const                  { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void  oopDesc::long_field_put(int offset, jlong value)       { HeapAccess<>::store_at(as_oop(), offset, value); }

inline jfloat oopDesc::float_field(int offset) const                { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void   oopDesc::float_field_put(int offset, jfloat value)    { HeapAccess<>::store_at(as_oop(), offset, value); }

inline jdouble oopDesc::double_field(int offset) const              { return HeapAccess<>::load_at(as_oop(), offset);  }
inline void    oopDesc::double_field_put(int offset, jdouble value) { HeapAccess<>::store_at(as_oop(), offset, value); }

bool oopDesc::is_locked() const {
  return mark().is_locked();
}

bool oopDesc::is_unlocked() const {
  return mark().is_unlocked();
}

// Used only for markSweep, scavenging
bool oopDesc::is_gc_marked() const {
  return mark().is_marked();
}

// Used by scavengers
bool oopDesc::is_forwarded() const {
  // The extra heap check is needed since the obj might be locked, in which case the
  // mark would point to a stack location and have the sentinel bit cleared
  return mark().is_marked();
}

// Used by scavengers
void oopDesc::forward_to(oop p) {
  verify_forwardee(p);
  markWord m = markWord::encode_pointer_as_mark(p);
  assert(m.decode_pointer() == p, "encoding must be reversable");
  set_mark(m);
}

// Used by parallel scavengers
bool oopDesc::cas_forward_to(oop p, markWord compare, atomic_memory_order order) {
  verify_forwardee(p);
  markWord m = markWord::encode_pointer_as_mark(p);
  assert(m.decode_pointer() == p, "encoding must be reversable");
  return cas_set_mark(m, compare, order) == compare;
}

oop oopDesc::forward_to_atomic(oop p, markWord compare, atomic_memory_order order) {
  verify_forwardee(p);
  markWord m = markWord::encode_pointer_as_mark(p);
  assert(m.decode_pointer() == p, "encoding must be reversable");
  markWord old_mark = cas_set_mark(m, compare, order);
  if (old_mark == compare) {
    return NULL;
  } else {
    return cast_to_oop(old_mark.decode_pointer());
  }
}

// Note that the forwardee is not the same thing as the displaced_mark.
// The forwardee is used when copying during scavenge and mark-sweep.
// It does need to clear the low two locking- and GC-related bits.
oop oopDesc::forwardee() const {
  return cast_to_oop(mark().decode_pointer());
}

// The following method needs to be MT safe.
uint oopDesc::age() const {
  assert(!is_forwarded(), "Attempt to read age from forwarded mark");
  if (has_displaced_mark()) {
    return displaced_mark().age();
  } else {
    return mark().age();
  }
}

void oopDesc::incr_age() {
  assert(!is_forwarded(), "Attempt to increment age of forwarded mark");
  if (has_displaced_mark()) {
    set_displaced_mark(displaced_mark().incr_age());
  } else {
    set_mark(mark().incr_age());
  }
}

template <typename OopClosureType>
void oopDesc::oop_iterate(OopClosureType* cl) {
  OopIteratorClosureDispatch::oop_oop_iterate(cl, this, klass());
}

template <typename OopClosureType>
void oopDesc::oop_iterate(OopClosureType* cl, MemRegion mr) {
  OopIteratorClosureDispatch::oop_oop_iterate(cl, this, klass(), mr);
}

template <typename OopClosureType>
int oopDesc::oop_iterate_size(OopClosureType* cl) {
  Klass* k = klass();
  int size = size_given_klass(k);
  OopIteratorClosureDispatch::oop_oop_iterate(cl, this, k);
  return size;
}

template <typename OopClosureType>
int oopDesc::oop_iterate_size(OopClosureType* cl, MemRegion mr) {
  Klass* k = klass();
  int size = size_given_klass(k);
  OopIteratorClosureDispatch::oop_oop_iterate(cl, this, k, mr);
  return size;
}

template <typename OopClosureType>
void oopDesc::oop_iterate_backwards(OopClosureType* cl) {
  oop_iterate_backwards(cl, klass());
}

template <typename OopClosureType>
void oopDesc::oop_iterate_backwards(OopClosureType* cl, Klass* k) {
  assert(k == klass(), "wrong klass");
  OopIteratorClosureDispatch::oop_oop_iterate_backwards(cl, this, k);
}

bool oopDesc::is_instanceof_or_null(oop obj, Klass* klass) {
  return obj == NULL || obj->klass()->is_subtype_of(klass);
}

intptr_t oopDesc::identity_hash() {
  // Fast case; if the object is unlocked and the hash value is set, no locking is needed
  // Note: The mark must be read into local variable to avoid concurrent updates.
  markWord mrk = mark();
  if (mrk.is_unlocked() && !mrk.has_no_hash()) {
    return mrk.hash();
  } else if (mrk.is_marked()) {
    return mrk.hash();
  } else {
    return slow_identity_hash();
  }
}

bool oopDesc::has_displaced_mark() const {
  return mark().has_displaced_mark_helper();
}

markWord oopDesc::displaced_mark() const {
  return mark().displaced_mark_helper();
}

void oopDesc::set_displaced_mark(markWord m) {
  mark().set_displaced_mark_helper(m);
}

bool oopDesc::mark_must_be_preserved() const {
  return mark_must_be_preserved(mark());
}

bool oopDesc::mark_must_be_preserved(markWord m) const {
  return m.must_be_preserved(this);
}

bool oopDesc::mark_must_be_preserved_for_promotion_failure(markWord m) const {
  return m.must_be_preserved_for_promotion_failure(this);
}

#endif // SHARE_OOPS_OOP_INLINE_HPP
