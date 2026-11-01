/*
 * Copyright (c) 2026, Eduardo Casadei <educasadei@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/API/KeyCode.h>
#include <ctype.h>

#define GUI_ENUMERATE_KEYS                                                           \
    __ENUMERATE_PHYSICAL_KEY(Invalid, Invalid, "Invalid")                            \
    __ENUMERATE_PHYSICAL_KEY(Escape, Escape, "Escape")                               \
    __ENUMERATE_PHYSICAL_KEY(Tab, Tab, "Tab")                                        \
    __ENUMERATE_PHYSICAL_KEY(Backspace, Backspace, "Backspace")                      \
    __ENUMERATE_PHYSICAL_KEY(Return, Return, "Return")                               \
    __ENUMERATE_PHYSICAL_KEY(Insert, Insert, "Insert")                               \
    __ENUMERATE_PHYSICAL_KEY(Delete, Delete, "Delete")                               \
    __ENUMERATE_PHYSICAL_KEY(PrintScreen, PrintScreen, "PrintScreen")                \
    __ENUMERATE_PHYSICAL_KEY(PauseBreak, PauseBreak, "PauseBreak")                   \
    __ENUMERATE_PHYSICAL_KEY(SysRq, SysRq, "SysRq")                                  \
    __ENUMERATE_PHYSICAL_KEY(Home, Home, "Home")                                     \
    __ENUMERATE_PHYSICAL_KEY(End, End, "End")                                        \
    __ENUMERATE_PHYSICAL_KEY(Left, Left, "Left")                                     \
    __ENUMERATE_PHYSICAL_KEY(Up, Up, "Up")                                           \
    __ENUMERATE_PHYSICAL_KEY(Right, Right, "Right")                                  \
    __ENUMERATE_PHYSICAL_KEY(Down, Down, "Down")                                     \
    __ENUMERATE_PHYSICAL_KEY(PageUp, PageUp, "PageUp")                               \
    __ENUMERATE_PHYSICAL_KEY(PageDown, PageDown, "PageDown")                         \
    __ENUMERATE_PHYSICAL_KEY(Shift, LeftShift, "Shift")                              \
    __ENUMERATE_PHYSICAL_KEY(Control, LeftControl, "Control")                        \
    __ENUMERATE_PHYSICAL_KEY(Alt, LeftAlt, "Alt")                                    \
    __ENUMERATE_PHYSICAL_KEY(AltGr, AltGr, "AltGr")                                  \
    __ENUMERATE_PHYSICAL_KEY(CapsLock, CapsLock, "CapsLock")                         \
    __ENUMERATE_PHYSICAL_KEY(NumLock, NumLock, "NumLock")                            \
    __ENUMERATE_PHYSICAL_KEY(ScrollLock, ScrollLock, "ScrollLock")                   \
    __ENUMERATE_PHYSICAL_KEY(F1, F1, "F1")                                           \
    __ENUMERATE_PHYSICAL_KEY(F2, F2, "F2")                                           \
    __ENUMERATE_PHYSICAL_KEY(F3, F3, "F3")                                           \
    __ENUMERATE_PHYSICAL_KEY(F4, F4, "F4")                                           \
    __ENUMERATE_PHYSICAL_KEY(F5, F5, "F5")                                           \
    __ENUMERATE_PHYSICAL_KEY(F6, F6, "F6")                                           \
    __ENUMERATE_PHYSICAL_KEY(F7, F7, "F7")                                           \
    __ENUMERATE_PHYSICAL_KEY(F8, F8, "F8")                                           \
    __ENUMERATE_PHYSICAL_KEY(F9, F9, "F9")                                           \
    __ENUMERATE_PHYSICAL_KEY(F10, F10, "F10")                                        \
    __ENUMERATE_PHYSICAL_KEY(F11, F11, "F11")                                        \
    __ENUMERATE_PHYSICAL_KEY(F12, F12, "F12")                                        \
    __ENUMERATE_PHYSICAL_KEY(Space, Space, "Space")                                  \
    __ENUMERATE_LOGICAL_KEY(ExclamationPoint, '!', "!")                              \
    __ENUMERATE_LOGICAL_KEY(DoubleQuote, '"', "\"")                                  \
    __ENUMERATE_LOGICAL_KEY(Hashtag, '#', "#")                                       \
    __ENUMERATE_LOGICAL_KEY(Dollar, '$', "$")                                        \
    __ENUMERATE_LOGICAL_KEY(Percent, '%', "%")                                       \
    __ENUMERATE_LOGICAL_KEY(Ampersand, '&', "&")                                     \
    __ENUMERATE_LOGICAL_KEY(Apostrophe, '\'', "'")                                   \
    __ENUMERATE_LOGICAL_KEY(LeftParen, '(', "(")                                     \
    __ENUMERATE_LOGICAL_KEY(RightParen, ')', ")")                                    \
    __ENUMERATE_LOGICAL_KEY(Asterisk, '*', "*")                                      \
    __ENUMERATE_LOGICAL_KEY(Plus, '+', "+")                                          \
    __ENUMERATE_LOGICAL_KEY(Comma, ',', ",")                                         \
    __ENUMERATE_LOGICAL_KEY(Minus, '-', "-")                                         \
    __ENUMERATE_LOGICAL_KEY(Period, '.', ".")                                        \
    __ENUMERATE_LOGICAL_KEY(Slash, '/', "/")                                         \
    __ENUMERATE_LOGICAL_KEY(0, '0', "0")                                             \
    __ENUMERATE_LOGICAL_KEY(1, '1', "1")                                             \
    __ENUMERATE_LOGICAL_KEY(2, '2', "2")                                             \
    __ENUMERATE_LOGICAL_KEY(3, '3', "3")                                             \
    __ENUMERATE_LOGICAL_KEY(4, '4', "4")                                             \
    __ENUMERATE_LOGICAL_KEY(5, '5', "5")                                             \
    __ENUMERATE_LOGICAL_KEY(6, '6', "6")                                             \
    __ENUMERATE_LOGICAL_KEY(7, '7', "7")                                             \
    __ENUMERATE_LOGICAL_KEY(8, '8', "8")                                             \
    __ENUMERATE_LOGICAL_KEY(9, '9', "9")                                             \
    __ENUMERATE_LOGICAL_KEY(Colon, ':', ":")                                         \
    __ENUMERATE_LOGICAL_KEY(Semicolon, ';', ";")                                     \
    __ENUMERATE_LOGICAL_KEY(LessThan, '<', "<")                                      \
    __ENUMERATE_LOGICAL_KEY(Equal, '=', "=")                                         \
    __ENUMERATE_LOGICAL_KEY(GreaterThan, '>', ">")                                   \
    __ENUMERATE_LOGICAL_KEY(QuestionMark, '?', "?")                                  \
    __ENUMERATE_LOGICAL_KEY(AtSign, '@', "@")                                        \
    __ENUMERATE_LOGICAL_KEY(A, 'A', "A")                                             \
    __ENUMERATE_LOGICAL_KEY(B, 'B', "B")                                             \
    __ENUMERATE_LOGICAL_KEY(C, 'C', "C")                                             \
    __ENUMERATE_LOGICAL_KEY(D, 'D', "D")                                             \
    __ENUMERATE_LOGICAL_KEY(E, 'E', "E")                                             \
    __ENUMERATE_LOGICAL_KEY(F, 'F', "F")                                             \
    __ENUMERATE_LOGICAL_KEY(G, 'G', "G")                                             \
    __ENUMERATE_LOGICAL_KEY(H, 'H', "H")                                             \
    __ENUMERATE_LOGICAL_KEY(I, 'I', "I")                                             \
    __ENUMERATE_LOGICAL_KEY(J, 'J', "J")                                             \
    __ENUMERATE_LOGICAL_KEY(K, 'K', "K")                                             \
    __ENUMERATE_LOGICAL_KEY(L, 'L', "L")                                             \
    __ENUMERATE_LOGICAL_KEY(M, 'M', "M")                                             \
    __ENUMERATE_LOGICAL_KEY(N, 'N', "N")                                             \
    __ENUMERATE_LOGICAL_KEY(O, 'O', "O")                                             \
    __ENUMERATE_LOGICAL_KEY(P, 'P', "P")                                             \
    __ENUMERATE_LOGICAL_KEY(Q, 'Q', "Q")                                             \
    __ENUMERATE_LOGICAL_KEY(R, 'R', "R")                                             \
    __ENUMERATE_LOGICAL_KEY(S, 'S', "S")                                             \
    __ENUMERATE_LOGICAL_KEY(T, 'T', "T")                                             \
    __ENUMERATE_LOGICAL_KEY(U, 'U', "U")                                             \
    __ENUMERATE_LOGICAL_KEY(V, 'V', "V")                                             \
    __ENUMERATE_LOGICAL_KEY(W, 'W', "W")                                             \
    __ENUMERATE_LOGICAL_KEY(X, 'X', "X")                                             \
    __ENUMERATE_LOGICAL_KEY(Y, 'Y', "Y")                                             \
    __ENUMERATE_LOGICAL_KEY(Z, 'Z', "Z")                                             \
    __ENUMERATE_LOGICAL_KEY(LeftBracket, '[', "[")                                   \
    __ENUMERATE_LOGICAL_KEY(RightBracket, ']', "]")                                  \
    __ENUMERATE_LOGICAL_KEY(Backslash, '\\', "\\")                                   \
    __ENUMERATE_LOGICAL_KEY(Circumflex, '^', "^")                                    \
    __ENUMERATE_LOGICAL_KEY(Underscore, '_', "_")                                    \
    __ENUMERATE_LOGICAL_KEY(LeftBrace, '{', "{")                                     \
    __ENUMERATE_LOGICAL_KEY(RightBrace, '}', "}")                                    \
    __ENUMERATE_LOGICAL_KEY(Pipe, '|', "|")                                          \
    __ENUMERATE_LOGICAL_KEY(Tilde, '~', "~")                                         \
    __ENUMERATE_LOGICAL_KEY(Backtick, '`', "`")                                      \
    __ENUMERATE_PHYSICAL_KEY(Super, LeftSuper, "Super")                              \
    __ENUMERATE_PHYSICAL_KEY(BrowserSearch, BrowserSearch, "BrowserSearch")          \
    __ENUMERATE_PHYSICAL_KEY(BrowserFavorites, BrowserFavorites, "BrowserFavorites") \
    __ENUMERATE_PHYSICAL_KEY(BrowserHome, BrowserHome, "BrowserHome")                \
    __ENUMERATE_PHYSICAL_KEY(PreviousTrack, PreviousTrack, "PreviousTrack")          \
    __ENUMERATE_PHYSICAL_KEY(BrowserBack, BrowserBack, "BrowserBack")                \
    __ENUMERATE_PHYSICAL_KEY(BrowserForward, BrowserForward, "BrowserForward")       \
    __ENUMERATE_PHYSICAL_KEY(BrowserRefresh, BrowserRefresh, "BrowserRefresh")       \
    __ENUMERATE_PHYSICAL_KEY(BrowserStop, BrowserStop, "BrowserStop")                \
    __ENUMERATE_PHYSICAL_KEY(VolumeDown, VolumeDown, "VolumeDown")                   \
    __ENUMERATE_PHYSICAL_KEY(VolumeUp, VolumeUp, "VolumeUp")                         \
    __ENUMERATE_PHYSICAL_KEY(Wake, Wake, "Wake")                                     \
    __ENUMERATE_PHYSICAL_KEY(Sleep, Sleep, "Sleep")                                  \
    __ENUMERATE_PHYSICAL_KEY(NextTrack, NextTrack, "NextTrack")                      \
    __ENUMERATE_PHYSICAL_KEY(MediaSelect, MediaSelect, "MediaSelect")                \
    __ENUMERATE_PHYSICAL_KEY(Email, Email, "Email")                                  \
    __ENUMERATE_PHYSICAL_KEY(MyComputer, MyComputer, "MyComputer")                   \
    __ENUMERATE_PHYSICAL_KEY(Power, Power, "Power")                                  \
    __ENUMERATE_PHYSICAL_KEY(Stop, Stop, "Stop")                                     \
    __ENUMERATE_PHYSICAL_KEY(Mute, Mute, "Mute")                                     \
    __ENUMERATE_PHYSICAL_KEY(Calculator, Calculator, "Calculator")                   \
    __ENUMERATE_PHYSICAL_KEY(Apps, Apps, "Apps")                                     \
    __ENUMERATE_PHYSICAL_KEY(PlayPause, PlayPause, "PlayPause")                      \
    __ENUMERATE_PHYSICAL_KEY(Menu, Menu, "Menu")

namespace GUI {
enum Key {
#define __ENUMERATE_PHYSICAL_KEY(name, key_code, ui_name) Key_##name,
#define __ENUMERATE_LOGICAL_KEY(name, code_point, ui_name) Key_##name,
    GUI_ENUMERATE_KEYS
#undef __ENUMERATE_PHYSICAL_KEY
#undef __ENUMERATE_LOGICAL_KEY
};

inline Key key_from_key_event(KeyCode event_key_code, u32 event_code_point)
{
    switch (toupper(event_code_point)) {
#define __ENUMERATE_PHYSICAL_KEY(name, key_code, ui_name)
#define __ENUMERATE_LOGICAL_KEY(name, code_point, ui_name) \
    case code_point:                                       \
        return Key::Key_##name;
        GUI_ENUMERATE_KEYS
#undef __ENUMERATE_PHYSICAL_KEY
#undef __ENUMERATE_LOGICAL_KEY
    }

    switch (event_key_code) {
#define __ENUMERATE_LOGICAL_KEY(name, code_point, ui_name)
#define __ENUMERATE_PHYSICAL_KEY(name, key_code, ui_name) \
    case KeyCode::Key_##key_code:                         \
        return Key::Key_##name;
        GUI_ENUMERATE_KEYS
#undef __ENUMERATE_PHYSICAL_KEY
#undef __ENUMERATE_LOGICAL_KEY
    case KeyCode::Key_RightShift:
        return Key::Key_Shift;
    case KeyCode::Key_RightControl:
        return Key::Key_Control;
    case KeyCode::Key_RightAlt:
        return Key::Key_Alt;
    case KeyCode::Key_RightSuper:
        return Key::Key_Super;
    default:
        return Key::Key_Invalid;
    }
}

inline char const* key_to_string(Key key)
{
    switch (key) {
#define __ENUMERATE_PHYSICAL_KEY(name, key_code, ui_name) \
    case Key::Key_##name:                                 \
        return ui_name;
#define __ENUMERATE_LOGICAL_KEY(name, code_point, ui_name) \
    case Key::Key_##name:                                  \
        return ui_name;
        GUI_ENUMERATE_KEYS
#undef __ENUMERATE_PHYSICAL_KEY
#undef __ENUMERATE_LOGICAL_KEY
    default:
        return nullptr;
    }
}
}
