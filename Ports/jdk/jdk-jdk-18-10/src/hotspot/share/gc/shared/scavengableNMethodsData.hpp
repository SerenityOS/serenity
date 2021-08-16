/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_SCAVENGABLENMETHODDATAS_HPP
#define SHARE_GC_SHARED_SCAVENGABLENMETHODDATAS_HPP

#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"

class nmethod;

class ScavengableNMethodsData : public CHeapObj<mtGC> {
  // State bits put into the two lower alignment bits.
  static const uintptr_t state_bits = 2;
  static const uintptr_t state_mask = (1 << state_bits) - 1;
  static const uintptr_t state_on_list = 0x1;
  static const uintptr_t state_marked  = 0x2;

  // nmethod containing the GC data.
  nmethod* const _nm;

  // The data is stored as a bit pattern in a void* inside the nmethod.
  uintptr_t data() const                    { return reinterpret_cast<uintptr_t>(_nm->gc_data<void>()); }
  void set_data(uintptr_t data) const       { _nm->set_gc_data(reinterpret_cast<void*>(data)); }

  jbyte state() const                       { return data() & state_mask; }
  void set_state(jbyte state) const         { set_data((data() & ~state_mask) | state); }

  uintptr_t from_nmethod(nmethod* nm) const { return reinterpret_cast<uintptr_t>(nm); }
  nmethod* to_nmethod(uintptr_t data) const { return reinterpret_cast<nmethod*>(data); }

public:
  ScavengableNMethodsData(nmethod* nm) : _nm(nm) {
    assert(is_aligned(nm, 4), "Must be aligned to fit state bits");
  }

  // Scavengable oop support
  bool  on_list() const { return (state() & state_on_list) != 0; }
  void  set_on_list()   { set_state(state_on_list); }
  void  clear_on_list() { set_state(0); }

#ifndef PRODUCT
  void  set_marked()   { set_state(state() | state_marked); }
  void  clear_marked() { set_state(state() & ~state_marked); }
  bool  not_marked()   { return (state() & ~state_on_list) == 0; }
  // N.B. there is no positive marked query, and we only use the not_marked query for asserts.
#endif //PRODUCT

  nmethod* next() const     { return to_nmethod(data() & ~state_mask); }
  void set_next(nmethod *n) { set_data(from_nmethod(n) | state()); }
};

#endif // SHARE_GC_SHARED_SCAVENGABLENMETHODDATAS_HPP
