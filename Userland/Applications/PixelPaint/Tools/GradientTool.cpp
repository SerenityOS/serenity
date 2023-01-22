/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GradientTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"

#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/Painter.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Color.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Path.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> GradientTool::cursor()
{
    if (m_hover_over_drag_handle || m_hover_over_start_handle || m_hover_over_end_handle)
        return Gfx::StandardCursor::Hand;

    if (m_button_pressed)
        return Gfx::StandardCursor::Move;

    return Gfx::StandardCursor::Crosshair;
}

void GradientTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    m_button_pressed = true;
    if (!m_hover_over_start_handle && !m_hover_over_end_handle) {
        if (has_gradient_start_end()) {
            Gfx::IntPoint movement_delta = layer_event.position() - m_gradient_center.value();
            m_gradient_center = layer_event.position();
            translate_gradient_start_end(movement_delta, false);
            calculate_gradient_lines();
        } else {
            m_gradient_center = layer_event.position();
        }
    }

    m_physical_diagonal_layer_length = Gfx::IntPoint(0, 0).distance_from({ layer->rect().width(), layer->rect().height() });

    m_editor->update_tool_cursor();
}

void GradientTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    // Check if user is hovering over a handle
    if (layer && m_editor && !m_button_pressed && has_gradient_start_end()) {
        auto set_hover_flag = [&](bool& flag, const Gfx::IntPoint p) {
            auto frame_postion = m_editor->content_to_frame_position(p).to_type<int>();
            auto handle = Gfx::IntRect::centered_at(frame_postion, { 16, 16 });
            if (flag != handle.contains(event.raw_event().position())) {
                flag = !flag;
                m_editor->update_tool_cursor();
                m_editor->update();
            }
        };

        set_hover_flag(m_hover_over_start_handle, m_gradient_start.value());
        set_hover_flag(m_hover_over_drag_handle, m_gradient_center.value());
        set_hover_flag(m_hover_over_end_handle, m_gradient_end.value());
    }

    if (!layer || !m_button_pressed)
        return;

    auto& layer_event = event.layer_event();
    if (!m_hover_over_drag_handle && (m_hover_over_start_handle || m_hover_over_end_handle)) {
        auto movement_delta = m_hover_over_start_handle ? layer_event.position() - m_gradient_start.value() : layer_event.position() - m_gradient_end.value();
        translate_gradient_start_end(m_hover_over_start_handle ? movement_delta.scaled({ -1, -1 }) : movement_delta);
    }

    if (m_hover_over_drag_handle) {
        auto movement_delta = layer_event.position() - m_gradient_center.value();
        m_gradient_center.value().translate_by(movement_delta);
        translate_gradient_start_end(movement_delta, false);
    }

    if (!(m_hover_over_drag_handle || m_hover_over_start_handle || m_hover_over_end_handle))
        update_gradient_end_and_derive_start(layer_event.position());

    // If Shift is pressed, align the gradient horizontally or vertically
    if (m_shift_pressed && has_gradient_start_end()) {
        auto delta = m_gradient_center.value() - m_gradient_end.value();
        if (AK::abs(delta.x()) < AK::abs(delta.y())) {
            m_gradient_start.value().set_x(m_gradient_center.value().x());
            m_gradient_end.value().set_x(m_gradient_center.value().x());
        } else {
            m_gradient_start.value().set_y(m_gradient_center.value().y());
            m_gradient_end.value().set_y(m_gradient_center.value().y());
        }
    }

    calculate_gradient_lines();
}

void GradientTool::on_mouseup(Layer*, MouseEvent& event)
{
    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    m_button_pressed = false;
    m_editor->update_tool_cursor();
}

bool GradientTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_LeftShift || event.key() == Key_RightShift) {
        m_shift_pressed = true;
        if (m_button_pressed)
            m_editor->update();
        return true;
    }

    if (event.key() == Key_Return) {
        rasterize_gradient();
        return true;
    }

    if (event.key() == Key_Escape) {
        reset();
        return true;
    }

    return Tool::on_keydown(event);
}

void GradientTool::on_keyup(GUI::KeyEvent& event)
{
    Tool::on_keydown(event);
    if (event.key() == Key_Shift) {
        m_shift_pressed = false;
        event.accept();
    }
}

void GradientTool::on_second_paint(Layer const* layer, GUI::PaintEvent&)
{
    if (!layer || !has_gradient_start_end())
        return;

    // FIXME: Clipping still does overwrite the ruler if we are zoomed in.
    auto clipping_rect = m_editor->rect().contains(m_editor->content_rect()) ? m_editor->content_rect() : m_editor->rect();
    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(clipping_rect);
    draw_gradient(painter, true, m_editor->content_to_frame_position(Gfx::IntPoint(0, 0)), m_editor->scale());
}

void GradientTool::on_tool_activation()
{
    m_editor->on_primary_color_change = [this](Color) {
        if (m_gradient_end.has_value())
            m_editor->update();
    };

    reset();
}

GUI::Widget* GradientTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& size_container = m_properties_widget->add<GUI::Widget>();
        size_container.set_fixed_height(20);
        size_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& size_label = size_container.add<GUI::Label>("Opacity:");
        size_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label.set_fixed_size(80, 20);

        auto& opacity_slider = size_container.add<GUI::HorizontalOpacitySlider>();
        opacity_slider.set_range(1, 100);
        opacity_slider.set_value(100);

        opacity_slider.on_change = [&](int value) {
            m_opacity = value;
            m_editor->update();
        };

        set_primary_slider(&opacity_slider);

        auto& button_container = m_properties_widget->add<GUI::Widget>();
        button_container.set_fixed_height(22);
        auto& button_container_layout = button_container.set_layout<GUI::HorizontalBoxLayout>();
        button_container_layout.add_spacer();

        auto& apply_button = button_container.add<GUI::DialogButton>("Apply");
        apply_button.on_click = [this](auto) {
            rasterize_gradient();
        };
    }

    return m_properties_widget.ptr();
}

void GradientTool::rasterize_gradient()
{
    if (!has_gradient_start_end())
        return;

    GUI::Painter painter(m_editor->active_layer()->get_scratch_edited_bitmap());
    draw_gradient(painter);

    m_editor->did_complete_action("Gradient Tool"sv);

    reset();
}

void GradientTool::calculate_gradient_lines()
{
    m_gradient_half_length = m_gradient_end.value().distance_from(m_gradient_center.value());

    // Create a perpendicular point between the center and end point.
    m_perpendicular_point = m_gradient_end.value();
    m_perpendicular_point -= m_gradient_center.value();
    m_perpendicular_point = { -m_perpendicular_point.y(), m_perpendicular_point.x() };
    m_perpendicular_point += m_gradient_center.value();

    auto to_edge_scale_direction = (m_physical_diagonal_layer_length * 2) / m_gradient_center.value().distance_from(m_perpendicular_point);

    m_gradient_center_line.set_a({ m_gradient_center.value().x() + (to_edge_scale_direction * (m_gradient_center.value().x() - m_perpendicular_point.x())), m_gradient_center.value().y() + (to_edge_scale_direction * (m_gradient_center.value().y() - m_perpendicular_point.y())) });
    m_gradient_center_line.set_b({ m_gradient_center.value().x() + (-to_edge_scale_direction * (m_gradient_center.value().x() - m_perpendicular_point.x())), m_gradient_center.value().y() + (-to_edge_scale_direction * (m_gradient_center.value().y() - m_perpendicular_point.y())) });

    m_gradient_begin_line.set_a(m_gradient_center_line.a().translated(static_cast<Gfx::FloatPoint>(m_gradient_end.value() - m_gradient_center.value())));
    m_gradient_begin_line.set_b(m_gradient_center_line.b().translated(static_cast<Gfx::FloatPoint>(m_gradient_end.value() - m_gradient_center.value())));

    m_gradient_end_line.set_a(m_gradient_center_line.a().translated(static_cast<Gfx::FloatPoint>(m_gradient_center.value() - m_gradient_end.value())));
    m_gradient_end_line.set_b(m_gradient_center_line.b().translated(static_cast<Gfx::FloatPoint>(m_gradient_center.value() - m_gradient_end.value())));
    m_editor->update();
}

void GradientTool::draw_gradient(GUI::Painter& painter, bool with_guidelines, const Gfx::FloatPoint drawing_offset, float scale)
{
    auto t_gradient_begin_line = m_gradient_begin_line.scaled(scale, scale).translated(drawing_offset);
    auto t_gradient_center_line = m_gradient_center_line.scaled(scale, scale).translated(drawing_offset);
    auto t_gradient_end_line = m_gradient_end_line.scaled(scale, scale).translated(drawing_offset);
    auto t_gradient_center = m_gradient_center.value().to_type<float>().scaled(scale, scale).translated(drawing_offset).to_type<int>();
    int width = m_editor->active_layer()->rect().width() * scale;
    int height = m_editor->active_layer()->rect().height() * scale;

    float rotation_radians = atan2f(t_gradient_begin_line.a().y() - t_gradient_end_line.a().y(), t_gradient_begin_line.a().x() - t_gradient_end_line.a().x());
    float rotation_degrees = ((rotation_radians * 180) / static_cast<float>(M_PI)) - 90;

    auto determine_required_side_length = [&](int center, int side_length) {
        if (center < 0)
            return 2 * (AK::abs(center) + side_length);
        if (center > side_length)
            return 2 * center;

        return 2 * (AK::max(center, side_length - center));
    };

    auto scaled_gradient_center = m_gradient_center.value().to_type<float>().scaled(scale, scale).to_type<int>();
    auto gradient_rect_height = determine_required_side_length(scaled_gradient_center.y(), height);
    auto gradient_rect_width = determine_required_side_length(scaled_gradient_center.x(), width);
    auto gradient_max_side_length = AK::max(gradient_rect_height, gradient_rect_width);
    auto gradient_rect = Gfx::IntRect::centered_at(t_gradient_center, { gradient_max_side_length, gradient_max_side_length });
    float overall_gradient_length_in_rect = Gfx::calculate_gradient_length(gradient_rect.size(), rotation_degrees);

    if (m_gradient_half_length == 0 || overall_gradient_length_in_rect == 0 || isnan(overall_gradient_length_in_rect))
        return;

    auto gradient_half_width_percentage_offset = (m_gradient_half_length * scale) / overall_gradient_length_in_rect;
    auto color_to_use = m_editor->color_for(GUI::MouseButton::Primary);
    int base_opacity = color_to_use.alpha() * m_opacity / 100;
    color_to_use.set_alpha(base_opacity);
    auto gradient_start_color = color_to_use;
    gradient_start_color.set_alpha(0);

    painter.fill_rect_with_linear_gradient(gradient_rect, Array { Gfx::ColorStop { gradient_start_color, 0.5f - gradient_half_width_percentage_offset }, Gfx::ColorStop { color_to_use, 0.5f + gradient_half_width_percentage_offset } }, rotation_degrees);

    if (with_guidelines) {
        Gfx::AntiAliasingPainter aa_painter = Gfx::AntiAliasingPainter(painter);
        aa_painter.draw_line(t_gradient_begin_line, Color::LightGray);
        aa_painter.draw_line(t_gradient_center_line, Color::MidGray);
        aa_painter.draw_line(t_gradient_end_line, Color::Black);

        Gfx::FloatLine icon_line1_rotated_offset = Gfx::FloatLine({ -2, -4 }, { -2, 4 }).rotated(rotation_radians);
        Gfx::FloatLine icon_line2_rotated_offset = Gfx::FloatLine({ 2, -4 }, { 2, 4 }).rotated(rotation_radians);

        auto draw_handle = [&](Gfx::IntPoint p, bool is_hovered, bool with_icon) {
            auto alpha = is_hovered ? 255 : 100;
            auto translated_p = p.to_type<float>().scaled(scale, scale).translated(drawing_offset);
            aa_painter.fill_circle(translated_p.to_type<int>(), 10, Color(Color::MidGray).with_alpha(alpha));
            aa_painter.fill_circle(translated_p.to_type<int>(), 8, Color(Color::LightGray).with_alpha(alpha));

            if (with_icon) {
                aa_painter.draw_line(icon_line1_rotated_offset.translated(translated_p), Color(Color::MidGray).with_alpha(alpha), 2);
                aa_painter.draw_line(icon_line2_rotated_offset.translated(translated_p), Color(Color::MidGray).with_alpha(alpha), 2);
            }
        };
        draw_handle(m_gradient_start.value(), m_hover_over_start_handle, true);
        draw_handle(m_gradient_center.value(), m_hover_over_drag_handle, false);
        draw_handle(m_gradient_end.value(), m_hover_over_end_handle, true);
    }
}

void GradientTool::reset()
{
    m_gradient_start = {};
    m_gradient_center = {};
    m_gradient_end = {};
    m_gradient_half_length = 0;
    m_physical_diagonal_layer_length = 0;
    m_hover_over_drag_handle = false;
    m_hover_over_start_handle = false;
    m_hover_over_end_handle = false;

    if (m_editor) {
        m_editor->update();
        m_editor->update_tool_cursor();
    }
}

void GradientTool::update_gradient_end_and_derive_start(Gfx::IntPoint const new_end_point)
{
    VERIFY(m_gradient_center.has_value());
    m_gradient_end = new_end_point;
    m_gradient_start = m_gradient_center.value() - (m_gradient_end.value() - m_gradient_center.value());
}

void GradientTool::translate_gradient_start_end(Gfx::IntPoint const delta, bool update_start_counterwise)
{
    m_gradient_end.value().translate_by(delta);
    if (update_start_counterwise)
        m_gradient_start.value().translate_by(delta.scaled(-1, -1));
    else
        m_gradient_start.value().translate_by(delta);
}

}
