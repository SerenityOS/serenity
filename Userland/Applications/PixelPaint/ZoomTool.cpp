/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include "ZoomTool.h"
#include "ImageEditor.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Slider.h>

namespace PixelPaint {

ZoomTool::ZoomTool()
{
}

ZoomTool::~ZoomTool()
{
}

void ZoomTool::on_mousedown(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    auto scale_factor = (event.button() == GUI::MouseButton::Left) ? m_sensitivity : -m_sensitivity;
    m_editor->scale_centered_on_position(event.position(), scale_factor);
}

GUI::Widget* ZoomTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& sensitivity_container = m_properties_widget->add<GUI::Widget>();
        sensitivity_container.set_fixed_height(20);
        sensitivity_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& sensitivity_label = sensitivity_container.add<GUI::Label>("Sensitivity:");
        sensitivity_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        sensitivity_label.set_fixed_size(80, 20);

        auto& sensitivity_slider = sensitivity_container.add<GUI::HorizontalSlider>();
        sensitivity_slider.set_fixed_height(20);
        sensitivity_slider.set_range(1, 100);
        sensitivity_slider.set_value(100 * m_sensitivity);
        sensitivity_slider.on_change = [this](int value) {
            m_sensitivity = (double)value / 100.0;
        };
    }

    return m_properties_widget.ptr();
}

}
