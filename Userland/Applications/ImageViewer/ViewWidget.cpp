/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2021, Mohsan Ali <mohsan0073@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ViewWidget.h"
#include <AK/LexicalPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/Timer.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Palette.h>
#include <LibImageDecoderClient/Client.h>

namespace ImageViewer {

ViewWidget::ViewWidget()
    : m_timer(Core::Timer::construct())
{
    set_fill_with_background_color(false);
}

ViewWidget::~ViewWidget()
{
}

void ViewWidget::clear()
{
    m_timer->stop();
    m_decoded_image.clear();
    m_bitmap = nullptr;
    if (on_image_change)
        on_image_change(m_bitmap);
    m_path = {};

    reset_view();
    update();
}

void ViewWidget::flip(Gfx::Orientation orientation)
{
    m_bitmap = m_bitmap->flipped(orientation).release_value_but_fixme_should_propagate_errors();
    set_scale(m_scale);

    resize_window();
}

void ViewWidget::rotate(Gfx::RotationDirection rotation_direction)
{
    m_bitmap = m_bitmap->rotated(rotation_direction).release_value_but_fixme_should_propagate_errors();
    set_scale(m_scale);

    resize_window();
}

bool ViewWidget::is_next_available() const
{
    if (m_current_index.has_value())
        return m_current_index.value() + 1 < m_files_in_same_dir.size();
    return false;
}

bool ViewWidget::is_previous_available() const
{
    if (m_current_index.has_value())
        return m_current_index.value() > 0;
    return false;
}

Vector<String> ViewWidget::load_files_from_directory(const String& path) const
{
    Vector<String> files_in_directory;

    auto current_dir = LexicalPath(path).parent().string();
    Core::DirIterator iterator(current_dir, Core::DirIterator::Flags::SkipDots);
    while (iterator.has_next()) {
        String file = iterator.next_full_path();
        if (!Gfx::Bitmap::is_path_a_supported_image_format(file))
            continue;
        files_in_directory.append(file);
    }
    return files_in_directory;
}

void ViewWidget::set_path(const String& path)
{
    m_path = path;
    m_files_in_same_dir = load_files_from_directory(path);
    m_current_index = m_files_in_same_dir.find_first_index(path);
}

void ViewWidget::navigate(Directions direction)
{
    if (!m_current_index.has_value()) {
        return;
    }

    auto index = m_current_index.value();
    if (direction == Directions::Back) {
        index--;
    } else if (direction == Directions::Forward) {
        index++;
    } else if (direction == Directions::First) {
        index = 0;
    } else if (direction == Directions::Last) {
        index = m_files_in_same_dir.size() - 1;
    }

    m_current_index = index;
    this->load_from_file(m_files_in_same_dir.at(index));
}

void ViewWidget::set_scale(int scale)
{
    if (m_bitmap.is_null())
        return;

    if (scale < 10)
        scale = 10;
    if (scale > 1000)
        scale = 1000;

    m_scale = scale;
    float scale_factor = (float)m_scale / 100.0f;

    Gfx::IntSize new_size;
    new_size.set_width(m_bitmap->width() * scale_factor);
    new_size.set_height(m_bitmap->height() * scale_factor);
    m_bitmap_rect.set_size(new_size);

    if (on_scale_change)
        on_scale_change(m_scale);

    relayout();
}

void ViewWidget::relayout()
{
    if (m_bitmap.is_null())
        return;

    Gfx::IntSize new_size = m_bitmap_rect.size();

    Gfx::IntPoint new_location;
    new_location.set_x((width() / 2) - (new_size.width() / 2) - m_pan_origin.x());
    new_location.set_y((height() / 2) - (new_size.height() / 2) - m_pan_origin.y());
    m_bitmap_rect.set_location(new_location);

    update();
}

void ViewWidget::resize_event(GUI::ResizeEvent& event)
{
    relayout();
    GUI::Widget::resize_event(event);
}

void ViewWidget::doubleclick_event(GUI::MouseEvent&)
{
    on_doubleclick();
}

void ViewWidget::paint_event(GUI::PaintEvent& event)
{
    Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());

    Gfx::StylePainter::paint_transparency_grid(painter, frame_inner_rect(), palette());

    if (!m_bitmap.is_null())
        painter.draw_scaled_bitmap(m_bitmap_rect, *m_bitmap, m_bitmap->rect());
}

void ViewWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (event.button() != GUI::MouseButton::Primary)
        return;
    m_click_position = event.position();
    m_saved_pan_origin = m_pan_origin;
}

void ViewWidget::mouseup_event([[maybe_unused]] GUI::MouseEvent& event) { }

void ViewWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!(event.buttons() & GUI::MouseButton::Primary))
        return;

    auto delta = event.position() - m_click_position;
    m_pan_origin = m_saved_pan_origin.translated(
        -delta.x(),
        -delta.y());

    relayout();
}

void ViewWidget::mousewheel_event(GUI::MouseEvent& event)
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

    // focus_point is the window position the cursor is pointing to.
    // The pixel (in image space) the cursor points to is located at
    // (m_pan_origin + focus_point) / scale_factor.
    // We want the image after scaling to be panned in such a way that the cursor
    // will still point to the same image pixel. Basically, we need to solve
    // (m_pan_origin + focus_point) / old_scale_factor = (new_m_pan_origin + focus_point) / new_scale_factor.
    Gfx::FloatPoint focus_point {
        event.x() - width() / 2.0f,
        event.y() - height() / 2.0f
    };

    // A little algebra shows that new m_pan_origin equals to:
    m_pan_origin = (m_pan_origin + focus_point) * (new_scale_factor / old_scale_factor) - focus_point;

    set_scale(new_scale);
}

void ViewWidget::load_from_file(const String& path)
{
    auto show_error = [&] {
        GUI::MessageBox::show(window(), String::formatted("Failed to open {}", path), "Cannot open image", GUI::MessageBox::Type::Error);
    };

    auto file_or_error = Core::MappedFile::map(path);
    if (file_or_error.is_error()) {
        show_error();
        return;
    }

    auto& mapped_file = *file_or_error.value();

    // Spawn a new ImageDecoder service process and connect to it.
    auto client = ImageDecoderClient::Client::construct();

    auto decoded_image_or_error = client->decode_image(mapped_file.bytes());
    if (!decoded_image_or_error.has_value()) {
        show_error();
        return;
    }

    m_decoded_image = decoded_image_or_error.release_value();
    m_bitmap = m_decoded_image->frames[0].bitmap;
    if (on_image_change)
        on_image_change(m_bitmap);

    if (m_decoded_image->is_animated && m_decoded_image->frames.size() > 1) {
        const auto& first_frame = m_decoded_image->frames[0];
        m_timer->set_interval(first_frame.duration);
        m_timer->on_timeout = [this] { animate(); };
        m_timer->start();
    } else {
        m_timer->stop();
    }

    m_path = Core::File::real_path_for(path);
    m_scale = -1;
    reset_view();
}

void ViewWidget::drop_event(GUI::DropEvent& event)
{
    event.accept();
    if (on_drop)
        on_drop(event);
}

void ViewWidget::resize_window()
{
    if (window()->is_fullscreen() || window()->is_maximized())
        return;

    auto absolute_bitmap_rect = m_bitmap_rect;
    absolute_bitmap_rect.translate_by(window()->rect().top_left());
    if (window()->rect().contains(absolute_bitmap_rect))
        return;

    if (!m_bitmap)
        return;

    auto new_size = m_bitmap_rect.size();

    if (new_size.width() < 300)
        new_size.set_width(300);
    if (new_size.height() < 200)
        new_size.set_height(200);

    new_size.set_height(new_size.height() + m_toolbar_height);
    window()->resize(new_size);
}

void ViewWidget::reset_view()
{
    m_pan_origin = { 0, 0 };
    set_scale(100);
}

void ViewWidget::set_bitmap(const Gfx::Bitmap* bitmap)
{
    if (m_bitmap == bitmap)
        return;
    m_bitmap = bitmap;
    update();
}

// Same as ImageWidget::animate(), you probably want to keep any changes in sync
void ViewWidget::animate()
{
    if (!m_decoded_image.has_value())
        return;

    m_current_frame_index = (m_current_frame_index + 1) % m_decoded_image->frames.size();

    const auto& current_frame = m_decoded_image->frames[m_current_frame_index];
    set_bitmap(current_frame.bitmap);

    if ((int)current_frame.duration != m_timer->interval()) {
        m_timer->restart(current_frame.duration);
    }

    if (m_current_frame_index == m_decoded_image->frames.size() - 1) {
        ++m_loops_completed;
        if (m_loops_completed > 0 && m_loops_completed == m_decoded_image->loop_count) {
            m_timer->stop();
        }
    }
}

}
