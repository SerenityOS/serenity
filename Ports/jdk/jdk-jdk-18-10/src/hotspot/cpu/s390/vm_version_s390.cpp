/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2016, 2021 SAP SE. All rights reserved.
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
#include "jvm.h"
#include "asm/assembler.inline.hpp"
#include "compiler/disassembler.hpp"
#include "code/compiledIC.hpp"
#include "memory/resourceArea.hpp"
#include "runtime/java.hpp"
#include "runtime/stubCodeGenerator.hpp"
#include "runtime/vm_version.hpp"

# include <sys/sysinfo.h>

bool VM_Version::_is_determine_features_test_running  = false;
const char*   VM_Version::_model_string;

unsigned long VM_Version::_features[_features_buffer_len]              = {0, 0, 0, 0};
unsigned long VM_Version::_cipher_features_KM[_features_buffer_len]    = {0, 0, 0, 0};
unsigned long VM_Version::_cipher_features_KMA[_features_buffer_len]   = {0, 0, 0, 0};
unsigned long VM_Version::_cipher_features_KMF[_features_buffer_len]   = {0, 0, 0, 0};
unsigned long VM_Version::_cipher_features_KMCTR[_features_buffer_len] = {0, 0, 0, 0};
unsigned long VM_Version::_cipher_features_KMO[_features_buffer_len]   = {0, 0, 0, 0};
unsigned long VM_Version::_msgdigest_features[_features_buffer_len]    = {0, 0, 0, 0};
unsigned int  VM_Version::_nfeatures                                   = 0;
unsigned int  VM_Version::_ncipher_features_KM                         = 0;
unsigned int  VM_Version::_ncipher_features_KMA                        = 0;
unsigned int  VM_Version::_ncipher_features_KMF                        = 0;
unsigned int  VM_Version::_ncipher_features_KMCTR                      = 0;
unsigned int  VM_Version::_ncipher_features_KMO                        = 0;
unsigned int  VM_Version::_nmsgdigest_features                         = 0;
unsigned int  VM_Version::_Dcache_lineSize                             = DEFAULT_CACHE_LINE_SIZE;
unsigned int  VM_Version::_Icache_lineSize                             = DEFAULT_CACHE_LINE_SIZE;

// The following list contains the (approximate) announcement/availability
// dates of the many System z generations in existence as of now.
// Information compiled from https://www.ibm.com/support/techdocs/atsmastr.nsf/WebIndex/TD105503
//   z900: 2000-10
//   z990: 2003-06
//   z9:   2005-09
//   z10:  2007-04
//   z10:  2008-02
//   z196: 2010-08
//   ec12: 2012-09
//   z13:  2015-03
//   z14:  2017-09
//   z15:  2019-09

static const char* z_gen[]      = {"  ", "G1",         "G2",         "G3",         "G4",         "G5",         "G6",         "G7",         "G8",         "G9"  };
static const char* z_machine[]  = {"  ", "2064",       "2084",       "2094",       "2097",       "2817",       "2827",       "2964",       "3906",       "8561" };
static const char* z_name[]     = {"  ", "z900",       "z990",       "z9 EC",      "z10 EC",     "z196 EC",    "ec12",       "z13",        "z14",        "z15" };
static const char* z_WDFM[]     = {"  ", "2006-06-30", "2008-06-30", "2010-06-30", "2012-06-30", "2014-06-30", "2016-12-31", "2019-06-30", "2021-06-30", "tbd" };
static const char* z_EOS[]      = {"  ", "2014-12-31", "2014-12-31", "2017-10-31", "2019-12-31", "2021-12-31", "tbd",        "tbd",        "tbd",        "tbd" };
static const char* z_features[] = {"  ",
                                   "system-z, g1-z900, ldisp",
                                   "system-z, g2-z990, ldisp_fast",
                                   "system-z, g3-z9, ldisp_fast, extimm",
                                   "system-z, g4-z10, ldisp_fast, extimm, pcrel_load/store, cmpb",
                                   "system-z, g5-z196, ldisp_fast, extimm, pcrel_load/store, cmpb, cond_load/store, interlocked_update",
                                   "system-z, g6-ec12, ldisp_fast, extimm, pcrel_load/store, cmpb, cond_load/store, interlocked_update, txm",
                                   "system-z, g7-z13, ldisp_fast, extimm, pcrel_load/store, cmpb, cond_load/store, interlocked_update, txm, vectorinstr",
                                   "system-z, g8-z14, ldisp_fast, extimm, pcrel_load/store, cmpb, cond_load/store, interlocked_update, txm, vectorinstr, instrext2, venh1)",
                                   "system-z, g9-z15, ldisp_fast, extimm, pcrel_load/store, cmpb, cond_load/store, interlocked_update, txm, vectorinstr, instrext2, venh1, instrext3, VEnh2 )"
                                  };

void VM_Version::initialize() {
  determine_features();      // Get processor capabilities.
  set_features_string();     // Set a descriptive feature indication.

  if (Verbose || PrintAssembly || PrintStubCode) {
    print_features_internal("CPU Version as detected internally:", PrintAssembly || PrintStubCode);
  }

  intx cache_line_size = Dcache_lineSize(0);

#ifdef COMPILER2
  MaxVectorSize = 8;
#endif

  if (has_PrefetchRaw()) {
    if (FLAG_IS_DEFAULT(AllocatePrefetchStyle)) {  // not preset
      // 0 = no prefetch.
      // 1 = Prefetch instructions for each allocation.
      // 2 = Use TLAB watermark to gate allocation prefetch.
      AllocatePrefetchStyle = 1;
    }

    if (AllocatePrefetchStyle > 0) {  // Prefetching turned on at all?
      // Distance to prefetch ahead of allocation pointer.
      if (FLAG_IS_DEFAULT(AllocatePrefetchDistance) || (AllocatePrefetchDistance < 0)) {  // not preset
        AllocatePrefetchDistance = 0;
      }

      // Number of lines to prefetch ahead of allocation pointer.
      if (FLAG_IS_DEFAULT(AllocatePrefetchLines) || (AllocatePrefetchLines <= 0)) {      // not preset
        AllocatePrefetchLines = 3;
      }

      // Step size in bytes of sequential prefetch instructions.
      if (FLAG_IS_DEFAULT(AllocatePrefetchStepSize) || (AllocatePrefetchStepSize <= 0)) { // not preset
        FLAG_SET_DEFAULT(AllocatePrefetchStepSize, cache_line_size);
      } else if (AllocatePrefetchStepSize < cache_line_size) {
        FLAG_SET_DEFAULT(AllocatePrefetchStepSize, cache_line_size);
      } else {
        FLAG_SET_DEFAULT(AllocatePrefetchStepSize, cache_line_size);
      }
    } else {
      FLAG_SET_DEFAULT(AllocatePrefetchStyle, 0);
      AllocatePrefetchDistance = 0;
      AllocatePrefetchLines    = 0;
      // Can't be zero. Will SIGFPE during constraints checking.
      FLAG_SET_DEFAULT(AllocatePrefetchStepSize, cache_line_size);
    }

  } else {
    FLAG_SET_DEFAULT(AllocatePrefetchStyle, 0);
    AllocatePrefetchDistance = 0;
    AllocatePrefetchLines    = 0;
    // Can't be zero. Will SIGFPE during constraints checking.
    FLAG_SET_DEFAULT(AllocatePrefetchStepSize, cache_line_size);
  }

  // TODO:
  // On z/Architecture, cache line size is significantly large (256 bytes). Do we really need
  // to keep contended members that far apart? Performance tests are required.
  if (FLAG_IS_DEFAULT(ContendedPaddingWidth) && (cache_line_size > ContendedPaddingWidth)) {
    ContendedPaddingWidth = cache_line_size;
  }

  // On z/Architecture, the CRC32/CRC32C intrinsics are implemented "by hand".
  // TODO: Provide implementation based on the vector instructions available from z13.
  // Note: The CHECKSUM instruction, which has been there since the very beginning
  //       (of z/Architecture), computes "some kind of" a checksum.
  //       It has nothing to do with the CRC32 algorithm.
  if (FLAG_IS_DEFAULT(UseCRC32Intrinsics)) {
    FLAG_SET_DEFAULT(UseCRC32Intrinsics, true);
  }
  if (FLAG_IS_DEFAULT(UseCRC32CIntrinsics)) {
    FLAG_SET_DEFAULT(UseCRC32CIntrinsics, true);
  }

  // TODO: Provide implementation.
  if (UseAdler32Intrinsics) {
    warning("Adler32Intrinsics not available on this CPU.");
    FLAG_SET_DEFAULT(UseAdler32Intrinsics, false);
  }

  // On z/Architecture, we take UseAES as the general switch to enable/disable the AES intrinsics.
  // The specific, and yet to be defined, switches UseAESxxxIntrinsics will then be set
  // depending on the actual machine capabilities.
  // Explicitly setting them via CmdLine option takes precedence, of course.
  // TODO: UseAESIntrinsics must be made keylength specific.
  // As of March 2015 and Java8, only AES128 is supported by the Java Cryptographic Extensions.
  // Therefore, UseAESIntrinsics is of minimal use at the moment.
  if (FLAG_IS_DEFAULT(UseAES) && has_Crypto_AES()) {
    FLAG_SET_DEFAULT(UseAES, true);
  }
  if (UseAES && !has_Crypto_AES()) {
    warning("AES instructions are not available on this CPU");
    FLAG_SET_DEFAULT(UseAES, false);
  }
  if (UseAES) {
    if (FLAG_IS_DEFAULT(UseAESIntrinsics)) {
      FLAG_SET_DEFAULT(UseAESIntrinsics, true);
    }
  }
  if (UseAESIntrinsics && !has_Crypto_AES()) {
    warning("AES intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseAESIntrinsics, false);
  }
  if (UseAESIntrinsics && !UseAES) {
    warning("AES intrinsics require UseAES flag to be enabled. Intrinsics will be disabled.");
    FLAG_SET_DEFAULT(UseAESIntrinsics, false);
  }

  // TODO: implement AES/CTR intrinsics
  if (UseAESCTRIntrinsics) {
    warning("AES/CTR intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseAESCTRIntrinsics, false);
  }

  if (FLAG_IS_DEFAULT(UseGHASHIntrinsics) && has_Crypto_GHASH()) {
    FLAG_SET_DEFAULT(UseGHASHIntrinsics, true);
  }
  if (UseGHASHIntrinsics && !has_Crypto_GHASH()) {
    warning("GHASH intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseGHASHIntrinsics, false);
  }

  if (FLAG_IS_DEFAULT(UseFMA)) {
    FLAG_SET_DEFAULT(UseFMA, true);
  }

  if (UseMD5Intrinsics) {
    warning("MD5 intrinsics are not available on this CPU");
    FLAG_SET_DEFAULT(UseMD5Intrinsics, false);
  }

  // On z/Architecture, we take UseSHA as the general switch to enable/disable the SHA intrinsics.
  // The specific switches UseSHAxxxIntrinsics will then be set depending on the actual
  // machine capabilities.
  // Explicitly setting them via CmdLine option takes precedence, of course.
  if (FLAG_IS_DEFAULT(UseSHA) && has_Crypto_SHA()) {
    FLAG_SET_DEFAULT(UseSHA, true);
  }
  if (UseSHA && !has_Crypto_SHA()) {
    warning("SHA instructions are not available on this CPU");
    FLAG_SET_DEFAULT(UseSHA, false);
  }
  if (UseSHA && has_Crypto_SHA1()) {
    if (FLAG_IS_DEFAULT(UseSHA1Intrinsics)) {
      FLAG_SET_DEFAULT(UseSHA1Intrinsics, true);
    }
  } else if (UseSHA1Intrinsics) {
    warning("Intrinsics for SHA-1 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA1Intrinsics, false);
  }
  if (UseSHA && has_Crypto_SHA256()) {
    if (FLAG_IS_DEFAULT(UseSHA256Intrinsics)) {
      FLAG_SET_DEFAULT(UseSHA256Intrinsics, true);
    }
  } else if (UseSHA256Intrinsics) {
    warning("Intrinsics for SHA-224 and SHA-256 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA256Intrinsics, false);
  }
  if (UseSHA && has_Crypto_SHA512()) {
    if (FLAG_IS_DEFAULT(UseSHA512Intrinsics)) {
      FLAG_SET_DEFAULT(UseSHA512Intrinsics, true);
    }
  } else if (UseSHA512Intrinsics) {
    warning("Intrinsics for SHA-384 and SHA-512 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA512Intrinsics, false);
  }

  if (UseSHA3Intrinsics) {
    warning("Intrinsics for SHA3-224, SHA3-256, SHA3-384 and SHA3-512 crypto hash functions not available on this CPU.");
    FLAG_SET_DEFAULT(UseSHA3Intrinsics, false);
  }

  if (!(UseSHA1Intrinsics || UseSHA256Intrinsics || UseSHA512Intrinsics)) {
    FLAG_SET_DEFAULT(UseSHA, false);
  }

#ifdef COMPILER2
  if (FLAG_IS_DEFAULT(UseMultiplyToLenIntrinsic)) {
    FLAG_SET_DEFAULT(UseMultiplyToLenIntrinsic, true);
  }
  if (FLAG_IS_DEFAULT(UseMontgomeryMultiplyIntrinsic)) {
    FLAG_SET_DEFAULT(UseMontgomeryMultiplyIntrinsic, true);
  }
  if (FLAG_IS_DEFAULT(UseMontgomerySquareIntrinsic)) {
    FLAG_SET_DEFAULT(UseMontgomerySquareIntrinsic, true);
  }
#endif
  if (FLAG_IS_DEFAULT(UsePopCountInstruction)) {
    FLAG_SET_DEFAULT(UsePopCountInstruction, true);
  }

  // z/Architecture supports 8-byte compare-exchange operations
  // (see Atomic::cmpxchg)
  // and 'atomic long memory ops' (see Unsafe_GetLongVolatile).
  _supports_cx8 = true;

  _supports_atomic_getadd4 = VM_Version::has_LoadAndALUAtomicV1();
  _supports_atomic_getadd8 = VM_Version::has_LoadAndALUAtomicV1();

  // z/Architecture supports unaligned memory accesses.
  // Performance penalty is negligible. An additional tick or so
  // is lost if the accessed data spans a cache line boundary.
  // Unaligned accesses are not atomic, of course.
  if (FLAG_IS_DEFAULT(UseUnalignedAccesses)) {
    FLAG_SET_DEFAULT(UseUnalignedAccesses, true);
  }
}


int VM_Version::get_model_index() {
  // returns the index used to access the various model-dependent strings.
  //  > 0 valid (known) model detected.
  //  = 0 model not recognized, maybe not yet supported.
  //  < 0 model detection is ambiguous. The absolute value of the returned value
  //      is the index of the oldest detected model.
  int ambiguity = 0;
  int model_ix  = 0;
  if (is_z15()) {
    model_ix = 9;
    ambiguity++;
  }
  if (is_z14()) {
    model_ix = 8;
    ambiguity++;
  }
  if (is_z13()) {
    model_ix = 7;
    ambiguity++;
  }
  if (is_ec12()) {
    model_ix = 6;
    ambiguity++;
  }
  if (is_z196()) {
    model_ix = 5;
    ambiguity++;
  }
  if (is_z10()) {
    model_ix = 4;
    ambiguity++;
  }
  if (is_z9()) {
    model_ix = 3;
    ambiguity++;
  }
  if (is_z990()) {
    model_ix = 2;
    ambiguity++;
  }
  if (is_z900()) {
    model_ix = 1;
    ambiguity++;
  }

  if (ambiguity > 1) {
    model_ix = -model_ix;
  }
  return model_ix;
}


void VM_Version::set_features_string() {
  // A note on the _features_string format:
  //   There are jtreg tests checking the _features_string for various properties.
  //   For some strange reason, these tests require the string to contain
  //   only _lowercase_ characters. Keep that in mind when being surprised
  //   about the unusual notation of features - and when adding new ones.
  //   Features may have one comma at the end.
  //   Furthermore, use one, and only one, separator space between features.
  //   Multiple spaces are considered separate tokens, messing up everything.

  int model_ix = get_model_index();
  char buf[512];
  if (model_ix == 0) {
    _model_string = "unknown model";
    strcpy(buf, "z/Architecture (unknown generation)");
  } else if (model_ix > 0) {
    _model_string = z_name[model_ix];
    jio_snprintf(buf, sizeof(buf), "%s, out-of-support_as_of_", z_features[model_ix], z_EOS[model_ix]);
  } else if (model_ix < 0) {
    tty->print_cr("*** WARNING *** Ambiguous z/Architecture detection!");
    tty->print_cr("                oldest detected generation is %s", z_features[-model_ix]);
    _model_string = "unknown model";
    strcpy(buf, "z/Architecture (ambiguous detection)");
  }
  _features_string = os::strdup(buf);

  if (has_Crypto_AES()) {
    assert(strlen(_features_string) + 3*8 < sizeof(buf), "increase buffer size");
    jio_snprintf(buf, sizeof(buf), "%s%s%s%s",
                 _features_string,
                 has_Crypto_AES128() ? ", aes128" : "",
                 has_Crypto_AES192() ? ", aes192" : "",
                 has_Crypto_AES256() ? ", aes256" : "");
    os::free((void *)_features_string);
    _features_string = os::strdup(buf);
  }

  if (has_Crypto_SHA()) {
    assert(strlen(_features_string) + 6 + 2*8 + 7 < sizeof(buf), "increase buffer size");
    jio_snprintf(buf, sizeof(buf), "%s%s%s%s%s",
                 _features_string,
                 has_Crypto_SHA1()   ? ", sha1"   : "",
                 has_Crypto_SHA256() ? ", sha256" : "",
                 has_Crypto_SHA512() ? ", sha512" : "",
                 has_Crypto_GHASH()  ? ", ghash"  : "");
    os::free((void *)_features_string);
    _features_string = os::strdup(buf);
  }
}

// featureBuffer - bit array indicating availability of various features
// featureNum    - bit index of feature to be tested
//                 Featurenum < 0 requests test for any nonzero bit in featureBuffer.
// bufLen        - length of featureBuffer in bits
bool VM_Version::test_feature_bit(unsigned long* featureBuffer, int featureNum, unsigned int bufLen) {
  assert(bufLen > 0,             "buffer len must be positive");
  assert((bufLen & 0x0007) == 0, "unaligned buffer len");
  assert(((intptr_t)featureBuffer&0x0007) == 0, "unaligned feature buffer");
  if (featureNum < 0) {
    // Any bit set at all?
    bool anyBit = false;
    for (size_t i = 0; i < bufLen/(8*sizeof(long)); i++) {
      anyBit = anyBit || (featureBuffer[i] != 0);
    }
    return anyBit;
  } else {
    assert((unsigned int)featureNum < bufLen,    "feature index out of range");
    unsigned char* byteBuffer = (unsigned char*)featureBuffer;
    int   byteIndex  = featureNum/(8*sizeof(char));
    int   bitIndex   = featureNum%(8*sizeof(char));
    // Indexed bit set?
    return (byteBuffer[byteIndex] & (1U<<(7-bitIndex))) != 0;
  }
}

void VM_Version::print_features_internal(const char* text, bool print_anyway) {
  tty->print_cr("%s %s", text, features_string());
  tty->cr();

  if (Verbose || print_anyway) {
    // z900
    if (has_long_displacement()        ) tty->print_cr("available: %s", "LongDispFacility");
    // z990
    if (has_long_displacement_fast()   ) tty->print_cr("available: %s", "LongDispFacilityHighPerf");
    if (has_ETF2() && has_ETF3()       ) tty->print_cr("available: %s", "ETF2 and ETF3");
    if (has_Crypto()                   ) tty->print_cr("available: %s", "CryptoFacility");
    // z9
    if (has_extended_immediate()       ) tty->print_cr("available: %s", "ExtImmedFacility");
    if (has_StoreFacilityListExtended()) tty->print_cr("available: %s", "StoreFacilityListExtended");
    if (has_StoreClockFast()           ) tty->print_cr("available: %s", "StoreClockFast");
    if (has_ETF2Enhancements()         ) tty->print_cr("available: %s", "ETF2 Enhancements");
    if (has_ETF3Enhancements()         ) tty->print_cr("available: %s", "ETF3 Enhancements");
    if (has_HFPUnnormalized()          ) tty->print_cr("available: %s", "HFPUnnormalizedFacility");
    if (has_HFPMultiplyAndAdd()        ) tty->print_cr("available: %s", "HFPMultiplyAndAddFacility");
    // z10
    if (has_ParsingEnhancements()      ) tty->print_cr("available: %s", "Parsing Enhancements");
    if (has_ExtractCPUtime()           ) tty->print_cr("available: %s", "ExtractCPUTime");
    if (has_CompareSwapStore()         ) tty->print_cr("available: %s", "CompareSwapStore");
    if (has_GnrlInstrExtensions()      ) tty->print_cr("available: %s", "General Instruction Extensions");
    if (has_CompareBranch()            ) tty->print_cr("  available: %s", "Compare and Branch");
    if (has_CompareTrap()              ) tty->print_cr("  available: %s", "Compare and Trap");
    if (has_RelativeLoadStore()        ) tty->print_cr("  available: %s", "Relative Load/Store");
    if (has_MultiplySingleImm32()      ) tty->print_cr("  available: %s", "MultiplySingleImm32");
    if (has_Prefetch()                 ) tty->print_cr("  available: %s", "Prefetch");
    if (has_MoveImmToMem()             ) tty->print_cr("  available: %s", "Direct Moves Immediate to Memory");
    if (has_MemWithImmALUOps()         ) tty->print_cr("  available: %s", "Direct ALU Ops Memory .op. Immediate");
    if (has_ExtractCPUAttributes()     ) tty->print_cr("  available: %s", "Extract CPU Attributes");
    if (has_ExecuteExtensions()        ) tty->print_cr("available: %s", "ExecuteExtensions");
    if (has_FPSupportEnhancements()    ) tty->print_cr("available: %s", "FPSupportEnhancements");
    if (has_DecimalFloatingPoint()     ) tty->print_cr("available: %s", "DecimalFloatingPoint");
    // z196
    if (has_DistinctOpnds()            ) tty->print_cr("available: %s", "Distinct Operands");
    if (has_InterlockedAccessV1()      ) tty->print_cr("  available: %s", "InterlockedAccess V1 (fast)");
    if (has_PopCount()                 ) tty->print_cr("  available: %s", "PopCount");
    if (has_LoadStoreConditional()     ) tty->print_cr("  available: %s", "LoadStoreConditional");
    if (has_HighWordInstr()            ) tty->print_cr("  available: %s", "HighWord Instructions");
    if (has_FastSync()                 ) tty->print_cr("  available: %s", "FastSync (bcr 14,0)");
    if (has_AtomicMemWithImmALUOps()   ) tty->print_cr("available: %s", "Atomic Direct ALU Ops Memory .op. Immediate");
    if (has_FPExtensions()             ) tty->print_cr("available: %s", "Floatingpoint Extensions");
    if (has_CryptoExt3()               ) tty->print_cr("available: %s", "Crypto Extensions 3");
    if (has_CryptoExt4()               ) tty->print_cr("available: %s", "Crypto Extensions 4");
    // EC12
    if (has_MiscInstrExt()             ) tty->print_cr("available: %s", "Miscellaneous Instruction Extensions");
    if (has_ExecutionHint()            ) tty->print_cr("  available: %s", "Execution Hints (branch prediction)");
    if (has_ProcessorAssist()          ) tty->print_cr("  available: %s", "Processor Assists");
    if (has_LoadAndTrap()              ) tty->print_cr("  available: %s", "Load and Trap");
    if (has_TxMem()                    ) tty->print_cr("available: %s", "Transactional Memory");
    if (has_InterlockedAccessV2()      ) tty->print_cr("  available: %s", "InterlockedAccess V2 (fast)");
    if (has_DFPZonedConversion()       ) tty->print_cr("  available: %s", "DFP Zoned Conversions");
    // z13
    if (has_LoadStoreConditional2()    ) tty->print_cr("available: %s", "Load/Store Conditional 2");
    if (has_CryptoExt5()               ) tty->print_cr("available: %s", "Crypto Extensions 5");
    if (has_DFPPackedConversion()      ) tty->print_cr("available: %s", "DFP Packed Conversions");
    if (has_VectorFacility()           ) tty->print_cr("available: %s", "Vector Facility");
    // z14
    if (has_MiscInstrExt2()            ) tty->print_cr("available: %s", "Miscellaneous Instruction Extensions 2");
    if (has_VectorEnhancements1()      ) tty->print_cr("available: %s", "Vector Facility Enhancements 3");
    if (has_CryptoExt8()               ) tty->print_cr("available: %s", "Crypto Extensions 8");
    // z15
    if (has_MiscInstrExt3()            ) tty->print_cr("available: %s", "Miscellaneous Instruction Extensions 3");
    if (has_VectorEnhancements2()      ) tty->print_cr("available: %s", "Vector Facility Enhancements 3");
    if (has_CryptoExt9()               ) tty->print_cr("available: %s", "Crypto Extensions 9");

    if (has_Crypto()) {
      tty->cr();
      tty->print_cr("detailed availability of %s capabilities:", "CryptoFacility");
      if (test_feature_bit(&_cipher_features_KM[0], -1, 2*Cipher::_featureBits)) {
        tty->cr();
        tty->print_cr("  available: %s", "Message Cipher Functions");
      }

      if (test_feature_bit(&_cipher_features_KM[0], -1, (int)Cipher::_featureBits)) {
        tty->print_cr("    available Crypto Features of KM  (Cipher Message):");
        for (unsigned int i = 0; i < Cipher::_featureBits; i++) {
          if (test_feature_bit(&_cipher_features_KM[0], i, (int)Cipher::_featureBits)) {
            switch (i) {
              case Cipher::_Query:              tty->print_cr("      available: KM   Query");                  break;
              case Cipher::_DEA:                tty->print_cr("      available: KM   DEA");                    break;
              case Cipher::_TDEA128:            tty->print_cr("      available: KM   TDEA-128");               break;
              case Cipher::_TDEA192:            tty->print_cr("      available: KM   TDEA-192");               break;
              case Cipher::_EncryptedDEA:       tty->print_cr("      available: KM   Encrypted DEA");          break;
              case Cipher::_EncryptedDEA128:    tty->print_cr("      available: KM   Encrypted DEA-128");      break;
              case Cipher::_EncryptedDEA192:    tty->print_cr("      available: KM   Encrypted DEA-192");      break;
              case Cipher::_AES128:             tty->print_cr("      available: KM   AES-128");                break;
              case Cipher::_AES192:             tty->print_cr("      available: KM   AES-192");                break;
              case Cipher::_AES256:             tty->print_cr("      available: KM   AES-256");                break;
              case Cipher::_EnccryptedAES128:   tty->print_cr("      available: KM   Encrypted-AES-128");      break;
              case Cipher::_EnccryptedAES192:   tty->print_cr("      available: KM   Encrypted-AES-192");      break;
              case Cipher::_EnccryptedAES256:   tty->print_cr("      available: KM   Encrypted-AES-256");      break;
              case Cipher::_XTSAES128:          tty->print_cr("      available: KM   XTS-AES-128");            break;
              case Cipher::_XTSAES256:          tty->print_cr("      available: KM   XTS-AES-256");            break;
              case Cipher::_EncryptedXTSAES128: tty->print_cr("      available: KM   XTS-Encrypted-AES-128");  break;
              case Cipher::_EncryptedXTSAES256: tty->print_cr("      available: KM   XTS-Encrypted-AES-256");  break;
              default: tty->print_cr("      available: unknown KM  code %d", i);      break;
            }
          }
        }
      }

      if (test_feature_bit(&_cipher_features_KM[2], -1, (int)Cipher::_featureBits)) {
        tty->print_cr("    available Crypto Features of KMC (Cipher Message with Chaining):");
        for (unsigned int i = 0; i < Cipher::_featureBits; i++) {
          if (test_feature_bit(&_cipher_features_KM[2], i, (int)Cipher::_featureBits)) {
            switch (i) {
              case Cipher::_Query:              tty->print_cr("      available: KMC  Query");                  break;
              case Cipher::_DEA:                tty->print_cr("      available: KMC  DEA");                    break;
              case Cipher::_TDEA128:            tty->print_cr("      available: KMC  TDEA-128");               break;
              case Cipher::_TDEA192:            tty->print_cr("      available: KMC  TDEA-192");               break;
              case Cipher::_EncryptedDEA:       tty->print_cr("      available: KMC  Encrypted DEA");          break;
              case Cipher::_EncryptedDEA128:    tty->print_cr("      available: KMC  Encrypted DEA-128");      break;
              case Cipher::_EncryptedDEA192:    tty->print_cr("      available: KMC  Encrypted DEA-192");      break;
              case Cipher::_AES128:             tty->print_cr("      available: KMC  AES-128");                break;
              case Cipher::_AES192:             tty->print_cr("      available: KMC  AES-192");                break;
              case Cipher::_AES256:             tty->print_cr("      available: KMC  AES-256");                break;
              case Cipher::_EnccryptedAES128:   tty->print_cr("      available: KMC  Encrypted-AES-128");      break;
              case Cipher::_EnccryptedAES192:   tty->print_cr("      available: KMC  Encrypted-AES-192");      break;
              case Cipher::_EnccryptedAES256:   tty->print_cr("      available: KMC  Encrypted-AES-256");      break;
              case Cipher::_PRNG:               tty->print_cr("      available: KMC  PRNG");                   break;
              default: tty->print_cr("      available: unknown KMC code %d", i);      break;
            }
          }
        }
      }
    }

    if (has_CryptoExt4()) {
      if (test_feature_bit(&_cipher_features_KMF[0], -1, (int)Cipher::_featureBits)) {
        tty->print_cr("    available Crypto Features of KMF (Cipher Message with Cipher Feedback):");
        for (unsigned int i = 0; i < Cipher::_featureBits; i++) {
          if (test_feature_bit(&_cipher_features_KMF[0], i, (int)Cipher::_featureBits)) {
            switch (i) {
              case Cipher::_Query:              tty->print_cr("      available: KMF  Query");                  break;
              case Cipher::_DEA:                tty->print_cr("      available: KMF  DEA");                    break;
              case Cipher::_TDEA128:            tty->print_cr("      available: KMF  TDEA-128");               break;
              case Cipher::_TDEA192:            tty->print_cr("      available: KMF  TDEA-192");               break;
              case Cipher::_EncryptedDEA:       tty->print_cr("      available: KMF  Encrypted DEA");          break;
              case Cipher::_EncryptedDEA128:    tty->print_cr("      available: KMF  Encrypted DEA-128");      break;
              case Cipher::_EncryptedDEA192:    tty->print_cr("      available: KMF  Encrypted DEA-192");      break;
              case Cipher::_AES128:             tty->print_cr("      available: KMF  AES-128");                break;
              case Cipher::_AES192:             tty->print_cr("      available: KMF  AES-192");                break;
              case Cipher::_AES256:             tty->print_cr("      available: KMF  AES-256");                break;
              case Cipher::_EnccryptedAES128:   tty->print_cr("      available: KMF  Encrypted-AES-128");      break;
              case Cipher::_EnccryptedAES192:   tty->print_cr("      available: KMF  Encrypted-AES-192");      break;
              case Cipher::_EnccryptedAES256:   tty->print_cr("      available: KMF  Encrypted-AES-256");      break;
              default: tty->print_cr("      available: unknown KMF code %d", i);      break;
            }
          }
        }
      }

      if (test_feature_bit(&_cipher_features_KMCTR[0], -1, (int)Cipher::_featureBits)) {
        tty->print_cr("    available Crypto Features of KMCTR (Cipher Message with Counter):");
        for (unsigned int i = 0; i < Cipher::_featureBits; i++) {
          if (test_feature_bit(&_cipher_features_KMCTR[0], i, (int)Cipher::_featureBits)) {
            switch (i) {
              case Cipher::_Query:              tty->print_cr("      available: KMCTR  Query");                break;
              case Cipher::_DEA:                tty->print_cr("      available: KMCTR  DEA");                  break;
              case Cipher::_TDEA128:            tty->print_cr("      available: KMCTR  TDEA-128");             break;
              case Cipher::_TDEA192:            tty->print_cr("      available: KMCTR  TDEA-192");             break;
              case Cipher::_EncryptedDEA:       tty->print_cr("      available: KMCTR  Encrypted DEA");        break;
              case Cipher::_EncryptedDEA128:    tty->print_cr("      available: KMCTR  Encrypted DEA-128");    break;
              case Cipher::_EncryptedDEA192:    tty->print_cr("      available: KMCTR  Encrypted DEA-192");    break;
              case Cipher::_AES128:             tty->print_cr("      available: KMCTR  AES-128");              break;
              case Cipher::_AES192:             tty->print_cr("      available: KMCTR  AES-192");              break;
              case Cipher::_AES256:             tty->print_cr("      available: KMCTR  AES-256");              break;
              case Cipher::_EnccryptedAES128:   tty->print_cr("      available: KMCTR  Encrypted-AES-128");    break;
              case Cipher::_EnccryptedAES192:   tty->print_cr("      available: KMCTR  Encrypted-AES-192");    break;
              case Cipher::_EnccryptedAES256:   tty->print_cr("      available: KMCTR  Encrypted-AES-256");    break;
              default: tty->print_cr("      available: unknown KMCTR code %d", i);      break;
            }
          }
        }
      }

      if (test_feature_bit(&_cipher_features_KMO[0], -1, (int)Cipher::_featureBits)) {
        tty->print_cr("    available Crypto Features of KMO (Cipher Message with Output Feedback):");
        for (unsigned int i = 0; i < Cipher::_featureBits; i++) {
          if (test_feature_bit(&_cipher_features_KMO[0], i, (int)Cipher::_featureBits)) {
            switch (i) {
              case Cipher::_Query:              tty->print_cr("      available: KMO  Query");                  break;
              case Cipher::_DEA:                tty->print_cr("      available: KMO  DEA");                    break;
              case Cipher::_TDEA128:            tty->print_cr("      available: KMO  TDEA-128");               break;
              case Cipher::_TDEA192:            tty->print_cr("      available: KMO  TDEA-192");               break;
              case Cipher::_EncryptedDEA:       tty->print_cr("      available: KMO  Encrypted DEA");          break;
              case Cipher::_EncryptedDEA128:    tty->print_cr("      available: KMO  Encrypted DEA-128");      break;
              case Cipher::_EncryptedDEA192:    tty->print_cr("      available: KMO  Encrypted DEA-192");      break;
              case Cipher::_AES128:             tty->print_cr("      available: KMO  AES-128");                break;
              case Cipher::_AES192:             tty->print_cr("      available: KMO  AES-192");                break;
              case Cipher::_AES256:             tty->print_cr("      available: KMO  AES-256");                break;
              case Cipher::_EnccryptedAES128:   tty->print_cr("      available: KMO  Encrypted-AES-128");      break;
              case Cipher::_EnccryptedAES192:   tty->print_cr("      available: KMO  Encrypted-AES-192");      break;
              case Cipher::_EnccryptedAES256:   tty->print_cr("      available: KMO  Encrypted-AES-256");      break;
              default: tty->print_cr("      available: unknown KMO code %d", i);      break;
            }
          }
        }
      }
    }

    if (has_CryptoExt8()) {
      if (test_feature_bit(&_cipher_features_KMA[0], -1, (int)Cipher::_featureBits)) {
        tty->print_cr("    available Crypto Features of KMA (Cipher Message with Authentication):");
        for (unsigned int i = 0; i < Cipher::_featureBits; i++) {
          if (test_feature_bit(&_cipher_features_KMA[0], i, (int)Cipher::_featureBits)) {
            switch (i) {
              case Cipher::_Query:              tty->print_cr("      available: KMA      Query");              break;
              case Cipher::_AES128:             tty->print_cr("      available: KMA-GCM  AES-128");            break;
              case Cipher::_AES192:             tty->print_cr("      available: KMA-GCM  AES-192");            break;
              case Cipher::_AES256:             tty->print_cr("      available: KMA-GCM  AES-256");            break;
              case Cipher::_EnccryptedAES128:   tty->print_cr("      available: KMA-GCM  Encrypted-AES-128");  break;
              case Cipher::_EnccryptedAES192:   tty->print_cr("      available: KMA-GCM  Encrypted-AES-192");  break;
              case Cipher::_EnccryptedAES256:   tty->print_cr("      available: KMA-GCM  Encrypted-AES-256");  break;
              default: tty->print_cr("      available: unknown KMA code %d", i);      break;
            }
          }
        }
      }
    }

    if (has_Crypto()) {
      if (test_feature_bit(&_msgdigest_features[0], -1, 2*MsgDigest::_featureBits)) {
        tty->cr();
        tty->print_cr("  available: %s", "Message Digest Functions for SHA");
      }

      if (test_feature_bit(&_msgdigest_features[0], -1, (int)MsgDigest::_featureBits)) {
        tty->print_cr("    available Features of KIMD (Msg Digest):");
        for (unsigned int i = 0; i < MsgDigest::_featureBits; i++) {
          if (test_feature_bit(&_msgdigest_features[0], i, (int)MsgDigest::_featureBits)) {
            switch (i) {
              case MsgDigest::_Query:     tty->print_cr("      available: KIMD Query");   break;
              case MsgDigest::_SHA1:      tty->print_cr("      available: KIMD SHA-1");   break;
              case MsgDigest::_SHA256:    tty->print_cr("      available: KIMD SHA-256"); break;
              case MsgDigest::_SHA512:    tty->print_cr("      available: KIMD SHA-512"); break;
              case MsgDigest::_SHA3_224:  tty->print_cr("      available: KIMD SHA3-224");  break;
              case MsgDigest::_SHA3_256:  tty->print_cr("      available: KIMD SHA3-256");  break;
              case MsgDigest::_SHA3_384:  tty->print_cr("      available: KIMD SHA3-384");  break;
              case MsgDigest::_SHA3_512:  tty->print_cr("      available: KIMD SHA3-512");  break;
              case MsgDigest::_SHAKE_128: tty->print_cr("      available: KIMD SHAKE-128"); break;
              case MsgDigest::_SHAKE_256: tty->print_cr("      available: KIMD SHAKE-256"); break;
              case MsgDigest::_GHASH:     tty->print_cr("      available: KIMD GHASH");   break;
              default: tty->print_cr("      available: unknown code %d", i);  break;
            }
          }
        }
      }

      if (test_feature_bit(&_msgdigest_features[2], -1, (int)MsgDigest::_featureBits)) {
        tty->print_cr("    available Features of KLMD (Msg Digest):");
        for (unsigned int i = 0; i < MsgDigest::_featureBits; i++) {
          if (test_feature_bit(&_msgdigest_features[2], i, (int)MsgDigest::_featureBits)) {
            switch (i) {
              case MsgDigest::_Query:     tty->print_cr("      available: KLMD Query");   break;
              case MsgDigest::_SHA1:      tty->print_cr("      available: KLMD SHA-1");   break;
              case MsgDigest::_SHA256:    tty->print_cr("      available: KLMD SHA-256"); break;
              case MsgDigest::_SHA512:    tty->print_cr("      available: KLMD SHA-512"); break;
              case MsgDigest::_SHA3_224:  tty->print_cr("      available: KLMD SHA3-224");  break;
              case MsgDigest::_SHA3_256:  tty->print_cr("      available: KLMD SHA3-256");  break;
              case MsgDigest::_SHA3_384:  tty->print_cr("      available: KLMD SHA3-384");  break;
              case MsgDigest::_SHA3_512:  tty->print_cr("      available: KLMD SHA3-512");  break;
              case MsgDigest::_SHAKE_128: tty->print_cr("      available: KLMD SHAKE-128"); break;
              case MsgDigest::_SHAKE_256: tty->print_cr("      available: KLMD SHAKE-256"); break;
              default: tty->print_cr("      available: unknown code %d", i);  break;
            }
          }
        }
      }
    }
    if (ContendedPaddingWidth > 0) {
      tty->cr();
      tty->print_cr("ContendedPaddingWidth " INTX_FORMAT, ContendedPaddingWidth);
    }
  }
}

void VM_Version::print_platform_virtualization_info(outputStream* st) {
  // /proc/sysinfo contains interesting information about
  // - LPAR
  // - whole "Box" (CPUs )
  // - z/VM / KVM (VM<nn>); this is not available in an LPAR-only setup
  const char* kw[] = { "LPAR", "CPUs", "VM", NULL };
  const char* info_file = "/proc/sysinfo";

  if (!print_matching_lines_from_file(info_file, st, kw)) {
    st->print_cr("  <%s Not Available>", info_file);
  }
}

void VM_Version::print_features() {
  print_features_internal("Version:");
}

void VM_Version::reset_features(bool reset) {
  if (reset) {
    for (unsigned int i = 0; i < _features_buffer_len; i++) {
      VM_Version::_features[i] = 0;
    }
  }
}

void VM_Version::set_features_z900(bool reset) {
  reset_features(reset);

  set_has_long_displacement();
  set_has_ETF2();
}

void VM_Version::set_features_z990(bool reset) {
  reset_features(reset);

  set_features_z900(false);
  set_has_ETF3();
  set_has_long_displacement_fast();
  set_has_HFPMultiplyAndAdd();
}

void VM_Version::set_features_z9(bool reset) {
  reset_features(reset);

  set_features_z990(false);
  set_has_StoreFacilityListExtended();
  // set_has_Crypto();   // Do not set, crypto features must be retrieved separately.
  set_has_ETF2Enhancements();
  set_has_ETF3Enhancements();
  set_has_extended_immediate();
  set_has_StoreClockFast();
  set_has_HFPUnnormalized();
}

void VM_Version::set_features_z10(bool reset) {
  reset_features(reset);

  set_features_z9(false);
  set_has_CompareSwapStore();
  set_has_RelativeLoadStore();
  set_has_CompareBranch();
  set_has_CompareTrap();
  set_has_MultiplySingleImm32();
  set_has_Prefetch();
  set_has_MoveImmToMem();
  set_has_MemWithImmALUOps();
  set_has_ExecuteExtensions();
  set_has_FPSupportEnhancements();
  set_has_DecimalFloatingPoint();
  set_has_ExtractCPUtime();
  set_has_CryptoExt3();
}

void VM_Version::set_features_z196(bool reset) {
  reset_features(reset);

  set_features_z10(false);
  set_has_InterlockedAccessV1();
  set_has_PopCount();
  set_has_LoadStoreConditional();
  set_has_HighWordInstr();
  set_has_FastSync();
  set_has_FPExtensions();
  set_has_DistinctOpnds();
  set_has_CryptoExt4();
}

void VM_Version::set_features_ec12(bool reset) {
  reset_features(reset);

  set_features_z196(false);
  set_has_MiscInstrExt();
  set_has_InterlockedAccessV2();
  set_has_LoadAndALUAtomicV2();
  set_has_TxMem();
}

void VM_Version::set_features_z13(bool reset) {
  reset_features(reset);

  set_features_ec12(false);
  set_has_LoadStoreConditional2();
  set_has_CryptoExt5();
  set_has_VectorFacility();
}

void VM_Version::set_features_z14(bool reset) {
  reset_features(reset);

  set_features_z13(false);
  set_has_MiscInstrExt2();
  set_has_VectorEnhancements1();
  has_VectorPackedDecimal();
  set_has_CryptoExt8();
}

void VM_Version::set_features_z15(bool reset) {
  reset_features(reset);

  set_features_z14(false);
  set_has_MiscInstrExt3();
  set_has_VectorEnhancements2();
  has_VectorPackedDecimalEnh();
  set_has_CryptoExt9();
}

void VM_Version::set_features_from(const char* march) {
  bool err = false;
  bool prt = false;

  if ((march != NULL) && (march[0] != '\0')) {
    const int buf_len = 16;
    const int hdr_len =  5;
    char buf[buf_len];
    if (strlen(march) >= hdr_len) {
      memcpy(buf, march, hdr_len);
      buf[hdr_len] = '\00';
    } else {
      buf[0]       = '\00';
    }

    if (!strcmp(march, "z900")) {
      set_features_z900();
    } else if (!strcmp(march, "z990")) {
        set_features_z990();
    } else if (!strcmp(march, "z9")) {
        set_features_z9();
    } else if (!strcmp(march, "z10")) {
        set_features_z10();
    } else if (!strcmp(march, "z196")) {
        set_features_z196();
    } else if (!strcmp(march, "ec12")) {
        set_features_ec12();
    } else if (!strcmp(march, "z13")) {
        set_features_z13();
    } else if (!strcmp(march, "z14")) {
        set_features_z14();
    } else if (!strcmp(march, "z15")) {
        set_features_z15();
    } else {
      err = true;
    }
    if (!err) {
      set_features_string();
      if (prt || PrintAssembly) {
        print_features_internal("CPU Version as set by cmdline option:", prt);
      }
    } else {
      tty->print_cr("***Warning: Unsupported ProcessorArchitecture: %s, internal settings left undisturbed.", march);
    }
  }

}

// getFeatures call interface
// Z_ARG1 (R2) - feature bit buffer address.
//               Must be DW aligned.
// Z_ARG2 (R3) -  > 0 feature bit buffer length (#DWs).
//                    Implies request to store cpu feature list via STFLE.
//                = 0 invalid
//                < 0 function code (which feature information to retrieve)
//                    Implies that a buffer of at least two DWs is passed in.
//                =-1 - retrieve cache topology
//                =-2 - basic cipher instruction capabilities
//                =-3 - msg digest (secure hash) instruction capabilities
//                =-4 - vector instruction OS support availability
//               =-17 - cipher (KMF) support
//               =-18 - cipher (KMCTR) support
//               =-19 - cipher (KMO) support
//               =-20 - cipher (KMA) support
// Z_ARG3 (R4) - feature code for ECAG instruction
//
// Z_RET (R2)  - return value
//                >  0: success: number of retrieved feature bit string words.
//                <  0: failure: required number of feature bit string words (buffer too small).
//                == 0: failure: operation aborted.
//
static long (*getFeatures)(unsigned long*, int, int) = NULL;

void VM_Version::set_getFeatures(address entryPoint) {
  if (getFeatures == NULL) {
    getFeatures = (long(*)(unsigned long*, int, int))entryPoint;
  }
}

long VM_Version::call_getFeatures(unsigned long* buffer, int buflen, int functionCode) {
  VM_Version::_is_determine_features_test_running = true;
  long functionResult = (*getFeatures)(buffer, buflen, functionCode);
  VM_Version::_is_determine_features_test_running = false;
  return functionResult;
}

// Helper function for "extract cache attribute" instruction.
int VM_Version::calculate_ECAG_functionCode(unsigned int attributeIndication,
                                            unsigned int levelIndication,
                                            unsigned int typeIndication) {
  return (attributeIndication<<4) | (levelIndication<<1) | typeIndication;
}

void VM_Version::clear_buffer(unsigned long* buffer, unsigned int len) {
  memset(buffer, 0, sizeof(buffer[0])*len);
}

void VM_Version::copy_buffer(unsigned long* to, unsigned long* from, unsigned int len) {
  memcpy(to, from, sizeof(to[0])*len);
}

void VM_Version::determine_features() {

  const int      cbuf_size = _code_buffer_len;
  const int      buf_len   = _features_buffer_len;

  // Allocate code buffer space for the detection code.
  ResourceMark    rm;
  CodeBuffer      cbuf("determine CPU features", cbuf_size, 0);
  MacroAssembler* a = new MacroAssembler(&cbuf);

  // Emit code.
  set_getFeatures(a->pc());
  address   code = a->pc();

  // Try STFLE. Possible INVOP will cause defaults to be used.
  Label    getFEATURES;
  Label    getCPUFEATURES;                   // fcode = -1 (cache)
  Label    getCIPHERFEATURES_KM;             // fcode = -2 (cipher)
  Label    getCIPHERFEATURES_KMA;            // fcode = -20 (cipher)
  Label    getCIPHERFEATURES_KMF;            // fcode = -17 (cipher)
  Label    getCIPHERFEATURES_KMCTR;          // fcode = -18 (cipher)
  Label    getCIPHERFEATURES_KMO;            // fcode = -19 (cipher)
  Label    getMSGDIGESTFEATURES;             // fcode = -3 (SHA)
  Label    getVECTORFEATURES;                // fcode = -4 (OS support for vector instructions)
  Label    errRTN;
  a->z_ltgfr(Z_R0, Z_ARG2);                  // buf_len/fcode to r0 and test.
  a->z_brl(getFEATURES);                     // negative -> Get machine features or instruction-specific features
  a->z_lghi(Z_R1,0);
  a->z_brz(errRTN);                          // zero -> Function code currently not used, indicate "aborted".

  //---<  store feature list  >---
  // We have three possible outcomes here:
  // success:    cc = 0 and first DW of feature bit array != 0
  //             Z_R0 contains index of last stored DW (used_len - 1)
  // incomplete: cc = 3 and first DW of feature bit array != 0
  //             Z_R0 contains index of last DW that would have been stored (required_len - 1)
  a->z_aghi(Z_R0, -1);                       // STFLE needs last index, not length, of feature bit array.
  a->z_stfle(0, Z_ARG1);
  a->z_lg(Z_R1, Address(Z_ARG1, (intptr_t)0)); // Get first DW of facility list.
  a->z_lgr(Z_RET, Z_R0);                     // Calculate used/required len
  a->z_la(Z_RET, 1, Z_RET);                  // don't destroy cc from stfle!
  a->z_brnz(errRTN);                         // Instr failed if non-zero CC.
  a->z_ltgr(Z_R1, Z_R1);                     // Check if first DW of facility list was filled.
  a->z_bcr(Assembler::bcondNotZero, Z_R14);  // Successful return.

  //---<  error exit  >---
  a->bind(errRTN);
  a->z_lngr(Z_RET, Z_RET);                   // negative return value to indicate "buffer too small"
  a->z_ltgr(Z_R1, Z_R1);                     // Check if first DW of facility list was filled.
  a->z_bcr(Assembler::bcondNotZero, Z_R14);  // Return "buffer too small".
  a->z_xgr(Z_RET, Z_RET);
  a->z_br(Z_R14);                            // Return "operation aborted".

  a->bind(getFEATURES);
  a->z_cghi(Z_R0, -1);                       // -1: Extract CPU attributes, currently: cache layout only.
  a->z_bre(getCPUFEATURES);
  a->z_cghi(Z_R0, -2);                       // -2: Extract detailed crypto capabilities (cipher instructions).
  a->z_bre(getCIPHERFEATURES_KM);
  a->z_cghi(Z_R0, -3);                       // -3: Extract detailed crypto capabilities (msg digest instructions).
  a->z_bre(getMSGDIGESTFEATURES);
  a->z_cghi(Z_R0, -4);                       // -4: Verify vector instruction availability (OS support).
  a->z_bre(getVECTORFEATURES);

  a->z_cghi(Z_R0, -17);                      // -17: Extract detailed crypto capabilities (cipher instructions).
  a->z_bre(getCIPHERFEATURES_KMF);
  a->z_cghi(Z_R0, -18);                      // -18: Extract detailed crypto capabilities (cipher instructions).
  a->z_bre(getCIPHERFEATURES_KMCTR);
  a->z_cghi(Z_R0, -19);                      // -19: Extract detailed crypto capabilities (cipher instructions).
  a->z_bre(getCIPHERFEATURES_KMO);
  a->z_cghi(Z_R0, -20);                      // -20: Extract detailed crypto capabilities (cipher instructions).
  a->z_bre(getCIPHERFEATURES_KMA);

  a->z_xgr(Z_RET, Z_RET);                    // Not a valid function code.
  a->z_br(Z_R14);                            // Return "operation aborted".

  // Try KIMD/KLMD query function to get details about msg digest (secure hash, SHA) instructions.
  a->bind(getMSGDIGESTFEATURES);
  a->z_lghi(Z_R0,(int)MsgDigest::_Query);    // query function code
  a->z_lgr(Z_R1,Z_R2);                       // param block addr, 2*16 bytes min size
  a->z_kimd(Z_R2,Z_R2);                      // Get available KIMD functions (bit pattern in param blk). Must use even regs.
  a->z_la(Z_R1,16,Z_R1);                     // next param block addr
  a->z_klmd(Z_R2,Z_R4);                      // Get available KLMD functions (bit pattern in param blk). Must use distinct even regs.
  a->z_lghi(Z_RET,4);                        // #used words in output buffer
  a->z_br(Z_R14);

  // Try KM/KMC query function to get details about crypto instructions.
  a->bind(getCIPHERFEATURES_KM);
  a->z_lghi(Z_R0,(int)Cipher::_Query);       // query function code
  a->z_lgr(Z_R1,Z_R2);                       // param block addr, 2*16 bytes min size (KIMD/KLMD output)
  a->z_km(Z_R2,Z_R2);                        // get available KM functions. Must use even regs.
  a->z_la(Z_R1,16,Z_R1);                     // next param block addr
  a->z_kmc(Z_R2,Z_R2);                       // get available KMC functions
  a->z_lghi(Z_RET,4);                        // #used words in output buffer
  a->z_br(Z_R14);

  // Try KMA query function to get details about crypto instructions.
  a->bind(getCIPHERFEATURES_KMA);
  a->z_lghi(Z_R0,(int)Cipher::_Query);       // query function code
  a->z_lgr(Z_R1,Z_R2);                       // param block addr, 2*16 bytes min size (KIMD/KLMD output)
  a->z_kma(Z_R2,Z_R4,Z_R6);                  // get available KMA functions. Must use distinct even regs.
  a->z_lghi(Z_RET,2);                        // #used words in output buffer
  a->z_br(Z_R14);

  // Try KMF query function to get details about crypto instructions.
  a->bind(getCIPHERFEATURES_KMF);
  a->z_lghi(Z_R0,(int)Cipher::_Query);       // query function code
  a->z_lgr(Z_R1,Z_R2);                       // param block addr, 2*16 bytes min size (KIMD/KLMD output)
  a->z_kmf(Z_R2,Z_R2);                       // get available KMA functions. Must use even regs.
  a->z_lghi(Z_RET,2);                        // #used words in output buffer
  a->z_br(Z_R14);

  // Try KMCTR query function to get details about crypto instructions.
  a->bind(getCIPHERFEATURES_KMCTR);
  a->z_lghi(Z_R0,(int)Cipher::_Query);       // query function code
  a->z_lgr(Z_R1,Z_R2);                       // param block addr, 2*16 bytes min size (KIMD/KLMD output)
  a->z_kmctr(Z_R2,Z_R2,Z_R2);                     // get available KMCTR functions. Must use even regs.
  a->z_lghi(Z_RET,2);                        // #used words in output buffer
  a->z_br(Z_R14);

  // Try KMO query function to get details about crypto instructions.
  a->bind(getCIPHERFEATURES_KMO);
  a->z_lghi(Z_R0,(int)Cipher::_Query);       // query function code
  a->z_lgr(Z_R1,Z_R2);                       // param block addr, 2*16 bytes min size (KIMD/KLMD output)
  a->z_kmo(Z_R2,Z_R2);                       // get available KMO functions. Must use even regs.
  a->z_lghi(Z_RET,2);                        // #used words in output buffer
  a->z_br(Z_R14);

  // Use EXTRACT CPU ATTRIBUTE instruction to get information about cache layout.
  a->bind(getCPUFEATURES);
  a->z_xgr(Z_R0,Z_R0);                       // as recommended in instruction documentation
  a->z_ecag(Z_RET,Z_R0,0,Z_ARG3);            // Extract information as requested by Z_ARG1 contents.
  a->z_br(Z_R14);

  // Use a vector instruction to verify OS support. Will fail with SIGFPE if OS support is missing.
  a->bind(getVECTORFEATURES);
  a->z_vtm(Z_V0,Z_V0);                       // non-destructive vector instruction. Will cause SIGFPE if not supported.
  a->z_br(Z_R14);

  address code_end = a->pc();
  a->flush();

  cbuf.insts()->set_end(code_end);

  // Print the detection code.
  bool printVerbose = Verbose || PrintAssembly || PrintStubCode;
  if (printVerbose) {
    ttyLocker ttyl;
    tty->print_cr("Decoding CPU feature detection stub at " INTPTR_FORMAT " before execution:", p2i(code));
    tty->print_cr("Stub length is %ld bytes, codebuffer reserves %d bytes, %ld bytes spare.",
                  code_end-code, cbuf_size, cbuf_size-(code_end-code));

    // Use existing decode function. This enables the [MachCode] format which is needed to DecodeErrorFile.
    Disassembler::decode(code, code_end, tty);
  }

  // prepare work buffer
  unsigned long  buffer[buf_len];
  clear_buffer(buffer, buf_len);

  // execute code
  // Illegal instructions will be replaced by 0 in signal handler.
  // In case of problems, call_getFeatures will return a not-positive result.
  long used_len = call_getFeatures(buffer, buf_len, 0);

  bool ok;
  if ((used_len > 0) && (used_len <= buf_len)) {
    ok = true;
    if (printVerbose) {
      bool compact = Verbose;
      tty->print_cr("Note: feature list uses %ld array elements.", used_len);
      if (compact) {
        tty->print("non-zero feature list elements:");
        for (unsigned int k = 0; k < used_len; k++) {
          if (buffer[k] != 0) {
            tty->print("  [%d]: 0x%16.16lx", k, buffer[k]);
          }
        }
        tty->cr();
      } else {
        for (unsigned int k = 0; k < used_len; k++) {
          tty->print_cr("non-zero feature list[%d]: 0x%16.16lx", k, buffer[k]);
        }
      }

      if (compact) {
        tty->print_cr("Active features (compact view):");
        for (unsigned int k = 0; k < used_len; k++) {
          tty->print_cr("  buffer[%d]:", k);
          for (unsigned int j = k*sizeof(long); j < (k+1)*sizeof(long); j++) {
            bool line = false;
            for (unsigned int i = j*8; i < (j+1)*8; i++) {
              bool bit  = test_feature_bit(buffer, i, used_len*sizeof(long)*8);
              if (bit) {
                if (!line) {
                  tty->print("    byte[%d]:", j);
                  tty->fill_to(13);
                  line = true;
                }
                tty->print("  [%3.3d]", i);
              }
            }
            if (line) {
              tty->cr();
            }
          }
        }
      } else {
        tty->print_cr("Active features (full view):");
        for (unsigned int k = 0; k < used_len; k++) {
          tty->print_cr("  buffer[%d]:", k);
          for (unsigned int j = k*sizeof(long); j < (k+1)*sizeof(long); j++) {
            tty->print("    byte[%d]:", j);
            tty->fill_to(13);
            for (unsigned int i = j*8; i < (j+1)*8; i++) {
              bool bit  = test_feature_bit(buffer, i, used_len*sizeof(long)*8);
              if (bit) {
                tty->print("  [%3.3d]", i);
              } else {
                tty->print("       ");
              }
            }
            tty->cr();
          }
        }
      }
    }
  } else {  // No features retrieved if we reach here. Buffer too short or instr not available.
    ok = false;
    if (used_len < 0) {
      if (printVerbose) {
        tty->print_cr("feature list buffer[%d] too short, required: buffer[%ld]", buf_len, -used_len);
      }
    } else {
      if (printVerbose) {
        tty->print_cr("feature list could not be retrieved. Bad function code? Running on z900 or z990?");
      }
    }
  }

  if (ok) {
    // Copy detected features to features buffer.
    copy_buffer(_features, buffer, buf_len);
    _nfeatures = used_len;
  } else {
    // Something went wrong with feature detection. Disable everything.
    clear_buffer(_features, buf_len);
    _nfeatures = 0;
  }

  if (has_VectorFacility()) {
    // Verify that feature can actually be used. OS support required.
    // We will get a signal if not. Signal handler will disable vector facility
    call_getFeatures(buffer, -4, 0);
    if (printVerbose) {
      ttyLocker ttyl;
      if (has_VectorFacility()) {
        tty->print_cr("  Vector Facility has been verified to be supported by OS");
      } else {
        tty->print_cr("  Vector Facility has been disabled - not supported by OS");
      }
    }
  }

  // Clear all Cipher feature buffers and the work buffer.
  clear_buffer(_cipher_features_KM, buf_len);
  clear_buffer(_cipher_features_KMA, buf_len);
  clear_buffer(_cipher_features_KMF, buf_len);
  clear_buffer(_cipher_features_KMCTR, buf_len);
  clear_buffer(_cipher_features_KMO, buf_len);
  clear_buffer(_msgdigest_features, buf_len);
  _ncipher_features_KM    = 0;
  _ncipher_features_KMA   = 0;
  _ncipher_features_KMF   = 0;
  _ncipher_features_KMCTR = 0;
  _ncipher_features_KMO   = 0;
  _nmsgdigest_features    = 0;

  //---------------------------------------
  //--  Extract Crypto Facility details  --
  //---------------------------------------

  if (has_Crypto()) {
    // Get features of KM/KMC cipher instructions
    clear_buffer(buffer, buf_len);
    used_len = call_getFeatures(buffer, -2, 0);
    copy_buffer(_cipher_features_KM, buffer, buf_len);
    _ncipher_features_KM = used_len;

    // Get msg digest features.
    clear_buffer(buffer, buf_len);
    used_len = call_getFeatures(buffer, -3, 0);
    copy_buffer(_msgdigest_features, buffer, buf_len);
    _nmsgdigest_features = used_len;
  }

  if (has_CryptoExt4()) {
    // Get features of KMF cipher instruction
    clear_buffer(buffer, buf_len);
    used_len = call_getFeatures(buffer, -17, 0);
    copy_buffer(_cipher_features_KMF, buffer, buf_len);
    _ncipher_features_KMF = used_len;

    // Get features of KMCTR cipher instruction
    clear_buffer(buffer, buf_len);
    used_len = call_getFeatures(buffer, -18, 0);
    copy_buffer(_cipher_features_KMCTR, buffer, buf_len);
    _ncipher_features_KMCTR = used_len;

    // Get features of KMO cipher instruction
    clear_buffer(buffer, buf_len);
    used_len = call_getFeatures(buffer, -19, 0);
    copy_buffer(_cipher_features_KMO, buffer, buf_len);
    _ncipher_features_KMO = used_len;
  }

  if (has_CryptoExt8()) {
    // Get features of KMA cipher instruction
    clear_buffer(buffer, buf_len);
    used_len = call_getFeatures(buffer, -20, 0);
    copy_buffer(_cipher_features_KMA, buffer, buf_len);
    _ncipher_features_KMA = used_len;
  }
  if (printVerbose) {
    tty->print_cr("  Crypto capabilities retrieved.");
  }

  static int   levelProperties[_max_cache_levels];     // All property indications per level.
  static int   levelScope[_max_cache_levels];          // private/shared
  static const char* levelScopeText[4] = {"No cache   ",
                                          "CPU private",
                                          "shared     ",
                                          "reserved   "};

  static int   levelType[_max_cache_levels];           // D/I/mixed
  static const char* levelTypeText[4]  = {"separate D and I caches",
                                          "I cache only           ",
                                          "D-cache only           ",
                                          "combined D/I cache     "};

  static unsigned int levelReserved[_max_cache_levels];    // reserved property bits
  static unsigned int levelLineSize[_max_cache_levels];
  static unsigned int levelTotalSize[_max_cache_levels];
  static unsigned int levelAssociativity[_max_cache_levels];


  // Extract Cache Layout details.
  if (has_ExtractCPUAttributes() && printVerbose) { // For information only, as of now.
    bool         lineSize_mismatch;
    bool         print_something;
    long         functionResult;
    unsigned int attributeIndication = 0; // 0..15
    unsigned int levelIndication     = 0; // 0..8
    unsigned int typeIndication      = 0; // 0..1 (D-Cache, I-Cache)
    int          functionCode        = calculate_ECAG_functionCode(attributeIndication, levelIndication, typeIndication);

    // Get cache topology.
    functionResult = call_getFeatures(buffer, -1, functionCode);

    for (unsigned int i = 0; i < _max_cache_levels; i++) {
      if (functionResult > 0) {
        int shiftVal          = 8*(_max_cache_levels-(i+1));
        levelProperties[i]    = (functionResult & (0xffUL<<shiftVal)) >> shiftVal;
        levelReserved[i]      = (levelProperties[i] & 0xf0) >> 4;
        levelScope[i]         = (levelProperties[i] & 0x0c) >> 2;
        levelType[i]          = (levelProperties[i] & 0x03);
      } else {
        levelProperties[i]    = 0;
        levelReserved[i]      = 0;
        levelScope[i]         = 0;
        levelType[i]          = 0;
      }
      levelLineSize[i]      = 0;
      levelTotalSize[i]     = 0;
      levelAssociativity[i] = 0;
    }

    tty->cr();
    tty->print_cr("------------------------------------");
    tty->print_cr("---  Cache Topology Information  ---");
    tty->print_cr("------------------------------------");
    for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
      tty->print_cr("  Cache Level %d: <scope>  %s | <type>  %s",
                    i+1, levelScopeText[levelScope[i]], levelTypeText[levelType[i]]);
    }

    // Get D-cache details per level.
    _Dcache_lineSize   = 0;
    lineSize_mismatch  = false;
    print_something    = false;
    typeIndication     = 0; // 0..1 (D-Cache, I-Cache)
    for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
      if ((levelType[i] == 0) || (levelType[i] == 2)) {
        print_something     = true;

        // Get cache line size of level i.
        attributeIndication   = 1;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelLineSize[i]      = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        // Get cache total size of level i.
        attributeIndication   = 2;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelTotalSize[i]     = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        // Get cache associativity of level i.
        attributeIndication   = 3;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelAssociativity[i] = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        _Dcache_lineSize      = _Dcache_lineSize == 0 ? levelLineSize[i] : _Dcache_lineSize;
        lineSize_mismatch     = lineSize_mismatch || (_Dcache_lineSize != levelLineSize[i]);
      } else {
        levelLineSize[i]      = 0;
      }
    }

    if (print_something) {
      tty->cr();
      tty->print_cr("------------------------------------");
      tty->print_cr("---  D-Cache Detail Information  ---");
      tty->print_cr("------------------------------------");
      if (lineSize_mismatch) {
        tty->print_cr("WARNING: D-Cache line size mismatch!");
      }
      for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
        if (levelLineSize[i] > 0) {
          tty->print_cr("  D-Cache Level %d: line size = %4d,  total size = %6dKB,  associativity = %2d",
                        i+1, levelLineSize[i], levelTotalSize[i]/(int)K, levelAssociativity[i]);
        }
      }
    }

    // Get I-cache details per level.
    _Icache_lineSize   = 0;
    lineSize_mismatch  = false;
    print_something    = false;
    typeIndication     = 1; // 0..1 (D-Cache, I-Cache)
    for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
      if ((levelType[i] == 0) || (levelType[i] == 1)) {
        print_something     = true;

        // Get cache line size of level i.
        attributeIndication   = 1;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelLineSize[i]      = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        // Get cache total size of level i.
        attributeIndication   = 2;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelTotalSize[i]     = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        // Get cache associativity of level i.
        attributeIndication   = 3;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelAssociativity[i] = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        _Icache_lineSize      = _Icache_lineSize == 0 ? levelLineSize[i] : _Icache_lineSize;
        lineSize_mismatch     = lineSize_mismatch || (_Icache_lineSize != levelLineSize[i]);
      } else {
        levelLineSize[i]      = 0;
      }
    }

    if (print_something) {
      tty->cr();
      tty->print_cr("------------------------------------");
      tty->print_cr("---  I-Cache Detail Information  ---");
      tty->print_cr("------------------------------------");
      if (lineSize_mismatch) {
        tty->print_cr("WARNING: I-Cache line size mismatch!");
      }
      for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
        if (levelLineSize[i] > 0) {
          tty->print_cr("  I-Cache Level %d: line size = %4d,  total size = %6dKB,  associativity = %2d",
                        i+1, levelLineSize[i], levelTotalSize[i]/(int)K, levelAssociativity[i]);
        }
      }
    }

    // Get D/I-cache details per level.
    lineSize_mismatch  = false;
    print_something    = false;
    typeIndication     = 0; // 0..1 (D-Cache, I-Cache)
    for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
      if (levelType[i] == 3) {
        print_something     = true;

        // Get cache line size of level i.
        attributeIndication   = 1;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelLineSize[i]      = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        // Get cache total size of level i.
        attributeIndication   = 2;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelTotalSize[i]     = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        // Get cache associativity of level i.
        attributeIndication   = 3;
        functionCode          = calculate_ECAG_functionCode(attributeIndication, i, typeIndication);
        levelAssociativity[i] = (unsigned int)call_getFeatures(buffer, -1, functionCode);

        _Dcache_lineSize      = _Dcache_lineSize == 0 ? levelLineSize[i] : _Dcache_lineSize;
        _Icache_lineSize      = _Icache_lineSize == 0 ? levelLineSize[i] : _Icache_lineSize;
        lineSize_mismatch     = lineSize_mismatch || (_Dcache_lineSize != levelLineSize[i])
                                                  || (_Icache_lineSize != levelLineSize[i]);
      } else {
        levelLineSize[i]      = 0;
      }
    }

    if (print_something) {
      tty->cr();
      tty->print_cr("--------------------------------------");
      tty->print_cr("---  D/I-Cache Detail Information  ---");
      tty->print_cr("--------------------------------------");
      if (lineSize_mismatch) {
        tty->print_cr("WARNING: D/I-Cache line size mismatch!");
      }
      for (unsigned int i = 0; (i < _max_cache_levels) && (levelProperties[i] != 0); i++) {
        if (levelLineSize[i] > 0) {
          tty->print_cr("  D/I-Cache Level %d: line size = %4d,  total size = %6dKB,  associativity = %2d",
                        i+1, levelLineSize[i], levelTotalSize[i]/(int)K, levelAssociativity[i]);
        }
      }
    }
    tty->cr();
  }
  return;
}

unsigned long VM_Version::z_SIGILL() {
  unsigned long   ZeroBuffer = 0;
  unsigned long   work;
  asm(
    "     LA      %[work],%[buffer]  \n\t"   // Load address of buffer.
    "     LARL    14,+6              \n\t"   // Load address of faulting instruction.
    "     BCR     15,%[work]         \n\t"   // Branch into buffer, execute whatever is in there.
    : [buffer]  "+Q"  (ZeroBuffer)   /* outputs   */
    , [work]   "=&a"  (work)         /* outputs   */
    :                                /* inputs    */
    : "cc"                           /* clobbered */
 );
  return ZeroBuffer;
}

unsigned long VM_Version::z_SIGSEGV() {
  unsigned long   ZeroBuffer = 0;
  unsigned long   work;
  asm(
    "     LG      %[work],%[buffer]  \n\t"   // Load zero address.
    "     STG     %[work],0(,%[work])\n\t"   // Store to address zero.
    : [buffer]  "+Q"  (ZeroBuffer)   /* outputs   */
    , [work]   "=&a"  (work)         /* outputs   */
    :                                /* inputs    */
    : "cc"                           /* clobbered */
 );
  return ZeroBuffer;
}
