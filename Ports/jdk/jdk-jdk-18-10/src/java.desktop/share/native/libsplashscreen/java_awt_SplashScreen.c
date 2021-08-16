/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "splashscreen_impl.h"
#include <jlong_md.h>
#include <jni.h>
#include <jni_util.h>
#include <sizecalc.h>
#include "java_awt_SplashScreen.h"

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM * vm, void *reserved)
{
    return JNI_VERSION_1_2;
}

/* FIXME: safe_ExceptionOccured, why and how? */

/*
* Class:     java_awt_SplashScreen
* Method:    _update
* Signature: (J[IIIIII)V
*/
JNIEXPORT void JNICALL
Java_java_awt_SplashScreen__1update(JNIEnv * env, jclass thisClass,
                                    jlong jsplash, jintArray data,
                                    jint x, jint y, jint width, jint height,
                                    jint stride)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);
    int dataSize;

    if (!splash) {
        return;
    }
    SplashLock(splash);
    dataSize = (*env)->GetArrayLength(env, data);
    if (splash->overlayData) {
        free(splash->overlayData);
    }
    splash->overlayData = SAFE_SIZE_ARRAY_ALLOC(malloc, dataSize, sizeof(rgbquad_t));
    if (splash->overlayData) {
        /* we need a copy anyway, so we'll be using GetIntArrayRegion */
        (*env)->GetIntArrayRegion(env, data, 0, dataSize,
            (jint *) splash->overlayData);
        initFormat(&splash->overlayFormat, 0xFF0000, 0xFF00, 0xFF, 0xFF000000);
        initRect(&splash->overlayRect, x, y, width, height, 1,
            stride * sizeof(rgbquad_t), splash->overlayData,
            &splash->overlayFormat);
        SplashUpdate(splash);
    }
    SplashUnlock(splash);
}


/*
* Class:     java_awt_SplashScreen
* Method:    _isVisible
* Signature: (J)Z
*/
JNIEXPORT jboolean JNICALL
Java_java_awt_SplashScreen__1isVisible(JNIEnv * env, jclass thisClass,
                                       jlong jsplash)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);

    if (!splash) {
        return JNI_FALSE;
    }
    return splash->isVisible>0 ? JNI_TRUE : JNI_FALSE;
}

/*
* Class:     java_awt_SplashScreen
* Method:    _getBounds
* Signature: (J)Ljava/awt/Rectangle;
*/
JNIEXPORT jobject JNICALL
Java_java_awt_SplashScreen__1getBounds(JNIEnv * env, jclass thisClass,
                                       jlong jsplash)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);
    static jclass clazz = NULL;
    static jmethodID mid = NULL;
    jobject bounds = NULL;

    if (!splash) {
        return NULL;
    }
    SplashLock(splash);
    if (!clazz) {
        clazz = (*env)->FindClass(env, "java/awt/Rectangle");
        if (clazz) {
            clazz = (*env)->NewGlobalRef(env, clazz);
        }
    }
    if (clazz && !mid) {
        mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIII)V");
    }
    if (clazz && mid) {
        bounds = (*env)->NewObject(env, clazz, mid, splash->x, splash->y,
            splash->width, splash->height);
        if ((*env)->ExceptionOccurred(env)) {
            bounds = NULL;
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    SplashUnlock(splash);
    return bounds;
}

/*
* Class:     java_awt_SplashScreen
* Method:    _getInstance
* Signature: ()J
*/
JNIEXPORT jlong JNICALL
Java_java_awt_SplashScreen__1getInstance(JNIEnv * env, jclass thisClass)
{
    return ptr_to_jlong(SplashGetInstance());
}

/*
* Class:     java_awt_SplashScreen
* Method:    _close
* Signature: (J)V
*/
JNIEXPORT void JNICALL
Java_java_awt_SplashScreen__1close(JNIEnv * env, jclass thisClass,
                                   jlong jsplash)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);

    if (!splash) {
        return;
    }
    SplashLock(splash);
    SplashClosePlatform(splash);
    SplashUnlock(splash);
}

/*
 * Class:     java_awt_SplashScreen
 * Method:    _getImageFileName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_java_awt_SplashScreen__1getImageFileName
    (JNIEnv * env, jclass thisClass, jlong jsplash)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);


    if (!splash || !splash->fileName) {
        return NULL;
    }
    /* splash->fileName is of type char*, but in fact it contains jchars */
    return (*env)->NewString(env, (const jchar*)splash->fileName,
                             splash->fileNameLen);
}

/*
 * Class:     java_awt_SplashScreen
 * Method:    _getImageJarName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_java_awt_SplashScreen__1getImageJarName
  (JNIEnv * env, jclass thisClass, jlong jsplash)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);

    if (!splash || !splash->jarName) {
        return NULL;
    }
    /* splash->jarName is of type char*, but in fact it contains jchars */
    return (*env)->NewString(env, (const jchar*)splash->jarName,
                             splash->jarNameLen);
}

/*
 * Class:     java_awt_SplashScreen
 * Method:    _setImageData
 * Signature: (J[B)Z
 */
JNIEXPORT jboolean JNICALL Java_java_awt_SplashScreen__1setImageData
  (JNIEnv * env, jclass thisClass, jlong jsplash, jbyteArray data)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);
    int size, rc;
    jbyte* pBytes;

    if (!splash) {
        return JNI_FALSE;
    }
    pBytes = (*env)->GetByteArrayElements(env, data, NULL);
    CHECK_NULL_RETURN(pBytes, JNI_FALSE);
    size = (*env)->GetArrayLength(env, data);
    rc = SplashLoadMemory(pBytes, size);
    (*env)->ReleaseByteArrayElements(env, data, pBytes, JNI_ABORT);
    return rc ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     java_awt_SplashScreen
 * Method:    _getScaleFactor
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_java_awt_SplashScreen__1getScaleFactor
(JNIEnv *env, jclass thisClass, jlong jsplash)
{
    Splash *splash = (Splash *) jlong_to_ptr(jsplash);
    if (!splash) {
        return 1;
    }
    return splash->scaleFactor;
}
