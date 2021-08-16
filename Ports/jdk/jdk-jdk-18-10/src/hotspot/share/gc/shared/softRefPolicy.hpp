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

#ifndef SHARE_GC_SHARED_SOFTREFPOLICY_HPP
#define SHARE_GC_SHARED_SOFTREFPOLICY_HPP

#include "memory/allocation.hpp"

class SoftRefPolicy {
 private:
  // Set to true when policy wants soft refs cleared.
  // Reset to false by gc after it clears all soft refs.
  bool _should_clear_all_soft_refs;

  // Set to true by the GC if the just-completed gc cleared all
  // softrefs.  This is set to true whenever a gc clears all softrefs, and
  // set to false each time gc returns to the mutator.  For example, in the
  // ParallelScavengeHeap case the latter would be done toward the end of
  // mem_allocate() where it returns op.result()
  bool _all_soft_refs_clear;

 public:
  SoftRefPolicy();

  bool should_clear_all_soft_refs() { return _should_clear_all_soft_refs; }
  void set_should_clear_all_soft_refs(bool v) { _should_clear_all_soft_refs = v; }

  bool all_soft_refs_clear() { return _all_soft_refs_clear; }
  void set_all_soft_refs_clear(bool v) { _all_soft_refs_clear = v; }

  // Called by the GC after Soft Refs have been cleared to indicate
  // that the request in _should_clear_all_soft_refs has been fulfilled.
  virtual void cleared_all_soft_refs();
};

class ClearedAllSoftRefs : public StackObj {
  bool           _clear_all_soft_refs;
  SoftRefPolicy* _soft_ref_policy;
 public:
  ClearedAllSoftRefs(bool clear_all_soft_refs, SoftRefPolicy* soft_ref_policy) :
    _clear_all_soft_refs(clear_all_soft_refs),
    _soft_ref_policy(soft_ref_policy) {}

  ~ClearedAllSoftRefs() {
    if (_clear_all_soft_refs) {
      _soft_ref_policy->cleared_all_soft_refs();
    }
  }

  bool should_clear() { return _clear_all_soft_refs; }
};

#endif // SHARE_GC_SHARED_SOFTREFPOLICY_HPP
