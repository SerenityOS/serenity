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
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Palette.h>

QSWidget::QSWidget()
{
    set_fill_with_background_color(false);
}

QSWidget::~QSWidget()
{
}

void QSWidget::clear()
{
    m_bitmap = nullptr;
    m_path = {};

    set_scale(100);
    update();
}

void QSWidget::flip(Gfx::Orientation orientation)
{
    m_bitmap = m_bitmap->flipped(orientation);
    set_scale(m_scale);

    resize_window();
}

void QSWidget::rotate(Gfx::RotationDirection rotation_direction)
{
    m_bitmap = m_bitmap->rotated(rotation_direction);
    set_scale(m_scale);

    resize_window();
}

void QSWidget::navigate(Directions direction)
{
    if (m_path == nullptr)
        return;

    auto parts = m_path.split('/');
    parts.remove(parts.size() - 1);
    StringBuilder sb;
    sb.append("/");
    sb.join("/", parts);
    AK::String current_dir = sb.to_string();

    if (m_files_in_same_dir.is_empty()) {
        Core::DirIterator iterator(current_dir, Core::DirIterator::Flags::SkipDots);
        while (iterator.has_next()) {
            String file = iterator.next_full_path();
            if (!Gfx::Bitmap::is_path_a_supported_image_format(file))
                continue;
            m_files_in_same_dir.append(file);
        }
    }

    auto current_index = m_files_in_same_dir.find_first_index(m_path);
    if (!current_index.has_value()) {
        return;
    }

    size_t index = current_index.value();
    if (direction == Directions::Back) {
        if (index == 0) {
            GUI::MessageBox::show(window(), "This is the first file.", "Cannot open image", GUI::MessageBox::Type::Error);
            return;
        }

        index--;
    } else if (direction == Directions::Forward) {
        if (index == m_files_in_same_dir.size() - 1) {
            GUI::MessageBox::show(window(), "This is the last file.", "Cannot open image", GUI::MessageBox::Type::Error);
            return;
        }

        index++;
    } else if (direction == Directions::First) {
        index = 0;
    } else if (direction == Directions::Last) {
        index = m_files_in_same_dir.size() - 1;
    }

    this->load_from_file(m_files_in_same_dir.at(index));
}

void QSWidget::set_scale(int scale)
{
    if (m_bitmap.is_null())
        return;

    if (m_scale == scale) {
        update();
        return;
    }

    if (scale < 10)
        scale = 10;
    if (scale > 1000)
        scale = 1000;

    if (scale == 100)
        m_pan_origin = { 0, 0 };

    m_scale = scale;
    float scale_factor = (float)m_scale / 100.0f;

    Gfx::IntSize new_size;
    new_size.set_width(m_bitmap->width() * scale_factor);
    new_size.set_height(m_bitmap->height() * scale_factor);
    m_bitmap_rect.set_size(new_size);

    if (on_scale_change)
        on_scale_change(m_scale, m_bitmap_rect);

    relayout();
}

void QSWidget::relayout()
{
    if (m_bitmap.is_null())
        return;

    float scale_factor = (float)m_scale / 100.0f;
    Gfx::IntSize new_size = m_bitmap_rect.size();

    Gfx::IntPoint new_location;
    new_location.set_x((width() / 2) - (new_size.width() / 2) - (m_pan_origin.x() * scale_factor));
    new_location.set_y((height() / 2) - (new_size.height() / 2) - (m_pan_origin.y() * scale_factor));
    m_bitmap_rect.set_location(new_location);

    update();
}

void QSWidget::resize_event(GUI::ResizeEvent& event)
{
    relayout();
    GUI::Widget::resize_event(event);
}

void QSWidget::doubleclick_event(GUI::MouseEvent&)
{
    on_doubleclick();
}

void QSWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    Gfx::StylePainter::paint_transparency_grid(painter, frame_inner_rect(), palette());

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

    auto delta = event.position() - m_click_position;
    float scale_factor = (float)m_scale / 100.0f;
    m_pan_origin = m_saved_pan_origin.translated(
        -delta.x() / scale_factor,
        -delta.y() / scale_factor);

    relayout();
}

void QSWidget::mousewheel_event(GUI::MouseEvent& event)
{
    int new_scale = m_scale - event.wheel_delta() * 10;
    if (new_scale < 10)
        new_scale = 10;
    if (new_scale > 1000)
        new_scale = 1000;

    if (new_scale == m_scale) {
        return;
    }

    auto old_scale_factor = (float)m_scale / 100.0f;
    auto new_scale_factor = (float)new_scale / 100.0f;

    auto focus_point = Gfx::FloatPoint(
        m_pan_origin.x() - ((float)event.x() - (float)width() / 2.0) / old_scale_factor,
        m_pan_origin.y() - ((float)event.y() - (float)height() / 2.0) / old_scale_factor);

    m_pan_origin = Gfx::FloatPoint(
        focus_point.x() - new_scale_factor / old_scale_factor * (focus_point.x() - m_pan_origin.x()),
        focus_point.y() - new_scale_factor / old_scale_factor * (focus_point.y() - m_pan_origin.y()));

    set_scale(new_scale);
}

void QSWidget::load_from_file(const String& path)
{
    auto bitmap = Gfx::Bitmap::load_from_file(path);
    if (!bitmap) {
        GUI::MessageBox::show(window(), String::formatted("Failed to open {}", path), "Cannot open image", GUI::MessageBox::Type::Error);
        return;
    }

    m_path = path;
    m_bitmap = bitmap;
    m_scale = -1;
    set_scale(100);
}

void QSWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    if (on_drop)
        on_drop(event);
}

void QSWidget::resize_window()
{
    if (window()->is_fullscreen())
        return;

    if (!m_bitmap)
        return;

    auto new_size = m_bitmap->size();

    if (new_size.width() < 300)
        new_size.set_width(300);
    if (new_size.height() < 200)
        new_size.set_height(200);

    new_size.set_height(new_size.height() + m_toolbar_height);
    window()->resize(new_size);
}
