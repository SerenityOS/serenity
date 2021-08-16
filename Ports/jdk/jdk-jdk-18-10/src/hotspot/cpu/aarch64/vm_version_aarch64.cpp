/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2020, Red Hat Inc. All rights reserved.
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
#include "runtime/arguments.hpp"
#include "runtime/globals_extension.hpp"
#include "runtime/java.hpp"
#include "runtime/os.hpp"
#include "runtime/vm_version.hpp"
#include "utilities/formatBuffer.hpp"
#include "utilities/macros.hpp"

#include OS_HEADER_INLINE(os)

int VM_Version::_cpu;
int VM_Version::_model;
int VM_Version::_model2;
int VM_Version::_variant;
int VM_Version::_revision;
int VM_Version::_stepping;

int VM_Version::_zva_length;
int VM_Version::_dcache_line_size;
int VM_Version::_icache_line_size;
int VM_Version::_initial_sve_vector_length;

void VM_Version::initialize() {
  _supports_cx8 = true;
  _supports_atomic_getset4 = true;
  _supports_atomic_getadd4 = true;
  _supports_atomic_getset8 = true;
  _supports_atomic_getadd8 = true;

  get_os_cpu_info();

  int dcache_line = VM_Version::dcache_line_size();

  // Limit AllocatePrefetchDistance so that it does not exceed the
  // constraint in AllocatePrefetchDistanceConstraintFunc.
  if (FLAG_IS_DEFAULT(AllocatePrefetchDistance))
    FLAG_SET_DEFAULT(AllocatePrefetchDistance, MIN2(512, 3*dcache_line));

  if (FLAG_IS_DEFAULT(AllocatePrefetchStepSize))
    FLAG_SET_DEFAULT(AllocatePrefetchStepSize, dcache_line);
  if (FLAG_IS_DEFAULT(PrefetchScanIntervalInBytes))
    FLAG_SET_DEFAULT(PrefetchScanIntervalInBytes, 3*dcache_line);
  if (FLAG_IS_DEFAULT(PrefetchCopyIntervalInBytes))
    FLAG_SET_DEFAULT(PrefetchCopyIntervalInBytes, 3*dcache_line);
  if (FLAG_IS_DEFAULT(SoftwarePrefetchHintDistance))
    FLAG_SET_DEFAULT(SoftwarePrefetchHintDistance, 3*dcache_line);

  if (PrefetchCopyIntervalInBytes != -1 &&
       ((PrefetchCopyIntervalInBytes & 7) || (PrefetchCopyIntervalInBytes >= 32768))) {
    warning("PrefetchCopyIntervalInBytes must be -1, or a multiple of 8 and < 32768");
    PrefetchCopyIntervalInBytes &= ~7;
    if (PrefetchCopyIntervalInBytes >= 32768)
      PrefetchCopyIntervalInBytes = 32760;
  }

  if (AllocatePrefetchDistance !=-1 && (AllocatePrefetchDistance & 7)) {
    warning("AllocatePrefetchDistance must be multiple of 8");
    AllocatePrefetchDistance &= ~7;
  }

  if (AllocatePrefetchStepSize & 7) {
    warning("AllocatePrefetchStepSize must be multiple of 8");
    AllocatePrefetchStepSize &= ~7;
  }

  if (SoftwarePrefetchHintDistance != -1 &&
       (SoftwarePrefetchHintDistance & 7)) {
    warning("SoftwarePrefetchHintDistance must be -1, or a multiple of 8");
    SoftwarePrefetchHintDistance &= ~7;
  }

  if (FLAG_IS_DEFAULT(ContendedPaddingWidth) && (dcache_line > ContendedPaddingWidth)) {
    ContendedPaddingWidth = dcache_line;
  }

  if (os::supports_map_sync()) {
    // if dcpop is available publish data cache line flush size via
    // generic field, otherwise let if default to zero thereby
    // disabling writeback
    if (_features & CPU_DCPOP) {
      _data_cache_line_flush_size = dcache_line;
    }
  }

  // Enable vendor specific features

  // Ampere eMAG
  if (_cpu == CPU_AMCC && (_model == 0) && (_variant == 0x3)) {
    if (FLAG_IS_DEFAULT(AvoidUnalignedAccesses)) {
      FLAG_SET_DEFAULT(AvoidUnalignedAccesses, true);
    }
    if (FLAG_IS_DEFAULT(UseSIMDForMemoryOps)) {
      FLAG_SET_DEFAULT(UseSIMDForMemoryOps, true);
    }
    if (FLAG_IS_DEFAULT(UseSIMDForArrayEquals)) {
      FLAG_SET_DEFAULT(UseSIMDForArrayEquals, !(_revision == 1 || _revision == 2));
    }
  }

  // ThunderX
  if (_cpu == CPU_CAVIUM && (_model == 0xA1)) {
    guarantee(_variant != 0, "Pre-release hardware no longer supported.");
    if (FLAG_IS_DEFAULT(AvoidUnalignedAccesses)) {
      FLAG_SET_DEFAULT(AvoidUnalignedAccesses, true);
    }
    if (FLAG_IS_DEFAULT(UseSIMDForMemoryOps)) {
      FLAG_SET_DEFAULT(UseSIMDForMemoryOps, (_variant > 0));
    }
    if (FLAG_IS_DEFAULT(UseSIMDForArrayEquals)) {
      FLAG_SET_DEFAULT(UseSIMDForArrayEquals, false);
    }
  }

  // ThunderX2
  if ((_cpu == CPU_CAVIUM && (_model == 0xAF)) ||
      (_cpu == CPU_BROADCOM && (_model == 0x516))) {
    if (FLAG_IS_DEFAULT(AvoidUnalignedAccesses)) {
      FLAG_SET_DEFAULT(AvoidUnalignedAccesses, true);
    }
    if (FLAG_IS_DEFAULT(UseSIMDForMemoryOps)) {
      FLAG_SET_DEFAULT(UseSIMDForMemoryOps, true);
    }
  }

  // HiSilicon TSV110
  if (_cpu == CPU_HISILICON && _model == 0xd01) {
    if (FLAG_IS_DEFAULT(AvoidUnalignedAccesses)) {
      FLAG_SET_DEFAULT(AvoidUnalignedAccesses, true);
    }
    if (FLAG_IS_DEFAULT(UseSIMDForMemoryOps)) {
      FLAG_SET_DEFAULT(UseSIMDForMemoryOps, true);
    }
  }

  // Cortex A53
  if (_cpu == CPU_ARM && (_model == 0xd03 || _model2 == 0xd03)) {
    _features |= CPU_A53MAC;
    if (FLAG_IS_DEFAULT(UseSIMDForArrayEquals)) {
      FLAG_SET_DEFAULT(UseSIMDForArrayEquals, false);
    }
  }

  // Cortex A73
  if (_cpu == CPU_ARM && (_model == 0xd09 || _model2 == 0xd09)) {
    if (FLAG_IS_DEFAULT(SoftwarePrefetchHintDistance)) {
      FLAG_SET_DEFAULT(SoftwarePrefetchHintDistance, -1);
    }
    // A73 is faster with short-and-easy-for-speculative-execution-loop
    if (FLAG_IS_DEFAULT(UseSimpleArrayEquals)) {
      FLAG_SET_DEFAULT(UseSimpleArrayEquals, true);
    }
  }

  // Neoverse N1
  if (_cpu == CPU_ARM && (_model == 0xd0c || _model2 == 0xd0c)) {
    if (FLAG_IS_DEFAULT(UseSIMDForMemoryOps)) {
      FLAG_SET_DEFAULT(UseSIMDForMemoryOps, true);
    }
  }

  if (_cpu == CPU_ARM) {
    if (FLAG_IS_DEFAULT(UseSignumIntrinsic)) {
      FLAG_SET_DEFAULT(UseSignumIntrinsic, true);
    }
  }

  if (_cpu == CPU_ARM && (_model == 0xd07 || _model2 == 0xd07)) _features |= CPU_STXR_PREFETCH;

  char buf[512];
  sprintf(buf, "0x%02x:0x%x:0x%03x:%d", _cpu, _variant, _model, _revision);
  if (_model2) sprintf(buf+strlen(buf), "(0x%03x)", _model2);
#define ADD_FEATURE_IF_SUPPORTED(id, name, bit) if (_features & CPU_##id) strcat(buf, ", " name);
  CPU_FEATURE_FLAGS(ADD_FEATURE_IF_SUPPORTED)
#undef ADD_FEATURE_IF_SUPPORTED

  _features_string = os::strdup(buf);

  if (FLAG_IS_DEFAULT(UseCRC32)) {
    UseCRC32 = (_features & CPU_CRC32) != 0;
  }

  if (UseCRC32 && (_features & CPU_CRC32) == 0) {
    warning("UseCRC32 specified, but not supported on this CPU");
    FLAG_SET_DEFAULT(UseCRC32, false);
  }

  if (FLAG_IS_DEFAULT(UseAdler32Intrinsics)) {
    FLAG_SET_DEFAULT(UseAdler32Intrinsics, true);
  }

  if (UseVectorizedMismatchIntrinsic) {
    warning("UseVectorizedMismatchIntrinsic specified, but not available on this CPU.");
    FLAG_SET_DEFAULT(UseVectorizedMismatchIntrinsic, false);
  }

  if (_features & CPU_LSE) {
    if (FLAG_IS_DEFAULT(UseLSE))
      FLAG_SET_DEFAULT(UseLSE, true);
  } else {
    if (UseLSE) {
      warning("UseLSE specified, but not supported on this CPU");
      FLAG_SET_DEFAULT(UseLSE, false);
    }
  }

  if (_features & CPU_AES) {
    UseAES = UseAES || FLAG_IS_DEFAULT(UseAES);
    UseAESIntrinsics =
        UseAESIntrinsics || (UseAES && FLAG_IS_DEFAULT(UseAESIntrinsics));
    if (UseAESIntrinsics && !UseAES) {
      warning("UseAESIntrinsics enabled, but UseAES not, enabling");
      UseAES = true;
    }
  } else {
    if (UseAES) {
      warning("AES instructions are not available on this CPU");
      FLAG_SET_DEFAULT(UseAES, false);
    }
    if (UseAESIntrinsics) {
      warning("AES intrinsics are not available on this CPU");
      FLAG_SET_DEFAULT(UseAESIntrinsics, false);
    }
  }

  if (UseAESCTRIntrinsics) {
    warning("AES/CTR intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseAESCTRIntrinsics, false);
  }

  if (FLAG_IS_DEFAULT(UseCRC32Intrinsics)) {
    UseCRC32Intrinsics = true;
  }

  if (_features & CPU_CRC32) {
    if (FLAG_IS_DEFAULT(UseCRC32CIntrinsics)) {
      FLAG_SET_DEFAULT(UseCRC32CIntrinsics, true);
    }
  } else if (UseCRC32CIntrinsics) {
    warning("CRC32C is not available on the CPU");
    FLAG_SET_DEFAULT(UseCRC32CIntrinsics, false);
  }

  if (FLAG_IS_DEFAULT(UseFMA)) {
    FLAG_SET_DEFAULT(UseFMA, true);
  }

  if (UseMD5Intrinsics) {
    warning("MD5 intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseMD5Intrinsics, false);
  }

  if (_features & (CPU_SHA1 | CPU_SHA2 | CPU_SHA3 | CPU_SHA512)) {
    if (FLAG_IS_DEFAULT(UseSHA)) {
      FLAG_SET_DEFAULT(UseSHA, true);
    }
  } else if (UseSHA) {
    warning("SHA instructions are not available on this CPU");
    FLAG_SET_DEFAULT(UseSHA, false);
  }

  if (UseSHA && (_features & CPU_SHA1)) {
    if (FLAG_IS_DEFAULT(UseSHA1Intrinsics)) {
      FLAG_SET_DEFAULT(UseSHA1Intrinsics, true);
    }
  } else if (UseSHA1Intrinsics) {
    warning("Intrinsics for SHA-1 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA1Intrinsics, false);
  }

  if (UseSHA && (_features & CPU_SHA2)) {
    if (FLAG_IS_DEFAULT(UseSHA256Intrinsics)) {
      FLAG_SET_DEFAULT(UseSHA256Intrinsics, true);
    }
  } else if (UseSHA256Intrinsics) {
    warning("Intrinsics for SHA-224 and SHA-256 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA256Intrinsics, false);
  }

  if (UseSHA && (_features & CPU_SHA3)) {
    // Do not auto-enable UseSHA3Intrinsics until it has been fully tested on hardware
    // if (FLAG_IS_DEFAULT(UseSHA3Intrinsics)) {
      // FLAG_SET_DEFAULT(UseSHA3Intrinsics, true);
    // }
  } else if (UseSHA3Intrinsics) {
    warning("Intrinsics for SHA3-224, SHA3-256, SHA3-384 and SHA3-512 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA3Intrinsics, false);
  }

  if (UseSHA && (_features & CPU_SHA512)) {
    // Do not auto-enable UseSHA512Intrinsics until it has been fully tested on hardware
    // if (FLAG_IS_DEFAULT(UseSHA512Intrinsics)) {
      // FLAG_SET_DEFAULT(UseSHA512Intrinsics, true);
    // }
  } else if (UseSHA512Intrinsics) {
    warning("Intrinsics for SHA-384 and SHA-512 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA512Intrinsics, false);
  }

  if (!(UseSHA1Intrinsics || UseSHA256Intrinsics || UseSHA3Intrinsics || UseSHA512Intrinsics)) {
    FLAG_SET_DEFAULT(UseSHA, false);
  }

  if (_features & CPU_PMULL) {
    if (FLAG_IS_DEFAULT(UseGHASHIntrinsics)) {
      FLAG_SET_DEFAULT(UseGHASHIntrinsics, true);
    }
  } else if (UseGHASHIntrinsics) {
    warning("GHASH intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseGHASHIntrinsics, false);
  }

  if (FLAG_IS_DEFAULT(UseBASE64Intrinsics)) {
    UseBASE64Intrinsics = true;
  }

  if (is_zva_enabled()) {
    if (FLAG_IS_DEFAULT(UseBlockZeroing)) {
      FLAG_SET_DEFAULT(UseBlockZeroing, true);
    }
    if (FLAG_IS_DEFAULT(BlockZeroingLowLimit)) {
      FLAG_SET_DEFAULT(BlockZeroingLowLimit, 4 * VM_Version::zva_length());
    }
  } else if (UseBlockZeroing) {
    warning("DC ZVA is not available on this CPU");
    FLAG_SET_DEFAULT(UseBlockZeroing, false);
  }

  if (_features & CPU_SVE) {
    if (FLAG_IS_DEFAULT(UseSVE)) {
      FLAG_SET_DEFAULT(UseSVE, (_features & CPU_SVE2) ? 2 : 1);
    }
    if (UseSVE > 0) {
      _initial_sve_vector_length = get_current_sve_vector_length();
    }
  } else if (UseSVE > 0) {
    warning("UseSVE specified, but not supported on current CPU. Disabling SVE.");
    FLAG_SET_DEFAULT(UseSVE, 0);
  }

  // This machine allows unaligned memory accesses
  if (FLAG_IS_DEFAULT(UseUnalignedAccesses)) {
    FLAG_SET_DEFAULT(UseUnalignedAccesses, true);
  }

  if (FLAG_IS_DEFAULT(UsePopCountInstruction)) {
    FLAG_SET_DEFAULT(UsePopCountInstruction, true);
  }

  if (!UsePopCountInstruction) {
    warning("UsePopCountInstruction is always enabled on this CPU");
    UsePopCountInstruction = true;
  }

#ifdef COMPILER2
  if (FLAG_IS_DEFAULT(UseMultiplyToLenIntrinsic)) {
    UseMultiplyToLenIntrinsic = true;
  }

  if (FLAG_IS_DEFAULT(UseSquareToLenIntrinsic)) {
    UseSquareToLenIntrinsic = true;
  }

  if (FLAG_IS_DEFAULT(UseMulAddIntrinsic)) {
    UseMulAddIntrinsic = true;
  }

  if (FLAG_IS_DEFAULT(UseMontgomeryMultiplyIntrinsic)) {
    UseMontgomeryMultiplyIntrinsic = true;
  }
  if (FLAG_IS_DEFAULT(UseMontgomerySquareIntrinsic)) {
    UseMontgomerySquareIntrinsic = true;
  }

  if (UseSVE > 0) {
    if (FLAG_IS_DEFAULT(MaxVectorSize)) {
      MaxVectorSize = _initial_sve_vector_length;
    } else if (MaxVectorSize < 16) {
      warning("SVE does not support vector length less than 16 bytes. Disabling SVE.");
      UseSVE = 0;
    } else if ((MaxVectorSize % 16) == 0 && is_power_of_2(MaxVectorSize)) {
      int new_vl = set_and_get_current_sve_vector_length(MaxVectorSize);
      _initial_sve_vector_length = new_vl;
      // Update MaxVectorSize to the largest supported value.
      if (new_vl < 0) {
        vm_exit_during_initialization(
          err_msg("Current system does not support SVE vector length for MaxVectorSize: %d",
                  (int)MaxVectorSize));
      } else if (new_vl != MaxVectorSize) {
        warning("Current system only supports max SVE vector length %d. Set MaxVectorSize to %d",
                new_vl, new_vl);
      }
      MaxVectorSize = new_vl;
    } else {
      vm_exit_during_initialization(err_msg("Unsupported MaxVectorSize: %d", (int)MaxVectorSize));
    }
  }

  if (UseSVE == 0) {  // NEON
    int min_vector_size = 8;
    int max_vector_size = 16;
    if (!FLAG_IS_DEFAULT(MaxVectorSize)) {
      if (!is_power_of_2(MaxVectorSize)) {
        vm_exit_during_initialization(err_msg("Unsupported MaxVectorSize: %d", (int)MaxVectorSize));
      } else if (MaxVectorSize < min_vector_size) {
        warning("MaxVectorSize must be at least %i on this platform", min_vector_size);
        FLAG_SET_DEFAULT(MaxVectorSize, min_vector_size);
      } else if (MaxVectorSize > max_vector_size) {
        warning("MaxVectorSize must be at most %i on this platform", max_vector_size);
        FLAG_SET_DEFAULT(MaxVectorSize, max_vector_size);
      }
    } else {
      FLAG_SET_DEFAULT(MaxVectorSize, 16);
    }
  }

  if (FLAG_IS_DEFAULT(OptoScheduling)) {
    OptoScheduling = true;
  }

  if (FLAG_IS_DEFAULT(AlignVector)) {
    AlignVector = AvoidUnalignedAccesses;
  }
#endif

  UNSUPPORTED_OPTION(CriticalJNINatives);
}
