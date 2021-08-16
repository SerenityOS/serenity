/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "code/codeCache.hpp"
#include "code/compiledIC.hpp"
#include "code/icBuffer.hpp"
#include "code/nmethod.hpp"
#include "code/scopeDesc.hpp"
#include "gc/shared/collectedHeap.inline.hpp"
#include "interpreter/interpreter.hpp"
#include "interpreter/linkResolver.hpp"
#include "memory/resourceArea.hpp"
#include "oops/method.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/stubRoutines.hpp"
#include "runtime/thread.inline.hpp"
#include "runtime/vmOperations.hpp"

DEF_STUB_INTERFACE(ICStub);

StubQueue* InlineCacheBuffer::_buffer    = NULL;

CompiledICHolder* InlineCacheBuffer::_pending_released = NULL;
int InlineCacheBuffer::_pending_count = 0;

#ifdef ASSERT
ICRefillVerifier::ICRefillVerifier()
  : _refill_requested(false),
    _refill_remembered(false)
{
  Thread* thread = Thread::current();
  assert(thread->missed_ic_stub_refill_verifier() == NULL, "nesting not supported");
  thread->set_missed_ic_stub_refill_verifier(this);
}

ICRefillVerifier::~ICRefillVerifier() {
  assert(!_refill_requested || _refill_remembered,
         "Forgot to refill IC stubs after failed IC transition");
  Thread::current()->set_missed_ic_stub_refill_verifier(NULL);
}

ICRefillVerifierMark::ICRefillVerifierMark(ICRefillVerifier* verifier) {
  Thread* thread = Thread::current();
  assert(thread->missed_ic_stub_refill_verifier() == NULL, "nesting not supported");
  thread->set_missed_ic_stub_refill_verifier(verifier);
}

ICRefillVerifierMark::~ICRefillVerifierMark() {
  Thread::current()->set_missed_ic_stub_refill_verifier(NULL);
}

static ICRefillVerifier* current_ic_refill_verifier() {
  Thread* current = Thread::current();
  ICRefillVerifier* verifier = current->missed_ic_stub_refill_verifier();
  assert(verifier != NULL, "need a verifier for safety");
  return verifier;
}
#endif

void ICStub::finalize() {
  if (!is_empty()) {
    ResourceMark rm;
    CompiledIC *ic = CompiledIC_at(CodeCache::find_compiled(ic_site()), ic_site());
    assert(CodeCache::find_compiled(ic->instruction_address()) != NULL, "inline cache in non-compiled?");

    assert(this == ICStub_from_destination_address(ic->stub_address()), "wrong owner of ic buffer");
    ic->set_ic_destination_and_value(destination(), cached_value());
  }
}


address ICStub::destination() const {
  return InlineCacheBuffer::ic_buffer_entry_point(code_begin());
}

void* ICStub::cached_value() const {
  return InlineCacheBuffer::ic_buffer_cached_value(code_begin());
}


void ICStub::set_stub(CompiledIC *ic, void* cached_val, address dest_addr) {
  // We cannot store a pointer to the 'ic' object, since it is resource allocated. Instead we
  // store the location of the inline cache. Then we have enough information recreate the CompiledIC
  // object when we need to remove the stub.
  _ic_site = ic->instruction_address();

  // Assemble new stub
  InlineCacheBuffer::assemble_ic_buffer_code(code_begin(), cached_val, dest_addr);
  assert(destination() == dest_addr,   "can recover destination");
  assert(cached_value() == cached_val, "can recover destination");
}


void ICStub::clear() {
  if (CompiledIC::is_icholder_entry(destination())) {
    InlineCacheBuffer::queue_for_release((CompiledICHolder*)cached_value());
  }
  _ic_site = NULL;
}


#ifndef PRODUCT
// anybody calling to this stub will trap

void ICStub::verify() {
}

void ICStub::print() {
  tty->print_cr("ICStub: site: " INTPTR_FORMAT, p2i(_ic_site));
}
#endif

//-----------------------------------------------------------------------------------------------
// Implementation of InlineCacheBuffer


void InlineCacheBuffer::initialize() {
  if (_buffer != NULL) return; // already initialized
  _buffer = new StubQueue(new ICStubInterface, 10*K, InlineCacheBuffer_lock, "InlineCacheBuffer");
  assert (_buffer != NULL, "cannot allocate InlineCacheBuffer");
}


ICStub* InlineCacheBuffer::new_ic_stub() {
  return (ICStub*)buffer()->request_committed(ic_stub_code_size());
}


void InlineCacheBuffer::refill_ic_stubs() {
#ifdef ASSERT
  ICRefillVerifier* verifier = current_ic_refill_verifier();
  verifier->request_remembered();
#endif
  // we ran out of inline cache buffer space; must enter safepoint.
  // We do this by forcing a safepoint
  VM_ICBufferFull ibf;
  VMThread::execute(&ibf);
}

void InlineCacheBuffer::update_inline_caches() {
  if (buffer()->number_of_stubs() > 0) {
    if (TraceICBuffer) {
      tty->print_cr("[updating inline caches with %d stubs]", buffer()->number_of_stubs());
    }
    buffer()->remove_all();
  }
  release_pending_icholders();
}


bool InlineCacheBuffer::contains(address instruction_address) {
  return buffer()->contains(instruction_address);
}


bool InlineCacheBuffer::is_empty() {
  return buffer()->number_of_stubs() == 0;
}


void InlineCacheBuffer_init() {
  InlineCacheBuffer::initialize();
}

bool InlineCacheBuffer::create_transition_stub(CompiledIC *ic, void* cached_value, address entry) {
  assert(!SafepointSynchronize::is_at_safepoint(), "should not be called during a safepoint");
  assert(CompiledICLocker::is_safe(ic->instruction_address()), "mt unsafe call");
  if (TraceICBuffer) {
    tty->print_cr("  create transition stub for " INTPTR_FORMAT " destination " INTPTR_FORMAT " cached value " INTPTR_FORMAT,
                  p2i(ic->instruction_address()), p2i(entry), p2i(cached_value));
  }

  // allocate and initialize new "out-of-line" inline-cache
  ICStub* ic_stub = new_ic_stub();
  if (ic_stub == NULL) {
#ifdef ASSERT
    ICRefillVerifier* verifier = current_ic_refill_verifier();
    verifier->request_refill();
#endif
    return false;
  }

  // If an transition stub is already associate with the inline cache, then we remove the association.
  if (ic->is_in_transition_state()) {
    ICStub* old_stub = ICStub_from_destination_address(ic->stub_address());
    old_stub->clear();
  }

  ic_stub->set_stub(ic, cached_value, entry);

  // Update inline cache in nmethod to point to new "out-of-line" allocated inline cache
  ic->set_ic_destination(ic_stub);
  return true;
}


address InlineCacheBuffer::ic_destination_for(CompiledIC *ic) {
  ICStub* stub = ICStub_from_destination_address(ic->stub_address());
  return stub->destination();
}


void* InlineCacheBuffer::cached_value_for(CompiledIC *ic) {
  ICStub* stub = ICStub_from_destination_address(ic->stub_address());
  return stub->cached_value();
}


// Free CompiledICHolder*s that are no longer in use
void InlineCacheBuffer::release_pending_icholders() {
  assert(SafepointSynchronize::is_at_safepoint(), "should only be called during a safepoint");
  CompiledICHolder* holder = _pending_released;
  _pending_released = NULL;
  while (holder != NULL) {
    CompiledICHolder* next = holder->next();
    delete holder;
    holder = next;
    _pending_count--;
  }
  assert(_pending_count == 0, "wrong count");
}

// Enqueue this icholder for release during the next safepoint.  It's
// not safe to free them until them since they might be visible to
// another thread.
void InlineCacheBuffer::queue_for_release(CompiledICHolder* icholder) {
  MutexLocker mex(InlineCacheBuffer_lock, Mutex::_no_safepoint_check_flag);
  icholder->set_next(_pending_released);
  _pending_released = icholder;
  _pending_count++;
  if (TraceICBuffer) {
    tty->print_cr("enqueueing icholder " INTPTR_FORMAT " to be freed", p2i(icholder));
  }
}
