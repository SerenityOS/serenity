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
/** ------------------------------------------------------------------------
        This file contains routines for manipulating generic lists.
        Lists are implemented with a "harness".  In other words, each
        node in the list consists of two pointers, one to the data item
        and one to the next node in the list.  The head of the list is
        the same struct as each node, but the "item" ptr is used to point
        to the current member of the list (used by the first_in_list and
        next_in_list functions).

 This file is available under and governed by the GNU General Public
 License version 2 only, as published by the Free Software Foundation.
 However, the following notice accompanied the original version of this
 file:

Copyright 1994 Hewlett-Packard Co.
Copyright 1996, 1998  The Open Group

Permission to use, copy, modify, distribute, and sell this software and its
documentation for any purpose is hereby granted without fee, provided that
the above copyright notice appear in all copies and that both that
copyright notice and this permission notice appear in supporting
documentation.

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE OPEN GROUP BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of The Open Group shall
not be used in advertising or otherwise to promote the sale, use or
other dealings in this Software without prior written authorization
from The Open Group.

    ------------------------------------------------------------------------ **/

/******************************************************************************
 *
 * This file contains various typedef's, macros and procedure declarations for
 * a set of example utility procedures contained in the file "wsutils.c".
 *
 ******************************************************************************/

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

typedef unsigned long Pixel;

/* This is the actual structure returned by the X server describing the
 * SERVER_OVERLAY_VISUAL property.
 */
typedef struct
{
  VisualID      visualID;               /* The VisualID of the overlay visual */
  int           transparentType;        /* Can be None, TransparentPixel or
                                         * TransparentMask */
  Pixel         value;                  /* Pixel value */
  int           layer;                  /* Overlay planes will always be in
                                         * layer 1 */
} OverlayVisualPropertyRec;


/* This is structure also describes the SERVER_OVERLAY_VISUAL property, but
 * should be more useful than the one actually returned by the X server
 * because it actually points to the visual's XVisualInfo struct rather than
 * refering to the visual's ID.
 */
typedef struct
{
  XVisualInfo   *pOverlayVisualInfo;    /* Pointer to the XVisualInfo struct */
  int           transparentType;        /* Can be None, TransparentPixel or
                                         * TransparentMask */
  Pixel         value;                  /* Pixel value */
  int           layer;                  /* Overlay planes will always be in
                                         * layer 1 */
} OverlayInfo;


/* These macros are the values of the "transparentType" above: */
#ifndef None
#define None 0
#endif
#ifndef TransparentPixel
#define TransparentPixel        1
#endif


/* These macros define how flexible a program is when it requests a window's
 * creation with either the CreateImagePlanesWindow() or
 * CreateOverlayPlanesWindow():
 */
#ifndef NOT_FLEXIBLE
#define NOT_FLEXIBLE            0
#define FLEXIBLE                1
#endif


/* These macros define the values of the "sbCmapHint" parameter of the
 * CreateImagePlanesWindow():
 */
#ifndef SB_CMAP_TYPE_NORMAL
#define SB_CMAP_TYPE_NORMAL     1
#endif

#ifndef SB_CMAP_TYPE_MONOTONIC
#define SB_CMAP_TYPE_MONOTONIC  2
#endif

#ifndef SB_CMAP_TYPE_FULL
#define SB_CMAP_TYPE_FULL       4
#endif


/******************************************************************************
 *
 * GetXVisualInfo()
 *
 * This routine takes an X11 Display, screen number, and returns whether the
 * screen supports transparent overlays and three arrays:
 *
 *      1) All of the XVisualInfo struct's for the screen.
 *      2) All of the OverlayInfo struct's for the screen.
 *      3) An array of pointers to the screen's image plane XVisualInfo
 *         structs.
 *
 * The code below obtains the array of all the screen's visuals, and obtains
 * the array of all the screen's overlay visual information.  It then processes
 * the array of the screen's visuals, determining whether the visual is an
 * overlay or image visual.
 *
 * If the routine sucessfully obtained the visual information, it returns zero.
 * If the routine didn't obtain the visual information, it returns non-zero.
 *
 ******************************************************************************/

extern int GetXVisualInfo(
    Display     *display,               /* Which X server (aka "display"). */
    int         screen,                 /* Which screen of the "display". */
    int         *transparentOverlays,   /* Non-zero if there's at least one
                                         * overlay visual and if at least one
                                         * of those supports a transparent
                                         * pixel. */
    int         *numVisuals,            /* Number of XVisualInfo struct's
                                         * pointed to to by pVisuals. */
    XVisualInfo **pVisuals,             /* All of the device's visuals. */
    int         *numOverlayVisuals,     /* Number of OverlayInfo's pointed
                                         * to by pOverlayVisuals.  If this
                                         * number is zero, the device does
                                         * not have overlay planes. */
    OverlayInfo **pOverlayVisuals,      /* The device's overlay plane visual
                                         * information. */
    int         *numImageVisuals,       /* Number of XVisualInfo's pointed
                                         * to by pImageVisuals. */
    XVisualInfo ***pImageVisuals        /* The device's image visuals. */
                    );


/******************************************************************************
 *
 * FreeXVisualInfo()
 *
 * This routine frees the data that was allocated by GetXVisualInfo().
 *
 ******************************************************************************/

extern void FreeXVisualInfo(
    XVisualInfo *pVisuals,
    OverlayInfo *pOverlayVisuals,
    XVisualInfo **pImageVisuals
                     );


/******************************************************************************
 *
 * FindImagePlanesVisual()
 *
 * This routine attempts to find a visual to use to create an image planes
 * window based upon the information passed in.
 *
 * The "Hint" values give guides to the routine as to what the program wants.
 * The "depthFlexibility" value tells the routine how much the program wants
 * the actual "depthHint" specified.  If the program can't live with the
 * screen's image planes visuals, the routine returns non-zero, and the
 * "depthObtained" and "pImageVisualToUse" return parameters are NOT valid.
 * Otherwise, the "depthObtained" and "pImageVisualToUse" return parameters
 * are valid and the routine returns zero.
 *
 * NOTE: This is just an example of what can be done.  It may or may not be
 * useful for any specific application.
 *
 ******************************************************************************/

extern int FindImagePlanesVisual(
    Display     *display,               /* Which X server (aka "display"). */
    int         screen,                 /* Which screen of the "display". */
    int         numImageVisuals,        /* Number of XVisualInfo's pointed
                                         * to by pImageVisuals. */
    XVisualInfo **pImageVisuals,        /* The device's image visuals. */
    int         sbCmapHint,             /* What Starbase cmap modes will be
                                         * used with the visual.  NOTE: This
                                         * is a mask of the possible values. */
    int         depthHint,              /* Desired depth. */
    int         depthFlexibility,       /* How much the actual value in
                                         * "depthHint" is desired. */
    Visual      **pImageVisualToUse,    /* The screen's image visual to use. */
    int         *depthObtained          /* Actual depth of the visual. */
                                     );


/******************************************************************************
 *
 * FindOverlayPlanesVisual()
 *
 * This routine attempts to find a visual to use to create an overlay planes
 * window based upon the information passed in.
 *
 * While the CreateImagePlanesWindow() routine took a sbCmapHint, this
 * routine doesn't.  Starbase's CMAP_FULL shouldn't be used in overlay planes
 * windows.  This is partially because this functionality is better suited in
 * the image planes where there are generally more planes, and partially
 * because the overlay planes generally have PseudoColor visuals with one
 * color being transparent (the transparent normally being the "white" color
 * for CMAP_FULL).
 *
 * The "depthHint" values give guides to the routine as to what depth the
 * program wants the window to be.  The "depthFlexibility" value tells the
 * routine how much the program wants the actual "depthHint" specified.  If
 * the program can't live with the screen's overlay planes visuals, the
 * routine returns non-zero, and the "depthObtained" and "pOverlayVisualToUse"
 * return parameters are NOT valid.  Otherwise, the "depthObtained" and
 * "pOverlayVisualToUse" return parameters are valid and the routine returns
 * zero.
 *
 * NOTE: This is just an example of what can be done.  It may or may not be
 * useful for any specific application.
 *
 ******************************************************************************/

extern int FindOverlayPlanesVisual(
    Display     *display,               /* Which X server (aka "display"). */
    int         screen,                 /* Which screen of the "display". */
    int         numOverlayVisuals,      /* Number of OverlayInfo's pointed
                                         * to by pOverlayVisuals. */
    OverlayInfo *pOverlayVisuals,       /* The device's overlay plane visual
                                         * information. */
    int         depthHint,              /* Desired depth. */
    int         depthFlexibility,       /* How much the actual value in
                                         * "depthHint" is desired. */
    int         transparentBackground,  /* Non-zero if the visual must have
                                         * a transparent color. */
    Visual      **pOverlayVisualToUse,  /* The screen's overlay visual to
                                         * use. */
    int         *depthObtained,         /* Actual depth of the visual. */
    int         *transparentColor       /* The transparent color the program
                                         * can use with the visual. */
                                );


/******************************************************************************
 *
 * CreateImagePlanesWindow()
 *
 * This routine creates an image planes window, potentially creates a colormap
 * for the window to use, and sets the window's standard properties, based
 * upon the information passed in to the routine.  While "created," the window
 * has not been mapped.
 *
 * If the routine suceeds, it returns zero and the return parameters
 * "imageWindow", "imageColormap" and "mustFreeImageColormap" are valid.
 * Otherwise, the routine returns non-zero and the return parameters are
 * NOT valid.
 *
 * NOTE: This is just an example of what can be done.  It may or may not be
 * useful for any specific application.
 *
 ******************************************************************************/

extern int CreateImagePlanesWindow(
    Display     *display,               /* Which X server (aka "display"). */
    int         screen,                 /* Which screen of the "display". */
    Window      parentWindow,           /* Window ID of the parent window for
                                         * the created window. */
    int         windowX,                /* Desired X coord. of the window. */
    int         windowY,                /* Desired Y coord of the window. */
    int         windowWidth,            /* Desired width of the window. */
    int         windowHeight,           /* Desired height of the window. */
    int         windowDepth,            /* Desired depth of the window. */
    Visual      *pImageVisualToUse,     /* The window's image planes visual. */
    int         argc,                   /* Program's argc parameter. */
    char        *argv[],                /* Program's argv parameter. */
    char        *windowName,            /* Name to put on window's border. */
    char        *iconName,              /* Name to put on window's icon. */
    Window      *imageWindow,           /* Window ID of the created window. */
    Colormap    *imageColormap,         /* The window's colormap. */
    int         *mustFreeImageColormap  /* Non-zero if the program must call
                                         * XFreeColormap() for imageColormap. */
                                );


/******************************************************************************
 *
 * CreateOverlayPlanesWindow()
 *
 * This routine creates an overlay planes window, potentially creates a colormap
 * for the window to use, and sets the window's standard properties, based
 * upon the information passed in to the routine.  While "created," the window
 * has not been mapped.
 *
 * If the routine suceeds, it returns zero and the return parameters
 * "overlayWindow", "overlayColormap" and "mustFreeOverlayColormap" are valid.
 * Otherwise, the routine returns non-zero and the return parameters are
 * NOT valid.
 *
 * NOTE: This is just an example of what can be done.  It may or may not be
 * useful for any specific application.
 *
 ******************************************************************************/

int CreateOverlayPlanesWindow(
    Display     *display,               /* Which X server (aka "display"). */
    int         screen,                 /* Which screen of the "display". */
    Window      parentWindow,           /* Window ID of the parent window for
                                         * the created window. */
    int         windowX,                /* Desired X coord. of the window. */
    int         windowY,                /* Desired Y coord of the window. */
    int         windowWidth,            /* Desired width of the window. */
    int         windowHeight,           /* Desired height of the window. */
    int         windowDepth,            /* Desired depth of the window. */
    Visual      *pOverlayVisualToUse,   /* The window's overlay planes visual.*/
    int         argc,                   /* Program's argc parameter. */
    char        *argv[],                /* Program's argv parameter. */
    char        *windowName,            /* Name to put on window's border. */
    char        *iconName,              /* Name to put on window's icon. */
    int         transparentBackground,  /* Non-zero if the window's background
                                         * should be a transparent color. */
    int         *transparentColor,      /* The transparent color to use as the
                                         * window's background. */
    Window      *overlayWindow,         /* Window ID of the created window. */
    Colormap    *overlayColormap,       /* The window's colormap. */
    int         *mustFreeOverlayColormap/* Non-zero if the program must call
                                          * XFreeColormap() for
                                          * overlayColormap. */
                                );
