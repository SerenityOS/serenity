/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2019, Google and/or its affiliates. All rights reserved.
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
#ifndef NSK_EXCEPTIONCHECKINGJNIENV_DEFINED
#define NSK_EXCEPTIONCHECKINGJNIENV_DEFINED

#include <jni.h>

/**
 * ExceptionCheckingJniEnv wraps around the JNIEnv data structure and
 * methods to enable automatic exception checking. This allows test writers
 * and readers to concentrate on what the test is to do and leave the
 * error checking and throwing to this data structure and subsystem.
 *
 * For example:
 *
 * ... JNIEnv* env ...
 *  jclass klass = env->GetObjectClass(o);
 *  if (klass == NULL) {
 *      printf("Error: GetObjectClass returned NULL\n");
 *      return;
 *  }
 *  if (env->ExceptionCheck()) {
 *    ...
 *  }
 *
 *  Can be simplified to:
 * ... ExceptionCheckingJniEnv* env ...
 *  jclass klass = env->GetObjectClass(o, TRACE_JNI_CALL);
 *
 *  Where now the JNI Exception checking and the NULL return checking are done
 *  internally and will perform whatever action the ErrorHandler requires.
 *
 *  Note the TRACE_JNI_CALL parameter that allows to trace where the call is
 *  happening from for debugging.
 *
 *  By default, the error handler describes the exception via the JNI
 *  ExceptionDescribe method and calls FatalError.
 */

#define TRACE_JNI_CALL __LINE__, __FILE__
#define TRACE_JNI_CALL_VARARGS(...) __LINE__, __FILE__, __VA_ARGS__

class ExceptionCheckingJniEnv {
 public:
  // JNIEnv API redefinitions.
  jclass FindClass(const char *name, int line, const char* file_name);

  jfieldID GetStaticFieldID(jclass klass, const char* name, const char* type,
                            int line, const char* file_name);
  jfieldID GetFieldID(jclass klass, const char* name, const char* type,
                      int line, const char* file_name);
  jmethodID GetStaticMethodID(jclass klass, const char* name, const char* sig,
                              int line, const char* file_name);
  jmethodID GetMethodID(jclass klass, const char* name, const char* sig,
                        int line, const char* file_name);

  jclass GetObjectClass(jobject obj, int line, const char* file_name);
  jobject GetObjectField(jobject obj, jfieldID field, int line, const char* file_name);
  jobject GetStaticObjectField(jclass kls, jfieldID field, int line, const char* file_name);
  void SetObjectField(jobject obj, jfieldID field, jobject value,
                      int line, const char* file_name);

  jsize GetArrayLength(jarray array, int line, const char* file_name);
  jsize GetStringLength(jstring str, int line, const char* file_name);

  void* GetPrimitiveArrayCritical(jarray array, jboolean* isCopy,
                                  int line, const char* file_name);
  void ReleasePrimitiveArrayCritical(jarray array, void* carray, jint mode,
                                     int line, const char* file_name);
  const jchar* GetStringCritical(jstring str, jboolean* isCopy,
                                 int line, const char* file_name);
  void ReleaseStringCritical(jstring str, const jchar* carray,
                             int line, const char* file_name);

  jbyte* GetByteArrayElements(jbyteArray array, jboolean* isCopy,
                              int line, const char* file_name);
  void ReleaseByteArrayElements(jbyteArray array, jbyte* byte_array, jint mode,
                                int line, const char* file_name);
  jint RegisterNatives(jclass clazz, const JNINativeMethod *methods, jint nMethods,
                       int line, const char* file_name);

  jobject NewObject(jclass kls, jmethodID methodID,
                    int line, const char* file_name, ...);
  jobject NewGlobalRef(jobject obj, int line, const char* file_name);
  void DeleteGlobalRef(jobject obj, int line, const char* file_name);
  jobject NewLocalRef(jobject ref, int line, const char* file_name);
  void DeleteLocalRef(jobject ref, int line, const char* file_name);
  jweak NewWeakGlobalRef(jobject obj, int line, const char* file_name);
  void DeleteWeakGlobalRef(jweak obj, int line, const char* file_name);

  jboolean IsSameObject(jobject ref1, jobject ref2, int line,
                        const char* file_name);

  jobject CallObjectMethod(jobject obj, jmethodID methodID, int line,
                           const char* file_name, ...);
  void CallVoidMethod(jobject obj, jmethodID methodID, int line,
                      const char* file_name, ...);

  // ExceptionCheckingJniEnv methods.
  JNIEnv* GetJNIEnv() {
    return _jni_env;
  }

  void HandleError(const char* msg) {
    if (_error_handler) {
      _error_handler(_jni_env, msg);
    }
  }

  typedef void (*ErrorHandler)(JNIEnv* env, const char* error_message);

  static void FatalError(JNIEnv* env, const char* message) {
    if (env->ExceptionCheck()) {
      env->ExceptionDescribe();
    }
    env->FatalError(message);
  }

  ExceptionCheckingJniEnv(JNIEnv* jni_env, ErrorHandler error_handler) :
    _jni_env(jni_env), _error_handler(error_handler) {}

 private:
  JNIEnv* _jni_env;
  ErrorHandler _error_handler;
};

// We cannot use unique_ptr due to this being gnu98++, so use this instead:
class ExceptionCheckingJniEnvPtr {
 private:
  ExceptionCheckingJniEnv _env;

 public:
  ExceptionCheckingJniEnv* operator->() {
    return &_env;
  }

  ExceptionCheckingJniEnvPtr(
      JNIEnv* jni_env,
      ExceptionCheckingJniEnv::ErrorHandler error_handler = ExceptionCheckingJniEnv::FatalError) :
          _env(jni_env, error_handler) {
  }
};

#endif
