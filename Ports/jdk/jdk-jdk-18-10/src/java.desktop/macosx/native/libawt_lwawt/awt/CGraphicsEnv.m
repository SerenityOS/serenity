/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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

#import "AWT_debug.h"

#import "JNIUtilities.h"
#import "ThreadUtilities.h"

#define MAX_DISPLAYS 64

/*
 * Class:     sun_awt_CGraphicsEnvironment
 * Method:    getDisplayIDs
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL
Java_sun_awt_CGraphicsEnvironment_getDisplayIDs
(JNIEnv *env, jclass class)
{
    jintArray ret = NULL;

JNI_COCOA_ENTER(env);

    /* Get the count */
    CGDisplayCount displayCount;
    if (CGGetOnlineDisplayList(MAX_DISPLAYS, NULL, &displayCount) != kCGErrorSuccess) {
        JNU_ThrowInternalError(env, "CGGetOnlineDisplayList() failed to get display count");
        return NULL;
    }

    /* Allocate an array and get the size list of display Ids */
    CGDirectDisplayID displays[MAX_DISPLAYS];
    if (CGGetOnlineDisplayList(displayCount, displays, &displayCount) != kCGErrorSuccess) {
        JNU_ThrowInternalError(env, "CGGetOnlineDisplayList() failed to get display list");
        return NULL;
    }

    CGDisplayCount i;
    CGDisplayCount displayActiveCount = 0; //Active and sleeping.
    for (i = 0; i < displayCount; ++i) {
        if (CGDisplayMirrorsDisplay(displays[i]) == kCGNullDirectDisplay) {
            ++displayActiveCount;
        } else {
            displays[i] = kCGNullDirectDisplay;
        }
    }

    /* Allocate a java array for display identifiers */
    ret = (*env)->NewIntArray(env, displayActiveCount);
    CHECK_NULL_RETURN(ret, NULL);

    /* Initialize and return the backing int array */
    assert(sizeof(jint) >= sizeof(CGDirectDisplayID));
    jint *elems = (*env)->GetIntArrayElements(env, ret, 0);
    CHECK_NULL_RETURN(elems, NULL);

    /* Filter out the mirrored displays */
    for (i = 0; i < displayCount; ++i) {
        if (displays[i] != kCGNullDirectDisplay) {
            elems[--displayActiveCount] = displays[i];
        }
    }

    (*env)->ReleaseIntArrayElements(env, ret, elems, 0);

JNI_COCOA_EXIT(env);

    return ret;
}

/*
 * Class:     sun_awt_CGraphicsEnvironment
 * Method:    getMainDisplayID
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_CGraphicsEnvironment_getMainDisplayID
(JNIEnv *env, jclass class)
{
    return CGMainDisplayID();
}

/*
 * Post the display reconfiguration event.
 */
static void displaycb_handle
(CGDirectDisplayID display, CGDisplayChangeSummaryFlags flags, void *userInfo)
{
    if (flags == kCGDisplayBeginConfigurationFlag) return;

    [ThreadUtilities performOnMainThreadWaiting:NO block:^() {

        JNIEnv *env = [ThreadUtilities getJNIEnvUncached];
        jobject cgeRef = (jobject)userInfo;

        jobject graphicsEnv = (*env)->NewLocalRef(env, cgeRef);
        if (graphicsEnv == NULL) return; // ref already GC'd
        DECLARE_CLASS(jc_CGraphicsEnvironment, "sun/awt/CGraphicsEnvironment");
        DECLARE_METHOD(jm_displayReconfiguration,
                jc_CGraphicsEnvironment, "_displayReconfiguration","(IZ)V");
        (*env)->CallVoidMethod(env, graphicsEnv, jm_displayReconfiguration,
                (jint) display, (jboolean) flags & kCGDisplayRemoveFlag);
        (*env)->DeleteLocalRef(env, graphicsEnv);
        CHECK_EXCEPTION();
    }];
}

/*
 * Class:     sun_awt_CGraphicsEnvironment
 * Method:    registerDisplayReconfiguration
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_awt_CGraphicsEnvironment_registerDisplayReconfiguration
(JNIEnv *env, jobject this)
{
    jlong ret = 0L;

JNI_COCOA_ENTER(env);

    jobject cgeRef = (*env)->NewWeakGlobalRef(env, this);

    /* Register the callback */
    if (CGDisplayRegisterReconfigurationCallback(&displaycb_handle, cgeRef) != kCGErrorSuccess) {
        JNU_ThrowInternalError(env, "CGDisplayRegisterReconfigurationCallback() failed");
        return 0L;
    }

    ret = ptr_to_jlong(cgeRef);

JNI_COCOA_EXIT(env);

    return ret;
}

/*
 * Class:     sun_awt_CGraphicsEnvironment
 * Method:    deregisterDisplayReconfiguration
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_CGraphicsEnvironment_deregisterDisplayReconfiguration
(JNIEnv *env, jobject this, jlong p)
{
JNI_COCOA_ENTER(env);

    jobject cgeRef = (jobject)jlong_to_ptr(p);
    if (!cgeRef) return;

    /* Remove the registration */
    if (CGDisplayRemoveReconfigurationCallback(&displaycb_handle, cgeRef) != kCGErrorSuccess) {
        JNU_ThrowInternalError(env, "CGDisplayRemoveReconfigurationCallback() failed, leaking the callback context!");
        return;
    }

    (*env)->DeleteWeakGlobalRef(env, cgeRef);

JNI_COCOA_EXIT(env);
}
