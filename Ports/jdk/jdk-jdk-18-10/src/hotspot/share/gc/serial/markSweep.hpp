/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_SERIAL_MARKSWEEP_HPP
#define SHARE_GC_SERIAL_MARKSWEEP_HPP

#include "gc/shared/collectedHeap.hpp"
#include "gc/shared/genOopClosures.hpp"
#include "gc/shared/taskqueue.hpp"
#include "memory/iterator.hpp"
#include "oops/markWord.hpp"
#include "oops/oop.hpp"
#include "runtime/timer.hpp"
#include "utilities/growableArray.hpp"
#include "utilities/stack.hpp"

class ReferenceProcessor;
class DataLayout;
class SerialOldTracer;
class STWGCTimer;

// MarkSweep takes care of global mark-compact garbage collection for a
// GenCollectedHeap using a four-phase pointer forwarding algorithm.  All
// generations are assumed to support marking; those that can also support
// compaction.
//
// Class unloading will only occur when a full gc is invoked.

// declared at end
class PreservedMark;
class MarkAndPushClosure;
class AdjustPointerClosure;

class MarkSweep : AllStatic {
  //
  // Inline closure decls
  //
  class FollowRootClosure: public BasicOopIterateClosure {
   public:
    virtual void do_oop(oop* p);
    virtual void do_oop(narrowOop* p);
  };

  class FollowStackClosure: public VoidClosure {
   public:
    virtual void do_void();
  };

  // Used for java/lang/ref handling
  class IsAliveClosure: public BoolObjectClosure {
   public:
    virtual bool do_object_b(oop p);
  };

  class KeepAliveClosure: public OopClosure {
   protected:
    template <class T> void do_oop_work(T* p);
   public:
    virtual void do_oop(oop* p);
    virtual void do_oop(narrowOop* p);
  };

  //
  // Friend decls
  //
  friend class AdjustPointerClosure;
  friend class KeepAliveClosure;
  friend class VM_MarkSweep;

  //
  // Vars
  //
 protected:
  // Total invocations of a MarkSweep collection
  static uint _total_invocations;

  // Traversal stacks used during phase1
  static Stack<oop, mtGC>                      _marking_stack;
  static Stack<ObjArrayTask, mtGC>             _objarray_stack;

  // Space for storing/restoring mark word
  static Stack<markWord, mtGC>                 _preserved_mark_stack;
  static Stack<oop, mtGC>                      _preserved_oop_stack;
  static size_t                          _preserved_count;
  static size_t                          _preserved_count_max;
  static PreservedMark*                  _preserved_marks;

  // Reference processing (used in ...follow_contents)
  static ReferenceProcessor*             _ref_processor;

  static STWGCTimer*                     _gc_timer;
  static SerialOldTracer*                _gc_tracer;

  // Non public closures
  static KeepAliveClosure keep_alive;

 public:
  static void initialize();

  // Public closures
  static IsAliveClosure       is_alive;
  static FollowRootClosure    follow_root_closure;
  static MarkAndPushClosure   mark_and_push_closure;
  static FollowStackClosure   follow_stack_closure;
  static CLDToOopClosure      follow_cld_closure;
  static AdjustPointerClosure adjust_pointer_closure;
  static CLDToOopClosure      adjust_cld_closure;

  // Accessors
  static uint total_invocations() { return _total_invocations; }

  // Reference Processing
  static ReferenceProcessor* const ref_processor() { return _ref_processor; }
  static void set_ref_processor(ReferenceProcessor* rp);

  static STWGCTimer* gc_timer() { return _gc_timer; }
  static SerialOldTracer* gc_tracer() { return _gc_tracer; }

  static void preserve_mark(oop p, markWord mark);
                                // Save the mark word so it can be restored later
  static void adjust_marks();   // Adjust the pointers in the preserved marks table
  static void restore_marks();  // Restore the marks that we saved in preserve_mark

  static int adjust_pointers(oop obj);

  static void follow_stack();   // Empty marking stack.

  static void follow_klass(Klass* klass);

  static void follow_cld(ClassLoaderData* cld);

  template <class T> static inline void adjust_pointer(T* p);

  // Check mark and maybe push on marking stack
  template <class T> static void mark_and_push(T* p);

 private:
  // Call backs for marking
  static void mark_object(oop obj);
  // Mark pointer and follow contents.  Empty marking stack afterwards.
  template <class T> static inline void follow_root(T* p);

  static inline void push_objarray(oop obj, size_t index);

  static void follow_object(oop obj);

  static void follow_array(objArrayOop array);

  static void follow_array_chunk(objArrayOop array, int index);
};

class MarkAndPushClosure: public OopIterateClosure {
public:
  template <typename T> void do_oop_work(T* p);
  virtual void do_oop(oop* p);
  virtual void do_oop(narrowOop* p);

  virtual bool do_metadata() { return true; }
  virtual void do_klass(Klass* k);
  virtual void do_cld(ClassLoaderData* cld);

  void set_ref_discoverer(ReferenceDiscoverer* rd) {
    set_ref_discoverer_internal(rd);
  }
};

class AdjustPointerClosure: public BasicOopIterateClosure {
 public:
  template <typename T> void do_oop_work(T* p);
  virtual void do_oop(oop* p);
  virtual void do_oop(narrowOop* p);
  virtual ReferenceIterationMode reference_iteration_mode() { return DO_FIELDS; }
};

class PreservedMark {
private:
  oop _obj;
  markWord _mark;

public:
  void init(oop obj, markWord mark) {
    _obj = obj;
    _mark = mark;
  }

  void adjust_pointer();
  void restore();
};

#endif // SHARE_GC_SERIAL_MARKSWEEP_HPP
