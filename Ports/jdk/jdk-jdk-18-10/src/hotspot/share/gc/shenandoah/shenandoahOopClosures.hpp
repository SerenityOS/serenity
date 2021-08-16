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

#ifndef SHARE_GC_SHENANDOAH_SHENANDOAHOOPCLOSURES_HPP
#define SHARE_GC_SHENANDOAH_SHENANDOAHOOPCLOSURES_HPP

#include "gc/shared/stringdedup/stringDedup.hpp"
#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahTaskqueue.hpp"
#include "gc/shenandoah/shenandoahUtils.hpp"
#include "memory/iterator.hpp"
#include "runtime/thread.hpp"

enum StringDedupMode {
  NO_DEDUP,      // Do not do anything for String deduplication
  ENQUEUE_DEDUP, // Enqueue candidate Strings for deduplication, if meet age threshold
  ALWAYS_DEDUP   // Enqueue Strings for deduplication
};

class ShenandoahMarkRefsSuperClosure : public MetadataVisitingOopIterateClosure {
private:
  StringDedup::Requests     _stringDedup_requests;
  ShenandoahObjToScanQueue* _queue;
  ShenandoahMarkingContext* const _mark_context;
  bool _weak;

protected:
  template <class T, StringDedupMode STRING_DEDUP>
  void work(T *p);

public:
  ShenandoahMarkRefsSuperClosure(ShenandoahObjToScanQueue* q, ShenandoahReferenceProcessor* rp);

  bool is_weak() const {
    return _weak;
  }

  void set_weak(bool weak) {
    _weak = weak;
  }
};

class ShenandoahMarkUpdateRefsSuperClosure : public ShenandoahMarkRefsSuperClosure {
protected:
  ShenandoahHeap* const _heap;

  template <class T, StringDedupMode STRING_DEDUP>
  inline void work(T* p);

public:
  ShenandoahMarkUpdateRefsSuperClosure(ShenandoahObjToScanQueue* q, ShenandoahReferenceProcessor* rp) :
    ShenandoahMarkRefsSuperClosure(q, rp),
    _heap(ShenandoahHeap::heap()) {
    assert(_heap->is_stw_gc_in_progress(), "Can only be used for STW GC");
  };
};

template <StringDedupMode STRING_DEDUP>
class ShenandoahMarkUpdateRefsClosure : public ShenandoahMarkUpdateRefsSuperClosure {
private:
  template <class T>
  inline void do_oop_work(T* p)     { work<T, STRING_DEDUP>(p); }

public:
  ShenandoahMarkUpdateRefsClosure(ShenandoahObjToScanQueue* q, ShenandoahReferenceProcessor* rp) :
    ShenandoahMarkUpdateRefsSuperClosure(q, rp) {}

  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual bool do_metadata()        { return false; }
};

template <StringDedupMode STRING_DEDUP>
class ShenandoahMarkUpdateRefsMetadataClosure : public ShenandoahMarkUpdateRefsSuperClosure {
private:
  template <class T>
  inline void do_oop_work(T* p)     { work<T, STRING_DEDUP>(p); }

public:
  ShenandoahMarkUpdateRefsMetadataClosure(ShenandoahObjToScanQueue* q, ShenandoahReferenceProcessor* rp) :
    ShenandoahMarkUpdateRefsSuperClosure(q, rp) {}

  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual bool do_metadata()        { return true; }
};


template <StringDedupMode STRING_DEDUP>
class ShenandoahMarkRefsClosure : public ShenandoahMarkRefsSuperClosure {
private:
  template <class T>
  inline void do_oop_work(T* p)     { work<T, STRING_DEDUP>(p); }

public:
  ShenandoahMarkRefsClosure(ShenandoahObjToScanQueue* q, ShenandoahReferenceProcessor* rp) :
    ShenandoahMarkRefsSuperClosure(q, rp) {};

  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual bool do_metadata()        { return false; }
};


template <StringDedupMode STRING_DEDUP>
class ShenandoahMarkRefsMetadataClosure : public ShenandoahMarkRefsSuperClosure {
private:
  template <class T>
  inline void do_oop_work(T* p)     { work<T, STRING_DEDUP>(p); }

public:
  ShenandoahMarkRefsMetadataClosure(ShenandoahObjToScanQueue* q, ShenandoahReferenceProcessor* rp) :
    ShenandoahMarkRefsSuperClosure(q, rp) {};

  virtual void do_oop(narrowOop* p) { do_oop_work(p); }
  virtual void do_oop(oop* p)       { do_oop_work(p); }
  virtual bool do_metadata()        { return true; }
};

class ShenandoahUpdateRefsSuperClosure : public BasicOopIterateClosure {
protected:
  ShenandoahHeap* _heap;

public:
  ShenandoahUpdateRefsSuperClosure() :  _heap(ShenandoahHeap::heap()) {}
};

class ShenandoahSTWUpdateRefsClosure : public ShenandoahUpdateRefsSuperClosure {
private:
  template<class T>
  inline void work(T* p);

public:
  ShenandoahSTWUpdateRefsClosure() : ShenandoahUpdateRefsSuperClosure() {
    assert(ShenandoahSafepoint::is_at_shenandoah_safepoint(), "Must only be used at safepoints");
  }

  virtual void do_oop(narrowOop* p) { work(p); }
  virtual void do_oop(oop* p)       { work(p); }
};

class ShenandoahConcUpdateRefsClosure : public ShenandoahUpdateRefsSuperClosure {
private:
  template<class T>
  inline void work(T* p);

public:
  ShenandoahConcUpdateRefsClosure() : ShenandoahUpdateRefsSuperClosure() {}

  virtual void do_oop(narrowOop* p) { work(p); }
  virtual void do_oop(oop* p)       { work(p); }
};

#endif // SHARE_GC_SHENANDOAH_SHENANDOAHOOPCLOSURES_HPP
