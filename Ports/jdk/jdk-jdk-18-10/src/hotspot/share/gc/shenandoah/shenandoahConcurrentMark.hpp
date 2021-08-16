/*
 * Copyright (c) 2013, 2021, Red Hat, Inc. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHCONCURRENTMARK_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHCONCURRENTMARK_HPP

#include "gc/shenandoah/shenandoahMark.hpp"

class ShenandoahConcurrentMarkingTask;
class ShenandoahFinalMarkingTask;

class ShenandoahConcurrentMark: public ShenandoahMark {
  friend class ShenandoahConcurrentMarkingTask;
  friend class ShenandoahFinalMarkingTask;

public:
  ShenandoahConcurrentMark();
  // Concurrent mark roots
  void mark_concurrent_roots();
  // Concurrent mark
  void concurrent_mark();
  // Finish mark at a safepoint
  void finish_mark();

  static void cancel();

private:
  void finish_mark_work();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHCONCURRENTMARK_HPP
