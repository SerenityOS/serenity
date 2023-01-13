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
#include <AK/String.h>
#include <LibGUI/Action.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
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
    if (!layer->rect().contains(layer_event.position()) && !m_resize_anchor_location.has_value())
        return;
    m_scaling = m_resize_anchor_location.has_value();
    m_layer_being_moved = *layer;
    m_event_origin = image_event.position();
    m_layer_origin = layer->location();
    m_new_layer_rect = m_editor->active_layer()->rect();
}

void MoveTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (m_editor->is_panning()) {
        m_editor->pan_to(event.raw_event().position());
        return;
    }

    if (!layer)
        return;

    if (!m_scaling) {
        auto current_resize_anchor_location = resize_anchor_location_from_cursor_position(layer, event);
        if (m_resize_anchor_location != current_resize_anchor_location) {
            m_resize_anchor_location = current_resize_anchor_location;
            m_editor->update_tool_cursor();
        }
    }

    if (!m_layer_being_moved)
        return;

    auto cursor_position = event.image_event().position();
    auto delta = cursor_position - m_event_origin;
    auto rect_being_moved = m_layer_being_moved->relative_rect();
    Gfx::IntPoint scaling_origin;
    Gfx::IntPoint opposite_corner;
    if (m_scaling) {
        VERIFY(m_resize_anchor_location.has_value());
        switch (m_resize_anchor_location.value()) {
        case ResizeAnchorLocation::TopLeft:
            scaling_origin = rect_being_moved.top_left();
            opposite_corner = rect_being_moved.bottom_right().translated(1, 1);
            break;
        case ResizeAnchorLocation::BottomRight:
            scaling_origin = rect_being_moved.bottom_right().translated(1, 1);
            opposite_corner = rect_being_moved.top_left();
            break;
        case ResizeAnchorLocation::BottomLeft:
            scaling_origin = rect_being_moved.bottom_left().translated(0, 1);
            opposite_corner = rect_being_moved.top_right().translated(1, 0);
            break;
        case ResizeAnchorLocation::TopRight:
            scaling_origin = rect_being_moved.top_right().translated(1, 0);
            opposite_corner = rect_being_moved.bottom_left().translated(0, 1);
            break;
        }
        scaling_origin.translate_by(delta);
        if (m_keep_aspect_ratio) {
            auto aspect_ratio = m_layer_being_moved->size().aspect_ratio();
            scaling_origin = opposite_corner.end_point_for_aspect_ratio(scaling_origin, aspect_ratio);
        }

        auto scaled_rect = Gfx::IntRect::from_two_points(scaling_origin, opposite_corner);
        if (!scaled_rect.is_empty())
            m_new_layer_rect = scaled_rect;
    } else {
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
        auto resized_or_error = m_editor->active_layer()->resize(m_new_layer_rect, Gfx::Painter::ScalingMode::BilinearBlend);
        if (resized_or_error.is_error())
            GUI::MessageBox::show_error(m_editor->window(), MUST(String::formatted("Failed to resize layer: {}", resized_or_error.error().string_literal())));
        else
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
        m_keep_aspect_ratio = true;

    if (m_scaling)
        return true;

    if (!(event.modifiers() == Mod_None || event.modifiers() == Mod_Shift))
        return false;

    auto* layer = m_editor->active_layer();
    if (!layer)
        return false;

    auto new_location = layer->location();
    auto speed = event.shift() ? 10 : 1;
    switch (event.key()) {
    case Key_Up:
        new_location.translate_by(0, -speed);
        break;
    case Key_Down:
        new_location.translate_by(0, speed);
        break;
    case Key_Left:
        new_location.translate_by(-speed, 0);
        break;
    case Key_Right:
        new_location.translate_by(speed, 0);
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
        m_keep_aspect_ratio = false;
}

void MoveTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (layer != m_layer_being_moved.ptr() || !m_scaling)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto rect_in_editor = m_editor->content_to_frame_rect(m_new_layer_rect).to_rounded<int>();
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

Optional<ResizeAnchorLocation const> MoveTool::resize_anchor_location_from_cursor_position(Layer const* layer, MouseEvent& event)
{
    auto cursor_within_resize_anchor_rect = [&](Gfx::IntPoint layer_position) {
        constexpr int sensitivity = 20;
        auto resize_anchor_rect_center = m_editor->content_to_frame_position(layer_position).to_rounded<int>();
        auto resize_anchor_rect_top_left = resize_anchor_rect_center.translated(-sensitivity / 2);
        auto resize_anchor_rect = Gfx::IntRect(resize_anchor_rect_top_left, Gfx::IntSize(sensitivity, sensitivity));
        return resize_anchor_rect.contains(event.raw_event().position());
    };
    auto layer_rect = layer->relative_rect();
    if (cursor_within_resize_anchor_rect(layer_rect.top_left()))
        return ResizeAnchorLocation::TopLeft;
    if (cursor_within_resize_anchor_rect(layer_rect.top_right().translated(1, 0)))
        return ResizeAnchorLocation::TopRight;
    if (cursor_within_resize_anchor_rect(layer_rect.bottom_left().translated(0, 1)))
        return ResizeAnchorLocation::BottomLeft;
    if (cursor_within_resize_anchor_rect(layer_rect.bottom_right().translated(1)))
        return ResizeAnchorLocation::BottomRight;
    return {};
}

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> MoveTool::cursor()
{
    if (m_resize_anchor_location.has_value()) {
        switch (m_resize_anchor_location.value()) {
        case ResizeAnchorLocation::TopLeft:
        case ResizeAnchorLocation::BottomRight:
            return Gfx::StandardCursor::ResizeDiagonalTLBR;
        case ResizeAnchorLocation::BottomLeft:
        case ResizeAnchorLocation::TopRight:
            return Gfx::StandardCursor::ResizeDiagonalBLTR;
        }
    }
    return Gfx::StandardCursor::Move;
}

}
