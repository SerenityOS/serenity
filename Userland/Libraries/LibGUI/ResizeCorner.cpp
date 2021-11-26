/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Painter.h>
#include <LibGUI/ResizeCorner.h>
#include <LibGUI/Window.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/Palette.h>

namespace GUI {

static const char* s_resize_corner_shadows_data = {
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
    "                "
};

static const char* s_resize_corner_highlights_data = {
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
    "                "
};

static Gfx::CharacterBitmap* s_resize_corner_shadows_bitmap;
static Gfx::CharacterBitmap* s_resize_corner_highlights_bitmap;
static const int s_resize_corner_bitmap_width = 16;
static const int s_resize_corner_bitmap_height = 16;

ResizeCorner::ResizeCorner()
{
    set_override_cursor(Gfx::StandardCursor::ResizeDiagonalTLBR);
    set_background_role(ColorRole::Button);
    set_fixed_size(16, 18);
}

ResizeCorner::~ResizeCorner()
{
}

void ResizeCorner::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), palette().color(background_role()));

    if (!s_resize_corner_shadows_bitmap)
        s_resize_corner_shadows_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_resize_corner_shadows_data, s_resize_corner_bitmap_width, s_resize_corner_bitmap_height).leak_ref();
    painter.draw_bitmap({ 0, 2 }, *s_resize_corner_shadows_bitmap, palette().threed_shadow1());

    if (!s_resize_corner_highlights_bitmap)
        s_resize_corner_highlights_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_resize_corner_highlights_data, s_resize_corner_bitmap_width, s_resize_corner_bitmap_height).leak_ref();
    painter.draw_bitmap({ 0, 2 }, *s_resize_corner_highlights_bitmap, palette().threed_highlight());

    Widget::paint_event(event);
}

void ResizeCorner::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Primary)
        window()->start_interactive_resize();
    Widget::mousedown_event(event);
}

}
