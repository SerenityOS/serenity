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

ErrorOr<GUI::Widget*> EraseTool::get_properties_widget()
{
    if (!m_properties_widget) {
        auto properties_widget = TRY(GUI::Widget::try_create());
        (void)TRY(properties_widget->try_set_layout<GUI::VerticalBoxLayout>());

        auto size_container = TRY(properties_widget->try_add<GUI::Widget>());
        size_container->set_fixed_height(20);
        (void)TRY(size_container->try_set_layout<GUI::HorizontalBoxLayout>());

        auto size_label = TRY(size_container->try_add<GUI::Label>("Size:"_short_string));
        size_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        size_label->set_fixed_size(80, 20);

        auto size_slider = TRY(size_container->try_add<GUI::ValueSlider>(Orientation::Horizontal, "px"_short_string));
        size_slider->set_range(1, 100);
        size_slider->set_value(size());

        size_slider->on_change = [this, size_slider](int value) {
            set_size(value);
            size_slider->set_override_cursor(cursor());
        };
        set_primary_slider(size_slider);

        auto hardness_container = TRY(properties_widget->try_add<GUI::Widget>());
        hardness_container->set_fixed_height(20);
        (void)TRY(hardness_container->try_set_layout<GUI::HorizontalBoxLayout>());

        auto hardness_label = TRY(hardness_container->try_add<GUI::Label>(TRY("Hardness:"_string)));
        hardness_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label->set_fixed_size(80, 20);

        auto hardness_slider = TRY(hardness_container->try_add<GUI::ValueSlider>(Orientation::Horizontal, "%"_short_string));
        hardness_slider->set_range(1, 100);
        hardness_slider->set_value(hardness());

        hardness_slider->on_change = [this](int value) {
            set_hardness(value);
        };
        set_secondary_slider(hardness_slider);

        auto secondary_color_container = TRY(properties_widget->try_add<GUI::Widget>());
        secondary_color_container->set_fixed_height(20);
        (void)TRY(secondary_color_container->try_set_layout<GUI::HorizontalBoxLayout>());

        auto use_secondary_color_checkbox = TRY(secondary_color_container->try_add<GUI::CheckBox>());
        use_secondary_color_checkbox->set_checked(m_use_secondary_color);
        use_secondary_color_checkbox->set_text(TRY("Use secondary color"_string));
        use_secondary_color_checkbox->on_checked = [this](bool checked) {
            m_use_secondary_color = checked;
        };

        auto mode_container = TRY(properties_widget->try_add<GUI::Widget>());
        mode_container->set_fixed_height(46);
        (void)TRY(mode_container->try_set_layout<GUI::HorizontalBoxLayout>());
        auto mode_label = TRY(mode_container->try_add<GUI::Label>(TRY("Draw Mode:"_string)));
        mode_label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        mode_label->set_fixed_size(80, 20);

        auto mode_radio_container = TRY(mode_container->try_add<GUI::Widget>());
        (void)TRY(mode_radio_container->try_set_layout<GUI::VerticalBoxLayout>());
        auto pencil_mode_radio = TRY(mode_radio_container->try_add<GUI::RadioButton>("Pencil"_short_string));
        auto brush_mode_radio = TRY(mode_radio_container->try_add<GUI::RadioButton>("Brush"_short_string));

        pencil_mode_radio->on_checked = [this, hardness_slider, size_slider](bool) {
            m_draw_mode = DrawMode::Pencil;
            hardness_slider->set_enabled(false);
            refresh_editor_cursor();
            size_slider->set_override_cursor(cursor());
        };
        brush_mode_radio->on_checked = [this, hardness_slider, size_slider](bool) {
            m_draw_mode = DrawMode::Brush;
            hardness_slider->set_enabled(true);
            refresh_editor_cursor();
            size_slider->set_override_cursor(cursor());
        };

        pencil_mode_radio->set_checked(true);
        m_properties_widget = properties_widget;
    }

    return m_properties_widget.ptr();
}

NonnullRefPtr<Gfx::Bitmap> EraseTool::build_cursor()
{
    if (m_draw_mode == DrawMode::Brush)
        return BrushTool::build_cursor();

    m_scale_last_created_cursor = m_editor ? m_editor->scale() : 1;
    int scaled_size = size() * m_scale_last_created_cursor;

    // If we have an ImageEditor the scaled_size should not excede the longest size of the ImageEditor (width or height)
    if (m_editor) {
        int max_dimension = max(m_editor->width(), m_editor->height());
        if (scaled_size > max_dimension * 2) {
            // scaled_size needs to change in order to draw the cursor in the correct location.
            scaled_size = max_dimension * 2;
        }
    }
    scaled_size = max(scaled_size, 1);
    NonnullRefPtr<Gfx::Bitmap> new_cursor = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize(scaled_size, scaled_size)).release_value_but_fixme_should_propagate_errors();

    Gfx::IntRect rect { 0, 0, scaled_size, scaled_size };
    Gfx::Painter painter { new_cursor };

    // If no ImageEditor is present, we cannot bind the rect within the editor. Then we will just draw the rect as normal.

    auto half_scaled_size = scaled_size / 2;
    if (!m_editor) {
        painter.draw_rect(rect, Color::LightGray);
    } else {
        if (m_current_tool_position.x() >= (half_scaled_size) && (m_current_tool_position.x() + half_scaled_size) <= m_editor->width() && m_current_tool_position.y() >= (half_scaled_size) && (m_current_tool_position.y() + half_scaled_size) <= m_editor->height()) {
            painter.draw_rect(rect, Color::LightGray);
        } else {
            int left_overlap = (m_current_tool_position.x() < (half_scaled_size)) ? ((half_scaled_size)-m_current_tool_position.x()) : 0;
            int right_overlap = ((m_current_tool_position.x() + half_scaled_size) > m_editor->width()) ? ((m_current_tool_position.x() + half_scaled_size) - m_editor->width()) : 0;
            int top_overlap = (m_current_tool_position.y() < (half_scaled_size)) ? ((half_scaled_size)-m_current_tool_position.y()) : 0;
            int bottom_overlap = ((m_current_tool_position.y() + half_scaled_size) > m_editor->height()) ? ((m_current_tool_position.y() + half_scaled_size) - m_editor->height()) : 0;

            if (left_overlap == 0) {
                painter.draw_line(Gfx::IntPoint(0, top_overlap), Gfx::IntPoint(0, scaled_size - bottom_overlap), Color::LightGray);
            }
            if (right_overlap == 0) {
                painter.draw_line(Gfx::IntPoint(scaled_size - 1, top_overlap), Gfx::IntPoint(scaled_size - 1, scaled_size - bottom_overlap), Color::LightGray);
            }
            if (top_overlap == 0) {
                painter.draw_line(Gfx::IntPoint(left_overlap, 0), Gfx::IntPoint(scaled_size - right_overlap, 0), Color::LightGray);
            }
            if (bottom_overlap == 0) {
                painter.draw_line(Gfx::IntPoint(left_overlap, scaled_size - 1), Gfx::IntPoint(scaled_size - right_overlap, scaled_size - 1), Color::LightGray);
            }
        }
    }
    painter.draw_line({ half_scaled_size - 5, half_scaled_size }, { half_scaled_size + 5, half_scaled_size }, Color::LightGray, 3);
    painter.draw_line({ half_scaled_size, half_scaled_size - 5 }, { half_scaled_size, half_scaled_size + 5 }, Color::LightGray, 3);
    painter.draw_line({ half_scaled_size - 5, half_scaled_size }, { half_scaled_size + 5, half_scaled_size }, Color::MidGray, 1);
    painter.draw_line({ half_scaled_size, half_scaled_size - 5 }, { half_scaled_size, half_scaled_size + 5 }, Color::MidGray, 1);

    return new_cursor;
}

}
