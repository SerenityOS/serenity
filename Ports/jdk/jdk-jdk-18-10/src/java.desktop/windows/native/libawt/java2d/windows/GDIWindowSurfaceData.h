/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _WIN32SURFACEDATA_H_
#define _WIN32SURFACEDATA_H_


#include "SurfaceData.h"

#include "colordata.h"
#include "awt_Brush.h"
#include "awt_Pen.h"
#include "awt_Win32GraphicsDevice.h"

#include "stdhdrs.h"


#define TEST_SURFACE_BITS(a,f) (((a)&(f)) == (f))

/**
 * This include file contains support definitions for loops using the
 * SurfaceData interface to talk to a Win32 drawable from native code.
 */

typedef struct _GDIWinSDOps GDIWinSDOps;

#define CONTEXT_NORMAL 0
#define CONTEXT_DISPLAY_CHANGE 1
#define CONTEXT_ENTER_FULL_SCREEN 2
#define CONTEXT_CHANGE_BUFFER_COUNT 3
#define CONTEXT_EXIT_FULL_SCREEN 4

/*
 * The definitions of the various attribute flags for requesting
 * which rendering objects should be selected into the HDC returned
 * from GetDC().
 */
#define PEN             1
#define NOPEN           2
#define BRUSH           4
#define NOBRUSH         8
#define CLIP            16              /* For tracking purposes only */
#define PENBRUSH        (PEN | BRUSH)
#define PENONLY         (PEN | NOBRUSH)
#define BRUSHONLY       (BRUSH | NOPEN)

/*
 * This function retrieves an HDC for rendering to the destination
 * managed by the indicated GDIWinSDOps structure.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The flags parameter should be an inclusive OR of any of the attribute
 * flags defined above.
 *
 * The patrop parameter should be a pointer to a jint that will receive
 * the appropriate ROP code (PATCOPY or PATINVERT) based on the current
 * composite, or NULL if the ROP code will be ignored by the caller.
 *
 * The clip parameter should be a pointer to a rectangle indicating the
 * desired clip.
 *
 * The comp parameter should be a pointer to a Composite object, or NULL
 * which means the Src (default) compositing rule will be used.
 *
 * The pixel parameter should be a 24-bit XRGB value indicating the
 * color that will be used for rendering.  The upper 8 bits are allowed
 * to be any value.
 *
 * The ReleaseDC function should be called to release the lock on the DC
 * after a given atomic set of rendering operations is complete.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 */
typedef HDC GetDCFunc(JNIEnv *env,
                      GDIWinSDOps *wsdo,
                      jint flags,
                      jint *patrop,
                      jobject clip,
                      jobject comp,
                      jint color);

/*
 * This function releases an HDC that was retrieved from the GetDC
 * function of the indicated GDIWinSDOps structure.
 *
 * The env parameter should be the JNIEnv of the surrounding JNI context.
 *
 * The ops parameter should be a pointer to the ops object upon which
 * this function is being invoked.
 *
 * The hdc parameter should be the handle to the HDC object that was
 * returned from the GetDC function.
 *
 * Note to callers:
 *      This function may use JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 */
typedef void ReleaseDCFunc(JNIEnv *env,
                           GDIWinSDOps *wsdo,
                           HDC hdc);


typedef void InvalidateSDFunc(JNIEnv *env,
                              GDIWinSDOps *wsdo);

/*
 * A structure that holds all state global to the native surfaceData
 * object.
 *
 * Note:
 * This structure will be shared between different threads that
 * operate on the same surfaceData, so it should not contain any
 * variables that could be changed by one thread thus placing other
 * threads in a state of confusion.  For example, the hDC field was
 * removed because each thread now has its own shared DC.  But the
 * window field remains because once it is set for a given wsdo
 * structure it stays the same until that structure is destroyed.
 */
struct _GDIWinSDOps {
    SurfaceDataOps      sdOps;
    LONG                timeStamp; // creation time stamp.
                                   // Doesn't store a real time -
                                   // just counts creation events of this structure
                                   // made by GDIWindowSurfaceData_initOps()
                                   // see bug# 6859086
    jboolean            invalid;
    GetDCFunc           *GetDC;
    ReleaseDCFunc       *ReleaseDC;
    InvalidateSDFunc    *InvalidateSD;
    jint                lockType;       // REMIND: store in TLS
    jint                lockFlags;      // REMIND: store in TLS
    jobject             peer;
    HWND                window;
    RECT                insets;
    jint                depth;
    jint                pixelStride;    // Bytes per pixel
    DWORD               pixelMasks[3];  // RGB Masks for Windows DIB creation
    HBITMAP             bitmap;         // REMIND: store in TLS
    HBITMAP             oldmap;         // REMIND: store in TLS
    HDC                 bmdc;           // REMIND: store in TLS
    int                 bmScanStride;   // REMIND: store in TLS
    int                 bmWidth;        // REMIND: store in TLS
    int                 bmHeight;       // REMIND: store in TLS
    void                *bmBuffer;      // REMIND: store in TLS
    jboolean            bmCopyToScreen; // Used to track whether we
                                        // actually should copy the bitmap
                                        // to the screen
    AwtBrush            *brush;         // used for offscreen surfaces only
    jint                brushclr;
    AwtPen              *pen;           // used for offscreen surfaces only
    jint                penclr;

    int                 x, y, w, h;     // REMIND: store in TLS
    CriticalSection     *surfaceLock;   // REMIND: try to remove
    AwtWin32GraphicsDevice *device;
};

#define WIN32SD_LOCK_UNLOCKED   0       /* surface is not locked */
#define WIN32SD_LOCK_BY_NULL    1       /* surface locked for NOP */
#define WIN32SD_LOCK_BY_DIB     2       /* surface locked by BitBlt */

extern "C" {

/*
 * Structure for holding the graphics state of a thread.
 */
typedef struct {
    HDC         hDC;
    HWND        hWnd;
    GDIWinSDOps *wsdo;
    LONG        wsdoTimeStamp; // wsdo creation time stamp.
                               // Other threads may deallocate wsdo
                               // and then allocate a new GDIWinSDOps
                               // structure at the same memory location.
                               // Time stamp is the only way to detect if
                               // wsdo got changed.
                               // see bug# 6859086
    RECT        bounds;
    jobject     clip;
    jobject     comp;
    jint        xorcolor;
    jint        patrop;
    jint        type;
    AwtBrush    *brush;
    jint        brushclr;
    AwtPen      *pen;
    jint        penclr;
} ThreadGraphicsInfo;


/*
 * This function returns a pointer to a native GDIWinSDOps structure
 * for accessing the indicated Win32 SurfaceData Java object.  It
 * verifies that the indicated SurfaceData object is an instance
 * of GDIWindowSurfaceData before returning and will return NULL if the
 * wrong SurfaceData object is being accessed.  This function will
 * throw the appropriate Java exception if it returns NULL so that
 * the caller can simply return.
 *
 * Note to callers:
 *      This function uses JNI methods so it is important that the
 *      caller not have any outstanding GetPrimitiveArrayCritical or
 *      GetStringCritical locks which have not been released.
 *
 *      The caller may continue to use JNI methods after this method
 *      is called since this function will not leave any outstanding
 *      JNI Critical locks unreleased.
 */
JNIEXPORT GDIWinSDOps * JNICALL
GDIWindowSurfaceData_GetOps(JNIEnv *env, jobject sData);

JNIEXPORT GDIWinSDOps * JNICALL
GDIWindowSurfaceData_GetOpsNoSetup(JNIEnv *env, jobject sData);

JNIEXPORT HWND JNICALL
GDIWindowSurfaceData_GetWindow(JNIEnv *env, GDIWinSDOps *wsdo);

JNIEXPORT void JNICALL
GDIWinSD_InitDC(JNIEnv *env, GDIWinSDOps *wsdo, ThreadGraphicsInfo *info,
               jint type, jint *patrop,
               jobject clip, jobject comp, jint color);

JNIEXPORT AwtComponent * JNICALL
GDIWindowSurfaceData_GetComp(JNIEnv *env, GDIWinSDOps *wsdo);

} /* extern "C" */


#endif _WIN32SURFACEDATA_H_
