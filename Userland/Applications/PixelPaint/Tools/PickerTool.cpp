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
    if (!layer)
        return;
    auto layer_event = event.layer_event();
    m_editor->set_editor_color_to_color_at_mouse_position(layer_event, m_sample_all_layers);
}

void PickerTool::on_mouseup(Layer*, MouseEvent& event)
{
    auto layer_event = event.layer_event();
    if (layer_event.buttons() & GUI::MouseButton::Primary || layer_event.buttons() & GUI::MouseButton::Secondary)
        return;
    m_editor->set_appended_status_info(DeprecatedString::empty());
}

void PickerTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;
    auto layer_event = event.layer_event();
    if (!(layer_event.buttons() & GUI::MouseButton::Primary || layer_event.buttons() & GUI::MouseButton::Secondary))
        return;
    m_editor->set_editor_color_to_color_at_mouse_position(layer_event, m_sample_all_layers);
}

ErrorOr<GUI::Widget*> PickerTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = TRY(GUI::Widget::try_create());
        (void)TRY(properties_widget->try_set_layout<GUI::VerticalBoxLayout>());

        auto sample_checkbox = TRY(properties_widget->try_add<GUI::CheckBox>(TRY(String::from_utf8("Sample all layers"sv))));
        sample_checkbox->set_checked(m_sample_all_layers);
        sample_checkbox->on_checked = [this](bool value) {
            m_sample_all_layers = value;
        };
        m_properties_widget = properties_widget;
    }

    return m_properties_widget.ptr();
}

}
