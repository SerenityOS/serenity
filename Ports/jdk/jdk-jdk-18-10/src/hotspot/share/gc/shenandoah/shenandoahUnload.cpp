/*
 * Copyright (c) 2019, 2021, Red Hat, Inc. All rights reserved.
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
#include "classfile/systemDictionary.hpp"
#include "code/codeBehaviours.hpp"
#include "code/codeCache.hpp"
#include "code/dependencyContext.hpp"
#include "gc/shared/gcBehaviours.hpp"
#include "gc/shared/suspendibleThreadSet.hpp"
#include "gc/shenandoah/shenandoahNMethod.inline.hpp"
#include "gc/shenandoah/shenandoahLock.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahRootProcessor.hpp"
#include "gc/shenandoah/shenandoahUnload.hpp"
#include "gc/shenandoah/shenandoahVerifier.hpp"
#include "memory/iterator.hpp"
#include "memory/metaspaceUtils.hpp"
#include "memory/resourceArea.hpp"
#include "oops/access.inline.hpp"

class ShenandoahIsUnloadingOopClosure : public OopClosure {
private:
  ShenandoahMarkingContext* const _marking_context;
  bool                            _is_unloading;

public:
  ShenandoahIsUnloadingOopClosure() :
    _marking_context(ShenandoahHeap::heap()->complete_marking_context()),
    _is_unloading(false) {
  }

  virtual void do_oop(oop* p) {
    if (_is_unloading) {
      return;
    }

    const oop o = RawAccess<>::oop_load(p);
    if (!CompressedOops::is_null(o) &&
        !_marking_context->is_marked(o)) {
      _is_unloading = true;
    }
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }

  bool is_unloading() const {
    return _is_unloading;
  }
};

class ShenandoahIsUnloadingBehaviour : public IsUnloadingBehaviour {
public:
  virtual bool is_unloading(CompiledMethod* method) const {
    nmethod* const nm = method->as_nmethod();
    assert(ShenandoahHeap::heap()->is_concurrent_weak_root_in_progress(), "Only for this phase");
    ShenandoahNMethod* data = ShenandoahNMethod::gc_data(nm);
    ShenandoahReentrantLocker locker(data->lock());
    ShenandoahIsUnloadingOopClosure cl;
    data->oops_do(&cl);
    return  cl.is_unloading();
  }
};

class ShenandoahCompiledICProtectionBehaviour : public CompiledICProtectionBehaviour {
public:
  virtual bool lock(CompiledMethod* method) {
    nmethod* const nm = method->as_nmethod();
    ShenandoahReentrantLock* const lock = ShenandoahNMethod::lock_for_nmethod(nm);
    assert(lock != NULL, "Not yet registered?");
    lock->lock();
    return true;
  }

  virtual void unlock(CompiledMethod* method) {
    nmethod* const nm = method->as_nmethod();
    ShenandoahReentrantLock* const lock = ShenandoahNMethod::lock_for_nmethod(nm);
    assert(lock != NULL, "Not yet registered?");
    lock->unlock();
  }

  virtual bool is_safe(CompiledMethod* method) {
    if (SafepointSynchronize::is_at_safepoint()) {
      return true;
    }

    nmethod* const nm = method->as_nmethod();
    ShenandoahReentrantLock* const lock = ShenandoahNMethod::lock_for_nmethod(nm);
    assert(lock != NULL, "Not yet registered?");
    return lock->owned_by_self();
  }
};

ShenandoahUnload::ShenandoahUnload() {
  if (ClassUnloading) {
    static ShenandoahIsUnloadingBehaviour is_unloading_behaviour;
    IsUnloadingBehaviour::set_current(&is_unloading_behaviour);

    static ShenandoahCompiledICProtectionBehaviour ic_protection_behaviour;
    CompiledICProtectionBehaviour::set_current(&ic_protection_behaviour);
  }
}

void ShenandoahUnload::prepare() {
  assert(SafepointSynchronize::is_at_safepoint(), "Should be at safepoint");
  assert(ClassUnloading, "Sanity");
  CodeCache::increment_unloading_cycle();
  DependencyContext::cleaning_start();
}

void ShenandoahUnload::unload() {
  ShenandoahHeap* heap = ShenandoahHeap::heap();
  assert(ClassUnloading, "Filtered by caller");
  assert(heap->is_concurrent_weak_root_in_progress(), "Filtered by caller");

  // Unlink stale metadata and nmethods
  {
    ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_unlink);

    SuspendibleThreadSetJoiner sts;
    bool unloadingOccurred;
    {
      ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_unlink_sd);
      MutexLocker cldgMl(ClassLoaderDataGraph_lock);
      unloadingOccurred = SystemDictionary::do_unloading(heap->gc_timer());
    }

    {
      ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_unlink_weak_klass);
      Klass::clean_weak_klass_links(unloadingOccurred);
    }

    {
      ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_unlink_code_roots);
      ShenandoahCodeRoots::unlink(heap->workers(), unloadingOccurred);
    }

    DependencyContext::cleaning_end();
  }

  // Make sure stale metadata and nmethods are no longer observable
  {
    ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_rendezvous);
    heap->rendezvous_threads();
  }

  // Purge stale metadata and nmethods that were unlinked
  {
    ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_purge);

    {
      ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_purge_coderoots);
      SuspendibleThreadSetJoiner sts;
      ShenandoahCodeRoots::purge(heap->workers());
    }

    {
      ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_purge_cldg);
      ClassLoaderDataGraph::purge(/*at_safepoint*/false);
    }

    {
      ShenandoahTimingsTracker t(ShenandoahPhaseTimings::conc_class_unload_purge_ec);
      CodeCache::purge_exception_caches();
    }
  }
}

void ShenandoahUnload::finish() {
  MetaspaceGC::compute_new_size();
  DEBUG_ONLY(MetaspaceUtils::verify();)
}
