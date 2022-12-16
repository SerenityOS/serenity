/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MoveTool.h"
#include "../Image.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Filters/ContrastFilter.h>

namespace PixelPaint {

void MoveTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (event.image_event().button() == GUI::MouseButton::Secondary) {
        m_editor->start_panning(event.raw_event().position());
        return;
    }

    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    auto& image_event = event.image_event();
    if (layer_event.button() != GUI::MouseButton::Primary)
        return;
    if (!layer->rect().contains(layer_event.position()) && !m_mouse_in_resize_corner)
        return;
    m_scaling = m_mouse_in_resize_corner;
    m_layer_being_moved = *layer;
    m_event_origin = image_event.position();
    m_layer_origin = layer->location();
}

void MoveTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (m_editor->is_panning()) {
        m_editor->pan_to(event.raw_event().position());
        return;
    }

    if (!layer)
        return;

    constexpr int sensitivity = 20;
    auto bottom_right_layer_coordinates = layer->relative_rect().bottom_right().translated(1);
    auto bottom_right_frame_coordinates = m_editor->content_to_frame_position(bottom_right_layer_coordinates).to_rounded<int>();
    auto grab_rect_top_left = bottom_right_frame_coordinates.translated(-sensitivity / 2);
    auto grab_rect = Gfx::IntRect(grab_rect_top_left, Gfx::IntSize(sensitivity, sensitivity));
    auto updated_is_in_lower_right_corner = grab_rect.contains(event.raw_event().position()); // check if the mouse is in the lower right corner
    if (m_mouse_in_resize_corner != updated_is_in_lower_right_corner) {
        m_mouse_in_resize_corner = updated_is_in_lower_right_corner;
        m_editor->update_tool_cursor();
    }

    if (!m_layer_being_moved)
        return;

    if (m_scaling) {
        auto cursor_location = event.image_event().position();
        auto width = abs(m_layer_being_moved->location().x() - cursor_location.x());
        auto height = abs(m_layer_being_moved->location().y() - cursor_location.y());
        if (m_keep_ascept_ratio) {
            if (abs(width - m_layer_being_moved->size().width()) > abs(height - m_layer_being_moved->size().height())) {
                height = width * m_layer_being_moved->size().height() / m_layer_being_moved->size().width();
            } else {
                width = height * m_layer_being_moved->size().width() / m_layer_being_moved->size().height();
            }
        }
        m_new_layer_size = Gfx::IntSize(width, height);
        // TODO: Change this according to which direction the user is scaling
        m_new_scaled_layer_location = m_layer_being_moved->location();
    } else {
        auto& image_event = event.image_event();
        auto delta = image_event.position() - m_event_origin;
        m_layer_being_moved->set_location(m_layer_origin.translated(delta));
    }
    m_editor->update();
}

void MoveTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (event.image_event().button() == GUI::MouseButton::Secondary) {
        m_editor->stop_panning();
        m_editor->set_override_cursor(cursor());
        return;
    }

    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary)
        return;

    if (m_scaling) {
        m_editor->active_layer()->resize(m_new_layer_size, m_new_scaled_layer_location, Gfx::Painter::ScalingMode::BilinearBlend);
        m_editor->layers_did_change();
    }

    m_scaling = false;
    m_layer_being_moved = nullptr;
    m_cached_preview_bitmap = nullptr;
    m_editor->update_tool_cursor();
    m_editor->did_complete_action(tool_name());
}

bool MoveTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Shift)
        m_keep_ascept_ratio = true;

    if (m_scaling)
        return true;

    if (event.modifiers() != 0)
        return false;

    auto* layer = m_editor->active_layer();
    if (!layer)
        return false;

    auto new_location = layer->location();

    switch (event.key()) {
    case Key_Up:
        new_location.translate_by(0, -1);
        break;
    case Key_Down:
        new_location.translate_by(0, 1);
        break;
    case Key_Left:
        new_location.translate_by(-1, 0);
        break;
    case Key_Right:
        new_location.translate_by(1, 0);
        break;
    default:
        return false;
    }

    layer->set_location(new_location);
    m_editor->layers_did_change();
    return true;
}

void MoveTool::on_keyup(GUI::KeyEvent& event)
{
    if (event.key() == Key_Shift)
        m_keep_ascept_ratio = false;
}

void MoveTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (layer != m_layer_being_moved.ptr() || !m_scaling)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    painter.translate(editor_layer_location(*layer));

    auto dst_rect = Gfx::IntRect(Gfx::IntPoint(0, 0), m_scaling ? m_new_layer_size : layer->size());
    auto rect_in_editor = m_editor->content_to_frame_rect(dst_rect).to_rounded<int>();
    painter.draw_rect(rect_in_editor, Color::Black);
    if (!m_cached_preview_bitmap.is_null() || !update_cached_preview_bitmap(layer).is_error()) {
        painter.add_clip_rect(m_editor->content_rect());
        painter.draw_scaled_bitmap(rect_in_editor, *m_cached_preview_bitmap, m_cached_preview_bitmap->rect(), 1.0f, Gfx::Painter::ScalingMode::BilinearBlend);
    }
}

ErrorOr<void> MoveTool::update_cached_preview_bitmap(Layer const* layer)
{
    auto editor_rect_size = m_editor->frame_inner_rect().size();
    auto const& source_bitmap = layer->content_bitmap();
    auto preview_bitmap_size = editor_rect_size.contains(source_bitmap.size()) ? source_bitmap.size() : editor_rect_size;

    m_cached_preview_bitmap = TRY(Gfx::Bitmap::try_create(source_bitmap.format(), preview_bitmap_size));
    GUI::Painter preview_painter(*m_cached_preview_bitmap);
    preview_painter.draw_scaled_bitmap(m_cached_preview_bitmap->rect(), source_bitmap, source_bitmap.rect(), 0.8f, Gfx::Painter::ScalingMode::BilinearBlend);
    Gfx::ContrastFilter preview_filter(0.5f);
    preview_filter.apply(*m_cached_preview_bitmap, m_cached_preview_bitmap->rect(), *m_cached_preview_bitmap, m_cached_preview_bitmap->rect());
    return {};
}

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> MoveTool::cursor()
{
    if (m_mouse_in_resize_corner || m_scaling)
        return Gfx::StandardCursor::ResizeDiagonalTLBR;
    return Gfx::StandardCursor::Move;
}

}
