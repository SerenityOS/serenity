/*
 * Copyright (c) 2016, 2020, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHTASKQUEUE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHTASKQUEUE_HPP

#include "gc/shared/taskTerminator.hpp"
#include "gc/shared/taskqueue.hpp"
#include "gc/shenandoah/shenandoahPadding.hpp"
#include "memory/allocation.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutex.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"

template<class E, MEMFLAGS F, unsigned int N = TASKQUEUE_SIZE>
class BufferedOverflowTaskQueue: public OverflowTaskQueue<E, F, N>
{
public:
  typedef OverflowTaskQueue<E, F, N> taskqueue_t;

  BufferedOverflowTaskQueue() : _buf_empty(true) {};

  TASKQUEUE_STATS_ONLY(using taskqueue_t::stats;)

  // Push task t into the queue. Returns true on success.
  inline bool push(E t);

  // Attempt to pop from the queue. Returns true on success.
  inline bool pop(E &t);

  inline void clear();

  inline bool is_empty()        const {
    return _buf_empty && taskqueue_t::is_empty();
  }

private:
  bool _buf_empty;
  E _elem;
};

// ShenandoahMarkTask
//
// Encodes both regular oops, and the array oops plus chunking data for parallel array processing.
// The design goal is to make the regular oop ops very fast, because that would be the prevailing
// case. On the other hand, it should not block parallel array processing from efficiently dividing
// the array work.
//
// The idea is to steal the bits from the 64-bit oop to encode array data, if needed. For the
// proper divide-and-conquer strategies, we want to encode the "blocking" data. It turns out, the
// most efficient way to do this is to encode the array block as (chunk * 2^pow), where it is assumed
// that the block has the size of 2^pow. This requires for pow to have only 5 bits (2^32) to encode
// all possible arrays.
//
//    |xx-------oop---------|-pow-|--chunk---|
//    0                    49     54        64
//
// By definition, chunk == 0 means "no chunk", i.e. chunking starts from 1.
//
// Lower bits of oop are reserved to handle "skip_live" and "strong" properties. Since this encoding
// stores uncompressed oops, those bits are always available. These bits default to zero for "skip_live"
// and "weak". This aligns with their frequent values: strong/counted-live references.
//
// This encoding gives a few interesting benefits:
//
// a) Encoding/decoding regular oops is very simple, because the upper bits are zero in that task:
//
//    |---------oop---------|00000|0000000000| // no chunk data
//
//    This helps the most ubiquitous path. The initialization amounts to putting the oop into the word
//    with zero padding. Testing for "chunkedness" is testing for zero with chunk mask.
//
// b) Splitting tasks for divide-and-conquer is possible. Suppose we have chunk <C, P> that covers
// interval [ (C-1)*2^P; C*2^P ). We can then split it into two chunks:
//      <2*C - 1, P-1>, that covers interval [ (2*C - 2)*2^(P-1); (2*C - 1)*2^(P-1) )
//      <2*C, P-1>,     that covers interval [ (2*C - 1)*2^(P-1);       2*C*2^(P-1) )
//
//    Observe that the union of these two intervals is:
//      [ (2*C - 2)*2^(P-1); 2*C*2^(P-1) )
//
//    ...which is the original interval:
//      [ (C-1)*2^P; C*2^P )
//
// c) The divide-and-conquer strategy could even start with chunk <1, round-log2-len(arr)>, and split
//    down in the parallel threads, which alleviates the upfront (serial) splitting costs.
//
// Encoding limitations caused by current bitscales mean:
//    10 bits for chunk: max 1024 blocks per array
//     5 bits for power: max 2^32 array
//    49 bits for   oop: max 512 TB of addressable space
//
// Stealing bits from oop trims down the addressable space. Stealing too few bits for chunk ID limits
// potential parallelism. Stealing too few bits for pow limits the maximum array size that can be handled.
// In future, these might be rebalanced to favor one degree of freedom against another. For example,
// if/when Arrays 2.0 bring 2^64-sized arrays, we might need to steal another bit for power. We could regain
// some bits back if chunks are counted in ObjArrayMarkingStride units.
//
// There is also a fallback version that uses plain fields, when we don't have enough space to steal the
// bits from the native pointer. It is useful to debug the optimized version.
//

#ifdef _MSC_VER
#pragma warning(push)
// warning C4522: multiple assignment operators specified
#pragma warning( disable:4522 )
#endif

#ifdef _LP64
#define SHENANDOAH_OPTIMIZED_MARKTASK 1
#else
#define SHENANDOAH_OPTIMIZED_MARKTASK 0
#endif

#if SHENANDOAH_OPTIMIZED_MARKTASK
class ShenandoahMarkTask
{
private:
  // Everything is encoded into this field...
  uintptr_t _obj;

  // ...with these:
  static const uint8_t chunk_bits  = 10;
  static const uint8_t pow_bits    = 5;
  static const uint8_t oop_bits    = sizeof(uintptr_t)*8 - chunk_bits - pow_bits;

  static const uint8_t oop_shift   = 0;
  static const uint8_t pow_shift   = oop_bits;
  static const uint8_t chunk_shift = oop_bits + pow_bits;

  static const uintptr_t oop_extract_mask       = right_n_bits(oop_bits) - 3;
  static const uintptr_t skip_live_extract_mask = 1 << 0;
  static const uintptr_t weak_extract_mask      = 1 << 1;
  static const uintptr_t chunk_pow_extract_mask = ~right_n_bits(oop_bits);

  static const int chunk_range_mask = right_n_bits(chunk_bits);
  static const int pow_range_mask   = right_n_bits(pow_bits);

  inline oop decode_oop(uintptr_t val) const {
    STATIC_ASSERT(oop_shift == 0);
    return cast_to_oop(val & oop_extract_mask);
  }

  inline bool decode_not_chunked(uintptr_t val) const {
    // No need to shift for a comparison to zero
    return (val & chunk_pow_extract_mask) == 0;
  }

  inline int decode_chunk(uintptr_t val) const {
    return (int) ((val >> chunk_shift) & chunk_range_mask);
  }

  inline int decode_pow(uintptr_t val) const {
    return (int) ((val >> pow_shift) & pow_range_mask);
  }

  inline bool decode_weak(uintptr_t val) const {
    return (val & weak_extract_mask) != 0;
  }

  inline bool decode_cnt_live(uintptr_t val) const {
    return (val & skip_live_extract_mask) == 0;
  }

  inline uintptr_t encode_oop(oop obj, bool skip_live, bool weak) const {
    STATIC_ASSERT(oop_shift == 0);
    uintptr_t encoded = cast_from_oop<uintptr_t>(obj);
    if (skip_live) {
      encoded |= skip_live_extract_mask;
    }
    if (weak) {
      encoded |= weak_extract_mask;
    }
    return encoded;
  }

  inline uintptr_t encode_chunk(int chunk) const {
    return ((uintptr_t) chunk) << chunk_shift;
  }

  inline uintptr_t encode_pow(int pow) const {
    return ((uintptr_t) pow) << pow_shift;
  }

public:
  ShenandoahMarkTask(oop o = NULL, bool skip_live = false, bool weak = false) {
    uintptr_t enc = encode_oop(o, skip_live, weak);
    assert(decode_oop(enc) == o,     "oop encoding should work: " PTR_FORMAT, p2i(o));
    assert(decode_cnt_live(enc) == !skip_live, "skip_live encoding should work");
    assert(decode_weak(enc) == weak, "weak encoding should work");
    assert(decode_not_chunked(enc),  "task should not be chunked");
    _obj = enc;
  }

  ShenandoahMarkTask(oop o, bool skip_live, bool weak, int chunk, int pow) {
    uintptr_t enc_oop = encode_oop(o, skip_live, weak);
    uintptr_t enc_chunk = encode_chunk(chunk);
    uintptr_t enc_pow = encode_pow(pow);
    uintptr_t enc = enc_oop | enc_chunk | enc_pow;
    assert(decode_oop(enc) == o,       "oop encoding should work: " PTR_FORMAT, p2i(o));
    assert(decode_cnt_live(enc) == !skip_live, "skip_live should be true for chunked tasks");
    assert(decode_weak(enc) == weak,   "weak encoding should work");
    assert(decode_chunk(enc) == chunk, "chunk encoding should work: %d", chunk);
    assert(decode_pow(enc) == pow,     "pow encoding should work: %d", pow);
    assert(!decode_not_chunked(enc),   "task should be chunked");
    _obj = enc;
  }

  // Trivially copyable.

public:
  inline oop  obj()            const { return decode_oop(_obj);   }
  inline int  chunk()          const { return decode_chunk(_obj); }
  inline int  pow()            const { return decode_pow(_obj);   }

  inline bool is_not_chunked() const { return decode_not_chunked(_obj); }
  inline bool is_weak()        const { return decode_weak(_obj);        }
  inline bool count_liveness() const { return decode_cnt_live(_obj);    }

  DEBUG_ONLY(bool is_valid() const;) // Tasks to be pushed/popped must be valid.

  static uintptr_t max_addressable() {
    return nth_bit(oop_bits);
  }

  static int chunk_size() {
    return nth_bit(chunk_bits);
  }
};
#else
class ShenandoahMarkTask
{
private:
  static const uint8_t chunk_bits  = 10;
  static const uint8_t pow_bits    = 5;

  static const int chunk_max       = nth_bit(chunk_bits) - 1;
  static const int pow_max         = nth_bit(pow_bits) - 1;

  oop _obj;
  bool _skip_live;
  bool _weak;
  int _chunk;
  int _pow;

public:
  ShenandoahMarkTask(oop o = NULL, bool skip_live = false, bool weak = false, int chunk = 0, int pow = 0):
    _obj(o), _skip_live(skip_live), _weak(weak), _chunk(chunk), _pow(pow) {
    assert(0 <= chunk && chunk <= chunk_max, "chunk is in range: %d", chunk);
    assert(0 <= pow && pow <= pow_max, "pow is in range: %d", pow);
  }

  // Trivially copyable.

  inline oop obj()             const { return _obj; }
  inline int chunk()           const { return _chunk; }
  inline int pow()             const { return _pow; }
  inline bool is_not_chunked() const { return _chunk == 0; }
  inline bool is_weak()        const { return _weak; }
  inline bool count_liveness() const { return !_skip_live; }

  DEBUG_ONLY(bool is_valid() const;) // Tasks to be pushed/popped must be valid.

  static size_t max_addressable() {
    return sizeof(oop);
  }

  static int chunk_size() {
    return nth_bit(chunk_bits);
  }
};
#endif // SHENANDOAH_OPTIMIZED_MARKTASK

#ifdef _MSC_VER
#pragma warning(pop)
#endif

typedef BufferedOverflowTaskQueue<ShenandoahMarkTask, mtGC> ShenandoahBufferedOverflowTaskQueue;
typedef Padded<ShenandoahBufferedOverflowTaskQueue> ShenandoahObjToScanQueue;

template <class T, MEMFLAGS F>
class ParallelClaimableQueueSet: public GenericTaskQueueSet<T, F> {
private:
  shenandoah_padding(0);
  volatile jint     _claimed_index;
  shenandoah_padding(1);

  debug_only(uint   _reserved;  )

public:
  using GenericTaskQueueSet<T, F>::size;

public:
  ParallelClaimableQueueSet(int n) : GenericTaskQueueSet<T, F>(n), _claimed_index(0) {
    debug_only(_reserved = 0; )
  }

  void clear_claimed() { _claimed_index = 0; }
  T*   claim_next();

  // reserve queues that not for parallel claiming
  void reserve(uint n) {
    assert(n <= size(), "Sanity");
    _claimed_index = (jint)n;
    debug_only(_reserved = n;)
  }

  debug_only(uint get_reserved() const { return (uint)_reserved; })
};

template <class T, MEMFLAGS F>
T* ParallelClaimableQueueSet<T, F>::claim_next() {
  jint size = (jint)GenericTaskQueueSet<T, F>::size();

  if (_claimed_index >= size) {
    return NULL;
  }

  jint index = Atomic::add(&_claimed_index, 1);

  if (index <= size) {
    return GenericTaskQueueSet<T, F>::queue((uint)index - 1);
  } else {
    return NULL;
  }
}

class ShenandoahObjToScanQueueSet: public ParallelClaimableQueueSet<ShenandoahObjToScanQueue, mtGC> {
public:
  ShenandoahObjToScanQueueSet(int n) : ParallelClaimableQueueSet<ShenandoahObjToScanQueue, mtGC>(n) {}

  bool is_empty();
  void clear();

#if TASKQUEUE_STATS
  static void print_taskqueue_stats_hdr(outputStream* const st);
  void print_taskqueue_stats() const;
  void reset_taskqueue_stats();
#endif // TASKQUEUE_STATS
};

class ShenandoahTerminatorTerminator : public TerminatorTerminator {
private:
  ShenandoahHeap* _heap;
public:
  ShenandoahTerminatorTerminator(ShenandoahHeap* const heap) : _heap(heap) { }
  virtual bool should_exit_termination();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHTASKQUEUE_HPP
