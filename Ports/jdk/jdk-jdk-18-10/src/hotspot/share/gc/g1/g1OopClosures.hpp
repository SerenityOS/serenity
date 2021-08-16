/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1OOPCLOSURES_HPP
#define SHARE_GC_G1_G1OOPCLOSURES_HPP

#include "gc/g1/g1HeapRegionAttr.hpp"
#include "memory/iterator.hpp"
#include "oops/markWord.hpp"

class HeapRegion;
class G1CollectedHeap;
class G1RemSet;
class G1ConcurrentMark;
class DirtyCardToOopClosure;
class G1CMBitMap;
class G1ParScanThreadState;
class G1ScanEvacuatedObjClosure;
class G1CMTask;
class ReferenceProcessor;

class G1ScanClosureBase : public BasicOopIterateClosure {
protected:
  G1CollectedHeap* _g1h;
  G1ParScanThreadState* _par_scan_state;

  G1ScanClosureBase(G1CollectedHeap* g1h, G1ParScanThreadState* par_scan_state);
  ~G1ScanClosureBase() { }

  template <class T>
  inline void prefetch_and_push(T* p, oop const obj);

  template <class T>
  inline void handle_non_cset_obj_common(G1HeapRegionAttr const region_attr, T* p, oop const obj);
public:
  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS; }

  inline void trim_queue_partially();
};

// Used to scan cards from the DCQS or the remembered sets during garbage collection.
class G1ScanCardClosure : public G1ScanClosureBase {
public:
  G1ScanCardClosure(G1CollectedHeap* g1h,
                    G1ParScanThreadState* pss) :
    G1ScanClosureBase(g1h, pss) { }

  template <class T> void do_oop_work(T* p);
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(oop* p)       { do_oop_work(p); }
};

// Used during Optional RS scanning to make sure we trim the queues in a timely manner.
class G1ScanRSForOptionalClosure : public OopClosure {
  G1CollectedHeap* _g1h;
  G1ScanCardClosure* _scan_cl;
public:
  G1ScanRSForOptionalClosure(G1CollectedHeap* g1h, G1ScanCardClosure* cl) : _g1h(g1h), _scan_cl(cl) { }

  template <class T> void do_oop_work(T* p);
  virtual void do_oop(oop* p)          { do_oop_work(p); }
  virtual void do_oop(narrowOop* p)    { do_oop_work(p); }
};

// This closure is applied to the fields of the objects that have just been copied during evacuation.
class G1ScanEvacuatedObjClosure : public G1ScanClosureBase {
  friend class G1ScanInYoungSetter;

  enum ScanningInYoungValues {
    False = 0,
    True,
    Uninitialized
  };

  ScanningInYoungValues _scanning_in_young;

public:
  G1ScanEvacuatedObjClosure(G1CollectedHeap* g1h, G1ParScanThreadState* par_scan_state) :
    G1ScanClosureBase(g1h, par_scan_state), _scanning_in_young(Uninitialized) { }

  template <class T> void do_oop_work(T* p);
  virtual void do_oop(oop* p)          { do_oop_work(p); }
  virtual void do_oop(narrowOop* p)    { do_oop_work(p); }

  // We need to do reference discovery while processing evacuated objects.
  virtual ReferenceIterationMode reference_iteration_mode() { return DO_DISCOVERED_AND_DISCOVERY; }

  void set_ref_discoverer(ReferenceDiscoverer* rd) {
    set_ref_discoverer_internal(rd);
  }
};

// RAII object to properly set the _scanning_in_young field in G1ScanEvacuatedObjClosure.
class G1ScanInYoungSetter : public StackObj {
  G1ScanEvacuatedObjClosure* _closure;

public:
  G1ScanInYoungSetter(G1ScanEvacuatedObjClosure* closure, bool new_value) : _closure(closure) {
    assert(_closure->_scanning_in_young == G1ScanEvacuatedObjClosure::Uninitialized, "Must not be set");
    _closure->_scanning_in_young = new_value ? G1ScanEvacuatedObjClosure::True : G1ScanEvacuatedObjClosure::False;
  }

  ~G1ScanInYoungSetter() {
    DEBUG_ONLY(_closure->_scanning_in_young = G1ScanEvacuatedObjClosure::Uninitialized;)
  }
};

// Add back base class for metadata
class G1ParCopyHelper : public OopClosure {
protected:
  G1CollectedHeap* _g1h;
  G1ParScanThreadState* _par_scan_state;
  uint _worker_id;              // Cache value from par_scan_state.
  ClassLoaderData* _scanned_cld;
  G1ConcurrentMark* _cm;

  // Mark the object if it's not already marked. This is used to mark
  // objects pointed to by roots that are guaranteed not to move
  // during the GC (i.e., non-CSet objects). It is MT-safe.
  inline void mark_object(oop obj);

  G1ParCopyHelper(G1CollectedHeap* g1h,  G1ParScanThreadState* par_scan_state);

 public:
  void set_scanned_cld(ClassLoaderData* cld) { _scanned_cld = cld; }
  inline void do_cld_barrier(oop new_obj);

  inline void trim_queue_partially();
};

enum G1Barrier {
  G1BarrierNone,
  G1BarrierCLD,
  G1BarrierNoOptRoots  // Do not collect optional roots.
};

template <G1Barrier barrier, bool should_mark>
class G1ParCopyClosure : public G1ParCopyHelper {
public:
  G1ParCopyClosure(G1CollectedHeap* g1h, G1ParScanThreadState* par_scan_state) :
      G1ParCopyHelper(g1h, par_scan_state) { }

  template <class T> void do_oop_work(T* p);
  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
};

class G1CLDScanClosure : public CLDClosure {
  G1ParCopyHelper* _closure;
  bool             _process_only_dirty;
  int              _count;
public:
  G1CLDScanClosure(G1ParCopyHelper* closure, bool process_only_dirty)
  : _closure(closure), _process_only_dirty(process_only_dirty), _count(0) {}
  void do_cld(ClassLoaderData* cld);
};

// Closure for iterating over object fields during concurrent marking
class G1CMOopClosure : public MetadataVisitingOopIterateClosure {
  G1CollectedHeap*   _g1h;
  G1CMTask*          _task;
public:
  G1CMOopClosure(G1CollectedHeap* g1h,G1CMTask* task);
  template <class T> void do_oop_work(T* p);
  virtual void do_oop(      oop* p) { do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
};

// Closure to scan the root regions during concurrent marking
class G1RootRegionScanClosure : public MetadataVisitingOopIterateClosure {
private:
  G1CollectedHeap* _g1h;
  G1ConcurrentMark* _cm;
  uint _worker_id;
public:
  G1RootRegionScanClosure(G1CollectedHeap* g1h, G1ConcurrentMark* cm, uint worker_id) :
    _g1h(g1h), _cm(cm), _worker_id(worker_id) { }
  template <class T> void do_oop_work(T* p);
  virtual void do_oop(      oop* p) { do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
};

class G1ConcurrentRefineOopClosure: public BasicOopIterateClosure {
  G1CollectedHeap* _g1h;
  uint _worker_id;

public:
  G1ConcurrentRefineOopClosure(G1CollectedHeap* g1h, uint worker_id) :
    _g1h(g1h),
    _worker_id(worker_id) {
  }

  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS; }

  template <class T> void do_oop_work(T* p);
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(oop* p)       { do_oop_work(p); }
};

class G1RebuildRemSetClosure : public BasicOopIterateClosure {
  G1CollectedHeap* _g1h;
  uint _worker_id;
public:
  G1RebuildRemSetClosure(G1CollectedHeap* g1h, uint worker_id) : _g1h(g1h), _worker_id(worker_id) {
  }

  template <class T> void do_oop_work(T* p);
  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual void do_oop(narrowOop* p) { do_oop_work(p); }

  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS; }
};

#endif // SHARE_GC_G1_G1OOPCLOSURES_HPP
