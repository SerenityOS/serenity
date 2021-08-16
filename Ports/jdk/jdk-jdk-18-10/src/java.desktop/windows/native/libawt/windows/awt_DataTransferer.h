/*
 * Copyright (c) 2000, 2006, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_DATATRANSFERER_H
#define AWT_DATATRANSFERER_H

#include "stdhdrs.h"
struct IUnknown;

class AwtDataTransferer {
  public:
    static jobject GetDataTransferer(JNIEnv* env);
    static jbyteArray ConvertData(JNIEnv* env, jobject source, jobject contents,
                                  jlong format, jobject formatMap);
    static jobject ConcatData(JNIEnv* env, jobject obj1, jobject obj2);

    static jbyteArray GetPaletteBytes(HGDIOBJ hGdiObj, DWORD dwGdiObjType,
                                      BOOL bFailSafe);
    static jbyteArray LCIDToTextEncoding(JNIEnv *env, LCID lcid);
    static void SecondaryMessageLoop();
};

/*
 * NOTE: You need these macros only if you take care of performance, since they
 * provide proper caching. Otherwise you can use JNU_CallMethodByName etc.
 */

/*
 * This macro defines a function which returns the class for the specified
 * class name with proper caching and error handling.
 */
#define DECLARE_JAVA_CLASS(javaclazz, name)                                    \
static jclass                                                                  \
get_ ## javaclazz(JNIEnv* env) {                                               \
    static jclass javaclazz = NULL;                                            \
                                                                               \
    if (JNU_IsNull(env, javaclazz)) {                                          \
        jclass javaclazz ## Local = env->FindClass(name);                      \
                                                                               \
        if (!JNU_IsNull(env, javaclazz ## Local)) {                            \
            javaclazz = (jclass)env->NewGlobalRef(javaclazz ## Local);         \
            env->DeleteLocalRef(javaclazz ## Local);                           \
            if (JNU_IsNull(env, javaclazz)) {                                  \
                JNU_ThrowOutOfMemoryError(env, "");                            \
            }                                                                  \
        }                                                                      \
                                                                               \
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {                   \
            env->ExceptionDescribe();                                          \
            env->ExceptionClear();                                             \
        }                                                                      \
    }                                                                          \
                                                                               \
    DASSERT(!JNU_IsNull(env, javaclazz));                                      \
                                                                               \
    return javaclazz;                                                          \
}

/*
 * The following macros defines blocks of code which retrieve a method of the
 * specified class identified with the specified name and signature.
 * The specified class should be previously declared with DECLARE_JAVA_CLASS.
 * These macros should be placed at the beginning of a block, after definition
 * of local variables, but before the code begins.
 */
#define DECLARE_VOID_JAVA_METHOD(method, javaclazz, name, signature)           \
    static jmethodID method = NULL;                                            \
                                                                               \
    if (JNU_IsNull(env, method)) {                                             \
        jclass clazz = get_ ## javaclazz(env);                                 \
                                                                               \
        if (JNU_IsNull(env, clazz)) {                                          \
            return;                                                            \
        }                                                                      \
                                                                               \
        method = env->GetMethodID(clazz, name, signature);                     \
                                                                               \
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {                   \
            env->ExceptionDescribe();                                          \
            env->ExceptionClear();                                             \
        }                                                                      \
                                                                               \
        if (JNU_IsNull(env, method)) {                                         \
            DASSERT(FALSE);                                                    \
            return;                                                            \
        }                                                                      \
    }

#define DECLARE_JINT_JAVA_METHOD(method, javaclazz, name, signature)           \
    static jmethodID method = NULL;                                            \
                                                                               \
    if (JNU_IsNull(env, method)) {                                             \
        jclass clazz = get_ ## javaclazz(env);                                 \
                                                                               \
        if (JNU_IsNull(env, clazz)) {                                          \
            return java_awt_dnd_DnDConstants_ACTION_NONE;                      \
        }                                                                      \
                                                                               \
        method = env->GetMethodID(clazz, name, signature);                     \
                                                                               \
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {                   \
            env->ExceptionDescribe();                                          \
            env->ExceptionClear();                                             \
        }                                                                      \
                                                                               \
        if (JNU_IsNull(env, method)) {                                         \
            DASSERT(FALSE);                                                    \
            return java_awt_dnd_DnDConstants_ACTION_NONE;                      \
        }                                                                      \
    }

#define DECLARE_OBJECT_JAVA_METHOD(method, javaclazz, name, signature)         \
    static jmethodID method = NULL;                                            \
                                                                               \
    if (JNU_IsNull(env, method)) {                                             \
        jclass clazz = get_ ## javaclazz(env);                                 \
                                                                               \
        if (JNU_IsNull(env, clazz)) {                                          \
            return NULL;                                                       \
        }                                                                      \
                                                                               \
        method = env->GetMethodID(clazz, name, signature);                     \
                                                                               \
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {                   \
            env->ExceptionDescribe();                                          \
            env->ExceptionClear();                                             \
        }                                                                      \
                                                                               \
        if (JNU_IsNull(env, method)) {                                         \
            DASSERT(FALSE);                                                    \
            return NULL;                                                       \
        }                                                                      \
    }

#define DECLARE_STATIC_OBJECT_JAVA_METHOD(method, javaclazz, name, signature)  \
    static jmethodID method = NULL;                                            \
    jclass clazz = get_ ## javaclazz(env);                                     \
                                                                               \
    if (JNU_IsNull(env, clazz)) {                                              \
        return NULL;                                                           \
    }                                                                          \
                                                                               \
    if (JNU_IsNull(env, method)) {                                             \
        method = env->GetStaticMethodID(clazz, name, signature);               \
                                                                               \
        if (!JNU_IsNull(env, safe_ExceptionOccurred(env))) {                   \
            env->ExceptionDescribe();                                          \
            env->ExceptionClear();                                             \
        }                                                                      \
                                                                               \
        if (JNU_IsNull(env, method)) {                                         \
            DASSERT(FALSE);                                                    \
            return NULL;                                                       \
        }                                                                      \
    }

#endif /* AWT_DATATRANSFERER_H */
