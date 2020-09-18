/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    set_preferred_size(16, 18);
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
    if (event.button() == MouseButton::Left)
        window()->start_wm_resize();
    Widget::mousedown_event(event);
}

}
