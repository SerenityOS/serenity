/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CARDSETMEMORY_HPP
#define SHARE_GC_G1_G1CARDSETMEMORY_HPP

#include "gc/g1/g1CardSet.hpp"
#include "gc/g1/g1CardSetContainers.hpp"
#include "memory/allocation.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/lockFreeStack.hpp"

class G1CardSetConfiguration;
class outputStream;

// Collects G1CardSetAllocator options/heuristics. Called by G1CardSetAllocator
// to determine the next size of the allocated G1CardSetBuffer.
class G1CardSetAllocOptions {
  uint _elem_size;
  uint _initial_num_elems;
  // Defines a limit to the number of elements in the buffer
  uint _max_num_elems;

  uint exponential_expand(uint prev_num_elems) {
    return clamp(prev_num_elems * 2, _initial_num_elems, _max_num_elems);
  }

public:
  static const uint BufferAlignment = 8;
  static const uint MinimumBufferSize = 8;
  static const uint MaximumBufferSize =  UINT_MAX / 2;

  G1CardSetAllocOptions(uint elem_size, uint initial_num_elems = MinimumBufferSize, uint max_num_elems = MaximumBufferSize) :
    _elem_size(align_up(elem_size, BufferAlignment)),
    _initial_num_elems(initial_num_elems),
    _max_num_elems(max_num_elems) {
  }

  uint next_num_elems(uint prev_num_elems) {
    return exponential_expand(prev_num_elems);
  }

  uint elem_size () const {return _elem_size;}
};

// A single buffer/arena containing _num_elems blocks of memory of _elem_size.
// G1CardSetBuffers can be linked together using a singly linked list.
class G1CardSetBuffer : public CHeapObj<mtGCCardSet> {
  uint _elem_size;
  uint _num_elems;

  G1CardSetBuffer* volatile _next;

  char* _buffer;  // Actual data.

  // Index into the next free block to allocate into. Full if equal (or larger)
  // to _num_elems (can be larger because we atomically increment this value and
  // check only afterwards if the allocation has been successful).
  uint volatile _next_allocate;

public:
  G1CardSetBuffer(uint elem_size, uint num_elems, G1CardSetBuffer* next);
  ~G1CardSetBuffer();

  G1CardSetBuffer* volatile* next_addr() { return &_next; }

  void* get_new_buffer_elem();

  uint num_elems() const { return _num_elems; }

  G1CardSetBuffer* next() const { return _next; }

  void set_next(G1CardSetBuffer* next) {
    assert(next != this, " loop condition");
    _next = next;
  }

  void reset(G1CardSetBuffer* next) {
    _next_allocate = 0;
    assert(next != this, " loop condition");
    set_next(next);
    memset((void*)_buffer, 0, (size_t)_num_elems * _elem_size);
  }

  uint elem_size() const { return _elem_size; }

  size_t mem_size() const { return sizeof(*this) + (size_t)_num_elems * _elem_size; }

  bool is_full() const { return _next_allocate >= _num_elems; }
};

// Set of (free) G1CardSetBuffers. The assumed usage is that allocation
// to it and removal of elements is strictly separate, but every action may be
// performed by multiple threads at the same time.
// Counts and memory usage are current on a best-effort basis if accessed concurrently.
class G1CardSetBufferList {
  static G1CardSetBuffer* volatile* next_ptr(G1CardSetBuffer& node) {
    return node.next_addr();
  }
  typedef LockFreeStack<G1CardSetBuffer, &next_ptr> NodeStack;

  NodeStack _list;

  volatile size_t _num_buffers;
  volatile size_t _mem_size;

public:
  G1CardSetBufferList() : _list(), _num_buffers(0), _mem_size(0) { }
  ~G1CardSetBufferList() { free_all(); }

  void bulk_add(G1CardSetBuffer& first, G1CardSetBuffer& last, size_t num, size_t mem_size);
  void add(G1CardSetBuffer& elem) { _list.prepend(elem); }

  G1CardSetBuffer* get();
  G1CardSetBuffer* get_all(size_t& num_buffers, size_t& mem_size);

  // Give back all memory to the OS.
  void free_all();

  void print_on(outputStream* out, const char* prefix = "");

  size_t num_buffers() const { return Atomic::load(&_num_buffers); }
  size_t mem_size() const { return Atomic::load(&_mem_size); }
};

// Arena-like allocator for (card set) heap memory objects (Elem elements).
//
// Actual allocation from the C heap occurs on G1CardSetBuffer basis, i.e. sets
// of elements. The assumed allocation pattern for these G1CardSetBuffer elements
// is assumed to be strictly two-phased:
//
// - in the first phase, G1CardSetBuffers are allocated from the C heap (or a free
// list given at initialization time). This allocation may occur in parallel. This
// typically corresponds to a single mutator phase, but may extend over multiple.
//
// - in the second phase, G1CardSetBuffers are given back in bulk to the free list.
// This is typically done during a GC pause.
//
// Some third party is responsible for giving back memory from the free list to
// the operating system.
//
// Allocation and deallocation in the first phase on G1CardSetContainer basis
// may occur by multiple threads at once.
//
// Allocation occurs from an internal free list of G1CardSetContainers first,
// only then trying to bump-allocate from the current G1CardSetBuffer. If there is
// none, this class allocates a new G1CardSetBuffer (allocated from the C heap,
// asking the G1CardSetAllocOptions instance about sizes etc) and uses that one.
//
// The G1CardSetContainerOnHeaps free list is a linked list of G1CardSetContainers
// within all G1CardSetBuffer instances allocated so far. It uses a separate
// pending list and global synchronization to avoid the ABA problem when the
// user frees a memory object.
//
// The class also manages a few counters for statistics using atomic operations.
// Their values are only consistent within each other with extra global
// synchronization.
//
// Since it is expected that every CardSet (and in extension each region) has its
// own set of allocators, there is intentionally no padding between them to save
// memory.
template <class Elem>
class G1CardSetAllocator {
  // G1CardSetBuffer management.

  // G1CardSetAllocOptions provides parameters for allocation buffer
  // sizing and expansion.
  G1CardSetAllocOptions _alloc_options;

  G1CardSetBuffer* volatile _first;       // The (start of the) list of all buffers.
  G1CardSetBuffer* _last;                 // The last element of the list of all buffers.
  volatile uint _num_buffers;             // Number of assigned buffers to this allocator.
  volatile size_t _mem_size;              // Memory used by all buffers.

  G1CardSetBufferList* _free_buffer_list; // The global free buffer list to
                                          // preferentially get new buffers from.

  // G1CardSetContainer node management within the G1CardSetBuffers allocated
  // by this allocator.

  static G1CardSetContainer* volatile* next_ptr(G1CardSetContainer& node);
  typedef LockFreeStack<G1CardSetContainer, &G1CardSetAllocator::next_ptr> NodeStack;

  volatile bool _transfer_lock;
  NodeStack _free_nodes_list;
  NodeStack _pending_nodes_list;

  volatile uint _num_pending_nodes;   // Number of nodes in the pending list.
  volatile uint _num_free_nodes;      // Number of nodes in the free list.

  volatile uint _num_allocated_nodes; // Number of total nodes allocated and in use.
  volatile uint _num_available_nodes; // Number of nodes available in all buffers (allocated + free + pending + not yet used).

  // Try to transfer nodes from _pending_nodes_list to _free_nodes_list, with a
  // synchronization delay for any in-progress pops from the _free_nodes_list
  // to solve ABA here.
  bool try_transfer_pending();

  uint num_free_elems() const;

  G1CardSetBuffer* create_new_buffer(G1CardSetBuffer* const prev);

  uint elem_size() const { return _alloc_options.elem_size(); }

public:
  G1CardSetAllocator(const char* name,
                     const G1CardSetAllocOptions& buffer_options,
                     G1CardSetBufferList* free_buffer_list);
  ~G1CardSetAllocator() {
    drop_all();
  }

  Elem* allocate();
  void free(Elem* elem);

  // Deallocate all buffers to the free buffer list and reset this allocator. Must
  // be called in a globally synchronized area.
  void drop_all();

  uint num_buffers() const;

  size_t mem_size() const {
    return sizeof(*this) +
      num_buffers() * sizeof(G1CardSetBuffer) + (size_t)_num_available_nodes * elem_size();
  }

  size_t wasted_mem_size() const {
    return ((size_t)_num_available_nodes - (_num_allocated_nodes - _num_pending_nodes)) * elem_size();
  }

  void print(outputStream* os);
};

// Statistics for a fixed set of buffer lists. Contains the number of buffers and memory
// used for each. Note that statistics are typically not taken atomically so there
// can be inconsistencies. The user must be prepared for them.
class G1CardSetMemoryStats {
public:

  size_t _num_mem_sizes[G1CardSetConfiguration::num_mem_object_types()];
  size_t _num_buffers[G1CardSetConfiguration::num_mem_object_types()];

  // Returns all-zero statistics.
  G1CardSetMemoryStats();
  // For every element in the set (indicated by i), call fn to provide the
  // memory size and number of buffers for that i'th buffer list.
  G1CardSetMemoryStats(void (*fn)(const void* context, uint i, size_t& mem_size, size_t& num_buffers), const void* context);

  void add(G1CardSetMemoryStats const other) {
    STATIC_ASSERT(ARRAY_SIZE(_num_buffers) == ARRAY_SIZE(_num_mem_sizes));
    for (uint i = 0; i < ARRAY_SIZE(_num_mem_sizes); i++) {
      _num_mem_sizes[i] += other._num_mem_sizes[i];
      _num_buffers[i] += other._num_buffers[i];
    }
  }

  void clear();

  uint num_pools() const { return G1CardSetConfiguration::num_mem_object_types(); }
};

// A set of free lists holding memory buffers for use by G1CardSetAllocators.
class G1CardSetFreePool {
  // The global free pool.
  static G1CardSetFreePool _freelist_pool;

  uint _num_free_lists;
  G1CardSetBufferList* _free_lists;

public:
  static G1CardSetFreePool* free_list_pool() { return &_freelist_pool; }
  static G1CardSetMemoryStats free_list_sizes() { return _freelist_pool.memory_sizes(); }

  class G1ReturnMemoryProcessor;
  typedef GrowableArrayCHeap<G1ReturnMemoryProcessor*, mtGC> G1ReturnMemoryProcessorSet;

  static void update_unlink_processors(G1ReturnMemoryProcessorSet* unlink_processors);

  explicit G1CardSetFreePool(uint num_free_lists);
  ~G1CardSetFreePool();

  G1CardSetBufferList* free_list(uint i) {
    assert(i < _num_free_lists, "must be");
    return &_free_lists[i];
  }

  uint num_free_lists() const { return _num_free_lists; }

  // Return sizes for free list i in this free list pool.
  void get_size(uint i, size_t& mem_size, size_t& num_buffers) const;

  G1CardSetMemoryStats memory_sizes() const;
  size_t mem_size() const;

  void print_on(outputStream* out);
};

// Data structure containing current in-progress state for returning memory to the
// operating system for a single G1CardSetBufferList.
class G1CardSetFreePool::G1ReturnMemoryProcessor : public CHeapObj<mtGC> {
  G1CardSetBufferList* _source;
  size_t _return_to_vm_size;

  G1CardSetBuffer* _first;
  size_t _unlinked_bytes;
  size_t _num_unlinked;

public:
  explicit G1ReturnMemoryProcessor(size_t return_to_vm) :
    _source(nullptr), _return_to_vm_size(return_to_vm), _first(nullptr), _unlinked_bytes(0), _num_unlinked(0) {
  }

  // Updates the instance members about the given card set buffer list for the purpose
  // of giving back memory. Only necessary members are updated, e.g. if there is
  // nothing to return to the VM, do not set the source list.
  void visit_free_list(G1CardSetBufferList* source);

  bool finished_return_to_vm() const { return _return_to_vm_size == 0; }
  bool finished_return_to_os() const { return _first == nullptr; }

  // Returns memory to the VM until the given deadline expires. Returns true if
  // there is no more work. Guarantees forward progress, i.e. at least one buffer
  // has been processed after returning.
  // return_to_vm() re-adds buffers to the respective free list.
  bool return_to_vm(jlong deadline);
  // Returns memory to the VM until the given deadline expires. Returns true if
  // there is no more work. Guarantees forward progress, i.e. at least one buffer
  // has been processed after returning.
  // return_to_os() gives back buffers to the OS.
  bool return_to_os(jlong deadline);
};

class G1CardSetMemoryManager : public CHeapObj<mtGCCardSet> {
  G1CardSetConfiguration* _config;

  G1CardSetAllocator<G1CardSetContainer>* _allocators;

  uint num_mem_object_types() const;
public:
  G1CardSetMemoryManager(G1CardSetConfiguration* config,
                         G1CardSetFreePool* free_list_pool);

  virtual ~G1CardSetMemoryManager();

  // Allocate and free a memory object of given type.
  inline uint8_t* allocate(uint type);
  void free(uint type, void* value);

  // Allocate and free a hash table node.
  inline uint8_t* allocate_node();
  inline void free_node(void* value);

  void flush();

  void print(outputStream* os);

  size_t mem_size() const;
  size_t wasted_mem_size() const;

  G1CardSetMemoryStats memory_stats() const;
};

#endif // SHARE_GC_G1_G1CARDSETMEMORY_HPP
