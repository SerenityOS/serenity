/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/metaspace/freeChunkList.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"

namespace metaspace {

// Calculates total number of committed words over all chunks (walks chunks).
size_t FreeChunkList::calc_committed_word_size() const {
  size_t s = 0;
  for (const Metachunk* c = _first; c != NULL; c = c->next()) {
    s += c->committed_words();
  }
  return s;
}

void FreeChunkList::print_on(outputStream* st) const {
  if (_num_chunks.get() > 0) {
    for (const Metachunk* c = _first; c != NULL; c = c->next()) {
      st->print(" - <");
      c->print_on(st);
      st->print(">");
    }
    st->print(" - total : %d chunks.", _num_chunks.get());
  } else {
    st->print("empty");
  }
}

#ifdef ASSERT

bool FreeChunkList::contains(const Metachunk* c) const {
  for (Metachunk* c2 = _first; c2 != NULL; c2 = c2->next()) {
    if (c2 == c) {
      return true;
    }
  }
  return false;
}

void FreeChunkList::verify() const {
  if (_first == NULL) {
    assert(_last == NULL, "Sanity");
  } else {
    assert(_last != NULL, "Sanity");
    int num = 0;
    bool uncommitted = (_first->committed_words() == 0);
    for (Metachunk* c = _first; c != NULL; c = c->next()) {
      assert(c->is_free(), "Chunks in freelist should be free");
      assert(c->used_words() == 0, "Chunk in freelist should have not used words.");
      assert(c->level() == _first->level(), "wrong level");
      assert(c->next() == NULL || c->next()->prev() == c, "front link broken");
      assert(c->prev() == NULL || c->prev()->next() == c, "back link broken");
      assert(c != c->prev() && c != c->next(), "circle");
      c->verify();
      num++;
    }
    _num_chunks.check(num);
  }
}

#endif // ASSERT

// Returns total size in all lists (regardless of commit state of underlying memory)
size_t FreeChunkListVector::word_size() const {
  size_t sum = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    sum += list_for_level(l)->num_chunks() * chunklevel::word_size_for_level(l);
  }
  return sum;
}

// Calculates total number of committed words over all chunks (walks chunks).
size_t FreeChunkListVector::calc_committed_word_size() const {
  size_t sum = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    sum += calc_committed_word_size_at_level(l);
  }
  return sum;
}

size_t FreeChunkListVector::calc_committed_word_size_at_level(chunklevel_t lvl) const {
  return list_for_level(lvl)->calc_committed_word_size();
}

// Returns total committed size in all lists
int FreeChunkListVector::num_chunks() const {
  int n = 0;
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    n += list_for_level(l)->num_chunks();
  }
  return n;
}

// Look for a chunk: starting at level, up to and including max_level,
//  return the first chunk whose committed words >= min_committed_words.
// Return NULL if no such chunk was found.
Metachunk* FreeChunkListVector::search_chunk_ascending(chunklevel_t level, chunklevel_t max_level, size_t min_committed_words) {
  assert(min_committed_words <= chunklevel::word_size_for_level(max_level),
         "min chunk size too small to hold min_committed_words");
  for (chunklevel_t l = level; l <= max_level; l++) {
    FreeChunkList* list = list_for_level(l);
    Metachunk* c = list->first_minimally_committed(min_committed_words);
    if (c != NULL) {
      list->remove(c);
      return c;
    }
  }
  return NULL;
}

// Look for a chunk: starting at level, down to (including) the root chunk level,
// return the first chunk whose committed words >= min_committed_words.
// Return NULL if no such chunk was found.
Metachunk* FreeChunkListVector::search_chunk_descending(chunklevel_t level, size_t min_committed_words) {
  for (chunklevel_t l = level; l >= chunklevel::LOWEST_CHUNK_LEVEL; l --) {
    FreeChunkList* list = list_for_level(l);
    Metachunk* c = list->first_minimally_committed(min_committed_words);
    if (c != NULL) {
      list->remove(c);
      return c;
    }
  }
  return NULL;
}

void FreeChunkListVector::print_on(outputStream* st) const {
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    st->print("-- List[" CHKLVL_FORMAT "]: ", l);
    list_for_level(l)->print_on(st);
    st->cr();
  }
  st->print_cr("total chunks: %d, total word size: " SIZE_FORMAT ".",
               num_chunks(), word_size());
}

#ifdef ASSERT

void FreeChunkListVector::verify() const {
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    list_for_level(l)->verify();
  }
}

bool FreeChunkListVector::contains(const Metachunk* c) const {
  for (chunklevel_t l = chunklevel::LOWEST_CHUNK_LEVEL; l <= chunklevel::HIGHEST_CHUNK_LEVEL; l++) {
    if (list_for_level(l)->contains(c)) {
      return true;
    }
  }
  return false;
}

#endif // ASSERT

} // namespace metaspace

