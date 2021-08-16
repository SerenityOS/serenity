/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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
#include "classfile/classLoaderData.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "memory/metaspace.hpp"
#include "memory/metaspace/chunkHeaderPool.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/internalStats.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceReporter.hpp"
#include "memory/metaspace/metaspaceSettings.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "memory/metaspace/printCLDMetaspaceInfoClosure.hpp"
#include "memory/metaspace/runningCounters.hpp"
#include "memory/metaspace/virtualSpaceList.hpp"
#include "memory/metaspaceUtils.hpp"
#include "runtime/os.hpp"

namespace metaspace {

static const char* describe_spacetype(Metaspace::MetaspaceType st) {
  const char* s = NULL;
  switch (st) {
    case Metaspace::StandardMetaspaceType: s = "Standard"; break;
    case Metaspace::BootMetaspaceType: s = "Boot"; break;
    case Metaspace::ClassMirrorHolderMetaspaceType: s = "ClassMirrorHolder"; break;
    case Metaspace::ReflectionMetaspaceType: s = "Reflection"; break;
    default: ShouldNotReachHere();
  }
  return s;
}

static void print_vs(outputStream* out, size_t scale) {
  const size_t reserved_nc = RunningCounters::reserved_words_nonclass();
  const size_t committed_nc = RunningCounters::committed_words_nonclass();
  const int num_nodes_nc = VirtualSpaceList::vslist_nonclass()->num_nodes();

  if (Metaspace::using_class_space()) {
    const size_t reserved_c = RunningCounters::reserved_words_class();
    const size_t committed_c = RunningCounters::committed_words_class();
    const int num_nodes_c = VirtualSpaceList::vslist_class()->num_nodes();

    out->print("  Non-class space:  ");
    print_scaled_words(out, reserved_nc, scale, 7);
    out->print(" reserved, ");
    print_scaled_words_and_percentage(out, committed_nc, reserved_nc, scale, 7);
    out->print(" committed, ");
    out->print(" %d nodes.", num_nodes_nc);
    out->cr();
    out->print("      Class space:  ");
    print_scaled_words(out, reserved_c, scale, 7);
    out->print(" reserved, ");
    print_scaled_words_and_percentage(out, committed_c, reserved_c, scale, 7);
    out->print(" committed, ");
    out->print(" %d nodes.", num_nodes_c);
    out->cr();
    out->print("             Both:  ");
    print_scaled_words(out, reserved_c + reserved_nc, scale, 7);
    out->print(" reserved, ");
    print_scaled_words_and_percentage(out, committed_c + committed_nc, reserved_c + reserved_nc, scale, 7);
    out->print(" committed. ");
    out->cr();
  } else {
    print_scaled_words(out, reserved_nc, scale, 7);
    out->print(" reserved, ");
    print_scaled_words_and_percentage(out, committed_nc, reserved_nc, scale, 7);
    out->print(" committed, ");
    out->print(" %d nodes.", num_nodes_nc);
    out->cr();
  }
}

static void print_settings(outputStream* out, size_t scale) {
  out->print("MaxMetaspaceSize: ");
  if (MaxMetaspaceSize == max_uintx) {
    out->print("unlimited");
  } else {
    print_human_readable_size(out, MaxMetaspaceSize, scale);
  }
  out->cr();
  if (Metaspace::using_class_space()) {
    out->print("CompressedClassSpaceSize: ");
    print_human_readable_size(out, CompressedClassSpaceSize, scale);
  } else {
    out->print("No class space");
  }
  out->cr();
  out->print("Initial GC threshold: ");
  print_human_readable_size(out, MetaspaceSize, scale);
  out->cr();
  out->print("Current GC threshold: ");
  print_human_readable_size(out, MetaspaceGC::capacity_until_GC(), scale);
  out->cr();
  out->print_cr("CDS: %s", (UseSharedSpaces ? "on" : (DumpSharedSpaces ? "dump" : "off")));
  out->print_cr("MetaspaceReclaimPolicy: %s", MetaspaceReclaimPolicy);
  Settings::print_on(out);
}

// This will print out a basic metaspace usage report but
// unlike print_report() is guaranteed not to lock or to walk the CLDG.
void MetaspaceReporter::print_basic_report(outputStream* out, size_t scale) {
  if (!Metaspace::initialized()) {
    out->print_cr("Metaspace not yet initialized.");
    return;
  }
  out->cr();
  out->print_cr("Usage:");
  if (Metaspace::using_class_space()) {
    out->print("  Non-class:  ");
  }

  // Note: since we want to purely rely on counters, without any locking or walking the CLDG,
  // for Usage stats (statistics over in-use chunks) all we can print is the
  // used words. We cannot print committed areas, or free/waste areas, of in-use chunks require
  // walking.
  const size_t used_nc = MetaspaceUtils::used_words(Metaspace::NonClassType);

  print_scaled_words(out, used_nc, scale, 5);
  out->print(" used.");
  out->cr();
  if (Metaspace::using_class_space()) {
    const size_t used_c = MetaspaceUtils::used_words(Metaspace::ClassType);
    out->print("      Class:  ");
    print_scaled_words(out, used_c, scale, 5);
    out->print(" used.");
    out->cr();
    out->print("       Both:  ");
    const size_t used = used_nc + used_c;
    print_scaled_words(out, used, scale, 5);
    out->print(" used.");
    out->cr();
  }
  out->cr();
  out->print_cr("Virtual space:");
  print_vs(out, scale);
  out->cr();
  out->print_cr("Chunk freelists:");
  if (Metaspace::using_class_space()) {
    out->print("   Non-Class:  ");
  }
  print_scaled_words(out, ChunkManager::chunkmanager_nonclass()->total_word_size(), scale);
  out->cr();
  if (Metaspace::using_class_space()) {
    out->print("       Class:  ");
    print_scaled_words(out, ChunkManager::chunkmanager_class()->total_word_size(), scale);
    out->cr();
    out->print("        Both:  ");
    print_scaled_words(out, ChunkManager::chunkmanager_nonclass()->total_word_size() +
                            ChunkManager::chunkmanager_class()->total_word_size(), scale);
    out->cr();
  }
  out->cr();

  // Print basic settings
  print_settings(out, scale);
  out->cr();
  out->cr();
  out->print_cr("Internal statistics:");
  out->cr();
  InternalStats::print_on(out);
  out->cr();
}

void MetaspaceReporter::print_report(outputStream* out, size_t scale, int flags) {
  if (!Metaspace::initialized()) {
    out->print_cr("Metaspace not yet initialized.");
    return;
  }
  const bool print_loaders = (flags & (int)Option::ShowLoaders) > 0;
  const bool print_classes = (flags & (int)Option::ShowClasses) > 0;
  const bool print_by_chunktype = (flags & (int)Option::BreakDownByChunkType) > 0;
  const bool print_by_spacetype = (flags & (int)Option::BreakDownBySpaceType) > 0;

  // Some report options require walking the class loader data graph.
  metaspace::PrintCLDMetaspaceInfoClosure cl(out, scale, print_loaders, print_classes, print_by_chunktype);
  if (print_loaders) {
    out->cr();
    out->print_cr("Usage per loader:");
    out->cr();
  }

  ClassLoaderDataGraph::loaded_cld_do(&cl); // collect data and optionally print

  // Print totals, broken up by space type.
  if (print_by_spacetype) {
    out->cr();
    out->print_cr("Usage per space type:");
    out->cr();
    for (int space_type = (int)Metaspace::ZeroMetaspaceType;
         space_type < (int)Metaspace::MetaspaceTypeCount; space_type++)
    {
      uintx num_loaders = cl._num_loaders_by_spacetype[space_type];
      uintx num_classes = cl._num_classes_by_spacetype[space_type];
      out->print("%s - " UINTX_FORMAT " %s",
        describe_spacetype((Metaspace::MetaspaceType)space_type),
        num_loaders, loaders_plural(num_loaders));
      if (num_classes > 0) {
        out->print(", ");

        print_number_of_classes(out, num_classes, cl._num_classes_shared_by_spacetype[space_type]);
        out->print(":");
        cl._stats_by_spacetype[space_type].print_on(out, scale, print_by_chunktype);
      } else {
        out->print(".");
        out->cr();
      }
      out->cr();
    }
  }

  // Print totals for in-use data:
  out->cr();
  {
    uintx num_loaders = cl._num_loaders;
    out->print("Total Usage - " UINTX_FORMAT " %s, ",
      num_loaders, loaders_plural(num_loaders));
    print_number_of_classes(out, cl._num_classes, cl._num_classes_shared);
    out->print(":");
    cl._stats_total.print_on(out, scale, print_by_chunktype);
    out->cr();
  }

  /////////////////////////////////////////////////
  // -- Print Virtual space.
  out->cr();
  out->print_cr("Virtual space:");

  print_vs(out, scale);

  // -- Print VirtualSpaceList details.
  if ((flags & (int)Option::ShowVSList) > 0) {
    out->cr();
    out->print_cr("Virtual space list%s:", Metaspace::using_class_space() ? "s" : "");

    if (Metaspace::using_class_space()) {
      out->print_cr("   Non-Class:");
    }
    VirtualSpaceList::vslist_nonclass()->print_on(out);
    out->cr();
    if (Metaspace::using_class_space()) {
      out->print_cr("       Class:");
      VirtualSpaceList::vslist_class()->print_on(out);
      out->cr();
    }
  }
  out->cr();

  //////////// Freelists (ChunkManager) section ///////////////////////////

  out->cr();
  out->print_cr("Chunk freelist%s:", Metaspace::using_class_space() ? "s" : "");

  ChunkManagerStats non_class_cm_stat;
  ChunkManagerStats class_cm_stat;
  ChunkManagerStats total_cm_stat;

  ChunkManager::chunkmanager_nonclass()->add_to_statistics(&non_class_cm_stat);
  if (Metaspace::using_class_space()) {
    ChunkManager::chunkmanager_nonclass()->add_to_statistics(&non_class_cm_stat);
    ChunkManager::chunkmanager_class()->add_to_statistics(&class_cm_stat);
    total_cm_stat.add(non_class_cm_stat);
    total_cm_stat.add(class_cm_stat);

    out->print_cr("   Non-Class:");
    non_class_cm_stat.print_on(out, scale);
    out->cr();
    out->print_cr("       Class:");
    class_cm_stat.print_on(out, scale);
    out->cr();
    out->print_cr("        Both:");
    total_cm_stat.print_on(out, scale);
    out->cr();
  } else {
    ChunkManager::chunkmanager_nonclass()->add_to_statistics(&non_class_cm_stat);
    non_class_cm_stat.print_on(out, scale);
    out->cr();
  }

  //////////// Waste section ///////////////////////////
  // As a convenience, print a summary of common waste.
  out->cr();
  out->print("Waste (unused committed space):");
  // For all wastages, print percentages from total. As total use the total size of memory committed for metaspace.
  const size_t committed_words = RunningCounters::committed_words();

  out->print("(percentages refer to total committed size ");
  print_scaled_words(out, committed_words, scale);
  out->print_cr("):");

  // Print waste for in-use chunks.
  InUseChunkStats ucs_nonclass = cl._stats_total._arena_stats_nonclass.totals();
  InUseChunkStats ucs_class = cl._stats_total._arena_stats_class.totals();
  const size_t waste_in_chunks_in_use = ucs_nonclass._waste_words + ucs_class._waste_words;
  const size_t free_in_chunks_in_use = ucs_nonclass._free_words + ucs_class._free_words;

  out->print("        Waste in chunks in use: ");
  print_scaled_words_and_percentage(out, waste_in_chunks_in_use, committed_words, scale, 6);
  out->cr();
  out->print("        Free in chunks in use: ");
  print_scaled_words_and_percentage(out, free_in_chunks_in_use, committed_words, scale, 6);
  out->cr();

  // Print waste in free chunks.
  const size_t committed_in_free_chunks = total_cm_stat.total_committed_word_size();
  out->print("                In free chunks: ");
  print_scaled_words_and_percentage(out, committed_in_free_chunks, committed_words, scale, 6);
  out->cr();

  // Print waste in deallocated blocks.
  const uintx free_blocks_num =
      cl._stats_total._arena_stats_nonclass._free_blocks_num +
      cl._stats_total._arena_stats_class._free_blocks_num;
  const size_t free_blocks_cap_words =
      cl._stats_total._arena_stats_nonclass._free_blocks_word_size +
      cl._stats_total._arena_stats_class._free_blocks_word_size;
  out->print("Deallocated from chunks in use: ");
  print_scaled_words_and_percentage(out, free_blocks_cap_words, committed_words, scale, 6);
  out->print(" (" UINTX_FORMAT " blocks)", free_blocks_num);
  out->cr();

  // Print total waste.
  const size_t total_waste =
      waste_in_chunks_in_use +
      free_in_chunks_in_use +
      committed_in_free_chunks +
      free_blocks_cap_words;
  out->print("                       -total-: ");
  print_scaled_words_and_percentage(out, total_waste, committed_words, scale, 6);
  out->cr();

  // Also print chunk header pool size.
  out->cr();
  out->print("chunk header pool: %u items, ", ChunkHeaderPool::pool()->used());
  print_scaled_words(out, ChunkHeaderPool::pool()->memory_footprint_words(), scale);
  out->print(".");
  out->cr();

  // Print internal statistics
  out->cr();
  out->print_cr("Internal statistics:");
  out->cr();
  InternalStats::print_on(out);
  out->cr();

  // Print some interesting settings
  out->cr();
  out->print_cr("Settings:");
  print_settings(out, scale);

  out->cr();
  out->cr();

  DEBUG_ONLY(MetaspaceUtils::verify();)
} // MetaspaceUtils::print_report()

} // namespace metaspace

