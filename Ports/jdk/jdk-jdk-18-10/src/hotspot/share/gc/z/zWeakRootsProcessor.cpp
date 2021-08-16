/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "gc/z/zBarrier.inline.hpp"
#include "gc/z/zRootsIterator.hpp"
#include "gc/z/zTask.hpp"
#include "gc/z/zWeakRootsProcessor.hpp"
#include "gc/z/zWorkers.hpp"

class ZPhantomCleanOopClosure : public OopClosure {
public:
  virtual void do_oop(oop* p) {
    // Read the oop once, to make sure the liveness check
    // and the later clearing uses the same value.
    const oop obj = Atomic::load(p);
    if (ZBarrier::is_alive_barrier_on_phantom_oop(obj)) {
      ZBarrier::keep_alive_barrier_on_phantom_oop_field(p);
    } else {
      // The destination could have been modified/reused, in which case
      // we don't want to clear it. However, no one could write the same
      // oop here again (the object would be strongly live and we would
      // not consider clearing such oops), so therefore we don't have an
      // ABA problem here.
      Atomic::cmpxchg(p, obj, oop(NULL));
    }
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }
};

ZWeakRootsProcessor::ZWeakRootsProcessor(ZWorkers* workers) :
    _workers(workers) {}

class ZProcessWeakRootsTask : public ZTask {
private:
  ZWeakRootsIterator _weak_roots;

public:
  ZProcessWeakRootsTask() :
      ZTask("ZProcessWeakRootsTask"),
      _weak_roots() {}

  ~ZProcessWeakRootsTask() {
    _weak_roots.report_num_dead();
  }

  virtual void work() {
    ZPhantomCleanOopClosure cl;
    _weak_roots.apply(&cl);
  }
};

void ZWeakRootsProcessor::process_weak_roots() {
  ZProcessWeakRootsTask task;
  _workers->run(&task);
}
