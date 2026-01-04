/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <Kernel/Devices/Input/Definitions.h>

namespace Kernel::VirtIO {

// clang-format off
static constexpr auto unshifted_evdev_key_map = to_array<KeyCodeEntry const>({
    // 0x00-0x0f
    { Key_Invalid, 0xff },           { Key_Escape, 0x01 },            { Key_1, 0x02 },                 { Key_2, 0x03 },
    { Key_3, 0x04 },                 { Key_4, 0x05 },                 { Key_5, 0x06 },                 { Key_6, 0x07 },
    { Key_7, 0x08 },                 { Key_8, 0x09 },                 { Key_9, 0x0a },                 { Key_0, 0x0b },
    { Key_Minus, 0x0c },             { Key_Equal, 0x0d },             { Key_Backspace, 0x0e },         { Key_Tab, 0x0f },

    // 0x10-0x1f
    { Key_Q, 0x10 },                 { Key_W, 0x11 },                 { Key_E, 0x12 },                 { Key_R, 0x13 },
    { Key_T, 0x14 },                 { Key_Y, 0x15 },                 { Key_U, 0x16 },                 { Key_I, 0x17 },
    { Key_O, 0x18 },                 { Key_P, 0x19 },                 { Key_LeftBracket, 0x1a },       { Key_RightBracket, 0x1b },
    { Key_Return, 0x1c },            { Key_LeftControl, 0xff },       { Key_A, 0x1e },                 { Key_S, 0x1f },

    // 0x20-0x2f
    { Key_D, 0x20 },                 { Key_F, 0x21 },                 { Key_G, 0x22 },                 { Key_H, 0x23 },
    { Key_J, 0x24 },                 { Key_K, 0x25 },                 { Key_L, 0x26 },                 { Key_Semicolon, 0x27 },
    { Key_Apostrophe, 0x28 },        { Key_Backtick, 0x29 },          { Key_LeftShift, 0xff },         { Key_Backslash, 0x2b },
    { Key_Z, 0x2c },                 { Key_X, 0x2d },                 { Key_C, 0x2e },                 { Key_V, 0x2f },

    // 0x30-0x3f
    { Key_B, 0x30 },                 { Key_N, 0x31 },                 { Key_M, 0x32 },                 { Key_Comma, 0x33 },
    { Key_Period, 0x34 },            { Key_Slash, 0x35 },             { Key_RightShift, 0xff },        { Key_Asterisk, 0x37 },
    { Key_LeftAlt, 0xff },           { Key_Space, 0x39 },             { Key_CapsLock, 0xff },          { Key_F1, 0xff },
    { Key_F2, 0xff },                { Key_F3, 0xff },                { Key_F4, 0xff },                { Key_F5, 0xff },

    // 0x40-0x4f
    { Key_F6, 0xff },                { Key_F7, 0xff },                { Key_F8, 0xff },                { Key_F9, 0xff },
    { Key_F10, 0xff },               { Key_NumLock, 0xff },           { Key_ScrollLock, 0xff },        { Key_Home, 0xff },
    { Key_Up, 0xff },                { Key_PageUp, 0xff },            { Key_Minus, 0x4a },             { Key_Left, 0xff },
    { Key_Invalid, 0xff },           { Key_Right, 0xff },             { Key_Plus, 0x4e },              { Key_End, 0xff },

    // 0x50-0x5f
    { Key_Down, 0xff },              { Key_PageDown, 0xff },          { Key_Insert, 0xff },            { Key_Delete, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Backslash, 0x56 },         { Key_F11, 0xff },
    { Key_F12, 0xff },               { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },

    // 0x60-0x6f
    // FIXME: Add Numpad "/" key to character map for key code 0x62
    { Key_Return, 0x1c },            { Key_RightControl, 0xff },      { Key_Slash, 0xff },             { Key_SysRq, 0xff },
    { Key_RightAlt, 0xff },          { Key_Invalid, 0xff },           { Key_Home, 0xff },              { Key_Up, 0xff },
    { Key_PageUp, 0xff },            { Key_Left, 0xff },              { Key_Right, 0xff },             { Key_End, 0xff },
    { Key_Down, 0xff },              { Key_PageDown, 0xff },          { Key_Insert, 0xff },            { Key_Delete, 0xff },

    // 0x70-0x7f
    { Key_Invalid, 0xff },           { Key_Mute, 0xff },              { Key_VolumeDown, 0xff },        { Key_VolumeUp, 0xff },
    { Key_Power, 0xff },             { Key_Equal, 0xff },             { Key_Invalid, 0xff },           { Key_PauseBreak, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_LeftSuper, 0xff },         { Key_RightSuper, 0xff },        { Key_Menu, 0xff },

    // 0x80-0x8f
    { Key_Stop, 0xff },              { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Calculator, 0xff },        { Key_Invalid, 0xff },           { Key_Sleep, 0xff },             { Key_Wake, 0xff },
});
// clang-format on

// clang-format off
static constexpr auto shifted_evdev_key_map = to_array<KeyCodeEntry const>({
    // 0x00-0x0f
    { Key_Invalid, 0xff },           { Key_Escape, 0x01 },            { Key_ExclamationPoint, 0x02 },  { Key_AtSign, 0x03 },
    { Key_Hashtag, 0x04 },           { Key_Dollar, 0x05 },            { Key_Percent, 0x06 },           { Key_Circumflex, 0x07 },
    { Key_Ampersand, 0x08 },         { Key_Asterisk, 0x09 },          { Key_LeftParen, 0x0a },         { Key_RightParen, 0x0b },
    { Key_Underscore, 0x0c },        { Key_Plus, 0x0d },              { Key_Backspace, 0x0e },         { Key_Tab, 0x0f },

    // 0x10-0x1f
    { Key_Q, 0x10 },                 { Key_W, 0x11 },                 { Key_E, 0x12 },                 { Key_R, 0x13 },
    { Key_T, 0x14 },                 { Key_Y, 0x15 },                 { Key_U, 0x16 },                 { Key_I, 0x17 },
    { Key_O, 0x18 },                 { Key_P, 0x19 },                 { Key_LeftBrace, 0x1a },         { Key_RightBrace, 0x1b },
    { Key_Return, 0x1c },            { Key_LeftControl, 0xff },       { Key_A, 0x1e },                 { Key_S, 0x1f },

    // 0x20-0x2f
    { Key_D, 0x20 },                 { Key_F, 0x21 },                 { Key_G, 0x22 },                 { Key_H, 0x23 },
    { Key_J, 0x24 },                 { Key_K, 0x25 },                 { Key_L, 0x26 },                 { Key_Colon, 0x27 },
    { Key_DoubleQuote, 0x28 },       { Key_Tilde, 0x29 },             { Key_LeftShift, 0xff },         { Key_Pipe, 0x2b },
    { Key_Z, 0x2c },                 { Key_X, 0x2d },                 { Key_C, 0x2e },                 { Key_V, 0x2f },

    // 0x30-0x3f
    { Key_B, 0x30 },                 { Key_N, 0x31 },                 { Key_M, 0x32 },                 { Key_LessThan, 0x33 },
    { Key_GreaterThan, 0x34 },       { Key_QuestionMark, 0x35 },      { Key_RightShift, 0xff },        { Key_Asterisk, 0x37 },
    { Key_LeftAlt, 0xff },           { Key_Space, 0x39 },             { Key_CapsLock, 0xff },          { Key_F1, 0xff },
    { Key_F2, 0xff },                { Key_F3, 0xff },                { Key_F4, 0xff },                { Key_F5, 0xff },

    // 0x40-0x4f
    { Key_F6, 0xff },                { Key_F7, 0xff },                { Key_F8, 0xff },                { Key_F9, 0xff },
    { Key_F10, 0xff },               { Key_NumLock, 0xff },           { Key_ScrollLock, 0xff },        { Key_Home, 0xff },
    { Key_Up, 0xff },                { Key_PageUp, 0xff },            { Key_Minus, 0x4a },             { Key_Left, 0xff },
    { Key_Invalid, 0xff },           { Key_Right, 0xff },             { Key_Plus, 0x4e },              { Key_End, 0xff },

    // 0x50-0x5f
    { Key_Down, 0xff },              { Key_PageDown, 0xff },          { Key_Insert, 0xff },            { Key_Delete, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Pipe, 0x56 },              { Key_F11, 0xff },
    { Key_F12, 0xff },               { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
    { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },           { Key_Invalid, 0xff },
});
// clang-format on

}
