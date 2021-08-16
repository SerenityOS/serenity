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

#include "precompiled.hpp"
#include "gc/shared/stringdedup/stringDedupStorageUse.hpp"
#include "runtime/atomic.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalCounter.inline.hpp"
#include "utilities/globalDefinitions.hpp"

StringDedup::StorageUse::StorageUse(OopStorage* storage) :
  _storage(storage), _use_count(0)
{}

bool StringDedup::StorageUse::is_used_acquire() const {
  return Atomic::load_acquire(&_use_count) > 0;
}

StringDedup::StorageUse*
StringDedup::StorageUse::obtain(StorageUse* volatile* ptr) {
  GlobalCounter::CriticalSection cs(Thread::current());
  StorageUse* storage = Atomic::load(ptr);
  Atomic::inc(&storage->_use_count);
  return storage;
}

void StringDedup::StorageUse::relinquish() {
  size_t result = Atomic::sub(&_use_count, size_t(1));
  assert(result != SIZE_MAX, "use count underflow");
}
