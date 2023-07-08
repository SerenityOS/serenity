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
#include <LibGfx/Bitmap.h>
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

    // Paint styles that have paint time dependent state (e.g. based on the paint size) may find it easier to override paint().
    // If paint() is overridden sample_color() is unused.
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const
    {
        (void)physical_bounding_box;
        paint([this](IntPoint point) { return sample_color(point); });
    }

private:
    // Simple paint styles can simply override sample_color() if they can easily generate a color from a coordinate.
    virtual Color sample_color(IntPoint) const { return Color(); }
};

class SolidColorPaintStyle final : public PaintStyle {
public:
    static ErrorOr<NonnullRefPtr<SolidColorPaintStyle>> create(Color color)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) SolidColorPaintStyle(color));
    }

    virtual Color sample_color(IntPoint) const override { return m_color; }

private:
    SolidColorPaintStyle(Color color)
        : m_color(color)
    {
    }

    Color m_color;
};

class BitmapPaintStyle : public PaintStyle {
public:
    static ErrorOr<NonnullRefPtr<BitmapPaintStyle>> create(Bitmap const& bitmap, IntPoint offset = {})
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) BitmapPaintStyle(bitmap, offset));
    }

    virtual Color sample_color(IntPoint point) const override
    {
        point += m_offset;
        if (m_bitmap->rect().contains(point))
            return m_bitmap->get_pixel(point);
        return Color();
    }

private:
    BitmapPaintStyle(Bitmap const& bitmap, IntPoint offset)
        : m_bitmap(bitmap)
        , m_offset(offset)
    {
    }

    NonnullRefPtr<Bitmap const> m_bitmap;
    IntPoint m_offset;
};

class GradientPaintStyle : public PaintStyle {
public:
    ErrorOr<void> add_color_stop(float position, Color color, Optional<float> transition_hint = {})
    {
        return add_color_stop(ColorStop { color, position, transition_hint });
    }

    ErrorOr<void> add_color_stop(ColorStop stop, bool sort = true)
    {
        TRY(m_color_stops.try_append(stop));
        if (sort)
            quick_sort(m_color_stops, [](auto& a, auto& b) { return a.position < b.position; });
        return {};
    }

    void set_repeat_length(float repeat_length)
    {
        m_repeat_length = repeat_length;
    }

    ReadonlySpan<ColorStop> color_stops() const { return m_color_stops; }
    Optional<float> repeat_length() const { return m_repeat_length; }

private:
    Vector<ColorStop, 4> m_color_stops;
    Optional<float> m_repeat_length;
};

// These paint styles are based on the CSS gradients. They are relative to the painted
// shape and support premultiplied alpha.

class LinearGradientPaintStyle final : public GradientPaintStyle {
public:
    static ErrorOr<ErrorOr<NonnullRefPtr<LinearGradientPaintStyle>>> create(float angle = 0.0f)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) LinearGradientPaintStyle(angle));
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
    static ErrorOr<NonnullRefPtr<ConicGradientPaintStyle>> create(IntPoint center, float start_angle = 0.0f)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) ConicGradientPaintStyle(center, start_angle));
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
    static ErrorOr<NonnullRefPtr<RadialGradientPaintStyle>> create(IntPoint center, IntSize size)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) RadialGradientPaintStyle(center, size));
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

// The following paint styles implement the gradients required for the HTML canvas.
// These gradients are (unlike CSS ones) not relative to the painted shape, and do not
// support premultiplied alpha.

class CanvasLinearGradientPaintStyle final : public GradientPaintStyle {
public:
    static ErrorOr<NonnullRefPtr<CanvasLinearGradientPaintStyle>> create(FloatPoint p0, FloatPoint p1)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) CanvasLinearGradientPaintStyle(p0, p1));
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    CanvasLinearGradientPaintStyle(FloatPoint p0, FloatPoint p1)
        : m_p0(p0)
        , m_p1(p1)
    {
    }

    FloatPoint m_p0;
    FloatPoint m_p1;
};

class CanvasConicGradientPaintStyle final : public GradientPaintStyle {
public:
    static ErrorOr<NonnullRefPtr<CanvasConicGradientPaintStyle>> create(FloatPoint center, float start_angle = 0.0f)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) CanvasConicGradientPaintStyle(center, start_angle));
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    CanvasConicGradientPaintStyle(FloatPoint center, float start_angle)
        : m_center(center)
        , m_start_angle(start_angle)
    {
    }

    FloatPoint m_center;
    float m_start_angle { 0.0f };
};

class CanvasRadialGradientPaintStyle final : public GradientPaintStyle {
public:
    static ErrorOr<NonnullRefPtr<CanvasRadialGradientPaintStyle>> create(FloatPoint start_center, float start_radius, FloatPoint end_center, float end_radius)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) CanvasRadialGradientPaintStyle(start_center, start_radius, end_center, end_radius));
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    CanvasRadialGradientPaintStyle(FloatPoint start_center, float start_radius, FloatPoint end_center, float end_radius)
        : m_start_center(start_center)
        , m_start_radius(start_radius)
        , m_end_center(end_center)
        , m_end_radius(end_radius)
    {
    }

    FloatPoint m_start_center;
    float m_start_radius { 0.0f };
    FloatPoint m_end_center;
    float m_end_radius { 0.0f };
};

// The following paint styles implement the gradients required for SVGs

class SVGGradientPaintStyle : public GradientPaintStyle {
public:
    void set_gradient_transform(Gfx::AffineTransform transform);

protected:
    Optional<AffineTransform> const& scale_adjusted_inverse_gradient_transform() const { return m_inverse_transform; }
    float gradient_transform_scale() const { return m_scale; }

private:
    Optional<AffineTransform> m_inverse_transform {};
    float m_scale = 1.0f;
};

class SVGLinearGradientPaintStyle final : public SVGGradientPaintStyle {
public:
    static ErrorOr<NonnullRefPtr<SVGLinearGradientPaintStyle>> create(FloatPoint p0, FloatPoint p1)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) SVGLinearGradientPaintStyle(p0, p1));
    }

    void set_start_point(FloatPoint start_point)
    {
        m_p0 = start_point;
    }

    void set_end_point(FloatPoint end_point)
    {
        m_p1 = end_point;
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    SVGLinearGradientPaintStyle(FloatPoint p0, FloatPoint p1)
        : m_p0(p0)
        , m_p1(p1)
    {
    }

    FloatPoint m_p0;
    FloatPoint m_p1;
};

class SVGRadialGradientPaintStyle final : public SVGGradientPaintStyle {
public:
    static ErrorOr<NonnullRefPtr<SVGRadialGradientPaintStyle>> create(FloatPoint start_center, float start_radius, FloatPoint end_center, float end_radius)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) SVGRadialGradientPaintStyle(start_center, start_radius, end_center, end_radius));
    }

    void set_start_center(FloatPoint start_center)
    {
        m_start_center = start_center;
    }

    void set_start_radius(float start_radius)
    {
        m_start_radius = start_radius;
    }

    void set_end_center(FloatPoint end_center)
    {
        m_end_center = end_center;
    }

    void set_end_radius(float end_radius)
    {
        m_end_radius = end_radius;
    }

private:
    virtual void paint(IntRect physical_bounding_box, PaintFunction paint) const override;

    SVGRadialGradientPaintStyle(FloatPoint start_center, float start_radius, FloatPoint end_center, float end_radius)
        : m_start_center(start_center)
        , m_start_radius(start_radius)
        , m_end_center(end_center)
        , m_end_radius(end_radius)
    {
    }

    FloatPoint m_start_center;
    float m_start_radius { 0.0f };
    FloatPoint m_end_center;
    float m_end_radius { 0.0f };
};

}
