/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <dlfcn.h>

#include <jni.h>
#include <sizecalc.h>
#include "sun_awt_UNIXToolkit.h"

#include "awt.h"
#include "gtk_interface.h"

static jclass this_class = NULL;
static jmethodID icon_upcall_method = NULL;


/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    check_gtk
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_UNIXToolkit_check_1gtk(JNIEnv *env, jclass klass, jint version) {
    return (jboolean)gtk_check_version(version);
}


/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    load_gtk
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_UNIXToolkit_load_1gtk(JNIEnv *env, jclass klass, jint version,
                                                             jboolean verbose) {
    return (jboolean)gtk_load(env, version, verbose);
}


/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    unload_gtk
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_UNIXToolkit_unload_1gtk(JNIEnv *env, jclass klass)
{
    return (jboolean)gtk->unload();
}

jboolean init_method(JNIEnv *env, jobject this)
{
    if (this_class == NULL) {
        this_class = (*env)->NewGlobalRef(env,
                                          (*env)->GetObjectClass(env, this));
        icon_upcall_method = (*env)->GetMethodID(env, this_class,
                                 "loadIconCallback", "([BIIIIIZ)V");
        CHECK_NULL_RETURN(icon_upcall_method, JNI_FALSE);
    }
    return JNI_TRUE;
}

/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    load_gtk_icon
 * Signature: (Ljava/lang/String)Z
 *
 * This method assumes that GTK libs are present.
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_UNIXToolkit_load_1gtk_1icon(JNIEnv *env, jobject this,
        jstring filename)
{
    int len;
    jsize jlen;
    char *filename_str = NULL;
    GError **error = NULL;

    if (filename == NULL)
    {
        return JNI_FALSE;
    }

    len = (*env)->GetStringUTFLength(env, filename);
    jlen = (*env)->GetStringLength(env, filename);
    filename_str = (char *)SAFE_SIZE_ARRAY_ALLOC(malloc,
            sizeof(char), len + 1);
    if (filename_str == NULL) {
        JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
        return JNI_FALSE;
    }
    if (!init_method(env, this) ) {
        free(filename_str);
        return JNI_FALSE;
    }
    (*env)->GetStringUTFRegion(env, filename, 0, jlen, filename_str);
    jboolean result = gtk->get_file_icon_data(env, filename_str, error,
                                            icon_upcall_method, this);

    /* Release the strings we've allocated. */
    free(filename_str);

    return result;
}

/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    load_stock_icon
 * Signature: (ILjava/lang/String;IILjava/lang/String;)Z
 *
 * This method assumes that GTK libs are present.
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_UNIXToolkit_load_1stock_1icon(JNIEnv *env, jobject this,
        jint widget_type, jstring stock_id, jint icon_size,
        jint text_direction, jstring detail)
{
    int len;
    jsize jlen;
    char *stock_id_str = NULL;
    char *detail_str = NULL;
    jboolean result = JNI_FALSE;

    if (stock_id == NULL)
    {
        return JNI_FALSE;
    }

    len = (*env)->GetStringUTFLength(env, stock_id);
    jlen = (*env)->GetStringLength(env, stock_id);
    stock_id_str = (char *)SAFE_SIZE_ARRAY_ALLOC(malloc,
            sizeof(char), len + 1);
    if (stock_id_str == NULL) {
        JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
        return JNI_FALSE;
    }
    (*env)->GetStringUTFRegion(env, stock_id, 0, jlen, stock_id_str);

    /* Detail isn't required so check for NULL. */
    if (detail != NULL)
    {
        len = (*env)->GetStringUTFLength(env, detail);
        jlen = (*env)->GetStringLength(env, detail);
        detail_str = (char *)SAFE_SIZE_ARRAY_ALLOC(malloc,
                sizeof(char), len + 1);
        if (detail_str == NULL) {
            free(stock_id_str);
            JNU_ThrowOutOfMemoryError(env, "OutOfMemoryError");
            return JNI_FALSE;
        }
        (*env)->GetStringUTFRegion(env, detail, 0, jlen, detail_str);
    }

    if (init_method(env, this)) {
        result = gtk->get_icon_data(env, widget_type, stock_id_str,
                                    icon_size, text_direction, detail_str,
                                    icon_upcall_method, this);
    }
    /* Release the strings we've allocated. */
    free(stock_id_str);
    free(detail_str);

    return result;
}

/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    nativeSync
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_UNIXToolkit_nativeSync(JNIEnv *env, jobject this)
{
    AWT_LOCK();
    XSync(awt_display, False);
    AWT_UNLOCK();
}

/*
 * Class:     sun_awt_SunToolkit
 * Method:    closeSplashScreen
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_awt_SunToolkit_closeSplashScreen(JNIEnv *env, jclass cls)
{
    typedef void (*SplashClose_t)();
    SplashClose_t splashClose;
    void* hSplashLib = dlopen(0, RTLD_LAZY);
    if (!hSplashLib) {
        return;
    }
    splashClose = (SplashClose_t)dlsym(hSplashLib,
        "SplashClose");
    if (splashClose) {
        splashClose();
    }
    dlclose(hSplashLib);
}

/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    gtkCheckVersionImpl
 * Signature: (III)Ljava/lang/String;
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_UNIXToolkit_gtkCheckVersionImpl(JNIEnv *env, jobject this,
        jint major, jint minor, jint micro)
{
    char *ret;

    ret = gtk->gtk_check_version(major, minor, micro);
    if (ret == NULL) {
        return TRUE;
    }

    return FALSE;
}

/*
 * Class:     sun_awt_UNIXToolkit
 * Method:    get_gtk_version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_UNIXToolkit_get_1gtk_1version(JNIEnv *env, jclass klass)
{
    return gtk ? gtk->version : GTK_ANY;
}
