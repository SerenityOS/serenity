/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "SprayTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <AK/Queue.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Bitmap.h>
#include <math.h>
#include <stdio.h>

namespace PixelPaint {

SprayTool::SprayTool()
{
    m_timer = Core::Timer::construct();
    m_timer->on_timeout = [&]() {
        paint_it();
    };
    m_timer->set_interval(200);
}

SprayTool::~SprayTool()
{
}

static double nrand()
{
    return double(rand()) / double(RAND_MAX);
}

void SprayTool::paint_it()
{
    auto* layer = m_editor->active_layer();
    if (!layer)
        return;

    auto& bitmap = layer->bitmap();
    GUI::Painter painter(bitmap);
    VERIFY(bitmap.bpp() == 32);
    m_editor->update();
    const double minimal_radius = 2;
    const double base_radius = minimal_radius * m_thickness;
    for (int i = 0; i < M_PI * base_radius * base_radius * (m_density / 100.0f); i++) {
        double radius = base_radius * nrand();
        double angle = 2 * M_PI * nrand();
        const int xpos = m_last_pos.x() + radius * cos(angle);
        const int ypos = m_last_pos.y() - radius * sin(angle);
        if (xpos < 0 || xpos >= bitmap.width())
            continue;
        if (ypos < 0 || ypos >= bitmap.height())
            continue;
        bitmap.set_pixel<Gfx::StorageFormat::BGRA8888>(xpos, ypos, m_color);
    }

    layer->did_modify_bitmap(*m_editor->image());
}

void SprayTool::on_mousedown(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    m_color = m_editor->color_for(event);
    m_last_pos = event.position();
    m_timer->start();
    paint_it();
}

void SprayTool::on_mousemove(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    m_last_pos = event.position();
    if (m_timer->is_active()) {
        paint_it();
        m_timer->restart(m_timer->interval());
    }
}

void SprayTool::on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent&)
{
    if (m_timer->is_active()) {
        m_timer->stop();
        m_editor->did_complete_action();
    }
}

void SprayTool::on_tool_button_contextmenu(GUI::ContextMenuEvent& event)
{
    if (!m_context_menu) {
        m_context_menu = GUI::Menu::construct();
        m_thickness_actions.set_exclusive(true);
        auto insert_action = [&](int size, bool checked = false) {
            auto action = GUI::Action::create_checkable(String::number(size), [this, size](auto&) {
                m_thickness = size;
            });
            action->set_checked(checked);
            m_thickness_actions.add_action(*action);
            m_context_menu->add_action(move(action));
        };
        insert_action(1, true);
        insert_action(2);
        insert_action(3);
        insert_action(4);
    }
    m_context_menu->popup(event.screen_position());
}

GUI::Widget* SprayTool::get_properties_widget()
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

        auto& thickness_slider = thickness_container.add<GUI::HorizontalSlider>();
        thickness_slider.set_fixed_height(20);
        thickness_slider.set_range(1, 20);
        thickness_slider.set_value(m_thickness);
        thickness_slider.on_change = [this](int value) {
            m_thickness = value;
        };

        auto& density_container = m_properties_widget->add<GUI::Widget>();
        density_container.set_fixed_height(20);
        density_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& density_label = density_container.add<GUI::Label>("Density:");
        density_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        density_label.set_fixed_size(80, 20);

        auto& density_slider = density_container.add<GUI::HorizontalSlider>();
        density_slider.set_fixed_height(30);
        density_slider.set_range(1, 100);
        density_slider.set_value(m_density);
        density_slider.on_change = [this](int value) {
            m_density = value;
        };
    }

    return m_properties_widget.ptr();
}

}
