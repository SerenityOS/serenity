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
#include <LibGUI/CheckBox.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/Label.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Color.h>
#include <LibGfx/Gradients.h>
#include <LibGfx/Path.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> GradientTool::cursor()
{
    if (m_hover_over_drag_handle || m_hover_over_start_handle || m_hover_over_end_handle || m_hover_over_transversal_a_handle || m_hover_over_transversal_b_handle)
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
    if (!m_hover_over_start_handle && !m_hover_over_end_handle && !m_hover_over_transversal_a_handle && !m_hover_over_transversal_b_handle) {
        if (has_gradient_data()) {
            Gfx::IntPoint movement_delta = layer_event.position() - m_gradient_center.value();
            m_gradient_center = layer_event.position();
            move_gradient_position(movement_delta);
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
    if (layer && m_editor && !m_button_pressed && has_gradient_data()) {
        auto set_hover_flag = [&](bool& flag, Optional<Gfx::IntPoint> const p) {
            auto handle_offset = m_editor->content_to_frame_position(layer->location());
            float scale = m_editor->scale();
            auto frame_postion = p.value().to_type<float>().scaled(scale, scale).translated(handle_offset).to_type<int>();
            auto handle = Gfx::IntRect::centered_on(frame_postion, { 16, 16 });
            if (flag != handle.contains(event.raw_event().position())) {
                flag = !flag;
                m_editor->update_tool_cursor();
                m_editor->update();
            }
        };

        set_hover_flag(m_hover_over_start_handle, m_gradient_start.value());
        set_hover_flag(m_hover_over_drag_handle, m_gradient_center.value());
        set_hover_flag(m_hover_over_end_handle, m_gradient_end.value());

        if (m_mode == GradientMode::Radial) {
            set_hover_flag(m_hover_over_transversal_a_handle, m_gradient_transversal_a.value());
            set_hover_flag(m_hover_over_transversal_b_handle, m_gradient_transversal_b.value());
        }
    }

    if (!layer || !m_button_pressed)
        return;

    auto& layer_event = event.layer_event();
    if (!m_hover_over_drag_handle && (m_hover_over_start_handle || m_hover_over_end_handle)) {
        auto movement_delta = m_hover_over_start_handle ? layer_event.position() - m_gradient_start.value() : layer_event.position() - m_gradient_end.value();
        rotate_gradient_points(m_hover_over_start_handle ? movement_delta.scaled({ -1, -1 }) : movement_delta);
    }

    if (!m_hover_over_drag_handle && (m_hover_over_transversal_a_handle || m_hover_over_transversal_b_handle)) {
        auto distance_to_center = layer_event.position().distance_from(m_gradient_center.value());
        auto new_left_right_distance_fraction = distance_to_center / m_gradient_center.value().distance_from(m_gradient_start.value());
        calculate_transversal_points(new_left_right_distance_fraction);
    }

    if (m_hover_over_drag_handle) {
        auto movement_delta = layer_event.position() - m_gradient_center.value();
        m_gradient_center.value().translate_by(movement_delta);
        move_gradient_position(movement_delta);
    }

    if (!(m_hover_over_drag_handle || m_hover_over_start_handle || m_hover_over_end_handle || m_hover_over_transversal_a_handle || m_hover_over_transversal_b_handle))
        update_gradient_with_initial_values(layer_event.position());

    // If Shift is pressed, align the gradient horizontally or vertically
    if (m_shift_pressed && has_gradient_data() && m_mode == GradientMode::Linear) {
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

void GradientTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!layer || !has_gradient_data())
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto gradient_clip_rect = m_editor->content_to_frame_rect(layer->relative_rect()).to_type<int>().intersected(m_editor->content_rect());
    draw_gradient(painter, true, m_editor->content_to_frame_position(layer->location()), m_editor->scale(), gradient_clip_rect);
}

void GradientTool::on_primary_color_change(Color)
{
    if (has_gradient_data())
        m_editor->update();
}

void GradientTool::on_secondary_color_change(Color)
{
    if (has_gradient_data())
        m_editor->update();
}

void GradientTool::on_tool_activation()
{
    reset();
}

NonnullRefPtr<GUI::Widget> GradientTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& mode_container = properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(20);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Gradient Type:"_string);
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(80, 20);

        static constexpr auto s_mode_names = [] {
            Array<StringView, (int)GradientMode::__Count> names;
            for (size_t i = 0; i < names.size(); i++) {
                switch ((GradientMode)i) {
                case GradientMode::Linear:
                    names[i] = "Linear"sv;
                    break;
                case GradientMode::Radial:
                    names[i] = "Radial"sv;
                    break;
                default:
                    break;
                }
            }
            return names;
        }();

        auto& mode_combobox = mode_container.add<GUI::ComboBox>();
        mode_combobox.set_only_allow_values_from_model(true);
        mode_combobox.set_model(*GUI::ItemListModel<StringView, decltype(s_mode_names)>::create(s_mode_names));
        mode_combobox.set_selected_index((int)m_mode, GUI::AllowCallback::No);

        auto& opacity_container = properties_widget->add<GUI::Widget>();
        opacity_container.set_fixed_height(20);
        opacity_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& opacity_label = opacity_container.add<GUI::Label>("Opacity:"_string);
        opacity_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        opacity_label.set_fixed_size(80, 20);

        auto& opacity_slider = opacity_container.add<GUI::HorizontalOpacitySlider>();
        opacity_slider.set_range(1, 100);
        opacity_slider.set_value(100);

        opacity_slider.on_change = [this](int value) {
            m_opacity = value;
            m_editor->update();
        };

        set_primary_slider(&opacity_slider);

        auto& hardness_container = properties_widget->add<GUI::Widget>();
        hardness_container.set_layout<GUI::HorizontalBoxLayout>();
        hardness_container.set_fixed_height(20);
        hardness_container.set_visible(m_mode == GradientMode::Radial);

        mode_combobox.on_change = [this, &hardness_container](auto&, auto& model_index) {
            VERIFY(model_index.row() >= 0);
            VERIFY(model_index.row() < (int)GradientMode::__Count);

            GradientMode selected_mode = model_index.row() == 0 ? GradientMode::Linear : GradientMode::Radial;

            if (m_mode != selected_mode) {
                m_mode = selected_mode;
                reset();
            }

            hardness_container.set_visible(m_mode == GradientMode::Radial);
        };

        auto& hardness_label = hardness_container.add<GUI::Label>("Hardness:"_string);
        hardness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label.set_fixed_size(80, 20);

        auto& hardness_slider = hardness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        hardness_slider.set_range(1, 99);
        hardness_slider.set_value(m_hardness);
        hardness_slider.on_change = [this](int value) {
            if (m_mode == GradientMode::Radial && m_editor) {
                m_hardness = value;
                m_editor->update();
            }
        };
        set_secondary_slider(&hardness_slider);

        auto& use_secondary_color_checkbox = properties_widget->add<GUI::CheckBox>("Use secondary color"_string);
        use_secondary_color_checkbox.on_checked = [this](bool checked) {
            m_use_secondary_color = checked;
            m_editor->update();
        };

        auto& button_container = properties_widget->add<GUI::Widget>();
        button_container.set_fixed_height(22);
        button_container.set_layout<GUI::HorizontalBoxLayout>();
        button_container.add_spacer();

        auto& apply_button = button_container.add<GUI::DialogButton>("Apply"_string);
        apply_button.on_click = [this](auto) {
            rasterize_gradient();
        };
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

void GradientTool::rasterize_gradient()
{
    auto layer = m_editor->active_layer();
    if (!layer || !has_gradient_data())
        return;

    GUI::Painter painter(layer->get_scratch_edited_bitmap());
    draw_gradient(painter);
    layer->did_modify_bitmap(layer->get_scratch_edited_bitmap().rect());
    m_editor->did_complete_action(tool_name());
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

void GradientTool::draw_gradient(GUI::Painter& painter, bool with_guidelines, Gfx::FloatPoint const drawing_offset, float scale, Optional<Gfx::IntRect const&> gradient_clip)
{
    auto t_gradient_begin_line = m_gradient_begin_line.scaled(scale, scale).translated(drawing_offset);
    auto t_gradient_center_line = m_gradient_center_line.scaled(scale, scale).translated(drawing_offset);
    auto t_gradient_end_line = m_gradient_end_line.scaled(scale, scale).translated(drawing_offset);
    auto t_gradient_center = m_gradient_center.value().to_type<float>().scaled(scale, scale).translated(drawing_offset).to_type<int>();

    int width = m_editor->active_layer()->rect().width() * scale;
    int height = m_editor->active_layer()->rect().height() * scale;

    float rotation_radians = atan2f(t_gradient_begin_line.a().y() - t_gradient_end_line.a().y(), t_gradient_begin_line.a().x() - t_gradient_end_line.a().x());
    float rotation_degrees = AK::to_degrees(rotation_radians);

    auto determine_required_side_length = [&](int center, int side_length) {
        if (center < 0)
            return 2 * (AK::abs(center) + side_length);
        if (center > side_length)
            return 2 * center;

        return 2 * (AK::max(center + side_length, side_length - center));
    };

    auto scaled_gradient_center = m_gradient_center.value().to_type<float>().scaled(scale, scale).to_type<int>();
    auto gradient_rect_height = determine_required_side_length(t_gradient_center.y(), height);
    auto gradient_rect_width = determine_required_side_length(t_gradient_center.x(), width);
    auto gradient_max_side_length = AK::max(gradient_rect_height, gradient_rect_width);
    auto gradient_rect = Gfx::IntRect::centered_on(t_gradient_center, { gradient_max_side_length, gradient_max_side_length });
    float overall_gradient_length_in_rect = Gfx::calculate_gradient_length(gradient_rect.size().to_type<float>(), rotation_degrees - 90);

    if (m_gradient_half_length == 0 || overall_gradient_length_in_rect == 0 || isnan(overall_gradient_length_in_rect))
        return;

    auto gradient_half_width_percentage_offset = (m_gradient_half_length * scale) / overall_gradient_length_in_rect;
    auto start_color = m_editor->color_for(GUI::MouseButton::Primary);
    start_color.set_alpha(start_color.alpha() * m_opacity / 100);

    auto end_color = [&] {
        if (m_use_secondary_color) {
            auto color = m_editor->color_for(GUI::MouseButton::Secondary);
            return color.with_alpha(color.alpha() * m_opacity / 100);
        }
        return start_color.with_alpha(0);
    }();

    {
        Gfx::PainterStateSaver saver(painter);
        if (gradient_clip.has_value())
            painter.add_clip_rect(*gradient_clip);

        switch (m_mode) {
        case GradientMode::__Count:
            break;
        case GradientMode::Linear:
            painter.fill_rect_with_linear_gradient(gradient_rect, Array { Gfx::ColorStop { start_color, 0.5f - gradient_half_width_percentage_offset }, Gfx::ColorStop { end_color, 0.5f + gradient_half_width_percentage_offset } }, rotation_degrees - 90);
            break;
        case GradientMode::Radial:

            auto t_gradient_longitudinal = m_gradient_start.value().to_type<float>().scaled(scale, scale).translated(drawing_offset).to_type<int>();
            auto t_gradient_transversal = m_gradient_transversal_a.value().to_type<float>().scaled(scale, scale).translated(drawing_offset).to_type<int>();
            auto radial_size = Gfx::IntSize((AK::abs(t_gradient_center.distance_from(t_gradient_longitudinal))), (AK::abs(t_gradient_center.distance_from(t_gradient_transversal))));

            AK::Array<Gfx::ColorStop, 3> colors = {
                Gfx::ColorStop { .color = start_color, .position = 0.0f },
                Gfx::ColorStop { .color = start_color, .position = m_hardness / 100.0f },
                Gfx::ColorStop { .color = end_color, .position = 1.0f },
            };

            painter.fill_rect_with_radial_gradient(Gfx::IntRect(drawing_offset, { width, height }), colors, scaled_gradient_center, radial_size, {}, 180 - rotation_degrees);
            break;
        }
    }

    if (with_guidelines) {
        Gfx::AntiAliasingPainter aa_painter = Gfx::AntiAliasingPainter(painter);
        Gfx::FloatLine icon_line1_rotated_offset = Gfx::FloatLine({ -2, -4 }, { -2, 4 }).rotated(rotation_radians);
        Gfx::FloatLine icon_line2_rotated_offset = Gfx::FloatLine({ 2, -4 }, { 2, 4 }).rotated(rotation_radians);
        Gfx::FloatLine icon_line3_rotated_offset = Gfx::FloatLine({ -3, -2 }, { -3, 2 }).rotated(rotation_radians);
        Gfx::FloatLine icon_line4_rotated_offset = Gfx::FloatLine({ 3, -2 }, { 3, 2 }).rotated(rotation_radians);
        Gfx::FloatLine icon_line5_rotated_offset = Gfx::FloatLine({ 0, -5 }, { 0, 5 }).rotated(rotation_radians);

        auto draw_handle = [&](Gfx::IntPoint p, bool is_hovered, IconStyle with_icon) {
            auto alpha = is_hovered ? 255 : 100;
            auto translated_p = p.to_type<float>().scaled(scale, scale).translated(drawing_offset);
            aa_painter.fill_circle(translated_p.to_type<int>(), 10, Color(Color::MidGray).with_alpha(alpha));
            aa_painter.fill_circle(translated_p.to_type<int>(), 8, Color(Color::LightGray).with_alpha(alpha));

            if (with_icon == IconStyle::ChangeWidthAndAngle) {
                aa_painter.draw_line(icon_line1_rotated_offset.translated(translated_p), Color(Color::MidGray).with_alpha(alpha), 2);
                aa_painter.draw_line(icon_line2_rotated_offset.translated(translated_p), Color(Color::MidGray).with_alpha(alpha), 2);
            }
            if (with_icon == IconStyle::RadialWidth) {
                auto make_triangle_path = [&](Gfx::FloatPoint p1, Gfx::FloatPoint p2, Gfx::FloatPoint p3) {
                    Gfx::Path triangle;
                    triangle.move_to(p1.translated(translated_p));
                    triangle.line_to(p2.translated(translated_p));
                    triangle.line_to(p3.translated(translated_p));
                    triangle.close();
                    return triangle;
                };

                aa_painter.fill_path(make_triangle_path(
                                         icon_line3_rotated_offset.a(),
                                         icon_line4_rotated_offset.a(),
                                         icon_line5_rotated_offset.a()),
                    Color(Color::MidGray).with_alpha(alpha), Gfx::WindingRule::EvenOdd);
                aa_painter.fill_path(make_triangle_path(
                                         icon_line3_rotated_offset.b(),
                                         icon_line4_rotated_offset.b(),
                                         icon_line5_rotated_offset.b()),
                    Color(Color::MidGray).with_alpha(alpha), Gfx::WindingRule::EvenOdd);
            }
        };

        if (m_mode == GradientMode::Linear) {
            aa_painter.draw_line(t_gradient_begin_line, Color::Black);
            aa_painter.draw_line(t_gradient_center_line, Color::MidGray);
            aa_painter.draw_line(t_gradient_end_line, Color::LightGray);
        } else {
            draw_handle(m_gradient_transversal_a.value(), m_hover_over_transversal_a_handle, IconStyle::RadialWidth);
            draw_handle(m_gradient_transversal_b.value(), m_hover_over_transversal_b_handle, IconStyle::RadialWidth);
        }

        draw_handle(m_gradient_start.value(), m_hover_over_start_handle, IconStyle::ChangeWidthAndAngle);
        draw_handle(m_gradient_center.value(), m_hover_over_drag_handle, IconStyle::None);
        draw_handle(m_gradient_end.value(), m_hover_over_end_handle, IconStyle::ChangeWidthAndAngle);
    }
}

void GradientTool::reset()
{
    m_gradient_start = {};
    m_gradient_center = {};
    m_gradient_end = {};
    m_gradient_transversal_a = {};
    m_gradient_transversal_b = {};
    m_gradient_half_length = 0;
    m_physical_diagonal_layer_length = 0;
    m_hover_over_drag_handle = false;
    m_hover_over_start_handle = false;
    m_hover_over_end_handle = false;
    m_hover_over_transversal_a_handle = false;
    m_hover_over_transversal_b_handle = false;

    if (m_editor) {
        m_editor->update();
        m_editor->update_tool_cursor();
    }
}

void GradientTool::update_gradient_with_initial_values(Gfx::IntPoint const new_end_point)
{
    VERIFY(m_gradient_center.has_value());
    m_gradient_end = new_end_point;
    auto deltaCenter = m_gradient_end.value() - m_gradient_center.value();
    m_gradient_start = m_gradient_center.value() - deltaCenter;

    if (m_mode == GradientMode::Radial) {
        Gfx::IntPoint perpendicularDeltaCenter = { -deltaCenter.y(), deltaCenter.x() };
        m_gradient_transversal_a = m_gradient_center.value() + perpendicularDeltaCenter;
        m_gradient_transversal_b = m_gradient_center.value() - perpendicularDeltaCenter;
    }
}

void GradientTool::move_gradient_position(Gfx::IntPoint const movement_delta)
{
    m_gradient_end.value().translate_by(movement_delta);
    m_gradient_start.value().translate_by(movement_delta);

    if (m_mode == GradientMode::Radial) {
        m_gradient_transversal_a.value().translate_by(movement_delta);
        m_gradient_transversal_b.value().translate_by(movement_delta);
    }
}

void GradientTool::rotate_gradient_points(Gfx::IntPoint const delta)
{
    m_gradient_end.value().translate_by(delta);
    auto translation_distance_to_center = m_gradient_center.value().distance_from(m_gradient_end.value()) - m_gradient_center.value().distance_from(m_gradient_start.value());
    m_gradient_start.value().translate_by(delta.scaled(-1, -1));

    if (m_mode == GradientMode::Radial) {
        auto new_horizontal_distance_fraction = (translation_distance_to_center + m_gradient_center.value().distance_from(m_gradient_transversal_a.value())) / m_gradient_center.value().distance_from(m_gradient_start.value());
        calculate_transversal_points(new_horizontal_distance_fraction);
    }
}

void GradientTool::calculate_transversal_points(float scale_fraction)
{
    m_gradient_transversal_a = Gfx::IntPoint(m_gradient_center.value().x() + (scale_fraction * (m_gradient_center.value().x() - m_perpendicular_point.x())),
        m_gradient_center.value().y() + (scale_fraction * (m_gradient_center.value().y() - m_perpendicular_point.y())));
    m_gradient_transversal_b = Gfx::IntPoint(m_gradient_center.value().x() + (-scale_fraction * (m_gradient_center.value().x() - m_perpendicular_point.x())),
        m_gradient_center.value().y() + (-scale_fraction * (m_gradient_center.value().y() - m_perpendicular_point.y())));
}

}
