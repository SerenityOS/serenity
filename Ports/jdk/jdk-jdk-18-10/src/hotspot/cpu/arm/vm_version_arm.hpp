/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifndef CPU_ARM_VM_VERSION_ARM_HPP
#define CPU_ARM_VM_VERSION_ARM_HPP

#include "runtime/abstract_vm_version.hpp"
#include "runtime/globals_extension.hpp"

class VM_Version: public Abstract_VM_Version {
  friend class JVMCIVMStructs;

  static bool _has_simd;
  static bool _has_mp_ext;

 protected:
  // Are we done with vm version initialization
  static bool _is_initialized;

 public:
  static void initialize();
  static bool is_initialized()      { return _is_initialized; }


 protected:
  enum Feature_Flag {
    vfp = 0,
    vfp3_32 = 1,
    simd = 2,
    mp_ext = 3
  };

  enum Feature_Flag_Set {
    unknown_m           = 0,
    all_features_m      = -1,

    vfp_m     = 1 << vfp,
    vfp3_32_m = 1 << vfp3_32,
    simd_m    = 1 << simd,
    mp_ext_m  = 1 << mp_ext
  };

  // The value stored by "STR PC, [addr]" instruction can be either
  // (address of this instruction + 8) or (address of this instruction + 12)
  // depending on hardware implementation.
  // This adjustment is calculated in runtime.
  static int _stored_pc_adjustment;

  // ARM architecture version: 5 = ARMv5, 6 = ARMv6, 7 = ARMv7 etc.
  static int _arm_arch;

  // linux kernel atomic helper function version info
  // __kuser_cmpxchg() if version >= 2
  // __kuser_cmpxchg64() if version >= 5
  static int _kuser_helper_version;

#define KUSER_HELPER_VERSION_ADDR 0xffff0ffc
#define KUSER_VERSION_CMPXCHG32 2
#define KUSER_VERSION_CMPXCHG64 5

  // Read additional info using OS-specific interfaces
  static void get_os_cpu_info();

 public:
  static void early_initialize();

  static int arm_arch()             { return _arm_arch; }
  static int stored_pc_adjustment() { return _stored_pc_adjustment; }
  static bool supports_rev()        { return _arm_arch >= 6; }
  static bool supports_ldrex()      { return _arm_arch >= 6; }
  static bool supports_movw()       { return _arm_arch >= 7; }
  static bool supports_ldrexd()     { return _arm_arch >= 7; }
  static bool supports_compare_and_exchange() { return true; }
  static bool supports_kuser_cmpxchg32() { return _kuser_helper_version >= KUSER_VERSION_CMPXCHG32; }
  static bool supports_kuser_cmpxchg64() { return _kuser_helper_version >= KUSER_VERSION_CMPXCHG64; }

  static bool has_vfp()             { return (_features & vfp_m) != 0; }
  static bool has_vfp3_32()         { return (_features & vfp3_32_m) != 0; }
  static bool has_simd()            { return (_features & simd_m) != 0; }
  static bool has_multiprocessing_extensions() { return (_features & mp_ext_m) != 0; }

  static bool simd_math_is_compliant() { return false; }

  static bool prefer_moves_over_load_literal() { return supports_movw(); }

  friend class VM_Version_StubGenerator;

};

#endif // CPU_ARM_VM_VERSION_ARM_HPP
