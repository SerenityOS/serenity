/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MoveTool.h"
#include "../Image.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <AK/String.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Window.h>
#include <LibGfx/Filters/ContrastFilter.h>

namespace PixelPaint {

constexpr int resize_anchor_min_size = 5;
constexpr int resize_anchor_max_size = 20;

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
    m_new_layer_rect = m_editor->active_layer()->relative_rect();
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
            opposite_corner = rect_being_moved.bottom_right();
            break;
        case ResizeAnchorLocation::BottomRight:
            scaling_origin = rect_being_moved.bottom_right();
            opposite_corner = rect_being_moved.top_left();
            break;
        case ResizeAnchorLocation::BottomLeft:
            scaling_origin = rect_being_moved.bottom_left();
            opposite_corner = rect_being_moved.top_right();
            break;
        case ResizeAnchorLocation::TopRight:
            scaling_origin = rect_being_moved.top_right();
            opposite_corner = rect_being_moved.bottom_left();
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
        auto scaled_layer_or_error = m_editor->active_layer()->scale(m_new_layer_rect, Gfx::ScalingMode::BilinearBlend);
        if (scaled_layer_or_error.is_error())
            GUI::MessageBox::show_error(m_editor->window(), MUST(String::formatted("Failed to resize layer: {}", scaled_layer_or_error.release_error())));
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
    if (event.key() == Key_LeftShift)
        m_keep_aspect_ratio = true;

    if (event.key() == Key_LeftAlt)
        toggle_selection_mode();

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
    if (event.key() == Key_LeftShift)
        m_keep_aspect_ratio = false;

    if (event.key() == Key_LeftAlt)
        toggle_selection_mode();
}

void MoveTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    VERIFY(m_editor);
    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto content_rect = m_scaling ? m_new_layer_rect : m_editor->active_layer()->relative_rect();
    auto rect_in_editor = m_editor->content_to_frame_rect(content_rect).to_type<int>();
    if (m_scaling && (!m_cached_preview_bitmap.is_null() || !update_cached_preview_bitmap(layer).is_error())) {
        Gfx::PainterStateSaver saver(painter);
        painter.add_clip_rect(m_editor->content_rect());
        painter.draw_scaled_bitmap(rect_in_editor, *m_cached_preview_bitmap, m_cached_preview_bitmap->rect(), 1.0f, Gfx::ScalingMode::BilinearBlend);
    }
    painter.draw_rect_with_thickness(rect_in_editor, Color::Black, 3);
    painter.draw_rect_with_thickness(rect_in_editor, Color::White, 1);
    auto size = resize_anchor_size(rect_in_editor);
    if (size < resize_anchor_min_size)
        return;

    auto resize_anchors = resize_anchor_rects(rect_in_editor, size);
    for (auto const& resize_anchor_rect : resize_anchors) {
        painter.draw_rect_with_thickness(resize_anchor_rect, Color::Black, 3);
        painter.draw_rect_with_thickness(resize_anchor_rect, Color::White, 1);
    }
}

Gfx::IntRect MoveTool::resize_anchor_rect_from_position(Gfx::IntPoint position, int size)
{
    auto resize_anchor_rect_top_left = position.translated(-size / 2);
    return Gfx::IntRect(resize_anchor_rect_top_left, Gfx::IntSize(size, size));
}

int MoveTool::resize_anchor_size(Gfx::IntRect layer_rect_in_frame_coordinates)
{
    auto shortest_side = min(layer_rect_in_frame_coordinates.width(), layer_rect_in_frame_coordinates.height());
    if (shortest_side <= 1)
        return 1;
    int x = ceilf(shortest_side / 3.0f);
    return min(resize_anchor_max_size, x);
}

Array<Gfx::IntRect, 4> MoveTool::resize_anchor_rects(Gfx::IntRect layer_rect_in_frame_coordinates, int resize_anchor_size)
{
    return Array {
        resize_anchor_rect_from_position(layer_rect_in_frame_coordinates.top_left(), resize_anchor_size),
        resize_anchor_rect_from_position(layer_rect_in_frame_coordinates.top_right(), resize_anchor_size),
        resize_anchor_rect_from_position(layer_rect_in_frame_coordinates.bottom_left(), resize_anchor_size),
        resize_anchor_rect_from_position(layer_rect_in_frame_coordinates.bottom_right(), resize_anchor_size)
    };
}

ErrorOr<void> MoveTool::update_cached_preview_bitmap(Layer const* layer)
{
    auto editor_rect_size = m_editor->frame_inner_rect().size();
    auto const& source_bitmap = layer->content_bitmap();
    auto preview_bitmap_size = editor_rect_size.contains(source_bitmap.size()) ? source_bitmap.size() : editor_rect_size;

    m_cached_preview_bitmap = TRY(Gfx::Bitmap::create(source_bitmap.format(), preview_bitmap_size));
    GUI::Painter preview_painter(*m_cached_preview_bitmap);
    preview_painter.draw_scaled_bitmap(m_cached_preview_bitmap->rect(), source_bitmap, source_bitmap.rect(), 0.8f, Gfx::ScalingMode::BilinearBlend);
    Gfx::ContrastFilter preview_filter(0.5f);
    preview_filter.apply(*m_cached_preview_bitmap, m_cached_preview_bitmap->rect(), *m_cached_preview_bitmap, m_cached_preview_bitmap->rect());
    return {};
}

Optional<ResizeAnchorLocation const> MoveTool::resize_anchor_location_from_cursor_position(Layer const* layer, MouseEvent& event)
{
    auto layer_rect = m_editor->content_to_frame_rect(layer->relative_rect()).to_type<int>();
    auto size = max(resize_anchor_min_size, resize_anchor_size(layer_rect));

    auto cursor_within_resize_anchor_rect = [&event, size](Gfx::IntPoint layer_position_in_frame_coordinates) {
        auto resize_anchor_rect = resize_anchor_rect_from_position(layer_position_in_frame_coordinates, size);
        return resize_anchor_rect.contains(event.raw_event().position());
    };

    if (cursor_within_resize_anchor_rect(layer_rect.top_left()))
        return ResizeAnchorLocation::TopLeft;
    if (cursor_within_resize_anchor_rect(layer_rect.top_right()))
        return ResizeAnchorLocation::TopRight;
    if (cursor_within_resize_anchor_rect(layer_rect.bottom_left()))
        return ResizeAnchorLocation::BottomLeft;
    if (cursor_within_resize_anchor_rect(layer_rect.bottom_right()))
        return ResizeAnchorLocation::BottomRight;
    return {};
}

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> MoveTool::cursor()
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

NonnullRefPtr<GUI::Widget> MoveTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& selection_mode_container = properties_widget->add<GUI::Widget>();
        selection_mode_container.set_layout<GUI::HorizontalBoxLayout>();
        selection_mode_container.set_fixed_height(46);
        auto& selection_mode_label = selection_mode_container.add<GUI::Label>("Selection Mode:"_string);
        selection_mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        selection_mode_label.set_fixed_size(80, 40);

        auto& mode_radio_container = selection_mode_container.add<GUI::Widget>();
        mode_radio_container.set_layout<GUI::VerticalBoxLayout>();
        m_selection_mode_foreground = mode_radio_container.add<GUI::RadioButton>("Foreground"_string);

        m_selection_mode_active = mode_radio_container.add<GUI::RadioButton>("Active Layer"_string);

        m_selection_mode_foreground->on_checked = [this](bool) {
            m_layer_selection_mode = LayerSelectionMode::ForegroundLayer;
        };
        m_selection_mode_active->on_checked = [this](bool) {
            m_layer_selection_mode = LayerSelectionMode::ActiveLayer;
        };

        m_selection_mode_foreground->set_checked(true);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

void MoveTool::toggle_selection_mode()
{
    if (m_selection_mode_foreground->is_checked())
        m_selection_mode_active->set_checked(true);
    else
        m_selection_mode_foreground->set_checked(true);
}

}
