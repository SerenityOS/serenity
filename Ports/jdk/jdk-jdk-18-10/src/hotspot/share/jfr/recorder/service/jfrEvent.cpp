/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/recorder/service/jfrEvent.hpp"
#include "utilities/bitMap.inline.hpp"
#include "utilities/macros.hpp"

#ifdef ASSERT
JfrEventVerifier::JfrEventVerifier() : _committed(false) {
  memset(_verification_storage, 0, (sizeof(_verification_storage)));
  _verification_bit_map = BitMapView(_verification_storage, (BitMap::idx_t)(sizeof(_verification_storage) * BitsPerByte));
}

void JfrEventVerifier::check(BitMap::idx_t field_idx) const {
  assert(field_idx < _verification_bit_map.size(), "too many fields to verify, please resize _verification_storage");
}

void JfrEventVerifier::set_field_bit(size_t field_idx) {
  check((BitMap::idx_t)field_idx);
  _verification_bit_map.set_bit((BitMap::idx_t)field_idx);
}

bool JfrEventVerifier::verify_field_bit(size_t field_idx) const {
  check((BitMap::idx_t)field_idx);
  return _verification_bit_map.at((BitMap::idx_t)field_idx);
}

void JfrEventVerifier::set_committed() {
  assert(!_committed, "invariant");
  _committed = true;
}

void JfrEventVerifier::clear_committed() {
  _committed = false;
}

bool JfrEventVerifier::committed() const {
  return _committed;
}

#endif // ASSERT
