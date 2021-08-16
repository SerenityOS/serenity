/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SHARE_OPTO_ADLCVMDEPS_HPP
#define SHARE_OPTO_ADLCVMDEPS_HPP


// adlcVMDeps.hpp is used by both adlc and vm builds.
// Don't inherit from AllStatic to avoid including memory/allocation.hpp.

// Declare commonly known constant and data structures between the
// ADLC and the VM
//

class AdlcVMDeps {   // AllStatic
 public:
  // Mirror of TypeFunc types
  enum { Control, I_O, Memory, FramePtr, ReturnAdr, Parms };

  enum Cisc_Status { Not_cisc_spillable = -1 };

  // Mirror of OptoReg::Name names
  enum Name {
    Physical = 0                // Start of physical regs
  };

  // relocInfo
  static const char* oop_reloc_type()  { return "relocInfo::oop_type"; }
  static const char* none_reloc_type() { return "relocInfo::none"; }
};

#endif // SHARE_OPTO_ADLCVMDEPS_HPP
