/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016 SAP SE. All rights reserved.
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

#ifndef CPU_S390_FRAME_S390_INLINE_HPP
#define CPU_S390_FRAME_S390_INLINE_HPP

#include "code/codeCache.hpp"
#include "code/vmreg.inline.hpp"
#include "utilities/align.hpp"

// Inline functions for z/Architecture frames:

inline void frame::find_codeblob_and_set_pc_and_deopt_state(address pc) {
  assert(pc != NULL, "precondition: must have PC");

  _cb = CodeCache::find_blob(pc);
  _pc = pc;   // Must be set for get_deopt_original_pc().

  _fp = (intptr_t *) own_abi()->callers_sp;

  address original_pc = CompiledMethod::get_deopt_original_pc(this);
  if (original_pc != NULL) {
    _pc = original_pc;
    _deopt_state = is_deoptimized;
  } else {
    _deopt_state = not_deoptimized;
  }

  assert(((uint64_t)_sp & 0x7) == 0, "SP must be 8-byte aligned");
}

// Constructors

// Initialize all fields, _unextended_sp will be adjusted in find_codeblob_and_set_pc_and_deopt_state.
inline frame::frame() : _sp(NULL), _pc(NULL), _cb(NULL), _deopt_state(unknown), _unextended_sp(NULL), _fp(NULL) {}

inline frame::frame(intptr_t* sp) : _sp(sp), _unextended_sp(sp) {
  find_codeblob_and_set_pc_and_deopt_state((address)own_abi()->return_pc);
}

inline frame::frame(intptr_t* sp, address pc) : _sp(sp), _unextended_sp(sp) {
  find_codeblob_and_set_pc_and_deopt_state(pc); // Also sets _fp and adjusts _unextended_sp.
}

inline frame::frame(intptr_t* sp, address pc, intptr_t* unextended_sp) : _sp(sp), _unextended_sp(unextended_sp) {
  find_codeblob_and_set_pc_and_deopt_state(pc); // Also sets _fp and adjusts _unextended_sp.
}

// Generic constructor. Used by pns() in debug.cpp only
#ifndef PRODUCT
inline frame::frame(void* sp, void* pc, void* unextended_sp) :
  _sp((intptr_t*)sp), _pc(NULL), _cb(NULL), _unextended_sp((intptr_t*)unextended_sp) {
  find_codeblob_and_set_pc_and_deopt_state((address)pc); // Also sets _fp and adjusts _unextended_sp.
}
#endif

// template interpreter state
inline frame::z_ijava_state* frame::ijava_state_unchecked() const {
  z_ijava_state* state = (z_ijava_state*) ((uintptr_t)fp() - z_ijava_state_size);
  return state;
}

inline frame::z_ijava_state* frame::ijava_state() const {
  z_ijava_state* state = ijava_state_unchecked();
  assert(state->magic == (intptr_t) frame::z_istate_magic_number,
         "wrong z_ijava_state in interpreter frame (no magic found)");
  return state;
}

inline BasicObjectLock** frame::interpreter_frame_monitors_addr() const {
  return (BasicObjectLock**) &(ijava_state()->monitors);
}

// The next two funcions read and write z_ijava_state.monitors.
inline BasicObjectLock* frame::interpreter_frame_monitors() const {
  return *interpreter_frame_monitors_addr();
}
inline void frame::interpreter_frame_set_monitors(BasicObjectLock* monitors) {
  *interpreter_frame_monitors_addr() = monitors;
}

// Accessors

// Return unique id for this frame. The id must have a value where we
// can distinguish identity and younger/older relationship. NULL
// represents an invalid (incomparable) frame.
inline intptr_t* frame::id(void) const {
  // Use _fp. _sp or _unextended_sp wouldn't be correct due to resizing.
  return _fp;
}

// Return true if this frame is older (less recent activation) than
// the frame represented by id.
inline bool frame::is_older(intptr_t* id) const {
  assert(this->id() != NULL && id != NULL, "NULL frame id");
  // Stack grows towards smaller addresses on z/Architecture.
  return this->id() > id;
}

inline int frame::frame_size(RegisterMap* map) const {
  // Stack grows towards smaller addresses on z/Linux: sender is at a higher address.
  return sender_sp() - sp();
}

// Ignore c2i adapter frames.
inline intptr_t* frame::unextended_sp() const {
  return _unextended_sp;
}

inline address frame::sender_pc() const {
  return (address) callers_abi()->return_pc;
}

// Get caller pc, if caller is native from stack slot of gpr14.
inline address frame::native_sender_pc() const {
  return (address) callers_abi()->gpr14;
}

// Get caller pc from stack slot of gpr10.
inline address frame::callstub_sender_pc() const {
  return (address) callers_abi()->gpr10;
}

inline address* frame::sender_pc_addr() const {
  return (address*) &(callers_abi()->return_pc);
}

inline intptr_t* frame::sender_sp() const {
  return (intptr_t*) callers_abi();
}

inline intptr_t* frame::link() const {
  return (intptr_t*) callers_abi()->callers_sp;
}

inline intptr_t** frame::interpreter_frame_locals_addr() const {
  return (intptr_t**) &(ijava_state()->locals);
}

inline intptr_t* frame::interpreter_frame_bcp_addr() const {
  return (intptr_t*) &(ijava_state()->bcp);
}

inline intptr_t* frame::interpreter_frame_mdp_addr() const {
  return (intptr_t*) &(ijava_state()->mdx);
}

// Bottom(base) of the expression stack (highest address).
inline intptr_t* frame::interpreter_frame_expression_stack() const {
  return (intptr_t*)interpreter_frame_monitor_end() - 1;
}

inline intptr_t* frame::interpreter_frame_tos_at(jint offset) const {
  return &interpreter_frame_tos_address()[offset];
}


// monitor elements

// End is lower in memory than begin, and beginning element is oldest element.
// Also begin is one past last monitor.

inline intptr_t* frame::interpreter_frame_top_frame_sp() {
  return (intptr_t*)ijava_state()->top_frame_sp;
}

inline void frame::interpreter_frame_set_top_frame_sp(intptr_t* top_frame_sp) {
  ijava_state()->top_frame_sp = (intptr_t) top_frame_sp;
}

inline void frame::interpreter_frame_set_sender_sp(intptr_t* sender_sp) {
  ijava_state()->sender_sp = (intptr_t) sender_sp;
}

#ifdef ASSERT
inline void frame::interpreter_frame_set_magic() {
  ijava_state()->magic = (intptr_t) frame::z_istate_magic_number;
}
#endif

// Where z_ijava_state.esp is saved.
inline intptr_t** frame::interpreter_frame_esp_addr() const {
  return (intptr_t**) &(ijava_state()->esp);
}

// top of expression stack (lowest address)
inline intptr_t* frame::interpreter_frame_tos_address() const {
  return *interpreter_frame_esp_addr() + 1;
}

inline void frame::interpreter_frame_set_tos_address(intptr_t* x) {
  *interpreter_frame_esp_addr() = x - 1;
}

// Stack slot needed for native calls and GC.
inline oop * frame::interpreter_frame_temp_oop_addr() const {
  return (oop *) ((address) _fp + _z_ijava_state_neg(oop_tmp));
}

// In keeping with Intel side: end is lower in memory than begin.
// Beginning element is oldest element. Also begin is one past last monitor.
inline BasicObjectLock * frame::interpreter_frame_monitor_begin() const {
  return (BasicObjectLock*)ijava_state();
}

inline BasicObjectLock * frame::interpreter_frame_monitor_end() const {
  return interpreter_frame_monitors();
}

inline void frame::interpreter_frame_set_monitor_end(BasicObjectLock* monitors) {
  interpreter_frame_set_monitors((BasicObjectLock *)monitors);
}

inline int frame::interpreter_frame_monitor_size() {
  // Number of stack slots for a monitor
  return align_up(BasicObjectLock::size() /* number of stack slots */,
                  WordsPerLong /* Number of stack slots for a Java long. */);
}

inline int frame::interpreter_frame_monitor_size_in_bytes() {
  // Number of bytes for a monitor.
  return frame::interpreter_frame_monitor_size() * wordSize;
}

inline int frame::interpreter_frame_interpreterstate_size_in_bytes() {
  return z_ijava_state_size;
}

inline Method** frame::interpreter_frame_method_addr() const {
  return (Method**)&(ijava_state()->method);
}

inline oop* frame::interpreter_frame_mirror_addr() const {
  return (oop*)&(ijava_state()->mirror);
}

// Constant pool cache

inline ConstantPoolCache** frame::interpreter_frame_cache_addr() const {
  return (ConstantPoolCache**)&(ijava_state()->cpoolCache);
}

// entry frames

inline intptr_t* frame::entry_frame_argument_at(int offset) const {
  // Since an entry frame always calls the interpreter first,
  // the parameters are on the stack and relative to known register in the
  // entry frame.
  intptr_t* tos = (intptr_t*) entry_frame_locals()->arguments_tos_address;
  return &tos[offset + 1]; // prepushed tos
}

inline JavaCallWrapper** frame::entry_frame_call_wrapper_addr() const {
  return (JavaCallWrapper**) &entry_frame_locals()->call_wrapper_address;
}

inline oop frame::saved_oop_result(RegisterMap* map) const {
  return *((oop*) map->location(Z_R2->as_VMReg()));  // R2 is return register.
}

inline void frame::set_saved_oop_result(RegisterMap* map, oop obj) {
  *((oop*) map->location(Z_R2->as_VMReg())) = obj;  // R2 is return register.
}

inline intptr_t* frame::real_fp() const {
  return fp();
}

#endif // CPU_S390_FRAME_S390_INLINE_HPP
