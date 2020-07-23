/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "LayerPropertiesWidget.h"
#include "Layer.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/GroupBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGUI/TextBox.h>
#include <LibGfx/Font.h>

namespace PixelPaint {

LayerPropertiesWidget::LayerPropertiesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    auto& group_box = add<GUI::GroupBox>("Layer properties");
    auto& layout = group_box.set_layout<GUI::VerticalBoxLayout>();

    layout.set_margins({ 10, 20, 10, 10 });

    auto& name_container = group_box.add<GUI::Widget>();
    name_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    name_container.set_preferred_size(0, 20);
    name_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& name_label = name_container.add<GUI::Label>("Name:");
    name_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    name_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    name_label.set_preferred_size(80, 20);

    m_name_textbox = name_container.add<GUI::TextBox>();
    m_name_textbox->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_name_textbox->set_preferred_size(0, 20);
    m_name_textbox->on_change = [this] {
        if (m_layer)
            m_layer->set_name(m_name_textbox->text());
    };

    auto& opacity_container = group_box.add<GUI::Widget>();
    opacity_container.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    opacity_container.set_preferred_size(0, 20);
    opacity_container.set_layout<GUI::HorizontalBoxLayout>();

    auto& opacity_label = opacity_container.add<GUI::Label>("Opacity:");
    opacity_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
    opacity_label.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fixed);
    opacity_label.set_preferred_size(80, 20);

    m_opacity_slider = opacity_container.add<GUI::HorizontalSlider>();
    m_opacity_slider->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_opacity_slider->set_preferred_size(0, 20);
    m_opacity_slider->set_range(0, 100);
    m_opacity_slider->on_value_changed = [this](int value) {
        if (m_layer)
            m_layer->set_opacity_percent(value);
    };

    m_visibility_checkbox = group_box.add<GUI::CheckBox>("Visible");
    m_visibility_checkbox->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_visibility_checkbox->set_preferred_size(0, 20);
    m_visibility_checkbox->on_checked = [this](bool checked) {
        if (m_layer)
            m_layer->set_visible(checked);
    };
}

LayerPropertiesWidget::~LayerPropertiesWidget()
{
}

void LayerPropertiesWidget::set_layer(Layer* layer)
{
    if (m_layer == layer)
        return;

    if (layer) {
        m_layer = layer->make_weak_ptr();
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
