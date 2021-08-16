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

#include "X11SurfaceData.h"
#include <jni.h>
#include <math.h>
#include "Region.h"
#include "fontscalerdefs.h"

#include <X11/extensions/Xrender.h>

#ifdef __linux__
    #include <sys/utsname.h>
#endif

#ifndef X_RenderCreateLinearGradient
typedef struct _XLinearGradient {
    XPointFixed p1;
    XPointFixed p2;
} XLinearGradient;
#endif

#ifndef X_RenderCreateRadialGradient
typedef struct _XCircle {
    XFixed x;
    XFixed y;
    XFixed radius;
} XCircle;

typedef struct _XRadialGradient {
    XCircle inner;
    XCircle outer;
} XRadialGradient;
#endif

#include <dlfcn.h>

#define BUILD_TRANSFORM_MATRIX(TRANSFORM, M00, M01, M02, M10, M11, M12)                        \
    {                                                                                          \
      TRANSFORM.matrix[0][0] = M00;                                                            \
      TRANSFORM.matrix[0][1] = M01;                                                            \
      TRANSFORM.matrix[0][2] = M02;                                                            \
      TRANSFORM.matrix[1][0] = M10;                                                            \
      TRANSFORM.matrix[1][1] = M11;                                                            \
      TRANSFORM.matrix[1][2] = M12;                                                            \
      TRANSFORM.matrix[2][0] = 0;                                                              \
      TRANSFORM.matrix[2][1] = 0;                                                              \
      TRANSFORM.matrix[2][2] = 1<<16;                                                          \
    }

/* The xrender pipleine requires libXrender.so version 0.9.3 or later. */
#define REQUIRED_XRENDER_VER1 0
#define REQUIRED_XRENDER_VER2 9
#define REQUIRED_XRENDER_VER3 3

#define PKGINFO_LINE_LEN_MAX 256
#define PKGINFO_LINE_CNT_MAX 50

/*
 * X protocol uses (u_int16)length to specify the length in 4 bytes quantities
 * of the whole request.  Both XRenderFillRectangles() and XFillRectangles()
 * have provisions to fragment into several requests if the number of rectangles
 * plus the current x request does not fit into 65535*4 bytes.  While
 * XRenderCreateLinearGradient() and XRenderCreateRadialGradient() have
 * provisions to gracefully degrade if the resulting request would exceed
 * 65535*4 bytes.
 *
 * Below, we define a cap of 65535*4 bytes for the maximum X request payload
 * allowed for Non-(XRenderFillRectangles() or XFillRectangles()) API calls,
 * just to be conservative.  This is offset by the size of our maximum x*Req
 * type in this compilation unit, which is xRenderCreateRadiaGradientReq.
 *
 * Note that sizeof(xRenderCreateRadiaGradientReq) = 36
 */
#define MAX_PAYLOAD (262140u - 36u)
#define MAXUINT (0xffffffffu)

static jboolean IsXRenderAvailable(jboolean verbose, jboolean ignoreLinuxVersion) {

    void *xrenderlib;

    int major_opcode, first_event, first_error;
    jboolean available = JNI_TRUE;

    if (!XQueryExtension(awt_display, "RENDER",
                         &major_opcode, &first_event, &first_error)) {
        return JNI_FALSE;
    }

#if defined(_AIX)
    // On AIX we have to use a special syntax because the shared libraries are packed in
    // multi-architecture archives. We first try to load the system default libXrender
    // which is contained in the 'X11.base.lib' fileset starting with AIX 6.1
    xrenderlib = dlopen("libXrender.a(shr_64.o)", RTLD_GLOBAL | RTLD_LAZY | RTLD_MEMBER);
    if (xrenderlib == NULL) {
      // If the latter wasn't successful, we also try to load the version under /opt/freeware
      // This may be downloaded from the "AIX Toolbox for Linux Applications" even for AIX 5.3
      xrenderlib = dlopen("libXrender.a(libXrender.so.0)", RTLD_GLOBAL | RTLD_LAZY | RTLD_MEMBER);
    }
    if (xrenderlib != NULL) {
      dlclose(xrenderlib);
    } else {
      available = JNI_FALSE;
    }
#else
    Dl_info info;
    jboolean versionInfoIsFound = JNI_FALSE;

    memset(&info, 0, sizeof(Dl_info));
    if (dladdr(&XRenderChangePicture, &info) && info.dli_fname != NULL) {
      char pkgInfoPath[FILENAME_MAX];
      char *pkgFileName = "/pkgconfig/xrender.pc";
      size_t pkgFileNameLen = strlen(pkgFileName);
      size_t pos, len = strlen(info.dli_fname);

      pos = len;
      while (pos > 0 && info.dli_fname[pos] != '/') {
        pos -= 1;
      }

      if (pos > 0 && pos < (FILENAME_MAX - pkgFileNameLen - 1)) {
        struct stat stat_info;

        // compose absolute filename to package config
        strncpy(pkgInfoPath, info.dli_fname, pos);

        strcpy(pkgInfoPath + pos, pkgFileName);
        pkgInfoPath[pos + pkgFileNameLen] = '\0';

        // check whether the config file exist and is a regular file
        if ((stat(pkgInfoPath, &stat_info)== 0) &&
            S_ISREG(stat_info.st_mode))
        {
          FILE *fp = fopen(pkgInfoPath, "r");
          if (fp != NULL) {
            char line[PKGINFO_LINE_LEN_MAX];
            int lineCount = PKGINFO_LINE_CNT_MAX;
            char *versionPrefix = "Version: ";
            size_t versionPrefixLen = strlen(versionPrefix);

            // look for version
            while(fgets(line,sizeof(line),fp) != NULL && --lineCount > 0) {
              size_t lineLen = strlen(line);

              if (lineLen > versionPrefixLen &&
                  strncmp(versionPrefix, line, versionPrefixLen) == 0)
              {
                int v1 = 0, v2 = 0, v3 = 0;
                int numNeeded = 3,numProcessed;
                char* version = line + versionPrefixLen;
                numProcessed = sscanf(version, "%d.%d.%d", &v1, &v2, &v3);

                if (numProcessed == numNeeded) {
                  // we successfuly read the library version
                  versionInfoIsFound = JNI_TRUE;

                  if (REQUIRED_XRENDER_VER1 == v1 &&
                      ((REQUIRED_XRENDER_VER2 > v2) ||
                       ((REQUIRED_XRENDER_VER2 == v2) && (REQUIRED_XRENDER_VER3 > v3))))
                  {
                    available = JNI_FALSE;

                    if (verbose) {
                      printf("INFO: the version %d.%d.%d of libXrender.so is "
                             "not supported.\n\tSee release notes for more details.\n",
                             v1, v2, v3);
                      fflush(stdout);
                    }
                  } else {
                    if (verbose) {
                      printf("INFO: The version of libXrender.so "
                             "is detected as %d.%d%d\n", v1, v2, v3);
                      fflush(stdout);
                    }
                  }
                }
                break;
              }
            }
            fclose(fp);
          }
        }
      }
    }
    if (verbose && !versionInfoIsFound) {
      printf("WARNING: The version of libXrender.so cannot be detected.\n,"
             "The pipe line will be enabled, but note that versions less than 0.9.3\n"
             "may cause hangs and crashes\n"
             "\tSee the release notes for more details.\n");
      fflush(stdout);
    }
#endif

#ifdef __linux__
    /*
     * Check for Linux >= 3.5 (Ubuntu 12.04.02 LTS) to avoid hitting
     * https://bugs.freedesktop.org/show_bug.cgi?id=48045
     */
    struct utsname utsbuf;
    if(uname(&utsbuf) >= 0) {
        int major, minor, revision;
        if(sscanf(utsbuf.release, "%i.%i.%i", &major, &minor, &revision) == 3) {
            if(major < 3 || (major == 3 && minor < 5)) {
                if(!ignoreLinuxVersion) {
                    available = JNI_FALSE;
                }
                else if(verbose) {
                 printf("WARNING: Linux < 3.5 detected.\n"
                        "The pipeline will be enabled, but graphical "
                        "artifacts can occur with old graphic drivers.\n"
                        "See the release notes for more details.\n");
                        fflush(stdout);
                }
            }
        }
    }
#endif // __linux__

    return available;
}
/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    initGLX
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_X11GraphicsEnvironment_initXRender
(JNIEnv *env, jclass x11ge, jboolean verbose, jboolean ignoreLinuxVersion)
{
    static jboolean xrenderAvailable = JNI_FALSE;
    static jboolean firstTime = JNI_TRUE;

    if (firstTime) {
#ifdef DISABLE_XRENDER_BY_DEFAULT
        if (verbose == JNI_FALSE) {
            xrenderAvailable = JNI_FALSE;
            firstTime = JNI_FALSE;
            return xrenderAvailable;
        }
#endif
        AWT_LOCK();
        xrenderAvailable = IsXRenderAvailable(verbose, ignoreLinuxVersion);
        AWT_UNLOCK();
        firstTime = JNI_FALSE;
    }
    return xrenderAvailable;
}


JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_initIDs(JNIEnv *env, jclass cls) {
    char *maskData;
    XImage* defaultImg;
    jfieldID maskImgID;
    jlong fmt8;
    jlong fmt32;

    jfieldID a8ID = (*env)->GetStaticFieldID(env, cls, "FMTPTR_A8", "J");
    if (a8ID == NULL) {
        return;
    }
    jfieldID argb32ID = (*env)->GetStaticFieldID(env, cls, "FMTPTR_ARGB32", "J");
    if (argb32ID == NULL) {
        return;
    }

    if (awt_display == (Display *)NULL) {
        return;
    }

    fmt8 = ptr_to_jlong(XRenderFindStandardFormat(awt_display, PictStandardA8));
    fmt32 = ptr_to_jlong(XRenderFindStandardFormat(awt_display, PictStandardARGB32));

    (*env)->SetStaticLongField(env, cls, a8ID, fmt8);
    (*env)->SetStaticLongField(env, cls, argb32ID, fmt32);

    maskData = (char *) malloc(32*32);
    if (maskData == NULL) {
       return;
    }

    defaultImg = XCreateImage(awt_display, NULL, 8, ZPixmap, 0, maskData, 32, 32, 8, 0);
    defaultImg->data = maskData; //required?
    maskImgID = (*env)->GetStaticFieldID(env, cls, "MASK_XIMG", "J");
    if (maskImgID == NULL) {
       return;
    }

    (*env)->SetStaticLongField(env, cls, maskImgID, ptr_to_jlong(defaultImg));
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_freeGC
 (JNIEnv *env, jobject this, jlong gc) {
    XFreeGC(awt_display, (GC) jlong_to_ptr(gc));
}

JNIEXPORT jlong JNICALL
Java_sun_java2d_xr_XRBackendNative_createGC
 (JNIEnv *env, jobject this, jint drawable) {
  GC xgc = XCreateGC(awt_display, (Drawable) drawable, 0L, NULL);
  return ptr_to_jlong(xgc);
}

JNIEXPORT jint JNICALL
Java_sun_java2d_xr_XRBackendNative_createPixmap(JNIEnv *env, jobject this,
                                                jint drawable, jint depth,
                                                jint width, jint height) {
    return (jint) XCreatePixmap(awt_display, (Drawable) drawable,
                                width, height, depth);
}

JNIEXPORT jint JNICALL
Java_sun_java2d_xr_XRBackendNative_createPictureNative
 (JNIEnv *env, jclass cls, jint drawable, jlong formatPtr) {
  XRenderPictureAttributes pict_attr;
  return XRenderCreatePicture(awt_display, (Drawable) drawable,
                              (XRenderPictFormat *) jlong_to_ptr(formatPtr),
                               0, &pict_attr);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_freePicture
 (JNIEnv *env, jobject this, jint picture) {
      XRenderFreePicture(awt_display, (Picture) picture);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_freePixmap
 (JNIEnv *env, jobject this, jint pixmap) {
   XFreePixmap(awt_display, (Pixmap) pixmap);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_setPictureRepeat
 (JNIEnv *env, jobject this, jint picture, jint repeat) {
    XRenderPictureAttributes pict_attr;
    pict_attr.repeat = repeat;
    XRenderChangePicture (awt_display, (Picture) picture, CPRepeat, &pict_attr);
}


JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_setGCExposures
 (JNIEnv *env, jobject this, jlong gc, jboolean exposure) {
    XSetGraphicsExposures(awt_display,
                         (GC) jlong_to_ptr(gc), exposure ? True : False); //TODO: ????
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_setGCForeground
 (JNIEnv *env, jobject this, jlong gc, jint pixel) {
    XSetForeground(awt_display, (GC) jlong_to_ptr(gc), (unsigned long) pixel);
}


JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_copyArea
 (JNIEnv *env, jobject this, jint src, jint dst, jlong gc,
  jint srcx, jint srcy, jint width, jint height, jint dstx, jint dsty) {
    XCopyArea(awt_display, (Drawable) src, (Drawable) dst,
             (GC) jlong_to_ptr(gc), srcx, srcy, width, height, dstx, dsty);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_renderComposite
 (JNIEnv *env, jobject this, jbyte op, jint src, jint mask, jint dst,
  jint srcX, jint srcY, jint maskX, jint maskY,
  jint dstX, jint dstY, jint width, jint height) {
    XRenderComposite (awt_display, op,
                      (Picture)src, (Picture)mask, (Picture)dst,
                       srcX, srcY, maskX, maskY, dstX, dstY, width, height);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_renderRectangle
 (JNIEnv *env, jobject this, jint dst, jbyte op,
  jshort red, jshort green, jshort blue, jshort alpha,
  jint x, jint y, jint width, jint height) {
    XRenderColor color;
    color.alpha = alpha;
    color.red = red;
    color.green = green;
    color.blue = blue;
    XRenderFillRectangle(awt_display, op, (Picture) dst, &color,
                         x, y, width, height);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_XRenderRectanglesNative
 (JNIEnv *env, jclass xsd, jint dst, jbyte op,
  jshort red, jshort green, jshort blue, jshort alpha,
  jintArray rectArray, jint rectCnt) {
    int i;
    jint* rects;
    XRectangle *xRects;
    XRectangle sRects[256];

    XRenderColor color;
    color.alpha = alpha;
    color.red = red;
    color.green = green;
    color.blue = blue;

    if (rectCnt <= 256) {
        xRects = &sRects[0];
    } else {
        if (MAXUINT / sizeof(XRectangle) < (unsigned)rectCnt) {
            /* rectCnt too big, integer overflow */
            return;
        }
        xRects = (XRectangle *) malloc(sizeof(XRectangle) * rectCnt);
        if (xRects == NULL) {
            return;
        }
    }

    if ((rects = (jint *)
         (*env)->GetPrimitiveArrayCritical(env, rectArray, NULL)) == NULL) {
        if (xRects != &sRects[0]) {
            free(xRects);
        }
        return;
    }

    for (i=0; i < rectCnt; i++) {
        xRects[i].x = rects[i*4 + 0];
        xRects[i].y = rects[i*4 + 1];
        xRects[i].width = rects[i*4 + 2];
        xRects[i].height = rects[i*4 + 3];
    }

    XRenderFillRectangles(awt_display, op,
                          (Picture) dst, &color, xRects, rectCnt);

    (*env)->ReleasePrimitiveArrayCritical(env, rectArray, rects, JNI_ABORT);
    if (xRects != &sRects[0]) {
        free(xRects);
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_XRSetTransformNative
 (JNIEnv *env, jclass xsd, jint pic,
  jint m00, jint m01, jint m02, jint m10, jint m11, jint m12) {

  XTransform tr;
  BUILD_TRANSFORM_MATRIX(tr, m00, m01, m02, m10, m11, m12);
  XRenderSetPictureTransform (awt_display, (Picture) pic, &tr);
}

JNIEXPORT jint JNICALL
Java_sun_java2d_xr_XRBackendNative_XRCreateLinearGradientPaintNative
    (JNIEnv *env, jclass xsd, jfloatArray fractionsArray,
     jshortArray pixelsArray, jint x1, jint y1, jint x2, jint y2,
     jint numStops, jint repeat) {
   jint i;
   jshort* pixels;
   jfloat* fractions;
   XRenderPictureAttributes pict_attr;
   Picture gradient = 0;
   XRenderColor *colors;
   XFixed *stops;
   XLinearGradient grad;

   if (MAX_PAYLOAD / (sizeof(XRenderColor) + sizeof(XFixed))
       < (unsigned)numStops) {
       /* numStops too big, payload overflow */
       return -1;
   }

   if ((pixels = (jshort *)
        (*env)->GetPrimitiveArrayCritical(env, pixelsArray, NULL)) == NULL) {
       return -1;
   }
   if ((fractions = (jfloat *)
       (*env)->GetPrimitiveArrayCritical(env, fractionsArray, NULL)) == NULL) {
       (*env)->ReleasePrimitiveArrayCritical(env,
                                              pixelsArray, pixels, JNI_ABORT);
       return -1;
   }

    grad.p1.x = x1;
    grad.p1.y = y1;
    grad.p2.x = x2;
    grad.p2.y = y2;

    /*TODO optimized & malloc check*/
    colors = (XRenderColor *) malloc(numStops * sizeof(XRenderColor));
    stops =  (XFixed *) malloc(numStops * sizeof(XFixed));

    if (colors == NULL || stops == NULL) {
        if (colors != NULL) {
            free(colors);
        }
        if (stops != NULL) {
            free(stops);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, pixelsArray, pixels, JNI_ABORT);
        (*env)->ReleasePrimitiveArrayCritical(env, fractionsArray, fractions, JNI_ABORT);
        return -1;
    }

    for (i=0; i < numStops; i++) {
      stops[i] = XDoubleToFixed(fractions[i]);
      colors[i].alpha = pixels[i*4 + 0];
      colors[i].red = pixels[i*4 + 1];
      colors[i].green = pixels[i*4 + 2];
      colors[i].blue = pixels[i*4 + 3];
    }
    gradient = XRenderCreateLinearGradient(awt_display, &grad, stops, colors, numStops);
    free(colors);
    free(stops);

   (*env)->ReleasePrimitiveArrayCritical(env, pixelsArray, pixels, JNI_ABORT);
   (*env)->ReleasePrimitiveArrayCritical(env, fractionsArray, fractions, JNI_ABORT);

    if (gradient != 0) {
        pict_attr.repeat = repeat;
        XRenderChangePicture (awt_display, gradient, CPRepeat, &pict_attr);
    }

   return (jint) gradient;
}


JNIEXPORT jint JNICALL
Java_sun_java2d_xr_XRBackendNative_XRCreateRadialGradientPaintNative
    (JNIEnv *env, jclass xsd, jfloatArray fractionsArray,
     jshortArray pixelsArray, jint numStops,
     jint centerX, jint centerY,
     jint innerRadius, jint outerRadius, jint repeat) {
   jint i;
   jshort* pixels;
   jfloat* fractions;
   XRenderPictureAttributes pict_attr;
   Picture gradient = 0;
   XRenderColor *colors;
   XFixed *stops;
   XRadialGradient grad;

   if (MAX_PAYLOAD / (sizeof(XRenderColor) + sizeof(XFixed))
       < (unsigned)numStops) {
       /* numStops too big, payload overflow */
       return -1;
   }

   if ((pixels =
       (jshort *)(*env)->GetPrimitiveArrayCritical(env, pixelsArray, NULL)) == NULL) {
       return -1;
   }
   if ((fractions = (jfloat *)
        (*env)->GetPrimitiveArrayCritical(env, fractionsArray, NULL)) == NULL) {
       (*env)->ReleasePrimitiveArrayCritical(env,
                                             pixelsArray, pixels, JNI_ABORT);
       return -1; //TODO release pixels first
   }

    grad.inner.x = centerX;
    grad.inner.y = centerY;
    grad.inner.radius = innerRadius;
    grad.outer.x = centerX;
    grad.outer.y = centerY;
    grad.outer.radius = outerRadius;

    /*TODO optimized & malloc check*/
    colors = (XRenderColor *) malloc(numStops * sizeof(XRenderColor));
    stops =  (XFixed *) malloc(numStops * sizeof(XFixed));

    if (colors == NULL || stops == NULL) {
        if (colors != NULL) {
            free(colors);
        }
        if (stops != NULL) {
            free(stops);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, pixelsArray, pixels, JNI_ABORT);
        (*env)->ReleasePrimitiveArrayCritical(env, fractionsArray, fractions, JNI_ABORT);
        return -1;
    }

    for (i=0; i < numStops; i++) {
      stops[i] = XDoubleToFixed(fractions[i]);
      colors[i].alpha = pixels[i*4 + 0];
      colors[i].red = pixels[i*4 + 1];
      colors[i].green = pixels[i*4 + 2];
      colors[i].blue = pixels[i*4 + 3];
    }
    gradient = (jint) XRenderCreateRadialGradient(awt_display, &grad, stops, colors, numStops);
    free(colors);
    free(stops);

   (*env)->ReleasePrimitiveArrayCritical(env, pixelsArray, pixels, JNI_ABORT);
   (*env)->ReleasePrimitiveArrayCritical(env, fractionsArray, fractions, JNI_ABORT);


    if (gradient != 0) {
        pict_attr.repeat = repeat;
        XRenderChangePicture (awt_display, gradient, CPRepeat, &pict_attr);
    }

   return (jint) gradient;
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_setFilter
 (JNIEnv *env, jobject this, jint picture, jint filter) {

  char * filterName = "fast";

  switch(filter) {
    case 0:
      filterName = "fast";
      break;

    case 1:
      filterName = "good";
      break;

    case 2:
      filterName = "best";
      break;
  }

    XRenderSetPictureFilter(awt_display, (Picture) picture, filterName, NULL, 0);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_XRSetClipNative
    (JNIEnv *env, jclass xsd, jlong dst,
     jint x1, jint y1, jint x2, jint y2,
     jobject complexclip, jboolean isGC)
{
    int numrects;
    XRectangle rects[256];
    XRectangle *pRect = rects;

    numrects = RegionToYXBandedRectangles(env,
            x1, y1, x2, y2, complexclip,
            &pRect, 256);

    if (isGC == JNI_TRUE) {
      if (dst != (jlong) 0) {
          XSetClipRectangles(awt_display, (GC) jlong_to_ptr(dst), 0, 0, pRect, numrects, YXBanded);
      }
    } else {
       XRenderSetPictureClipRectangles (awt_display, (Picture) dst, 0, 0, pRect, numrects);
    }

    if (pRect != rects) {
        free(pRect);
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_putMaskNative
 (JNIEnv *env, jclass cls, jint drawable, jlong gc, jbyteArray imageData,
  jint sx, jint sy, jint dx, jint dy, jint width, jint height,
  jint maskOff, jint maskScan, jfloat ea, jlong imgPtr) {

    int line, pix;
    char *mask;
    char *defaultData;
    XImage *defaultImg, *img;
    jboolean imageFits;

    if ((mask = (char *)
         (*env)->GetPrimitiveArrayCritical(env, imageData, NULL)) == NULL) {
        return;
     }

    defaultImg = (XImage *) jlong_to_ptr(imgPtr);

    if (ea != 1.0f) {
        for (line=0; line < height; line++) {
            for (pix=0; pix < width; pix++) {
                size_t index = (size_t) maskScan * line + pix + maskOff;
                mask[index] = (((unsigned char) mask[index])*ea);
            }
        }
    }

    /*
    * 1. If existing XImage and supplied buffer match, only adjust the data pointer
    * 2. If existing XImage is large enough to hold the data but does not match in
    *    scan the data is copied to fit the XImage.
    * 3. If data is larger than the existing XImage a new temporary XImage is
    *    allocated.
    * The default XImage is optimized for the AA tiles, which are currently 32x32.
    */
    defaultData = defaultImg->data;
    img = defaultImg;
    imageFits = defaultImg->width >= width && defaultImg->height >= height;

    if (imageFits &&
        maskOff == defaultImg->xoffset && maskScan == defaultImg->bytes_per_line) {
        defaultImg->data = mask;
    } else {
        if (imageFits) {
            for (line=0; line < height; line++) {
                for (pix=0; pix < width; pix++) {
                    img->data[(size_t) line * img->bytes_per_line + pix] =
                        (unsigned char) (mask[(size_t) maskScan * line + pix + maskOff]);
                }
            }
        } else {
            img = XCreateImage(awt_display, NULL, 8, ZPixmap,
                               maskOff, mask, maskScan, height, 8, 0);
        }
    }

    XPutImage(awt_display, (Pixmap) drawable, (GC) jlong_to_ptr(gc),
              img, 0, 0, 0, 0, width, height);
    (*env)->ReleasePrimitiveArrayCritical(env, imageData, mask, JNI_ABORT);

    if (img != defaultImg) {
        img->data = NULL;
        XDestroyImage(img);
    }
    defaultImg->data = defaultData;
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_XRAddGlyphsNative
 (JNIEnv *env, jclass cls, jint glyphSet,
  jlongArray glyphInfoPtrsArray, jint glyphCnt,
  jbyteArray pixelDataArray, int pixelDataLength) {
    jlong *glyphInfoPtrs;
    unsigned char *pixelData;
    int i;

    if (MAX_PAYLOAD / (sizeof(XGlyphInfo) + sizeof(Glyph))
        < (unsigned)glyphCnt) {
        /* glyphCnt too big, payload overflow */
        return;
    }

    XGlyphInfo *xginfo = (XGlyphInfo *) malloc(sizeof(XGlyphInfo) * glyphCnt);
    Glyph *gid = (Glyph *) malloc(sizeof(Glyph) * glyphCnt);

    if (xginfo == NULL || gid == NULL) {
        if (xginfo != NULL) {
            free(xginfo);
        }
        if (gid != NULL) {
            free(gid);
        }
        return;
    }

    if ((glyphInfoPtrs = (jlong *)(*env)->
        GetPrimitiveArrayCritical(env, glyphInfoPtrsArray, NULL)) == NULL)
    {
        free(xginfo);
        free(gid);
        return;
    }

    if ((pixelData = (unsigned char *)
        (*env)->GetPrimitiveArrayCritical(env, pixelDataArray, NULL)) == NULL)
    {
        (*env)->ReleasePrimitiveArrayCritical(env,
                                glyphInfoPtrsArray, glyphInfoPtrs, JNI_ABORT);
        free(xginfo);
        free(gid);
        return;
    }

    for (i=0; i < glyphCnt; i++) {
      GlyphInfo *jginfo = (GlyphInfo *) jlong_to_ptr(glyphInfoPtrs[i]);

      // 'jginfo->cellInfo' is of type 'void*'
      // (see definition of 'GlyphInfo' in fontscalerdefs.h)
      // 'Glyph' is typedefed to 'unsigned long'
      // (see http://www.x.org/releases/X11R7.7/doc/libXrender/libXrender.txt)
      // Maybe we should assert that (sizeof(void*) == sizeof(Glyph)) ?
      gid[i] = (Glyph) (jginfo->cellInfo);
      xginfo[i].x = (-jginfo->topLeftX);
      xginfo[i].y = (-jginfo->topLeftY);
      xginfo[i].width = jginfo->width;
      xginfo[i].height = jginfo->height;
      xginfo[i].xOff = round(jginfo->advanceX);
      xginfo[i].yOff = round(jginfo->advanceY);
    }

    XRenderAddGlyphs(awt_display, glyphSet, &gid[0], &xginfo[0], glyphCnt,
                     (const char*)pixelData, pixelDataLength);

    (*env)->ReleasePrimitiveArrayCritical(env, glyphInfoPtrsArray, glyphInfoPtrs, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, pixelDataArray, pixelData, JNI_ABORT);

    free(xginfo);
    free(gid);
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_XRFreeGlyphsNative
 (JNIEnv *env, jclass cls, jint glyphSet, jintArray gidArray, jint glyphCnt) {

    if (MAX_PAYLOAD / sizeof(Glyph) < (unsigned)glyphCnt) {
        /* glyphCnt too big, payload overflow */
        return;
    }

    /* The glyph ids are 32 bit but may be stored in a 64 bit long on
     * a 64 bit architecture. So optimise the 32 bit case to avoid
     * extra stack or heap allocations by directly referencing the
     * underlying Java array and only allocate on 64 bit.
     */
    if (sizeof(jint) == sizeof(Glyph)) {
        jint *gids =
            (*env)->GetPrimitiveArrayCritical(env, gidArray, NULL);
        if (gids == NULL) {
            return;
        } else {
             XRenderFreeGlyphs(awt_display,
                               (GlyphSet)glyphSet, (Glyph *)gids, glyphCnt);
             (*env)->ReleasePrimitiveArrayCritical(env, gidArray,
                                                   gids, JNI_ABORT);
        }
        return;
    } else {
        Glyph stack_ids[64];
        Glyph *gids = NULL;
        jint* jgids = NULL;
        int i;

        if (glyphCnt <= 64) {
            gids = stack_ids;
        } else {
            gids = (Glyph *)malloc(sizeof(Glyph) * glyphCnt);
            if (gids == NULL) {
                return;
            }
        }
        jgids = (*env)->GetPrimitiveArrayCritical(env, gidArray, NULL);
        if (jgids == NULL) {
            if (gids != stack_ids) {
                free(gids);
            }
            return;
        }
        for (i=0; i < glyphCnt; i++) {
            gids[i] = jgids[i];
        }
        XRenderFreeGlyphs(awt_display,
                          (GlyphSet) glyphSet, gids, glyphCnt);
        (*env)->ReleasePrimitiveArrayCritical(env, gidArray,
                                              jgids, JNI_ABORT);
        if (gids != stack_ids) {
            free(gids);
        }
    }
}

JNIEXPORT jint JNICALL
Java_sun_java2d_xr_XRBackendNative_XRenderCreateGlyphSetNative
 (JNIEnv *env, jclass cls, jlong format) {
  return XRenderCreateGlyphSet(awt_display, (XRenderPictFormat *) jlong_to_ptr(format));
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_XRenderCompositeTextNative
 (JNIEnv *env, jclass cls, jint op, jint src, jint dst,
  jint sx, jint sy, jlong maskFmt, jintArray eltArray,
  jintArray  glyphIDArray, jint eltCnt, jint glyphCnt) {
    jint i;
    jint *ids;
    jint *elts;
    XGlyphElt32 *xelts;
    unsigned int *xids;
    XGlyphElt32 selts[24];
    unsigned int sids[256];
    int charCnt = 0;

    if ((MAX_PAYLOAD / sizeof(XGlyphElt32) < (unsigned)eltCnt)
        || (MAX_PAYLOAD / sizeof(unsigned int) < (unsigned)glyphCnt)
        || ((MAX_PAYLOAD - sizeof(XGlyphElt32)*(unsigned)eltCnt) /
            sizeof(unsigned int) < (unsigned)glyphCnt))
    {
        /* (eltCnt, glyphCnt) too big, payload overflow */
        return;
    }

    if (eltCnt <= 24) {
      xelts = &selts[0];
    }else {
      xelts = (XGlyphElt32 *) malloc(sizeof(XGlyphElt32) * eltCnt);
      if (xelts == NULL) {
          return;
      }
    }

    if (glyphCnt <= 256) {
      xids = &sids[0];
    } else {
      xids = (unsigned int*)malloc(sizeof(unsigned int) * glyphCnt);
      if (xids == NULL) {
          if (xelts != &selts[0]) {
            free(xelts);
          }
          return;
      }
    }

    if ((ids = (jint *)
         (*env)->GetPrimitiveArrayCritical(env, glyphIDArray, NULL)) == NULL) {
        if (xelts != &selts[0]) {
            free(xelts);
        }
        if (xids != &sids[0]) {
            free(xids);
        }
        return;
    }
    if ((elts = (jint *)
          (*env)->GetPrimitiveArrayCritical(env, eltArray, NULL)) == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env,
                                              glyphIDArray, ids, JNI_ABORT);
        if (xelts != &selts[0]) {
            free(xelts);
        }
        if (xids != &sids[0]) {
            free(xids);
        }
        return;
    }

    for (i=0; i < glyphCnt; i++) {
      xids[i] = ids[i];
    }

    for (i=0; i < eltCnt; i++) {
      xelts[i].nchars = elts[i*4 + 0];
      xelts[i].xOff = elts[i*4 + 1];
      xelts[i].yOff = elts[i*4 + 2];
      xelts[i].glyphset = (GlyphSet) elts[i*4 + 3];
      xelts[i].chars = &xids[charCnt];

      charCnt += xelts[i].nchars;
    }

    XRenderCompositeText32(awt_display, op, (Picture) src, (Picture) dst,
                           (XRenderPictFormat *) jlong_to_ptr(maskFmt),
                            sx, sy, 0, 0, xelts, eltCnt);

    (*env)->ReleasePrimitiveArrayCritical(env, glyphIDArray, ids, JNI_ABORT);
    (*env)->ReleasePrimitiveArrayCritical(env, eltArray, elts, JNI_ABORT);

    if (xelts != &selts[0]) {
        free(xelts);
    }

    if (xids != &sids[0]) {
        free(xids);
    }
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_setGCMode
 (JNIEnv *env, jobject this, jlong gc, jboolean copy) {
  GC xgc = (GC) jlong_to_ptr(gc);

  if (copy == JNI_TRUE) {
    XSetFunction(awt_display, xgc, GXcopy);
  } else {
    XSetFunction(awt_display, xgc, GXxor);
  }
}

JNIEXPORT void JNICALL
Java_sun_java2d_xr_XRBackendNative_GCRectanglesNative
 (JNIEnv *env, jclass xsd, jint dst, jlong gc,
  jintArray rectArray, jint rectCnt) {
    int i;
    jint* rects;
    XRectangle *xRects;
    XRectangle sRects[256];

    if (rectCnt <= 256) {
      xRects = &sRects[0];
    } else {
      if (MAXUINT / sizeof(XRectangle) < (unsigned)rectCnt) {
        /* rectCnt too big, integer overflow */
        return;
      }

      xRects = (XRectangle *) malloc(sizeof(XRectangle) * rectCnt);
      if (xRects == NULL) {
        return;
      }
    }

    if ((rects = (jint*)
         (*env)->GetPrimitiveArrayCritical(env, rectArray, NULL)) == NULL) {
        if (xRects != &sRects[0]) {
            free(xRects);
        }
        return;
    }

    for (i=0; i < rectCnt; i++) {
      xRects[i].x = rects[i*4 + 0];
      xRects[i].y = rects[i*4 + 1];
      xRects[i].width = rects[i*4 + 2];
      xRects[i].height = rects[i*4 + 3];
    }

    XFillRectangles(awt_display, (Drawable) dst, (GC) jlong_to_ptr(gc), xRects, rectCnt);

    (*env)->ReleasePrimitiveArrayCritical(env, rectArray, rects, JNI_ABORT);
    if (xRects != &sRects[0]) {
      free(xRects);
    }
}
