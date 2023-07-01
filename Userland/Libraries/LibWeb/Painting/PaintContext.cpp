/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web {

PaintContext::PaintContext(Gfx::Painter& painter, Palette const& palette, double device_pixels_per_css_pixel)
    : m_painter(painter)
    , m_palette(palette)
    , m_device_pixels_per_css_pixel(device_pixels_per_css_pixel)
{
}

CSSPixelRect PaintContext::css_viewport_rect() const
{
    return {
        m_device_viewport_rect.x().value() / m_device_pixels_per_css_pixel,
        m_device_viewport_rect.y().value() / m_device_pixels_per_css_pixel,
        m_device_viewport_rect.width().value() / m_device_pixels_per_css_pixel,
        m_device_viewport_rect.height().value() / m_device_pixels_per_css_pixel
    };
}

DevicePixels PaintContext::rounded_device_pixels(CSSPixels css_pixels) const
{
    return roundf(css_pixels.to_double() * m_device_pixels_per_css_pixel);
}

DevicePixels PaintContext::enclosing_device_pixels(CSSPixels css_pixels) const
{
    return ceilf(css_pixels.to_double() * m_device_pixels_per_css_pixel);
}

DevicePixels PaintContext::floored_device_pixels(CSSPixels css_pixels) const
{
    return floorf(css_pixels.to_double() * m_device_pixels_per_css_pixel);
}

DevicePixelPoint PaintContext::rounded_device_point(CSSPixelPoint point) const
{
    return {
        roundf(point.x().to_double() * m_device_pixels_per_css_pixel),
        roundf(point.y().to_double() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelPoint PaintContext::floored_device_point(CSSPixelPoint point) const
{
    return {
        floorf(point.x().to_double() * m_device_pixels_per_css_pixel),
        floorf(point.y().to_double() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelRect PaintContext::enclosing_device_rect(CSSPixelRect rect) const
{
    return {
        floorf(rect.x().to_double() * m_device_pixels_per_css_pixel),
        floorf(rect.y().to_double() * m_device_pixels_per_css_pixel),
        ceilf(rect.width().to_double() * m_device_pixels_per_css_pixel),
        ceilf(rect.height().to_double() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelRect PaintContext::rounded_device_rect(CSSPixelRect rect) const
{
    return {
        roundf(rect.x().to_double() * m_device_pixels_per_css_pixel),
        roundf(rect.y().to_double() * m_device_pixels_per_css_pixel),
        roundf(rect.width().to_double() * m_device_pixels_per_css_pixel),
        roundf(rect.height().to_double() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelSize PaintContext::enclosing_device_size(CSSPixelSize size) const
{
    return {
        ceilf(size.width().to_double() * m_device_pixels_per_css_pixel),
        ceilf(size.height().to_double() * m_device_pixels_per_css_pixel)
    };
}

DevicePixelSize PaintContext::rounded_device_size(CSSPixelSize size) const
{
    return {
        roundf(size.width().to_double() * m_device_pixels_per_css_pixel),
        roundf(size.height().to_double() * m_device_pixels_per_css_pixel)
    };
}

CSSPixels PaintContext::scale_to_css_pixels(DevicePixels device_pixels) const
{
    return device_pixels.value() / m_device_pixels_per_css_pixel;
}

CSSPixelPoint PaintContext::scale_to_css_point(DevicePixelPoint point) const
{
    return {
        scale_to_css_pixels(point.x()),
        scale_to_css_pixels(point.y())
    };
}

CSSPixelSize PaintContext::scale_to_css_size(DevicePixelSize size) const
{
    return {
        scale_to_css_pixels(size.width()),
        scale_to_css_pixels(size.height())
    };
}

CSSPixelRect PaintContext::scale_to_css_rect(DevicePixelRect rect) const
{
    return {
        scale_to_css_point(rect.location()),
        scale_to_css_size(rect.size())
    };
}

bool PaintContext::would_be_fully_clipped_by_painter(DevicePixelRect rect) const
{
    return !painter().clip_rect().intersects(rect.to_type<int>().translated(painter().translation()));
}

}
