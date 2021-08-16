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
#include "code/codeCache.hpp"
#include "gc/shenandoah/shenandoahAsserts.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahPhaseTimings.hpp"
#include "gc/shenandoah/shenandoahRootVerifier.hpp"
#include "gc/shenandoah/shenandoahStringDedup.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "gc/shared/oopStorage.inline.hpp"
#include "gc/shared/oopStorageSet.hpp"
#include "runtime/jniHandles.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/enumIterator.hpp"

ShenandoahGCStateResetter::ShenandoahGCStateResetter() :
  _heap(ShenandoahHeap::heap()),
  _gc_state(_heap->gc_state()) {
  _heap->_gc_state.clear();
}

ShenandoahGCStateResetter::~ShenandoahGCStateResetter() {
  _heap->_gc_state.set(_gc_state);
  assert(_heap->gc_state() == _gc_state, "Should be restored");
}

void ShenandoahRootVerifier::roots_do(OopClosure* oops) {
  ShenandoahGCStateResetter resetter;
  shenandoah_assert_safepoint();

  CodeBlobToOopClosure blobs(oops, !CodeBlobToOopClosure::FixRelocations);
  CodeCache::blobs_do(&blobs);

  CLDToOopClosure clds(oops, ClassLoaderData::_claim_none);
  ClassLoaderDataGraph::cld_do(&clds);

  for (auto id : EnumRange<OopStorageSet::StrongId>()) {
    OopStorageSet::storage(id)->oops_do(oops);
  }

  // Do thread roots the last. This allows verification code to find
  // any broken objects from those special roots first, not the accidental
  // dangling reference from the thread root.
  Threads::possibly_parallel_oops_do(true, oops, NULL);
}

void ShenandoahRootVerifier::strong_roots_do(OopClosure* oops) {
  ShenandoahGCStateResetter resetter;
  shenandoah_assert_safepoint();

  CLDToOopClosure clds(oops, ClassLoaderData::_claim_none);
  ClassLoaderDataGraph::always_strong_cld_do(&clds);

  for (auto id : EnumRange<OopStorageSet::StrongId>()) {
    OopStorageSet::storage(id)->oops_do(oops);
  }
  // Do thread roots the last. This allows verification code to find
  // any broken objects from those special roots first, not the accidental
  // dangling reference from the thread root.
  CodeBlobToOopClosure blobs(oops, !CodeBlobToOopClosure::FixRelocations);
  Threads::possibly_parallel_oops_do(true, oops, &blobs);
}
