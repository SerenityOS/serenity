/*
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
 */

#ifndef SHARE_GC_Z_ZSTACKWATERMARK_HPP
#define SHARE_GC_Z_ZSTACKWATERMARK_HPP

#include "gc/shared/barrierSet.hpp"
#include "gc/shared/barrierSetNMethod.hpp"
#include "gc/shared/threadLocalAllocBuffer.hpp"
#include "gc/z/zBarrier.hpp"
#include "memory/allocation.hpp"
#include "memory/iterator.hpp"
#include "oops/oopsHierarchy.hpp"
#include "runtime/stackWatermark.hpp"
#include "utilities/globalDefinitions.hpp"

class frame;
class JavaThread;

class ZOnStackCodeBlobClosure : public CodeBlobClosure {
private:
  BarrierSetNMethod* _bs_nm;

  virtual void do_code_blob(CodeBlob* cb);

public:
  ZOnStackCodeBlobClosure();
};

class ZStackWatermark : public StackWatermark {
private:
  ZLoadBarrierOopClosure  _jt_cl;
  ZOnStackCodeBlobClosure _cb_cl;
  ThreadLocalAllocStats   _stats;

  OopClosure* closure_from_context(void* context);

  virtual uint32_t epoch_id() const;
  virtual void start_processing_impl(void* context);
  virtual void process(const frame& fr, RegisterMap& register_map, void* context);

public:
  ZStackWatermark(JavaThread* jt);

  ThreadLocalAllocStats& stats();
};

#endif // SHARE_GC_Z_ZSTACKWATERMARK_HPP
