/*
 * Copyright (c) 1996, 2020, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWTMSG_H
#define AWTMSG_H

#include <awt.h>

extern const UINT SYSCOMMAND_IMM;

/*
 * #defines for MouseWheel support
 *
 * Most of this is defined in winuser.h, however
 * it is enclosed by #ifdefs that aren't true
 * for all windows platforms.  To ensure that
 * necessary #defines are always available,
 * they're defined here as necessary.
 * See winuser.h for details.
 */

#ifndef WM_DPICHANGED
#define WM_DPICHANGED                   0x02E0
#endif //WM_DPICHANGED

#ifndef WM_MOUSEWHEEL
#define WM_MOUSEWHEEL                   0x020A
#endif //WM_MOUSEWHEEL

#ifndef WM_MOUSEHWHEEL
#define WM_MOUSEHWHEEL                  0x020E
#endif //WM_MOUSEHWHEEL

#ifndef WHEEL_DELTA
#define WHEEL_DELTA                     120
#endif //WHEEL_DELTA

#ifndef WHEEL_PAGESCROLL
#define WHEEL_PAGESCROLL                (UINT_MAX)
#endif //WHEEL_PAGESCROLL

#ifndef SPI_GETWHEELSCROLLLINES
#define SPI_GETWHEELSCROLLLINES         0x0068
#endif //SPI_GETWHEELSCROLLLINES

#ifndef SPI_GETWHEELSCROLLCHARS
#define SPI_GETWHEELSCROLLCHARS         0x006C
#endif //SPI_GETWHEELSCROLLCHARS

#ifndef SM_MOUSEWHEELPRESENT
#define SM_MOUSEWHEELPRESENT            75
#endif //SM_MOUSEWHEELPRESENT

#ifndef COLOR_HOTLIGHT
#define COLOR_HOTLIGHT                  26
#endif //COLOR_HOTLIGHT

#ifndef COLOR_GRADIENTACTIVECAPTION
#define COLOR_GRADIENTACTIVECAPTION     27
#endif //COLOR_GRADIENTACTIVECAPTION

#ifndef COLOR_GRADIENTINACTIVECAPTION
#define COLOR_GRADIENTINACTIVECAPTION   28
#endif //COLOR_GRADIENTINACTIVECAPTION

#ifndef SPI_GETACTIVEWINDOWTRACKING
#define SPI_GETACTIVEWINDOWTRACKING     0x1000
#endif //SPI_GETACTIVEWINDOWTRACKING

#ifndef SPI_GETMENUANIMATION
#define SPI_GETMENUANIMATION            0x1002
#endif //SPI_GETMENUANIMATION

#ifndef SPI_GETCOMBOBOXANIMATION
#define SPI_GETCOMBOBOXANIMATION        0x1004
#endif //SPI_GETCOMBOBOXANIMATION

#ifndef SPI_GETLISTBOXSMOOTHSCROLLING
#define SPI_GETLISTBOXSMOOTHSCROLLING   0x1006
#endif //SPI_GETLISTBOXSMOOTHSCROLLING

#ifndef SPI_GETGRADIENTCAPTIONS
#define SPI_GETGRADIENTCAPTIONS         0x1008
#endif //SPI_GETGRADIENTCAPTIONS

#ifndef SPI_GETKEYBOARDCUES
#define SPI_GETKEYBOARDCUES             0x100A
#endif //SPI_GETKEYBOARDCUES

#ifndef SPI_GETACTIVEWNDTRKZORDER
#define SPI_GETACTIVEWNDTRKZORDER       0x100C
#endif //SPI_GETACTIVEWNDTRKZORDER

#ifndef SPI_GETHOTTRACKING
#define SPI_GETHOTTRACKING              0x100E
#endif //SPI_GETHOTTRACKING

#ifndef SPI_GETMENUFADE
#define SPI_GETMENUFADE                 0x1012
#endif //SPI_GETMENUFADE

#ifndef SPI_GETSELECTIONFADE
#define SPI_GETSELECTIONFADE            0x1014
#endif //SPI_GETSELECTIONFADE

#ifndef SPI_GETTOOLTIPANIMATION
#define SPI_GETTOOLTIPANIMATION         0x1016
#endif //SPI_GETTOOLTIPANIMATION

#ifndef SPI_GETTOOLTIPFADE
#define SPI_GETTOOLTIPFADE              0x1018
#endif //SPI_GETTOOLTIPFADE

#ifndef SPI_GETFOREGROUNDLOCKTIMEOUT
#define SPI_GETFOREGROUNDLOCKTIMEOUT    0x2000
#endif //SPI_GETFOREGROUNDLOCKTIMEOUT

#ifndef SPI_GETACTIVEWNDTRKTIMEOUT
#define SPI_GETACTIVEWNDTRKTIMEOUT      0x2002
#endif //SPI_GETACTIVEWNDTRKTIMEOUT

#ifndef SPI_GETFOREGROUNDFLASHCOUNT
#define SPI_GETFOREGROUNDFLASHCOUNT     0x2004
#endif //SPI_GETFOREGROUNDFLASHCOUNT

#ifndef SPI_GETFONTSMOOTHINGTYPE
#define SPI_GETFONTSMOOTHINGTYPE        0x200A
#endif //SPI_GETFONTSMOOTHINGTYPE

#ifndef SPI_GETFONTSMOOTHINGCONTRAST
#define SPI_GETFONTSMOOTHINGCONTRAST    0x200C
#endif //SPI_GETFONTSMOOTHINGCONTRAST


//
// Flags for AnimateWindow
//
#ifndef AW_HOR_POSITIVE
#define AW_HOR_POSITIVE             0x00000001
#endif //AW_HOR_POSITIVE

#ifndef AW_HOR_NEGATIVE
#define AW_HOR_NEGATIVE             0x00000002
#endif //AW_HOR_NEGATIVE

#ifndef AW_VER_POSITIVE
#define AW_VER_POSITIVE             0x00000004
#endif //AW_VER_POSITIVE

#ifndef AW_VER_NEGATIVE
#define AW_VER_NEGATIVE             0x00000008
#endif //AW_VER_NEGATIVE

#ifndef AW_CENTER
#define AW_CENTER                   0x00000010
#endif //AW_CENTER

#ifndef AW_HIDE
#define AW_HIDE                     0x00010000
#endif //AW_HIDE

#ifndef AW_ACTIVATE
#define AW_ACTIVATE                 0x00020000
#endif //AW_ACTIVATE

#ifndef AW_SLIDE
#define AW_SLIDE                    0x00040000
#endif //AW_SLIDE

#ifndef AW_BLEND
#define AW_BLEND                    0x00080000
#endif //AW_BLEND


// AwtComponent messages
enum {
    // 6427323: unfortunately WM_APP+nnn conflicts with edit control messages
    // on XP with IME support, so we're shifting our messages
    // to some random value just to avoid the conflict
    WM_AWT_COMPONENT_CREATE = WM_APP+0x1800,
    WM_AWT_DESTROY_WINDOW,
    WM_AWT_MOUSEENTER,
    WM_AWT_MOUSEEXIT,
    WM_AWT_COMPONENT_SHOW,
    WM_AWT_COMPONENT_HIDE,
    WM_AWT_COMPONENT_SETFOCUS,
    WM_AWT_WINDOW_SETACTIVE,
    WM_AWT_LIST_SETMULTISELECT,
    WM_AWT_HANDLE_EVENT,
    WM_AWT_PRINT_COMPONENT,
    WM_AWT_RESHAPE_COMPONENT,
    WM_AWT_SETALWAYSONTOP,
    WM_AWT_BEGIN_VALIDATE,
    WM_AWT_END_VALIDATE,
    WM_AWT_FORWARD_CHAR,
    WM_AWT_FORWARD_BYTE,
    WM_AWT_SET_SCROLL_INFO,
    WM_AWT_CREATECONTEXT,
    WM_AWT_DESTROYCONTEXT,
    WM_AWT_ASSOCIATECONTEXT,
    WM_AWT_GET_DEFAULT_IME_HANDLER,
    WM_AWT_HANDLE_NATIVE_IME_EVENT,
    WM_AWT_PRE_KEYDOWN,
    WM_AWT_PRE_KEYUP,
    WM_AWT_PRE_SYSKEYDOWN,
    WM_AWT_PRE_SYSKEYUP,

    /* deleted DND mesg's */

    WM_AWT_ENDCOMPOSITION,
    WM_AWT_DISPOSE,
    WM_AWT_DISPOSEPDATA,
    WM_AWT_DELETEOBJECT,
    WM_AWT_SETCONVERSIONSTATUS,
    WM_AWT_GETCONVERSIONSTATUS,
    WM_AWT_SETOPENSTATUS,
    WM_AWT_GETOPENSTATUS,
    WM_AWT_ACTIVATEKEYBOARDLAYOUT,
    WM_AWT_OPENCANDIDATEWINDOW,
    WM_AWT_DLG_SHOWMODAL,
    WM_AWT_DLG_ENDMODAL,
    WM_AWT_SETCURSOR,
    WM_AWT_WAIT_FOR_SINGLE_OBJECT,
    WM_AWT_INVOKE_METHOD,
    WM_AWT_INVOKE_VOID_METHOD,
    WM_AWT_EXECUTE_SYNC,
    WM_AWT_OBJECTLISTCLEANUP,

    WM_AWT_CURSOR_SYNC,
    WM_AWT_GETDC,
    WM_AWT_RELEASEDC,
    WM_AWT_RELEASE_ALL_DCS,
    WM_AWT_SHOWCURSOR,
    WM_AWT_HIDECURSOR,
    WM_AWT_CREATE_PRINTED_PIXELS,

    // Tray messages
    WM_AWT_TRAY_NOTIFY,

    WM_SYNC_WAIT
};

#ifndef WM_UNDOCUMENTED_CLICKMENUBAR
#define WM_UNDOCUMENTED_CLICKMENUBAR 0x0313
#endif

#ifndef WM_UNDOCUMENTED_CLIENTSHUTDOWN
#define WM_UNDOCUMENTED_CLIENTSHUTDOWN 0x003b
#endif

#endif  // AWTMSG_H
