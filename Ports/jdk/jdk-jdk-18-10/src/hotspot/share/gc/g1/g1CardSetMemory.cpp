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

#include "precompiled.hpp"

#include "gc/g1/g1CardSetMemory.inline.hpp"
#include "logging/log.hpp"
#include "runtime/atomic.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/ostream.hpp"

G1CardSetBuffer::G1CardSetBuffer(uint elem_size, uint num_instances, G1CardSetBuffer* next) :
    _elem_size(elem_size), _num_elems(num_instances), _next(next), _next_allocate(0) {

  _buffer = NEW_C_HEAP_ARRAY(char, (size_t)_num_elems * elem_size, mtGCCardSet);
}

G1CardSetBuffer::~G1CardSetBuffer() {
  FREE_C_HEAP_ARRAY(mtGCCardSet, _buffer);
}

void* G1CardSetBuffer::get_new_buffer_elem() {
  if (_next_allocate >= _num_elems) {
    return nullptr;
  }
  uint result = Atomic::fetch_and_add(&_next_allocate, 1u, memory_order_relaxed);
  if (result >= _num_elems) {
    return nullptr;
  }
  void* r = _buffer + (uint)result * _elem_size;
  return r;
}

void G1CardSetBufferList::bulk_add(G1CardSetBuffer& first, G1CardSetBuffer& last, size_t num, size_t mem_size) {
  _list.prepend(first, last);
  Atomic::add(&_num_buffers, num, memory_order_relaxed);
  Atomic::add(&_mem_size, mem_size, memory_order_relaxed);
}

void G1CardSetBufferList::print_on(outputStream* out, const char* prefix) {
  out->print_cr("%s: buffers %zu size %zu", prefix, Atomic::load(&_num_buffers), Atomic::load(&_mem_size));
}

G1CardSetBuffer* G1CardSetBufferList::get() {
  GlobalCounter::CriticalSection cs(Thread::current());

  G1CardSetBuffer* result = _list.pop();
  if (result != nullptr) {
    Atomic::dec(&_num_buffers, memory_order_relaxed);
    Atomic::sub(&_mem_size, result->mem_size(), memory_order_relaxed);
  }
  return result;
}

G1CardSetBuffer* G1CardSetBufferList::get_all(size_t& num_buffers, size_t& mem_size) {
  GlobalCounter::CriticalSection cs(Thread::current());

  G1CardSetBuffer* result = _list.pop_all();
  num_buffers = Atomic::load(&_num_buffers);
  mem_size = Atomic::load(&_mem_size);

  if (result != nullptr) {
    Atomic::sub(&_num_buffers, num_buffers, memory_order_relaxed);
    Atomic::sub(&_mem_size, mem_size, memory_order_relaxed);
  }
  return result;
}

void G1CardSetBufferList::free_all() {
  size_t num_freed = 0;
  size_t mem_size_freed = 0;
  G1CardSetBuffer* cur;

  while ((cur = _list.pop()) != nullptr) {
    mem_size_freed += cur->mem_size();
    num_freed++;
    delete cur;
  }

  Atomic::sub(&_num_buffers, num_freed, memory_order_relaxed);
  Atomic::sub(&_mem_size, mem_size_freed, memory_order_relaxed);
}

template <class Elem>
G1CardSetAllocator<Elem>::G1CardSetAllocator(const char* name,
                                             const G1CardSetAllocOptions& buffer_options,
                                             G1CardSetBufferList* free_buffer_list) :
  _alloc_options(buffer_options),
  _first(nullptr),
  _last(nullptr),
  _num_buffers(0),
  _mem_size(0),
  _free_buffer_list(free_buffer_list),
  _transfer_lock(false),
  _free_nodes_list(),
  _pending_nodes_list(),
  _num_pending_nodes(0),
  _num_free_nodes(0),
  _num_allocated_nodes(0),
  _num_available_nodes(0)
{
  assert(elem_size() >= sizeof(G1CardSetContainer), "Element instance size %u for allocator %s too small",
         elem_size(), name);
  assert(_free_buffer_list != nullptr, "precondition!");
}

template <class Elem>
bool G1CardSetAllocator<Elem>::try_transfer_pending() {
  // Attempt to claim the lock.
  if (Atomic::load_acquire(&_transfer_lock) || // Skip CAS if likely to fail.
      Atomic::cmpxchg(&_transfer_lock, false, true)) {
    return false;
  }
  // Have the lock; perform the transfer.

  // Claim all the pending nodes.
  G1CardSetContainer* first = _pending_nodes_list.pop_all();

  if (first != nullptr) {
    // Prepare to add the claimed nodes, and update _num_pending_nodes.
    G1CardSetContainer* last = first;
    Atomic::load_acquire(&_num_pending_nodes);

    uint count = 1;
    for (G1CardSetContainer* next = first->next(); next != nullptr; next = next->next()) {
      last = next;
      ++count;
    }

    Atomic::sub(&_num_pending_nodes, count);

    // Wait for any in-progress pops to avoid ABA for them.
    GlobalCounter::write_synchronize();
    // Add synchronized nodes to _free_node_list.
    // Update count first so there can be no underflow in allocate().
    Atomic::add(&_num_free_nodes, count);
    _free_nodes_list.prepend(*first, *last);
  }
  Atomic::release_store(&_transfer_lock, false);
  return true;
}

template <class Elem>
void G1CardSetAllocator<Elem>::free(Elem* elem) {
  assert(elem != nullptr, "precondition");
  assert(elem_size() >= sizeof(G1CardSetContainer), "size mismatch");
  // Desired minimum transfer batch size.  There is relatively little
  // importance to the specific number.  It shouldn't be too big, else
  // we're wasting space when the release rate is low.  If the release
  // rate is high, we might accumulate more than this before being
  // able to start a new transfer, but that's okay.  Also note that
  // the allocation rate and the release rate are going to be fairly
  // similar, due to how the buffers are used. - kbarret
  uint const trigger_transfer = 10;

  uint pending_count = Atomic::add(&_num_pending_nodes, 1u, memory_order_relaxed);

  G1CardSetContainer* node =  reinterpret_cast<G1CardSetContainer*>(reinterpret_cast<char*>(elem));

  node->set_next(nullptr);
  assert(node->next() == nullptr, "precondition");

  _pending_nodes_list.push(*node);

  if (pending_count > trigger_transfer) {
    try_transfer_pending();
  }
}

template <class Elem>
void G1CardSetAllocator<Elem>::drop_all() {
  _free_nodes_list.pop_all();
  _pending_nodes_list.pop_all();
  G1CardSetBuffer* cur = Atomic::load_acquire(&_first);

  if (cur != nullptr) {
    assert(_last != nullptr, "If there is at least one element, there must be a last one.");

    G1CardSetBuffer* first = cur;
#ifdef ASSERT
    // Check list consistency.
    G1CardSetBuffer* last = cur;
    uint num_buffers = 0;
    size_t mem_size = 0;
    while (cur != nullptr) {
      mem_size += cur->mem_size();
      num_buffers++;

      G1CardSetBuffer* next = cur->next();
      last = cur;
      cur = next;
    }
#endif
    assert(num_buffers == _num_buffers, "Buffer count inconsistent %u %u", num_buffers, _num_buffers);
    assert(mem_size == _mem_size, "Memory size inconsistent");
    assert(last == _last, "Inconsistent last element");

    _free_buffer_list->bulk_add(*first, *_last, _num_buffers, _mem_size);
  }

  _first = nullptr;
  _last = nullptr;
  _num_available_nodes = 0;
  _num_allocated_nodes = 0;
  _num_pending_nodes = 0;
  _num_buffers = 0;
  _mem_size = 0;
  _num_free_nodes = 0;
}

template <class Elem>
void G1CardSetAllocator<Elem>::print(outputStream* os) {
  os->print("MA " PTR_FORMAT ": %u elems pending (allocated %u available %u) used %.3f highest %u buffers %u size %zu ",
                p2i(this), _num_pending_nodes, _num_allocated_nodes, _num_available_nodes, percent_of(_num_allocated_nodes - _num_pending_nodes, _num_available_nodes), _first != nullptr ? _first->num_elems() : 0, _num_buffers, mem_size());
}

G1CardSetMemoryStats::G1CardSetMemoryStats() {
  clear();
}

G1CardSetMemoryStats::G1CardSetMemoryStats(void(*fn)(const void*,uint,size_t&,size_t&), const void* context) {
  clear();
  for (uint i = 0; i < num_pools(); i++) {
    fn(context, i, _num_mem_sizes[i], _num_buffers[i]);
  }
}

void G1CardSetMemoryStats::clear() {
  for (uint i = 0; i < num_pools(); i++) {
    _num_mem_sizes[i] = 0;
    _num_buffers[i] = 0;
  }
}

void G1CardSetFreePool::update_unlink_processors(G1ReturnMemoryProcessorSet* unlink_processor) {
  uint num_free_lists = _freelist_pool.num_free_lists();

  for (uint i = 0; i < num_free_lists; i++) {
    unlink_processor->at(i)->visit_free_list(_freelist_pool.free_list(i));
  }
}

void G1CardSetFreePool::G1ReturnMemoryProcessor::visit_free_list(G1CardSetBufferList* source) {
  assert(_source == nullptr, "already visited");
  if (_return_to_vm_size > 0) {
    _source = source;
  } else {
    assert(_source == nullptr, "must be");
  }
  if (source->mem_size() > _return_to_vm_size) {
    _first = source->get_all(_num_unlinked, _unlinked_bytes);
  } else {
    assert(_first == nullptr, "must be");
  }
  // Above we were racing with other threads getting the contents of the free list,
  // so while we might have been asked to return something to the OS initially,
  // the free list might be empty anyway. In this case just reset internal values
  // used for checking whether there is work available.
  if (_first == nullptr) {
    _source = nullptr;
    _return_to_vm_size = 0;
  }
}

bool G1CardSetFreePool::G1ReturnMemoryProcessor::return_to_vm(jlong deadline) {
  assert(!finished_return_to_vm(), "already returned everything to the VM");
  assert(_first != nullptr, "must have element to return");

  size_t keep_size = 0;
  size_t keep_num = 0;

  G1CardSetBuffer* cur = _first;
  G1CardSetBuffer* last = nullptr;

  while (cur != nullptr && _return_to_vm_size > 0) {
    size_t cur_size = cur->mem_size();
    _return_to_vm_size -= MIN2(_return_to_vm_size, cur_size);

    keep_size += cur_size;
    keep_num++;

    last = cur;
    cur = cur->next();
    // To ensure progress, perform the deadline check here.
    if (os::elapsed_counter() > deadline) {
      break;
    }
  }

  assert(_first != nullptr, "must be");
  assert(last != nullptr, "must be");

  last->set_next(nullptr);

  // Wait for any in-progress pops to avoid ABA for them.
  GlobalCounter::write_synchronize();
  _source->bulk_add(*_first, *last, keep_num, keep_size);
  _first = cur;

  log_trace(gc, task)("Card Set Free Memory: Returned to VM %zu buffers size %zu", keep_num, keep_size);

  // _return_to_vm_size may be larger than what is available in the list at the
  // time we actually get the list. I.e. the list and _return_to_vm_size may be
  // inconsistent.
  // So also check if we actually already at the end of the list for the exit
  // condition.
  if (_return_to_vm_size == 0 || _first == nullptr) {
    _source = nullptr;
    _return_to_vm_size = 0;
  }
  return _source != nullptr;
}

bool G1CardSetFreePool::G1ReturnMemoryProcessor::return_to_os(jlong deadline) {
  assert(finished_return_to_vm(), "not finished returning to VM");
  assert(!finished_return_to_os(), "already returned everything to the OS");

  // Now delete the rest.
  size_t num_delete = 0;
  size_t mem_size_deleted = 0;

  while (_first != nullptr) {
    G1CardSetBuffer* next = _first->next();
    num_delete++;
    mem_size_deleted += _first->mem_size();
    delete _first;
    _first = next;

    // To ensure progress, perform the deadline check here.
    if (os::elapsed_counter() > deadline) {
      break;
    }
  }

  log_trace(gc, task)("Card Set Free Memory: Return to OS %zu buffers size %zu", num_delete, mem_size_deleted);

  return _first != nullptr;
}

G1CardSetFreePool G1CardSetFreePool::_freelist_pool(G1CardSetConfiguration::num_mem_object_types());

G1CardSetFreePool::G1CardSetFreePool(uint num_free_lists) :
  _num_free_lists(num_free_lists) {

  _free_lists = NEW_C_HEAP_ARRAY(G1CardSetBufferList, _num_free_lists, mtGC);
  for (uint i = 0; i < _num_free_lists; i++) {
    new (&_free_lists[i]) G1CardSetBufferList();
  }
}

G1CardSetFreePool::~G1CardSetFreePool() {
  for (uint i = 0; i < _num_free_lists; i++) {
    _free_lists[i].~G1CardSetBufferList();
  }
  FREE_C_HEAP_ARRAY(mtGC, _free_lists);
}

static void collect_mem_sizes(const void* context, uint i, size_t& mem_size, size_t& num_buffers) {
  ((G1CardSetFreePool*)context)->get_size(i, mem_size, num_buffers);
}

void G1CardSetFreePool::get_size(uint i, size_t& mem_size, size_t& num_buffers) const {
  mem_size = _free_lists[i].mem_size();
  num_buffers = _free_lists[i].num_buffers();
}

G1CardSetMemoryStats G1CardSetFreePool::memory_sizes() const {
  return G1CardSetMemoryStats(collect_mem_sizes, this);
}

size_t G1CardSetFreePool::mem_size() const {
  size_t result = 0;
  for (uint i = 0; i < _num_free_lists; i++) {
    result += _free_lists[i].mem_size();
  }
  return result;
}

void G1CardSetFreePool::print_on(outputStream* out) {
  out->print_cr("  Free Pool: size %zu", free_list_pool()->mem_size());
  for (uint i = 0; i < _num_free_lists; i++) {
    FormatBuffer<> fmt("    %s", G1CardSetConfiguration::mem_object_type_name_str(i));
    _free_lists[i].print_on(out, fmt);
  }
}

G1CardSetMemoryManager::G1CardSetMemoryManager(G1CardSetConfiguration* config,
                                               G1CardSetFreePool* free_list_pool) : _config(config) {

  _allocators = NEW_C_HEAP_ARRAY(G1CardSetAllocator<G1CardSetContainer>,
                                 _config->num_mem_object_types(),
                                 mtGC);
  G1CardSetAllocOptions* alloc_options = _config->mem_object_alloc_options();
  for (uint i = 0; i < num_mem_object_types(); i++) {
    new (&_allocators[i]) G1CardSetAllocator<G1CardSetContainer>(_config->mem_object_type_name_str(i),
                                                                 alloc_options[i],
                                                                 free_list_pool->free_list(i));
  }
  FREE_C_HEAP_ARRAY(size_t, alloc_options);
}

uint G1CardSetMemoryManager::num_mem_object_types() const {
  return _config->num_mem_object_types();
}


G1CardSetMemoryManager::~G1CardSetMemoryManager() {
  for (uint i = 0; i < num_mem_object_types(); i++) {
    _allocators[i].~G1CardSetAllocator();
  }
  FREE_C_HEAP_ARRAY(G1CardSetAllocator<G1CardSetContainer>, _allocators);
}

void G1CardSetMemoryManager::free(uint type, void* value) {
  assert(type < num_mem_object_types(), "must be");
  _allocators[type].free((G1CardSetContainer*)value);
}

void G1CardSetMemoryManager::flush() {
  for (uint i = 0; i < num_mem_object_types(); i++) {
    _allocators[i].drop_all();
  }
}

void G1CardSetMemoryManager::print(outputStream* os) {
  os->print_cr("MM " PTR_FORMAT " size %zu", p2i(this), sizeof(*this));
  for (uint i = 0; i < num_mem_object_types(); i++) {
    _allocators[i].print(os);
  }
}

size_t G1CardSetMemoryManager::mem_size() const {
  size_t result = 0;
  for (uint i = 0; i < num_mem_object_types(); i++) {
    result += _allocators[i].mem_size();
  }
  return sizeof(*this) -
    (sizeof(G1CardSetAllocator<G1CardSetContainer>) * num_mem_object_types()) +
    result;
}

size_t G1CardSetMemoryManager::wasted_mem_size() const {
  size_t result = 0;
  for (uint i = 0; i < num_mem_object_types(); i++) {
    result += _allocators[i].wasted_mem_size();
  }
  return result;
}

G1CardSetMemoryStats G1CardSetMemoryManager::memory_stats() const {
  G1CardSetMemoryStats result;
  for (uint i = 0; i < num_mem_object_types(); i++) {
    result._num_mem_sizes[i] += _allocators[i].mem_size();
    result._num_buffers[i] += _allocators[i].num_buffers();
  }
  return result;
}
