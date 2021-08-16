/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_UTILITIES_CONCURRENTHASHTABLE_INLINE_HPP
#define SHARE_UTILITIES_CONCURRENTHASHTABLE_INLINE_HPP

#include "utilities/concurrentHashTable.hpp"

#include "memory/allocation.inline.hpp"
#include "runtime/atomic.hpp"
#include "runtime/orderAccess.hpp"
#include "runtime/prefetch.inline.hpp"
#include "utilities/globalCounter.inline.hpp"
#include "utilities/numberSeq.hpp"
#include "utilities/spinYield.hpp"

// 2^30 = 1G buckets
#define SIZE_BIG_LOG2 30
// 2^2  = 4 buckets
#define SIZE_SMALL_LOG2 2

// Number from spinYield.hpp. In some loops SpinYield would be unfair.
#define SPINPAUSES_PER_YIELD 8192

#ifdef ASSERT
#ifdef _LP64
// Two low bits are not usable.
static const void* POISON_PTR = (void*)UCONST64(0xfbadbadbadbadbac);
#else
// Two low bits are not usable.
static const void* POISON_PTR = (void*)0xffbadbac;
#endif
#endif

// Node
template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::Node*
ConcurrentHashTable<CONFIG, F>::
  Node::next() const
{
  return Atomic::load_acquire(&_next);
}

// Bucket
template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::Node*
ConcurrentHashTable<CONFIG, F>::
  Bucket::first_raw() const
{
  return Atomic::load_acquire(&_first);
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  Bucket::release_assign_node_ptr(
    typename ConcurrentHashTable<CONFIG, F>::Node* const volatile * dst,
    typename ConcurrentHashTable<CONFIG, F>::Node* node) const
{
  // Due to this assert this methods is not static.
  assert(is_locked(), "Must be locked.");
  Node** tmp = (Node**)dst;
  Atomic::release_store(tmp, clear_set_state(node, *dst));
}

template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::Node*
ConcurrentHashTable<CONFIG, F>::
  Bucket::first() const
{
  // We strip the states bit before returning the ptr.
  return clear_state(Atomic::load_acquire(&_first));
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  Bucket::have_redirect() const
{
  return is_state(first_raw(), STATE_REDIRECT_BIT);
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  Bucket::is_locked() const
{
  return is_state(first_raw(), STATE_LOCK_BIT);
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  Bucket::lock()
{
  int i = 0;
  // SpinYield would be unfair here
  while (!this->trylock()) {
    if ((++i) == SPINPAUSES_PER_YIELD) {
      // On contemporary OS yielding will give CPU to another runnable thread if
      // there is no CPU available.
      os::naked_yield();
      i = 0;
    } else {
      SpinPause();
    }
  }
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  Bucket::release_assign_last_node_next(
     typename ConcurrentHashTable<CONFIG, F>::Node* node)
{
  assert(is_locked(), "Must be locked.");
  Node* const volatile * ret = first_ptr();
  while (clear_state(*ret) != NULL) {
    ret = clear_state(*ret)->next_ptr();
  }
  release_assign_node_ptr(ret, node);
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  Bucket::cas_first(typename ConcurrentHashTable<CONFIG, F>::Node* node,
                    typename ConcurrentHashTable<CONFIG, F>::Node* expect
                    )
{
  if (is_locked()) {
    return false;
  }
  if (Atomic::cmpxchg(&_first, expect, node) == expect) {
    return true;
  }
  return false;
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  Bucket::trylock()
{
  if (is_locked()) {
    return false;
  }
  // We will expect a clean first pointer.
  Node* tmp = first();
  if (Atomic::cmpxchg(&_first, tmp, set_state(tmp, STATE_LOCK_BIT)) == tmp) {
    return true;
  }
  return false;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  Bucket::unlock()
{
  assert(is_locked(), "Must be locked.");
  assert(!have_redirect(),
         "Unlocking a bucket after it has reached terminal state.");
  Atomic::release_store(&_first, clear_state(first()));
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  Bucket::redirect()
{
  assert(is_locked(), "Must be locked.");
  Atomic::release_store(&_first, set_state(_first, STATE_REDIRECT_BIT));
}

// InternalTable
template <typename CONFIG, MEMFLAGS F>
inline ConcurrentHashTable<CONFIG, F>::
  InternalTable::InternalTable(size_t log2_size)
    : _log2_size(log2_size), _size(((size_t)1ul) << _log2_size),
      _hash_mask(~(~((size_t)0) << _log2_size))
{
  assert(_log2_size >= SIZE_SMALL_LOG2 && _log2_size <= SIZE_BIG_LOG2,
         "Bad size");
  _buckets = NEW_C_HEAP_ARRAY(Bucket, _size, F);
  // Use placement new for each element instead of new[] which could use more
  // memory than allocated.
  for (size_t i = 0; i < _size; ++i) {
    new (_buckets + i) Bucket();
  }
}

template <typename CONFIG, MEMFLAGS F>
inline ConcurrentHashTable<CONFIG, F>::
  InternalTable::~InternalTable()
{
  FREE_C_HEAP_ARRAY(Bucket, _buckets);
}

// ScopedCS
template <typename CONFIG, MEMFLAGS F>
inline ConcurrentHashTable<CONFIG, F>::
  ScopedCS::ScopedCS(Thread* thread, ConcurrentHashTable<CONFIG, F>* cht)
    : _thread(thread),
      _cht(cht),
      _cs_context(GlobalCounter::critical_section_begin(_thread))
{
  // This version is published now.
  if (Atomic::load_acquire(&_cht->_invisible_epoch) != NULL) {
    Atomic::release_store_fence(&_cht->_invisible_epoch, (Thread*)NULL);
  }
}

template <typename CONFIG, MEMFLAGS F>
inline ConcurrentHashTable<CONFIG, F>::
  ScopedCS::~ScopedCS()
{
  GlobalCounter::critical_section_end(_thread, _cs_context);
}

template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC>
inline typename CONFIG::Value* ConcurrentHashTable<CONFIG, F>::
  MultiGetHandle::get(LOOKUP_FUNC& lookup_f, bool* grow_hint)
{
  return ScopedCS::_cht->internal_get(ScopedCS::_thread, lookup_f, grow_hint);
}

// HaveDeletables
template <typename CONFIG, MEMFLAGS F>
template <typename EVALUATE_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  HaveDeletables<true, EVALUATE_FUNC>::have_deletable(Bucket* bucket,
                                                      EVALUATE_FUNC& eval_f,
                                                      Bucket* prefetch_bucket)
{
  // Instantiated for pointer type (true), so we can use prefetch.
  // When visiting all Nodes doing this prefetch give around 30%.
  Node* pref = prefetch_bucket != NULL ? prefetch_bucket->first() : NULL;
  for (Node* next = bucket->first(); next != NULL ; next = next->next()) {
    if (pref != NULL) {
      Prefetch::read(*pref->value(), 0);
      pref = pref->next();
    }
    // Read next() Node* once.  May be racing with a thread moving the next
    // pointers.
    Node* next_pref = next->next();
    if (next_pref != NULL) {
      Prefetch::read(*next_pref->value(), 0);
    }
    if (eval_f(next->value())) {
      return true;
    }
  }
  return false;
}

template <typename CONFIG, MEMFLAGS F>
template <bool b, typename EVALUATE_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  HaveDeletables<b, EVALUATE_FUNC>::have_deletable(Bucket* bucket,
                                                   EVALUATE_FUNC& eval_f,
                                                   Bucket* preb)
{
  for (Node* next = bucket->first(); next != NULL ; next = next->next()) {
    if (eval_f(next->value())) {
      return true;
    }
  }
  return false;
}

// ConcurrentHashTable
template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  write_synchonize_on_visible_epoch(Thread* thread)
{
  assert(_resize_lock_owner == thread, "Re-size lock not held");
  OrderAccess::fence(); // Prevent below load from floating up.
  // If no reader saw this version we can skip write_synchronize.
  if (Atomic::load_acquire(&_invisible_epoch) == thread) {
    return;
  }
  assert(_invisible_epoch == NULL, "Two thread doing bulk operations");
  // We set this/next version that we are synchronizing for to not published.
  // A reader will zero this flag if it reads this/next version.
  Atomic::release_store(&_invisible_epoch, thread);
  GlobalCounter::write_synchronize();
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  try_resize_lock(Thread* locker)
{
  if (_resize_lock->try_lock()) {
    if (_resize_lock_owner != NULL) {
      assert(locker != _resize_lock_owner, "Already own lock");
      // We got mutex but internal state is locked.
      _resize_lock->unlock();
      return false;
    }
  } else {
    return false;
  }
  _invisible_epoch = 0;
  _resize_lock_owner = locker;
  return true;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  lock_resize_lock(Thread* locker)
{
  size_t i = 0;
  // If lock is hold by some other thread, the chances that it is return quick
  // is low. So we will prefer yielding.
  SpinYield yield(1, 512);
  do {
    _resize_lock->lock_without_safepoint_check();
    // If holder of lock dropped mutex for safepoint mutex might be unlocked,
    // and _resize_lock_owner will contain the owner.
    if (_resize_lock_owner != NULL) {
      assert(locker != _resize_lock_owner, "Already own lock");
      // We got mutex but internal state is locked.
      _resize_lock->unlock();
      yield.wait();
    } else {
      break;
    }
  } while(true);
  _resize_lock_owner = locker;
  _invisible_epoch = 0;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  unlock_resize_lock(Thread* locker)
{
  _invisible_epoch = 0;
  assert(locker == _resize_lock_owner, "Not unlocked by locker.");
  _resize_lock_owner = NULL;
  _resize_lock->unlock();
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  free_nodes()
{
  // We assume we are not MT during freeing.
  for (size_t node_it = 0; node_it < _table->_size; node_it++) {
    Bucket* bucket = _table->get_buckets() + node_it;
    Node* node = bucket->first();
    while (node != NULL) {
      Node* free_node = node;
      node = node->next();
      Node::destroy_node(_context, free_node);
    }
  }
}

template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::InternalTable*
ConcurrentHashTable<CONFIG, F>::
  get_table() const
{
  return Atomic::load_acquire(&_table);
}

template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::InternalTable*
ConcurrentHashTable<CONFIG, F>::
  get_new_table() const
{
  return Atomic::load_acquire(&_new_table);
}

template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::InternalTable*
ConcurrentHashTable<CONFIG, F>::
  set_table_from_new()
{
  InternalTable* old_table = _table;
  // Publish the new table.
  Atomic::release_store(&_table, _new_table);
  // All must see this.
  GlobalCounter::write_synchronize();
  // _new_table not read any more.
  _new_table = NULL;
  DEBUG_ONLY(_new_table = (InternalTable*)POISON_PTR;)
  return old_table;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  internal_grow_range(Thread* thread, size_t start, size_t stop)
{
  assert(stop <= _table->_size, "Outside backing array");
  assert(_new_table != NULL, "Grow not proper setup before start");
  // The state is also copied here. Hence all buckets in new table will be
  // locked. I call the siblings odd/even, where even have high bit 0 and odd
  // have high bit 1.
  for (size_t even_index = start; even_index < stop; even_index++) {
    Bucket* bucket = _table->get_bucket(even_index);

    bucket->lock();

    size_t odd_index = even_index + _table->_size;
    _new_table->get_buckets()[even_index] = *bucket;
    _new_table->get_buckets()[odd_index] = *bucket;

    // Moves lockers go to new table, where they will wait until unlock() below.
    bucket->redirect(); /* Must release stores above */

    // When this is done we have separated the nodes into corresponding buckets
    // in new table.
    if (!unzip_bucket(thread, _table, _new_table, even_index, odd_index)) {
      // If bucket is empty, unzip does nothing.
      // We must make sure readers go to new table before we poison the bucket.
      DEBUG_ONLY(GlobalCounter::write_synchronize();)
    }

    // Unlock for writes into the new table buckets.
    _new_table->get_bucket(even_index)->unlock();
    _new_table->get_bucket(odd_index)->unlock();

    DEBUG_ONLY(
       bucket->release_assign_node_ptr(
          _table->get_bucket(even_index)->first_ptr(), (Node*)POISON_PTR);
    )
  }
}

template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC, typename DELETE_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  internal_remove(Thread* thread, LOOKUP_FUNC& lookup_f, DELETE_FUNC& delete_f)
{
  Bucket* bucket = get_bucket_locked(thread, lookup_f.get_hash());
  assert(bucket->is_locked(), "Must be locked.");
  Node* const volatile * rem_n_prev = bucket->first_ptr();
  Node* rem_n = bucket->first();
  bool have_dead = false;
  while (rem_n != NULL) {
    if (lookup_f.equals(rem_n->value(), &have_dead)) {
      bucket->release_assign_node_ptr(rem_n_prev, rem_n->next());
      break;
    } else {
      rem_n_prev = rem_n->next_ptr();
      rem_n = rem_n->next();
    }
  }

  bucket->unlock();

  if (rem_n == NULL) {
    return false;
  }
  // Publish the deletion.
  GlobalCounter::write_synchronize();
  delete_f(rem_n->value());
  Node::destroy_node(_context, rem_n);
  JFR_ONLY(_stats_rate.remove();)
  return true;
}

template <typename CONFIG, MEMFLAGS F>
template <typename EVALUATE_FUNC, typename DELETE_FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  do_bulk_delete_locked_for(Thread* thread, size_t start_idx, size_t stop_idx,
                            EVALUATE_FUNC& eval_f, DELETE_FUNC& del_f, bool is_mt)
{
  // Here we have resize lock so table is SMR safe, and there is no new
  // table. Can do this in parallel if we want.
  assert((is_mt && _resize_lock_owner != NULL) ||
         (!is_mt && _resize_lock_owner == thread), "Re-size lock not held");
  Node* ndel[BULK_DELETE_LIMIT];
  InternalTable* table = get_table();
  assert(start_idx < stop_idx, "Must be");
  assert(stop_idx <= _table->_size, "Must be");
  // Here manual do critical section since we don't want to take the cost of
  // locking the bucket if there is nothing to delete. But we can have
  // concurrent single deletes. The _invisible_epoch can only be used by the
  // owner of _resize_lock, us here. There we should not changed it in our
  // own read-side.
  GlobalCounter::CSContext cs_context = GlobalCounter::critical_section_begin(thread);
  for (size_t bucket_it = start_idx; bucket_it < stop_idx; bucket_it++) {
    Bucket* bucket = table->get_bucket(bucket_it);
    Bucket* prefetch_bucket = (bucket_it+1) < stop_idx ?
                              table->get_bucket(bucket_it+1) : NULL;

    if (!HaveDeletables<IsPointer<VALUE>::value, EVALUATE_FUNC>::
        have_deletable(bucket, eval_f, prefetch_bucket)) {
        // Nothing to remove in this bucket.
        continue;
    }

    GlobalCounter::critical_section_end(thread, cs_context);
    // We left critical section but the bucket cannot be removed while we hold
    // the _resize_lock.
    bucket->lock();
    size_t nd = delete_check_nodes(bucket, eval_f, BULK_DELETE_LIMIT, ndel);
    bucket->unlock();
    if (is_mt) {
      GlobalCounter::write_synchronize();
    } else {
      write_synchonize_on_visible_epoch(thread);
    }
    for (size_t node_it = 0; node_it < nd; node_it++) {
      del_f(ndel[node_it]->value());
      Node::destroy_node(_context, ndel[node_it]);
      JFR_ONLY(_stats_rate.remove();)
      DEBUG_ONLY(ndel[node_it] = (Node*)POISON_PTR;)
    }
    cs_context = GlobalCounter::critical_section_begin(thread);
  }
  GlobalCounter::critical_section_end(thread, cs_context);
}

template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  delete_in_bucket(Thread* thread, Bucket* bucket, LOOKUP_FUNC& lookup_f)
{
  assert(bucket->is_locked(), "Must be locked.");

  size_t dels = 0;
  Node* ndel[BULK_DELETE_LIMIT];
  Node* const volatile * rem_n_prev = bucket->first_ptr();
  Node* rem_n = bucket->first();
  while (rem_n != NULL) {
    bool is_dead = false;
    lookup_f.equals(rem_n->value(), &is_dead);
    if (is_dead) {
      ndel[dels++] = rem_n;
      Node* next_node = rem_n->next();
      bucket->release_assign_node_ptr(rem_n_prev, next_node);
      rem_n = next_node;
      if (dels == BULK_DELETE_LIMIT) {
        break;
      }
    } else {
      rem_n_prev = rem_n->next_ptr();
      rem_n = rem_n->next();
    }
  }
  if (dels > 0) {
    GlobalCounter::write_synchronize();
    for (size_t node_it = 0; node_it < dels; node_it++) {
      Node::destroy_node(_context, ndel[node_it]);
      JFR_ONLY(_stats_rate.remove();)
      DEBUG_ONLY(ndel[node_it] = (Node*)POISON_PTR;)
    }
  }
}

template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::Bucket*
ConcurrentHashTable<CONFIG, F>::
  get_bucket(uintx hash) const
{
  InternalTable* table = get_table();
  Bucket* bucket = get_bucket_in(table, hash);
  if (bucket->have_redirect()) {
    table = get_new_table();
    bucket = get_bucket_in(table, hash);
  }
  return bucket;
}

template <typename CONFIG, MEMFLAGS F>
inline typename ConcurrentHashTable<CONFIG, F>::Bucket*
ConcurrentHashTable<CONFIG, F>::
  get_bucket_locked(Thread* thread, const uintx hash)
{
  Bucket* bucket;
  int i = 0;
  // SpinYield would be unfair here
  while(true) {
    {
      // We need a critical section to protect the table itself. But if we fail
      // we must leave critical section otherwise we would deadlock.
      ScopedCS cs(thread, this);
      bucket = get_bucket(hash);
      if (bucket->trylock()) {
        break; /* ends critical section */
      }
    } /* ends critical section */
    if ((++i) == SPINPAUSES_PER_YIELD) {
      // On contemporary OS yielding will give CPU to another runnable thread if
      // there is no CPU available.
      os::naked_yield();
      i = 0;
    } else {
      SpinPause();
    }
  }
  return bucket;
}

// Always called within critical section
template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC>
typename ConcurrentHashTable<CONFIG, F>::Node*
ConcurrentHashTable<CONFIG, F>::
  get_node(const Bucket* const bucket, LOOKUP_FUNC& lookup_f,
           bool* have_dead, size_t* loops) const
{
  size_t loop_count = 0;
  Node* node = bucket->first();
  while (node != NULL) {
    bool is_dead = false;
    ++loop_count;
    if (lookup_f.equals(node->value(), &is_dead)) {
      break;
    }
    if (is_dead && !(*have_dead)) {
      *have_dead = true;
    }
    node = node->next();
  }
  if (loops != NULL) {
    *loops = loop_count;
  }
  return node;
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  unzip_bucket(Thread* thread, InternalTable* old_table,
               InternalTable* new_table, size_t even_index, size_t odd_index)
{
  Node* aux = old_table->get_bucket(even_index)->first();
  if (aux == NULL) {
    // This is an empty bucket and in debug we poison first ptr in bucket.
    // Therefore we must make sure no readers are looking at this bucket.
    // If we don't do a write_synch here, caller must do it.
    return false;
  }
  Node* delete_me = NULL;
  Node* const volatile * even = new_table->get_bucket(even_index)->first_ptr();
  Node* const volatile * odd = new_table->get_bucket(odd_index)->first_ptr();
  while (aux != NULL) {
    bool dead_hash = false;
    size_t aux_hash = CONFIG::get_hash(*aux->value(), &dead_hash);
    Node* aux_next = aux->next();
    if (dead_hash) {
      delete_me = aux;
      // This item is dead, move both list to next
      new_table->get_bucket(odd_index)->release_assign_node_ptr(odd,
                                                                aux_next);
      new_table->get_bucket(even_index)->release_assign_node_ptr(even,
                                                                 aux_next);
    } else {
      size_t aux_index = bucket_idx_hash(new_table, aux_hash);
      if (aux_index == even_index) {
        // This is a even, so move odd to aux/even next
        new_table->get_bucket(odd_index)->release_assign_node_ptr(odd,
                                                                  aux_next);
        // Keep in even list
        even = aux->next_ptr();
      } else if (aux_index == odd_index) {
        // This is a odd, so move odd to aux/odd next
        new_table->get_bucket(even_index)->release_assign_node_ptr(even,
                                                                   aux_next);
        // Keep in odd list
        odd = aux->next_ptr();
      } else {
        fatal("aux_index does not match even or odd indices");
      }
    }
    aux = aux_next;

    // We can only move 1 pointer otherwise a reader might be moved to the wrong
    // chain. E.g. looking for even hash value but got moved to the odd bucket
    // chain.
    write_synchonize_on_visible_epoch(thread);
    if (delete_me != NULL) {
      Node::destroy_node(_context, delete_me);
      delete_me = NULL;
    }
  }
  return true;
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  internal_shrink_prolog(Thread* thread, size_t log2_size)
{
  if (!try_resize_lock(thread)) {
    return false;
  }
  assert(_resize_lock_owner == thread, "Re-size lock not held");
  if (_table->_log2_size == _log2_start_size ||
      _table->_log2_size <= log2_size) {
    unlock_resize_lock(thread);
    return false;
  }
  _new_table = new InternalTable(_table->_log2_size - 1);
  return true;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  internal_shrink_epilog(Thread* thread)
{
  assert(_resize_lock_owner == thread, "Re-size lock not held");

  InternalTable* old_table = set_table_from_new();
  _size_limit_reached = false;
  unlock_resize_lock(thread);
#ifdef ASSERT
  for (size_t i = 0; i < old_table->_size; i++) {
    assert(old_table->get_bucket(i++)->first() == POISON_PTR,
           "No poison found");
  }
#endif
  // ABA safe, old_table not visible to any other threads.
  delete old_table;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  internal_shrink_range(Thread* thread, size_t start, size_t stop)
{
  // The state is also copied here.
  // Hence all buckets in new table will be locked.
  for (size_t bucket_it = start; bucket_it < stop; bucket_it++) {
    size_t even_hash_index = bucket_it; // High bit 0
    size_t odd_hash_index = bucket_it + _new_table->_size; // High bit 1

    Bucket* b_old_even = _table->get_bucket(even_hash_index);
    Bucket* b_old_odd  = _table->get_bucket(odd_hash_index);

    b_old_even->lock();
    b_old_odd->lock();

    _new_table->get_buckets()[bucket_it] = *b_old_even;

    // Put chains together.
    _new_table->get_bucket(bucket_it)->
      release_assign_last_node_next(*(b_old_odd->first_ptr()));

    b_old_even->redirect();
    b_old_odd->redirect();

    write_synchonize_on_visible_epoch(thread);

    // Unlock for writes into new smaller table.
    _new_table->get_bucket(bucket_it)->unlock();

    DEBUG_ONLY(b_old_even->release_assign_node_ptr(b_old_even->first_ptr(),
                                                   (Node*)POISON_PTR);)
    DEBUG_ONLY(b_old_odd->release_assign_node_ptr(b_old_odd->first_ptr(),
                                                  (Node*)POISON_PTR);)
  }
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  internal_shrink(Thread* thread, size_t log2_size)
{
  if (!internal_shrink_prolog(thread, log2_size)) {
    assert(_resize_lock_owner != thread, "Re-size lock held");
    return false;
  }
  assert(_resize_lock_owner == thread, "Should be locked by me");
  internal_shrink_range(thread, 0, _new_table->_size);
  internal_shrink_epilog(thread);
  assert(_resize_lock_owner != thread, "Re-size lock held");
  return true;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  internal_reset(size_t log2_size)
{
  assert(_table != NULL, "table failed");
  assert(_log2_size_limit >= log2_size, "bad ergo");

  delete _table;
  // Create and publish a new table
  InternalTable* table = new InternalTable(log2_size);
  _size_limit_reached = (log2_size == _log2_size_limit);
  Atomic::release_store(&_table, table);
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  internal_grow_prolog(Thread* thread, size_t log2_size)
{
  // This double checking of _size_limit_reached/is_max_size_reached()
  //  we only do in grow path, since grow means high load on table
  // while shrink means low load.
  if (is_max_size_reached()) {
    return false;
  }
  if (!try_resize_lock(thread)) {
    // Either we have an ongoing resize or an operation which doesn't want us
    // to resize now.
    return false;
  }
  if (is_max_size_reached() || _table->_log2_size >= log2_size) {
    unlock_resize_lock(thread);
    return false;
  }

  _new_table = new InternalTable(_table->_log2_size + 1);
  _size_limit_reached = _new_table->_log2_size == _log2_size_limit;

  return true;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  internal_grow_epilog(Thread* thread)
{
  assert(_resize_lock_owner == thread, "Should be locked");

  InternalTable* old_table = set_table_from_new();
  unlock_resize_lock(thread);
#ifdef ASSERT
  for (size_t i = 0; i < old_table->_size; i++) {
    assert(old_table->get_bucket(i++)->first() == POISON_PTR,
           "No poison found");
  }
#endif
  // ABA safe, old_table not visible to any other threads.
  delete old_table;
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  internal_grow(Thread* thread, size_t log2_size)
{
  if (!internal_grow_prolog(thread, log2_size)) {
    assert(_resize_lock_owner != thread, "Re-size lock held");
    return false;
  }
  assert(_resize_lock_owner == thread, "Should be locked by me");
  internal_grow_range(thread, 0, _table->_size);
  internal_grow_epilog(thread);
  assert(_resize_lock_owner != thread, "Re-size lock held");
  return true;
}

// Always called within critical section
template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC>
inline typename CONFIG::Value* ConcurrentHashTable<CONFIG, F>::
  internal_get(Thread* thread, LOOKUP_FUNC& lookup_f, bool* grow_hint)
{
  bool clean = false;
  size_t loops = 0;
  VALUE* ret = NULL;

  const Bucket* bucket = get_bucket(lookup_f.get_hash());
  Node* node = get_node(bucket, lookup_f, &clean, &loops);
  if (node != NULL) {
    ret = node->value();
  }
  if (grow_hint != NULL) {
    *grow_hint = loops > _grow_hint;
  }

  return ret;
}

template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC, typename FOUND_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  internal_insert_get(Thread* thread, LOOKUP_FUNC& lookup_f, const VALUE& value,
                      FOUND_FUNC& foundf, bool* grow_hint, bool* clean_hint)
{
  bool ret = false;
  bool clean = false;
  bool locked;
  size_t loops = 0;
  size_t i = 0;
  uintx hash = lookup_f.get_hash();
  Node* new_node = Node::create_node(_context, value, NULL);

  while (true) {
    {
      ScopedCS cs(thread, this); /* protected the table/bucket */
      Bucket* bucket = get_bucket(hash);
      Node* first_at_start = bucket->first();
      Node* old = get_node(bucket, lookup_f, &clean, &loops);
      if (old == NULL) {
        new_node->set_next(first_at_start);
        if (bucket->cas_first(new_node, first_at_start)) {
          foundf(new_node->value());
          JFR_ONLY(_stats_rate.add();)
          new_node = NULL;
          ret = true;
          break; /* leave critical section */
        }
        // CAS failed we must leave critical section and retry.
        locked = bucket->is_locked();
      } else {
        // There is a duplicate.
        foundf(old->value());
        break; /* leave critical section */
      }
    } /* leave critical section */
    i++;
    if (locked) {
      os::naked_yield();
    } else {
      SpinPause();
    }
  }

  if (new_node != NULL) {
    // CAS failed and a duplicate was inserted, we must free this node.
    Node::destroy_node(_context, new_node);
  } else if (i == 0 && clean) {
    // We only do cleaning on fast inserts.
    Bucket* bucket = get_bucket_locked(thread, lookup_f.get_hash());
    delete_in_bucket(thread, bucket, lookup_f);
    bucket->unlock();
    clean = false;
  }

  if (grow_hint != NULL) {
    *grow_hint = loops > _grow_hint;
  }

  if (clean_hint != NULL) {
    *clean_hint = clean;
  }

  return ret;
}

template <typename CONFIG, MEMFLAGS F>
template <typename FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  visit_nodes(Bucket* bucket, FUNC& visitor_f)
{
  Node* current_node = bucket->first();
  while (current_node != NULL) {
    Prefetch::read(current_node->next(), 0);
    if (!visitor_f(current_node->value())) {
      return false;
    }
    current_node = current_node->next();
  }
  return true;
}

template <typename CONFIG, MEMFLAGS F>
template <typename FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  do_scan_locked(Thread* thread, FUNC& scan_f)
{
  assert(_resize_lock_owner == thread, "Re-size lock not held");
  // We can do a critical section over the entire loop but that would block
  // updates for a long time. Instead we choose to block resizes.
  InternalTable* table = get_table();
  for (size_t bucket_it = 0; bucket_it < table->_size; bucket_it++) {
    ScopedCS cs(thread, this);
    if (!visit_nodes(table->get_bucket(bucket_it), scan_f)) {
      break; /* ends critical section */
    }
  } /* ends critical section */
}

template <typename CONFIG, MEMFLAGS F>
template <typename EVALUATE_FUNC>
inline size_t ConcurrentHashTable<CONFIG, F>::
  delete_check_nodes(Bucket* bucket, EVALUATE_FUNC& eval_f,
                     size_t num_del, Node** ndel)
{
  size_t dels = 0;
  Node* const volatile * rem_n_prev = bucket->first_ptr();
  Node* rem_n = bucket->first();
  while (rem_n != NULL) {
    if (eval_f(rem_n->value())) {
      ndel[dels++] = rem_n;
      Node* next_node = rem_n->next();
      bucket->release_assign_node_ptr(rem_n_prev, next_node);
      rem_n = next_node;
      if (dels == num_del) {
        break;
      }
    } else {
      rem_n_prev = rem_n->next_ptr();
      rem_n = rem_n->next();
    }
  }
  return dels;
}

// Constructor
template <typename CONFIG, MEMFLAGS F>
inline ConcurrentHashTable<CONFIG, F>::
  ConcurrentHashTable(size_t log2size, size_t log2size_limit, size_t grow_hint, void* context)
    : _context(context), _new_table(NULL), _log2_size_limit(log2size_limit),
      _log2_start_size(log2size), _grow_hint(grow_hint),
      _size_limit_reached(false), _resize_lock_owner(NULL),
      _invisible_epoch(0)
{
  _stats_rate = TableRateStatistics();
  _resize_lock =
    new Mutex(Mutex::leaf, "ConcurrentHashTable", true,
              Mutex::_safepoint_check_never);
  _table = new InternalTable(log2size);
  assert(log2size_limit >= log2size, "bad ergo");
  _size_limit_reached = _table->_log2_size == _log2_size_limit;
}

template <typename CONFIG, MEMFLAGS F>
inline ConcurrentHashTable<CONFIG, F>::
  ~ConcurrentHashTable()
{
  delete _resize_lock;
  free_nodes();
  delete _table;
}

template <typename CONFIG, MEMFLAGS F>
inline size_t ConcurrentHashTable<CONFIG, F>::
  get_mem_size(Thread* thread)
{
  ScopedCS cs(thread, this);
  return sizeof(*this) + _table->get_mem_size();
}

template <typename CONFIG, MEMFLAGS F>
inline size_t ConcurrentHashTable<CONFIG, F>::
  get_size_log2(Thread* thread)
{
  ScopedCS cs(thread, this);
  return _table->_log2_size;
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  shrink(Thread* thread, size_t size_limit_log2)
{
  size_t tmp = size_limit_log2 == 0 ? _log2_start_size : size_limit_log2;
  bool ret = internal_shrink(thread, tmp);
  return ret;
}

template <typename CONFIG, MEMFLAGS F>
inline void ConcurrentHashTable<CONFIG, F>::
  unsafe_reset(size_t size_log2)
{
  size_t tmp = size_log2 == 0 ? _log2_start_size : size_log2;
  internal_reset(tmp);
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  grow(Thread* thread, size_t size_limit_log2)
{
  size_t tmp = size_limit_log2 == 0 ? _log2_size_limit : size_limit_log2;
  return internal_grow(thread, tmp);
}

template <typename CONFIG, MEMFLAGS F>
template <typename LOOKUP_FUNC, typename FOUND_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  get(Thread* thread, LOOKUP_FUNC& lookup_f, FOUND_FUNC& found_f, bool* grow_hint)
{
  bool ret = false;
  ScopedCS cs(thread, this);
  VALUE* val = internal_get(thread, lookup_f, grow_hint);
  if (val != NULL) {
    found_f(val);
    ret = true;
  }
  return ret;
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  unsafe_insert(const VALUE& value) {
  bool dead_hash = false;
  size_t hash = CONFIG::get_hash(value, &dead_hash);
  if (dead_hash) {
    return false;
  }
  // This is an unsafe operation.
  InternalTable* table = get_table();
  Bucket* bucket = get_bucket_in(table, hash);
  assert(!bucket->have_redirect() && !bucket->is_locked(), "bad");
  Node* new_node = Node::create_node(_context, value, bucket->first());
  if (!bucket->cas_first(new_node, bucket->first())) {
    assert(false, "bad");
  }
  JFR_ONLY(_stats_rate.add();)
  return true;
}

template <typename CONFIG, MEMFLAGS F>
template <typename SCAN_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  try_scan(Thread* thread, SCAN_FUNC& scan_f)
{
  if (!try_resize_lock(thread)) {
    return false;
  }
  do_scan_locked(thread, scan_f);
  unlock_resize_lock(thread);
  return true;
}

template <typename CONFIG, MEMFLAGS F>
template <typename SCAN_FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  do_scan(Thread* thread, SCAN_FUNC& scan_f)
{
  assert(!SafepointSynchronize::is_at_safepoint(),
         "must be outside a safepoint");
  assert(_resize_lock_owner != thread, "Re-size lock held");
  lock_resize_lock(thread);
  do_scan_locked(thread, scan_f);
  unlock_resize_lock(thread);
  assert(_resize_lock_owner != thread, "Re-size lock held");
}

template <typename CONFIG, MEMFLAGS F>
template <typename SCAN_FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  do_safepoint_scan(SCAN_FUNC& scan_f)
{
  // We only allow this method to be used during a safepoint.
  assert(SafepointSynchronize::is_at_safepoint(),
         "must only be called in a safepoint");

  // Here we skip protection,
  // thus no other thread may use this table at the same time.
  InternalTable* table = get_table();
  for (size_t bucket_it = 0; bucket_it < table->_size; bucket_it++) {
    Bucket* bucket = table->get_bucket(bucket_it);
    // If bucket have a redirect the items will be in the new table.
    // We must visit them there since the new table will contain any
    // concurrent inserts done after this bucket was resized.
    // If the bucket don't have redirect flag all items is in this table.
    if (!bucket->have_redirect()) {
      if(!visit_nodes(bucket, scan_f)) {
        return;
      }
    } else {
      assert(bucket->is_locked(), "Bucket must be locked.");
    }
  }
  // If there is a paused resize we also need to visit the already resized items.
  table = get_new_table();
  if (table == NULL) {
    return;
  }
  DEBUG_ONLY(if (table == POISON_PTR) { return; })
  for (size_t bucket_it = 0; bucket_it < table->_size; bucket_it++) {
    Bucket* bucket = table->get_bucket(bucket_it);
    assert(!bucket->is_locked(), "Bucket must be unlocked.");
    if (!visit_nodes(bucket, scan_f)) {
      return;
    }
  }
}

template <typename CONFIG, MEMFLAGS F>
template <typename EVALUATE_FUNC, typename DELETE_FUNC>
inline bool ConcurrentHashTable<CONFIG, F>::
  try_bulk_delete(Thread* thread, EVALUATE_FUNC& eval_f, DELETE_FUNC& del_f)
{
  if (!try_resize_lock(thread)) {
    return false;
  }
  do_bulk_delete_locked(thread, eval_f, del_f);
  unlock_resize_lock(thread);
  assert(_resize_lock_owner != thread, "Re-size lock held");
  return true;
}

template <typename CONFIG, MEMFLAGS F>
template <typename EVALUATE_FUNC, typename DELETE_FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  bulk_delete(Thread* thread, EVALUATE_FUNC& eval_f, DELETE_FUNC& del_f)
{
  assert(!SafepointSynchronize::is_at_safepoint(),
         "must be outside a safepoint");
  lock_resize_lock(thread);
  do_bulk_delete_locked(thread, eval_f, del_f);
  unlock_resize_lock(thread);
}

template <typename CONFIG, MEMFLAGS F>
template <typename VALUE_SIZE_FUNC>
inline TableStatistics ConcurrentHashTable<CONFIG, F>::
  statistics_calculate(Thread* thread, VALUE_SIZE_FUNC& vs_f)
{
  NumberSeq summary;
  size_t literal_bytes = 0;
  InternalTable* table = get_table();
  for (size_t bucket_it = 0; bucket_it < table->_size; bucket_it++) {
    ScopedCS cs(thread, this);
    size_t count = 0;
    Bucket* bucket = table->get_bucket(bucket_it);
    if (bucket->have_redirect() || bucket->is_locked()) {
      continue;
    }
    Node* current_node = bucket->first();
    while (current_node != NULL) {
      ++count;
      literal_bytes += vs_f(current_node->value());
      current_node = current_node->next();
    }
    summary.add((double)count);
  }

  return TableStatistics(_stats_rate, summary, literal_bytes, sizeof(Bucket), sizeof(Node));
}

template <typename CONFIG, MEMFLAGS F>
template <typename VALUE_SIZE_FUNC>
inline TableStatistics ConcurrentHashTable<CONFIG, F>::
  statistics_get(Thread* thread, VALUE_SIZE_FUNC& vs_f, TableStatistics old)
{
  if (!try_resize_lock(thread)) {
    return old;
  }

  TableStatistics ts = statistics_calculate(thread, vs_f);
  unlock_resize_lock(thread);

  return ts;
}

template <typename CONFIG, MEMFLAGS F>
template <typename VALUE_SIZE_FUNC>
inline void ConcurrentHashTable<CONFIG, F>::
  statistics_to(Thread* thread, VALUE_SIZE_FUNC& vs_f,
                outputStream* st, const char* table_name)
{
  if (!try_resize_lock(thread)) {
    st->print_cr("statistics unavailable at this moment");
    return;
  }

  TableStatistics ts = statistics_calculate(thread, vs_f);
  unlock_resize_lock(thread);

  ts.print(st, table_name);
}

template <typename CONFIG, MEMFLAGS F>
inline bool ConcurrentHashTable<CONFIG, F>::
  try_move_nodes_to(Thread* thread, ConcurrentHashTable<CONFIG, F>* to_cht)
{
  if (!try_resize_lock(thread)) {
    return false;
  }
  assert(_new_table == NULL || _new_table == POISON_PTR, "Must be NULL");
  for (size_t bucket_it = 0; bucket_it < _table->_size; bucket_it++) {
    Bucket* bucket = _table->get_bucket(bucket_it);
    assert(!bucket->have_redirect() && !bucket->is_locked(), "Table must be uncontended");
    while (bucket->first() != NULL) {
      Node* move_node = bucket->first();
      bool ok = bucket->cas_first(move_node->next(), move_node);
      assert(ok, "Uncontended cas must work");
      bool dead_hash = false;
      size_t insert_hash = CONFIG::get_hash(*move_node->value(), &dead_hash);
      if (!dead_hash) {
        Bucket* insert_bucket = to_cht->get_bucket(insert_hash);
        assert(!bucket->have_redirect() && !bucket->is_locked(), "Not bit should be present");
        move_node->set_next(insert_bucket->first());
        ok = insert_bucket->cas_first(move_node, insert_bucket->first());
        assert(ok, "Uncontended cas must work");
      }
    }
  }
  unlock_resize_lock(thread);
  return true;
}

#endif // SHARE_UTILITIES_CONCURRENTHASHTABLE_INLINE_HPP
