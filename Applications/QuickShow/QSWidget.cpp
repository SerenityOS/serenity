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

#include "QSWidget.h"
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>

QSWidget::QSWidget()
{
    set_fill_with_background_color(false);
}

QSWidget::~QSWidget()
{
}

void QSWidget::relayout()
{
    if (m_bitmap.is_null())
        return;

    float scale_factor = (float)m_scale / 100.0f;

    auto new_size = (Gfx::Size)((Gfx::FloatPoint)m_bitmap->size() * scale_factor);
    m_bitmap_rect.set_size(new_size);

    auto new_location = (Gfx::Point)(size() / 2) - (Gfx::Point)(new_size / 2) - (Gfx::Point)(m_pan_origin * scale_factor);
    m_bitmap_rect.set_location(new_location);

    update();
}

void QSWidget::resize_event(GUI::ResizeEvent& event)
{
    relayout();
    GUI::Widget::resize_event(event);
}

void QSWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.fill_rect_with_checkerboard(rect(), { 8, 8 }, palette().base().darkened(0.9), palette().base());

    if (!m_bitmap.is_null())
        painter.draw_scaled_bitmap(m_bitmap_rect, *m_bitmap, m_bitmap->rect());
}

void QSWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Left)
        return;
    m_click_position = event.position();
    m_saved_pan_origin = m_pan_origin;
}

void QSWidget::mouseup_event(GUI::MouseEvent& event)
{
    UNUSED_PARAM(event);
}

void QSWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!(event.buttons() & GUI::MouseButton::Left))
        return;

    auto delta = (Gfx::FloatPoint)(event.position() - m_click_position);
    float scale_factor = (float)m_scale / 100.0f;
    m_pan_origin = m_saved_pan_origin.translated(-delta / scale_factor);

    relayout();
}

void QSWidget::mousewheel_event(GUI::MouseEvent& event)
{
    auto old_scale = m_scale;
    auto old_scale_factor = (float)m_scale / 100.0f;

    m_scale += -event.wheel_delta() * 10;
    if (m_scale < 10)
        m_scale = 10;
    if (m_scale > 1000)
        m_scale = 1000;

    auto new_scale_factor = (float)m_scale / 100.0f;

    auto focus_point = m_pan_origin - (Gfx::FloatPoint)(event.position() - (Gfx::Point)(size() / 2)) / old_scale_factor;
    m_pan_origin = focus_point - (focus_point - m_pan_origin) * (new_scale_factor / old_scale_factor);

    relayout();

    if (old_scale != m_scale) {
        if (on_scale_change)
            on_scale_change(m_scale);
    }
}

void QSWidget::load_from_file(const String& path)
{
    auto bitmap = Gfx::Bitmap::load_from_file(path);
    if (!bitmap) {
        GUI::MessageBox::show(String::format("Failed to open %s", path.characters()), "Cannot open image", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, window());
        return;
    }

    window()->resize(bitmap->size());

    m_path = path;
    m_bitmap = bitmap;
    m_scale = 100;
    m_pan_origin = { 0, 0 };
    if (on_scale_change)
        on_scale_change(m_scale);
    relayout();
}

void QSWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    if (on_drop)
        on_drop(event);
}
