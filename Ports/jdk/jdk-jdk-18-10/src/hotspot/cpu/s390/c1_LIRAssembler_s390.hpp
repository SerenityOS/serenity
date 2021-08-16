/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_S390_C1_LIRASSEMBLER_S390_HPP
#define CPU_S390_C1_LIRASSEMBLER_S390_HPP

 private:

  // Record the type of the receiver in ReceiverTypeData.
  void type_profile_helper(Register mdo, ciMethodData *md, ciProfileData *data,
                           Register recv, Register tmp1, Label* update_done);
  // Setup pointers to MDO, MDO slot, also compute offset bias to access the slot.
  void setup_md_access(ciMethod* method, int bci,
                       ciMethodData*& md, ciProfileData*& data, int& mdo_offset_bias);
 public:
  address emit_call_c(address a);

  void store_parameter(Register r, int param_num);
  void store_parameter(jint     c, int param_num);

  void check_reserved_argument_area(int bytes) {
    assert(bytes + FrameMap::first_available_sp_in_frame <= frame_map()->reserved_argument_area_size(),
           "reserved_argument_area too small");
  }

  enum {
    _call_stub_size = 512, // See Compile::MAX_stubs_size and CompiledStaticCall::emit_to_interp_stub.
    _exception_handler_size = DEBUG_ONLY(1*K) NOT_DEBUG(128),
    _deopt_handler_size = DEBUG_ONLY(1*K) NOT_DEBUG(64)
  };

#endif // CPU_S390_C1_LIRASSEMBLER_S390_HPP
