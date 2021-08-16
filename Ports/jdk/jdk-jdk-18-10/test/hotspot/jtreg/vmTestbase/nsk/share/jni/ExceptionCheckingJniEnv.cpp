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

#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#include "ExceptionCheckingJniEnv.hpp"
#include "nsk_tools.h"

namespace {

static const char* get_dirname(const char* fullname) {
  const char* p;
  const char* base = fullname;;

  if (fullname == NULL) {
    return NULL;
  }

  for (p = fullname; *p != '\0'; p++) {
    if (*p == '/' || *p == '\\') {
      base = p + 1;
    }
  }
  return base;
}

template<class T = void*>
class JNIVerifier {
 public:
  JNIVerifier(ExceptionCheckingJniEnv *env, const char* base_message,
              int line, const char* file)
      : _env(env), _base_message(base_message), _error_message(NULL),
        _line(line), _file(get_dirname(file)) {
  }

  // Until C++11 is supported, we have to write multiple template constructors.
  template <typename U>
  JNIVerifier(ExceptionCheckingJniEnv *env, const char* base_message,
              U parameter,
              int line, const char* file)
      : _env(env), _base_message(base_message), _error_message(NULL),
        _line(line), _file(get_dirname(file)) {
          PrintPreCall(parameter);
  }

  template <typename U, typename V>
  JNIVerifier(ExceptionCheckingJniEnv *env, const char* base_message,
              U parameter1,
              V parameter2,
              int line, const char* file)
      : _env(env), _base_message(base_message), _error_message(NULL),
        _line(line), _file(get_dirname(file)) {
          PrintPreCall(parameter1, parameter2);
  }

  template <typename U, typename V, typename W>
  JNIVerifier(ExceptionCheckingJniEnv *env, const char* base_message,
              U parameter1, V parameter2, W parameter3,
              int line, const char* file)
      : _env(env), _base_message(base_message), _error_message(NULL),
        _line(line), _file(get_dirname(file)) {
          PrintPreCall(parameter1, parameter2, parameter3);
  }

  ~JNIVerifier() {
    PrintPostCall();

    JNIEnv* jni_env = _env->GetJNIEnv();
    if (jni_env->ExceptionCheck() && !_error_message) {
      _error_message = "internal error";
    }

    if (_error_message != NULL) {
      GenerateErrorMessage();
    }
  }

  int DecimalToAsciiRec(char *str, int line) {
    if (line == 0) {
      return 0;
    }

    int remainder = line % 10;
    long quotient = line / 10;

    int pos = DecimalToAsciiRec(str, quotient);
    str[pos] = '0' + remainder;
    return pos + 1;
  }

  // Implementing a simple version of sprintf for "%d"...
  void DecimalToAscii(char *str, int line) {
    if (line == 0) {
      str[0] = '0';
      str[1] = '\0';
      return;
    }

    // Special case for INT32_MIN because otherwise the *1 below will overflow
    // and it won't work. Let us just be simple here due to this being for
    // tests.
    if (line == INT32_MIN) {
      strcat(str, "-2147483648");
      return;
    }

    if (line < 0) {
      *str = '-';
      line *= -1;
      str++;
    }

    str[DecimalToAsciiRec(str, line)] = '\0';
  }

  void GenerateErrorMessage() {
    // This is error prone, but:
    //   - Seems like we cannot use std::string (due to windows/solaris not
    //   building when used, seemingly due to exception libraries not linking).
    //   - Seems like we cannot use sprintf due to VS2013 (JDK-8213622).
    //
    //   We are aiming to do:
    //     snprintf(full_message, len, "JNI method %s : %s from %s : %d", _base_message, _error_message,
    //              _file, _line);
    //   but will use strlen + memcpy instead.
    const char* pre_message = "JNI method ";
    const char* between_msg = " : ";
    const char* from_msg = " from ";

    const char* file_name = _file ? _file : "Unknown File";
    const char* strs[] = {
      pre_message,
      _base_message,
      between_msg,
      _error_message,
      from_msg,
      file_name,
      between_msg,
    };

    size_t msg_number = sizeof(strs) / sizeof(strs[0]);
    size_t len = 0;
    for (size_t i = 0; i < msg_number; i++) {
      len += strlen(strs[i]);
    }

    // 32-bit signed means 11 characters due to the '-'.
    const int MAX_INTEGER_DIGITS = 11;
    // Add for the line number and 1 for the '\0'.
    len += MAX_INTEGER_DIGITS + 1;

    char* full_message = (char*) malloc(len);
    if (full_message == NULL) {
      _env->HandleError(_error_message);
      return;
    }

    // Now we construct the string using strcat to not use sprintf/std::string
    // instead of:
    //     snprintf(full_message, len, "JNI method %s : %s from %s:%d", _base_message,
    //         _error_message, _file, _line);
    full_message[0] = '\0';
    for (size_t i = 0; i < msg_number; i++) {
      strcat(full_message, strs[i]);
    }

    // Add line number to end of the string.
    DecimalToAscii(full_message + strlen(full_message), _line);

    if (strlen(full_message) >= len) {
      _env->GetJNIEnv()->FatalError("Final length of message is not what was expected");
    }

    _env->HandleError(full_message);
    free(full_message);
  }

  T ResultNotNull(T ptr) {
    if (ptr == NULL) {
      _error_message = "Return is NULL";
    }
    return ptr;
  }

  T ResultIsZero(T value) {
    if (value != 0) {
      _error_message = "Return is not zero";
    }
    return value;
  }

  void PrintPreCallHeader() {
    if (!nsk_getVerboseMode()) {
      return;
    }

    fprintf(stdout, ">> Calling JNI method %s from %s:%d\n",
            _base_message, _file, _line);
    fprintf(stdout, ">> Calling with these parameter(s):\n");
  }

  // Until we can actually link with C++ more uniformely across architectures,
  // we have to do this...
  template<class U>
  void PrintParameter(U* ptr) {
    fprintf(stdout, "\t%p\n", ptr);
  }

  void PrintParameter(int value) {
    fprintf(stdout, "\t%d\n", value);
  }

  // Until C++11 is supported, we have to write multiple PrintPreCall.
  template<class U>
  void PrintPreCall(U first_parameter) {
    if (!nsk_getVerboseMode()) {
      return;
    }

    PrintPreCallHeader();
    PrintParameter(first_parameter);
  }

  template<class U, class V>
  void PrintPreCall(U parameter1, V parameter2) {
    if (!nsk_getVerboseMode()) {
      return;
    }

    PrintPreCallHeader();
    PrintParameter(parameter1);
    PrintParameter(parameter2);
  }

  template<class U, class V, class W>
  void PrintPreCall(U parameter1, V parameter2, W parameter3) {
    if (!nsk_getVerboseMode()) {
      return;
    }

    PrintPreCallHeader();
    PrintParameter(parameter1);
    PrintParameter(parameter2);
    PrintParameter(parameter3);
  }

  void PrintPostCall() {
    if (!nsk_getVerboseMode()) {
      return;
    }

    fprintf(stderr, "<< Called JNI method %s from %s:%d\n",
            _base_message, _file, _line);
  }

 private:
  ExceptionCheckingJniEnv* _env;
  const char* const _base_message;
  const char* _error_message;
  int _line;
  const char* const _file;
};

}

jclass ExceptionCheckingJniEnv::FindClass(const char *class_name,
                                          int line, const char* file_name) {
  JNIVerifier<jclass> marker(this, "FindClass", class_name, line, file_name);
  return marker.ResultNotNull(_jni_env->FindClass(class_name));
}

jint ExceptionCheckingJniEnv::RegisterNatives(jclass clazz,
                                              const JNINativeMethod *methods,
                                              jint nMethods,
                                              int line,
                                              const char* file_name) {
  JNIVerifier<jint> marker(this, "RegisterNatives", methods, nMethods, line, file_name);
  return marker.ResultIsZero(_jni_env->RegisterNatives(clazz, methods, nMethods));
}

jclass ExceptionCheckingJniEnv::GetObjectClass(jobject obj, int line,
                                               const char* file_name) {
  JNIVerifier<jclass> marker(this, "GetObjectClass", obj, line, file_name);
  return marker.ResultNotNull(_jni_env->GetObjectClass(obj));
}

jfieldID ExceptionCheckingJniEnv::GetStaticFieldID(jclass klass, const char *name,
                                                   const char* type,
                                                   int line, const char* file_name) {
  JNIVerifier<jfieldID> marker(this, "GetStaticFieldID", klass, name, type,
                               line, file_name);
  return marker.ResultNotNull(_jni_env->GetStaticFieldID(klass, name, type));
}

jfieldID ExceptionCheckingJniEnv::GetFieldID(jclass klass, const char *name,
                                             const char* type,
                                             int line, const char* file_name) {
  JNIVerifier<jfieldID> marker(this, "GetFieldID", klass, name, type, line, file_name);
  return marker.ResultNotNull(_jni_env->GetFieldID(klass, name, type));
}

jobject ExceptionCheckingJniEnv::GetStaticObjectField(jclass klass, jfieldID field,
                                                      int line, const char* file_name) {
  JNIVerifier<jobject> marker(this, "GetStaticObjectField", klass, field,
                              line, file_name);
  return marker.ResultNotNull(_jni_env->GetStaticObjectField(klass, field));
}

jobject ExceptionCheckingJniEnv::GetObjectField(jobject obj, jfieldID field,
                                                int line, const char* file_name) {
  JNIVerifier<jobject> marker(this, "GetObjectField", obj, field, line, file_name);
  return marker.ResultNotNull(_jni_env->GetObjectField(obj, field));
}

void ExceptionCheckingJniEnv::SetObjectField(jobject obj, jfieldID field, jobject value,
                                             int line, const char* file_name) {
  JNIVerifier<> marker(this, "SetObjectField", obj, field, value, line, file_name);
  _jni_env->SetObjectField(obj, field, value);
}

jobject ExceptionCheckingJniEnv::NewGlobalRef(jobject obj, int line, const char* file_name) {
  JNIVerifier<jobject> marker(this, "NewGlobalRef", obj, line, file_name);
  return marker.ResultNotNull(_jni_env->NewGlobalRef(obj));
}

void ExceptionCheckingJniEnv::DeleteGlobalRef(jobject obj, int line, const char* file_name) {
  JNIVerifier<> marker(this, "DeleteGlobalRef", obj, line, file_name);
  _jni_env->DeleteGlobalRef(obj);
}

jobject ExceptionCheckingJniEnv::NewLocalRef(jobject obj, int line, const char* file_name) {
  JNIVerifier<jobject> marker(this, "NewLocalRef", obj, line, file_name);
  return marker.ResultNotNull(_jni_env->NewLocalRef(obj));
}

void ExceptionCheckingJniEnv::DeleteLocalRef(jobject obj, int line, const char* file_name) {
  JNIVerifier<> marker(this, "DeleteLocalRef", obj, line, file_name);
  _jni_env->DeleteLocalRef(obj);
}

jweak ExceptionCheckingJniEnv::NewWeakGlobalRef(jobject obj, int line, const char* file_name) {
  JNIVerifier<jweak> marker(this, "NewWeakGlobalRef", obj, line, file_name);
  return marker.ResultNotNull(_jni_env->NewWeakGlobalRef(obj));
}

void ExceptionCheckingJniEnv::DeleteWeakGlobalRef(jweak weak_ref, int line, const char* file_name) {
  JNIVerifier<> marker(this, "DeleteWeakGlobalRef", weak_ref, line, file_name);
  _jni_env->DeleteWeakGlobalRef(weak_ref);
}

jsize ExceptionCheckingJniEnv::GetArrayLength(jarray array, int line, const char* file_name) {
  JNIVerifier<> marker(this, "GetArrayLength", array, line, file_name);
  return _jni_env->GetArrayLength(array);
}

jsize ExceptionCheckingJniEnv::GetStringLength(jstring str, int line, const char* file_name) {
  JNIVerifier<> marker(this, "GetStringLength", str, line, file_name);
  return _jni_env->GetStringLength(str);
}

void* ExceptionCheckingJniEnv::GetPrimitiveArrayCritical(jarray array, jboolean* is_copy,
                                                         int line, const char* file_name) {
  JNIVerifier<> marker(this, "GetPrimitiveArrayCritical", array, is_copy, line, file_name);
  return marker.ResultNotNull(_jni_env->GetPrimitiveArrayCritical(array, is_copy));
}

void ExceptionCheckingJniEnv::ReleasePrimitiveArrayCritical(jarray array, void* carray, jint mode,
                                                            int line, const char* file_name) {
  JNIVerifier<> marker(this, "ReleasePrimitiveArrayCritical", array, carray, mode,
                       line, file_name);
  _jni_env->ReleasePrimitiveArrayCritical(array, carray, mode);
}

const jchar* ExceptionCheckingJniEnv::GetStringCritical(jstring str, jboolean* is_copy,
                                                        int line, const char* file_name) {
  JNIVerifier<const jchar*> marker(this, "GetPrimitiveArrayCritical", str, is_copy,
                                   line, file_name);
  return marker.ResultNotNull(_jni_env->GetStringCritical(str, is_copy));
}

void ExceptionCheckingJniEnv::ReleaseStringCritical(jstring str, const jchar* carray,
                                                    int line, const char* file_name) {
  JNIVerifier<> marker(this, "ReleaseStringCritical", str, carray, line, file_name);
  _jni_env->ReleaseStringCritical(str, carray);
}

jbyte* ExceptionCheckingJniEnv::GetByteArrayElements(jbyteArray array, jboolean* is_copy,
                                                   int line, const char* file_name) {
  JNIVerifier<jbyte*> marker(this, "GetByteArrayElements", array, is_copy, line, file_name);
  return marker.ResultNotNull(_jni_env->GetByteArrayElements(array, is_copy));
}

void ExceptionCheckingJniEnv::ReleaseByteArrayElements(jbyteArray array, jbyte* byte_array, jint mode,
                                                       int line, const char* file_name) {
  JNIVerifier<> marker(this, "ReleaseByteArrayElements", array, byte_array, mode,
                       line, file_name);
  _jni_env->ReleaseByteArrayElements(array, byte_array, mode);
}

jmethodID ExceptionCheckingJniEnv::GetMethodID(jclass klass, const char* name, const char* sig,
                                               int line, const char* file_name) {
  JNIVerifier<jmethodID> marker(this, "GetMethodID", klass, name, sig, line, file_name);
  return marker.ResultNotNull(_jni_env->GetMethodID(klass, name, sig));
}

jmethodID ExceptionCheckingJniEnv::GetStaticMethodID(jclass klass, const char* name, const char* sig,
                                                     int line, const char* file_name) {
  JNIVerifier<jmethodID> marker(this, "GetStaticMethodID", klass, name, sig, line, file_name);
  return marker.ResultNotNull(_jni_env->GetStaticMethodID(klass, name, sig));
}

jboolean ExceptionCheckingJniEnv::IsSameObject(jobject ref1, jobject ref2, int line, const char* file_name) {
  JNIVerifier<> marker(this, "IsSameObject", ref1, ref2, line, file_name);
  return _jni_env->IsSameObject(ref1, ref2);
}

jobject ExceptionCheckingJniEnv::NewObject(jclass klass, jmethodID methodID,
                                           int line, const char* file_name, ...) {
  // In the case of NewObject, we miss the extra arguments passed to NewObject sadly.
  JNIVerifier<jobject> marker(this, "NewObject", klass, methodID, line, file_name);

  va_list args;
  va_start(args, file_name);
  jobject result = marker.ResultNotNull(_jni_env->NewObjectV(klass, methodID, args));
  va_end(args);
  return result;
}

jobject ExceptionCheckingJniEnv::CallObjectMethod(jobject obj, jmethodID methodID, int line,
                         const char* file_name, ...) {
  JNIVerifier<> marker(this, "CallObjectMethod", obj, methodID, line, file_name);

  va_list args;
  va_start(args, file_name);
  jobject result = _jni_env->CallObjectMethodV(obj, methodID, args);
  va_end(args);
  return result;
}

void ExceptionCheckingJniEnv::CallVoidMethod(jobject obj, jmethodID methodID, int line,
                    const char* file_name, ...) {
  JNIVerifier<> marker(this, "CallVoidMethod", obj, methodID, line, file_name);

  va_list args;
  va_start(args, file_name);
  _jni_env->CallVoidMethodV(obj, methodID, args);
  va_end(args);
}
