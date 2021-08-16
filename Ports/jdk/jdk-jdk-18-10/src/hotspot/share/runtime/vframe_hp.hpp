/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_RUNTIME_VFRAME_HP_HPP
#define SHARE_RUNTIME_VFRAME_HP_HPP

#include "runtime/vframe.hpp"

class compiledVFrame: public javaVFrame {

  friend class EscapeBarrier;

 public:
  // JVM state
  Method*                      method()             const;
  int                          bci()                const;
  bool                         should_reexecute()   const;
  StackValueCollection*        locals()             const;
  StackValueCollection*        expressions()        const;
  GrowableArray<MonitorInfo*>* monitors()           const;
  int                          vframe_id()          const { return _vframe_id; }
  bool                         has_ea_local_in_scope() const;
  bool                         arg_escape()         const; // at call with arg escape in parameter list

  void set_locals(StackValueCollection* values) const;

  // Virtuals defined in vframe
  bool is_compiled_frame() const { return true; }
  vframe* sender() const;
  bool is_top() const;

  // Casting
  static compiledVFrame* cast(vframe* vf) {
    assert(vf == NULL || vf->is_compiled_frame(), "must be compiled frame");
    return (compiledVFrame*) vf;
  }

  void update_deferred_value(BasicType type, int index, jvalue value);

  // After object deoptimization, that is object reallocation and relocking, we
  // create deferred updates for all objects in scope. No new update will be
  // created if a deferred update already exists.
  void create_deferred_updates_after_object_deoptimization();

 public:
  // Constructors
  compiledVFrame(const frame* fr, const RegisterMap* reg_map, JavaThread* thread, CompiledMethod* nm);

  // Update a local in a compiled frame. Update happens when deopt occurs
  void update_local(BasicType type, int index, jvalue value);

  // Update an expression stack value in a compiled frame. Update happens when deopt occurs
  void update_stack(BasicType type, int index, jvalue value);

  // Update a lock value in a compiled frame. Update happens when deopt occurs
  void update_monitor(int index, MonitorInfo* value);

  // Returns the active nmethod
  CompiledMethod*  code() const;

  // Returns the scopeDesc
  ScopeDesc* scope() const { return _scope; }

  // Return the compiledVFrame for the desired scope
  compiledVFrame* at_scope(int decode_offset, int vframe_id);

  // Returns SynchronizationEntryBCI or bci() (used for synchronization)
  int raw_bci() const;

 protected:
  ScopeDesc* _scope;
  int _vframe_id;

  //StackValue resolve(ScopeValue* sv) const;
  BasicLock* resolve_monitor_lock(Location location) const;
  StackValue *create_stack_value(ScopeValue *sv) const;

 private:
  compiledVFrame(const frame* fr, const RegisterMap* reg_map, JavaThread* thread, ScopeDesc* scope, int vframe_id);

#ifndef PRODUCT
 public:
  void verify() const;
#endif
};

#endif // SHARE_RUNTIME_VFRAME_HP_HPP
