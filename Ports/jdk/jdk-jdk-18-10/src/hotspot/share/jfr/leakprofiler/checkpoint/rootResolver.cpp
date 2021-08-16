/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderDataGraph.hpp"
#include "classfile/stringTable.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "gc/shared/strongRootsScope.hpp"
#include "jfr/leakprofiler/utilities/unifiedOopRef.inline.hpp"
#include "jfr/leakprofiler/checkpoint/rootResolver.hpp"
#include "jfr/utilities/jfrThreadIterator.hpp"
#include "memory/iterator.hpp"
#include "prims/jvmtiDeferredUpdates.hpp"
#include "oops/klass.hpp"
#include "oops/oop.hpp"
#include "prims/jvmtiThreadState.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/mutexLocker.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/vframe_hp.hpp"
#include "services/management.hpp"
#include "utilities/enumIterator.hpp"
#include "utilities/growableArray.hpp"

class ReferenceLocateClosure : public OopClosure {
 protected:
  RootCallback& _callback;
  RootCallbackInfo _info;
  bool _complete;

  void do_oop_shared(UnifiedOopRef ref);

 public:
  ReferenceLocateClosure(RootCallback& callback,
                         OldObjectRoot::System system,
                         OldObjectRoot::Type type,
                         const void* context) : _callback(callback),
                                                _info(),
                                                _complete(false) {
    _info._high = NULL;
    _info._low = NULL;
    _info._system = system;
    _info._type = type;
    _info._context = context;
  }

  virtual void do_oop(oop* ref);
  virtual void do_oop(narrowOop* ref);

  bool complete() const {
    return _complete;
  }
};

void ReferenceLocateClosure::do_oop_shared(UnifiedOopRef ref) {
  assert(!ref.is_null(), "invariant");
  if (!_complete) {
    _info._high = ref.addr<address>();
    _complete = _callback.process(_info);
  }
}

void ReferenceLocateClosure::do_oop(oop* ref) {
  do_oop_shared(UnifiedOopRef::encode_in_native(ref));
}

void ReferenceLocateClosure::do_oop(narrowOop* ref) {
  do_oop_shared(UnifiedOopRef::encode_in_native(ref));
}

class ReferenceToRootClosure : public StackObj {
 private:
  RootCallback& _callback;
  RootCallbackInfo _info;
  bool _complete;

  bool do_cldg_roots();
  bool do_oop_storage_roots();
  bool do_string_table_roots();

  bool do_roots();

 public:
  ReferenceToRootClosure(RootCallback& callback) : _callback(callback),
                                                   _info(),
                                                   _complete(false) {
    _info._high = NULL;
    _info._low = NULL;
    _info._context = NULL;
    _info._system = OldObjectRoot::_system_undetermined;
    _info._type = OldObjectRoot::_type_undetermined;

    assert_locked_or_safepoint(Threads_lock);
    do_roots();
  }

  bool complete() const {
    return _complete;
  }
};

bool ReferenceToRootClosure::do_cldg_roots() {
  assert(!complete(), "invariant");
  ReferenceLocateClosure rlc(_callback, OldObjectRoot::_class_loader_data, OldObjectRoot::_type_undetermined, NULL);
  CLDToOopClosure cldt_closure(&rlc, ClassLoaderData::_claim_none);
  ClassLoaderDataGraph::always_strong_cld_do(&cldt_closure);
  return rlc.complete();
}

bool ReferenceToRootClosure::do_oop_storage_roots() {
  using Range = EnumRange<OopStorageSet::StrongId>;
  for (auto id : Range()) {
    assert(!complete(), "invariant");
    OopStorage* oop_storage = OopStorageSet::storage(id);
    OldObjectRoot::Type type = JNIHandles::is_global_storage(oop_storage) ?
                               OldObjectRoot::_global_jni_handle :
                               OldObjectRoot::_global_oop_handle;
    OldObjectRoot::System system = OldObjectRoot::System(OldObjectRoot::_strong_oop_storage_set_first + Range().index(id));
    ReferenceLocateClosure rlc(_callback, system, type, NULL);
    oop_storage->oops_do(&rlc);
    if (rlc.complete()) {
      return true;
    }
  }
  return false;
}

bool ReferenceToRootClosure::do_roots() {
  assert(!complete(), "invariant");
  assert(OldObjectRoot::_system_undetermined == _info._system, "invariant");
  assert(OldObjectRoot::_type_undetermined == _info._type, "invariant");

  if (do_cldg_roots()) {
    _complete = true;
    return true;
  }

  if (do_oop_storage_roots()) {
   _complete = true;
    return true;
  }

  return false;
}

class ReferenceToThreadRootClosure : public StackObj {
 private:
  RootCallback& _callback;
  bool _complete;

  bool do_java_threads_oops(JavaThread* jt);
  bool do_thread_roots(JavaThread* jt);
  bool do_thread_stack_fast(JavaThread* jt);
  bool do_thread_stack_detailed(JavaThread* jt);
  bool do_thread_jni_handles(JavaThread* jt);
  bool do_thread_handle_area(JavaThread* jt);

 public:
  ReferenceToThreadRootClosure(RootCallback& callback) :_callback(callback), _complete(false) {
    assert_locked_or_safepoint(Threads_lock);
    JfrJavaThreadIterator iter;
    while (iter.has_next()) {
      if (do_thread_roots(iter.next())) {
        return;
      }
    }
  }

  bool complete() const {
    return _complete;
  }
};

bool ReferenceToThreadRootClosure::do_thread_handle_area(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(!complete(), "invariant");
  ReferenceLocateClosure rcl(_callback, OldObjectRoot::_threads, OldObjectRoot::_handle_area, jt);
  jt->handle_area()->oops_do(&rcl);
  return rcl.complete();
}

bool ReferenceToThreadRootClosure::do_thread_jni_handles(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(!complete(), "invariant");

  ReferenceLocateClosure rcl(_callback, OldObjectRoot::_threads, OldObjectRoot::_local_jni_handle, jt);
  jt->active_handles()->oops_do(&rcl);
  return rcl.complete();
}

bool ReferenceToThreadRootClosure::do_thread_stack_fast(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(!complete(), "invariant");

  if (_callback.entries() == 0) {
    _complete = true;
    return true;
  }

  RootCallbackInfo info;
  info._high = NULL;
  info._low = NULL;
  info._context = jt;
  info._system = OldObjectRoot::_threads;
  info._type = OldObjectRoot::_stack_variable;

  for (int i = 0; i < _callback.entries(); ++i) {
    const address adr = _callback.at(i).addr<address>();
    if (jt->is_in_usable_stack(adr)) {
      info._high = adr;
      _complete = _callback.process(info);
      if (_complete) {
        return true;
      }
    }
  }
  assert(!complete(), "invariant");
  return false;
}

bool ReferenceToThreadRootClosure::do_thread_stack_detailed(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(!complete(), "invariant");

  ReferenceLocateClosure rcl(_callback, OldObjectRoot::_threads, OldObjectRoot::_stack_variable, jt);

  if (jt->has_last_Java_frame()) {
    // Traverse the monitor chunks
    MonitorChunk* chunk = jt->monitor_chunks();
    for (; chunk != NULL; chunk = chunk->next()) {
      chunk->oops_do(&rcl);
    }

    if (rcl.complete()) {
      return true;
    }

    // Traverse the execution stack
    for (StackFrameStream fst(jt, true /* update */, true /* process_frames */); !fst.is_done(); fst.next()) {
      fst.current()->oops_do(&rcl, NULL, fst.register_map());
    }

  } // last java frame

  if (rcl.complete()) {
    return true;
  }

  GrowableArray<jvmtiDeferredLocalVariableSet*>* const list = JvmtiDeferredUpdates::deferred_locals(jt);
  if (list != NULL) {
    for (int i = 0; i < list->length(); i++) {
      list->at(i)->oops_do(&rcl);
    }
  }

  if (rcl.complete()) {
    return true;
  }

  // Traverse instance variables at the end since the GC may be moving things
  // around using this function
  /*
  * // can't reach these oop* from the outside
  f->do_oop((oop*) &_vm_result);
  f->do_oop((oop*) &_exception_oop);
  f->do_oop((oop*) &_pending_async_exception);
  */

  JvmtiThreadState* const jvmti_thread_state = jt->jvmti_thread_state();
  if (jvmti_thread_state != NULL) {
    jvmti_thread_state->oops_do(&rcl, NULL);
  }

  return rcl.complete();
}

bool ReferenceToThreadRootClosure::do_java_threads_oops(JavaThread* jt) {
  assert(jt != NULL, "invariant");
  assert(!complete(), "invariant");

  ReferenceLocateClosure rcl(_callback, OldObjectRoot::_threads, OldObjectRoot::_global_jni_handle, jt);
  jt->oops_do(&rcl, NULL);
  return rcl.complete();
}

bool ReferenceToThreadRootClosure::do_thread_roots(JavaThread* jt) {
  assert(jt != NULL, "invariant");

  if (do_thread_stack_fast(jt)) {
    _complete = true;
    return true;
  }

  if (do_thread_jni_handles(jt)) {
    _complete = true;
    return true;
  }

  if (do_thread_handle_area(jt)) {
    _complete = true;
    return true;
  }

  if (do_thread_stack_detailed(jt)) {
    _complete = true;
    return true;
  }

  return false;
}

class RootResolverMarkScope : public MarkScope {
};

void RootResolver::resolve(RootCallback& callback) {
  RootResolverMarkScope mark_scope;

  // thread local roots
  ReferenceToThreadRootClosure rtrc(callback);
  if (rtrc.complete()) {
    return;
  }
  // system global roots
  ReferenceToRootClosure rrc(callback);
}
