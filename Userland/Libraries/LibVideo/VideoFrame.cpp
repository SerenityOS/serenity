/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <LibVideo/Color/ColorConverter.h>

#include "VideoFrame.h"

namespace Video {

ErrorOr<NonnullOwnPtr<SubsampledYUVFrame>> SubsampledYUVFrame::try_create(
    Gfx::IntSize size,
    u8 bit_depth, CodingIndependentCodePoints cicp,
    bool subsampling_horizontal, bool subsampling_vertical,
    Span<u16> plane_y, Span<u16> plane_u, Span<u16> plane_v)
{
    auto plane_y_array = TRY(FixedArray<u16>::try_create(plane_y));
    auto plane_u_array = TRY(FixedArray<u16>::try_create(plane_u));
    auto plane_v_array = TRY(FixedArray<u16>::try_create(plane_v));
    return adopt_nonnull_own_or_enomem(new (nothrow) SubsampledYUVFrame(size, bit_depth, cicp, subsampling_horizontal, subsampling_vertical, plane_y_array, plane_u_array, plane_v_array));
}

DecoderErrorOr<void> SubsampledYUVFrame::output_to_bitmap(Gfx::Bitmap& bitmap)
{
    size_t width = this->width();
    size_t height = this->height();
    auto u_sample_row = DECODER_TRY_ALLOC(FixedArray<u16>::try_create(width));
    auto v_sample_row = DECODER_TRY_ALLOC(FixedArray<u16>::try_create(width));
    size_t uv_width = width >> m_subsampling_horizontal;

    auto converter = TRY(ColorConverter::create(bit_depth(), cicp()));

    for (size_t row = 0; row < height; row++) {
        auto uv_row = row >> m_subsampling_vertical;

        // Linearly interpolate the UV samples vertically first.
        // This will write all UV samples that are located on the Y sample as well,
        // so we only need to interpolate horizontally between UV samples in the next
        // step.
        if ((row & m_subsampling_vertical) == 0 || row == height - 1) {
            for (size_t uv_column = 0; uv_column < uv_width; uv_column++) {
                size_t column = uv_column << m_subsampling_horizontal;
                size_t index = uv_row * uv_width + uv_column;
                u_sample_row[column] = m_plane_u[index];
                v_sample_row[column] = m_plane_v[index];
            }
        } else {
            for (size_t uv_column = 0; uv_column < uv_width; uv_column++) {
                size_t column = uv_column << m_subsampling_horizontal;
                size_t index = (uv_row + 1) * uv_width + uv_column;
                u_sample_row[column] = (u_sample_row[column] + m_plane_u[index]) >> 1;
                v_sample_row[column] = (v_sample_row[column] + m_plane_v[index]) >> 1;
            }
        }
        // Fill in the last pixel of the row which may not be applied by the above
        // loops if the last pixel in each row is on an uneven index.
        if ((width & 1) == 0) {
            u_sample_row[width - 1] = u_sample_row[width - 2];
            v_sample_row[width - 1] = v_sample_row[width - 2];
        }

        // Interpolate the samples horizontally.
        if (m_subsampling_horizontal) {
            for (size_t column = 1; column < width - 1; column += 2) {
                u_sample_row[column] = (u_sample_row[column - 1] + u_sample_row[column + 1]) >> 1;
                v_sample_row[column] = (v_sample_row[column - 1] + v_sample_row[column + 1]) >> 1;
            }
        }

        for (size_t column = 0; column < width; column++) {
            auto y_sample = m_plane_y[row * width + column];
            auto u_sample = u_sample_row[column];
            auto v_sample = v_sample_row[column];

            bitmap.set_pixel(Gfx::IntPoint(column, row), converter.convert_yuv_to_full_range_rgb(y_sample, u_sample, v_sample));

            /*auto r_float = clamp(y_sample + (v_sample - 128) * 219.0f / 224.0f * 1.5748f, 0, 255);
            auto g_float = clamp(y_sample + (u_sample - 128) * 219.0f / 224.0f * -0.0722f * 1.8556f / 0.7152f + (v_sample - 128) * 219.0f / 224.0f * -0.2126f * 1.5748f / 0.7152f, 0, 255);
            auto b_float = clamp(y_sample + (u_sample - 128) * 219.0f / 224.0f * 1.8556f, 0, 255);
            auto r = static_cast<u8>(r_float);
            auto g = static_cast<u8>(g_float);
            auto b = static_cast<u8>(b_float);
            bitmap.set_pixel(Gfx::IntPoint(column, row), Color(r, g, b));*/
        }
    }

    return {};
}

}
