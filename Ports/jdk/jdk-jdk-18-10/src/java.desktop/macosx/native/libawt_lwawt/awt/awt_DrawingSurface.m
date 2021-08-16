/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

#import "JNIUtilities.h"

#import "AWTSurfaceLayers.h"


JNIEXPORT JAWT_DrawingSurfaceInfo* JNICALL awt_DrawingSurface_GetDrawingSurfaceInfo
(JAWT_DrawingSurface* ds)
{
    JAWT_DrawingSurfaceInfo* dsi = (JAWT_DrawingSurfaceInfo*)malloc(sizeof(JAWT_DrawingSurfaceInfo));

    JNIEnv *env = ds->env;
    jobject target = ds->target;

    DECLARE_CLASS_RETURN(jc_Component, "java/awt/Component", NULL);
    DECLARE_FIELD_RETURN(jf_peer, jc_Component, "peer", "Ljava/awt/peer/ComponentPeer;", NULL);
    jobject peer = (*env)->GetObjectField(env, target, jf_peer);

    DECLARE_CLASS_RETURN(jc_ComponentPeer, "sun/lwawt/LWComponentPeer", NULL);
    DECLARE_FIELD_RETURN(jf_platformComponent, jc_ComponentPeer,
                            "platformComponent", "Lsun/lwawt/PlatformComponent;", NULL);
    jobject platformComponent = (*env)->GetObjectField(env, peer, jf_platformComponent);

    DECLARE_CLASS_RETURN(jc_PlatformComponent, "sun/lwawt/macosx/CPlatformComponent", NULL);
    DECLARE_METHOD_RETURN(jm_getPointer, jc_PlatformComponent, "getPointer", "()J", NULL);
    AWTSurfaceLayers *surfaceLayers = jlong_to_ptr((*env)->CallLongMethod(env, platformComponent, jm_getPointer));
    // REMIND: assert(surfaceLayers)

    dsi->platformInfo = surfaceLayers;
    dsi->ds = ds;

    DECLARE_FIELD_RETURN(jf_x, jc_Component, "x", "I", NULL);
    DECLARE_FIELD_RETURN(jf_y, jc_Component, "y", "I", NULL);
    DECLARE_FIELD_RETURN(jf_width, jc_Component, "width", "I", NULL);
    DECLARE_FIELD_RETURN(jf_height, jc_Component, "height", "I", NULL);

    dsi->bounds.x = (*env)->GetIntField(env, target, jf_x);
    dsi->bounds.y = (*env)->GetIntField(env, target, jf_y);
    dsi->bounds.width = (*env)->GetIntField(env, target, jf_width);
    dsi->bounds.height = (*env)->GetIntField(env, target, jf_height);

    dsi->clipSize = 1;
    dsi->clip = &(dsi->bounds);

    return dsi;
}

JNIEXPORT jint JNICALL awt_DrawingSurface_Lock
(JAWT_DrawingSurface* ds)
{
    // TODO: implement
    return 0;
}

JNIEXPORT void JNICALL awt_DrawingSurface_Unlock
(JAWT_DrawingSurface* ds)
{
    // TODO: implement
}

JNIEXPORT void JNICALL awt_DrawingSurface_FreeDrawingSurfaceInfo
(JAWT_DrawingSurfaceInfo* dsi)
{
    free(dsi);
}

JNIEXPORT JAWT_DrawingSurface* JNICALL awt_GetDrawingSurface
(JNIEnv* env, jobject target)
{
    JAWT_DrawingSurface* ds = (JAWT_DrawingSurface*)malloc(sizeof(JAWT_DrawingSurface));

    // TODO: "target instanceof" check

    ds->env = env;
    ds->target = (*env)->NewGlobalRef(env, target);
    ds->Lock = awt_DrawingSurface_Lock;
    ds->GetDrawingSurfaceInfo = awt_DrawingSurface_GetDrawingSurfaceInfo;
    ds->FreeDrawingSurfaceInfo = awt_DrawingSurface_FreeDrawingSurfaceInfo;
    ds->Unlock = awt_DrawingSurface_Unlock;

    return ds;
}

JNIEXPORT void JNICALL awt_FreeDrawingSurface
(JAWT_DrawingSurface* ds)
{
    JNIEnv *env = ds->env;
    (*env)->DeleteGlobalRef(env, ds->target);
    free(ds);
}

JNIEXPORT void JNICALL awt_Lock
(JNIEnv* env)
{
    // TODO: implement
}

JNIEXPORT void JNICALL awt_Unlock
(JNIEnv* env)
{
    // TODO: implement
}

JNIEXPORT jobject JNICALL awt_GetComponent
(JNIEnv* env, void* platformInfo)
{
    // TODO: implement
    return NULL;
}

// EmbeddedFrame support

static char *const embeddedClassName = "sun/lwawt/macosx/CViewEmbeddedFrame";

JNIEXPORT jobject JNICALL awt_CreateEmbeddedFrame
(JNIEnv* env, void* platformInfo)
{
    static jmethodID mid = NULL;
    static jclass cls;
    if (mid == NULL) {
        cls = (*env)->FindClass(env, embeddedClassName);
        CHECK_NULL_RETURN(cls, NULL);
        mid = (*env)->GetMethodID(env, cls, "<init>", "(J)V");
        CHECK_NULL_RETURN(mid, NULL);
    }
    jobject o = (*env)->NewObject(env, cls, mid, platformInfo);
    CHECK_EXCEPTION();
    return o;
}

JNIEXPORT void JNICALL awt_SetBounds
(JNIEnv *env, jobject embeddedFrame, jint x, jint y, jint w, jint h)
{
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass cls = (*env)->FindClass(env, embeddedClassName);
        CHECK_NULL(cls);
        mid = (*env)->GetMethodID(env, cls, "setBoundsPrivate", "(IIII)V");
        CHECK_NULL(mid);
    }
    (*env)->CallVoidMethod(env, embeddedFrame, mid, x, y, w, h);
    CHECK_EXCEPTION();
}

JNIEXPORT void JNICALL awt_SynthesizeWindowActivation
(JNIEnv *env, jobject embeddedFrame, jboolean doActivate)
{
    static jmethodID mid = NULL;
    if (mid == NULL) {
        jclass cls = (*env)->FindClass(env, embeddedClassName);
        CHECK_NULL(cls);
        mid = (*env)->GetMethodID(env, cls, "synthesizeWindowActivation", "(Z)V");
        CHECK_NULL(mid);
    }
    (*env)->CallVoidMethod(env, embeddedFrame, mid, doActivate);
    CHECK_EXCEPTION();
}
