/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <stdio.h>
#include <dlfcn.h>
#include <string.h>
#include <stdlib.h>
#include <jni.h>
#include <jni_util.h>
#include <jvm.h>
#include "gdefs.h"

#include <sys/param.h>
#include <sys/utsname.h>

#ifdef AIX
#include "porting_aix.h" /* For the 'dladdr' function. */
#endif

#ifdef DEBUG
#define VERBOSE_AWT_DEBUG
#endif

static void *awtHandle = NULL;

typedef jint JNICALL JNI_OnLoad_type(JavaVM *vm, void *reserved);

/* Initialize the Java VM instance variable when the library is
   first loaded */
JNIEXPORT JavaVM *jvm;

JNIEXPORT jboolean JNICALL AWTIsHeadless() {
    static JNIEnv *env = NULL;
    static jboolean isHeadless;
    jmethodID headlessFn;
    jclass graphicsEnvClass;

    if (env == NULL) {
        env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        graphicsEnvClass = (*env)->FindClass(env,
                                             "java/awt/GraphicsEnvironment");
        if (graphicsEnvClass == NULL) {
            return JNI_TRUE;
        }
        headlessFn = (*env)->GetStaticMethodID(env,
                                               graphicsEnvClass, "isHeadless", "()Z");
        if (headlessFn == NULL) {
            return JNI_TRUE;
        }
        isHeadless = (*env)->CallStaticBooleanMethod(env, graphicsEnvClass,
                                                     headlessFn);
        if ((*env)->ExceptionCheck(env)) {
            (*env)->ExceptionClear(env);
            return JNI_TRUE;
        }
    }
    return isHeadless;
}

#define CHECK_EXCEPTION_FATAL(env, message) \
    if ((*env)->ExceptionCheck(env)) { \
        (*env)->ExceptionClear(env); \
        (*env)->FatalError(env, message); \
    }

/*
 * Pathnames to the various awt toolkits
 */

#ifdef MACOSX
  #define LWAWT_PATH "/libawt_lwawt.dylib"
  #define DEFAULT_PATH LWAWT_PATH
#else
  #define XAWT_PATH "/libawt_xawt.so"
  #define DEFAULT_PATH XAWT_PATH
  #define HEADLESS_PATH "/libawt_headless.so"
#endif

jint
AWT_OnLoad(JavaVM *vm, void *reserved)
{
    Dl_info dlinfo;
    char buf[MAXPATHLEN];
    int32_t len;
    char *p, *tk;
    JNI_OnLoad_type *JNI_OnLoad_ptr;
    struct utsname name;
    JNIEnv *env = (JNIEnv *)JNU_GetEnv(vm, JNI_VERSION_1_2);
    void *v;
    jstring fmanager = NULL;
    jstring fmProp = NULL;

    if (awtHandle != NULL) {
        /* Avoid several loading attempts */
        return JNI_VERSION_1_2;
    }

    jvm = vm;
#ifndef STATIC_BUILD
    /* Get address of this library and the directory containing it. */
    dladdr((void *)AWT_OnLoad, &dlinfo);
    realpath((char *)dlinfo.dli_fname, buf);
    len = strlen(buf);
    p = strrchr(buf, '/');
#endif
    /*
     * The code below is responsible for:
     * 1. Loading appropriate awt library, i.e. libawt_xawt or libawt_headless
     * 2. Set the "sun.font.fontmanager" system property.
     */

    fmProp = (*env)->NewStringUTF(env, "sun.font.fontmanager");
    CHECK_EXCEPTION_FATAL(env, "Could not allocate font manager property");

#ifdef MACOSX
        fmanager = (*env)->NewStringUTF(env, "sun.font.CFontManager");
        tk = LWAWT_PATH;
#else
        fmanager = (*env)->NewStringUTF(env, "sun.awt.X11FontManager");
        tk = XAWT_PATH;
#endif
    CHECK_EXCEPTION_FATAL(env, "Could not allocate font manager name");

    if (fmanager && fmProp) {
        JNU_CallStaticMethodByName(env, NULL, "java/lang/System", "setProperty",
                                   "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                                   fmProp, fmanager);
        CHECK_EXCEPTION_FATAL(env, "Could not allocate set properties");
    }

#ifndef MACOSX
    if (AWTIsHeadless()) {
        tk = HEADLESS_PATH;
    }
#endif

#ifndef STATIC_BUILD
    /* Calculate library name to load */
    strncpy(p, tk, MAXPATHLEN-len-1);
#endif

    if (fmProp) {
        (*env)->DeleteLocalRef(env, fmProp);
    }
    if (fmanager) {
        (*env)->DeleteLocalRef(env, fmanager);
    }


#ifndef STATIC_BUILD
    jstring jbuf = JNU_NewStringPlatform(env, buf);
    CHECK_EXCEPTION_FATAL(env, "Could not allocate library name");
    JNU_CallStaticMethodByName(env, NULL, "java/lang/System", "load",
                               "(Ljava/lang/String;)V",
                               jbuf);

    awtHandle = dlopen(buf, RTLD_LAZY | RTLD_GLOBAL);
#endif
    return JNI_VERSION_1_2;
}

JNIEXPORT jint JNICALL
DEF_JNI_OnLoad(JavaVM *vm, void *reserved)
{
    return AWT_OnLoad(vm, reserved);
}
