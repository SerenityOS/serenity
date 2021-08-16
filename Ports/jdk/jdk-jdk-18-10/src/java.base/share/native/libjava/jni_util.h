/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

#ifndef JNI_UTIL_H
#define JNI_UTIL_H

#include "jni.h"
#include "jlong.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * This file contains utility functions that can be implemented in pure JNI.
 *
 * Caution: Callers of functions declared in this file should be
 * particularly aware of the fact that these functions are convenience
 * functions, and as such are often compound operations, each one of
 * which may throw an exception. Therefore, the functions this file
 * will often return silently if an exception has occurred, and callers
 * must check for exception themselves.
 */

/* Throw a Java exception by name. Similar to SignalError. */
JNIEXPORT void JNICALL
JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg);

/* Throw common exceptions */
JNIEXPORT void JNICALL
JNU_ThrowNullPointerException(JNIEnv *env, const char *msg);

JNIEXPORT void JNICALL
JNU_ThrowArrayIndexOutOfBoundsException(JNIEnv *env, const char *msg);

JNIEXPORT void JNICALL
JNU_ThrowOutOfMemoryError(JNIEnv *env, const char *msg);

JNIEXPORT void JNICALL
JNU_ThrowIllegalArgumentException(JNIEnv *env, const char *msg);

JNIEXPORT void JNICALL
JNU_ThrowInternalError(JNIEnv *env, const char *msg);

JNIEXPORT void JNICALL
JNU_ThrowIOException(JNIEnv *env, const char *msg);

JNIEXPORT void JNICALL
JNU_ThrowClassNotFoundException(JNIEnv *env, const char *msg);

/* Throw an exception by name, using the string returned by
 * getLastErrorString for the detail string. If the last-error
 * string is NULL, use the given default detail string.
 */
JNIEXPORT void JNICALL
JNU_ThrowByNameWithLastError(JNIEnv *env, const char *name,
                             const char *defaultDetail);

/* Throw an exception by name, using a given message and the string
 * returned by getLastErrorString to construct the detail string.
 */
JNIEXPORT void JNICALL
JNU_ThrowByNameWithMessageAndLastError
  (JNIEnv *env, const char *name, const char *message);

/* Throw an IOException, using the last-error string for the detail
 * string. If the last-error string is NULL, use the given default
 * detail string.
 */
JNIEXPORT void JNICALL
JNU_ThrowIOExceptionWithLastError(JNIEnv *env, const char *defaultDetail);

/* Convert between Java strings and i18n C strings */
JNIEXPORT const char *
GetStringPlatformChars(JNIEnv *env, jstring jstr, jboolean *isCopy);

JNIEXPORT jstring JNICALL
JNU_NewStringPlatform(JNIEnv *env, const char *str);

JNIEXPORT const char * JNICALL
JNU_GetStringPlatformChars(JNIEnv *env, jstring jstr, jboolean *isCopy);

JNIEXPORT void JNICALL
JNU_ReleaseStringPlatformChars(JNIEnv *env, jstring jstr, const char *str);

/* Class constants */
JNIEXPORT jclass JNICALL
JNU_ClassString(JNIEnv *env);

/* Copy count number of arguments from src to dst. Array bounds
 * and ArrayStoreException are checked.
 */
JNIEXPORT jint JNICALL
JNU_CopyObjectArray(JNIEnv *env, jobjectArray dst, jobjectArray src,
                    jint count);

/* Invoke a object-returning static method, based on class name,
 * method name, and signature string.
 *
 * The caller should check for exceptions by setting hasException
 * argument. If the caller is not interested in whether an exception
 * has occurred, pass in NULL.
 */
JNIEXPORT jvalue JNICALL
JNU_CallStaticMethodByName(JNIEnv *env,
                           jboolean *hasException,
                           const char *class_name,
                           const char *name,
                           const char *signature,
                           ...);

/* Invoke an instance method by name.
 */
JNIEXPORT jvalue JNICALL
JNU_CallMethodByName(JNIEnv *env,
                     jboolean *hasException,
                     jobject obj,
                     const char *name,
                     const char *signature,
                     ...);

JNIEXPORT jvalue JNICALL
JNU_CallMethodByNameV(JNIEnv *env,
                      jboolean *hasException,
                      jobject obj,
                      const char *name,
                      const char *signature,
                      va_list args);

/* Construct a new object of class, specifying the class by name,
 * and specififying which constructor to run and what arguments to
 * pass to it.
 *
 * The method will return an initialized instance if successful.
 * It will return NULL if an error has occurred (for example if
 * it ran out of memory) and the appropriate Java exception will
 * have been thrown.
 */
JNIEXPORT jobject JNICALL
JNU_NewObjectByName(JNIEnv *env, const char *class_name,
                    const char *constructor_sig, ...);

/* returns:
 * 0: object is not an instance of the class named by classname.
 * 1: object is an instance of the class named by classname.
 * -1: the class named by classname cannot be found. An exception
 * has been thrown.
 */
JNIEXPORT jint JNICALL
JNU_IsInstanceOfByName(JNIEnv *env, jobject object, const char *classname);


/* Get or set class and instance fields.
 * Note that set functions take a variable number of arguments,
 * but only one argument of the appropriate type can be passed.
 * For example, to set an integer field i to 100:
 *
 * JNU_SetFieldByName(env, &exc, obj, "i", "I", 100);
 *
 * To set a float field f to 12.3:
 *
 * JNU_SetFieldByName(env, &exc, obj, "f", "F", 12.3);
 *
 * The caller should check for exceptions by setting hasException
 * argument. If the caller is not interested in whether an exception
 * has occurred, pass in NULL.
 */
JNIEXPORT jvalue JNICALL
JNU_GetFieldByName(JNIEnv *env,
                   jboolean *hasException,
                   jobject obj,
                   const char *name,
                   const char *sig);
JNIEXPORT void JNICALL
JNU_SetFieldByName(JNIEnv *env,
                   jboolean *hasException,
                   jobject obj,
                   const char *name,
                   const char *sig,
                   ...);

JNIEXPORT jvalue JNICALL
JNU_GetStaticFieldByName(JNIEnv *env,
                         jboolean *hasException,
                         const char *classname,
                         const char *name,
                         const char *sig);


/************************************************************************
 * Miscellaneous utilities used by the class libraries
 */

#define IS_NULL(obj) ((obj) == NULL)
#define JNU_IsNull(env,obj) ((obj) == NULL)

/************************************************************************
 * Miscellaneous utilities used by the class libraries to return from
 * a function if a value is NULL or an exception is pending.
 */

#define CHECK_NULL(x)                           \
    do {                                        \
        if ((x) == NULL) {                      \
            return;                             \
        }                                       \
    } while (0)                                 \

#define CHECK_NULL_THROW_NPE(env, x, msg)         \
    do {                                        \
        if ((x) == NULL) {                      \
           JNU_ThrowNullPointerException((env), (msg));\
           return;                              \
        }                                       \
    } while(0)                                  \

#define CHECK_NULL_THROW_NPE_RETURN(env, x, msg, z)\
    do {                                        \
        if ((x) == NULL) {                      \
           JNU_ThrowNullPointerException((env), (msg));\
           return (z);                          \
        }                                       \
    } while(0)                                  \

#define CHECK_NULL_RETURN(x, y)                 \
    do {                                        \
        if ((x) == NULL) {                      \
            return (y);                         \
        }                                       \
    } while (0)                                 \

#ifdef __cplusplus
#define JNU_CHECK_EXCEPTION(env)                \
    do {                                        \
        if ((env)->ExceptionCheck()) {          \
            return;                             \
        }                                       \
    } while (0)                                 \

#define JNU_CHECK_EXCEPTION_RETURN(env, y)      \
    do {                                        \
        if ((env)->ExceptionCheck()) {          \
            return (y);                         \
        }                                       \
    } while (0)
#else
#define JNU_CHECK_EXCEPTION(env)                \
    do {                                        \
        if ((*env)->ExceptionCheck(env)) {      \
            return;                             \
        }                                       \
    } while (0)                                 \

#define JNU_CHECK_EXCEPTION_RETURN(env, y)      \
    do {                                        \
        if ((*env)->ExceptionCheck(env)) {      \
            return (y);                         \
        }                                       \
    } while (0)
#endif /* __cplusplus */

/************************************************************************
 * Debugging utilities
 */

JNIEXPORT jstring JNICALL
JNU_ToString(JNIEnv *env, jobject object);


/*
 * Package shorthand for use by native libraries
 */
#define JNU_JAVAPKG         "java/lang/"
#define JNU_JAVAIOPKG       "java/io/"
#define JNU_JAVANETPKG      "java/net/"

/*
 * Check if the current thread is attached to the VM, and returns
 * the JNIEnv of the specified version if the thread is attached.
 *
 * If the current thread is not attached, this function returns 0.
 *
 * If the current thread is attached, this function returns the
 * JNI environment, or returns (void *)JNI_ERR if the specified
 * version is not supported.
 */
JNIEXPORT void * JNICALL
JNU_GetEnv(JavaVM *vm, jint version);

/*
 * Warning free access to pointers stored in Java long fields.
 */
#define JNU_GetLongFieldAsPtr(env,obj,id) \
    (jlong_to_ptr((*(env))->GetLongField((env),(obj),(id))))
#define JNU_SetLongFieldFromPtr(env,obj,id,val) \
    (*(env))->SetLongField((env),(obj),(id),ptr_to_jlong(val))

/*
 * Internal use only.
 */
enum {
    NO_ENCODING_YET = 0,        /* "sun.jnu.encoding" not yet set */
    NO_FAST_ENCODING,           /* Platform encoding is not fast */
    FAST_8859_1,                /* ISO-8859-1 */
    FAST_CP1252,                /* MS-DOS Cp1252 */
    FAST_646_US,                /* US-ASCII : ISO646-US */
    FAST_UTF_8
};

JNIEXPORT void InitializeEncoding(JNIEnv *env, const char *name);

void* getProcessHandle();

void buildJniFunctionName(const char *sym, const char *cname,
                          char *jniEntryName);

JNIEXPORT size_t JNICALL
getLastErrorString(char *buf, size_t len);

JNIEXPORT int JNICALL
getErrorString(int err, char *buf, size_t len);

#ifdef STATIC_BUILD
/* Macros for handling declaration of static/dynamic
 * JNI library Load/Unload functions
 *
 * Use DEF_JNI_On{Un}Load when you want a static and non-static entry points.
 * Use DEF_STATIC_JNI_On{Un}Load when you only want a static one.
 *
 * LIBRARY_NAME must be set to the name of the library
 */

/* These three macros are needed to get proper concatenation of
 * the LIBRARY_NAME
 *
 * NOTE: LIBRARY_NAME must be set for static builds.
 */
#define ADD_LIB_NAME3(name, lib) name ## lib
#define ADD_LIB_NAME2(name, lib) ADD_LIB_NAME3(name, lib)
#define ADD_LIB_NAME(entry) ADD_LIB_NAME2(entry, LIBRARY_NAME)

#define DEF_JNI_OnLoad \
ADD_LIB_NAME(JNI_OnLoad_)(JavaVM *vm, void *reserved) \
{ \
  jint JNICALL ADD_LIB_NAME(JNI_OnLoad_dynamic_)(JavaVM *vm, void *reserved); \
  ADD_LIB_NAME(JNI_OnLoad_dynamic_)(vm, reserved); \
  return JNI_VERSION_1_8; \
} \
jint JNICALL ADD_LIB_NAME(JNI_OnLoad_dynamic_)

#define DEF_STATIC_JNI_OnLoad \
JNIEXPORT jint JNICALL ADD_LIB_NAME(JNI_OnLoad_)(JavaVM *vm, void *reserved) { \
    return JNI_VERSION_1_8; \
}

#define DEF_JNI_OnUnload \
ADD_LIB_NAME(JNI_OnUnload_)(JavaVM *vm, void *reserved) \
{ \
  void JNICALL ADD_LIB_NAME(JNI_OnUnload_dynamic_)(JavaVM *vm, void *reserved); \
  ADD_LIB_NAME(JNI_OnUnload_dynamic_)(vm, reserved); \
} \
void JNICALL ADD_LIB_NAME(JNI_OnUnload_dynamic_)

#define DEF_STATIC_JNI_OnUnload \
ADD_LIB_NAME(JNI_OnUnload_)

#else

#define DEF_JNI_OnLoad JNI_OnLoad
#define DEF_STATIC_JNI_OnLoad
#define DEF_JNI_OnUnload JNI_OnUnload
#define DEF_STATIC_JNI_OnUnload
#endif

#ifdef STATIC_BUILD
/* Macros for handling declaration of static/dynamic
 * Agent library Load/Attach/Unload functions
 *
 * Use DEF_Agent_OnLoad, DEF_Agent_OnAttach or DEF_Agent_OnUnload
 *     when you want both static and non-static entry points.
 * Use DEF_STATIC_Agent_OnLoad, DEF_STATIC_Agent_OnAttach or
 *     DEF_STATIC_Agent_OnUnload when you only want a static one.
 *
 * LIBRARY_NAME must be set to the name of the library for static builds.
 */

#define DEF_Agent_OnLoad \
ADD_LIB_NAME(Agent_OnLoad_)(JavaVM *vm, char *options, void *reserved) \
{ \
  jint JNICALL ADD_LIB_NAME(Agent_OnLoad_dynamic_)(JavaVM *vm, char *options, void *reserved); \
  return ADD_LIB_NAME(Agent_OnLoad_dynamic_)(vm, options, reserved); \
} \
jint JNICALL ADD_LIB_NAME(Agent_OnLoad_dynamic_)

#define DEF_STATIC_Agent_OnLoad \
JNIEXPORT jint JNICALL ADD_LIB_NAME(Agent_OnLoad_)(JavaVM *vm, char *options, void *reserved) { \
    return JNI_FALSE; \
}

#define DEF_Agent_OnAttach \
ADD_LIB_NAME(Agent_OnAttach_)(JavaVM *vm, char *options, void *reserved) \
{ \
  jint JNICALL ADD_LIB_NAME(Agent_OnAttach_dynamic_)(JavaVM *vm, char *options, void *reserved); \
  return ADD_LIB_NAME(Agent_OnAttach_dynamic_)(vm, options, reserved); \
} \
jint JNICALL ADD_LIB_NAME(Agent_OnAttach_dynamic_)

#define DEF_STATIC_Agent_OnAttach \
JNIEXPORT jint JNICALL ADD_LIB_NAME(Agent_OnLoad_)(JavaVM *vm, char *options, void *reserved) { \
    return JNI_FALSE; \
}

#define DEF_Agent_OnUnload \
ADD_LIB_NAME(Agent_OnUnload_)(JavaVM *vm) \
{ \
  void JNICALL ADD_LIB_NAME(Agent_OnUnload_dynamic_)(JavaVM *vm); \
  ADD_LIB_NAME(Agent_OnUnload_dynamic_)(vm); \
} \
void JNICALL ADD_LIB_NAME(Agent_OnUnload_dynamic_)

#define DEF_STATIC_Agent_OnUnload \
ADD_LIB_NAME(Agent_OnUnload_)

#else
#define DEF_Agent_OnLoad Agent_OnLoad
#define DEF_Agent_OnAttach Agent_OnAttach
#define DEF_Agent_OnUnload Agent_OnUnload
#define DEF_STATIC_Agent_OnLoad
#define DEF_STATIC_Agent_OnAttach
#define DEF_STATIC_Agent_OnUnload
#endif

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* JNI_UTIL_H */
