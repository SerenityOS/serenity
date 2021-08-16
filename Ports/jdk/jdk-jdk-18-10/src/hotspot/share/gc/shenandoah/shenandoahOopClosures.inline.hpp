/*
 * Copyright (c) 2015, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHOOPCLOSURES_INLINE_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHOOPCLOSURES_INLINE_HPP

#include "gc/shenandoah/shenandoahOopClosures.hpp"

#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahMark.inline.hpp"

template<class T, StringDedupMode STRING_DEDUP>
inline void ShenandoahMarkRefsSuperClosure::work(T* p) {
  ShenandoahMark::mark_through_ref<T, STRING_DEDUP>(p, _queue, _mark_context, &_stringDedup_requests, _weak);
}

template<class T, StringDedupMode STRING_DEDUP>
inline void ShenandoahMarkUpdateRefsSuperClosure::work(T* p) {
  // Update the location
  _heap->update_with_forwarded(p);

  // ...then do the usual thing
  ShenandoahMarkRefsSuperClosure::work<T, STRING_DEDUP>(p);
}

template<class T>
inline void ShenandoahSTWUpdateRefsClosure::work(T* p) {
  _heap->update_with_forwarded(p);
}

template<class T>
inline void ShenandoahConcUpdateRefsClosure::work(T* p) {
  _heap->conc_update_with_forwarded(p);
}

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHOOPCLOSURES_INLINE_HPP
