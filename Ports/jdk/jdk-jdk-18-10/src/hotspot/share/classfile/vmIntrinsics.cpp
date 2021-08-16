/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jvm_constants.h"
#include "jvm_io.h"
#include "classfile/vmIntrinsics.hpp"
#include "classfile/vmSymbols.hpp"
#include "compiler/compilerDirectives.hpp"
#include "utilities/xmlstream.hpp"

// These are flag-matching functions:
inline bool match_F_R(jshort flags) {
  const int req = 0;
  const int neg = JVM_ACC_STATIC | JVM_ACC_SYNCHRONIZED;
  return (flags & (req | neg)) == req;
}

inline bool match_F_Y(jshort flags) {
  const int req = JVM_ACC_SYNCHRONIZED;
  const int neg = JVM_ACC_STATIC;
  return (flags & (req | neg)) == req;
}

inline bool match_F_RN(jshort flags) {
  const int req = JVM_ACC_NATIVE;
  const int neg = JVM_ACC_STATIC | JVM_ACC_SYNCHRONIZED;
  return (flags & (req | neg)) == req;
}

inline bool match_F_S(jshort flags) {
  const int req = JVM_ACC_STATIC;
  const int neg = JVM_ACC_SYNCHRONIZED;
  return (flags & (req | neg)) == req;
}

inline bool match_F_SN(jshort flags) {
  const int req = JVM_ACC_STATIC | JVM_ACC_NATIVE;
  const int neg = JVM_ACC_SYNCHRONIZED;
  return (flags & (req | neg)) == req;
}

bool vmIntrinsics::preserves_state(vmIntrinsics::ID id) {
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");
  switch(id) {
#ifdef JFR_HAVE_INTRINSICS
  case vmIntrinsics::_counterTime:
#endif
  case vmIntrinsics::_currentTimeMillis:
  case vmIntrinsics::_nanoTime:
  case vmIntrinsics::_floatToRawIntBits:
  case vmIntrinsics::_intBitsToFloat:
  case vmIntrinsics::_doubleToRawLongBits:
  case vmIntrinsics::_longBitsToDouble:
  case vmIntrinsics::_getClass:
  case vmIntrinsics::_isInstance:
  case vmIntrinsics::_currentThread:
  case vmIntrinsics::_dabs:
  case vmIntrinsics::_fabs:
  case vmIntrinsics::_iabs:
  case vmIntrinsics::_labs:
  case vmIntrinsics::_dsqrt:
  case vmIntrinsics::_dsin:
  case vmIntrinsics::_dcos:
  case vmIntrinsics::_dtan:
  case vmIntrinsics::_dlog:
  case vmIntrinsics::_dlog10:
  case vmIntrinsics::_dexp:
  case vmIntrinsics::_dpow:
  case vmIntrinsics::_Preconditions_checkIndex:
  case vmIntrinsics::_Preconditions_checkLongIndex:
  case vmIntrinsics::_Reference_get:
  case vmIntrinsics::_updateCRC32:
  case vmIntrinsics::_updateBytesCRC32:
  case vmIntrinsics::_updateByteBufferCRC32:
  case vmIntrinsics::_updateBytesAdler32:
  case vmIntrinsics::_vectorizedMismatch:
  case vmIntrinsics::_fmaD:
  case vmIntrinsics::_fmaF:
  case vmIntrinsics::_isDigit:
  case vmIntrinsics::_isLowerCase:
  case vmIntrinsics::_isUpperCase:
  case vmIntrinsics::_isWhitespace:
    return true;
  default:
    return false;
  }
}

bool vmIntrinsics::can_trap(vmIntrinsics::ID id) {
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");
  switch(id) {
#ifdef JFR_HAVE_INTRINSICS
  case vmIntrinsics::_counterTime:
  case vmIntrinsics::_getClassId:
#endif
  case vmIntrinsics::_currentTimeMillis:
  case vmIntrinsics::_nanoTime:
  case vmIntrinsics::_floatToRawIntBits:
  case vmIntrinsics::_intBitsToFloat:
  case vmIntrinsics::_doubleToRawLongBits:
  case vmIntrinsics::_longBitsToDouble:
  case vmIntrinsics::_currentThread:
  case vmIntrinsics::_dabs:
  case vmIntrinsics::_fabs:
  case vmIntrinsics::_iabs:
  case vmIntrinsics::_labs:
  case vmIntrinsics::_dsqrt:
  case vmIntrinsics::_dsin:
  case vmIntrinsics::_dcos:
  case vmIntrinsics::_dtan:
  case vmIntrinsics::_dlog:
  case vmIntrinsics::_dlog10:
  case vmIntrinsics::_dexp:
  case vmIntrinsics::_dpow:
  case vmIntrinsics::_updateCRC32:
  case vmIntrinsics::_updateBytesCRC32:
  case vmIntrinsics::_updateByteBufferCRC32:
  case vmIntrinsics::_vectorizedMismatch:
  case vmIntrinsics::_fmaD:
  case vmIntrinsics::_fmaF:
    return false;
  default:
    return true;
  }
}

// Some intrinsics produce different results if they are not pinned
bool vmIntrinsics::should_be_pinned(vmIntrinsics::ID id) {
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");
  switch(id) {
#ifdef JFR_HAVE_INTRINSICS
  case vmIntrinsics::_counterTime:
#endif
  case vmIntrinsics::_currentTimeMillis:
  case vmIntrinsics::_nanoTime:
  case vmIntrinsics::_blackhole:
    return true;
  default:
    return false;
  }
}

bool vmIntrinsics::does_virtual_dispatch(vmIntrinsics::ID id) {
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");
  switch(id) {
  case vmIntrinsics::_hashCode:
  case vmIntrinsics::_clone:
    return true;
    break;
  default:
    return false;
  }
}

int vmIntrinsics::predicates_needed(vmIntrinsics::ID id) {
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");
  switch (id) {
  case vmIntrinsics::_cipherBlockChaining_encryptAESCrypt:
  case vmIntrinsics::_cipherBlockChaining_decryptAESCrypt:
  case vmIntrinsics::_electronicCodeBook_encryptAESCrypt:
  case vmIntrinsics::_electronicCodeBook_decryptAESCrypt:
  case vmIntrinsics::_counterMode_AESCrypt:
    return 1;
  case vmIntrinsics::_digestBase_implCompressMB:
    return 5;
  default:
    return 0;
  }
}

bool vmIntrinsics::disabled_by_jvm_flags(vmIntrinsics::ID id) {
  assert(id != vmIntrinsics::_none, "must be a VM intrinsic");

  // -XX:-InlineNatives disables nearly all intrinsics except the ones listed in
  // the following switch statement.
  if (!InlineNatives) {
    switch (id) {
    case vmIntrinsics::_indexOfL:
    case vmIntrinsics::_indexOfU:
    case vmIntrinsics::_indexOfUL:
    case vmIntrinsics::_indexOfIL:
    case vmIntrinsics::_indexOfIU:
    case vmIntrinsics::_indexOfIUL:
    case vmIntrinsics::_indexOfU_char:
    case vmIntrinsics::_indexOfL_char:
    case vmIntrinsics::_compareToL:
    case vmIntrinsics::_compareToU:
    case vmIntrinsics::_compareToLU:
    case vmIntrinsics::_compareToUL:
    case vmIntrinsics::_equalsL:
    case vmIntrinsics::_equalsU:
    case vmIntrinsics::_equalsC:
    case vmIntrinsics::_getCharStringU:
    case vmIntrinsics::_putCharStringU:
    case vmIntrinsics::_compressStringC:
    case vmIntrinsics::_compressStringB:
    case vmIntrinsics::_inflateStringC:
    case vmIntrinsics::_inflateStringB:
    case vmIntrinsics::_getAndAddInt:
    case vmIntrinsics::_getAndAddLong:
    case vmIntrinsics::_getAndSetInt:
    case vmIntrinsics::_getAndSetLong:
    case vmIntrinsics::_getAndSetReference:
    case vmIntrinsics::_loadFence:
    case vmIntrinsics::_storeFence:
    case vmIntrinsics::_fullFence:
    case vmIntrinsics::_hasNegatives:
    case vmIntrinsics::_Reference_get:
      break;
    default:
      return true;
    }
  }

  switch (id) {
  case vmIntrinsics::_isInstance:
  case vmIntrinsics::_isAssignableFrom:
  case vmIntrinsics::_getModifiers:
  case vmIntrinsics::_isInterface:
  case vmIntrinsics::_isArray:
  case vmIntrinsics::_isPrimitive:
  case vmIntrinsics::_isHidden:
  case vmIntrinsics::_getSuperclass:
  case vmIntrinsics::_Class_cast:
  case vmIntrinsics::_getLength:
  case vmIntrinsics::_newArray:
  case vmIntrinsics::_getClass:
    if (!InlineClassNatives) return true;
    break;
  case vmIntrinsics::_currentThread:
    if (!InlineThreadNatives) return true;
    break;
  case vmIntrinsics::_floatToRawIntBits:
  case vmIntrinsics::_intBitsToFloat:
  case vmIntrinsics::_doubleToRawLongBits:
  case vmIntrinsics::_longBitsToDouble:
  case vmIntrinsics::_ceil:
  case vmIntrinsics::_floor:
  case vmIntrinsics::_rint:
  case vmIntrinsics::_dabs:
  case vmIntrinsics::_fabs:
  case vmIntrinsics::_iabs:
  case vmIntrinsics::_labs:
  case vmIntrinsics::_dsqrt:
  case vmIntrinsics::_dsin:
  case vmIntrinsics::_dcos:
  case vmIntrinsics::_dtan:
  case vmIntrinsics::_dlog:
  case vmIntrinsics::_dexp:
  case vmIntrinsics::_dpow:
  case vmIntrinsics::_dlog10:
  case vmIntrinsics::_datan2:
  case vmIntrinsics::_min:
  case vmIntrinsics::_max:
  case vmIntrinsics::_floatToIntBits:
  case vmIntrinsics::_doubleToLongBits:
  case vmIntrinsics::_maxF:
  case vmIntrinsics::_minF:
  case vmIntrinsics::_maxD:
  case vmIntrinsics::_minD:
    if (!InlineMathNatives) return true;
    break;
  case vmIntrinsics::_fmaD:
  case vmIntrinsics::_fmaF:
    if (!InlineMathNatives || !UseFMA) return true;
    break;
  case vmIntrinsics::_arraycopy:
    if (!InlineArrayCopy) return true;
    break;
  case vmIntrinsics::_updateCRC32:
  case vmIntrinsics::_updateBytesCRC32:
  case vmIntrinsics::_updateByteBufferCRC32:
    if (!UseCRC32Intrinsics) return true;
    break;
  case vmIntrinsics::_getReference:
  case vmIntrinsics::_getBoolean:
  case vmIntrinsics::_getByte:
  case vmIntrinsics::_getShort:
  case vmIntrinsics::_getChar:
  case vmIntrinsics::_getInt:
  case vmIntrinsics::_getLong:
  case vmIntrinsics::_getFloat:
  case vmIntrinsics::_getDouble:
  case vmIntrinsics::_putReference:
  case vmIntrinsics::_putBoolean:
  case vmIntrinsics::_putByte:
  case vmIntrinsics::_putShort:
  case vmIntrinsics::_putChar:
  case vmIntrinsics::_putInt:
  case vmIntrinsics::_putLong:
  case vmIntrinsics::_putFloat:
  case vmIntrinsics::_putDouble:
  case vmIntrinsics::_getReferenceVolatile:
  case vmIntrinsics::_getBooleanVolatile:
  case vmIntrinsics::_getByteVolatile:
  case vmIntrinsics::_getShortVolatile:
  case vmIntrinsics::_getCharVolatile:
  case vmIntrinsics::_getIntVolatile:
  case vmIntrinsics::_getLongVolatile:
  case vmIntrinsics::_getFloatVolatile:
  case vmIntrinsics::_getDoubleVolatile:
  case vmIntrinsics::_putReferenceVolatile:
  case vmIntrinsics::_putBooleanVolatile:
  case vmIntrinsics::_putByteVolatile:
  case vmIntrinsics::_putShortVolatile:
  case vmIntrinsics::_putCharVolatile:
  case vmIntrinsics::_putIntVolatile:
  case vmIntrinsics::_putLongVolatile:
  case vmIntrinsics::_putFloatVolatile:
  case vmIntrinsics::_putDoubleVolatile:
  case vmIntrinsics::_getReferenceAcquire:
  case vmIntrinsics::_getBooleanAcquire:
  case vmIntrinsics::_getByteAcquire:
  case vmIntrinsics::_getShortAcquire:
  case vmIntrinsics::_getCharAcquire:
  case vmIntrinsics::_getIntAcquire:
  case vmIntrinsics::_getLongAcquire:
  case vmIntrinsics::_getFloatAcquire:
  case vmIntrinsics::_getDoubleAcquire:
  case vmIntrinsics::_putReferenceRelease:
  case vmIntrinsics::_putBooleanRelease:
  case vmIntrinsics::_putByteRelease:
  case vmIntrinsics::_putShortRelease:
  case vmIntrinsics::_putCharRelease:
  case vmIntrinsics::_putIntRelease:
  case vmIntrinsics::_putLongRelease:
  case vmIntrinsics::_putFloatRelease:
  case vmIntrinsics::_putDoubleRelease:
  case vmIntrinsics::_getReferenceOpaque:
  case vmIntrinsics::_getBooleanOpaque:
  case vmIntrinsics::_getByteOpaque:
  case vmIntrinsics::_getShortOpaque:
  case vmIntrinsics::_getCharOpaque:
  case vmIntrinsics::_getIntOpaque:
  case vmIntrinsics::_getLongOpaque:
  case vmIntrinsics::_getFloatOpaque:
  case vmIntrinsics::_getDoubleOpaque:
  case vmIntrinsics::_putReferenceOpaque:
  case vmIntrinsics::_putBooleanOpaque:
  case vmIntrinsics::_putByteOpaque:
  case vmIntrinsics::_putShortOpaque:
  case vmIntrinsics::_putCharOpaque:
  case vmIntrinsics::_putIntOpaque:
  case vmIntrinsics::_putLongOpaque:
  case vmIntrinsics::_putFloatOpaque:
  case vmIntrinsics::_putDoubleOpaque:
  case vmIntrinsics::_getAndAddInt:
  case vmIntrinsics::_getAndAddLong:
  case vmIntrinsics::_getAndSetInt:
  case vmIntrinsics::_getAndSetLong:
  case vmIntrinsics::_getAndSetReference:
  case vmIntrinsics::_loadFence:
  case vmIntrinsics::_storeFence:
  case vmIntrinsics::_fullFence:
  case vmIntrinsics::_compareAndSetLong:
  case vmIntrinsics::_weakCompareAndSetLong:
  case vmIntrinsics::_weakCompareAndSetLongPlain:
  case vmIntrinsics::_weakCompareAndSetLongAcquire:
  case vmIntrinsics::_weakCompareAndSetLongRelease:
  case vmIntrinsics::_compareAndSetInt:
  case vmIntrinsics::_weakCompareAndSetInt:
  case vmIntrinsics::_weakCompareAndSetIntPlain:
  case vmIntrinsics::_weakCompareAndSetIntAcquire:
  case vmIntrinsics::_weakCompareAndSetIntRelease:
  case vmIntrinsics::_compareAndSetReference:
  case vmIntrinsics::_weakCompareAndSetReference:
  case vmIntrinsics::_weakCompareAndSetReferencePlain:
  case vmIntrinsics::_weakCompareAndSetReferenceAcquire:
  case vmIntrinsics::_weakCompareAndSetReferenceRelease:
  case vmIntrinsics::_compareAndExchangeInt:
  case vmIntrinsics::_compareAndExchangeIntAcquire:
  case vmIntrinsics::_compareAndExchangeIntRelease:
  case vmIntrinsics::_compareAndExchangeLong:
  case vmIntrinsics::_compareAndExchangeLongAcquire:
  case vmIntrinsics::_compareAndExchangeLongRelease:
  case vmIntrinsics::_compareAndExchangeReference:
  case vmIntrinsics::_compareAndExchangeReferenceAcquire:
  case vmIntrinsics::_compareAndExchangeReferenceRelease:
    if (!InlineUnsafeOps) return true;
    break;
  case vmIntrinsics::_getShortUnaligned:
  case vmIntrinsics::_getCharUnaligned:
  case vmIntrinsics::_getIntUnaligned:
  case vmIntrinsics::_getLongUnaligned:
  case vmIntrinsics::_putShortUnaligned:
  case vmIntrinsics::_putCharUnaligned:
  case vmIntrinsics::_putIntUnaligned:
  case vmIntrinsics::_putLongUnaligned:
  case vmIntrinsics::_allocateInstance:
    if (!InlineUnsafeOps || !UseUnalignedAccesses) return true;
    break;
  case vmIntrinsics::_hashCode:
    if (!InlineObjectHash) return true;
    break;
  case vmIntrinsics::_aescrypt_encryptBlock:
  case vmIntrinsics::_aescrypt_decryptBlock:
    if (!UseAESIntrinsics) return true;
    break;
  case vmIntrinsics::_cipherBlockChaining_encryptAESCrypt:
  case vmIntrinsics::_cipherBlockChaining_decryptAESCrypt:
    if (!UseAESIntrinsics) return true;
    break;
  case vmIntrinsics::_electronicCodeBook_encryptAESCrypt:
  case vmIntrinsics::_electronicCodeBook_decryptAESCrypt:
    if (!UseAESIntrinsics) return true;
    break;
  case vmIntrinsics::_counterMode_AESCrypt:
    if (!UseAESCTRIntrinsics) return true;
    break;
  case vmIntrinsics::_md5_implCompress:
    if (!UseMD5Intrinsics) return true;
    break;
  case vmIntrinsics::_sha_implCompress:
    if (!UseSHA1Intrinsics) return true;
    break;
  case vmIntrinsics::_sha2_implCompress:
    if (!UseSHA256Intrinsics) return true;
    break;
  case vmIntrinsics::_sha5_implCompress:
    if (!UseSHA512Intrinsics) return true;
    break;
  case vmIntrinsics::_sha3_implCompress:
    if (!UseSHA3Intrinsics) return true;
    break;
  case vmIntrinsics::_digestBase_implCompressMB:
    if (!(UseMD5Intrinsics || UseSHA1Intrinsics || UseSHA256Intrinsics || UseSHA512Intrinsics || UseSHA3Intrinsics)) return true;
    break;
  case vmIntrinsics::_ghash_processBlocks:
    if (!UseGHASHIntrinsics) return true;
    break;
  case vmIntrinsics::_base64_encodeBlock:
  case vmIntrinsics::_base64_decodeBlock:
    if (!UseBASE64Intrinsics) return true;
    break;
  case vmIntrinsics::_updateBytesCRC32C:
  case vmIntrinsics::_updateDirectByteBufferCRC32C:
    if (!UseCRC32CIntrinsics) return true;
    break;
  case vmIntrinsics::_vectorizedMismatch:
    if (!UseVectorizedMismatchIntrinsic) return true;
    break;
  case vmIntrinsics::_updateBytesAdler32:
  case vmIntrinsics::_updateByteBufferAdler32:
    if (!UseAdler32Intrinsics) return true;
    break;
  case vmIntrinsics::_copyMemory:
    if (!InlineArrayCopy || !InlineUnsafeOps) return true;
    break;
#ifdef COMPILER2
  case vmIntrinsics::_clone:
  case vmIntrinsics::_copyOf:
  case vmIntrinsics::_copyOfRange:
    // These intrinsics use both the objectcopy and the arraycopy
    // intrinsic mechanism.
    if (!InlineObjectCopy || !InlineArrayCopy) return true;
    break;
  case vmIntrinsics::_compareToL:
  case vmIntrinsics::_compareToU:
  case vmIntrinsics::_compareToLU:
  case vmIntrinsics::_compareToUL:
    if (!SpecialStringCompareTo) return true;
    break;
  case vmIntrinsics::_indexOfL:
  case vmIntrinsics::_indexOfU:
  case vmIntrinsics::_indexOfUL:
  case vmIntrinsics::_indexOfIL:
  case vmIntrinsics::_indexOfIU:
  case vmIntrinsics::_indexOfIUL:
  case vmIntrinsics::_indexOfU_char:
  case vmIntrinsics::_indexOfL_char:
    if (!SpecialStringIndexOf) return true;
    break;
  case vmIntrinsics::_equalsL:
  case vmIntrinsics::_equalsU:
    if (!SpecialStringEquals) return true;
    break;
  case vmIntrinsics::_equalsB:
  case vmIntrinsics::_equalsC:
    if (!SpecialArraysEquals) return true;
    break;
  case vmIntrinsics::_encodeISOArray:
  case vmIntrinsics::_encodeByteISOArray:
    if (!SpecialEncodeISOArray) return true;
    break;
  case vmIntrinsics::_getCallerClass:
    if (!InlineReflectionGetCallerClass) return true;
    break;
  case vmIntrinsics::_multiplyToLen:
    if (!UseMultiplyToLenIntrinsic) return true;
    break;
  case vmIntrinsics::_squareToLen:
    if (!UseSquareToLenIntrinsic) return true;
    break;
  case vmIntrinsics::_mulAdd:
    if (!UseMulAddIntrinsic) return true;
    break;
  case vmIntrinsics::_montgomeryMultiply:
    if (!UseMontgomeryMultiplyIntrinsic) return true;
    break;
  case vmIntrinsics::_montgomerySquare:
    if (!UseMontgomerySquareIntrinsic) return true;
    break;
  case vmIntrinsics::_bigIntegerRightShiftWorker:
  case vmIntrinsics::_bigIntegerLeftShiftWorker:
    break;
  case vmIntrinsics::_addExactI:
  case vmIntrinsics::_addExactL:
  case vmIntrinsics::_decrementExactI:
  case vmIntrinsics::_decrementExactL:
  case vmIntrinsics::_incrementExactI:
  case vmIntrinsics::_incrementExactL:
  case vmIntrinsics::_multiplyExactI:
  case vmIntrinsics::_multiplyExactL:
  case vmIntrinsics::_negateExactI:
  case vmIntrinsics::_negateExactL:
  case vmIntrinsics::_subtractExactI:
  case vmIntrinsics::_subtractExactL:
    if (!UseMathExactIntrinsics || !InlineMathNatives) return true;
    break;
  case vmIntrinsics::_isDigit:
  case vmIntrinsics::_isLowerCase:
  case vmIntrinsics::_isUpperCase:
  case vmIntrinsics::_isWhitespace:
    if (!UseCharacterCompareIntrinsics) return true;
    break;
  case vmIntrinsics::_dcopySign:
  case vmIntrinsics::_fcopySign:
    if (!InlineMathNatives || !UseCopySignIntrinsic) return true;
    break;
  case vmIntrinsics::_dsignum:
  case vmIntrinsics::_fsignum:
    if (!InlineMathNatives || !UseSignumIntrinsic) return true;
    break;
#endif // COMPILER2
  default:
    return false;
  }

  return false;
}

#define VM_INTRINSIC_INITIALIZE(id, klass, name, sig, flags) #id "\0"
static const char* vm_intrinsic_name_bodies =
  VM_INTRINSICS_DO(VM_INTRINSIC_INITIALIZE,
                   VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_ALIAS_IGNORE);

static const char* vm_intrinsic_name_table[vmIntrinsics::number_of_intrinsics()];
static TriBoolArray<(size_t)vmIntrinsics::number_of_intrinsics(), int> vm_intrinsic_control_words;

void vmIntrinsics::init_vm_intrinsic_name_table() {
  const char** nt = &vm_intrinsic_name_table[0];
  char* string = (char*) &vm_intrinsic_name_bodies[0];

  for (auto index : EnumRange<vmIntrinsicID>{}) {
    nt[as_int(index)] = string;
    string += strlen(string); // skip string body
    string += 1;              // skip trailing null
  }
  assert(!strcmp(nt[as_int(vmIntrinsics::_hashCode)], "_hashCode"), "lined up");
  nt[as_int(vmIntrinsics::_none)] = "_none";
}

const char* vmIntrinsics::name_at(vmIntrinsics::ID id) {
  const char** nt = &vm_intrinsic_name_table[0];
  if (nt[as_int(_none)] == NULL) {
    init_vm_intrinsic_name_table();
  }

  if (id < ID_LIMIT)
    return vm_intrinsic_name_table[as_int(id)];
  else
    return "(unknown intrinsic)";
}

vmIntrinsics::ID vmIntrinsics::find_id(const char* name) {
  const char** nt = &vm_intrinsic_name_table[0];
  if (nt[as_int(_none)] == NULL) {
    init_vm_intrinsic_name_table();
  }

  for (auto index : EnumRange<vmIntrinsicID>{}) {
    if (0 == strcmp(name, nt[as_int(index)])) {
      return index;
    }
  }

  return _none;
}

bool vmIntrinsics::is_disabled_by_flags(const methodHandle& method) {
  vmIntrinsics::ID id = method->intrinsic_id();
  return is_disabled_by_flags(id);
}

bool vmIntrinsics::is_disabled_by_flags(vmIntrinsics::ID id) {
  assert(id > _none && id < ID_LIMIT, "must be a VM intrinsic");

  // not initialized yet, process Control/DisableIntrinsic
  if (vm_intrinsic_control_words[as_int(_none)].is_default()) {
    for (ControlIntrinsicIter iter(ControlIntrinsic); *iter != NULL; ++iter) {
      vmIntrinsics::ID id = vmIntrinsics::find_id(*iter);

      if (id != vmIntrinsics::_none) {
        vm_intrinsic_control_words[as_int(id)] = iter.is_enabled() && !disabled_by_jvm_flags(id);
      }
    }

    // Order matters, DisableIntrinsic can overwrite ControlIntrinsic
    for (ControlIntrinsicIter iter(DisableIntrinsic, true/*disable_all*/); *iter != NULL; ++iter) {
      vmIntrinsics::ID id = vmIntrinsics::find_id(*iter);

      if (id != vmIntrinsics::_none) {
        vm_intrinsic_control_words[as_int(id)] = false;
      }
    }

    vm_intrinsic_control_words[as_int(_none)] = true;
  }

  TriBool b = vm_intrinsic_control_words[as_int(id)];
  if (b.is_default()) {
    // unknown yet, query and cache it
    b = vm_intrinsic_control_words[as_int(id)] = !disabled_by_jvm_flags(id);
  }

  return !b;
}

// These are for forming case labels:
#define ID3(x, y, z) (( jlong)(z) +                                  \
                      ((jlong)(y) <<    vmSymbols::log2_SID_LIMIT) + \
                      ((jlong)(x) << (2*vmSymbols::log2_SID_LIMIT))  )
#define SID_ENUM(n) VM_SYMBOL_ENUM_NAME(n)

vmIntrinsics::ID vmIntrinsics::find_id_impl(vmSymbolID holder,
                                            vmSymbolID name,
                                            vmSymbolID sig,
                                            jshort flags) {
  assert((int)vmSymbolID::SID_LIMIT <= (1<<vmSymbols::log2_SID_LIMIT), "must fit");

  // Let the C compiler build the decision tree.

#define VM_INTRINSIC_CASE(id, klass, name, sig, fcode) \
  case ID3(SID_ENUM(klass), SID_ENUM(name), SID_ENUM(sig)): \
    if (!match_##fcode(flags))  break; \
    return id;

  switch (ID3(holder, name, sig)) {
    VM_INTRINSICS_DO(VM_INTRINSIC_CASE,
                     VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_ALIAS_IGNORE);
  }
  return vmIntrinsics::_none;

#undef VM_INTRINSIC_CASE
}

class vmIntrinsicsLookup {
  bool _class_map[vmSymbols::number_of_symbols()];

  constexpr int as_index(vmSymbolID id) const {
    int index = vmSymbols::as_int(id);
    assert(0 <= index && index < int(sizeof(_class_map)), "must be");
    return index;
  }

  constexpr void set_class_map(vmSymbolID id) {
    _class_map[as_index(id)] = true;
  }

public:
  constexpr vmIntrinsicsLookup() : _class_map() {

#define VM_INTRINSIC_CLASS_MAP(id, klass, name, sig, fcode) \
    set_class_map(SID_ENUM(klass));

    VM_INTRINSICS_DO(VM_INTRINSIC_CLASS_MAP,
                     VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_ALIAS_IGNORE);
#undef VM_INTRINSIC_CLASS_MAP


    // A few slightly irregular cases. See Method::init_intrinsic_id
    set_class_map(SID_ENUM(java_lang_StrictMath));
    set_class_map(SID_ENUM(java_lang_invoke_MethodHandle));
    set_class_map(SID_ENUM(java_lang_invoke_VarHandle));
  }

  bool class_has_intrinsics(vmSymbolID holder) const {
    return _class_map[as_index(holder)];
  }
};

constexpr vmIntrinsicsLookup _intrinsics_lookup;

bool vmIntrinsics::class_has_intrinsics(vmSymbolID holder) {
  return _intrinsics_lookup.class_has_intrinsics(holder);
}

const char* vmIntrinsics::short_name_as_C_string(vmIntrinsics::ID id, char* buf, int buflen) {
  const char* str = name_at(id);
#ifndef PRODUCT
  const char* kname = vmSymbols::name_for(class_for(id));
  const char* mname = vmSymbols::name_for(name_for(id));
  const char* sname = vmSymbols::name_for(signature_for(id));
  const char* fname = "";
  switch (flags_for(id)) {
  case F_Y:  fname = "synchronized ";  break;
  case F_RN: fname = "native ";        break;
  case F_SN: fname = "native static "; break;
  case F_S:  fname = "static ";        break;
  default:   break;
  }
  const char* kptr = strrchr(kname, JVM_SIGNATURE_SLASH);
  if (kptr != NULL)  kname = kptr + 1;
  int len = jio_snprintf(buf, buflen, "%s: %s%s.%s%s",
                         str, fname, kname, mname, sname);
  if (len < buflen)
    str = buf;
#endif //PRODUCT
  return str;
}


// These are to get information about intrinsics.

#define ID4(x, y, z, f) ((ID3(x, y, z) << vmIntrinsics::log2_FLAG_LIMIT) | (jlong) (f))

#ifndef PRODUCT
static const jlong intrinsic_info_array[vmIntrinsics::number_of_intrinsics()+1] = {
#define VM_INTRINSIC_INFO(ignore_id, klass, name, sig, fcode) \
  ID4(SID_ENUM(klass), SID_ENUM(name), SID_ENUM(sig), vmIntrinsics::fcode),

  0, VM_INTRINSICS_DO(VM_INTRINSIC_INFO,
                     VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_SYMBOL_IGNORE, VM_ALIAS_IGNORE)
  0
#undef VM_INTRINSIC_INFO
};

inline jlong intrinsic_info(vmIntrinsics::ID id) {
  return intrinsic_info_array[vmIntrinsics::as_int(id)];
}

vmSymbolID vmIntrinsics::class_for(vmIntrinsics::ID id) {
  jlong info = intrinsic_info(id);
  int shift = 2*vmSymbols::log2_SID_LIMIT + log2_FLAG_LIMIT, mask = right_n_bits(vmSymbols::log2_SID_LIMIT);
  assert(((ID4(1021,1022,1023,7) >> shift) & mask) == 1021, "");
  return vmSymbols::as_SID( (info >> shift) & mask );
}

vmSymbolID vmIntrinsics::name_for(vmIntrinsics::ID id) {
  jlong info = intrinsic_info(id);
  int shift = vmSymbols::log2_SID_LIMIT + log2_FLAG_LIMIT, mask = right_n_bits(vmSymbols::log2_SID_LIMIT);
  assert(((ID4(1021,1022,1023,7) >> shift) & mask) == 1022, "");
  return vmSymbols::as_SID( (info >> shift) & mask );
}

vmSymbolID vmIntrinsics::signature_for(vmIntrinsics::ID id) {
  jlong info = intrinsic_info(id);
  int shift = log2_FLAG_LIMIT, mask = right_n_bits(vmSymbols::log2_SID_LIMIT);
  assert(((ID4(1021,1022,1023,7) >> shift) & mask) == 1023, "");
  return vmSymbols::as_SID( (info >> shift) & mask );
}

vmIntrinsics::Flags vmIntrinsics::flags_for(vmIntrinsics::ID id) {
  jlong info = intrinsic_info(id);
  int shift = 0, mask = right_n_bits(log2_FLAG_LIMIT);
  assert(((ID4(1021,1022,1023,7) >> shift) & mask) == 7, "");
  return Flags( (info >> shift) & mask );
}
#endif // !PRODUCT
