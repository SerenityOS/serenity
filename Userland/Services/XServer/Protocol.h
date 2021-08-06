/*
 * Copyright (c) 2021, Peter Elliott <pelliott@ualberta.ca>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Types.h"

namespace X {

// The X11 protocol is implemented as described in this document:
// https://www.x.org/releases/X11R7.7/doc/xproto/x11protocol.html

constexpr inline size_t align(size_t to, size_t n)
{
    return (to - (n % to)) % to;
}

constexpr inline size_t aligned(size_t to, size_t n)
{
    return n + align(to, n);
}

enum class ByteOrderByte : Byte {
    Little = 'l',
    Big = 'B',
};

struct ConnectionSetup {
    Card16 protocol_major_version;
    Card16 protocol_minor_version;
    String8 authorization_protocol_name;
    String8 authorization_protocol_data;
};

enum class Success : Card8 {
    Failed = 0,
    Success = 1,
    Authenticate = 2,
};

// FIXME: Handle failed connections better than just dying

struct ConnectionSetupSuccess {
    Card16 protocol_major_version;
    Card16 protocol_minor_version;
    String8 vendor;
    Card32 release_number;
    Card32 resource_id_base, resource_id_mask;

    enum class ByteOrder : Card8 {
        LSBFirst = 0,
        MSBFirst = 1,
    };

    ByteOrder image_byte_order;
    Card8 bitmap_scanline_unit;
    Card8 bitmap_scanline_pad;

    enum class BitmapBitOrder : Card8 {
        LeastSignificant = 0,
        MostSignificant = 1,
    };

    BitmapBitOrder bitmap_bit_order;

    struct Format {
        Card8 depth;
        Card8 bits_per_pixel;
        Card8 scanline_pad;
    };

    ListOf<Format> pixmap_formats;

    struct VisualType {
        VisualID visual_id;

        enum class Class : Card8 {
            StaticGray = 0,
            GrayScale = 1,
            StaticColor = 2,
            PseudoColor = 3,
            TrueColor = 4,
            DirectColor = 5,
        };

        Class clas;
        Card32 red_mask, green_mask, blue_mask;
        Card8 bits_per_rgb_value;
        Card16 colormap_entries;
    };

    struct Depth {
        Card8 depth;
        ListOf<VisualType> visuals;
    };

    struct Screen {
        Window root;
        Card16 width_in_pixels, height_in_pixels;
        Card16 width_in_millimeters, height_in_millimeters;
        ListOf<Depth> allowed_depths;
        Card8 root_depth;
        VisualID root_visual;
        ColorMap default_colormap;
        Card32 white_pixel, black_pixel;
        Card16 min_installed_maps, max_installed_maps;

        enum class BackingStores : Card8 {
            Never = 0,
            WhenMapped = 1,
            Always = 2,
        };

        BackingStores backing_stores;
        Bool save_unders;
        SetOf<Event> current_input_masks;
    };

    ListOf<Screen> roots;

    Card32 motion_buffer_size;
    Card16 maximum_request_length;
    KeyCode min_keycode, max_keycode;
};

}
