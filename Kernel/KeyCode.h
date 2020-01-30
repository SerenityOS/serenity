/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Types.h>

enum KeyCode : u8 {
    Key_Invalid = 0,
    Key_Escape,
    Key_Tab,
    Key_Backspace,
    Key_Return,
    Key_Insert,
    Key_Delete,
    Key_PrintScreen,
    Key_SysRq,
    Key_Home,
    Key_End,
    Key_Left,
    Key_Up,
    Key_Right,
    Key_Down,
    Key_PageUp,
    Key_PageDown,
    Key_LeftShift,
    Key_RightShift,
    Key_Control,
    Key_Alt,
    Key_CapsLock,
    Key_NumLock,
    Key_ScrollLock,
    Key_F1,
    Key_F2,
    Key_F3,
    Key_F4,
    Key_F5,
    Key_F6,
    Key_F7,
    Key_F8,
    Key_F9,
    Key_F10,
    Key_F11,
    Key_F12,
    Key_Space,
    Key_ExclamationPoint,
    Key_DoubleQuote,
    Key_Hashtag,
    Key_Dollar,
    Key_Percent,
    Key_Ampersand,
    Key_Apostrophe,
    Key_LeftParen,
    Key_RightParen,
    Key_Asterisk,
    Key_Plus,
    Key_Comma,
    Key_Minus,
    Key_Period,
    Key_Slash,
    Key_0,
    Key_1,
    Key_2,
    Key_3,
    Key_4,
    Key_5,
    Key_6,
    Key_7,
    Key_8,
    Key_9,
    Key_Colon,
    Key_Semicolon,
    Key_LessThan,
    Key_Equal,
    Key_GreaterThan,
    Key_QuestionMark,
    Key_AtSign,
    Key_A,
    Key_B,
    Key_C,
    Key_D,
    Key_E,
    Key_F,
    Key_G,
    Key_H,
    Key_I,
    Key_J,
    Key_K,
    Key_L,
    Key_M,
    Key_N,
    Key_O,
    Key_P,
    Key_Q,
    Key_R,
    Key_S,
    Key_T,
    Key_U,
    Key_V,
    Key_W,
    Key_X,
    Key_Y,
    Key_Z,
    Key_LeftBracket,
    Key_RightBracket,
    Key_Backslash,
    Key_Circumflex,
    Key_Underscore,
    Key_LeftBrace,
    Key_RightBrace,
    Key_Pipe,
    Key_Tilde,
    Key_Backtick,
    Key_Logo,

    Key_Shift = Key_LeftShift,
};
const int key_code_count = Key_Logo;

enum KeyModifier {
    Mod_None = 0x00,
    Mod_Alt = 0x01,
    Mod_Ctrl = 0x02,
    Mod_Shift = 0x04,
    Mod_Logo = 0x08,
    Mod_AltGr = 0x10,
    Mod_Mask = 0x1f,

    Is_Press = 0x80,
};

struct KeyEvent {
    KeyCode key { Key_Invalid };
    u8 character { 0 };
    u8 flags { 0 };
    bool alt() const { return flags & Mod_Alt; }
    bool ctrl() const { return flags & Mod_Ctrl; }
    bool shift() const { return flags & Mod_Shift; }
    bool logo() const { return flags & Mod_Logo; }
    bool altgr() const { return flags & Mod_AltGr; }
    unsigned modifiers() const { return flags & Mod_Mask; }
    bool is_press() const { return flags & Is_Press; }
};
