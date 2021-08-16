/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_C1_C1_FRAMEMAP_HPP
#define SHARE_C1_C1_FRAMEMAP_HPP

#include "asm/macroAssembler.hpp"
#include "c1/c1_Defs.hpp"
#include "c1/c1_LIR.hpp"
#include "code/vmreg.hpp"
#include "memory/allocation.hpp"
#include "runtime/frame.hpp"
#include "runtime/synchronizer.hpp"
#include "utilities/globalDefinitions.hpp"
#include "utilities/macros.hpp"

class ciMethod;
class CallingConvention;

//--------------------------------------------------------
//               FrameMap
//--------------------------------------------------------

//  This class is responsible of mapping items (locals, monitors, spill
//  slots and registers to their frame location
//
//  The monitors are specified by a consecutive index, although each monitor entry
//  occupies two words. The monitor_index is 0.._num_monitors
//  The spill index is similar to local index; it is in range 0..(open)
//
//  The CPU registers are mapped using a fixed table; register with number 0
//  is the most used one.


//   stack grow direction -->                                        SP
//  +----------+---+----------+-------+------------------------+-----+
//  |arguments | x | monitors | spill | reserved argument area | ABI |
//  +----------+---+----------+-------+------------------------+-----+
//
//  x =  ABI area (SPARC) or  return adress and link (i486)
//  ABI  = ABI area (SPARC) or nothing (i486)


class LIR_OprDesc;
typedef LIR_OprDesc* LIR_Opr;


class FrameMap : public CompilationResourceObj {
 public:
  enum {
    nof_cpu_regs = pd_nof_cpu_regs_frame_map,
    nof_fpu_regs = pd_nof_fpu_regs_frame_map,

    nof_cpu_regs_reg_alloc = pd_nof_cpu_regs_reg_alloc,
    nof_fpu_regs_reg_alloc = pd_nof_fpu_regs_reg_alloc,

    max_nof_caller_save_cpu_regs = pd_nof_caller_save_cpu_regs_frame_map,
    nof_caller_save_fpu_regs     = pd_nof_caller_save_fpu_regs_frame_map,

    spill_slot_size_in_bytes = 4
  };

#include CPU_HEADER(c1_FrameMap)

  friend class LIR_OprDesc;

 private:
  static bool         _init_done;
  static Register     _cpu_rnr2reg [nof_cpu_regs];
  static int          _cpu_reg2rnr [nof_cpu_regs];

  static LIR_Opr      _caller_save_cpu_regs [max_nof_caller_save_cpu_regs];
  static LIR_Opr      _caller_save_fpu_regs [nof_caller_save_fpu_regs];

  int                 _framesize;
  int                 _argcount;
  int                 _num_monitors;
  int                 _num_spills;
  int                 _reserved_argument_area_size;
  int                 _oop_map_arg_count;

  CallingConvention*  _incoming_arguments;
  intArray*           _argument_locations;

  void check_spill_index   (int spill_index)   const { assert(spill_index   >= 0, "bad index"); }
  void check_monitor_index (int monitor_index) const { assert(monitor_index >= 0 &&
                                                              monitor_index < _num_monitors, "bad index"); }

  static Register cpu_rnr2reg (int rnr) {
    assert(_init_done, "tables not initialized");
    debug_only(cpu_range_check(rnr);)
    return _cpu_rnr2reg[rnr];
  }

  static int cpu_reg2rnr (Register reg) {
    assert(_init_done, "tables not initialized");
    debug_only(cpu_range_check(reg->encoding());)
    return _cpu_reg2rnr[reg->encoding()];
  }

  static void map_register(int rnr, Register reg) {
    debug_only(cpu_range_check(rnr);)
    debug_only(cpu_range_check(reg->encoding());)
    _cpu_rnr2reg[rnr] = reg;
    _cpu_reg2rnr[reg->encoding()] = rnr;
  }

  void update_reserved_argument_area_size (int size) {
    assert(size >= 0, "check");
    _reserved_argument_area_size = MAX2(_reserved_argument_area_size, size);
  }

 protected:
#ifndef PRODUCT
  static void cpu_range_check (int rnr)          { assert(0 <= rnr && rnr < nof_cpu_regs, "cpu register number is too big"); }
  static void fpu_range_check (int rnr)          { assert(0 <= rnr && rnr < nof_fpu_regs, "fpu register number is too big"); }
#endif


  ByteSize sp_offset_for_monitor_base(const int idx) const;

  Address make_new_address(ByteSize sp_offset) const;

  ByteSize sp_offset_for_slot(const int idx) const;
  ByteSize sp_offset_for_double_slot(const int idx) const;
  ByteSize sp_offset_for_spill(const int idx) const;
  ByteSize sp_offset_for_monitor_lock(int monitor_index) const;
  ByteSize sp_offset_for_monitor_object(int monitor_index) const;

  VMReg sp_offset2vmreg(ByteSize offset) const;

  // platform dependent hook used to check that frame is properly
  // addressable on the platform.  Used by arm, ppc to verify that all
  // stack addresses are valid.
  bool validate_frame();

  static LIR_Opr map_to_opr(BasicType type, VMRegPair* reg, bool incoming);

 public:
  // Opr representing the stack_pointer on this platform
  static LIR_Opr stack_pointer();

  // JSR 292
  static LIR_Opr method_handle_invoke_SP_save_opr();

  static BasicTypeArray*     signature_type_array_for(const ciMethod* method);

  // for outgoing calls, these also update the reserved area to
  // include space for arguments and any ABI area.
  CallingConvention* c_calling_convention(const BasicTypeArray* signature);
  CallingConvention* java_calling_convention(const BasicTypeArray* signature, bool outgoing);

  // deopt support
  ByteSize sp_offset_for_orig_pc() { return sp_offset_for_monitor_base(_num_monitors); }

  static LIR_Opr as_opr(Register r) {
    return LIR_OprFact::single_cpu(cpu_reg2rnr(r));
  }
  static LIR_Opr as_oop_opr(Register r) {
    return LIR_OprFact::single_cpu_oop(cpu_reg2rnr(r));
  }

  static LIR_Opr as_metadata_opr(Register r) {
    return LIR_OprFact::single_cpu_metadata(cpu_reg2rnr(r));
  }

  static LIR_Opr as_address_opr(Register r) {
    return LIR_OprFact::single_cpu_address(cpu_reg2rnr(r));
  }

  FrameMap(ciMethod* method, int monitors, int reserved_argument_area_size);
  bool finalize_frame(int nof_slots);

  int   reserved_argument_area_size () const     { return _reserved_argument_area_size; }
  int   framesize                   () const     { assert(_framesize != -1, "hasn't been calculated"); return _framesize; }
  ByteSize framesize_in_bytes       () const     { return in_ByteSize(framesize() * 4); }
  int   num_monitors                () const     { return _num_monitors; }
  int   num_spills                  () const     { assert(_num_spills >= 0, "not set"); return _num_spills; }
  int   argcount              () const     { assert(_argcount >= 0, "not set"); return _argcount; }

  int oop_map_arg_count() const { return _oop_map_arg_count; }

  CallingConvention* incoming_arguments() const  { return _incoming_arguments; }

  // convenience routines
  Address address_for_slot(int index, int sp_adjust = 0) const {
    return make_new_address(sp_offset_for_slot(index) + in_ByteSize(sp_adjust));
  }
  Address address_for_double_slot(int index, int sp_adjust = 0) const {
    return make_new_address(sp_offset_for_double_slot(index) + in_ByteSize(sp_adjust));
  }
  Address address_for_monitor_lock(int monitor_index) const {
    return make_new_address(sp_offset_for_monitor_lock(monitor_index));
  }
  Address address_for_monitor_object(int monitor_index) const {
    return make_new_address(sp_offset_for_monitor_object(monitor_index));
  }

  // Creates Location describing desired slot and returns it via pointer
  // to Location object. Returns true if the stack frame offset was legal
  // (as defined by Location::legal_offset_in_bytes()), false otherwise.
  // Do not use the returned location if this returns false.
  bool location_for_sp_offset(ByteSize byte_offset_from_sp,
                              Location::Type loc_type, Location* loc) const;

  bool location_for_monitor_lock  (int monitor_index, Location* loc) const {
    return location_for_sp_offset(sp_offset_for_monitor_lock(monitor_index), Location::normal, loc);
  }
  bool location_for_monitor_object(int monitor_index, Location* loc) const {
    return location_for_sp_offset(sp_offset_for_monitor_object(monitor_index), Location::oop, loc);
  }
  bool locations_for_slot  (int index, Location::Type loc_type,
                            Location* loc, Location* second = NULL) const;

  VMReg slot_regname(int index) const {
    return sp_offset2vmreg(sp_offset_for_slot(index));
  }
  VMReg monitor_object_regname(int monitor_index) const {
    return sp_offset2vmreg(sp_offset_for_monitor_object(monitor_index));
  }
  VMReg regname(LIR_Opr opr) const;

  static LIR_Opr caller_save_cpu_reg_at(int i) {
    assert(i >= 0 && i < max_nof_caller_save_cpu_regs, "out of bounds");
    return _caller_save_cpu_regs[i];
  }

  static LIR_Opr caller_save_fpu_reg_at(int i) {
    assert(i >= 0 && i < nof_caller_save_fpu_regs, "out of bounds");
    return _caller_save_fpu_regs[i];
  }

  static void initialize();
};

//               CallingConvention
//--------------------------------------------------------

class CallingConvention: public ResourceObj {
 private:
  LIR_OprList* _args;
  int          _reserved_stack_slots;

 public:
  CallingConvention (LIR_OprList* args, int reserved_stack_slots)
    : _args(args)
    , _reserved_stack_slots(reserved_stack_slots)  {}

  LIR_OprList* args()       { return _args; }

  LIR_Opr at(int i) const   { return _args->at(i); }
  int length() const        { return _args->length(); }

  // Indicates number of real frame slots used by arguments passed on stack.
  int reserved_stack_slots() const            { return _reserved_stack_slots; }

#ifndef PRODUCT
  void print () const {
    for (int i = 0; i < length(); i++) {
      at(i)->print();
    }
  }
#endif // PRODUCT
};

#endif // SHARE_C1_C1_FRAMEMAP_HPP
