/*
 * Copyright (c) 1996, 2016, Oracle and/or its affiliates. All rights reserved.
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
#include "java_awt_Component.h"

#include "awt_Component.h"

#include <jni.h>
#include <jni_util.h>
#include <jawt_md.h>

extern struct ComponentIDs componentIDs;

#include "awt_GraphicsEnv.h"
extern jfieldID windowID;
extern jfieldID targetID;
extern jfieldID graphicsConfigID;
extern jfieldID drawStateID;
extern struct X11GraphicsConfigIDs x11GraphicsConfigIDs;

/*
 * Lock the surface of the target component for native rendering.
 * When finished drawing, the surface must be unlocked with
 * Unlock().  This function returns a bitmask with one or more of the
 * following values:
 *
 * JAWT_LOCK_ERROR - When an error has occurred and the surface could not
 * be locked.
 *
 * JAWT_LOCK_CLIP_CHANGED - When the clip region has changed.
 *
 * JAWT_LOCK_BOUNDS_CHANGED - When the bounds of the surface have changed.
 *
 * JAWT_LOCK_SURFACE_CHANGED - When the surface itself has changed
 */
JNIEXPORT jint JNICALL awt_DrawingSurface_Lock(JAWT_DrawingSurface* ds)
{
    JNIEnv* env;
    jobject target, peer;
    jclass componentClass;
    jint drawState;

    if (ds == NULL) {
#ifdef DEBUG
        fprintf(stderr, "Drawing Surface is NULL\n");
#endif
        return (jint)JAWT_LOCK_ERROR;
    }
    env = ds->env;
    target = ds->target;

    /* Make sure the target is a java.awt.Component */
    componentClass = (*env)->FindClass(env, "java/awt/Component");
    CHECK_NULL_RETURN(componentClass, (jint)JAWT_LOCK_ERROR);

    if (!(*env)->IsInstanceOf(env, target, componentClass)) {
#ifdef DEBUG
            fprintf(stderr, "Target is not a component\n");
#endif
        return (jint)JAWT_LOCK_ERROR;
        }

    if (!awtLockInited) {
        return (jint)JAWT_LOCK_ERROR;
    }
    AWT_LOCK();

    /* Get the peer of the target component */
    peer = (*env)->GetObjectField(env, target, componentIDs.peer);
    if (JNU_IsNull(env, peer)) {
#ifdef DEBUG
        fprintf(stderr, "Component peer is NULL\n");
#endif
                AWT_FLUSH_UNLOCK();
        return (jint)JAWT_LOCK_ERROR;
    }

   drawState = (*env)->GetIntField(env, peer, drawStateID);
    (*env)->SetIntField(env, peer, drawStateID, 0);
    return drawState;
}

JNIEXPORT int32_t JNICALL
    awt_GetColor(JAWT_DrawingSurface* ds, int32_t r, int32_t g, int32_t b)
{
    JNIEnv* env;
    jobject target, peer;
    jclass componentClass;
    AwtGraphicsConfigDataPtr adata;
    int32_t result;
     jobject gc_object;
    if (ds == NULL) {
#ifdef DEBUG
        fprintf(stderr, "Drawing Surface is NULL\n");
#endif
        return (int32_t) 0;
    }

    env = ds->env;
    target = ds->target;

    /* Make sure the target is a java.awt.Component */
    componentClass = (*env)->FindClass(env, "java/awt/Component");
    CHECK_NULL_RETURN(componentClass, (int32_t) 0);

    if (!(*env)->IsInstanceOf(env, target, componentClass)) {
#ifdef DEBUG
        fprintf(stderr, "DrawingSurface target must be a component\n");
#endif
        return (int32_t) 0;
    }

    if (!awtLockInited) {
        return (int32_t) 0;
    }

    AWT_LOCK();

    /* Get the peer of the target component */
    peer = (*env)->GetObjectField(env, target, componentIDs.peer);
    if (JNU_IsNull(env, peer)) {
#ifdef DEBUG
        fprintf(stderr, "Component peer is NULL\n");
#endif
        AWT_UNLOCK();
        return (int32_t) 0;
    }
     /* GraphicsConfiguration object of MComponentPeer */
    gc_object = (*env)->GetObjectField(env, peer, graphicsConfigID);

    if (gc_object != NULL) {
        adata = (AwtGraphicsConfigDataPtr)
            JNU_GetLongFieldAsPtr(env, gc_object,
                                  x11GraphicsConfigIDs.aData);
    } else {
        adata = getDefaultConfig(DefaultScreen(awt_display));
    }

    result = adata->AwtColorMatch(r, g, b, adata);
        AWT_UNLOCK();
        return result;
}

/*
 * Get the drawing surface info.
 * The value returned may be cached, but the values may change if
 * additional calls to Lock() or Unlock() are made.
 * Lock() must be called before this can return a valid value.
 * Returns NULL if an error has occurred.
 * When finished with the returned value, FreeDrawingSurfaceInfo must be
 * called.
 */
JNIEXPORT JAWT_DrawingSurfaceInfo* JNICALL
awt_DrawingSurface_GetDrawingSurfaceInfo(JAWT_DrawingSurface* ds)
{
    JNIEnv* env;
    jobject target, peer;
    jclass componentClass;
    JAWT_X11DrawingSurfaceInfo* px;
    JAWT_DrawingSurfaceInfo* p;
    XWindowAttributes attrs;

    if (ds == NULL) {
#ifdef DEBUG
        fprintf(stderr, "Drawing Surface is NULL\n");
#endif
        return NULL;
    }

    env = ds->env;
    target = ds->target;

    /* Make sure the target is a java.awt.Component */
    componentClass = (*env)->FindClass(env, "java/awt/Component");
    CHECK_NULL_RETURN(componentClass, NULL);

    if (!(*env)->IsInstanceOf(env, target, componentClass)) {
#ifdef DEBUG
        fprintf(stderr, "DrawingSurface target must be a component\n");
#endif
        return NULL;
        }

    if (!awtLockInited) {
        return NULL;
    }

    AWT_LOCK();

    /* Get the peer of the target component */
    peer = (*env)->GetObjectField(env, target, componentIDs.peer);
    if (JNU_IsNull(env, peer)) {
#ifdef DEBUG
        fprintf(stderr, "Component peer is NULL\n");
#endif
                AWT_UNLOCK();
        return NULL;
    }

    AWT_UNLOCK();

    /* Allocate platform-specific data */
    px = (JAWT_X11DrawingSurfaceInfo*)
        malloc(sizeof(JAWT_X11DrawingSurfaceInfo));

    /* Set drawable and display */
    px->drawable = (*env)->GetLongField(env, peer, windowID);
    px->display = awt_display;

    /* Get window attributes to set other values */
    XGetWindowAttributes(awt_display, (Window)(px->drawable), &attrs);

    /* Set the other values */
    px->visualID = XVisualIDFromVisual(attrs.visual);
    px->colormapID = attrs.colormap;
    px->depth = attrs.depth;
    px->GetAWTColor = awt_GetColor;

    /* Allocate and initialize platform-independent data */
    p = (JAWT_DrawingSurfaceInfo*)malloc(sizeof(JAWT_DrawingSurfaceInfo));
    p->platformInfo = px;
    p->ds = ds;
    p->bounds.x = (*env)->GetIntField(env, target, componentIDs.x);
    p->bounds.y = (*env)->GetIntField(env, target, componentIDs.y);
    p->bounds.width = (*env)->GetIntField(env, target, componentIDs.width);
    p->bounds.height = (*env)->GetIntField(env, target, componentIDs.height);
    p->clipSize = 1;
    p->clip = &(p->bounds);

    /* Return our new structure */
    return p;
}

/*
 * Free the drawing surface info.
 */
JNIEXPORT void JNICALL
awt_DrawingSurface_FreeDrawingSurfaceInfo(JAWT_DrawingSurfaceInfo* dsi)
{
    if (dsi == NULL ) {
#ifdef DEBUG
        fprintf(stderr, "Drawing Surface Info is NULL\n");
#endif
        return;
    }
    free(dsi->platformInfo);
    free(dsi);
}

/*
 * Unlock the drawing surface of the target component for native rendering.
 */
JNIEXPORT void JNICALL awt_DrawingSurface_Unlock(JAWT_DrawingSurface* ds)
{
    JNIEnv* env;
    if (ds == NULL) {
#ifdef DEBUG
        fprintf(stderr, "Drawing Surface is NULL\n");
#endif
        return;
    }
    env = ds->env;
    AWT_FLUSH_UNLOCK();
}

JNIEXPORT JAWT_DrawingSurface* JNICALL
    awt_GetDrawingSurface(JNIEnv* env, jobject target)
{
    jclass componentClass;
    JAWT_DrawingSurface* p;

    /* Make sure the target component is a java.awt.Component */
    componentClass = (*env)->FindClass(env, "java/awt/Component");
    CHECK_NULL_RETURN(componentClass, NULL);

    if (!(*env)->IsInstanceOf(env, target, componentClass)) {
#ifdef DEBUG
        fprintf(stderr,
            "GetDrawingSurface target must be a java.awt.Component\n");
#endif
        return NULL;
    }

    p = (JAWT_DrawingSurface*)malloc(sizeof(JAWT_DrawingSurface));
    p->env = env;
    p->target = (*env)->NewGlobalRef(env, target);
    p->Lock = awt_DrawingSurface_Lock;
    p->GetDrawingSurfaceInfo = awt_DrawingSurface_GetDrawingSurfaceInfo;
    p->FreeDrawingSurfaceInfo = awt_DrawingSurface_FreeDrawingSurfaceInfo;
    p->Unlock = awt_DrawingSurface_Unlock;
    return p;
}

JNIEXPORT void JNICALL
    awt_FreeDrawingSurface(JAWT_DrawingSurface* ds)
{
    JNIEnv* env;

    if (ds == NULL ) {
#ifdef DEBUG
        fprintf(stderr, "Drawing Surface is NULL\n");
#endif
        return;
    }
    env = ds->env;
    (*env)->DeleteGlobalRef(env, ds->target);
    free(ds);
}

JNIEXPORT void JNICALL
    awt_Lock(JNIEnv* env)
{
    if (awtLockInited) {
        AWT_LOCK();
    }
}

JNIEXPORT void JNICALL
    awt_Unlock(JNIEnv* env)
{
    if (awtLockInited) {
        AWT_FLUSH_UNLOCK();
    }
}

JNIEXPORT jobject JNICALL
    awt_GetComponent(JNIEnv* env, void* platformInfo)
{
    Window window = (Window)platformInfo;
    jobject peer = NULL;
    jobject target = NULL;

    AWT_LOCK();

    if (window != None) {
        peer = JNU_CallStaticMethodByName(env, NULL, "sun/awt/X11/XToolkit",
            "windowToXWindow", "(J)Lsun/awt/X11/XBaseWindow;", (jlong)window).l;
        if ((*env)->ExceptionCheck(env)) {
            AWT_UNLOCK();
            return (jobject)NULL;
        }
    }
    if ((peer != NULL) &&
        (JNU_IsInstanceOfByName(env, peer, "sun/awt/X11/XWindow") == 1)) {
        target = (*env)->GetObjectField(env, peer, targetID);
    }

    if (target == NULL) {
        (*env)->ExceptionClear(env);
        JNU_ThrowNullPointerException(env, "NullPointerException");
        AWT_UNLOCK();
        return (jobject)NULL;
    }

    AWT_UNLOCK();

    return target;
}

// EmbeddedFrame support

static char *const embeddedClassName = "sun/awt/X11/XEmbeddedFrame";

JNIEXPORT jobject JNICALL awt_CreateEmbeddedFrame
(JNIEnv* env, void* platformInfo)
{
    static jmethodID mid = NULL;
    static jclass cls;
    if (mid == NULL) {
        cls = (*env)->FindClass(env, embeddedClassName);
        CHECK_NULL_RETURN(cls, NULL);
        mid = (*env)->GetMethodID(env, cls, "<init>", "(JZ)V");
        CHECK_NULL_RETURN(mid, NULL);
    }
    return (*env)->NewObject(env, cls, mid, platformInfo, JNI_TRUE);
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
}
