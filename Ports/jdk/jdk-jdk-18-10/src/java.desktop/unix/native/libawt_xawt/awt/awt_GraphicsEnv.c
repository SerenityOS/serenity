/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "jni_util.h"
#include "awt_p.h"
#include "awt.h"
#include "color.h"
#include <java_awt_DisplayMode.h>
#include <sun_awt_X11GraphicsEnvironment.h>
#include <sun_awt_X11GraphicsDevice.h>
#include <sun_awt_X11GraphicsConfig.h>
#include <X11/extensions/Xdbe.h>
#include <X11/XKBlib.h>
#ifndef NO_XRANDR
#include <X11/extensions/Xrandr.h>
#endif
#include "GLXGraphicsConfig.h"

#include <jni.h>
#include <jni_util.h>
#include <jvm.h>
#include <jvm_md.h>
#include <jlong.h>
#include "systemScale.h"
#include <stdlib.h>

#include "awt_GraphicsEnv.h"
#include "awt_util.h"
#include "gdefs.h"
#include <dlfcn.h>
#include "Trace.h"

int awt_numScreens;     /* Xinerama-aware number of screens */

AwtScreenDataPtr x11Screens; // should be guarded by AWT_LOCK()/AWT_UNLOCK()

/*
 * Set in initDisplay() to indicate whether we should attempt to initialize
 * GLX for the default configuration.
 */
static jboolean glxRequested = JNI_FALSE;

Display *awt_display;

jclass tkClass = NULL;
jmethodID awtLockMID = NULL;
jmethodID awtUnlockMID = NULL;
jmethodID awtWaitMID = NULL;
jmethodID awtNotifyMID = NULL;
jmethodID awtNotifyAllMID = NULL;
jboolean awtLockInited = JNI_FALSE;

/** Convenience macro for loading the lock-related method IDs. */
#define GET_STATIC_METHOD(klass, method_id, method_name, method_sig) \
    do { \
        method_id = (*env)->GetStaticMethodID(env, klass, \
                                              method_name, method_sig); \
        if (method_id == NULL) return NULL; \
    } while (0)

struct X11GraphicsConfigIDs x11GraphicsConfigIDs;

int awtCreateX11Colormap(AwtGraphicsConfigDataPtr adata);

static char *x11GraphicsConfigClassName = "sun/awt/X11GraphicsConfig";

/* AWT and Xinerama
 *
 * As of fix 4356756, AWT is Xinerama-aware.  X11GraphicsDevices are created for
 * each screen of a Xinerama setup, though X11 itself still only sees a single
 * display.
 * In many places where we talk to X11, a xinawareScreen variable is used to
 * pass the correct Display value, depending on the circumstances (a single
 * X display, multiple X displays, or a single X display with multiple
 * Xinerama screens).
 */

#define MAXFRAMEBUFFERS 16
typedef struct {
   int   screen_number;
   short x_org;
   short y_org;
   short width;
   short height;
} XineramaScreenInfo;

typedef XineramaScreenInfo* XineramaQueryScreensFunc(Display*, int*);
static XineramaQueryScreensFunc* XineramaQueryScreens = NULL;
Bool usingXinerama = False;

JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsConfig_initIDs (JNIEnv *env, jclass cls)
{
    x11GraphicsConfigIDs.aData = NULL;
    x11GraphicsConfigIDs.bitsPerPixel = NULL;

    x11GraphicsConfigIDs.aData = (*env)->GetFieldID (env, cls, "aData", "J");
    CHECK_NULL(x11GraphicsConfigIDs.aData);
    x11GraphicsConfigIDs.bitsPerPixel = (*env)->GetFieldID (env, cls, "bitsPerPixel", "I");
    CHECK_NULL(x11GraphicsConfigIDs.bitsPerPixel);
}

/*
 * XIOErrorHandler
 */
static int xioerror_handler(Display *disp)
{
    if (awtLockInited) {
        if (errno == EPIPE) {
            jio_fprintf(stderr, "X connection to %s host broken (explicit kill or server shutdown)\n", XDisplayName(NULL));
        }
        /*SignalError(lockedee->lastpc, lockedee, "fp/ade/gui/GUIException", "I/O error"); */
    }
    return 0;
}

static AwtGraphicsConfigDataPtr
findWithTemplate(XVisualInfo *vinfo,
                 long mask)
{

    XVisualInfo *visualList;
    XColor color;
    AwtGraphicsConfigDataPtr defaultConfig;
    int visualsMatched, i;

    visualList = XGetVisualInfo(awt_display,
                                mask, vinfo, &visualsMatched);
    if (visualList) {
        int id = -1;
        VisualID defaultVisual = XVisualIDFromVisual(DefaultVisual(awt_display, vinfo->screen));
        defaultConfig = ZALLOC(_AwtGraphicsConfigData);
        if (defaultConfig == NULL) {
            XFree(visualList);
            return NULL;
        }
        for (i = 0; i < visualsMatched; i++) {
            memcpy(&defaultConfig->awt_visInfo, &visualList[i], sizeof(XVisualInfo));
            defaultConfig->awt_depth = visualList[i].depth;

            /* we can't use awtJNI_CreateColorData here, because it'll pull,
               SystemColor, which in turn will cause toolkit to be reinitialized */
            if (awtCreateX11Colormap(defaultConfig)) {
                if (visualList[i].visualid == defaultVisual) {
                    id = i;
                    break;
                } else if (-1 == id) {
                    // Keep 1st match for fallback
                    id = i;
                }
            }
        }
        if (-1 != id) {
            memcpy(&defaultConfig->awt_visInfo, &visualList[id], sizeof(XVisualInfo));
            defaultConfig->awt_depth = visualList[id].depth;
            /* Allocate white and black pixels for this visual */
            color.flags = DoRed | DoGreen | DoBlue;
            color.red = color.green = color.blue = 0x0000;
            XAllocColor(awt_display, defaultConfig->awt_cmap, &color);
            x11Screens[visualList[id].screen].blackpixel = color.pixel;
            color.flags = DoRed | DoGreen | DoBlue;
            color.red = color.green = color.blue = 0xffff;
            XAllocColor(awt_display, defaultConfig->awt_cmap, &color);
            x11Screens[visualList[id].screen].whitepixel = color.pixel;

            XFree(visualList);
            return defaultConfig;
        }
        XFree(visualList);
        free((void *)defaultConfig);
    }
    return NULL;
}

/* default config is based on X11 screen.  All Xinerama screens of that X11
   screen will have the same default config */
/* Need more notes about which fields of the structure are based on the X
   screen, and which are based on the Xinerama screen */
static AwtGraphicsConfigDataPtr
makeDefaultConfig(JNIEnv *env, int screen) {

    AwtGraphicsConfigDataPtr defaultConfig;
    int xinawareScreen = 0;
    VisualID forcedVisualID = 0, defaultVisualID;
    char *forcedVisualStr;
    XVisualInfo vinfo;
    long mask;

    xinawareScreen = usingXinerama ? 0 : screen;
    defaultVisualID =
        XVisualIDFromVisual(DefaultVisual(awt_display, xinawareScreen));

    memset(&vinfo, 0, sizeof(XVisualInfo));
    vinfo.screen = xinawareScreen;

    if ((forcedVisualStr = getenv("FORCEDEFVIS"))) {
        mask = VisualIDMask | VisualScreenMask;
        if (sscanf(forcedVisualStr, "%lx", &forcedVisualID) > 0 &&
            forcedVisualID > 0)
        {
            vinfo.visualid = forcedVisualID;
        } else {
            vinfo.visualid = defaultVisualID;
        }
    } else {
        VisualID bestGLXVisualID;
        if (glxRequested &&
            (bestGLXVisualID = GLXGC_FindBestVisual(env, xinawareScreen)) > 0)
        {
            /* we've found the best visual for use with GLX, so use it */
            vinfo.visualid = bestGLXVisualID;
            mask = VisualIDMask | VisualScreenMask;
        } else {
            /* otherwise, continue looking for the best X11 visual */
            vinfo.depth = 24;
            vinfo.class = TrueColor;
            mask = VisualDepthMask | VisualScreenMask | VisualClassMask;
        }
    }

    /* try the best, or forced visual */
    defaultConfig = findWithTemplate(&vinfo, mask);
    if (defaultConfig) {
        return defaultConfig;
    }

    /* try the default visual */
    vinfo.visualid = defaultVisualID;
    mask = VisualIDMask | VisualScreenMask;
    defaultConfig = findWithTemplate(&vinfo, mask);
    if (defaultConfig) {
        return defaultConfig;
    }

    /* try any TrueColor */
    vinfo.class = TrueColor;
    mask = VisualScreenMask | VisualClassMask;
    defaultConfig = findWithTemplate(&vinfo, mask);
    if (defaultConfig) {
        return defaultConfig;
    }

    /* try 8-bit PseudoColor */
    vinfo.depth = 8;
    vinfo.class = PseudoColor;
    mask = VisualDepthMask | VisualScreenMask | VisualClassMask;
    defaultConfig = findWithTemplate(&vinfo, mask);
    if (defaultConfig) {
        return defaultConfig;
    }

    /* try any 8-bit */
    vinfo.depth = 8;
    mask = VisualDepthMask | VisualScreenMask;
    defaultConfig = findWithTemplate(&vinfo, mask);
    if (defaultConfig) {
        return defaultConfig;
    }

    /* we tried everything, give up */
    JNU_ThrowInternalError(env, "Can't find supported visual");
    XCloseDisplay(awt_display);
    awt_display = NULL;
    return NULL;
}

static void
getAllConfigs (JNIEnv *env, int screen, AwtScreenDataPtr screenDataPtr) {

    int i;
    int n8p=0, n12p=0, n8s=0, n8gs=0, n8sg=0, n1sg=0, nTrue=0;
    int nConfig;
    XVisualInfo *pVI8p, *pVI12p, *pVI8s, *pVITrue, *pVI8gs,
                *pVI8sg, *pVI1sg = NULL, viTmp;
    AwtGraphicsConfigDataPtr *graphicsConfigs;
    AwtGraphicsConfigDataPtr defaultConfig;
    int ind;
    char errmsg[128];
    int xinawareScreen;
    void* xrenderLibHandle = NULL;
    XRenderFindVisualFormatFunc* xrenderFindVisualFormat = NULL;
    int major_opcode, first_event, first_error;

    if (usingXinerama) {
        xinawareScreen = 0;
    }
    else {
        xinawareScreen = screen;
    }

    AWT_LOCK ();

    viTmp.screen = xinawareScreen;

    viTmp.depth = 8;
    viTmp.class = PseudoColor;
    viTmp.colormap_size = 256;
    pVI8p = XGetVisualInfo (awt_display,
                            VisualDepthMask | VisualClassMask |
                            VisualColormapSizeMask | VisualScreenMask,
                            &viTmp, &n8p);

    viTmp.depth = 12;
    viTmp.class = PseudoColor;
    viTmp.colormap_size = 4096;
    pVI12p = XGetVisualInfo (awt_display,
                             VisualDepthMask | VisualClassMask |
                             VisualColormapSizeMask | VisualScreenMask,
                             &viTmp, &n12p);

    viTmp.class = TrueColor;
    pVITrue = XGetVisualInfo (awt_display,
                              VisualClassMask |
                              VisualScreenMask,
                              &viTmp, &nTrue);

    viTmp.depth = 8;
    viTmp.class = StaticColor;
    pVI8s = XGetVisualInfo (awt_display, VisualDepthMask | VisualClassMask |
                            VisualScreenMask, &viTmp, &n8s);

    viTmp.depth = 8;
    viTmp.class = GrayScale;
    viTmp.colormap_size = 256;
    pVI8gs = XGetVisualInfo (awt_display,
                             VisualDepthMask | VisualClassMask |
                             VisualColormapSizeMask | VisualScreenMask,
                             &viTmp, &n8gs);
    viTmp.depth = 8;
    viTmp.class = StaticGray;
    viTmp.colormap_size = 256;
    pVI8sg = XGetVisualInfo (awt_display,
                             VisualDepthMask | VisualClassMask |
                             VisualColormapSizeMask | VisualScreenMask,
                             &viTmp, &n8sg);

/* REMIND.. remove when we have support for the color classes below */
/*     viTmp.depth = 1; */
/*     viTmp.class = StaticGray; */
/*     pVI1sg = XGetVisualInfo (awt_display, VisualDepthMask | VisualClassMask, */
/*                              viTmp, &n1sg); */

    nConfig = n8p + n12p + n8s + n8gs + n8sg  + n1sg + nTrue + 1;
    graphicsConfigs = (AwtGraphicsConfigDataPtr *)
        calloc(nConfig, sizeof(AwtGraphicsConfigDataPtr));
    if (graphicsConfigs == NULL) {
        JNU_ThrowOutOfMemoryError((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2),
                                  NULL);
        AWT_UNLOCK();
        return;
    }

    if (screenDataPtr->defaultConfig == NULL) {
        /*
         * After a display change event, the default config field will have
         * been reset, so we need to recreate the default config here.
         */
        screenDataPtr->defaultConfig = makeDefaultConfig(env, screen);
    }

    defaultConfig = screenDataPtr->defaultConfig;
    graphicsConfigs[0] = defaultConfig;
    nConfig = 1; /* reserve index 0 for default config */

    // Only use the RENDER extension if it is available on the X server
    if (XQueryExtension(awt_display, "RENDER",
                        &major_opcode, &first_event, &first_error))
    {
        DTRACE_PRINTLN("RENDER extension available");
        xrenderLibHandle = dlopen("libXrender.so.1", RTLD_LAZY | RTLD_GLOBAL);

        if (xrenderLibHandle == NULL) {
            xrenderLibHandle = dlopen("libXrender.so", RTLD_LAZY | RTLD_GLOBAL);
        }

#if defined(_AIX)
        if (xrenderLibHandle == NULL) {
            xrenderLibHandle = dlopen("libXrender.a(libXrender.so.0)",
                                      RTLD_MEMBER | RTLD_LAZY | RTLD_GLOBAL);
        }
#endif
        if (xrenderLibHandle != NULL) {
            DTRACE_PRINTLN("Loaded libXrender");
            xrenderFindVisualFormat =
                (XRenderFindVisualFormatFunc*)dlsym(xrenderLibHandle,
                                                    "XRenderFindVisualFormat");
            if (xrenderFindVisualFormat == NULL) {
                DTRACE_PRINTLN1("Can't find 'XRenderFindVisualFormat' in libXrender (%s)", dlerror());
            }
        } else {
            DTRACE_PRINTLN1("Can't load libXrender (%s)", dlerror());
        }
    } else {
        DTRACE_PRINTLN("RENDER extension NOT available");
    }

    for (i = 0; i < nTrue; i++) {
        if (XVisualIDFromVisual(pVITrue[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual) ||
            pVITrue[i].depth == 12) {
            /* Skip the non-supported 12-bit TrueColor visual */
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVITrue [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVITrue [i],
                sizeof (XVisualInfo));
        if (xrenderFindVisualFormat != NULL) {
            XRenderPictFormat *format = xrenderFindVisualFormat (awt_display,
                                                                 pVITrue [i].visual);
            if (format &&
                format->type == PictTypeDirect &&
                format->direct.alphaMask)
            {
                DTRACE_PRINTLN1("GraphicsConfig[%d] supports Translucency", ind);
                graphicsConfigs [ind]->isTranslucencySupported = 1;
                memcpy(&graphicsConfigs [ind]->renderPictFormat, format,
                        sizeof(*format));
            } else {
                DTRACE_PRINTLN1(format ?
                                "GraphicsConfig[%d] has no Translucency support" :
                                "Error calling 'XRenderFindVisualFormat'", ind);
            }
       }
    }

    if (xrenderLibHandle != NULL) {
        dlclose(xrenderLibHandle);
        xrenderLibHandle = NULL;
    }

    for (i = 0; i < n8p; i++) {
        if (XVisualIDFromVisual(pVI8p[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual)) {
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVI8p [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVI8p [i],
                sizeof (XVisualInfo));
    }

    for (i = 0; i < n12p; i++) {
        if (XVisualIDFromVisual(pVI12p[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual)) {
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVI12p [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVI12p [i],
                sizeof (XVisualInfo));
    }

    for (i = 0; i < n8s; i++) {
        if (XVisualIDFromVisual(pVI8s[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual)) {
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVI8s [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVI8s [i],
                sizeof (XVisualInfo));
    }

    for (i = 0; i < n8gs; i++) {
        if (XVisualIDFromVisual(pVI8gs[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual)) {
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVI8gs [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVI8gs [i],
                sizeof (XVisualInfo));
    }

    for (i = 0; i < n8sg; i++) {
        if (XVisualIDFromVisual(pVI8sg[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual)) {
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVI8sg [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVI8sg [i],
                sizeof (XVisualInfo));
    }

    for (i = 0; i < n1sg; i++) {
        if (XVisualIDFromVisual(pVI1sg[i].visual) ==
            XVisualIDFromVisual(defaultConfig->awt_visInfo.visual)) {
            continue;
        } else {
            ind = nConfig++;
        }
        graphicsConfigs[ind] = ZALLOC (_AwtGraphicsConfigData);
        if (graphicsConfigs[ind] == NULL) {
            JNU_ThrowOutOfMemoryError(env, "allocation in getAllConfigs failed");
            goto cleanup;
        }
        graphicsConfigs[ind]->awt_depth = pVI1sg [i].depth;
        memcpy (&graphicsConfigs [ind]->awt_visInfo, &pVI1sg [i],
                sizeof (XVisualInfo));
    }

    screenDataPtr->numConfigs = nConfig;
    screenDataPtr->configs = graphicsConfigs;

cleanup:
    if (n8p != 0)
       XFree (pVI8p);
    if (n12p != 0)
       XFree (pVI12p);
    if (n8s != 0)
       XFree (pVI8s);
    if (n8gs != 0)
       XFree (pVI8gs);
    if (n8sg != 0)
       XFree (pVI8sg);
    if (n1sg != 0)
       XFree (pVI1sg);

    AWT_UNLOCK ();
}

/*
 * Checks if Xinerama is running and perform Xinerama-related initialization.
 */
static void xineramaInit(void) {
    char* XinExtName = "XINERAMA";
    int32_t major_opcode, first_event, first_error;
    Bool gotXinExt = False;
    void* libHandle = NULL;
    char* XineramaQueryScreensName = "XineramaQueryScreens";

    gotXinExt = XQueryExtension(awt_display, XinExtName, &major_opcode,
                                &first_event, &first_error);

    if (!gotXinExt) {
        DTRACE_PRINTLN("Xinerama extension is not available");
        return;
    }

    DTRACE_PRINTLN("Xinerama extension is available");

    /* load library */
    libHandle = dlopen(VERSIONED_JNI_LIB_NAME("Xinerama", "1"),
                       RTLD_LAZY | RTLD_GLOBAL);
    if (libHandle == NULL) {
#if defined(_AIX)
        libHandle = dlopen("libXext.a(shr_64.o)", RTLD_MEMBER | RTLD_LAZY | RTLD_GLOBAL);
#else
        libHandle = dlopen(JNI_LIB_NAME("Xinerama"), RTLD_LAZY | RTLD_GLOBAL);
#endif
    }
    if (libHandle != NULL) {
        XineramaQueryScreens = (XineramaQueryScreensFunc*)
            dlsym(libHandle, XineramaQueryScreensName);

        if (XineramaQueryScreens == NULL) {
            DTRACE_PRINTLN("couldn't load XineramaQueryScreens symbol");
            dlclose(libHandle);
        }
    } else {
        DTRACE_PRINTLN1("\ncouldn't open shared library: %s\n", dlerror());
    }
}

static void resetNativeData(int screen) {
    /*
     * Reset references to the various configs; the actual native config data
     * will be free'd later by the Disposer mechanism when the Java-level
     * X11GraphicsConfig objects go away.  By setting these values to NULL,
     * we ensure that they will be reinitialized as necessary (for example,
     * see the getNumConfigs() method).
     */
    if (x11Screens[screen].configs) {
        free(x11Screens[screen].configs);
        x11Screens[screen].configs = NULL;
    }
    x11Screens[screen].defaultConfig = NULL;
    x11Screens[screen].numConfigs = 0;
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    initDevices
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsEnvironment_initNativeData(JNIEnv *env, jobject this) {
    usingXinerama = False;
    if (x11Screens) {
        for (int i = 0; i < awt_numScreens; ++i) {
            resetNativeData(i);
        }
        free((void *)x11Screens);
        x11Screens = NULL;
        awt_numScreens = 0;
    }

    // will try xinerama first
    if (XineramaQueryScreens) {
        int32_t locNumScr = 0;
        XineramaScreenInfo *xinInfo;
        DTRACE_PRINTLN("calling XineramaQueryScreens func");
        xinInfo = (*XineramaQueryScreens)(awt_display, &locNumScr);
        if (xinInfo != NULL) {
            if (locNumScr > XScreenCount(awt_display)) {
                DTRACE_PRINTLN("Enabling Xinerama support");
                usingXinerama = True;
                /* set global number of screens */
                DTRACE_PRINTLN1(" num screens = %i\n", locNumScr);
                awt_numScreens = locNumScr;
            } else {
                DTRACE_PRINTLN("XineramaQueryScreens <= XScreenCount");
            }
            XFree(xinInfo);
        } else {
            DTRACE_PRINTLN("calling XineramaQueryScreens didn't work");
        }
    }
    // if xinerama is not enabled or does not work will use X11
    if (!usingXinerama) {
        awt_numScreens =  XScreenCount(awt_display);
    }
    DTRACE_PRINTLN1("allocating %i screens\n", awt_numScreens);
    /* Allocate screen data structure array */
    x11Screens = calloc(awt_numScreens, sizeof(AwtScreenData));
    if (x11Screens == NULL) {
        JNU_ThrowOutOfMemoryError((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2),
                                  NULL);
        return;
    }

    for (int i = 0; i < awt_numScreens; i++) {
        if (usingXinerama) {
            /* All Xinerama screens use the same X11 root for now */
            x11Screens[i].root = RootWindow(awt_display, 0);
        }
        else {
            x11Screens[i].root = RootWindow(awt_display, i);
        }
        x11Screens[i].defaultConfig = makeDefaultConfig(env, i);
        JNU_CHECK_EXCEPTION(env);
    }
}

Display *
awt_init_Display(JNIEnv *env, jobject this)
{
    jclass klass;
    Display *dpy;
    char errmsg[128];
    int i;

    if (awt_display) {
        return awt_display;
    }

    /* Load AWT lock-related methods in SunToolkit */
    klass = (*env)->FindClass(env, "sun/awt/SunToolkit");
    if (klass == NULL) return NULL;
    GET_STATIC_METHOD(klass, awtLockMID, "awtLock", "()V");
    GET_STATIC_METHOD(klass, awtUnlockMID, "awtUnlock", "()V");
    GET_STATIC_METHOD(klass, awtWaitMID, "awtLockWait", "(J)V");
    GET_STATIC_METHOD(klass, awtNotifyMID, "awtLockNotify", "()V");
    GET_STATIC_METHOD(klass, awtNotifyAllMID, "awtLockNotifyAll", "()V");
    tkClass = (*env)->NewGlobalRef(env, klass);
    awtLockInited = JNI_TRUE;

    if (getenv("_AWT_IGNORE_XKB") != NULL &&
        strlen(getenv("_AWT_IGNORE_XKB")) > 0) {
        if (XkbIgnoreExtension(True)) {
            printf("Ignoring XKB.\n");
        }
    }

    dpy = awt_display = XOpenDisplay(NULL);
    if (!dpy) {
        jio_snprintf(errmsg,
                     sizeof(errmsg),
                     "Can't connect to X11 window server using '%s' as the value of the DISPLAY variable.",
                     (getenv("DISPLAY") == NULL) ? ":0.0" : getenv("DISPLAY"));
        JNU_ThrowByName(env, "java/awt/AWTError", errmsg);
        return NULL;
    }

    XSetIOErrorHandler(xioerror_handler);
    JNU_CallStaticMethodByName(env, NULL, "sun/awt/X11/XErrorHandlerUtil", "init", "(J)V",
        ptr_to_jlong(awt_display));
    JNU_CHECK_EXCEPTION_RETURN(env, NULL);

    /* set awt_numScreens, and whether or not we're using Xinerama */
    xineramaInit();
    return dpy;
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    getDefaultScreenNum
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsEnvironment_getDefaultScreenNum(
JNIEnv *env, jobject this)
{
    return DefaultScreen(awt_display);
}

static void ensureConfigsInited(JNIEnv* env, int screen) {
   if (x11Screens[screen].numConfigs == 0) {
       if (env == NULL) {
           env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
       }
       getAllConfigs (env, screen, &(x11Screens[screen]));
    }
}

AwtGraphicsConfigDataPtr
getDefaultConfig(int screen) {
    ensureConfigsInited(NULL, screen);
    return x11Screens[screen].defaultConfig;
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    initDisplay
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsEnvironment_initDisplay(JNIEnv *env, jobject this,
                                                jboolean glxReq)
{
    glxRequested = glxReq;
    (void) awt_init_Display(env, this);
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    initGLX
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_X11GraphicsEnvironment_initGLX(JNIEnv *env, jclass x11ge)
{
    jboolean glxAvailable;

    AWT_LOCK();
    glxAvailable = GLXGC_IsGLXAvailable();
    AWT_UNLOCK();

    return glxAvailable;
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    getNumScreens
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsEnvironment_getNumScreens(JNIEnv *env, jobject this)
{
    return awt_numScreens;
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getDisplay
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_sun_awt_X11GraphicsDevice_getDisplay(JNIEnv *env, jobject this)
{
    return ptr_to_jlong(awt_display);
}

#ifdef MITSHM

static jint canUseShmExt = UNSET_MITSHM;
static jint canUseShmExtPixmaps = UNSET_MITSHM;
static jboolean xshmAttachFailed = JNI_FALSE;

int XShmAttachXErrHandler(Display *display, XErrorEvent *xerr) {
    if (xerr->minor_code == X_ShmAttach) {
        xshmAttachFailed = JNI_TRUE;
    }
    return 0;
}
jboolean isXShmAttachFailed() {
    return xshmAttachFailed;
}
void resetXShmAttachFailed() {
    xshmAttachFailed = JNI_FALSE;
}

extern int mitShmPermissionMask;

void TryInitMITShm(JNIEnv *env, jint *shmExt, jint *shmPixmaps) {
    XShmSegmentInfo shminfo;
    int XShmMajor, XShmMinor;
    int a, b, c;

    AWT_LOCK();
    if (canUseShmExt != UNSET_MITSHM) {
        *shmExt = canUseShmExt;
        *shmPixmaps = canUseShmExtPixmaps;
        AWT_UNLOCK();
        return;
    }

    *shmExt = canUseShmExt = CANT_USE_MITSHM;
    *shmPixmaps = canUseShmExtPixmaps = CANT_USE_MITSHM;

    if (awt_display == (Display *)NULL) {
        AWT_NOFLUSH_UNLOCK();
        return;
    }

    /**
     * XShmQueryExtension returns False in remote server case.
     * Unfortunately it also returns True in ssh case, so
     * we need to test that we can actually do XShmAttach.
     */
    if (XShmQueryExtension(awt_display)) {
        shminfo.shmid = shmget(IPC_PRIVATE, 0x10000,
                               IPC_CREAT|mitShmPermissionMask);
        if (shminfo.shmid < 0) {
            AWT_UNLOCK();
            J2dRlsTraceLn1(J2D_TRACE_ERROR,
                           "TryInitMITShm: shmget has failed: %s",
                           strerror(errno));
            return;
        }
        shminfo.shmaddr = (char *) shmat(shminfo.shmid, 0, 0);
        if (shminfo.shmaddr == ((char *) -1)) {
            shmctl(shminfo.shmid, IPC_RMID, 0);
            AWT_UNLOCK();
            J2dRlsTraceLn1(J2D_TRACE_ERROR,
                           "TryInitMITShm: shmat has failed: %s",
                           strerror(errno));
            return;
        }
        shminfo.readOnly = True;

        resetXShmAttachFailed();
        /**
         * The J2DXErrHandler handler will set xshmAttachFailed
         * to JNI_TRUE if any Shm error has occured.
         */
        EXEC_WITH_XERROR_HANDLER(XShmAttachXErrHandler,
                                 XShmAttach(awt_display, &shminfo));

        /**
         * Get rid of the id now to reduce chances of leaking
         * system resources.
         */
        shmctl(shminfo.shmid, IPC_RMID, 0);

        if (isXShmAttachFailed() == JNI_FALSE) {
            canUseShmExt = CAN_USE_MITSHM;
            /* check if we can use shared pixmaps */
            XShmQueryVersion(awt_display, &XShmMajor, &XShmMinor,
                             (Bool*)&canUseShmExtPixmaps);
            canUseShmExtPixmaps = canUseShmExtPixmaps &&
                (XShmPixmapFormat(awt_display) == ZPixmap);
            XShmDetach(awt_display, &shminfo);
        }
        shmdt(shminfo.shmaddr);
        *shmExt = canUseShmExt;
        *shmPixmaps = canUseShmExtPixmaps;
    }
    AWT_UNLOCK();
}
#endif /* MITSHM */

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    checkShmExt
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsEnvironment_checkShmExt(JNIEnv *env, jobject this)
{

    int shmExt = NOEXT_MITSHM, shmPixmaps;
#ifdef MITSHM
    TryInitMITShm(env, &shmExt, &shmPixmaps);
#endif
    return shmExt;
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    getDisplayString
 * Signature: ()Ljava/lang/String
 */
JNIEXPORT jstring JNICALL
Java_sun_awt_X11GraphicsEnvironment_getDisplayString
  (JNIEnv *env, jobject this)
{
    return (*env)->NewStringUTF(env, DisplayString(awt_display));
}


/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getNumConfigs
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsDevice_getNumConfigs(
JNIEnv *env, jobject this, jint screen)
{
    AWT_LOCK();
    ensureConfigsInited(env, screen);
    int configs = x11Screens[screen].numConfigs;
    AWT_UNLOCK();
    return configs;
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getConfigVisualId
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsDevice_getConfigVisualId(
JNIEnv *env, jobject this, jint index, jint screen)
{
    int visNum;
    AWT_LOCK();
    ensureConfigsInited(env, screen);
    jint id = (jint) (index == 0 ? x11Screens[screen].defaultConfig
                                 : x11Screens[screen].configs[index])->awt_visInfo.visualid;
    AWT_UNLOCK();
    return id;
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getConfigDepth
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsDevice_getConfigDepth(
JNIEnv *env, jobject this, jint index, jint screen)
{
    int visNum;
    AWT_LOCK();
    ensureConfigsInited(env, screen);
    jint depth = (jint) (index == 0 ? x11Screens[screen].defaultConfig
                                    : x11Screens[screen].configs[index])->awt_visInfo.depth;
    AWT_UNLOCK();
    return depth;
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getConfigColormap
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsDevice_getConfigColormap(
JNIEnv *env, jobject this, jint index, jint screen)
{
    int visNum;
    AWT_LOCK();
    ensureConfigsInited(env, screen);
    jint colormap = (jint) (index == 0 ? x11Screens[screen].defaultConfig
                                       : x11Screens[screen].configs[index])->awt_cmap;
    AWT_UNLOCK();
    return colormap;
}


/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    dispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsConfig_dispose
    (JNIEnv *env, jclass x11gc, jlong configData)
{
    AwtGraphicsConfigDataPtr aData = (AwtGraphicsConfigDataPtr)
        jlong_to_ptr(configData);

    if (aData == NULL) {
        return;
    }

    AWT_LOCK();
    if (aData->awt_cmap) {
        XFreeColormap(awt_display, aData->awt_cmap);
    }
    if (aData->awtImage) {
        free(aData->awtImage);
    }
    if (aData->monoImage) {
        XFree(aData->monoImage);
    }
    if (aData->monoPixmap) {
        XFreePixmap(awt_display, aData->monoPixmap);
    }
    if (aData->monoPixmapGC) {
        XFreeGC(awt_display, aData->monoPixmapGC);
    }
    if (aData->color_data) {
        free(aData->color_data);
    }
    AWT_UNLOCK();

    if (aData->glxInfo) {
        /*
         * The native GLXGraphicsConfig data needs to be disposed separately
         * on the OGL queue flushing thread (should not be called while
         * the AWT lock is held).
         */
        JNU_CallStaticMethodByName(env, NULL,
                                   "sun/java2d/opengl/OGLRenderQueue",
                                   "disposeGraphicsConfig", "(J)V",
                                   ptr_to_jlong(aData->glxInfo));
    }

    free(aData);
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    getXResolution
 * Signature: ()I
 */
JNIEXPORT jdouble JNICALL
Java_sun_awt_X11GraphicsConfig_getXResolution(
JNIEnv *env, jobject this, jint screen)
{
    return ((DisplayWidth(awt_display, screen) * 25.4) /
            DisplayWidthMM(awt_display, screen));
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    getYResolution
 * Signature: ()I
 */
JNIEXPORT jdouble JNICALL
Java_sun_awt_X11GraphicsConfig_getYResolution(
JNIEnv *env, jobject this, jint screen)
{
    return ((DisplayHeight(awt_display, screen) * 25.4) /
            DisplayHeightMM(awt_display, screen));
}


/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    getNumColors
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_sun_awt_X11GraphicsConfig_getNumColors(
JNIEnv *env, jobject this)
{
    AwtGraphicsConfigData *adata;

    adata = (AwtGraphicsConfigData *) JNU_GetLongFieldAsPtr(env, this,
                                              x11GraphicsConfigIDs.aData);

    return adata->awt_num_colors;
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    init
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsConfig_init(
JNIEnv *env, jobject this, jint visualNum, jint screen)
{
    AwtGraphicsConfigData *adata = NULL;
    AWT_LOCK();
    AwtScreenData asd = x11Screens[screen];
    int i, n;
    int depth;
    XImage * tempImage;

    /* If haven't gotten all of the configs yet, do it now. */
    if (asd.numConfigs == 0) {
        getAllConfigs (env, screen, &asd);
    }

    /* Check the graphicsConfig for this visual */
    for (i = 0; i < asd.numConfigs; i++) {
        AwtGraphicsConfigDataPtr agcPtr = asd.configs[i];
        if ((jint)agcPtr->awt_visInfo.visualid == visualNum) {
           adata = agcPtr;
           break;
        }
    }

    /* If didn't find the visual, throw an exception... */
    if (adata == (AwtGraphicsConfigData *) NULL) {
        AWT_UNLOCK();
        JNU_ThrowIllegalArgumentException(env, "Unknown Visual Specified");
        return;
    }

    /*  adata->awt_cmap initialization has been deferred to
     *  makeColorModel call
     */

    JNU_SetLongFieldFromPtr(env, this, x11GraphicsConfigIDs.aData, adata);

    depth = adata->awt_visInfo.depth;
    tempImage = XCreateImage(awt_display,
                             adata->awt_visInfo.visual,
                             depth, ZPixmap, 0, NULL, 1, 1, 32, 0);
    adata->pixelStride = (tempImage->bits_per_pixel + 7) / 8;
    (*env)->SetIntField(env, this, x11GraphicsConfigIDs.bitsPerPixel,
                        (jint)tempImage->bits_per_pixel);
    XDestroyImage(tempImage);
    AWT_UNLOCK();
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    makeColorModel
 * Signature: ()Ljava/awt/image/ColorModel
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_X11GraphicsConfig_makeColorModel(
JNIEnv *env, jobject this)
{
    AwtGraphicsConfigData *adata;
    jobject colorModel;

    /*
     * If awt is not locked yet, return null since the toolkit is not
     * initialized yet.
     */
    if (!awtLockInited) {
        return NULL;
    }

    AWT_LOCK ();

    adata = (AwtGraphicsConfigData *) JNU_GetLongFieldAsPtr(env, this,
                                              x11GraphicsConfigIDs.aData);

    /* If colormap entry of adata is NULL, need to create it now */
    if (adata->awt_cmap == (Colormap) NULL) {
        awtJNI_CreateColorData (env, adata, 1);
    }

    /* Make Color Model object for this GraphicsConfiguration */
    colorModel = (*env)->ExceptionCheck(env)
                 ? NULL : awtJNI_GetColorModel (env, adata);

    AWT_UNLOCK ();

    return colorModel;
}


/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getBounds
 * Signature: ()Ljava/awt/Rectangle
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_X11GraphicsDevice_pGetBounds(JNIEnv *env, jobject this, jint screen)
{
    jclass clazz;
    jmethodID mid;
    jobject bounds = NULL;
    int32_t locNumScr = 0;
    XineramaScreenInfo *xinInfo;

    clazz = (*env)->FindClass(env, "java/awt/Rectangle");
    CHECK_NULL_RETURN(clazz, NULL);
    mid = (*env)->GetMethodID(env, clazz, "<init>", "(IIII)V");
    if (mid != NULL) {
        if (usingXinerama) {
            if (0 <= screen && screen < awt_numScreens) {
                AWT_LOCK();
                xinInfo = (*XineramaQueryScreens)(awt_display, &locNumScr);
                AWT_UNLOCK();
                if (xinInfo != NULL && locNumScr > 0) {
                    if (screen >= locNumScr) {
                        screen = 0; // fallback to the main screen
                    }
                    DASSERT(xinInfo[screen].screen_number == screen);
                    bounds = (*env)->NewObject(env, clazz, mid,
                                               xinInfo[screen].x_org,
                                               xinInfo[screen].y_org,
                                               xinInfo[screen].width,
                                               xinInfo[screen].height);
                    XFree(xinInfo);
                }
            } else {
                jclass exceptionClass = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
                if (exceptionClass != NULL) {
                    (*env)->ThrowNew(env, exceptionClass, "Illegal screen index");
                }
            }
        }
        if (!bounds) {
            // Xinerama cannot provide correct bounds, will try X11
            XWindowAttributes xwa;
            memset(&xwa, 0, sizeof(xwa));

            AWT_LOCK ();
            XGetWindowAttributes(awt_display, RootWindow(awt_display, screen),
                    &xwa);
            AWT_UNLOCK ();

            bounds = (*env)->NewObject(env, clazz, mid, 0, 0,
                    xwa.width, xwa.height);
        }

        if ((*env)->ExceptionOccurred(env)) {
            return NULL;
        }
    }
    return bounds;
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    createBackBuffer
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL
Java_sun_awt_X11GraphicsConfig_createBackBuffer
    (JNIEnv *env, jobject this, jlong window, jint swapAction)
{
    int32_t v1, v2;
    XdbeBackBuffer ret = (unsigned long) 0;
    Window w = (Window)window;
    AWT_LOCK();
    if (!XdbeQueryExtension(awt_display, &v1, &v2)) {
        JNU_ThrowByName(env, "java/lang/Exception",
                        "Could not query double-buffer extension");
        AWT_UNLOCK();
        return (jlong)0;
    }
    ret = XdbeAllocateBackBufferName(awt_display, w,
                                     (XdbeSwapAction)swapAction);
    AWT_FLUSH_UNLOCK();
    return (jlong)ret;
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    destroyBackBuffer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsConfig_destroyBackBuffer
    (JNIEnv *env, jobject this, jlong backBuffer)
{
    AWT_LOCK();
    XdbeDeallocateBackBufferName(awt_display, (XdbeBackBuffer)backBuffer);
    AWT_FLUSH_UNLOCK();
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    swapBuffers
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsConfig_swapBuffers
    (JNIEnv *env, jobject this,
     jlong window, jint swapAction)
{
    XdbeSwapInfo swapInfo;

    AWT_LOCK();

    XdbeBeginIdiom(awt_display);
    swapInfo.swap_window = (Window)window;
    swapInfo.swap_action = (XdbeSwapAction)swapAction;
    if (!XdbeSwapBuffers(awt_display, &swapInfo, 1)) {
        JNU_ThrowInternalError(env, "Could not swap buffers");
    }
    XdbeEndIdiom(awt_display);

    AWT_FLUSH_UNLOCK();
}

/*
 * Class:     sun_awt_X11GraphicsConfig
 * Method:    isTranslucencyCapable
 * Signature: (J)V
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_X11GraphicsConfig_isTranslucencyCapable
    (JNIEnv *env, jobject this, jlong configData)
{
    AwtGraphicsConfigDataPtr aData = (AwtGraphicsConfigDataPtr)jlong_to_ptr(configData);
    if (aData == NULL) {
        return JNI_FALSE;
    }
    return aData->isTranslucencySupported ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    isDBESupported
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_X11GraphicsDevice_isDBESupported(JNIEnv *env, jobject this)
{
    int opcode = 0, firstEvent = 0, firstError = 0;
    jboolean ret;

    AWT_LOCK();
    ret = (jboolean)XQueryExtension(awt_display, "DOUBLE-BUFFER",
                                    &opcode, &firstEvent, &firstError);
    AWT_FLUSH_UNLOCK();
    return ret;
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getDoubleBufferVisuals
 * Signature: (I)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsDevice_getDoubleBufferVisuals(JNIEnv *env,
    jobject this, jint screen)
{
    jclass clazz;
    jmethodID midAddVisual;
    Window rootWindow;
    int i, n = 1;
    XdbeScreenVisualInfo* visScreenInfo;
    int xinawareScreen;

    if (usingXinerama) {
        xinawareScreen = 0;
    }
    else {
        xinawareScreen = screen;
    }

    clazz = (*env)->GetObjectClass(env, this);
    midAddVisual = (*env)->GetMethodID(env, clazz, "addDoubleBufferVisual",
        "(I)V");
    CHECK_NULL(midAddVisual);
    AWT_LOCK();
    rootWindow = RootWindow(awt_display, xinawareScreen);
    visScreenInfo = XdbeGetVisualInfo(awt_display, &rootWindow, &n);
    if (visScreenInfo == NULL) {
        JNU_ThrowInternalError(env, "Could not get visual info");
        AWT_UNLOCK();
        return;
    }
    AWT_FLUSH_UNLOCK();
    for (i = 0; i < visScreenInfo->count; i++) {
        XdbeVisualInfo* visInfo = visScreenInfo->visinfo;
        (*env)->CallVoidMethod(env, this, midAddVisual, (visInfo[i]).visual);
        if ((*env)->ExceptionCheck(env)) {
            break;
        }
    }
}

/*
 * Class:     sun_awt_X11GraphicsEnvironment
 * Method:    pRunningXinerama
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_X11GraphicsEnvironment_pRunningXinerama(JNIEnv *env,
    jobject this)
{
    return usingXinerama ? JNI_TRUE : JNI_FALSE;
}

/**
 * Begin DisplayMode/FullScreen support
 */

#ifndef NO_XRANDR

#define BIT_DEPTH_MULTI java_awt_DisplayMode_BIT_DEPTH_MULTI
#define REFRESH_RATE_UNKNOWN java_awt_DisplayMode_REFRESH_RATE_UNKNOWN

typedef Status
    (*XRRQueryVersionType) (Display *dpy, int *major_versionp, int *minor_versionp);
typedef XRRScreenConfiguration*
    (*XRRGetScreenInfoType)(Display *dpy, Drawable root);
typedef void
    (*XRRFreeScreenConfigInfoType)(XRRScreenConfiguration *config);
typedef short*
    (*XRRConfigRatesType)(XRRScreenConfiguration *config,
                          int sizeID, int *nrates);
typedef short
    (*XRRConfigCurrentRateType)(XRRScreenConfiguration *config);
typedef XRRScreenSize*
    (*XRRConfigSizesType)(XRRScreenConfiguration *config,
                          int *nsizes);
typedef SizeID
    (*XRRConfigCurrentConfigurationType)(XRRScreenConfiguration *config,
                                         Rotation *rotation);
typedef Status
    (*XRRSetScreenConfigAndRateType)(Display *dpy,
                                     XRRScreenConfiguration *config,
                                     Drawable draw,
                                     int size_index,
                                     Rotation rotation,
                                     short rate,
                                     Time timestamp);
typedef Rotation
    (*XRRConfigRotationsType)(XRRScreenConfiguration *config,
                              Rotation *current_rotation);

typedef XRRScreenResources* (*XRRGetScreenResourcesType)(Display *dpy,
                                                                 Window window);

typedef void (*XRRFreeScreenResourcesType)(XRRScreenResources *resources);

typedef XRROutputInfo * (*XRRGetOutputInfoType)(Display *dpy,
                                XRRScreenResources *resources, RROutput output);

typedef void (*XRRFreeOutputInfoType)(XRROutputInfo *outputInfo);

typedef XRRCrtcInfo* (*XRRGetCrtcInfoType)(Display *dpy,
                                    XRRScreenResources *resources, RRCrtc crtc);

typedef void (*XRRFreeCrtcInfoType)(XRRCrtcInfo *crtcInfo);

static XRRQueryVersionType               awt_XRRQueryVersion;
static XRRGetScreenInfoType              awt_XRRGetScreenInfo;
static XRRFreeScreenConfigInfoType       awt_XRRFreeScreenConfigInfo;
static XRRConfigRatesType                awt_XRRConfigRates;
static XRRConfigCurrentRateType          awt_XRRConfigCurrentRate;
static XRRConfigSizesType                awt_XRRConfigSizes;
static XRRConfigCurrentConfigurationType awt_XRRConfigCurrentConfiguration;
static XRRSetScreenConfigAndRateType     awt_XRRSetScreenConfigAndRate;
static XRRConfigRotationsType            awt_XRRConfigRotations;
static XRRGetScreenResourcesType         awt_XRRGetScreenResources;
static XRRFreeScreenResourcesType        awt_XRRFreeScreenResources;
static XRRGetOutputInfoType              awt_XRRGetOutputInfo;
static XRRFreeOutputInfoType             awt_XRRFreeOutputInfo;
static XRRGetCrtcInfoType                awt_XRRGetCrtcInfo;
static XRRFreeCrtcInfoType               awt_XRRFreeCrtcInfo;

#define LOAD_XRANDR_FUNC(f) \
    do { \
        awt_##f = (f##Type)dlsym(pLibRandR, #f); \
        if (awt_##f == NULL) { \
            J2dRlsTraceLn1(J2D_TRACE_ERROR, \
                           "X11GD_InitXrandrFuncs: Could not load %s", #f); \
            dlclose(pLibRandR); \
            return JNI_FALSE; \
        } \
    } while (0)

static jboolean
X11GD_InitXrandrFuncs(JNIEnv *env)
{
    int rr_maj_ver = 0, rr_min_ver = 0;

    void *pLibRandR = dlopen(VERSIONED_JNI_LIB_NAME("Xrandr", "2"),
                             RTLD_LAZY | RTLD_LOCAL);
    if (pLibRandR == NULL) {
        pLibRandR = dlopen(JNI_LIB_NAME("Xrandr"), RTLD_LAZY | RTLD_LOCAL);
    }
    if (pLibRandR == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "X11GD_InitXrandrFuncs: Could not open libXrandr.so.2");
        return JNI_FALSE;
    }

    LOAD_XRANDR_FUNC(XRRQueryVersion);

    if (!(*awt_XRRQueryVersion)(awt_display, &rr_maj_ver, &rr_min_ver)) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "X11GD_InitXrandrFuncs: XRRQueryVersion returned an error status");
        dlclose(pLibRandR);
        return JNI_FALSE;
    }

    if (usingXinerama) {
        /*
         * We can proceed as long as this is RANDR 1.2 or above.
         * As of Xorg server 1.3 onwards the Xinerama backend may actually be
         * a fake one provided by RANDR itself. See Java bug 6636469 for info.
         */
        if (!(rr_maj_ver > 1 || (rr_maj_ver == 1 && rr_min_ver >= 2))) {
            J2dRlsTraceLn2(J2D_TRACE_INFO, "X11GD_InitXrandrFuncs: Can't use Xrandr. "
                           "Xinerama is active and Xrandr version is %d.%d",
                           rr_maj_ver, rr_min_ver);
            dlclose(pLibRandR);
            return JNI_FALSE;
        }

        /*
         * REMIND: Fullscreen mode doesn't work quite right with multi-monitor
         * setups and RANDR 1.2.
         */
        if ((rr_maj_ver == 1 && rr_min_ver <= 2) && awt_numScreens > 1) {
            J2dRlsTraceLn(J2D_TRACE_INFO, "X11GD_InitXrandrFuncs: Can't use Xrandr. "
                          "Multiple screens in use");
            dlclose(pLibRandR);
            return JNI_FALSE;
        }
    }

    LOAD_XRANDR_FUNC(XRRGetScreenInfo);
    LOAD_XRANDR_FUNC(XRRFreeScreenConfigInfo);
    LOAD_XRANDR_FUNC(XRRConfigRates);
    LOAD_XRANDR_FUNC(XRRConfigCurrentRate);
    LOAD_XRANDR_FUNC(XRRConfigSizes);
    LOAD_XRANDR_FUNC(XRRConfigCurrentConfiguration);
    LOAD_XRANDR_FUNC(XRRSetScreenConfigAndRate);
    LOAD_XRANDR_FUNC(XRRConfigRotations);
    LOAD_XRANDR_FUNC(XRRGetScreenResources);
    LOAD_XRANDR_FUNC(XRRFreeScreenResources);
    LOAD_XRANDR_FUNC(XRRGetOutputInfo);
    LOAD_XRANDR_FUNC(XRRFreeOutputInfo);
    LOAD_XRANDR_FUNC(XRRGetCrtcInfo);
    LOAD_XRANDR_FUNC(XRRFreeCrtcInfo);

    return JNI_TRUE;
}

static jobject
X11GD_CreateDisplayMode(JNIEnv *env, jint width, jint height,
                        jint bitDepth, jint refreshRate)
{
    jclass displayModeClass;
    jmethodID cid;
    jint validRefreshRate = refreshRate;

    displayModeClass = (*env)->FindClass(env, "java/awt/DisplayMode");
    CHECK_NULL_RETURN(displayModeClass, NULL);
    if (JNU_IsNull(env, displayModeClass)) {
        JNU_ThrowInternalError(env,
                               "Could not get display mode class");
        return NULL;
    }

    cid = (*env)->GetMethodID(env, displayModeClass, "<init>", "(IIII)V");
    CHECK_NULL_RETURN(cid, NULL);
    if (cid == NULL) {
        JNU_ThrowInternalError(env,
                               "Could not get display mode constructor");
        return NULL;
    }

    // early versions of xrandr may report "empty" rates (6880694)
    if (validRefreshRate <= 0) {
        validRefreshRate = REFRESH_RATE_UNKNOWN;
    }

    return (*env)->NewObject(env, displayModeClass, cid,
                             width, height, bitDepth, validRefreshRate);
}

static void
X11GD_AddDisplayMode(JNIEnv *env, jobject arrayList,
                     jint width, jint height,
                     jint bitDepth, jint refreshRate)
{
    jobject displayMode = X11GD_CreateDisplayMode(env, width, height,
                                                  bitDepth, refreshRate);
    if (!JNU_IsNull(env, displayMode)) {
        jclass arrayListClass;
        jmethodID mid;
        arrayListClass = (*env)->GetObjectClass(env, arrayList);
        if (JNU_IsNull(env, arrayListClass)) {
            JNU_ThrowInternalError(env,
                                   "Could not get class java.util.ArrayList");
            return;
        }
        mid = (*env)->GetMethodID(env, arrayListClass, "add",
                                  "(Ljava/lang/Object;)Z");
        CHECK_NULL(mid);
        if (mid == NULL) {
            JNU_ThrowInternalError(env,
                "Could not get method java.util.ArrayList.add()");
            return;
        }
        (*env)->CallObjectMethod(env, arrayList, mid, displayMode);
        (*env)->DeleteLocalRef(env, displayMode);
    }
}

#endif /* !NO_XRANDR */

static void
X11GD_SetFullscreenMode(Window win, jboolean enabled)
{
    Atom wmState = XInternAtom(awt_display, "_NET_WM_STATE", False);
    Atom wmStateFs = XInternAtom(awt_display,
                                 "_NET_WM_STATE_FULLSCREEN", False);
    XWindowAttributes attr;
    XEvent event;

    if (wmState == None || wmStateFs == None
            || !XGetWindowAttributes(awt_display, win, &attr)) {
        return;
    }

    memset(&event, 0, sizeof(event));
    event.xclient.type = ClientMessage;
    event.xclient.message_type = wmState;
    event.xclient.display = awt_display;
    event.xclient.window = win;
    event.xclient.format = 32;
    event.xclient.data.l[0] = enabled ? 1 : 0; // 1==add, 0==remove
    event.xclient.data.l[1] = wmStateFs;

    XSendEvent(awt_display, attr.root, False,
               SubstructureRedirectMask | SubstructureNotifyMask,
               &event);
    XSync(awt_display, False);
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    initXrandrExtension
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_sun_awt_X11GraphicsDevice_initXrandrExtension
    (JNIEnv *env, jclass x11gd)
{
#if defined(NO_XRANDR)
    return JNI_FALSE;
#else
    int opcode = 0, firstEvent = 0, firstError = 0;
    jboolean ret;

    AWT_LOCK();
    ret = (jboolean)XQueryExtension(awt_display, "RANDR",
                                    &opcode, &firstEvent, &firstError);
    if (ret) {
        ret = X11GD_InitXrandrFuncs(env);
    }
    AWT_FLUSH_UNLOCK();

    return ret;
#endif /* NO_XRANDR */
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getCurrentDisplayMode
 * Signature: (I)Ljava/awt/DisplayMode;
 */
JNIEXPORT jobject JNICALL
Java_sun_awt_X11GraphicsDevice_getCurrentDisplayMode
    (JNIEnv* env, jclass x11gd, jint screen)
{
#if defined(NO_XRANDR)
    return NULL;
#else
    XRRScreenConfiguration *config;
    jobject displayMode = NULL;

    AWT_LOCK();

    if (screen < ScreenCount(awt_display)) {

        config = awt_XRRGetScreenInfo(awt_display,
                                      RootWindow(awt_display, screen));
        if (config != NULL) {
            Rotation rotation;
            short curRate;
            SizeID curSizeIndex;
            XRRScreenSize *sizes;
            int nsizes;

            curSizeIndex = awt_XRRConfigCurrentConfiguration(config, &rotation);
            sizes = awt_XRRConfigSizes(config, &nsizes);
            curRate = awt_XRRConfigCurrentRate(config);

            if ((sizes != NULL) &&
                (curSizeIndex < nsizes))
            {
                XRRScreenSize curSize = sizes[curSizeIndex];
                displayMode = X11GD_CreateDisplayMode(env,
                                                      curSize.width,
                                                      curSize.height,
                                                      BIT_DEPTH_MULTI,
                                                      curRate);
            }

            awt_XRRFreeScreenConfigInfo(config);
        }
    }

    AWT_FLUSH_UNLOCK();

    return displayMode;
#endif /* NO_XRANDR */
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    enumDisplayModes
 * Signature: (ILjava/util/ArrayList;)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsDevice_enumDisplayModes
    (JNIEnv* env, jclass x11gd,
     jint screen, jobject arrayList)
{
#if !defined(NO_XRANDR)

    AWT_LOCK();

    if (XScreenCount(awt_display) > 0) {

        XRRScreenConfiguration *config;

        config = awt_XRRGetScreenInfo(awt_display,
                                      RootWindow(awt_display, screen));
        if (config != NULL) {
            int nsizes, i, j;
            XRRScreenSize *sizes = awt_XRRConfigSizes(config, &nsizes);

            if (sizes != NULL) {
                for (i = 0; i < nsizes; i++) {
                    int nrates;
                    XRRScreenSize size = sizes[i];
                    short *rates = awt_XRRConfigRates(config, i, &nrates);

                    for (j = 0; j < nrates; j++) {
                        X11GD_AddDisplayMode(env, arrayList,
                                             size.width,
                                             size.height,
                                             BIT_DEPTH_MULTI,
                                             rates[j]);
                        if ((*env)->ExceptionCheck(env)) {
                            goto ret1;
                        }
                    }
                }
            }
ret1:
            awt_XRRFreeScreenConfigInfo(config);
        }
    }

    AWT_FLUSH_UNLOCK();
#endif /* !NO_XRANDR */
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    configDisplayMode
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsDevice_configDisplayMode
    (JNIEnv* env, jclass x11gd,
     jint screen, jint width, jint height, jint refreshRate)
{
#if !defined(NO_XRANDR)
    jboolean success = JNI_FALSE;
    XRRScreenConfiguration *config;
    Drawable root;
    Rotation currentRotation = RR_Rotate_0;

    AWT_LOCK();

    root = RootWindow(awt_display, screen);
    config = awt_XRRGetScreenInfo(awt_display, root);
    if (config != NULL) {
        jboolean foundConfig = JNI_FALSE;
        int chosenSizeIndex = -1;
        short chosenRate = -1;
        int nsizes;
        XRRScreenSize *sizes = awt_XRRConfigSizes(config, &nsizes);
        awt_XRRConfigRotations(config, &currentRotation);

        if (sizes != NULL) {
            int i, j;

            /* find the size index that matches the requested dimensions */
            for (i = 0; i < nsizes; i++) {
                XRRScreenSize size = sizes[i];

                if ((size.width == width) && (size.height == height)) {
                    /* we've found our size index... */
                    int nrates;
                    short *rates = awt_XRRConfigRates(config, i, &nrates);

                    /* now find rate that matches requested refresh rate */
                    for (j = 0; j < nrates; j++) {
                        if (rates[j] == refreshRate) {
                            /* we've found our rate; break out of the loop */
                            chosenSizeIndex = i;
                            chosenRate = rates[j];
                            foundConfig = JNI_TRUE;
                            break;
                        }
                    }

                    break;
                }
            }
        }

        if (foundConfig) {
            Status status =
                awt_XRRSetScreenConfigAndRate(awt_display, config, root,
                                              chosenSizeIndex,
                                              currentRotation,
                                              chosenRate,
                                              CurrentTime);

            /* issue XSync to ensure immediate mode change */
            XSync(awt_display, False);

            if (status == RRSetConfigSuccess) {
                success = JNI_TRUE;
            }
        }

        awt_XRRFreeScreenConfigInfo(config);
    }

    AWT_FLUSH_UNLOCK();

    if (!success && !(*env)->ExceptionCheck(env)) {
        JNU_ThrowInternalError(env, "Could not set display mode");
    }
#endif /* !NO_XRANDR */
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    enterFullScreenExclusive
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsDevice_enterFullScreenExclusive
    (JNIEnv* env, jclass x11gd,
     jlong window)
{
    Window win = (Window)window;

    AWT_LOCK();
    XSync(awt_display, False); /* ensures window is visible first */
    X11GD_SetFullscreenMode(win, JNI_TRUE);
    AWT_UNLOCK();
}

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    exitFullScreenExclusive
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_sun_awt_X11GraphicsDevice_exitFullScreenExclusive
    (JNIEnv* env, jclass x11gd,
     jlong window)
{
    Window win = (Window)window;

    AWT_LOCK();
    X11GD_SetFullscreenMode(win, JNI_FALSE);
    AWT_UNLOCK();
}

/**
 * End DisplayMode/FullScreen support
 */

/*
 * Class:     sun_awt_X11GraphicsDevice
 * Method:    getNativeScaleFactor
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL
Java_sun_awt_X11GraphicsDevice_getNativeScaleFactor
    (JNIEnv *env, jobject this, jint screen) {

    return getNativeScaleFactor();
}
