/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "code/scopeDesc.hpp"
#include "code/vtableStubs.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "prims/jvmtiCodeBlobEvents.hpp"
#include "prims/jvmtiExport.hpp"
#include "prims/jvmtiThreadState.inline.hpp"
#include "runtime/handles.inline.hpp"
#include "runtime/safepointVerifiers.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/vmThread.hpp"

// Support class to collect a list of the non-nmethod CodeBlobs in
// the CodeCache.
//
// This class actually creates a list of JvmtiCodeBlobDesc - each JvmtiCodeBlobDesc
// describes a single CodeBlob in the CodeCache. Note that collection is
// done to a static list - this is because CodeCache::blobs_do is defined
// as void CodeCache::blobs_do(void f(CodeBlob* nm)) and hence requires
// a C or static method.
//
// Usage :-
//
// CodeBlobCollector collector;
//
// collector.collect();
// JvmtiCodeBlobDesc* blob = collector.first();
// while (blob != NULL) {
//   :
//   blob = collector.next();
// }
//

class CodeBlobCollector : StackObj {
 private:
  GrowableArray<JvmtiCodeBlobDesc*>* _code_blobs;   // collected blobs
  int _pos;                                         // iterator position

  // used during a collection
  static GrowableArray<JvmtiCodeBlobDesc*>* _global_code_blobs;
  static void do_blob(CodeBlob* cb);
  static void do_vtable_stub(VtableStub* vs);
 public:
  CodeBlobCollector() {
    _code_blobs = NULL;
    _pos = -1;
  }
  ~CodeBlobCollector() {
    if (_code_blobs != NULL) {
      for (int i=0; i<_code_blobs->length(); i++) {
        FreeHeap(_code_blobs->at(i));
      }
      delete _code_blobs;
    }
  }

  // collect list of code blobs in the cache
  void collect();

  // iteration support - return first code blob
  JvmtiCodeBlobDesc* first() {
    assert(_code_blobs != NULL, "not collected");
    if (_code_blobs->length() == 0) {
      return NULL;
    }
    _pos = 0;
    return _code_blobs->at(0);
  }

  // iteration support - return next code blob
  JvmtiCodeBlobDesc* next() {
    assert(_pos >= 0, "iteration not started");
    if (_pos+1 >= _code_blobs->length()) {
      return NULL;
    }
    return _code_blobs->at(++_pos);
  }

};

// used during collection
GrowableArray<JvmtiCodeBlobDesc*>* CodeBlobCollector::_global_code_blobs;


// called for each CodeBlob in the CodeCache
//
// This function filters out nmethods as it is only interested in
// other CodeBlobs. This function also filters out CodeBlobs that have
// a duplicate starting address as previous blobs. This is needed to
// handle the case where multiple stubs are generated into a single
// BufferBlob.

void CodeBlobCollector::do_blob(CodeBlob* cb) {

  // ignore nmethods
  if (cb->is_nmethod()) {
    return;
  }
  // exclude VtableStubs, which are processed separately
  if (cb->is_buffer_blob() && strcmp(cb->name(), "vtable chunks") == 0) {
    return;
  }

  // check if this starting address has been seen already - the
  // assumption is that stubs are inserted into the list before the
  // enclosing BufferBlobs.
  address addr = cb->code_begin();
  for (int i=0; i<_global_code_blobs->length(); i++) {
    JvmtiCodeBlobDesc* scb = _global_code_blobs->at(i);
    if (addr == scb->code_begin()) {
      return;
    }
  }

  // record the CodeBlob details as a JvmtiCodeBlobDesc
  JvmtiCodeBlobDesc* scb = new JvmtiCodeBlobDesc(cb->name(), cb->code_begin(), cb->code_end());
  _global_code_blobs->append(scb);
}

// called for each VtableStub in VtableStubs

void CodeBlobCollector::do_vtable_stub(VtableStub* vs) {
    JvmtiCodeBlobDesc* scb = new JvmtiCodeBlobDesc(vs->is_vtable_stub() ? "vtable stub" : "itable stub",
                                                   vs->code_begin(), vs->code_end());
    _global_code_blobs->append(scb);
}

// collects a list of CodeBlobs in the CodeCache.
//
// The created list is growable array of JvmtiCodeBlobDesc - each one describes
// a CodeBlob. Note that the list is static - this is because CodeBlob::blobs_do
// requires a a C or static function so we can't use an instance function. This
// isn't a problem as the iteration is serial anyway as we need the CodeCache_lock
// to iterate over the code cache.
//
// Note that the CodeBlobs in the CodeCache will include BufferBlobs that may
// contain multiple stubs. As a profiler is interested in the stubs rather than
// the enclosing container we first iterate over the stub code descriptors so
// that the stubs go into the list first. do_blob will then filter out the
// enclosing blobs if the starting address of the enclosing blobs matches the
// starting address of first stub generated in the enclosing blob.

void CodeBlobCollector::collect() {
  assert_locked_or_safepoint(CodeCache_lock);
  assert(_global_code_blobs == NULL, "checking");

  // create the global list
  _global_code_blobs = new (ResourceObj::C_HEAP, mtServiceability) GrowableArray<JvmtiCodeBlobDesc*>(50, mtServiceability);

  // iterate over the stub code descriptors and put them in the list first.
  for (StubCodeDesc* desc = StubCodeDesc::first(); desc != NULL; desc = StubCodeDesc::next(desc)) {
    _global_code_blobs->append(new JvmtiCodeBlobDesc(desc->name(), desc->begin(), desc->end()));
  }

  // Vtable stubs are not described with StubCodeDesc,
  // process them separately
  VtableStubs::vtable_stub_do(do_vtable_stub);

  // next iterate over all the non-nmethod code blobs and add them to
  // the list - as noted above this will filter out duplicates and
  // enclosing blobs.
  CodeCache::blobs_do(do_blob);

  // make the global list the instance list so that it can be used
  // for other iterations.
  _code_blobs = _global_code_blobs;
  _global_code_blobs = NULL;
}


// Generate a DYNAMIC_CODE_GENERATED event for each non-nmethod code blob.

jvmtiError JvmtiCodeBlobEvents::generate_dynamic_code_events(JvmtiEnv* env) {
  CodeBlobCollector collector;

  // First collect all the code blobs.  This has to be done in a
  // single pass over the code cache with CodeCache_lock held because
  // there isn't any safe way to iterate over regular CodeBlobs since
  // they can be freed at any point.
  {
    MutexLocker mu(CodeCache_lock, Mutex::_no_safepoint_check_flag);
    collector.collect();
  }

  // iterate over the collected list and post an event for each blob
  JvmtiCodeBlobDesc* blob = collector.first();
  while (blob != NULL) {
    JvmtiExport::post_dynamic_code_generated(env, blob->name(), blob->code_begin(), blob->code_end());
    blob = collector.next();
  }
  return JVMTI_ERROR_NONE;
}


// Generate a COMPILED_METHOD_LOAD event for each nnmethod
jvmtiError JvmtiCodeBlobEvents::generate_compiled_method_load_events(JvmtiEnv* env) {
  JavaThread* java_thread = JavaThread::current();
  JvmtiThreadState* state = JvmtiThreadState::state_for(java_thread);
  {
    NoSafepointVerifier nsv;  // safepoints are not safe while collecting methods to post.
    {
      // Walk the CodeCache notifying for live nmethods. We hold the CodeCache_lock
      // to ensure the iteration is safe and nmethods are not concurrently freed.
      // However, they may still change states and become !is_alive(). Filtering
      // those out is done inside of nmethod::post_compiled_method_load_event().
      // Save events to the queue for posting outside the CodeCache_lock.
      MutexLocker mu(java_thread, CodeCache_lock, Mutex::_no_safepoint_check_flag);
      // Iterate over non-profiled and profiled nmethods
      NMethodIterator iter(NMethodIterator::only_alive_and_not_unloading);
      while(iter.next()) {
        nmethod* current = iter.method();
        current->post_compiled_method_load_event(state);
      }
    }

    // Enter nmethod barrier code if present outside CodeCache_lock
    state->run_nmethod_entry_barriers();
  }

  // Now post all the events outside the CodeCache_lock.
  // If there's a safepoint, the queued events will be kept alive.
  // Adding these events to the service thread to post is something that
  // should work, but the service thread doesn't keep up in stress scenarios and
  // the os eventually kills the process with OOM.
  // We want this thread to wait until the events are all posted.
  state->post_events(env);
  return JVMTI_ERROR_NONE;
}


// create a C-heap allocated address location map for an nmethod
void JvmtiCodeBlobEvents::build_jvmti_addr_location_map(nmethod *nm,
                                                        jvmtiAddrLocationMap** map_ptr,
                                                        jint *map_length_ptr)
{
  ResourceMark rm;
  jvmtiAddrLocationMap* map = NULL;
  jint map_length = 0;


  // Generate line numbers using PcDesc and ScopeDesc info
  methodHandle mh(Thread::current(), nm->method());

  if (!mh->is_native()) {
    PcDesc *pcd;
    int pcds_in_method;

    pcds_in_method = (nm->scopes_pcs_end() - nm->scopes_pcs_begin());
    map = NEW_C_HEAP_ARRAY(jvmtiAddrLocationMap, pcds_in_method, mtInternal);

    address scopes_data = nm->scopes_data_begin();
    for( pcd = nm->scopes_pcs_begin(); pcd < nm->scopes_pcs_end(); ++pcd ) {
      ScopeDesc sc0(nm, pcd, true);
      ScopeDesc *sd  = &sc0;
      while( !sd->is_top() ) { sd = sd->sender(); }
      int bci = sd->bci();
      if (bci >= 0) {
        assert(map_length < pcds_in_method, "checking");
        map[map_length].start_address = (const void*)pcd->real_pc(nm);
        map[map_length].location = bci;
        ++map_length;
      }
    }
  }

  *map_ptr = map;
  *map_length_ptr = map_length;
}
