/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "LayerPropertiesWidget.h"
#include "Layer.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/OpacitySlider.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font/Font.h>

REGISTER_WIDGET(PixelPaint, LayerPropertiesWidget);

namespace PixelPaint {

LayerPropertiesWidget::LayerPropertiesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    auto& group_box = add<GUI::GroupBox>("Layer properties"sv);
    group_box.set_layout<GUI::VerticalBoxLayout>(8);

    auto& name_container = group_box.add<GUI::Widget>();
    name_container.set_fixed_height(20);
    name_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& name_label = name_container.add<GUI::Label>("Name:"_string);
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    name_label.set_fixed_size(80, 20);

    m_name_textbox = name_container.add<GUI::TextBox>();
    m_name_textbox->set_fixed_height(20);
    m_name_textbox->on_change = [this] {
        if (m_layer)
            m_layer->set_name(m_name_textbox->text());
    };

    auto& opacity_container = group_box.add<GUI::Widget>();
    opacity_container.set_fixed_height(20);
    opacity_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& opacity_label = opacity_container.add<GUI::Label>("Opacity:"_string);
    opacity_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    opacity_label.set_fixed_size(80, 20);

    m_opacity_slider = opacity_container.add<GUI::HorizontalOpacitySlider>();
    m_opacity_slider->set_range(0, 100);
    m_opacity_slider->on_change = [this](int value) {
        if (m_layer)
            m_layer->set_opacity_percent(value);
    };

    m_visibility_checkbox = group_box.add<GUI::CheckBox>("Visible"_string);
    m_visibility_checkbox->set_fixed_height(20);
    m_visibility_checkbox->on_checked = [this](bool checked) {
        if (m_layer)
            m_layer->set_visible(checked);
    };
}

LayerPropertiesWidget::~LayerPropertiesWidget() = default;

void LayerPropertiesWidget::set_layer(Layer* layer)
{
    if (m_layer == layer)
        return;

    if (layer) {
        m_layer = layer;
        m_name_textbox->set_text(layer->name());
        m_opacity_slider->set_value(layer->opacity_percent());
        m_visibility_checkbox->set_checked(layer->is_visible());
        set_enabled(true);
    } else {
        m_layer = nullptr;
        set_enabled(false);
    }
}

}
