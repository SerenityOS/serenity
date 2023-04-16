/*
 * Copyright (c) 2022, Gregory Bertilson <zaggy1024@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Function.h>
#include <LibGfx/Color.h>
#include <LibGfx/Matrix4x4.h>
#include <LibVideo/Color/CodingIndependentCodePoints.h>
#include <LibVideo/DecoderError.h>

namespace Video {

template<size_t N, size_t Scale = 1>
struct InterpolatedLookupTable {
public:
    static InterpolatedLookupTable<N, Scale> create(Function<float(float)> transfer_function)
    {
        // We'll allocate one extra index to allow the values to reach 1.0.
        InterpolatedLookupTable<N, Scale> lookup_table;
        float index_to_value_mult = static_cast<float>(Scale) / maximum_value;
        for (size_t i = 0; i < N; i++) {
            float value = i * index_to_value_mult;
            value = transfer_function(value);
            lookup_table.m_lookup_table[i] = value;
        }
        return lookup_table;
    }

    ALWAYS_INLINE float do_lookup(float value) const
    {
        float float_index = value * (maximum_value / static_cast<float>(Scale));
        if (float_index > maximum_value) [[unlikely]]
            float_index = maximum_value;
        size_t index = static_cast<size_t>(float_index);
        float partial_index = float_index - index;
        value = m_lookup_table[index] * (1.0f - partial_index) + m_lookup_table[index + 1] * partial_index;
        return value;
    }

    ALWAYS_INLINE FloatVector4 do_lookup(FloatVector4 vector) const
    {
        return {
            do_lookup(vector.x()),
            do_lookup(vector.y()),
            do_lookup(vector.z()),
            vector.w()
        };
    }

private:
    static constexpr size_t maximum_value = N - 2;

    Array<float, N> m_lookup_table;
};

static auto hlg_ootf_lookup_table = InterpolatedLookupTable<32, 1000>::create(
    [](float value) {
        return AK::pow(value, 1.2f - 1.0f);
    });

class ColorConverter final {

private:
    // Tonemapping methods are outlined here:
    // https://64.github.io/tonemapping/

    template<typename T>
    static ALWAYS_INLINE constexpr T scalar_to_color_vector(float value)
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
    static ALWAYS_INLINE constexpr T hable_tonemapping_partial(T value)
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
    static ALWAYS_INLINE constexpr T hable_tonemapping(T value)
    {
        constexpr auto exposure_bias = scalar_to_color_vector<T>(2.0f);
        value = hable_tonemapping_partial<T>(value * exposure_bias);
        constexpr auto scale = scalar_to_color_vector<T>(1.0f) / scalar_to_color_vector<T>(hable_tonemapping_partial(11.2f));
        return value * scale;
    }

public:
    static DecoderErrorOr<ColorConverter> create(u8 bit_depth, CodingIndependentCodePoints cicp);

    // Referencing https://en.wikipedia.org/wiki/YCbCr
    ALWAYS_INLINE Gfx::Color convert_yuv_to_full_range_rgb(u16 y, u16 u, u16 v) const
    {
        auto max_zero = [](FloatVector4 vector) {
            return FloatVector4(max(0.0f, vector.x()), max(0.0f, vector.y()), max(0.0f, vector.z()), vector.w());
        };

        FloatVector4 color_vector = { static_cast<float>(y), static_cast<float>(u), static_cast<float>(v), 1.0f };
        color_vector = m_input_conversion_matrix * color_vector;

        if (m_should_skip_color_remapping) {
            color_vector.clamp(0.0f, 1.0f);
        } else {
            color_vector = max_zero(color_vector);
            color_vector = m_to_linear_lookup.do_lookup(color_vector);

            if (m_cicp.transfer_characteristics() == TransferCharacteristics::HLG) {
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

private:
    static constexpr size_t to_linear_size = 64;
    static constexpr size_t to_non_linear_size = 64;

    ColorConverter(u8 bit_depth, CodingIndependentCodePoints cicp, bool should_skip_color_remapping, bool should_tonemap, FloatMatrix4x4 input_conversion_matrix, InterpolatedLookupTable<to_linear_size> to_linear_lookup, FloatMatrix4x4 color_space_conversion_matrix, InterpolatedLookupTable<to_non_linear_size> to_non_linear_lookup)
        : m_bit_depth(bit_depth)
        , m_cicp(cicp)
        , m_should_skip_color_remapping(should_skip_color_remapping)
        , m_should_tonemap(should_tonemap)
        , m_input_conversion_matrix(input_conversion_matrix)
        , m_to_linear_lookup(move(to_linear_lookup))
        , m_color_space_conversion_matrix(color_space_conversion_matrix)
        , m_to_non_linear_lookup(move(to_non_linear_lookup))
    {
    }

    u8 m_bit_depth;
    CodingIndependentCodePoints m_cicp;
    bool m_should_skip_color_remapping;
    bool m_should_tonemap;
    FloatMatrix4x4 m_input_conversion_matrix;
    InterpolatedLookupTable<to_linear_size> m_to_linear_lookup;
    FloatMatrix4x4 m_color_space_conversion_matrix;
    InterpolatedLookupTable<to_non_linear_size> m_to_non_linear_lookup;
};

}
