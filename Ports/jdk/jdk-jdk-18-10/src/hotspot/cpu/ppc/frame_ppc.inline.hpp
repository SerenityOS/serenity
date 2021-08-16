/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2012, 2015 SAP SE. All rights reserved.
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

#ifndef CPU_PPC_FRAME_PPC_INLINE_HPP
#define CPU_PPC_FRAME_PPC_INLINE_HPP

#include "code/codeCache.hpp"
#include "code/vmreg.inline.hpp"
#include "utilities/align.hpp"

// Inline functions for ppc64 frames:

// Find codeblob and set deopt_state.
inline void frame::find_codeblob_and_set_pc_and_deopt_state(address pc) {
  assert(pc != NULL, "precondition: must have PC");

  _cb = CodeCache::find_blob(pc);
  _pc = pc;   // Must be set for get_deopt_original_pc()

  _fp = (intptr_t*)own_abi()->callers_sp;

  address original_pc = CompiledMethod::get_deopt_original_pc(this);
  if (original_pc != NULL) {
    _pc = original_pc;
    _deopt_state = is_deoptimized;
  } else {
    _deopt_state = not_deoptimized;
  }

  assert(((uint64_t)_sp & 0xf) == 0, "SP must be 16-byte aligned");
}

// Constructors

// Initialize all fields, _unextended_sp will be adjusted in find_codeblob_and_set_pc_and_deopt_state.
inline frame::frame() : _sp(NULL), _pc(NULL), _cb(NULL),  _deopt_state(unknown), _unextended_sp(NULL), _fp(NULL) {}

inline frame::frame(intptr_t* sp) : _sp(sp), _unextended_sp(sp) {
  find_codeblob_and_set_pc_and_deopt_state((address)own_abi()->lr); // also sets _fp and adjusts _unextended_sp
}

inline frame::frame(intptr_t* sp, address pc) : _sp(sp), _unextended_sp(sp) {
  find_codeblob_and_set_pc_and_deopt_state(pc); // also sets _fp and adjusts _unextended_sp
}

inline frame::frame(intptr_t* sp, address pc, intptr_t* unextended_sp) : _sp(sp), _unextended_sp(unextended_sp) {
  find_codeblob_and_set_pc_and_deopt_state(pc); // also sets _fp and adjusts _unextended_sp
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
   // Stack grows towards smaller addresses on ppc64.
   return this->id() > id;
}

inline int frame::frame_size(RegisterMap* map) const {
  // Stack grows towards smaller addresses on PPC64: sender is at a higher address.
  return sender_sp() - sp();
}

// Return the frame's stack pointer before it has been extended by a
// c2i adapter. This is needed by deoptimization for ignoring c2i adapter
// frames.
inline intptr_t* frame::unextended_sp() const {
  return _unextended_sp;
}

// All frames have this field.
inline address frame::sender_pc() const {
  return (address)callers_abi()->lr;
}
inline address* frame::sender_pc_addr() const {
  return (address*)&(callers_abi()->lr);
}

// All frames have this field.
inline intptr_t* frame::sender_sp() const {
  return (intptr_t*)callers_abi();
}

// All frames have this field.
inline intptr_t* frame::link() const {
  return (intptr_t*)callers_abi()->callers_sp;
}

inline intptr_t* frame::real_fp() const {
  return fp();
}

// Template Interpreter frame value accessors.

inline frame::ijava_state* frame::get_ijava_state() const {
  return (ijava_state*) ((uintptr_t)fp() - ijava_state_size);
}

inline intptr_t** frame::interpreter_frame_locals_addr() const {
  return (intptr_t**) &(get_ijava_state()->locals);
}

inline intptr_t* frame::interpreter_frame_bcp_addr() const {
  return (intptr_t*) &(get_ijava_state()->bcp);
}

inline intptr_t* frame::interpreter_frame_mdp_addr() const {
  return (intptr_t*) &(get_ijava_state()->mdx);
}

// Pointer beyond the "oldest/deepest" BasicObjectLock on stack.
inline BasicObjectLock* frame::interpreter_frame_monitor_end() const {
  return (BasicObjectLock*) get_ijava_state()->monitors;
}

inline BasicObjectLock* frame::interpreter_frame_monitor_begin() const {
  return (BasicObjectLock*) get_ijava_state();
}

// Return register stack slot addr at which currently interpreted method is found.
inline Method** frame::interpreter_frame_method_addr() const {
  return (Method**) &(get_ijava_state()->method);
}

inline oop* frame::interpreter_frame_mirror_addr() const {
  return (oop*) &(get_ijava_state()->mirror);
}

inline ConstantPoolCache** frame::interpreter_frame_cache_addr() const {
  return (ConstantPoolCache**) &(get_ijava_state()->cpoolCache);
}

inline oop* frame::interpreter_frame_temp_oop_addr() const {
  return (oop*) &(get_ijava_state()->oop_tmp);
}

inline intptr_t* frame::interpreter_frame_esp() const {
  return (intptr_t*) get_ijava_state()->esp;
}

// Convenient setters
inline void frame::interpreter_frame_set_monitor_end(BasicObjectLock* end)    { get_ijava_state()->monitors = (intptr_t) end;}
inline void frame::interpreter_frame_set_cpcache(ConstantPoolCache* cp)       { *interpreter_frame_cache_addr() = cp; }
inline void frame::interpreter_frame_set_esp(intptr_t* esp)                   { get_ijava_state()->esp = (intptr_t) esp; }
inline void frame::interpreter_frame_set_top_frame_sp(intptr_t* top_frame_sp) { get_ijava_state()->top_frame_sp = (intptr_t) top_frame_sp; }
inline void frame::interpreter_frame_set_sender_sp(intptr_t* sender_sp)       { get_ijava_state()->sender_sp = (intptr_t) sender_sp; }

inline intptr_t* frame::interpreter_frame_expression_stack() const {
  return (intptr_t*)interpreter_frame_monitor_end() - 1;
}

// top of expression stack
inline intptr_t* frame::interpreter_frame_tos_address() const {
  return ((intptr_t*) get_ijava_state()->esp) + Interpreter::stackElementWords;
}

inline intptr_t* frame::interpreter_frame_tos_at(jint offset) const {
  return &interpreter_frame_tos_address()[offset];
}

inline int frame::interpreter_frame_monitor_size() {
  // Number of stack slots for a monitor.
  return align_up(BasicObjectLock::size(),  // number of stack slots
                  WordsPerLong);            // number of stack slots for a Java long
}

inline int frame::interpreter_frame_monitor_size_in_bytes() {
  return frame::interpreter_frame_monitor_size() * wordSize;
}

// entry frames

inline intptr_t* frame::entry_frame_argument_at(int offset) const {
  // Since an entry frame always calls the interpreter first, the
  // parameters are on the stack and relative to known register in the
  // entry frame.
  intptr_t* tos = (intptr_t*)get_entry_frame_locals()->arguments_tos_address;
  return &tos[offset + 1]; // prepushed tos
}

inline JavaCallWrapper** frame::entry_frame_call_wrapper_addr() const {
  return (JavaCallWrapper**)&get_entry_frame_locals()->call_wrapper_address;
}

inline oop frame::saved_oop_result(RegisterMap* map) const {
  return *((oop*)map->location(R3->as_VMReg()));
}

inline void frame::set_saved_oop_result(RegisterMap* map, oop obj) {
  *((oop*)map->location(R3->as_VMReg())) = obj;
}

#endif // CPU_PPC_FRAME_PPC_INLINE_HPP
