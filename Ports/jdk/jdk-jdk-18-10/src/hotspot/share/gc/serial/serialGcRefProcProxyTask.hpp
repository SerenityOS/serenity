/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SERIAL_SERIALGCREFPROCPROXYTASK_HPP
#define SHARE_GC_SERIAL_SERIALGCREFPROCPROXYTASK_HPP

#include "gc/shared/referenceProcessor.hpp"

class SerialGCRefProcProxyTask : public RefProcProxyTask {
  BoolObjectClosure& _is_alive;
  OopClosure& _keep_alive;
  VoidClosure& _complete_gc;

public:
  SerialGCRefProcProxyTask(BoolObjectClosure& is_alive, OopClosure& keep_alive, VoidClosure& complete_gc)
    : RefProcProxyTask("SerialGCRefProcProxyTask", 1),
      _is_alive(is_alive),
      _keep_alive(keep_alive),
      _complete_gc(complete_gc) {}

  void work(uint worker_id) override {
    assert(worker_id < _max_workers, "sanity");
    _rp_task->rp_work(worker_id, &_is_alive, &_keep_alive, &_complete_gc);
  }
};

#endif /* SHARE_GC_SERIAL_SERIALGCREFPROCPROXYTASK_HPP */
