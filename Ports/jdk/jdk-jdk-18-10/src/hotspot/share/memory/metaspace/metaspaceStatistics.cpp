/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/metaspaceStatistics.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

namespace metaspace {

// Returns total word size of all chunks in this manager.
void ChunkManagerStats::add(const ChunkManagerStats& other) {
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    _num_chunks[l] += other._num_chunks[l];
    _committed_word_size[l] += other._committed_word_size[l];
  }
}

// Returns total word size of all chunks in this manager.
size_t ChunkManagerStats::total_word_size() const {
  size_t s = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    s += _num_chunks[l] * chunklevel::word_size_for_level(l);
  }
  return s;
}

// Returns total committed word size of all chunks in this manager.
size_t ChunkManagerStats::total_committed_word_size() const {
  size_t s = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    s += _committed_word_size[l];
  }
  return s;
}

void ChunkManagerStats::print_on(outputStream* st, size_t scale) const {
  // Note: used as part of MetaspaceReport so formatting matters.
  size_t total_size = 0;
  size_t total_committed_size = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    st->cr();
    chunklevel::print_chunk_size(st, l);
    st->print(": ");
    if (_num_chunks[l] > 0) {
      const size_t word_size = _num_chunks[l] * chunklevel::word_size_for_level(l);

      st->print("%4d, capacity=", _num_chunks[l]);
      print_scaled_words(st, word_size, scale);

      st->print(", committed=");
      print_scaled_words_and_percentage(st, _committed_word_size[l], word_size, scale);

      total_size += word_size;
      total_committed_size += _committed_word_size[l];
    } else {
      st->print("(none)");
    }
  }
  st->cr();
  st->print("Total word size: ");
  print_scaled_words(st, total_size, scale);
  st->print(", committed: ");
  print_scaled_words_and_percentage(st, total_committed_size, total_size, scale);
  st->cr();
}

#ifdef ASSERT
void ChunkManagerStats::verify() const {
  assert(total_committed_word_size() <= total_word_size(),
         "Sanity");
}
#endif

void InUseChunkStats::print_on(outputStream* st, size_t scale) const {
  int col = st->position();
  st->print("%4d chunk%s, ", _num, _num != 1 ? "s" : "");
  if (_num > 0) {
    col += 14; st->fill_to(col);

    print_scaled_words(st, _word_size, scale, 5);
    st->print(" capacity,");

    col += 20; st->fill_to(col);
    print_scaled_words_and_percentage(st, _committed_words, _word_size, scale, 5);
    st->print(" committed, ");

    col += 18; st->fill_to(col);
    print_scaled_words_and_percentage(st, _used_words, _word_size, scale, 5);
    st->print(" used, ");

    col += 20; st->fill_to(col);
    print_scaled_words_and_percentage(st, _free_words, _word_size, scale, 5);
    st->print(" free, ");

    col += 20; st->fill_to(col);
    print_scaled_words_and_percentage(st, _waste_words, _word_size, scale, 5);
    st->print(" waste ");

  }
}

#ifdef ASSERT
void InUseChunkStats::verify() const {
  assert(_word_size >= _committed_words &&
      _committed_words == _used_words + _free_words + _waste_words,
         "Sanity: cap " SIZE_FORMAT ", committed " SIZE_FORMAT ", used " SIZE_FORMAT ", free " SIZE_FORMAT ", waste " SIZE_FORMAT ".",
         _word_size, _committed_words, _used_words, _free_words, _waste_words);
}
#endif

void ArenaStats::add(const ArenaStats& other) {
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    _stats[l].add(other._stats[l]);
  }
  _free_blocks_num += other._free_blocks_num;
  _free_blocks_word_size += other._free_blocks_word_size;
}

// Returns total chunk statistics over all chunk types.
InUseChunkStats ArenaStats::totals() const {
  InUseChunkStats out;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    out.add(_stats[l]);
  }
  return out;
}

void ArenaStats::print_on(outputStream* st, size_t scale,  bool detailed) const {
  streamIndentor sti(st);
  if (detailed) {
    st->cr_indent();
    st->print("Usage by chunk level:");
    {
      streamIndentor sti2(st);
      for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
        st->cr_indent();
        chunklevel::print_chunk_size(st, l);
        st->print(" chunks: ");
        if (_stats[l]._num == 0) {
          st->print(" (none)");
        } else {
          _stats[l].print_on(st, scale);
        }
      }

      st->cr_indent();
      st->print("%15s: ", "-total-");
      totals().print_on(st, scale);
    }
    if (_free_blocks_num > 0) {
      st->cr_indent();
      st->print("deallocated: " UINTX_FORMAT " blocks with ", _free_blocks_num);
      print_scaled_words(st, _free_blocks_word_size, scale);
    }
  } else {
    totals().print_on(st, scale);
    st->print(", ");
    st->print("deallocated: " UINTX_FORMAT " blocks with ", _free_blocks_num);
    print_scaled_words(st, _free_blocks_word_size, scale);
  }
}

#ifdef ASSERT

void ArenaStats::verify() const {
  size_t total_used = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    _stats[l].verify();
    total_used += _stats[l]._used_words;
  }
  // Deallocated allocations still count as used
  assert(total_used >= _free_blocks_word_size,
         "Sanity");
}
#endif

// Returns total arena statistics for both class and non-class metaspace
ArenaStats ClmsStats::totals() const {
  ArenaStats out;
  out.add(_arena_stats_nonclass);
  out.add(_arena_stats_class);
  return out;
}

void ClmsStats::print_on(outputStream* st, size_t scale, bool detailed) const {
  streamIndentor sti(st);
  st->cr_indent();
  if (Metaspace::using_class_space()) {
    st->print("Non-Class: ");
  }
  _arena_stats_nonclass.print_on(st, scale, detailed);
  if (detailed) {
    st->cr();
  }
  if (Metaspace::using_class_space()) {
    st->cr_indent();
    st->print("    Class: ");
    _arena_stats_class.print_on(st, scale, detailed);
    if (detailed) {
      st->cr();
    }
    st->cr_indent();
    st->print("     Both: ");
    totals().print_on(st, scale, detailed);
    if (detailed) {
      st->cr();
    }
  }
  st->cr();
}

#ifdef ASSERT
void ClmsStats::verify() const {
  _arena_stats_nonclass.verify();
  _arena_stats_class.verify();
}
#endif

} // end namespace metaspace

