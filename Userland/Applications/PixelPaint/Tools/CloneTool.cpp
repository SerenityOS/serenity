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

void CloneTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color const&, Gfx::IntPoint const& point)
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
            pixel_color.set_alpha(falloff * 255);
            bitmap.set_pixel(target_x, target_y, bitmap.get_pixel(target_x, target_y).blend(pixel_color));
        }
    }
}

void CloneTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& start, Gfx::IntPoint const& end)
{
    if (!m_sample_location.has_value())
        return;
    BrushTool::draw_line(bitmap, color, start, end);
}

Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> CloneTool::cursor()
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
        m_sample_location = image_event.position() - m_cursor_offset.value();
        // FIXME: This is a really inefficient way to update the marker's location
        m_editor->update();
    }

    BrushTool::on_mousemove(layer, event);
}

void CloneTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    auto& image_event = event.image_event();
    if (image_event.alt()) {
        m_sample_location = image_event.position();
        m_cursor_offset = {};
        // FIXME: This is a really dumb way to get the marker to show up
        m_editor->update();
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

    auto sample_pos = m_editor->content_to_frame_position(m_sample_location.value());
    // We don't want the marker to be a single pixel and hide the color.
    auto offset = AK::max(2, size() / 2);
    Gfx::IntRect rect = {
        (int)sample_pos.x() - offset,
        (int)sample_pos.y() - offset,
        offset * 2,
        offset * 2
    };
    painter.draw_ellipse_intersecting(rect, m_marker_color, 1);
}

void CloneTool::on_keydown(GUI::KeyEvent& event)
{
    Tool::on_keydown(event);
    if (event.key() == KeyCode::Key_Alt && !m_is_selecting_location) {
        m_is_selecting_location = true;
        m_editor->update_tool_cursor();
        return;
    }
}

void CloneTool::on_keyup(GUI::KeyEvent& event)
{
    if (m_is_selecting_location && event.key() == KeyCode::Key_Alt) {
        m_is_selecting_location = false;
        m_editor->update_tool_cursor();
        return;
    }
}

GUI::Widget* CloneTool::get_properties_widget()
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
        hardness_slider.set_range(1, 100);
        hardness_slider.on_change = [&](int value) {
            set_hardness(value);
        };
        hardness_slider.set_value(100);
        set_secondary_slider(&hardness_slider);
    }

    return m_properties_widget.ptr();
}

}
