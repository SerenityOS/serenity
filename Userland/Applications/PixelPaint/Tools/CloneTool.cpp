/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CloneTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Bitmap.h>

namespace PixelPaint {

void CloneTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color, Gfx::IntPoint point)
{
    if (!m_sample_location.has_value())
        return;

    auto source_point = point - m_cursor_offset.value();
    for (int y = -size(); y < size(); y++) {
        for (int x = -size(); x < size(); x++) {
            auto target_x = point.x() + x;
            auto target_y = point.y() + y;
            auto distance = point.distance_from({ target_x, target_y });
            if (target_x < 0 || target_x >= bitmap.width() || target_y < 0 || target_y >= bitmap.height())
                continue;
            if (distance >= size())
                continue;

            auto source_x = source_point.x() + x;
            auto source_y = source_point.y() + y;
            if (source_x < 0 || source_x >= bitmap.width() || source_y < 0 || source_y >= bitmap.height())
                continue;

            auto falloff = get_falloff(distance);
            auto pixel_color = bitmap.get_pixel(source_x, source_y);
            pixel_color.set_alpha(falloff * pixel_color.alpha());
            bitmap.set_pixel(target_x, target_y, bitmap.get_pixel(target_x, target_y).blend(pixel_color));
        }
    }
}

void CloneTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end)
{
    if (!m_sample_location.has_value())
        return;
    BrushTool::draw_line(bitmap, color, start, end);
}

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> CloneTool::cursor()
{
    if (m_is_selecting_location)
        return Gfx::StandardCursor::Eyedropper;
    return Gfx::StandardCursor::Crosshair;
}

void CloneTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (image_event.alt())
        return;

    if (m_cursor_offset.has_value()) {
        auto old_sample_marker_rect = sample_marker_rect();
        m_sample_location = image_event.position() - m_cursor_offset.value();
        update_sample_marker(old_sample_marker_rect);
    }

    BrushTool::on_mousemove(layer, event);
}

void CloneTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (image_event.alt()) {
        auto old_sample_marker_rect = sample_marker_rect();
        m_sample_location = image_event.position();
        m_cursor_offset = {};
        update_sample_marker(old_sample_marker_rect);
        return;
    }

    if (!m_sample_location.has_value())
        return;

    if (!m_cursor_offset.has_value())
        m_cursor_offset = event.image_event().position() - m_sample_location.value();

    BrushTool::on_mousedown(layer, event);
}

void CloneTool::on_second_paint(Layer const*, GUI::PaintEvent& event)
{
    if (!m_sample_location.has_value())
        return;

    GUI::Painter painter(*m_editor);
    painter.add_clip_rect(event.rect());

    auto rect = sample_marker_rect();
    painter.draw_ellipse_intersecting(rect.value(), m_marker_color, 1);
}

bool CloneTool::on_keydown(GUI::KeyEvent& event)
{
    if (event.key() == KeyCode::Key_LeftAlt && !m_is_selecting_location) {
        m_is_selecting_location = true;
        m_editor->update_tool_cursor();
        return true;
    }
    return Tool::on_keydown(event);
}

void CloneTool::on_keyup(GUI::KeyEvent& event)
{
    if (m_is_selecting_location && event.key() == KeyCode::Key_LeftAlt) {
        m_is_selecting_location = false;
        m_editor->update_tool_cursor();
        return;
    }
}

NonnullRefPtr<GUI::Widget> CloneTool::get_properties_widget()
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
        size_slider.set_range(1, 100);
        size_slider.set_value(size());

        size_slider.on_change = [this](int value) {
            auto old_sample_marker_rect = sample_marker_rect();
            set_size(value);
            update_sample_marker(old_sample_marker_rect);
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
        hardness_slider.on_change = [&](int value) {
            set_hardness(value);
        };
        hardness_slider.set_value(100);
        set_secondary_slider(&hardness_slider);
        m_properties_widget = properties_widget;
    }

    return *m_properties_widget;
}

Optional<Gfx::IntRect> CloneTool::sample_marker_rect()
{
    if (!m_sample_location.has_value())
        return {};

    auto offset = AK::max(2, size());
    Gfx::IntRect content_rect = {
        m_sample_location.value().x() - offset,
        m_sample_location.value().y() - offset,
        offset * 2,
        offset * 2
    };
    return m_editor->content_to_frame_rect(content_rect).to_type<int>();
}

void CloneTool::update_sample_marker(Optional<Gfx::IntRect> old_rect)
{
    if (old_rect.has_value())
        m_editor->update(old_rect.value().inflated(2, 2));

    auto current_rect = sample_marker_rect();
    if (current_rect.has_value())
        m_editor->update(current_rect.value().inflated(2, 2));
}

}
