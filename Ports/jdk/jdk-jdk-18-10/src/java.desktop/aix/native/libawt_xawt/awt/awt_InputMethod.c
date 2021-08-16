/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "awt.h"
#include "awt_p.h"

#include <sun_awt_X11InputMethodBase.h>
#include <sun_awt_X11InputMethod.h>
#include <sun_awt_X11_XInputMethod.h>

#include <langinfo.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
#include <wchar.h>
#include <wctype.h>
#include <X11/Intrinsic.h>
#include <X11/keysym.h>
#include <X11/Xlib.h>

#define THROW_OUT_OF_MEMORY_ERROR() \
        JNU_ThrowOutOfMemoryError((JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2), NULL)

struct X11InputMethodIDs {
  jfieldID pData;
} x11InputMethodIDs;

static int PreeditStartCallback(XIC, XPointer, XPointer);
static void PreeditDoneCallback(XIC, XPointer, XPointer);
static void PreeditDrawCallback(XIC, XPointer,
                                XIMPreeditDrawCallbackStruct *);
static void PreeditCaretCallback(XIC, XPointer,
                                 XIMPreeditCaretCallbackStruct *);
static void StatusStartCallback(XIC, XPointer, XPointer);
static void StatusDoneCallback(XIC, XPointer, XPointer);
static void StatusDrawCallback(XIC, XPointer,
                               XIMStatusDrawCallbackStruct *);

#define ROOT_WINDOW_STYLES      (XIMPreeditNothing | XIMStatusNothing)
#define NO_STYLES               (XIMPreeditNone | XIMStatusNone)
/* added style to allow for in-place composition, such as "dead" keys for accents */
#define IN_PLACE_STYLES         (XIMPreeditNothing | XIMStatusNone)

#define PreeditStartIndex       0
#define PreeditDoneIndex        1
#define PreeditDrawIndex        2
#define PreeditCaretIndex       3
#define StatusStartIndex        4
#define StatusDoneIndex         5
#define StatusDrawIndex         6
#define NCALLBACKS              7

#define STATUS_BORDER 2         /* Status Border width */
#define CARET_OFFSET 1          /* Offset of caret position (pixel) */
#define BORDER_MARGIN 3         /* BORDER MARGIN width */
#define STATUS_MARGIN 7         /* Margin between the status window and its parent window */
#define PREEDIT_ATTR_MASK (XIMReverse|XIMUnderline)
          /* Preedit attribute which host adapter can handle */

/*
 * Callback function pointers: the order has to match the *Index
 * values above.
 */
static XIMProc callback_funcs[NCALLBACKS] = {
    (XIMProc)(void *)&PreeditStartCallback,
    (XIMProc)PreeditDoneCallback,
    (XIMProc)PreeditDrawCallback,
    (XIMProc)PreeditCaretCallback,
    (XIMProc)StatusStartCallback,
    (XIMProc)StatusDoneCallback,
    (XIMProc)StatusDrawCallback,
};

#define MAX_STATUS_LEN  100
typedef struct {
    Window   w;                /*status window id        */
    Window   root;             /*the root window id      */
    Window   parent;           /*parent shell window     */
    Window   grandParent;      /*window has WM frame     */
    int      x, y;             /*parent's upperleft position */
    int      width, height;    /*parent's width, height  */
    GC       lightGC;          /*gc for light border     */
    GC       dimGC;            /*gc for dim border       */
    GC       bgGC;             /*normal painting         */
    GC       fgGC;             /*normal painting         */
    int      statusW, statusH; /*status window's w, h    */
    int      rootW, rootH;     /*root window's w, h    */
    int      bWidth;           /*border width            */
    wchar_t  status[MAX_STATUS_LEN + 1]; /*status text       */
    XFontSet fontset;           /*fontset for drawing    */
    int      off_x, off_y;
    Bool     on;                /*if the status window on*/
    int      fOff;              /* font base line(in pixel) from top */
    int      fBot;              /* font bottom line(in pixel) from top */
    int      peTextW;           /* Composition text width in pixel */
    wchar_t* peText;            /* Composed string (wide char.) */
    XIMFeedback* peAttr;        /* Composed text attribute */
    int      peCaret;           /* Caret position in number of character */
    Bool     status_ready;      /* Not draw Status at XCreateIC */
} StatusWindow;

/*
 * X11InputMethodData keeps per X11InputMethod instance information. A pointer
 * to this data structure is kept in an X11InputMethod object (pData).
 */
typedef struct _X11InputMethodData {
    XIC         current_ic;     /* current X Input Context */
    XIC         ic_active;      /* X Input Context for active clients */
    XIC         ic_passive;     /* X Input Context for passive clients */
    XIMCallback *callbacks;     /* callback parameters */
    jobject     x11inputmethod; /* global ref to X11InputMethod instance */
                                /* associated with the XIC */
    StatusWindow *statusWindow; /* our own status window  */
    Bool        passiveStatusWindow;/* Passive Client uses StatusWindow */
    Bool        isActiveClient;     /* True:clinet is active */
    Bool        forceReset;     /* True: call resetXIC before UnsetICFocus */
} X11InputMethodData;

/* reference to the current X11InputMethod instance, it is always
   point to the global reference to the X11InputMethodObject since
   it could be referenced by different threads. */
jobject currentX11InputMethodInstance = NULL;

Window  currentFocusWindow = 0;  /* current window that has focus for input
                                       method. (the best place to put this
                                       information should be
                                       currentX11InputMethodInstance's pData) */
static XIM X11im = NULL;
Display * dpy = NULL;

#define GetJNIEnv() (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2)

static X11InputMethodData * getX11InputMethodData(JNIEnv *, jobject);
static void setX11InputMethodData(JNIEnv *, jobject, X11InputMethodData *);
static void destroyX11InputMethodData(JNIEnv *, X11InputMethodData *);
static void freeX11InputMethodData(JNIEnv *, X11InputMethodData *);

/* Prototype for this function is missing in AIX Xlib.h */
extern char *XSetIMValues(
#if NeedVarargsPrototypes
    XIM /* im */, ...
#endif
);

static int st_wcslen(wchar_t *string);
static Bool isPreeditStateActive(XIC ic);
static void * buf_insert(void * src, void * insert, int size,
                         int src_len, int ins_len, int offset);
static void * handle_buffer(void * source, void * insert,
                            int size, int src_len, int ins_len,
                            int del_len, int offset);
static void preedit_draw_passive(X11InputMethodData *pX11IMData,
                                 XIMPreeditDrawCallbackStruct *pre_draw);
static void resetPassivePreeditText(StatusWindow *statusWindow);
static void draw_caret(StatusWindow *statusWindow, GC gc, int pos);
static int  get_next_attr(int len, unsigned long *attr);
static void draw_preedit(StatusWindow *statusWindow);
static void align_status(StatusWindow *statusWindow);
static void shrink_status(StatusWindow *statusWindow);
static XFontSet create_fontset(void);
static Bool is_text_available(XIMText * text);
static Bool isNativeIm();
static Window getGrandParent(Window parent);
static void moveStatusWindow(StatusWindow *statusWindow);
static void arrange_window_stack(StatusWindow* statusWindow);
static Window get_current_focus(XIC ic);

/*
 * This function is stolen from /src/solaris/hpi/src/system_md.c
 * It is used in setting the time in Java-level InputEvents
 */
jlong
awt_util_nowMillisUTC()
{
    struct timeval t;
    gettimeofday(&t, NULL);
    return ((jlong)t.tv_sec) * 1000 + (jlong)(t.tv_usec/1000);
}

/*
 * Converts the wchar_t string to a multi-byte string calling wcstombs(). A
 * buffer is allocated by malloc() to store the multi-byte string. NULL is
 * returned if the given wchar_t string pointer is NULL or buffer allocation is
 * failed.
 */
static char *
wcstombsdmp(wchar_t *wcs, int len)
{
    size_t n;
    char *mbs;

    if (wcs == NULL)
        return NULL;

    n = len*MB_CUR_MAX + 1;

    mbs = (char *) malloc(n * sizeof(char));
    if (mbs == NULL) {
        THROW_OUT_OF_MEMORY_ERROR();
        return NULL;
    }

    /* TODO: check return values... Handle invalid characters properly...  */
    if (wcstombs(mbs, wcs, n) == (size_t)-1) {
        free(mbs);
        return NULL;
    }

    return mbs;
}

static X11InputMethodData * getX11InputMethodData(JNIEnv * env, jobject imInstance) {
    X11InputMethodData *pX11IMData =
        (X11InputMethodData *)JNU_GetLongFieldAsPtr(env, imInstance, x11InputMethodIDs.pData);

    /*
     * In case the XIM server was killed somehow, reset X11InputMethodData.
     */
    if (X11im == NULL && pX11IMData != NULL) {
        JNU_CallMethodByName(env, NULL, pX11IMData->x11inputmethod,
                             "flushText",
                             "()V");
        JNU_CHECK_EXCEPTION_RETURN(env, NULL);
        /* IMPORTANT:
           The order of the following calls is critical since "imInstance" may
           point to the global reference itself, if "freeX11InputMethodData" is called
           first, the global reference will be destroyed and "setX11InputMethodData"
           will in fact fail silently. So pX11IMData will not be set to NULL.
           This could make the original java object refers to a deleted pX11IMData
           object.
        */
        setX11InputMethodData(env, imInstance, NULL);
        freeX11InputMethodData(env, pX11IMData);
        pX11IMData = NULL;
    }

    return pX11IMData;
}

static void setX11InputMethodData(JNIEnv * env, jobject imInstance, X11InputMethodData *pX11IMData) {
    JNU_SetLongFieldFromPtr(env, imInstance, x11InputMethodIDs.pData, pX11IMData);
}

/* this function should be called within AWT_LOCK() */
static void
destroyX11InputMethodData(JNIEnv *env, X11InputMethodData *pX11IMData)
{
    /*
     * Destroy XICs
     */
    if (pX11IMData == NULL) {
        return;
    }

    if (pX11IMData->ic_active != (XIC)0) {
        XUnsetICFocus(pX11IMData->ic_active);
        XDestroyIC(pX11IMData->ic_active);
        if (pX11IMData->ic_active != pX11IMData->ic_passive) {
            if (pX11IMData->ic_passive != (XIC)0) {
                XUnsetICFocus(pX11IMData->ic_passive);
                XDestroyIC(pX11IMData->ic_passive);
            }
            pX11IMData->ic_passive = (XIC)0;
            pX11IMData->current_ic = (XIC)0;
        }
    }

    freeX11InputMethodData(env, pX11IMData);
}

static void
freeX11InputMethodData(JNIEnv *env, X11InputMethodData *pX11IMData)
{
    if (pX11IMData->statusWindow != NULL){
        StatusWindow *sw = pX11IMData->statusWindow;
        XFreeGC(awt_display, sw->lightGC);
        XFreeGC(awt_display, sw->dimGC);
        XFreeGC(awt_display, sw->bgGC);
        XFreeGC(awt_display, sw->fgGC);
        if (sw->fontset != NULL) {
            XFreeFontSet(awt_display, sw->fontset);
        }
        XDestroyWindow(awt_display, sw->w);
        if (pX11IMData->statusWindow->peText){
            free((void *)pX11IMData->statusWindow->peText);
            pX11IMData->statusWindow->peText = NULL;
        }
        if (pX11IMData->statusWindow->peAttr){
            free((void *)pX11IMData->statusWindow->peAttr);
            pX11IMData->statusWindow->peAttr = NULL;
        }
        free((void*)sw);
    }

    if (pX11IMData->callbacks)
        free((void *)pX11IMData->callbacks);

    if (env) {
        (*env)->DeleteGlobalRef(env, pX11IMData->x11inputmethod);
    }

    free((void *)pX11IMData);
}

/*
 * Sets or unsets the focus to the given XIC.
 */
static void
setXICFocus(XIC ic, unsigned short req)
{
    if (ic == NULL) {
        (void)fprintf(stderr, "Couldn't find X Input Context\n");
        return;
    }
    if (req == 1)
        XSetICFocus(ic);
    else
        XUnsetICFocus(ic);
}

/*
 * Sets the focus window to the given XIC.
 */
static void
setXICWindowFocus(XIC ic, Window w)
{
    if (ic == NULL) {
        (void)fprintf(stderr, "Couldn't find X Input Context\n");
        return;
    }
    (void) XSetICValues(ic, XNFocusWindow, w, NULL);
}

/*
 * Invokes XmbLookupString() to get something from the XIM. It invokes
 * X11InputMethod.dispatchCommittedText() if XmbLookupString() returns
 * committed text.  This function is called from handleKeyEvent in canvas.c and
 * it's under the Motif event loop thread context.
 *
 * Buffer usage: There is a bug in XFree86-4.3.0 XmbLookupString implementation,
 * where it never returns XBufferOverflow.  We need to allocate the initial lookup buffer
 * big enough, so that the possibility that user encounters this problem is relatively
 * small.  When this bug gets fixed, we can make the initial buffer size smaller.
 * Note that XmbLookupString() sometimes produces a non-null-terminated string.
 *
 * Returns True when there is a keysym value to be handled.
 */
#define INITIAL_LOOKUP_BUF_SIZE 512

Boolean
awt_x11inputmethod_lookupString(XKeyPressedEvent *event, KeySym *keysymp)
{
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    int buf_len = INITIAL_LOOKUP_BUF_SIZE;
    char mbbuf[INITIAL_LOOKUP_BUF_SIZE];
    char *buf;
    KeySym keysym = NoSymbol;
    Status status;
    int mblen;
    jstring javastr;
    XIC ic;
    Boolean result = True;
    static Boolean composing = False;

    /*
      printf("lookupString: entering...\n");
     */

    pX11IMData = getX11InputMethodData(env, currentX11InputMethodInstance);

    if (pX11IMData == NULL) {
        return False;
    }

    if ((ic = pX11IMData->current_ic) == (XIC)0){
        return False;
    }

    buf = mbbuf;
    mblen = XmbLookupString(ic, event, buf,
                            buf_len - 1, &keysym, &status);

    /*
     * In case of overflow, a buffer is allocated and it retries
     * XmbLookupString().
     */
    if (status == XBufferOverflow) {
        buf_len = mblen + 1;
        buf = (char *)malloc(buf_len);
        if (buf == NULL) {
            THROW_OUT_OF_MEMORY_ERROR();
            return result;
        }
        mblen = XmbLookupString(ic, event, buf, buf_len, &keysym, &status);
    }
    buf[mblen] = 0;

    /* Get keysym without taking modifiers into account first to map
     * to AWT keyCode table.
     */
    switch (status) {
    case XLookupBoth:
        if (!composing) {
            if (event->keycode != 0) {
                *keysymp = keysym;
                result = False;
                break;
            }
        }
        composing = False;
        /*FALLTHRU*/
    case XLookupChars:
        /*
        printf("lookupString: status=XLookupChars, type=%d, state=%x, keycode=%x, keysym=%x\n",
               event->type, event->state, event->keycode, keysym);
        */
        javastr = JNU_NewStringPlatform(env, (const char *)buf);
        if (javastr != NULL) {
            JNU_CallMethodByName(env, NULL,
                                 currentX11InputMethodInstance,
                                 "dispatchCommittedText",
                                 "(Ljava/lang/String;J)V",
                                 javastr,
                                 event->time);
            if ((*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
        }
        break;

    case XLookupKeySym:
        /*
        printf("lookupString: status=XLookupKeySym, type=%d, state=%x, keycode=%x, keysym=%x\n",
               event->type, event->state, event->keycode, keysym);
        */
        if (keysym == XK_Multi_key)
            composing = True;
        if (! composing) {
            *keysymp = keysym;
            result = False;
        }
        break;

    case XLookupNone:
        /*
        printf("lookupString: status=XLookupNone, type=%d, state=%x, keycode=%x, keysym=%x\n",
               event->type, event->state, event->keycode, keysym);
        */
        break;
    }

    if (buf != mbbuf) {
        free(buf);
    }
    return result;
}

static StatusWindow *createStatusWindow(Window parent) {
    StatusWindow *statusWindow;
    XSetWindowAttributes attrib;
    unsigned long attribmask;
    Window containerWindow;
    Window status;
    Window child;
    XWindowAttributes xwa;
    XWindowAttributes xxwa;
    /* Variable for XCreateFontSet()*/
    char **mclr;
    int  mccr = 0;
    char *dsr;
    unsigned long bg, fg, light, dim;
    int x, y, off_x, off_y, xx, yy;
    unsigned int w, h, bw, depth;
    XGCValues values;
    unsigned long valuemask = 0;  /*ignore XGCvalue and use defaults*/
    int screen = 0;
    int i;
    AwtGraphicsConfigDataPtr adata;
    extern int awt_numScreens;
    /*hardcode the size right now, should get the size base on font*/
    int width=80, height=22;
    Window rootWindow;
    Window *ignoreWindowPtr;
    unsigned int ignoreUnit;
    Window grandParent;
    Window target;
    XFontSet fontset;

    fontset = create_fontset();
    if (NULL == fontset) {
        return NULL;
    }

    XGetGeometry(dpy, parent, &rootWindow, &x, &y, &w, &h, &bw, &depth);

    attrib.override_redirect = True;
    attribmask = CWOverrideRedirect;
    for (i = 0; i < awt_numScreens; i++) {
        if (RootWindow(dpy, i) == rootWindow) {
            screen = i;
            break;
        }
    }
    adata = getDefaultConfig(screen);
    bg    = adata->AwtColorMatch(255, 255, 255, adata);
    fg    = adata->AwtColorMatch(0, 0, 0, adata);
    light = adata->AwtColorMatch(195, 195, 195, adata);
    dim   = adata->AwtColorMatch(128, 128, 128, adata);

    grandParent = getGrandParent(parent);
    target = (grandParent == 0) ? parent : grandParent;
    XGetWindowAttributes(dpy, target, &xwa);
    bw = 2; /*xwa.border_width does not have the correct value*/

    /*compare the size difference between parent container
      and shell widget, the diff should be the border frame
      and title bar height (?)*/

    XQueryTree( dpy,
                target,
                &rootWindow,
                &containerWindow,
                &ignoreWindowPtr,
                &ignoreUnit);
    XGetWindowAttributes(dpy, containerWindow, &xxwa);

    XTranslateCoordinates(dpy,
                          target, xwa.root,
                          0, 0,
                          &x, &y, &child);

    if (containerWindow == rootWindow) {
        off_x = 0; off_y = STATUS_MARGIN;
    } else {
        XGetWindowAttributes(dpy, containerWindow, &xxwa);
        off_x = (xxwa.width - xwa.width) / 2;
        /* off_y = xxwa.height - xwa.height - off_x;*/ /*it's magic:-) */
        {
            int cx, cy;
            XTranslateCoordinates(dpy,
                                  containerWindow, xxwa.root,
                                  0, 0,
                                  &cx, &cy,
                                  &child);
            off_y = (xxwa.height + cy) - (xwa.height + y);
        }
    }

    /*get the size of root window*/
    XGetWindowAttributes(dpy, rootWindow, &xxwa);

    XTranslateCoordinates(dpy,
                          target, xwa.root,
                          xwa.x, xwa.y,
                          &x, &y,
                          &child);
    xx = x - off_x;
    yy = y + xwa.height - off_y;
    if (xx < 0 ){
        xx = 0;
    }
    if (xx + width > xxwa.width) {
        xx = xxwa.width - width;
    }
    if (yy + height > xxwa.height) {
        yy = xxwa.height - height;
    }

    if ((DefaultVisual(dpy,screen))->class != adata->awt_visInfo.visual->class &&
        adata->awt_visInfo.visual->class == TrueColor) {
        attrib.colormap = XCreateColormap(dpy, xwa.root,
            adata->awt_visInfo.visual, AllocNone );
        attrib.border_pixel = BlackPixel(dpy, screen) ;
        attribmask |= CWColormap | CWBorderPixel;
    }

    status =  XCreateWindow(dpy,
                            xwa.root,
                            xx, yy,
                            width, height,
                            0,
                            xwa.depth,
                            InputOutput,
                            adata->awt_visInfo.visual,
                            attribmask, &attrib);
    XSelectInput(dpy, status,
                 ExposureMask | StructureNotifyMask | EnterWindowMask |
                 LeaveWindowMask | VisibilityChangeMask);
    if (grandParent != 0){
        long mask;
        XGetWindowAttributes(dpy, grandParent, &xwa);
        mask = xwa.your_event_mask | StructureNotifyMask |
               VisibilityChangeMask | PropertyChangeMask;
        XSelectInput(dpy, grandParent,mask);
    }

    statusWindow = (StatusWindow*) calloc(1, sizeof(StatusWindow));
    if (statusWindow == NULL){
        THROW_OUT_OF_MEMORY_ERROR();
        return NULL;
    }
    statusWindow->w = status;
    statusWindow->fontset = fontset;
    statusWindow->parent = parent;
    statusWindow->grandParent = grandParent;
    statusWindow->on  = False;
    statusWindow->x = x;
    statusWindow->y = y;
    statusWindow->width = xwa.width;
    statusWindow->height = xwa.height;
    statusWindow->off_x = off_x;
    statusWindow->off_y = off_y;
    statusWindow->bWidth  = bw;
    statusWindow->statusH = height;
    statusWindow->statusW = width;
    statusWindow->peTextW = 0;
    statusWindow->rootH = xxwa.height;
    statusWindow->rootW = xxwa.width;
    statusWindow->lightGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->lightGC, light);
    statusWindow->dimGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->dimGC, dim);
    statusWindow->fgGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->fgGC, fg);
    XSetBackground(dpy, statusWindow->fgGC, bg);
    statusWindow->bgGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->bgGC, bg);
    XSetBackground(dpy, statusWindow->bgGC, fg);
    statusWindow->status_ready = False;
    wcscpy(statusWindow->status, L"");
    return statusWindow;
}

/* This method is to turn off or turn on the status window. */
static void onoffStatusWindow(X11InputMethodData* pX11IMData,
                                Window parent,
                                Bool ON){
    XWindowAttributes xwa;
    Window child;
    int x, y;
    StatusWindow *statusWindow = NULL;

    if (NULL == pX11IMData ||
        NULL == (statusWindow =  pX11IMData->statusWindow)){
        return;
    }

    if (ON == False) {
        XUnmapWindow(dpy, statusWindow->w);
        return;
    }
    if (NULL == currentX11InputMethodInstance){
        return;
    }
    {
        JNIEnv *env = GetJNIEnv();
        parent = JNU_CallMethodByName(env, NULL, pX11IMData->x11inputmethod,
                                      "getCurrentParentWindow",
                                      "()J").j;
        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }
    if (statusWindow->parent != parent) {
        statusWindow->parent = parent;
    }
    if (st_wcslen(statusWindow->status) > 0 ||
        (statusWindow->peText != NULL && st_wcslen(statusWindow->peText) > 0 )) {
        moveStatusWindow(statusWindow);
        XMapRaised(dpy, statusWindow->w);
    }
}

void paintStatusWindow(StatusWindow *statusWindow){
    Window  win  = statusWindow->w;
    GC  lightgc = statusWindow->lightGC;
    GC  dimgc = statusWindow->dimGC;
    GC  bggc = statusWindow->bgGC;
    GC  fggc = statusWindow->fgGC;

    int width = statusWindow->statusW;
    int height = statusWindow->statusH;
    int bwidth = statusWindow->bWidth;
    int len;
    XRectangle logical, ink;

    if (NULL == statusWindow) return;
    if ((len = st_wcslen(statusWindow->status)) == 0) {
        return;
    }
    XwcTextExtents(statusWindow->fontset, statusWindow->status,
                   len, &ink, &logical);
    width = logical.width;
    height = logical.height;

    XFillRectangle(dpy, win, bggc, 0, 0, width+2, height+2);

    XDrawLine(dpy, win, fggc, 0, 0, width+2, 0);
    XDrawLine(dpy, win, fggc, 0, height+2, width+2, height+2);
    XDrawLine(dpy, win, fggc, 0, 0, 0, height+2);
    XDrawLine(dpy, win, fggc, width+2, 0, width+2, height+2);

    if (statusWindow->fontset) {
        XwcDrawString(dpy, win, statusWindow->fontset, fggc,
                      -logical.x + 1, -logical.y + 1,
                      statusWindow->status,
                      st_wcslen(statusWindow->status));
    } else {
        /*too bad we failed to create a fontset for this locale*/
        XDrawString(dpy, win, fggc, bwidth + 2, height - bwidth - 4,
                    "[InputMethod ON]", strlen("[InputMethod ON]"));
    }
}

Bool statusWindowEventHandler(XEvent event) {
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    StatusWindow *statusWindow;

    if (NULL == currentX11InputMethodInstance ||
        NULL == (pX11IMData = getX11InputMethodData(env, currentX11InputMethodInstance)) ||
        NULL == (statusWindow = pX11IMData->statusWindow))
    {
        return False;
    }

    if (statusWindow->w == event.xany.window) {
        switch (event.type) {
        case Expose:
            paintStatusWindow(statusWindow);
            if (statusWindow->peText)
                draw_preedit(statusWindow);
            arrange_window_stack(statusWindow);
            break;
        case ConfigureNotify:
        case VisibilityNotify:
            arrange_window_stack(statusWindow);
            break;
        /*
        case UnmapNotify:
        case VisibilityNotify:
            break;
        */
        default:
            break;
        }
        return True;
    } else if ((statusWindow->parent == event.xany.window) ||
               (statusWindow->grandParent && statusWindow->grandParent == event.xany.window)) {
        switch (event.type) {
        case MapNotify:
            if (statusWindow->on) {
                onoffStatusWindow(pX11IMData, statusWindow->parent, True);
            }
            break;
        case UnmapNotify:
            onoffStatusWindow(pX11IMData, 0, False);
            break;
        case VisibilityNotify:
            if (statusWindow->on) {
                arrange_window_stack(statusWindow);
            }
            break;
        case ConfigureNotify:
            if (statusWindow->grandParent && statusWindow->on) {
                moveStatusWindow(statusWindow);
            }
        case PropertyNotify:
            if (statusWindow->on) {
                arrange_window_stack(statusWindow);
            }
            break;
        default:
            break;
        }
    }
    return False;
}

static void adjustStatusWindow(Window shell) {
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    StatusWindow *statusWindow;

    if (NULL == currentX11InputMethodInstance
        || NULL == (pX11IMData = getX11InputMethodData(env,currentX11InputMethodInstance))
        || NULL == (statusWindow = pX11IMData->statusWindow)
        || !statusWindow->on)
    {
        return;
    }

    moveStatusWindow(statusWindow);
}

/*
 * Creates two XICs, one for active clients and the other for passive
 * clients. All information on those XICs are stored in the
 * X11InputMethodData given by the pX11IMData parameter.
 *
 * For active clients: Try to use preedit callback to support
 * on-the-spot. If tc is not null, the XIC to be created will
 * share the Status Area with Motif widgets (TextComponents). If the
 * preferable styles can't be used, fallback to root-window styles. If
 * root-window styles failed, fallback to None styles.
 *
 * For passive clients: Try to use root-window styles. If failed,
 * fallback to None styles.
 */
static Bool
createXIC(JNIEnv * env, X11InputMethodData *pX11IMData, Window w)
{
    XVaNestedList preedit = NULL;
    XVaNestedList status = NULL;
    XIMStyle on_the_spot_styles = XIMPreeditCallbacks,
             in_place_styles = 0,
             active_styles = 0,
             passive_styles = 0,
             no_styles = 0;
    XIMCallback *callbacks;
    unsigned short i;
    XIMStyles *im_styles;
    char *ret = NULL;
    Bool passiveStatusWindow = False;
    pX11IMData->statusWindow = NULL;

    if (X11im == NULL) {
        return False;
    }
    if (!w) {
        return False;
    }

    if (getenv("IBMJAVA_PASSIVE") == NULL) {
        passiveStatusWindow = False;
    } else {
        passiveStatusWindow = True;
    }

    if (isNativeIm()) { passiveStatusWindow = True; }

    ret = XGetIMValues(X11im, XNQueryInputStyle, &im_styles, NULL);

    if (ret != NULL) {
        jio_fprintf(stderr,"XGetIMValues: %s\n",ret);
        return FALSE ;
    }

    on_the_spot_styles |= XIMStatusNothing;

    /*kinput does not support XIMPreeditCallbacks and XIMStatusArea
      at the same time, so use StatusCallback to draw the status
      ourself
    */
    for (i = 0; i < im_styles->count_styles; i++) {
        if (im_styles->supported_styles[i] == (XIMPreeditCallbacks | XIMStatusCallbacks)) {
            on_the_spot_styles = (XIMPreeditCallbacks | XIMStatusCallbacks);
            break;
        }
    }

    for (i = 0; i < im_styles->count_styles; i++) {
        if (im_styles->supported_styles[i] == on_the_spot_styles)
            active_styles = im_styles->supported_styles[i];
        if (im_styles->supported_styles[i] == ROOT_WINDOW_STYLES)
            passive_styles = im_styles->supported_styles[i];
        if (im_styles->supported_styles[i] == IN_PLACE_STYLES) {
            in_place_styles = im_styles->supported_styles[i];
        }
        if (im_styles->supported_styles[i] == NO_STYLES) {
            no_styles = im_styles->supported_styles[i];
        }
    }

    XFree(im_styles);

    if (active_styles != on_the_spot_styles) {
        if (passive_styles == ROOT_WINDOW_STYLES)
            active_styles = passive_styles;
        else {
          if (in_place_styles == IN_PLACE_STYLES){
              active_styles = passive_styles = IN_PLACE_STYLES;
          } else {
            if (no_styles == NO_STYLES)
                active_styles = passive_styles = NO_STYLES;
            else
                active_styles = passive_styles = 0;
          }
        }
    } else {
      if (!passiveStatusWindow) {
        if (passive_styles != ROOT_WINDOW_STYLES) {
            if (no_styles == NO_STYLES)
                active_styles = passive_styles = NO_STYLES;
            else
                active_styles = passive_styles = 0;
        }
      } else
          passive_styles = active_styles;
    }

    if (active_styles == on_the_spot_styles) {
        callbacks = (XIMCallback *)malloc(sizeof(XIMCallback) * NCALLBACKS);
        if (callbacks == (XIMCallback *)NULL)
            return False;
        pX11IMData->callbacks = callbacks;

        for (i = 0; i < NCALLBACKS; i++, callbacks++) {
            callbacks->client_data = (XPointer) pX11IMData->x11inputmethod;
            callbacks->callback = callback_funcs[i];
        }

        callbacks = pX11IMData->callbacks;
        preedit = (XVaNestedList)XVaCreateNestedList(0,
                        XNPreeditStartCallback, &callbacks[PreeditStartIndex],
                        XNPreeditDoneCallback,  &callbacks[PreeditDoneIndex],
                        XNPreeditDrawCallback,  &callbacks[PreeditDrawIndex],
                        XNPreeditCaretCallback, &callbacks[PreeditCaretIndex],
                        NULL);
        if (preedit == (XVaNestedList)NULL)
            goto err;
        /*always try XIMStatusCallbacks for active client...*/
        {
        if (on_the_spot_styles & XIMStatusCallbacks) {
            status = (XVaNestedList)XVaCreateNestedList(0,
                        XNStatusStartCallback, &callbacks[StatusStartIndex],
                        XNStatusDoneCallback,  &callbacks[StatusDoneIndex],
                        XNStatusDrawCallback, &callbacks[StatusDrawIndex],
                        NULL);

            if (status == NULL)
                goto err;
          }
            pX11IMData->statusWindow = createStatusWindow(w);
            pX11IMData->ic_active = XCreateIC(X11im,
                                              XNClientWindow, w,
                                              XNFocusWindow, w,
                                              XNInputStyle, active_styles,
                                              XNPreeditAttributes, preedit,
                                              XNStatusAttributes, status,
                                              NULL);
            if (NULL != pX11IMData->statusWindow) {
                pX11IMData->statusWindow->status_ready = True;
            }
            XFree((void *)status);
            XFree((void *)preedit);
        }
        if (passiveStatusWindow) {
            pX11IMData->ic_passive = pX11IMData->ic_active;
        } else {
            pX11IMData->ic_passive = XCreateIC(X11im,
                                               XNClientWindow, w,
                                               XNFocusWindow, w,
                                               XNInputStyle, passive_styles,
                                               NULL);
        }
    } else {
        pX11IMData->ic_active = XCreateIC(X11im,
                                          XNClientWindow, w,
                                          XNFocusWindow, w,
                                          XNInputStyle, active_styles,
                                          NULL);
        pX11IMData->ic_passive = pX11IMData->ic_active;
    }

    // The code set the IC mode that the preedit state is not initialied
    // at XmbResetIC.  This attribute can be set at XCreateIC.  I separately
    // set the attribute to avoid the failure of XCreateIC at some platform
    // which does not support the attribute.
    if (pX11IMData->ic_active != 0)
        XSetICValues(pX11IMData->ic_active,
                        XNResetState, XIMPreserveState, NULL);
    if (pX11IMData->ic_passive != 0 &&
        pX11IMData->ic_active != pX11IMData->ic_passive)
            XSetICValues(pX11IMData->ic_passive,
             XNResetState, XIMInitialState, NULL);

    pX11IMData->passiveStatusWindow = passiveStatusWindow;

    if (pX11IMData->ic_active == (XIC)0
        || pX11IMData->ic_passive == (XIC)0) {
        return False;
    }

    /* Unset focus to avoid unexpected IM on */
    setXICFocus(pX11IMData->ic_active, False);
    if (pX11IMData->ic_active != pX11IMData->ic_passive)
        setXICFocus(pX11IMData->ic_passive, False);

    return True;

 err:
    if (preedit)
        XFree((void *)preedit);
    THROW_OUT_OF_MEMORY_ERROR();
    return False;
}

static int
PreeditStartCallback(XIC ic, XPointer client_data, XPointer call_data)
{
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData;

    pX11IMData = getX11InputMethodData(env, (jobject)client_data);
    if (pX11IMData == NULL || pX11IMData->statusWindow == NULL) {
        return 0;
    }
    resetPassivePreeditText(pX11IMData->statusWindow);

    return -1;  /* unlimited length for preedit text  */
}

static void
PreeditDoneCallback(XIC ic, XPointer client_data, XPointer call_data)
{
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData;

    pX11IMData = getX11InputMethodData(env, (jobject)client_data);
    if (pX11IMData == NULL) {
        return;
    }

    if (!pX11IMData->isActiveClient) {
        resetPassivePreeditText(pX11IMData->statusWindow);
        shrink_status(pX11IMData->statusWindow);
    }
    else{
            JNU_CallMethodByName(env, NULL, pX11IMData->x11inputmethod,
                                 "clearComposedText",
                                 "(J)V",
                                 awt_util_nowMillisUTC());
            if ((*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
    }
}

/*
 * Translate the preedit draw callback items to Java values and invoke
 * X11InputMethod.dispatchComposedText().
 *
 * client_data: X11InputMethod object
 */
static void
PreeditDrawCallback(XIC ic, XPointer client_data,
                    XIMPreeditDrawCallbackStruct *pre_draw)
{
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    jmethodID x11imMethodID;

    XIMText *text;
    jstring javastr = NULL;
    jintArray style = NULL;

    /* printf("Native: PreeditDrawCallback() \n"); */
    if (pre_draw == NULL) {
        return;
    }
    AWT_LOCK();
    if ((pX11IMData = getX11InputMethodData(env, (jobject)client_data)) == NULL) {
        goto finally;
    }

    if (!pX11IMData->isActiveClient){
        if (ic == pX11IMData->ic_passive) {
            preedit_draw_passive(pX11IMData, pre_draw);
        }
        goto finally;
    }

    if ((text = pre_draw->text) != NULL) {
        if (is_text_available(text)) {
            if (text->string.multi_byte != NULL) {
                if (pre_draw->text->encoding_is_wchar == False) {
                    javastr = JNU_NewStringPlatform(env, (const char *)text->string.multi_byte);
                    if (javastr == NULL) {
                        goto finally;
                    }
                } else {
                    char *mbstr = wcstombsdmp(text->string.wide_char, text->length);
                    if (mbstr == NULL) {
                        goto finally;
                    }
                    javastr = JNU_NewStringPlatform(env, (const char *)mbstr);
                    free(mbstr);
                    if (javastr == NULL) {
                        goto finally;
                    }
                }
            }
        }
        if (text->feedback != NULL) {
            int cnt;
            jint *tmpstyle;

            style = (*env)->NewIntArray(env, text->length);
            if (JNU_IsNull(env, style)) {
                (*env)->ExceptionClear(env);
                THROW_OUT_OF_MEMORY_ERROR();
                goto finally;
            }

            if (sizeof(XIMFeedback) == sizeof(jint)) {
                /*
                 * Optimization to avoid copying the array
                 */
                (*env)->SetIntArrayRegion(env, style, 0,
                                          text->length, (jint *)text->feedback);
            } else {
                tmpstyle  = (jint *)malloc(sizeof(jint)*(text->length));
                if (tmpstyle == (jint *) NULL) {
                    THROW_OUT_OF_MEMORY_ERROR();
                    goto finally;
                }
                for (cnt = 0; cnt < (int)text->length; cnt++)
                        tmpstyle[cnt] = text->feedback[cnt];
                (*env)->SetIntArrayRegion(env, style, 0,
                                          text->length, (jint *)tmpstyle);
                free(tmpstyle);
            }
        }
    }
    JNU_CallMethodByName(env, NULL, pX11IMData->x11inputmethod,
                         "dispatchComposedText",
                         "(Ljava/lang/String;[IIIIJ)V",
                         javastr,
                         style,
                         (jint)pre_draw->chg_first,
                         (jint)pre_draw->chg_length,
                         (jint)pre_draw->caret,
                         awt_util_nowMillisUTC());

    if ((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }

finally:
    AWT_UNLOCK();
    return;
}

static void
PreeditCaretCallback(XIC ic, XPointer client_data,
                     XIMPreeditCaretCallbackStruct *pre_caret)
{
    XIMPreeditDrawCallbackStruct pre_draw;

    if (pre_caret != NULL && pre_caret->direction == XIMAbsolutePosition) {
        pre_draw.caret = pre_caret->position;
        pre_draw.chg_first = 0;
        pre_draw.chg_length = 0;
        pre_draw.text = NULL;
        PreeditDrawCallback(ic, client_data, &pre_draw);
    }
}

static void
StatusStartCallback(XIC ic, XPointer client_data, XPointer call_data)
{
    /*ARGSUSED*/
    /*printf("StatusStartCallback:\n");  */
}

static void
StatusDoneCallback(XIC ic, XPointer client_data, XPointer call_data)
{
    /*ARGSUSED*/
    /*printf("StatusDoneCallback:\n"); */
}

static void StatusDrawCallback
  (XIC ic, XPointer client_data, XIMStatusDrawCallbackStruct *status_draw)
{
    /*ARGSUSED*/
    /*printf("StatusDrawCallback:\n"); */
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    StatusWindow *statusWindow;
    int value_make = CWX|CWWidth|CWHeight;
    XRectangle logical, ink;
    XWindowChanges xwc;
    int len;

    AWT_LOCK();

    if (NULL == (pX11IMData = getX11InputMethodData(env, (jobject)client_data))
        || NULL == (statusWindow = pX11IMData->statusWindow)){
        goto finally;
    }

    if (status_draw->type == XIMTextType) {
        XIMText *text = (status_draw->data).text;
        if (text != NULL) {
            if (text->string.multi_byte != NULL) {
                if(!strcmp(text->string.multi_byte," ")){
                    wcscpy(statusWindow->status, L"");
                    onoffStatusWindow(pX11IMData, 0, False);
                    goto finally;
                }
                mbstowcs(statusWindow->status,
                         (const char *)text->string.multi_byte,
                         (size_t)MAX_STATUS_LEN);
            } else {
                if (0 == st_wcslen(text->string.wide_char)){
                    wcscpy(statusWindow->status, L"");
                    onoffStatusWindow(pX11IMData, 0, False);
                    goto finally;
                }
                wcsncpy(statusWindow->status,
                        text->string.wide_char,
                        MAX_STATUS_LEN);
            }
            XwcTextExtents(statusWindow->fontset, statusWindow->status,
                           st_wcslen(statusWindow->status), &ink, &logical);
            statusWindow->statusW = logical.width + BORDER_MARGIN;
            statusWindow->statusH = logical.height + BORDER_MARGIN;
            xwc.x = statusWindow->x - statusWindow->off_x;
            if (xwc.x < 0 ) xwc.x = 0;
            xwc.width = statusWindow->statusW;
            xwc.height = statusWindow->statusH;
            if (xwc.x + xwc.width > statusWindow->rootW){
                xwc.x = statusWindow->rootW - xwc.width;
            }
            XConfigureWindow(dpy, statusWindow->w, value_make, &xwc);
          if (statusWindow->status_ready && statusWindow->on == True){
            onoffStatusWindow(pX11IMData, statusWindow->parent, True);
          }
            paintStatusWindow(statusWindow);
            if (statusWindow->peText)
                draw_preedit(statusWindow);
        }
        else {
            wcscpy(statusWindow->status, L"");
            /*just turnoff the status window
            paintStatusWindow(statusWindow);
            */
            onoffStatusWindow(pX11IMData, 0, False);
        }
    }

 finally:
    AWT_UNLOCK();
}

/* return the string length without trailing spaces    */
/* work around code for Japanese AIXIM is implemented. */
static int st_wcslen(wchar_t *string)
{
    int len = (int32_t)wcslen(string);
    if (len == 0)
        return 0;
   for (len--;len >= 0; len--) {
        if (!iswspace((wint_t) string[len])) break;
    }
    return len+1;
}

/*
 * Checks whether given XIMText contains a string data.
 */
static Bool is_text_available(XIMText * text)
{
    if (text == NULL || text->length==0)
        return False;
    if (text->encoding_is_wchar) {
        if(text->string.wide_char[0] == L'\0')
            return False;
    } else {
        if (text->string.multi_byte[0] == '\0')
            return False;
    }
    return True;
}

/*
 * check if preedit status is active
*/
static Bool isPreeditStateActive(XIC ic)
{
    XIMPreeditState state = XIMPreeditUnKnown;
    XVaNestedList pr_atrb;
    char* nosupportAttr;

    if (ic == NULL) return False;

    pr_atrb = XVaCreateNestedList(0,XNPreeditState,&state,NULL);
    nosupportAttr=XGetICValues(ic,XNPreeditAttributes,pr_atrb,NULL);
    XFree(pr_atrb);
    if (nosupportAttr==NULL && state & XIMPreeditDisable)
        return False;
    else
        return True;
}

static void * buf_insert(void * src, void * insert, int size,
                         int src_len, int ins_len, int offset)
{
    char *temp;

    temp = realloc(src, size*(src_len+ins_len+1));
    if (temp == NULL) {
        THROW_OUT_OF_MEMORY_ERROR();
        return src;
    }
    if (offset != src_len) {
        memmove(&temp[size*(offset+ins_len)],
                &((char *)temp)[size*offset],
                size*(src_len-offset));
    }
    memcpy(&temp[size*offset], insert, size*ins_len);

    return (void *)temp;
}

static void * handle_buffer(void * source, void * insert,
                            int size,int src_len, int ins_len,
                            int del_len, int offset)
{
    void * temp = source;

    if (del_len > 0) {
        if (del_len == ins_len) {
            memcpy(&((char *)source)[size*offset], insert, size*ins_len);
            return source;
        }
        else if (src_len > offset+del_len) {
            memmove(&((char *)source)[size*offset],
                    &((char *)source)[size*(offset+del_len)],
                    size*(src_len-offset-del_len));
        }
    }
    if (ins_len > 0) {
        temp = buf_insert(source, insert, size, src_len,
                          ins_len, offset);
    }
    return temp;
}
/*
 * Display the given preedit text to the root window which is ownd by
 * myself. All of the character is converted to wide char.
 * this function is used for the passive client.
 */
static void preedit_draw_passive(X11InputMethodData *pX11IMData,
                    XIMPreeditDrawCallbackStruct *pre_draw)
{
    XIMText *text;
    wchar_t *tempbuf = NULL;
    StatusWindow *statusWindow;
    wchar_t *cur_text;
    unsigned long *cur_attr;
    int     cur_len = 0;
    int     chg_len = pre_draw->chg_length;
    int     chg_1st = pre_draw->chg_first;

    if (NULL == (statusWindow = pX11IMData->statusWindow))
        return;
    cur_text = statusWindow->peText;
    cur_attr = statusWindow->peAttr;
    if (cur_text == NULL && pre_draw->text == NULL)
        return;

    if (cur_text != NULL)
        cur_len = (int32_t)wcslen(cur_text);
    text = pre_draw->text;
    if (text == NULL) {
        /* delete only */
        if (cur_len >  chg_1st+chg_len) {
            memmove(&cur_text[chg_1st],
                    &cur_text[chg_1st+chg_len],
                    sizeof(wchar_t)*(cur_len-chg_1st-chg_len));
            memmove(&cur_attr[chg_1st],
                    &cur_attr[chg_1st+chg_len],
                    sizeof(long)*(cur_len-chg_1st-chg_len));
        }
        if ((pre_draw->chg_length <= cur_len ) && (pre_draw->chg_length >0))
            cur_text[cur_len-pre_draw->chg_length] =L'\0';
    } else {
        /* insert or replace */
        int     ins_len = 0;
        void *  ins_text = NULL;

        /* if invalid offset is specified, do nothing. */
        /* this fix is for aixim for eucTW             */
        if (cur_len < chg_1st)
            return;
        if(is_text_available(text)) {
            /* insert or replace the text */
            if (text->encoding_is_wchar == False) {
                /* convert the text to wide chars.
                   allocate enough size buffer
                */
                tempbuf = (wchar_t *)malloc(sizeof(wchar_t)*(text->length+1));
                if (tempbuf == NULL) {
                    THROW_OUT_OF_MEMORY_ERROR();
                    return;
                }
                ins_len = (int32_t)mbstowcs(tempbuf, text->string.multi_byte,
                                   text->length);
                if (ins_len == -1) {
                        free(tempbuf);
                        return;
                }
                ins_text = (void *)tempbuf;
            }
            else {
                ins_len = text->length;
                ins_text = text->string.wide_char;
            }
            /* finish prepare the data to be inserted */

            statusWindow->peText =
                    handle_buffer(cur_text, ins_text, sizeof(wchar_t),
                                  cur_len, ins_len, chg_len, chg_1st);
            statusWindow->peAttr =
                    handle_buffer(cur_attr, text->feedback, sizeof(long),
                                  cur_len, ins_len, chg_len, chg_1st);
            statusWindow->peText[cur_len-chg_len+ins_len] =L'\0';

            if (tempbuf != NULL)
                free(tempbuf);
        } /* endof insert or replace text */
        else {
            /* change attribute only */
            memcpy(&cur_attr[chg_1st], text->feedback,
                    sizeof(long)*text->length);
        }
    }
    statusWindow->peCaret= pre_draw->caret;
    draw_preedit(statusWindow);
    if (statusWindow->on && wcslen(statusWindow->peText) > 0)
        onoffStatusWindow(pX11IMData, statusWindow->parent, True);
    else if (wcslen(statusWindow->status) == 0)
        onoffStatusWindow(pX11IMData, 0, False);
}

/*
 * reset predit test of passive mode
 */
static void
resetPassivePreeditText(StatusWindow *statusWindow)
{
    if (NULL == statusWindow) return;
    if(statusWindow->peText != NULL) {
        free(statusWindow->peText);
        statusWindow->peText = NULL;
    }
    if(statusWindow->peAttr != NULL) {
        free(statusWindow->peAttr);
        statusWindow->peAttr = NULL;
    }
    statusWindow->peCaret= 0;
}

static void draw_caret(StatusWindow *statusWindow, GC gc, int pos)
{
    if (NULL == statusWindow) return;
    XSetFunction(dpy, gc, GXinvert);
    XDrawLine(dpy, statusWindow->w,
              gc, pos, STATUS_BORDER/2,
              pos, STATUS_BORDER/2+statusWindow->fOff);
    XSetFunction(dpy, gc, GXcopy);
}

static int  get_next_attr(int len, unsigned long *attr)
{
    int count;

    for (count = 1; count < len; count++)  {
        if ((attr[count-1] & PREEDIT_ATTR_MASK)
            != (attr[count] & PREEDIT_ATTR_MASK))
            break;
    }
    return count;
}

static void draw_preedit(StatusWindow *statusWindow)
{
    unsigned long *attr;
    int x_pos,x_caret;
    unsigned int  len;
    int len_disp, pos;
    wchar_t *str;
    GC  gc;
    XRectangle ink, rect, rect_c;
    Bool caret_done = False;

    if (NULL == statusWindow) return;
    align_status(statusWindow);
    XFillRectangle(dpy, statusWindow->w,
                   statusWindow->bgGC,
                   statusWindow->statusW,0,
                   statusWindow->statusW + statusWindow->peTextW + BORDER_MARGIN,
                   statusWindow->fBot+2);


    XDrawLine(dpy, statusWindow->w, statusWindow->fgGC,
              statusWindow->statusW, 0,
              statusWindow->statusW + statusWindow->peTextW + BORDER_MARGIN, 0);
    XDrawLine(dpy, statusWindow->w, statusWindow->fgGC,
              statusWindow->statusW, statusWindow->fBot+2,
              statusWindow->statusW + statusWindow->peTextW + BORDER_MARGIN,
              statusWindow->fBot+2);
    XDrawLine(dpy, statusWindow->w, statusWindow->fgGC,
              statusWindow->statusW + statusWindow->peTextW + BORDER_MARGIN, 0,
              statusWindow->statusW + statusWindow->peTextW + BORDER_MARGIN,
              statusWindow->fBot+2);
    if (0 == statusWindow->statusW)
        XDrawLine(dpy, statusWindow->w, statusWindow->fgGC,
                  0, 0, 0, statusWindow->fBot+2);

    str =  statusWindow->peText;

    if (str != NULL &&  (len = (int32_t)wcslen(str)) != 0) {
        pos = 0;
        attr = statusWindow->peAttr;
        x_pos = x_caret = statusWindow->statusW + STATUS_BORDER;
        while((int)len-1 >= pos) {
            len_disp = get_next_attr(len - pos, &attr[pos]);
            if (attr[pos] & XIMReverse) {
                gc = statusWindow->bgGC;
            }
            else {
                gc = statusWindow->fgGC;
            }
            XwcTextExtents(statusWindow->fontset,
                           &str[pos],
                           len_disp, &ink, &rect);
            XwcDrawImageString(dpy, statusWindow->w,
                               statusWindow->fontset, gc,
                               x_pos, statusWindow->fOff+1, &str[pos], len_disp);
            if (attr[pos] & XIMUnderline) {
                XDrawLine(dpy, statusWindow->w,
                          gc, x_pos, statusWindow->fBot,
                          x_pos+rect.width, statusWindow->fBot);
            }
            if (!caret_done) {
                if( statusWindow->peCaret >= pos &&
                    statusWindow->peCaret <= pos+len_disp) {
                    if (statusWindow->peCaret == 0)
                        x_caret = x_pos;
                    else if (statusWindow->peCaret == pos+len_disp)
                        x_caret = x_pos+rect.width;
                    else {
                        XwcTextExtents(statusWindow->fontset,
                                        &str[pos],
                                        statusWindow->peCaret-pos,
                                        &ink, &rect_c);
                        x_caret = x_pos+ rect_c.width;
                    }
                    x_caret-=CARET_OFFSET;
                    caret_done = True;
                }
            }
            pos += len_disp;
            x_pos += rect.width;
        }
        if (caret_done)
            draw_caret(statusWindow, statusWindow->fgGC, x_caret);
    }
}

/* calc required status window size and resize the window */
static void align_status(StatusWindow *statusWindow)
{
    int len_st, len_pe = 0;
    XRectangle rect_st, rect_pe, ink;
    Dimension cur_w;
    int value_make = CWX|CWWidth|CWHeight;
    XWindowChanges xwc;

    if (NULL == statusWindow) return;
    if ((len_st = st_wcslen(statusWindow->status)) == 0
        && (statusWindow->peText == NULL || st_wcslen(statusWindow->peText) == 0 ))
        return;

    rect_pe.x = rect_pe.y = rect_pe.width = rect_pe.height = 0;

    XwcTextExtents(statusWindow->fontset,
                   statusWindow->status,
                   len_st, &ink, &rect_st);
    if (statusWindow->peText != NULL
        && (len_pe = (int32_t)wcslen(statusWindow->peText)) > 0) {
        XwcTextExtents(statusWindow->fontset,
                       statusWindow->peText,
                       len_pe, &ink, &rect_pe);
    }
    statusWindow->fOff = max(-rect_st.y, -rect_pe.y);
    statusWindow->fBot = max(rect_st.height, rect_pe.height);
    statusWindow->statusW =rect_st.width;
    if (rect_st.width > 0) statusWindow->statusW += BORDER_MARGIN;
    statusWindow->peTextW = rect_pe.width;

    xwc.x = statusWindow->x - statusWindow->off_x;
    if (xwc.x < 0 ) xwc.x = 0;

    if (len_pe > 0) {
        xwc.width = statusWindow->statusW
                    + statusWindow->peTextW + BORDER_MARGIN + 1;
        xwc.height = statusWindow->fBot + BORDER_MARGIN;
    } else {
        xwc.width = statusWindow->statusW;
        xwc.height = statusWindow->fBot + BORDER_MARGIN;
    }
    if (xwc.x + xwc.width > statusWindow->rootW){
      xwc.x = statusWindow->rootW - xwc.width;
    }
    XConfigureWindow(dpy, statusWindow->w, value_make, &xwc);
}

static void shrink_status(StatusWindow *statusWindow)
{
    int value_make = CWX|CWWidth|CWHeight;
    XWindowChanges xwc;

    if (NULL == statusWindow) return;
    xwc.width  = statusWindow->statusW;
    xwc.height = statusWindow->statusH;
    statusWindow->peTextW = 0;
    xwc.x = statusWindow->x - statusWindow->off_x;
    if (xwc.x < 0 ) xwc.x = 0;
    if (xwc.x + xwc.width > statusWindow->rootW){
      xwc.x = statusWindow->rootW - xwc.width;
    }
    XConfigureWindow(dpy, statusWindow->w, value_make, &xwc);
}

static Bool isNativeIm()
{
#define XIMMODIFIER          "@im="
#define XIM_SERVER_CATEGORY  "@server="
    char *immodifiers;
    char *imserver, *imserverPtr;
    Atom imserverAtom;

    if (!(immodifiers = getenv("XMODIFIERS"))) return True;
    if (!(imserver = calloc(1,strlen(immodifiers)+strlen(XIM_SERVER_CATEGORY)+1))) return True;
    if (!(immodifiers = strstr(immodifiers,XIMMODIFIER))) return True;
    immodifiers += strlen(XIMMODIFIER);
    strcpy(imserver,XIM_SERVER_CATEGORY);
    imserverPtr = imserver + strlen(imserver);
    while(*immodifiers != '@' && *immodifiers != '\0') {
        *imserverPtr = *immodifiers;
        imserverPtr++;
        immodifiers++;
    }
    imserverAtom = XInternAtom(awt_display, imserver, True);
    free(imserver);
    if (imserverAtom > 0)
        return False;
    else
        return True;
}

static Window getGrandParent(Window parent)
{
    Window containerWindow,rootWindow,tmp;
    Window *ignoreWindowPtr;
    unsigned int ignoreUnit;
    Window grandParent=0;
    XWindowAttributes xwa;
    Atom WM_STATE;
    Atom type = None;
    int32_t format;
    unsigned long nitems, after;
    unsigned char * data;

    if (parent == 0) return grandParent;
    WM_STATE = XInternAtom(dpy, "WM_STATE", True);
    if (WM_STATE == None) return grandParent;

    tmp=parent;
    while(XQueryTree(dpy, tmp,
                     &rootWindow, &containerWindow,
                     &ignoreWindowPtr, &ignoreUnit)){
        XFree(ignoreWindowPtr);
        if (containerWindow == rootWindow) break;
        if (XGetWindowProperty(dpy, containerWindow, WM_STATE,
                    0, 0, False, AnyPropertyType,
                    &type, &format, &nitems, &after, &data) == Success) {
            XFree(data);
            if (type) {
                XGetWindowAttributes(dpy, containerWindow, &xwa);
                if (FALSE == xwa.override_redirect){
                    grandParent=containerWindow;
                }
            }
        }
        tmp=containerWindow;
    }
    return grandParent;
}

static void moveStatusWindow(StatusWindow *statusWindow)
{
    XWindowAttributes xwa;
    Window child;
    int x, y, width;
    Window target;

    if (NULL == statusWindow) return;
    if (statusWindow->grandParent) {
        target = statusWindow->grandParent;
    } else {
        target = statusWindow->parent;
    }
    XGetWindowAttributes(dpy, target, &xwa);
    XTranslateCoordinates(dpy,
                          target, xwa.root,
                          0, 0,
                          &x, &y,
                          &child);
    if (statusWindow->x != x
        || statusWindow->y != y
        || statusWindow->width != xwa.width
        || statusWindow->height != xwa.height){
        statusWindow->x = x;
        statusWindow->y = y;
        statusWindow->height = xwa.height;
        statusWindow->width = xwa.width;
        x = statusWindow->x - statusWindow->off_x;
        y = statusWindow->y + statusWindow->height + statusWindow->off_y;
        if (x < 0 ){
            x = 0;
        }
        if (statusWindow->peTextW > 0) {
            width = statusWindow->statusW + statusWindow->peTextW + BORDER_MARGIN + 1;
            if (x + width > statusWindow->rootW){
                x = statusWindow->rootW - width;
            }
        } else {
            if (x + statusWindow->statusW > statusWindow->rootW){
                x = statusWindow->rootW - statusWindow->statusW;
            }
        }
        if (y + statusWindow->statusH > statusWindow->rootH){
            y = statusWindow->rootH - statusWindow->statusH;
        }
        XMoveWindow(dpy, statusWindow->w, x, y);
    }
}

static void arrange_window_stack(StatusWindow* statusWindow)
{
    XWindowChanges xwc;
    int value_make = CWSibling|CWStackMode;
    Window root, parent, *children;
    unsigned int nchildren;

    if (NULL == statusWindow) return;
    if (XQueryTree(dpy, statusWindow->parent,
                       &root, &parent, &children, &nchildren)){
        XFree(children);
        xwc.sibling = parent;
        while(XQueryTree(dpy, xwc.sibling, &root, &parent, &children, &nchildren)) {
            XFree(children);
            if (root != parent) {
                xwc.sibling = parent;
            } else {
                break;
            }
        }
        xwc.stack_mode = Above;
        XConfigureWindow(dpy, statusWindow->w, value_make, &xwc);
    }
}

static int count_missing_fonts(char **charset_list, int charset_count)
{
    int i,j;
    if (charset_count > 0) {
        j=charset_count;
        for(i=0; i < charset_count; i++) {
            if ((strstr(charset_list[i], "IBM-udc")) ||
                (strstr(charset_list[i], "IBM-sbd")) ||
                (strstr(charset_list[i], "IBM-ucdTW")))
                j--;
        }
        return j;
    }
    else
        return 0;
}

static XFontSet create_fontset_name(char * font_name, Bool force)
{
    XFontSet fontset = NULL;
    char **charset_list;
    int charset_count;
    char *def_string;
    int missing_fonts;

    fontset = XCreateFontSet(dpy, font_name,
                &charset_list, &charset_count, &def_string);
    if (charset_count > 0) {
        missing_fonts = count_missing_fonts(charset_list,
                                            charset_count);
        XFreeStringList(charset_list);
        if (fontset && (missing_fonts > 0)) {
            if (!force) {
                XFreeFontSet(dpy, fontset);
                fontset = NULL;
            }
        }
    }
    return fontset;
}

static XFontSet create_fontset()
{
    XFontSet fontset = NULL;
    int i;
    static char * fontlist[] = {
        "-dt-interface user-medium-r-normal-S*-*-*-*-*-*-*-*-*",
        "-*-*-medium-r-normal-*-14-*-*-*-c-*-*-*",
        "-*-*-medium-r-normal-*-14-*-*-*-m-*-*-*",
        "-*-*-medium-r-normal--14-0-0-0-m-*-*-*",
        "-monotype-sansmonowt-medium-r-normal--14-*-*-*-m-*-*-*",
        "-*--14-*",
        "-dt-interface user-medium-r-normal-s*-*-*-*-*-*-*-*-*",
        "-*--16-*",
        "-*--17-*",
        "-*--18-*",
        "-*--19-*",
        "-*--20-*",
        "-*--24-*",
        NULL};

    for (i=0; fontlist[i] != NULL && fontset==NULL; i++)
        fontset = create_fontset_name(fontlist[i], False);

    if (!fontset)
        fprintf(stdout, "Cannot load fonts for IMF.\n");
    return  fontset;
}

static Window get_current_focus(XIC ic) {
    Window w = 0;
    if (ic != NULL)
        XGetICValues(ic, XNFocusWindow, &w, NULL);
    return w;
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_X11_XInputMethod_openXIMNative(JNIEnv *env,
                                            jobject this,
                                            jlong display)
{
    Bool registered;

    AWT_LOCK();

    dpy = (Display *)jlong_to_ptr(display);

    if (X11im == NULL) {
        X11im = XOpenIM(dpy, NULL, NULL, NULL);
    }

    AWT_UNLOCK();

    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_X11_XInputMethod_createXICNative(JNIEnv *env,
                                              jobject this,
                                              jlong window)
{
    X11InputMethodData *pX11IMData;
    jobject globalRef;
    XIC ic;

    AWT_LOCK();

    if (!window) {
        JNU_ThrowNullPointerException(env, "NullPointerException");
        AWT_UNLOCK();
        return JNI_FALSE;
    }

    pX11IMData = (X11InputMethodData *) calloc(1, sizeof(X11InputMethodData));
    if (pX11IMData == NULL) {
        THROW_OUT_OF_MEMORY_ERROR();
        AWT_UNLOCK();
        return JNI_FALSE;
    }

    globalRef = (*env)->NewGlobalRef(env, this);
    pX11IMData->x11inputmethod = globalRef;
    pX11IMData->statusWindow = NULL;

    setX11InputMethodData(env, this, pX11IMData);

    if (createXIC(env, pX11IMData, (Window)window) == False) {
        destroyX11InputMethodData((JNIEnv *) NULL, pX11IMData);
        pX11IMData = (X11InputMethodData *) NULL;
        setX11InputMethodData(env, this, pX11IMData);
        if ((*env)->ExceptionCheck(env)) {
            goto finally;
        }
    }

finally:
    AWT_UNLOCK();
    return (pX11IMData != NULL);
}

JNIEXPORT void JNICALL
Java_sun_awt_X11_XInputMethod_setXICFocusNative(JNIEnv *env,
                                                jobject this,
                                                jlong w,
                                                jboolean req,
                                                jboolean active)
{
    X11InputMethodData *pX11IMData;
    AWT_LOCK();
    pX11IMData = getX11InputMethodData(env, this);
    if (pX11IMData == NULL) {
        AWT_UNLOCK();
        return;
    }

    if (req) {
        if (!w) {
            AWT_UNLOCK();
            return;
        }
        pX11IMData->isActiveClient = active;
        pX11IMData->current_ic = active ?
                        pX11IMData->ic_active : pX11IMData->ic_passive;
        /*
         * On Solaris2.6, setXICWindowFocus() has to be invoked
         * before setting focus.
         */
        get_current_focus(pX11IMData->current_ic); /* workaround for kinput2 and SCIM */
        if (currentFocusWindow != w) {
            setXICWindowFocus(pX11IMData->current_ic, w);
            setXICFocus(pX11IMData->current_ic, req);
            currentX11InputMethodInstance = pX11IMData->x11inputmethod;
            currentFocusWindow =  w;
        } else {
            setXICFocus(pX11IMData->current_ic, req);
        }
        if ((active || pX11IMData->passiveStatusWindow)
            && (pX11IMData->statusWindow && pX11IMData->statusWindow->on))
            onoffStatusWindow(pX11IMData, w, True);
    } else {
        currentX11InputMethodInstance = NULL;
        currentFocusWindow = 0;
        onoffStatusWindow(pX11IMData, 0, False);
        if (pX11IMData->current_ic != NULL)
        setXICFocus(pX11IMData->current_ic, req);

        pX11IMData->current_ic = (XIC)0;
    }

    XFlush(dpy);
    AWT_UNLOCK();
}

/*
 * Class:     sun_awt_X11InputMethodBase
 * Method:    initIDs
 * Signature: ()V
 * This function gets called from the static initializer for
 * X11InputMethod.java to initialize the fieldIDs for fields
 * that may be accessed from C
 */
JNIEXPORT void JNICALL Java_sun_awt_X11InputMethodBase_initIDs
  (JNIEnv *env, jclass cls)
{
    x11InputMethodIDs.pData = (*env)->GetFieldID(env, cls, "pData", "J");
}

/*
 * Class:     sun_awt_X11InputMethodBase
 * Method:    turnoffStatusWindow
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11InputMethodBase_turnoffStatusWindow
  (JNIEnv *env, jobject this)
{
    X11InputMethodData *pX11IMData;
    StatusWindow *statusWindow;

    AWT_LOCK();

    if (NULL == currentX11InputMethodInstance
        || NULL == (pX11IMData = getX11InputMethodData(env,currentX11InputMethodInstance))
        || NULL == (statusWindow = pX11IMData->statusWindow)
        || !statusWindow->on ){
        AWT_UNLOCK();
        return;
    }
    onoffStatusWindow(pX11IMData, 0, False);
    statusWindow->on = False;

    AWT_UNLOCK();
}

/*
 * Class:     sun_awt_X11InputMethodBase
 * Method:    disposeXIC
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11InputMethodBase_disposeXIC
  (JNIEnv *env, jobject this)
{
    X11InputMethodData *pX11IMData = NULL;

    AWT_LOCK();
    pX11IMData = getX11InputMethodData(env, this);
    if (pX11IMData == NULL) {
        AWT_UNLOCK();
        return;
    }

    setX11InputMethodData(env, this, NULL);

    if (pX11IMData->x11inputmethod == currentX11InputMethodInstance) {
        currentX11InputMethodInstance = NULL;
        currentFocusWindow = 0;
    }
    destroyX11InputMethodData(env, pX11IMData);
    AWT_UNLOCK();
}

/*
 * Class:     sun_awt_X11InputMethodBase
 * Method:    resetXIC
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sun_awt_X11InputMethodBase_resetXIC
  (JNIEnv *env, jobject this)
{
    X11InputMethodData *pX11IMData;
    char *xText = NULL;
    jstring jText = (jstring)0;

    AWT_LOCK();
    pX11IMData = getX11InputMethodData(env, this);
    if (pX11IMData == NULL) {
        AWT_UNLOCK();
        return jText;
    }

    if (pX11IMData->current_ic) {
        if (!isPreeditStateActive(pX11IMData->current_ic)) {
            xText = NULL;
        } else {
            if (!(pX11IMData->forceReset))
                setXICFocus(pX11IMData->current_ic, FALSE);
            xText = XmbResetIC(pX11IMData->current_ic);
            if (!(pX11IMData->forceReset))
                setXICFocus(pX11IMData->current_ic, TRUE);
        }
    } else {
        /*
         * If there is no reference to the current XIC, try to reset both XICs.
         */
        if (!isPreeditStateActive(pX11IMData->ic_active))
            xText = NULL;
        else
        xText = XmbResetIC(pX11IMData->ic_active);
        /*it may also means that the real client component does
          not have focus -- has been deactivated... its xic should
          not have the focus, bug#4284651 showes reset XIC for htt
          may bring the focus back, so de-focus it again.
        */
        setXICFocus(pX11IMData->ic_active, FALSE);
        if (pX11IMData->ic_active != pX11IMData->ic_passive) {
            char *tmpText;
            if (!isPreeditStateActive(pX11IMData->ic_passive))
                tmpText = NULL;
            else
                tmpText = XmbResetIC(pX11IMData->ic_passive);
            setXICFocus(pX11IMData->ic_passive, FALSE);
            if (xText == (char *)NULL && tmpText)
                xText = tmpText;
        }
    }
    if (xText != NULL) {
        jText = JNU_NewStringPlatform(env, (const char *)xText);
        XFree((void *)xText);
    }

    /* workaround
     * Some IME do not call PreeditDoneCallback routine even
     * when XmbResetIC is called. I force to reset the preedit string.
     */
    if (!pX11IMData->isActiveClient) {
        resetPassivePreeditText(pX11IMData->statusWindow);
        shrink_status(pX11IMData->statusWindow);
    } else {
        JNU_CallMethodByName(env, NULL, pX11IMData->x11inputmethod,
                             "clearComposedText",
                             "()V");
        if ((*env)->ExceptionOccurred(env)) {
            (*env)->ExceptionDescribe(env);
            (*env)->ExceptionClear(env);
        }
    }

    AWT_UNLOCK();
    return jText;
}

/*
 * Class:     sun_awt_X11InputMethodBase
 * Method:    setCompositionEnabledNative
 * Signature: (Z)Z
 *
 * This method tries to set the XNPreeditState attribute associated with the current
 * XIC to the passed in 'enable' state.
 *
 * Return JNI_TRUE if XNPreeditState attribute is successfully changed to the
 * 'enable' state; Otherwise, if XSetICValues fails to set this attribute,
 * java.lang.UnsupportedOperationException will be thrown. JNI_FALSE is returned if this
 * method fails due to other reasons.
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11InputMethodBase_setCompositionEnabledNative
  (JNIEnv *env, jobject this, jboolean enable)
{
    X11InputMethodData *pX11IMData;
    char * ret = NULL;
    XVaNestedList pr_atrb;

    AWT_LOCK();
    pX11IMData = getX11InputMethodData(env, this);

    if ((pX11IMData == NULL) || (pX11IMData->current_ic == NULL)) {
        AWT_UNLOCK();
        return JNI_FALSE;
    }

    pr_atrb = XVaCreateNestedList(0, XNPreeditState,
                  (enable ? XIMPreeditEnable : XIMPreeditDisable), NULL);
    ret = XSetICValues(pX11IMData->current_ic, XNPreeditAttributes, pr_atrb, NULL);
    XFree((void *)pr_atrb);
    AWT_UNLOCK();

    if ((ret != 0) &&
        ((strcmp(ret, XNPreeditAttributes) == 0)
         || (strcmp(ret, XNPreeditState) == 0))) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException", "");
    }

    return (jboolean)(ret == 0);
}

/*
 * Class:     sun_awt_X11InputMethodBase
 * Method:    isCompositionEnabledNative
 * Signature: ()Z
 *
 * This method tries to get the XNPreeditState attribute associated with the current XIC.
 *
 * Return JNI_TRUE if the XNPreeditState is successfully retrieved. Otherwise, if
 * XGetICValues fails to get this attribute, java.lang.UnsupportedOperationException
 * will be thrown. JNI_FALSE is returned if this method fails due to other reasons.
 */
JNIEXPORT jboolean JNICALL Java_sun_awt_X11InputMethodBase_isCompositionEnabledNative
  (JNIEnv *env, jobject this)
{
    X11InputMethodData *pX11IMData = NULL;
    char * ret = NULL;
    XIMPreeditState state = XIMPreeditUnKnown;
    XVaNestedList   pr_atrb;

    AWT_LOCK();
    pX11IMData = getX11InputMethodData(env, this);

    if ((pX11IMData == NULL) || (pX11IMData->current_ic == NULL)) {
        AWT_UNLOCK();
        return JNI_FALSE;
    }

    pr_atrb = XVaCreateNestedList(0, XNPreeditState, &state, NULL);
    ret = XGetICValues(pX11IMData->current_ic, XNPreeditAttributes, pr_atrb, NULL);
    XFree((void *)pr_atrb);
    AWT_UNLOCK();

    if ((ret != 0) &&
        ((strcmp(ret, XNPreeditAttributes) == 0)
         || (strcmp(ret, XNPreeditState) == 0))) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException", "");
        return JNI_FALSE;
    }

    return (jboolean)(state == XIMPreeditEnable);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XInputMethod_adjustStatusWindow
  (JNIEnv *env, jobject this, jlong window)
{

}

/*
 * Class:     sun_awt_X11InputMethod
 * Method:    setStatusAreaVisible
 * Signature: (ZJ)V
 */
JNIEXPORT void JNICALL Java_sun_awt_X11InputMethod_setStatusAreaVisible
  (JNIEnv *env, jobject this, jboolean value, jlong data)
{
    X11InputMethodData *pX11IMData;

    pX11IMData = getX11InputMethodData(env, this);
    if (NULL == pX11IMData) return;
    if (NULL == pX11IMData->statusWindow) return;

    if ((int)value){
        pX11IMData->statusWindow->on = True;
    }else{
        pX11IMData->statusWindow->on = False;
    }
    return;
}
