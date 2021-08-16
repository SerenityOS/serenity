/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020 SAP SE. All rights reserved.
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
#include "memory/metaspace.hpp"
#include "memory/metaspace/commitLimiter.hpp"
#include "memory/metaspaceUtils.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

// Returns the size, in words, by which we may expand the metaspace committed area without:
// - _cap == 0: hitting GC threshold or the MaxMetaspaceSize
// - _cap > 0: hitting cap (this is just for testing purposes)
size_t CommitLimiter::possible_expansion_words() const {
  if (_cap > 0) { // Testing.
    assert(_cnt.get() <= _cap, "Beyond limit?");
    return _cap - _cnt.get();
  }
  assert(_cnt.get() * BytesPerWord <= MaxMetaspaceSize, "Beyond limit?");
  const size_t words_left_below_max = MaxMetaspaceSize / BytesPerWord - _cnt.get();
  const size_t words_left_below_gc_threshold = MetaspaceGC::allowed_expansion();
  return MIN2(words_left_below_max, words_left_below_gc_threshold);
}

static CommitLimiter g_global_limiter(0);

// Returns the global metaspace commit counter
CommitLimiter* CommitLimiter::globalLimiter() {
  return &g_global_limiter;
}

} // namespace metaspace

