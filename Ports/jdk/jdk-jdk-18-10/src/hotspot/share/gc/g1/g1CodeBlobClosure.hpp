/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1CODEBLOBCLOSURE_HPP
#define SHARE_GC_G1_G1CODEBLOBCLOSURE_HPP

#include "gc/g1/g1CollectedHeap.hpp"
#include "memory/iterator.hpp"

class G1ConcurrentMark;
class nmethod;

class G1CodeBlobClosure : public CodeBlobClosure {
  // Gather nmethod remembered set entries.
  class HeapRegionGatheringOopClosure : public OopClosure {
    G1CollectedHeap* _g1h;
    OopClosure* _work;
    nmethod* _nm;

    template <typename T>
    void do_oop_work(T* p);

  public:
    HeapRegionGatheringOopClosure(OopClosure* oc) : _g1h(G1CollectedHeap::heap()), _work(oc), _nm(NULL) {}

    void do_oop(oop* o);
    void do_oop(narrowOop* o);

    void set_nm(nmethod* nm) {
      _nm = nm;
    }
  };

  // Mark all oops below TAMS.
  class MarkingOopClosure : public OopClosure {
    G1ConcurrentMark* _cm;
    uint _worker_id;

    template <typename T>
    void do_oop_work(T* p);

  public:
    MarkingOopClosure(uint worker_id);

    void do_oop(oop* o);
    void do_oop(narrowOop* o);
  };

  HeapRegionGatheringOopClosure _oc;
  MarkingOopClosure _marking_oc;

  bool _strong;
public:
  G1CodeBlobClosure(uint worker_id, OopClosure* oc, bool strong) :
    _oc(oc), _marking_oc(worker_id), _strong(strong) { }

  void do_evacuation_and_fixup(nmethod* nm);
  void do_marking(nmethod* nm);

  void do_code_blob(CodeBlob* cb);
};

#endif // SHARE_GC_G1_G1CODEBLOBCLOSURE_HPP
