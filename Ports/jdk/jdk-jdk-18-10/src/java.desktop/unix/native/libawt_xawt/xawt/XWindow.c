/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* Note that the contents of this file were taken from canvas.c
 * in the old motif-based AWT.
 */

#ifdef HEADLESS
    #error This file should not be included in headless library
#endif

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/Xos.h>
#include <X11/Xatom.h>
#include <ctype.h>

#include <jvm.h>
#include <jni.h>
#include <jlong.h>
#include <jni_util.h>

#include "sun_awt_X11_XWindow.h"

#include "awt_p.h"
#include "awt_GraphicsEnv.h"

#define XK_KATAKANA
#include <X11/keysym.h>     /* standard X keysyms */
#include <X11/DECkeysym.h>  /* DEC vendor-specific */
#include <X11/Sunkeysym.h>  /* Sun vendor-specific */
#include <X11/ap_keysym.h>  /* Apollo (HP) vendor-specific */
/*
 * HPKeysym.h is used not only for the hp keysyms, but also to
 * give us the osf keysyms that are also defined in HPkeysym.h.
 * However, HPkeysym.h is missing a couple of osf keysyms,
 * so I have #defined them below.
 */
#include <X11/HPkeysym.h>   /* HP vendor-specific */

#include "java_awt_event_KeyEvent.h"
#include "java_awt_event_InputEvent.h"
#include "java_awt_event_MouseEvent.h"
#include "java_awt_event_MouseWheelEvent.h"
#include "java_awt_AWTEvent.h"

/*
 * Two osf keys are not defined in standard keysym.h,
 * /Xm/VirtKeys.h, or HPkeysym.h, so I added them below.
 * I found them in /usr/openwin/lib/X11/XKeysymDB
 */
#ifndef osfXK_Prior
#define osfXK_Prior 0x1004FF55
#endif
#ifndef osfXK_Next
#define osfXK_Next 0x1004FF56
#endif

jfieldID windowID;
jfieldID drawStateID;
jfieldID targetID;
jfieldID graphicsConfigID;

extern jobject currentX11InputMethodInstance;
extern Boolean awt_x11inputmethod_lookupString(XKeyPressedEvent *, KeySym *);
Boolean awt_UseType4Patch = False;
Boolean awt_ServerDetected = False;
Boolean awt_XKBDetected = False;
Boolean awt_IsXsun = False;
Boolean awt_UseXKB = False;

typedef struct KEYMAP_ENTRY {
    jint awtKey;
    KeySym x11Key;
    Boolean mapsToUnicodeChar;
    jint keyLocation;
} KeymapEntry;

/* NB: XK_R? keysyms are for Type 4 keyboards.
 * The corresponding XK_F? keysyms are for Type 5
 *
 * Note: this table must be kept in sorted order, since it is traversed
 * according to both Java keycode and X keysym.  There are a number of
 * keycodes that map to more than one corresponding keysym, and we need
 * to choose the right one.  Unfortunately, there are some keysyms that
 * can map to more than one keycode, depending on what kind of keyboard
 * is in use (e.g. F11 and F12).
 */

KeymapEntry keymapTable[] =
{
    {java_awt_event_KeyEvent_VK_A, XK_a, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_B, XK_b, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_C, XK_c, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_D, XK_d, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_E, XK_e, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F, XK_f, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_G, XK_g, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_H, XK_h, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_I, XK_i, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_J, XK_j, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_K, XK_k, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_L, XK_l, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_M, XK_m, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_N, XK_n, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_O, XK_o, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_P, XK_p, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_Q, XK_q, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_R, XK_r, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_S, XK_s, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_T, XK_t, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_U, XK_u, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_V, XK_v, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_W, XK_w, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_X, XK_x, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_Y, XK_y, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_Z, XK_z, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* TTY Function keys */
    {java_awt_event_KeyEvent_VK_BACK_SPACE, XK_BackSpace, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_TAB, XK_Tab, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_TAB, XK_ISO_Left_Tab, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CLEAR, XK_Clear, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ENTER, XK_Return, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ENTER, XK_Linefeed, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAUSE, XK_Pause, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAUSE, XK_F21, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAUSE, XK_R1, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_SCROLL_LOCK, XK_Scroll_Lock, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_SCROLL_LOCK, XK_F23, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_SCROLL_LOCK, XK_R3, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ESCAPE, XK_Escape, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Other vendor-specific versions of TTY Function keys */
    {java_awt_event_KeyEvent_VK_BACK_SPACE, osfXK_BackSpace, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CLEAR, osfXK_Clear, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ESCAPE, osfXK_Escape, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Modifier keys */
    {java_awt_event_KeyEvent_VK_SHIFT, XK_Shift_L, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_LEFT},
    {java_awt_event_KeyEvent_VK_SHIFT, XK_Shift_R, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_RIGHT},
    {java_awt_event_KeyEvent_VK_CONTROL, XK_Control_L, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_LEFT},
    {java_awt_event_KeyEvent_VK_CONTROL, XK_Control_R, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_RIGHT},
    {java_awt_event_KeyEvent_VK_ALT, XK_Alt_L, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_LEFT},
    {java_awt_event_KeyEvent_VK_ALT, XK_Alt_R, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_RIGHT},
    {java_awt_event_KeyEvent_VK_META, XK_Meta_L, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_LEFT},
    {java_awt_event_KeyEvent_VK_META, XK_Meta_R, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_RIGHT},
    {java_awt_event_KeyEvent_VK_CAPS_LOCK, XK_Caps_Lock, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CAPS_LOCK, XK_Shift_Lock, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Misc Functions */
    {java_awt_event_KeyEvent_VK_PRINTSCREEN, XK_Print, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PRINTSCREEN, XK_F22, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PRINTSCREEN, XK_R2, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CANCEL, XK_Cancel, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_HELP, XK_Help, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_NUM_LOCK, XK_Num_Lock, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},

    /* Other vendor-specific versions of Misc Functions */
    {java_awt_event_KeyEvent_VK_CANCEL, osfXK_Cancel, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_HELP, osfXK_Help, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Rectangular Navigation Block */
    {java_awt_event_KeyEvent_VK_HOME, XK_Home, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_HOME, XK_R7, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_UP, XK_Page_Up, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_UP, XK_Prior, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_UP, XK_R9, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, XK_Page_Down, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, XK_Next, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, XK_R15, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_END, XK_End, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_END, XK_R13, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_INSERT, XK_Insert, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DELETE, XK_Delete, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Keypad equivalents of Rectangular Navigation Block */
    {java_awt_event_KeyEvent_VK_HOME, XK_KP_Home, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_PAGE_UP, XK_KP_Page_Up, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_PAGE_UP, XK_KP_Prior, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, XK_KP_Page_Down, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, XK_KP_Next, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_END, XK_KP_End, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_INSERT, XK_KP_Insert, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_DELETE, XK_KP_Delete, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},

    /* Other vendor-specific Rectangular Navigation Block */
    {java_awt_event_KeyEvent_VK_PAGE_UP, osfXK_PageUp, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_UP, osfXK_Prior, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, osfXK_PageDown, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PAGE_DOWN, osfXK_Next, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_END, osfXK_EndLine, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_INSERT, osfXK_Insert, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DELETE, osfXK_Delete, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Triangular Navigation Block */
    {java_awt_event_KeyEvent_VK_LEFT, XK_Left, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UP, XK_Up, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_RIGHT, XK_Right, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DOWN, XK_Down, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Keypad equivalents of Triangular Navigation Block */
    {java_awt_event_KeyEvent_VK_KP_LEFT, XK_KP_Left, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_KP_UP, XK_KP_Up, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_KP_RIGHT, XK_KP_Right, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_KP_DOWN, XK_KP_Down, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},

    /* Other vendor-specific Triangular Navigation Block */
    {java_awt_event_KeyEvent_VK_LEFT, osfXK_Left, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UP, osfXK_Up, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_RIGHT, osfXK_Right, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DOWN, osfXK_Down, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Remaining Cursor control & motion */
    {java_awt_event_KeyEvent_VK_BEGIN, XK_Begin, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_BEGIN, XK_KP_Begin, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},

    {java_awt_event_KeyEvent_VK_0, XK_0, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_1, XK_1, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_2, XK_2, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_3, XK_3, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_4, XK_4, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_5, XK_5, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_6, XK_6, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_7, XK_7, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_8, XK_8, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_9, XK_9, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_SPACE, XK_space, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_EXCLAMATION_MARK, XK_exclam, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_QUOTEDBL, XK_quotedbl, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_NUMBER_SIGN, XK_numbersign, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DOLLAR, XK_dollar, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_AMPERSAND, XK_ampersand, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_QUOTE, XK_apostrophe, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_LEFT_PARENTHESIS, XK_parenleft, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_RIGHT_PARENTHESIS, XK_parenright, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ASTERISK, XK_asterisk, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PLUS, XK_plus, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_COMMA, XK_comma, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_MINUS, XK_minus, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PERIOD, XK_period, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_SLASH, XK_slash, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_COLON, XK_colon, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_SEMICOLON, XK_semicolon, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_LESS, XK_less, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_EQUALS, XK_equal, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_GREATER, XK_greater, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_AT, XK_at, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_OPEN_BRACKET, XK_bracketleft, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_BACK_SLASH, XK_backslash, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CLOSE_BRACKET, XK_bracketright, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CIRCUMFLEX, XK_asciicircum, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UNDERSCORE, XK_underscore, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_BACK_QUOTE, XK_grave, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_BRACELEFT, XK_braceleft, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_BRACERIGHT, XK_braceright, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_INVERTED_EXCLAMATION_MARK, XK_exclamdown, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Remaining Numeric Keypad Keys */
    {java_awt_event_KeyEvent_VK_NUMPAD0, XK_KP_0, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD1, XK_KP_1, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD2, XK_KP_2, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD3, XK_KP_3, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD4, XK_KP_4, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD5, XK_KP_5, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD6, XK_KP_6, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD7, XK_KP_7, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD8, XK_KP_8, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_NUMPAD9, XK_KP_9, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_SPACE, XK_KP_Space, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_TAB, XK_KP_Tab, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_ENTER, XK_KP_Enter, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_EQUALS, XK_KP_Equal, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_EQUALS, XK_R4, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_MULTIPLY, XK_KP_Multiply, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_MULTIPLY, XK_F26, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_MULTIPLY, XK_R6, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_ADD, XK_KP_Add, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_SEPARATOR, XK_KP_Separator, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_SUBTRACT, XK_KP_Subtract, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_SUBTRACT, XK_F24, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_DECIMAL, XK_KP_Decimal, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_DIVIDE, XK_KP_Divide, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_DIVIDE, XK_F25, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},
    {java_awt_event_KeyEvent_VK_DIVIDE, XK_R5, TRUE, java_awt_event_KeyEvent_KEY_LOCATION_NUMPAD},

    /* Function Keys */
    {java_awt_event_KeyEvent_VK_F1, XK_F1, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F2, XK_F2, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F3, XK_F3, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F4, XK_F4, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F5, XK_F5, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F6, XK_F6, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F7, XK_F7, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F8, XK_F8, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F9, XK_F9, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F10, XK_F10, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F11, XK_F11, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F12, XK_F12, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Sun vendor-specific version of F11 and F12 */
    {java_awt_event_KeyEvent_VK_F11, SunXK_F36, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_F12, SunXK_F37, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* X11 keysym names for input method related keys don't always
     * match keytop engravings or Java virtual key names, so here we
     * only map constants that we've found on real keyboards.
     */
    /* Type 5c Japanese keyboard: kakutei */
    {java_awt_event_KeyEvent_VK_ACCEPT, XK_Execute, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    /* Type 5c Japanese keyboard: henkan */
    {java_awt_event_KeyEvent_VK_CONVERT, XK_Kanji, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    /* Type 5c Japanese keyboard: nihongo */
    {java_awt_event_KeyEvent_VK_INPUT_METHOD_ON_OFF, XK_Henkan_Mode, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    /* VK_KANA_LOCK is handled separately because it generates the
     * same keysym as ALT_GRAPH in spite of its different behavior.
     */

    {java_awt_event_KeyEvent_VK_ALL_CANDIDATES, XK_Zen_Koho, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ALPHANUMERIC, XK_Eisu_Shift, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ALPHANUMERIC, XK_Eisu_toggle, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CODE_INPUT, XK_Kanji_Bangou, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_FULL_WIDTH, XK_Zenkaku, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_HALF_WIDTH, XK_Hankaku, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_HIRAGANA, XK_Hiragana, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_JAPANESE_HIRAGANA, XK_Hiragana, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_KATAKANA, XK_Katakana, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_JAPANESE_KATAKANA, XK_Katakana, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_JAPANESE_ROMAN, XK_Romaji, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_KANA, XK_Kana_Shift, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_KANA_LOCK, XK_Kana_Lock, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_KANJI, XK_Kanji, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_NONCONVERT, XK_Muhenkan, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PREVIOUS_CANDIDATE, XK_Mae_Koho, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ROMAN_CHARACTERS, XK_Romaji, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_COMPOSE, XK_Multi_key, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_ALT_GRAPH, XK_ISO_Level3_Shift, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Editing block */
    {java_awt_event_KeyEvent_VK_AGAIN, XK_Redo, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_AGAIN, XK_L2, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UNDO, XK_Undo, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UNDO, XK_L4, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_COPY, XK_L6, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PASTE, XK_L8, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CUT, XK_L10, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_FIND, XK_Find, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_FIND, XK_L9, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PROPS, XK_L3, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_STOP, XK_L1, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Sun vendor-specific versions for editing block */
    {java_awt_event_KeyEvent_VK_AGAIN, SunXK_Again, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UNDO, SunXK_Undo, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_COPY, SunXK_Copy, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PASTE, SunXK_Paste, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CUT, SunXK_Cut, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_FIND, SunXK_Find, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PROPS, SunXK_Props, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_STOP, SunXK_Stop, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Apollo (HP) vendor-specific versions for editing block */
    {java_awt_event_KeyEvent_VK_COPY, apXK_Copy, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CUT, apXK_Cut, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PASTE, apXK_Paste, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Other vendor-specific versions for editing block */
    {java_awt_event_KeyEvent_VK_COPY, osfXK_Copy, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_CUT, osfXK_Cut, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_PASTE, osfXK_Paste, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_UNDO, osfXK_Undo, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Dead key mappings (for European keyboards) */
    {java_awt_event_KeyEvent_VK_DEAD_GRAVE, XK_dead_grave, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_ACUTE, XK_dead_acute, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CIRCUMFLEX, XK_dead_circumflex, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_TILDE, XK_dead_tilde, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_MACRON, XK_dead_macron, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_BREVE, XK_dead_breve, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_ABOVEDOT, XK_dead_abovedot, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_DIAERESIS, XK_dead_diaeresis, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_ABOVERING, XK_dead_abovering, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_DOUBLEACUTE, XK_dead_doubleacute, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CARON, XK_dead_caron, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CEDILLA, XK_dead_cedilla, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_OGONEK, XK_dead_ogonek, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_IOTA, XK_dead_iota, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_VOICED_SOUND, XK_dead_voiced_sound, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_SEMIVOICED_SOUND, XK_dead_semivoiced_sound, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Sun vendor-specific dead key mappings (for European keyboards) */
    {java_awt_event_KeyEvent_VK_DEAD_GRAVE, SunXK_FA_Grave, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CIRCUMFLEX, SunXK_FA_Circum, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_TILDE, SunXK_FA_Tilde, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_ACUTE, SunXK_FA_Acute, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_DIAERESIS, SunXK_FA_Diaeresis, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CEDILLA, SunXK_FA_Cedilla, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* DEC vendor-specific dead key mappings (for European keyboards) */
    {java_awt_event_KeyEvent_VK_DEAD_ABOVERING, DXK_ring_accent, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CIRCUMFLEX, DXK_circumflex_accent, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CEDILLA, DXK_cedilla_accent, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_ACUTE, DXK_acute_accent, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_GRAVE, DXK_grave_accent, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_TILDE, DXK_tilde, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_DIAERESIS, DXK_diaeresis, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    /* Other vendor-specific dead key mappings (for European keyboards) */
    {java_awt_event_KeyEvent_VK_DEAD_ACUTE, hpXK_mute_acute, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_GRAVE, hpXK_mute_grave, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_CIRCUMFLEX, hpXK_mute_asciicircum, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_DIAERESIS, hpXK_mute_diaeresis, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},
    {java_awt_event_KeyEvent_VK_DEAD_TILDE, hpXK_mute_asciitilde, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_STANDARD},

    {java_awt_event_KeyEvent_VK_UNDEFINED, NoSymbol, FALSE, java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN}
};

static Boolean
keyboardHasKanaLockKey()
{
    static Boolean haveResult = FALSE;
    static Boolean result = FALSE;

    int32_t minKeyCode, maxKeyCode, keySymsPerKeyCode;
    KeySym *keySyms, *keySymsStart, keySym;
    int32_t i;
    int32_t kanaCount = 0;

    // Solaris doesn't let you swap keyboards without rebooting,
    // so there's no need to check for the kana lock key more than once.
    if (haveResult) {
       return result;
    }

    // There's no direct way to determine whether the keyboard has
    // a kana lock key. From available keyboard mapping tables, it looks
    // like only keyboards with the kana lock key can produce keysyms
    // for kana characters. So, as an indirect test, we check for those.
    XDisplayKeycodes(awt_display, &minKeyCode, &maxKeyCode);
    keySyms = XGetKeyboardMapping(awt_display, minKeyCode, maxKeyCode - minKeyCode + 1, &keySymsPerKeyCode);
    keySymsStart = keySyms;
    for (i = 0; i < (maxKeyCode - minKeyCode + 1) * keySymsPerKeyCode; i++) {
        keySym = *keySyms++;
        if ((keySym & 0xff00) == 0x0400) {
            kanaCount++;
        }
    }
    XFree(keySymsStart);

    // use a (somewhat arbitrary) minimum so we don't get confused by a stray function key
    result = kanaCount > 10;
    haveResult = TRUE;
    return result;
}

static void
keysymToAWTKeyCode(KeySym x11Key, jint *keycode, Boolean *mapsToUnicodeChar,
  jint *keyLocation)
{
    int32_t i;

    // Solaris uses XK_Mode_switch for both the non-locking AltGraph
    // and the locking Kana key, but we want to keep them separate for
    // KeyEvent.
    if (x11Key == XK_Mode_switch && keyboardHasKanaLockKey()) {
        *keycode = java_awt_event_KeyEvent_VK_KANA_LOCK;
        *mapsToUnicodeChar = FALSE;
        *keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;
        return;
    }

    for (i = 0;
         keymapTable[i].awtKey != java_awt_event_KeyEvent_VK_UNDEFINED;
         i++)
    {
        if (keymapTable[i].x11Key == x11Key) {
            *keycode = keymapTable[i].awtKey;
            *mapsToUnicodeChar = keymapTable[i].mapsToUnicodeChar;
            *keyLocation = keymapTable[i].keyLocation;
            return;
        }
    }

    *keycode = java_awt_event_KeyEvent_VK_UNDEFINED;
    *mapsToUnicodeChar = FALSE;
    *keyLocation = java_awt_event_KeyEvent_KEY_LOCATION_UNKNOWN;

    DTRACE_PRINTLN1("keysymToAWTKeyCode: no key mapping found: keysym = 0x%x", x11Key);
}

KeySym
awt_getX11KeySym(jint awtKey)
{
    int32_t i;

    if (awtKey == java_awt_event_KeyEvent_VK_KANA_LOCK && keyboardHasKanaLockKey()) {
        return XK_Mode_switch;
    }

    for (i = 0; keymapTable[i].awtKey != 0; i++) {
        if (keymapTable[i].awtKey == awtKey) {
            return keymapTable[i].x11Key;
        }
    }

    DTRACE_PRINTLN1("awt_getX11KeySym: no key mapping found: awtKey = 0x%x", awtKey);
    return NoSymbol;
}

/* Called from handleKeyEvent.  The purpose of this function is
 * to check for a list of vendor-specific keysyms, most of which
 * have values greater than 0xFFFF.  Most of these keys don't map
 * to unicode characters, but some do.
 *
 * For keys that don't map to unicode characters, the keysym
 * is irrelevant at this point.  We set the keysym to zero
 * to ensure that the switch statement immediately below
 * this function call (in adjustKeySym) won't incorrectly act
 * on them after the high bits are stripped off.
 *
 * For keys that do map to unicode characters, we change the keysym
 * to the equivalent that is < 0xFFFF
 */
static void
handleVendorKeySyms(XEvent *event, KeySym *keysym)
{
    KeySym originalKeysym = *keysym;

    switch (*keysym) {
        /* Apollo (HP) vendor-specific from <X11/ap_keysym.h> */
        case apXK_Copy:
        case apXK_Cut:
        case apXK_Paste:
        /* DEC vendor-specific from <X11/DECkeysym.h> */
        case DXK_ring_accent:         /* syn usldead_ring */
        case DXK_circumflex_accent:
        case DXK_cedilla_accent:      /* syn usldead_cedilla */
        case DXK_acute_accent:
        case DXK_grave_accent:
        case DXK_tilde:
        case DXK_diaeresis:
        /* Sun vendor-specific from <X11/Sunkeysym.h> */
        case SunXK_FA_Grave:
        case SunXK_FA_Circum:
        case SunXK_FA_Tilde:
        case SunXK_FA_Acute:
        case SunXK_FA_Diaeresis:
        case SunXK_FA_Cedilla:
        case SunXK_F36:                /* Labeled F11 */
        case SunXK_F37:                /* Labeled F12 */
        case SunXK_Props:
        case SunXK_Copy:
        case SunXK_Open:
        case SunXK_Paste:
        case SunXK_Cut:
        /* Other vendor-specific from HPkeysym.h */
        case hpXK_mute_acute:          /* syn usldead_acute */
        case hpXK_mute_grave:          /* syn usldead_grave */
        case hpXK_mute_asciicircum:    /* syn usldead_asciicircum */
        case hpXK_mute_diaeresis:      /* syn usldead_diaeresis */
        case hpXK_mute_asciitilde:     /* syn usldead_asciitilde */
        case osfXK_Copy:
        case osfXK_Cut:
        case osfXK_Paste:
        case osfXK_PageUp:
        case osfXK_PageDown:
        case osfXK_EndLine:
        case osfXK_Clear:
        case osfXK_Left:
        case osfXK_Up:
        case osfXK_Right:
        case osfXK_Down:
        case osfXK_Prior:
        case osfXK_Next:
        case osfXK_Insert:
        case osfXK_Undo:
        case osfXK_Help:
            *keysym = 0;
            break;
        /*
         * The rest DO map to unicode characters, so translate them
         */
        case osfXK_BackSpace:
            *keysym = XK_BackSpace;
            break;
        case osfXK_Escape:
            *keysym = XK_Escape;
            break;
        case osfXK_Cancel:
            *keysym = XK_Cancel;
            break;
        case osfXK_Delete:
            *keysym = XK_Delete;
            break;
        default:
            break;
    }

    if (originalKeysym != *keysym) {
        DTRACE_PRINTLN3("%s originalKeysym=0x%x, keysym=0x%x",
          "In handleVendorKeySyms:", originalKeysym, *keysym);
    }
}

/* Called from handleKeyEvent.
 * The purpose of this function is to adjust the keysym and XEvent
 * keycode for a key event.  This is basically a conglomeration of
 * bugfixes that require these adjustments.
 * Note that none of the keysyms in this function are less than 256.
 */
static void
adjustKeySym(XEvent *event, KeySym *keysym)
{
    KeySym originalKeysym = *keysym;
    KeyCode originalKeycode = event->xkey.keycode;

    /* We have seen bits set in the high two bytes on Linux,
     * which prevents this switch statement from executing
     * correctly.  Strip off the high order bits.
     */
    *keysym &= 0x0000FFFF;

    switch (*keysym) {
        case XK_ISO_Left_Tab:        /* shift-tab on Linux */
            *keysym = XK_Tab;
            break;
        case XK_KP_Decimal:
            *keysym = '.';
            break;
        case XK_KP_Add:
            *keysym = '+';
            break;
        case XK_F24:           /* NumLock off */
        case XK_KP_Subtract:   /* NumLock on */
            *keysym = '-';
            break;
        case XK_F25:           /* NumLock off */
        case XK_KP_Divide:     /* NumLock on */
            *keysym = '/';
            break;
        case XK_F26:           /* NumLock off */
        case XK_KP_Multiply:   /* NumLock on */
            *keysym = '*';
            break;
        case XK_KP_Equal:
            *keysym = '=';
            break;
        case XK_KP_0:
            *keysym = '0';
            break;
        case XK_KP_1:
            *keysym = '1';
            break;
        case XK_KP_2:
            *keysym = '2';
            break;
        case XK_KP_3:
            *keysym = '3';
            break;
        case XK_KP_4:
            *keysym = '4';
            break;
        case XK_KP_5:
            *keysym = '5';
            break;
        case XK_KP_6:
            *keysym = '6';
            break;
        case XK_KP_7:
            *keysym = '7';
            break;
        case XK_KP_8:
            *keysym = '8';
            break;
        case XK_KP_9:
            *keysym = '9';
            break;
        case XK_KP_Left:  /* Bug 4350175 */
            *keysym = XK_Left;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Up:
            *keysym = XK_Up;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Right:
            *keysym = XK_Right;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Down:
            *keysym = XK_Down;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Home:
            *keysym = XK_Home;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_End:
            *keysym = XK_End;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Page_Up:
            *keysym = XK_Page_Up;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Page_Down:
            *keysym = XK_Page_Down;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Begin:
            *keysym = XK_Begin;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Insert:
            *keysym = XK_Insert;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Delete:
            *keysym = XK_Delete;
            event->xkey.keycode = XKeysymToKeycode(awt_display, *keysym);
            break;
        case XK_KP_Enter:
            *keysym = XK_Linefeed;
            event->xkey.keycode = XKeysymToKeycode(awt_display, XK_Return);
            break;
        default:
            break;
    }

    if (originalKeysym != *keysym) {
        DTRACE_PRINTLN2("In adjustKeySym: originalKeysym=0x%x, keysym=0x%x",
          originalKeysym, *keysym);
    }
    if (originalKeycode != event->xkey.keycode) {
        DTRACE_PRINTLN2("In adjustKeySym: originalKeycode=0x%x, keycode=0x%x",
          originalKeycode, event->xkey.keycode);
    }
}

/*
 * What a sniffer sez?
 * Xsun and Xorg if NumLock is on do two thing different:
 * keep Keypad key in different places of keysyms array and
 * ignore/obey "ModLock is ShiftLock", so we should choose.
 * People say, it's right to use behavior and not Vendor tags to decide.
 * Maybe. But why these tags were invented, then?
 * TODO: use behavior, not tags. Maybe.
 */
static Boolean
isXsunServer(XEvent *event) {
    if( awt_ServerDetected ) return awt_IsXsun;
    if( (strncmp( ServerVendor( event->xkey.display ), "Sun Microsystems, Inc.", 22) != 0) &&
        (strncmp( ServerVendor( event->xkey.display ), "Oracle Corporation", 18) != 0) )
    {
        awt_ServerDetected = True;
        awt_IsXsun = False;
        return False;
    }
    // Now, it's Sun. It still may be Xorg though, eg on Solaris 10, x86.
    // Today (2005), VendorRelease of Xorg is a Big Number unlike Xsun.
    if( VendorRelease( event->xkey.display ) > 10000 ) {
        awt_ServerDetected = True;
        awt_IsXsun = False;
        return False;
    }
    awt_ServerDetected = True;
    awt_IsXsun = True;
    return True;
}
/*
 * +kb or -kb ?
 */
static Boolean
isXKBenabled(Display *display) {
    int mop, beve, berr;
    if( !awt_XKBDetected ) {
        /*
         * NB: TODO: hope it will return False if XkbIgnoreExtension was called!
         */
        awt_UseXKB = XQueryExtension(display, "XKEYBOARD", &mop, &beve, &berr);
        awt_XKBDetected = True;
    }
    return awt_UseXKB;
}

/*
 * Map a keycode to the corresponding keysym.
 * This replaces the deprecated X11 function XKeycodeToKeysym
 */
KeySym
keycodeToKeysym(Display *display, KeyCode keycode, int index) {
    static int min_kc = -1;
    static int max_kc;
    if (min_kc == -1) {
        (void) XDisplayKeycodes(display, &min_kc, &max_kc);
    }
    if (keycode < min_kc || keycode > max_kc || index < 0) {
        return NoSymbol;
    }
    int num_syms;
    KeySym *key_syms = XGetKeyboardMapping(display, keycode, 1, &num_syms);
    if (index >= num_syms) {
        XFree(key_syms);
        return NoSymbol;
    }
    KeySym ks = key_syms[index];
    XFree(key_syms);
    return ks;
}

static Boolean
isKPevent(XEvent *event)
{
    /*
     *  Xlib manual, ch 12.7 says, as a first rule for choice of keysym:
     *  The numlock modifier is on and the second KeySym is a keypad KeySym. In this case,
     *  if the Shift modifier is on, or if the Lock modifier is on and is interpreted as ShiftLock,
     *  then the first KeySym is used, otherwise the second KeySym is used.
     *
     *  However, Xsun server does ignore ShiftLock and always takes 3-rd element from an array.
     *
     *  So, is it a keypad keysym?
     */
    Boolean bsun = isXsunServer( event );
    Boolean bxkb = isXKBenabled( event->xkey.display );
    return IsKeypadKey( keycodeToKeysym(event->xkey.display, event->xkey.keycode,(bsun && !bxkb ? 2 : 1) ) );
}
static void
dumpKeysymArray(XEvent *event) {
    printf("    0x%lX\n", (unsigned long)keycodeToKeysym(event->xkey.display, event->xkey.keycode, 0));
    printf("    0x%lX\n", (unsigned long)keycodeToKeysym(event->xkey.display, event->xkey.keycode, 1));
    printf("    0x%lX\n", (unsigned long)keycodeToKeysym(event->xkey.display, event->xkey.keycode, 2));
    printf("    0x%lX\n", (unsigned long)keycodeToKeysym(event->xkey.display, event->xkey.keycode, 3));
}
/*
 * In a next redesign, get rid of this code altogether.
 *
 */
static void
handleKeyEventWithNumLockMask_New(XEvent *event, KeySym *keysym)
{
    KeySym originalKeysym = *keysym;
    if( !isKPevent( event ) ) {
        return;
    }
    if( isXsunServer( event ) && !awt_UseXKB) {
        if( (event->xkey.state & ShiftMask) ) { // shift modifier is on
            *keysym = keycodeToKeysym(event->xkey.display,
                                   event->xkey.keycode, 3);
         }else {
            *keysym = keycodeToKeysym(event->xkey.display,
                                   event->xkey.keycode, 2);
         }
    } else {
        if( (event->xkey.state & ShiftMask) || // shift modifier is on
            ((event->xkey.state & LockMask) && // lock modifier is on
             (awt_ModLockIsShiftLock)) ) {     // it is interpreted as ShiftLock
            *keysym = keycodeToKeysym(event->xkey.display,
                                   event->xkey.keycode, 0);
        }else{
            *keysym = keycodeToKeysym(event->xkey.display,
                                   event->xkey.keycode, 1);
        }
    }
}

/* Called from handleKeyEvent.
 * The purpose of this function is to make some adjustments to keysyms
 * that have been found to be necessary when the NumLock mask is set.
 * They come from various bug fixes and rearchitectures.
 * This function is meant to be called when
 * (event->xkey.state & awt_NumLockMask) is TRUE.
 */
static void
handleKeyEventWithNumLockMask(XEvent *event, KeySym *keysym)
{
    KeySym originalKeysym = *keysym;

#if !defined(__linux__)
    /* The following code on Linux will cause the keypad keys
     * not to echo on JTextField when the NumLock is on. The
     * keysyms will be 0, because the last parameter 2 is not defined.
     * See Xlib Programming Manual, O'Reilly & Associates, Section
     * 9.1.5 "Other Keyboard-handling Routines", "The meaning of
     * the keysym list beyond the first two (unmodified, Shift or
     * Shift Lock) is not defined."
     */

    /* Translate again with NumLock as modifier. */
    /* ECH - I wonder why we think that NumLock corresponds to 2?
       On Linux, we've seen xmodmap -pm yield mod2 as NumLock,
       but I don't know that it will be for every configuration.
       Perhaps using the index (modn in awt_MToolkit.c:setup_modifier_map)
       would be more correct.
     */
    *keysym = keycodeToKeysym(event->xkey.display,
                               event->xkey.keycode, 2);
    if (originalKeysym != *keysym) {
        DTRACE_PRINTLN3("%s originalKeysym=0x%x, keysym=0x%x",
          "In handleKeyEventWithNumLockMask ifndef linux:",
          originalKeysym, *keysym);
    }
#endif

    /* Note: the XK_R? key assignments are for Type 4 kbds */
    switch (*keysym) {
        case XK_R13:
            *keysym = XK_KP_1;
            break;
        case XK_R14:
            *keysym = XK_KP_2;
            break;
        case XK_R15:
            *keysym = XK_KP_3;
            break;
        case XK_R10:
            *keysym = XK_KP_4;
            break;
        case XK_R11:
            *keysym = XK_KP_5;
            break;
        case XK_R12:
            *keysym = XK_KP_6;
            break;
        case XK_R7:
            *keysym = XK_KP_7;
            break;
        case XK_R8:
            *keysym = XK_KP_8;
            break;
        case XK_R9:
            *keysym = XK_KP_9;
            break;
        case XK_KP_Insert:
            *keysym = XK_KP_0;
            break;
        case XK_KP_Delete:
            *keysym = XK_KP_Decimal;
            break;
        case XK_R4:
            *keysym = XK_KP_Equal;  /* Type 4 kbd */
            break;
        case XK_R5:
            *keysym = XK_KP_Divide;
            break;
        case XK_R6:
            *keysym = XK_KP_Multiply;
            break;
        /*
         * Need the following keysym changes for Linux key releases.
         * Sometimes the modifier state gets messed up, so we get a
         * KP_Left when we should get a KP_4, for example.
         * XK_KP_Insert and XK_KP_Delete were already handled above.
         */
        case XK_KP_Left:
            *keysym = XK_KP_4;
            break;
        case XK_KP_Up:
            *keysym = XK_KP_8;
            break;
        case XK_KP_Right:
            *keysym = XK_KP_6;
            break;
        case XK_KP_Down:
            *keysym = XK_KP_2;
            break;
        case XK_KP_Home:
            *keysym = XK_KP_7;
            break;
        case XK_KP_End:
            *keysym = XK_KP_1;
            break;
        case XK_KP_Page_Up:
            *keysym = XK_KP_9;
            break;
        case XK_KP_Page_Down:
            *keysym = XK_KP_3;
            break;
        case XK_KP_Begin:
            *keysym = XK_KP_5;
            break;
        default:
            break;
    }

    if (originalKeysym != *keysym) {
        DTRACE_PRINTLN3("%s originalKeysym=0x%x, keysym=0x%x",
          "In handleKeyEventWithNumLockMask:", originalKeysym, *keysym);
    }
}

/* This function is called as the keyChar parameter of a call to
 * awt_post_java_key_event.  It depends on being called after adjustKeySym.
 *
 * This function just handles a few values where we know that the
 * keysym is not the same as the unicode value.  For values that
 * we don't handle explicitly, we just cast the keysym to a jchar.
 * Most of the real mapping work that gets the correct keysym is handled
 * in the mapping table, adjustKeySym, etc.
 *
 * XXX
 * Maybe we should enumerate the keysyms for which we have a mapping
 * in the keyMap, but that don't map to unicode chars, and return
 * CHAR_UNDEFINED?  Then use the buffer value from XLookupString
 * instead of the keysym as the keychar when posting.  Then we don't
 * need to test using mapsToUnicodeChar.  That way, we would post keyTyped
 * for all the chars that generate unicode chars, including LATIN2-4, etc.
 * Note: what does the buffer from XLookupString contain when
 * the character is a non-printable unicode character like Cancel or Delete?
 */
jchar
keySymToUnicodeCharacter(KeySym keysym) {
    jchar unicodeValue = (jchar) keysym;

    switch (keysym) {
      case XK_BackSpace:
      case XK_Tab:
      case XK_Linefeed:
      case XK_Escape:
      case XK_Delete:
          /* Strip off highorder bits defined in xkeysymdef.h
           * I think doing this converts them to values that
           * we can cast to jchars and use as java keychars.
           */
          unicodeValue = (jchar) (keysym & 0x007F);
          break;
      case XK_Return:
          unicodeValue = (jchar) 0x000a;  /* the unicode char for Linefeed */
          break;
      case XK_Cancel:
          unicodeValue = (jchar) 0x0018;  /* the unicode char for Cancel */
          break;
      default:
          break;
    }

    if (unicodeValue != (jchar)keysym) {
        DTRACE_PRINTLN3("%s originalKeysym=0x%x, keysym=0x%x",
          "In keysymToUnicode:", keysym, unicodeValue);
    }

    return unicodeValue;
}


void
awt_post_java_key_event(JNIEnv *env, jobject peer, jint id,
  jlong when, jint keyCode, jchar keyChar, jint keyLocation, jint state, XEvent * event)
{
    JNU_CallMethodByName(env, NULL, peer, "postKeyEvent", "(IJICIIJI)V", id,
        when, keyCode, keyChar, keyLocation, state, ptr_to_jlong(event), (jint)sizeof(XEvent));
} /* awt_post_java_key_event() */



JNIEXPORT jint JNICALL
Java_sun_awt_X11_XWindow_getAWTKeyCodeForKeySym(JNIEnv *env, jclass clazz, jint keysym) {
    jint keycode = java_awt_event_KeyEvent_VK_UNDEFINED;
    Boolean mapsToUnicodeChar;
    jint keyLocation;
    keysymToAWTKeyCode(keysym, &keycode, &mapsToUnicodeChar, &keyLocation);
    return keycode;
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XWindow_haveCurrentX11InputMethodInstance
(JNIEnv *env, jobject object) {
    /*printf("Java_sun_awt_X11_XWindow_haveCurrentX11InputMethodInstance: %s\n", (currentX11InputMethodInstance==NULL? "NULL":" notnull"));
    */
    return currentX11InputMethodInstance != NULL ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_sun_awt_X11_XWindow_x11inputMethodLookupString
(JNIEnv *env, jobject object, jlong event, jlongArray keysymArray) {
   KeySym keysym = NoSymbol;
   Boolean boo;
   /* keysymArray (and testbuf[]) have dimension 2 because we put there two
    * perhaps different values of keysyms.
    * XXX: not anymore at the moment, but I'll still keep them as arrays
    * for a while.  If in the course of testing we will be satisfied with
    * a current single result from awt_x11inputmethod_lookupString, we'll
    * change this.
    */
   jlong testbuf[2];

   testbuf[1]=0;

   boo = awt_x11inputmethod_lookupString((XKeyPressedEvent*)jlong_to_ptr(event), &keysym);
   testbuf[0] = keysym;

   (*env)->SetLongArrayRegion(env, keysymArray, 0, 2, (jlong *)(testbuf));
   return boo ? JNI_TRUE : JNI_FALSE;
}


extern struct X11GraphicsConfigIDs x11GraphicsConfigIDs;

/* syncTopLevelPos() is necessary to insure that the window manager has in
 * fact moved us to our final position relative to the reParented WM window.
 * We have noted a timing window which our shell has not been moved so we
 * screw up the insets thinking they are 0,0.  Wait (for a limited period of
 * time to let the WM hava a chance to move us
 */
void syncTopLevelPos( Display *d, Window w, XWindowAttributes *winAttr ) {
    int32_t i = 0;
    do {
         XGetWindowAttributes( d, w, winAttr );
         /* Sometimes we get here before the WM has updated the
         ** window data struct with the correct position.  Loop
         ** until we get a non-zero position.
         */
         if ((winAttr->x != 0) || (winAttr->y != 0)) {
             break;
         }
         else {
             /* What we really want here is to sync with the WM,
             ** but there's no explicit way to do this, so we
             ** call XSync for a delay.
             */
             XSync(d, False);
         }
    } while (i++ < 50);
}

JNIEXPORT void JNICALL Java_sun_awt_X11_XWindow_setSizeHints
(JNIEnv *env, jclass clazz, jlong window, jlong x, jlong y, jlong width, jlong height) {
    XSizeHints *size_hints = XAllocSizeHints();
    size_hints->flags = USPosition | PPosition | PSize;
    size_hints->x = (int)x;
    size_hints->y = (int)y;
    size_hints->width = (int)width;
    size_hints->height = (int)height;
    XSetWMNormalHints(awt_display, (Window)window, size_hints);
    XFree((char*)size_hints);
}


JNIEXPORT void JNICALL
Java_sun_awt_X11_XWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
   char *ptr = NULL;
   windowID = (*env)->GetFieldID(env, clazz, "window", "J");
   CHECK_NULL(windowID);
   targetID = (*env)->GetFieldID(env, clazz, "target", "Ljava/awt/Component;");
   CHECK_NULL(targetID);
   graphicsConfigID = (*env)->GetFieldID(env, clazz, "graphicsConfig", "Lsun/awt/X11GraphicsConfig;");
   CHECK_NULL(graphicsConfigID);
   drawStateID = (*env)->GetFieldID(env, clazz, "drawState", "I");
   CHECK_NULL(drawStateID);
   ptr = getenv("_AWT_USE_TYPE4_PATCH");
   if( ptr != NULL && ptr[0] != 0 ) {
       if( strncmp("true", ptr, 4) == 0 ) {
          awt_UseType4Patch = True;
       }else if( strncmp("false", ptr, 5) == 0 ) {
          awt_UseType4Patch = False;
       }
   }
}

JNIEXPORT jint JNICALL
Java_sun_awt_X11_XWindow_getKeySymForAWTKeyCode(JNIEnv* env, jclass clazz, jint keycode) {
    return awt_getX11KeySym(keycode);
}
