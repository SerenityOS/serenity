/*
 * Copyright (c) 2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

enum class Colorspace;
enum class Channels;

class QOIWriter {
public:
    static ErrorOr<ByteBuffer> encode(Gfx::Bitmap const&);

private:
    QOIWriter() = default;

    Vector<u8> m_data;
    ErrorOr<void> add_header(u32 width, u32 height, Channels, Colorspace);
    ErrorOr<void> add_rgb_chunk(u8, u8, u8);
    ErrorOr<void> add_rgba_chunk(u8, u8, u8, u8);
    ErrorOr<void> add_index_chunk(u32 index);
    ErrorOr<void> add_diff_chunk(i8 red_difference, i8 green_difference, i8 blue_difference);
    ErrorOr<void> add_luma_chunk(i8 relative_red_difference, i8 green_difference, i8 relative_blue_difference);
    ErrorOr<void> add_run_chunk(u32 run_length);
    ErrorOr<void> add_end_marker();

    Array<Color, 64> running_array;
    static u32 pixel_hash_function(Color pixel);
    void insert_into_running_array(Color pixel);
};

}
