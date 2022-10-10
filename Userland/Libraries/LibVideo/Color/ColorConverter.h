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

    float do_lookup(float value) const
    {
        float float_index = value * (maximum_value / static_cast<float>(Scale));
        if (float_index > maximum_value) [[unlikely]]
            float_index = maximum_value;
        size_t index = static_cast<size_t>(float_index);
        float partial_index = float_index - index;
        value = m_lookup_table[index] * (1.0f - partial_index) + m_lookup_table[index + 1] * partial_index;
        return value;
    }

    FloatVector4 do_lookup(FloatVector4 vector) const
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

class ColorConverter final {

public:
    static DecoderErrorOr<ColorConverter> create(u8 bit_depth, CodingIndependentCodePoints cicp);

    Gfx::Color convert_yuv_to_full_range_rgb(u16 y, u16 u, u16 v);

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
