/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021 SAP SE. All rights reserved.
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
#include "logging/log.hpp"
#include "memory/allocation.hpp"
#include "memory/metaspace/chunkHeaderPool.hpp"
#include "memory/metaspace/chunkManager.hpp"
#include "memory/metaspace/freeChunkList.hpp"
#include "memory/metaspace/metachunk.hpp"
#include "memory/metaspace/metaspaceCommon.hpp"
#include "memory/metaspace/rootChunkArea.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"

namespace metaspace {

RootChunkArea::RootChunkArea(const MetaWord* base) :
  _base(base),
  _first_chunk(NULL)
{}

RootChunkArea::~RootChunkArea() {
  // This is called when a VirtualSpaceNode is destructed (purged).
  // All chunks should be free of course. In fact, there should only
  // be one chunk, since all free chunks should have been merged.
  if (_first_chunk != NULL) {
    assert(_first_chunk->is_root_chunk() && _first_chunk->is_free(),
           "Cannot delete root chunk area if not all chunks are free.");
    ChunkHeaderPool::pool()->return_chunk_header(_first_chunk);
  }
}

// Initialize: allocate a root node and a root chunk header; return the
// root chunk header. It will be partly initialized.
// Note: this just allocates a memory-less header; memory itself is allocated inside VirtualSpaceNode.
Metachunk* RootChunkArea::alloc_root_chunk_header(VirtualSpaceNode* node) {
  assert(_first_chunk == 0, "already have a root");
  Metachunk* c = ChunkHeaderPool::pool()->allocate_chunk_header();
  c->initialize(node, const_cast<MetaWord*>(_base), chunklevel::ROOT_CHUNK_LEVEL);
  _first_chunk = c;
  return c;
}

// Given a chunk c, split it recursively until you get a chunk of the given target_level.
//
// The resulting target chunk resides at the same address as the original chunk.
// The resulting splinters are added to freelists.
//
// Returns pointer to the result chunk; the splitted-off chunks are added as
//  free chunks to the freelists.
void RootChunkArea::split(chunklevel_t target_level, Metachunk* c, FreeChunkListVector* freelists) {
  // Splitting a chunk once works like this:
  //
  // For a given chunk we want to split:
  // - increase the chunk level (which halves its size)
  // - (but leave base address as it is since it will be the leader of the newly
  //    created chunk pair)
  // - then create a new chunk header of the same level, set its memory range
  //   to cover the second half of the old chunk.
  // - wire them up (prev_in_vs/next_in_vs)
  // - return the follower chunk as "splinter chunk" in the splinters array.

  // Doing this multiple times will create a new free splinter chunk for every
  // level we split:
  //
  // A  <- original chunk
  //
  // B B  <- split into two halves
  //
  // C C B  <- first half split again
  //
  // D D C B  <- first half split again ...
  //

  DEBUG_ONLY(check_pointer(c->base());)
  DEBUG_ONLY(c->verify();)
  assert(c->is_free(), "Can only split free chunks.");

  DEBUG_ONLY(chunklevel::check_valid_level(target_level));
  assert(target_level > c->level(), "Wrong target level");

  const chunklevel_t starting_level = c->level();

  while (c->level() < target_level) {

    log_trace(metaspace)("Splitting chunk: " METACHUNK_FULL_FORMAT ".", METACHUNK_FULL_FORMAT_ARGS(c));

    c->inc_level();
    Metachunk* splinter_chunk = ChunkHeaderPool::pool()->allocate_chunk_header();
    splinter_chunk->initialize(c->vsnode(), c->end(), c->level());

    // Fix committed words info: If over the half of the original chunk was
    // committed, committed area spills over into the follower chunk.
    const size_t old_committed_words = c->committed_words();
    if (old_committed_words > c->word_size()) {
      c->set_committed_words(c->word_size());
      splinter_chunk->set_committed_words(old_committed_words - c->word_size());
    } else {
      splinter_chunk->set_committed_words(0);
    }

    // Insert splinter chunk into vs list
    if (c->next_in_vs() != NULL) {
      c->next_in_vs()->set_prev_in_vs(splinter_chunk);
    }
    splinter_chunk->set_next_in_vs(c->next_in_vs());
    splinter_chunk->set_prev_in_vs(c);
    c->set_next_in_vs(splinter_chunk);

    log_trace(metaspace)(".. Result chunk: " METACHUNK_FULL_FORMAT ".", METACHUNK_FULL_FORMAT_ARGS(c));
    log_trace(metaspace)(".. Splinter chunk: " METACHUNK_FULL_FORMAT ".", METACHUNK_FULL_FORMAT_ARGS(splinter_chunk));

    // Add splinter to free lists
    freelists->add(splinter_chunk);
  }

  assert(c->level() == target_level, "Sanity");

  DEBUG_ONLY(verify();)
  DEBUG_ONLY(c->verify();)
}

// Given a chunk, attempt to merge it recursively with its neighboring chunks.
//
// If successful (merged at least once), returns address of
// the merged chunk; NULL otherwise.
//
// The merged chunks are removed from the freelists.
//
// !!! Please note that if this method returns a non-NULL value, the
// original chunk will be invalid and should not be accessed anymore! !!!
Metachunk* RootChunkArea::merge(Metachunk* c, FreeChunkListVector* freelists) {
  // Note rules:
  //
  // - a chunk always has a buddy, unless it is a root chunk.
  // - In that buddy pair, a chunk is either leader or follower.
  // - a chunk's base address is always aligned at its size.
  // - if chunk is leader, its base address is also aligned to the size of the next
  //   lower level, at least. A follower chunk is not.

  // How we merge once:
  //
  // For a given chunk c, which has to be free and non-root, we do:
  // - find out if we are the leader or the follower chunk
  // - if we are leader, next_in_vs must be the follower; if we are follower,
  //   prev_in_vs must be the leader. Now we have the buddy chunk.
  // - However, if the buddy chunk itself is split (of a level higher than us)
  //   we cannot merge.
  // - we can only merge if the buddy is of the same level as we are and it is
  //   free.
  // - Then we merge by simply removing the follower chunk from the address range
  //   linked list (returning the now useless header to the pool) and decreasing
  //   the leader chunk level by one. That makes it double the size.

  // Example:
  // (lower case chunks are free, the * indicates the chunk we want to merge):
  //
  // ........................
  // d d*c   b       A           <- we return the second (d*) chunk...
  //
  // c*  c   b       A           <- we merge it with its predecessor and decrease its level...
  //
  // b*      b       A           <- we merge it again, since its new neighbor was free too...
  //
  // a*              A           <- we merge it again, since its new neighbor was free too...
  //
  // And we are done, since its new neighbor, (A), is not free. We would also be done
  // if the new neighbor itself is splintered.

  DEBUG_ONLY(check_pointer(c->base());)
  assert(!c->is_root_chunk(), "Cannot be merged further.");
  assert(c->is_free(), "Can only merge free chunks.");

  DEBUG_ONLY(c->verify();)

  log_trace(metaspace)("Attempting to merge chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(c));

  const chunklevel_t starting_level = c->level();

  bool stop = false;
  Metachunk* result = NULL;

  do {

    // First find out if this chunk is the leader of its pair
    const bool is_leader = c->is_leader();

    // Note: this is either our buddy or a splinter of the buddy.
    Metachunk* const buddy = c->is_leader() ? c->next_in_vs() : c->prev_in_vs();
    DEBUG_ONLY(buddy->verify();)

    // A buddy chunk must be of the same or higher level (so, same size or smaller)
    // never be larger.
    assert(buddy->level() >= c->level(), "Sanity");

    // Is this really my buddy (same level) or a splinter of it (higher level)?
    // Also, is it free?
    if (buddy->level() != c->level() || buddy->is_free() == false) {
      log_trace(metaspace)("cannot merge with chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(buddy));
      stop = true;
    } else {
      log_trace(metaspace)("will merge with chunk " METACHUNK_FORMAT ".", METACHUNK_FORMAT_ARGS(buddy));

      // We can merge with the buddy.
      // First, remove buddy from the chunk manager.
      assert(buddy->is_free(), "Sanity");
      freelists->remove(buddy);

      // Determine current leader and follower
      Metachunk* leader;
      Metachunk* follower;
      if (is_leader) {
        leader = c; follower = buddy;
      } else {
        leader = buddy; follower = c;
      }

      // Last checkpoint
      assert(leader->end() == follower->base() &&
             leader->level() == follower->level() &&
             leader->is_free() && follower->is_free(), "Sanity");

      // The new merged chunk is as far committed as possible (if leader
      // chunk is fully committed, as far as the follower chunk).
      size_t merged_committed_words = leader->committed_words();
      if (merged_committed_words == leader->word_size()) {
        merged_committed_words += follower->committed_words();
      }

      // Leader survives, follower chunk is freed. Remove follower from vslist ..
      leader->set_next_in_vs(follower->next_in_vs());
      if (follower->next_in_vs() != NULL) {
        follower->next_in_vs()->set_prev_in_vs(leader);
      }

      // .. and return follower chunk header to pool for reuse.
      ChunkHeaderPool::pool()->return_chunk_header(follower);

      // Leader level gets decreased (leader chunk doubles in size) but
      // base address stays the same.
      leader->dec_level();

      // set commit boundary
      leader->set_committed_words(merged_committed_words);

      // If the leader is now of root chunk size, stop merging
      if (leader->is_root_chunk()) {
        stop = true;
      }

      result = c = leader;
      DEBUG_ONLY(leader->verify();)
    }
  } while (!stop);

#ifdef ASSERT
  verify();
  if (result != NULL) {
    result->verify();
  }
#endif // ASSERT
  return result;
}

// Given a chunk c, which must be "in use" and must not be a root chunk, attempt to
// enlarge it in place by claiming its trailing buddy.
//
// This will only work if c is the leader of the buddy pair and the trailing buddy is free.
//
// If successful, the follower chunk will be removed from the freelists, the leader chunk c will
// double in size (level decreased by one).
//
// On success, true is returned, false otherwise.
bool RootChunkArea::attempt_enlarge_chunk(Metachunk* c, FreeChunkListVector* freelists) {
  DEBUG_ONLY(check_pointer(c->base());)
  assert(!c->is_root_chunk(), "Cannot be merged further.");

  // There is no real reason for this limitation other than it is not
  // needed on free chunks since they should be merged already:
  assert(c->is_in_use(), "Can only enlarge in use chunks.");
  DEBUG_ONLY(c->verify();)

  if (!c->is_leader()) {
    return false;
  }

  // We are the leader, so the buddy must follow us.
  Metachunk* const buddy = c->next_in_vs();
  DEBUG_ONLY(buddy->verify();)

  // Of course buddy cannot be larger than us.
  assert(buddy->level() >= c->level(), "Sanity");

  // We cannot merge buddy in if it is not free...
  if (!buddy->is_free()) {
    return false;
  }
  // ... nor if it is splintered.
  if (buddy->level() != c->level()) {
    return false;
  }

  // Okay, lets enlarge c.
  log_trace(metaspace)("Enlarging chunk " METACHUNK_FULL_FORMAT " by merging in follower " METACHUNK_FULL_FORMAT ".",
                       METACHUNK_FULL_FORMAT_ARGS(c), METACHUNK_FULL_FORMAT_ARGS(buddy));

  // the enlarged c is as far committed as possible:
  size_t merged_committed_words = c->committed_words();
  if (merged_committed_words == c->word_size()) {
    merged_committed_words += buddy->committed_words();
  }

  // Remove buddy from vs list...
  Metachunk* successor = buddy->next_in_vs();
  if (successor != NULL) {
    successor->set_prev_in_vs(c);
  }
  c->set_next_in_vs(successor);

  // .. and from freelist ...
  freelists->remove(buddy);

  // .. and return its empty husk to the pool...
  ChunkHeaderPool::pool()->return_chunk_header(buddy);

  // Then decrease level of c.
  c->dec_level();

  // and correct committed words if needed.
  c->set_committed_words(merged_committed_words);

  log_debug(metaspace)("Enlarged chunk " METACHUNK_FULL_FORMAT ".", METACHUNK_FULL_FORMAT_ARGS(c));

  DEBUG_ONLY(verify());
  return true;
}

// Returns true if this root chunk area is completely free:
//  In that case, it should only contain one chunk (maximally merged, so a root chunk)
//  and it should be free.
bool RootChunkArea::is_free() const {
  return _first_chunk == NULL ||
      (_first_chunk->is_root_chunk() && _first_chunk->is_free());
}

#ifdef ASSERT

#define assrt_(cond, ...) \
  if (!(cond)) { \
    fdStream errst(2); \
    this->print_on(&errst); \
    vmassert(cond, __VA_ARGS__); \
  }

void RootChunkArea::verify() const {
  assert_lock_strong(Metaspace_lock);
  assert_is_aligned(_base, chunklevel::MAX_CHUNK_BYTE_SIZE);

  // Iterate thru all chunks in this area. They must be ordered correctly,
  // being adjacent to each other, and cover the complete area
  int num_chunk = 0;

  if (_first_chunk != NULL) {
    assrt_(_first_chunk->prev_in_vs() == NULL, "Sanity");

    const Metachunk* c = _first_chunk;
    const MetaWord* expected_next_base = _base;
    const MetaWord* const area_end = _base + word_size();

    while (c != NULL) {
      assrt_(c->is_free() || c->is_in_use(),
          "Chunk No. %d " METACHUNK_FORMAT " - invalid state.",
          num_chunk, METACHUNK_FORMAT_ARGS(c));
      assrt_(c->base() == expected_next_base,
             "Chunk No. %d " METACHUNK_FORMAT " - unexpected base.",
             num_chunk, METACHUNK_FORMAT_ARGS(c));
      assrt_(c->base() >= base() && c->end() <= end(),
             "chunk %d " METACHUNK_FORMAT " oob for this root area [" PTR_FORMAT ".." PTR_FORMAT ").",
             num_chunk, METACHUNK_FORMAT_ARGS(c), p2i(base()), p2i(end()));
      assrt_(is_aligned(c->base(), c->word_size()),
             "misaligned chunk %d " METACHUNK_FORMAT ".", num_chunk, METACHUNK_FORMAT_ARGS(c));

      c->verify_neighborhood();
      c->verify();
      expected_next_base = c->end();
      num_chunk++;
      c = c->next_in_vs();
    }
    assrt_(expected_next_base == _base + word_size(), "Sanity");
  }
}

void RootChunkArea::verify_area_is_ideally_merged() const {
  SOMETIMES(assert_lock_strong(Metaspace_lock);)
  int num_chunk = 0;
  for (const Metachunk* c = _first_chunk; c != NULL; c = c->next_in_vs()) {
    if (!c->is_root_chunk() && c->is_free()) {
      // If a chunk is free, it must not have a buddy which is also free, because
      // those chunks should have been merged.
      // In other words, a buddy shall be either in-use or splintered
      // (which in turn would mean part of it are in use).
      Metachunk* const buddy = c->is_leader() ? c->next_in_vs() : c->prev_in_vs();
      assrt_(buddy->is_in_use() || buddy->level() > c->level(),
             "Chunk No. %d " METACHUNK_FORMAT " : missed merge opportunity with neighbor " METACHUNK_FORMAT ".",
             num_chunk, METACHUNK_FORMAT_ARGS(c), METACHUNK_FORMAT_ARGS(buddy));
    }
    num_chunk++;
  }
}

#endif

void RootChunkArea::print_on(outputStream* st) const {
  st->print(PTR_FORMAT ": ", p2i(base()));
  if (_first_chunk != NULL) {
    const Metachunk* c = _first_chunk;
    //                                    01234567890123
    const char* letters_for_levels_cap = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    const char* letters_for_levels =     "abcdefghijklmnopqrstuvwxyz";
    while (c != NULL) {
      const chunklevel_t l = c->level();
      if (l >= 0 && (size_t)l < strlen(letters_for_levels)) {
        st->print("%c", c->is_free() ? letters_for_levels[c->level()] : letters_for_levels_cap[c->level()]);
      } else {
        // Obviously garbage, but lets not crash.
        st->print("?");
      }
      c = c->next_in_vs();
    }
  } else {
    st->print(" (no chunks)");
  }
  st->cr();
}

// Create an array of ChunkTree objects, all initialized to NULL, covering
// a given memory range. Memory range must be a multiple of root chunk size.
RootChunkAreaLUT::RootChunkAreaLUT(const MetaWord* base, size_t word_size) :
  _base(base),
  _num((int)(word_size / chunklevel::MAX_CHUNK_WORD_SIZE)),
  _arr(NULL)
{
  assert_is_aligned(word_size, chunklevel::MAX_CHUNK_WORD_SIZE);
  _arr = NEW_C_HEAP_ARRAY(RootChunkArea, _num, mtClass);
  const MetaWord* this_base = _base;
  for (int i = 0; i < _num; i++) {
    RootChunkArea* rca = new(_arr + i) RootChunkArea(this_base);
    assert(rca == _arr + i, "Sanity");
    this_base += chunklevel::MAX_CHUNK_WORD_SIZE;
  }
}

RootChunkAreaLUT::~RootChunkAreaLUT() {
  for (int i = 0; i < _num; i++) {
    _arr[i].~RootChunkArea();
  }
  FREE_C_HEAP_ARRAY(RootChunkArea, _arr);
}

// Returns true if all areas in this area table are free (only contain free chunks).
bool RootChunkAreaLUT::is_free() const {
  for (int i = 0; i < _num; i++) {
    if (!_arr[i].is_free()) {
      return false;
    }
  }
  return true;
}

#ifdef ASSERT

void RootChunkAreaLUT::verify() const {
  for (int i = 0; i < _num; i++) {
    check_pointer(_arr[i].base());
    _arr[i].verify();
  }
}

#endif

void RootChunkAreaLUT::print_on(outputStream* st) const {
  for (int i = 0; i < _num; i++) {
    st->print("%2d:", i);
    _arr[i].print_on(st);
  }
}

} // end: namespace metaspace
