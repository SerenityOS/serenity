/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RectangleTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/AntiAliasingPainter.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

void RectangleTool::draw_using(GUI::Painter& painter, Gfx::IntPoint start_position, Gfx::IntPoint end_position, int thickness, int corner_radius)
{
    Gfx::IntRect rect;
    if (m_draw_mode == DrawMode::FromCenter) {
        auto delta = end_position - start_position;
        rect = Gfx::IntRect::from_two_points(start_position - delta, end_position);
    } else {
        rect = Gfx::IntRect::from_two_points(start_position, end_position);
    }

    switch (m_fill_mode) {
    case FillMode::Fill:
        painter.fill_rect(rect, m_editor->color_for(m_drawing_button));
        break;
    case FillMode::Outline:
        painter.draw_rect_with_thickness(rect, m_editor->color_for(m_drawing_button), thickness);
        break;
    case FillMode::Gradient:
        painter.fill_rect_with_gradient(rect, m_editor->primary_color(), m_editor->secondary_color());
        break;
    case FillMode::RoundedCorners: {
        int min_dimension = corner_radius * 2;
        if (rect.width() < min_dimension)
            rect.set_width(min_dimension);
        if (rect.height() < min_dimension)
            rect.set_height(min_dimension);
        if (m_antialias_enabled) {
            Gfx::AntiAliasingPainter aa_painter { painter };
            aa_painter.fill_rect_with_rounded_corners(rect, m_editor->color_for(m_drawing_button), corner_radius);
        } else {
            painter.fill_rect_with_rounded_corners(rect, m_editor->color_for(m_drawing_button), corner_radius);
        }
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
}

void RectangleTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = layer_event.button();
    m_rectangle_start_position = layer_event.position();
    m_rectangle_end_position = layer_event.position();
    m_editor->update();
}

void RectangleTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    if (event.layer_event().button() == m_drawing_button) {
        GUI::Painter painter(layer->get_scratch_edited_bitmap());
        draw_using(painter, m_rectangle_start_position, m_rectangle_end_position, m_thickness, m_corner_radius);
        m_drawing_button = GUI::MouseButton::None;
        auto modified_rect = Gfx::IntRect::from_two_points(m_rectangle_start_position, m_rectangle_end_position).inflated(m_thickness * 2, m_thickness * 2);
        layer->did_modify_bitmap(modified_rect);
        m_editor->update();
        m_editor->did_complete_action(tool_name());
    }
}

void RectangleTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    if (m_drawing_button == GUI::MouseButton::None)
        return;

    m_draw_mode = event.layer_event().alt() ? DrawMode::FromCenter : DrawMode::FromCorner;

    if (event.layer_event().shift())
        m_rectangle_end_position = m_rectangle_start_position.end_point_for_aspect_ratio(event.layer_event().position(), 1.0);
    else if (m_aspect_ratio.has_value())
        m_rectangle_end_position = m_rectangle_start_position.end_point_for_aspect_ratio(event.layer_event().position(), m_aspect_ratio.value());
    else
        m_rectangle_end_position = event.layer_event().position();

    m_editor->update();
}

void RectangleTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!layer || m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    painter.translate(editor_layer_location(*layer));
    auto start_position = editor_stroke_position(m_rectangle_start_position, m_thickness);
    auto end_position = editor_stroke_position(m_rectangle_end_position, m_thickness);
    draw_using(painter, start_position, end_position, AK::max(m_thickness * m_editor->scale(), 1), m_corner_radius * m_editor->scale());
}

bool RectangleTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        return true;
    }
    return Tool::on_keydown(event);
}

NonnullRefPtr<GUI::Widget> RectangleTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& thickness_or_radius_container = properties_widget->add<GUI::Widget>();
        thickness_or_radius_container.set_fixed_height(20);
        thickness_or_radius_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& thickness_or_radius_label = thickness_or_radius_container.add<GUI::Label>();
        thickness_or_radius_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        thickness_or_radius_label.set_fixed_size(80, 20);

        auto& thickness_or_radius_slider = thickness_or_radius_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px"_string);

        thickness_or_radius_slider.on_change = [&](int value) {
            if (m_fill_mode == FillMode::RoundedCorners) {
                m_corner_radius = value;
            } else
                m_thickness = value;
        };

        auto update_slider = [this, &thickness_or_radius_label, &thickness_or_radius_slider] {
            auto update_values = [&](auto label, int value, int range_min, int range_max = 10) {
                thickness_or_radius_label.set_text(String::from_utf8(label).release_value_but_fixme_should_propagate_errors());
                thickness_or_radius_slider.set_range(range_min, range_max);
                thickness_or_radius_slider.set_value(value);
            };
            if (m_fill_mode == FillMode::RoundedCorners)
                update_values("Radius:"sv, m_corner_radius, 0, 50);
            else
                update_values("Thickness:"sv, m_thickness, 1);
        };

        update_slider();
        set_primary_slider(&thickness_or_radius_slider);

        auto& mode_container = properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(90);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Mode:"_string);
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(30, 20);

        auto& mode_radio_container = mode_container.add<GUI::Widget>();
        mode_radio_container.set_layout<GUI::VerticalBoxLayout>();
        auto& outline_mode_radio = mode_radio_container.add<GUI::RadioButton>("Outline"_string);
        auto& fill_mode_radio = mode_radio_container.add<GUI::RadioButton>("Fill"_string);
        auto& gradient_mode_radio = mode_radio_container.add<GUI::RadioButton>("Gradient"_string);
        mode_radio_container.set_fixed_width(70);

        auto& rounded_corners_mode_radio = mode_radio_container.add<GUI::RadioButton>("Rounded"_string);

        outline_mode_radio.on_checked = [this, update_slider](bool) {
            m_fill_mode = FillMode::Outline;
            update_slider();
        };
        fill_mode_radio.on_checked = [this, update_slider](bool) {
            m_fill_mode = FillMode::Fill;
            update_slider();
        };
        gradient_mode_radio.on_checked = [this, update_slider](bool) {
            m_fill_mode = FillMode::Gradient;
            update_slider();
        };
        rounded_corners_mode_radio.on_checked = [this, update_slider](bool) {
            m_fill_mode = FillMode::RoundedCorners;
            update_slider();
        };
        outline_mode_radio.set_checked(true);

        auto& mode_extras_container = mode_container.add<GUI::Widget>();
        mode_extras_container.set_layout<GUI::VerticalBoxLayout>();

        auto& aa_enable_checkbox = mode_extras_container.add<GUI::CheckBox>("Anti-alias"_string);
        aa_enable_checkbox.on_checked = [this](bool checked) {
            m_antialias_enabled = checked;
        };
        aa_enable_checkbox.set_checked(true);

        auto& aspect_container = mode_extras_container.add<GUI::Widget>();
        aspect_container.set_layout<GUI::VerticalBoxLayout>();
        aspect_container.set_fixed_width(75);

        auto& aspect_label = aspect_container.add<GUI::Label>("Aspect Ratio:"_string);
        aspect_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        aspect_label.set_fixed_size(75, 20);

        auto& aspect_fields_container = aspect_container.add<GUI::Widget>();
        aspect_fields_container.set_fixed_width(75);
        aspect_fields_container.set_layout<GUI::HorizontalBoxLayout>();

        m_aspect_w_textbox = aspect_fields_container.add<GUI::TextBox>();
        m_aspect_w_textbox->set_fixed_height(20);
        m_aspect_w_textbox->set_fixed_width(25);
        m_aspect_w_textbox->on_change = [this] {
            auto x = m_aspect_w_textbox->text().to_number<int>().value_or(0);
            auto y = m_aspect_h_textbox->text().to_number<int>().value_or(0);
            if (x > 0 && y > 0) {
                m_aspect_ratio = (float)x / (float)y;
            } else {
                m_aspect_ratio = {};
            }
        };

        auto& multiply_label = aspect_fields_container.add<GUI::Label>("x"_string);
        multiply_label.set_text_alignment(Gfx::TextAlignment::Center);
        multiply_label.set_fixed_size(10, 20);

        m_aspect_h_textbox = aspect_fields_container.add<GUI::TextBox>();
        m_aspect_h_textbox->set_fixed_height(20);
        m_aspect_h_textbox->set_fixed_width(25);
        m_aspect_h_textbox->on_change = [this] { m_aspect_w_textbox->on_change(); };

        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

}
