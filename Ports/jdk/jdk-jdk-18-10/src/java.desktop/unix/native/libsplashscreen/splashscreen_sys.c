/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "splashscreen_impl.h"
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/shape.h>
#include <X11/Xmd.h>
#include <X11/Xatom.h>
#include <X11/cursorfont.h>
#include <sys/types.h>
#include <pthread.h>
#include <signal.h>
#include <unistd.h>
#include <sys/time.h>
#include <errno.h>
#include <iconv.h>
#include <langinfo.h>
#include <locale.h>
#include <fcntl.h>
#include <poll.h>
#include <sizecalc.h>
#include "jni.h"

static Bool shapeSupported;
static int shapeEventBase, shapeErrorBase;

void SplashRemoveDecoration(Splash * splash);


/* Could use npt but decided to cut down on linked code size */
char* SplashConvertStringAlloc(const char* in, int* size) {
    const char     *codeset;
    const char     *codeset_out;
    iconv_t         cd;
    size_t          rc;
    char           *buf = NULL, *out;
    size_t          bufSize, inSize, outSize;
    const char* old_locale;

    if (!in) {
        return NULL;
    }
    old_locale = setlocale(LC_ALL, "");

    codeset = nl_langinfo(CODESET);
    if ( codeset == NULL || codeset[0] == 0 ) {
        goto done;
    }
    /* we don't need BOM in output so we choose native BE or LE encoding here */
    codeset_out = (platformByteOrder()==BYTE_ORDER_MSBFIRST) ?
        "UCS-2BE" : "UCS-2LE";

    cd = iconv_open(codeset_out, codeset);
    if (cd == (iconv_t)-1 ) {
        goto done;
    }
    inSize = strlen(in);
    buf = SAFE_SIZE_ARRAY_ALLOC(malloc, inSize, 2);
    if (!buf) {
        return NULL;
    }
    bufSize = inSize*2; // need 2 bytes per char for UCS-2, this is
                        // 2 bytes per source byte max
    out = buf; outSize = bufSize;
    /* linux iconv wants char** source and solaris wants const char**...
       cast to void* */
    rc = iconv(cd, (void*)&in, &inSize, &out, &outSize);
    iconv_close(cd);

    if (rc == (size_t)-1) {
        free(buf);
        buf = NULL;
    } else {
        if (size) {
            *size = (bufSize-outSize)/2; /* bytes to wchars */
        }
    }
done:
    setlocale(LC_ALL, old_locale);
    return buf;
}

void
SplashInitFrameShape(Splash * splash, int imageIndex) {
    ImageRect maskRect;
    XRectangle *rects;
    SplashImage *frame = splash->frames + imageIndex;

    frame->rects = NULL;
    frame->numRects = 0;

    if (!splash->maskRequired)
        return;
    if (!shapeSupported)
        return;
    initRect(&maskRect, 0, 0, splash->width, splash->height, 1,
            splash->width * splash->imageFormat.depthBytes,
            splash->frames[imageIndex].bitmapBits, &splash->imageFormat);
    if (!IS_SAFE_SIZE_MUL(splash->width / 2 + 1, splash->height)) {
        return;
    }
    rects = SAFE_SIZE_ARRAY_ALLOC(malloc,
            sizeof(XRectangle), (splash->width / 2 + 1) * splash->height);
    if (!rects) {
        return;
    }

    frame->numRects = BitmapToYXBandedRectangles(&maskRect, rects);
    frame->rects = SAFE_SIZE_ARRAY_ALLOC(malloc, frame->numRects, sizeof(XRectangle));
    if (frame->rects) { // handle the error after the if(){}
        memcpy(frame->rects, rects, frame->numRects * sizeof(XRectangle));
    }
    free(rects);
}

unsigned
SplashTime(void) {
    struct timeval tv;
    struct timezone tz;
    unsigned long long msec;

    gettimeofday(&tv, &tz);
    msec = (unsigned long long) tv.tv_sec * 1000 +
        (unsigned long long) tv.tv_usec / 1000;

    return (unsigned) msec;
}

void
msec2timeval(unsigned time, struct timeval *tv) {
    tv->tv_sec = time / 1000;
    tv->tv_usec = (time % 1000) * 1000;
}

int
GetNumAvailableColors(Display * display, Screen * screen, unsigned map_entries) {
    unsigned long pmr[1];
    unsigned long pr[SPLASH_COLOR_MAP_SIZE];
    unsigned nFailed, nAllocated, done = 0, nPlanes = 0;
    Colormap cmap;
    unsigned numColors = SPLASH_COLOR_MAP_SIZE; // never try allocating more than that

    if (numColors > map_entries) {
        numColors = map_entries;
    }
    cmap = XDefaultColormapOfScreen(screen);
    nAllocated = 0;             /* lower bound */
    nFailed = numColors + 1;    /* upper bound */

    /* Binary search to determine the number of available cells */
    for (done = 0; !done;) {
        if (XAllocColorCells(display, cmap, 0, pmr, nPlanes, pr, numColors)) {
            nAllocated = numColors;
            XFreeColors(display, cmap, pr, numColors, 0);
            if (nAllocated < (nFailed - 1)) {
                numColors = (nAllocated + nFailed) / 2;
            } else
                done = 1;
        } else {
            nFailed = numColors;
            if (nFailed > (nAllocated + 1))
                numColors = (nAllocated + nFailed) / 2;
            else
                done = 1;
        }
    }
    return nAllocated;
}

Colormap
AllocColors(Display * display, Screen * screen, int numColors,
        unsigned long *pr) {
    unsigned long pmr[1];
    Colormap cmap = XDefaultColormapOfScreen(screen);

    XAllocColorCells(display, cmap, 0, pmr, 0, pr, numColors);
    return cmap;
}

void
FreeColors(Display * display, Screen * screen, int numColors,
        unsigned long *pr) {
    Colormap cmap = XDefaultColormapOfScreen(screen);

    XFreeColors(display, cmap, pr, numColors, 0);
}

static void SplashCenter(Splash * splash) {
    Atom type, atom, actual_type;
    int status, actual_format;
    unsigned long nitems, bytes_after;
    CARD16 *prop = NULL;

    /*  try centering using Xinerama hint
        if there's no hint, use the center of the screen */
    atom = XInternAtom(splash->display, "XINERAMA_CENTER_HINT", True);
    if (atom != None) {
        status = XGetWindowProperty(splash->display,
            XRootWindowOfScreen(splash->screen), atom, 0, 1, False, XA_INTEGER,
            &actual_type, &actual_format, &nitems,
            &bytes_after, (unsigned char**)(&prop));
        if (status == Success && actual_type != None && prop != NULL) {
            splash->x = prop[0] - splash->width/2;
            splash->y = prop[1] - splash->height/2;
            XFree(prop);
            return;
        }
        if (prop != NULL) {
            XFree(prop);
        }
    }
    splash->x = (XWidthOfScreen(splash->screen) - splash->width) / 2;
    splash->y = (XHeightOfScreen(splash->screen) - splash->height) / 2;
}

static void SplashUpdateSizeHints(Splash * splash) {
    if (splash->window) {
        XSizeHints sizeHints;

        sizeHints.flags = USPosition | PPosition | USSize | PSize | PMinSize | PMaxSize | PWinGravity;
        sizeHints.width = sizeHints.base_width = sizeHints.min_width = sizeHints.max_width = splash->width;
        sizeHints.height = sizeHints.base_height = sizeHints.min_height = sizeHints.max_height = splash->height;
        sizeHints.win_gravity = NorthWestGravity;

        XSetWMNormalHints(splash->display, splash->window, &sizeHints);
    }
}

void
SplashCreateWindow(Splash * splash) {
    XSizeHints sizeHints;

    XSetWindowAttributes attr;

    attr.backing_store = NotUseful;
    attr.colormap = XDefaultColormapOfScreen(splash->screen);
    attr.save_under = True;
    attr.cursor = splash->cursor = XCreateFontCursor(splash->display, XC_watch);
    attr.event_mask = ExposureMask;

    SplashCenter(splash);

    splash->window = XCreateWindow(splash->display, XRootWindowOfScreen(splash->screen),
        splash->x, splash->y, splash->width, splash->height, 0, CopyFromParent,
        InputOutput, CopyFromParent, CWColormap | CWBackingStore | CWSaveUnder | CWCursor | CWEventMask,
        &attr);
    SplashUpdateSizeHints(splash);


    splash->wmHints = XAllocWMHints();
    if (splash->wmHints) {
        splash->wmHints->flags = InputHint | StateHint;
        splash->wmHints->input = False;
        splash->wmHints->initial_state = NormalState;
        XSetWMHints(splash->display, splash->window, splash->wmHints);
    }
}

/* for changing the visible shape of a window to an nonrectangular form */
void
SplashUpdateShape(Splash * splash) {
    if (splash->currentFrame < 0 || !shapeSupported || !splash->maskRequired) {
        return;
    }
    XShapeCombineRectangles(splash->display, splash->window, ShapeClip, 0, 0,
            splash->frames[splash->currentFrame].rects,
            splash->frames[splash->currentFrame].numRects, ShapeSet, YXBanded);
    XShapeCombineRectangles(splash->display, splash->window, ShapeBounding,
            0, 0, splash->frames[splash->currentFrame].rects,
            splash->frames[splash->currentFrame].numRects, ShapeSet, YXBanded);
}

/* for reverting the visible shape of a window to an rectangular form */
void
SplashRevertShape(Splash * splash) {
    if (!shapeSupported)
        return;
    if (splash->maskRequired)
        return;

    XShapeCombineMask (splash->display, splash->window, ShapeClip,
                       0, 0, None, ShapeSet);
    XShapeCombineMask (splash->display, splash->window , ShapeBounding,
                       0, 0, None, ShapeSet);
}

int
ByteOrderToX(int byteOrder) {
    if (byteOrder == BYTE_ORDER_NATIVE)
        byteOrder = platformByteOrder();
    switch (byteOrder) {
    case BYTE_ORDER_LSBFIRST:
        return LSBFirst;
    case BYTE_ORDER_MSBFIRST:
        return MSBFirst;
    default:
        return -1;
    }
}

void
SplashRedrawWindow(Splash * splash) {
    if (splash->currentFrame < 0) {
        return;
    }

    XImage *ximage;

    // making this method redraw a part of the image does not make
    // much sense as SplashUpdateScreenData always re-generates
    // the image completely, so whole window is always redrawn

    SplashUpdateScreenData(splash);
    ximage = XCreateImage(splash->display, splash->visual,
            splash->screenFormat.depthBytes * 8, ZPixmap, 0, (char *) NULL,
            splash->width, splash->height, 8, 0);
    ximage->data = (char *) splash->screenData;
    ximage->bits_per_pixel = ximage->depth;
    ximage->bytes_per_line = ximage->depth * ximage->width / 8;
    ximage->byte_order = ByteOrderToX(splash->screenFormat.byteOrder);
    ximage->bitmap_unit = 8;
    XPutImage(splash->display, splash->window,
            XDefaultGCOfScreen(splash->screen), ximage, 0, 0, 0, 0,
            splash->width, splash->height);
    ximage->data = NULL;
    XDestroyImage(ximage);
    SplashRemoveDecoration(splash);
    XMapWindow(splash->display, splash->window);
    XFlush(splash->display);
}

void SplashReconfigureNow(Splash * splash) {
    SplashCenter(splash);
    if (splash->window) {
        XUnmapWindow(splash->display, splash->window);
        XMoveResizeWindow(splash->display, splash->window,
            splash->x, splash->y,
            splash->width, splash->height);
        SplashUpdateSizeHints(splash);
    }
    if (splash->maskRequired) {
        SplashUpdateShape(splash);
    } else {
        SplashRevertShape(splash);
    }
    SplashRedrawWindow(splash);
}


void
sendctl(Splash * splash, char code) {
//    if (splash->isVisible>0) {
    if (splash && splash->controlpipe[1]) {
        write(splash->controlpipe[1], &code, 1);
    }
}

int
HandleError(Display * disp, XErrorEvent * err) {
    // silently ignore non-fatal errors
    /*
    char msg[0x1000];
    char buf[0x1000];
    XGetErrorText(disp, err->error_code, msg, sizeof(msg));
    fprintf(stderr, "Xerror %s, XID %x, ser# %d\n", msg, err->resourceid,
        err->serial);
    sprintf(buf, "%d", err->request_code);
    XGetErrorDatabaseText(disp, "XRequest", buf, "Unknown", msg, sizeof(msg));
    fprintf(stderr, "Major opcode %d (%s)\n", err->request_code, msg);
    if (err->request_code > 128) {
        fprintf(stderr, "Minor opcode %d\n", err->minor_code);
    }
    */
    return 0;
}

int
HandleIOError(Display * display) {
    // for really bad errors, we should exit the thread we're on
    SplashCleanup(SplashGetInstance());
    pthread_exit(NULL);
    return 0;
}

int
SplashInitPlatform(Splash * splash) {
    int shapeVersionMajor, shapeVersionMinor;

    // This setting enables the synchronous Xlib mode!
    // Don't use it == 1 in production builds!
#if (defined DEBUG)
    _Xdebug = 1;
#endif

    pthread_mutex_init(&splash->lock, NULL);

    // We should not ignore any errors.
    //XSetErrorHandler(HandleError);
//    XSetIOErrorHandler(HandleIOError);
    XSetIOErrorHandler(NULL);
    splash->display = XOpenDisplay(NULL);
    if (!splash->display) {
        splash->isVisible = -1;
        return 0;
    }

    shapeSupported = XShapeQueryExtension(splash->display, &shapeEventBase,
            &shapeErrorBase);
    if (shapeSupported) {
        XShapeQueryVersion(splash->display, &shapeVersionMajor,
                &shapeVersionMinor);
    }

    splash->screen = XDefaultScreenOfDisplay(splash->display);
    splash->visual = XDefaultVisualOfScreen(splash->screen);
    switch (splash->visual->class) {
    case TrueColor: {
            int depth = XDefaultDepthOfScreen(splash->screen);

            splash->byteAlignment = 1;
            splash->maskRequired = shapeSupported;
            initFormat(&splash->screenFormat, splash->visual->red_mask,
                    splash->visual->green_mask, splash->visual->blue_mask, 0);
            splash->screenFormat.byteOrder =
                (XImageByteOrder(splash->display) == LSBFirst ?
                 BYTE_ORDER_LSBFIRST : BYTE_ORDER_MSBFIRST);
            splash->screenFormat.depthBytes = (depth + 7) / 8;
            // TrueColor depth probably can't be less
            // than 8 bits, and it's always byte padded
            break;
        }
    case PseudoColor: {
            int availableColors;
            int numColors;
            int numComponents[3];
            unsigned long colorIndex[SPLASH_COLOR_MAP_SIZE];
            XColor xColors[SPLASH_COLOR_MAP_SIZE];
            int i;
            int depth = XDefaultDepthOfScreen(splash->screen);
            int scale = 65535 / MAX_COLOR_VALUE;

            availableColors = GetNumAvailableColors(splash->display, splash->screen,
                    splash->visual->map_entries);
            numColors = quantizeColors(availableColors, numComponents);
            if (numColors > availableColors) {
                // Could not allocate the color cells. Most probably
                // the pool got exhausted. Disable the splash screen.
                XCloseDisplay(splash->display);
                splash->isVisible = -1;
                splash->display = NULL;
                splash->screen = NULL;
                splash->visual = NULL;
                fprintf(stderr, "Warning: unable to initialize the splashscreen. Not enough available color cells.\n");
                return 0;
            }
            splash->cmap = AllocColors(splash->display, splash->screen,
                    numColors, colorIndex);
            for (i = 0; i < numColors; i++) {
                splash->colorIndex[i] = colorIndex[i];
            }
            initColorCube(numComponents, splash->colorMap, splash->dithers,
                    splash->colorIndex);
            for (i = 0; i < numColors; i++) {
                xColors[i].pixel = colorIndex[i];
                xColors[i].red = (unsigned short)
                    QUAD_RED(splash->colorMap[colorIndex[i]]) * scale;
                xColors[i].green = (unsigned short)
                    QUAD_GREEN(splash->colorMap[colorIndex[i]]) * scale;
                xColors[i].blue = (unsigned short)
                    QUAD_BLUE(splash->colorMap[colorIndex[i]]) * scale;
                xColors[i].flags = DoRed | DoGreen | DoBlue;
            }
            XStoreColors(splash->display, splash->cmap, xColors, numColors);
            initFormat(&splash->screenFormat, 0, 0, 0, 0);
            splash->screenFormat.colorIndex = splash->colorIndex;
            splash->screenFormat.depthBytes = (depth + 7) / 8;  // or always 8?
            splash->screenFormat.colorMap = splash->colorMap;
            splash->screenFormat.dithers = splash->dithers;
            splash->screenFormat.numColors = numColors;
            splash->screenFormat.byteOrder = BYTE_ORDER_NATIVE;
            break;
        }
    default:
        ; /* FIXME: should probably be fixed, but javaws splash screen doesn't support other visuals either */
    }
    return 1;
}


void
SplashCleanupPlatform(Splash * splash) {
    int i;

    if (splash->frames) {
        for (i = 0; i < splash->frameCount; i++) {
            if (splash->frames[i].rects) {
                free(splash->frames[i].rects);
                splash->frames[i].rects = NULL;
            }
        }
    }
    splash->maskRequired = shapeSupported;
}

void
SplashDonePlatform(Splash * splash) {
    pthread_mutex_destroy(&splash->lock);
    if (splash->cmap) {
        unsigned long colorIndex[SPLASH_COLOR_MAP_SIZE];
        int i;

        for (i = 0; i < splash->screenFormat.numColors; i++) {
            colorIndex[i] = splash->colorIndex[i];
        }
        FreeColors(splash->display, splash->screen,
                splash->screenFormat.numColors, colorIndex);
    }
    if (splash->window)
        XDestroyWindow(splash->display, splash->window);
    if (splash->wmHints)
        XFree(splash->wmHints);
    if (splash->cursor)
        XFreeCursor(splash->display, splash->cursor);
    if (splash->display)
        XCloseDisplay(splash->display);
}

void
SplashEventLoop(Splash * splash) {

    /*      Different from win32 implementation - this loop
       uses poll timeouts instead of a timer */
    /* we should have splash _locked_ on entry!!! */

    int xconn = XConnectionNumber(splash->display);

    while (1) {
        struct pollfd pfd[2];
        int timeout = -1;
        int ctl = splash->controlpipe[0];
        int rc;
        int pipes_empty;

        pfd[0].fd = xconn;
        pfd[0].events = POLLIN | POLLPRI;

        pfd[1].fd = ctl;
        pfd[1].events = POLLIN | POLLPRI;

        errno = 0;
        if (splash->isVisible>0 && SplashIsStillLooping(splash)) {
            timeout = splash->time + splash->frames[splash->currentFrame].delay
                - SplashTime();
            if (timeout < 0) {
                timeout = 0;
            }
        }
        SplashUnlock(splash);
        rc = poll(pfd, 2, timeout);
        SplashLock(splash);
        if (splash->isVisible > 0 && splash->currentFrame >= 0 &&
                SplashTime() >= splash->time + splash->frames[splash->currentFrame].delay) {
            SplashNextFrame(splash);
            SplashUpdateShape(splash);
            SplashRedrawWindow(splash);
        }
        if (rc <= 0) {
            errno = 0;
            continue;
        }
        pipes_empty = 0;
        while(!pipes_empty) {
            char buf;

            pipes_empty = 1;
            if (read(ctl, &buf, sizeof(buf)) > 0) {
                pipes_empty = 0;
                switch (buf) {
                case SPLASHCTL_UPDATE:
                    if (splash->isVisible>0) {
                        SplashRedrawWindow(splash);
                    }
                    break;
                case SPLASHCTL_RECONFIGURE:
                    if (splash->isVisible>0) {
                        SplashReconfigureNow(splash);
                    }
                    break;
                case SPLASHCTL_QUIT:
                    return;
                }
            }
            // we're not using "while(XPending)", processing one event
            // at a time to avoid control pipe starvation
            if (XPending(splash->display)) {
                XEvent evt;

                pipes_empty = 0;
                XNextEvent(splash->display, &evt);
                switch (evt.type) {
                    case Expose:
                        if (splash->isVisible>0) {
                            // we're doing full redraw so we just
                            // skip the remaining painting events in the queue
                            while(XCheckTypedEvent(splash->display, Expose,
                                &evt));
                            SplashRedrawWindow(splash);
                        }
                        break;
                    /* ... */
                }
            }
        }
    }
}

/*  we can't use OverrideRedirect for the window as the window should not be
    always-on-top, so we must set appropriate wm hints

    this functions sets olwm, mwm and EWMH hints for undecorated window at once

    It works for: mwm, openbox, wmaker, metacity, KWin (FIXME: test more wm's)
    Should work for: fvwm2.5.x, blackbox, olwm
    Maybe works for: enlightenment, icewm
    Does not work for: twm, fvwm2.4.7

*/

void
SplashRemoveDecoration(Splash * splash) {
    Atom atom_set;
    Atom atom_list[4];

    /* the struct below was copied from MwmUtil.h */

    struct PROPMOTIFWMHINTS {
    /* 32-bit property items are stored as long on the client (whether
     * that means 32 bits or 64).  XChangeProperty handles the conversion
     * to the actual 32-bit quantities sent to the server.
     */
        unsigned long   flags;
        unsigned long   functions;
        unsigned long   decorations;
        long            inputMode;
        unsigned long   status;
    }
    mwm_hints;

    /* WM_TAKE_FOCUS hint to avoid wm's transfer of focus to this window */
    /* WM_DELETE_WINDOW hint to avoid closing this window with Alt-F4. See bug 6474035 */
    atom_set = XInternAtom(splash->display, "WM_PROTOCOLS", True);
    if (atom_set != None) {
        atom_list[0] = XInternAtom(splash->display, "WM_TAKE_FOCUS", True);
        atom_list[1] = XInternAtom(splash->display, "WM_DELETE_WINDOW", True);

        XChangeProperty(splash->display, splash->window, atom_set, XA_ATOM, 32,
                PropModeReplace, (unsigned char *) atom_list, 2);
    }

    /* mwm hints */
    atom_set = XInternAtom(splash->display, "_MOTIF_WM_HINTS", True);
    if (atom_set != None) {
        /* flags for decoration and functions */
        mwm_hints.flags = (1L << 1) | (1L << 0);
        mwm_hints.decorations = 0;
        mwm_hints.functions = 0;
        XChangeProperty(splash->display, splash->window, atom_set, atom_set,
                32, PropModeReplace, (unsigned char *) &mwm_hints, 5);
    }

    /* olwm hints */
    atom_set = XInternAtom(splash->display, "_OL_DECOR_DEL", True);
    if (atom_set != None) {
        atom_list[0] = XInternAtom(splash->display, "_OL_DECOR_RESIZE", True);
        atom_list[1] = XInternAtom(splash->display, "_OL_DECOR_HEADER", True);
        atom_list[2] = XInternAtom(splash->display, "_OL_DECOR_PIN", True);
        atom_list[3] = XInternAtom(splash->display, "_OL_DECOR_CLOSE", True);
        XChangeProperty(splash->display, splash->window, atom_set, XA_ATOM, 32,
                PropModeReplace, (unsigned char *) atom_list, 4);
    }

    /* generic EMWH hints
       we do not set _NET_WM_WINDOW_TYPE to _NET_WM_WINDOW_TYPE_SPLASH
       hint support due to gnome making this window always-on-top
       so we have to set _NET_WM_STATE and _NET_WM_ALLOWED_ACTIONS correctly
       _NET_WM_STATE: SKIP_TASKBAR and SKIP_PAGER
       _NET_WM_ALLOWED_ACTIONS: disable all actions */
    atom_set = XInternAtom(splash->display, "_NET_WM_STATE", True);
    if (atom_set != None) {
        atom_list[0] = XInternAtom(splash->display,
                "_NET_WM_STATE_SKIP_TASKBAR", True);
        atom_list[1] = XInternAtom(splash->display,
                "_NET_WM_STATE_SKIP_PAGER", True);
        XChangeProperty(splash->display, splash->window, atom_set, XA_ATOM, 32,
                PropModeReplace, (unsigned char *) atom_list, 2);
    }
    atom_set = XInternAtom(splash->display, "_NET_WM_ALLOWED_ACTIONS", True);
    if (atom_set != None) {
        XChangeProperty(splash->display, splash->window, atom_set, XA_ATOM, 32,
                PropModeReplace, (unsigned char *) atom_list, 0);
    }
}

void
SplashPThreadDestructor(void *arg) {
    /* this will be used in case of emergency thread exit on xlib error */
    Splash *splash = (Splash *) arg;

    if (splash) {
        SplashCleanup(splash);
    }
}

void *
SplashScreenThread(void *param) {
    Splash *splash = (Splash *) param;
//    pthread_key_t key;

//    pthread_key_create(&key, SplashPThreadDestructor);
//    pthread_setspecific(key, splash);

    SplashLock(splash);
    pipe(splash->controlpipe);
    fcntl(splash->controlpipe[0], F_SETFL,
        fcntl(splash->controlpipe[0], F_GETFL, 0) | O_NONBLOCK);
    splash->time = SplashTime();
    SplashCreateWindow(splash);
    fflush(stdout);
    if (splash->window) {
        SplashRemoveDecoration(splash);
        XStoreName(splash->display, splash->window, "Java");
        XMapRaised(splash->display, splash->window);
        SplashUpdateShape(splash);
        SplashRedrawWindow(splash);
        //map the splash co-ordinates as per system scale
        splash->x /= splash->scaleFactor;
        splash->y /= splash->scaleFactor;
        SplashEventLoop(splash);
    }
    SplashUnlock(splash);
    SplashDone(splash);

    splash->isVisible=-1;
    return 0;
}

void
SplashCreateThread(Splash * splash) {
    pthread_t thr;
    pthread_attr_t attr;
    int rc;

    pthread_attr_init(&attr);
    rc = pthread_create(&thr, &attr, SplashScreenThread, (void *) splash);
}

void
SplashLock(Splash * splash) {
    pthread_mutex_lock(&splash->lock);
}

void
SplashUnlock(Splash * splash) {
    pthread_mutex_unlock(&splash->lock);
}

void
SplashClosePlatform(Splash * splash) {
    sendctl(splash, SPLASHCTL_QUIT);
}

void
SplashUpdate(Splash * splash) {
    sendctl(splash, SPLASHCTL_UPDATE);
}

void
SplashReconfigure(Splash * splash) {
    sendctl(splash, SPLASHCTL_RECONFIGURE);
}

JNIEXPORT jboolean
SplashGetScaledImageName(const char* jarName, const char* fileName,
                           float *scaleFactor, char *scaledImgName,
                           const size_t scaledImageNameLength)
{
    *scaleFactor = 1;
#ifndef __linux__
    return JNI_FALSE;
#endif
    *scaleFactor = (float)getNativeScaleFactor();
    return GetScaledImageName(fileName, scaledImgName, scaleFactor, scaledImageNameLength);
}

