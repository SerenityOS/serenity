/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LineTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <AK/Math.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/AntiAliasingPainter.h>

namespace PixelPaint {

void LineTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    if (m_drawing_button != GUI::MouseButton::None)
        return;

    m_drawing_button = layer_event.button();

    m_drag_start_position = layer_event.position();
    m_line_start_position = layer_event.position();
    m_line_end_position = layer_event.position();

    m_editor->update();
}

void LineTool::draw_using(GUI::Painter& painter, Gfx::IntPoint start_position, Gfx::IntPoint end_position, Color color, int thickness)
{
    if (m_antialias_enabled) {
        Gfx::AntiAliasingPainter aa_painter { painter };
        auto as_float_point = [](auto const& point) {
            return Gfx::FloatPoint { point.x(), point.y() };
        };
        aa_painter.draw_line(as_float_point(start_position), as_float_point(end_position), color, thickness);
    } else {
        painter.draw_line(start_position, end_position, color, thickness);
    }
}

void LineTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() == m_drawing_button) {
        GUI::Painter painter(layer->get_scratch_edited_bitmap());
        draw_using(painter, m_line_start_position, m_line_end_position, m_editor->color_for(m_drawing_button), m_thickness);
        m_drawing_button = GUI::MouseButton::None;
        auto modified_rect = Gfx::IntRect::from_two_points(m_line_start_position, m_line_end_position).inflated(m_thickness * 2, m_thickness * 2);
        layer->did_modify_bitmap(modified_rect);
        m_editor->update();
        m_editor->did_complete_action(tool_name());
    }
}

void LineTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (m_drawing_button == GUI::MouseButton::None)
        return;

    if (layer_event.shift())
        m_line_end_position = constrain_line_angle(m_drag_start_position, layer_event.position());
    else
        m_line_end_position = layer_event.position();

    if (layer_event.alt()) {
        m_line_start_position = m_drag_start_position + (m_drag_start_position - m_line_end_position);
    } else {
        m_line_start_position = m_drag_start_position;
    }

    m_editor->update();
}

void LineTool::on_second_paint(Layer const* layer, GUI::PaintEvent& event)
{
    if (!layer || m_drawing_button == GUI::MouseButton::None)
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());
    painter.translate(editor_layer_location(*layer));
    auto preview_start = editor_stroke_position(m_line_start_position, m_thickness);
    auto preview_end = editor_stroke_position(m_line_end_position, m_thickness);
    draw_using(painter, preview_start, preview_end, m_editor->color_for(m_drawing_button), AK::max(m_thickness * m_editor->scale(), 1));
}

bool LineTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == Key_Escape && m_drawing_button != GUI::MouseButton::None) {
        m_drawing_button = GUI::MouseButton::None;
        m_editor->update();
        return true;
    }
    return Tool::on_keydown(event);
}

NonnullRefPtr<GUI::Widget> LineTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& thickness_container = properties_widget->add<GUI::Widget>();
        thickness_container.set_fixed_height(20);
        thickness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& thickness_label = thickness_container.add<GUI::Label>("Thickness:"_string);
        thickness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        thickness_label.set_fixed_size(80, 20);

        auto& thickness_slider = thickness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px"_string);
        thickness_slider.set_range(1, 10);
        thickness_slider.set_value(m_thickness);

        thickness_slider.on_change = [this](int value) {
            m_thickness = value;
        };
        set_primary_slider(&thickness_slider);

        auto& mode_container = properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(20);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& mode_label = mode_container.add<GUI::Label>("Mode:"_string);
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(80, 20);

        auto& aa_enable_checkbox = mode_container.add<GUI::CheckBox>("Anti-alias"_string);
        aa_enable_checkbox.on_checked = [this](bool checked) {
            m_antialias_enabled = checked;
        };
        aa_enable_checkbox.set_checked(true);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

}
