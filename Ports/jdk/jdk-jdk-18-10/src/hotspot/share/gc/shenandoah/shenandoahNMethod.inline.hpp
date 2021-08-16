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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHNMETHOD_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHNMETHOD_INLINE_HPP

#include "gc/shenandoah/shenandoahNMethod.hpp"

#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shenandoah/shenandoahClosures.inline.hpp"

nmethod* ShenandoahNMethod::nm() const {
  return _nm;
}

ShenandoahReentrantLock* ShenandoahNMethod::lock() {
  return &_lock;
}

int ShenandoahNMethod::oop_count() const {
  return _oops_count + static_cast<int>(nm()->oops_end() - nm()->oops_begin());
}

bool ShenandoahNMethod::has_oops() const {
  return oop_count() > 0;
}

void ShenandoahNMethod::mark_unregistered() {
  _unregistered = true;
}

bool ShenandoahNMethod::is_unregistered() const {
  return _unregistered;
}

void ShenandoahNMethod::oops_do(OopClosure* oops, bool fix_relocations) {
  for (int c = 0; c < _oops_count; c ++) {
    oops->do_oop(_oops[c]);
  }

  oop* const begin = _nm->oops_begin();
  oop* const end = _nm->oops_end();
  for (oop* p = begin; p < end; p++) {
    if (*p != Universe::non_oop_word()) {
      oops->do_oop(p);
    }
  }

  if (fix_relocations && _has_non_immed_oops) {
    _nm->fix_oop_relocations();
  }
}

void ShenandoahNMethod::heal_nmethod_metadata(ShenandoahNMethod* nmethod_data) {
  ShenandoahEvacuateUpdateMetadataClosure<> cl;
  nmethod_data->oops_do(&cl, true /*fix relocation*/);
}

void ShenandoahNMethod::disarm_nmethod(nmethod* nm) {
  BarrierSetNMethod* const bs = BarrierSet::barrier_set()->barrier_set_nmethod();
  assert(bs != NULL || !ShenandoahNMethodBarrier,
        "Must have nmethod barrier for concurrent GC");
  if (bs != NULL && bs->is_armed(nm)) {
    bs->disarm(nm);
  }
}

ShenandoahNMethod* ShenandoahNMethod::gc_data(nmethod* nm) {
  return nm->gc_data<ShenandoahNMethod>();
}

void ShenandoahNMethod::attach_gc_data(nmethod* nm, ShenandoahNMethod* gc_data) {
  nm->set_gc_data<ShenandoahNMethod>(gc_data);
}

ShenandoahReentrantLock* ShenandoahNMethod::lock_for_nmethod(nmethod* nm) {
  return gc_data(nm)->lock();
}

bool ShenandoahNMethodTable::iteration_in_progress() const {
  shenandoah_assert_locked_or_safepoint(CodeCache_lock);
  return _itr_cnt > 0;
}

int ShenandoahNMethodList::size() const {
  return _size;
}

ShenandoahNMethod* ShenandoahNMethodList::at(int index) const {
  assert(index < size(), "Index out of bound");
  return _list[index];
}

void ShenandoahNMethodList::set(int index, ShenandoahNMethod* snm) {
  assert(index < size(), "Index out of bound");
  _list[index] = snm;
}

ShenandoahNMethod** ShenandoahNMethodList::list() const {
  return _list;
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHNMETHOD_INLINE_HPP
