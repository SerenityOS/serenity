/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#include "precompiled.hpp"
#include "classfile/classLoaderDataGraph.hpp"
#include "gc/shared/oopStorageSetParState.inline.hpp"
#include "gc/z/zNMethod.hpp"
#include "gc/z/zNMethodTable.hpp"
#include "gc/z/zRootsIterator.hpp"
#include "gc/z/zStat.hpp"
#include "memory/resourceArea.hpp"
#include "prims/jvmtiTagMap.hpp"
#include "runtime/atomic.hpp"
#include "runtime/globals.hpp"
#include "runtime/safepoint.hpp"
#include "utilities/debug.hpp"

static const ZStatSubPhase ZSubPhaseConcurrentRootsOopStorageSet("Concurrent Roots OopStorageSet");
static const ZStatSubPhase ZSubPhaseConcurrentRootsClassLoaderDataGraph("Concurrent Roots ClassLoaderDataGraph");
static const ZStatSubPhase ZSubPhaseConcurrentRootsJavaThreads("Concurrent Roots JavaThreads");
static const ZStatSubPhase ZSubPhaseConcurrentRootsCodeCache("Concurrent Roots CodeCache");
static const ZStatSubPhase ZSubPhaseConcurrentWeakRootsOopStorageSet("Concurrent Weak Roots OopStorageSet");

template <typename Iterator>
template <typename ClosureType>
void ZParallelApply<Iterator>::apply(ClosureType* cl) {
  if (!Atomic::load(&_completed)) {
    _iter.apply(cl);
    if (!Atomic::load(&_completed)) {
      Atomic::store(&_completed, true);
    }
  }
}

ZStrongOopStorageSetIterator::ZStrongOopStorageSetIterator() :
    _iter() {}

void ZStrongOopStorageSetIterator::apply(OopClosure* cl) {
  ZStatTimer timer(ZSubPhaseConcurrentRootsOopStorageSet);
  _iter.oops_do(cl);
}

void ZStrongCLDsIterator::apply(CLDClosure* cl) {
  ZStatTimer timer(ZSubPhaseConcurrentRootsClassLoaderDataGraph);
  ClassLoaderDataGraph::always_strong_cld_do(cl);
}

ZJavaThreadsIterator::ZJavaThreadsIterator() :
    _threads(),
    _claimed(0) {}

uint ZJavaThreadsIterator::claim() {
  return Atomic::fetch_and_add(&_claimed, 1u);
}

void ZJavaThreadsIterator::apply(ThreadClosure* cl) {
  ZStatTimer timer(ZSubPhaseConcurrentRootsJavaThreads);

  // The resource mark is needed because interpreter oop maps are
  // not reused in concurrent mode. Instead, they are temporary and
  // resource allocated.
  ResourceMark                 _rm;

  for (uint i = claim(); i < _threads.length(); i = claim()) {
    cl->do_thread(_threads.thread_at(i));
  }
}

ZNMethodsIterator::ZNMethodsIterator() {
  if (!ClassUnloading) {
    ZNMethod::nmethods_do_begin();
  }
}

ZNMethodsIterator::~ZNMethodsIterator() {
  if (!ClassUnloading) {
    ZNMethod::nmethods_do_end();
  }
}

void ZNMethodsIterator::apply(NMethodClosure* cl) {
  ZStatTimer timer(ZSubPhaseConcurrentRootsCodeCache);
  ZNMethod::nmethods_do(cl);
}

ZRootsIterator::ZRootsIterator(int cld_claim) {
  if (cld_claim != ClassLoaderData::_claim_none) {
    ClassLoaderDataGraph::clear_claimed_marks(cld_claim);
  }
}

void ZRootsIterator::apply(OopClosure* cl,
                           CLDClosure* cld_cl,
                           ThreadClosure* thread_cl,
                           NMethodClosure* nm_cl) {
  _oop_storage_set.apply(cl);
  _class_loader_data_graph.apply(cld_cl);
  _java_threads.apply(thread_cl);
  if (!ClassUnloading) {
    _nmethods.apply(nm_cl);
  }
}

ZWeakOopStorageSetIterator::ZWeakOopStorageSetIterator() :
    _iter() {}

void ZWeakOopStorageSetIterator::apply(OopClosure* cl) {
  ZStatTimer timer(ZSubPhaseConcurrentWeakRootsOopStorageSet);
  _iter.oops_do(cl);
}

void ZWeakOopStorageSetIterator::report_num_dead() {
  _iter.report_num_dead();
}

void ZWeakRootsIterator::report_num_dead() {
  _oop_storage_set.iter().report_num_dead();
}

void ZWeakRootsIterator::apply(OopClosure* cl) {
  _oop_storage_set.apply(cl);
}
