/*
 * Copyright (c) 2003, 2008, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

public final class XUtilConstants {

    private XUtilConstants(){}

    /*
     * Bitmask returned by XParseGeometry().  Each bit tells if the corresponding
     * value (x, y, width, height) was found in the parsed string.
     */
    public static final int NoValue = 0x0000 ;
    public static final int XValue = 0x0001 ;
    public static final int YValue = 0x0002 ;
    public static final int WidthValue = 0x0004 ;
    public static final int HeightValue = 0x0008 ;
    public static final int AllValues = 0x000F ;
    public static final int XNegative = 0x0010 ;
    public static final int YNegative = 0x0020 ;

    /*
     * The next block of definitions are for window manager properties that
     * clients and applications use for communication.
     */

    /* flags argument in size hints */
    public static final long USPosition = 1L << 0; /* user specified x, y */
    public static final long USSize = 1L << 1; /* user specified width, height */

    public static final long PPosition = 1L << 2; /* program specified position */
    public static final long PSize = 1L << 3; /* program specified size */
    public static final long PMinSize = 1L << 4; /* program specified minimum size */
    public static final long PMaxSize = 1L << 5; /* program specified maximum size */
    public static final long PResizeInc = 1L << 6; /* program specified resize increments */
    public static final long PAspect = 1L << 7; /* program specified min and max aspect ratios */
    public static final long PBaseSize = 1L << 8; /* program specified base for incrementing */
    public static final long PWinGravity = 1L << 9; /* program specified window gravity */

    /* obsolete */
    public static final long PAllHints = (PPosition|PSize|PMinSize|PMaxSize|PResizeInc|PAspect) ;

    /* definition for flags of XWMHints */

    public static final long InputHint = 1L << 0;
    public static final long StateHint = 1L << 1;
    public static final long IconPixmapHint = 1L << 2;
    public static final long IconWindowHint = 1L << 3;
    public static final long IconPositionHint = 1L << 4;
    public static final long IconMaskHint = 1L << 5;
    public static final long WindowGroupHint = 1L << 6;
    public static final long AllHints = (InputHint|StateHint|IconPixmapHint|IconWindowHint|
        IconPositionHint|IconMaskHint|WindowGroupHint);
    public static final long XUrgencyHint = 1L << 8;

    /* definitions for initial window state */
    public static final int WithdrawnState = 0 ; /* for windows that are not mapped */
    public static final int NormalState = 1 ; /* most applications want to start this way */
    public static final int IconicState = 3 ; /* application wants to start as an icon */

    /*
     * Obsolete states no longer defined by ICCCM
     */
    public static final int DontCareState = 0 ; /* don't know or care */
    public static final int ZoomState = 2 ; /* application wants to start zoomed */
    /* application believes it is seldom used; some wm's may put it on inactive menu */
    public static final int InactiveState = 4 ;

    public static final int XNoMemory = -1 ;
    public static final int XLocaleNotSupported = -2 ;
    public static final int XConverterNotFound = -3 ;

    /* Return values from XRectInRegion() */
    public static final int RectangleOut = 0 ;
    public static final int RectangleIn = 1 ;
    public static final int RectanglePart = 2 ;

    /*
     * Information used by the visual utility routines to find desired visual
     * type from the many visuals a display may support.
     */
    public static final int VisualNoMask = 0x0 ;
    public static final int VisualIDMask = 0x1 ;
    public static final int VisualScreenMask = 0x2 ;
    public static final int VisualDepthMask = 0x4 ;
    public static final int VisualClassMask = 0x8 ;
    public static final int VisualRedMaskMask = 0x10 ;
    public static final int VisualGreenMaskMask = 0x20 ;
    public static final int VisualBlueMaskMask = 0x40 ;
    public static final int VisualColormapSizeMask = 0x80 ;
    public static final int VisualBitsPerRGBMask = 0x100 ;
    public static final int VisualAllMask = 0x1FF ;

    /*
     * return codes for XReadBitmapFile and XWriteBitmapFile
     */
    public static final int BitmapSuccess = 0 ;
    public static final int BitmapOpenFailed = 1 ;
    public static final int BitmapFileInvalid = 2 ;
    public static final int BitmapNoMemory = 3 ;

    /****************************************************************
     *
     * Context Management
     *
     ****************************************************************
     */
    /* Associative lookup table return codes */
    public static final int XCSUCCESS = 0 ; /* No error. */
    public static final int XCNOMEM = 1 ; /* Out of memory */
    public static final int XCNOENT = 2 ; /* No entry in table */

    // typedef int XContext;
}
