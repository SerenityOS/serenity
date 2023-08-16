/*
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Image.h"
#include "ScopeWidget.h"
#include <AK/Array.h>

namespace PixelPaint {

// Gfx::Color can produce 64-bit floating-point HSV.
// However, as it internally only uses 8 bits for each color channel, the hue can never have a higher usable resolution than 256 steps.
static constexpr size_t const u_v_steps = 160;

// Convert to and from U or V (-1 to +1) and an index suitable for the vectorscope table.
constexpr size_t u_v_to_index(float u_v)
{
    float normalized_u_v = (u_v + 1.0f) / 2.0f;
    return static_cast<size_t>(floorf(normalized_u_v * u_v_steps)) % u_v_steps;
}
constexpr float u_v_from_index(size_t index)
{
    float normalized_u_v = static_cast<float>(index) / u_v_steps;
    return normalized_u_v * 2.0f - 1.0f;
}

struct PrimaryColorVector;

struct ColorVector {
    constexpr ColorVector(float u, float v)
        : u(u)
        , v(v)
    {
    }

    constexpr explicit ColorVector(Color color)
        : ColorVector(color.to_yuv().u, color.to_yuv().v)
    {
    }

    static constexpr ColorVector from_indices(size_t u_index, size_t v_index)
    {
        return ColorVector(u_v_from_index(u_index), u_v_from_index(v_index));
    }

    constexpr Gfx::FloatPoint to_vector(float scope_size) const
    {
        auto x = u * scope_size / 2.0f;
        // Computer graphics y increases downwards, but mathematical y increases upwards.
        auto y = -v * scope_size / 2.0f;
        return { x, y };
    }

    constexpr operator PrimaryColorVector() const;

    float u;
    float v;
};

struct PrimaryColorVector : public ColorVector {
    constexpr PrimaryColorVector(Color::NamedColor named_color, char symbol)
        : ColorVector(Color { named_color })
        , symbol(symbol)
    {
    }

    constexpr PrimaryColorVector(Color color, char symbol)
        : ColorVector(color.to_yuv().u, color.to_yuv().v)
        , symbol(symbol)
    {
    }

    constexpr PrimaryColorVector(float u, float v, char symbol)
        : ColorVector(u, v)
        , symbol(symbol)
    {
    }

    char symbol;
};

constexpr ColorVector::operator PrimaryColorVector() const
{
    return PrimaryColorVector { u, v, 'X' };
}

// Color vectors that are found in this percentage of pixels and above are displayed with maximum brightness in the scope.
static constexpr float const pixel_percentage_for_max_brightness = 0.01f;
// Which normalized brightness value (and above) gets to be rendered at 100% opacity.
static constexpr float const alpha_range = 2.5f;
// Skin tone line. This was determined manually with a couple of common hex skin tone colors.
static constexpr PrimaryColorVector const skin_tone_color = { Color::from_hsv(18.0, 1.0, 1.0), 'S' };
// Used for primary color box graticules.
static constexpr Array<PrimaryColorVector, 6> const primary_colors = { {
    { Color::Red, 'R' },
    { Color::Magenta, 'M' },
    { Color::Blue, 'B' },
    { Color::Cyan, 'C' },
    { Color::Green, 'G' },
    { Color::Yellow, 'Y' },
} };

// Vectorscopes are a standard tool in professional video/film color grading.
// The Vectorscope shows image colors along the I and Q axis (from YIQ color space), which, to oversimplify, means that you get a weirdly shifted hue circle with the radius being the saturation.
// The brightness for each point in the scope is determined by the number of "color vectors" at that point.
// FIXME: We would want a lot of the scope settings to be user-adjustable. For example: scale, color/bw scope, graticule brightness
class VectorscopeWidget final
    : public ScopeWidget {
    C_OBJECT(VectorscopeWidget);

public:
    virtual ~VectorscopeWidget() override = default;

    virtual void image_changed() override;

private:
    virtual AK::StringView widget_config_name() const override { return "ShowVectorscope"sv; }
    virtual void paint_event(GUI::PaintEvent&) override;

    ErrorOr<void> rebuild_vectorscope_data();
    void rebuild_vectorscope_image();

    // First index is u, second index is v. The value is y.
    Array<Array<float, u_v_steps + 1>, u_v_steps + 1> m_vectorscope_data;
    RefPtr<Gfx::Bitmap> m_vectorscope_image;
};

}
