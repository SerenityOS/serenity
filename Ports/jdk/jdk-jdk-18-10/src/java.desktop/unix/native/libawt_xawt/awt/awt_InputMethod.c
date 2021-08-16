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

#include "awt.h"
#include "awt_p.h"

#include <sun_awt_X11InputMethodBase.h>
#include <sun_awt_X11_XInputMethod.h>

#include <stdio.h>
#include <stdlib.h>
#include <sys/time.h>
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
#if defined(__linux__)
static void StatusStartCallback(XIC, XPointer, XPointer);
static void StatusDoneCallback(XIC, XPointer, XPointer);
static void StatusDrawCallback(XIC, XPointer,
                               XIMStatusDrawCallbackStruct *);
#endif

#define ROOT_WINDOW_STYLES      (XIMPreeditNothing | XIMStatusNothing)
#define NO_STYLES               (XIMPreeditNone | XIMStatusNone)

#define PreeditStartIndex       0
#define PreeditDoneIndex        1
#define PreeditDrawIndex        2
#define PreeditCaretIndex       3
#if defined(__linux__)
#define StatusStartIndex        4
#define StatusDoneIndex         5
#define StatusDrawIndex         6
#define NCALLBACKS              7
#else
#define NCALLBACKS              4
#endif

/*
 * Callback function pointers: the order has to match the *Index
 * values above.
 */
static XIMProc callback_funcs[NCALLBACKS] = {
    (XIMProc)(void *)&PreeditStartCallback,
    (XIMProc)PreeditDoneCallback,
    (XIMProc)PreeditDrawCallback,
    (XIMProc)PreeditCaretCallback,
#if defined(__linux__)
    (XIMProc)StatusStartCallback,
    (XIMProc)StatusDoneCallback,
    (XIMProc)StatusDrawCallback,
#endif
};

#if defined(__linux__)
#define MAX_STATUS_LEN  100
typedef struct {
    Window   w;                /*status window id        */
    Window   root;             /*the root window id      */
    Window   parent;           /*parent shell window     */
    int      x, y;             /*parent's upperleft position */
    int      width, height;    /*parent's width, height  */
    GC       lightGC;          /*gc for light border     */
    GC       dimGC;            /*gc for dim border       */
    GC       bgGC;             /*normal painting         */
    GC       fgGC;             /*normal painting         */
    int      statusW, statusH; /*status window's w, h    */
    int      rootW, rootH;     /*root window's w, h    */
    int      bWidth;           /*border width            */
    char     status[MAX_STATUS_LEN]; /*status text       */
    XFontSet fontset;           /*fontset for drawing    */
    int      off_x, off_y;
    Bool     on;                /*if the status window on*/
} StatusWindow;
#endif

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
#if defined(__linux__)
    StatusWindow *statusWindow; /* our own status window  */
#endif
    char        *lookup_buf;    /* buffer used for XmbLookupString */
    int         lookup_buf_len; /* lookup buffer size in bytes */
} X11InputMethodData;

/*
 * When XIC is created, a global reference is created for
 * sun.awt.X11InputMethod object so that it could be used by the XIM callback
 * functions. This could be a dangerous thing to do when the original
 * X11InputMethod object is garbage collected and as a result,
 * destroyX11InputMethodData is called to delete the global reference.
 * If any XIM callback function still holds and uses the "already deleted"
 * global reference, disaster is going to happen. So we have to maintain
 * a list for these global references which is consulted first when the
 * callback functions or any function tries to use "currentX11InputMethodObject"
 * which always refers to the global reference try to use it.
 *
 */
typedef struct _X11InputMethodGRefNode {
    jobject inputMethodGRef;
    struct _X11InputMethodGRefNode* next;
} X11InputMethodGRefNode;

X11InputMethodGRefNode *x11InputMethodGRefListHead = NULL;

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

static void DestroyXIMCallback(XIM, XPointer, XPointer);
static void OpenXIMCallback(Display *, XPointer, XPointer);
/* Solaris XIM Extention */
#define XNCommitStringCallback "commitStringCallback"
static void CommitStringCallback(XIC, XPointer, XPointer);

static X11InputMethodData * getX11InputMethodData(JNIEnv *, jobject);
static void setX11InputMethodData(JNIEnv *, jobject, X11InputMethodData *);
static void destroyX11InputMethodData(JNIEnv *, X11InputMethodData *);
static void freeX11InputMethodData(JNIEnv *, X11InputMethodData *);
#if defined(__linux__)
static Window getParentWindow(Window);
#endif

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

/*
 * Returns True if the global reference is still in the list,
 * otherwise False.
 */
static Bool isX11InputMethodGRefInList(jobject imGRef) {
    X11InputMethodGRefNode *pX11InputMethodGRef = x11InputMethodGRefListHead;

    if (imGRef == NULL) {
        return False;
    }

    while (pX11InputMethodGRef != NULL) {
        if (pX11InputMethodGRef->inputMethodGRef == imGRef) {
            return True;
        }
        pX11InputMethodGRef = pX11InputMethodGRef->next;
    }

    return False;
}

/*
 * Add the new created global reference to the list.
 */
static void addToX11InputMethodGRefList(jobject newX11InputMethodGRef) {
    X11InputMethodGRefNode *newNode = NULL;

    if (newX11InputMethodGRef == NULL ||
        isX11InputMethodGRefInList(newX11InputMethodGRef)) {
        return;
    }

    newNode = (X11InputMethodGRefNode *)malloc(sizeof(X11InputMethodGRefNode));

    if (newNode == NULL) {
        return;
    } else {
        newNode->inputMethodGRef = newX11InputMethodGRef;
        newNode->next = x11InputMethodGRefListHead;
        x11InputMethodGRefListHead = newNode;
    }
}

/*
 * Remove the global reference from the list.
 */
static void removeX11InputMethodGRefFromList(jobject x11InputMethodGRef) {
     X11InputMethodGRefNode *pX11InputMethodGRef = NULL;
     X11InputMethodGRefNode *cX11InputMethodGRef = x11InputMethodGRefListHead;

     if (x11InputMethodGRefListHead == NULL ||
         x11InputMethodGRef == NULL) {
         return;
     }

     /* cX11InputMethodGRef always refers to the current node while
        pX11InputMethodGRef refers to the previous node.
     */
     while (cX11InputMethodGRef != NULL) {
         if (cX11InputMethodGRef->inputMethodGRef == x11InputMethodGRef) {
             break;
         }
         pX11InputMethodGRef = cX11InputMethodGRef;
         cX11InputMethodGRef = cX11InputMethodGRef->next;
     }

     if (cX11InputMethodGRef == NULL) {
         return; /* Not found. */
     }

     if (cX11InputMethodGRef == x11InputMethodGRefListHead) {
         x11InputMethodGRefListHead = x11InputMethodGRefListHead->next;
     } else {
         pX11InputMethodGRef->next = cX11InputMethodGRef->next;
     }
     free(cX11InputMethodGRef);

     return;
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
#if defined(__linux__)
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
        free((void*)sw);
    }
#endif

    if (pX11IMData->callbacks)
        free((void *)pX11IMData->callbacks);

    if (env) {
        /* Remove the global reference from the list, so that
           the callback function or whoever refers to it could know.
        */
        removeX11InputMethodGRefFromList(pX11IMData->x11inputmethod);
        (*env)->DeleteGlobalRef(env, pX11IMData->x11inputmethod);
    }

    if (pX11IMData->lookup_buf) {
        free((void *)pX11IMData->lookup_buf);
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

    if (!isX11InputMethodGRefInList(currentX11InputMethodInstance)) {
        currentX11InputMethodInstance = NULL;
        return False;
    }

    pX11IMData = getX11InputMethodData(env, currentX11InputMethodInstance);

    if (pX11IMData == NULL) {
#if defined(__linux__)
        return False;
#else
        return result;
#endif
    }

    if ((ic = pX11IMData->current_ic) == (XIC)0){
#if defined(__linux__)
        return False;
#else
        return result;
#endif
    }

    /* allocate the lookup buffer at the first invocation */
    if (pX11IMData->lookup_buf_len == 0) {
        pX11IMData->lookup_buf = (char *)malloc(INITIAL_LOOKUP_BUF_SIZE);
        if (pX11IMData->lookup_buf == NULL) {
            THROW_OUT_OF_MEMORY_ERROR();
            return result;
        }
        pX11IMData->lookup_buf_len = INITIAL_LOOKUP_BUF_SIZE;
    }

    mblen = XmbLookupString(ic, event, pX11IMData->lookup_buf,
                            pX11IMData->lookup_buf_len - 1, &keysym, &status);

    /*
     * In case of overflow, a buffer is allocated and it retries
     * XmbLookupString().
     */
    if (status == XBufferOverflow) {
        free((void *)pX11IMData->lookup_buf);
        pX11IMData->lookup_buf_len = 0;
        pX11IMData->lookup_buf = (char *)malloc(mblen + 1);
        if (pX11IMData->lookup_buf == NULL) {
            THROW_OUT_OF_MEMORY_ERROR();
            return result;
        }
        pX11IMData->lookup_buf_len = mblen + 1;
        mblen = XmbLookupString(ic, event, pX11IMData->lookup_buf,
                            pX11IMData->lookup_buf_len - 1, &keysym, &status);
    }
    pX11IMData->lookup_buf[mblen] = 0;

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
        javastr = JNU_NewStringPlatform(env, (const char *)pX11IMData->lookup_buf);
        if (javastr != NULL) {
            JNU_CallMethodByName(env, NULL,
                                 currentX11InputMethodInstance,
                                 "dispatchCommittedText",
                                 "(Ljava/lang/String;J)V",
                                 javastr,
                                 event->time);
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

    return result;
}

#if defined(__linux__)
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

    XGetWindowAttributes(dpy, parent, &xwa);
    bw = 2; /*xwa.border_width does not have the correct value*/

    /*compare the size difference between parent container
      and shell widget, the diff should be the border frame
      and title bar height (?)*/

    XQueryTree( dpy,
                parent,
                &rootWindow,
                &containerWindow,
                &ignoreWindowPtr,
                &ignoreUnit);
    XGetWindowAttributes(dpy, containerWindow, &xxwa);

    off_x = (xxwa.width - xwa.width) / 2;
    off_y = xxwa.height - xwa.height - off_x; /*it's magic:-) */

    /*get the size of root window*/
    XGetWindowAttributes(dpy, rootWindow, &xxwa);

    XTranslateCoordinates(dpy,
                          parent, xwa.root,
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
    statusWindow = (StatusWindow*) calloc(1, sizeof(StatusWindow));
    if (statusWindow == NULL){
        THROW_OUT_OF_MEMORY_ERROR();
        return NULL;
    }
    statusWindow->w = status;
    //12, 13-point fonts
    statusWindow->fontset = XCreateFontSet(dpy,
                                           "-*-*-medium-r-normal-*-*-120-*-*-*-*," \
                                           "-*-*-medium-r-normal-*-*-130-*-*-*-*",
                                           &mclr, &mccr, &dsr);
    /* In case we didn't find the font set, release the list of missing characters */
    if (mccr > 0) {
        XFreeStringList(mclr);
    }
    statusWindow->parent = parent;
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
    statusWindow->rootH = xxwa.height;
    statusWindow->rootW = xxwa.width;
    statusWindow->lightGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->lightGC, light);
    statusWindow->dimGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->dimGC, dim);
    statusWindow->fgGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->fgGC, fg);
    statusWindow->bgGC = XCreateGC(dpy, status, valuemask, &values);
    XSetForeground(dpy, statusWindow->bgGC, bg);
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

    if (NULL == currentX11InputMethodInstance ||
        NULL == pX11IMData ||
        NULL == (statusWindow =  pX11IMData->statusWindow)){
        return;
    }

    if (ON == False) {
        XUnmapWindow(dpy, statusWindow->w);
        statusWindow->on = False;
        return;
    }
    parent = JNU_CallMethodByName(GetJNIEnv(), NULL, pX11IMData->x11inputmethod,
                                  "getCurrentParentWindow",
                                  "()J").j;
    if (statusWindow->parent != parent) {
        statusWindow->parent = parent;
    }
    XGetWindowAttributes(dpy, parent, &xwa);
    XTranslateCoordinates(dpy,
                          parent, xwa.root,
                          xwa.x, xwa.y,
                          &x, &y,
                          &child);
    if (statusWindow->x != x ||
        statusWindow->y != y ||
        statusWindow->height != xwa.height)
    {
        statusWindow->x = x;
        statusWindow->y = y;
        statusWindow->height = xwa.height;
        x = statusWindow->x - statusWindow->off_x;
        y = statusWindow->y + statusWindow->height - statusWindow->off_y;
        if (x < 0 ) {
            x = 0;
        }
        if (x + statusWindow->statusW > statusWindow->rootW) {
            x = statusWindow->rootW - statusWindow->statusW;
        }
        if (y + statusWindow->statusH > statusWindow->rootH) {
            y = statusWindow->rootH - statusWindow->statusH;
        }
        XMoveWindow(dpy, statusWindow->w, x, y);
    }
    statusWindow->on = True;
    XMapWindow(dpy, statusWindow->w);
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
    XFillRectangle(dpy, win, bggc, 0, 0, width, height);
    /* draw border */
    XDrawLine(dpy, win, fggc, 0, 0, width, 0);
    XDrawLine(dpy, win, fggc, 0, height-1, width-1, height-1);
    XDrawLine(dpy, win, fggc, 0, 0, 0, height-1);
    XDrawLine(dpy, win, fggc, width-1, 0, width-1, height-1);

    XDrawLine(dpy, win, lightgc, 1, 1, width-bwidth, 1);
    XDrawLine(dpy, win, lightgc, 1, 1, 1, height-2);
    XDrawLine(dpy, win, lightgc, 1, height-2, width-bwidth, height-2);
    XDrawLine(dpy, win, lightgc, width-bwidth-1, 1, width-bwidth-1, height-2);

    XDrawLine(dpy, win, dimgc, 2, 2, 2, height-3);
    XDrawLine(dpy, win, dimgc, 2, height-3, width-bwidth-1, height-3);
    XDrawLine(dpy, win, dimgc, 2, 2, width-bwidth-2, 2);
    XDrawLine(dpy, win, dimgc, width-bwidth, 2, width-bwidth, height-3);
    if (statusWindow->fontset) {
        XmbDrawString(dpy, win, statusWindow->fontset, fggc,
                      bwidth + 2, height - bwidth - 4,
                      statusWindow->status,
                      strlen(statusWindow->status));
    } else {
        /*too bad we failed to create a fontset for this locale*/
        XDrawString(dpy, win, fggc, bwidth + 2, height - bwidth - 4,
                    "[InputMethod ON]", strlen("[InputMethod ON]"));
    }
}

static void adjustStatusWindow(Window shell) {
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    StatusWindow *statusWindow;

    if (NULL == currentX11InputMethodInstance
        || !isX11InputMethodGRefInList(currentX11InputMethodInstance)
        || NULL == (pX11IMData = getX11InputMethodData(env,currentX11InputMethodInstance))
        || NULL == (statusWindow = pX11IMData->statusWindow)
        || !statusWindow->on)
    {
        return;
    }

    {
        XWindowAttributes xwa;
        int x, y;
        Window child;
        XGetWindowAttributes(dpy, shell, &xwa);
        XTranslateCoordinates(dpy,
                              shell, xwa.root,
                              xwa.x, xwa.y,
                              &x, &y,
                              &child);
        if (statusWindow->x != x
            || statusWindow->y != y
            || statusWindow->height != xwa.height){
          statusWindow->x = x;
          statusWindow->y = y;
          statusWindow->height = xwa.height;

          x = statusWindow->x - statusWindow->off_x;
          y = statusWindow->y + statusWindow->height - statusWindow->off_y;
          if (x < 0 ) {
              x = 0;
          }
          if (x + statusWindow->statusW > statusWindow->rootW){
              x = statusWindow->rootW - statusWindow->statusW;
          }
          if (y + statusWindow->statusH > statusWindow->rootH){
              y = statusWindow->rootH - statusWindow->statusH;
          }
          XMoveWindow(dpy, statusWindow->w, x, y);
        }
    }
}
#endif  /* __linux__ */

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
             active_styles = 0,
             passive_styles = 0,
             no_styles = 0;
    XIMCallback *callbacks;
    unsigned short i;
    XIMStyles *im_styles;
    char *ret = NULL;

    if (X11im == NULL) {
        return False;
    }
    if (!w) {
        return False;
    }

    ret = XGetIMValues(X11im, XNQueryInputStyle, &im_styles, NULL);

    if (ret != NULL) {
        jio_fprintf(stderr,"XGetIMValues: %s\n",ret);
        return FALSE ;
    }

    on_the_spot_styles |= XIMStatusNothing;

#if defined(__linux__)
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
#endif /* __linux__ */

    for (i = 0; i < im_styles->count_styles; i++) {
        active_styles |= im_styles->supported_styles[i] & on_the_spot_styles;
        passive_styles |= im_styles->supported_styles[i] & ROOT_WINDOW_STYLES;
        no_styles |= im_styles->supported_styles[i] & NO_STYLES;
    }

    XFree(im_styles);

    if (active_styles != on_the_spot_styles) {
        if (passive_styles == ROOT_WINDOW_STYLES)
            active_styles = passive_styles;
        else {
            if (no_styles == NO_STYLES)
                active_styles = passive_styles = NO_STYLES;
            else
                active_styles = passive_styles = 0;
        }
    } else {
        if (passive_styles != ROOT_WINDOW_STYLES) {
            if (no_styles == NO_STYLES)
                active_styles = passive_styles = NO_STYLES;
            else
                active_styles = passive_styles = 0;
        }
    }

    if (active_styles == on_the_spot_styles) {
        pX11IMData->ic_passive = XCreateIC(X11im,
                                   XNClientWindow, w,
                                   XNFocusWindow, w,
                                   XNInputStyle, passive_styles,
                                   NULL);

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
#if defined(__linux__)
        /*always try XIMStatusCallbacks for active client...*/
        {
            status = (XVaNestedList)XVaCreateNestedList(0,
                        XNStatusStartCallback, &callbacks[StatusStartIndex],
                        XNStatusDoneCallback,  &callbacks[StatusDoneIndex],
                        XNStatusDrawCallback, &callbacks[StatusDrawIndex],
                        NULL);

            if (status == NULL)
                goto err;
            pX11IMData->statusWindow = createStatusWindow(w);
            pX11IMData->ic_active = XCreateIC(X11im,
                                              XNClientWindow, w,
                                              XNFocusWindow, w,
                                              XNInputStyle, active_styles,
                                              XNPreeditAttributes, preedit,
                                              XNStatusAttributes, status,
                                              NULL);
            XFree((void *)status);
            XFree((void *)preedit);
        }
#else /* !__linux__ */
        pX11IMData->ic_active = XCreateIC(X11im,
                                          XNClientWindow, w,
                                          XNFocusWindow, w,
                                          XNInputStyle, active_styles,
                                          XNPreeditAttributes, preedit,
                                          NULL);
        XFree((void *)preedit);
#endif /* __linux__ */
    } else {
        pX11IMData->ic_active = XCreateIC(X11im,
                                          XNClientWindow, w,
                                          XNFocusWindow, w,
                                          XNInputStyle, active_styles,
                                          NULL);
        pX11IMData->ic_passive = pX11IMData->ic_active;
    }

    if (pX11IMData->ic_active == (XIC)0
        || pX11IMData->ic_passive == (XIC)0) {
        return False;
    }

    /*
     * Use commit string call back if possible.
     * This will ensure the correct order of preedit text and commit text
     */
    {
        XIMCallback cb;
        cb.client_data = (XPointer) pX11IMData->x11inputmethod;
        cb.callback = (XIMProc) CommitStringCallback;
        XSetICValues (pX11IMData->ic_active, XNCommitStringCallback, &cb, NULL);
        if (pX11IMData->ic_active != pX11IMData->ic_passive) {
            XSetICValues (pX11IMData->ic_passive, XNCommitStringCallback, &cb, NULL);
        }
    }

    // The code set the IC mode that the preedit state is not initialied
    // at XmbResetIC.  This attribute can be set at XCreateIC.  I separately
    // set the attribute to avoid the failure of XCreateIC at some platform
    // which does not support the attribute.
    if (pX11IMData->ic_active != 0)
        XSetICValues(pX11IMData->ic_active,
                     XNResetState, XIMInitialState,
                     NULL);
    if (pX11IMData->ic_passive != 0
            && pX11IMData->ic_active != pX11IMData->ic_passive)
        XSetICValues(pX11IMData->ic_passive,
                     XNResetState, XIMInitialState,
                     NULL);

    /* Add the global reference object to X11InputMethod to the list. */
    addToX11InputMethodGRefList(pX11IMData->x11inputmethod);

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
    /*ARGSUSED*/
    /* printf("Native: PreeditStartCallback\n"); */
    return -1;
}

static void
PreeditDoneCallback(XIC ic, XPointer client_data, XPointer call_data)
{
    /*ARGSUSED*/
    /* printf("Native: PreeditDoneCallback\n"); */
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
    if (!isX11InputMethodGRefInList((jobject)client_data)) {
        if ((jobject)client_data == currentX11InputMethodInstance) {
            currentX11InputMethodInstance = NULL;
        }
        goto finally;
    }
    if ((pX11IMData = getX11InputMethodData(env, (jobject)client_data)) == NULL) {
        goto finally;
    }

    if ((text = pre_draw->text) != NULL) {
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
finally:
    AWT_UNLOCK();
    return;
}

static void
PreeditCaretCallback(XIC ic, XPointer client_data,
                     XIMPreeditCaretCallbackStruct *pre_caret)
{
    /*ARGSUSED*/
    /* printf("Native: PreeditCaretCallback\n"); */
}

#if defined(__linux__)
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
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    StatusWindow *statusWindow;

    AWT_LOCK();

    if (!isX11InputMethodGRefInList((jobject)client_data)) {
        if ((jobject)client_data == currentX11InputMethodInstance) {
            currentX11InputMethodInstance = NULL;
        }
        goto finally;
    }

    if (NULL == (pX11IMData = getX11InputMethodData(env, (jobject)client_data))
        || NULL == (statusWindow = pX11IMData->statusWindow)){
        goto finally;
    }
    currentX11InputMethodInstance = (jobject)client_data;

    onoffStatusWindow(pX11IMData, 0, False);

 finally:
    AWT_UNLOCK();
}

static void
StatusDrawCallback(XIC ic, XPointer client_data,
                     XIMStatusDrawCallbackStruct *status_draw)
{
    /*ARGSUSED*/
    /*printf("StatusDrawCallback:\n"); */
    JNIEnv *env = GetJNIEnv();
    X11InputMethodData *pX11IMData = NULL;
    StatusWindow *statusWindow;

    AWT_LOCK();

    if (!isX11InputMethodGRefInList((jobject)client_data)) {
        if ((jobject)client_data == currentX11InputMethodInstance) {
            currentX11InputMethodInstance = NULL;
        }
        goto finally;
    }

    if (NULL == (pX11IMData = getX11InputMethodData(env, (jobject)client_data))
        || NULL == (statusWindow = pX11IMData->statusWindow)){
        goto finally;
    }
    currentX11InputMethodInstance = (jobject)client_data;

    if (status_draw->type == XIMTextType) {
        XIMText *text = (status_draw->data).text;
        if (text != NULL) {
            if (text->string.multi_byte != NULL) {
                strncpy(statusWindow->status, text->string.multi_byte, MAX_STATUS_LEN);
                statusWindow->status[MAX_STATUS_LEN - 1] = '\0';
            } else {
                char *mbstr = wcstombsdmp(text->string.wide_char, text->length);
                if (mbstr == NULL) {
                    goto finally;
                }
                strncpy(statusWindow->status, mbstr, MAX_STATUS_LEN);
                statusWindow->status[MAX_STATUS_LEN - 1] = '\0';
                free(mbstr);
            }
            statusWindow->on = True;
            onoffStatusWindow(pX11IMData, statusWindow->parent, True);
            paintStatusWindow(statusWindow);
        } else {
            statusWindow->on = False;
            /*just turnoff the status window
            paintStatusWindow(statusWindow);
            */
            onoffStatusWindow(pX11IMData, 0, False);
        }
    }

 finally:
    AWT_UNLOCK();
}
#endif /* __linux__ */

static void CommitStringCallback(XIC ic, XPointer client_data, XPointer call_data) {
    JNIEnv *env = GetJNIEnv();
    XIMText * text = (XIMText *)call_data;
    X11InputMethodData *pX11IMData = NULL;
    jstring javastr;

    AWT_LOCK();

    if (!isX11InputMethodGRefInList((jobject)client_data)) {
        if ((jobject)client_data == currentX11InputMethodInstance) {
            currentX11InputMethodInstance = NULL;
        }
        goto finally;
    }

    if ((pX11IMData = getX11InputMethodData(env, (jobject)client_data)) == NULL) {
        goto finally;
    }
    currentX11InputMethodInstance = (jobject)client_data;

    if (text->encoding_is_wchar == False) {
        javastr = JNU_NewStringPlatform(env, (const char *)text->string.multi_byte);
    } else {
        char *mbstr = wcstombsdmp(text->string.wide_char, text->length);
        if (mbstr == NULL) {
            goto finally;
        }
        javastr = JNU_NewStringPlatform(env, (const char *)mbstr);
        free(mbstr);
    }

    if (javastr != NULL) {
        JNU_CallMethodByName(env, NULL,
                                 pX11IMData->x11inputmethod,
                                 "dispatchCommittedText",
                                 "(Ljava/lang/String;J)V",
                                 javastr,
                                 awt_util_nowMillisUTC());
    }
 finally:
    AWT_UNLOCK();
}

static void OpenXIMCallback(Display *display, XPointer client_data, XPointer call_data) {
    XIMCallback ximCallback;

    X11im = XOpenIM(display, NULL, NULL, NULL);
    if (X11im == NULL) {
        return;
    }

    ximCallback.callback = (XIMProc)DestroyXIMCallback;
    ximCallback.client_data = NULL;
    XSetIMValues(X11im, XNDestroyCallback, &ximCallback, NULL);
}

static void DestroyXIMCallback(XIM im, XPointer client_data, XPointer call_data) {
    /* mark that XIM server was destroyed */
    X11im = NULL;
    JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);

    AWT_LOCK();
    /* free the old pX11IMData and set it to null. this also avoids crashing
     * the jvm if the XIM server reappears */
    while (x11InputMethodGRefListHead != NULL) {
        if (getX11InputMethodData(env,
                x11InputMethodGRefListHead->inputMethodGRef) == NULL) {
            /* Clear possible exceptions
             */
            if ((*env)->ExceptionOccurred(env)) {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
        }
    }
    AWT_UNLOCK();
}

JNIEXPORT jboolean JNICALL
Java_sun_awt_X11_XInputMethod_openXIMNative(JNIEnv *env,
                                            jobject this,
                                            jlong display)
{
    Bool registered;

    AWT_LOCK();

    dpy = (Display *)jlong_to_ptr(display);

/* Use IMInstantiate call back only on Linux, as there is a bug in Solaris
   (4768335)
*/
#if defined(__linux__)
    registered = XRegisterIMInstantiateCallback(dpy, NULL, NULL,
                     NULL, (XIDProc)OpenXIMCallback, NULL);
    if (!registered) {
        /* directly call openXIM callback */
#endif
        OpenXIMCallback(dpy, NULL, NULL);
#if defined(__linux__)
    }
#endif

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
#if defined(__linux__)
    pX11IMData->statusWindow = NULL;
#endif /* __linux__ */

    pX11IMData->lookup_buf = 0;
    pX11IMData->lookup_buf_len = 0;

    if (createXIC(env, pX11IMData, (Window)window) == False) {
        destroyX11InputMethodData((JNIEnv *) NULL, pX11IMData);
        pX11IMData = (X11InputMethodData *) NULL;
        if ((*env)->ExceptionCheck(env)) {
            goto finally;
        }
    }

    setX11InputMethodData(env, this, pX11IMData);

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
        pX11IMData->current_ic = active ?
                        pX11IMData->ic_active : pX11IMData->ic_passive;
        /*
         * On Solaris2.6, setXICWindowFocus() has to be invoked
         * before setting focus.
         */
        setXICWindowFocus(pX11IMData->current_ic, w);
        setXICFocus(pX11IMData->current_ic, req);
        currentX11InputMethodInstance = pX11IMData->x11inputmethod;
        currentFocusWindow =  w;
#if defined(__linux__)
        if (active && pX11IMData->statusWindow && pX11IMData->statusWindow->on)
            onoffStatusWindow(pX11IMData, w, True);
#endif
    } else {
        currentX11InputMethodInstance = NULL;
        currentFocusWindow = 0;
#if defined(__linux__)
        onoffStatusWindow(pX11IMData, 0, False);
        if (pX11IMData->current_ic != NULL)
#endif
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
#if defined(__linux__)
    X11InputMethodData *pX11IMData;
    StatusWindow *statusWindow;

    AWT_LOCK();

    if (NULL == currentX11InputMethodInstance
        || !isX11InputMethodGRefInList(currentX11InputMethodInstance)
        || NULL == (pX11IMData = getX11InputMethodData(env,currentX11InputMethodInstance))
        || NULL == (statusWindow = pX11IMData->statusWindow)
        || !statusWindow->on ){
        AWT_UNLOCK();
        return;
    }
    onoffStatusWindow(pX11IMData, 0, False);

    AWT_UNLOCK();
#endif
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

    if (pX11IMData->current_ic)
        xText = XmbResetIC(pX11IMData->current_ic);
    else {
        /*
         * If there is no reference to the current XIC, try to reset both XICs.
         */
        xText = XmbResetIC(pX11IMData->ic_active);
        /*it may also means that the real client component does
          not have focus -- has been deactivated... its xic should
          not have the focus, bug#4284651 showes reset XIC for htt
          may bring the focus back, so de-focus it again.
        */
        setXICFocus(pX11IMData->ic_active, FALSE);
        if (pX11IMData->ic_active != pX11IMData->ic_passive) {
            char *tmpText = XmbResetIC(pX11IMData->ic_passive);
            setXICFocus(pX11IMData->ic_passive, FALSE);
            if (xText == (char *)NULL && tmpText)
                xText = tmpText;
        }

    }
    if (xText != NULL) {
        jText = JNU_NewStringPlatform(env, (const char *)xText);
        XFree((void *)xText);
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
    XVaNestedList   pr_atrb;
#if defined(__linux__)
    Boolean calledXSetICFocus = False;
#endif

    AWT_LOCK();
    pX11IMData = getX11InputMethodData(env, this);

    if ((pX11IMData == NULL) || (pX11IMData->current_ic == NULL)) {
        AWT_UNLOCK();
        return JNI_FALSE;
    }

#if defined(__linux__)
    if (NULL != pX11IMData->statusWindow) {
        Window focus = 0;
        int revert_to;
#if defined(_LP64) && !defined(_LITTLE_ENDIAN)
        // The Window value which is used for XGetICValues must be 32bit on BigEndian XOrg's xlib
        unsigned int w = 0;
#else
        Window w = 0;
#endif
        XGetInputFocus(awt_display, &focus, &revert_to);
        XGetICValues(pX11IMData->current_ic, XNFocusWindow, &w, NULL);
        if (RevertToPointerRoot == revert_to
                && pX11IMData->ic_active != pX11IMData->ic_passive) {
            if (pX11IMData->current_ic == pX11IMData->ic_active) {
                if (getParentWindow(focus) == getParentWindow(w)) {
                    XUnsetICFocus(pX11IMData->ic_active);
                    calledXSetICFocus = True;
                }
            }
        }
    }
#endif
    pr_atrb = XVaCreateNestedList(0,
                  XNPreeditState, (enable ? XIMPreeditEnable : XIMPreeditDisable),
                  NULL);
    ret = XSetICValues(pX11IMData->current_ic, XNPreeditAttributes, pr_atrb, NULL);
    XFree((void *)pr_atrb);
#if defined(__linux__)
    if (calledXSetICFocus) {
        XSetICFocus(pX11IMData->ic_active);
    }
#endif
    AWT_UNLOCK();

    if ((ret != 0)
            && ((strcmp(ret, XNPreeditAttributes) == 0)
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
#if defined(__linux__) && defined(_LP64) && !defined(_LITTLE_ENDIAN)
    // XIMPreeditState value which is used for XGetICValues must be 32bit on BigEndian XOrg's xlib
    unsigned int state = XIMPreeditUnKnown;
#else
    XIMPreeditState state = XIMPreeditUnKnown;
#endif

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

    if ((ret != 0)
            && ((strcmp(ret, XNPreeditAttributes) == 0)
            || (strcmp(ret, XNPreeditState) == 0))) {
        JNU_ThrowByName(env, "java/lang/UnsupportedOperationException", "");
        return JNI_FALSE;
    }

    return (jboolean)(state == XIMPreeditEnable);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XInputMethod_adjustStatusWindow
  (JNIEnv *env, jobject this, jlong window)
{
#if defined(__linux__)
    AWT_LOCK();
    adjustStatusWindow(window);
    AWT_UNLOCK();
#endif
}

#if defined(__linux__)
static Window getParentWindow(Window w)
{
    Window root=None, parent=None, *ignore_children=NULL;
    unsigned int ignore_uint=0;
    Status status = 0;

    if (w == None)
        return None;
    status = XQueryTree(dpy, w, &root, &parent, &ignore_children, &ignore_uint);
    XFree(ignore_children);
    if (status == 0)
        return None;
    return parent;
}
#endif
