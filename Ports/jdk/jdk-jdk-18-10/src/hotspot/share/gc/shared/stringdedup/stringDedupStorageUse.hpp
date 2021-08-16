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

#ifndef SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPSTORAGEUSE_HPP
#define SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPSTORAGEUSE_HPP

#include "gc/shared/stringdedup/stringDedup.hpp"
#include "memory/allocation.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class OopStorage;

// Manage access to one of the OopStorage objects used for requests.
class StringDedup::StorageUse : public CHeapObj<mtStringDedup> {
  OopStorage* const _storage;
  volatile size_t _use_count;

  NONCOPYABLE(StorageUse);

public:
  explicit StorageUse(OopStorage* storage);

  OopStorage* storage() const { return _storage; }

  // Return true if the storage is currently in use for registering requests.
  bool is_used_acquire() const;

  // Get the current requests object, and increment its in-use count.
  static StorageUse* obtain(StorageUse* volatile* ptr);

  // Discard a prior "obtain" request, decrementing the in-use count, and
  // permitting the deduplication thread to start processing if needed.
  void relinquish();
};

#endif // SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPSTORAGEUSE_HPP
