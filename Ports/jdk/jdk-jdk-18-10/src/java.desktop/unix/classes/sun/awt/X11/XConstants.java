/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

public final class XConstants {

    private XConstants(){}

    public static final int X_PROTOCOL = 11 ; /* current protocol version */
    public static final int X_PROTOCOL_REVISION = 0 ; /* current minor version */

    /* Resources */

    /*
     * _XSERVER64 must ONLY be defined when compiling X server sources on
     * systems where unsigned long is not 32 bits, must NOT be used in
     * client or library code.
     */

    /*
    #ifndef _XSERVER64
    typedef unsigned long XID;
    typedef unsigned long Mask;
    typedef unsigned long Atom;
    typedef unsigned long VisualID;
    typedef unsigned long Time;
    #else
    typedef CARD32 XID;
    typedef CARD32 Mask;
    typedef CARD32 Atom;
    typedef CARD32 VisualID;
    typedef CARD32 Time;
    #endif

    typedef XID Window;
    typedef XID Drawable;
    typedef XID Font;
    typedef XID Pixmap;
    typedef XID Cursor;
    typedef XID Colormap;
    typedef XID GContext;
    typedef XID KeySym;

    typedef unsigned char KeyCode;
     */
    /*****************************************************************
     * RESERVED RESOURCE AND CONSTANT DEFINITIONS
     *****************************************************************/

    public static final long None = 0L ; /* universal null resource or null atom */

    /* background pixmap in CreateWindow and ChangeWindowAttributes */
    public static final long ParentRelative = 1L ;

    /* border pixmap in CreateWindow and ChangeWindowAttributes special
     * VisualID and special window class passed to CreateWindow */
    public static final long CopyFromParent = 0L ;

    public static final long PointerWindow = 0L ; /* destination window in SendEvent */
    public static final long InputFocus = 1L ; /* destination window in SendEvent */

    public static final long PointerRoot = 1L ; /* focus window in SetInputFocus */

    public static final long AnyPropertyType = 0L ; /* special Atom, passed to GetProperty */

    public static final long AnyKey = 0L ; /* special Key Code, passed to GrabKey */

    public static final long AnyButton = 0L ; /* special Button Code, passed to GrabButton */

    public static final long AllTemporary = 0L ; /* special Resource ID passed to KillClient */

    public static final long CurrentTime = 0L ; /* special Time */

    public static final long NoSymbol = 0L ; /* special KeySym */

    /*****************************************************************
     * EVENT DEFINITIONS
     *****************************************************************/

    /* Input Event Masks. Used as event-mask window attribute and as arguments
       to Grab requests.  Not to be confused with event names.  */

    public static final long NoEventMask = 0L ;
    public static final long KeyPressMask = (1L<<0) ;
    public static final long KeyReleaseMask = (1L<<1) ;
    public static final long ButtonPressMask = (1L<<2) ;
    public static final long ButtonReleaseMask = (1L<<3) ;
    public static final long EnterWindowMask = (1L<<4) ;
    public static final long LeaveWindowMask = (1L<<5) ;
    public static final long PointerMotionMask = (1L<<6) ;
    public static final long PointerMotionHintMask = (1L<<7) ;
    public static final long Button1MotionMask = (1L<<8) ;
    public static final long Button2MotionMask = (1L<<9) ;
    public static final long Button3MotionMask = (1L<<10) ;
    public static final long Button4MotionMask = (1L<<11) ;
    public static final long Button5MotionMask = (1L<<12) ;
    public static final long ButtonMotionMask = (1L<<13) ;
    public static final long KeymapStateMask = (1L<<14) ;
    public static final long ExposureMask = (1L<<15) ;
    public static final long VisibilityChangeMask = (1L<<16) ;
    public static final long StructureNotifyMask = (1L<<17) ;
    public static final long ResizeRedirectMask = (1L<<18) ;
    public static final long SubstructureNotifyMask = (1L<<19) ;
    public static final long SubstructureRedirectMask = (1L<<20) ;
    public static final long FocusChangeMask = (1L<<21) ;
    public static final long PropertyChangeMask = (1L<<22) ;
    public static final long ColormapChangeMask = (1L<<23) ;
    public static final long OwnerGrabButtonMask = (1L<<24) ;

    public static final int MAX_BUTTONS = 5;
    public static final int ALL_BUTTONS_MASK = (int) (Button1MotionMask | Button2MotionMask | Button3MotionMask | Button4MotionMask | Button5MotionMask);

    /* Event names.  Used in "type" field in XEvent structures.  Not to be
    confused with event masks above.  They start from 2 because 0 and 1
    are reserved in the protocol for errors and replies. */

    public static final int KeyPress = 2 ;
    public static final int KeyRelease = 3 ;
    public static final int ButtonPress = 4 ;
    public static final int ButtonRelease = 5 ;
    public static final int MotionNotify = 6 ;
    public static final int EnterNotify = 7 ;
    public static final int LeaveNotify = 8 ;
    public static final int FocusIn = 9 ;
    public static final int FocusOut = 10 ;
    public static final int KeymapNotify = 11 ;
    public static final int Expose = 12 ;
    public static final int GraphicsExpose = 13 ;
    public static final int NoExpose = 14 ;
    public static final int VisibilityNotify = 15 ;
    public static final int CreateNotify = 16 ;
    public static final int DestroyNotify = 17 ;
    public static final int UnmapNotify = 18 ;
    public static final int MapNotify = 19 ;
    public static final int MapRequest = 20 ;
    public static final int ReparentNotify = 21 ;
    public static final int ConfigureNotify = 22 ;
    public static final int ConfigureRequest = 23 ;
    public static final int GravityNotify = 24 ;
    public static final int ResizeRequest = 25 ;
    public static final int CirculateNotify = 26 ;
    public static final int CirculateRequest = 27 ;
    public static final int PropertyNotify = 28 ;
    public static final int SelectionClear = 29 ;
    public static final int SelectionRequest = 30 ;
    public static final int SelectionNotify = 31 ;
    public static final int ColormapNotify = 32 ;
    public static final int ClientMessage = 33 ;
    public static final int MappingNotify = 34 ;
    public static final int LASTEvent = 35 ; /* must be bigger than any event # */


    /* Key masks. Used as modifiers to GrabButton and GrabKey, results of QueryPointer,
       state in various key-, mouse-, and button-related events. */

    public static final int ShiftMask = (1<<0) ;
    public static final int LockMask = (1<<1) ;
    public static final int ControlMask = (1<<2) ;
    public static final int Mod1Mask = (1<<3) ;
    public static final int Mod2Mask = (1<<4) ;
    public static final int Mod3Mask = (1<<5) ;
    public static final int Mod4Mask = (1<<6) ;
    public static final int Mod5Mask = (1<<7) ;

    /* modifier names.  Used to build a SetModifierMapping request or
       to read a GetModifierMapping request.  These correspond to the
       masks defined above. */
    public static final int ShiftMapIndex = 0 ;
    public static final int LockMapIndex = 1 ;
    public static final int ControlMapIndex = 2 ;
    public static final int Mod1MapIndex = 3 ;
    public static final int Mod2MapIndex = 4 ;
    public static final int Mod3MapIndex = 5 ;
    public static final int Mod4MapIndex = 6 ;
    public static final int Mod5MapIndex = 7 ;

    public static final int AnyModifier = (1<<15) ; /* used in GrabButton, GrabKey */


    /* button names. Used as arguments to GrabButton and as detail in ButtonPress
       and ButtonRelease events.  Not to be confused with button masks above.
       Note that 0 is already defined above as "AnyButton".  */

    public static final int[] buttons  = new int [] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24};

    // those should probably be wrapped in a method or such
    // as it may be possible to remap them via x11 configuration files
    public static final int MouseWheelUp = buttons[3];
    public static final int MouseWheelDown = buttons[4];

    /* Notify modes */

    public static final int NotifyNormal = 0 ;
    public static final int NotifyGrab = 1 ;
    public static final int NotifyUngrab = 2 ;
    public static final int NotifyWhileGrabbed = 3 ;

    public static final int NotifyHint = 1 ; /* for MotionNotify events */

    /* Notify detail */

    public static final int NotifyAncestor = 0 ;
    public static final int NotifyVirtual = 1 ;
    public static final int NotifyInferior = 2 ;
    public static final int NotifyNonlinear = 3 ;
    public static final int NotifyNonlinearVirtual = 4 ;
    public static final int NotifyPointer = 5 ;
    public static final int NotifyPointerRoot = 6 ;
    public static final int NotifyDetailNone = 7 ;

    /* Visibility notify */

    public static final int VisibilityUnobscured = 0 ;
    public static final int VisibilityPartiallyObscured = 1 ;
    public static final int VisibilityFullyObscured = 2 ;

    /* Circulation request */

    public static final int PlaceOnTop = 0 ;
    public static final int PlaceOnBottom = 1 ;

    /* protocol families */

    public static final int FamilyInternet = 0 ;
    public static final int FamilyDECnet = 1 ;
    public static final int FamilyChaos = 2 ;

    /* Property notification */

    public static final int PropertyNewValue = 0 ;
    public static final int PropertyDelete = 1 ;

    /* Color Map notification */

    public static final int ColormapUninstalled = 0 ;
    public static final int ColormapInstalled = 1 ;

    /* GrabPointer, GrabButton, GrabKeyboard, GrabKey Modes */

    public static final int GrabModeSync = 0 ;
    public static final int GrabModeAsync = 1 ;

    /* GrabPointer, GrabKeyboard reply status */

    public static final int GrabSuccess = 0 ;
    public static final int AlreadyGrabbed = 1 ;
    public static final int GrabInvalidTime = 2 ;
    public static final int GrabNotViewable = 3 ;
    public static final int GrabFrozen = 4 ;

    /* AllowEvents modes */

    public static final int AsyncPointer = 0 ;
    public static final int SyncPointer = 1 ;
    public static final int ReplayPointer = 2 ;
    public static final int AsyncKeyboard = 3 ;
    public static final int SyncKeyboard = 4 ;
    public static final int ReplayKeyboard = 5 ;
    public static final int AsyncBoth = 6 ;
    public static final int SyncBoth = 7 ;

    /* Used in SetInputFocus, GetInputFocus */

    public static final int RevertToNone = (int)None ;
    public static final int RevertToPointerRoot = (int)PointerRoot ;
    public static final int RevertToParent = 2 ;

    /* Used in XEventsQueued */
    public static final int QueuedAlready = 0;
    public static final int QueuedAfterReading = 1;
    public static final int QueuedAfterFlush = 2;


    /*****************************************************************
     * ERROR CODES
     *****************************************************************/

    public static final int Success = 0 ; /* everything's okay */
    public static final int BadRequest = 1 ; /* bad request code */
    public static final int BadValue = 2 ; /* int parameter out of range */
    public static final int BadWindow = 3 ; /* parameter not a Window */
    public static final int BadPixmap = 4 ; /* parameter not a Pixmap */
    public static final int BadAtom = 5 ; /* parameter not an Atom */
    public static final int BadCursor = 6 ; /* parameter not a Cursor */
    public static final int BadFont = 7 ; /* parameter not a Font */
    public static final int BadMatch = 8 ; /* parameter mismatch */
    public static final int BadDrawable = 9 ; /* parameter not a Pixmap or Window */
    public static final int BadAccess = 10 ; /* depending on context:
                     - key/button already grabbed
                     - attempt to free an illegal
                       cmap entry
                    - attempt to store into a read-only
                       color map entry.
                    - attempt to modify the access control
                       list from other than the local host.
                    */
    public static final int BadAlloc = 11 ; /* insufficient resources */
    public static final int BadColor = 12 ; /* no such colormap */
    public static final int BadGC = 13 ; /* parameter not a GC */
    public static final int BadIDChoice = 14 ; /* choice not in range or already used */
    public static final int BadName = 15 ; /* font or color name doesn't exist */
    public static final int BadLength = 16 ; /* Request length incorrect */
    public static final int BadImplementation = 17 ; /* server is defective */

    public static final int FirstExtensionError = 128 ;
    public static final int LastExtensionError = 255 ;

    /*****************************************************************
     * WINDOW DEFINITIONS
     *****************************************************************/

    /* Window classes used by CreateWindow */
    /* Note that CopyFromParent is already defined as 0 above */

    public static final int InputOutput = 1 ;
    public static final int InputOnly = 2 ;

    /* Window attributes for CreateWindow and ChangeWindowAttributes */

    public static final long CWBackPixmap = (1L<<0) ;
    public static final long CWBackPixel = (1L<<1) ;
    public static final long CWBorderPixmap = (1L<<2) ;
    public static final long CWBorderPixel = (1L<<3) ;
    public static final long CWBitGravity = (1L<<4) ;
    public static final long CWWinGravity = (1L<<5) ;
    public static final long CWBackingStore = (1L<<6) ;
    public static final long CWBackingPlanes = (1L<<7) ;
    public static final long CWBackingPixel = (1L<<8) ;
    public static final long CWOverrideRedirect = (1L<<9) ;
    public static final long CWSaveUnder = (1L<<10) ;
    public static final long CWEventMask = (1L<<11) ;
    public static final long CWDontPropagate = (1L<<12) ;
    public static final long CWColormap = (1L<<13) ;
    public static final long CWCursor = (1L<<14) ;

    /* ConfigureWindow structure */

    public static final int CWX = (1<<0) ;
    public static final int CWY = (1<<1) ;
    public static final int CWWidth = (1<<2) ;
    public static final int CWHeight = (1<<3) ;
    public static final int CWBorderWidth = (1<<4) ;
    public static final int CWSibling = (1<<5) ;
    public static final int CWStackMode = (1<<6) ;


    /* Bit Gravity */

    public static final int ForgetGravity = 0 ;
    public static final int NorthWestGravity = 1 ;
    public static final int NorthGravity = 2 ;
    public static final int NorthEastGravity = 3 ;
    public static final int WestGravity = 4 ;
    public static final int CenterGravity = 5 ;
    public static final int EastGravity = 6 ;
    public static final int SouthWestGravity = 7 ;
    public static final int SouthGravity = 8 ;
    public static final int SouthEastGravity = 9 ;
    public static final int StaticGravity = 10 ;

    /* Window gravity + bit gravity above */

    public static final int UnmapGravity = 0 ;

    /* Used in CreateWindow for backing-store hint */

    public static final int NotUseful = 0 ;
    public static final int WhenMapped = 1 ;
    public static final int Always = 2 ;

    /* Used in GetWindowAttributes reply */

    public static final int IsUnmapped = 0 ;
    public static final int IsUnviewable = 1 ;
    public static final int IsViewable = 2 ;

    /* Used in ChangeSaveSet */

    public static final int SetModeInsert = 0 ;
    public static final int SetModeDelete = 1 ;

    /* Used in ChangeCloseDownMode */

    public static final int DestroyAll = 0 ;
    public static final int RetainPermanent = 1 ;
    public static final int RetainTemporary = 2 ;

    /* Window stacking method (in configureWindow) */

    public static final int Above = 0 ;
    public static final int Below = 1 ;
    public static final int TopIf = 2 ;
    public static final int BottomIf = 3 ;
    public static final int Opposite = 4 ;

    /* Circulation direction */

    public static final int RaiseLowest = 0 ;
    public static final int LowerHighest = 1 ;

    /* Property modes */

    public static final int PropModeReplace = 0 ;
    public static final int PropModePrepend = 1 ;
    public static final int PropModeAppend = 2 ;

    /*****************************************************************
     * GRAPHICS DEFINITIONS
     *****************************************************************/

    /* graphics functions, as in GC.alu */

    public static final int GXclear = 0x0 ; /* 0 */
    public static final int GXand = 0x1 ; /* src AND dst */
    public static final int GXandReverse = 0x2 ; /* src AND NOT dst */
    public static final int GXcopy = 0x3 ; /* src */
    public static final int GXandInverted = 0x4 ; /* NOT src AND dst */
    public static final int GXnoop = 0x5 ; /* dst */
    public static final int GXxor = 0x6 ; /* src XOR dst */
    public static final int GXor = 0x7 ; /* src OR dst */
    public static final int GXnor = 0x8 ; /* NOT src AND NOT dst */
    public static final int GXequiv = 0x9 ; /* NOT src XOR dst */
    public static final int GXinvert = 0xa ; /* NOT dst */
    public static final int GXorReverse = 0xb ; /* src OR NOT dst */
    public static final int GXcopyInverted = 0xc ; /* NOT src */
    public static final int GXorInverted = 0xd ; /* NOT src OR dst */
    public static final int GXnand = 0xe ; /* NOT src OR NOT dst */
    public static final int GXset = 0xf ; /* 1 */

    /* LineStyle */

    public static final int LineSolid = 0 ;
    public static final int LineOnOffDash = 1 ;
    public static final int LineDoubleDash = 2 ;

    /* capStyle */

    public static final int CapNotLast = 0 ;
    public static final int CapButt = 1 ;
    public static final int CapRound = 2 ;
    public static final int CapProjecting = 3 ;

    /* joinStyle */

    public static final int JoinMiter = 0 ;
    public static final int JoinRound = 1 ;
    public static final int JoinBevel = 2 ;

    /* fillStyle */

    public static final int FillSolid = 0 ;
    public static final int FillTiled = 1 ;
    public static final int FillStippled = 2 ;
    public static final int FillOpaqueStippled = 3 ;

    /* fillRule */

    public static final int EvenOddRule = 0 ;
    public static final int WindingRule = 1 ;

    /* subwindow mode */

    public static final int ClipByChildren = 0 ;
    public static final int IncludeInferiors = 1 ;

    /* SetClipRectangles ordering */

    public static final int Unsorted = 0 ;
    public static final int YSorted = 1 ;
    public static final int YXSorted = 2 ;
    public static final int YXBanded = 3 ;

    /* CoordinateMode for drawing routines */

    public static final int CoordModeOrigin = 0 ; /* relative to the origin */
    public static final int CoordModePrevious = 1 ; /* relative to previous point */

    /* Polygon shapes */

    public static final int Complex = 0 ; /* paths may intersect */
    public static final int Nonconvex = 1 ; /* no paths intersect, but not convex */
    public static final int Convex = 2 ; /* wholly convex */

    /* Arc modes for PolyFillArc */

    public static final int ArcChord = 0 ; /* join endpoints of arc */
    public static final int ArcPieSlice = 1 ; /* join endpoints to center of arc */

    /* GC components: masks used in CreateGC, CopyGC, ChangeGC, OR'ed into
       GC.stateChanges */

    public static final long GCFunction = (1L<<0) ;
    public static final long GCPlaneMask = (1L<<1) ;
    public static final long GCForeground = (1L<<2) ;
    public static final long GCBackground = (1L<<3) ;
    public static final long GCLineWidth = (1L<<4) ;
    public static final long GCLineStyle = (1L<<5) ;
    public static final long GCCapStyle = (1L<<6) ;
    public static final long GCJoinStyle = (1L<<7) ;
    public static final long GCFillStyle = (1L<<8) ;
    public static final long GCFillRule = (1L<<9) ;
    public static final long GCTile = (1L<<10) ;
    public static final long GCStipple = (1L<<11) ;
    public static final long GCTileStipXOrigin = (1L<<12) ;
    public static final long GCTileStipYOrigin = (1L<<13) ;
    public static final long GCFont = (1L<<14) ;
    public static final long GCSubwindowMode = (1L<<15) ;
    public static final long GCGraphicsExposures = (1L<<16) ;
    public static final long GCClipXOrigin = (1L<<17) ;
    public static final long GCClipYOrigin = (1L<<18) ;
    public static final long GCClipMask = (1L<<19) ;
    public static final long GCDashOffset = (1L<<20) ;
    public static final long GCDashList = (1L<<21) ;
    public static final long GCArcMode = (1L<<22) ;

    public static final int GCLastBit = 22 ;
    /*****************************************************************
     * FONTS
     *****************************************************************/

    /* used in QueryFont -- draw direction */

    public static final int FontLeftToRight = 0 ;
    public static final int FontRightToLeft = 1 ;

    public static final int FontChange = 255 ;

    /*****************************************************************
     *  IMAGING
     *****************************************************************/

    /* ImageFormat -- PutImage, GetImage */

    public static final int XYBitmap = 0 ; /* depth 1, XYFormat */
    public static final int XYPixmap = 1 ; /* depth == drawable depth */
    public static final int ZPixmap = 2 ; /* depth == drawable depth */

    /*****************************************************************
     *  COLOR MAP STUFF
     *****************************************************************/

    /* For CreateColormap */

    public static final int AllocNone = 0 ; /* create map with no entries */
    public static final int AllocAll = 1 ; /* allocate entire map writeable */


    /* Flags used in StoreNamedColor, StoreColors */

    public static final int DoRed = (1<<0) ;
    public static final int DoGreen = (1<<1) ;
    public static final int DoBlue = (1<<2) ;

    /*****************************************************************
     * CURSOR STUFF
     *****************************************************************/

    /* QueryBestSize Class */

    public static final int CursorShape = 0 ; /* largest size that can be displayed */
    public static final int TileShape = 1 ; /* size tiled fastest */
    public static final int StippleShape = 2 ; /* size stippled fastest */

    /*****************************************************************
     * KEYBOARD/POINTER STUFF
     *****************************************************************/

    public static final int AutoRepeatModeOff = 0 ;
    public static final int AutoRepeatModeOn = 1 ;
    public static final int AutoRepeatModeDefault = 2 ;

    public static final int LedModeOff = 0 ;
    public static final int LedModeOn = 1 ;

    /* masks for ChangeKeyboardControl */

    public static final long KBKeyClickPercent = (1L<<0) ;
    public static final long KBBellPercent = (1L<<1) ;
    public static final long KBBellPitch = (1L<<2) ;
    public static final long KBBellDuration = (1L<<3) ;
    public static final long KBLed = (1L<<4) ;
    public static final long KBLedMode = (1L<<5) ;
    public static final long KBKey = (1L<<6) ;
    public static final long KBAutoRepeatMode = (1L<<7) ;

    public static final int MappingSuccess = 0 ;
    public static final int MappingBusy = 1 ;
    public static final int MappingFailed = 2 ;

    public static final int MappingModifier = 0 ;
    public static final int MappingKeyboard = 1 ;
    public static final int MappingPointer = 2 ;

    /*****************************************************************
     * SCREEN SAVER STUFF
     *****************************************************************/

    public static final int DontPreferBlanking = 0 ;
    public static final int PreferBlanking = 1 ;
    public static final int DefaultBlanking = 2 ;

    public static final int DisableScreenSaver = 0 ;
    public static final int DisableScreenInterval = 0 ;

    public static final int DontAllowExposures = 0 ;
    public static final int AllowExposures = 1 ;
    public static final int DefaultExposures = 2 ;

    /* for ForceScreenSaver */

    public static final int ScreenSaverReset = 0 ;
    public static final int ScreenSaverActive = 1 ;

    /*****************************************************************
     * HOSTS AND CONNECTIONS
     *****************************************************************/

    /* for ChangeHosts */

    public static final int HostInsert = 0 ;
    public static final int HostDelete = 1 ;

    /* for ChangeAccessControl */

    public static final int EnableAccess = 1 ;
    public static final int DisableAccess = 0 ;

    /* Display classes  used in opening the connection
     * Note that the statically allocated ones are even numbered and the
     * dynamically changeable ones are odd numbered */

    public static final int StaticGray = 0 ;
    public static final int GrayScale = 1 ;
    public static final int StaticColor = 2 ;
    public static final int PseudoColor = 3 ;
    public static final int TrueColor = 4 ;
    public static final int DirectColor = 5 ;


    /* Byte order  used in imageByteOrder and bitmapBitOrder */

    public static final int LSBFirst = 0 ;
    public static final int MSBFirst = 1 ;

    /* XKB support */
    public static final int  XkbUseCoreKbd = 0x0100 ;
    public static final int  XkbNewKeyboardNotify = 0;
    public static final int  XkbMapNotify = 1;
    public static final int  XkbStateNotify = 2;
    public static final long XkbNewKeyboardNotifyMask = (1L << 0);
    public static final long XkbMapNotifyMask = (1L << 1);
    public static final long XkbStateNotifyMask = (1L << 2);
    public static final long XkbGroupStateMask  = (1L << 4);
    public static final long XkbKeyTypesMask = (1L<<0);
    public static final long XkbKeySymsMask = (1L<<1);
    public static final long XkbModifierMapMask = (1L<<2);
    public static final long XkbVirtualModsMask = (1L<<6); //server map

}
