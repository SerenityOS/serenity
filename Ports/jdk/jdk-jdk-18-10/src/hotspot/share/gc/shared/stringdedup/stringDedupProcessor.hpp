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

#ifndef SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPPROCESSOR_HPP
#define SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPPROCESSOR_HPP

#include "gc/shared/concurrentGCThread.hpp"
#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shared/stringdedup/stringDedupStat.hpp"
#include "utilities/macros.hpp"

class OopStorage;
class SuspendibleThreadSetJoiner;

// Thread class for string deduplication.  There is only one instance of
// this class.  This thread processes deduplication requests.  It also
// manages the deduplication table, performing resize and cleanup operations
// as needed.  This includes managing the OopStorage objects used to hold
// requests.
//
// This thread uses the SuspendibleThreadSet mechanism to take part in the
// safepoint protocol.  It checks for safepoints between processing requests
// in order to minimize safepoint latency.  The Table provides incremental
// operations for resizing and for removing dead entries, so this thread can
// perform safepoint checks between steps in those operations.
class StringDedup::Processor : public ConcurrentGCThread {
  Processor();
  ~Processor() = default;

  NONCOPYABLE(Processor);

  static OopStorage* _storages[2];
  static StorageUse* volatile _storage_for_requests;
  static StorageUse* _storage_for_processing;

  // Returns !should_terminate();
  bool wait_for_requests() const;

  // Yield if requested.  Returns !should_terminate() after possible yield.
  bool yield_or_continue(SuspendibleThreadSetJoiner* joiner, Stat::Phase phase) const;

  class ProcessRequest;
  void process_requests(SuspendibleThreadSetJoiner* joiner) const;
  void cleanup_table(SuspendibleThreadSetJoiner* joiner, bool grow_only, bool force) const;

  void log_statistics();

protected:
  virtual void run_service();
  virtual void stop_service();

public:
  static void initialize();

  static void initialize_storage();
  static StorageUse* storage_for_requests();
};

#endif // SHARE_GC_SHARED_STRINGDEDUP_STRINGDEDUPPROCESSOR_HPP
