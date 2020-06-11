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

#define ENUMERATE_KEY_CODES                          \
    __ENUMERATE_KEY_CODE(Invalid, "Invalid")         \
    __ENUMERATE_KEY_CODE(Escape, "Escape")           \
    __ENUMERATE_KEY_CODE(Tab, "Tab")                 \
    __ENUMERATE_KEY_CODE(Backspace, "Backspace")     \
    __ENUMERATE_KEY_CODE(Return, "Return")           \
    __ENUMERATE_KEY_CODE(Insert, "Insert")           \
    __ENUMERATE_KEY_CODE(Delete, "Delete")           \
    __ENUMERATE_KEY_CODE(PrintScreen, "PrintScreen") \
    __ENUMERATE_KEY_CODE(SysRq, "SysRq")             \
    __ENUMERATE_KEY_CODE(Home, "Home")               \
    __ENUMERATE_KEY_CODE(End, "End")                 \
    __ENUMERATE_KEY_CODE(Left, "Left")               \
    __ENUMERATE_KEY_CODE(Up, "Up")                   \
    __ENUMERATE_KEY_CODE(Right, "Right")             \
    __ENUMERATE_KEY_CODE(Down, "Down")               \
    __ENUMERATE_KEY_CODE(PageUp, "PageUp")           \
    __ENUMERATE_KEY_CODE(PageDown, "PageDown")       \
    __ENUMERATE_KEY_CODE(LeftShift, "LeftShift")     \
    __ENUMERATE_KEY_CODE(RightShift, "RightShift")   \
    __ENUMERATE_KEY_CODE(Control, "Ctrl")            \
    __ENUMERATE_KEY_CODE(Alt, "Alt")                 \
    __ENUMERATE_KEY_CODE(CapsLock, "CapsLock")       \
    __ENUMERATE_KEY_CODE(NumLock, "NumLock")         \
    __ENUMERATE_KEY_CODE(ScrollLock, "ScrollLock")   \
    __ENUMERATE_KEY_CODE(F1, "F1")                   \
    __ENUMERATE_KEY_CODE(F2, "F2")                   \
    __ENUMERATE_KEY_CODE(F3, "F3")                   \
    __ENUMERATE_KEY_CODE(F4, "F4")                   \
    __ENUMERATE_KEY_CODE(F5, "F5")                   \
    __ENUMERATE_KEY_CODE(F6, "F6")                   \
    __ENUMERATE_KEY_CODE(F7, "F7")                   \
    __ENUMERATE_KEY_CODE(F8, "F8")                   \
    __ENUMERATE_KEY_CODE(F9, "F9")                   \
    __ENUMERATE_KEY_CODE(F10, "F10")                 \
    __ENUMERATE_KEY_CODE(F11, "F11")                 \
    __ENUMERATE_KEY_CODE(F12, "F12")                 \
    __ENUMERATE_KEY_CODE(Space, "Space")             \
    __ENUMERATE_KEY_CODE(ExclamationPoint, "!")      \
    __ENUMERATE_KEY_CODE(DoubleQuote, "\"")          \
    __ENUMERATE_KEY_CODE(Hashtag, "#")               \
    __ENUMERATE_KEY_CODE(Dollar, "$")                \
    __ENUMERATE_KEY_CODE(Percent, "%")               \
    __ENUMERATE_KEY_CODE(Ampersand, "&")             \
    __ENUMERATE_KEY_CODE(Apostrophe, "'")            \
    __ENUMERATE_KEY_CODE(LeftParen, "(")             \
    __ENUMERATE_KEY_CODE(RightParen, ")")            \
    __ENUMERATE_KEY_CODE(Asterisk, "*")              \
    __ENUMERATE_KEY_CODE(Plus, "+")                  \
    __ENUMERATE_KEY_CODE(Comma, ",")                 \
    __ENUMERATE_KEY_CODE(Minus, "-")                 \
    __ENUMERATE_KEY_CODE(Period, ".")                \
    __ENUMERATE_KEY_CODE(Slash, "/")                 \
    __ENUMERATE_KEY_CODE(0, "0")                     \
    __ENUMERATE_KEY_CODE(1, "1")                     \
    __ENUMERATE_KEY_CODE(2, "2")                     \
    __ENUMERATE_KEY_CODE(3, "3")                     \
    __ENUMERATE_KEY_CODE(4, "4")                     \
    __ENUMERATE_KEY_CODE(5, "5")                     \
    __ENUMERATE_KEY_CODE(6, "6")                     \
    __ENUMERATE_KEY_CODE(7, "7")                     \
    __ENUMERATE_KEY_CODE(8, "8")                     \
    __ENUMERATE_KEY_CODE(9, "9")                     \
    __ENUMERATE_KEY_CODE(Colon, ":")                 \
    __ENUMERATE_KEY_CODE(Semicolon, ";")             \
    __ENUMERATE_KEY_CODE(LessThan, "<")              \
    __ENUMERATE_KEY_CODE(Equal, "=")                 \
    __ENUMERATE_KEY_CODE(GreaterThan, ">")           \
    __ENUMERATE_KEY_CODE(QuestionMark, "?")          \
    __ENUMERATE_KEY_CODE(AtSign, "@")                \
    __ENUMERATE_KEY_CODE(A, "A")                     \
    __ENUMERATE_KEY_CODE(B, "B")                     \
    __ENUMERATE_KEY_CODE(C, "C")                     \
    __ENUMERATE_KEY_CODE(D, "D")                     \
    __ENUMERATE_KEY_CODE(E, "E")                     \
    __ENUMERATE_KEY_CODE(F, "F")                     \
    __ENUMERATE_KEY_CODE(G, "G")                     \
    __ENUMERATE_KEY_CODE(H, "H")                     \
    __ENUMERATE_KEY_CODE(I, "I")                     \
    __ENUMERATE_KEY_CODE(J, "J")                     \
    __ENUMERATE_KEY_CODE(K, "K")                     \
    __ENUMERATE_KEY_CODE(L, "L")                     \
    __ENUMERATE_KEY_CODE(M, "M")                     \
    __ENUMERATE_KEY_CODE(N, "N")                     \
    __ENUMERATE_KEY_CODE(O, "O")                     \
    __ENUMERATE_KEY_CODE(P, "P")                     \
    __ENUMERATE_KEY_CODE(Q, "Q")                     \
    __ENUMERATE_KEY_CODE(R, "R")                     \
    __ENUMERATE_KEY_CODE(S, "S")                     \
    __ENUMERATE_KEY_CODE(T, "T")                     \
    __ENUMERATE_KEY_CODE(U, "U")                     \
    __ENUMERATE_KEY_CODE(V, "V")                     \
    __ENUMERATE_KEY_CODE(W, "W")                     \
    __ENUMERATE_KEY_CODE(X, "X")                     \
    __ENUMERATE_KEY_CODE(Y, "Y")                     \
    __ENUMERATE_KEY_CODE(Z, "Z")                     \
    __ENUMERATE_KEY_CODE(LeftBracket, "[")           \
    __ENUMERATE_KEY_CODE(RightBracket, "]")          \
    __ENUMERATE_KEY_CODE(Backslash, "\\")            \
    __ENUMERATE_KEY_CODE(Circumflex, "^")            \
    __ENUMERATE_KEY_CODE(Underscore, "_")            \
    __ENUMERATE_KEY_CODE(LeftBrace, "{")             \
    __ENUMERATE_KEY_CODE(RightBrace, "}")            \
    __ENUMERATE_KEY_CODE(Pipe, "|")                  \
    __ENUMERATE_KEY_CODE(Tilde, "~")                 \
    __ENUMERATE_KEY_CODE(Backtick, "`")              \
    __ENUMERATE_KEY_CODE(Logo, "Logo")

enum KeyCode : u8 {
#define __ENUMERATE_KEY_CODE(name, ui_name) Key_##name,
    ENUMERATE_KEY_CODES
#undef __ENUMERATE_KEY_CODE

        Key_Shift
    = Key_LeftShift,
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
    u32 scancode { 0 };
    u32 code_point { 0 };
    u8 flags { 0 };
    bool caps_lock_on { false };
    bool e0_prefix { false };
    bool alt() const { return flags & Mod_Alt; }
    bool ctrl() const { return flags & Mod_Ctrl; }
    bool shift() const { return flags & Mod_Shift; }
    bool logo() const { return flags & Mod_Logo; }
    bool altgr() const { return flags & Mod_AltGr; }
    unsigned modifiers() const { return flags & Mod_Mask; }
    bool is_press() const { return flags & Is_Press; }
};

inline const char* key_code_to_string(KeyCode key)
{
    switch (key) {
#define __ENUMERATE_KEY_CODE(name, ui_name) \
    case Key_##name:                        \
        return ui_name;
        ENUMERATE_KEY_CODES
#undef __ENUMERATE_KEY_CODE
    default:
        return nullptr;
    }
}
