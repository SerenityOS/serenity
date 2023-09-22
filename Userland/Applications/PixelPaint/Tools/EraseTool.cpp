/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
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

Color EraseTool::color_for(GUI::MouseEvent const&)
{
    if (m_use_secondary_color)
        return m_editor->secondary_color();
    return Color(255, 255, 255, 0);
}

void EraseTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point)
{
    if (m_draw_mode == DrawMode::Pencil) {
        int radius = size() / 2;
        Gfx::IntRect rect { point.x() - radius, point.y() - radius, size(), size() };
        GUI::Painter painter(bitmap);
        // FIXME: Currently this mode does not respect the editing mask if present.
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
                auto falloff = get_falloff(distance);
                auto new_color = old_color.interpolate(color, falloff);
                set_pixel_with_possible_mask(x, y, new_color, bitmap);
            }
        }
    }
}

NonnullRefPtr<GUI::Widget> EraseTool::get_properties_widget()
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
        size_slider.set_range(1, 250);
        size_slider.set_value(size());

        size_slider.on_change = [this, &size_slider](int value) {
            set_size(value);
            size_slider.set_override_cursor(cursor());
        };
        set_primary_slider(&size_slider);

        auto& hardness_container = properties_widget->add<GUI::Widget>();
        hardness_container.set_fixed_height(20);
        hardness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& hardness_label = hardness_container.add<GUI::Label>("Hardness:"_string);
        hardness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label.set_fixed_size(80, 20);

        auto& hardness_slider = hardness_container.add<GUI::ValueSlider>(Orientation::Horizontal, "%"_string);
        hardness_slider.set_range(1, 100);
        hardness_slider.set_value(hardness());

        hardness_slider.on_change = [this](int value) {
            set_hardness(value);
        };
        set_secondary_slider(&hardness_slider);

        auto& secondary_color_container = properties_widget->add<GUI::Widget>();
        secondary_color_container.set_fixed_height(20);
        secondary_color_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& use_secondary_color_checkbox = secondary_color_container.add<GUI::CheckBox>();
        use_secondary_color_checkbox.set_checked(m_use_secondary_color);
        use_secondary_color_checkbox.set_text("Use secondary color"_string);
        use_secondary_color_checkbox.on_checked = [this](bool checked) {
            m_use_secondary_color = checked;
        };

        auto& mode_container = properties_widget->add<GUI::Widget>();
        mode_container.set_fixed_height(46);
        mode_container.set_layout<GUI::HorizontalBoxLayout>();
        auto& mode_label = mode_container.add<GUI::Label>("Draw Mode:"_string);
        mode_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label.set_fixed_size(80, 20);

        auto& mode_radio_container = mode_container.add<GUI::Widget>();
        mode_radio_container.set_layout<GUI::VerticalBoxLayout>();
        auto& pencil_mode_radio = mode_radio_container.add<GUI::RadioButton>("Pencil"_string);
        auto& brush_mode_radio = mode_radio_container.add<GUI::RadioButton>("Brush"_string);

        pencil_mode_radio.on_checked = [this, &hardness_slider, &size_slider](bool) {
            m_draw_mode = DrawMode::Pencil;
            hardness_slider.set_enabled(false);
            refresh_editor_cursor();
            size_slider.set_override_cursor(cursor());
        };
        brush_mode_radio.on_checked = [this, &hardness_slider, &size_slider](bool) {
            m_draw_mode = DrawMode::Brush;
            hardness_slider.set_enabled(true);
            refresh_editor_cursor();
            size_slider.set_override_cursor(cursor());
        };

        pencil_mode_radio.set_checked(true);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

NonnullRefPtr<Gfx::Bitmap> EraseTool::build_cursor()
{
    if (m_draw_mode == DrawMode::Brush)
        return BrushTool::build_cursor();

    m_scale_last_created_cursor = m_editor ? m_editor->scale() : 1;
    int cursor_size = AK::clamp(preferred_cursor_size(), 1, max_allowed_cursor_size());

    NonnullRefPtr<Gfx::Bitmap> new_cursor = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize(cursor_size, cursor_size)).release_value_but_fixme_should_propagate_errors();

    Gfx::Painter painter { new_cursor };

    if (preferred_cursor_size() > max_allowed_cursor_size()) {
        painter.draw_rect({ 0, 0, cursor_size, cursor_size }, Color::Red);
        painter.draw_rect({ 3, 3, cursor_size - 6, cursor_size - 6 }, Color::LightGray);
    } else {
        painter.draw_rect({ 0, 0, cursor_size, cursor_size }, Color::LightGray);
    }
    painter.draw_line({ cursor_size / 2 - 5, cursor_size / 2 }, { cursor_size / 2 + 5, cursor_size / 2 }, Color::LightGray, 3);
    painter.draw_line({ cursor_size / 2, cursor_size / 2 - 5 }, { cursor_size / 2, cursor_size / 2 + 5 }, Color::LightGray, 3);
    painter.draw_line({ cursor_size / 2 - 5, cursor_size / 2 }, { cursor_size / 2 + 5, cursor_size / 2 }, Color::MidGray, 1);
    painter.draw_line({ cursor_size / 2, cursor_size / 2 - 5 }, { cursor_size / 2, cursor_size / 2 + 5 }, Color::MidGray, 1);

    return new_cursor;
}

float EraseTool::preferred_cursor_size()
{
    return size() * (m_editor ? m_editor->scale() : 1);
}
}
