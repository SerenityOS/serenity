/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1EVACFAILURE_HPP
#define SHARE_GC_G1_G1EVACFAILURE_HPP

#include "gc/g1/g1OopClosures.hpp"
#include "gc/g1/heapRegionManager.hpp"
#include "gc/shared/workgroup.hpp"
#include "utilities/globalDefinitions.hpp"

class G1CollectedHeap;
class G1RedirtyCardsQueueSet;

// Task to fixup self-forwarding pointers
// installed as a result of an evacuation failure.
class G1ParRemoveSelfForwardPtrsTask: public AbstractGangTask {
protected:
  G1CollectedHeap* _g1h;
  G1RedirtyCardsQueueSet* _rdcqs;
  HeapRegionClaimer _hrclaimer;

  uint volatile _num_failed_regions;

public:
  G1ParRemoveSelfForwardPtrsTask(G1RedirtyCardsQueueSet* rdcqs);

  void work(uint worker_id);

  uint num_failed_regions() const;
};

#endif // SHARE_GC_G1_G1EVACFAILURE_HPP
