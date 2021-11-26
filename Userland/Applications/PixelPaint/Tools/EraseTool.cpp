/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "EraseTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/RadioButton.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

EraseTool::EraseTool()
{
}

EraseTool::~EraseTool()
{
}

Color EraseTool::color_for(GUI::MouseEvent const&)
{
    if (m_use_secondary_color)
        return m_editor->secondary_color();
    return Color(255, 255, 255, 0);
}

void EraseTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point)
{
    if (m_draw_mode == DrawMode::Pencil) {
        int radius = size() / 2;
        Gfx::IntRect rect { point.x() - radius, point.y() - radius, size(), size() };
        GUI::Painter painter(bitmap);
        painter.clear_rect(rect, color);
    } else {
        for (int y = point.y() - size(); y < point.y() + size(); y++) {
            for (int x = point.x() - size(); x < point.x() + size(); x++) {
                auto distance = point.distance_from({ x, y });
                if (x < 0 || x >= bitmap.width() || y < 0 || y >= bitmap.height())
                    continue;
                if (distance >= size())
                    continue;
                auto old_color = bitmap.get_pixel(x, y);
                auto falloff = (1.0 - double { distance / size() }) * (1.0 / (100 - hardness()));
                auto new_color = old_color.interpolate(color, falloff);
                bitmap.set_pixel(x, y, new_color);
            }
        }
    }
}

GUI::Widget* EraseTool::get_properties_widget()
{
    if (!m_properties_widget) {
        m_properties_widget = GUI::Widget::construct();
        m_properties_widget->set_layout<GUI::VerticalBoxLayout>();

        auto& size_container = m_properties_widget->add<GUI::Widget>();
        size_container.set_fixed_height(20);
        size_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& size_label = size_container.add<GUI::Label>("Size:");
        size_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label.set_fixed_size(80, 20);

        auto& size_slider = size_container.add<GUI::ValueSlider>(Orientation::Horizontal, "px");
        size_slider.set_range(1, 100);
        size_slider.set_value(size());

        size_slider.on_change = [&](int value) {
            set_size(value);
        };
        set_primary_slider(&size_slider);

        auto& hardness_container = m_properties_widget->add<GUI::Widget>();
        hardness_container.set_fixed_height(20);
        hardness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& hardness_label = hardness_container.add<GUI::Label>("Hardness:");
        hardness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label.set_fixed_size(80, 20);

        auto& hardness_slider = hardness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%");
        hardness_slider.set_range(1, 99);
        hardness_slider.set_value(hardness());

        hardness_slider.on_change = [&](int value) {
            set_hardness(value);
        };
        set_secondary_slider(&hardness_slider);

        auto& secondary_color_container = m_properties_widget->add<GUI::Widget>();
        secondary_color_container.set_fixed_height(20);
        secondary_color_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& use_secondary_color_checkbox = secondary_color_container.add<GUI::CheckBox>();
        use_secondary_color_checkbox.set_checked(m_use_secondary_color);
        use_secondary_color_checkbox.set_text("Use secondary color");
        use_secondary_color_checkbox.on_checked = [&](bool checked) {
            m_use_secondary_color = checked;
        };

        auto& mode_container = m_properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(46);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Draw Mode:");
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(80, 20);

        auto& mode_radio_container = mode_container.add<GUI::Widget>();
        mode_radio_container.set_layout<GUI::VerticalBoxLayout>();
        auto& pencil_mode_radio = mode_radio_container.add<GUI::RadioButton>("Pencil");
        auto& brush_mode_radio = mode_radio_container.add<GUI::RadioButton>("Brush");

        pencil_mode_radio.on_checked = [&](bool) {
            m_draw_mode = DrawMode::Pencil;
            hardness_slider.set_enabled(false);
        };
        brush_mode_radio.on_checked = [&](bool) {
            m_draw_mode = DrawMode::Brush;
            hardness_slider.set_enabled(true);
        };

        pencil_mode_radio.set_checked(true);
    }

    return m_properties_widget.ptr();
}

}
