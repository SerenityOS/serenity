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

#include "precompiled.hpp"
#include "gc/g1/g1BarrierSet.inline.hpp"
#include "gc/g1/g1BufferNodeList.hpp"
#include "gc/g1/g1CardTableEntryClosure.hpp"
#include "gc/g1/g1CollectedHeap.inline.hpp"
#include "gc/g1/g1ConcurrentRefineStats.hpp"
#include "gc/g1/g1ConcurrentRefineThread.hpp"
#include "gc/g1/g1DirtyCardQueue.hpp"
#include "gc/g1/g1FreeIdSet.hpp"
#include "gc/g1/g1RedirtyCardsQueue.hpp"
#include "gc/g1/g1RemSet.hpp"
#include "gc/g1/g1ThreadLocalData.hpp"
#include "gc/g1/heapRegionRemSet.inline.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "memory/iterator.hpp"
#include "runtime/atomic.hpp"
#include "runtime/mutex.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/os.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/threadSMR.hpp"
#include "utilities/globalCounter.inline.hpp"
#include "utilities/macros.hpp"
#include "utilities/nonblockingQueue.inline.hpp"
#include "utilities/pair.hpp"
#include "utilities/quickSort.hpp"
#include "utilities/ticks.hpp"

G1DirtyCardQueue::G1DirtyCardQueue(G1DirtyCardQueueSet* qset) :
  PtrQueue(qset),
  _refinement_stats(new G1ConcurrentRefineStats())
{ }

G1DirtyCardQueue::~G1DirtyCardQueue() {
  G1BarrierSet::dirty_card_queue_set().flush_queue(*this);
  delete _refinement_stats;
}

// Assumed to be zero by concurrent threads.
static uint par_ids_start() { return 0; }

G1DirtyCardQueueSet::G1DirtyCardQueueSet(BufferNode::Allocator* allocator) :
  PtrQueueSet(allocator),
  _primary_refinement_thread(NULL),
  _num_cards(0),
  _completed(),
  _paused(),
  _free_ids(par_ids_start(), num_par_ids()),
  _process_cards_threshold(ProcessCardsThresholdNever),
  _max_cards(MaxCardsUnlimited),
  _padded_max_cards(MaxCardsUnlimited),
  _detached_refinement_stats()
{}

G1DirtyCardQueueSet::~G1DirtyCardQueueSet() {
  abandon_completed_buffers();
}

// Determines how many mutator threads can process the buffers in parallel.
uint G1DirtyCardQueueSet::num_par_ids() {
  return (uint)os::initial_active_processor_count();
}

void G1DirtyCardQueueSet::flush_queue(G1DirtyCardQueue& queue) {
  if (queue.buffer() != nullptr) {
    G1ConcurrentRefineStats* stats = queue.refinement_stats();
    stats->inc_dirtied_cards(buffer_size() - queue.index());
  }
  PtrQueueSet::flush_queue(queue);
}

void G1DirtyCardQueueSet::enqueue(G1DirtyCardQueue& queue,
                                  volatile CardValue* card_ptr) {
  CardValue* value = const_cast<CardValue*>(card_ptr);
  if (!try_enqueue(queue, value)) {
    handle_zero_index(queue);
    retry_enqueue(queue, value);
  }
}

void G1DirtyCardQueueSet::handle_zero_index(G1DirtyCardQueue& queue) {
  assert(queue.index() == 0, "precondition");
  BufferNode* old_node = exchange_buffer_with_new(queue);
  if (old_node != nullptr) {
    G1ConcurrentRefineStats* stats = queue.refinement_stats();
    stats->inc_dirtied_cards(buffer_size());
    handle_completed_buffer(old_node, stats);
  }
}

void G1DirtyCardQueueSet::handle_zero_index_for_thread(Thread* t) {
  G1DirtyCardQueue& queue = G1ThreadLocalData::dirty_card_queue(t);
  G1BarrierSet::dirty_card_queue_set().handle_zero_index(queue);
}

void G1DirtyCardQueueSet::enqueue_completed_buffer(BufferNode* cbn) {
  assert(cbn != NULL, "precondition");
  // Increment _num_cards before adding to queue, so queue removal doesn't
  // need to deal with _num_cards possibly going negative.
  size_t new_num_cards = Atomic::add(&_num_cards, buffer_size() - cbn->index());
  _completed.push(*cbn);
  if ((new_num_cards > process_cards_threshold()) &&
      (_primary_refinement_thread != NULL)) {
    _primary_refinement_thread->activate();
  }
}

// Thread-safe attempt to remove and return the first buffer from
// the _completed queue, using the NonblockingQueue::try_pop() underneath.
// It has a limitation that it may return NULL when there are objects
// in the queue if there is a concurrent push/append operation.
BufferNode* G1DirtyCardQueueSet::dequeue_completed_buffer() {
  Thread* current_thread = Thread::current();
  BufferNode* result = NULL;
  while (true) {
    // Use GlobalCounter critical section to avoid ABA problem.
    // The release of a buffer to its allocator's free list uses
    // GlobalCounter::write_synchronize() to coordinate with this
    // dequeuing operation.
    // We use a CS per iteration, rather than over the whole loop,
    // because we're not guaranteed to make progress. Lingering in
    // one CS could defer releasing buffer to the free list for reuse,
    // leading to excessive allocations.
    GlobalCounter::CriticalSection cs(current_thread);
    if (_completed.try_pop(&result)) return result;
  }
}

BufferNode* G1DirtyCardQueueSet::get_completed_buffer() {
  BufferNode* result = dequeue_completed_buffer();
  if (result == NULL) {         // Unlikely if no paused buffers.
    enqueue_previous_paused_buffers();
    result = dequeue_completed_buffer();
    if (result == NULL) return NULL;
  }
  Atomic::sub(&_num_cards, buffer_size() - result->index());
  return result;
}

#ifdef ASSERT
void G1DirtyCardQueueSet::verify_num_cards() const {
  size_t actual = 0;
  for (BufferNode* cur = _completed.first();
       !_completed.is_end(cur);
       cur = cur->next()) {
    actual += buffer_size() - cur->index();
  }
  assert(actual == Atomic::load(&_num_cards),
         "Num entries in completed buffers should be " SIZE_FORMAT " but are " SIZE_FORMAT,
         Atomic::load(&_num_cards), actual);
}
#endif // ASSERT

G1DirtyCardQueueSet::PausedBuffers::PausedList::PausedList() :
  _head(NULL), _tail(NULL),
  _safepoint_id(SafepointSynchronize::safepoint_id())
{}

#ifdef ASSERT
G1DirtyCardQueueSet::PausedBuffers::PausedList::~PausedList() {
  assert(Atomic::load(&_head) == NULL, "precondition");
  assert(_tail == NULL, "precondition");
}
#endif // ASSERT

bool G1DirtyCardQueueSet::PausedBuffers::PausedList::is_next() const {
  assert_not_at_safepoint();
  return _safepoint_id == SafepointSynchronize::safepoint_id();
}

void G1DirtyCardQueueSet::PausedBuffers::PausedList::add(BufferNode* node) {
  assert_not_at_safepoint();
  assert(is_next(), "precondition");
  BufferNode* old_head = Atomic::xchg(&_head, node);
  if (old_head == NULL) {
    assert(_tail == NULL, "invariant");
    _tail = node;
  } else {
    node->set_next(old_head);
  }
}

G1DirtyCardQueueSet::HeadTail G1DirtyCardQueueSet::PausedBuffers::PausedList::take() {
  BufferNode* head = Atomic::load(&_head);
  BufferNode* tail = _tail;
  Atomic::store(&_head, (BufferNode*)NULL);
  _tail = NULL;
  return HeadTail(head, tail);
}

G1DirtyCardQueueSet::PausedBuffers::PausedBuffers() : _plist(NULL) {}

#ifdef ASSERT
G1DirtyCardQueueSet::PausedBuffers::~PausedBuffers() {
  assert(Atomic::load(&_plist) == NULL, "invariant");
}
#endif // ASSERT

void G1DirtyCardQueueSet::PausedBuffers::add(BufferNode* node) {
  assert_not_at_safepoint();
  PausedList* plist = Atomic::load_acquire(&_plist);
  if (plist == NULL) {
    // Try to install a new next list.
    plist = new PausedList();
    PausedList* old_plist = Atomic::cmpxchg(&_plist, (PausedList*)NULL, plist);
    if (old_plist != NULL) {
      // Some other thread installed a new next list.  Use it instead.
      delete plist;
      plist = old_plist;
    }
  }
  assert(plist->is_next(), "invariant");
  plist->add(node);
}

G1DirtyCardQueueSet::HeadTail G1DirtyCardQueueSet::PausedBuffers::take_previous() {
  assert_not_at_safepoint();
  PausedList* previous;
  {
    // Deal with plist in a critical section, to prevent it from being
    // deleted out from under us by a concurrent take_previous().
    GlobalCounter::CriticalSection cs(Thread::current());
    previous = Atomic::load_acquire(&_plist);
    if ((previous == NULL) ||   // Nothing to take.
        previous->is_next() ||  // Not from a previous safepoint.
        // Some other thread stole it.
        (Atomic::cmpxchg(&_plist, previous, (PausedList*)NULL) != previous)) {
      return HeadTail();
    }
  }
  // We now own previous.
  HeadTail result = previous->take();
  // There might be other threads examining previous (in concurrent
  // take_previous()).  Synchronize to wait until any such threads are
  // done with such examination before deleting.
  GlobalCounter::write_synchronize();
  delete previous;
  return result;
}

G1DirtyCardQueueSet::HeadTail G1DirtyCardQueueSet::PausedBuffers::take_all() {
  assert_at_safepoint();
  HeadTail result;
  PausedList* plist = Atomic::load(&_plist);
  if (plist != NULL) {
    Atomic::store(&_plist, (PausedList*)NULL);
    result = plist->take();
    delete plist;
  }
  return result;
}

void G1DirtyCardQueueSet::record_paused_buffer(BufferNode* node) {
  assert_not_at_safepoint();
  assert(node->next() == NULL, "precondition");
  // Ensure there aren't any paused buffers from a previous safepoint.
  enqueue_previous_paused_buffers();
  // Cards for paused buffers are included in count, to contribute to
  // notification checking after the coming safepoint if it doesn't GC.
  // Note that this means the queue's _num_cards differs from the number
  // of cards in the queued buffers when there are paused buffers.
  Atomic::add(&_num_cards, buffer_size() - node->index());
  _paused.add(node);
}

void G1DirtyCardQueueSet::enqueue_paused_buffers_aux(const HeadTail& paused) {
  if (paused._head != NULL) {
    assert(paused._tail != NULL, "invariant");
    // Cards from paused buffers are already recorded in the queue count.
    _completed.append(*paused._head, *paused._tail);
  }
}

void G1DirtyCardQueueSet::enqueue_previous_paused_buffers() {
  assert_not_at_safepoint();
  enqueue_paused_buffers_aux(_paused.take_previous());
}

void G1DirtyCardQueueSet::enqueue_all_paused_buffers() {
  assert_at_safepoint();
  enqueue_paused_buffers_aux(_paused.take_all());
}

void G1DirtyCardQueueSet::abandon_completed_buffers() {
  enqueue_all_paused_buffers();
  verify_num_cards();
  G1BufferNodeList list = take_all_completed_buffers();
  BufferNode* buffers_to_delete = list._head;
  while (buffers_to_delete != NULL) {
    BufferNode* bn = buffers_to_delete;
    buffers_to_delete = bn->next();
    bn->set_next(NULL);
    deallocate_buffer(bn);
  }
}

void G1DirtyCardQueueSet::notify_if_necessary() {
  if ((_primary_refinement_thread != NULL) &&
      (num_cards() > process_cards_threshold())) {
    _primary_refinement_thread->activate();
  }
}

// Merge lists of buffers. The source queue set is emptied as a
// result. The queue sets must share the same allocator.
void G1DirtyCardQueueSet::merge_bufferlists(G1RedirtyCardsQueueSet* src) {
  assert(allocator() == src->allocator(), "precondition");
  const G1BufferNodeList from = src->take_all_completed_buffers();
  if (from._head != NULL) {
    Atomic::add(&_num_cards, from._entry_count);
    _completed.append(*from._head, *from._tail);
  }
}

G1BufferNodeList G1DirtyCardQueueSet::take_all_completed_buffers() {
  enqueue_all_paused_buffers();
  verify_num_cards();
  Pair<BufferNode*, BufferNode*> pair = _completed.take_all();
  size_t num_cards = Atomic::load(&_num_cards);
  Atomic::store(&_num_cards, size_t(0));
  return G1BufferNodeList(pair.first, pair.second, num_cards);
}

class G1RefineBufferedCards : public StackObj {
  BufferNode* const _node;
  CardTable::CardValue** const _node_buffer;
  const size_t _node_buffer_size;
  const uint _worker_id;
  G1ConcurrentRefineStats* _stats;
  G1RemSet* const _g1rs;

  static inline int compare_card(const CardTable::CardValue* p1,
                                 const CardTable::CardValue* p2) {
    return p2 - p1;
  }

  // Sorts the cards from start_index to _node_buffer_size in *decreasing*
  // address order. Tests showed that this order is preferable to not sorting
  // or increasing address order.
  void sort_cards(size_t start_index) {
    QuickSort::sort(&_node_buffer[start_index],
                    _node_buffer_size - start_index,
                    compare_card,
                    false);
  }

  // Returns the index to the first clean card in the buffer.
  size_t clean_cards() {
    const size_t start = _node->index();
    assert(start <= _node_buffer_size, "invariant");

    // Two-fingered compaction algorithm similar to the filtering mechanism in
    // SATBMarkQueue. The main difference is that clean_card_before_refine()
    // could change the buffer element in-place.
    // We don't check for SuspendibleThreadSet::should_yield(), because
    // cleaning and redirtying the cards is fast.
    CardTable::CardValue** src = &_node_buffer[start];
    CardTable::CardValue** dst = &_node_buffer[_node_buffer_size];
    assert(src <= dst, "invariant");
    for ( ; src < dst; ++src) {
      // Search low to high for a card to keep.
      if (_g1rs->clean_card_before_refine(src)) {
        // Found keeper.  Search high to low for a card to discard.
        while (src < --dst) {
          if (!_g1rs->clean_card_before_refine(dst)) {
            *dst = *src;         // Replace discard with keeper.
            break;
          }
        }
        // If discard search failed (src == dst), the outer loop will also end.
      }
    }

    // dst points to the first retained clean card, or the end of the buffer
    // if all the cards were discarded.
    const size_t first_clean = dst - _node_buffer;
    assert(first_clean >= start && first_clean <= _node_buffer_size, "invariant");
    // Discarded cards are considered as refined.
    _stats->inc_refined_cards(first_clean - start);
    _stats->inc_precleaned_cards(first_clean - start);
    return first_clean;
  }

  bool refine_cleaned_cards(size_t start_index) {
    bool result = true;
    size_t i = start_index;
    for ( ; i < _node_buffer_size; ++i) {
      if (SuspendibleThreadSet::should_yield()) {
        redirty_unrefined_cards(i);
        result = false;
        break;
      }
      _g1rs->refine_card_concurrently(_node_buffer[i], _worker_id);
    }
    _node->set_index(i);
    _stats->inc_refined_cards(i - start_index);
    return result;
  }

  void redirty_unrefined_cards(size_t start) {
    for ( ; start < _node_buffer_size; ++start) {
      *_node_buffer[start] = G1CardTable::dirty_card_val();
    }
  }

public:
  G1RefineBufferedCards(BufferNode* node,
                        size_t node_buffer_size,
                        uint worker_id,
                        G1ConcurrentRefineStats* stats) :
    _node(node),
    _node_buffer(reinterpret_cast<CardTable::CardValue**>(BufferNode::make_buffer_from_node(node))),
    _node_buffer_size(node_buffer_size),
    _worker_id(worker_id),
    _stats(stats),
    _g1rs(G1CollectedHeap::heap()->rem_set()) {}

  bool refine() {
    size_t first_clean_index = clean_cards();
    if (first_clean_index == _node_buffer_size) {
      _node->set_index(first_clean_index);
      return true;
    }
    // This fence serves two purposes. First, the cards must be cleaned
    // before processing the contents. Second, we can't proceed with
    // processing a region until after the read of the region's top in
    // collect_and_clean_cards(), for synchronization with possibly concurrent
    // humongous object allocation (see comment at the StoreStore fence before
    // setting the regions' tops in humongous allocation path).
    // It's okay that reading region's top and reading region's type were racy
    // wrto each other. We need both set, in any order, to proceed.
    OrderAccess::fence();
    sort_cards(first_clean_index);
    return refine_cleaned_cards(first_clean_index);
  }
};

bool G1DirtyCardQueueSet::refine_buffer(BufferNode* node,
                                        uint worker_id,
                                        G1ConcurrentRefineStats* stats) {
  Ticks start_time = Ticks::now();
  G1RefineBufferedCards buffered_cards(node,
                                       buffer_size(),
                                       worker_id,
                                       stats);
  bool result = buffered_cards.refine();
  stats->inc_refinement_time(Ticks::now() - start_time);
  return result;
}

void G1DirtyCardQueueSet::handle_refined_buffer(BufferNode* node,
                                                bool fully_processed) {
  if (fully_processed) {
    assert(node->index() == buffer_size(),
           "Buffer not fully consumed: index: " SIZE_FORMAT ", size: " SIZE_FORMAT,
           node->index(), buffer_size());
    deallocate_buffer(node);
  } else {
    assert(node->index() < buffer_size(), "Buffer fully consumed.");
    // Buffer incompletely processed because there is a pending safepoint.
    // Record partially processed buffer, to be finished later.
    record_paused_buffer(node);
  }
}

void G1DirtyCardQueueSet::handle_completed_buffer(BufferNode* new_node,
                                                  G1ConcurrentRefineStats* stats) {
  enqueue_completed_buffer(new_node);

  // No need for mutator refinement if number of cards is below limit.
  if (Atomic::load(&_num_cards) <= Atomic::load(&_padded_max_cards)) {
    return;
  }

  // Only Java threads perform mutator refinement.
  if (!Thread::current()->is_Java_thread()) {
    return;
  }

  BufferNode* node = get_completed_buffer();
  if (node == NULL) return;     // Didn't get a buffer to process.

  // Refine cards in buffer.

  uint worker_id = _free_ids.claim_par_id(); // temporarily claim an id
  bool fully_processed = refine_buffer(node, worker_id, stats);
  _free_ids.release_par_id(worker_id); // release the id

  // Deal with buffer after releasing id, to let another thread use id.
  handle_refined_buffer(node, fully_processed);
}

bool G1DirtyCardQueueSet::refine_completed_buffer_concurrently(uint worker_id,
                                                               size_t stop_at,
                                                               G1ConcurrentRefineStats* stats) {
  // Not enough cards to trigger processing.
  if (Atomic::load(&_num_cards) <= stop_at) return false;

  BufferNode* node = get_completed_buffer();
  if (node == NULL) return false; // Didn't get a buffer to process.

  bool fully_processed = refine_buffer(node, worker_id, stats);
  handle_refined_buffer(node, fully_processed);
  return true;
}

void G1DirtyCardQueueSet::abandon_logs() {
  assert_at_safepoint();
  abandon_completed_buffers();
  _detached_refinement_stats.reset();

  // Since abandon is done only at safepoints, we can safely manipulate
  // these queues.
  struct AbandonThreadLogClosure : public ThreadClosure {
    G1DirtyCardQueueSet& _qset;
    AbandonThreadLogClosure(G1DirtyCardQueueSet& qset) : _qset(qset) {}
    virtual void do_thread(Thread* t) {
      G1DirtyCardQueue& queue = G1ThreadLocalData::dirty_card_queue(t);
      _qset.reset_queue(queue);
      queue.refinement_stats()->reset();
    }
  } closure(*this);
  Threads::threads_do(&closure);

  G1BarrierSet::shared_dirty_card_queue().reset();
}

void G1DirtyCardQueueSet::concatenate_logs() {
  // Iterate over all the threads, if we find a partial log add it to
  // the global list of logs.  Temporarily turn off the limit on the number
  // of outstanding buffers.
  assert_at_safepoint();
  size_t old_limit = max_cards();
  set_max_cards(MaxCardsUnlimited);

  struct ConcatenateThreadLogClosure : public ThreadClosure {
    G1DirtyCardQueueSet& _qset;
    ConcatenateThreadLogClosure(G1DirtyCardQueueSet& qset) : _qset(qset) {}
    virtual void do_thread(Thread* t) {
      G1DirtyCardQueue& queue = G1ThreadLocalData::dirty_card_queue(t);
      if ((queue.buffer() != nullptr) &&
          (queue.index() != _qset.buffer_size())) {
        _qset.flush_queue(queue);
      }
    }
  } closure(*this);
  Threads::threads_do(&closure);

  G1BarrierSet::shared_dirty_card_queue().flush();
  enqueue_all_paused_buffers();
  verify_num_cards();
  set_max_cards(old_limit);
}

G1ConcurrentRefineStats G1DirtyCardQueueSet::get_and_reset_refinement_stats() {
  assert_at_safepoint();

  // Since we're at a safepoint, there aren't any races with recording of
  // detached refinement stats.  In particular, there's no risk of double
  // counting a thread that detaches after we've examined it but before
  // we've processed the detached stats.

  // Collect and reset stats for attached threads.
  struct CollectStats : public ThreadClosure {
    G1ConcurrentRefineStats _total_stats;
    virtual void do_thread(Thread* t) {
      G1DirtyCardQueue& dcq = G1ThreadLocalData::dirty_card_queue(t);
      G1ConcurrentRefineStats& stats = *dcq.refinement_stats();
      _total_stats += stats;
      stats.reset();
    }
  } closure;
  Threads::threads_do(&closure);

  // Collect and reset stats from detached threads.
  MutexLocker ml(G1DetachedRefinementStats_lock, Mutex::_no_safepoint_check_flag);
  closure._total_stats += _detached_refinement_stats;
  _detached_refinement_stats.reset();

  return closure._total_stats;
}

void G1DirtyCardQueueSet::record_detached_refinement_stats(G1ConcurrentRefineStats* stats) {
  MutexLocker ml(G1DetachedRefinementStats_lock, Mutex::_no_safepoint_check_flag);
  _detached_refinement_stats += *stats;
  stats->reset();
}

size_t G1DirtyCardQueueSet::max_cards() const {
  return _max_cards;
}

void G1DirtyCardQueueSet::set_max_cards(size_t value) {
  _max_cards = value;
  Atomic::store(&_padded_max_cards, value);
}

void G1DirtyCardQueueSet::set_max_cards_padding(size_t padding) {
  // Compute sum, clipping to max.
  size_t limit = _max_cards + padding;
  if (limit < padding) {        // Check for overflow.
    limit = MaxCardsUnlimited;
  }
  Atomic::store(&_padded_max_cards, limit);
}

void G1DirtyCardQueueSet::discard_max_cards_padding() {
  // Being racy here is okay, since all threads store the same value.
  if (_max_cards != Atomic::load(&_padded_max_cards)) {
    Atomic::store(&_padded_max_cards, _max_cards);
  }
}
