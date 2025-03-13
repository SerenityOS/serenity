/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Types.h>
#include <Kernel/Bus/SerialIO/PS2Definitions.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Input/Management.h>
#include <Kernel/Devices/Input/PS2/KeyboardDevice.h>
#include <Kernel/Devices/Input/ScanCodeEvent.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Scheduler.h>
#include <Kernel/Tasks/WorkQueue.h>

namespace Kernel {

// clang-format off
static constexpr KeyCodeEntry unshifted_scan_code_set1_key_map[0x80] = {
    { Key_Invalid, 0xFF },    { Key_Escape, 1 },         { Key_1, 2 },                 { Key_2, 3 },
    { Key_3, 4 },             { Key_4, 5 },              { Key_5, 6 },                 { Key_6, 7 },
    { Key_7, 8 },             { Key_8, 9 },              { Key_9, 0x0A },              { Key_0, 0x0B },
    { Key_Minus, 0x0C },      { Key_Equal, 0x0D },       { Key_Backspace, 0x0E },      { Key_Tab, 0x0F },
    { Key_Q, 0x10 },          { Key_W, 0x11 },           { Key_E, 0x12 },              { Key_R, 0x13 },
    { Key_T, 0x14 },          { Key_Y, 0x15 },           { Key_U, 0x16 },              { Key_I, 0x17 },
    { Key_O, 0x18 },          { Key_P, 0x19 },           { Key_LeftBracket, 0x1A },    { Key_RightBracket, 0x1B },
    { Key_Return, 0x1C },     { Key_LeftControl, 0x1D }, { Key_A, 0x1E },              { Key_S, 0x1F },
    { Key_D, 0x20 },          { Key_F, 0x21 },           { Key_G, 0x22 },              { Key_H, 0x23 },
    { Key_J, 0x24 },          { Key_K, 0x25 },           { Key_L, 0x26 },              { Key_Semicolon, 0x27 },
    { Key_Apostrophe, 0x28 }, { Key_Backtick, 0x29 },    { Key_LeftShift, 0xFF },      { Key_Backslash, 0x2B },
    { Key_Z, 0x2C },          { Key_X, 0x2D },           { Key_C, 0x2E },              { Key_V, 0x2F },
    { Key_B, 0x30 },          { Key_N, 0x31 },           { Key_M, 0x32 },              { Key_Comma, 0x33 },
    { Key_Period, 0x34 },     { Key_Slash, 0x35 },       { Key_RightShift, 0xFF },     { Key_Asterisk, 0x37 },
    { Key_LeftAlt, 0xFF },    { Key_Space, 0x39 },       { Key_CapsLock, 0xFF },       { Key_F1, 0xFF },
    { Key_F2, 0xFF },         { Key_F3, 0xFF },          { Key_F4, 0xFF },             { Key_F5, 0xFF },
    { Key_F6, 0xFF },         { Key_F7, 0xFF },          { Key_F8, 0xFF },             { Key_F9, 0xFF },
    { Key_F10, 0xFF },        { Key_NumLock, 0x45 },     { Key_ScrollLock, 0xFF },     { Key_Home, 0xFF },
    { Key_Up, 0xFF },         { Key_PageUp, 0xFF },      { Key_Minus, 0x4A },          { Key_Left, 0xFF },
    { Key_Invalid, 0xFF },    { Key_Right, 0xFF },       { Key_Plus, 0x4E },           { Key_End, 0xFF },
    { Key_Down, 0xFF },       { Key_PageDown, 0xFF },    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },
    { Key_Invalid, 0xFF },    { Key_Invalid, 0xFF },     { Key_Backslash, 0x56 },      { Key_F11, 0xFF },
    { Key_F12, 0xFF },        { Key_Invalid, 0xFF },     { Key_Invalid, 0xFF },        { Key_LeftSuper, 0xFF },
    { Key_Invalid, 0xFF },    { Key_Menu, 0xFF },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry shifted_scan_code_set1_key_map[0x100] = {
    { Key_Invalid, 0xFF },        { Key_Escape, 1 },                    { Key_ExclamationPoint, 2 },      { Key_AtSign, 3 },
    { Key_Hashtag, 4 },           { Key_Dollar, 5 },                    { Key_Percent, 6 },               { Key_Circumflex, 7 },
    { Key_Ampersand, 8 },         { Key_Asterisk, 9 },                  { Key_LeftParen, 0x0A },          { Key_RightParen, 0x0B },
    { Key_Underscore, 0xC },      { Key_Plus, 0x4E },                   { Key_Backspace, 0x0E },          { Key_Tab, 0x0F },
    { Key_Q, 0x10 },              { Key_W, 0x11 },                      { Key_E, 0x12 },                  { Key_R, 0x13 },
    { Key_T, 0x14 },              { Key_Y, 0x15 },                      { Key_U, 0x16 },                  { Key_I, 0x17 },
    { Key_O, 0x18 },              { Key_P, 0x19 },                      { Key_LeftBrace, 0x1A },          { Key_RightBrace, 0x1B },
    { Key_Return, 0x1C },         { Key_LeftControl, 0x1D },            { Key_A, 0x1E },                  { Key_S, 0x1F },
    { Key_D, 0x20 },              { Key_F, 0x21 },                      { Key_G, 0x22 },                  { Key_H, 0x23 },
    { Key_J, 0x24 },              { Key_K, 0x25 },                      { Key_L, 0x26 },                  { Key_Colon, 0x27 },
    { Key_DoubleQuote, 0x28 },    { Key_Tilde, 0x29 },                  { Key_LeftShift, 0xFF },          { Key_Pipe, 0x2B },
    { Key_Z, 0x2C },              { Key_X, 0x2D },                      { Key_C, 0x2E },                  { Key_V, 0x2F },
    { Key_B, 0x30 },              { Key_N, 0x31 },                      { Key_M, 0x32 },                  { Key_LessThan, 0x33 },
    { Key_GreaterThan, 0x34 },    { Key_QuestionMark, 0x35 },           { Key_RightShift, 0xFF },         { Key_Asterisk, 0x37 },
    { Key_LeftAlt, 0xFF },        { Key_Space, 0x39 },                  { Key_CapsLock, 0xFF },           { Key_F1, 0xFF },
    { Key_F2, 0xFF },             { Key_F3, 0xFF },                     { Key_F4, 0xFF },                 { Key_F5, 0xFF },
    { Key_F6, 0xFF },             { Key_F7, 0xFF },                     { Key_F8, 0xFF },                 { Key_F9, 0xFF },
    { Key_F10, 0xFF },            { Key_NumLock, 0xFF },                { Key_ScrollLock, 0xFF },         { Key_Home, 0xFF },
    { Key_Up, 0xFF },             { Key_PageUp, 0xFF },                 { Key_Minus, 0x4A },              { Key_Left, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Right, 0xFF },                  { Key_Plus, 0x4E },               { Key_End, 0xFF },
    { Key_Down, 0xFF },           { Key_PageDown, 0xFF },               { Key_Insert, 0xFF },             { Key_Delete, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },                { Key_Pipe, 0x56 },               { Key_F11, 0xFF },
    { Key_F12, 0xFF },            { Key_Invalid, 0xFF },                { Key_Invalid, 0xFF },            { Key_LeftSuper, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Menu, 0xFF },
};
// clang-format on

// clang-format off
static constexpr KeyCodeEntry unshifted_simple_scan_code_set2_key_map_with_key_num_pad[0x84] = {
    { Key_Invalid, 0xFF },        { Key_F9, 0x43 },             { Key_Invalid, 0xFF },          { Key_F5, 0x3F },
    { Key_F3, 0x3D },             { Key_F1, 0x3B },             { Key_F2, 0x3C },               { Key_F12, 0xFF },
    { Key_Invalid, 0xFF },        { Key_F10, 0x44 },            { Key_F8, 0x42 },               { Key_F6, 0x40 },
    { Key_F4, 0x3E },             { Key_Tab, 0x0F },            { Key_Backtick, 0x29 },         { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_LeftAlt, 0x38 },        { Key_LeftShift, 0x2A },        { Key_Invalid, 0xFF },
    { Key_LeftControl, 0x1D },    { Key_Q, 0x10 },              { Key_1, 2 },                   { Key_Invalid, 0xFF },
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
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Backslash, 0x2B },      { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },        { Key_Invalid, 0xFF },
    // Keypad numbers from here
    { Key_Invalid, 0xFF },        { Key_1, 2 },                 { Key_Invalid, 0xFF },          { Key_4, 5 },
    { Key_7, 8 },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },          { Key_Invalid, 0xFF },
    { Key_0, 0x0B },              { Key_Period, 0x34 },         { Key_2, 3 },                   { Key_5, 6 },
    { Key_6, 7 },                 { Key_8, 9 },                 { Key_Escape, 1 },              { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0x4E },           { Key_3, 4 },                   { Key_Minus, 0x0C },
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
    { Key_Invalid, 0xFF },        { Key_LeftAlt, 0x38 },        { Key_LeftShift, 0x2A },      { Key_Invalid, 0xFF },
    { Key_LeftControl, 0x1D },    { Key_Q, 0x10 },              { Key_1, 2 },                 { Key_Invalid, 0xFF },
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
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Backslash, 0x2B },      { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },      { Key_Invalid, 0xFF },
    // Keypad numbers from here, and disabled or converted to arrows
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Left, 0x4B },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },         { Key_Down, 0xFF },           { Key_Invalid, 0xFF },
    { Key_Right, 0xFF },          { Key_Up, 0x48 },             { Key_Escape, 1 },            { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0x4E },           { Key_Invalid, 0xFF },        { Key_Minus, 0x0C },
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
    { Key_Invalid, 0xFF },        { Key_LeftAlt, 0x38 },        { Key_LeftShift, 0x2A },          { Key_Invalid, 0xFF },
    { Key_LeftControl, 0x1D },    { Key_Q, 0x10 },              { Key_Escape, 2 },                { Key_Invalid, 0xFF },
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
    { Key_LeftBrace, 0x1A },      { Key_Plus, 0x4E },           { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_CapsLock, 0x3A },       { Key_RightShift, 0x36 },     { Key_Return, 0x1C },             { Key_RightBrace, 0x1B },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Pipe, 0x2B },           { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },          { Key_Invalid, 0xFF },
    // Keypad numbers from here
    { Key_Invalid, 0xFF },        { Key_1, 2 },                 { Key_Invalid, 0xFF },            { Key_4, 5 },
    { Key_7, 8 },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },
    { Key_0, 0x0B },              { Key_Period, 0x34 },         { Key_2, 3 },                     { Key_5, 6 },
    { Key_6, 7 },                 { Key_8, 9 },                 { Key_Escape, 1 },                { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0x4E },           { Key_3, 4 },                     { Key_Minus, 0x0C },
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
    { Key_Invalid, 0xFF },        { Key_LeftAlt, 0x38 },        { Key_LeftShift, 0x2A },            { Key_Invalid, 0xFF },
    { Key_LeftControl, 0x1D },    { Key_Q, 0x10 },              { Key_Escape, 2 },                  { Key_Invalid, 0xFF },
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
    { Key_LeftBrace, 0x1A },      { Key_Plus, 0x4E },           { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_CapsLock, 0x3A },       { Key_RightShift, 0x36 },     { Key_Return, 0x1C },               { Key_RightBrace, 0x1B },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Pipe, 0x2B },           { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Backspace, 0x0E },            { Key_Invalid, 0xFF },
    // Keypad numbers from here, and disabled or converted to arrows
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_Left, 0x4B },
    { Key_7, 8 },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },              { Key_Invalid, 0xFF },
    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },         { Key_Down, 0xFF },                 { Key_Invalid, 0xFF },
    { Key_Right, 0xFF },          { Key_Up, 0x48 },             { Key_Escape, 1 },                  { Key_NumLock, 0x45 },
    { Key_F11, 0xFF },            { Key_Plus, 0x4E },           { Key_Invalid, 0xFF },              { Key_Minus, 0x0C },
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
    { Key_BrowserSearch, 0xFF },      { Key_RightAlt, 0xFF },           { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_RightControl, 0xFF },   { Key_PreviousTrack, 0xFF },      { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_BrowserFavorites, 0xFF },  { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_LeftGUI, 0xFF },
    { Key_BrowserRefresh, 0xFF },     { Key_VolumeDown, 0xFF },         { Key_Invalid, 0xFF },        { Key_Mute, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_RightGUI, 0xFF },
    { Key_BrowserStop, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Calculator, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Apps, 0xFF },
    { Key_BrowserForward, 0xFF },     { Key_Invalid, 0xFF },            { Key_VolumeUp, 0xFF },       { Key_Invalid, 0xFF },
    { Key_PlayPause, 0xFF },      { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Power, 0xFF },
    { Key_BrowserBack, 0xFF },        { Key_Invalid, 0xFF },            { Key_BrowserHome, 0xFF },        { Key_Stop, 0xFF },
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
    { Key_Invalid, 0xFF },        { Key_End, 0xFF },                { Key_Invalid, 0xFF },        { Key_Left, 0xFF },
    { Key_Home, 0x47 },           { Key_Invalid, 0xFF },            { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Insert, 0xFF },         { Key_Delete, 0xFF },             { Key_Down, 0xFF },           { Key_Invalid, 0xFF },
    { Key_Right, 0xFF },          { Key_Up, 0xFF },                 { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },            { Key_PageDown, 0xFF },       { Key_Invalid, 0xFF },
    { Key_Invalid, 0xFF },        { Key_PageUp, 0x49 },             { Key_Invalid, 0xFF },        { Key_Invalid, 0xFF },
};
// clang-format on

RawKeyEvent PS2KeyboardDevice::generate_raw_key_event_input_from_set1(ScanCodeEvent event)
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
            m_keyboard_device->update_modifier(Mod_AltGr, key_event.is_press());
        else
            m_keyboard_device->update_modifier(Mod_Alt, key_event.is_press());
        break;
    case 0x1d:
        m_keyboard_device->update_modifier(Mod_Ctrl, key_event.is_press());
        break;
    case 0x5b:
        m_left_super_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x5c:
        m_right_super_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case 0x2a:
        m_left_shift_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case 0x36:
        m_right_shift_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case 0x1c:
    case 0x35:
        if (has_e0_prefix)
            m_keyboard_device->update_modifier(Mod_Keypad, key_event.is_press());
        break;
    case 0x37:
    case 0x47:
    case 0x48:
    case 0x49:
    case 0x4a:
    case 0x4b:
    case 0x4c:
    case 0x4d:
    case 0x4e:
    case 0x4f:
    case 0x50:
    case 0x51:
    case 0x52:
    case 0x53:
        if (!has_e0_prefix)
            m_keyboard_device->update_modifier(Mod_Keypad, key_event.is_press());
        break;
    }

    key_event.code_entry = (m_keyboard_device->modifiers() & Mod_Shift) ? shifted_scan_code_set1_key_map[ch] : unshifted_scan_code_set1_key_map[ch];
    key_event.scancode = has_e0_prefix ? 0xe000 + ch : ch;
    return key_event;
}

Optional<RawKeyEvent> PS2KeyboardDevice::generate_raw_key_event_input_from_set2(ScanCodeEvent event)
{
    VERIFY(event.sent_scan_code_set == ScanCodeSet::Set2);

    auto get_key_from_standard_key_map = [this](u8 byte) -> KeyCodeEntry {
        if (!(m_keyboard_device->modifiers() & Mod_Shift))
            return (m_keyboard_device->num_lock_on()) ? unshifted_simple_scan_code_set2_key_map_with_key_num_pad[byte] : unshifted_simple_scan_code_set2_key_map_with_disabled_key_num_pad[byte];
        return (m_keyboard_device->num_lock_on()) ? shifted_simple_scan_code_set2_key_map_with_key_num_pad[byte] : shifted_simple_scan_code_set2_key_map_with_disabled_key_num_pad[byte];
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
        if (first_byte_prefix != 0xe0
            || second_byte_prefix != 0xf0) {
            return Optional<RawKeyEvent> {};
        }

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
        if (first_byte_prefix != 0xe0
            || second_byte_prefix != 0x12
            || third_byte_prefix != 0xe0
            || fourth_byte_prefix != 0x7c) {
            return Optional<RawKeyEvent> {};
        }

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
        if (first_byte_prefix != 0xe0
            || second_byte_prefix != 0xf0
            || third_byte_prefix != 0x7c
            || fourth_byte_prefix != 0xe0
            || fifth_byte_prefix != 0xf0
            || sixth_byte_prefix != 0x12) {
            return Optional<RawKeyEvent> {};
        }

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
        if (first_byte_prefix != 0xe1
            || second_byte_prefix != 0x14
            || third_byte_prefix != 0x77
            || fourth_byte_prefix != 0xe1
            || fifth_byte_prefix != 0xf0
            || sixth_byte_prefix != 0x14
            || seventh_byte_prefix != 0xf0
            || eight_byte_prefix != 0x77) {
            return Optional<RawKeyEvent> {};
        }

        key_event.code_entry = KeyCodeEntry { Key_PauseBreak, 0xFF };
        key_event.scancode = 0xe11477e1f014f077;
    }

    switch (key_event.code_entry.key_code) {
    case Key_RightAlt:
        m_keyboard_device->update_modifier(Mod_AltGr, key_event.is_press());
        break;
    case Key_LeftAlt:
        m_keyboard_device->update_modifier(Mod_Alt, key_event.is_press());
        break;
    case Key_LeftControl:
        m_keyboard_device->update_modifier(Mod_Ctrl, key_event.is_press());
        break;
    case Key_LeftSuper:
        m_left_super_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Super, m_left_super_pressed || m_right_super_pressed);
        break;
    case Key_LeftShift:
        m_left_shift_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    case Key_RightShift:
        m_right_shift_pressed = key_event.is_press();
        m_keyboard_device->update_modifier(Mod_Shift, m_left_shift_pressed || m_right_shift_pressed);
        break;
    default:
        break;
    }

    return key_event;
}

void PS2KeyboardDevice::handle_scan_code_input_event(ScanCodeEvent event)
{
    RawKeyEvent raw_event {};
    if (event.sent_scan_code_set == ScanCodeSet::Set1) {
        raw_event = generate_raw_key_event_input_from_set1(event);
    } else if (event.sent_scan_code_set == ScanCodeSet::Set2) {
        auto possible_raw_event = generate_raw_key_event_input_from_set2(event);
        if (!possible_raw_event.has_value()) {
            dmesgln("PS2KeyboardDevice BUG: Invalid scan code (set 2) event, length: {}, bytes: {}", event.bytes_count, event.scan_code_bytes.span().trim(event.bytes_count));
            return;
        }
        raw_event = possible_raw_event.release_value();
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

    // NOTE: This piece of code is needed for the ScanCodeSet::Set1 to ensure some keys could
    // function properly.
    if (event.sent_scan_code_set == ScanCodeSet::Set1) {
        if ((queued_event.scancode & 0xe000) && queued_event.key == Key_Slash) {
            // FIXME: Find a way to propagate this when the keyboard is "shifted"!
            // If Key_Slash (scancode = 0x35) mapped to other form "/", we fix num pad key of "/" with this case.
            queued_event.code_point = '/';
        } else if ((queued_event.scancode & 0xe000) && queued_event.key != Key_Return) {
            // Except for `keypad-/` and 'keypad-return', all e0 scan codes are not actually characters. i.e., `keypad-0` and
            // `Insert` have the same scancode except for the prefix, but insert should not have a code_point.
            queued_event.code_point = 0;
        }
    }

    // NOTE: This piece of code is needed for the ScanCodeSet::Set1 when NumLock is enabled
    // because we don't have special mappings when NumLock is enabled for this scan code set.
    // Scan code set 2 handling code in handle_scan_code_input_event_set2() already handles this fine.
    if (event.sent_scan_code_set == ScanCodeSet::Set1 && m_keyboard_device->num_lock_on()) {
        if (queued_event.scancode >= 0x47 && queued_event.scancode <= 0x53) {
            u8 index = queued_event.scancode - 0x47;
            constexpr KeyCodeEntry numpad_key_map[13] = {
                { Key_7, 8 },
                { Key_8, 9 },
                { Key_9, 10 },
                { Key_Invalid, 0xFF },
                { Key_4, 5 },
                { Key_5, 6 },
                { Key_6, 7 },
                { Key_Invalid, 0xFF },
                { Key_1, 2 },
                { Key_2, 3 },
                { Key_3, 4 },
                { Key_0, 0x0B },
                { Key_Period, 0x34 },
            };

            if (numpad_key_map[index].key_code != Key_Invalid) {
                queued_event.key = numpad_key_map[index].key_code;
                queued_event.map_entry_index = numpad_key_map[index].map_entry_index;
            }
        }
    }

    m_keyboard_device->handle_input_event(queued_event);
}

void PS2KeyboardDevice::handle_byte_read_for_scan_code_set1(u8 byte)
{
    u8 ch = byte & 0x7f;
    bool pressed = !(byte & 0x80);
    dbgln_if(KEYBOARD_DEBUG, "Keyboard::handle_byte_read_for_scan_code_set1: {:#02x} {}", ch, (pressed ? "down" : "up"));
    if (byte == 0xe0) {
        m_has_e0_prefix = true;
        return;
    }

    ScanCodeEvent event {};
    event.sent_scan_code_set = ScanCodeSet::Set1;
    if (m_has_e0_prefix) {
        event.scan_code_bytes[0] = 0xe0;
        event.scan_code_bytes[1] = byte;
        event.bytes_count = 2;
    } else {
        event.scan_code_bytes[0] = byte;
        event.bytes_count = 1;
    }
    m_has_e0_prefix = false;
    handle_scan_code_input_event(event);
}

void PS2KeyboardDevice::handle_byte_read_for_scan_code_set2(u8 byte)
{
    dbgln_if(KEYBOARD_DEBUG, "Keyboard::handle_byte_read_for_scan_code_set2: {:#02x}", byte);

    ScanCodeEvent event {};
    event.sent_scan_code_set = ScanCodeSet::Set2;
    if (m_received_bytes_count == 0) {
        if (byte == 0xe0 || byte == 0xf0 || byte == 0xe1) {
            m_received_bytes[0] = byte;
            m_received_bytes_count++;
            return;
        }
        event.scan_code_bytes[0] = byte;
        event.bytes_count = 1;
        m_received_bytes_count = 0;
        handle_scan_code_input_event(event);
        return;
    } else if (m_received_bytes_count == 1) {
        if (byte == 0xf0) {
            VERIFY(m_received_bytes[0] == 0xe0);
            m_received_bytes[1] = byte;
            m_received_bytes_count++;
            return;
        }
        if (m_received_bytes[0] == 0xe0 && byte == 0x12) {
            m_received_bytes[1] = byte;
            m_received_bytes_count++;
            return;
        }

        if (m_received_bytes[0] == 0xe1 && byte == 0x14) {
            m_received_bytes[1] = byte;
            m_received_bytes_count++;
            return;
        }

        event.scan_code_bytes[0] = m_received_bytes[0];
        event.scan_code_bytes[1] = byte;
        event.bytes_count = 2;
        m_received_bytes_count = 0;
        handle_scan_code_input_event(event);
        return;
    } else if (m_received_bytes_count == 2) {
        if (m_received_bytes[0] == 0xe0 && m_received_bytes[1] == 0x12 && byte == 0xe0) {
            m_received_bytes[2] = byte;
            m_received_bytes_count++;
            return;
        }

        if (m_received_bytes[0] == 0xe0 && m_received_bytes[1] == 0xf0 && byte == 0x7c) {
            m_received_bytes[2] = byte;
            m_received_bytes_count++;
            return;
        }

        if (m_received_bytes[0] == 0xe1) {
            VERIFY(m_received_bytes[1] == 0x14);
            m_received_bytes[2] = byte;
            m_received_bytes_count++;
            return;
        }

        event.scan_code_bytes[0] = m_received_bytes[0];
        event.scan_code_bytes[1] = m_received_bytes[1];
        event.scan_code_bytes[2] = byte;
        event.bytes_count = 3;
        m_received_bytes_count = 0;
        handle_scan_code_input_event(event);
        return;
    } else if (m_received_bytes_count == 3) {
        if (m_received_bytes[0] == 0xe0
            && m_received_bytes[1] == 0x12
            && m_received_bytes[2] == 0xe0
            && byte == 0x7c) {
            ScanCodeEvent event {};
            event.sent_scan_code_set = ScanCodeSet::Set2;
            event.scan_code_bytes[0] = m_received_bytes[0];
            event.scan_code_bytes[1] = m_received_bytes[1];
            event.scan_code_bytes[2] = m_received_bytes[2];
            event.scan_code_bytes[3] = byte;
            event.bytes_count = 4;
            m_received_bytes_count = 0;
            handle_scan_code_input_event(event);
            return;
        }

        m_received_bytes[3] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 4) {
        m_received_bytes[4] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 5) {
        if (m_received_bytes[0] == 0xe0
            && m_received_bytes[1] == 0xf0
            && m_received_bytes[2] == 0x7c
            && m_received_bytes[3] == 0xe0
            && m_received_bytes[4] == 0xf0
            && byte == 0x12) {

            event.scan_code_bytes[0] = m_received_bytes[0];
            event.scan_code_bytes[1] = m_received_bytes[1];
            event.scan_code_bytes[2] = m_received_bytes[2];
            event.scan_code_bytes[3] = m_received_bytes[3];
            event.scan_code_bytes[4] = m_received_bytes[4];
            event.scan_code_bytes[5] = byte;
            event.bytes_count = 6;
            m_received_bytes_count = 0;
            handle_scan_code_input_event(event);
            return;
        }
        m_received_bytes[5] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 6) {
        m_received_bytes[6] = byte;
        m_received_bytes_count++;
        return;
    } else if (m_received_bytes_count == 7) {
        event.scan_code_bytes[0] = m_received_bytes[0];
        event.scan_code_bytes[1] = m_received_bytes[1];
        event.scan_code_bytes[2] = m_received_bytes[2];
        event.scan_code_bytes[3] = m_received_bytes[3];
        event.scan_code_bytes[4] = m_received_bytes[4];
        event.scan_code_bytes[5] = m_received_bytes[5];
        event.scan_code_bytes[6] = m_received_bytes[6];
        event.scan_code_bytes[7] = byte;
        event.bytes_count = 8;
        m_received_bytes_count = 0;
        handle_scan_code_input_event(event);
        return;
    } else {
        VERIFY_NOT_REACHED();
    }
}

void PS2KeyboardDevice::handle_byte_read_from_serial_input(u8 byte)
{
    switch (m_scan_code_set) {
    case ScanCodeSet::Set1:
        handle_byte_read_for_scan_code_set1(byte);
        break;
    case ScanCodeSet::Set2:
        handle_byte_read_for_scan_code_set2(byte);
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

UNMAP_AFTER_INIT ErrorOr<NonnullOwnPtr<PS2KeyboardDevice>> PS2KeyboardDevice::try_to_initialize(SerialIOController const& serial_io_controller, SerialIOController::PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const& keyboard_device)
{
    auto device = TRY(adopt_nonnull_own_or_enomem(new (nothrow) PS2KeyboardDevice(serial_io_controller, port_index, scan_code_set, keyboard_device)));
    TRY(device->initialize());
    return device;
}

UNMAP_AFTER_INIT ErrorOr<void> PS2KeyboardDevice::initialize()
{
    ErrorOr<void> err = attached_controller().reset_device(attached_port_index());

    if (err.is_error()) {
        TRY(attached_controller().send_command(attached_port_index(), SerialIOController::DeviceCommand::GetDeviceID));
        ErrorOr<u8> res = attached_controller().read_from_device(attached_port_index());
        if (res.is_error()) {
            return err;
        }
        switch (res.value()) {
            // Regular and NCD Sun keyboards.
        case 0xab:
        case 0xac:
            // Trust keyboard, raw and translated
        case 0x2b:
        case 0x5d:
            // NMB SGI keyboard, raw and translated
        case 0x60:
        case 0x47:
            return {};
        }
    }

    return err;
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::PS2KeyboardDevice(SerialIOController const& serial_io_controller, SerialIOController::PortIndex port_index, ScanCodeSet scan_code_set, KeyboardDevice const& keyboard_device)
    : SerialIODevice(serial_io_controller, port_index)
    , m_keyboard_device(keyboard_device)
    , m_scan_code_set(scan_code_set)
{
}

// FIXME: UNMAP_AFTER_INIT might not be correct, because in practice PS/2 devices
// are hot pluggable.
UNMAP_AFTER_INIT PS2KeyboardDevice::~PS2KeyboardDevice() = default;

}
