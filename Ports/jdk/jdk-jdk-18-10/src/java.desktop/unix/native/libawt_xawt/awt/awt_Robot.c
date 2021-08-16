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

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#include "jvm_md.h"
#include <dlfcn.h>

#include "awt_p.h"
#include "awt_GraphicsEnv.h"
#define XK_MISCELLANY
#include <X11/keysymdef.h>
#include <X11/Xutil.h>
#include <X11/Xmd.h>
#include <X11/extensions/xtestext1.h>
#include <X11/extensions/XTest.h>
#include <X11/extensions/XInput.h>
#include <X11/extensions/XI.h>
#include <jni.h>
#include <sizecalc.h>
#include "canvas.h"
#include "wsutils.h"
#include "list.h"
#include "multiVis.h"
#include "gtk_interface.h"

#include "java_awt_event_InputEvent.h"

#if defined(__linux__)
#include <sys/socket.h>
#endif

static Bool   (*compositeQueryExtension)   (Display*, int*, int*);
static Status (*compositeQueryVersion)     (Display*, int*, int*);
static Window (*compositeGetOverlayWindow) (Display *, Window);

extern struct X11GraphicsConfigIDs x11GraphicsConfigIDs;

static jint * masks;
static jint num_buttons;

static void *xCompositeHandle;

static const char* XCOMPOSITE = JNI_LIB_NAME("Xcomposite");
static const char* XCOMPOSITE_VERSIONED = VERSIONED_JNI_LIB_NAME("Xcomposite", "1");

static Bool checkXCompositeFunctions(void) {
    return (compositeQueryExtension   != NULL   &&
            compositeQueryVersion     != NULL   &&
            compositeGetOverlayWindow != NULL);
}

static void initXCompositeFunctions(void) {

    if (xCompositeHandle == NULL) {
        xCompositeHandle = dlopen(XCOMPOSITE, RTLD_LAZY | RTLD_GLOBAL);
        if (xCompositeHandle == NULL) {
            xCompositeHandle = dlopen(XCOMPOSITE_VERSIONED, RTLD_LAZY | RTLD_GLOBAL);
        }
    }
    //*(void **)(&asyncGetCallTraceFunction)
    if (xCompositeHandle != NULL) {
        *(void **)(&compositeQueryExtension) = dlsym(xCompositeHandle, "XCompositeQueryExtension");
        *(void **)(&compositeQueryVersion) = dlsym(xCompositeHandle, "XCompositeQueryVersion");
        *(void **)(&compositeGetOverlayWindow) = dlsym(xCompositeHandle, "XCompositeGetOverlayWindow");
    }

    if (xCompositeHandle && !checkXCompositeFunctions()) {
        dlclose(xCompositeHandle);
    }
}

static int32_t isXTestAvailable() {
    int32_t major_opcode, first_event, first_error;
    int32_t  event_basep, error_basep, majorp, minorp;
    int32_t isXTestAvailable;

    /* check if XTest is available */
    isXTestAvailable = XQueryExtension(awt_display, XTestExtensionName, &major_opcode, &first_event, &first_error);
    if (isXTestAvailable) {
        DTRACE_PRINTLN3("RobotPeer: XQueryExtension(XTEST) returns major_opcode = %d, first_event = %d, first_error = %d",
                        major_opcode, first_event, first_error);
        /* check if XTest version is OK */
        XTestQueryExtension(awt_display, &event_basep, &error_basep, &majorp, &minorp);
        DTRACE_PRINTLN4("RobotPeer: XTestQueryExtension returns event_basep = %d, error_basep = %d, majorp = %d, minorp = %d",
                        event_basep, error_basep, majorp, minorp);
        if (majorp < 2 || (majorp == 2 && minorp < 2)) {
            /* bad version*/
            DTRACE_PRINTLN2("XRobotPeer: XTEST version is %d.%d \n", majorp, minorp);
            if (majorp == 2 && minorp == 1) {
                DTRACE_PRINTLN("XRobotPeer: XTEST is 2.1 - no grab is available\n");
            } else {
                isXTestAvailable = False;
            }
        } else {
            /* allow XTest calls even if someone else has the grab; e.g. during
             * a window resize operation. Works only with XTEST2.2*/
            XTestGrabControl(awt_display, True);
        }
    } else {
        DTRACE_PRINTLN("RobotPeer: XTEST extension is unavailable");
    }

    return isXTestAvailable;
}

static Bool hasXCompositeOverlayExtension(Display *display) {

    int xoverlay = False;
    int eventBase, errorBase;
    if (checkXCompositeFunctions() &&
        compositeQueryExtension(display, &eventBase, &errorBase))
    {
        int major = 0;
        int minor = 0;

        compositeQueryVersion(display, &major, &minor);
        if (major > 0 || minor >= 3) {
            xoverlay = True;
        }
    }

    return xoverlay;
}

static jboolean isXCompositeDisplay(Display *display, int screenNumber) {

    char NET_WM_CM_Sn[25];
    snprintf(NET_WM_CM_Sn, sizeof(NET_WM_CM_Sn), "_NET_WM_CM_S%d", screenNumber);

    Atom managerSelection = XInternAtom(display, NET_WM_CM_Sn, 0);
    Window owner = XGetSelectionOwner(display, managerSelection);

    return owner != 0;
}

static XImage *getWindowImage(Display * display, Window window,
                              int32_t x, int32_t y,
                              int32_t w, int32_t h) {
    XImage         *image;
    int32_t        transparentOverlays;
    int32_t        numVisuals;
    XVisualInfo    *pVisuals;
    int32_t        numOverlayVisuals;
    OverlayInfo    *pOverlayVisuals;
    int32_t        numImageVisuals;
    XVisualInfo    **pImageVisuals;
    list_ptr       vis_regions;    /* list of regions to read from */
    list_ptr       vis_image_regions ;
    int32_t        allImage = 0 ;
    int32_t        format = ZPixmap;

    /* prevent user from moving stuff around during the capture */
    XGrabServer(display);

    /*
     * The following two functions live in multiVis.c-- they are pretty
     * much verbatim taken from the source to the xwd utility from the
     * X11 source. This version of the xwd source was somewhat better written
     * for reuse compared to Sun's version.
     *
     *        ftp.x.org/pub/R6.3/xc/programs/xwd
     *
     * We use these functions since they do the very tough job of capturing
     * the screen correctly when it contains multiple visuals. They take into
     * account the depth/colormap of each visual and produce a capture as a
     * 24-bit RGB image so we don't have to fool around with colormaps etc.
     */

    GetMultiVisualRegions(
        display,
        window,
        x, y, w, h,
        &transparentOverlays,
        &numVisuals,
        &pVisuals,
        &numOverlayVisuals,
        &pOverlayVisuals,
        &numImageVisuals,
        &pImageVisuals,
        &vis_regions,
        &vis_image_regions,
        &allImage );

    image = ReadAreaToImage(
        display,
        window,
        x, y, w, h,
        numVisuals,
        pVisuals,
        numOverlayVisuals,
        pOverlayVisuals,
        numImageVisuals,
        pImageVisuals,
        vis_regions,
        vis_image_regions,
        format,
        allImage );

    /* allow user to do stuff again */
    XUngrabServer(display);

    /* make sure the grab/ungrab is flushed */
    XSync(display, False);

    return image;
}

/*********************************************************************************************/

// this should be called from XRobotPeer constructor
JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_setup (JNIEnv * env, jclass cls, jint numberOfButtons, jintArray buttonDownMasks)
{
    int32_t xtestAvailable;
    jint *tmp;
    int i;

    DTRACE_PRINTLN("RobotPeer: setup()");

    num_buttons = numberOfButtons;
    tmp = (*env)->GetIntArrayElements(env, buttonDownMasks, JNI_FALSE);
    CHECK_NULL(tmp);

    masks = (jint *)SAFE_SIZE_ARRAY_ALLOC(malloc, sizeof(jint), num_buttons);
    if (masks == (jint *) NULL) {
        (*env)->ExceptionClear(env);
        (*env)->ReleaseIntArrayElements(env, buttonDownMasks, tmp, 0);
        JNU_ThrowOutOfMemoryError((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), NULL);
        return;
    }
    for (i = 0; i < num_buttons; i++) {
        masks[i] = tmp[i];
    }
    (*env)->ReleaseIntArrayElements(env, buttonDownMasks, tmp, 0);

    AWT_LOCK();
    xtestAvailable = isXTestAvailable();
    DTRACE_PRINTLN1("RobotPeer: XTest available = %d", xtestAvailable);
    if (!xtestAvailable) {
        JNU_ThrowByName(env, "java/awt/AWTException", "java.awt.Robot requires your X server support the XTEST extension version 2.2");
    }

    AWT_UNLOCK();
}


JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_getRGBPixelsImpl( JNIEnv *env,
                             jclass cls,
                             jobject xgc,
                             jint jx,
                             jint jy,
                             jint jwidth,
                             jint jheight,
                             jintArray pixelArray,
                             jboolean useGtk) {
    XImage *image;
    jint *ary;               /* Array of jints for sending pixel values back
                              * to parent process.
                              */
    Window rootWindow;
    XWindowAttributes attr;
    AwtGraphicsConfigDataPtr adata;

    DTRACE_PRINTLN6("RobotPeer: getRGBPixelsImpl(%lx, %d, %d, %d, %d, %x)", xgc, jx, jy, jwidth, jheight, pixelArray);

    if (jwidth <= 0 || jheight <= 0) {
        return;
    }

    adata = (AwtGraphicsConfigDataPtr) JNU_GetLongFieldAsPtr(env, xgc, x11GraphicsConfigIDs.aData);
    DASSERT(adata != NULL);

    AWT_LOCK();

    rootWindow = XRootWindow(awt_display, adata->awt_visInfo.screen);

    if (!useGtk) {
        if (hasXCompositeOverlayExtension(awt_display) &&
            isXCompositeDisplay(awt_display, adata->awt_visInfo.screen))
        {
            rootWindow = compositeGetOverlayWindow(awt_display, rootWindow);
        }
    }

    if (!XGetWindowAttributes(awt_display, rootWindow, &attr)
            || jx + jwidth <= attr.x
            || attr.x + attr.width <= jx
            || jy + jheight <= attr.y
            || attr.y + attr.height <= jy) {

        AWT_UNLOCK();
        return; // Does not intersect with root window
    }

    gboolean gtk_failed = TRUE;
    jint _x, _y;

    jint x = MAX(jx, attr.x);
    jint y = MAX(jy, attr.y);
    jint width = MIN(jx + jwidth, attr.x + attr.width) - x;
    jint height = MIN(jy + jheight, attr.y + attr.height) - y;

    int dx = attr.x > jx ? attr.x - jx : 0;
    int dy = attr.y > jy ? attr.y - jy : 0;

    int index;

    if (useGtk) {
        gtk->gdk_threads_enter();
        gtk_failed = gtk->get_drawable_data(env, pixelArray, x, y, width,
                                            height, jwidth, dx, dy, 1);
        gtk->gdk_threads_leave();
    }

    if (gtk_failed) {
        image = getWindowImage(awt_display, rootWindow, x, y, width, height);

        ary = (*env)->GetPrimitiveArrayCritical(env, pixelArray, NULL);

        if (!ary) {
            XDestroyImage(image);
            AWT_UNLOCK();
            return;
        }

        /* convert to Java ARGB pixels */
        for (_y = 0; _y < height; _y++) {
            for (_x = 0; _x < width; _x++) {
                jint pixel = (jint) XGetPixel(image, _x, _y);
                                                              /* Note ignore upper
                                                               * 32-bits on 64-bit
                                                               * OSes.
                                                               */
                pixel |= 0xff000000; /* alpha - full opacity */

                index = (_y + dy) * jwidth + (_x + dx);
                ary[index] = pixel;
            }
        }

        XDestroyImage(image);
        (*env)->ReleasePrimitiveArrayCritical(env, pixelArray, ary, 0);
    }
    AWT_UNLOCK();
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_keyPressImpl (JNIEnv *env,
                         jclass cls,
                         jint keycode) {

    AWT_LOCK();

    DTRACE_PRINTLN1("RobotPeer: keyPressImpl(%i)", keycode);

    XTestFakeKeyEvent(awt_display,
                      XKeysymToKeycode(awt_display, awt_getX11KeySym(keycode)),
                      True,
                      CurrentTime);

    XSync(awt_display, False);

    AWT_UNLOCK();

}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_keyReleaseImpl (JNIEnv *env,
                           jclass cls,
                           jint keycode) {
    AWT_LOCK();

    DTRACE_PRINTLN1("RobotPeer: keyReleaseImpl(%i)", keycode);

    XTestFakeKeyEvent(awt_display,
                      XKeysymToKeycode(awt_display, awt_getX11KeySym(keycode)),
                      False,
                      CurrentTime);

    XSync(awt_display, False);

    AWT_UNLOCK();
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_mouseMoveImpl (JNIEnv *env,
                          jclass cls,
                          jobject xgc,
                          jint root_x,
                          jint root_y) {

    AwtGraphicsConfigDataPtr adata;

    AWT_LOCK();

    DTRACE_PRINTLN3("RobotPeer: mouseMoveImpl(%lx, %i, %i)", xgc, root_x, root_y);

    adata = (AwtGraphicsConfigDataPtr) JNU_GetLongFieldAsPtr(env, xgc, x11GraphicsConfigIDs.aData);
    DASSERT(adata != NULL);

    XWarpPointer(awt_display, None, XRootWindow(awt_display, adata->awt_visInfo.screen), 0, 0, 0, 0, root_x, root_y);
    XSync(awt_display, False);

    AWT_UNLOCK();
}

/*
  * Function joining the code of mousePressImpl and mouseReleaseImpl
  */
void mouseAction(JNIEnv *env,
                 jclass cls,
                 jint buttonMask,
                 Bool isMousePress)
{
    AWT_LOCK();

    DTRACE_PRINTLN1("RobotPeer: mouseAction(%i)", buttonMask);
    DTRACE_PRINTLN1("RobotPeer: mouseAction, press = %d", isMousePress);

    if (buttonMask & java_awt_event_InputEvent_BUTTON1_MASK ||
        buttonMask & java_awt_event_InputEvent_BUTTON1_DOWN_MASK )
    {
        XTestFakeButtonEvent(awt_display, 1, isMousePress, CurrentTime);
    }
    if ((buttonMask & java_awt_event_InputEvent_BUTTON2_MASK ||
         buttonMask & java_awt_event_InputEvent_BUTTON2_DOWN_MASK) &&
        (num_buttons >= 2)) {
        XTestFakeButtonEvent(awt_display, 2, isMousePress, CurrentTime);
    }
    if ((buttonMask & java_awt_event_InputEvent_BUTTON3_MASK ||
         buttonMask & java_awt_event_InputEvent_BUTTON3_DOWN_MASK) &&
        (num_buttons >= 3)) {
        XTestFakeButtonEvent(awt_display, 3, isMousePress, CurrentTime);
    }

    if (num_buttons > 3){
        int32_t i;
        int32_t button = 0;
        for (i = 3; i<num_buttons; i++){
            if ((buttonMask & masks[i])) {
                // arrays starts from zero index => +1
                // users wants to affect 4 or 5 button but they are assigned
                // to the wheel so => we have to shift it to the right by 2.
                button = i + 3;
                XTestFakeButtonEvent(awt_display, button, isMousePress, CurrentTime);
            }
        }
    }

    XSync(awt_display, False);
    AWT_UNLOCK();
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_mousePressImpl (JNIEnv *env,
                           jclass cls,
                           jint buttonMask) {
    mouseAction(env, cls, buttonMask, True);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_mouseReleaseImpl (JNIEnv *env,
                             jclass cls,
                             jint buttonMask) {
    mouseAction(env, cls, buttonMask, False);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_mouseWheelImpl (JNIEnv *env,
                           jclass cls,
                           jint wheelAmt) {
/* Mouse wheel is implemented as a button press of button 4 and 5, so it */
/* probably could have been hacked into robot_mouseButtonEvent, but it's */
/* cleaner to give it its own command type, in case the implementation   */
/* needs to be changed later.  -bchristi, 6/20/01                        */

    int32_t repeat = abs(wheelAmt);
    int32_t button = wheelAmt < 0 ? 4 : 5;  /* wheel up:   button 4 */
                                                 /* wheel down: button 5 */
    int32_t loopIdx;

    AWT_LOCK();

    DTRACE_PRINTLN1("RobotPeer: mouseWheelImpl(%i)", wheelAmt);

    for (loopIdx = 0; loopIdx < repeat; loopIdx++) { /* do nothing for   */
                                                     /* wheelAmt == 0    */
        XTestFakeButtonEvent(awt_display, button, True, CurrentTime);
        XTestFakeButtonEvent(awt_display, button, False, CurrentTime);
    }
    XSync(awt_display, False);

    AWT_UNLOCK();
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XRobotPeer_loadNativeLibraries (JNIEnv *env, jclass cls) {
    initXCompositeFunctions();
}
