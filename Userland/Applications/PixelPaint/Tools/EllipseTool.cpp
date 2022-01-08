/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EllipseTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

EllipseTool::EllipseTool()
{
}

EllipseTool::~EllipseTool()
{
}

void EllipseTool::draw_using(GUI::Painter& painter, Gfx::IntPoint const& start_position, Gfx::IntPoint const& end_position, int thickness)
{
    Gfx::IntRect ellipse_intersecting_rect;
    if (m_draw_mode == DrawMode::FromCenter) {
        auto delta = end_position - start_position;
        ellipse_intersecting_rect = Gfx::IntRect::from_two_points(start_position - delta, end_position);
    } else {
        ellipse_intersecting_rect = Gfx::IntRect::from_two_points(start_position, end_position);
    }

    switch (m_fill_mode) {
    case FillMode::Outline:
        painter.draw_ellipse_intersecting(ellipse_intersecting_rect, m_editor->color_for(m_drawing_button), thickness);
        break;
    case FillMode::Fill:
        painter.fill_ellipse(ellipse_intersecting_rect, m_editor->color_for(m_drawing_button));
        break;
    default:
        VERIFY_NOT_REACHED();
    }
}

void EllipseTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = layer_event.button();
    m_ellipse_start_position = layer_event.position();
    m_ellipse_end_position = layer_event.position();
    m_editor->update();
}

void EllipseTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    if (event.layer_event().button() == m_drawing_button) {
        GUI::Painter painter(layer->bitmap());
        draw_using(painter, m_ellipse_start_position, m_ellipse_end_position, m_thickness);
        m_drawing_button = GUI::MouseButton::None;
        layer->did_modify_bitmap();
        m_editor->update();
        m_editor->did_complete_action();
    }
}

void EllipseTool::on_mousemove(Layer*, MouseEvent& event)
{
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    m_draw_mode = event.layer_event().alt() ? DrawMode::FromCenter : DrawMode::FromCorner;

    if (event.layer_event().shift())
        m_ellipse_end_position = m_ellipse_start_position.end_point_for_aspect_ratio(event.layer_event().position(), 1.0);
    else if (m_aspect_ratio.has_value())
        m_ellipse_end_position = m_ellipse_start_position.end_point_for_aspect_ratio(event.layer_event().position(), m_aspect_ratio.value());
    else
        m_ellipse_end_position = event.layer_event().position();

    m_editor->update();
}

void EllipseTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!layer || m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    auto preview_start = m_editor->content_to_frame_position(m_ellipse_start_position).to_type<int>();
    auto preview_end = m_editor->content_to_frame_position(m_ellipse_end_position).to_type<int>();
    draw_using(painter, preview_start, preview_end, AK::max(m_thickness * m_editor->scale(), 1));
}

void EllipseTool::on_keydown(GUI::KeyEvent& event)
{
    Tool::on_keydown(event);
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        event.accept();
    }
}

GUI::Widget* EllipseTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& thickness_container = m_properties_widget->add<GUI::Widget>();
        thickness_container.set_fixed_height(20);
        thickness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& thickness_label = thickness_container.add<GUI::Label>("Thickness:");
        thickness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        thickness_label.set_fixed_size(80, 20);

        auto& thickness_slider = thickness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        thickness_slider.set_range(1, 10);
        thickness_slider.set_value(m_thickness);

        thickness_slider.on_change = [&](int value) {
            m_thickness = value;
        };
        set_primary_slider(&thickness_slider);

        auto& mode_container = m_properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(46);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Mode:");
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(80, 20);

        auto& mode_radio_container = mode_container.add<GUI::Widget>();
        mode_radio_container.set_layout<GUI::VerticalBoxLayout>();
        auto& outline_mode_radio = mode_radio_container.add<GUI::RadioButton>("Outline");
        auto& fill_mode_radio = mode_radio_container.add<GUI::RadioButton>("Fill");

        outline_mode_radio.on_checked = [&](bool) {
            m_fill_mode = FillMode::Outline;
        };
        fill_mode_radio.on_checked = [&](bool) {
            m_fill_mode = FillMode::Fill;
        };

        outline_mode_radio.set_checked(true);

        auto& aspect_container = m_properties_widget->add<GUI::Widget>();
        aspect_container.set_fixed_height(20);
        aspect_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& aspect_label = aspect_container.add<GUI::Label>("Aspect Ratio:");
        aspect_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        aspect_label.set_fixed_size(80, 20);

        m_aspect_w_textbox = aspect_container.add<GUI::TextBox>();
        m_aspect_w_textbox->set_fixed_height(20);
        m_aspect_w_textbox->set_fixed_width(25);
        m_aspect_w_textbox->on_change = [&] {
            auto x = m_aspect_w_textbox->text().to_int().value_or(0);
            auto y = m_aspect_h_textbox->text().to_int().value_or(0);
            if (x > 0 && y > 0) {
                m_aspect_ratio = (float)x / (float)y;
            } else {
                m_aspect_ratio = {};
            }
        };

        auto& multiply_label = aspect_container.add<GUI::Label>("x");
        multiply_label.set_text_alignment(Gfx::TextAlignment::Center);
        multiply_label.set_fixed_size(10, 20);

        m_aspect_h_textbox = aspect_container.add<GUI::TextBox>();
        m_aspect_h_textbox->set_fixed_height(20);
        m_aspect_h_textbox->set_fixed_width(25);
        m_aspect_h_textbox->on_change = [&] { m_aspect_w_textbox->on_change(); };
    }

    return m_properties_widget.ptr();
}

}
