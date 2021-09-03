/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EraseTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

EraseTool::EraseTool()
{
}

EraseTool::~EraseTool()
{
}

Gfx::IntRect EraseTool::build_rect(Gfx::IntPoint const& pos, Gfx::IntRect const& widget_rect)
{
    const int eraser_size = m_thickness;
    const int eraser_radius = eraser_size / 2;
    const auto ex = pos.x();
    const auto ey = pos.y();
    return Gfx::IntRect(ex - eraser_radius, ey - eraser_radius, eraser_size, eraser_size).intersected(widget_rect);
}

void EraseTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Left && layer_event.button() != GUI::MouseButton::Right)
        return;
    Gfx::IntRect r = build_rect(layer_event.position(), layer->rect());
    GUI::Painter painter(layer->bitmap());
    painter.clear_rect(r, get_color());
    layer->did_modify_bitmap(r.inflated(2, 2));
}

void EraseTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.buttons() & GUI::MouseButton::Left || layer_event.buttons() & GUI::MouseButton::Right) {
        Gfx::IntRect r = build_rect(layer_event.position(), layer->rect());
        GUI::Painter painter(layer->bitmap());
        painter.clear_rect(r, get_color());
        layer->did_modify_bitmap(r.inflated(2, 2));
    }
}

void EraseTool::on_mouseup(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Left && layer_event.button() != GUI::MouseButton::Right)
        return;
    m_editor->did_complete_action();
}

Color EraseTool::get_color() const
{
    if (m_use_secondary_color)
        return m_editor->secondary_color();
    return Color(255, 255, 255, 0);
}

GUI::Widget* EraseTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& thickness_container = m_properties_widget->add<GUI::Widget>();
        thickness_container.set_fixed_height(20);
        thickness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& thickness_label = thickness_container.add<GUI::Label>("Size:");
        thickness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        thickness_label.set_fixed_size(80, 20);

        auto& thickness_slider = thickness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        thickness_slider.set_range(1, 50);
        thickness_slider.set_value(m_thickness);

        thickness_slider.on_change = [&](int value) {
            m_thickness = value;
        };
        set_primary_slider(&thickness_slider);

        auto& checkbox_container = m_properties_widget->add<GUI::Widget>();
        checkbox_container.set_fixed_height(20);
        checkbox_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& use_secondary_color_checkbox = checkbox_container.add<GUI::CheckBox>();
        use_secondary_color_checkbox.set_checked(m_use_secondary_color);
        use_secondary_color_checkbox.set_text("Use secondary color");
        use_secondary_color_checkbox.on_checked = [&](bool checked) {
            m_use_secondary_color = checked;
        };
    }

    return m_properties_widget.ptr();
}

}
