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

#include "precompiled.hpp"
#include "c1/c1_FrameMap.hpp"
#include "c1/c1_LIR.hpp"
#include "code/vmreg.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "utilities/align.hpp"

//-----------------------------------------------------

// Convert method signature into an array of BasicTypes for the arguments
BasicTypeArray* FrameMap::signature_type_array_for(const ciMethod* method) {
  ciSignature* sig = method->signature();
  BasicTypeList* sta = new BasicTypeList(method->arg_size());
  // add receiver, if any
  if (!method->is_static()) sta->append(T_OBJECT);
  // add remaining arguments
  for (int i = 0; i < sig->count(); i++) {
    ciType* type = sig->type_at(i);
    BasicType t = type->basic_type();
    if (t == T_ARRAY) {
      t = T_OBJECT;
    }
    sta->append(t);
  }
  // done
  return sta;
}


CallingConvention* FrameMap::java_calling_convention(const BasicTypeArray* signature, bool outgoing) {
  // compute the size of the arguments first.  The signature array
  // that java_calling_convention takes includes a T_VOID after double
  // work items but our signatures do not.
  int i;
  int sizeargs = 0;
  for (i = 0; i < signature->length(); i++) {
    sizeargs += type2size[signature->at(i)];
  }

  BasicType* sig_bt = NEW_RESOURCE_ARRAY(BasicType, sizeargs);
  VMRegPair* regs = NEW_RESOURCE_ARRAY(VMRegPair, sizeargs);
  int sig_index = 0;
  for (i = 0; i < sizeargs; i++, sig_index++) {
    sig_bt[i] = signature->at(sig_index);
    if (sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
      sig_bt[i + 1] = T_VOID;
      i++;
    }
  }

  intptr_t out_preserve = SharedRuntime::java_calling_convention(sig_bt, regs, sizeargs);
  LIR_OprList* args = new LIR_OprList(signature->length());
  for (i = 0; i < sizeargs;) {
    BasicType t = sig_bt[i];
    assert(t != T_VOID, "should be skipping these");
    LIR_Opr opr = map_to_opr(t, regs + i, outgoing);
    args->append(opr);
    if (opr->is_address()) {
      LIR_Address* addr = opr->as_address_ptr();
      assert(addr->disp() == (int)addr->disp(), "out of range value");
      out_preserve = MAX2(out_preserve, (intptr_t)addr->disp() / 4);
    }
    i += type2size[t];
  }
  assert(args->length() == signature->length(), "size mismatch");
  out_preserve += SharedRuntime::out_preserve_stack_slots();

  if (outgoing) {
    // update the space reserved for arguments.
    update_reserved_argument_area_size(out_preserve * BytesPerWord);
  }
  return new CallingConvention(args, out_preserve);
}


CallingConvention* FrameMap::c_calling_convention(const BasicTypeArray* signature) {
  // compute the size of the arguments first.  The signature array
  // that java_calling_convention takes includes a T_VOID after double
  // work items but our signatures do not.
  int i;
  int sizeargs = 0;
  for (i = 0; i < signature->length(); i++) {
    sizeargs += type2size[signature->at(i)];
  }

  BasicType* sig_bt = NEW_RESOURCE_ARRAY(BasicType, sizeargs);
  VMRegPair* regs = NEW_RESOURCE_ARRAY(VMRegPair, sizeargs);
  int sig_index = 0;
  for (i = 0; i < sizeargs; i++, sig_index++) {
    sig_bt[i] = signature->at(sig_index);
    if (sig_bt[i] == T_LONG || sig_bt[i] == T_DOUBLE) {
      sig_bt[i + 1] = T_VOID;
      i++;
    }
  }

  intptr_t out_preserve = SharedRuntime::c_calling_convention(sig_bt, regs, NULL, sizeargs);
  LIR_OprList* args = new LIR_OprList(signature->length());
  for (i = 0; i < sizeargs;) {
    BasicType t = sig_bt[i];
    assert(t != T_VOID, "should be skipping these");

    // C calls are always outgoing
    bool outgoing = true;
    LIR_Opr opr = map_to_opr(t, regs + i, outgoing);
    // they might be of different types if for instance floating point
    // values are passed in cpu registers, but the sizes must match.
    assert(type2size[opr->type()] == type2size[t], "type mismatch");
    args->append(opr);
    if (opr->is_address()) {
      LIR_Address* addr = opr->as_address_ptr();
      out_preserve = MAX2(out_preserve, (intptr_t)addr->disp() / 4);
    }
    i += type2size[t];
  }
  assert(args->length() == signature->length(), "size mismatch");
  out_preserve += SharedRuntime::out_preserve_stack_slots();
  update_reserved_argument_area_size(out_preserve * BytesPerWord);
  return new CallingConvention(args, out_preserve);
}


//--------------------------------------------------------
//               FrameMap
//--------------------------------------------------------

bool      FrameMap::_init_done = false;
Register  FrameMap::_cpu_rnr2reg [FrameMap::nof_cpu_regs];
int       FrameMap::_cpu_reg2rnr [FrameMap::nof_cpu_regs];


FrameMap::FrameMap(ciMethod* method, int monitors, int reserved_argument_area_size) {
  assert(_init_done, "should already be completed");

  _framesize = -1;
  _num_spills = -1;

  assert(monitors >= 0, "not set");
  _num_monitors = monitors;
  assert(reserved_argument_area_size >= 0, "not set");
  _reserved_argument_area_size = MAX2(4, reserved_argument_area_size) * BytesPerWord;

  _argcount = method->arg_size();
  _argument_locations = new intArray(_argcount, _argcount, -1);
  _incoming_arguments = java_calling_convention(signature_type_array_for(method), false);
  _oop_map_arg_count = _incoming_arguments->reserved_stack_slots();

  int java_index = 0;
  for (int i = 0; i < _incoming_arguments->length(); i++) {
    LIR_Opr opr = _incoming_arguments->at(i);
    if (opr->is_address()) {
      LIR_Address* address = opr->as_address_ptr();
      _argument_locations->at_put(java_index, address->disp());
      _incoming_arguments->args()->at_put(i, LIR_OprFact::stack(java_index, as_BasicType(as_ValueType(address->type()))));
    }
    java_index += type2size[opr->type()];
  }

}


bool FrameMap::finalize_frame(int nof_slots) {
  assert(nof_slots >= 0, "must be positive");
  assert(_num_spills == -1, "can only be set once");
  _num_spills = nof_slots;
  assert(_framesize == -1, "should only be calculated once");
  _framesize =  align_up(in_bytes(sp_offset_for_monitor_base(0)) +
                         _num_monitors * (int)sizeof(BasicObjectLock) +
                         (int)sizeof(intptr_t) +                        // offset of deopt orig pc
                         frame_pad_in_bytes,
                         StackAlignmentInBytes) / 4;
  int java_index = 0;
  for (int i = 0; i < _incoming_arguments->length(); i++) {
    LIR_Opr opr = _incoming_arguments->at(i);
    if (opr->is_stack()) {
      _argument_locations->at_put(java_index, in_bytes(framesize_in_bytes()) +
                                  _argument_locations->at(java_index));
    }
    java_index += type2size[opr->type()];
  }
  // make sure it's expressible on the platform
  return validate_frame();
}

VMReg FrameMap::sp_offset2vmreg(ByteSize offset) const {
  int offset_in_bytes = in_bytes(offset);
  assert(offset_in_bytes % 4 == 0, "must be multiple of 4 bytes");
  assert(offset_in_bytes / 4 < framesize() + oop_map_arg_count(), "out of range");
  return VMRegImpl::stack2reg(offset_in_bytes / 4);
}


bool FrameMap::location_for_sp_offset(ByteSize byte_offset_from_sp,
                                      Location::Type loc_type,
                                      Location* loc) const {
  int offset = in_bytes(byte_offset_from_sp);
  assert(offset >= 0, "incorrect offset");
  if (!Location::legal_offset_in_bytes(offset)) {
    return false;
  }
  Location tmp_loc = Location::new_stk_loc(loc_type, offset);
  *loc = tmp_loc;
  return true;
}


bool FrameMap::locations_for_slot  (int index, Location::Type loc_type,
                                     Location* loc, Location* second) const {
  ByteSize offset_from_sp = sp_offset_for_slot(index);
  if (!location_for_sp_offset(offset_from_sp, loc_type, loc)) {
    return false;
  }
  if (second != NULL) {
    // two word item
    offset_from_sp = offset_from_sp + in_ByteSize(4);
    return location_for_sp_offset(offset_from_sp, loc_type, second);
  }
  return true;
}

//////////////////////
// Public accessors //
//////////////////////


ByteSize FrameMap::sp_offset_for_slot(const int index) const {
  if (index < argcount()) {
    int offset = _argument_locations->at(index);
    assert(offset != -1, "not a memory argument");
    assert(offset >= framesize() * 4, "argument inside of frame");
    return in_ByteSize(offset);
  }
  ByteSize offset = sp_offset_for_spill(index - argcount());
  assert(in_bytes(offset) < framesize() * 4, "spill outside of frame");
  return offset;
}


ByteSize FrameMap::sp_offset_for_double_slot(const int index) const {
  ByteSize offset = sp_offset_for_slot(index);
  if (index >= argcount()) {
    assert(in_bytes(offset) + 4 < framesize() * 4, "spill outside of frame");
  }
  return offset;
}


ByteSize FrameMap::sp_offset_for_spill(const int index) const {
  assert(index >= 0 && index < _num_spills, "out of range");
  int offset = align_up(first_available_sp_in_frame + _reserved_argument_area_size, (int)sizeof(double)) +
    index * spill_slot_size_in_bytes;
  return in_ByteSize(offset);
}

ByteSize FrameMap::sp_offset_for_monitor_base(const int index) const {
  int end_of_spills = align_up(first_available_sp_in_frame + _reserved_argument_area_size, (int)sizeof(double)) +
    _num_spills * spill_slot_size_in_bytes;
  int offset = align_up(end_of_spills, HeapWordSize) + index * (int)sizeof(BasicObjectLock);
  return in_ByteSize(offset);
}

ByteSize FrameMap::sp_offset_for_monitor_lock(int index) const {
  check_monitor_index(index);
  return sp_offset_for_monitor_base(index) + in_ByteSize(BasicObjectLock::lock_offset_in_bytes());;
}

ByteSize FrameMap::sp_offset_for_monitor_object(int index) const {
  check_monitor_index(index);
  return sp_offset_for_monitor_base(index) + in_ByteSize(BasicObjectLock::obj_offset_in_bytes());
}


// For OopMaps, map a local variable or spill index to an VMReg.
// This is the offset from sp() in the frame of the slot for the index,
// skewed by SharedInfo::stack0 to indicate a stack location (vs.a register.)
//
//         C ABI size +
//         framesize +     framesize +
//         stack0          stack0         stack0          0 <- VMReg->value()
//            |              |              | <registers> |
//  ..........|..............|..............|.............|
//    0 1 2 3 | <C ABI area> | 4 5 6 ...... |               <- local indices
//    ^                        ^          sp()
//    |                        |
//  arguments            non-argument locals


VMReg FrameMap::regname(LIR_Opr opr) const {
  if (opr->is_single_cpu()) {
    assert(!opr->is_virtual(), "should not see virtual registers here");
    return opr->as_register()->as_VMReg();
  } else if (opr->is_single_stack()) {
    return sp_offset2vmreg(sp_offset_for_slot(opr->single_stack_ix()));
  } else if (opr->is_address()) {
    LIR_Address* addr = opr->as_address_ptr();
    assert(addr->base() == stack_pointer(), "sp based addressing only");
    return sp_offset2vmreg(in_ByteSize(addr->index()->as_jint()));
  }
  ShouldNotReachHere();
  return VMRegImpl::Bad();
}




// ------------ extra spill slots ---------------
