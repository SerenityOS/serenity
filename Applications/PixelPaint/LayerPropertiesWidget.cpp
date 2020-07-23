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
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Font.h>

namespace PixelPaint {

LayerPropertiesWidget::LayerPropertiesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();
    auto& label = add<GUI::Label>("Layer properties");
    label.set_font(Gfx::Font::default_bold_font());

    m_opacity_slider = add<GUI::HorizontalSlider>();
    m_opacity_slider->set_range(0, 100);
    m_opacity_slider->on_value_changed = [this](int value) {
        if (m_layer)
            m_layer->set_opacity_percent(value);
    };

    m_visibility_checkbox = add<GUI::CheckBox>("Visible");
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
        m_opacity_slider->set_value(layer->opacity_percent());
        m_visibility_checkbox->set_checked(layer->is_visible());
        set_enabled(true);
    } else {
        m_layer = nullptr;
        set_enabled(false);
    }
}

}
