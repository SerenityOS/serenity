/*
 * Copyright (c) 1995, 2014, Oracle and/or its affiliates. All rights reserved.
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

#include "awt_p.h"
#include "color.h"
#include <X11/Xatom.h>
#include <X11/Xmd.h>
#include <X11/Xutil.h>
#include <X11/Xproto.h>
#include <jni.h>
#include <jni_util.h>
#include <sys/time.h>


#include "java_awt_event_MouseWheelEvent.h"

/*
 * Called by "ToolkitErrorHandler" function in "XlibWrapper.c" file.
 */
XErrorHandler current_native_xerror_handler = NULL;

extern jint getModifiers(uint32_t state, jint button, jint keyCode);
extern jint getButton(uint32_t button);

static Atom OLDecorDelAtom = 0;
static Atom MWMHints = 0;
static Atom DTWMHints = 0;
static Atom decor_list[9];

#ifndef MAX
#define MAX(a,b) ((a) > (b) ? (a) : (b))
#endif

#ifndef MIN
#define MIN(a,b) ((a) < (b) ? (a) : (b))
#endif

jboolean
awtJNI_ThreadYield(JNIEnv *env) {

    static jclass threadClass = NULL;
    static jmethodID yieldMethodID = NULL;

    /* Initialize our java identifiers once. Checking before locking
     * is a huge performance win.
     */
    if (threadClass == NULL) {
        // should enter a monitor here...
        Boolean err = FALSE;
        if (threadClass == NULL) {
            jclass tc = (*env)->FindClass(env, "java/lang/Thread");
            CHECK_NULL_RETURN(tc, JNI_FALSE);
            threadClass = (*env)->NewGlobalRef(env, tc);
            (*env)->DeleteLocalRef(env, tc);
            if (threadClass != NULL) {
                yieldMethodID = (*env)->GetStaticMethodID(env,
                                              threadClass,
                                              "yield",
                                              "()V"
                                                );
            }
        }
        if (yieldMethodID == NULL) {
            threadClass = NULL;
            err = TRUE;
        }
        if (err) {
            return JNI_FALSE;
        }
    } /* threadClass == NULL*/

    (*env)->CallStaticVoidMethod(env, threadClass, yieldMethodID);
    DASSERT(!((*env)->ExceptionOccurred(env)));
    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
} /* awtJNI_ThreadYield() */
