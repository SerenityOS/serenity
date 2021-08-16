/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "sun_java2d_windows_GDIWindowSurfaceData.h"

#include "GDIWindowSurfaceData.h"
#include "GraphicsPrimitiveMgr.h"
#include "Region.h"
#include "Disposer.h"
#include "WindowsFlags.h"
#include "awt_Component.h"
#include "awt_Palette.h"
#include "awt_Win32GraphicsDevice.h"
#include "gdefs.h"
#include "Trace.h"
#include "Devices.h"

#include "jni_util.h"

static LockFunc GDIWinSD_Lock;
static GetRasInfoFunc GDIWinSD_GetRasInfo;
static UnlockFunc GDIWinSD_Unlock;
static DisposeFunc GDIWinSD_Dispose;
static SetupFunc GDIWinSD_Setup;
static GetDCFunc GDIWinSD_GetDC;
static ReleaseDCFunc GDIWinSD_ReleaseDC;
static InvalidateSDFunc GDIWinSD_InvalidateSD;

static HBRUSH   nullbrush;
static HPEN     nullpen;

static jclass xorCompClass;

static jboolean beingShutdown = JNI_FALSE;
static volatile LONG timeStamp = 0;
extern CriticalSection windowMoveLock;

extern "C"
{
GeneralDisposeFunc DisposeThreadGraphicsInfo;
jobject JNI_GetCurrentThread(JNIEnv *env);
int threadInfoIndex = TLS_OUT_OF_INDEXES;

static jclass threadClass = NULL;
static jmethodID currentThreadMethodID = NULL;

void SetupThreadGraphicsInfo(JNIEnv *env, GDIWinSDOps *wsdo) {
    J2dTraceLn(J2D_TRACE_INFO, "SetupThreadGraphicsInfo");

    // REMIND: handle error when creation fails
    ThreadGraphicsInfo *info =
        (ThreadGraphicsInfo*)TlsGetValue(threadInfoIndex);
    if (info == NULL) {
        info = new ThreadGraphicsInfo;
        ZeroMemory(info, sizeof(ThreadGraphicsInfo));
        TlsSetValue(threadInfoIndex, (LPVOID)info);
        J2dTraceLn2(J2D_TRACE_VERBOSE,
                    "  current batch limit for thread 0x%x is %d",
                     GetCurrentThreadId(), ::GdiGetBatchLimit());
        J2dTraceLn(J2D_TRACE_VERBOSE, "  setting to the limit to 1");
        // Fix for bug 4374079
        ::GdiSetBatchLimit(1);

        Disposer_AddRecord(env, JNI_GetCurrentThread(env),
                           DisposeThreadGraphicsInfo,
                           ptr_to_jlong(info));
    }

    HDC oldhDC = info->hDC;
    // the hDC is NULL for offscreen surfaces - we don't store it
    // in TLS as it must be created new every time.

    if( ((oldhDC == NULL) && wsdo->window != NULL) ||
         (info->wsdo != wsdo) ||
         (info->wsdoTimeStamp != wsdo->timeStamp) )
    {

        // Init graphics state, either because this is our first time
        // using it in this thread or because this thread is now
        // dealing with a different window than it was last time.

        //check extra condition:
        //(info->wsdoTimeStamp != wsdo->timeStamp).
        //Checking memory addresses (info->wsdo != wsdo) will not detect
        //that wsdo points to a newly allocated structure in case
        //that structure just got allocated at a "recycled" memory location
        //which previously was pointed by info->wsdo
        //see bug# 6859086

        // Release cached DC. We use deferred DC releasing mechanism because
        // the DC is associated with cached wsdo and component peer,
        // which may've been disposed by this time, and we have
        // no means of checking against it.
        if (oldhDC != NULL) {
            MoveDCToPassiveList(oldhDC, info->hWnd);
            info->hDC = NULL;
            info->hWnd = NULL;
        }

        if (wsdo->window != NULL){
            HDC hDC;
            // This is a window surface
            // First, init the HDC object
            AwtComponent *comp = GDIWindowSurfaceData_GetComp(env, wsdo);
            if (comp == NULL) {
                // wsdo->invalid is set by GDIWindowSurfaceData_GetComp
                return;
            }
            hDC = comp->GetDCFromComponent();
            if (hDC == NULL) {
                wsdo->invalid = JNI_TRUE;
                return;
            }
            if (hDC != NULL && wsdo->device != NULL) {
                ::SelectObject(hDC, nullbrush);
                ::SelectObject(hDC, nullpen);
                ::SelectClipRgn(hDC, (HRGN) NULL);
                ::SetROP2(hDC, R2_COPYPEN);
                wsdo->device->SelectPalette(hDC);
                // Note that on NT4 we don't need to do a realize here: the
                // palette-sharing takes care of color issues for us.  But
                // on win98 if we don't realize a DC's palette, that
                // palette does not appear to have correct access to the
                // logical->system mapping.
                wsdo->device->RealizePalette(hDC);

                // Second, init the rest of the graphics state
                ::GetClientRect(wsdo->window, &info->bounds);
                // Make window-relative from client-relative
                ::OffsetRect(&info->bounds, wsdo->insets.left, wsdo->insets.top);
                //Likewise, translate GDI calls from client-relative to window-relative
                ::OffsetViewportOrgEx(hDC, -wsdo->insets.left, -wsdo->insets.top, NULL);
            }

            // Finally, set these new values in the info for this thread
            info->hDC = hDC;
            info->hWnd = wsdo->window;
        }

        // cached brush and pen are not associated with any DC, and can be
        // reused, but have to set type to 0 to indicate that no pen/brush
        // were set to the new hdc
        info->type = 0;

        if (info->clip != NULL) {
            env->DeleteWeakGlobalRef(info->clip);
        }
        info->clip = NULL;

        if (info->comp != NULL) {
            env->DeleteWeakGlobalRef(info->comp);
        }
        info->comp = NULL;

        info->xorcolor = 0;
        info->patrop = PATCOPY;

        //store the address and time stamp of newly allocated GDIWinSDOps structure
        info->wsdo = wsdo;
        info->wsdoTimeStamp = wsdo->timeStamp;
    }
}

/**
 * Releases native data stored in Thread local storage.
 * Called by the Disposer when the associated thread dies.
 */
void DisposeThreadGraphicsInfo(JNIEnv *env, jlong tgi) {
    J2dTraceLn(J2D_TRACE_INFO, "DisposeThreadGraphicsInfo");
    ThreadGraphicsInfo *info = (ThreadGraphicsInfo*)jlong_to_ptr(tgi);
    if (info != NULL) {
        if (info->hDC != NULL) {
            // move the DC from the active dcs list to
            // the passive dc list to be released later
            MoveDCToPassiveList(info->hDC, info->hWnd);
        }

        if (info->clip != NULL) {
            env->DeleteWeakGlobalRef(info->clip);
        }
        if (info->comp != NULL) {
            env->DeleteWeakGlobalRef(info->comp);
        }

        if (info->brush != NULL) {
            info->brush->Release();
        }
        if (info->pen != NULL) {
            info->pen->Release();
        }

        delete info;
    }
}

/**
 * Returns current Thread object.
 */
jobject
JNI_GetCurrentThread(JNIEnv *env) {
    return env->CallStaticObjectMethod(threadClass, currentThreadMethodID);
} /* JNI_GetCurrentThread() */

/**
 * Return the data associated with this thread.
 * NOTE: This function assumes that the SetupThreadGraphicsInfo()
 * function has already been called for this situation (thread,
 * window, etc.), so we can assume that the thread info contains
 * a valid hDC.  This should usually be the case since GDIWinSD_Setup
 * is called as part of the GetOps() process.
 */
ThreadGraphicsInfo *GetThreadGraphicsInfo(JNIEnv *env,
                                          GDIWinSDOps *wsdo) {
    return (ThreadGraphicsInfo*)TlsGetValue(threadInfoIndex);
}

__inline HDC GetThreadDC(JNIEnv *env, GDIWinSDOps *wsdo) {
    ThreadGraphicsInfo *info =
        (ThreadGraphicsInfo *)GetThreadGraphicsInfo(env, wsdo);
    if (!info) {
        return (HDC) NULL;
    }
    return info->hDC;
}

} // extern "C"

/**
 * This source file contains support code for loops using the
 * SurfaceData interface to talk to a Win32 drawable from native
 * code.
 */

static BOOL GDIWinSD_CheckMonitorArea(GDIWinSDOps *wsdo,
                                     SurfaceDataBounds *bounds,
                                     HDC hDC)
{
    HWND hW = wsdo->window;
    BOOL retCode = TRUE;

    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_CheckMonitorArea");
    int numScreens;
    {
        Devices::InstanceAccess devices;
        numScreens = devices->GetNumDevices();
    }
    if( numScreens > 1 ) {

        LPMONITORINFO miInfo;
        RECT rSect ={0,0,0,0};
        RECT rView ={bounds->x1, bounds->y1, bounds->x2, bounds->y2};
        retCode = FALSE;

        miInfo = wsdo->device->GetMonitorInfo();

        POINT ptOrig = {0, 0};
        ::ClientToScreen(hW, &ptOrig);
        ::OffsetRect(&rView,
            (ptOrig.x), (ptOrig.y));

        ::IntersectRect(&rSect,&rView,&(miInfo->rcMonitor));

        if( FALSE == ::IsRectEmpty(&rSect) ) {
            if( TRUE == ::EqualRect(&rSect,&rView) ) {
                retCode = TRUE;
            }
        }
    }
    return retCode;
}

extern "C" {

void
initThreadInfoIndex()
{
    if (threadInfoIndex == TLS_OUT_OF_INDEXES) {
        threadInfoIndex = TlsAlloc();
    }
}


/**
 * Utility function to make sure that native and java-level
 * surface depths are matched.  They can be mismatched when display-depths
 * change, either between the creation of the Java surfaceData structure
 * and the native ddraw surface, or later when a surface is automatically
 * adjusted to be the new display depth (even if it was created in a different
 * depth to begin with)
 */
BOOL SurfaceDepthsCompatible(int javaDepth, int nativeDepth)
{
    if (nativeDepth != javaDepth) {
        switch (nativeDepth) {
        case 0: // Error condition: something is wrong with the surface
        case 8:
        case 24:
            // Java and native surface depths should match exactly for
            // these cases
            return FALSE;
            break;
        case 16:
            // Java surfaceData should be 15 or 16 bits
            if (javaDepth < 15 || javaDepth > 16) {
                return FALSE;
            }
            break;
        case 32:
            // Could have this native depth for either 24- or 32-bit
            // Java surfaceData
            if (javaDepth != 24 && javaDepth != 32) {
                return FALSE;
            }
            break;
        default:
            // should not get here, but if we do something is odd, so
            // just register a failure
            return FALSE;
        }
    }
    return TRUE;
}


/*
 * Class:     sun_java2d_windows_GDIWindowSurfaceData
 * Method:    initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIWindowSurfaceData_initIDs(JNIEnv *env, jclass wsd,
                                                 jclass XORComp)
{
    jclass tc;
    J2dTraceLn(J2D_TRACE_INFO, "GDIWindowSurfaceData_initIDs");
    nullbrush = (HBRUSH) ::GetStockObject(NULL_BRUSH);
    nullpen = (HPEN) ::GetStockObject(NULL_PEN);

    initThreadInfoIndex();

    xorCompClass = (jclass)env->NewGlobalRef(XORComp);
    if (env->ExceptionCheck()) {
        return;
    }

    tc = env->FindClass("java/lang/Thread");
    DASSERT(tc != NULL);
    CHECK_NULL(tc);

    threadClass = (jclass)env->NewGlobalRef(tc);
    DASSERT(threadClass != NULL);
    CHECK_NULL(threadClass);

    currentThreadMethodID =
        env->GetStaticMethodID(threadClass,
                               "currentThread",  "()Ljava/lang/Thread;");
    DASSERT(currentThreadMethodID != NULL);
}

#undef ExceptionOccurred

/*
 * Class:     sun_java2d_windows_GDIWindowSurfaceData
 * Method:    initOps
 * Signature: (Ljava/lang/Object;IIIIII)V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIWindowSurfaceData_initOps(JNIEnv *env, jobject wsd,
                                                 jobject peer, jint depth,
                                                 jint redMask, jint greenMask,
                                                 jint blueMask, jint screen)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWindowSurfaceData_initOps");
    GDIWinSDOps *wsdo = (GDIWinSDOps *)SurfaceData_InitOps(env, wsd, sizeof(GDIWinSDOps));
    if (wsdo == NULL) {
        JNU_ThrowOutOfMemoryError(env, "Initialization of SurfaceData failed.");
        return;
    }
    wsdo->timeStamp = InterlockedIncrement(&timeStamp); //creation time stamp
    wsdo->sdOps.Lock = GDIWinSD_Lock;
    wsdo->sdOps.GetRasInfo = GDIWinSD_GetRasInfo;
    wsdo->sdOps.Unlock = GDIWinSD_Unlock;
    wsdo->sdOps.Dispose = GDIWinSD_Dispose;
    wsdo->sdOps.Setup = GDIWinSD_Setup;
    wsdo->GetDC = GDIWinSD_GetDC;
    wsdo->ReleaseDC = GDIWinSD_ReleaseDC;
    wsdo->InvalidateSD = GDIWinSD_InvalidateSD;
    wsdo->invalid = JNI_FALSE;
    wsdo->lockType = WIN32SD_LOCK_UNLOCKED;
    wsdo->peer = env->NewWeakGlobalRef(peer);
    if (env->ExceptionOccurred()) {
        return;
    }
    wsdo->depth = depth;
    wsdo->pixelMasks[0] = redMask;
    wsdo->pixelMasks[1] = greenMask;
    wsdo->pixelMasks[2] = blueMask;
    // Init the DIB pixelStride and pixel masks according to
    // the pixel depth. In the 8-bit case, there are no
    // masks as a palette DIB is used instead. Likewise
    // in the 24-bit case, Windows doesn't expect the masks
    switch (depth) {
        case 8:
            wsdo->pixelStride = 1;
            break;
        case 15: //555
            wsdo->pixelStride = 2;
            break;
        case 16: //565
            wsdo->pixelStride = 2;
            break;
        case 24:
            wsdo->pixelStride = 3;
            break;
        case 32: //888
            wsdo->pixelStride = 4;
            break;
    }
    // GDIWindowSurfaceData_GetWindow will throw NullPointerException
    // if wsdo->window is NULL
    wsdo->window = GDIWindowSurfaceData_GetWindow(env, wsdo);
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  wsdo=0x%x wsdo->window=0x%x",
                wsdo, wsdo->window);

    {
        Devices::InstanceAccess devices;
        wsdo->device = devices->GetDeviceReference(screen, FALSE);
    }
    if (wsdo->device == NULL ||
        !SurfaceDepthsCompatible(depth, wsdo->device->GetBitDepth()))
    {
        if (wsdo->device != NULL) {
            J2dTraceLn2(J2D_TRACE_WARNING,
                        "GDIWindowSurfaceData_initOps: Surface depth mismatch: "\
                        "wsdo->depth=%d device depth=%d. Surface invalidated.",
                        wsdo->depth, wsdo->device->GetBitDepth());
        } else {
            J2dTraceLn1(J2D_TRACE_WARNING,
                        "GDIWindowSurfaceData_initOps: Incorrect "\
                        "screen number (screen=%d). Surface invalidated.",
                        screen);
        }

        wsdo->invalid = JNI_TRUE;
    }
    wsdo->surfaceLock = new CriticalSection();
    wsdo->bitmap = NULL;
    wsdo->bmdc = NULL;
    wsdo->bmCopyToScreen = FALSE;
}

JNIEXPORT GDIWinSDOps * JNICALL
GDIWindowSurfaceData_GetOps(JNIEnv *env, jobject sData)
{
    SurfaceDataOps *ops = SurfaceData_GetOps(env, sData);
    // REMIND: There was originally a condition check here to make sure
    // that we were really dealing with a GDIWindowSurfaceData object, but
    // it did not allow for the existence of other win32-accelerated
    // surface data objects (e.g., Win32OffScreenSurfaceData).  I've
    // removed the check for now, but we should replace it with another,
    // more general check against Win32-related surfaces.
    return (GDIWinSDOps *) ops;
}

JNIEXPORT GDIWinSDOps * JNICALL
GDIWindowSurfaceData_GetOpsNoSetup(JNIEnv *env, jobject sData)
{
    // use the 'no setup' version of GetOps
    SurfaceDataOps *ops = SurfaceData_GetOpsNoSetup(env, sData);
    return (GDIWinSDOps *) ops;
}

JNIEXPORT AwtComponent * JNICALL
GDIWindowSurfaceData_GetComp(JNIEnv *env, GDIWinSDOps *wsdo)
{
    PDATA pData = NULL;
    jobject localObj = env->NewLocalRef(wsdo->peer);

    if (localObj == NULL || (pData = JNI_GET_PDATA(localObj)) == NULL) {
        J2dTraceLn1(J2D_TRACE_WARNING,
                    "GDIWindowSurfaceData_GetComp: Null pData? pData=0x%x",
                    pData);
        if (beingShutdown == JNI_TRUE) {
            wsdo->invalid = JNI_TRUE;
            return (AwtComponent *) NULL;
        }
        try {
            AwtToolkit::GetInstance().VerifyActive();
        } catch (awt_toolkit_shutdown&) {
            beingShutdown = JNI_TRUE;
            wsdo->invalid = JNI_TRUE;
            return (AwtComponent *) NULL;
        }
        if (wsdo->invalid == JNI_TRUE) {
            SurfaceData_ThrowInvalidPipeException(env,
                "GDIWindowSurfaceData: bounds changed");
        } else {
            JNU_ThrowNullPointerException(env, "component argument pData");
        }
        return (AwtComponent *) NULL;
    }
    return static_cast<AwtComponent*>(pData);
}

JNIEXPORT HWND JNICALL
GDIWindowSurfaceData_GetWindow(JNIEnv *env, GDIWinSDOps *wsdo)
{
    HWND window = wsdo->window;

    if (window == (HWND) NULL) {
        AwtComponent *comp = GDIWindowSurfaceData_GetComp(env, wsdo);
        if (comp == NULL) {
            J2dTraceLn(J2D_TRACE_WARNING,
                   "GDIWindowSurfaceData_GetWindow: null component");
            return (HWND) NULL;
        }
        comp->GetInsets(&wsdo->insets);
        window = comp->GetHWnd();
        if (::IsWindow(window) == FALSE) {
            J2dRlsTraceLn(J2D_TRACE_ERROR,
                          "GDIWindowSurfaceData_GetWindow: disposed component");
            JNU_ThrowNullPointerException(env, "disposed component");
            return (HWND) NULL;
        }
        wsdo->window = window;
    }

    return window;
}

} /* extern "C" */

static jboolean GDIWinSD_SimpleClip(JNIEnv *env, GDIWinSDOps *wsdo,
                                   SurfaceDataBounds *bounds,
                                   HDC hDC)
{
    RECT rClip;

    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_SimpleClip");
    if (hDC == NULL) {
        return JNI_FALSE;
    }

    int nComplexity = ::GetClipBox(hDC, &rClip);

    switch (nComplexity) {
    case COMPLEXREGION:
        {
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "  complex clipping region");
            // if complex user/system clip, more detailed testing required
            // check to see if the view itself has a complex clip.
            // ::GetClipBox is only API which returns overlapped window status
            // so we set the rView as our clip, and then see if resulting
            // clip is complex.
            // Only other way to figure this out would be to walk the
            // overlapping windows (no API to get the actual visible clip
            // list).  Then we'd still have to merge that info with the
            // clip region for the dc (if it exists).
            // REMIND: we can cache the CreateRectRgnIndirect result,
            // and only override with ::SetRectRgn

            // First, create a region handle (need existing HRGN for
            // the following call).
            HRGN rgnSave = ::CreateRectRgn(0, 0, 0, 0);
            int  clipStatus = ::GetClipRgn(hDC, rgnSave);
            if (-1 == clipStatus) {
                J2dTraceLn(J2D_TRACE_WARNING,
                           "GDIWinSD_SimpleClip: failed due to clip status");
                ::DeleteObject(rgnSave);
                return JNI_FALSE;
            }
            HRGN rgnBounds = ::CreateRectRgn(
                bounds->x1 - wsdo->insets.left,
                bounds->y1 - wsdo->insets.top,
                bounds->x2 - wsdo->insets.left,
                bounds->y2 - wsdo->insets.top);
            ::SelectClipRgn(hDC, rgnBounds);
            nComplexity = ::GetClipBox(hDC, &rClip);
            ::SelectClipRgn(hDC, clipStatus? rgnSave: NULL);
            ::DeleteObject(rgnSave);
            ::DeleteObject(rgnBounds);

            // Now, test the new clip box.  If it's still not a
            // SIMPLE region, then our bounds must intersect part of
            // the clipping article
            if (SIMPLEREGION != nComplexity) {
                J2dTraceLn(J2D_TRACE_WARNING,
                           "GDIWinSD_SimpleClip: failed due to complexity");
                return JNI_FALSE;
            }
        }
        // NOTE: No break here - we want to fall through into the
        // SIMPLE case, adjust our bounds by the new rClip rect
        // and make sure that our locking bounds are not empty.
    case SIMPLEREGION:
        J2dTraceLn(J2D_TRACE_VERBOSE, "  simple clipping region");
        // Constrain the bounds to the given clip box
        if (bounds->x1 < rClip.left) {
            bounds->x1 = rClip.left;
        }
        if (bounds->y1 < rClip.top) {
            bounds->y1 = rClip.top;
        }
        if (bounds->x2 > rClip.right) {
            bounds->x2 = rClip.right;
        }
        if (bounds->y2 > rClip.bottom) {
            bounds->y2 = rClip.bottom;
        }
        // If the bounds are 0 or negative, then the bounds have
        // been obscured by the clip box, so return FALSE
        if ((bounds->x2 <= bounds->x1) ||
            (bounds->y2 <= bounds->y1)) {
            // REMIND: We should probably do something different here
            // instead of simply returning FALSE.  Since the bounds are
            // empty we won't end up drawing anything, so why spend the
            // effort of returning false and having GDI do a LOCK_BY_DIB?
            // Perhaps we need a new lock code that will indicate that we
            // shouldn't bother drawing?
            J2dTraceLn(J2D_TRACE_WARNING,
                       "GDIWinSD_SimpleClip: failed due to empty bounds");
            return JNI_FALSE;
        }
        break;
    case NULLREGION:
    case ERROR:
    default:
        J2dTraceLn1(J2D_TRACE_ERROR,
                   "GDIWinSD_SimpleClip: failed due to incorrect complexity=%d",
                    nComplexity);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static jint GDIWinSD_Lock(JNIEnv *env,
                         SurfaceDataOps *ops,
                         SurfaceDataRasInfo *pRasInfo,
                         jint lockflags)
{
    GDIWinSDOps *wsdo = (GDIWinSDOps *) ops;
    int ret = SD_SUCCESS;
    HDC hDC;
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_Lock");

    /* This surfaceLock replaces an earlier implementation which used a
    monitor associated with the peer.  That implementation was prone
    to deadlock problems, so it was replaced by a lock that does not
    have dependencies outside of this thread or object.
    However, this lock doesn't necessarily do all that we want.
    For example, a user may issue a call which results in a DIB lock
    and another call which results in a DDraw Blt.  We can't guarantee
    what order these operations happen in (they are driver and
    video-card dependent), so locking around the issue of either of
    those calls won't necessarily guarantee a particular result.
    The real solution might be to move away from mixing our
    rendering API's.  That is, if we only used DDraw, then we could
    guarantee that all rendering operations would happen in a given
    order.  Similarly for GDI.  But by mixing them, we leave our
    code at the mercy of driver bugs.*/
    wsdo->surfaceLock->Enter();
    if (wsdo->invalid == JNI_TRUE) {
        J2dTraceLn(J2D_TRACE_WARNING, "GDIWinSD_Lock: surface is invalid");
        wsdo->surfaceLock->Leave();
        if (beingShutdown != JNI_TRUE) {
            SurfaceData_ThrowInvalidPipeException(env,
                "GDIWindowSurfaceData: bounds changed");
        }
        return SD_FAILURE;
    }
    if (wsdo->lockType != WIN32SD_LOCK_UNLOCKED) {
        wsdo->surfaceLock->Leave();
        if (!safe_ExceptionOccurred(env)) {
            JNU_ThrowInternalError(env, "Win32 LockRasData cannot nest locks");
        }
        return SD_FAILURE;
    }

    hDC = wsdo->GetDC(env, wsdo, 0, NULL, NULL, NULL, 0);
    if (hDC == NULL) {
        wsdo->surfaceLock->Leave();
        if (beingShutdown != JNI_TRUE) {
            JNU_ThrowNullPointerException(env, "HDC for component");
        }
        return SD_FAILURE;
    }

    if (lockflags & SD_LOCK_RD_WR) {
        // Do an initial clip to the client region of the window
        RECT crect;
        ::GetClientRect(wsdo->window, &crect);

        // Translate to window coords
        crect.left += wsdo->insets.left;
        crect.top += wsdo->insets.top;
        crect.right += wsdo->insets.left;
        crect.bottom += wsdo->insets.top;

        SurfaceDataBounds *bounds = &pRasInfo->bounds;

        if (bounds->x1 < crect.left) {
            bounds->x1 = crect.left;
        }
        if (bounds->y1 < crect.top) {
            bounds->y1 = crect.top;
        }
        if (bounds->x2 > crect.right) {
            bounds->x2 = crect.right;
        }
        if (bounds->y2 > crect.bottom) {
            bounds->y2 = crect.bottom;
        }

        if (bounds->x2 > bounds->x1 && bounds->y2 > bounds->y1) {
            wsdo->lockType = WIN32SD_LOCK_BY_DIB;
            if (lockflags & SD_LOCK_FASTEST) {
                ret = SD_SLOWLOCK;
            }
            J2dTraceLn(J2D_TRACE_VERBOSE, " locked by DIB");
        } else {
            wsdo->ReleaseDC(env, wsdo, hDC);
            wsdo->lockType = WIN32SD_LOCK_UNLOCKED;
            wsdo->surfaceLock->Leave();
            ret = SD_FAILURE;
            J2dTraceLn(J2D_TRACE_ERROR,
                       "GDIWinSD_Lock: error locking by DIB");
        }
    } else {
        J2dTraceLn(J2D_TRACE_VERBOSE, "GDIWinSD_Lock: surface wasn't locked");
        /* They didn't lock for anything - we won't give them anything */
        wsdo->ReleaseDC(env, wsdo, hDC);
        wsdo->lockType = WIN32SD_LOCK_UNLOCKED;
        wsdo->surfaceLock->Leave();
        ret = SD_FAILURE;
    }

    wsdo->lockFlags = lockflags;
    return ret;
}

static void GDIWinSD_GetRasInfo(JNIEnv *env,
                               SurfaceDataOps *ops,
                               SurfaceDataRasInfo *pRasInfo)
{
    GDIWinSDOps *wsdo = (GDIWinSDOps *) ops;
    jint lockflags = wsdo->lockFlags;
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_GetRasInfo");
    HDC hDC = GetThreadDC(env, wsdo);

    if (wsdo->lockType == WIN32SD_LOCK_UNLOCKED) {
        memset(pRasInfo, 0, sizeof(*pRasInfo));
        return;
    }

    if (wsdo->lockType == WIN32SD_LOCK_BY_DIB) {
        int x, y, w, h;
        int pixelStride = wsdo->pixelStride;
        // do not subtract insets from x,y as we take care of it in SD_GetDC
        x = pRasInfo->bounds.x1;
        y = pRasInfo->bounds.y1;
        w = pRasInfo->bounds.x2 - x;
        h = pRasInfo->bounds.y2 - y;

        struct tagBitmapheader  {
            BITMAPINFOHEADER bmiHeader;
            union {
                DWORD           dwMasks[3];
                RGBQUAD         palette[256];
            } colors;
        } bmi;

        // Need to create bitmap if we don't have one already or
        // if the existing one is not large enough for this operation
        // or if we are in 8 bpp display mode (because we need to
        // make sure that the latest palette info gets loaded into
        // the bitmap)
        // REMIND: we should find some way to dynamically force bitmap
        // recreation only when the palette changes
        if (pixelStride == 1 || !wsdo->bitmap || (w > wsdo->bmWidth) ||
            (h > wsdo->bmHeight))
        {
            if (wsdo->bitmap) {
                // delete old objects
                J2dTraceLn(J2D_TRACE_VERBOSE,
                           "GDIWinSD_GetRasInfo: recreating GDI bitmap");
                if (wsdo->bmdc) {   // should not be null
                    ::SelectObject(wsdo->bmdc, wsdo->oldmap);
                    ::DeleteDC(wsdo->bmdc);
                    wsdo->bmdc = 0;
                }
                ::DeleteObject(wsdo->bitmap);
                wsdo->bitmap = 0;
            }
            bmi.bmiHeader.biSize = sizeof(bmi.bmiHeader);
            bmi.bmiHeader.biWidth = w;
            bmi.bmiHeader.biHeight = -h;
            wsdo->bmWidth = w;
            wsdo->bmHeight = h;
            bmi.bmiHeader.biPlanes = 1;
            bmi.bmiHeader.biBitCount = pixelStride * 8;
            // 1,3 byte use BI_RGB, 2,4 byte use BI_BITFIELD...
            bmi.bmiHeader.biCompression =
                (pixelStride & 1)
                    ? BI_RGB
                    : BI_BITFIELDS;
            bmi.bmiHeader.biSizeImage = 0;
            bmi.bmiHeader.biXPelsPerMeter = 0;
            bmi.bmiHeader.biYPelsPerMeter = 0;
            bmi.bmiHeader.biClrUsed = 0;
            bmi.bmiHeader.biClrImportant = 0;
            if (pixelStride == 1) {
                // we can use systemEntries here because
                // RGBQUAD is xRGB and systemEntries are stored as xRGB
                memcpy(bmi.colors.palette, wsdo->device->GetSystemPaletteEntries(),
                       sizeof(bmi.colors.palette));
            } else {
                // For non-index cases, init the masks for the pixel depth
                for (int i = 0; i < 3; i++) {
                    bmi.colors.dwMasks[i] = wsdo->pixelMasks[i];
                }
            }

            // REMIND: This would be better if moved to the Lock function
            // so that errors could be dealt with.
            wsdo->bitmap = ::CreateDIBSection(hDC, (BITMAPINFO *) &bmi,
                                              DIB_RGB_COLORS, &wsdo->bmBuffer, NULL, 0);
            if (wsdo->bitmap != 0) {
                // scanStride is cached along with reuseable bitmap
                // Round up to the next DWORD boundary
                wsdo->bmScanStride = (wsdo->bmWidth * pixelStride + 3) & ~3;
                wsdo->bmdc = ::CreateCompatibleDC(hDC);
                if (wsdo->bmdc == 0) {
                    ::DeleteObject(wsdo->bitmap);
                    wsdo->bitmap = 0;
                } else {
                    wsdo->oldmap = (HBITMAP) ::SelectObject(wsdo->bmdc,
                                                            wsdo->bitmap);
                }
            }
        }
        if (wsdo->bitmap != 0) {
            if (lockflags & SD_LOCK_NEED_PIXELS) {
                int ret = ::BitBlt(wsdo->bmdc, 0, 0, w, h,
                                   hDC, x, y, SRCCOPY);
                ::GdiFlush();
            }
            wsdo->x = x;
            wsdo->y = y;
            wsdo->w = w;
            wsdo->h = h;
            pRasInfo->rasBase = (char *)wsdo->bmBuffer - (x*pixelStride +
                                y*wsdo->bmScanStride);
            pRasInfo->pixelStride = pixelStride;
            pRasInfo->pixelBitOffset = 0;
            pRasInfo->scanStride = wsdo->bmScanStride;
            if (lockflags & SD_LOCK_WRITE) {
                // If the user writes to the bitmap then we should
                // copy the bitmap to the screen during Unlock
                wsdo->bmCopyToScreen = TRUE;
            }
        } else {
            pRasInfo->rasBase = NULL;
            pRasInfo->pixelStride = 0;
            pRasInfo->pixelBitOffset = 0;
            pRasInfo->scanStride = 0;
        }
    } else {
        /* They didn't lock for anything - we won't give them anything */
        pRasInfo->rasBase = NULL;
        pRasInfo->pixelStride = 0;
        pRasInfo->pixelBitOffset = 0;
        pRasInfo->scanStride = 0;
    }
    if (wsdo->lockFlags & SD_LOCK_LUT) {
        pRasInfo->lutBase =
            (long *) wsdo->device->GetSystemPaletteEntries();
        pRasInfo->lutSize = 256;
    } else {
        pRasInfo->lutBase = NULL;
        pRasInfo->lutSize = 0;
    }
    if (wsdo->lockFlags & SD_LOCK_INVCOLOR) {
        pRasInfo->invColorTable = wsdo->device->GetSystemInverseLUT();
        ColorData *cData = wsdo->device->GetColorData();
        pRasInfo->redErrTable = cData->img_oda_red;
        pRasInfo->grnErrTable = cData->img_oda_green;
        pRasInfo->bluErrTable = cData->img_oda_blue;
    } else {
        pRasInfo->invColorTable = NULL;
        pRasInfo->redErrTable = NULL;
        pRasInfo->grnErrTable = NULL;
        pRasInfo->bluErrTable = NULL;
    }
    if (wsdo->lockFlags & SD_LOCK_INVGRAY) {
        pRasInfo->invGrayTable =
            wsdo->device->GetColorData()->pGrayInverseLutData;
    } else {
        pRasInfo->invGrayTable = NULL;
    }
}

static void GDIWinSD_Setup(JNIEnv *env,
                          SurfaceDataOps *ops)
{
    // Call SetupTGI to ensure that this thread already has a DC that is
    // compatible with this window.  This means that we won't be calling
    // ::SendMessage(GETDC) in the middle of a lock procedure, which creates
    // a potential deadlock situation.
    // Note that calling SetupTGI here means that anybody needing a DC
    // later in this rendering process need only call GetTGI, which
    // assumes that the TGI structure is valid for this thread/window.
    SetupThreadGraphicsInfo(env, (GDIWinSDOps*)ops);
}


static void GDIWinSD_Unlock(JNIEnv *env,
                           SurfaceDataOps *ops,
                           SurfaceDataRasInfo *pRasInfo)
{
    GDIWinSDOps *wsdo = (GDIWinSDOps *) ops;
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_Unlock");
    HDC hDC = GetThreadDC(env, wsdo);

    if (wsdo->lockType == WIN32SD_LOCK_UNLOCKED) {
        if (!safe_ExceptionOccurred(env)) {
            JNU_ThrowInternalError(env,
                                   "Unmatched unlock on Win32 SurfaceData");
        }
        return;
    }

    if (wsdo->lockType == WIN32SD_LOCK_BY_DIB) {
        if (wsdo->lockFlags & SD_LOCK_WRITE) {
            J2dTraceLn(J2D_TRACE_VERBOSE,
                       "GDIWinSD_Unlock: do Blt of the bitmap");
            if (wsdo->bmCopyToScreen && ::IsWindowVisible(wsdo->window)) {
                // Don't bother copying to screen if our window has gone away
                // or if the bitmap was not actually written to during this
                // Lock/Unlock procedure.
                ::BitBlt(hDC, wsdo->x, wsdo->y, wsdo->w, wsdo->h,
                    wsdo->bmdc, 0, 0, SRCCOPY);
                ::GdiFlush();
            }
            wsdo->bmCopyToScreen = FALSE;
        }
        wsdo->lockType = WIN32SD_LOCK_UNLOCKED;
        wsdo->ReleaseDC(env, wsdo, hDC);
    }
    wsdo->surfaceLock->Leave();
}

/*
 * REMIND: This mechanism is just a prototype of a way to manage a
 * small cache of DC objects.  It is incomplete in the following ways:
 *
 * - It is not thread-safe!  It needs appropriate locking and release calls
 *   (perhaps the AutoDC mechanisms from Kestrel)
 * - It does hardly any error checking (What if GetDCEx returns NULL?)
 * - It cannot handle printer DCs and their resolution
 * - It should probably "live" in the native SurfaceData object to allow
 *   alternate implementations for printing and embedding
 * - It doesn't handle XOR
 * - It caches the client bounds to determine if clipping is really needed
 *   (no way to invalidate the cached bounds and there is probably a better
 *    way to manage clip validation in any case)
 */

#define COLORFOR(c)     (PALETTERGB(((c)>>16)&0xff,((c)>>8)&0xff,((c)&0xff)))

COLORREF CheckGrayColor(GDIWinSDOps *wsdo, int c) {
    if (wsdo->device->GetGrayness() != GS_NOTGRAY) {
        int g = (77 *(c & 0xFF) +
                 150*((c >> 8) & 0xFF) +
                 29 *((c >> 16) & 0xFF) + 128) / 256;
        c = g | (g << 8) | (g << 16);
    }
    return COLORFOR(c);
}

static HDC GDIWinSD_GetDC(JNIEnv *env, GDIWinSDOps *wsdo,
                         jint type, jint *patrop,
                         jobject clip, jobject comp, jint color)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_GetDC");

    if (wsdo->invalid == JNI_TRUE) {
        if (beingShutdown != JNI_TRUE) {
            SurfaceData_ThrowInvalidPipeException(env, "bounds changed");
        }
        return (HDC) NULL;
    }

    ThreadGraphicsInfo *info = GetThreadGraphicsInfo(env, wsdo);
    GDIWinSD_InitDC(env, wsdo, info, type, patrop, clip, comp, color);
    return env->ExceptionCheck() ? (HDC)NULL : info->hDC;
}

JNIEXPORT void JNICALL
GDIWinSD_InitDC(JNIEnv *env, GDIWinSDOps *wsdo, ThreadGraphicsInfo *info,
               jint type, jint *patrop,
               jobject clip, jobject comp, jint color)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_InitDC");

    // init clip
    if (clip == NULL) {
        if (info->type & CLIP) {
            ::SelectClipRgn(info->hDC, (HRGN) NULL);
            info->type ^= CLIP;
        }
        if (info->clip != NULL) {
            env->DeleteWeakGlobalRef(info->clip);
            info->clip = NULL;
        }
    } else if (!env->IsSameObject(clip, info->clip)) {
        SurfaceDataBounds span;
        RegionData clipInfo;
        if (Region_GetInfo(env, clip, &clipInfo)) {
            // return; // REMIND: What to do here?
        }

        if (Region_IsEmpty(&clipInfo)) {
            HRGN hrgn = ::CreateRectRgn(0, 0, 0, 0);
            ::SelectClipRgn(info->hDC, hrgn);
            ::DeleteObject(hrgn);
            info->type |= CLIP;
        } else if (Region_IsRectangular(&clipInfo)) {
            if (clipInfo.bounds.x1 <= info->bounds.left &&
                clipInfo.bounds.y1 <= info->bounds.top &&
                clipInfo.bounds.x2 >= info->bounds.right &&
                clipInfo.bounds.y2 >= info->bounds.bottom)
            {
                if (info->type & CLIP) {
                    ::SelectClipRgn(info->hDC, (HRGN) NULL);
                    info->type ^= CLIP;
                }
            } else {
                // Make the window-relative rect a client-relative
                // one for Windows
                HRGN hrgn =
                    ::CreateRectRgn(clipInfo.bounds.x1 - wsdo->insets.left,
                                    clipInfo.bounds.y1 - wsdo->insets.top,
                                    clipInfo.bounds.x2 - wsdo->insets.left,
                                    clipInfo.bounds.y2 - wsdo->insets.top);
                ::SelectClipRgn(info->hDC, hrgn);
                ::DeleteObject(hrgn);
                info->type |= CLIP;
            }
        } else {
            int leftInset = wsdo->insets.left;
            int topInset = wsdo->insets.top;
            Region_StartIteration(env, &clipInfo);
            jint numrects = Region_CountIterationRects(&clipInfo);
            RGNDATA *lpRgnData;
            try {
                lpRgnData = (RGNDATA *) SAFE_SIZE_STRUCT_ALLOC(safe_Malloc,
                    sizeof(RGNDATAHEADER), numrects, sizeof(RECT));
            } catch (std::bad_alloc&) {
                JNU_ThrowOutOfMemoryError(env, "Initialization of surface region data failed.");
                return;
            }
            const DWORD nCount = sizeof(RGNDATAHEADER) + numrects * sizeof(RECT);
            lpRgnData->rdh.dwSize = sizeof(RGNDATAHEADER);
            lpRgnData->rdh.iType = RDH_RECTANGLES;
            lpRgnData->rdh.nCount = numrects;
            lpRgnData->rdh.nRgnSize = 0;
            lpRgnData->rdh.rcBound.left = clipInfo.bounds.x1 - leftInset;
            lpRgnData->rdh.rcBound.top = clipInfo.bounds.y1 - topInset;
            lpRgnData->rdh.rcBound.right = clipInfo.bounds.x2 - leftInset;
            lpRgnData->rdh.rcBound.bottom = clipInfo.bounds.y2 - topInset;
            RECT *pRect = (RECT *) &(((RGNDATA *)lpRgnData)->Buffer);
            while (Region_NextIteration(&clipInfo, &span)) {
                pRect->left = span.x1 - leftInset;
                pRect->top = span.y1 - topInset;
                pRect->right = span.x2 - leftInset;
                pRect->bottom = span.y2 - topInset;
                pRect++;
            }
            Region_EndIteration(env, &clipInfo);
            HRGN hrgn = ::ExtCreateRegion(NULL, nCount, lpRgnData);
            free(lpRgnData);
            ::SelectClipRgn(info->hDC, hrgn);
            ::DeleteObject(hrgn);
            info->type |= CLIP;
        }
        if (info->clip != NULL) {
            env->DeleteWeakGlobalRef(info->clip);
        }
        info->clip = env->NewWeakGlobalRef(clip);
        if (env->ExceptionCheck()) {
            return;
        }
    }

    // init composite
    if ((comp == NULL) || !env->IsInstanceOf(comp, xorCompClass)) {
        if (info->comp != NULL) {
            env->DeleteWeakGlobalRef(info->comp);
            info->comp = NULL;
            info->patrop = PATCOPY;
            ::SetROP2(info->hDC, R2_COPYPEN);
        }
    } else {
        if (!env->IsSameObject(comp, info->comp)) {
            info->xorcolor = GrPrim_CompGetXorColor(env, comp);
            if (info->comp != NULL) {
                env->DeleteWeakGlobalRef(info->comp);
            }
            info->comp = env->NewWeakGlobalRef(comp);
            info->patrop = PATINVERT;
            ::SetROP2(info->hDC, R2_XORPEN);
        }
        color ^= info->xorcolor;
    }

    if (patrop != NULL) {
        *patrop = info->patrop;
    }

    // init brush and pen
    if (type & BRUSH) {
        if (info->brushclr != color || (info->brush == NULL)) {
            if (info->type & BRUSH) {
                ::SelectObject(info->hDC, nullbrush);
                info->type ^= BRUSH;
            }
            if (info->brush != NULL) {
                info->brush->Release();
            }
            info->brush = AwtBrush::Get(CheckGrayColor(wsdo, color));
            info->brushclr = color;
        }
        if ((info->type & BRUSH) == 0) {
            ::SelectObject(info->hDC, info->brush->GetHandle());
            info->type ^= BRUSH;
        }
    } else if (type & NOBRUSH) {
        if (info->type & BRUSH) {
            ::SelectObject(info->hDC, nullbrush);
            info->type ^= BRUSH;
        }
    }
    if (type & PEN) {
        if (info->penclr != color || (info->pen == NULL)) {
            if (info->type & PEN) {
                ::SelectObject(info->hDC, nullpen);
                info->type ^= PEN;
            }
            if (info->pen != NULL) {
                info->pen->Release();
            }
            info->pen = AwtPen::Get(CheckGrayColor(wsdo, color));
            info->penclr = color;
        }
        if ((info->type & PEN) == 0) {
            ::SelectObject(info->hDC, info->pen->GetHandle());
            info->type ^= PEN;
        }
    } else if (type & NOPEN) {
        if (info->type & PEN) {
            ::SelectObject(info->hDC, nullpen);
            info->type ^= PEN;
        }
    }
}

static void GDIWinSD_ReleaseDC(JNIEnv *env, GDIWinSDOps *wsdo, HDC hDC)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_ReleaseDC");
    // Don't actually do anything here: every thread holds its own
    // wsdo-specific DC until the thread goes away or the wsdo
    // is disposed.
}


static void GDIWinSD_InvalidateSD(JNIEnv *env, GDIWinSDOps *wsdo)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_InvalidateSD");
    J2dTraceLn2(J2D_TRACE_VERBOSE, "  wsdo=0x%x wsdo->window=0x%x",
                wsdo, wsdo->window);

    wsdo->invalid = JNI_TRUE;
}



/*
 * Method:    GDIWinSD_Dispose
 */
static void
GDIWinSD_Dispose(JNIEnv *env, SurfaceDataOps *ops)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWinSD_Dispose");
    // ops is assumed non-null as it is checked in SurfaceData_DisposeOps
    GDIWinSDOps *wsdo = (GDIWinSDOps*)ops;
    if (wsdo->bitmap) {
        // delete old objects
        J2dTraceLn(J2D_TRACE_VERBOSE, "  disposing the GDI bitmap");
        if (wsdo->bmdc) {   // should not be null
            ::SelectObject(wsdo->bmdc, wsdo->oldmap);
            ::DeleteDC(wsdo->bmdc);
            wsdo->bmdc = 0;
        }
        ::DeleteObject(wsdo->bitmap);
        wsdo->bitmap = 0;
    }
    env->DeleteWeakGlobalRef(wsdo->peer);
    if (wsdo->device != NULL) {
        wsdo->device->Release();
        wsdo->device = NULL;
    }
    delete wsdo->surfaceLock;
}


/*
 * Class:     sun_java2d_windows_GDIWindowSurfaceData
 * Method:    invalidateSD
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_sun_java2d_windows_GDIWindowSurfaceData_invalidateSD(JNIEnv *env, jobject wsd)
{
    J2dTraceLn(J2D_TRACE_INFO, "GDIWindowSurfaceData_invalidateSD");
    GDIWinSDOps *wsdo = GDIWindowSurfaceData_GetOpsNoSetup(env, wsd);
    if (wsdo != NULL) {
        wsdo->InvalidateSD(env, wsdo);
    }
}
