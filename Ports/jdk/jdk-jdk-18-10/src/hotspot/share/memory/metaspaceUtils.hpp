/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 SAP SE. All rights reserved.
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
#ifndef SHARE_MEMORY_METASPACEUTILS_HPP
#define SHARE_MEMORY_METASPACEUTILS_HPP

#include "memory/metaspace.hpp"
#include "memory/metaspaceChunkFreeListSummary.hpp"
#include "memory/metaspaceStats.hpp"

class outputStream;

// Metaspace are deallocated when their class loader are GC'ed.
// This class implements a policy for inducing GC's to recover
// Metaspaces.

class MetaspaceGCThresholdUpdater : public AllStatic {
 public:
  enum Type {
    ComputeNewSize,
    ExpandAndAllocate,
    Last
  };

  static const char* to_string(MetaspaceGCThresholdUpdater::Type updater) {
    switch (updater) {
      case ComputeNewSize:
        return "compute_new_size";
      case ExpandAndAllocate:
        return "expand_and_allocate";
      default:
        assert(false, "Got bad updater: %d", (int) updater);
        return NULL;
    };
  }
};

class MetaspaceGC : public AllStatic {

  // The current high-water-mark for inducing a GC.
  // When committed memory of all metaspaces reaches this value,
  // a GC is induced and the value is increased. Size is in bytes.
  static volatile size_t _capacity_until_GC;
  static uint _shrink_factor;

  static size_t shrink_factor() { return _shrink_factor; }
  void set_shrink_factor(uint v) { _shrink_factor = v; }

 public:

  static void initialize();
  static void post_initialize();

  static size_t capacity_until_GC();
  static bool inc_capacity_until_GC(size_t v,
                                    size_t* new_cap_until_GC = NULL,
                                    size_t* old_cap_until_GC = NULL,
                                    bool* can_retry = NULL);
  static size_t dec_capacity_until_GC(size_t v);

  // The amount to increase the high-water-mark (_capacity_until_GC)
  static size_t delta_capacity_until_GC(size_t bytes);

  // Tells if we have can expand metaspace without hitting set limits.
  static bool can_expand(size_t words, bool is_class);

  // Returns amount that we can expand without hitting a GC,
  // measured in words.
  static size_t allowed_expansion();

  // Calculate the new high-water mark at which to induce
  // a GC.
  static void compute_new_size();
};

class MetaspaceUtils : AllStatic {
public:

  // Committed space actually in use by Metadata
  static size_t used_words();
  static size_t used_words(Metaspace::MetadataType mdtype);

  // Space committed for Metaspace
  static size_t committed_words();
  static size_t committed_words(Metaspace::MetadataType mdtype);

  // Space reserved for Metaspace
  static size_t reserved_words();
  static size_t reserved_words(Metaspace::MetadataType mdtype);

  // _bytes() variants for convenience...
  static size_t used_bytes()                                    { return used_words() * BytesPerWord; }
  static size_t used_bytes(Metaspace::MetadataType mdtype)      { return used_words(mdtype) * BytesPerWord; }
  static size_t committed_bytes()                               { return committed_words() * BytesPerWord; }
  static size_t committed_bytes(Metaspace::MetadataType mdtype) { return committed_words(mdtype) * BytesPerWord; }
  static size_t reserved_bytes()                                { return reserved_words() * BytesPerWord; }
  static size_t reserved_bytes(Metaspace::MetadataType mdtype)  { return reserved_words(mdtype) * BytesPerWord; }

  // Retrieve all statistics in one go; make sure the values are consistent.
  static MetaspaceStats get_statistics(Metaspace::MetadataType mdtype);
  static MetaspaceCombinedStats get_combined_statistics();

  // (See JDK-8251342). Implement or Consolidate.
  static MetaspaceChunkFreeListSummary chunk_free_list_summary(Metaspace::MetadataType mdtype) {
    return MetaspaceChunkFreeListSummary(0,0,0,0,0,0,0,0);
  }

  // Log change in used metadata.
  static void print_metaspace_change(const MetaspaceCombinedStats& pre_meta_values);

  // This will print out a basic metaspace usage report but
  // unlike print_report() is guaranteed not to lock or to walk the CLDG.
  static void print_basic_report(outputStream* st, size_t scale = 0);

  // Prints a report about the current metaspace state.
  // Function will walk the CLDG and will lock the expand lock; if that is not
  // convenient, use print_basic_report() instead.
  static void print_report(outputStream* out, size_t scale = 0);

  static void print_on(outputStream * out);

  DEBUG_ONLY(static void verify();)

};

#endif // SHARE_MEMORY_METASPACEUTILS_HPP
