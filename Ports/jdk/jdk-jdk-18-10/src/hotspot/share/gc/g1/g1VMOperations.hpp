/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_GC_G1_G1VMOPERATIONS_HPP
#define SHARE_GC_G1_G1VMOPERATIONS_HPP

#include "gc/shared/gcId.hpp"
#include "gc/shared/gcVMOperations.hpp"

// VM_operations for the G1 collector.

class VM_G1CollectFull : public VM_GC_Operation {
  bool _gc_succeeded;

protected:
  bool skip_operation() const override;

public:
  VM_G1CollectFull(uint gc_count_before,
                   uint full_gc_count_before,
                   GCCause::Cause cause) :
    VM_GC_Operation(gc_count_before, cause, full_gc_count_before, true),
    _gc_succeeded(false) { }
  VMOp_Type type() const override { return VMOp_G1CollectFull; }
  void doit() override;
  bool gc_succeeded() const { return _gc_succeeded; }
};

class VM_G1TryInitiateConcMark : public VM_GC_Operation {
  double _target_pause_time_ms;
  bool _transient_failure;
  bool _cycle_already_in_progress;
  bool _whitebox_attached;
  bool _terminating;
  bool _gc_succeeded;

public:
  VM_G1TryInitiateConcMark(uint gc_count_before,
                           GCCause::Cause gc_cause,
                           double target_pause_time_ms);
  virtual VMOp_Type type() const { return VMOp_G1TryInitiateConcMark; }
  virtual bool doit_prologue();
  virtual void doit();
  bool transient_failure() const { return _transient_failure; }
  bool cycle_already_in_progress() const { return _cycle_already_in_progress; }
  bool whitebox_attached() const { return _whitebox_attached; }
  bool terminating() const { return _terminating; }
  bool gc_succeeded() const { return _gc_succeeded; }
};

class VM_G1CollectForAllocation : public VM_CollectForAllocation {
  bool _gc_succeeded;
  double _target_pause_time_ms;

public:
  VM_G1CollectForAllocation(size_t         word_size,
                            uint           gc_count_before,
                            GCCause::Cause gc_cause,
                            double         target_pause_time_ms);
  virtual VMOp_Type type() const { return VMOp_G1CollectForAllocation; }
  virtual void doit();
  bool gc_succeeded() const { return _gc_succeeded; }

private:
  bool should_try_allocation_before_gc();
};

// Concurrent G1 stop-the-world operations such as remark and cleanup.
class VM_G1Concurrent : public VM_Operation {
  VoidClosure* _cl;
  const char*  _message;
  uint         _gc_id;

public:
  VM_G1Concurrent(VoidClosure* cl, const char* message) :
    _cl(cl), _message(message), _gc_id(GCId::current()) { }
  virtual VMOp_Type type() const { return VMOp_G1Concurrent; }
  virtual void doit();
  virtual bool doit_prologue();
  virtual void doit_epilogue();
};

#endif // SHARE_GC_G1_G1VMOPERATIONS_HPP
