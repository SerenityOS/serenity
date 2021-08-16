/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "classfile/classLoaderData.hpp"
#include "gc/shared/gc_globals.hpp"
#include "gc/z/zAddress.inline.hpp"
#include "gc/z/zHeap.inline.hpp"
#include "gc/z/zNMethod.hpp"
#include "gc/z/zOop.hpp"
#include "gc/z/zPageAllocator.hpp"
#include "gc/z/zResurrection.hpp"
#include "gc/z/zRootsIterator.hpp"
#include "gc/z/zStackWatermark.hpp"
#include "gc/z/zStat.hpp"
#include "gc/z/zVerify.hpp"
#include "memory/iterator.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.hpp"
#include "runtime/frame.inline.hpp"
#include "runtime/globals.hpp"
#include "runtime/handles.hpp"
#include "runtime/safepoint.hpp"
#include "runtime/stackFrameStream.inline.hpp"
#include "runtime/stackWatermark.inline.hpp"
#include "runtime/stackWatermarkSet.inline.hpp"
#include "runtime/thread.hpp"
#include "utilities/debug.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/preserveException.hpp"

#define BAD_OOP_ARG(o, p)   "Bad oop " PTR_FORMAT " found at " PTR_FORMAT, p2i(o), p2i(p)

static void z_verify_oop(oop* p) {
  const oop o = RawAccess<>::oop_load(p);
  if (o != NULL) {
    const uintptr_t addr = ZOop::to_address(o);
    guarantee(ZAddress::is_good(addr), BAD_OOP_ARG(o, p));
    guarantee(oopDesc::is_oop(ZOop::from_address(addr)), BAD_OOP_ARG(o, p));
  }
}

static void z_verify_possibly_weak_oop(oop* p) {
  const oop o = RawAccess<>::oop_load(p);
  if (o != NULL) {
    const uintptr_t addr = ZOop::to_address(o);
    guarantee(ZAddress::is_good(addr) || ZAddress::is_finalizable_good(addr), BAD_OOP_ARG(o, p));
    guarantee(oopDesc::is_oop(ZOop::from_address(ZAddress::good(addr))), BAD_OOP_ARG(o, p));
  }
}

class ZVerifyRootClosure : public OopClosure {
private:
  const bool _verify_fixed;

public:
  ZVerifyRootClosure(bool verify_fixed) :
      _verify_fixed(verify_fixed) {}

  virtual void do_oop(oop* p) {
    if (_verify_fixed) {
      z_verify_oop(p);
    } else {
      // Don't know the state of the oop.
      oop obj = *p;
      obj = NativeAccess<AS_NO_KEEPALIVE>::oop_load(&obj);
      z_verify_oop(&obj);
    }
  }

  virtual void do_oop(narrowOop*) {
    ShouldNotReachHere();
  }

  bool verify_fixed() const {
    return _verify_fixed;
  }
};

class ZVerifyCodeBlobClosure : public CodeBlobToOopClosure {
public:
  ZVerifyCodeBlobClosure(ZVerifyRootClosure* _cl) :
      CodeBlobToOopClosure(_cl, false /* fix_relocations */) {}

  virtual void do_code_blob(CodeBlob* cb) {
    CodeBlobToOopClosure::do_code_blob(cb);
  }
};

class ZVerifyStack : public OopClosure {
private:
  ZVerifyRootClosure* const _cl;
  JavaThread*         const _jt;
  uint64_t                  _last_good;
  bool                      _verifying_bad_frames;

public:
  ZVerifyStack(ZVerifyRootClosure* cl, JavaThread* jt) :
      _cl(cl),
      _jt(jt),
      _last_good(0),
      _verifying_bad_frames(false) {
    ZStackWatermark* const stack_watermark = StackWatermarkSet::get<ZStackWatermark>(jt, StackWatermarkKind::gc);

    if (_cl->verify_fixed()) {
      assert(stack_watermark->processing_started(), "Should already have been fixed");
      assert(stack_watermark->processing_completed(), "Should already have been fixed");
    } else {
      // We don't really know the state of the stack, verify watermark.
      if (!stack_watermark->processing_started()) {
        _verifying_bad_frames = true;
      } else {
        // Not time yet to verify bad frames
        _last_good = stack_watermark->last_processed();
      }
    }
  }

  void do_oop(oop* p) {
    if (_verifying_bad_frames) {
      const oop obj = *p;
      guarantee(!ZAddress::is_good(ZOop::to_address(obj)), BAD_OOP_ARG(obj, p));
    }
    _cl->do_oop(p);
  }

  void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }

  void prepare_next_frame(frame& frame) {
    if (_cl->verify_fixed()) {
      // All frames need to be good
      return;
    }

    // The verification has two modes, depending on whether we have reached the
    // last processed frame or not. Before it is reached, we expect everything to
    // be good. After reaching it, we expect everything to be bad.
    const uintptr_t sp = reinterpret_cast<uintptr_t>(frame.sp());

    if (!_verifying_bad_frames && sp == _last_good) {
      // Found the last good frame, now verify the bad ones
      _verifying_bad_frames = true;
    }
  }

  void verify_frames() {
    ZVerifyCodeBlobClosure cb_cl(_cl);
    for (StackFrameStream frames(_jt, true /* update */, false /* process_frames */);
         !frames.is_done();
         frames.next()) {
      frame& frame = *frames.current();
      frame.oops_do(this, &cb_cl, frames.register_map(), DerivedPointerIterationMode::_ignore);
      prepare_next_frame(frame);
    }
  }
};

class ZVerifyOopClosure : public ClaimMetadataVisitingOopIterateClosure {
private:
  const bool _verify_weaks;

public:
  ZVerifyOopClosure(bool verify_weaks) :
      ClaimMetadataVisitingOopIterateClosure(ClassLoaderData::_claim_other),
      _verify_weaks(verify_weaks) {}

  virtual void do_oop(oop* p) {
    if (_verify_weaks) {
      z_verify_possibly_weak_oop(p);
    } else {
      // We should never encounter finalizable oops through strong
      // paths. This assumes we have only visited strong roots.
      z_verify_oop(p);
    }
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }

  virtual ReferenceIterationMode reference_iteration_mode() {
    return _verify_weaks ? DO_FIELDS : DO_FIELDS_EXCEPT_REFERENT;
  }
};

typedef ClaimingCLDToOopClosure<ClassLoaderData::_claim_none> ZVerifyCLDClosure;

class ZVerifyThreadClosure : public ThreadClosure {
private:
  ZVerifyRootClosure* const _cl;

public:
  ZVerifyThreadClosure(ZVerifyRootClosure* cl) :
      _cl(cl) {}

  virtual void do_thread(Thread* thread) {
    thread->oops_do_no_frames(_cl, NULL);

    JavaThread* const jt = JavaThread::cast(thread);
    if (!jt->has_last_Java_frame()) {
      return;
    }

    ZVerifyStack verify_stack(_cl, jt);
    verify_stack.verify_frames();
  }
};

class ZVerifyNMethodClosure : public NMethodClosure {
private:
  OopClosure* const        _cl;
  BarrierSetNMethod* const _bs_nm;
  const bool               _verify_fixed;

  bool trust_nmethod_state() const {
    // The root iterator will visit non-processed
    // nmethods class unloading is turned off.
    return ClassUnloading || _verify_fixed;
  }

public:
  ZVerifyNMethodClosure(OopClosure* cl, bool verify_fixed) :
      _cl(cl),
      _bs_nm(BarrierSet::barrier_set()->barrier_set_nmethod()),
      _verify_fixed(verify_fixed) {}

  virtual void do_nmethod(nmethod* nm) {
    assert(!trust_nmethod_state() || !_bs_nm->is_armed(nm), "Should not encounter any armed nmethods");

    ZNMethod::nmethod_oops_do(nm, _cl);
  }
};

void ZVerify::roots_strong(bool verify_fixed) {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at a safepoint");
  assert(!ZResurrection::is_blocked(), "Invalid phase");

  ZVerifyRootClosure cl(verify_fixed);
  ZVerifyCLDClosure cld_cl(&cl);
  ZVerifyThreadClosure thread_cl(&cl);
  ZVerifyNMethodClosure nm_cl(&cl, verify_fixed);

  ZRootsIterator iter(ClassLoaderData::_claim_none);
  iter.apply(&cl,
             &cld_cl,
             &thread_cl,
             &nm_cl);
}

void ZVerify::roots_weak() {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at a safepoint");
  assert(!ZResurrection::is_blocked(), "Invalid phase");

  ZVerifyRootClosure cl(true /* verify_fixed */);
  ZWeakRootsIterator iter;
  iter.apply(&cl);
}

void ZVerify::objects(bool verify_weaks) {
  assert(SafepointSynchronize::is_at_safepoint(), "Must be at a safepoint");
  assert(ZGlobalPhase == ZPhaseMarkCompleted, "Invalid phase");
  assert(!ZResurrection::is_blocked(), "Invalid phase");

  ZVerifyOopClosure cl(verify_weaks);
  ObjectToOopClosure object_cl(&cl);
  ZHeap::heap()->object_iterate(&object_cl, verify_weaks);
}

void ZVerify::before_zoperation() {
  // Verify strong roots
  ZStatTimerDisable disable;
  if (ZVerifyRoots) {
    roots_strong(false /* verify_fixed */);
  }
}

void ZVerify::after_mark() {
  // Verify all strong roots and strong references
  ZStatTimerDisable disable;
  if (ZVerifyRoots) {
    roots_strong(true /* verify_fixed */);
  }
  if (ZVerifyObjects) {
    objects(false /* verify_weaks */);
  }
}

void ZVerify::after_weak_processing() {
  // Verify all roots and all references
  ZStatTimerDisable disable;
  if (ZVerifyRoots) {
    roots_strong(true /* verify_fixed */);
    roots_weak();
  }
  if (ZVerifyObjects) {
    objects(true /* verify_weaks */);
  }
}

template <bool Map>
class ZPageDebugMapOrUnmapClosure : public ZPageClosure {
private:
  const ZPageAllocator* const _allocator;

public:
  ZPageDebugMapOrUnmapClosure(const ZPageAllocator* allocator) :
      _allocator(allocator) {}

  void do_page(const ZPage* page) {
    if (Map) {
      _allocator->debug_map_page(page);
    } else {
      _allocator->debug_unmap_page(page);
    }
  }
};

ZVerifyViewsFlip::ZVerifyViewsFlip(const ZPageAllocator* allocator) :
    _allocator(allocator) {
  if (ZVerifyViews) {
    // Unmap all pages
    ZPageDebugMapOrUnmapClosure<false /* Map */> cl(_allocator);
    ZHeap::heap()->pages_do(&cl);
  }
}

ZVerifyViewsFlip::~ZVerifyViewsFlip() {
  if (ZVerifyViews) {
    // Map all pages
    ZPageDebugMapOrUnmapClosure<true /* Map */> cl(_allocator);
    ZHeap::heap()->pages_do(&cl);
  }
}

#ifdef ASSERT

class ZVerifyBadOopClosure : public OopClosure {
public:
  virtual void do_oop(oop* p) {
    const oop o = *p;
    assert(!ZAddress::is_good(ZOop::to_address(o)), "Should not be good: " PTR_FORMAT, p2i(o));
  }

  virtual void do_oop(narrowOop* p) {
    ShouldNotReachHere();
  }
};

// This class encapsulates various marks we need to deal with calling the
// frame iteration code from arbitrary points in the runtime. It is mostly
// due to problems that we might want to eventually clean up inside of the
// frame iteration code, such as creating random handles even though there
// is no safepoint to protect against, and fiddling around with exceptions.
class StackWatermarkProcessingMark {
  ResetNoHandleMark     _rnhm;
  HandleMark            _hm;
  PreserveExceptionMark _pem;
  ResourceMark          _rm;

public:
  StackWatermarkProcessingMark(Thread* thread) :
      _rnhm(),
      _hm(thread),
      _pem(thread),
      _rm(thread) {}
};

void ZVerify::verify_frame_bad(const frame& fr, RegisterMap& register_map) {
  ZVerifyBadOopClosure verify_cl;
  fr.oops_do(&verify_cl, NULL, &register_map, DerivedPointerIterationMode::_ignore);
}

void ZVerify::verify_thread_head_bad(JavaThread* jt) {
  ZVerifyBadOopClosure verify_cl;
  jt->oops_do_no_frames(&verify_cl, NULL);
}

void ZVerify::verify_thread_frames_bad(JavaThread* jt) {
  if (jt->has_last_Java_frame()) {
    ZVerifyBadOopClosure verify_cl;
    StackWatermarkProcessingMark swpm(Thread::current());
    // Traverse the execution stack
    for (StackFrameStream fst(jt, true /* update */, false /* process_frames */); !fst.is_done(); fst.next()) {
      fst.current()->oops_do(&verify_cl, NULL /* code_cl */, fst.register_map(), DerivedPointerIterationMode::_ignore);
    }
  }
}

#endif // ASSERT
