/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Math.h>
#include <AK/StdLibExtras.h>
#include <LibGfx/Matrix4x4.h>
#include <LibVideo/Color/ColorPrimaries.h>
#include <LibVideo/Color/TransferCharacteristics.h>

#include "ColorConverter.h"

namespace Video {

// Tonemapping methods are outlined here:
// https://64.github.io/tonemapping/

template<typename T>
ALWAYS_INLINE constexpr T scalar_to_color_vector(float value)
{
    if constexpr (IsSame<T, Gfx::VectorN<4, float>>) {
        return Gfx::VectorN<4, float>(value, value, value, 1.0f);
    } else if constexpr (IsSame<T, Gfx::VectorN<3, float>>) {
        return Gfx::VectorN<3, float>(value, value, value);
    } else {
        static_assert(IsFloatingPoint<T>);
        return static_cast<T>(value);
    }
}

template<typename T>
ALWAYS_INLINE constexpr T hable_tonemapping_partial(T value)
{
    constexpr auto a = scalar_to_color_vector<T>(0.15f);
    constexpr auto b = scalar_to_color_vector<T>(0.5f);
    constexpr auto c = scalar_to_color_vector<T>(0.1f);
    constexpr auto d = scalar_to_color_vector<T>(0.2f);
    constexpr auto e = scalar_to_color_vector<T>(0.02f);
    constexpr auto f = scalar_to_color_vector<T>(0.3f);
    return ((value * (a * value + c * b) + d * e) / (value * (a * value + b) + d * f)) - e / f;
}

template<typename T>
ALWAYS_INLINE constexpr T hable_tonemapping(T value)
{
    constexpr auto exposure_bias = scalar_to_color_vector<T>(2.0f);
    value = hable_tonemapping_partial<T>(value * exposure_bias);
    constexpr auto scale = scalar_to_color_vector<T>(1.0f) / scalar_to_color_vector<T>(hable_tonemapping_partial(11.2f));
    return value * scale;
}

DecoderErrorOr<ColorConverter> ColorConverter::create(u8 bit_depth, CodingIndependentCodePoints cicp)
{
    // We'll need to apply tonemapping for linear HDR values.
    bool should_tonemap = false;
    switch (cicp.transfer_characteristics()) {
    case TransferCharacteristics::SMPTE2084:
        should_tonemap = true;
        break;
    case TransferCharacteristics::HLG:
        should_tonemap = true;
        break;
    default:
        break;
    }

    // Conversion process:
    // 1. Scale integer YUV values with maximum values of (1 << bit_depth) - 1 into
    //    float 0..1 range.
    //    This can be done with a 3x3 scaling matrix.
    size_t maximum_value = (1u << bit_depth) - 1;
    float scale = 1.0 / maximum_value;
    FloatMatrix4x4 integer_scaling_matrix = {
        scale, 0.0f, 0.0f, 0.0f, // y
        0.0f, scale, 0.0f, 0.0f, // u
        0.0f, 0.0f, scale, 0.0f, // v
        0.0f, 0.0f, 0.0f, 1.0f,  // w
    };

    // 2. Scale YUV values into usable ranges.
    //    For studio range, Y range is 16..235, and UV is 16..240.
    //    UV values should be scaled to a range of -1..1.
    //    This can be done in a 4x4 matrix with translation and scaling.
    float y_min;
    float y_max;
    float uv_min;
    float uv_max;
    if (cicp.color_range() == ColorRange::Studio) {
        y_min = 16.0f / 255.0f;
        y_max = 235.0f / 255.0f;
        uv_min = y_min;
        uv_max = 240.0f / 255.0f;
    } else {
        y_min = 0.0f;
        y_max = 1.0f;
        uv_min = 0.0f;
        uv_max = 1.0f;
    }
    auto clip_y_scale = 1.0f / (y_max - y_min);
    auto clip_uv_scale = 2.0f / (uv_max - uv_min);

    FloatMatrix4x4 range_scaling_matrix = {
        clip_y_scale, 0.0f, 0.0f, -y_min * clip_y_scale,             // y
        0.0f, clip_uv_scale, 0.0f, -(uv_min * clip_uv_scale + 1.0f), // u
        0.0f, 0.0f, clip_uv_scale, -(uv_min * clip_uv_scale + 1.0f), // v
        0.0f, 0.0f, 0.0f, 1.0f,                                      // w
    };

    // 3. Convert YUV values to RGB.
    //    This is done with coefficients that can be put into a 3x3 matrix
    //    and combined with the above 4x4 matrix to combine steps 1 and 2.
    FloatMatrix4x4 color_conversion_matrix;

    // https://kdashg.github.io/misc/colors/from-coeffs.html
    switch (cicp.matrix_coefficients()) {
    case MatrixCoefficients::BT709:
        color_conversion_matrix = {
            1.0f, 0.0f, 0.78740f, 0.0f,       // y
            1.0f, -0.09366f, -0.23406f, 0.0f, // u
            1.0f, 0.92780f, 0.0f, 0.0f,       // v
            0.0f, 0.0f, 0.0f, 1.0f,           // w
        };
        break;
    case MatrixCoefficients::BT601:
        color_conversion_matrix = {
            1.0f, 0.0f, 0.70100f, 0.0f,       // y
            1.0f, -0.17207f, -0.35707f, 0.0f, // u
            1.0f, 0.88600f, 0.0f, 0.0f,       // v
            0.0f, 0.0f, 0.0f, 1.0f,           // w
        };
        break;
    case MatrixCoefficients::BT2020ConstantLuminance:
    case MatrixCoefficients::BT2020NonConstantLuminance:
        color_conversion_matrix = {
            1.0f, 0.0f, 0.73730f, 0.0f,       // y
            1.0f, -0.08228f, -0.28568f, 0.0f, // u
            1.0f, 0.94070f, 0.0f, 0.0f,       // v
            0.0f, 0.0f, 0.0f, 1.0f,           // w
        };
        break;
    default:
        return DecoderError::format(DecoderErrorCategory::Invalid, "Matrix coefficients {} not supported", matrix_coefficients_to_string(cicp.matrix_coefficients()));
    }

    // 4. Apply the inverse transfer function to convert RGB values to the
    //    linear color space.
    //    This will be turned into a lookup table and interpolated to speed
    //    up the conversion.
    auto to_linear_lookup_table = InterpolatedLookupTable<to_linear_size>::create(
        [&](float value) {
            return TransferCharacteristicsConversion::to_linear_luminance(value, cicp.transfer_characteristics());
        });

    // 5. Convert the RGB color to CIE XYZ coordinates using the input color
    //    primaries and then to the output color primaries.
    //    This is done with two 3x3 matrices that can be combined into one
    //    matrix multiplication.
    ColorPrimaries output_cp = ColorPrimaries::BT709;
    FloatMatrix3x3 color_primaries_matrix = TRY(get_conversion_matrix(cicp.color_primaries(), output_cp));

    // 6. Apply the output transfer function. For HDR color spaces, this
    //    should apply tonemapping as well.
    //    Use a lookup table as with step 3.
    TransferCharacteristics output_tc = TransferCharacteristics::SRGB;
    switch (cicp.transfer_characteristics()) {
    case TransferCharacteristics::Unspecified:
        break;
    case TransferCharacteristics::BT709:
    case TransferCharacteristics::BT601:
    case TransferCharacteristics::BT2020BitDepth10:
    case TransferCharacteristics::BT2020BitDepth12:
        // BT.601, BT.709 and BT.2020 have a similar transfer function to sRGB, and other applications
        // (Chromium, VLC) seem to keep video output in those transfer characteristics.
        output_tc = TransferCharacteristics::BT709;
        break;
    default:
        break;
    }

    auto to_non_linear_lookup_table = InterpolatedLookupTable<to_non_linear_size>::create(
        [&](float value) {
            return TransferCharacteristicsConversion::to_non_linear_luminance(value, output_tc);
        });

    // Expand color primaries matrix with identity elements.
    FloatMatrix4x4 color_primaries_matrix_4x4 = {
        color_primaries_matrix.elements()[0][0],
        color_primaries_matrix.elements()[0][1],
        color_primaries_matrix.elements()[0][2],
        0.0f, // y
        color_primaries_matrix.elements()[1][0],
        color_primaries_matrix.elements()[1][1],
        color_primaries_matrix.elements()[1][2],
        0.0f, // u
        color_primaries_matrix.elements()[2][0],
        color_primaries_matrix.elements()[2][1],
        color_primaries_matrix.elements()[2][2],
        0.0f, // v
        0.0f,
        0.0f,
        0.0f,
        1.0f, // w
    };

    bool should_skip_color_remapping = output_cp == cicp.color_primaries() && output_tc == cicp.transfer_characteristics();
    FloatMatrix4x4 input_conversion_matrix = color_conversion_matrix * range_scaling_matrix * integer_scaling_matrix;

    return ColorConverter(bit_depth, cicp, should_skip_color_remapping, should_tonemap, input_conversion_matrix, to_linear_lookup_table, color_primaries_matrix_4x4, to_non_linear_lookup_table);
}

ALWAYS_INLINE FloatVector4 max_zero(FloatVector4 vector)
{
    return { max(0.0f, vector.x()), max(0.0f, vector.y()), max(0.0f, vector.z()), vector.w() };
}

// Referencing https://en.wikipedia.org/wiki/YCbCr
Gfx::Color ColorConverter::convert_yuv_to_full_range_rgb(u16 y, u16 u, u16 v)
{
    FloatVector4 color_vector = { static_cast<float>(y), static_cast<float>(u), static_cast<float>(v), 1.0f };
    color_vector = m_input_conversion_matrix * color_vector;

    if (m_should_skip_color_remapping) {
        color_vector.clamp(0.0f, 1.0f);
    } else {
        color_vector = max_zero(color_vector);
        color_vector = m_to_linear_lookup.do_lookup(color_vector);

        if (m_cicp.transfer_characteristics() == TransferCharacteristics::HLG) {
            static auto hlg_ootf_lookup_table = InterpolatedLookupTable<32, 1000>::create(
                [](float value) {
                    return AK::pow(value, 1.2f - 1.0f);
                });
            // See: https://en.wikipedia.org/wiki/Hybrid_log-gamma under a bolded section "HLG reference OOTF"
            float luminance = (0.2627f * color_vector.x() + 0.6780f * color_vector.y() + 0.0593f * color_vector.z()) * 1000.0f;
            float coefficient = hlg_ootf_lookup_table.do_lookup(luminance);
            color_vector = { color_vector.x() * coefficient, color_vector.y() * coefficient, color_vector.z() * coefficient, 1.0f };
        }

        // FIXME: We could implement gamut compression here:
        //        https://github.com/jedypod/gamut-compress/blob/master/docs/gamut-compress-algorithm.md
        //        This would allow the color values outside the output gamut to be
        //        preserved relative to values within the gamut instead of clipping. The
        //        downside is that this requires a pass over the image before conversion
        //        back into gamut is done to find the maximum color values to compress.
        //        The compression would have to be somewhat temporally consistent as well.
        color_vector = m_color_space_conversion_matrix * color_vector;
        color_vector = max_zero(color_vector);
        if (m_should_tonemap)
            color_vector = hable_tonemapping(color_vector);
        color_vector = m_to_non_linear_lookup.do_lookup(color_vector);
        color_vector = max_zero(color_vector);
    }

    u8 r = static_cast<u8>(color_vector.x() * 255.0f);
    u8 g = static_cast<u8>(color_vector.y() * 255.0f);
    u8 b = static_cast<u8>(color_vector.z() * 255.0f);
    return Gfx::Color(r, g, b);
}

}
