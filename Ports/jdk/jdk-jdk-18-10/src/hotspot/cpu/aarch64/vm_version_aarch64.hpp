/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2014, 2020, Red Hat Inc. All rights reserved.
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

#ifndef CPU_AARCH64_VM_VERSION_AARCH64_HPP
#define CPU_AARCH64_VM_VERSION_AARCH64_HPP

#include "runtime/abstract_vm_version.hpp"
#include "utilities/sizes.hpp"

class VM_Version : public Abstract_VM_Version {
  friend class JVMCIVMStructs;

protected:
  static int _cpu;
  static int _model;
  static int _model2;
  static int _variant;
  static int _revision;
  static int _stepping;

  static int _zva_length;
  static int _dcache_line_size;
  static int _icache_line_size;
  static int _initial_sve_vector_length;

  // Read additional info using OS-specific interfaces
  static void get_os_cpu_info();

  // Sets the SVE length and returns a new actual value or negative on error.
  // If the len is larger than the system largest supported SVE vector length,
  // the function sets the largest supported value.
  static int set_and_get_current_sve_vector_length(int len);
  static int get_current_sve_vector_length();

public:
  // Initialization
  static void initialize();

  // Asserts
  static void assert_is_initialized() {
  }

  static bool expensive_load(int ld_size, int scale) {
    if (cpu_family() == CPU_ARM) {
      // Half-word load with index shift by 1 (aka scale is 2) has
      // extra cycle latency, e.g. ldrsh w0, [x1,w2,sxtw #1].
      if (ld_size == 2 && scale == 2) {
        return true;
      }
    }
    return false;
  }

  // The CPU implementer codes can be found in
  // ARM Architecture Reference Manual ARMv8, for ARMv8-A architecture profile
  // https://developer.arm.com/docs/ddi0487/latest
  // Arm can assign codes that are not published in the manual.
  // Apple's code is defined in
  // https://github.com/apple/darwin-xnu/blob/33eb983/osfmk/arm/cpuid.h#L62
  enum Family {
    CPU_AMPERE    = 0xC0,
    CPU_ARM       = 'A',
    CPU_BROADCOM  = 'B',
    CPU_CAVIUM    = 'C',
    CPU_DEC       = 'D',
    CPU_HISILICON = 'H',
    CPU_INFINEON  = 'I',
    CPU_MOTOROLA  = 'M',
    CPU_NVIDIA    = 'N',
    CPU_AMCC      = 'P',
    CPU_QUALCOM   = 'Q',
    CPU_MARVELL   = 'V',
    CPU_INTEL     = 'i',
    CPU_APPLE     = 'a',
  };

  enum Feature_Flag {
#define CPU_FEATURE_FLAGS(decl)               \
    decl(FP,            "fp",            0)   \
    decl(ASIMD,         "simd",          1)   \
    decl(EVTSTRM,       "evtstrm",       2)   \
    decl(AES,           "aes",           3)   \
    decl(PMULL,         "pmull",         4)   \
    decl(SHA1,          "sha1",          5)   \
    decl(SHA2,          "sha256",        6)   \
    decl(CRC32,         "crc",           7)   \
    decl(LSE,           "lse",           8)   \
    decl(DCPOP,         "dcpop",         16)  \
    decl(SHA3,          "sha3",          17)  \
    decl(SHA512,        "sha512",        21)  \
    decl(SVE,           "sve",           22)  \
    /* flags above must follow Linux HWCAP */ \
    decl(SVE2,          "sve2",          28)  \
    decl(STXR_PREFETCH, "stxr_prefetch", 29)  \
    decl(A53MAC,        "a53mac",        30)

#define DECLARE_CPU_FEATURE_FLAG(id, name, bit) CPU_##id = (1 << bit),
    CPU_FEATURE_FLAGS(DECLARE_CPU_FEATURE_FLAG)
#undef DECLARE_CPU_FEATURE_FLAG
  };

  static int cpu_family()                     { return _cpu; }
  static int cpu_model()                      { return _model; }
  static int cpu_model2()                     { return _model2; }
  static int cpu_variant()                    { return _variant; }
  static int cpu_revision()                   { return _revision; }

  static bool is_zva_enabled() { return 0 <= _zva_length; }
  static int zva_length() {
    assert(is_zva_enabled(), "ZVA not available");
    return _zva_length;
  }

  static int icache_line_size() { return _icache_line_size; }
  static int dcache_line_size() { return _dcache_line_size; }
  static int get_initial_sve_vector_length()  { return _initial_sve_vector_length; };

  static bool supports_fast_class_init_checks() { return true; }
  constexpr static bool supports_stack_watermark_barrier() { return true; }

  static void get_compatible_board(char *buf, int buflen);

#ifdef __APPLE__
  // Is the CPU running emulated (for example macOS Rosetta running x86_64 code on M1 ARM (aarch64)
  static bool is_cpu_emulated();
#endif
};

#endif // CPU_AARCH64_VM_VERSION_AARCH64_HPP
