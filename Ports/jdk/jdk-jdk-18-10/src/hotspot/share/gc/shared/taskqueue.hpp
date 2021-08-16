/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHARED_TASKQUEUE_HPP
#define SHARE_GC_SHARED_TASKQUEUE_HPP

#include "memory/allocation.hpp"
#include "memory/padded.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/atomic.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/ostream.hpp"
#include "utilities/stack.hpp"

// Simple TaskQueue stats that are collected by default in debug builds.

#if !defined(TASKQUEUE_STATS) && defined(ASSERT)
#define TASKQUEUE_STATS 1
#elif !defined(TASKQUEUE_STATS)
#define TASKQUEUE_STATS 0
#endif

#if TASKQUEUE_STATS
#define TASKQUEUE_STATS_ONLY(code) code
#else
#define TASKQUEUE_STATS_ONLY(code)
#endif // TASKQUEUE_STATS

#if TASKQUEUE_STATS
class TaskQueueStats {
public:
  enum StatId {
    push,             // number of taskqueue pushes
    pop,              // number of taskqueue pops
    pop_slow,         // subset of taskqueue pops that were done slow-path
    steal_attempt,    // number of taskqueue steal attempts
    steal,            // number of taskqueue steals
    overflow,         // number of overflow pushes
    overflow_max_len, // max length of overflow stack
    last_stat_id
  };

public:
  inline TaskQueueStats()       { reset(); }

  inline void record_push()          { ++_stats[push]; }
  inline void record_pop()           { ++_stats[pop]; }
  inline void record_pop_slow()      { record_pop(); ++_stats[pop_slow]; }
  inline void record_steal_attempt() { ++_stats[steal_attempt]; }
  inline void record_steal()         { ++_stats[steal]; }
  inline void record_overflow(size_t new_length);

  TaskQueueStats & operator +=(const TaskQueueStats & addend);

  inline size_t get(StatId id) const { return _stats[id]; }
  inline const size_t* get() const   { return _stats; }

  inline void reset();

  // Print the specified line of the header (does not include a line separator).
  static void print_header(unsigned int line, outputStream* const stream = tty,
                           unsigned int width = 10);
  // Print the statistics (does not include a line separator).
  void print(outputStream* const stream = tty, unsigned int width = 10) const;

  DEBUG_ONLY(void verify() const;)

private:
  size_t                    _stats[last_stat_id];
  static const char * const _names[last_stat_id];
};

void TaskQueueStats::record_overflow(size_t new_len) {
  ++_stats[overflow];
  if (new_len > _stats[overflow_max_len]) _stats[overflow_max_len] = new_len;
}

void TaskQueueStats::reset() {
  memset(_stats, 0, sizeof(_stats));
}
#endif // TASKQUEUE_STATS

// TaskQueueSuper collects functionality common to all GenericTaskQueue instances.

template <unsigned int N, MEMFLAGS F>
class TaskQueueSuper: public CHeapObj<F> {
protected:
  // Internal type for indexing the queue; also used for the tag.
  typedef NOT_LP64(uint16_t) LP64_ONLY(uint32_t) idx_t;
  STATIC_ASSERT(N == idx_t(N)); // Ensure N fits in an idx_t.

  // N must be a power of 2 for computing modulo via masking.
  // N must be >= 2 for the algorithm to work at all, though larger is better.
  STATIC_ASSERT(N >= 2);
  STATIC_ASSERT(is_power_of_2(N));
  static const uint MOD_N_MASK = N - 1;

  class Age {
    friend class TaskQueueSuper;

  public:
    explicit Age(size_t data = 0) : _data(data) {}
    Age(idx_t top, idx_t tag) { _fields._top = top; _fields._tag = tag; }

    idx_t top() const { return _fields._top; }
    idx_t tag() const { return _fields._tag; }

    bool operator ==(const Age& other) const { return _data == other._data; }

  private:
    struct fields {
      idx_t _top;
      idx_t _tag;
    };
    union {
      size_t _data;
      fields _fields;
    };
    STATIC_ASSERT(sizeof(size_t) >= sizeof(fields));
  };

  uint bottom_relaxed() const {
    return Atomic::load(&_bottom);
  }

  uint bottom_acquire() const {
    return Atomic::load_acquire(&_bottom);
  }

  void set_bottom_relaxed(uint new_bottom) {
    Atomic::store(&_bottom, new_bottom);
  }

  void release_set_bottom(uint new_bottom) {
    Atomic::release_store(&_bottom, new_bottom);
  }

  Age age_relaxed() const {
    return Age(Atomic::load(&_age._data));
  }

  void set_age_relaxed(Age new_age) {
    Atomic::store(&_age._data, new_age._data);
  }

  Age cmpxchg_age(Age old_age, Age new_age) {
    return Age(Atomic::cmpxchg(&_age._data, old_age._data, new_age._data));
  }

  idx_t age_top_relaxed() const {
    // Atomically accessing a subfield of an "atomic" member.
    return Atomic::load(&_age._fields._top);
  }

  // These both operate mod N.
  static uint increment_index(uint ind) {
    return (ind + 1) & MOD_N_MASK;
  }
  static uint decrement_index(uint ind) {
    return (ind - 1) & MOD_N_MASK;
  }

  // Returns a number in the range [0..N).  If the result is "N-1", it should be
  // interpreted as 0.
  uint dirty_size(uint bot, uint top) const {
    return (bot - top) & MOD_N_MASK;
  }

  // Returns the size corresponding to the given "bot" and "top".
  uint clean_size(uint bot, uint top) const {
    uint sz = dirty_size(bot, top);
    // Has the queue "wrapped", so that bottom is less than top?  There's a
    // complicated special case here.  A pair of threads could perform pop_local
    // and pop_global operations concurrently, starting from a state in which
    // _bottom == _top+1.  The pop_local could succeed in decrementing _bottom,
    // and the pop_global in incrementing _top (in which case the pop_global
    // will be awarded the contested queue element.)  The resulting state must
    // be interpreted as an empty queue.  (We only need to worry about one such
    // event: only the queue owner performs pop_local's, and several concurrent
    // threads attempting to perform the pop_global will all perform the same
    // CAS, and only one can succeed.)  Any stealing thread that reads after
    // either the increment or decrement will see an empty queue, and will not
    // join the competitors.  The "sz == -1" / "sz == N-1" state will not be
    // modified by concurrent threads, so the owner thread can reset the state
    // to _bottom == top so subsequent pushes will be performed normally.
    return (sz == N - 1) ? 0 : sz;
  }

  // Assert that we're not in the underflow state where bottom has
  // been decremented past top, so that _bottom+1 mod N == top.  See
  // the discussion in clean_size.

  void assert_not_underflow(uint bot, uint top) const {
    assert_not_underflow(dirty_size(bot, top));
  }

  void assert_not_underflow(uint dirty_size) const {
    assert(dirty_size != N - 1, "invariant");
  }

private:
  DEFINE_PAD_MINUS_SIZE(0, DEFAULT_CACHE_LINE_SIZE, 0);

  // Index of the first free element after the last one pushed (mod N).
  volatile uint _bottom;
  DEFINE_PAD_MINUS_SIZE(1, DEFAULT_CACHE_LINE_SIZE, sizeof(uint));

  // top() is the index of the oldest pushed element (mod N), and tag()
  // is the associated epoch, to distinguish different modifications of
  // the age.  There is no available element if top() == _bottom or
  // (_bottom - top()) mod N == N-1; the latter indicates underflow
  // during concurrent pop_local/pop_global.
  volatile Age _age;
  DEFINE_PAD_MINUS_SIZE(2, DEFAULT_CACHE_LINE_SIZE, sizeof(Age));

  NONCOPYABLE(TaskQueueSuper);

public:
  TaskQueueSuper() : _bottom(0), _age() {}

  // Assert the queue is empty.
  // Unreliable if there are concurrent pushes or pops.
  void assert_empty() const {
    assert(bottom_relaxed() == age_top_relaxed(), "not empty");
  }

  bool is_empty() const {
    return size() == 0;
  }

  // Return an estimate of the number of elements in the queue.
  // Treats pop_local/pop_global race that underflows as empty.
  uint size() const {
    return clean_size(bottom_relaxed(), age_top_relaxed());
  }

  // Discard the contents of the queue.
  void set_empty() {
    set_bottom_relaxed(0);
    set_age_relaxed(Age());
  }

  // Maximum number of elements allowed in the queue.  This is two less
  // than the actual queue size, so that a full queue can be distinguished
  // from underflow involving pop_local and concurrent pop_global operations
  // in GenericTaskQueue.
  uint max_elems() const { return N - 2; }

  TASKQUEUE_STATS_ONLY(TaskQueueStats stats;)
};

//
// GenericTaskQueue implements an ABP, Aurora-Blumofe-Plaxton, double-
// ended-queue (deque), intended for use in work stealing. Queue operations
// are non-blocking.
//
// A queue owner thread performs push() and pop_local() operations on one end
// of the queue, while other threads may steal work using the pop_global()
// method.
//
// The main difference to the original algorithm is that this
// implementation allows wrap-around at the end of its allocated
// storage, which is an array.
//
// The original paper is:
//
// Arora, N. S., Blumofe, R. D., and Plaxton, C. G.
// Thread scheduling for multiprogrammed multiprocessors.
// Theory of Computing Systems 34, 2 (2001), 115-144.
//
// The following paper provides an correctness proof and an
// implementation for weakly ordered memory models including (pseudo-)
// code containing memory barriers for a Chase-Lev deque. Chase-Lev is
// similar to ABP, with the main difference that it allows resizing of the
// underlying storage:
//
// Le, N. M., Pop, A., Cohen A., and Nardell, F. Z.
// Correct and efficient work-stealing for weak memory models
// Proceedings of the 18th ACM SIGPLAN symposium on Principles and
// practice of parallel programming (PPoPP 2013), 69-80
//

template <class E, MEMFLAGS F, unsigned int N = TASKQUEUE_SIZE>
class GenericTaskQueue: public TaskQueueSuper<N, F> {
protected:
  typedef typename TaskQueueSuper<N, F>::Age Age;
  typedef typename TaskQueueSuper<N, F>::idx_t idx_t;

  using TaskQueueSuper<N, F>::MOD_N_MASK;

  using TaskQueueSuper<N, F>::bottom_relaxed;
  using TaskQueueSuper<N, F>::bottom_acquire;

  using TaskQueueSuper<N, F>::set_bottom_relaxed;
  using TaskQueueSuper<N, F>::release_set_bottom;

  using TaskQueueSuper<N, F>::age_relaxed;
  using TaskQueueSuper<N, F>::set_age_relaxed;
  using TaskQueueSuper<N, F>::cmpxchg_age;
  using TaskQueueSuper<N, F>::age_top_relaxed;

  using TaskQueueSuper<N, F>::increment_index;
  using TaskQueueSuper<N, F>::decrement_index;
  using TaskQueueSuper<N, F>::dirty_size;
  using TaskQueueSuper<N, F>::clean_size;
  using TaskQueueSuper<N, F>::assert_not_underflow;

public:
  using TaskQueueSuper<N, F>::max_elems;
  using TaskQueueSuper<N, F>::size;

#if  TASKQUEUE_STATS
  using TaskQueueSuper<N, F>::stats;
#endif

private:
  // Slow path for pop_local, dealing with possible conflict with pop_global.
  bool pop_local_slow(uint localBot, Age oldAge);

public:
  typedef E element_type;

  // Initializes the queue to empty.
  GenericTaskQueue();

  void initialize();

  // Push the task "t" on the queue.  Returns "false" iff the queue is full.
  inline bool push(E t);

  // Attempts to claim a task from the "local" end of the queue (the most
  // recently pushed) as long as the number of entries exceeds the threshold.
  // If successfully claims a task, returns true and sets t to the task;
  // otherwise, returns false and t is unspecified.  May fail and return
  // false because of a successful steal by pop_global.
  inline bool pop_local(E& t, uint threshold = 0);

  // Like pop_local(), but uses the "global" end of the queue (the least
  // recently pushed).
  bool pop_global(E& t);

  // Delete any resource associated with the queue.
  ~GenericTaskQueue();

  // Apply fn to each element in the task queue.  The queue must not
  // be modified while iterating.
  template<typename Fn> void iterate(Fn fn);

private:
  // Base class has trailing padding.

  // Element array.
  E* _elems;

  DEFINE_PAD_MINUS_SIZE(1, DEFAULT_CACHE_LINE_SIZE, sizeof(E*));
  // Queue owner local variables. Not to be accessed by other threads.

  static const uint InvalidQueueId = uint(-1);
  uint _last_stolen_queue_id; // The id of the queue we last stole from

  int _seed; // Current random seed used for selecting a random queue during stealing.

  DEFINE_PAD_MINUS_SIZE(2, DEFAULT_CACHE_LINE_SIZE, sizeof(uint) + sizeof(int));
public:
  int next_random_queue_id();

  void set_last_stolen_queue_id(uint id)     { _last_stolen_queue_id = id; }
  uint last_stolen_queue_id() const          { return _last_stolen_queue_id; }
  bool is_last_stolen_queue_id_valid() const { return _last_stolen_queue_id != InvalidQueueId; }
  void invalidate_last_stolen_queue_id()     { _last_stolen_queue_id = InvalidQueueId; }
};

template<class E, MEMFLAGS F, unsigned int N>
GenericTaskQueue<E, F, N>::GenericTaskQueue() : _last_stolen_queue_id(InvalidQueueId), _seed(17 /* random number */) {
  assert(sizeof(Age) == sizeof(size_t), "Depends on this.");
}

// OverflowTaskQueue is a TaskQueue that also includes an overflow stack for
// elements that do not fit in the TaskQueue.
//
// This class hides two methods from super classes:
//
// push() - push onto the task queue or, if that fails, onto the overflow stack
// is_empty() - return true if both the TaskQueue and overflow stack are empty
//
// Note that size() is not hidden--it returns the number of elements in the
// TaskQueue, and does not include the size of the overflow stack.  This
// simplifies replacement of GenericTaskQueues with OverflowTaskQueues.
template<class E, MEMFLAGS F, unsigned int N = TASKQUEUE_SIZE>
class OverflowTaskQueue: public GenericTaskQueue<E, F, N>
{
public:
  typedef Stack<E, F>               overflow_t;
  typedef GenericTaskQueue<E, F, N> taskqueue_t;

  TASKQUEUE_STATS_ONLY(using taskqueue_t::stats;)

  // Push task t onto the queue or onto the overflow stack.  Return true.
  inline bool push(E t);
  // Try to push task t onto the queue only. Returns true if successful, false otherwise.
  inline bool try_push_to_taskqueue(E t);

  // Attempt to pop from the overflow stack; return true if anything was popped.
  inline bool pop_overflow(E& t);

  inline overflow_t* overflow_stack() { return &_overflow_stack; }

  inline bool taskqueue_empty() const { return taskqueue_t::is_empty(); }
  inline bool overflow_empty()  const { return _overflow_stack.is_empty(); }
  inline bool is_empty()        const {
    return taskqueue_empty() && overflow_empty();
  }

private:
  overflow_t _overflow_stack;
};

class TaskQueueSetSuper {
public:
  // Assert all queues in the set are empty.
  NOT_DEBUG(void assert_empty() const {})
  DEBUG_ONLY(virtual void assert_empty() const = 0;)

  // Tasks in queue
  virtual uint tasks() const = 0;
};

template <MEMFLAGS F> class TaskQueueSetSuperImpl: public CHeapObj<F>, public TaskQueueSetSuper {
};

template<class T, MEMFLAGS F>
class GenericTaskQueueSet: public TaskQueueSetSuperImpl<F> {
public:
  typedef typename T::element_type E;

private:
  uint _n;
  T** _queues;

  bool steal_best_of_2(uint queue_num, E& t);

public:
  GenericTaskQueueSet(uint n);
  ~GenericTaskQueueSet();

  // Set the i'th queue to the provided queue.
  // Does not transfer ownership of the queue to this queue set.
  void register_queue(uint i, T* q);

  T* queue(uint n);

  // Try to steal a task from some other queue than queue_num. It may perform several attempts at doing so.
  // Returns if stealing succeeds, and sets "t" to the stolen task.
  bool steal(uint queue_num, E& t);

  DEBUG_ONLY(virtual void assert_empty() const;)

  virtual uint tasks() const;

  uint size() const { return _n; }
};

template<class T, MEMFLAGS F> void
GenericTaskQueueSet<T, F>::register_queue(uint i, T* q) {
  assert(i < _n, "index out of range.");
  _queues[i] = q;
}

template<class T, MEMFLAGS F> T*
GenericTaskQueueSet<T, F>::queue(uint i) {
  return _queues[i];
}

#ifdef ASSERT
template<class T, MEMFLAGS F>
void GenericTaskQueueSet<T, F>::assert_empty() const {
  for (uint j = 0; j < _n; j++) {
    _queues[j]->assert_empty();
  }
}
#endif // ASSERT

template<class T, MEMFLAGS F>
uint GenericTaskQueueSet<T, F>::tasks() const {
  uint n = 0;
  for (uint j = 0; j < _n; j++) {
    n += _queues[j]->size();
  }
  return n;
}

// When to terminate from the termination protocol.
class TerminatorTerminator: public CHeapObj<mtInternal> {
public:
  virtual bool should_exit_termination() = 0;
};

class ObjArrayTask
{
public:
  ObjArrayTask(oop o = NULL, int idx = 0): _obj(o), _index(idx) { }
  ObjArrayTask(oop o, size_t idx): _obj(o), _index(int(idx)) {
    assert(idx <= size_t(max_jint), "too big");
  }
  // Trivially copyable, for use in GenericTaskQueue.

  inline oop obj()   const { return _obj; }
  inline int index() const { return _index; }

  DEBUG_ONLY(bool is_valid() const); // Tasks to be pushed/popped must be valid.

private:
  oop _obj;
  int _index;
};

// Wrapper over an oop that is a partially scanned array.
// Can be converted to a ScannerTask for placement in associated task queues.
// Refers to the partially copied source array oop.
class PartialArrayScanTask {
  oop _src;

public:
  PartialArrayScanTask() : _src() {}
  explicit PartialArrayScanTask(oop src_array) : _src(src_array) {}
  // Trivially copyable.

  oop to_source_array() const { return _src; }
};

// Discriminated union over oop*, narrowOop*, and PartialArrayScanTask.
// Uses a low tag in the associated pointer to identify the category.
// Used as a task queue element type.
class ScannerTask {
  void* _p;

  static const uintptr_t OopTag = 0;
  static const uintptr_t NarrowOopTag = 1;
  static const uintptr_t PartialArrayTag = 2;
  static const uintptr_t TagSize = 2;
  static const uintptr_t TagAlignment = 1 << TagSize;
  static const uintptr_t TagMask = TagAlignment - 1;

  static void* encode(void* p, uintptr_t tag) {
    assert(is_aligned(p, TagAlignment), "misaligned: " PTR_FORMAT, p2i(p));
    return static_cast<char*>(p) + tag;
  }

  uintptr_t raw_value() const {
    return reinterpret_cast<uintptr_t>(_p);
  }

  bool has_tag(uintptr_t tag) const {
    return (raw_value() & TagMask) == tag;
  }

  void* decode(uintptr_t tag) const {
    assert(has_tag(tag), "precondition");
    return static_cast<char*>(_p) - tag;
  }

public:
  ScannerTask() : _p(NULL) {}

  explicit ScannerTask(oop* p) : _p(encode(p, OopTag)) {}

  explicit ScannerTask(narrowOop* p) : _p(encode(p, NarrowOopTag)) {}

  explicit ScannerTask(PartialArrayScanTask t) :
    _p(encode(t.to_source_array(), PartialArrayTag)) {}

  // Trivially copyable.

  // Predicate implementations assume OopTag == 0, others are powers of 2.

  bool is_oop_ptr() const {
    return (raw_value() & (NarrowOopTag | PartialArrayTag)) == 0;
  }

  bool is_narrow_oop_ptr() const {
    return (raw_value() & NarrowOopTag) != 0;
  }

  bool is_partial_array_task() const {
    return (raw_value() & PartialArrayTag) != 0;
  }

  oop* to_oop_ptr() const {
    return static_cast<oop*>(decode(OopTag));
  }

  narrowOop* to_narrow_oop_ptr() const {
    return static_cast<narrowOop*>(decode(NarrowOopTag));
  }

  PartialArrayScanTask to_partial_array_task() const {
    return PartialArrayScanTask(cast_to_oop(decode(PartialArrayTag)));
  }
};

#endif // SHARE_GC_SHARED_TASKQUEUE_HPP
