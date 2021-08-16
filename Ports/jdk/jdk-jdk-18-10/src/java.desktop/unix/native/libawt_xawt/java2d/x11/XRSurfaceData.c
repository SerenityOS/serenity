/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "GraphicsPrimitiveMgr.h"
#include "Region.h"
#include "Trace.h"
#include "X11SurfaceData.h"

/*#include <xcb/xcb.h>*/
#include <X11/extensions/Xrender.h>

#ifndef RepeatNone  /* added in 0.10 */
#define RepeatNone    0
#define RepeatNormal  1
#define RepeatPad     2
#define RepeatReflect 3
#endif


#include <sys/uio.h>
#include <dlfcn.h>
#include <setjmp.h>

jfieldID pictID;
jfieldID xidID;
jfieldID blitMaskPMID;
jfieldID blitMaskPictID;

JNIEXPORT void JNICALL
   Java_sun_java2d_xr_XRSurfaceData_initXRPicture(JNIEnv *env, jobject xsd,
                                                  jlong pXSData,
                                                  jint pictFormat)
{
  X11SDOps *xsdo;
  XRenderPictFormat *fmt;

  J2dTraceLn(J2D_TRACE_INFO, "in XRSurfaceData_initXRender");

  xsdo = (X11SDOps *) jlong_to_ptr(pXSData);
  if (xsdo == NULL) {
      return;
  }

  if (xsdo->xrPic == None) {
      XRenderPictureAttributes pict_attr;
      pict_attr.repeat = RepeatNone;
      fmt = XRenderFindStandardFormat(awt_display, pictFormat);
      xsdo->xrPic =
         XRenderCreatePicture(awt_display, xsdo->drawable, fmt,
                              CPRepeat, &pict_attr);
  }

  (*env)->SetIntField (env, xsd, pictID, xsdo->xrPic);
  (*env)->SetIntField (env, xsd, xidID, xsdo->drawable);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRSurfaceData_initIDs(JNIEnv *env, jclass xsd)
{
  J2dTraceLn(J2D_TRACE_INFO, "in XRSurfaceData_initIDs");

  pictID = (*env)->GetFieldID(env, xsd, "picture", "I");
  if (pictID == NULL) {
      return;
  }
  xidID = (*env)->GetFieldID(env, xsd, "xid", "I");
  if (xidID == NULL) {
      return;
  }

  XShared_initIDs(env, JNI_FALSE);
}


JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRSurfaceData_XRInitSurface(JNIEnv *env, jclass xsd,
                                               jint depth,
                                               jint width, jint height,
                                               jlong drawable, jint pictFormat)
{
    X11SDOps *xsdo;

    J2dTraceLn(J2D_TRACE_INFO, "in XRSurfaceData_initSurface");

    xsdo = X11SurfaceData_GetOps(env, xsd);
    if (xsdo == NULL) {
        return;
    }

    XShared_initSurface(env, xsdo, depth, width, height, drawable);
}



JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRSurfaceData_freeXSDOPicture(JNIEnv *env, jobject xsd,
                                                  jlong pXSData)
{
    X11SDOps *xsdo;

    J2dTraceLn(J2D_TRACE_INFO, "in XRSurfaceData_freeXSDOPicture");

    xsdo = X11SurfaceData_GetOps(env, xsd);
    if (xsdo == NULL) {
        return;
    }

    if(xsdo->xrPic != None) {
       XRenderFreePicture(awt_display, xsdo->xrPic);
       xsdo->xrPic = None;
    }
}
