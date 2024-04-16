/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "SprayTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <AK/Math.h>
#include <AK/Queue.h>
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

SprayTool::SprayTool()
{
    m_timer = Core::Timer::create_repeating(200, [&]() {
        paint_it();
    });
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

    auto& bitmap = layer->get_scratch_edited_bitmap();
    GUI::Painter painter(bitmap);
    VERIFY(bitmap.bpp() == 32);
    double const minimal_radius = 2;
    double const base_radius = minimal_radius * m_thickness;
    for (int i = 0; i < M_PI * base_radius * base_radius * (m_density / 100.0); i++) {
        double radius = base_radius * nrand();
        double angle = 2 * M_PI * nrand();
        int const xpos = m_last_pos.x() + radius * AK::cos(angle);
        int const ypos = m_last_pos.y() - radius * AK::sin(angle);
        if (xpos < 0 || xpos >= bitmap.width())
            continue;
        if (ypos < 0 || ypos >= bitmap.height())
            continue;
        set_pixel_with_possible_mask<Gfx::StorageFormat::BGRA8888>(xpos, ypos, m_color, bitmap);
    }

    layer->did_modify_bitmap(Gfx::IntRect::centered_on(m_last_pos, Gfx::IntSize(base_radius * 2, base_radius * 2)));
}

void SprayTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    m_color = m_editor->color_for(layer_event);
    m_last_pos = layer_event.position();
    m_timer->start();
    paint_it();
}

void SprayTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    m_last_pos = event.layer_event().position();
    if (m_timer->is_active()) {
        paint_it();
        m_timer->restart(m_timer->interval());
    }
}

void SprayTool::on_mouseup(Layer*, MouseEvent&)
{
    if (m_timer->is_active()) {
        m_timer->stop();
        m_editor->did_complete_action(tool_name());
    }
}

NonnullRefPtr<GUI::Widget> SprayTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = GUI::Widget::construct();
        properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& size_container = properties_widget->add<GUI::Widget>();
        size_container.set_fixed_height(20);
        size_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& size_label = size_container.add<GUI::Label>("Size:"_string);
        size_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label.set_fixed_size(80, 20);

        auto& size_slider = size_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px"_string);
        size_slider.set_range(1, 20);
        size_slider.set_value(m_thickness);

        size_slider.on_change = [this](int value) {
            m_thickness = value;
        };
        set_primary_slider(&size_slider);

        auto& density_container = properties_widget->add<GUI::Widget>();
        density_container.set_fixed_height(20);
        density_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& density_label = density_container.add<GUI::Label>("Density:"_string);
        density_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        density_label.set_fixed_size(80, 20);

        auto& density_slider = density_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        density_slider.set_range(1, 100);
        density_slider.set_value(m_density);

        density_slider.on_change = [this](int value) {
            m_density = value;
        };
        set_secondary_slider(&density_slider);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

}
