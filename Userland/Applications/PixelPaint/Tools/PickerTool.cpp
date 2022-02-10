/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PickerTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>

namespace PixelPaint {

void PickerTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    auto& position = event.layer_event().position();

    Color color;
    if (m_sample_all_layers) {
        color = m_editor->image().color_at(position);
    } else {
        if (!layer || !layer->rect().contains(position))
            return;
        color = layer->bitmap().get_pixel(position);
    }

    // We picked a transparent pixel, do nothing.
    if (!color.alpha())
        return;

    if (event.layer_event().button() == GUI::MouseButton::Primary)
        m_editor->set_primary_color(color);
    else if (event.layer_event().button() == GUI::MouseButton::Secondary)
        m_editor->set_secondary_color(color);
}

GUI::Widget* PickerTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& sample_checkbox = m_properties_widget->add<GUI::CheckBox>("Sample all layers");
        sample_checkbox.set_checked(m_sample_all_layers);
        sample_checkbox.on_checked = [&](bool value) {
            m_sample_all_layers = value;
        };
    }

    return m_properties_widget.ptr();
}

}
