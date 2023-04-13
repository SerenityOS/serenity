/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2021, Edwin Hoksberg <mail@edwinhoksberg.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Types.h>
#include <Kernel/API/Ioctl.h>
#include <Kernel/API/KeyCode.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Devices/HID/KeyboardDevice.h>
#include <Kernel/Scheduler.h>
#include <Kernel/Sections.h>
#include <Kernel/TTY/ConsoleManagement.h>
#include <Kernel/TTY/VirtualConsole.h>
#include <Kernel/WorkQueue.h>

namespace Kernel {

// clang-format off
static constexpr KeyCodeEntry unshifted_scan_code_set1_key_map[0x80] = {
    { Key_Invalid, 0xFF },    { Key_Escape, 1 },        { Key_1, 2 },                 { Key_2, 3 },
    { Key_3, 4 },             { Key_4, 5 },             { Key_5, 6 },                 { Key_6, 7 },
    { Key_7, 8 },             { Key_8, 9 },             { Key_9, 0x0A },              { Key_0, 0x0B },
    { Key_Minus, 0x0C },      { Key_Equal, 0x0D },      { Key_Backspace, 0x0E },      { Key_Tab, 0x0F },
    { Key_Q, 0x10 },          { Key_W, 0x11 },          { Key_E, 0x12 },              { Key_R, 0x13 },
    { Key_T, 0x14 },          { Key_Y, 0x15 },          { Key_U, 0x16 },              { Key_I, 0x17 },
    { Key_O, 0x18 },          { Key_P, 0x19 },          { Key_LeftBracket, 0x1A },    { Key_RightBracket, 0x1B },
    { Key_Return, 0x1C },     { Key_Control, 0x1D },    { Key_A, 0x1E },              { Key_S, 0x1F },
    { Key_D, 0x20 },          { Key_F, 0x21 },          { Key_G, 0x22 },              { Key_H, 0x23 },
    { Key_J, 0x24 },          { Key_K, 0x25 },          { Key_L, 0x26 },              { Key_Semicolon, 0x27 },
    { Key_Apostrophe, 0x28 }, { Key_Backtick, 0x29 },   { Key_LeftShift, 0xFF },      { Key_Backslash, 0x2B },
    { Key_Z, 0x2C },          { Key_X, 0x2D },          { Key_C, 0x2E },              { Key_V, 0x2F },
    { Key_B, 0x30 },          { Key_N, 0x31 },          { Key_M, 0x32 },              { Key_Comma, 0x33 },
    { Key_Period, 0x34 },     { Key_Slash, 0x35 },      { Key_RightShift, 0xFF },     { Key_Asterisk, 0x37 },
    { Key_Alt, 0xFF },        { Key_Space, 0x39 },      { Key_CapsLock, 0xFF },       { Key_F1, 0xFF },
    { Key_F2, 0xFF },         { Key_F3, 0xFF },         { Key_F4, 0xFF },             { Key_F5, 0xFF },
    { Key_F6, 0xFF },         { Key_F7, 0xFF },         { Key_F8, 0xFF },             { Key_F9, 0xFF },
    { Key_F10, 0xFF },        { Key_NumLock, 0xFF },    { Key_ScrollLock, 0xFF },     { Key_Home, 0xFF },
    { Key_Up, 0xFF },         { Key_PageUp, 0xFF },     { Key_Minus, 0x4A },          { Key_Left, 0xFF },
    { Key_Invalid, 0xFF },    { Key_Right, 0xFF },      { Key_Plus, 0xFF },           { Key_End, 0xFF },
    { Key_Down, 0xFF },       { Key_PageDown, 0xFF },   { Key_Insert, 0xFF },         { Key_Delete, 0xFF },
    { Key_Invalid, 0xFF },    { Key_Invalid, 0xFF },    { Key_Backslash, 0x56 },      { Key_F11, 0xFF },
    { Key_F12, 0xFF },        { Key_Invalid, 0xFF },    { Key_Invalid, 0xFF },        { Key_Super, 0xFF },
    { Key_Invalid, 0xFF },    { Key_Menu, 0xFF },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry shifted_scan_code_set1_key_map[0x100] = {
    { Key_Invalid, 0xFF },        { Key_Escape, 1 },                    { Key_Escape, 2 },             { Key_AtSign, 3 },
    { Key_Hashtag, 4 },           { Key_Dollar, 5 },                    { Key_Percent, 6 },            { Key_Circumflex, 7 },
    { Key_Ampersand, 8 },         { Key_Asterisk, 9 },                  { Key_LeftParen, 0x0A },       { Key_RightParen, 0x0B },
    { Key_Underscore, 0xC },      { Key_Plus, 0xFF },                   { Key_Backspace, 0x0E },          { Key_Tab, 0x0F },
    { Key_Q, 0x10 },              { Key_W, 0x11 },                      { Key_E, 0x12 },                  { Key_R, 0x13 },
    { Key_T, 0x14 },              { Key_Y, 0x15 },                      { Key_U, 0x16 },                  { Key_I, 0x17 },
    { Key_O, 0x18 },              { Key_P, 0x19 },                      { Key_LeftBrace, 0x1A },          { Key_RightBrace, 0x1B },
    { Key_Return, 0x1C },         { Key_Control, 0x1D },                { Key_A, 0x1E },                  { Key_S, 0x1F },
    { Key_D, 0x20 },              { Key_F, 0x21 },                      { Key_G, 0x22 },                  { Key_H, 0x23 },
    { Key_J, 0x24 },              { Key_K, 0x25 },                      { Key_L, 0x26 },                  { Key_Colon, 0x27 },
    { Key_DoubleQuote, 0x28 },    { Key_Tilde, 0x29 },                  { Key_LeftShift, 0xFF },          { Key_Pipe, 0x2B },
    { Key_Z, 0x2C },              { Key_X, 0x2D },                      { Key_C, 0x2E },                  { Key_V, 0x2F },
    { Key_B, 0x30 },              { Key_N, 0x31 },                      { Key_M, 0x32 },                  { Key_LessThan, 0x33 },
    { Key_GreaterThan, 0x34 },    { Key_QuestionMark, 0x35 },           { Key_RightShift, 0x36 },         { Key_Asterisk, 0x37 },
    { Key_Alt, 0x38 },            { Key_Space, 0x39 },                  { Key_CapsLock, 0x3A },           { Key_F1, 0x3B },
    { Key_F2, 0x3C },             { Key_F3, 0x3D },                     { Key_F4, 0x3E },                 { Key_F5, 0x3F },
    { Key_F6, 0x40 },             { Key_F7, 0x41 },                     { Key_F8, 0x42 },                 { Key_F9, 0x43 },
    { Key_F10, 0x44 },            { Key_NumLock, 0x45 },                { Key_ScrollLock, 0x46 },         { Key_Home, 0x47 },
    { Key_Up, 0x48 },             { Key_PageUp, 0x49 },                 { Key_Minus, 0x0C },              { Key_Left, 0x4B },
    { Key_Invalid, 0xFF },        { Key_Right, 0xFF },                  { Key_Plus, 0xFF },               { Key_End, 0xFF },
    { Key_Down, 0xFF },           { Key_PageDown, 0xFF },               { Key_Insert, 0xFF },             { Key_Delete, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },                { Key_Pipe, 0x56 },               { Key_F11, 0xFF },
    { Key_F12, 0xFF },            { Key_Invalid, 0xFF },                { Key_Invalid, 0xFF },            { Key_Super, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Menu, 0xFF },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry unshifted_simple_scan_code_set2_key_map_with_key_num_pad[0x84] = {
    { Key_Invalid, 0xFF },        { Key_F9, 0x43 },             { Key_Invalid, 0xFF },          { Key_F5, 0x3F },
    { Key_F3, 0x3D },             { Key_F1, 0x3B },             { Key_F2, 0x3C },               { Key_F12, 0xFF },
    { Key_Invalid, 0xFF },        { Key_F10, 0x44 },            { Key_F8, 0x42 },               { Key_F6, 0x40 },
    { Key_F4, 0x3E },             { Key_Tab, 0x0F },            { Key_Backtick, 0x29 },         { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Alt, 0x38 },            { Key_LeftShift, 0x2A },        { Key_Invalid, 0xFF },
    { Key_Control, 0x1D },        { Key_Q, 0x10 },              { Key_1, 2 },                   { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Z, 0x2C },                { Key_S, 0x1F },
    { Key_A, 0x1E },              { Key_W, 0x11 },              { Key_2, 3 },                   { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_C, 0x2E },              { Key_X, 0x2D },                { Key_D, 0x20 },
    { Key_E, 0x12 },              { Key_4, 5 },                 { Key_3, 4 },                   { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Space, 0x39 },          { Key_V, 0x2F },                { Key_F, 0x21 },
    { Key_T, 0x14 },              { Key_R, 0x13 },              { Key_5, 6 },                   { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_N, 0x31 },              { Key_B, 0x30 },                { Key_H, 0x23 },
    { Key_G, 0x22 },              { Key_Y, 0x15 },              { Key_6, 7 },                   { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_M, 0x32 },                { Key_J, 0x24 },
    { Key_U, 0x16 },              { Key_7, 8 },                 { Key_8, 9 },                   { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Comma, 0x33 },          { Key_K, 0x25 },                { Key_I, 0x17 },
    { Key_O, 0x18 },              { Key_0, 0x0B },              { Key_9, 0x0A },                { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Period, 0x34 },         { Key_Slash, 0x35 },            { Key_L, 0x26 },
    { Key_Semicolon, 0x27 },      { Key_P, 0x19 },              { Key_Minus, 0x0C },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Apostrophe, 0x28 },       { Key_Invalid, 0xFF },
    { Key_LeftBracket, 0x1A },    { Key_Equal, 0x0D },          { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_CapsLock, 0x3A },       { Key_RightShift, 0x36 },     { Key_Return, 0x1C },           { Key_RightBracket, 0x1B },
    { Key_Invalid, 0xFF },        { Key_Backslash, 0x2B },      { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },        { Key_Invalid, 0xFF },
    // Keypad numbers from here
    { Key_Invalid, 0xFF },        { Key_1, 2 },                 { Key_Invalid, 0xFF },          { Key_4, 5 },
    { Key_7, 8 },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_0, 0x0B },              { Key_Period, 0x34 },         { Key_2, 3 },                   { Key_5, 6 },
    { Key_6, 7 },                 { Key_8, 9 },                 { Key_Escape, 1 },              { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0xFF },           { Key_3, 4 },                   { Key_Minus, 0x0C },
    { Key_Asterisk, 0x37 },       { Key_9, 0x0A },              { Key_ScrollLock, 0x46 },       { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },          { Key_F7, 0x41 },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry unshifted_simple_scan_code_set2_key_map_with_disabled_key_num_pad[0x84] = {
    { Key_Invalid, 0xFF },        { Key_F9, 0x43 },             { Key_Invalid, 0xFF },        { Key_F5, 0x3F },
    { Key_F3, 0x3D },             { Key_F1, 0x3B },             { Key_F2, 0x3C },             { Key_F12, 0xFF },
    { Key_Invalid, 0xFF },        { Key_F10, 0x44 },            { Key_F8, 0x42 },             { Key_F6, 0x40 },
    { Key_F4, 0x3E },             { Key_Tab, 0x0F },            { Key_Backtick, 0x29 },       { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Alt, 0x38 },            { Key_LeftShift, 0x2A },      { Key_Invalid, 0xFF },
    { Key_Control, 0x1D },        { Key_Q, 0x10 },              { Key_1, 2 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Z, 0x2C },              { Key_S, 0x1F },
    { Key_A, 0x1E },              { Key_W, 0x11 },              { Key_2, 3 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_C, 0x2E },              { Key_X, 0x2D },              { Key_D, 0x20 },
    { Key_E, 0x12 },              { Key_4, 5 },                 { Key_3, 4 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Space, 0x39 },          { Key_V, 0x2F },              { Key_F, 0x21 },
    { Key_T, 0x14 },              { Key_R, 0x13 },              { Key_5, 6 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_N, 0x31 },              { Key_B, 0x30 },              { Key_H, 0x23 },
    { Key_G, 0x22 },              { Key_Y, 0x15 },              { Key_6, 7 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_M, 0x32 },              { Key_J, 0x24 },
    { Key_U, 0x16 },              { Key_7, 8 },                 { Key_8, 9 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Comma, 0x33 },          { Key_K, 0x25 },              { Key_I, 0x17 },
    { Key_O, 0x18 },              { Key_0, 0x0B },              { Key_9, 0x0A },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Period, 0x34 },         { Key_Slash, 0x35 },          { Key_L, 0x26 },
    { Key_Semicolon, 0x27 },      { Key_P, 0x19 },              { Key_Underscore, 0x0C },     { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Apostrophe, 0x28 },     { Key_Invalid, 0xFF },
    { Key_LeftBracket, 0x1A },    { Key_Equal, 0x0D },          { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_CapsLock, 0x3A },       { Key_RightShift, 0x36 },     { Key_Return, 0x1C },         { Key_RightBracket, 0x1B },
    { Key_Invalid, 0xFF },        { Key_Backslash, 0x2B },      { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },      { Key_Invalid, 0xFF },
    // Keypad numbers from here, and disabled or converted to arrows
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Left, 0x4B },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },         { Key_Down, 0xFF },           { Key_Invalid, 0xFF },
    { Key_Right, 0xFF },          { Key_Up, 0x48 },             { Key_Escape, 1 },            { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0xFF },           { Key_Invalid, 0xFF },        { Key_Minus, 0x0C },
    { Key_Asterisk, 0x37 },       { Key_Invalid, 0xFF },        { Key_ScrollLock, 0x46 },     { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_F7, 0x41 },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry shifted_simple_scan_code_set2_key_map_with_key_num_pad[0x84] = {
    { Key_Invalid, 0xFF },        { Key_F9, 0x43 },             { Key_Invalid, 0xFF },            { Key_F5, 0x3F },
    { Key_F3, 0x3D },             { Key_F1, 0x3B },             { Key_F2, 0x3C },                 { Key_F12, 0xFF },
    { Key_Invalid, 0xFF },        { Key_F10, 0x44 },            { Key_F8, 0x42 },                 { Key_F6, 0x40 },
    { Key_F4, 0x3E },             { Key_Tab, 0x0F },            { Key_Backtick, 0x29 },           { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Alt, 0x38 },            { Key_LeftShift, 0x2A },          { Key_Invalid, 0xFF },
    { Key_Control, 0x1D },        { Key_Slash, 0x35 },          { Key_Escape, 2 },                { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Z, 0x2C },                  { Key_S, 0x1F },
    { Key_A, 0x1E },              { Key_W, 0x11 },              { Key_AtSign, 3 },                { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_C, 0x2E },              { Key_X, 0x2D },                  { Key_D, 0x20 },
    { Key_E, 0x12 },              { Key_Dollar, 5 },            { Key_Hashtag, 4 },               { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Space, 0x39 },          { Key_V, 0x2F },                  { Key_F, 0x21 },
    { Key_T, 0x14 },              { Key_R, 0x13 },              { Key_Percent, 6 },               { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_N, 0x31 },              { Key_B, 0x30 },                  { Key_H, 0x23 },
    { Key_G, 0x22 },              { Key_Y, 0x15 },              { Key_Circumflex, 7 },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_M, 0x32 },                  { Key_J, 0x24 },
    { Key_U, 0x16 },              { Key_Ampersand, 8 },         { Key_Asterisk, 0x37 },           { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_LessThan, 0x33 },       { Key_K, 0x25 },                  { Key_I, 0x17 },
    { Key_O, 0x18 },              { Key_RightParen, 0x0B },     { Key_LeftParen, 0x0A },          { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_GreaterThan, 0x34 },    { Key_Slash, 0x35 },              { Key_L, 0x26 },
    { Key_Semicolon, 0x27 },      { Key_P, 0x19 },              { Key_Minus, 0x0C },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_DoubleQuote, 0x28 },        { Key_Invalid, 0xFF },
    { Key_LeftBrace, 0x1A },      { Key_Plus, 0xFF },           { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_CapsLock, 0x3A },       { Key_RightShift, 0x36 },     { Key_Return, 0x1C },             { Key_RightBrace, 0x1B },
    { Key_Invalid, 0xFF },        { Key_Pipe, 0x2B },           { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },          { Key_Invalid, 0xFF },
    // Keypad numbers from here
    { Key_Invalid, 0xFF },        { Key_1, 2 },                 { Key_Invalid, 0xFF },            { Key_4, 5 },
    { Key_7, 8 },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_0, 0x0B },              { Key_Period, 0x34 },         { Key_2, 3 },                     { Key_5, 6 },
    { Key_6, 7 },                 { Key_8, 9 },                 { Key_Escape, 1 },                { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0xFF },           { Key_3, 4 },                     { Key_Minus, 0x0C },
    { Key_Asterisk, 0x37 },       { Key_9, 0x0A },              { Key_ScrollLock, 0x46 },         { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_F7, 0x41 },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry shifted_simple_scan_code_set2_key_map_with_disabled_key_num_pad[0x84] = {
    { Key_Invalid, 0xFF },        { Key_F9, 0x43 },             { Key_Invalid, 0xFF },              { Key_F5, 0x3F },
    { Key_F3, 0x3D },             { Key_F1, 0x3B },             { Key_F2, 0x3C },                   { Key_F12, 0xFF },
    { Key_Invalid, 0xFF },        { Key_F10, 0x44 },            { Key_F8, 0x42 },                   { Key_F6, 0x40 },
    { Key_F4, 0x3E },             { Key_Tab, 0x0F },            { Key_Backtick, 0x29 },             { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Alt, 0x38 },            { Key_LeftShift, 0x2A },            { Key_Invalid, 0xFF },
    { Key_Control, 0x1D },        { Key_Slash, 0x35 },          { Key_Escape, 2 },                  { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Z, 0x2C },                    { Key_S, 0x1F },
    { Key_A, 0x1E },              { Key_W, 0x11 },              { Key_AtSign, 3 },                  { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_C, 0x2E },              { Key_X, 0x2D },                    { Key_D, 0x20 },
    { Key_E, 0x12 },              { Key_Dollar, 5 },            { Key_Hashtag, 4 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Space, 0x39 },          { Key_V, 0x2F },                    { Key_F, 0x21 },
    { Key_T, 0x14 },              { Key_R, 0x13 },              { Key_Percent, 6 },                 { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_N, 0x31 },              { Key_B, 0x30 },                    { Key_H, 0x23 },
    { Key_G, 0x22 },              { Key_Y, 0x15 },              { Key_Circumflex, 7 },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_M, 0x32 },                    { Key_J, 0x24 },
    { Key_U, 0x16 },              { Key_Ampersand, 8 },         { Key_Asterisk, 0x37 },             { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_LessThan, 0xFF},        { Key_K, 0x25 },                    { Key_I, 0x17 },
    { Key_O, 0x18 },              { Key_RightParen, 0x0B },     { Key_LeftParen, 0x0A },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Period, 0x34 },         { Key_Slash, 0x35 },                { Key_L, 0x26 },
    { Key_Semicolon, 0x27 },      { Key_P, 0x19 },              { Key_Underscore, 0x0C },           { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_DoubleQuote, 0x28 },          { Key_Invalid, 0xFF },
    { Key_LeftBrace, 0x1A },      { Key_Plus, 0xFF },           { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_CapsLock, 0x3A },       { Key_RightShift, 0x36 },     { Key_Return, 0x1C },               { Key_RightBrace, 0x1B },
    { Key_Invalid, 0xFF },        { Key_Pipe, 0x2B },           { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },            { Key_Invalid, 0xFF },
    // Keypad numbers from here, and disabled or converted to arrows
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_Left, 0x4B },
    { Key_7, 8 },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },         { Key_Down, 0xFF },                 { Key_Invalid, 0xFF },
    { Key_Right, 0xFF },          { Key_Up, 0x48 },             { Key_Escape, 1 },                  { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0xFF },           { Key_Invalid, 0xFF },              { Key_Minus, 0x0C },
    { Key_Asterisk, 0x37 },       { Key_Invalid, 0xFF },        { Key_ScrollLock, 0x46 },           { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_F7, 0x41 },
};
// clang-format on

// Note: First scan code starts at actual 0xE0, 0x10, but we start from 0xE0, 0x0
// Note: All keycode are for pressing buttons, not releasing...
// clang-format off
static constexpr KeyCodeEntry unshifted_scan_code_set2_e0_key_map[0x80] = {
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_WWWSearch, 0xFF },      { Key_RightAlt, 0xFF },           { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_RightControl, 0xFF },   { Key_PreviousTrack, 0xFF },      { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_WWWFavourites, 0xFF },  { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_LeftGUI, 0xFF },
    { Key_WWWRefresh, 0xFF },     { Key_VolumeDown, 0xFF },         { Key_Invalid, 0xFF },        { Key_Mute, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_RightGUI, 0xFF },
    { Key_WWWStop, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Calculator, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Apps, 0xFF },
    { Key_WWWForward, 0xFF },     { Key_Invalid, 0xFF },            { Key_VolumeUp, 0xFF },       { Key_Invalid, 0xFF },
    { Key_PlayPause, 0xFF },      { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Power, 0xFF },
    { Key_WWWBack, 0xFF },        { Key_Invalid, 0xFF },            { Key_WWWHome, 0xFF },        { Key_Stop, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Sleep, 0xFF },
    { Key_MyComputer, 0xFF },     { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Email, 0xFF },          { Key_Invalid, 0xFF },            { Key_Slash, 0x35 },          { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_NextTrack, 0xFF },          { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_MediaSelect, 0xFF},     { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Return, 0x1C },         { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Wake, 0xFF },           { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_End, 0xFF },                { Key_Invalid, 0xFF },        { Key_CursorLeft, 0xFF },
    { Key_Home, 0x47 },           { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },             { Key_CursorDown, 0xFF },     { Key_Invalid, 0xFF },
    { Key_CursorRight, 0xFF },    { Key_CursorUp, 0xFF },           { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_PageDown, 0xFF },       { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_PageUp, 0x49 },             { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
};
// clang-format on

KeyboardDevice::RawKeyEvent KeyboardDevice::handle_scan_code_input_event_set1(ScanCodeEvent event)
{
    RawKeyEvent key_event {};
    VERIFY(event.sent_scan_code_set == ScanCodeSet::Set1);
    bool has_e0_prefix = event.scan_code_bytes[0] == 0xe0;
    if (has_e0_prefix)
        VERIFY(event.bytes_count == 2);
    else
        VERIFY(event.bytes_count == 1);

    u8 byte = has_e0_prefix ? event.scan_code_bytes[1] : event.scan_code_bytes[0];
    bool pressed = !(byte & 0x80);
    u8 ch = byte & 0x7f;

    key_event.is_press_down = pressed;

    m_entropy_source.add_random_event(byte);

    switch (ch) {
    case 0x38:
        if (has_e0_prefix)
            update_modifier(Mod_AltGr, key_event.is_press());
        else
            update_modifier(Mod_Alt, key_event.is_press());
        break;
    case 0x1d:
        update_modifier(Mod_Ctrl, key_event.is_press());
        break;
    case 0x5b:
        m_left_super_pressed = key_event.is_press();
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x5c:
        m_right_super_pressed = key_event.is_press();
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x2a:
        m_left_shift_pressed = key_event.is_press();
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case 0x36:
        m_right_shift_pressed = key_event.is_press();
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    }

    key_event.code_entry = (m_modifiers & Mod_Shift) ? shifted_scan_code_set1_key_map[ch] : unshifted_scan_code_set1_key_map[ch];
    key_event.scancode = has_e0_prefix ? 0xe000 + ch : ch;
    return key_event;
}

KeyboardDevice::RawKeyEvent KeyboardDevice::handle_scan_code_input_event_set2(ScanCodeEvent event)
{
    VERIFY(event.sent_scan_code_set == ScanCodeSet::Set2);

    auto get_key_from_standard_key_map = [this](u8 byte) -> KeyCodeEntry {
        if (!(m_modifiers & Mod_Shift))
            return (m_num_lock_on) ? unshifted_simple_scan_code_set2_key_map_with_key_num_pad[byte] : unshifted_simple_scan_code_set2_key_map_with_disabled_key_num_pad[byte];
        return (m_num_lock_on) ? shifted_simple_scan_code_set2_key_map_with_key_num_pad[byte] : shifted_simple_scan_code_set2_key_map_with_disabled_key_num_pad[byte];
    };

    RawKeyEvent key_event {};
    if (event.bytes_count == 1) {
        auto byte = event.scan_code_bytes[0];
        key_event.code_entry = get_key_from_standard_key_map(byte);
        key_event.scancode = byte;
        key_event.is_press_down = true;
        m_entropy_source.add_random_event(byte);
    } else if (event.bytes_count == 2) {
        auto byte_prefix = event.scan_code_bytes[0];
        auto byte = event.scan_code_bytes[1];
        if (byte_prefix == 0xe0) {
            key_event.code_entry = unshifted_scan_code_set2_e0_key_map[byte];
            key_event.scancode = 0xe000 + byte;
            key_event.is_press_down = true;
        } else if (byte_prefix == 0xf0) {
            key_event.code_entry = get_key_from_standard_key_map(byte);
            key_event.scancode = 0xf000 + byte;
        } else {
            VERIFY_NOT_REACHED();
        }
        m_entropy_source.add_random_event(byte);
    } else if (event.bytes_count == 3) {
        auto first_byte_prefix = event.scan_code_bytes[0];
        auto second_byte_prefix = event.scan_code_bytes[1];
        VERIFY(first_byte_prefix == 0xe0);
        VERIFY(second_byte_prefix == 0xf0);
        auto byte = event.scan_code_bytes[2];
        key_event.code_entry = unshifted_scan_code_set2_e0_key_map[byte];
        key_event.scancode = 0xe0f000 + byte;
        m_entropy_source.add_random_event(byte);
    } else if (event.bytes_count == 4) {
        // 0xE0, 0x12, 0xE0, 0x7C - print screen pressed
        auto first_byte_prefix = event.scan_code_bytes[0];
        auto second_byte_prefix = event.scan_code_bytes[1];
        auto third_byte_prefix = event.scan_code_bytes[2];
        auto fourth_byte_prefix = event.scan_code_bytes[3];
        VERIFY(first_byte_prefix == 0xe0);
        VERIFY(second_byte_prefix == 0x12);
        VERIFY(third_byte_prefix == 0xe0);
        VERIFY(fourth_byte_prefix == 0x7c);
        key_event.code_entry = KeyCodeEntry { Key_PrintScreen, 0xFF };
        key_event.scancode = 0xe012e07c;
        key_event.is_press_down = true;
    } else if (event.bytes_count == 6) {
        // 0xE0, 0xF0, 0x7C, 0xE0, 0xF0, 0x12 - print screen released
        auto first_byte_prefix = event.scan_code_bytes[0];
        auto second_byte_prefix = event.scan_code_bytes[1];
        auto third_byte_prefix = event.scan_code_bytes[2];
        auto fourth_byte_prefix = event.scan_code_bytes[3];
        auto fifth_byte_prefix = event.scan_code_bytes[4];
        auto sixth_byte_prefix = event.scan_code_bytes[5];
        VERIFY(first_byte_prefix == 0xe0);
        VERIFY(second_byte_prefix == 0xf0);
        VERIFY(third_byte_prefix == 0x7c);
        VERIFY(fourth_byte_prefix == 0xe0);
        VERIFY(fifth_byte_prefix == 0xf0);
        VERIFY(sixth_byte_prefix == 0x12);
        key_event.code_entry = KeyCodeEntry { Key_PrintScreen, 0xFF };
        key_event.scancode = 0xe0f07ce0f012;
    } else if (event.bytes_count == 8) {
        // 0xE1, 0x14, 0x77, 0xE1, 0xF0, 0x14, 0xF0, 0x77 - pause pressed
        auto first_byte_prefix = event.scan_code_bytes[0];
        auto second_byte_prefix = event.scan_code_bytes[1];
        auto third_byte_prefix = event.scan_code_bytes[2];
        auto fourth_byte_prefix = event.scan_code_bytes[3];
        auto fifth_byte_prefix = event.scan_code_bytes[4];
        auto sixth_byte_prefix = event.scan_code_bytes[5];
        auto seventh_byte_prefix = event.scan_code_bytes[6];
        auto eight_byte_prefix = event.scan_code_bytes[7];
        VERIFY(first_byte_prefix == 0xe1);
        VERIFY(second_byte_prefix == 0x14);
        VERIFY(third_byte_prefix == 0x77);
        VERIFY(fourth_byte_prefix == 0xe1);
        VERIFY(fifth_byte_prefix == 0xf0);
        VERIFY(sixth_byte_prefix == 0x14);
        VERIFY(seventh_byte_prefix == 0xf0);
        VERIFY(eight_byte_prefix == 0x77);
        key_event.code_entry = KeyCodeEntry { Key_PauseBreak, 0xFF };
        key_event.scancode = 0xe11477e1f014f077;
    }

    switch (key_event.code_entry.key_code) {
    case Key_RightAlt:
        update_modifier(Mod_AltGr, key_event.is_press());
        break;
    case Key_Alt:
        update_modifier(Mod_Alt, key_event.is_press());
        break;
    case Key_Control:
        update_modifier(Mod_Ctrl, key_event.is_press());
        break;
    case Key_Super:
        m_left_super_pressed = key_event.is_press();
        update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case Key_LeftShift:
        m_left_shift_pressed = key_event.is_press();
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case Key_RightShift:
        m_right_shift_pressed = key_event.is_press();
        update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    default:
        break;
    }

    return key_event;
}

void KeyboardDevice::handle_scan_code_input_event(ScanCodeEvent event)
{
    RawKeyEvent raw_event {};
    if (event.sent_scan_code_set == ScanCodeSet::Set1) {
        raw_event = handle_scan_code_input_event_set1(event);
    } else if (event.sent_scan_code_set == ScanCodeSet::Set2) {
        raw_event = handle_scan_code_input_event_set2(event);
    } else if (event.sent_scan_code_set == ScanCodeSet::Set3) {
        // FIXME: Implement support for scan code set 3!
        VERIFY_NOT_REACHED();
    } else {
        VERIFY_NOT_REACHED();
    }

    KeyEvent queued_event = {
        .key = raw_event.code_entry.key_code,
        .map_entry_index = raw_event.code_entry.map_entry_index,
        .scancode = raw_event.scancode,
        .flags = raw_event.is_press() ? (u8)Is_Press : (u8)0,
    };

    if (queued_event.key == Key_NumLock && queued_event.is_press())
        m_num_lock_on = !m_num_lock_on;

    // NOTE: This piece of code is needed for the ScanCodeSet::Set1 when NumLock is enabled
    // because we don't have special mappings when NumLock is enabled for this scan code set.
    // Scan code set 2 handling code in handle_scan_code_input_event_set2() already handles this fine.
    if (event.sent_scan_code_set == ScanCodeSet::Set1 && m_num_lock_on && !(queued_event.scancode & 0xe000)) {
        if (queued_event.scancode >= 0x47 && queued_event.scancode <= 0x53) {
            u8 index = queued_event.scancode - 0x47;
            constexpr KeyCode numpad_key_map[13] = { Key_7, Key_8, Key_9, Key_Invalid, Key_4, Key_5, Key_6, Key_Invalid, Key_1, Key_2, Key_3, Key_0, Key_Comma };
            KeyCode newKey = numpad_key_map[index];

            if (newKey != Key_Invalid) {
                queued_event.key = newKey;
            }
        }
    }

    queued_event.flags |= m_modifiers;
    queued_event.caps_lock_on = m_caps_lock_on;
    if (raw_event.code_entry.map_entry_index != 0xFF)
        queued_event.code_point = HIDManagement::the().get_char_from_character_map(queued_event, raw_event.code_entry.map_entry_index);

    // NOTE: This piece of code is needed for the ScanCodeSet::Set1 to ensure some keys could
    // function properly.
    if (event.sent_scan_code_set == ScanCodeSet::Set1) {
        if ((queued_event.scancode & 0xe000) && queued_event.key == Key_Slash) {
            // If Key_Slash (scancode = 0x35) mapped to other form "/", we fix num pad key of "/" with this case.
            queued_event.code_point = '/';
        } else if ((queued_event.scancode & 0xe000) && queued_event.key != Key_Return) {
            // Except for `keypad-/` and 'keypad-return', all e0 scan codes are not actually characters. i.e., `keypad-0` and
            // `Insert` have the same scancode except for the prefix, but insert should not have a code_point.
            queued_event.code_point = 0;
        }
    }

    if (queued_event.is_press() && (m_modifiers == (Mod_Alt | Mod_Shift) || m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift)) && queued_event.key == Key_F12) {
        // Alt+Shift+F12 pressed, dump some kernel state to the debug console.
        ConsoleManagement::the().switch_to_debug();
        Scheduler::dump_scheduler_state(m_modifiers == (Mod_Ctrl | Mod_Alt | Mod_Shift));
    }

    if (queued_event.is_press() && (m_modifiers & Mod_Alt) != 0 && queued_event.key >= Key_1 && queued_event.key <= Key_1 + ConsoleManagement::s_max_virtual_consoles + 1) {
        // FIXME: Do something sanely here if we can't allocate a work queue?
        auto key = queued_event.key;
        MUST(g_io_work->try_queue([key]() {
            ConsoleManagement::the().switch_to(key - Key_1);
        }));
    }

    // If using a non-QWERTY layout, queued_event.key needs to be updated to be the same as event.code_point
    KeyCode mapped_key = code_point_to_key_code(queued_event.code_point);
    if (mapped_key != KeyCode::Key_Invalid)
        queued_event.key = mapped_key;

    if (!g_caps_lock_remapped_to_ctrl && queued_event.key == Key_CapsLock && queued_event.is_press())
        m_caps_lock_on = !m_caps_lock_on;

    if (g_caps_lock_remapped_to_ctrl && queued_event.key == Key_CapsLock) {
        m_caps_lock_to_ctrl_pressed = queued_event.is_press();
        update_modifier(Mod_Ctrl, m_caps_lock_to_ctrl_pressed);
    }

    {
        SpinlockLocker locker(HIDManagement::the().m_client_lock);
        if (HIDManagement::the().m_client)
            HIDManagement::the().m_client->on_key_pressed(queued_event);
    }

    {
        SpinlockLocker lock(m_queue_lock);
        m_queue.enqueue(queued_event);
    }

    evaluate_block_conditions();
}

ErrorOr<NonnullRefPtr<KeyboardDevice>> KeyboardDevice::try_to_initialize()
{
    return *TRY(DeviceManagement::try_create_device<KeyboardDevice>());
}

// FIXME: UNMAP_AFTER_INIT is fine for now, but for hot-pluggable devices
// like USB keyboards, we need to remove this
UNMAP_AFTER_INIT KeyboardDevice::KeyboardDevice()
    : HIDDevice(85, HIDManagement::the().generate_minor_device_number_for_keyboard())
{
}

// FIXME: UNMAP_AFTER_INIT is fine for now, but for hot-pluggable devices
// like USB keyboards, we need to remove this
UNMAP_AFTER_INIT KeyboardDevice::~KeyboardDevice() = default;

bool KeyboardDevice::can_read(OpenFileDescription const&, u64) const
{
    return !m_queue.is_empty();
}

ErrorOr<size_t> KeyboardDevice::read(OpenFileDescription&, u64, UserOrKernelBuffer& buffer, size_t size)
{
    size_t nread = 0;
    SpinlockLocker lock(m_queue_lock);
    while (nread < size) {
        if (m_queue.is_empty())
            break;
        // Don't return partial data frames.
        if (size - nread < sizeof(Event))
            break;
        auto event = m_queue.dequeue();

        lock.unlock();

        auto result = TRY(buffer.write_buffered<sizeof(Event)>(sizeof(Event), [&](Bytes bytes) {
            memcpy(bytes.data(), &event, sizeof(Event));
            return bytes.size();
        }));
        VERIFY(result == sizeof(Event));
        nread += sizeof(Event);

        lock.lock();
    }
    return nread;
}

ErrorOr<void> KeyboardDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    switch (request) {
    case KEYBOARD_IOCTL_GET_NUM_LOCK: {
        auto output = static_ptr_cast<bool*>(arg);
        return copy_to_user(output, &m_num_lock_on);
    }
    case KEYBOARD_IOCTL_SET_NUM_LOCK: {
        // In this case we expect the value to be a boolean and not a pointer.
        auto num_lock_value = static_cast<u8>(arg.ptr());
        if (num_lock_value != 0 && num_lock_value != 1)
            return EINVAL;
        m_num_lock_on = !!num_lock_value;
        return {};
    }
    case KEYBOARD_IOCTL_GET_CAPS_LOCK: {
        auto output = static_ptr_cast<bool*>(arg);
        return copy_to_user(output, &m_caps_lock_on);
    }
    case KEYBOARD_IOCTL_SET_CAPS_LOCK: {
        auto caps_lock_value = static_cast<u8>(arg.ptr());
        if (caps_lock_value != 0 && caps_lock_value != 1)
            return EINVAL;
        m_caps_lock_on = !!caps_lock_value;
        return {};
    }
    default:
        return EINVAL;
    };
}

}
