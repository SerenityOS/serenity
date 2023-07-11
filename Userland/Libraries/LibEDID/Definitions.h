/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace EDID::Definitions {

struct [[gnu::packed]] StandardTimings {
    u8 horizontal_8_pixels;
    u8 ratio_and_refresh_rate;
};

struct [[gnu::packed]] DetailedTiming {
    u16 pixel_clock;
    u8 horizontal_addressable_pixels_low;
    u8 horizontal_blanking_pixels_low;
    u8 horizontal_addressable_and_blanking_pixels_high;
    u8 vertical_addressable_lines_low;
    u8 vertical_blanking_lines_low;
    u8 vertical_addressable_and_blanking_lines_high;
    u8 horizontal_front_porch_pixels_low;
    u8 horizontal_sync_pulse_width_pixels_low;
    u8 vertical_front_porch_and_sync_pulse_width_lines_low;
    u8 horizontal_and_vertical_front_porch_sync_pulse_width_high;
    u8 horizontal_addressable_image_size_mm_low;
    u8 vertical_addressable_image_size_mm_low;
    u8 horizontal_vertical_addressable_image_size_mm_high;
    u8 right_or_left_horizontal_border_pixels;
    u8 top_or_bottom_vertical_border_lines;
    u8 features;
};

enum class DisplayDescriptorTag : u8 {
    ManufacturerSpecified_First = 0x0,
    ManufacturerSpecified_Last = 0xf,
    Dummy = 0x10,
    EstablishedTimings3 = 0xf7,
    CVTTimingCodes = 0xf8,
    DisplayColorManagementData = 0xf9,
    StandardTimingIdentifications = 0xfa,
    ColorPointData = 0xfb,
    DisplayProductName = 0xfc,
    DisplayRangeLimits = 0xfd,
    AlphanumericDataString = 0xfe,
    DisplayProductSerialNumber = 0xff
};

struct [[gnu::packed]] DisplayDescriptor {
    u16 zero;
    u8 reserved1;
    u8 tag;
    u8 reserved2;
    union {
        struct [[gnu::packed]] {
            u8 ascii_name[13];
        } display_product_name;
        struct [[gnu::packed]] {
            u8 ascii_str[13];
        } display_product_serial_number;
        struct [[gnu::packed]] {
            u8 revision;
            u8 dmt_bits[6];
            u8 reserved[6];
        } established_timings3;
        struct [[gnu::packed]] {
            u8 version;
            u8 cvt[4][3];
        } coordinated_video_timings;
    };
};

static_assert(sizeof(DetailedTiming) == sizeof(DisplayDescriptor));

struct [[gnu::packed]] EDID {
    u64 header;
    struct [[gnu::packed]] {
        u16 manufacturer_id;
        u16 product_code;
        u32 serial_number;
        u8 week_of_manufacture;
        u8 year_of_manufacture;
    } vendor;
    struct [[gnu::packed]] {
        u8 version;
        u8 revision;
    } version;
    struct [[gnu::packed]] {
        u8 video_input_definition;
        u8 horizontal_size_or_aspect_ratio;
        u8 vertical_size_or_aspect_ratio;
        u8 display_transfer_characteristics;
        u8 feature_support;
    } basic_display_parameters;
    struct [[gnu::packed]] {
        u8 red_green_low_order_bits;
        u8 blue_white_low_order_bits;
        u8 red_x_high_order_bits;
        u8 red_y_high_order_bits;
        u8 green_x_high_order_bits;
        u8 green_y_high_order_bits;
        u8 blue_x_high_order_bits;
        u8 blue_y_high_order_bits;
        u8 white_x_high_order_bits;
        u8 white_y_high_order_bits;
    } color_characteristics;
    struct [[gnu::packed]] {
        u8 timings_1;
        u8 timings_2;
        u8 manufacturer_reserved;
    } established_timings;
    StandardTimings standard_timings[8];
    union {
        DetailedTiming detailed_timing;
        DisplayDescriptor display_descriptor;
    } detailed_timing_or_display_descriptors[4];
    u8 extension_block_count;
    u8 checksum;
};

enum class ExtensionBlockTag : u8 {
    CEA_861 = 0x2,
    VideoTimingBlock = 0x10,
    DisplayInformation = 0x40,
    LocalizedString = 0x50,
    DigitalPacketVideoLink = 0x60,
    ExtensionBlockMap = 0xf0,
    ManufacturerDefined = 0xff
};

struct [[gnu::packed]] ExtensionBlock {
    u8 tag;
    union {
        struct [[gnu::packed]] {
            u8 block_tags[126];
        } map;
        struct [[gnu::packed]] {
            u8 revision;
            u8 bytes[125];
        } block;
        struct [[gnu::packed]] {
            u8 revision;
            u8 dtd_start_offset;
            u8 flags;
            union {
                u8 bytes[123];
            };
        } cea861extension;
    };
    u8 checksum;
};
static_assert(AssertSize<ExtensionBlock, 128>());

}
