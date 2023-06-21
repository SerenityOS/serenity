/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Gradient.h"
#include <LibGUI/Painter.h>

namespace Profiler {

static Gfx::Bitmap const& heat_gradient()
{
    static RefPtr<Gfx::Bitmap> bitmap;
    if (!bitmap) {
        bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, { 101, 1 }).release_value_but_fixme_should_propagate_errors();
        GUI::Painter painter(*bitmap);
        painter.fill_rect_with_gradient(Orientation::Horizontal, bitmap->rect(), Color::from_rgb(0xffc080), Color::from_rgb(0xff3000));
    }
    return *bitmap;
}

Color color_for_percent(u8 percent)
{
    VERIFY(percent <= 100);
    return heat_gradient().get_pixel(percent, 0);
}

}
