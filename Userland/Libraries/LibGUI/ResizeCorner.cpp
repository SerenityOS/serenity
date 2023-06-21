/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/ResizeCorner.h>
#include <LibGUI/Window.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>

namespace GUI {

static constexpr Gfx::CharacterBitmap s_resize_corner_shadows_bitmap {

    "                "
    "             ## "
    "             #  "
    "                "
    "          ## ## "
    "          #  #  "
    "                "
    "       ## ## ## "
    "       #  #  #  "
    "                "
    "    ## ## ## ## "
    "    #  #  #  #  "
    "                "
    " ## ## ## ## ## "
    " #  #  #  #  #  "
    "                "sv,
    16, 16
};

static constexpr Gfx::CharacterBitmap s_resize_corner_highlights_bitmap {
    "                "
    "                "
    "              # "
    "                "
    "                "
    "           #  # "
    "                "
    "                "
    "        #  #  # "
    "                "
    "                "
    "     #  #  #  # "
    "                "
    "                "
    "  #  #  #  #  # "
    "                "sv,
    16, 16
};

ResizeCorner::ResizeCorner()
{
    set_override_cursor(Gfx::StandardCursor::ResizeDiagonalTLBR);
    set_background_role(ColorRole::Button);
    set_fixed_size(16, 18);
}

void ResizeCorner::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), palette().color(background_role()));

    painter.draw_bitmap({ 0, 2 }, s_resize_corner_shadows_bitmap, palette().threed_shadow1());
    painter.draw_bitmap({ 0, 2 }, s_resize_corner_highlights_bitmap, palette().threed_highlight());

    Widget::paint_event(event);
}

void ResizeCorner::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary)
        window()->start_interactive_resize(ResizeDirection::DownRight);
    Widget::mousedown_event(event);
}

}
