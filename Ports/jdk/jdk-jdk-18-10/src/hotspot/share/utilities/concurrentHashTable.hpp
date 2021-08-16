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

#ifndef SHARE_UTILITIES_CONCURRENTHASHTABLE_HPP
#define SHARE_UTILITIES_CONCURRENTHASHTABLE_HPP

#include "memory/allocation.hpp"
#include "utilities/globalCounter.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/tableStatistics.hpp"

// A mostly concurrent-hash-table where the read-side is wait-free, inserts are
// CAS and deletes mutual exclude each other on per bucket-basis. VALUE is the
// type kept inside each Node and CONFIG contains hash and allocation methods.
// A CALLBACK_FUNC and LOOKUP_FUNC needs to be provided for get and insert.

class Thread;
class Mutex;

template <typename CONFIG, MEMFLAGS F>
class ConcurrentHashTable : public CHeapObj<F> {
  typedef typename CONFIG::Value VALUE;
 private:
  // This is the internal node structure.
  // Only constructed with placement new from memory allocated with MEMFLAGS of
  // the InternalTable or user-defined memory.
  class Node {
   private:
    Node * volatile _next;
    VALUE _value;
   public:
    Node(const VALUE& value, Node* next = NULL)
      : _next(next), _value(value) {
      assert((((uintptr_t)this) & ((uintptr_t)0x3)) == 0,
             "Must 16 bit aligned.");
    }

    Node* next() const;
    void set_next(Node* node)         { _next = node; }
    Node* const volatile * next_ptr() { return &_next; }

    VALUE* value()                    { return &_value; }

    // Creates a node.
    static Node* create_node(void* context, const VALUE& value, Node* next = NULL) {
      return new (CONFIG::allocate_node(context, sizeof(Node), value)) Node(value, next);
    }
    // Destroys a node.
    static void destroy_node(void* context, Node* node) {
      CONFIG::free_node(context, (void*)node, node->_value);
    }

    void print_on(outputStream* st) const {};
    void print_value_on(outputStream* st) const {};
  };

  // Only constructed with placement new from an array allocated with MEMFLAGS
  // of InternalTable.
  class Bucket {
   private:

    // Embedded state in two low bits in first pointer is a spinlock with 3
    // states, unlocked, locked, redirect. You must never busy-spin on trylock()
    // or call lock() without _resize_lock, that would deadlock. Redirect can
    // only be installed by owner and is the final state of a bucket.
    // The only two valid flows are:
    // unlocked -> locked -> unlocked
    // unlocked -> locked -> redirect
    // Locked state only applies to an updater.
    // Reader only check for redirect.
    Node * volatile _first;

    static const uintptr_t STATE_LOCK_BIT     = 0x1;
    static const uintptr_t STATE_REDIRECT_BIT = 0x2;
    static const uintptr_t STATE_MASK         = 0x3;

    // Get the first pointer unmasked.
    Node* first_raw() const;

    // Methods to manipulate the embedded.
    static bool is_state(Node* node, uintptr_t bits) {
      return (bits & (uintptr_t)node) == bits;
    }

    static Node* set_state(Node* n, uintptr_t bits) {
      return (Node*)(bits | (uintptr_t)n);
    }

    static uintptr_t get_state(Node* node) {
      return (((uintptr_t)node) & STATE_MASK);
    }

    static Node* clear_state(Node* node) {
      return (Node*)(((uintptr_t)node) & (~(STATE_MASK)));
    }

    static Node* clear_set_state(Node* node, Node* state) {
      return (Node*)(((uintptr_t)clear_state(node)) ^ get_state(state));
    }

   public:
    // A bucket is only one pointer with the embedded state.
    Bucket() : _first(NULL) {};

    // Get the first pointer unmasked.
    Node* first() const;

    // Get a pointer to the const first pointer. Do not deference this
    // pointer, the pointer pointed to _may_ contain an embedded state. Such
    // pointer should only be used as input to release_assign_node_ptr.
    Node* const volatile * first_ptr() { return &_first; }

    // This is the only place where a pointer to a Node pointer that potentially
    // is _first should be changed. Otherwise we destroy the embedded state. We
    // only give out pointer to const Node pointer to avoid accidental
    // assignment, thus here we must cast const part away. Method is not static
    // due to an assert.
    void release_assign_node_ptr(Node* const volatile * dst, Node* node) const;

    // This method assigns this buckets last Node next ptr to input Node.
    void release_assign_last_node_next(Node* node);

    // Setting the first pointer must be done with CAS.
    bool cas_first(Node *node, Node* expect);

    // Returns true if this bucket is redirecting to a new table.
    // Redirect is a terminal state and will never change.
    bool have_redirect() const;

    // Return true if this bucket is locked for updates.
    bool is_locked() const;

    // Return true if this bucket was locked.
    bool trylock();

    // The bucket might be invalid, due to a concurrent resize. The lock()
    // method do no respect that and can deadlock if caller do not hold
    // _resize_lock.
    void lock();

    // Unlocks this bucket.
    void unlock();

    // Installs redirect in this bucket.
    // Prior to doing so you must have successfully locked this bucket.
    void redirect();
  };

  // The backing storage table holding the buckets and it's size and mask-bits.
  // Table is always a power of two for two reasons:
  // - Re-size can only change the size into half or double
  //   (any pow 2 would also be possible).
  // - Use masking of hash for bucket index.
  class InternalTable : public CHeapObj<F> {
   private:
    Bucket* _buckets;        // Bucket array.
   public:
    const size_t _log2_size; // Size in log2.
    const size_t _size;      // Size in log10.

    // The mask used on hash for selecting bucket.
    // The masked value is guaranteed be to inside the buckets array.
    const size_t _hash_mask;

    // Create a backing table
    InternalTable(size_t log2_size);
    ~InternalTable();

    Bucket* get_buckets() { return _buckets; }
    Bucket* get_bucket(size_t idx) { return &_buckets[idx]; }

    size_t get_mem_size() {
      return sizeof(*this) + _size * sizeof(Bucket);
    }
  };

  // For materializing a supplied value.
  class LazyValueRetrieve {
   private:
    const VALUE& _val;
   public:
    LazyValueRetrieve(const VALUE& val) : _val(val) {}
    const VALUE& operator()() { return _val; }
  };

  void* _context;

  InternalTable* _table;      // Active table.
  InternalTable* _new_table;  // Table we are resizing to.

  // Default sizes
  static const size_t DEFAULT_MAX_SIZE_LOG2 = 21;
  static const size_t DEFAULT_START_SIZE_LOG2 = 13;
  static const size_t DEFAULT_GROW_HINT = 4; // Chain length

  const size_t _log2_size_limit;  // The biggest size.
  const size_t _log2_start_size;  // Start size.
  const size_t _grow_hint;        // Number of linked items

  volatile bool _size_limit_reached;

  // We serialize resizers and other bulk operations which do not support
  // concurrent resize with this lock.
  Mutex* _resize_lock;
  // Since we need to drop mutex for safepoints, but stop other threads from
  // taking the mutex after a safepoint this bool is the actual state. After
  // acquiring the mutex you must check if this is already locked. If so you
  // must drop the mutex until the real lock holder grabs the mutex.
  volatile Thread* _resize_lock_owner;

  // Return true if lock mutex/state succeeded.
  bool try_resize_lock(Thread* locker);
  // Returns when both mutex and state are proper locked.
  void lock_resize_lock(Thread* locker);
  // Unlocks mutex and state.
  void unlock_resize_lock(Thread* locker);

  // This method sets the _invisible_epoch and do a write_synchronize.
  // Subsequent calls check the state of _invisible_epoch and determine if the
  // write_synchronize can be avoided. If not, it sets the _invisible_epoch
  // again and do a write_synchronize.
  void write_synchonize_on_visible_epoch(Thread* thread);
  // To be-able to avoid write_synchronize in resize and other bulk operation,
  // this field keep tracks if a version of the hash-table was ever been seen.
  // We the working thread pointer as tag for debugging. The _invisible_epoch
  // can only be used by the owner of _resize_lock.
  volatile Thread* _invisible_epoch;

  // Scoped critical section, which also handles the invisible epochs.
  // An invisible epoch/version do not need a write_synchronize().
  class ScopedCS: public StackObj {
   protected:
    Thread* _thread;
    ConcurrentHashTable<CONFIG, F>* _cht;
    GlobalCounter::CSContext _cs_context;
   public:
    ScopedCS(Thread* thread, ConcurrentHashTable<CONFIG, F>* cht);
    ~ScopedCS();
  };


  // Max number of deletes in one bucket chain during bulk delete.
  static const size_t BULK_DELETE_LIMIT = 256;

  // Simple getters and setters for the internal table.
  InternalTable* get_table() const;
  InternalTable* get_new_table() const;
  InternalTable* set_table_from_new();

  // Destroys all nodes.
  void free_nodes();

  // Mask away high bits of hash.
  static size_t bucket_idx_hash(InternalTable* table, const uintx hash) {
    return ((size_t)hash) & table->_hash_mask;
  }

  // Returns bucket for hash for that internal table.
  Bucket* get_bucket_in(InternalTable* table, const uintx hash) const {
    size_t bucket_index = bucket_idx_hash(table, hash);
    return table->get_bucket(bucket_index);
  }

  // Return correct bucket for reading and handles resizing.
  Bucket* get_bucket(const uintx hash) const;

  // Return correct bucket for updates and handles resizing.
  Bucket* get_bucket_locked(Thread* thread, const uintx hash);

  // Finds a node.
  template <typename LOOKUP_FUNC>
  Node* get_node(const Bucket* const bucket, LOOKUP_FUNC& lookup_f,
                 bool* have_dead, size_t* loops = NULL) const;

  // Method for shrinking.
  bool internal_shrink_prolog(Thread* thread, size_t log2_size);
  void internal_shrink_epilog(Thread* thread);
  void internal_shrink_range(Thread* thread, size_t start, size_t stop);
  bool internal_shrink(Thread* thread, size_t size_limit_log2);
  void internal_reset(size_t log2_size);

  // Methods for growing.
  bool unzip_bucket(Thread* thread, InternalTable* old_table,
                    InternalTable* new_table, size_t even_index,
                    size_t odd_index);
  bool internal_grow_prolog(Thread* thread, size_t log2_size);
  void internal_grow_epilog(Thread* thread);
  void internal_grow_range(Thread* thread, size_t start, size_t stop);
  bool internal_grow(Thread* thread, size_t log2_size);

  // Get a value.
  template <typename LOOKUP_FUNC>
  VALUE* internal_get(Thread* thread, LOOKUP_FUNC& lookup_f,
                      bool* grow_hint = NULL);

  // Insert and get current value.
  template <typename LOOKUP_FUNC, typename FOUND_FUNC>
  bool internal_insert_get(Thread* thread, LOOKUP_FUNC& lookup_f, const VALUE& value,
                           FOUND_FUNC& foundf, bool* grow_hint, bool* clean_hint);

  // Returns true if an item matching LOOKUP_FUNC is removed.
  // Calls DELETE_FUNC before destroying the node.
  template <typename LOOKUP_FUNC, typename DELETE_FUNC>
  bool internal_remove(Thread* thread, LOOKUP_FUNC& lookup_f,
                       DELETE_FUNC& delete_f);

  // Visits nodes with FUNC.
  template <typename FUNC>
  static bool visit_nodes(Bucket* bucket, FUNC& visitor_f);

  // During shrink/grow we cannot guarantee that we only visit nodes once, with
  // current algorithm. To keep it simple caller will have locked
  // _resize_lock.
  template <typename FUNC>
  void do_scan_locked(Thread* thread, FUNC& scan_f);

  // Check for dead items in a bucket.
  template <typename EVALUATE_FUNC>
  size_t delete_check_nodes(Bucket* bucket, EVALUATE_FUNC& eval_f,
                            size_t num_del, Node** ndel);

  // Check for dead items in this table. During shrink/grow we cannot guarantee
  // that we only visit nodes once. To keep it simple caller will have locked
  // _resize_lock.
  template <typename EVALUATE_FUNC, typename DELETE_FUNC>
  void do_bulk_delete_locked(Thread* thread, EVALUATE_FUNC& eval_f
                             , DELETE_FUNC& del_f) {
    do_bulk_delete_locked_for(thread, 0, _table->_size, eval_f, del_f);
  }

  // To have prefetching for a VALUE that is pointer during
  // do_bulk_delete_locked, we have this helper classes. One for non-pointer
  // case without prefect and one for pointer with prefect.
  template <bool b, typename EVALUATE_FUNC>
  struct HaveDeletables {
    static bool have_deletable(Bucket* bucket, EVALUATE_FUNC& eval_f,
                               Bucket* prefetch_bucket);
  };
  template<typename EVALUATE_FUNC>
  struct HaveDeletables<true, EVALUATE_FUNC> {
    static bool have_deletable(Bucket* bucket, EVALUATE_FUNC& eval_f,
                               Bucket* prefetch_bucket);
  };

  // Check for dead items in this table with range. During shrink/grow we cannot
  // guarantee that we only visit nodes once. To keep it simple caller will
  // have locked _resize_lock.
  template <typename EVALUATE_FUNC, typename DELETE_FUNC>
  void do_bulk_delete_locked_for(Thread* thread, size_t start_idx,
                                 size_t stop_idx, EVALUATE_FUNC& eval_f,
                                 DELETE_FUNC& del_f, bool is_mt = false);

  // Method to delete one items.
  template <typename LOOKUP_FUNC>
  void delete_in_bucket(Thread* thread, Bucket* bucket, LOOKUP_FUNC& lookup_f);

 public:
  ConcurrentHashTable(size_t log2size = DEFAULT_START_SIZE_LOG2,
                      size_t log2size_limit = DEFAULT_MAX_SIZE_LOG2,
                      size_t grow_hint = DEFAULT_GROW_HINT,
                      void* context = NULL);

  explicit ConcurrentHashTable(void* context, size_t log2size = DEFAULT_START_SIZE_LOG2) :
    ConcurrentHashTable(log2size, DEFAULT_MAX_SIZE_LOG2, DEFAULT_GROW_HINT, context) {}

  ~ConcurrentHashTable();

  TableRateStatistics _stats_rate;

  size_t get_mem_size(Thread* thread);

  size_t get_size_log2(Thread* thread);
  static size_t get_node_size() { return sizeof(Node); }
  bool is_max_size_reached() { return _size_limit_reached; }

  // This means no paused bucket resize operation is going to resume
  // on this table.
  bool is_safepoint_safe() { return _resize_lock_owner == NULL; }

  // Re-size operations.
  bool shrink(Thread* thread, size_t size_limit_log2 = 0);
  bool grow(Thread* thread, size_t size_limit_log2 = 0);
  // Unsafe reset and resize the table. This method assumes that we
  // want to clear and maybe resize the internal table without the
  // overhead of clearing individual items in the table.
  void unsafe_reset(size_t size_log2 = 0);

  // All callbacks for get are under critical sections. Other callbacks may be
  // under critical section or may have locked parts of table. Calling any
  // methods on the table during a callback is not supported.Only MultiGetHandle
  // supports multiple gets.

  // Get methods return true on found item with LOOKUP_FUNC and FOUND_FUNC is
  // called.
  template <typename LOOKUP_FUNC, typename FOUND_FUNC>
  bool get(Thread* thread, LOOKUP_FUNC& lookup_f, FOUND_FUNC& foundf,
           bool* grow_hint = NULL);

  // Returns true true if the item was inserted, duplicates are found with
  // LOOKUP_FUNC.
  template <typename LOOKUP_FUNC>
  bool insert(Thread* thread, LOOKUP_FUNC& lookup_f, const VALUE& value,
              bool* grow_hint = NULL, bool* clean_hint = NULL) {
    struct NOP {
        void operator()(...) const {}
    } nop;
    return internal_insert_get(thread, lookup_f, value, nop, grow_hint, clean_hint);
  }

  // Returns true if the item was inserted, duplicates are found with
  // LOOKUP_FUNC then FOUND_FUNC is called.
  template <typename LOOKUP_FUNC, typename FOUND_FUNC>
  bool insert_get(Thread* thread, LOOKUP_FUNC& lookup_f, VALUE& value, FOUND_FUNC& foundf,
                  bool* grow_hint = NULL, bool* clean_hint = NULL) {
    return internal_insert_get(thread, lookup_f, value, foundf, grow_hint, clean_hint);
  }

  // This does a fast unsafe insert and can thus only be used when there is no
  // risk for a duplicates and no other threads uses this table.
  bool unsafe_insert(const VALUE& value);

  // Returns true if items was deleted matching LOOKUP_FUNC and
  // prior to destruction DELETE_FUNC is called.
  template <typename LOOKUP_FUNC, typename DELETE_FUNC>
  bool remove(Thread* thread, LOOKUP_FUNC& lookup_f, DELETE_FUNC& del_f) {
    return internal_remove(thread, lookup_f, del_f);
  }

  // Same without DELETE_FUNC.
  template <typename LOOKUP_FUNC>
  bool remove(Thread* thread, LOOKUP_FUNC& lookup_f) {
    struct {
      void operator()(VALUE*) {}
    } ignore_del_f;
    return internal_remove(thread, lookup_f, ignore_del_f);
  }

  // Visit all items with SCAN_FUNC if no concurrent resize. Takes the resize
  // lock to avoid concurrent resizes. Else returns false.
  template <typename SCAN_FUNC>
  bool try_scan(Thread* thread, SCAN_FUNC& scan_f);

  // Visit all items with SCAN_FUNC when the resize lock is obtained.
  template <typename SCAN_FUNC>
  void do_scan(Thread* thread, SCAN_FUNC& scan_f);

  // Visit all items with SCAN_FUNC without any protection.
  // It will assume there is no other thread accessing this
  // table during the safepoint. Must be called with VM thread.
  template <typename SCAN_FUNC>
  void do_safepoint_scan(SCAN_FUNC& scan_f);

  // Destroying items matching EVALUATE_FUNC, before destroying items
  // DELETE_FUNC is called, if resize lock is obtained. Else returns false.
  template <typename EVALUATE_FUNC, typename DELETE_FUNC>
  bool try_bulk_delete(Thread* thread, EVALUATE_FUNC& eval_f,
                       DELETE_FUNC& del_f);

  // Destroying items matching EVALUATE_FUNC, before destroying items
  // DELETE_FUNC is called, when the resize lock is successfully obtained.
  template <typename EVALUATE_FUNC, typename DELETE_FUNC>
  void bulk_delete(Thread* thread, EVALUATE_FUNC& eval_f, DELETE_FUNC& del_f);

  // Calcuate statistics. Item sizes are calculated with VALUE_SIZE_FUNC.
  template <typename VALUE_SIZE_FUNC>
  TableStatistics statistics_calculate(Thread* thread, VALUE_SIZE_FUNC& vs_f);

  // Gets statistics if available, if not return old one. Item sizes are calculated with
  // VALUE_SIZE_FUNC.
  template <typename VALUE_SIZE_FUNC>
  TableStatistics statistics_get(Thread* thread, VALUE_SIZE_FUNC& vs_f, TableStatistics old);

  // Writes statistics to the outputStream. Item sizes are calculated with
  // VALUE_SIZE_FUNC.
  template <typename VALUE_SIZE_FUNC>
  void statistics_to(Thread* thread, VALUE_SIZE_FUNC& vs_f, outputStream* st,
                     const char* table_name);

  // Moves all nodes from this table to to_cht
  bool try_move_nodes_to(Thread* thread, ConcurrentHashTable<CONFIG, F>* to_cht);

  // Scoped multi getter.
  class MultiGetHandle : private ScopedCS {
   public:
    MultiGetHandle(Thread* thread, ConcurrentHashTable<CONFIG, F>* cht)
      : ScopedCS(thread, cht) {}
    // In the MultiGetHandle scope you can lookup items matching LOOKUP_FUNC.
    // The VALUEs are safe as long as you never save the VALUEs outside the
    // scope, e.g. after ~MultiGetHandle().
    template <typename LOOKUP_FUNC>
    VALUE* get(LOOKUP_FUNC& lookup_f, bool* grow_hint = NULL);
  };

 private:
  class BucketsOperation;

 public:
  class BulkDeleteTask;
  class GrowTask;
};

#endif // SHARE_UTILITIES_CONCURRENTHASHTABLE_HPP
