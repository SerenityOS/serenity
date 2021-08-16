/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "jfr/utilities/jfrBlob.hpp"

JfrBlob::JfrBlob(const u1* checkpoint, size_t size) :
  _data(JfrCHeapObj::new_array<u1>(size)),
  _next(),
  _size(size),
  _written(false) {
  assert(_data != NULL, "invariant");
  memcpy(const_cast<u1*>(_data), checkpoint, size);
}

JfrBlob::~JfrBlob() {
  JfrCHeapObj::free(const_cast<u1*>(_data), _size);
}

void JfrBlob::reset_write_state() const {
  if (!_written) {
    return;
  }
  _written = false;
  if (_next.valid()) {
    _next->reset_write_state();
  }
}

void JfrBlob::set_next(const JfrBlobHandle& ref) {
  if (_next == ref) {
    return;
  }
  assert(_next != ref, "invariant");
  if (_next.valid()) {
    _next->set_next(ref);
    return;
  }
  _next = ref;
}

JfrBlobHandle JfrBlob::make(const u1* data, size_t size) {
  const JfrBlob* const blob = new JfrBlob(data, size);
  assert(blob != NULL, "invariant");
  return JfrBlobReference::make(blob);
}
