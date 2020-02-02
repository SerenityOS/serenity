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

#include <LibDraw/GraphicsBitmap.h>
#include <LibDraw/Palette.h>
#include <LibGUI/GPainter.h>
#include <LibGUI/GResizeCorner.h>
#include <LibGUI/GWindow.h>

namespace GUI {

ResizeCorner::ResizeCorner(Widget* parent)
    : Widget(parent)
{
    set_background_role(ColorRole::Button);
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    set_preferred_size(16, 16);
    m_bitmap = GraphicsBitmap::load_from_file("/res/icons/resize-corner.png");
    ASSERT(m_bitmap);
}

ResizeCorner::~ResizeCorner()
{
}

void ResizeCorner::paint_event(PaintEvent& event)
{
    Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), palette().color(background_role()));
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
    Widget::paint_event(event);
}

void ResizeCorner::mousedown_event(MouseEvent& event)
{
    if (event.button() == MouseButton::Left)
        window()->start_wm_resize();
    Widget::mousedown_event(event);
}

void ResizeCorner::enter_event(Core::Event& event)
{
    window()->set_override_cursor(StandardCursor::ResizeDiagonalTLBR);
    Widget::enter_event(event);
}

void ResizeCorner::leave_event(Core::Event& event)
{
    window()->set_override_cursor(StandardCursor::None);
    Widget::leave_event(event);
}

}
