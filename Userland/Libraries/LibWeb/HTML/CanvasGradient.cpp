/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/HTML/CanvasGradient.h>

namespace Web::HTML {

NonnullRefPtr<CanvasGradient> CanvasGradient::create_radial(double x0, double y0, double r0, double x1, double y1, double r1)
{
    (void)x0;
    (void)y0;
    (void)r0;
    (void)x1;
    (void)y1;
    (void)r1;
    return adopt_ref(*new CanvasGradient(Type::Radial));
}

NonnullRefPtr<CanvasGradient> CanvasGradient::create_linear(double x0, double y0, double x1, double y1)
{
    (void)x0;
    (void)y0;
    (void)x1;
    (void)y1;
    return adopt_ref(*new CanvasGradient(Type::Linear));
}

NonnullRefPtr<CanvasGradient> CanvasGradient::create_conic(double start_angle, double x, double y)
{
    (void)start_angle;
    (void)x;
    (void)y;
    return adopt_ref(*new CanvasGradient(Type::Conic));
}

CanvasGradient::CanvasGradient(Type type)
    : m_type(type)
{
}

CanvasGradient::~CanvasGradient()
{
}

void CanvasGradient::add_color_stop(double offset, String const& color)
{
    dbgln("CanvasGradient#addColorStop({}, '{}')", offset, color);
}

}
