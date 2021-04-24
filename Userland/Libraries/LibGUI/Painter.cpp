/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>

namespace GUI {
Painter::Painter(Gfx::Bitmap& bitmap)
    : Gfx::Painter(bitmap)
{
}

Painter::Painter(Widget& widget)
    : Painter(*widget.window()->back_bitmap())
{
    state().font = &widget.font();
    auto origin_rect = widget.window_relative_rect();
    state().translation = origin_rect.location();
    state().clip_rect = origin_rect.intersected(m_target->rect());
    m_clip_origin = state().clip_rect;
}

}
