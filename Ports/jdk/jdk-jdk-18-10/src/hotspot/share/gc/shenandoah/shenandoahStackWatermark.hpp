/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHSTACKWATERMARK_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHSTACKWATERMARK_HPP

#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shenandoah/shenandoahClosures.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/stackWatermark.hpp"
#include "utilities/globalDefinitions.hpp"

class frame;
class JavaThread;

class ShenandoahOnStackCodeBlobClosure : public CodeBlobClosure {
private:
  BarrierSetNMethod* _bs_nm;

  void do_code_blob(CodeBlob* cb);
public:
  ShenandoahOnStackCodeBlobClosure();
};

class ShenandoahStackWatermark : public StackWatermark {
private:
  static uint32_t                      _epoch_id;
  ShenandoahHeap* const                _heap;
  ThreadLocalAllocStats                _stats;

  // Closures
  ShenandoahKeepAliveClosure           _keep_alive_cl;
  ShenandoahEvacuateUpdateRootsClosure _evac_update_oop_cl;
  ShenandoahOnStackCodeBlobClosure     _cb_cl;
public:
  ShenandoahStackWatermark(JavaThread* jt);
  ThreadLocalAllocStats& stats();

  static void change_epoch_id();
private:
  OopClosure* closure_from_context(void* context);
  uint32_t epoch_id() const;
  void start_processing_impl(void* context);
  void process(const frame& fr, RegisterMap& register_map, void* context);

  void retire_tlab();
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHSTACKWATERMARK_HPP
