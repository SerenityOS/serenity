/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2020, 2021, NTT DATA.
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

#include <jni.h>

#include "dwarf.hpp"
#include "libproc.h"

#define CHECK_EXCEPTION if (env->ExceptionOccurred()) { return; }

static jfieldID p_dwarf_context_ID = 0;
static jint sa_RAX = -1;
static jint sa_RDX = -1;
static jint sa_RCX = -1;
static jint sa_RBX = -1;
static jint sa_RSI = -1;
static jint sa_RDI = -1;
static jint sa_RBP = -1;
static jint sa_RSP = -1;
static jint sa_R8  = -1;
static jint sa_R9  = -1;
static jint sa_R10 = -1;
static jint sa_R11 = -1;
static jint sa_R12 = -1;
static jint sa_R13 = -1;
static jint sa_R14 = -1;
static jint sa_R15 = -1;

static jlong get_dwarf_context(JNIEnv *env, jobject obj) {
  return env->GetLongField(obj, p_dwarf_context_ID);
}

#define SET_REG(env, reg, reg_cls) \
  jfieldID reg##_ID = env->GetStaticFieldID(reg_cls, #reg, "I"); \
  CHECK_EXCEPTION \
  sa_##reg = env->GetStaticIntField(reg_cls, reg##_ID); \
  CHECK_EXCEPTION

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    init0
 * Signature: ()V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_init0
  (JNIEnv *env, jclass this_cls) {
  jclass cls = env->FindClass("sun/jvm/hotspot/debugger/linux/amd64/DwarfParser");
  CHECK_EXCEPTION
  p_dwarf_context_ID = env->GetFieldID(cls, "p_dwarf_context", "J");
  CHECK_EXCEPTION

  jclass reg_cls = env->FindClass("sun/jvm/hotspot/debugger/amd64/AMD64ThreadContext");
  CHECK_EXCEPTION
  SET_REG(env, RAX, reg_cls);
  SET_REG(env, RDX, reg_cls);
  SET_REG(env, RCX, reg_cls);
  SET_REG(env, RBX, reg_cls);
  SET_REG(env, RSI, reg_cls);
  SET_REG(env, RDI, reg_cls);
  SET_REG(env, RBP, reg_cls);
  SET_REG(env, RSP, reg_cls);
  SET_REG(env, R8,  reg_cls);
  SET_REG(env, R9,  reg_cls);
  SET_REG(env, R10, reg_cls);
  SET_REG(env, R11, reg_cls);
  SET_REG(env, R12, reg_cls);
  SET_REG(env, R13, reg_cls);
  SET_REG(env, R14, reg_cls);
  SET_REG(env, R15, reg_cls);
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    createDwarfContext
 * Signature: (J)J
 */
extern "C"
JNIEXPORT jlong JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_createDwarfContext
  (JNIEnv *env, jclass this_cls, jlong lib) {
  DwarfParser *parser = new DwarfParser(reinterpret_cast<lib_info *>(lib));
  if (!parser->is_parseable()) {
    jclass ex_cls = env->FindClass("sun/jvm/hotspot/debugger/DebuggerException");
    if (!env->ExceptionOccurred()) {
        env->ThrowNew(ex_cls, "DWARF not found");
    }
    delete parser;
    return 0L;
  }

  return reinterpret_cast<jlong>(parser);
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    destroyDwarfContext
 * Signature: (J)V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_destroyDwarfContext
  (JNIEnv *env, jclass this_cls, jlong context) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(context);
  delete parser;
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    isIn0
 * Signature: (J)Z
 */
extern "C"
JNIEXPORT jboolean JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_isIn0
  (JNIEnv *env, jobject this_obj, jlong pc) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  return static_cast<jboolean>(parser->is_in(pc));
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    processDwarf0
 * Signature: (J)V
 */
extern "C"
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_processDwarf0
  (JNIEnv *env, jobject this_obj, jlong pc) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  if (!parser->process_dwarf(pc)) {
    jclass ex_cls = env->FindClass("sun/jvm/hotspot/debugger/DebuggerException");
    if (!env->ExceptionOccurred()) {
        env->ThrowNew(ex_cls, "Could not find PC in DWARF");
    }
    return;
  }
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    getCFARegister
 * Signature: ()I
 */
extern "C"
JNIEXPORT jint JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_getCFARegister
  (JNIEnv *env, jobject this_obj) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  switch (parser->get_cfa_register()) {
    case RAX: return sa_RAX;
    case RDX: return sa_RDX;
    case RCX: return sa_RCX;
    case RBX: return sa_RBX;
    case RSI: return sa_RSI;
    case RDI: return sa_RDI;
    case RBP: return sa_RBP;
    case RSP: return sa_RSP;
    case R8:  return sa_R8;
    case R9:  return sa_R9;
    case R10: return sa_R10;
    case R11: return sa_R11;
    case R12: return sa_R12;
    case R13: return sa_R13;
    case R14: return sa_R14;
    case R15: return sa_R15;
    default:  return -1;
  }
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    getCFAOffset
 * Signature: ()I
 */
extern "C"
JNIEXPORT jint JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_getCFAOffset
  (JNIEnv *env, jobject this_obj) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  return parser->get_cfa_offset();
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    getReturnAddressOffsetFromCFA
 * Signature: ()I
 */
extern "C"
JNIEXPORT jint JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_getReturnAddressOffsetFromCFA
  (JNIEnv *env, jobject this_obj) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  return parser->get_ra_cfa_offset();
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    getBasePointerOffsetFromCFA
 * Signature: ()I
 */
extern "C"
JNIEXPORT jint JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_getBasePointerOffsetFromCFA
  (JNIEnv *env, jobject this_obj) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  return parser->get_bp_cfa_offset();
}

/*
 * Class:     sun_jvm_hotspot_debugger_linux_amd64_DwarfParser
 * Method:    isBPOffsetAvailable
 * Signature: ()Z
 */
extern "C"
JNIEXPORT jboolean JNICALL Java_sun_jvm_hotspot_debugger_linux_amd64_DwarfParser_isBPOffsetAvailable
  (JNIEnv *env, jobject this_obj) {
  DwarfParser *parser = reinterpret_cast<DwarfParser *>(get_dwarf_context(env, this_obj));
  return parser->is_bp_offset_available();
}

