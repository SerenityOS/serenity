/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 */

#include <string.h>
#include "jvmti.h"

extern "C" {

static const char* EXP_INTERF_SIG = "LP/Q/HCInterf;";
static const char* SIG_START      = "LP/Q/HiddenClassSig";
static const size_t SIG_START_LEN = strlen(SIG_START);
static const int    ACC_INTERFACE = 0x0200; // Interface class modifiers bit

static jvmtiEnv *jvmti = NULL;
static jint class_load_count = 0;
static jint class_prep_count = 0;
static bool failed = false;

#define LOG0(str)             { printf(str); fflush(stdout); }
#define LOG1(str, arg)        { printf(str, arg); fflush(stdout); }
#define LOG2(str, arg1, arg2) { printf(str, arg1, arg2); fflush(stdout); }

#define CHECK_JVMTI_ERROR(jni, err, msg) \
  if (err != JVMTI_ERROR_NONE) { \
    LOG1("CHECK_JVMTI_ERROR: JVMTI function returned error: %d\n", err); \
    jni->FatalError(msg); \
    return; \
  }

/* Return the jmethodID of j.l.Class.isHidden() method. */
static jmethodID
is_hidden_mid(JNIEnv* jni) {
  char* csig = NULL;
  jint count = 0;
  jmethodID *methods = NULL;
  jclass clazz  = jni->FindClass("java/lang/Class");
  if (clazz == NULL) {
    jni->FatalError("is_hidden_mid: Error: FindClass returned NULL for java/lang/Class\n");
    return NULL;
  }

  // find the jmethodID of j.l.Class.isHidden() method
  jmethodID mid = jni->GetMethodID(clazz, "isHidden", "()Z");
  if (mid == NULL) {
    jni->FatalError("is_hidden_mid: Error in jni GetMethodID: Cannot find j.l.Class.isHidden method\n");
  }
  return mid;
}

/* Return true if the klass is hidden. */
static bool
is_hidden(JNIEnv* jni, jclass klass) {
  static jmethodID is_hid_mid = NULL;

  if (is_hid_mid == NULL) {
    is_hid_mid = is_hidden_mid(jni);
  }
  // invoke j.l.Class.isHidden() method
  bool res = jni->CallBooleanMethod(klass, is_hid_mid);
  if (jni->ExceptionCheck()) {
    jni->ExceptionDescribe();
    jni->FatalError("is_hidden: Exception in jni CallBooleanMethod\n");
  }
  return res;
}

/* Check the class signature matches the expected. */
static void
check_class_signature(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass, bool is_hidden, const char* exp_sig) {
  jint class_modifiers = 0;
  char* sig = NULL;
  char* gsig = NULL;
  jvmtiError err;

  // get class signature
  err = jvmti->GetClassSignature(klass, &sig, &gsig);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class: Error in JVMTI GetClassSignature");

  LOG1("check_class_signature: class with sig: %s\n", sig);
  LOG1("check_class_signature: class with gsig: %s\n", gsig);

  if (strcmp(sig, exp_sig) != 0) {
    LOG2("check_class_signature: FAIL: Hidden class signature %s does not match expected: %s\n", sig, exp_sig);
    failed = true;
  }
  if (is_hidden && gsig == NULL) {
    LOG0("check_class_signature: FAIL: unexpected NULL generic signature for hidden class\n");
    failed = true;
  }
}

/* Test hidden class flags: it should not be interface, array nor modifiable. */
static void
check_hidden_class_flags(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  jint modifiers = 0;
  jboolean flag = false;
  jvmtiError err;

  err = jvmti->GetClassModifiers(klass, &modifiers);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_flags: Error in JVMTI GetClassModifiers");
  LOG1("check_hidden_class_flags: hidden class modifiers: 0x%x\n", modifiers);
  if ((modifiers & ACC_INTERFACE) != 0) {
    LOG0("check_hidden_class_flags: FAIL: unexpected ACC_INTERFACE bit in hidden class modifiers\n");
    failed = true;
    return;
  }

  err = jvmti->IsInterface(klass, &flag);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_flags: Error in JVMTI IsInterface");
  if (flag) {
    LOG0("check_hidden_class_flags: FAIL: hidden class is not expected to be interface\n");
    failed = true;
    return;
  }

  err = jvmti->IsArrayClass(klass, &flag);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_flags: Error in JVMTI IsArrayClass");
  if (flag) {
    LOG0("check_hidden_class_flags: FAIL: hidden class is not expected to be array\n");
    failed = true;
    return;
  }
  err = jvmti->IsModifiableClass(klass, &flag);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_flags: Error in JVMTI IsModifiableClass");
  if (flag) {
    LOG0("check_hidden_class_flags: FAIL: hidden class is not expected to be modifiable\n");
    failed = true;
  }
}

/* Test GetClassLoaderClasses: it should not return any hidden classes. */
static void
check_hidden_class_loader(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  jint count = 0;
  jobject loader = NULL;
  jclass* loader_classes = NULL;
  jboolean found = false;
  jvmtiError err;

  err = jvmti->GetClassLoader(klass, &loader);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_loader: Error in JVMTI GetClassLoader");

  jni->EnsureLocalCapacity(256); // to avoid warnings: JNI local refs NN exceeds capacity

  err = jvmti->GetClassLoaderClasses(loader, &count, &loader_classes);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_loader: Error in JVMTI GetClassLoaderClasses");

  for (int idx = 0; idx < count; idx++) {
    char* sig = NULL;
    jclass kls = loader_classes[idx];

    // GetClassLoaderClasses should not return any hidden classes
    if (!is_hidden(jni, kls)) {
      continue;
    }
    // get class signature
    err = jvmti->GetClassSignature(kls, &sig, NULL);
    CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_loader: Error in JVMTI GetClassSignature");

    LOG1("check_hidden_class_loader: FAIL: JVMTI GetClassLoaderClasses returned hidden class: %s\n", sig);
    failed = true;
    return;
  }
  LOG0("check_hidden_class_loader: not found hidden class in its loader classes as expected\n");
}

/* Test the hidden class implements expected interface. */
static void
check_hidden_class_impl_interf(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass) {
  char* sig = NULL;
  jint count = 0;
  jclass* interfaces = NULL;
  jvmtiError err;

  // check that hidden class implements just one interface
  err = jvmti->GetImplementedInterfaces(klass, &count, &interfaces);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_impl_interf: Error in JVMTI GetImplementedInterfaces");
  if (count != 1) {
    LOG1("check_hidden_class_impl_interf: FAIL: implemented interfaces count: %d, expected to be 1\n", count);
    failed = true;
    return;
  }
  // get interface signature
  err = jvmti->GetClassSignature(interfaces[0], &sig, NULL);
  CHECK_JVMTI_ERROR(jni, err, "check_hidden_class_impl_interf: Error in JVMTI GetClassSignature for implemented interface");

  // check the interface signature is matching the expected
  if (strcmp(sig, EXP_INTERF_SIG) != 0) {
    LOG2("check_hidden_class_impl_interf: FAIL: implemented interface signature: %s, expected to be: %s\n",
           sig, EXP_INTERF_SIG);
    failed = true;
  }
}

/* Test hidden class. */
static void
check_hidden_class(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass, const char* exp_sig) {
  char* source_file_name = NULL;

  LOG1("\n### Native agent: check_hidden_class started: class: %s\n", exp_sig);

  check_class_signature(jvmti, jni, klass, true /* not hidden */,  exp_sig);
  if (failed) return;

  check_hidden_class_flags(jvmti, jni, klass);
  if (failed) return;

  check_hidden_class_loader(jvmti, jni, klass);
  if (failed) return;

  check_hidden_class_impl_interf(jvmti, jni, klass);
  if (failed) return;

  LOG0("### Native agent: check_hidden_class finished\n");
}

/* Test hidden class array. */
static void
check_hidden_class_array(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass_array, const char* exp_sig) {
  char* source_file_name = NULL;

  LOG1("\n### Native agent: check_hidden_class_array started: array: %s\n", exp_sig);

  check_class_signature(jvmti, jni, klass_array, false /* is hidden */, exp_sig);
  if (failed) return;

  LOG0("### Native agent: check_hidden_class_array finished\n");
}

/* Process a CLASS_LOAD or aClassPrepare event. */
static void process_class_event(jvmtiEnv* jvmti, JNIEnv* jni, jclass klass,
                                jint* event_count_ptr, const char* event_name) {
  char* sig = NULL;
  char* gsig = NULL;
  jvmtiError err;

  // get class signature
  err = jvmti->GetClassSignature(klass, &sig, &gsig);
  CHECK_JVMTI_ERROR(jni, err, "ClassLoad event: Error in JVMTI GetClassSignature");

  // check if this is an expected class event for hidden class
  if (strlen(sig) > strlen(SIG_START) &&
      strncmp(sig, SIG_START, SIG_START_LEN) == 0 &&
      is_hidden(jni, klass)) {
    (*event_count_ptr)++;
    if (gsig == NULL) {
      LOG1("%s event: FAIL: GetClassSignature returned NULL generic signature for hidden class\n", event_name);
      failed = true;
    }
    LOG2("%s event: hidden class with sig: %s\n", event_name, sig);
    LOG2("%s event: hidden class with gsig: %s\n", event_name, gsig);
  }
}

/* Check CLASS_LOAD event is generated for the given hidden class. */
static void JNICALL
ClassLoad(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread, jclass klass) {
  process_class_event(jvmti, jni, klass, &class_load_count, "ClassLoad");
}

/* Check CLASS_PREPARE event is generated for the given hidden class. */
static void JNICALL
ClassPrepare(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread, jclass klass) {
  process_class_event(jvmti, jni, klass, &class_prep_count, "ClassPrepare");
}

/* Enable CLASS_LOAD event notification mode. */
static void JNICALL
VMInit(jvmtiEnv* jvmti, JNIEnv* jni, jthread thread) {
  jvmtiError err;

  printf("VMInit event: SIG_START: %s, SIG_START_LEN: %d\n", SIG_START, (int)SIG_START_LEN);
  fflush(stdout);

  // enable ClassLoad event notification mode
  err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_LOAD, NULL);
  CHECK_JVMTI_ERROR(jni, err, "VMInit event: Error in enabling ClassLoad events notification");

  // enable ClassPrepare event notification mode
  err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
  CHECK_JVMTI_ERROR(jni, err, "VMInit event: Error in enabling ClassPrepare events notification");
}

JNIEXPORT jint JNICALL
Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  jvmtiEventCallbacks callbacks;
  jvmtiError err;

  LOG0("Agent_OnLoad: started\n");
  if (jvm->GetEnv((void **) (&jvmti), JVMTI_VERSION) != JNI_OK) {
    LOG0("Agent_OnLoad: Error in GetEnv in obtaining jvmtiEnv*\n");
    failed = true;
    return JNI_ERR;
  }

  // set required event callbacks
  memset(&callbacks, 0, sizeof(callbacks));
  callbacks.ClassLoad = &ClassLoad;
  callbacks.ClassPrepare = &ClassPrepare;
  callbacks.VMInit = &VMInit;

  err = jvmti->SetEventCallbacks(&callbacks, sizeof(jvmtiEventCallbacks));
  if (err != JVMTI_ERROR_NONE) {
    LOG1("Agent_OnLoad: Error in JVMTI SetEventCallbacks: %d\n", err);
    failed = true;
    return JNI_ERR;
  }

  // enable VM_INIT event notification mode
  err = jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
  if (err != JVMTI_ERROR_NONE) {
    LOG1("Agent_OnLoad: Error in JVMTI SetEventNotificationMode: %d\n", err);
    failed = true;
    return JNI_ERR;
  }

  LOG0("Agent_OnLoad: finished\n");
  return JNI_OK;
}

/* Native method: checkHiddenClass(). */
JNIEXPORT void JNICALL
Java_P_Q_HiddenClassSigTest_checkHiddenClass(JNIEnv *jni, jclass klass, jclass hidden_klass, jstring exp_sig_str) {
  const char* exp_sig = jni->GetStringUTFChars(exp_sig_str, NULL);

  if (exp_sig == NULL) {
    jni->FatalError("check_hidden_class: Error: JNI GetStringChars returned NULL for jstring\n");
    return;
  }
  check_hidden_class(jvmti, jni, hidden_klass, exp_sig);

  jni->ReleaseStringUTFChars(exp_sig_str, exp_sig);
}

/* Native method: checkHiddenClassArray(). */
JNIEXPORT void JNICALL
Java_P_Q_HiddenClassSigTest_checkHiddenClassArray(JNIEnv *jni, jclass klass, jclass hidden_klass_array, jstring exp_sig_str) {
  const char* exp_sig = jni->GetStringUTFChars(exp_sig_str, NULL);

  if (exp_sig == NULL) {
    jni->FatalError("check_hidden_class_array: Error: JNI GetStringChars returned NULL for jstring\n");
    return;
  }
  check_hidden_class_array(jvmti, jni, hidden_klass_array, exp_sig);

  jni->ReleaseStringUTFChars(exp_sig_str, exp_sig);
}

/* Native method: checkFailed(). */
JNIEXPORT jboolean JNICALL
Java_P_Q_HiddenClassSigTest_checkFailed(JNIEnv *jni, jclass klass) {
  if (class_load_count == 0) {
    // expected ClassLoad event was not generated for hidden class
    LOG0("Native Agent: FAIL: missed ClassLoad event for hidden class\n");
    failed = true;
  }
  if (class_prep_count == 0) {
    // expected ClassPrepare event was not generated for hidden class
    LOG0("Native Agent: FAIL: missed ClassPrepare event for hidden class\n");
    failed = true;
  }
  return failed;
}

} // extern "C"
