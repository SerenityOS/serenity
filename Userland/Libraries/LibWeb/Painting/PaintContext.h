/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Rect.h>
#include <LibWeb/PixelUnits.h>
#include <LibWeb/SVG/SVGContext.h>

namespace Web {

class PaintContext {
public:
    PaintContext(Gfx::Painter& painter, Palette const& palette, float device_pixels_per_css_pixel);

    Gfx::Painter& painter() const { return m_painter; }
    Palette const& palette() const { return m_palette; }

    bool has_svg_context() const { return m_svg_context.has_value(); }
    SVGContext& svg_context();
    void set_svg_context(SVGContext);
    void clear_svg_context();

    bool should_show_line_box_borders() const { return m_should_show_line_box_borders; }
    void set_should_show_line_box_borders(bool value) { m_should_show_line_box_borders = value; }

    DevicePixelRect device_viewport_rect() const { return m_device_viewport_rect; }
    void set_device_viewport_rect(DevicePixelRect const& rect) { m_device_viewport_rect = rect; }
    CSSPixelRect css_viewport_rect() const;

    bool has_focus() const { return m_focus; }
    void set_has_focus(bool focus) { m_focus = focus; }

    DevicePixels enclosing_device_pixels(CSSPixels css_pixels) const;
    DevicePixels floored_device_pixels(CSSPixels css_pixels) const;
    DevicePixels rounded_device_pixels(CSSPixels css_pixels) const;
    DevicePixelPoint rounded_device_point(CSSPixelPoint) const;
    DevicePixelPoint floored_device_point(CSSPixelPoint) const;
    DevicePixelRect enclosing_device_rect(CSSPixelRect) const;
    DevicePixelRect rounded_device_rect(CSSPixelRect) const;
    DevicePixelSize enclosing_device_size(CSSPixelSize) const;
    DevicePixelSize rounded_device_size(CSSPixelSize) const;
    CSSPixels scale_to_css_pixels(DevicePixels) const;
    CSSPixelPoint scale_to_css_point(DevicePixelPoint) const;

    PaintContext clone(Gfx::Painter& painter) const
    {
        auto clone = PaintContext(painter, m_palette, m_device_pixels_per_css_pixel);
        clone.m_device_viewport_rect = m_device_viewport_rect;
        clone.m_should_show_line_box_borders = m_should_show_line_box_borders;
        clone.m_focus = m_focus;
        clone.m_svg_context = m_svg_context;
        return clone;
    }

    float device_pixels_per_css_pixel() const { return m_device_pixels_per_css_pixel; }

private:
    Gfx::Painter& m_painter;
    Palette m_palette;
    Optional<SVGContext> m_svg_context;
    float m_device_pixels_per_css_pixel;
    DevicePixelRect m_device_viewport_rect;
    bool m_should_show_line_box_borders { false };
    bool m_focus { false };
};

}
