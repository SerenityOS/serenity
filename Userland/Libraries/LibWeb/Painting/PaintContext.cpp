/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/PaintContext.h>

namespace Web {

PaintContext::PaintContext(Gfx::Painter& painter, Palette const& palette, float device_pixels_per_css_pixel)
    : m_painter(painter)
    , m_palette(palette)
    , m_device_pixels_per_css_pixel(device_pixels_per_css_pixel)
{
}

SVGContext& PaintContext::svg_context()
{
    // FIXME: This is a total hack to avoid crashing on content that has SVG elements embedded directly in HTML without an <svg> element.
    if (!m_svg_context.has_value())
        m_svg_context = SVGContext { {} };
    return m_svg_context.value();
}

void PaintContext::set_svg_context(SVGContext context)
{
    m_svg_context = move(context);
}

void PaintContext::clear_svg_context()
{
    m_svg_context.clear();
}

DevicePixels PaintContext::rounded_device_pixels(CSSPixels css_pixels) const
{
    return roundf(css_pixels.value() * m_device_pixels_per_css_pixel);
}

DevicePixels PaintContext::enclosing_device_pixels(CSSPixels css_pixels) const
{
    return ceilf(css_pixels.value() * m_device_pixels_per_css_pixel);
}

DevicePixels PaintContext::floored_device_pixels(CSSPixels css_pixels) const
{
    return floorf(css_pixels.value() * m_device_pixels_per_css_pixel);
}

DevicePixelPoint PaintContext::rounded_device_point(CSSPixelPoint point) const
{
    return {
        roundf(point.x().value() * m_device_pixels_per_css_pixel),
        roundf(point.y().value() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelRect PaintContext::enclosing_device_rect(CSSPixelRect rect) const
{
    return {
        floorf(rect.x().value() * m_device_pixels_per_css_pixel),
        floorf(rect.y().value() * m_device_pixels_per_css_pixel),
        ceilf(rect.width().value() * m_device_pixels_per_css_pixel),
        ceilf(rect.height().value() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelRect PaintContext::rounded_device_rect(CSSPixelRect rect) const
{
    return {
        roundf(rect.x().value() * m_device_pixels_per_css_pixel),
        roundf(rect.y().value() * m_device_pixels_per_css_pixel),
        roundf(rect.width().value() * m_device_pixels_per_css_pixel),
        roundf(rect.height().value() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelSize PaintContext::enclosing_device_size(CSSPixelSize size) const
{
    return {
        ceilf(size.width().value() * m_device_pixels_per_css_pixel),
        ceilf(size.height().value() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelSize PaintContext::rounded_device_size(CSSPixelSize size) const
{
    return {
        roundf(size.width().value() * m_device_pixels_per_css_pixel),
        roundf(size.height().value() * m_device_pixels_per_css_pixel)
    };
}

}
