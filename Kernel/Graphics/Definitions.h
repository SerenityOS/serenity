/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::Graphics {

struct Timings {
    size_t blanking_start() const
    {
        return active;
    }
    size_t blanking_end() const
    {
        return total;
    }

    size_t active;
    size_t sync_start;
    size_t sync_end;
    size_t total;
};

struct Modesetting {
    size_t pixel_clock_in_khz;
    Timings horizontal;
    Timings vertical;
};

struct [[gnu::packed]] StandardTimings {
    u8 resolution;
    u8 frequency;
};

struct [[gnu::packed]] DetailTimings {
    u16 pixel_clock;
    u8 horizontal_active;
    u8 horizontal_blank;
    u8 horizontal_active_blank_msb;
    u8 vertical_active;
    u8 vertical_blank;
    u8 vertical_active_blank_msb;
    u8 horizontal_sync_offset;
    u8 horizontal_sync_pulse;
    u8 vertical_sync;
    u8 sync_msb;
    u8 dimension_width;
    u8 dimension_height;
    u8 dimension_msb;
    u8 horizontal_border;
    u8 vertical_border;
    u8 features;
};

struct [[gnu::packed]] VideoInfoBlock {
    u64 padding;
    u16 manufacture_id;
    u16 product_id;
    u32 serial_number;
    u8 manufacture_week;
    u8 manufacture_year;
    u8 edid_version;
    u8 edid_revision;
    u8 video_input_type;
    u8 max_horizontal_size;
    u8 max_vertical_size;
    u8 gama_factor;
    u8 dpms_flags;
    u8 chroma_info[10];
    u8 established_timing[2];
    u8 manufacture_reserved_timings;
    StandardTimings timings[8];
    DetailTimings details[4];
    u8 unused;
    u8 checksum;
};

static_assert(AssertSize<VideoInfoBlock, 128>());

}
