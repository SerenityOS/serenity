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

#ifndef CPU_S390_VM_VERSION_S390_HPP
#define CPU_S390_VM_VERSION_S390_HPP


#include "runtime/abstract_vm_version.hpp"
#include "runtime/globals_extension.hpp"

class VM_Version: public Abstract_VM_Version {

 protected:
// z/Architecture is the name of the 64-bit extension of the 31-bit s390
// architecture.
//
// For information concerning the life span of the individual
// z/Architecture models, please check out the comments/tables
// in vm_version_s390.cpp

// ----------------------------------------------
// --- FeatureBitString Bits   0.. 63 (DW[0]) ---
// ----------------------------------------------
//                                           11222334445566
//                                        04826048260482604
#define  StoreFacilityListExtendedMask  0x0100000000000000UL  // z9
#define  ETF2Mask                       0x0000800000000000UL  // z900
#define  CryptoFacilityMask             0x0000400000000000UL  // z990 (aka message-security assist)
#define  LongDispFacilityMask           0x0000200000000000UL  // z900 with microcode update
#define  LongDispFacilityHighPerfMask   0x0000300000000000UL  // z990
#define  HFPMultiplyAndAddMask          0x0000080000000000UL  // z990
#define  ExtImmedFacilityMask           0x0000040000000000UL  // z9
#define  ETF3Mask                       0x0000020000000000UL  // z990/z9 (?)
#define  HFPUnnormalizedMask            0x0000010000000000UL  // z9
#define  ETF2EnhancementMask            0x0000008000000000UL  // z9
#define  StoreClockFastMask             0x0000004000000000UL  // z9
#define  ParsingEnhancementsMask        0x0000002000000000UL  // z10(?)
#define  ETF3EnhancementMask            0x0000000200000000UL  // z9
#define  ExtractCPUTimeMask             0x0000000100000000UL  // z10
#define  CompareSwapStoreMask           0x00000000c0000000UL  // z10
#define  GnrlInstrExtFacilityMask       0x0000000020000000UL  // z10
#define  ExecuteExtensionsMask          0x0000000010000000UL  // z10
#define  FPExtensionsMask               0x0000000004000000UL  // z196
#define  FPSupportEnhancementsMask      0x0000000000400000UL  // z10
#define  DecimalFloatingPointMask       0x0000000000300000UL  // z10
// z196 begin
#define  DistinctOpndsMask              0x0000000000040000UL  // z196
#define  FastBCRSerializationMask       DistinctOpndsMask
#define  HighWordMask                   DistinctOpndsMask
#define  LoadStoreConditionalMask       DistinctOpndsMask
#define  PopulationCountMask            DistinctOpndsMask
#define  InterlockedAccess1Mask         DistinctOpndsMask
// z196 end
// EC12 begin
#define  DFPZonedConversionMask         0x0000000000008000UL  // ec12
#define  MiscInstrExtMask               0x0000000000004000UL  // ec12
#define  ExecutionHintMask              MiscInstrExtMask
#define  LoadAndTrapMask                MiscInstrExtMask
#define  ProcessorAssistMask            MiscInstrExtMask
#define  ConstrainedTxExecutionMask     0x0000000000002000UL  // ec12
#define  InterlockedAccess2Mask         0x0000000000000800UL  // ec12
// EC12 end
// z13 begin
#define  LoadStoreConditional2Mask      0x0000000000000400UL  // z13
#define  CryptoExtension5Mask           0x0000000000000040UL  // z13
// z13 end
#define  MiscInstrExt2Mask              0x0000000000000020UL  // z14
#define  MiscInstrExt3Mask              0x0000000000000004UL  // z15
// ----------------------------------------------
// --- FeatureBitString Bits  64..127 (DW[1]) ---
// ----------------------------------------------
//                                                 11111111
//                                        66778889900011222
//                                        48260482604826048
#define  TransactionalExecutionMask     0x0040000000000000UL  // ec12
#define  CryptoExtension3Mask           0x0008000000000000UL  // z196
#define  CryptoExtension4Mask           0x0004000000000000UL  // z196 (aka message-security assist extension 4, for KMF, KMCTR, KMO)
#define  DFPPackedConversionMask        0x0000800000000000UL  // z13
// ----------------------------------------------
// --- FeatureBitString Bits 128..192 (DW[2]) ---
// ----------------------------------------------
//                                        11111111111111111
//                                        23344455666778889
//                                        82604826048260482
#define  VectorFacilityMask             0x4000000000000000UL  // z13, not avail in VM guest mode!
#define  ExecutionProtectionMask        0x2000000000000000UL  // z14
#define  GuardedStorageMask             0x0400000000000000UL  // z14
#define  VectorEnhancements1Mask        0x0100000000000000UL  // z14
#define  VectorPackedDecimalMask        0x0200000000000000UL  // z14
#define  CryptoExtension8Mask           0x0000200000000000UL  // z14 (aka message-security assist extension 8, for KMA)
#define  VectorEnhancements2Mask        0x0000080000000000UL  // z15
#define  VectorPackedDecimalEnhMask     0x0000008000000000UL  // z15
#define  CryptoExtension9Mask           0x0000001000000000UL  // z15 (aka message-security assist extension 9)
#define  DeflateMask                    0x0000010000000000UL  // z15

  enum {
    _max_cache_levels = 8,    // As limited by ECAG instruction.
    _features_buffer_len = 4, // in DW
    _code_buffer_len = 2*256  // For feature detection code.
  };
  static unsigned long _features[_features_buffer_len];
  static unsigned long _cipher_features_KM[_features_buffer_len];
  static unsigned long _cipher_features_KMA[_features_buffer_len];
  static unsigned long _cipher_features_KMF[_features_buffer_len];
  static unsigned long _cipher_features_KMCTR[_features_buffer_len];
  static unsigned long _cipher_features_KMO[_features_buffer_len];
  static unsigned long _msgdigest_features[_features_buffer_len];
  static unsigned int  _nfeatures;
  static unsigned int  _ncipher_features_KM;
  static unsigned int  _ncipher_features_KMA;
  static unsigned int  _ncipher_features_KMF;
  static unsigned int  _ncipher_features_KMCTR;
  static unsigned int  _ncipher_features_KMO;
  static unsigned int  _nmsgdigest_features;
  static unsigned int  _Dcache_lineSize;
  static unsigned int  _Icache_lineSize;
  static bool          _is_determine_features_test_running;
  static const char*   _model_string;

  static bool test_feature_bit(unsigned long* featureBuffer, int featureNum, unsigned int bufLen);
  static int  get_model_index();
  static void set_features_string();
  static void print_features_internal(const char* text, bool print_anyway=false);
  static void determine_features();
  static long call_getFeatures(unsigned long* buffer, int buflen, int functionCode);
  static void set_getFeatures(address entryPoint);
  static void clear_buffer(unsigned long* buffer, unsigned int len);
  static void copy_buffer(unsigned long* to, unsigned long* from, unsigned int len);
  static int  calculate_ECAG_functionCode(unsigned int attributeIndication,
                                          unsigned int levelIndication,
                                          unsigned int typeIndication);

  // Setting features via march=z900|z990|z9|z10|z196|ec12|z13|z14|z15 commandline option.
  static void reset_features(bool reset);
  static void set_features_z900(bool reset = true);
  static void set_features_z990(bool reset = true);
  static void set_features_z9(bool reset = true);
  static void set_features_z10(bool reset = true);
  static void set_features_z196(bool reset = true);
  static void set_features_ec12(bool reset = true);
  static void set_features_z13(bool reset = true);
  static void set_features_z14(bool reset = true);
  static void set_features_z15(bool reset = true);
  static void set_features_from(const char* march);

  // Get information about cache line sizes.
  // As of now and the foreseeable future, line size of all levels will be the same and 256.
  static unsigned int Dcache_lineSize(unsigned int level = 0) { return _Dcache_lineSize; }
  static unsigned int Icache_lineSize(unsigned int level = 0) { return _Icache_lineSize; }

 public:

  // Get the CPU type from feature bit settings.
  static bool is_z900() { return has_long_displacement()      && !has_long_displacement_fast(); }
  static bool is_z990() { return has_long_displacement_fast() && !has_extended_immediate();  }
  static bool is_z9()   { return has_extended_immediate()     && !has_GnrlInstrExtensions(); }
  static bool is_z10()  { return has_GnrlInstrExtensions()    && !has_DistinctOpnds(); }
  static bool is_z196() { return has_DistinctOpnds()          && !has_MiscInstrExt(); }
  static bool is_ec12() { return has_MiscInstrExt()           && !has_CryptoExt5(); }
  static bool is_z13()  { return has_CryptoExt5()             && !has_MiscInstrExt2();}
  static bool is_z14()  { return has_MiscInstrExt2()          && !has_MiscInstrExt3();}
  static bool is_z15()  { return has_MiscInstrExt3();}

  // Need to use nested class with unscoped enum.
  // C++11 declaration "enum class Cipher { ... } is not supported.
  class CipherMode {
    public:
      enum {
        cipher   = 0x00,
        decipher = 0x80
      };
  };
  class Cipher {
   public:
    enum { // KM only!!! KMC uses different parmBlk sizes.
      _Query              =   0,
      _DEA                =   1,
      _TDEA128            =   2,
      _TDEA192            =   3,
      _EncryptedDEA       =   9,
      _EncryptedDEA128    =  10,
      _EncryptedDEA192    =  11,
      _AES128             =  18,
      _AES192             =  19,
      _AES256             =  20,
      _EnccryptedAES128   =  26,
      _EnccryptedAES192   =  27,
      _EnccryptedAES256   =  28,
      _XTSAES128          =  50,
      _XTSAES256          =  52,
      _EncryptedXTSAES128 =  58,
      _EncryptedXTSAES256 =  60,
      _PRNG               =  67,
      _featureBits        = 128,

      // Parameter block sizes (in bytes) for KM instruction.
      _Query_parmBlk              =  16,
      _DEA_parmBlk                =   8,
      _TDEA128_parmBlk            =  16,
      _TDEA192_parmBlk            =  24,
      _EncryptedDEA_parmBlk       =  32,
      _EncryptedDEA128_parmBlk    =  40,
      _EncryptedDEA192_parmBlk    =  48,
      _AES128_parmBlk             =  16,
      _AES192_parmBlk             =  24,
      _AES256_parmBlk             =  32,
      _EnccryptedAES128_parmBlk   =  48,
      _EnccryptedAES192_parmBlk   =  56,
      _EnccryptedAES256_parmBlk   =  64,
      _XTSAES128_parmBlk          =  32,
      _XTSAES256_parmBlk          =  48,
      _EncryptedXTSAES128_parmBlk =  64,
      _EncryptedXTSAES256_parmBlk =  80,

      // Parameter block sizes (in bytes) for KMC instruction.
      _Query_parmBlk_C              =  16,
      _DEA_parmBlk_C                =  16,
      _TDEA128_parmBlk_C            =  24,
      _TDEA192_parmBlk_C            =  32,
      _EncryptedDEA_parmBlk_C       =  40,
      _EncryptedDEA128_parmBlk_C    =  48,
      _EncryptedDEA192_parmBlk_C    =  56,
      _AES128_parmBlk_C             =  32,
      _AES192_parmBlk_C             =  40,
      _AES256_parmBlk_C             =  48,
      _EnccryptedAES128_parmBlk_C   =  64,
      _EnccryptedAES192_parmBlk_C   =  72,
      _EnccryptedAES256_parmBlk_C   =  80,
      _XTSAES128_parmBlk_C          =  32,
      _XTSAES256_parmBlk_C          =  48,
      _EncryptedXTSAES128_parmBlk_C =  64,
      _EncryptedXTSAES256_parmBlk_C =  80,
      _PRNG_parmBlk_C               =  32,

      // Data block sizes (in bytes).
      _Query_dataBlk              =   0,
      _DEA_dataBlk                =   8,
      _TDEA128_dataBlk            =   8,
      _TDEA192_dataBlk            =   8,
      _EncryptedDEA_dataBlk       =   8,
      _EncryptedDEA128_dataBlk    =   8,
      _EncryptedDEA192_dataBlk    =   8,
      _AES128_dataBlk             =  16,
      _AES192_dataBlk             =  16,
      _AES256_dataBlk             =  16,
      _EnccryptedAES128_dataBlk   =  16,
      _EnccryptedAES192_dataBlk   =  16,
      _EnccryptedAES256_dataBlk   =  16,
      _XTSAES128_dataBlk          =  16,
      _XTSAES256_dataBlk          =  16,
      _EncryptedXTSAES128_dataBlk =  16,
      _EncryptedXTSAES256_dataBlk =  16,
      _PRNG_dataBlk               =   8,
    };
  };
  class MsgDigest {
    public:
      enum {
        _Query                =   0,
        _SHA1                 =   1,
        _SHA256               =   2,
        _SHA512               =   3,
        _SHA3_224             =  32,
        _SHA3_256             =  33,
        _SHA3_384             =  34,
        _SHA3_512             =  35,
        _SHAKE_128            =  36,
        _SHAKE_256            =  37,
        _GHASH                =  65,
        _featureBits          = 128,

        // Parameter block sizes (in bytes) for KIMD.
        _Query_parmBlk_I      =  16,
        _SHA1_parmBlk_I       =  20,
        _SHA256_parmBlk_I     =  32,
        _SHA512_parmBlk_I     =  64,
        _SHA3_224_parmBlk_I   = 200,
        _SHA3_256_parmBlk_I   = 200,
        _SHA3_384_parmBlk_I   = 200,
        _SHA3_512_parmBlk_I   = 200,
        _SHAKE_128_parmBlk_I  = 200,
        _SHAKE_256_parmBlk_I  = 200,
        _GHASH_parmBlk_I      =  32,

        // Parameter block sizes (in bytes) for KLMD.
        _Query_parmBlk_L      =  16,
        _SHA1_parmBlk_L       =  28,
        _SHA256_parmBlk_L     =  40,
        _SHA512_parmBlk_L     =  80,
        _SHA3_224_parmBlk_L   = 200,
        _SHA3_256_parmBlk_L   = 200,
        _SHA3_384_parmBlk_L   = 200,
        _SHA3_512_parmBlk_L   = 200,
        _SHAKE_128_parmBlk_L  = 200,
        _SHAKE_256_parmBlk_L  = 200,

        // Data block sizes (in bytes).
        _Query_dataBlk        =   0,
        _SHA1_dataBlk         =  64,
        _SHA256_dataBlk       =  64,
        _SHA512_dataBlk       = 128,
        _SHA3_224_dataBlk     = 144,
        _SHA3_256_dataBlk     = 136,
        _SHA3_384_dataBlk     = 104,
        _SHA3_512_dataBlk     =  72,
        _SHAKE_128_dataBlk    = 168,
        _SHAKE_256_dataBlk    = 136,
        _GHASH_dataBlk        =  16
      };
  };
  class MsgAuthent {
    public:
      enum {
        _Query              =   0,
        _DEA                =   1,
        _TDEA128            =   2,
        _TDEA192            =   3,
        _EncryptedDEA       =   9,
        _EncryptedDEA128    =  10,
        _EncryptedDEA192    =  11,
        _AES128             =  18,
        _AES192             =  19,
        _AES256             =  20,
        _EnccryptedAES128   =  26,
        _EnccryptedAES192   =  27,
        _EnccryptedAES256   =  28,
        _featureBits        = 128,

        _Query_parmBlk            =  16,
        _DEA_parmBlk              =  16,
        _TDEA128_parmBlk          =  24,
        _TDEA192_parmBlk          =  32,
        _EncryptedDEA_parmBlk     =  40,
        _EncryptedDEA128_parmBlk  =  48,
        _EncryptedDEA192_parmBlk  =  56,
        _AES128_parmBlk           =  32,
        _AES192_parmBlk           =  40,
        _AES256_parmBlk           =  48,
        _EnccryptedAES128_parmBlk =  64,
        _EnccryptedAES192_parmBlk =  72,
        _EnccryptedAES256_parmBlk =  80,

        _Query_dataBlk            =   0,
        _DEA_dataBlk              =   8,
        _TDEA128_dataBlk          =   8,
        _TDEA192_dataBlk          =   8,
        _EncryptedDEA_dataBlk     =   8,
        _EncryptedDEA128_dataBlk  =   8,
        _EncryptedDEA192_dataBlk  =   8,
        _AES128_dataBlk           =  16,
        _AES192_dataBlk           =  16,
        _AES256_dataBlk           =  16,
        _EnccryptedAES128_dataBlk =  16,
        _EnccryptedAES192_dataBlk =  16,
        _EnccryptedAES256_dataBlk =  16
      };
  };

  // Initialization
  static void initialize();
  static void print_features();
  static bool is_determine_features_test_running() { return _is_determine_features_test_running; }

  // Override Abstract_VM_Version implementation
  static void print_platform_virtualization_info(outputStream*);

  // s390 supports fast class initialization checks for static methods.
  static bool supports_fast_class_init_checks() { return true; }

  // CPU feature query functions
  static const char* get_model_string()       { return _model_string; }
  static bool has_StoreFacilityListExtended() { return  (_features[0] & StoreFacilityListExtendedMask) == StoreFacilityListExtendedMask; }
  static bool has_Crypto()                    { return  (_features[0] & CryptoFacilityMask)            == CryptoFacilityMask; }
  static bool has_ETF2()                      { return  (_features[0] & ETF2Mask)                      == ETF2Mask; }
  static bool has_ETF3()                      { return  (_features[0] & ETF3Mask)                      == ETF3Mask; }
  static bool has_ETF2Enhancements()          { return  (_features[0] & ETF2EnhancementMask)           == ETF2EnhancementMask; }
  static bool has_ETF3Enhancements()          { return  (_features[0] & ETF3EnhancementMask)           == ETF3EnhancementMask; }
  static bool has_ParsingEnhancements()       { return  (_features[0] & ParsingEnhancementsMask)       == ParsingEnhancementsMask; }
  static bool has_long_displacement()         { return  (_features[0] & LongDispFacilityMask)          == LongDispFacilityMask; }
  static bool has_long_displacement_fast()    { return  (_features[0] & LongDispFacilityHighPerfMask)  == LongDispFacilityHighPerfMask; }
  static bool has_extended_immediate()        { return  (_features[0] & ExtImmedFacilityMask)          == ExtImmedFacilityMask; }
  static bool has_StoreClockFast()            { return  (_features[0] & StoreClockFastMask)            == StoreClockFastMask; }
  static bool has_ExtractCPUtime()            { return  (_features[0] & ExtractCPUTimeMask)            == ExtractCPUTimeMask; }
  static bool has_CompareSwapStore()          { return  (_features[0] & CompareSwapStoreMask)          == CompareSwapStoreMask; }

  static bool has_HFPMultiplyAndAdd()         { return  (_features[0] & HFPMultiplyAndAddMask)         == HFPMultiplyAndAddMask; }
  static bool has_HFPUnnormalized()           { return  (_features[0] & HFPUnnormalizedMask)           == HFPUnnormalizedMask; }

  // Make sure we don't run on older ...
  static bool has_GnrlInstrExtensions()       { guarantee((_features[0] & GnrlInstrExtFacilityMask)    == GnrlInstrExtFacilityMask, "We no more support older than z10."); return true; }
  static bool has_CompareBranch()             { return  has_GnrlInstrExtensions() && is_z10(); } // Only z10 benefits from these.
  static bool has_CompareTrap()               { return  has_GnrlInstrExtensions(); }
  static bool has_RelativeLoadStore()         { return  has_GnrlInstrExtensions(); }
  static bool has_MultiplySingleImm32()       { return  has_GnrlInstrExtensions(); }
  static bool has_Prefetch()                  { return  has_GnrlInstrExtensions() && (AllocatePrefetchStyle > 0); }
  static bool has_PrefetchRaw()               { return  has_GnrlInstrExtensions(); }
  static bool has_MoveImmToMem()              { return  has_GnrlInstrExtensions(); }
  static bool has_ExtractCPUAttributes()      { return  has_GnrlInstrExtensions(); }
  static bool has_ExecuteExtensions()         { return  (_features[0] & ExecuteExtensionsMask)         == ExecuteExtensionsMask; }
  // Memory-immediate arithmetic instructions. There is no performance penalty in using them.
  // Moreover, these memory-immediate instructions are quasi-atomic (>99.99%) on z10
  // and 100% atomic from z196 onwards, thanks to the specific operand serialization that comes new with z196.
  static bool has_MemWithImmALUOps()          { return  has_GnrlInstrExtensions(); }
  static bool has_AtomicMemWithImmALUOps()    { return   has_MemWithImmALUOps() && has_InterlockedAccessV1(); }
  static bool has_FPExtensions()              { return  (_features[0] & FPExtensionsMask)              == FPExtensionsMask; }
  static bool has_FPSupportEnhancements()     { return  (_features[0] & FPSupportEnhancementsMask)     == FPSupportEnhancementsMask; }
  static bool has_DecimalFloatingPoint()      { return  (_features[0] & DecimalFloatingPointMask)      == DecimalFloatingPointMask; }
  static bool has_InterlockedAccessV1()       { return  (_features[0] & InterlockedAccess1Mask)        == InterlockedAccess1Mask; }
  static bool has_LoadAndALUAtomicV1()        { return  (_features[0] & InterlockedAccess1Mask)        == InterlockedAccess1Mask; }
  static bool has_PopCount()                  { return  (_features[0] & PopulationCountMask)           == PopulationCountMask; }
  static bool has_LoadStoreConditional()      { return  (_features[0] & LoadStoreConditionalMask)      == LoadStoreConditionalMask; }
  static bool has_HighWordInstr()             { return  (_features[0] & HighWordMask)                  == HighWordMask; }
  static bool has_FastSync()                  { return  (_features[0] & FastBCRSerializationMask)      == FastBCRSerializationMask; }
  static bool has_DistinctOpnds()             { return  (_features[0] & DistinctOpndsMask)             == DistinctOpndsMask; }
  static bool has_DFPZonedConversion()        { return  (_features[0] & DFPZonedConversionMask)        == DFPZonedConversionMask; }
  static bool has_DFPPackedConversion()       { return  (_features[1] & DFPPackedConversionMask)       == DFPPackedConversionMask; }
  static bool has_MiscInstrExt()              { return  (_features[0] & MiscInstrExtMask)              == MiscInstrExtMask; }
  static bool has_MiscInstrExt2()             { return  (_features[0] & MiscInstrExt2Mask)             == MiscInstrExt2Mask; }
  static bool has_MiscInstrExt3()             { return  (_features[0] & MiscInstrExt3Mask)             == MiscInstrExt3Mask; }
  static bool has_ExecutionHint()             { return  (_features[0] & ExecutionHintMask)             == ExecutionHintMask; }
  static bool has_LoadAndTrap()               { return  (_features[0] & LoadAndTrapMask)               == LoadAndTrapMask; }
  static bool has_ProcessorAssist()           { return  (_features[0] & ProcessorAssistMask)           == ProcessorAssistMask; }
  static bool has_InterlockedAccessV2()       { return  (_features[0] & InterlockedAccess2Mask)        == InterlockedAccess2Mask; }
  static bool has_LoadAndALUAtomicV2()        { return  (_features[0] & InterlockedAccess2Mask)        == InterlockedAccess2Mask; }
  static bool has_TxMem()                     { return ((_features[1] & TransactionalExecutionMask)    == TransactionalExecutionMask) &&
                                                       ((_features[0] & ConstrainedTxExecutionMask)    == ConstrainedTxExecutionMask); }
  static bool has_CryptoExt3()                { return  (_features[1] & CryptoExtension3Mask)          == CryptoExtension3Mask; }
  static bool has_CryptoExt4()                { return  (_features[1] & CryptoExtension4Mask)          == CryptoExtension4Mask; }
  static bool has_CryptoExt5()                { return  (_features[0] & CryptoExtension5Mask)          == CryptoExtension5Mask; }
  static bool has_CryptoExt8()                { return  (_features[2] & CryptoExtension8Mask)          == CryptoExtension8Mask; }
  static bool has_CryptoExt9()                { return  (_features[2] & CryptoExtension9Mask)          == CryptoExtension9Mask; }
  static bool has_LoadStoreConditional2()     { return  (_features[0] & LoadStoreConditional2Mask)     == LoadStoreConditional2Mask; }
  static bool has_VectorFacility()            { return  (_features[2] & VectorFacilityMask)            == VectorFacilityMask; }
  static bool has_VectorEnhancements1()       { return  (_features[2] & VectorEnhancements1Mask)       == VectorEnhancements1Mask; }
  static bool has_VectorEnhancements2()       { return  (_features[2] & VectorEnhancements2Mask)       == VectorEnhancements2Mask; }
  static bool has_VectorPackedDecimal()       { return  (_features[2] & VectorPackedDecimalMask)       == VectorPackedDecimalMask; }
  static bool has_VectorPackedDecimalEnh()    { return  (_features[2] & VectorPackedDecimalEnhMask)    == VectorPackedDecimalEnhMask; }

  // Crypto features query functions.
  static bool has_Crypto_AES128()             { return has_Crypto() && test_feature_bit(&_cipher_features_KM[0], Cipher::_AES128, Cipher::_featureBits); }
  static bool has_Crypto_AES192()             { return has_Crypto() && test_feature_bit(&_cipher_features_KM[0], Cipher::_AES192, Cipher::_featureBits); }
  static bool has_Crypto_AES256()             { return has_Crypto() && test_feature_bit(&_cipher_features_KM[0], Cipher::_AES256, Cipher::_featureBits); }
  static bool has_Crypto_AES()                { return has_Crypto_AES128() || has_Crypto_AES192() || has_Crypto_AES256(); }

  static bool has_Crypto_SHA1()               { return has_Crypto() && test_feature_bit(&_msgdigest_features[0], MsgDigest::_SHA1,   MsgDigest::_featureBits); }
  static bool has_Crypto_SHA256()             { return has_Crypto() && test_feature_bit(&_msgdigest_features[0], MsgDigest::_SHA256, MsgDigest::_featureBits); }
  static bool has_Crypto_SHA512()             { return has_Crypto() && test_feature_bit(&_msgdigest_features[0], MsgDigest::_SHA512, MsgDigest::_featureBits); }
  static bool has_Crypto_GHASH()              { return has_Crypto() && test_feature_bit(&_msgdigest_features[0], MsgDigest::_GHASH,  MsgDigest::_featureBits); }
  static bool has_Crypto_SHA()                { return has_Crypto_SHA1() || has_Crypto_SHA256() || has_Crypto_SHA512() || has_Crypto_GHASH(); }

  // CPU feature setters (to force model-specific behaviour). Test/debugging only.
  static void set_has_DecimalFloatingPoint()      { _features[0] |= DecimalFloatingPointMask; }
  static void set_has_FPSupportEnhancements()     { _features[0] |= FPSupportEnhancementsMask; }
  static void set_has_ExecuteExtensions()         { _features[0] |= ExecuteExtensionsMask; }
  static void set_has_MemWithImmALUOps()          { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_MoveImmToMem()              { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_Prefetch()                  { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_MultiplySingleImm32()       { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_CompareBranch()             { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_CompareTrap()               { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_RelativeLoadStore()         { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_GnrlInstrExtensions()       { _features[0] |= GnrlInstrExtFacilityMask; }
  static void set_has_CompareSwapStore()          { _features[0] |= CompareSwapStoreMask; }
  static void set_has_HFPMultiplyAndAdd()         { _features[0] |= HFPMultiplyAndAddMask; }
  static void set_has_HFPUnnormalized()           { _features[0] |= HFPUnnormalizedMask; }
  static void set_has_ExtractCPUtime()            { _features[0] |= ExtractCPUTimeMask; }
  static void set_has_StoreClockFast()            { _features[0] |= StoreClockFastMask; }
  static void set_has_extended_immediate()        { _features[0] |= ExtImmedFacilityMask; }
  static void set_has_long_displacement_fast()    { _features[0] |= LongDispFacilityHighPerfMask; }
  static void set_has_long_displacement()         { _features[0] |= LongDispFacilityMask; }
  static void set_has_ETF2()                      { _features[0] |= ETF2Mask; }
  static void set_has_ETF3()                      { _features[0] |= ETF3Mask; }
  static void set_has_ETF2Enhancements()          { _features[0] |= ETF2EnhancementMask; }
  static void set_has_ETF3Enhancements()          { _features[0] |= ETF3EnhancementMask; }
  static void set_has_Crypto()                    { _features[0] |= CryptoFacilityMask; }
  static void set_has_StoreFacilityListExtended() { _features[0] |= StoreFacilityListExtendedMask; }

  static void set_has_InterlockedAccessV1()       { _features[0] |= InterlockedAccess1Mask; }
  static void set_has_PopCount()                  { _features[0] |= PopulationCountMask; }
  static void set_has_LoadStoreConditional()      { _features[0] |= LoadStoreConditionalMask; }
  static void set_has_HighWordInstr()             { _features[0] |= HighWordMask; }
  static void set_has_FastSync()                  { _features[0] |= FastBCRSerializationMask; }
  static void set_has_DistinctOpnds()             { _features[0] |= DistinctOpndsMask; }
  static void set_has_FPExtensions()              { _features[0] |= FPExtensionsMask; }
  static void set_has_MiscInstrExt()              { _features[0] |= MiscInstrExtMask; }
  static void set_has_MiscInstrExt2()             { _features[0] |= MiscInstrExt2Mask; }
  static void set_has_MiscInstrExt3()             { _features[0] |= MiscInstrExt3Mask; }
  static void set_has_ProcessorAssist()           { _features[0] |= ProcessorAssistMask; }
  static void set_has_InterlockedAccessV2()       { _features[0] |= InterlockedAccess2Mask; }
  static void set_has_LoadAndALUAtomicV2()        { _features[0] |= InterlockedAccess2Mask; }
  static void set_has_TxMem()                     { _features[0] |= ConstrainedTxExecutionMask; _features[1] |= TransactionalExecutionMask; }
  static void set_has_LoadStoreConditional2()     { _features[0] |= LoadStoreConditional2Mask; }
  static void set_has_CryptoExt3()                { _features[1] |= CryptoExtension3Mask; }
  static void set_has_CryptoExt4()                { _features[1] |= CryptoExtension4Mask; }
  static void set_has_CryptoExt5()                { _features[0] |= CryptoExtension5Mask; }
  static void set_has_CryptoExt8()                { _features[2] |= CryptoExtension8Mask; }
  static void set_has_CryptoExt9()                { _features[2] |= CryptoExtension9Mask; }
  static void set_has_VectorFacility()            { _features[2] |= VectorFacilityMask; }
  static void set_has_VectorEnhancements1()       { _features[2] |= VectorEnhancements1Mask; }
  static void set_has_VectorEnhancements2()       { _features[2] |= VectorEnhancements2Mask; }
  static void set_has_VectorPackedDecimal()       { _features[2] |= VectorPackedDecimalMask; }
  static void set_has_VectorPackedDecimalEnh()    { _features[2] |= VectorPackedDecimalEnhMask; }

  static void reset_has_VectorFacility()          { _features[2] &= ~VectorFacilityMask; }

  // Assembler testing.
  static void allow_all();
  static void revert();

  // Generate trapping instructions into C-code.
  // Sometimes helpful for debugging.
  static unsigned long z_SIGILL();
  static unsigned long z_SIGSEGV();
};

#endif // CPU_S390_VM_VERSION_S390_HPP
