/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "memory/allocation.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/os.hpp"
#include "runtime/task.hpp"
#include "runtime/threadCritical.hpp"
#include "services/memTracker.hpp"
#include "utilities/align.hpp"
#include "utilities/debug.hpp"
#include "utilities/ostream.hpp"

// Pre-defined default chunk sizes must be arena-aligned, see Chunk::operator new()
STATIC_ASSERT(is_aligned((int)Chunk::tiny_size, ARENA_AMALLOC_ALIGNMENT));
STATIC_ASSERT(is_aligned((int)Chunk::init_size, ARENA_AMALLOC_ALIGNMENT));
STATIC_ASSERT(is_aligned((int)Chunk::medium_size, ARENA_AMALLOC_ALIGNMENT));
STATIC_ASSERT(is_aligned((int)Chunk::size, ARENA_AMALLOC_ALIGNMENT));
STATIC_ASSERT(is_aligned((int)Chunk::non_pool_size, ARENA_AMALLOC_ALIGNMENT));

//--------------------------------------------------------------------------------------
// ChunkPool implementation

// MT-safe pool of chunks to reduce malloc/free thrashing
// NB: not using Mutex because pools are used before Threads are initialized
class ChunkPool: public CHeapObj<mtInternal> {
  Chunk*       _first;        // first cached Chunk; its first word points to next chunk
  size_t       _num_chunks;   // number of unused chunks in pool
  size_t       _num_used;     // number of chunks currently checked out
  const size_t _size;         // size of each chunk (must be uniform)

  // Our four static pools
  static ChunkPool* _large_pool;
  static ChunkPool* _medium_pool;
  static ChunkPool* _small_pool;
  static ChunkPool* _tiny_pool;

  // return first element or null
  void* get_first() {
    Chunk* c = _first;
    if (_first) {
      _first = _first->next();
      _num_chunks--;
    }
    return c;
  }

 public:
  // All chunks in a ChunkPool has the same size
   ChunkPool(size_t size) : _size(size) { _first = NULL; _num_chunks = _num_used = 0; }

  // Allocate a new chunk from the pool (might expand the pool)
  NOINLINE void* allocate(size_t bytes, AllocFailType alloc_failmode) {
    assert(bytes == _size, "bad size");
    void* p = NULL;
    // No VM lock can be taken inside ThreadCritical lock, so os::malloc
    // should be done outside ThreadCritical lock due to NMT
    { ThreadCritical tc;
      _num_used++;
      p = get_first();
    }
    if (p == NULL) p = os::malloc(bytes, mtChunk, CURRENT_PC);
    if (p == NULL && alloc_failmode == AllocFailStrategy::EXIT_OOM) {
      vm_exit_out_of_memory(bytes, OOM_MALLOC_ERROR, "ChunkPool::allocate");
    }
    return p;
  }

  // Return a chunk to the pool
  void free(Chunk* chunk) {
    assert(chunk->length() + Chunk::aligned_overhead_size() == _size, "bad size");
    ThreadCritical tc;
    _num_used--;

    // Add chunk to list
    chunk->set_next(_first);
    _first = chunk;
    _num_chunks++;
  }

  // Prune the pool
  void free_all_but(size_t n) {
    Chunk* cur = NULL;
    Chunk* next;
    {
      // if we have more than n chunks, free all of them
      ThreadCritical tc;
      if (_num_chunks > n) {
        // free chunks at end of queue, for better locality
        cur = _first;
        for (size_t i = 0; i < (n - 1) && cur != NULL; i++) cur = cur->next();

        if (cur != NULL) {
          next = cur->next();
          cur->set_next(NULL);
          cur = next;

          // Free all remaining chunks while in ThreadCritical lock
          // so NMT adjustment is stable.
          while(cur != NULL) {
            next = cur->next();
            os::free(cur);
            _num_chunks--;
            cur = next;
          }
        }
      }
    }
  }

  // Accessors to preallocated pool's
  static ChunkPool* large_pool()  { assert(_large_pool  != NULL, "must be initialized"); return _large_pool;  }
  static ChunkPool* medium_pool() { assert(_medium_pool != NULL, "must be initialized"); return _medium_pool; }
  static ChunkPool* small_pool()  { assert(_small_pool  != NULL, "must be initialized"); return _small_pool;  }
  static ChunkPool* tiny_pool()   { assert(_tiny_pool   != NULL, "must be initialized"); return _tiny_pool;   }

  static void initialize() {
    _large_pool  = new ChunkPool(Chunk::size        + Chunk::aligned_overhead_size());
    _medium_pool = new ChunkPool(Chunk::medium_size + Chunk::aligned_overhead_size());
    _small_pool  = new ChunkPool(Chunk::init_size   + Chunk::aligned_overhead_size());
    _tiny_pool   = new ChunkPool(Chunk::tiny_size   + Chunk::aligned_overhead_size());
  }

  static void clean() {
    enum { BlocksToKeep = 5 };
     _tiny_pool->free_all_but(BlocksToKeep);
     _small_pool->free_all_but(BlocksToKeep);
     _medium_pool->free_all_but(BlocksToKeep);
     _large_pool->free_all_but(BlocksToKeep);
  }
};

ChunkPool* ChunkPool::_large_pool  = NULL;
ChunkPool* ChunkPool::_medium_pool = NULL;
ChunkPool* ChunkPool::_small_pool  = NULL;
ChunkPool* ChunkPool::_tiny_pool   = NULL;

void chunkpool_init() {
  ChunkPool::initialize();
}


//--------------------------------------------------------------------------------------
// ChunkPoolCleaner implementation
//

class ChunkPoolCleaner : public PeriodicTask {
  enum { CleaningInterval = 5000 };      // cleaning interval in ms

 public:
   ChunkPoolCleaner() : PeriodicTask(CleaningInterval) {}
   void task() {
     ChunkPool::clean();
   }
};

//--------------------------------------------------------------------------------------
// Chunk implementation

void* Chunk::operator new (size_t sizeofChunk, AllocFailType alloc_failmode, size_t length) throw() {

  // - requested_size = sizeof(Chunk)
  // - length = payload size
  // We must ensure that the boundaries of the payload (C and D) are aligned to 64-bit:
  //
  // +-----------+--+--------------------------------------------+
  // |           |g |                                            |
  // | Chunk     |a |               Payload                      |
  // |           |p |                                            |
  // +-----------+--+--------------------------------------------+
  // A           B  C                                            D
  //
  // - The Chunk is allocated from C-heap, therefore its start address (A) should be
  //   64-bit aligned on all our platforms, including 32-bit.
  // - sizeof(Chunk) (B) may not be aligned to 64-bit, and we have to take that into
  //   account when calculating the Payload bottom (C) (see Chunk::bottom())
  // - the payload size (length) must be aligned to 64-bit, which takes care of 64-bit
  //   aligning (D)

  assert(sizeofChunk == sizeof(Chunk), "weird request size");
  assert(is_aligned(length, ARENA_AMALLOC_ALIGNMENT), "chunk payload length misaligned: "
         SIZE_FORMAT ".", length);
  size_t bytes = ARENA_ALIGN(sizeofChunk) + length;
  switch (length) {
   case Chunk::size:        return ChunkPool::large_pool()->allocate(bytes, alloc_failmode);
   case Chunk::medium_size: return ChunkPool::medium_pool()->allocate(bytes, alloc_failmode);
   case Chunk::init_size:   return ChunkPool::small_pool()->allocate(bytes, alloc_failmode);
   case Chunk::tiny_size:   return ChunkPool::tiny_pool()->allocate(bytes, alloc_failmode);
   default: {
     void* p = os::malloc(bytes, mtChunk, CALLER_PC);
     if (p == NULL && alloc_failmode == AllocFailStrategy::EXIT_OOM) {
       vm_exit_out_of_memory(bytes, OOM_MALLOC_ERROR, "Chunk::new");
     }
     // We rely on arena alignment <= malloc alignment.
     assert(is_aligned(p, ARENA_AMALLOC_ALIGNMENT), "Chunk start address misaligned.");
     return p;
   }
  }
}

void Chunk::operator delete(void* p) {
  Chunk* c = (Chunk*)p;
  switch (c->length()) {
   case Chunk::size:        ChunkPool::large_pool()->free(c); break;
   case Chunk::medium_size: ChunkPool::medium_pool()->free(c); break;
   case Chunk::init_size:   ChunkPool::small_pool()->free(c); break;
   case Chunk::tiny_size:   ChunkPool::tiny_pool()->free(c); break;
   default:
     ThreadCritical tc;  // Free chunks under TC lock so that NMT adjustment is stable.
     os::free(c);
  }
}

Chunk::Chunk(size_t length) : _len(length) {
  _next = NULL;         // Chain on the linked list
}

void Chunk::chop() {
  Chunk *k = this;
  while( k ) {
    Chunk *tmp = k->next();
    // clear out this chunk (to detect allocation bugs)
    if (ZapResourceArea) memset(k->bottom(), badResourceValue, k->length());
    delete k;                   // Free chunk (was malloc'd)
    k = tmp;
  }
}

void Chunk::next_chop() {
  _next->chop();
  _next = NULL;
}

void Chunk::start_chunk_pool_cleaner_task() {
#ifdef ASSERT
  static bool task_created = false;
  assert(!task_created, "should not start chuck pool cleaner twice");
  task_created = true;
#endif
  ChunkPoolCleaner* cleaner = new ChunkPoolCleaner();
  cleaner->enroll();
}

//------------------------------Arena------------------------------------------

Arena::Arena(MEMFLAGS flag, size_t init_size) : _flags(flag), _size_in_bytes(0)  {
  init_size = ARENA_ALIGN(init_size);
  _first = _chunk = new (AllocFailStrategy::EXIT_OOM, init_size) Chunk(init_size);
  _hwm = _chunk->bottom();      // Save the cached hwm, max
  _max = _chunk->top();
  MemTracker::record_new_arena(flag);
  set_size_in_bytes(init_size);
}

Arena::Arena(MEMFLAGS flag) : _flags(flag), _size_in_bytes(0) {
  _first = _chunk = new (AllocFailStrategy::EXIT_OOM, Chunk::init_size) Chunk(Chunk::init_size);
  _hwm = _chunk->bottom();      // Save the cached hwm, max
  _max = _chunk->top();
  MemTracker::record_new_arena(flag);
  set_size_in_bytes(Chunk::init_size);
}

Arena *Arena::move_contents(Arena *copy) {
  copy->destruct_contents();
  copy->_chunk = _chunk;
  copy->_hwm   = _hwm;
  copy->_max   = _max;
  copy->_first = _first;

  // workaround rare racing condition, which could double count
  // the arena size by native memory tracking
  size_t size = size_in_bytes();
  set_size_in_bytes(0);
  copy->set_size_in_bytes(size);
  // Destroy original arena
  reset();
  return copy;            // Return Arena with contents
}

Arena::~Arena() {
  destruct_contents();
  MemTracker::record_arena_free(_flags);
}

void* Arena::operator new(size_t size) throw() {
  assert(false, "Use dynamic memory type binding");
  return NULL;
}

void* Arena::operator new (size_t size, const std::nothrow_t&  nothrow_constant) throw() {
  assert(false, "Use dynamic memory type binding");
  return NULL;
}

  // dynamic memory type binding
void* Arena::operator new(size_t size, MEMFLAGS flags) throw() {
  return (void *) AllocateHeap(size, flags, CALLER_PC);
}

void* Arena::operator new(size_t size, const std::nothrow_t& nothrow_constant, MEMFLAGS flags) throw() {
  return (void*)AllocateHeap(size, flags, CALLER_PC, AllocFailStrategy::RETURN_NULL);
}

void Arena::operator delete(void* p) {
  FreeHeap(p);
}

// Destroy this arenas contents and reset to empty
void Arena::destruct_contents() {
  if (UseMallocOnly && _first != NULL) {
    char* end = _first->next() ? _first->top() : _hwm;
    free_malloced_objects(_first, _first->bottom(), end, _hwm);
  }
  // reset size before chop to avoid a rare racing condition
  // that can have total arena memory exceed total chunk memory
  set_size_in_bytes(0);
  if (_first != NULL) {
    _first->chop();
  }
  reset();
}

// This is high traffic method, but many calls actually don't
// change the size
void Arena::set_size_in_bytes(size_t size) {
  if (_size_in_bytes != size) {
    ssize_t delta = size - size_in_bytes();
    _size_in_bytes = size;
    MemTracker::record_arena_size_change(delta, _flags);
  }
}

// Total of all Chunks in arena
size_t Arena::used() const {
  size_t sum = _chunk->length() - (_max-_hwm); // Size leftover in this Chunk
  Chunk *k = _first;
  while( k != _chunk) {         // Whilst have Chunks in a row
    sum += k->length();         // Total size of this Chunk
    k = k->next();              // Bump along to next Chunk
  }
  return sum;                   // Return total consumed space.
}

// Grow a new Chunk
void* Arena::grow(size_t x, AllocFailType alloc_failmode) {
  // Get minimal required size.  Either real big, or even bigger for giant objs
  // (Note: all chunk sizes have to be 64-bit aligned)
  size_t len = MAX2(ARENA_ALIGN(x), (size_t) Chunk::size);

  Chunk *k = _chunk;            // Get filled-up chunk address
  _chunk = new (alloc_failmode, len) Chunk(len);

  if (_chunk == NULL) {
    _chunk = k;                 // restore the previous value of _chunk
    return NULL;
  }
  if (k) k->set_next(_chunk);   // Append new chunk to end of linked list
  else _first = _chunk;
  _hwm  = _chunk->bottom();     // Save the cached hwm, max
  _max =  _chunk->top();
  set_size_in_bytes(size_in_bytes() + len);
  void* result = _hwm;
  _hwm += x;
  return result;
}



// Reallocate storage in Arena.
void *Arena::Arealloc(void* old_ptr, size_t old_size, size_t new_size, AllocFailType alloc_failmode) {
  if (new_size == 0) {
    Afree(old_ptr, old_size); // like realloc(3)
    return NULL;
  }
  if (old_ptr == NULL) {
    assert(old_size == 0, "sanity");
    return Amalloc(new_size, alloc_failmode); // as with realloc(3), a NULL old ptr is equivalent to malloc(3)
  }
#ifdef ASSERT
  if (UseMallocOnly) {
    // always allocate a new object  (otherwise we'll free this one twice)
    char* copy = (char*)Amalloc(new_size, alloc_failmode);
    if (copy == NULL) {
      return NULL;
    }
    size_t n = MIN2(old_size, new_size);
    if (n > 0) memcpy(copy, old_ptr, n);
    Afree(old_ptr,old_size);    // Mostly done to keep stats accurate
    return copy;
  }
#endif
  char *c_old = (char*)old_ptr; // Handy name
  // Stupid fast special case
  if( new_size <= old_size ) {  // Shrink in-place
    if( c_old+old_size == _hwm) // Attempt to free the excess bytes
      _hwm = c_old+new_size;    // Adjust hwm
    return c_old;
  }

  // make sure that new_size is legal
  size_t corrected_new_size = ARENA_ALIGN(new_size);

  // See if we can resize in-place
  if( (c_old+old_size == _hwm) &&       // Adjusting recent thing
      (c_old+corrected_new_size <= _max) ) {      // Still fits where it sits
    _hwm = c_old+corrected_new_size;      // Adjust hwm
    return c_old;               // Return old pointer
  }

  // Oops, got to relocate guts
  void *new_ptr = Amalloc(new_size, alloc_failmode);
  if (new_ptr == NULL) {
    return NULL;
  }
  memcpy( new_ptr, c_old, old_size );
  Afree(c_old,old_size);        // Mostly done to keep stats accurate
  return new_ptr;
}


// Determine if pointer belongs to this Arena or not.
bool Arena::contains( const void *ptr ) const {
#ifdef ASSERT
  if (UseMallocOnly) {
    // really slow, but not easy to make fast
    if (_chunk == NULL) return false;
    char** bottom = (char**)_chunk->bottom();
    for (char** p = (char**)_hwm - 1; p >= bottom; p--) {
      if (*p == ptr) return true;
    }
    for (Chunk *c = _first; c != NULL; c = c->next()) {
      if (c == _chunk) continue;  // current chunk has been processed
      char** bottom = (char**)c->bottom();
      for (char** p = (char**)c->top() - 1; p >= bottom; p--) {
        if (*p == ptr) return true;
      }
    }
    return false;
  }
#endif
  if( (void*)_chunk->bottom() <= ptr && ptr < (void*)_hwm )
    return true;                // Check for in this chunk
  for (Chunk *c = _first; c; c = c->next()) {
    if (c == _chunk) continue;  // current chunk has been processed
    if ((void*)c->bottom() <= ptr && ptr < (void*)c->top()) {
      return true;              // Check for every chunk in Arena
    }
  }
  return false;                 // Not in any Chunk, so not in Arena
}


#ifdef ASSERT
void* Arena::malloc(size_t size) {
  assert(UseMallocOnly, "shouldn't call");
  // use malloc, but save pointer in res. area for later freeing
  char** save = (char**)internal_amalloc(sizeof(char*));
  return (*save = (char*)os::malloc(size, mtChunk));
}
#endif


//--------------------------------------------------------------------------------------
// Non-product code

#ifndef PRODUCT

// debugging code
inline void Arena::free_all(char** start, char** end) {
  for (char** p = start; p < end; p++) if (*p) os::free(*p);
}

void Arena::free_malloced_objects(Chunk* chunk, char* hwm, char* max, char* hwm2) {
  assert(UseMallocOnly, "should not call");
  // free all objects malloced since resource mark was created; resource area
  // contains their addresses
  if (chunk->next()) {
    // this chunk is full, and some others too
    for (Chunk* c = chunk->next(); c != NULL; c = c->next()) {
      char* top = c->top();
      if (c->next() == NULL) {
        top = hwm2;     // last junk is only used up to hwm2
        assert(c->contains(hwm2), "bad hwm2");
      }
      free_all((char**)c->bottom(), (char**)top);
    }
    assert(chunk->contains(hwm), "bad hwm");
    assert(chunk->contains(max), "bad max");
    free_all((char**)hwm, (char**)max);
  } else {
    // this chunk was partially used
    assert(chunk->contains(hwm), "bad hwm");
    assert(chunk->contains(hwm2), "bad hwm2");
    free_all((char**)hwm, (char**)hwm2);
  }
}

#endif // Non-product
