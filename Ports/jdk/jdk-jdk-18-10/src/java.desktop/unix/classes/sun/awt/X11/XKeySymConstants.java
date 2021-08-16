/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

public interface XKeySymConstants {
    public static final long XK_VoidSymbol = 0xFFFFFF ; /* void symbol */

    /*
     * TTY Functions, cleverly chosen to map to ascii, for convenience of
     * programming, but could have been arbitrary (at the cost of lookup
     * tables in client code.
     */

    public static final long XK_BackSpace = 0xFF08 ; /* back space, back char */
    public static final long XK_Tab = 0xFF09 ;
    public static final long XK_Linefeed = 0xFF0A ; /* Linefeed, LF */
    public static final long XK_Clear = 0xFF0B ;
    public static final long XK_Return = 0xFF0D ; /* Return, enter */
    public static final long XK_Pause = 0xFF13 ; /* Pause, hold */
    public static final long XK_Scroll_Lock = 0xFF14 ;
    public static final long XK_Sys_Req = 0xFF15 ;
    public static final long XK_Escape = 0xFF1B ;
    public static final long XK_Delete = 0xFFFF ; /* Delete, rubout */



    /* International & multi-key character composition */

    public static final long XK_Multi_key = 0xFF20 ; /* Multi-key character compose */
    public static final long XK_Codeinput = 0xFF37 ;
    public static final long XK_SingleCandidate = 0xFF3C ;
    public static final long XK_MultipleCandidate = 0xFF3D ;
    public static final long XK_PreviousCandidate = 0xFF3E ;

    /* Japanese keyboard support */

    public static final long XK_Kanji = 0xFF21 ; /* Kanji, Kanji convert */
    public static final long XK_Muhenkan = 0xFF22 ; /* Cancel Conversion */
    public static final long XK_Henkan_Mode = 0xFF23 ; /* Start/Stop Conversion */
    public static final long XK_Henkan = 0xFF23 ; /* Alias for Henkan_Mode */
    public static final long XK_Romaji = 0xFF24 ; /* to Romaji */
    public static final long XK_Hiragana = 0xFF25 ; /* to Hiragana */
    public static final long XK_Katakana = 0xFF26 ; /* to Katakana */
    public static final long XK_Hiragana_Katakana = 0xFF27 ; /* Hiragana/Katakana toggle */
    public static final long XK_Zenkaku = 0xFF28 ; /* to Zenkaku */
    public static final long XK_Hankaku = 0xFF29 ; /* to Hankaku */
    public static final long XK_Zenkaku_Hankaku = 0xFF2A ; /* Zenkaku/Hankaku toggle */
    public static final long XK_Touroku = 0xFF2B ; /* Add to Dictionary */
    public static final long XK_Massyo = 0xFF2C ; /* Delete from Dictionary */
    public static final long XK_Kana_Lock = 0xFF2D ; /* Kana Lock */
    public static final long XK_Kana_Shift = 0xFF2E ; /* Kana Shift */
    public static final long XK_Eisu_Shift = 0xFF2F ; /* Alphanumeric Shift */
    public static final long XK_Eisu_toggle = 0xFF30 ; /* Alphanumeric toggle */
    public static final long XK_Kanji_Bangou = 0xFF37 ; /* Codeinput */
    public static final long XK_Zen_Koho = 0xFF3D ; /* Multiple/All Candidate(s) */
    public static final long XK_Mae_Koho = 0xFF3E ; /* Previous Candidate */

    /* 0xFF31 thru 0xFF3F are under XK_KOREAN */

    /* Cursor control & motion */

    public static final long XK_Home = 0xFF50 ;
    public static final long XK_Left = 0xFF51 ; /* Move left, left arrow */
    public static final long XK_Up = 0xFF52 ; /* Move up, up arrow */
    public static final long XK_Right = 0xFF53 ; /* Move right, right arrow */
    public static final long XK_Down = 0xFF54 ; /* Move down, down arrow */
    public static final long XK_Prior = 0xFF55 ; /* Prior, previous */
    public static final long XK_Page_Up = 0xFF55 ;
    public static final long XK_Next = 0xFF56 ; /* Next */
    public static final long XK_Page_Down = 0xFF56 ;
    public static final long XK_End = 0xFF57 ; /* EOL */
    public static final long XK_Begin = 0xFF58 ; /* BOL */


    /* Misc Functions */

    public static final long XK_Select = 0xFF60 ; /* Select, mark */
    public static final long XK_Print = 0xFF61 ;
    public static final long XK_Execute = 0xFF62 ; /* Execute, run, do */
    public static final long XK_Insert = 0xFF63 ; /* Insert, insert here */
    public static final long XK_Undo = 0xFF65 ; /* Undo, oops */
    public static final long XK_Redo = 0xFF66 ; /* redo, again */
    public static final long XK_Menu = 0xFF67 ;
    public static final long XK_Find = 0xFF68 ; /* Find, search */
    public static final long XK_Cancel = 0xFF69 ; /* Cancel, stop, abort, exit */
    public static final long XK_Help = 0xFF6A ; /* Help */
    public static final long XK_Break = 0xFF6B ;
    public static final long XK_Mode_switch = 0xFF7E ; /* Character set switch */
    public static final long XK_script_switch = 0xFF7E ; /* Alias for mode_switch */
    public static final long XK_Num_Lock = 0xFF7F ;

    /* Keypad Functions, keypad numbers cleverly chosen to map to ascii */

    public static final long XK_KP_Space = 0xFF80 ; /* space */
    public static final long XK_KP_Tab = 0xFF89 ;
    public static final long XK_KP_Enter = 0xFF8D ; /* enter */
    public static final long XK_KP_F1 = 0xFF91 ; /* PF1, KP_A, ... */
    public static final long XK_KP_F2 = 0xFF92 ;
    public static final long XK_KP_F3 = 0xFF93 ;
    public static final long XK_KP_F4 = 0xFF94 ;
    public static final long XK_KP_Home = 0xFF95 ;
    public static final long XK_KP_Left = 0xFF96 ;
    public static final long XK_KP_Up = 0xFF97 ;
    public static final long XK_KP_Right = 0xFF98 ;
    public static final long XK_KP_Down = 0xFF99 ;
    public static final long XK_KP_Prior = 0xFF9A ;
    public static final long XK_KP_Page_Up = 0xFF9A ;
    public static final long XK_KP_Next = 0xFF9B ;
    public static final long XK_KP_Page_Down = 0xFF9B ;
    public static final long XK_KP_End = 0xFF9C ;
    public static final long XK_KP_Begin = 0xFF9D ;
    public static final long XK_KP_Insert = 0xFF9E ;
    public static final long XK_KP_Delete = 0xFF9F ;
    public static final long XK_KP_Equal = 0xFFBD ; /* equals */
    public static final long XK_KP_Multiply = 0xFFAA ;
    public static final long XK_KP_Add = 0xFFAB ;
    public static final long XK_KP_Separator = 0xFFAC ; /* separator, often comma */
    public static final long XK_KP_Subtract = 0xFFAD ;
    public static final long XK_KP_Decimal = 0xFFAE ;
    public static final long XK_KP_Divide = 0xFFAF ;

    public static final long XK_KP_0 = 0xFFB0 ;
    public static final long XK_KP_1 = 0xFFB1 ;
    public static final long XK_KP_2 = 0xFFB2 ;
    public static final long XK_KP_3 = 0xFFB3 ;
    public static final long XK_KP_4 = 0xFFB4 ;
    public static final long XK_KP_5 = 0xFFB5 ;
    public static final long XK_KP_6 = 0xFFB6 ;
    public static final long XK_KP_7 = 0xFFB7 ;
    public static final long XK_KP_8 = 0xFFB8 ;
    public static final long XK_KP_9 = 0xFFB9 ;



    /*
     * Auxilliary Functions; note the duplicate definitions for left and right
     * function keys;  Sun keyboards and a few other manufactures have such
     * function key groups on the left and/or right sides of the keyboard.
     * We've not found a keyboard with more than 35 function keys total.
     */

    public static final long XK_F1 = 0xFFBE ;
    public static final long XK_F2 = 0xFFBF ;
    public static final long XK_F3 = 0xFFC0 ;
    public static final long XK_F4 = 0xFFC1 ;
    public static final long XK_F5 = 0xFFC2 ;
    public static final long XK_F6 = 0xFFC3 ;
    public static final long XK_F7 = 0xFFC4 ;
    public static final long XK_F8 = 0xFFC5 ;
    public static final long XK_F9 = 0xFFC6 ;
    public static final long XK_F10 = 0xFFC7 ;
    public static final long XK_F11 = 0xFFC8 ;
    public static final long XK_L1 = 0xFFC8 ;
    public static final long XK_F12 = 0xFFC9 ;
    public static final long XK_L2 = 0xFFC9 ;
    public static final long XK_F13 = 0xFFCA ;
    public static final long XK_L3 = 0xFFCA ;
    public static final long XK_F14 = 0xFFCB ;
    public static final long XK_L4 = 0xFFCB ;
    public static final long XK_F15 = 0xFFCC ;
    public static final long XK_L5 = 0xFFCC ;
    public static final long XK_F16 = 0xFFCD ;
    public static final long XK_L6 = 0xFFCD ;
    public static final long XK_F17 = 0xFFCE ;
    public static final long XK_L7 = 0xFFCE ;
    public static final long XK_F18 = 0xFFCF ;
    public static final long XK_L8 = 0xFFCF ;
    public static final long XK_F19 = 0xFFD0 ;
    public static final long XK_L9 = 0xFFD0 ;
    public static final long XK_F20 = 0xFFD1 ;
    public static final long XK_L10 = 0xFFD1 ;
    public static final long XK_F21 = 0xFFD2 ;
    public static final long XK_R1 = 0xFFD2 ;
    public static final long XK_F22 = 0xFFD3 ;
    public static final long XK_R2 = 0xFFD3 ;
    public static final long XK_F23 = 0xFFD4 ;
    public static final long XK_R3 = 0xFFD4 ;
    public static final long XK_F24 = 0xFFD5 ;
    public static final long XK_R4 = 0xFFD5 ;
    public static final long XK_F25 = 0xFFD6 ;
    public static final long XK_R5 = 0xFFD6 ;
    public static final long XK_F26 = 0xFFD7 ;
    public static final long XK_R6 = 0xFFD7 ;
    public static final long XK_F27 = 0xFFD8 ;
    public static final long XK_R7 = 0xFFD8 ;
    public static final long XK_F28 = 0xFFD9 ;
    public static final long XK_R8 = 0xFFD9 ;
    public static final long XK_F29 = 0xFFDA ;
    public static final long XK_R9 = 0xFFDA ;
    public static final long XK_F30 = 0xFFDB ;
    public static final long XK_R10 = 0xFFDB ;
    public static final long XK_F31 = 0xFFDC ;
    public static final long XK_R11 = 0xFFDC ;
    public static final long XK_F32 = 0xFFDD ;
    public static final long XK_R12 = 0xFFDD ;
    public static final long XK_F33 = 0xFFDE ;
    public static final long XK_R13 = 0xFFDE ;
    public static final long XK_F34 = 0xFFDF ;
    public static final long XK_R14 = 0xFFDF ;
    public static final long XK_F35 = 0xFFE0 ;
    public static final long XK_R15 = 0xFFE0 ;

    /* Modifiers */

    public static final long XK_Shift_L = 0xFFE1 ; /* Left shift */
    public static final long XK_Shift_R = 0xFFE2 ; /* Right shift */
    public static final long XK_Control_L = 0xFFE3 ; /* Left control */
    public static final long XK_Control_R = 0xFFE4 ; /* Right control */
    public static final long XK_Caps_Lock = 0xFFE5 ; /* Caps lock */
    public static final long XK_Shift_Lock = 0xFFE6 ; /* Shift lock */

    public static final long XK_Meta_L = 0xFFE7 ; /* Left meta */
    public static final long XK_Meta_R = 0xFFE8 ; /* Right meta */
    public static final long XK_Alt_L = 0xFFE9 ; /* Left alt */
    public static final long XK_Alt_R = 0xFFEA ; /* Right alt */
    public static final long XK_Super_L = 0xFFEB ; /* Left super */
    public static final long XK_Super_R = 0xFFEC ; /* Right super */
    public static final long XK_Hyper_L = 0xFFED ; /* Left hyper */
    public static final long XK_Hyper_R = 0xFFEE ; /* Right hyper */

    /*
     * ISO 9995 Function and Modifier Keys
     * Byte 3 = 0xFE
     */

    public static final long XK_ISO_Lock = 0xFE01 ;
    public static final long XK_ISO_Level2_Latch = 0xFE02 ;
    public static final long XK_ISO_Level3_Shift = 0xFE03 ;
    public static final long XK_ISO_Level3_Latch = 0xFE04 ;
    public static final long XK_ISO_Level3_Lock = 0xFE05 ;
    public static final long XK_ISO_Group_Shift = 0xFF7E ; /* Alias for mode_switch */
    public static final long XK_ISO_Group_Latch = 0xFE06 ;
    public static final long XK_ISO_Group_Lock = 0xFE07 ;
    public static final long XK_ISO_Next_Group = 0xFE08 ;
    public static final long XK_ISO_Next_Group_Lock = 0xFE09 ;
    public static final long XK_ISO_Prev_Group = 0xFE0A ;
    public static final long XK_ISO_Prev_Group_Lock = 0xFE0B ;
    public static final long XK_ISO_First_Group = 0xFE0C ;
    public static final long XK_ISO_First_Group_Lock = 0xFE0D ;
    public static final long XK_ISO_Last_Group = 0xFE0E ;
    public static final long XK_ISO_Last_Group_Lock = 0xFE0F ;

    public static final long XK_ISO_Left_Tab = 0xFE20 ;
    public static final long XK_ISO_Move_Line_Up = 0xFE21 ;
    public static final long XK_ISO_Move_Line_Down = 0xFE22 ;
    public static final long XK_ISO_Partial_Line_Up = 0xFE23 ;
    public static final long XK_ISO_Partial_Line_Down = 0xFE24 ;
    public static final long XK_ISO_Partial_Space_Left = 0xFE25 ;
    public static final long XK_ISO_Partial_Space_Right = 0xFE26 ;
    public static final long XK_ISO_Set_Margin_Left = 0xFE27 ;
    public static final long XK_ISO_Set_Margin_Right = 0xFE28 ;
    public static final long XK_ISO_Release_Margin_Left = 0xFE29 ;
    public static final long XK_ISO_Release_Margin_Right = 0xFE2A ;
    public static final long XK_ISO_Release_Both_Margins = 0xFE2B ;
    public static final long XK_ISO_Fast_Cursor_Left = 0xFE2C ;
    public static final long XK_ISO_Fast_Cursor_Right = 0xFE2D ;
    public static final long XK_ISO_Fast_Cursor_Up = 0xFE2E ;
    public static final long XK_ISO_Fast_Cursor_Down = 0xFE2F ;
    public static final long XK_ISO_Continuous_Underline = 0xFE30 ;
    public static final long XK_ISO_Discontinuous_Underline = 0xFE31 ;
    public static final long XK_ISO_Emphasize = 0xFE32 ;
    public static final long XK_ISO_Center_Object = 0xFE33 ;
    public static final long XK_ISO_Enter = 0xFE34 ;

    public static final long XK_dead_grave = 0xFE50 ;
    public static final long XK_dead_acute = 0xFE51 ;
    public static final long XK_dead_circumflex = 0xFE52 ;
    public static final long XK_dead_tilde = 0xFE53 ;
    public static final long XK_dead_macron = 0xFE54 ;
    public static final long XK_dead_breve = 0xFE55 ;
    public static final long XK_dead_abovedot = 0xFE56 ;
    public static final long XK_dead_diaeresis = 0xFE57 ;
    public static final long XK_dead_abovering = 0xFE58 ;
    public static final long XK_dead_doubleacute = 0xFE59 ;
    public static final long XK_dead_caron = 0xFE5A ;
    public static final long XK_dead_cedilla = 0xFE5B ;
    public static final long XK_dead_ogonek = 0xFE5C ;
    public static final long XK_dead_iota = 0xFE5D ;
    public static final long XK_dead_voiced_sound = 0xFE5E ;
    public static final long XK_dead_semivoiced_sound = 0xFE5F ;
    public static final long XK_dead_belowdot = 0xFE60 ;

    public static final long XK_First_Virtual_Screen = 0xFED0 ;
    public static final long XK_Prev_Virtual_Screen = 0xFED1 ;
    public static final long XK_Next_Virtual_Screen = 0xFED2 ;
    public static final long XK_Last_Virtual_Screen = 0xFED4 ;
    public static final long XK_Terminate_Server = 0xFED5 ;

    public static final long XK_AccessX_Enable = 0xFE70 ;
    public static final long XK_AccessX_Feedback_Enable = 0xFE71 ;
    public static final long XK_RepeatKeys_Enable = 0xFE72 ;
    public static final long XK_SlowKeys_Enable = 0xFE73 ;
    public static final long XK_BounceKeys_Enable = 0xFE74 ;
    public static final long XK_StickyKeys_Enable = 0xFE75 ;
    public static final long XK_MouseKeys_Enable = 0xFE76 ;
    public static final long XK_MouseKeys_Accel_Enable = 0xFE77 ;
    public static final long XK_Overlay1_Enable = 0xFE78 ;
    public static final long XK_Overlay2_Enable = 0xFE79 ;
    public static final long XK_AudibleBell_Enable = 0xFE7A ;

    public static final long XK_Pointer_Left = 0xFEE0 ;
    public static final long XK_Pointer_Right = 0xFEE1 ;
    public static final long XK_Pointer_Up = 0xFEE2 ;
    public static final long XK_Pointer_Down = 0xFEE3 ;
    public static final long XK_Pointer_UpLeft = 0xFEE4 ;
    public static final long XK_Pointer_UpRight = 0xFEE5 ;
    public static final long XK_Pointer_DownLeft = 0xFEE6 ;
    public static final long XK_Pointer_DownRight = 0xFEE7 ;
    public static final long XK_Pointer_Button_Dflt = 0xFEE8 ;
    public static final long XK_Pointer_Button1 = 0xFEE9 ;
    public static final long XK_Pointer_Button2 = 0xFEEA ;
    public static final long XK_Pointer_Button3 = 0xFEEB ;
    public static final long XK_Pointer_Button4 = 0xFEEC ;
    public static final long XK_Pointer_Button5 = 0xFEED ;
    public static final long XK_Pointer_DblClick_Dflt = 0xFEEE ;
    public static final long XK_Pointer_DblClick1 = 0xFEEF ;
    public static final long XK_Pointer_DblClick2 = 0xFEF0 ;
    public static final long XK_Pointer_DblClick3 = 0xFEF1 ;
    public static final long XK_Pointer_DblClick4 = 0xFEF2 ;
    public static final long XK_Pointer_DblClick5 = 0xFEF3 ;
    public static final long XK_Pointer_Drag_Dflt = 0xFEF4 ;
    public static final long XK_Pointer_Drag1 = 0xFEF5 ;
    public static final long XK_Pointer_Drag2 = 0xFEF6 ;
    public static final long XK_Pointer_Drag3 = 0xFEF7 ;
    public static final long XK_Pointer_Drag4 = 0xFEF8 ;
    public static final long XK_Pointer_Drag5 = 0xFEFD ;

    public static final long XK_Pointer_EnableKeys = 0xFEF9 ;
    public static final long XK_Pointer_Accelerate = 0xFEFA ;
    public static final long XK_Pointer_DfltBtnNext = 0xFEFB ;
    public static final long XK_Pointer_DfltBtnPrev = 0xFEFC ;


    /*
     * 3270 Terminal Keys
     * Byte 3 = 0xFD
     */

    public static final long XK_3270_Duplicate = 0xFD01 ;
    public static final long XK_3270_FieldMark = 0xFD02 ;
    public static final long XK_3270_Right2 = 0xFD03 ;
    public static final long XK_3270_Left2 = 0xFD04 ;
    public static final long XK_3270_BackTab = 0xFD05 ;
    public static final long XK_3270_EraseEOF = 0xFD06 ;
    public static final long XK_3270_EraseInput = 0xFD07 ;
    public static final long XK_3270_Reset = 0xFD08 ;
    public static final long XK_3270_Quit = 0xFD09 ;
    public static final long XK_3270_PA1 = 0xFD0A ;
    public static final long XK_3270_PA2 = 0xFD0B ;
    public static final long XK_3270_PA3 = 0xFD0C ;
    public static final long XK_3270_Test = 0xFD0D ;
    public static final long XK_3270_Attn = 0xFD0E ;
    public static final long XK_3270_CursorBlink = 0xFD0F ;
    public static final long XK_3270_AltCursor = 0xFD10 ;
    public static final long XK_3270_KeyClick = 0xFD11 ;
    public static final long XK_3270_Jump = 0xFD12 ;
    public static final long XK_3270_Ident = 0xFD13 ;
    public static final long XK_3270_Rule = 0xFD14 ;
    public static final long XK_3270_Copy = 0xFD15 ;
    public static final long XK_3270_Play = 0xFD16 ;
    public static final long XK_3270_Setup = 0xFD17 ;
    public static final long XK_3270_Record = 0xFD18 ;
    public static final long XK_3270_ChangeScreen = 0xFD19 ;
    public static final long XK_3270_DeleteWord = 0xFD1A ;
    public static final long XK_3270_ExSelect = 0xFD1B ;
    public static final long XK_3270_CursorSelect = 0xFD1C ;
    public static final long XK_3270_PrintScreen = 0xFD1D ;
    public static final long XK_3270_Enter = 0xFD1E ;

    /*
     *  Latin 1
     *  Byte 3 = 0
     */
    public static final long XK_space = 0x020 ;
    public static final long XK_exclam = 0x021 ;
    public static final long XK_quotedbl = 0x022 ;
    public static final long XK_numbersign = 0x023 ;
    public static final long XK_dollar = 0x024 ;
    public static final long XK_percent = 0x025 ;
    public static final long XK_ampersand = 0x026 ;
    public static final long XK_apostrophe = 0x027 ;
    public static final long XK_quoteright = 0x027 ; /* deprecated */
    public static final long XK_parenleft = 0x028 ;
    public static final long XK_parenright = 0x029 ;
    public static final long XK_asterisk = 0x02a ;
    public static final long XK_plus = 0x02b ;
    public static final long XK_comma = 0x02c ;
    public static final long XK_minus = 0x02d ;
    public static final long XK_period = 0x02e ;
    public static final long XK_slash = 0x02f ;
    public static final long XK_0 = 0x030 ;
    public static final long XK_1 = 0x031 ;
    public static final long XK_2 = 0x032 ;
    public static final long XK_3 = 0x033 ;
    public static final long XK_4 = 0x034 ;
    public static final long XK_5 = 0x035 ;
    public static final long XK_6 = 0x036 ;
    public static final long XK_7 = 0x037 ;
    public static final long XK_8 = 0x038 ;
    public static final long XK_9 = 0x039 ;
    public static final long XK_colon = 0x03a ;
    public static final long XK_semicolon = 0x03b ;
    public static final long XK_less = 0x03c ;
    public static final long XK_equal = 0x03d ;
    public static final long XK_greater = 0x03e ;
    public static final long XK_question = 0x03f ;
    public static final long XK_at = 0x040 ;
    public static final long XK_A = 0x041 ;
    public static final long XK_B = 0x042 ;
    public static final long XK_C = 0x043 ;
    public static final long XK_D = 0x044 ;
    public static final long XK_E = 0x045 ;
    public static final long XK_F = 0x046 ;
    public static final long XK_G = 0x047 ;
    public static final long XK_H = 0x048 ;
    public static final long XK_I = 0x049 ;
    public static final long XK_J = 0x04a ;
    public static final long XK_K = 0x04b ;
    public static final long XK_L = 0x04c ;
    public static final long XK_M = 0x04d ;
    public static final long XK_N = 0x04e ;
    public static final long XK_O = 0x04f ;
    public static final long XK_P = 0x050 ;
    public static final long XK_Q = 0x051 ;
    public static final long XK_R = 0x052 ;
    public static final long XK_S = 0x053 ;
    public static final long XK_T = 0x054 ;
    public static final long XK_U = 0x055 ;
    public static final long XK_V = 0x056 ;
    public static final long XK_W = 0x057 ;
    public static final long XK_X = 0x058 ;
    public static final long XK_Y = 0x059 ;
    public static final long XK_Z = 0x05a ;
    public static final long XK_bracketleft = 0x05b ;
    public static final long XK_backslash = 0x05c ;
    public static final long XK_bracketright = 0x05d ;
    public static final long XK_asciicircum = 0x05e ;
    public static final long XK_underscore = 0x05f ;
    public static final long XK_grave = 0x060 ;
    public static final long XK_quoteleft = 0x060 ; /* deprecated */
    public static final long XK_a = 0x061 ;
    public static final long XK_b = 0x062 ;
    public static final long XK_c = 0x063 ;
    public static final long XK_d = 0x064 ;
    public static final long XK_e = 0x065 ;
    public static final long XK_f = 0x066 ;
    public static final long XK_g = 0x067 ;
    public static final long XK_h = 0x068 ;
    public static final long XK_i = 0x069 ;
    public static final long XK_j = 0x06a ;
    public static final long XK_k = 0x06b ;
    public static final long XK_l = 0x06c ;
    public static final long XK_m = 0x06d ;
    public static final long XK_n = 0x06e ;
    public static final long XK_o = 0x06f ;
    public static final long XK_p = 0x070 ;
    public static final long XK_q = 0x071 ;
    public static final long XK_r = 0x072 ;
    public static final long XK_s = 0x073 ;
    public static final long XK_t = 0x074 ;
    public static final long XK_u = 0x075 ;
    public static final long XK_v = 0x076 ;
    public static final long XK_w = 0x077 ;
    public static final long XK_x = 0x078 ;
    public static final long XK_y = 0x079 ;
    public static final long XK_z = 0x07a ;
    public static final long XK_braceleft = 0x07b ;
    public static final long XK_bar = 0x07c ;
    public static final long XK_braceright = 0x07d ;
    public static final long XK_asciitilde = 0x07e ;

    public static final long XK_nobreakspace = 0x0a0 ;
    public static final long XK_exclamdown = 0x0a1 ;
    public static final long XK_cent = 0x0a2 ;
    public static final long XK_sterling = 0x0a3 ;
    public static final long XK_currency = 0x0a4 ;
    public static final long XK_yen = 0x0a5 ;
    public static final long XK_brokenbar = 0x0a6 ;
    public static final long XK_section = 0x0a7 ;
    public static final long XK_diaeresis = 0x0a8 ;
    public static final long XK_copyright = 0x0a9 ;
    public static final long XK_ordfeminine = 0x0aa ;
    public static final long XK_guillemotleft = 0x0ab ; /* left angle quotation mark */
    public static final long XK_notsign = 0x0ac ;
    public static final long XK_hyphen = 0x0ad ;
    public static final long XK_registered = 0x0ae ;
    public static final long XK_macron = 0x0af ;
    public static final long XK_degree = 0x0b0 ;
    public static final long XK_plusminus = 0x0b1 ;
    public static final long XK_twosuperior = 0x0b2 ;
    public static final long XK_threesuperior = 0x0b3 ;
    public static final long XK_acute = 0x0b4 ;
    public static final long XK_mu = 0x0b5 ;
    public static final long XK_paragraph = 0x0b6 ;
    public static final long XK_periodcentered = 0x0b7 ;
    public static final long XK_cedilla = 0x0b8 ;
    public static final long XK_onesuperior = 0x0b9 ;
    public static final long XK_masculine = 0x0ba ;
    public static final long XK_guillemotright = 0x0bb ; /* right angle quotation mark */
    public static final long XK_onequarter = 0x0bc ;
    public static final long XK_onehalf = 0x0bd ;
    public static final long XK_threequarters = 0x0be ;
    public static final long XK_questiondown = 0x0bf ;
    public static final long XK_Agrave = 0x0c0 ;
    public static final long XK_Aacute = 0x0c1 ;
    public static final long XK_Acircumflex = 0x0c2 ;
    public static final long XK_Atilde = 0x0c3 ;
    public static final long XK_Adiaeresis = 0x0c4 ;
    public static final long XK_Aring = 0x0c5 ;
    public static final long XK_AE = 0x0c6 ;
    public static final long XK_Ccedilla = 0x0c7 ;
    public static final long XK_Egrave = 0x0c8 ;
    public static final long XK_Eacute = 0x0c9 ;
    public static final long XK_Ecircumflex = 0x0ca ;
    public static final long XK_Ediaeresis = 0x0cb ;
    public static final long XK_Igrave = 0x0cc ;
    public static final long XK_Iacute = 0x0cd ;
    public static final long XK_Icircumflex = 0x0ce ;
    public static final long XK_Idiaeresis = 0x0cf ;
    public static final long XK_ETH = 0x0d0 ;
    public static final long XK_Eth = 0x0d0 ; /* deprecated */
    public static final long XK_Ntilde = 0x0d1 ;
    public static final long XK_Ograve = 0x0d2 ;
    public static final long XK_Oacute = 0x0d3 ;
    public static final long XK_Ocircumflex = 0x0d4 ;
    public static final long XK_Otilde = 0x0d5 ;
    public static final long XK_Odiaeresis = 0x0d6 ;
    public static final long XK_multiply = 0x0d7 ;
    public static final long XK_Ooblique = 0x0d8 ;
    public static final long XK_Ugrave = 0x0d9 ;
    public static final long XK_Uacute = 0x0da ;
    public static final long XK_Ucircumflex = 0x0db ;
    public static final long XK_Udiaeresis = 0x0dc ;
    public static final long XK_Yacute = 0x0dd ;
    public static final long XK_THORN = 0x0de ;
    public static final long XK_Thorn = 0x0de ; /* deprecated */
    public static final long XK_ssharp = 0x0df ;
    public static final long XK_agrave = 0x0e0 ;
    public static final long XK_aacute = 0x0e1 ;
    public static final long XK_acircumflex = 0x0e2 ;
    public static final long XK_atilde = 0x0e3 ;
    public static final long XK_adiaeresis = 0x0e4 ;
    public static final long XK_aring = 0x0e5 ;
    public static final long XK_ae = 0x0e6 ;
    public static final long XK_ccedilla = 0x0e7 ;
    public static final long XK_egrave = 0x0e8 ;
    public static final long XK_eacute = 0x0e9 ;
    public static final long XK_ecircumflex = 0x0ea ;
    public static final long XK_ediaeresis = 0x0eb ;
    public static final long XK_igrave = 0x0ec ;
    public static final long XK_iacute = 0x0ed ;
    public static final long XK_icircumflex = 0x0ee ;
    public static final long XK_idiaeresis = 0x0ef ;
    public static final long XK_eth = 0x0f0 ;
    public static final long XK_ntilde = 0x0f1 ;
    public static final long XK_ograve = 0x0f2 ;
    public static final long XK_oacute = 0x0f3 ;
    public static final long XK_ocircumflex = 0x0f4 ;
    public static final long XK_otilde = 0x0f5 ;
    public static final long XK_odiaeresis = 0x0f6 ;
    public static final long XK_division = 0x0f7 ;
    public static final long XK_oslash = 0x0f8 ;
    public static final long XK_ugrave = 0x0f9 ;
    public static final long XK_uacute = 0x0fa ;
    public static final long XK_ucircumflex = 0x0fb ;
    public static final long XK_udiaeresis = 0x0fc ;
    public static final long XK_yacute = 0x0fd ;
    public static final long XK_thorn = 0x0fe ;
    public static final long XK_ydiaeresis = 0x0ff ;

    /*
     *   Latin 2
     *   Byte 3 = 1
     */

    public static final long XK_Aogonek = 0x1a1 ;
    public static final long XK_breve = 0x1a2 ;
    public static final long XK_Lstroke = 0x1a3 ;
    public static final long XK_Lcaron = 0x1a5 ;
    public static final long XK_Sacute = 0x1a6 ;
    public static final long XK_Scaron = 0x1a9 ;
    public static final long XK_Scedilla = 0x1aa ;
    public static final long XK_Tcaron = 0x1ab ;
    public static final long XK_Zacute = 0x1ac ;
    public static final long XK_Zcaron = 0x1ae ;
    public static final long XK_Zabovedot = 0x1af ;
    public static final long XK_aogonek = 0x1b1 ;
    public static final long XK_ogonek = 0x1b2 ;
    public static final long XK_lstroke = 0x1b3 ;
    public static final long XK_lcaron = 0x1b5 ;
    public static final long XK_sacute = 0x1b6 ;
    public static final long XK_caron = 0x1b7 ;
    public static final long XK_scaron = 0x1b9 ;
    public static final long XK_scedilla = 0x1ba ;
    public static final long XK_tcaron = 0x1bb ;
    public static final long XK_zacute = 0x1bc ;
    public static final long XK_doubleacute = 0x1bd ;
    public static final long XK_zcaron = 0x1be ;
    public static final long XK_zabovedot = 0x1bf ;
    public static final long XK_Racute = 0x1c0 ;
    public static final long XK_Abreve = 0x1c3 ;
    public static final long XK_Lacute = 0x1c5 ;
    public static final long XK_Cacute = 0x1c6 ;
    public static final long XK_Ccaron = 0x1c8 ;
    public static final long XK_Eogonek = 0x1ca ;
    public static final long XK_Ecaron = 0x1cc ;
    public static final long XK_Dcaron = 0x1cf ;
    public static final long XK_Dstroke = 0x1d0 ;
    public static final long XK_Nacute = 0x1d1 ;
    public static final long XK_Ncaron = 0x1d2 ;
    public static final long XK_Odoubleacute = 0x1d5 ;
    public static final long XK_Rcaron = 0x1d8 ;
    public static final long XK_Uring = 0x1d9 ;
    public static final long XK_Udoubleacute = 0x1db ;
    public static final long XK_Tcedilla = 0x1de ;
    public static final long XK_racute = 0x1e0 ;
    public static final long XK_abreve = 0x1e3 ;
    public static final long XK_lacute = 0x1e5 ;
    public static final long XK_cacute = 0x1e6 ;
    public static final long XK_ccaron = 0x1e8 ;
    public static final long XK_eogonek = 0x1ea ;
    public static final long XK_ecaron = 0x1ec ;
    public static final long XK_dcaron = 0x1ef ;
    public static final long XK_dstroke = 0x1f0 ;
    public static final long XK_nacute = 0x1f1 ;
    public static final long XK_ncaron = 0x1f2 ;
    public static final long XK_odoubleacute = 0x1f5 ;
    public static final long XK_udoubleacute = 0x1fb ;
    public static final long XK_rcaron = 0x1f8 ;
    public static final long XK_uring = 0x1f9 ;
    public static final long XK_tcedilla = 0x1fe ;
    public static final long XK_abovedot = 0x1ff ;

    /*
     *   Latin 3
     *   Byte 3 = 2
     */

    public static final long XK_Hstroke = 0x2a1 ;
    public static final long XK_Hcircumflex = 0x2a6 ;
    public static final long XK_Iabovedot = 0x2a9 ;
    public static final long XK_Gbreve = 0x2ab ;
    public static final long XK_Jcircumflex = 0x2ac ;
    public static final long XK_hstroke = 0x2b1 ;
    public static final long XK_hcircumflex = 0x2b6 ;
    public static final long XK_idotless = 0x2b9 ;
    public static final long XK_gbreve = 0x2bb ;
    public static final long XK_jcircumflex = 0x2bc ;
    public static final long XK_Cabovedot = 0x2c5 ;
    public static final long XK_Ccircumflex = 0x2c6 ;
    public static final long XK_Gabovedot = 0x2d5 ;
    public static final long XK_Gcircumflex = 0x2d8 ;
    public static final long XK_Ubreve = 0x2dd ;
    public static final long XK_Scircumflex = 0x2de ;
    public static final long XK_cabovedot = 0x2e5 ;
    public static final long XK_ccircumflex = 0x2e6 ;
    public static final long XK_gabovedot = 0x2f5 ;
    public static final long XK_gcircumflex = 0x2f8 ;
    public static final long XK_ubreve = 0x2fd ;
    public static final long XK_scircumflex = 0x2fe ;


    /*
     *   Latin 4
     *   Byte 3 = 3
     */

    public static final long XK_kra = 0x3a2 ;
    public static final long XK_kappa = 0x3a2 ; /* deprecated */
    public static final long XK_Rcedilla = 0x3a3 ;
    public static final long XK_Itilde = 0x3a5 ;
    public static final long XK_Lcedilla = 0x3a6 ;
    public static final long XK_Emacron = 0x3aa ;
    public static final long XK_Gcedilla = 0x3ab ;
    public static final long XK_Tslash = 0x3ac ;
    public static final long XK_rcedilla = 0x3b3 ;
    public static final long XK_itilde = 0x3b5 ;
    public static final long XK_lcedilla = 0x3b6 ;
    public static final long XK_emacron = 0x3ba ;
    public static final long XK_gcedilla = 0x3bb ;
    public static final long XK_tslash = 0x3bc ;
    public static final long XK_ENG = 0x3bd ;
    public static final long XK_eng = 0x3bf ;
    public static final long XK_Amacron = 0x3c0 ;
    public static final long XK_Iogonek = 0x3c7 ;
    public static final long XK_Eabovedot = 0x3cc ;
    public static final long XK_Imacron = 0x3cf ;
    public static final long XK_Ncedilla = 0x3d1 ;
    public static final long XK_Omacron = 0x3d2 ;
    public static final long XK_Kcedilla = 0x3d3 ;
    public static final long XK_Uogonek = 0x3d9 ;
    public static final long XK_Utilde = 0x3dd ;
    public static final long XK_Umacron = 0x3de ;
    public static final long XK_amacron = 0x3e0 ;
    public static final long XK_iogonek = 0x3e7 ;
    public static final long XK_eabovedot = 0x3ec ;
    public static final long XK_imacron = 0x3ef ;
    public static final long XK_ncedilla = 0x3f1 ;
    public static final long XK_omacron = 0x3f2 ;
    public static final long XK_kcedilla = 0x3f3 ;
    public static final long XK_uogonek = 0x3f9 ;
    public static final long XK_utilde = 0x3fd ;
    public static final long XK_umacron = 0x3fe ;

    /*
     * Latin-9 (a.k.a. Latin-0)
     * Byte 3 = 19
     */

    public static final long XK_OE = 0x13bc ;
    public static final long XK_oe = 0x13bd ;
    public static final long XK_Ydiaeresis = 0x13be ;

    /*
     * Katakana
     * Byte 3 = 4
     */

    public static final long XK_overline = 0x47e ;
    public static final long XK_kana_fullstop = 0x4a1 ;
    public static final long XK_kana_openingbracket = 0x4a2 ;
    public static final long XK_kana_closingbracket = 0x4a3 ;
    public static final long XK_kana_comma = 0x4a4 ;
    public static final long XK_kana_conjunctive = 0x4a5 ;
    public static final long XK_kana_middledot = 0x4a5 ; /* deprecated */
    public static final long XK_kana_WO = 0x4a6 ;
    public static final long XK_kana_a = 0x4a7 ;
    public static final long XK_kana_i = 0x4a8 ;
    public static final long XK_kana_u = 0x4a9 ;
    public static final long XK_kana_e = 0x4aa ;
    public static final long XK_kana_o = 0x4ab ;
    public static final long XK_kana_ya = 0x4ac ;
    public static final long XK_kana_yu = 0x4ad ;
    public static final long XK_kana_yo = 0x4ae ;
    public static final long XK_kana_tsu = 0x4af ;
    public static final long XK_kana_tu = 0x4af ; /* deprecated */
    public static final long XK_prolongedsound = 0x4b0 ;
    public static final long XK_kana_A = 0x4b1 ;
    public static final long XK_kana_I = 0x4b2 ;
    public static final long XK_kana_U = 0x4b3 ;
    public static final long XK_kana_E = 0x4b4 ;
    public static final long XK_kana_O = 0x4b5 ;
    public static final long XK_kana_KA = 0x4b6 ;
    public static final long XK_kana_KI = 0x4b7 ;
    public static final long XK_kana_KU = 0x4b8 ;
    public static final long XK_kana_KE = 0x4b9 ;
    public static final long XK_kana_KO = 0x4ba ;
    public static final long XK_kana_SA = 0x4bb ;
    public static final long XK_kana_SHI = 0x4bc ;
    public static final long XK_kana_SU = 0x4bd ;
    public static final long XK_kana_SE = 0x4be ;
    public static final long XK_kana_SO = 0x4bf ;
    public static final long XK_kana_TA = 0x4c0 ;
    public static final long XK_kana_CHI = 0x4c1 ;
    public static final long XK_kana_TI = 0x4c1 ; /* deprecated */
    public static final long XK_kana_TSU = 0x4c2 ;
    public static final long XK_kana_TU = 0x4c2 ; /* deprecated */
    public static final long XK_kana_TE = 0x4c3 ;
    public static final long XK_kana_TO = 0x4c4 ;
    public static final long XK_kana_NA = 0x4c5 ;
    public static final long XK_kana_NI = 0x4c6 ;
    public static final long XK_kana_NU = 0x4c7 ;
    public static final long XK_kana_NE = 0x4c8 ;
    public static final long XK_kana_NO = 0x4c9 ;
    public static final long XK_kana_HA = 0x4ca ;
    public static final long XK_kana_HI = 0x4cb ;
    public static final long XK_kana_FU = 0x4cc ;
    public static final long XK_kana_HU = 0x4cc ; /* deprecated */
    public static final long XK_kana_HE = 0x4cd ;
    public static final long XK_kana_HO = 0x4ce ;
    public static final long XK_kana_MA = 0x4cf ;
    public static final long XK_kana_MI = 0x4d0 ;
    public static final long XK_kana_MU = 0x4d1 ;
    public static final long XK_kana_ME = 0x4d2 ;
    public static final long XK_kana_MO = 0x4d3 ;
    public static final long XK_kana_YA = 0x4d4 ;
    public static final long XK_kana_YU = 0x4d5 ;
    public static final long XK_kana_YO = 0x4d6 ;
    public static final long XK_kana_RA = 0x4d7 ;
    public static final long XK_kana_RI = 0x4d8 ;
    public static final long XK_kana_RU = 0x4d9 ;
    public static final long XK_kana_RE = 0x4da ;
    public static final long XK_kana_RO = 0x4db ;
    public static final long XK_kana_WA = 0x4dc ;
    public static final long XK_kana_N = 0x4dd ;
    public static final long XK_voicedsound = 0x4de ;
    public static final long XK_semivoicedsound = 0x4df ;
    public static final long XK_kana_switch = 0xFF7E ; /* Alias for mode_switch */

    /*
     *  Arabic
     *  Byte 3 = 5
     */

    public static final long XK_Arabic_comma = 0x5ac ;
    public static final long XK_Arabic_semicolon = 0x5bb ;
    public static final long XK_Arabic_question_mark = 0x5bf ;
    public static final long XK_Arabic_hamza = 0x5c1 ;
    public static final long XK_Arabic_maddaonalef = 0x5c2 ;
    public static final long XK_Arabic_hamzaonalef = 0x5c3 ;
    public static final long XK_Arabic_hamzaonwaw = 0x5c4 ;
    public static final long XK_Arabic_hamzaunderalef = 0x5c5 ;
    public static final long XK_Arabic_hamzaonyeh = 0x5c6 ;
    public static final long XK_Arabic_alef = 0x5c7 ;
    public static final long XK_Arabic_beh = 0x5c8 ;
    public static final long XK_Arabic_tehmarbuta = 0x5c9 ;
    public static final long XK_Arabic_teh = 0x5ca ;
    public static final long XK_Arabic_theh = 0x5cb ;
    public static final long XK_Arabic_jeem = 0x5cc ;
    public static final long XK_Arabic_hah = 0x5cd ;
    public static final long XK_Arabic_khah = 0x5ce ;
    public static final long XK_Arabic_dal = 0x5cf ;
    public static final long XK_Arabic_thal = 0x5d0 ;
    public static final long XK_Arabic_ra = 0x5d1 ;
    public static final long XK_Arabic_zain = 0x5d2 ;
    public static final long XK_Arabic_seen = 0x5d3 ;
    public static final long XK_Arabic_sheen = 0x5d4 ;
    public static final long XK_Arabic_sad = 0x5d5 ;
    public static final long XK_Arabic_dad = 0x5d6 ;
    public static final long XK_Arabic_tah = 0x5d7 ;
    public static final long XK_Arabic_zah = 0x5d8 ;
    public static final long XK_Arabic_ain = 0x5d9 ;
    public static final long XK_Arabic_ghain = 0x5da ;
    public static final long XK_Arabic_tatweel = 0x5e0 ;
    public static final long XK_Arabic_feh = 0x5e1 ;
    public static final long XK_Arabic_qaf = 0x5e2 ;
    public static final long XK_Arabic_kaf = 0x5e3 ;
    public static final long XK_Arabic_lam = 0x5e4 ;
    public static final long XK_Arabic_meem = 0x5e5 ;
    public static final long XK_Arabic_noon = 0x5e6 ;
    public static final long XK_Arabic_ha = 0x5e7 ;
    public static final long XK_Arabic_heh = 0x5e7 ; /* deprecated */
    public static final long XK_Arabic_waw = 0x5e8 ;
    public static final long XK_Arabic_alefmaksura = 0x5e9 ;
    public static final long XK_Arabic_yeh = 0x5ea ;
    public static final long XK_Arabic_fathatan = 0x5eb ;
    public static final long XK_Arabic_dammatan = 0x5ec ;
    public static final long XK_Arabic_kasratan = 0x5ed ;
    public static final long XK_Arabic_fatha = 0x5ee ;
    public static final long XK_Arabic_damma = 0x5ef ;
    public static final long XK_Arabic_kasra = 0x5f0 ;
    public static final long XK_Arabic_shadda = 0x5f1 ;
    public static final long XK_Arabic_sukun = 0x5f2 ;
    public static final long XK_Arabic_switch = 0xFF7E ; /* Alias for mode_switch */

    /*
     * Cyrillic
     * Byte 3 = 6
     */
    public static final long XK_Serbian_dje = 0x6a1 ;
    public static final long XK_Macedonia_gje = 0x6a2 ;
    public static final long XK_Cyrillic_io = 0x6a3 ;
    public static final long XK_Ukrainian_ie = 0x6a4 ;
    public static final long XK_Ukranian_je = 0x6a4 ; /* deprecated */
    public static final long XK_Macedonia_dse = 0x6a5 ;
    public static final long XK_Ukrainian_i = 0x6a6 ;
    public static final long XK_Ukranian_i = 0x6a6 ; /* deprecated */
    public static final long XK_Ukrainian_yi = 0x6a7 ;
    public static final long XK_Ukranian_yi = 0x6a7 ; /* deprecated */
    public static final long XK_Cyrillic_je = 0x6a8 ;
    public static final long XK_Serbian_je = 0x6a8 ; /* deprecated */
    public static final long XK_Cyrillic_lje = 0x6a9 ;
    public static final long XK_Serbian_lje = 0x6a9 ; /* deprecated */
    public static final long XK_Cyrillic_nje = 0x6aa ;
    public static final long XK_Serbian_nje = 0x6aa ; /* deprecated */
    public static final long XK_Serbian_tshe = 0x6ab ;
    public static final long XK_Macedonia_kje = 0x6ac ;
    public static final long XK_Byelorussian_shortu = 0x6ae ;
    public static final long XK_Cyrillic_dzhe = 0x6af ;
    public static final long XK_Serbian_dze = 0x6af ; /* deprecated */
    public static final long XK_numerosign = 0x6b0 ;
    public static final long XK_Serbian_DJE = 0x6b1 ;
    public static final long XK_Macedonia_GJE = 0x6b2 ;
    public static final long XK_Cyrillic_IO = 0x6b3 ;
    public static final long XK_Ukrainian_IE = 0x6b4 ;
    public static final long XK_Ukranian_JE = 0x6b4 ; /* deprecated */
    public static final long XK_Macedonia_DSE = 0x6b5 ;
    public static final long XK_Ukrainian_I = 0x6b6 ;
    public static final long XK_Ukranian_I = 0x6b6 ; /* deprecated */
    public static final long XK_Ukrainian_YI = 0x6b7 ;
    public static final long XK_Ukranian_YI = 0x6b7 ; /* deprecated */
    public static final long XK_Cyrillic_JE = 0x6b8 ;
    public static final long XK_Serbian_JE = 0x6b8 ; /* deprecated */
    public static final long XK_Cyrillic_LJE = 0x6b9 ;
    public static final long XK_Serbian_LJE = 0x6b9 ; /* deprecated */
    public static final long XK_Cyrillic_NJE = 0x6ba ;
    public static final long XK_Serbian_NJE = 0x6ba ; /* deprecated */
    public static final long XK_Serbian_TSHE = 0x6bb ;
    public static final long XK_Macedonia_KJE = 0x6bc ;
    public static final long XK_Byelorussian_SHORTU = 0x6be ;
    public static final long XK_Cyrillic_DZHE = 0x6bf ;
    public static final long XK_Serbian_DZE = 0x6bf ; /* deprecated */
    public static final long XK_Cyrillic_yu = 0x6c0 ;
    public static final long XK_Cyrillic_a = 0x6c1 ;
    public static final long XK_Cyrillic_be = 0x6c2 ;
    public static final long XK_Cyrillic_tse = 0x6c3 ;
    public static final long XK_Cyrillic_de = 0x6c4 ;
    public static final long XK_Cyrillic_ie = 0x6c5 ;
    public static final long XK_Cyrillic_ef = 0x6c6 ;
    public static final long XK_Cyrillic_ghe = 0x6c7 ;
    public static final long XK_Cyrillic_ha = 0x6c8 ;
    public static final long XK_Cyrillic_i = 0x6c9 ;
    public static final long XK_Cyrillic_shorti = 0x6ca ;
    public static final long XK_Cyrillic_ka = 0x6cb ;
    public static final long XK_Cyrillic_el = 0x6cc ;
    public static final long XK_Cyrillic_em = 0x6cd ;
    public static final long XK_Cyrillic_en = 0x6ce ;
    public static final long XK_Cyrillic_o = 0x6cf ;
    public static final long XK_Cyrillic_pe = 0x6d0 ;
    public static final long XK_Cyrillic_ya = 0x6d1 ;
    public static final long XK_Cyrillic_er = 0x6d2 ;
    public static final long XK_Cyrillic_es = 0x6d3 ;
    public static final long XK_Cyrillic_te = 0x6d4 ;
    public static final long XK_Cyrillic_u = 0x6d5 ;
    public static final long XK_Cyrillic_zhe = 0x6d6 ;
    public static final long XK_Cyrillic_ve = 0x6d7 ;
    public static final long XK_Cyrillic_softsign = 0x6d8 ;
    public static final long XK_Cyrillic_yeru = 0x6d9 ;
    public static final long XK_Cyrillic_ze = 0x6da ;
    public static final long XK_Cyrillic_sha = 0x6db ;
    public static final long XK_Cyrillic_e = 0x6dc ;
    public static final long XK_Cyrillic_shcha = 0x6dd ;
    public static final long XK_Cyrillic_che = 0x6de ;
    public static final long XK_Cyrillic_hardsign = 0x6df ;
    public static final long XK_Cyrillic_YU = 0x6e0 ;
    public static final long XK_Cyrillic_A = 0x6e1 ;
    public static final long XK_Cyrillic_BE = 0x6e2 ;
    public static final long XK_Cyrillic_TSE = 0x6e3 ;
    public static final long XK_Cyrillic_DE = 0x6e4 ;
    public static final long XK_Cyrillic_IE = 0x6e5 ;
    public static final long XK_Cyrillic_EF = 0x6e6 ;
    public static final long XK_Cyrillic_GHE = 0x6e7 ;
    public static final long XK_Cyrillic_HA = 0x6e8 ;
    public static final long XK_Cyrillic_I = 0x6e9 ;
    public static final long XK_Cyrillic_SHORTI = 0x6ea ;
    public static final long XK_Cyrillic_KA = 0x6eb ;
    public static final long XK_Cyrillic_EL = 0x6ec ;
    public static final long XK_Cyrillic_EM = 0x6ed ;
    public static final long XK_Cyrillic_EN = 0x6ee ;
    public static final long XK_Cyrillic_O = 0x6ef ;
    public static final long XK_Cyrillic_PE = 0x6f0 ;
    public static final long XK_Cyrillic_YA = 0x6f1 ;
    public static final long XK_Cyrillic_ER = 0x6f2 ;
    public static final long XK_Cyrillic_ES = 0x6f3 ;
    public static final long XK_Cyrillic_TE = 0x6f4 ;
    public static final long XK_Cyrillic_U = 0x6f5 ;
    public static final long XK_Cyrillic_ZHE = 0x6f6 ;
    public static final long XK_Cyrillic_VE = 0x6f7 ;
    public static final long XK_Cyrillic_SOFTSIGN = 0x6f8 ;
    public static final long XK_Cyrillic_YERU = 0x6f9 ;
    public static final long XK_Cyrillic_ZE = 0x6fa ;
    public static final long XK_Cyrillic_SHA = 0x6fb ;
    public static final long XK_Cyrillic_E = 0x6fc ;
    public static final long XK_Cyrillic_SHCHA = 0x6fd ;
    public static final long XK_Cyrillic_CHE = 0x6fe ;
    public static final long XK_Cyrillic_HARDSIGN = 0x6ff ;

    /*
     * Greek
     * Byte 3 = 7
     */

    public static final long XK_Greek_ALPHAaccent = 0x7a1 ;
    public static final long XK_Greek_EPSILONaccent = 0x7a2 ;
    public static final long XK_Greek_ETAaccent = 0x7a3 ;
    public static final long XK_Greek_IOTAaccent = 0x7a4 ;
    public static final long XK_Greek_IOTAdiaeresis = 0x7a5 ;
    public static final long XK_Greek_OMICRONaccent = 0x7a7 ;
    public static final long XK_Greek_UPSILONaccent = 0x7a8 ;
    public static final long XK_Greek_UPSILONdieresis = 0x7a9 ;
    public static final long XK_Greek_OMEGAaccent = 0x7ab ;
    public static final long XK_Greek_accentdieresis = 0x7ae ;
    public static final long XK_Greek_horizbar = 0x7af ;
    public static final long XK_Greek_alphaaccent = 0x7b1 ;
    public static final long XK_Greek_epsilonaccent = 0x7b2 ;
    public static final long XK_Greek_etaaccent = 0x7b3 ;
    public static final long XK_Greek_iotaaccent = 0x7b4 ;
    public static final long XK_Greek_iotadieresis = 0x7b5 ;
    public static final long XK_Greek_iotaaccentdieresis = 0x7b6 ;
    public static final long XK_Greek_omicronaccent = 0x7b7 ;
    public static final long XK_Greek_upsilonaccent = 0x7b8 ;
    public static final long XK_Greek_upsilondieresis = 0x7b9 ;
    public static final long XK_Greek_upsilonaccentdieresis = 0x7ba ;
    public static final long XK_Greek_omegaaccent = 0x7bb ;
    public static final long XK_Greek_ALPHA = 0x7c1 ;
    public static final long XK_Greek_BETA = 0x7c2 ;
    public static final long XK_Greek_GAMMA = 0x7c3 ;
    public static final long XK_Greek_DELTA = 0x7c4 ;
    public static final long XK_Greek_EPSILON = 0x7c5 ;
    public static final long XK_Greek_ZETA = 0x7c6 ;
    public static final long XK_Greek_ETA = 0x7c7 ;
    public static final long XK_Greek_THETA = 0x7c8 ;
    public static final long XK_Greek_IOTA = 0x7c9 ;
    public static final long XK_Greek_KAPPA = 0x7ca ;
    public static final long XK_Greek_LAMDA = 0x7cb ;
    public static final long XK_Greek_LAMBDA = 0x7cb ;
    public static final long XK_Greek_MU = 0x7cc ;
    public static final long XK_Greek_NU = 0x7cd ;
    public static final long XK_Greek_XI = 0x7ce ;
    public static final long XK_Greek_OMICRON = 0x7cf ;
    public static final long XK_Greek_PI = 0x7d0 ;
    public static final long XK_Greek_RHO = 0x7d1 ;
    public static final long XK_Greek_SIGMA = 0x7d2 ;
    public static final long XK_Greek_TAU = 0x7d4 ;
    public static final long XK_Greek_UPSILON = 0x7d5 ;
    public static final long XK_Greek_PHI = 0x7d6 ;
    public static final long XK_Greek_CHI = 0x7d7 ;
    public static final long XK_Greek_PSI = 0x7d8 ;
    public static final long XK_Greek_OMEGA = 0x7d9 ;
    public static final long XK_Greek_alpha = 0x7e1 ;
    public static final long XK_Greek_beta = 0x7e2 ;
    public static final long XK_Greek_gamma = 0x7e3 ;
    public static final long XK_Greek_delta = 0x7e4 ;
    public static final long XK_Greek_epsilon = 0x7e5 ;
    public static final long XK_Greek_zeta = 0x7e6 ;
    public static final long XK_Greek_eta = 0x7e7 ;
    public static final long XK_Greek_theta = 0x7e8 ;
    public static final long XK_Greek_iota = 0x7e9 ;
    public static final long XK_Greek_kappa = 0x7ea ;
    public static final long XK_Greek_lamda = 0x7eb ;
    public static final long XK_Greek_lambda = 0x7eb ;
    public static final long XK_Greek_mu = 0x7ec ;
    public static final long XK_Greek_nu = 0x7ed ;
    public static final long XK_Greek_xi = 0x7ee ;
    public static final long XK_Greek_omicron = 0x7ef ;
    public static final long XK_Greek_pi = 0x7f0 ;
    public static final long XK_Greek_rho = 0x7f1 ;
    public static final long XK_Greek_sigma = 0x7f2 ;
    public static final long XK_Greek_finalsmallsigma = 0x7f3 ;
    public static final long XK_Greek_tau = 0x7f4 ;
    public static final long XK_Greek_upsilon = 0x7f5 ;
    public static final long XK_Greek_phi = 0x7f6 ;
    public static final long XK_Greek_chi = 0x7f7 ;
    public static final long XK_Greek_psi = 0x7f8 ;
    public static final long XK_Greek_omega = 0x7f9 ;
    public static final long XK_Greek_switch = 0xFF7E ; /* Alias for mode_switch */

    /*
     * Technical
     * Byte 3 = 8
     */

    public static final long XK_leftradical = 0x8a1 ;
    public static final long XK_topleftradical = 0x8a2 ;
    public static final long XK_horizconnector = 0x8a3 ;
    public static final long XK_topintegral = 0x8a4 ;
    public static final long XK_botintegral = 0x8a5 ;
    public static final long XK_vertconnector = 0x8a6 ;
    public static final long XK_topleftsqbracket = 0x8a7 ;
    public static final long XK_botleftsqbracket = 0x8a8 ;
    public static final long XK_toprightsqbracket = 0x8a9 ;
    public static final long XK_botrightsqbracket = 0x8aa ;
    public static final long XK_topleftparens = 0x8ab ;
    public static final long XK_botleftparens = 0x8ac ;
    public static final long XK_toprightparens = 0x8ad ;
    public static final long XK_botrightparens = 0x8ae ;
    public static final long XK_leftmiddlecurlybrace = 0x8af ;
    public static final long XK_rightmiddlecurlybrace = 0x8b0 ;
    public static final long XK_topleftsummation = 0x8b1 ;
    public static final long XK_botleftsummation = 0x8b2 ;
    public static final long XK_topvertsummationconnector = 0x8b3 ;
    public static final long XK_botvertsummationconnector = 0x8b4 ;
    public static final long XK_toprightsummation = 0x8b5 ;
    public static final long XK_botrightsummation = 0x8b6 ;
    public static final long XK_rightmiddlesummation = 0x8b7 ;
    public static final long XK_lessthanequal = 0x8bc ;
    public static final long XK_notequal = 0x8bd ;
    public static final long XK_greaterthanequal = 0x8be ;
    public static final long XK_integral = 0x8bf ;
    public static final long XK_therefore = 0x8c0 ;
    public static final long XK_variation = 0x8c1 ;
    public static final long XK_infinity = 0x8c2 ;
    public static final long XK_nabla = 0x8c5 ;
    public static final long XK_approximate = 0x8c8 ;
    public static final long XK_similarequal = 0x8c9 ;
    public static final long XK_ifonlyif = 0x8cd ;
    public static final long XK_implies = 0x8ce ;
    public static final long XK_identical = 0x8cf ;
    public static final long XK_radical = 0x8d6 ;
    public static final long XK_includedin = 0x8da ;
    public static final long XK_includes = 0x8db ;
    public static final long XK_intersection = 0x8dc ;
    public static final long XK_union = 0x8dd ;
    public static final long XK_logicaland = 0x8de ;
    public static final long XK_logicalor = 0x8df ;
    public static final long XK_partialderivative = 0x8ef ;
    public static final long XK_function = 0x8f6 ;
    public static final long XK_leftarrow = 0x8fb ;
    public static final long XK_uparrow = 0x8fc ;
    public static final long XK_rightarrow = 0x8fd ;
    public static final long XK_downarrow = 0x8fe ;

    /*
     *  Special
     *  Byte 3 = 9
     */

    public static final long XK_blank = 0x9df ;
    public static final long XK_soliddiamond = 0x9e0 ;
    public static final long XK_checkerboard = 0x9e1 ;
    public static final long XK_ht = 0x9e2 ;
    public static final long XK_ff = 0x9e3 ;
    public static final long XK_cr = 0x9e4 ;
    public static final long XK_lf = 0x9e5 ;
    public static final long XK_nl = 0x9e8 ;
    public static final long XK_vt = 0x9e9 ;
    public static final long XK_lowrightcorner = 0x9ea ;
    public static final long XK_uprightcorner = 0x9eb ;
    public static final long XK_upleftcorner = 0x9ec ;
    public static final long XK_lowleftcorner = 0x9ed ;
    public static final long XK_crossinglines = 0x9ee ;
    public static final long XK_horizlinescan1 = 0x9ef ;
    public static final long XK_horizlinescan3 = 0x9f0 ;
    public static final long XK_horizlinescan5 = 0x9f1 ;
    public static final long XK_horizlinescan7 = 0x9f2 ;
    public static final long XK_horizlinescan9 = 0x9f3 ;
    public static final long XK_leftt = 0x9f4 ;
    public static final long XK_rightt = 0x9f5 ;
    public static final long XK_bott = 0x9f6 ;
    public static final long XK_topt = 0x9f7 ;
    public static final long XK_vertbar = 0x9f8 ;

    /*
     *  Publishing
     *  Byte 3 = a
     */

    public static final long XK_emspace = 0xaa1 ;
    public static final long XK_enspace = 0xaa2 ;
    public static final long XK_em3space = 0xaa3 ;
    public static final long XK_em4space = 0xaa4 ;
    public static final long XK_digitspace = 0xaa5 ;
    public static final long XK_punctspace = 0xaa6 ;
    public static final long XK_thinspace = 0xaa7 ;
    public static final long XK_hairspace = 0xaa8 ;
    public static final long XK_emdash = 0xaa9 ;
    public static final long XK_endash = 0xaaa ;
    public static final long XK_signifblank = 0xaac ;
    public static final long XK_ellipsis = 0xaae ;
    public static final long XK_doubbaselinedot = 0xaaf ;
    public static final long XK_onethird = 0xab0 ;
    public static final long XK_twothirds = 0xab1 ;
    public static final long XK_onefifth = 0xab2 ;
    public static final long XK_twofifths = 0xab3 ;
    public static final long XK_threefifths = 0xab4 ;
    public static final long XK_fourfifths = 0xab5 ;
    public static final long XK_onesixth = 0xab6 ;
    public static final long XK_fivesixths = 0xab7 ;
    public static final long XK_careof = 0xab8 ;
    public static final long XK_figdash = 0xabb ;
    public static final long XK_leftanglebracket = 0xabc ;
    public static final long XK_decimalpoint = 0xabd ;
    public static final long XK_rightanglebracket = 0xabe ;
    public static final long XK_marker = 0xabf ;
    public static final long XK_oneeighth = 0xac3 ;
    public static final long XK_threeeighths = 0xac4 ;
    public static final long XK_fiveeighths = 0xac5 ;
    public static final long XK_seveneighths = 0xac6 ;
    public static final long XK_trademark = 0xac9 ;
    public static final long XK_signaturemark = 0xaca ;
    public static final long XK_trademarkincircle = 0xacb ;
    public static final long XK_leftopentriangle = 0xacc ;
    public static final long XK_rightopentriangle = 0xacd ;
    public static final long XK_emopencircle = 0xace ;
    public static final long XK_emopenrectangle = 0xacf ;
    public static final long XK_leftsinglequotemark = 0xad0 ;
    public static final long XK_rightsinglequotemark = 0xad1 ;
    public static final long XK_leftdoublequotemark = 0xad2 ;
    public static final long XK_rightdoublequotemark = 0xad3 ;
    public static final long XK_prescription = 0xad4 ;
    public static final long XK_minutes = 0xad6 ;
    public static final long XK_seconds = 0xad7 ;
    public static final long XK_latincross = 0xad9 ;
    public static final long XK_hexagram = 0xada ;
    public static final long XK_filledrectbullet = 0xadb ;
    public static final long XK_filledlefttribullet = 0xadc ;
    public static final long XK_filledrighttribullet = 0xadd ;
    public static final long XK_emfilledcircle = 0xade ;
    public static final long XK_emfilledrect = 0xadf ;
    public static final long XK_enopencircbullet = 0xae0 ;
    public static final long XK_enopensquarebullet = 0xae1 ;
    public static final long XK_openrectbullet = 0xae2 ;
    public static final long XK_opentribulletup = 0xae3 ;
    public static final long XK_opentribulletdown = 0xae4 ;
    public static final long XK_openstar = 0xae5 ;
    public static final long XK_enfilledcircbullet = 0xae6 ;
    public static final long XK_enfilledsqbullet = 0xae7 ;
    public static final long XK_filledtribulletup = 0xae8 ;
    public static final long XK_filledtribulletdown = 0xae9 ;
    public static final long XK_leftpointer = 0xaea ;
    public static final long XK_rightpointer = 0xaeb ;
    public static final long XK_club = 0xaec ;
    public static final long XK_diamond = 0xaed ;
    public static final long XK_heart = 0xaee ;
    public static final long XK_maltesecross = 0xaf0 ;
    public static final long XK_dagger = 0xaf1 ;
    public static final long XK_doubledagger = 0xaf2 ;
    public static final long XK_checkmark = 0xaf3 ;
    public static final long XK_ballotcross = 0xaf4 ;
    public static final long XK_musicalsharp = 0xaf5 ;
    public static final long XK_musicalflat = 0xaf6 ;
    public static final long XK_malesymbol = 0xaf7 ;
    public static final long XK_femalesymbol = 0xaf8 ;
    public static final long XK_telephone = 0xaf9 ;
    public static final long XK_telephonerecorder = 0xafa ;
    public static final long XK_phonographcopyright = 0xafb ;
    public static final long XK_caret = 0xafc ;
    public static final long XK_singlelowquotemark = 0xafd ;
    public static final long XK_doublelowquotemark = 0xafe ;
    public static final long XK_cursor = 0xaff ;

    /*
     *  APL
     *  Byte 3 = b
     */

    public static final long XK_leftcaret = 0xba3 ;
    public static final long XK_rightcaret = 0xba6 ;
    public static final long XK_downcaret = 0xba8 ;
    public static final long XK_upcaret = 0xba9 ;
    public static final long XK_overbar = 0xbc0 ;
    public static final long XK_downtack = 0xbc2 ;
    public static final long XK_upshoe = 0xbc3 ;
    public static final long XK_downstile = 0xbc4 ;
    public static final long XK_underbar = 0xbc6 ;
    public static final long XK_jot = 0xbca ;
    public static final long XK_quad = 0xbcc ;
    public static final long XK_uptack = 0xbce ;
    public static final long XK_circle = 0xbcf ;
    public static final long XK_upstile = 0xbd3 ;
    public static final long XK_downshoe = 0xbd6 ;
    public static final long XK_rightshoe = 0xbd8 ;
    public static final long XK_leftshoe = 0xbda ;
    public static final long XK_lefttack = 0xbdc ;
    public static final long XK_righttack = 0xbfc ;

    /*
     * Hebrew
     * Byte 3 = c
     */

    public static final long XK_hebrew_doublelowline = 0xcdf ;
    public static final long XK_hebrew_aleph = 0xce0 ;
    public static final long XK_hebrew_bet = 0xce1 ;
    public static final long XK_hebrew_beth = 0xce1 ; /* deprecated */
    public static final long XK_hebrew_gimel = 0xce2 ;
    public static final long XK_hebrew_gimmel = 0xce2 ; /* deprecated */
    public static final long XK_hebrew_dalet = 0xce3 ;
    public static final long XK_hebrew_daleth = 0xce3 ; /* deprecated */
    public static final long XK_hebrew_he = 0xce4 ;
    public static final long XK_hebrew_waw = 0xce5 ;
    public static final long XK_hebrew_zain = 0xce6 ;
    public static final long XK_hebrew_zayin = 0xce6 ; /* deprecated */
    public static final long XK_hebrew_chet = 0xce7 ;
    public static final long XK_hebrew_het = 0xce7 ; /* deprecated */
    public static final long XK_hebrew_tet = 0xce8 ;
    public static final long XK_hebrew_teth = 0xce8 ; /* deprecated */
    public static final long XK_hebrew_yod = 0xce9 ;
    public static final long XK_hebrew_finalkaph = 0xcea ;
    public static final long XK_hebrew_kaph = 0xceb ;
    public static final long XK_hebrew_lamed = 0xcec ;
    public static final long XK_hebrew_finalmem = 0xced ;
    public static final long XK_hebrew_mem = 0xcee ;
    public static final long XK_hebrew_finalnun = 0xcef ;
    public static final long XK_hebrew_nun = 0xcf0 ;
    public static final long XK_hebrew_samech = 0xcf1 ;
    public static final long XK_hebrew_samekh = 0xcf1 ; /* deprecated */
    public static final long XK_hebrew_ayin = 0xcf2 ;
    public static final long XK_hebrew_finalpe = 0xcf3 ;
    public static final long XK_hebrew_pe = 0xcf4 ;
    public static final long XK_hebrew_finalzade = 0xcf5 ;
    public static final long XK_hebrew_finalzadi = 0xcf5 ; /* deprecated */
    public static final long XK_hebrew_zade = 0xcf6 ;
    public static final long XK_hebrew_zadi = 0xcf6 ; /* deprecated */
    public static final long XK_hebrew_qoph = 0xcf7 ;
    public static final long XK_hebrew_kuf = 0xcf7 ; /* deprecated */
    public static final long XK_hebrew_resh = 0xcf8 ;
    public static final long XK_hebrew_shin = 0xcf9 ;
    public static final long XK_hebrew_taw = 0xcfa ;
    public static final long XK_hebrew_taf = 0xcfa ; /* deprecated */
    public static final long XK_Hebrew_switch = 0xFF7E ; /* Alias for mode_switch */

    /*
     * Thai
     * Byte 3 = d
     */

    public static final long XK_Thai_kokai = 0xda1 ;
    public static final long XK_Thai_khokhai = 0xda2 ;
    public static final long XK_Thai_khokhuat = 0xda3 ;
    public static final long XK_Thai_khokhwai = 0xda4 ;
    public static final long XK_Thai_khokhon = 0xda5 ;
    public static final long XK_Thai_khorakhang = 0xda6 ;
    public static final long XK_Thai_ngongu = 0xda7 ;
    public static final long XK_Thai_chochan = 0xda8 ;
    public static final long XK_Thai_choching = 0xda9 ;
    public static final long XK_Thai_chochang = 0xdaa ;
    public static final long XK_Thai_soso = 0xdab ;
    public static final long XK_Thai_chochoe = 0xdac ;
    public static final long XK_Thai_yoying = 0xdad ;
    public static final long XK_Thai_dochada = 0xdae ;
    public static final long XK_Thai_topatak = 0xdaf ;
    public static final long XK_Thai_thothan = 0xdb0 ;
    public static final long XK_Thai_thonangmontho = 0xdb1 ;
    public static final long XK_Thai_thophuthao = 0xdb2 ;
    public static final long XK_Thai_nonen = 0xdb3 ;
    public static final long XK_Thai_dodek = 0xdb4 ;
    public static final long XK_Thai_totao = 0xdb5 ;
    public static final long XK_Thai_thothung = 0xdb6 ;
    public static final long XK_Thai_thothahan = 0xdb7 ;
    public static final long XK_Thai_thothong = 0xdb8 ;
    public static final long XK_Thai_nonu = 0xdb9 ;
    public static final long XK_Thai_bobaimai = 0xdba ;
    public static final long XK_Thai_popla = 0xdbb ;
    public static final long XK_Thai_phophung = 0xdbc ;
    public static final long XK_Thai_fofa = 0xdbd ;
    public static final long XK_Thai_phophan = 0xdbe ;
    public static final long XK_Thai_fofan = 0xdbf ;
    public static final long XK_Thai_phosamphao = 0xdc0 ;
    public static final long XK_Thai_moma = 0xdc1 ;
    public static final long XK_Thai_yoyak = 0xdc2 ;
    public static final long XK_Thai_rorua = 0xdc3 ;
    public static final long XK_Thai_ru = 0xdc4 ;
    public static final long XK_Thai_loling = 0xdc5 ;
    public static final long XK_Thai_lu = 0xdc6 ;
    public static final long XK_Thai_wowaen = 0xdc7 ;
    public static final long XK_Thai_sosala = 0xdc8 ;
    public static final long XK_Thai_sorusi = 0xdc9 ;
    public static final long XK_Thai_sosua = 0xdca ;
    public static final long XK_Thai_hohip = 0xdcb ;
    public static final long XK_Thai_lochula = 0xdcc ;
    public static final long XK_Thai_oang = 0xdcd ;
    public static final long XK_Thai_honokhuk = 0xdce ;
    public static final long XK_Thai_paiyannoi = 0xdcf ;
    public static final long XK_Thai_saraa = 0xdd0 ;
    public static final long XK_Thai_maihanakat = 0xdd1 ;
    public static final long XK_Thai_saraaa = 0xdd2 ;
    public static final long XK_Thai_saraam = 0xdd3 ;
    public static final long XK_Thai_sarai = 0xdd4 ;
    public static final long XK_Thai_saraii = 0xdd5 ;
    public static final long XK_Thai_saraue = 0xdd6 ;
    public static final long XK_Thai_sarauee = 0xdd7 ;
    public static final long XK_Thai_sarau = 0xdd8 ;
    public static final long XK_Thai_sarauu = 0xdd9 ;
    public static final long XK_Thai_phinthu = 0xdda ;
    public static final long XK_Thai_maihanakat_maitho = 0xdde ;
    public static final long XK_Thai_baht = 0xddf ;
    public static final long XK_Thai_sarae = 0xde0 ;
    public static final long XK_Thai_saraae = 0xde1 ;
    public static final long XK_Thai_sarao = 0xde2 ;
    public static final long XK_Thai_saraaimaimuan = 0xde3 ;
    public static final long XK_Thai_saraaimaimalai = 0xde4 ;
    public static final long XK_Thai_lakkhangyao = 0xde5 ;
    public static final long XK_Thai_maiyamok = 0xde6 ;
    public static final long XK_Thai_maitaikhu = 0xde7 ;
    public static final long XK_Thai_maiek = 0xde8 ;
    public static final long XK_Thai_maitho = 0xde9 ;
    public static final long XK_Thai_maitri = 0xdea ;
    public static final long XK_Thai_maichattawa = 0xdeb ;
    public static final long XK_Thai_thanthakhat = 0xdec ;
    public static final long XK_Thai_nikhahit = 0xded ;
    public static final long XK_Thai_leksun = 0xdf0 ;
    public static final long XK_Thai_leknung = 0xdf1 ;
    public static final long XK_Thai_leksong = 0xdf2 ;
    public static final long XK_Thai_leksam = 0xdf3 ;
    public static final long XK_Thai_leksi = 0xdf4 ;
    public static final long XK_Thai_lekha = 0xdf5 ;
    public static final long XK_Thai_lekhok = 0xdf6 ;
    public static final long XK_Thai_lekchet = 0xdf7 ;
    public static final long XK_Thai_lekpaet = 0xdf8 ;
    public static final long XK_Thai_lekkao = 0xdf9 ;

    /*
     *   Korean
     *   Byte 3 = e
     */


    public static final long XK_Hangul = 0xff31 ; /* Hangul start/stop(toggle) */
    public static final long XK_Hangul_Start = 0xff32 ; /* Hangul start */
    public static final long XK_Hangul_End = 0xff33 ; /* Hangul end, English start */
    public static final long XK_Hangul_Hanja = 0xff34 ; /* Start Hangul->Hanja Conversion */
    public static final long XK_Hangul_Jamo = 0xff35 ; /* Hangul Jamo mode */
    public static final long XK_Hangul_Romaja = 0xff36 ; /* Hangul Romaja mode */
    public static final long XK_Hangul_Codeinput = 0xff37 ; /* Hangul code input mode */
    public static final long XK_Hangul_Jeonja = 0xff38 ; /* Jeonja mode */
    public static final long XK_Hangul_Banja = 0xff39 ; /* Banja mode */
    public static final long XK_Hangul_PreHanja = 0xff3a ; /* Pre Hanja conversion */
    public static final long XK_Hangul_PostHanja = 0xff3b ; /* Post Hanja conversion */
    public static final long XK_Hangul_SingleCandidate = 0xff3c ; /* Single candidate */
    public static final long XK_Hangul_MultipleCandidate = 0xff3d ; /* Multiple candidate */
    public static final long XK_Hangul_PreviousCandidate = 0xff3e ; /* Previous candidate */
    public static final long XK_Hangul_Special = 0xff3f ; /* Special symbols */
    public static final long XK_Hangul_switch = 0xFF7E ; /* Alias for mode_switch */

    /* Hangul Consonant Characters */
    public static final long XK_Hangul_Kiyeog = 0xea1 ;
    public static final long XK_Hangul_SsangKiyeog = 0xea2 ;
    public static final long XK_Hangul_KiyeogSios = 0xea3 ;
    public static final long XK_Hangul_Nieun = 0xea4 ;
    public static final long XK_Hangul_NieunJieuj = 0xea5 ;
    public static final long XK_Hangul_NieunHieuh = 0xea6 ;
    public static final long XK_Hangul_Dikeud = 0xea7 ;
    public static final long XK_Hangul_SsangDikeud = 0xea8 ;
    public static final long XK_Hangul_Rieul = 0xea9 ;
    public static final long XK_Hangul_RieulKiyeog = 0xeaa ;
    public static final long XK_Hangul_RieulMieum = 0xeab ;
    public static final long XK_Hangul_RieulPieub = 0xeac ;
    public static final long XK_Hangul_RieulSios = 0xead ;
    public static final long XK_Hangul_RieulTieut = 0xeae ;
    public static final long XK_Hangul_RieulPhieuf = 0xeaf ;
    public static final long XK_Hangul_RieulHieuh = 0xeb0 ;
    public static final long XK_Hangul_Mieum = 0xeb1 ;
    public static final long XK_Hangul_Pieub = 0xeb2 ;
    public static final long XK_Hangul_SsangPieub = 0xeb3 ;
    public static final long XK_Hangul_PieubSios = 0xeb4 ;
    public static final long XK_Hangul_Sios = 0xeb5 ;
    public static final long XK_Hangul_SsangSios = 0xeb6 ;
    public static final long XK_Hangul_Ieung = 0xeb7 ;
    public static final long XK_Hangul_Jieuj = 0xeb8 ;
    public static final long XK_Hangul_SsangJieuj = 0xeb9 ;
    public static final long XK_Hangul_Cieuc = 0xeba ;
    public static final long XK_Hangul_Khieuq = 0xebb ;
    public static final long XK_Hangul_Tieut = 0xebc ;
    public static final long XK_Hangul_Phieuf = 0xebd ;
    public static final long XK_Hangul_Hieuh = 0xebe ;

    /* Hangul Vowel Characters */
    public static final long XK_Hangul_A = 0xebf ;
    public static final long XK_Hangul_AE = 0xec0 ;
    public static final long XK_Hangul_YA = 0xec1 ;
    public static final long XK_Hangul_YAE = 0xec2 ;
    public static final long XK_Hangul_EO = 0xec3 ;
    public static final long XK_Hangul_E = 0xec4 ;
    public static final long XK_Hangul_YEO = 0xec5 ;
    public static final long XK_Hangul_YE = 0xec6 ;
    public static final long XK_Hangul_O = 0xec7 ;
    public static final long XK_Hangul_WA = 0xec8 ;
    public static final long XK_Hangul_WAE = 0xec9 ;
    public static final long XK_Hangul_OE = 0xeca ;
    public static final long XK_Hangul_YO = 0xecb ;
    public static final long XK_Hangul_U = 0xecc ;
    public static final long XK_Hangul_WEO = 0xecd ;
    public static final long XK_Hangul_WE = 0xece ;
    public static final long XK_Hangul_WI = 0xecf ;
    public static final long XK_Hangul_YU = 0xed0 ;
    public static final long XK_Hangul_EU = 0xed1 ;
    public static final long XK_Hangul_YI = 0xed2 ;
    public static final long XK_Hangul_I = 0xed3 ;

    /* Hangul syllable-final (JongSeong) Characters */
    public static final long XK_Hangul_J_Kiyeog = 0xed4 ;
    public static final long XK_Hangul_J_SsangKiyeog = 0xed5 ;
    public static final long XK_Hangul_J_KiyeogSios = 0xed6 ;
    public static final long XK_Hangul_J_Nieun = 0xed7 ;
    public static final long XK_Hangul_J_NieunJieuj = 0xed8 ;
    public static final long XK_Hangul_J_NieunHieuh = 0xed9 ;
    public static final long XK_Hangul_J_Dikeud = 0xeda ;
    public static final long XK_Hangul_J_Rieul = 0xedb ;
    public static final long XK_Hangul_J_RieulKiyeog = 0xedc ;
    public static final long XK_Hangul_J_RieulMieum = 0xedd ;
    public static final long XK_Hangul_J_RieulPieub = 0xede ;
    public static final long XK_Hangul_J_RieulSios = 0xedf ;
    public static final long XK_Hangul_J_RieulTieut = 0xee0 ;
    public static final long XK_Hangul_J_RieulPhieuf = 0xee1 ;
    public static final long XK_Hangul_J_RieulHieuh = 0xee2 ;
    public static final long XK_Hangul_J_Mieum = 0xee3 ;
    public static final long XK_Hangul_J_Pieub = 0xee4 ;
    public static final long XK_Hangul_J_PieubSios = 0xee5 ;
    public static final long XK_Hangul_J_Sios = 0xee6 ;
    public static final long XK_Hangul_J_SsangSios = 0xee7 ;
    public static final long XK_Hangul_J_Ieung = 0xee8 ;
    public static final long XK_Hangul_J_Jieuj = 0xee9 ;
    public static final long XK_Hangul_J_Cieuc = 0xeea ;
    public static final long XK_Hangul_J_Khieuq = 0xeeb ;
    public static final long XK_Hangul_J_Tieut = 0xeec ;
    public static final long XK_Hangul_J_Phieuf = 0xeed ;
    public static final long XK_Hangul_J_Hieuh = 0xeee ;

    /* Ancient Hangul Consonant Characters */
    public static final long XK_Hangul_RieulYeorinHieuh = 0xeef ;
    public static final long XK_Hangul_SunkyeongeumMieum = 0xef0 ;
    public static final long XK_Hangul_SunkyeongeumPieub = 0xef1 ;
    public static final long XK_Hangul_PanSios = 0xef2 ;
    public static final long XK_Hangul_KkogjiDalrinIeung = 0xef3 ;
    public static final long XK_Hangul_SunkyeongeumPhieuf = 0xef4 ;
    public static final long XK_Hangul_YeorinHieuh = 0xef5 ;

    /* Ancient Hangul Vowel Characters */
    public static final long XK_Hangul_AraeA = 0xef6 ;
    public static final long XK_Hangul_AraeAE = 0xef7 ;

    /* Ancient Hangul syllable-final (JongSeong) Characters */
    public static final long XK_Hangul_J_PanSios = 0xef8 ;
    public static final long XK_Hangul_J_KkogjiDalrinIeung = 0xef9 ;
    public static final long XK_Hangul_J_YeorinHieuh = 0xefa ;

    /* Korean currency symbol */
    public static final long XK_Korean_Won = 0xeff ;


    public static final long XK_EcuSign = 0x20a0 ;
    public static final long XK_ColonSign = 0x20a1 ;
    public static final long XK_CruzeiroSign = 0x20a2 ;
    public static final long XK_FFrancSign = 0x20a3 ;
    public static final long XK_LiraSign = 0x20a4 ;
    public static final long XK_MillSign = 0x20a5 ;
    public static final long XK_NairaSign = 0x20a6 ;
    public static final long XK_PesetaSign = 0x20a7 ;
    public static final long XK_RupeeSign = 0x20a8 ;
    public static final long XK_WonSign = 0x20a9 ;
    public static final long XK_NewSheqelSign = 0x20aa ;
    public static final long XK_DongSign = 0x20ab ;
    public static final long XK_EuroSign = 0x20ac ;

    // vendor-specific keys from ap_keysym.h, DEC/Sun/HPkeysym.h

    public static final long apXK_Copy = 0x1000FF02;
    public static final long apXK_Cut = 0x1000FF03;
    public static final long apXK_Paste = 0x1000FF04;

    public static final long DXK_ring_accent = 0x1000FEB0;
    public static final long DXK_circumflex_accent = 0x1000FE5E;
    public static final long DXK_cedilla_accent = 0x1000FE2C;
    public static final long DXK_acute_accent = 0x1000FE27;
    public static final long DXK_grave_accent = 0x1000FE60;
    public static final long DXK_tilde = 0x1000FE7E;
    public static final long DXK_diaeresis = 0x1000FE22;

    public static final long hpXK_ClearLine  = 0x1000FF6F;
    public static final long hpXK_InsertLine  = 0x1000FF70;
    public static final long hpXK_DeleteLine  = 0x1000FF71;
    public static final long hpXK_InsertChar  = 0x1000FF72;
    public static final long hpXK_DeleteChar  = 0x1000FF73;
    public static final long hpXK_BackTab  = 0x1000FF74;
    public static final long hpXK_KP_BackTab  = 0x1000FF75;
    public static final long hpXK_Modelock1  = 0x1000FF48;
    public static final long hpXK_Modelock2  = 0x1000FF49;
    public static final long hpXK_Reset  = 0x1000FF6C;
    public static final long hpXK_System  = 0x1000FF6D;
    public static final long hpXK_User  = 0x1000FF6E;
    public static final long hpXK_mute_acute  = 0x100000A8;
    public static final long hpXK_mute_grave  = 0x100000A9;
    public static final long hpXK_mute_asciicircum  = 0x100000AA;
    public static final long hpXK_mute_diaeresis  = 0x100000AB;
    public static final long hpXK_mute_asciitilde  = 0x100000AC;
    public static final long hpXK_lira  = 0x100000AF;
    public static final long hpXK_guilder  = 0x100000BE;
    public static final long hpXK_Ydiaeresis  = 0x100000EE;
    public static final long hpXK_IO   = 0x100000EE;
    public static final long hpXK_longminus  = 0x100000F6;
    public static final long hpXK_block  = 0x100000FC;


    public static final long osfXK_Copy  = 0x1004FF02;
    public static final long osfXK_Cut  = 0x1004FF03;
    public static final long osfXK_Paste  = 0x1004FF04;
    public static final long osfXK_BackTab  = 0x1004FF07;
    public static final long osfXK_BackSpace  = 0x1004FF08;
    public static final long osfXK_Clear  = 0x1004FF0B;
    public static final long osfXK_Escape  = 0x1004FF1B;
    public static final long osfXK_AddMode  = 0x1004FF31;
    public static final long osfXK_PrimaryPaste  = 0x1004FF32;
    public static final long osfXK_QuickPaste  = 0x1004FF33;
    public static final long osfXK_PageLeft  = 0x1004FF40;
    public static final long osfXK_PageUp  = 0x1004FF41;
    public static final long osfXK_PageDown  = 0x1004FF42;
    public static final long osfXK_PageRight  = 0x1004FF43;
    public static final long osfXK_Activate  = 0x1004FF44;
    public static final long osfXK_MenuBar  = 0x1004FF45;
    public static final long osfXK_Left  = 0x1004FF51;
    public static final long osfXK_Up  = 0x1004FF52;
    public static final long osfXK_Right  = 0x1004FF53;
    public static final long osfXK_Down  = 0x1004FF54;
    public static final long osfXK_EndLine  = 0x1004FF57;
    public static final long osfXK_BeginLine  = 0x1004FF58;
    public static final long osfXK_EndData  = 0x1004FF59;
    public static final long osfXK_BeginData  = 0x1004FF5A;
    public static final long osfXK_PrevMenu  = 0x1004FF5B;
    public static final long osfXK_NextMenu  = 0x1004FF5C;
    public static final long osfXK_PrevField  = 0x1004FF5D;
    public static final long osfXK_NextField  = 0x1004FF5E;
    public static final long osfXK_Select  = 0x1004FF60;
    public static final long osfXK_Insert  = 0x1004FF63;
    public static final long osfXK_Undo  = 0x1004FF65;
    public static final long osfXK_Menu  = 0x1004FF67;
    public static final long osfXK_Cancel = 0x1004FF69;
    public static final long osfXK_Help = 0x1004FF6A;
    public static final long osfXK_Delete = 0x1004FFFF;
    public static final long osfXK_Prior = 0x1004FF55;
    public static final long osfXK_Next = 0x1004FF56;




    public static final long  SunXK_FA_Grave  = 0x1005FF00;
    public static final long  SunXK_FA_Circum  = 0x1005FF01;
    public static final long  SunXK_FA_Tilde  = 0x1005FF02;
    public static final long  SunXK_FA_Acute  = 0x1005FF03;
    public static final long  SunXK_FA_Diaeresis  = 0x1005FF04;
    public static final long  SunXK_FA_Cedilla  = 0x1005FF05;

    public static final long  SunXK_F36  = 0x1005FF10; /* Labeled F11 */
    public static final long  SunXK_F37  = 0x1005FF11; /* Labeled F12 */

    public static final long SunXK_Sys_Req     = 0x1005FF60;
    public static final long SunXK_Print_Screen  = 0x0000FF61; /* Same as XK_Print */

    public static final long SunXK_Compose  = 0x0000FF20; /* Same as XK_Multi_key */
    public static final long SunXK_AltGraph  = 0x0000FF7E; /* Same as XK_Mode_switch */

    public static final long SunXK_PageUp  = 0x0000FF55;  /* Same as XK_Prior */
    public static final long SunXK_PageDown  = 0x0000FF56; /* Same as XK_Next */

    public static final long SunXK_Undo  = 0x0000FF65; /* Same as XK_Undo */
    public static final long SunXK_Again  = 0x0000FF66; /* Same as XK_Redo */
    public static final long SunXK_Find  = 0x0000FF68; /* Same as XK_Find */
    public static final long SunXK_Stop  = 0x0000FF69; /* Same as XK_Cancel */
    public static final long SunXK_Props  = 0x1005FF70;
    public static final long SunXK_Front  = 0x1005FF71;
    public static final long SunXK_Copy  = 0x1005FF72;
    public static final long SunXK_Open  = 0x1005FF73;
    public static final long SunXK_Paste  = 0x1005FF74;
    public static final long SunXK_Cut  = 0x1005FF75;

    public static final long SunXK_PowerSwitch  = 0x1005FF76;
    public static final long SunXK_AudioLowerVolume  = 0x1005FF77;
    public static final long SunXK_AudioMute   = 0x1005FF78;
    public static final long SunXK_AudioRaiseVolume  = 0x1005FF79;
    public static final long SunXK_VideoDegauss  = 0x1005FF7A;
    public static final long SunXK_VideoLowerBrightness  = 0x1005FF7B;
    public static final long SunXK_VideoRaiseBrightness  = 0x1005FF7C;
    public static final long SunXK_PowerSwitchShift  = 0x1005FF7D;

}
