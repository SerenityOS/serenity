/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OOPS_CPCACHE_INLINE_HPP
#define SHARE_OOPS_CPCACHE_INLINE_HPP

#include "oops/cpCache.hpp"

#include "oops/oopHandle.inline.hpp"
#include "runtime/atomic.hpp"

inline int ConstantPoolCacheEntry::indices_ord() const { return Atomic::load_acquire(&_indices); }

inline Bytecodes::Code ConstantPoolCacheEntry::bytecode_1() const {
  return Bytecodes::cast((indices_ord() >> bytecode_1_shift) & bytecode_1_mask);
}

inline Bytecodes::Code ConstantPoolCacheEntry::bytecode_2() const {
  return Bytecodes::cast((indices_ord() >> bytecode_2_shift) & bytecode_2_mask);
}

// Has this bytecode been resolved? Only valid for invokes and get/put field/static.
inline bool ConstantPoolCacheEntry::is_resolved(Bytecodes::Code code) const {
  switch (bytecode_number(code)) {
    case 1:  return (bytecode_1() == code);
    case 2:  return (bytecode_2() == code);
  }
  return false;      // default: not resolved
}

inline Method* ConstantPoolCacheEntry::f2_as_interface_method() const {
  assert(bytecode_1() == Bytecodes::_invokeinterface, "");
  return (Method*)_f2;
}

inline Metadata* ConstantPoolCacheEntry::f1_ord() const { return (Metadata *)Atomic::load_acquire(&_f1); }

inline Method* ConstantPoolCacheEntry::f1_as_method() const {
  Metadata* f1 = f1_ord(); assert(f1 == NULL || f1->is_method(), "");
  return (Method*)f1;
}

inline Klass* ConstantPoolCacheEntry::f1_as_klass() const {
  Metadata* f1 = f1_ord(); assert(f1 == NULL || f1->is_klass(), "");
  return (Klass*)f1;
}

inline bool ConstantPoolCacheEntry::is_f1_null() const { Metadata* f1 = f1_ord(); return f1 == NULL; }

inline bool ConstantPoolCacheEntry::has_appendix() const {
  return (!is_f1_null()) && (_flags & (1 << has_appendix_shift)) != 0;
}

inline bool ConstantPoolCacheEntry::has_local_signature() const {
  return (!is_f1_null()) && (_flags & (1 << has_local_signature_shift)) != 0;
}

inline intx ConstantPoolCacheEntry::flags_ord() const   { return (intx)Atomic::load_acquire(&_flags); }

inline bool ConstantPoolCacheEntry::indy_resolution_failed() const {
  intx flags = flags_ord();
  return (flags & (1 << indy_resolution_failed_shift)) != 0;
}

// Constructor
inline ConstantPoolCache::ConstantPoolCache(int length,
                                            const intStack& inverse_index_map,
                                            const intStack& invokedynamic_inverse_index_map,
                                            const intStack& invokedynamic_references_map) :
                                                  _length(length),
                                                  _constant_pool(NULL) {
  CDS_JAVA_HEAP_ONLY(_archived_references_index = -1;)
  initialize(inverse_index_map, invokedynamic_inverse_index_map,
             invokedynamic_references_map);
  for (int i = 0; i < length; i++) {
    assert(entry_at(i)->is_f1_null(), "Failed to clear?");
  }
}

inline oop ConstantPoolCache::resolved_references() { return _resolved_references.resolve(); }

#endif // SHARE_OOPS_CPCACHE_INLINE_HPP
