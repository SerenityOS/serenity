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

// EFI_GRAPHICS_OUTPUT_BLT_PIXEL: https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol-blt
struct GraphicsOutputBltPixel {
    u8 blue;
    u8 green;
    u8 red;
    u8 reserved;
};
static_assert(AssertSize<GraphicsOutputBltPixel, 4>());

// EFI_GRAPHICS_OUTPUT_BLT_OPERATION: https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol-blt
enum class GraphicsOutputBltOperation {
    VideoFill,
    VideoToBltBuffer,
    BufferToVideo,
    VideoToVideo,
    Max,
};

// EFI_GRAPHICS_PIXEL_FORMAT: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol-blt
enum class GraphicsPixelFormat {
    RedGreenBlueReserved8BitPerColor,
    BlueGreenRedReserved8BitPerColor,
    BitMask,
    BltOnly,
    Max,
};

// EFI_PIXEL_BITMASK: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol-blt
struct PixelBitmask {
    u32 red_mask;
    u32 green_mask;
    u32 blue_mask;
    u32 reserved_mask;
};
static_assert(AssertSize<PixelBitmask, 16>());

// EFI_GRAPHICS_OUTPUT_MODE_INFORMATION: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol-blt
struct GraphicsOutputModeInformation {
    u32 version;
    u32 horizontal_resolution;
    u32 vertical_resolution;
    GraphicsPixelFormat pixel_format;
    PixelBitmask pixel_information;
    u32 pixels_per_scan_line;
};
static_assert(AssertSize<GraphicsOutputModeInformation, 36>());

// EFI_GRAPHICS_OUTPUT_PROTOCOL_MODE: See "Related Definitions" at https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol
struct GraphicsOutputProtocolMode {
    u32 max_mode;
    u32 mode;
    GraphicsOutputModeInformation* info;
    FlatPtr size_of_info;
    PhysicalAddress frame_buffer_base;
    FlatPtr frame_buffer_size;
};
static_assert(AssertSize<GraphicsOutputProtocolMode, 40>());

// EFI_GRAPHICS_OUTPUT_PROTOCOL: https://uefi.org/specs/UEFI/2.10/12_Protocols_Console_Support.html#efi-graphics-output-protocol
struct GraphicsOutputProtocol {
    static constexpr GUID guid = { 0x9042a9de, 0x23dc, 0x4a38, { 0x96, 0xfb, 0x7a, 0xde, 0xd0, 0x80, 0x51, 0x6a } };

    using QueryModeFn = EFIAPI Status (*)(GraphicsOutputProtocol*, u32 mode_number, FlatPtr* size_of_info, GraphicsOutputModeInformation** Info);
    using SetModeFn = EFIAPI Status (*)(GraphicsOutputProtocol*, u32 mode_number);
    using BltFn = EFIAPI Status (*)(GraphicsOutputProtocol*, GraphicsOutputBltPixel* blt_buffer, GraphicsOutputBltOperation blt_operation, FlatPtr source_x, FlatPtr source_y, FlatPtr destination_x, FlatPtr destination_y, FlatPtr width, FlatPtr height, FlatPtr delta);

    QueryModeFn query_mode;
    SetModeFn set_mode;
    BltFn blt;
    GraphicsOutputProtocolMode* mode;
};
static_assert(AssertSize<GraphicsOutputProtocol, 32>());

}
