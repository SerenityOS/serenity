/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
#include "jni.h"
#include "jvm.h"
#include "classfile/vmSymbols.hpp"
#include "memory/allocation.inline.hpp"
#include "memory/resourceArea.hpp"
#include "oops/oop.inline.hpp"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/perfData.inline.hpp"
#include "runtime/perfMemory.hpp"

/*
 *      Implementation of class jdk.internal.perf.Perf
 */


#define PERF_ENTRY(result_type, header) \
  JVM_ENTRY(result_type, header)

#define PERF_END JVM_END

#define PerfWrapper(arg) /* Unimplemented at this time */

static char* jstr_to_utf(JNIEnv *env, jstring str, TRAPS) {

  char* utfstr = NULL;

  if (str == NULL) {
    THROW_0(vmSymbols::java_lang_NullPointerException());
    //throw_new(env,"NullPointerException");
  }

  int len = env->GetStringUTFLength(str);
  int unicode_len = env->GetStringLength(str);

  utfstr = NEW_RESOURCE_ARRAY(char, len + 1);

  env->GetStringUTFRegion(str, 0, unicode_len, utfstr);

  return utfstr;
}

PERF_ENTRY(jobject, Perf_Attach(JNIEnv *env, jobject unused, jstring user, int vmid, int mode))

  PerfWrapper("Perf_Attach");

  char* address = 0;
  size_t capacity = 0;
  const char* user_utf = NULL;

  ResourceMark rm;

  {
    ThreadToNativeFromVM ttnfv(thread);

    user_utf = user == NULL ? NULL : jstr_to_utf(env, user, CHECK_NULL);
  }

  if (mode != PerfMemory::PERF_MODE_RO &&
      mode != PerfMemory::PERF_MODE_RW) {
    THROW_0(vmSymbols::java_lang_IllegalArgumentException());
  }

  // attach to the PerfData memory region for the specified VM
  PerfMemory::attach(user_utf, vmid, (PerfMemory::PerfMemoryMode) mode,
                     &address, &capacity, CHECK_NULL);

  {
    ThreadToNativeFromVM ttnfv(thread);
    return env->NewDirectByteBuffer(address, (jlong)capacity);
  }

PERF_END

PERF_ENTRY(void, Perf_Detach(JNIEnv *env, jobject unused, jobject buffer))

  PerfWrapper("Perf_Detach");

  if (!UsePerfData) {
    // With -XX:-UsePerfData, detach is just a NOP
    return;
  }

  void* address = 0;
  jlong capacity = 0;

  // get buffer address and capacity
  {
   ThreadToNativeFromVM ttnfv(thread);
   address = env->GetDirectBufferAddress(buffer);
   capacity = env->GetDirectBufferCapacity(buffer);
  }

  PerfMemory::detach((char*)address, capacity);

PERF_END

PERF_ENTRY(jobject, Perf_CreateLong(JNIEnv *env, jobject perf, jstring name,
           int variability, int units, jlong value))

  PerfWrapper("Perf_CreateLong");

  char* name_utf = NULL;

  if (units <= 0 || units > PerfData::U_Last) {
    debug_only(warning("unexpected units argument, units = %d", units));
    THROW_0(vmSymbols::java_lang_IllegalArgumentException());
  }

  ResourceMark rm;

  {
    ThreadToNativeFromVM ttnfv(thread);

    name_utf = jstr_to_utf(env, name, CHECK_NULL);
  }

  PerfLong* pl = NULL;

  // check that the PerfData name doesn't already exist
  if (PerfDataManager::exists(name_utf)) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "PerfLong name already exists");
  }

  switch(variability) {
  case PerfData::V_Constant:
    pl = PerfDataManager::create_long_constant(NULL_NS, (char *)name_utf,
                                               (PerfData::Units)units, value,
                                               CHECK_NULL);
    break;

  case PerfData::V_Monotonic:
    pl = PerfDataManager::create_long_counter(NULL_NS, (char *)name_utf,
                                               (PerfData::Units)units, value,
                                               CHECK_NULL);
    break;

  case PerfData::V_Variable:
    pl = PerfDataManager::create_long_variable(NULL_NS, (char *)name_utf,
                                              (PerfData::Units)units, value,
                                              CHECK_NULL);
    break;

  default: /* Illegal Argument */
    debug_only(warning("unexpected variability value: %d", variability));
    THROW_0(vmSymbols::java_lang_IllegalArgumentException());
    break;
  }

  long* lp = (long*)pl->get_address();

  {
    ThreadToNativeFromVM ttnfv(thread);
    return env->NewDirectByteBuffer(lp, sizeof(jlong));
  }

PERF_END

PERF_ENTRY(jobject, Perf_CreateByteArray(JNIEnv *env, jobject perf,
                                         jstring name, jint variability,
                                         jint units, jbyteArray value,
                                         jint maxlength))

  PerfWrapper("Perf_CreateByteArray");

  // check for valid byte array objects
  if (name == NULL || value == NULL) {
    THROW_0(vmSymbols::java_lang_NullPointerException());
  }

  // check for valid variability classification
  if (variability != PerfData::V_Constant &&
      variability != PerfData::V_Variable) {
    debug_only(warning("unexpected variability value: %d", variability));
    THROW_0(vmSymbols::java_lang_IllegalArgumentException());
  }

  // check for valid units
  if (units != PerfData::U_String) {
    // only String based ByteArray objects are currently supported
    debug_only(warning("unexpected units value: %d", variability));
    THROW_0(vmSymbols::java_lang_IllegalArgumentException());
  }

  int value_length;
  char* name_utf = NULL;
  jbyte* value_local = NULL;

  ResourceMark rm;

  {
    ThreadToNativeFromVM ttnfv(thread);

    name_utf = jstr_to_utf(env, name, CHECK_NULL);

    value_length = env->GetArrayLength(value);

    value_local = NEW_RESOURCE_ARRAY(jbyte, value_length + 1);

    env->GetByteArrayRegion(value, 0, value_length, value_local);
  }

  // check that the counter name doesn't already exist
  if (PerfDataManager::exists((char*)name_utf)) {
    THROW_MSG_0(vmSymbols::java_lang_IllegalArgumentException(), "PerfByteArray name already exists");
  }

  PerfByteArray* pbv = NULL;

  if (units == PerfData::U_String) {

    if (variability == PerfData::V_Constant) {
      // create the string constant
      pbv = PerfDataManager::create_string_constant(NULL_NS, (char*)name_utf,
                                                    (char*)value_local,
                                                    CHECK_NULL);

      assert(maxlength == value_length, "string constant length should be == maxlength");
      maxlength = value_length;
    }
    else {

      // create the string variable
      pbv = PerfDataManager::create_string_variable(NULL_NS, (char*)name_utf,
                                                    maxlength,
                                                    (char*)value_local,
                                                    CHECK_NULL);

     assert(maxlength >= value_length,"string variable length should be <= maxlength");
    }
  }

  char* cp = (char*)pbv->get_address();

  {
    ThreadToNativeFromVM ttnfv(thread);
    return env->NewDirectByteBuffer(cp, maxlength+1);
  }

PERF_END

PERF_ENTRY(jlong, Perf_HighResCounter(JNIEnv *env, jobject perf))

  PerfWrapper("Perf_HighResCounter");

  // this should be a method in java.lang.System. This value could
  // be acquired through access to a PerfData performance counter, but
  // doing so would require that the PerfData monitoring overhead be
  // incurred by all Java applications, which is unacceptable.

  return os::elapsed_counter();

PERF_END

PERF_ENTRY(jlong, Perf_HighResFrequency(JNIEnv *env, jobject perf))

  PerfWrapper("Perf_HighResFrequency");

  // this should be a method in java.lang.System. This value could
  // be acquired through access to a PerfData performance counter, but
  // doing so would require that the PerfData monitoring overhead be
  // incurred by all Java applications, which is unacceptable.

  return os::elapsed_frequency();

PERF_END

/// JVM_RegisterPerfMethods

#define CC (char*)  /*cast a literal from (const char*)*/
#define FN_PTR(f) CAST_FROM_FN_PTR(void*, &f)
#define BB "Ljava/nio/ByteBuffer;"
#define JLS "Ljava/lang/String;"
#define CL_ARGS     CC "(" JLS "IIJ)" BB
#define CBA_ARGS    CC "(" JLS "II[BI)" BB

static JNINativeMethod perfmethods[] = {

  {CC "attach",              CC "(" JLS "II)" BB, FN_PTR(Perf_Attach)},
  {CC "detach",              CC "(" BB ")V",      FN_PTR(Perf_Detach)},
  {CC "createLong",          CL_ARGS,             FN_PTR(Perf_CreateLong)},
  {CC "createByteArray",     CBA_ARGS,            FN_PTR(Perf_CreateByteArray)},
  {CC "highResCounter",      CC "()J",            FN_PTR(Perf_HighResCounter)},
  {CC "highResFrequency",    CC "()J",            FN_PTR(Perf_HighResFrequency)}
};

#undef CBA_ARGS
#undef CL_ARGS
#undef JLS
#undef BB
#undef FN_PTR
#undef CC

// This one function is exported, used by NativeLookup.
JVM_ENTRY(void, JVM_RegisterPerfMethods(JNIEnv *env, jclass perfclass))
  PerfWrapper("JVM_RegisterPerfMethods");
  {
    ThreadToNativeFromVM ttnfv(thread);
    int ok = env->RegisterNatives(perfclass, perfmethods, sizeof(perfmethods)/sizeof(JNINativeMethod));
    guarantee(ok == 0, "register perf natives");
  }
JVM_END
