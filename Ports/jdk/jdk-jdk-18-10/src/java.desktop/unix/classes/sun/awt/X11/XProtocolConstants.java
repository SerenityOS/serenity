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

public final class XProtocolConstants {

    private XProtocolConstants(){}

    /* Reply codes */
    public static final int X_Reply = 1 ; /* Normal reply */
    public static final int X_Error = 0 ; /* Error */

    /* Request codes */
    public static final int X_CreateWindow = 1 ;
    public static final int X_ChangeWindowAttributes = 2 ;
    public static final int X_GetWindowAttributes = 3 ;
    public static final int X_DestroyWindow = 4 ;
    public static final int X_DestroySubwindows = 5 ;
    public static final int X_ChangeSaveSet = 6 ;
    public static final int X_ReparentWindow = 7 ;
    public static final int X_MapWindow = 8 ;
    public static final int X_MapSubwindows = 9 ;
    public static final int X_UnmapWindow = 10 ;
    public static final int X_UnmapSubwindows = 11 ;
    public static final int X_ConfigureWindow = 12 ;
    public static final int X_CirculateWindow = 13 ;
    public static final int X_GetGeometry = 14 ;
    public static final int X_QueryTree = 15 ;
    public static final int X_InternAtom = 16 ;
    public static final int X_GetAtomName = 17 ;
    public static final int X_ChangeProperty = 18 ;
    public static final int X_DeleteProperty = 19 ;
    public static final int X_GetProperty = 20 ;
    public static final int X_ListProperties = 21 ;
    public static final int X_SetSelectionOwner = 22 ;
    public static final int X_GetSelectionOwner = 23 ;
    public static final int X_ConvertSelection = 24 ;
    public static final int X_SendEvent = 25 ;
    public static final int X_GrabPointer = 26 ;
    public static final int X_UngrabPointer = 27 ;
    public static final int X_GrabButton = 28 ;
    public static final int X_UngrabButton = 29 ;
    public static final int X_ChangeActivePointerGrab = 30 ;
    public static final int X_GrabKeyboard = 31 ;
    public static final int X_UngrabKeyboard = 32 ;
    public static final int X_GrabKey = 33 ;
    public static final int X_UngrabKey = 34 ;
    public static final int X_AllowEvents = 35 ;
    public static final int X_GrabServer = 36 ;
    public static final int X_UngrabServer = 37 ;
    public static final int X_QueryPointer = 38 ;
    public static final int X_GetMotionEvents = 39 ;
    public static final int X_TranslateCoords = 40 ;
    public static final int X_WarpPointer = 41 ;
    public static final int X_SetInputFocus = 42 ;
    public static final int X_GetInputFocus = 43 ;
    public static final int X_QueryKeymap = 44 ;
    public static final int X_OpenFont = 45 ;
    public static final int X_CloseFont = 46 ;
    public static final int X_QueryFont = 47 ;
    public static final int X_QueryTextExtents = 48 ;
    public static final int X_ListFonts = 49 ;
    public static final int X_ListFontsWithInfo = 50 ;
    public static final int X_SetFontPath = 51 ;
    public static final int X_GetFontPath = 52 ;
    public static final int X_CreatePixmap = 53 ;
    public static final int X_FreePixmap = 54 ;
    public static final int X_CreateGC = 55 ;
    public static final int X_ChangeGC = 56 ;
    public static final int X_CopyGC = 57 ;
    public static final int X_SetDashes = 58 ;
    public static final int X_SetClipRectangles = 59 ;
    public static final int X_FreeGC = 60 ;
    public static final int X_ClearArea = 61 ;
    public static final int X_CopyArea = 62 ;
    public static final int X_CopyPlane = 63 ;
    public static final int X_PolyPoint = 64 ;
    public static final int X_PolyLine = 65 ;
    public static final int X_PolySegment = 66 ;
    public static final int X_PolyRectangle = 67 ;
    public static final int X_PolyArc = 68 ;
    public static final int X_FillPoly = 69 ;
    public static final int X_PolyFillRectangle = 70 ;
    public static final int X_PolyFillArc = 71 ;
    public static final int X_PutImage = 72 ;
    public static final int X_GetImage = 73 ;
    public static final int X_PolyText8 = 74 ;
    public static final int X_PolyText16 = 75 ;
    public static final int X_ImageText8 = 76 ;
    public static final int X_ImageText16 = 77 ;
    public static final int X_CreateColormap = 78 ;
    public static final int X_FreeColormap = 79 ;
    public static final int X_CopyColormapAndFree = 80 ;
    public static final int X_InstallColormap = 81 ;
    public static final int X_UninstallColormap = 82 ;
    public static final int X_ListInstalledColormaps = 83 ;
    public static final int X_AllocColor = 84 ;
    public static final int X_AllocNamedColor = 85 ;
    public static final int X_AllocColorCells = 86 ;
    public static final int X_AllocColorPlanes = 87 ;
    public static final int X_FreeColors = 88 ;
    public static final int X_StoreColors = 89 ;
    public static final int X_StoreNamedColor = 90 ;
    public static final int X_QueryColors = 91 ;
    public static final int X_LookupColor = 92 ;
    public static final int X_CreateCursor = 93 ;
    public static final int X_CreateGlyphCursor = 94 ;
    public static final int X_FreeCursor = 95 ;
    public static final int X_RecolorCursor = 96 ;
    public static final int X_QueryBestSize = 97 ;
    public static final int X_QueryExtension = 98 ;
    public static final int X_ListExtensions = 99 ;
    public static final int X_ChangeKeyboardMapping = 100 ;
    public static final int X_GetKeyboardMapping = 101 ;
    public static final int X_ChangeKeyboardControl = 102 ;
    public static final int X_GetKeyboardControl = 103 ;
    public static final int X_Bell = 104 ;
    public static final int X_ChangePointerControl = 105 ;
    public static final int X_GetPointerControl = 106 ;
    public static final int X_SetScreenSaver = 107 ;
    public static final int X_GetScreenSaver = 108 ;
    public static final int X_ChangeHosts = 109 ;
    public static final int X_ListHosts = 110 ;
    public static final int X_SetAccessControl = 111 ;
    public static final int X_SetCloseDownMode = 112 ;
    public static final int X_KillClient = 113 ;
    public static final int X_RotateProperties = 114 ;
    public static final int X_ForceScreenSaver = 115 ;
    public static final int X_SetPointerMapping = 116 ;
    public static final int X_GetPointerMapping = 117 ;
    public static final int X_SetModifierMapping = 118 ;
    public static final int X_GetModifierMapping = 119 ;
    public static final int X_NoOperation = 127 ;
}
