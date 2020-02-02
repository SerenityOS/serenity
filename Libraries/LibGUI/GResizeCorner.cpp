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

GResizeCorner::GResizeCorner(GWidget* parent)
    : GWidget(parent)
{
    set_background_role(ColorRole::Button);
    set_size_policy(SizePolicy::Fixed, SizePolicy::Fixed);
    set_preferred_size(16, 16);
    m_bitmap = GraphicsBitmap::load_from_file("/res/icons/resize-corner.png");
    ASSERT(m_bitmap);
}

GResizeCorner::~GResizeCorner()
{
}

void GResizeCorner::paint_event(GPaintEvent& event)
{
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.fill_rect(rect(), palette().color(background_role()));
    painter.blit({ 0, 0 }, *m_bitmap, m_bitmap->rect());
    GWidget::paint_event(event);
}

void GResizeCorner::mousedown_event(GMouseEvent& event)
{
    if (event.button() == GMouseButton::Left)
        window()->start_wm_resize();
    GWidget::mousedown_event(event);
}

void GResizeCorner::enter_event(Core::Event& event)
{
    window()->set_override_cursor(GStandardCursor::ResizeDiagonalTLBR);
    GWidget::enter_event(event);
}

void GResizeCorner::leave_event(Core::Event& event)
{
    window()->set_override_cursor(GStandardCursor::None);
    GWidget::leave_event(event);
}
