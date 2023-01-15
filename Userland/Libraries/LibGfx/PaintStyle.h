/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <AK/QuickSort.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Rect.h>

namespace Gfx {

class PaintStyle : public RefCounted<PaintStyle> {
public:
    virtual ~PaintStyle() = default;
    using SamplerFunction = Function<Color(IntPoint)>;
    using PaintFunction = Function<void(SamplerFunction)>;

    friend Painter;
    friend AntiAliasingPainter;

private:
    // Simple paint styles can simply override sample_color() if they can easily generate a color from a coordinate.
    virtual Color sample_color(IntPoint) const { return Color(); };

    // Paint styles that have paint time dependent state (e.g. based on the paint size) may find it easier to override paint().
    // If paint() is overridden sample_color() is unused.
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const
    {
        (void)physical_bounding_box;
        paint([this](IntPoint point) { return sample_color(point); });
    }
};

class SolidColorPaintStyle final : public PaintStyle {
public:
    static NonnullRefPtr<SolidColorPaintStyle> create(Color color)
    {
        return adopt_ref(*new SolidColorPaintStyle(color));
    }

    virtual Color sample_color(IntPoint) const override { return m_color; }

private:
    SolidColorPaintStyle(Color color)
        : m_color(color)
    {
    }

    Color m_color;
};

class GradientPaintStyle : public PaintStyle {
public:
    void add_color_stop(float position, Color color, Optional<float> transition_hint = {})
    {
        add_color_stop(ColorStop { color, position, transition_hint });
    }

    void add_color_stop(ColorStop stop, bool sort = true)
    {
        m_color_stops.append(stop);
        if (sort)
            quick_sort(m_color_stops, [](auto& a, auto& b) { return a.position < b.position; });
    }

    void set_repeat_length(float repeat_length)
    {
        m_repeat_length = repeat_length;
    }

    Span<ColorStop const> color_stops() const { return m_color_stops; }
    Optional<float> repeat_length() const { return m_repeat_length; }

private:
    Vector<ColorStop, 4> m_color_stops;
    Optional<float> m_repeat_length;
};

// These paint styles are based on the CSS gradients. They are relative to the painted
// shape and support premultiplied alpha.

class LinearGradientPaintStyle final : public GradientPaintStyle {
public:
    static NonnullRefPtr<LinearGradientPaintStyle> create(float angle = 0.0f)
    {
        return adopt_ref(*new LinearGradientPaintStyle(angle));
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    LinearGradientPaintStyle(float angle)
        : m_angle(angle)
    {
    }

    float m_angle { 0.0f };
};

class ConicGradientPaintStyle final : public GradientPaintStyle {
public:
    static NonnullRefPtr<ConicGradientPaintStyle> create(IntPoint center, float start_angle = 0.0f)
    {
        return adopt_ref(*new ConicGradientPaintStyle(center, start_angle));
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    ConicGradientPaintStyle(IntPoint center, float start_angle)
        : m_center(center)
        , m_start_angle(start_angle)
    {
    }

    IntPoint m_center;
    float m_start_angle { 0.0f };
};

class RadialGradientPaintStyle final : public GradientPaintStyle {
public:
    static NonnullRefPtr<RadialGradientPaintStyle> create(IntPoint center, IntSize size)
    {
        return adopt_ref(*new RadialGradientPaintStyle(center, size));
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    RadialGradientPaintStyle(IntPoint center, IntSize size)
        : m_center(center)
        , m_size(size)
    {
    }

    IntPoint m_center;
    IntSize m_size;
};

}
