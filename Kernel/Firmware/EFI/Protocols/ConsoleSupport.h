/*
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Firmware/EFI/EFI.h>

// https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html

namespace Kernel::EFI {

// EFI_INPUT_KEY: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-simple-text-input-protocol-readkeystroke
struct InputKey {
    u16 scan_code;
    char16_t unicode_char;
};
static_assert(AssertSize<InputKey, 4>());

// EFI_SIMPLE_TEXT_INPUT_PROTOCOL: https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#simple-text-input-protocol
struct SimpleTextInputProtocol {
    static constexpr GUID guid = { 0x387477c1, 0x69c7, 0x11d2, { 0x8e, 0x39, 0x00, 0xa0, 0xc9, 0x69, 0x72, 0x3b } };

    using InputResetFn = EFIAPI Status (*)(SimpleTextInputProtocol*, Boolean extended_verification);
    using InputReadKeyFn = EFIAPI Status (*)(SimpleTextInputProtocol*, InputKey* key);

    InputResetFn reset;
    InputReadKeyFn read_key_stroke;
    Event wait_for_key;
};
static_assert(AssertSize<SimpleTextInputProtocol, 24>());

// https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#simple-text-output-protocol

// See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-simple-text-output-protocol-setattribute
struct [[gnu::packed]] TextAttribute {
    enum class ForegroundColor : i32 {
        Black = 0x00,
        Blue = 0x01,
        Green = 0x02,
        Cyan = 0x03,
        Red = 0x04,
        Magenta = 0x05,
        Brown = 0x06,
        LightGray = 0x07,
        DarkGray = 0x08,
        LightBlue = 0x09,
        LightGreen = 0x0a,
        LightCyan = 0x0b,
        LightRed = 0x0c,
        LightMagenta = 0x0d,
        Yellow = 0x0e,
        White = 0x0f,
    };

    enum class BackgroundColor : i32 {
        Black = 0x00,
        Blue = 0x01,
        Green = 0x02,
        Cyan = 0x03,
        Red = 0x04,
        Magenta = 0x05,
        Brown = 0x06,
        LightGray = 0x07,
    };

    ForegroundColor foreground_color : 4;
    BackgroundColor background_color : 3;
    i32 : 32 - (4 + 3);
};
static_assert(AssertSize<TextAttribute, 4>());

// SIMPLE_TEXT_OUTPUT_MODE: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#simple-text-output-protocol
struct SimpleTextOutputMode {
    i32 max_mode;
    i32 mode;
    TextAttribute attribute;
    i32 cursor_column;
    i32 cursor_row;
    Boolean cursor_visible;
};
static_assert(AssertSize<SimpleTextOutputMode, 24>());

// EFI_SIMPLE_TEXT_OUTPUT_PROTOCOL: https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#simple-text-output-protocol
struct SimpleTextOutputProtocol {
    static constexpr GUID guid = { 0x387477c2, 0x69c7, 0x11d2, { 0x8e, 0x39, 0x00, 0xa0, 0xc9, 0x69, 0x72, 0x3b } };

    using TextResetFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, Boolean extended_verification);
    using TextStringFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, char16_t* string);
    using TextTestStringFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, char16_t* string);
    using TextQueryModeFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, FlatPtr mode_number, FlatPtr* columns, FlatPtr* rows);
    using TextSetModeFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, FlatPtr mode_number);
    using TextSetAttributeFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, TextAttribute attribute);
    using TextClearScreenFn = EFIAPI Status (*)(SimpleTextOutputProtocol*);
    using TextSetCursorPositionFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, FlatPtr column, FlatPtr row);
    using TextEnableCursorFn = EFIAPI Status (*)(SimpleTextOutputProtocol*, Boolean visible);

    TextResetFn reset;
    TextStringFn output_string;
    TextTestStringFn test_string;
    TextQueryModeFn query_mode;
    TextSetModeFn set_mode;
    TextSetAttributeFn set_attribute;
    TextClearScreenFn clear_screen;
    TextSetCursorPositionFn set_cursor_position;
    TextEnableCursorFn enable_cursor;
    SimpleTextOutputMode* mode;
};
static_assert(AssertSize<SimpleTextOutputProtocol, 80>());
static_assert(__builtin_offsetof(SimpleTextOutputProtocol, output_string) == 8);

}
