/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Painting/BorderRadiiData.h>
#include <LibWeb/Painting/PaintContext.h>

namespace Web::Painting {

Gfx::AntiAliasingPainter::CornerRadius BorderRadiusData::as_corner(PaintContext& context) const
{
    return Gfx::AntiAliasingPainter::CornerRadius {
        context.floored_device_pixels(horizontal_radius).value(),
        context.floored_device_pixels(vertical_radius).value()
    };
}

}
