/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_jvm_hotspot_asm_Disassembler.h"

/*
 *  This file implements a binding between Java and the hsdis
 *  disassembler.  It should compile on Linux and Windows.
 *  The only platform dependent pieces of the code for doing
 *  dlopen/dlsym to find the entry point in hsdis.  All the rest is
 *  standard JNI code.
 */

#ifdef _WINDOWS
// Disable CRT security warning against _snprintf
#pragma warning (disable : 4996)

#define snprintf  _snprintf
#define vsnprintf _vsnprintf

#include <windows.h>
#include <sys/types.h>
#include <sys/stat.h>
#ifdef _DEBUG
#include <crtdbg.h>
#endif

#else

#include <string.h>
#include <dlfcn.h>

#ifndef __APPLE__
#include <link.h>
#endif

#endif

#include <limits.h>
#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <errno.h>

#ifdef _WINDOWS
#define JVM_MAXPATHLEN _MAX_PATH
#else
#include <sys/param.h>
#define JVM_MAXPATHLEN MAXPATHLEN
#endif

#include "jni_util.h"


/*
 * Class:     sun_jvm_hotspot_asm_Disassembler
 * Method:    load_library
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_sun_jvm_hotspot_asm_Disassembler_load_1library(JNIEnv * env,
                                                                           jclass disclass,
                                                                           jstring libname_s) {
  uintptr_t func = 0;
  const char *error_message = NULL;
  const char *libname = NULL;

#ifdef _WINDOWS
  char buffer[JVM_MAXPATHLEN];
  HINSTANCE hsdis_handle = (HINSTANCE) NULL;
#else
  void* hsdis_handle = NULL;
#endif

  libname = (*env)->GetStringUTFChars(env, libname_s, NULL);
  if (libname == NULL || (*env)->ExceptionOccurred(env)) {
    return 0;
  }

  /* Load the hsdis library */
#ifdef _WINDOWS
  hsdis_handle = LoadLibrary(libname);
  if (hsdis_handle != NULL) {
    func = (uintptr_t)GetProcAddress(hsdis_handle, "decode_instructions_virtual");
  }
  if (func == 0) {
    getLastErrorString(buffer, sizeof(buffer));
    error_message = buffer;
  }
#else
  hsdis_handle = dlopen(libname, RTLD_LAZY | RTLD_GLOBAL);
  if (hsdis_handle != NULL) {
    func = (uintptr_t)dlsym(hsdis_handle, "decode_instructions_virtual");
  }
  if (func == 0) {
    error_message = dlerror();
  }
#endif

  (*env)->ReleaseStringUTFChars(env, libname_s, libname);

  if (func == 0) {
    /* Couldn't find entry point.  error_message should contain some
     * platform dependent error message.
     */
    jstring s = JNU_NewStringPlatform(env, error_message);
    if (s != NULL) {
      jobject x = JNU_NewObjectByName(env, "sun/jvm/hotspot/debugger/DebuggerException", "(Ljava/lang/String;)V", s);
      if (x != NULL) {
        (*env)->Throw(env, x);
      }
    }
  }
  return (jlong)func;
}

/* signature of decode_instructions_virtual from hsdis.h */
typedef void* (*decode_func)(uintptr_t start_va, uintptr_t end_va,
                             unsigned char* start, uintptr_t length,
                             void* (*event_callback)(void*, const char*, void*),
                             void* event_stream,
                             int (*printf_callback)(void*, const char*, ...),
                             void* printf_stream,
                             const char* options,
                             int newline);

/* container for call back state when decoding instructions */
typedef struct {
  JNIEnv* env;
  jobject dis;
  jobject visitor;
  jmethodID handle_event;
  jmethodID raw_print;
  char buffer[4096];
} decode_env;


/* event callback binding to Disassembler.handleEvent */
static void* event_to_env(void* env_pv, const char* event, void* arg) {
  jlong result = 0;
  decode_env* denv = (decode_env*)env_pv;
  JNIEnv* env = denv->env;
  jstring event_string = (*env)->NewStringUTF(env, event);
  if ((*env)->ExceptionOccurred(env)) {
    return NULL;
  }

  result = (*env)->CallLongMethod(env, denv->dis, denv->handle_event, denv->visitor,
                                  event_string, (jlong) (uintptr_t)arg);
  if ((*env)->ExceptionOccurred(env)) {
    /* ignore exceptions for now */
    (*env)->ExceptionClear(env);
    return NULL;
  }

  return (void*)(uintptr_t)result;
}

/* printing callback binding to Disassembler.rawPrint */
static int printf_to_env(void* env_pv, const char* format, ...) {
  jstring output;
  va_list ap;
  int cnt;
  decode_env* denv = (decode_env*)env_pv;
  JNIEnv* env = denv->env;
  size_t flen = strlen(format);
  const char* raw = NULL;

  if (flen == 0)  return 0;
  if (flen < 2 ||
      strchr(format, '%') == NULL) {
    raw = format;
  } else if (format[0] == '%' && format[1] == '%' &&
             strchr(format+2, '%') == NULL) {
    // happens a lot on machines with names like %foo
    flen--;
    raw = format+1;
  }
  if (raw != NULL) {
    jstring output = (*env)->NewStringUTF(env, raw);
    if (!(*env)->ExceptionOccurred(env)) {
      /* make sure that UTF allocation doesn't cause OOM */
      (*env)->CallVoidMethod(env, denv->dis, denv->raw_print, denv->visitor, output);
    }
    if ((*env)->ExceptionOccurred(env)) {
      /* ignore exceptions for now */
        (*env)->ExceptionClear(env);
    }
    return (int) flen;
  }
  va_start(ap, format);
  cnt = vsnprintf(denv->buffer, sizeof(denv->buffer), format, ap);
  va_end(ap);

  output = (*env)->NewStringUTF(env, denv->buffer);
  if (!(*env)->ExceptionOccurred(env)) {
    /* make sure that UTF allocation doesn't cause OOM */
    (*env)->CallVoidMethod(env, denv->dis, denv->raw_print, denv->visitor, output);
  }

  if ((*env)->ExceptionOccurred(env)) {
    /* ignore exceptions for now */
    (*env)->ExceptionClear(env);
  }

  return cnt;
}

/*
 * Class:     sun_jvm_hotspot_asm_Disassembler
 * Method:    decode
 * Signature: (Lsun/jvm/hotspot/asm/InstructionVisitor;J[BLjava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_sun_jvm_hotspot_asm_Disassembler_decode(JNIEnv * env,
                                                                    jobject dis,
                                                                    jobject visitor,
                                                                    jlong startPc,
                                                                    jbyteArray code,
                                                                    jstring options_s,
                                                                    jlong decode_instructions_virtual) {
  jbyte *start = NULL;
  jbyte *end = NULL;
  jclass disclass = NULL;
  const char *options = NULL;
  decode_env denv;

  start = (*env)->GetByteArrayElements(env, code, NULL);
  if ((*env)->ExceptionOccurred(env)) {
    return;
  }
  end = start + (*env)->GetArrayLength(env, code);
  options = (*env)->GetStringUTFChars(env, options_s, NULL);
  if ((*env)->ExceptionOccurred(env)) {
    (*env)->ReleaseByteArrayElements(env, code, start, JNI_ABORT);
    return;
  }
  disclass = (*env)->GetObjectClass(env, dis);

  denv.env = env;
  denv.dis = dis;
  denv.visitor = visitor;

  /* find Disassembler.handleEvent callback */
  denv.handle_event = (*env)->GetMethodID(env, disclass, "handleEvent",
                                          "(Lsun/jvm/hotspot/asm/InstructionVisitor;Ljava/lang/String;J)J");
  if ((*env)->ExceptionOccurred(env)) {
    (*env)->ReleaseByteArrayElements(env, code, start, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, options_s, options);
    return;
  }

  /* find Disassembler.rawPrint callback */
  denv.raw_print = (*env)->GetMethodID(env, disclass, "rawPrint",
                                       "(Lsun/jvm/hotspot/asm/InstructionVisitor;Ljava/lang/String;)V");
  if ((*env)->ExceptionOccurred(env)) {
    (*env)->ReleaseByteArrayElements(env, code, start, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, options_s, options);
    return;
  }

  /* decode the buffer */
  (*(decode_func)(uintptr_t)decode_instructions_virtual)((uintptr_t) startPc,
                                                         startPc + end - start,
                                                         (unsigned char*)start,
                                                         end - start,
                                                         &event_to_env,  (void*) &denv,
                                                         &printf_to_env, (void*) &denv,
                                                         options, 0 /* newline */);

  /* cleanup */
  (*env)->ReleaseByteArrayElements(env, code, start, JNI_ABORT);
  (*env)->ReleaseStringUTFChars(env, options_s, options);
}
