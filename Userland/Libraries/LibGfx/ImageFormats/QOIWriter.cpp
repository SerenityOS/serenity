/*
 * Copyright (c) 2022, Olivier De Cannière <olivier.decanniere96@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "QOIWriter.h"
#include <AK/Endian.h>

namespace Gfx {

static constexpr Array<u8, 4> qoi_magic_bytes = { 'q', 'o', 'i', 'f' };
static constexpr Array<u8, 8> qoi_end_marker = { 0, 0, 0, 0, 0, 0, 0, 1 };

enum class Colorspace {
    sRGB,
    Linear,
};

enum class Channels {
    RGB,
    RGBA,
};

ErrorOr<ByteBuffer> QOIWriter::encode(Bitmap const& bitmap)
{
    QOIWriter writer;
    TRY(writer.add_header(bitmap.width(), bitmap.height(), Channels::RGBA, Colorspace::sRGB));

    Color previous_pixel = { 0, 0, 0, 255 };

    bool creating_run = false;
    int run_length = 0;

    for (auto y = 0; y < bitmap.height(); y++) {
        for (auto x = 0; x < bitmap.width(); x++) {
            auto pixel = bitmap.get_pixel(x, y);

            // Check for at most 62 consecutive identical pixels.
            if (pixel == previous_pixel) {
                if (!creating_run) {
                    creating_run = true;
                    run_length = 0;
                    writer.insert_into_running_array(pixel);
                }

                run_length++;

                // If the run reaches a maximum length of 62 or if this is the last pixel then create the chunk.
                if (run_length == 62 || (y == bitmap.height() - 1 && x == bitmap.width() - 1)) {
                    TRY(writer.add_run_chunk(run_length));
                    creating_run = false;
                }

                continue;
            }

            // Run ended with the previous pixel. Create a chunk for it and continue processing this pixel.
            if (creating_run) {
                TRY(writer.add_run_chunk(run_length));
                creating_run = false;
            }

            // Check if the pixel matches a pixel in the running array.
            auto index = pixel_hash_function(pixel);
            auto& array_pixel = writer.running_array[index];
            if (array_pixel == pixel) {
                TRY(writer.add_index_chunk(index));
                previous_pixel = pixel;
                continue;
            }

            writer.running_array[index] = pixel;

            // Check if pixel can be expressed as a difference of the previous pixel.
            if (pixel.alpha() == previous_pixel.alpha()) {
                int red_difference = pixel.red() - previous_pixel.red();
                int green_difference = pixel.green() - previous_pixel.green();
                int blue_difference = pixel.blue() - previous_pixel.blue();
                int relative_red_difference = red_difference - green_difference;
                int relative_blue_difference = blue_difference - green_difference;

                if (red_difference > -3 && red_difference < 2
                    && green_difference > -3 && green_difference < 2
                    && blue_difference > -3 && blue_difference < 2) {
                    TRY(writer.add_diff_chunk(red_difference, green_difference, blue_difference));
                    previous_pixel = pixel;
                    continue;
                }
                if (relative_red_difference > -9 && relative_red_difference < 8
                    && green_difference > -33 && green_difference < 32
                    && relative_blue_difference > -9 && relative_blue_difference < 8) {
                    TRY(writer.add_luma_chunk(relative_red_difference, green_difference, relative_blue_difference));
                    previous_pixel = pixel;
                    continue;
                }

                TRY(writer.add_rgb_chunk(pixel.red(), pixel.green(), pixel.blue()));
                previous_pixel = pixel;
                continue;
            }

            previous_pixel = pixel;

            // Write full color values.
            TRY(writer.add_rgba_chunk(pixel.red(), pixel.green(), pixel.blue(), pixel.alpha()));
        }
    }

    TRY(writer.add_end_marker());

    return ByteBuffer::copy(writer.m_data);
}

ErrorOr<void> QOIWriter::add_header(u32 width, u32 height, Channels channels = Channels::RGBA, Colorspace color_space = Colorspace::sRGB)
{
    // FIXME: Handle RGB and all linear channels.
    if (channels == Channels::RGB || color_space == Colorspace::Linear)
        TODO();

    TRY(m_data.try_append(qoi_magic_bytes.data(), sizeof(qoi_magic_bytes)));

    auto big_endian_width = AK::convert_between_host_and_big_endian(width);
    TRY(m_data.try_append(bit_cast<u8*>(&big_endian_width), sizeof(width)));

    auto big_endian_height = AK::convert_between_host_and_big_endian(height);
    TRY(m_data.try_append(bit_cast<u8*>(&big_endian_height), sizeof(height)));

    // Number of channels: 3 = RGB, 4 = RGBA.
    TRY(m_data.try_append(4));

    // Colorspace: 0 = sRGB, 1 = all linear channels.
    TRY(m_data.try_append(color_space == Colorspace::sRGB ? 0 : 1));

    return {};
}

ErrorOr<void> QOIWriter::add_rgb_chunk(u8 r, u8 g, u8 b)
{
    constexpr static u8 rgb_tag = 0b1111'1110;

    TRY(m_data.try_append(rgb_tag));
    TRY(m_data.try_append(r));
    TRY(m_data.try_append(g));
    TRY(m_data.try_append(b));
    return {};
}

ErrorOr<void> QOIWriter::add_rgba_chunk(u8 r, u8 g, u8 b, u8 a)
{
    constexpr static u8 rgba_tag = 0b1111'1111;

    TRY(m_data.try_append(rgba_tag));
    TRY(m_data.try_append(r));
    TRY(m_data.try_append(g));
    TRY(m_data.try_append(b));
    TRY(m_data.try_append(a));
    return {};
}

ErrorOr<void> QOIWriter::add_index_chunk(unsigned int index)
{
    constexpr static u8 index_tag = 0b0000'0000;

    u8 chunk = index_tag | index;
    TRY(m_data.try_append(chunk));
    return {};
}

ErrorOr<void> QOIWriter::add_diff_chunk(i8 red_difference, i8 green_difference, i8 blue_difference)
{
    constexpr static u8 diff_tag = 0b0100'0000;

    u8 bias = 2;
    u8 red = red_difference + bias;
    u8 green = green_difference + bias;
    u8 blue = blue_difference + bias;

    u8 chunk = diff_tag | (red << 4) | (green << 2) | blue;
    TRY(m_data.try_append(chunk));
    return {};
}

ErrorOr<void> QOIWriter::add_luma_chunk(i8 relative_red_difference, i8 green_difference, i8 relative_blue_difference)
{
    constexpr static u8 luma_tag = 0b1000'0000;
    u8 green_bias = 32;
    u8 red_blue_bias = 8;

    u8 chunk1 = luma_tag | (green_difference + green_bias);
    u8 chunk2 = ((relative_red_difference + red_blue_bias) << 4) | (relative_blue_difference + red_blue_bias);
    TRY(m_data.try_append(chunk1));
    TRY(m_data.try_append(chunk2));
    return {};
}

ErrorOr<void> QOIWriter::add_run_chunk(unsigned run_length)
{
    constexpr static u8 run_tag = 0b1100'0000;
    int bias = -1;

    u8 chunk = run_tag | (run_length + bias);
    TRY(m_data.try_append(chunk));
    return {};
}

ErrorOr<void> QOIWriter::add_end_marker()
{
    TRY(m_data.try_append(qoi_end_marker.data(), sizeof(qoi_end_marker)));
    return {};
}

u32 QOIWriter::pixel_hash_function(Color pixel)
{
    return (pixel.red() * 3 + pixel.green() * 5 + pixel.blue() * 7 + pixel.alpha() * 11) % 64;
}

void QOIWriter::insert_into_running_array(Color pixel)
{
    auto index = pixel_hash_function(pixel);
    running_array[index] = pixel;
}

}
