/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeBlob.hpp"
#include "code/codeCache.hpp"
#include "code/stubs.hpp"
#include "memory/allocation.inline.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "utilities/align.hpp"


// Implementation of StubQueue
//
// Standard wrap-around queue implementation; the queue dimensions
// are specified by the _queue_begin & _queue_end indices. The queue
// can be in two states (transparent to the outside):
//
// a) contiguous state: all queue entries in one block (or empty)
//
// Queue: |...|XXXXXXX|...............|
//        ^0  ^begin  ^end            ^size = limit
//            |_______|
//            one block
//
// b) non-contiguous state: queue entries in two blocks
//
// Queue: |XXX|.......|XXXXXXX|.......|
//        ^0  ^end    ^begin  ^limit  ^size
//        |___|       |_______|
//         1st block  2nd block
//
// In the non-contiguous state, the wrap-around point is
// indicated via the _buffer_limit index since the last
// queue entry may not fill up the queue completely in
// which case we need to know where the 2nd block's end
// is to do the proper wrap-around. When removing the
// last entry of the 2nd block, _buffer_limit is reset
// to _buffer_size.
//
// CAUTION: DO NOT MESS WITH THIS CODE IF YOU CANNOT PROVE
// ITS CORRECTNESS! THIS CODE IS MORE SUBTLE THAN IT LOOKS!


StubQueue::StubQueue(StubInterface* stub_interface, int buffer_size,
                     Mutex* lock, const char* name) : _mutex(lock) {
  intptr_t size = align_up(buffer_size, 2*BytesPerWord);
  BufferBlob* blob = BufferBlob::create(name, size);
  if( blob == NULL) {
    vm_exit_out_of_memory(size, OOM_MALLOC_ERROR, "CodeCache: no room for %s", name);
  }
  _stub_interface  = stub_interface;
  _buffer_size     = blob->content_size();
  _buffer_limit    = blob->content_size();
  _stub_buffer     = blob->content_begin();
  _queue_begin     = 0;
  _queue_end       = 0;
  _number_of_stubs = 0;
}


StubQueue::~StubQueue() {
  // Note: Currently StubQueues are never destroyed so nothing needs to be done here.
  //       If we want to implement the destructor, we need to release the BufferBlob
  //       allocated in the constructor (i.e., we need to keep it around or look it
  //       up via CodeCache::find_blob(...).
  Unimplemented();
}

void StubQueue::deallocate_unused_tail() {
  CodeBlob* blob = CodeCache::find_blob((void*)_stub_buffer);
  CodeCache::free_unused_tail(blob, used_space());
  // Update the limits to the new, trimmed CodeBlob size
  _buffer_size = blob->content_size();
  _buffer_limit = blob->content_size();
}

Stub* StubQueue::stub_containing(address pc) const {
  if (contains(pc)) {
    for (Stub* s = first(); s != NULL; s = next(s)) {
      if (stub_contains(s, pc)) return s;
    }
  }
  return NULL;
}


Stub* StubQueue::request_committed(int code_size) {
  Stub* s = request(code_size);
  CodeStrings strings;
  if (s != NULL) commit(code_size, strings);
  return s;
}


Stub* StubQueue::request(int requested_code_size) {
  assert(requested_code_size > 0, "requested_code_size must be > 0");
  if (_mutex != NULL) _mutex->lock_without_safepoint_check();
  Stub* s = current_stub();
  int requested_size = align_up(stub_code_size_to_size(requested_code_size), CodeEntryAlignment);
  if (requested_size <= available_space()) {
    if (is_contiguous()) {
      // Queue: |...|XXXXXXX|.............|
      //        ^0  ^begin  ^end          ^size = limit
      assert(_buffer_limit == _buffer_size, "buffer must be fully usable");
      if (_queue_end + requested_size <= _buffer_size) {
        // code fits in at the end => nothing to do
        CodeStrings strings;
        stub_initialize(s, requested_size, strings);
        return s;
      } else {
        // stub doesn't fit in at the queue end
        // => reduce buffer limit & wrap around
        assert(!is_empty(), "just checkin'");
        _buffer_limit = _queue_end;
        _queue_end = 0;
      }
    }
  }
  if (requested_size <= available_space()) {
    assert(!is_contiguous(), "just checkin'");
    assert(_buffer_limit <= _buffer_size, "queue invariant broken");
    // Queue: |XXX|.......|XXXXXXX|.......|
    //        ^0  ^end    ^begin  ^limit  ^size
    s = current_stub();
    CodeStrings strings;
    stub_initialize(s, requested_size, strings);
    return s;
  }
  // Not enough space left
  if (_mutex != NULL) _mutex->unlock();
  return NULL;
}


void StubQueue::commit(int committed_code_size, CodeStrings& strings) {
  assert(committed_code_size > 0, "committed_code_size must be > 0");
  int committed_size = align_up(stub_code_size_to_size(committed_code_size), CodeEntryAlignment);
  Stub* s = current_stub();
  assert(committed_size <= stub_size(s), "committed size must not exceed requested size");
  stub_initialize(s, committed_size, strings);
  _queue_end += committed_size;
  _number_of_stubs++;
  if (_mutex != NULL) _mutex->unlock();
  debug_only(stub_verify(s);)
}


void StubQueue::remove_first() {
  if (number_of_stubs() == 0) return;
  Stub* s = first();
  debug_only(stub_verify(s);)
  stub_finalize(s);
  _queue_begin += stub_size(s);
  assert(_queue_begin <= _buffer_limit, "sanity check");
  if (_queue_begin == _queue_end) {
    // buffer empty
    // => reset queue indices
    _queue_begin  = 0;
    _queue_end    = 0;
    _buffer_limit = _buffer_size;
  } else if (_queue_begin == _buffer_limit) {
    // buffer limit reached
    // => reset buffer limit & wrap around
    _buffer_limit = _buffer_size;
    _queue_begin = 0;
  }
  _number_of_stubs--;
}


void StubQueue::remove_first(int n) {
  int i = MIN2(n, number_of_stubs());
  while (i-- > 0) remove_first();
}


void StubQueue::remove_all(){
  debug_only(verify();)
  remove_first(number_of_stubs());
  assert(number_of_stubs() == 0, "sanity check");
}


void StubQueue::verify() {
  // verify only if initialized
  if (_stub_buffer == NULL) return;
  MutexLocker lock(_mutex, Mutex::_no_safepoint_check_flag);
  // verify index boundaries
  guarantee(0 <= _buffer_size, "buffer size must be positive");
  guarantee(0 <= _buffer_limit && _buffer_limit <= _buffer_size , "_buffer_limit out of bounds");
  guarantee(0 <= _queue_begin  && _queue_begin  <  _buffer_limit, "_queue_begin out of bounds");
  guarantee(0 <= _queue_end    && _queue_end    <= _buffer_limit, "_queue_end   out of bounds");
  // verify alignment
  guarantee(_buffer_size  % CodeEntryAlignment == 0, "_buffer_size  not aligned");
  guarantee(_buffer_limit % CodeEntryAlignment == 0, "_buffer_limit not aligned");
  guarantee(_queue_begin  % CodeEntryAlignment == 0, "_queue_begin  not aligned");
  guarantee(_queue_end    % CodeEntryAlignment == 0, "_queue_end    not aligned");
  // verify buffer limit/size relationship
  if (is_contiguous()) {
    guarantee(_buffer_limit == _buffer_size, "_buffer_limit must equal _buffer_size");
  }
  // verify contents
  int n = 0;
  for (Stub* s = first(); s != NULL; s = next(s)) {
    stub_verify(s);
    n++;
  }
  guarantee(n == number_of_stubs(), "number of stubs inconsistent");
  guarantee(_queue_begin != _queue_end || n == 0, "buffer indices must be the same");
}


void StubQueue::print() {
  MutexLocker lock(_mutex, Mutex::_no_safepoint_check_flag);
  for (Stub* s = first(); s != NULL; s = next(s)) {
    stub_print(s);
  }
}
