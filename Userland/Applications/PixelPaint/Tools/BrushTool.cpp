/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BrushTool.h"
#include "../ImageEditor.h"
#include "../Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/ValueSlider.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

BrushTool::BrushTool()
{
}

BrushTool::~BrushTool()
{
}

void BrushTool::on_mousedown(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (layer_event.button() != GUI::MouseButton::Primary && layer_event.button() != GUI::MouseButton::Secondary)
        return;

    // Shift+Click draws a line from the last position to current one.
    if (layer_event.shift() && m_has_clicked) {
        draw_line(layer->bitmap(), color_for(layer_event), m_last_position, layer_event.position());
        auto modified_rect = Gfx::IntRect::from_two_points(m_last_position, layer_event.position()).inflated(m_size * 2, m_size * 2);
        layer->did_modify_bitmap(modified_rect);
        m_last_position = layer_event.position();
        return;
    }

    const int first_draw_opacity = 10;

    for (int i = 0; i < first_draw_opacity; ++i)
        draw_point(layer->bitmap(), color_for(layer_event), layer_event.position());

    layer->did_modify_bitmap(Gfx::IntRect::centered_on(layer_event.position(), Gfx::IntSize { m_size * 2, m_size * 2 }));
    m_last_position = layer_event.position();
    m_has_clicked = true;
}

void BrushTool::on_mousemove(Layer* layer, MouseEvent& event)
{
    if (!layer)
        return;

    auto& layer_event = event.layer_event();
    if (!(layer_event.buttons() & GUI::MouseButton::Primary || layer_event.buttons() & GUI::MouseButton::Secondary))
        return;

    draw_line(layer->bitmap(), color_for(layer_event), m_last_position, layer_event.position());

    auto modified_rect = Gfx::IntRect::from_two_points(m_last_position, layer_event.position()).inflated(m_size * 2, m_size * 2);

    layer->did_modify_bitmap(modified_rect);
    m_last_position = layer_event.position();
    m_was_drawing = true;
}

void BrushTool::on_mouseup(Layer*, MouseEvent&)
{
    if (m_was_drawing) {
        m_editor->did_complete_action();
        m_was_drawing = false;
    }
}

Color BrushTool::color_for(GUI::MouseEvent const& event)
{
    return m_editor->color_for(event);
}

void BrushTool::draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point)
{
    for (int y = point.y() - size(); y < point.y() + size(); y++) {
        for (int x = point.x() - size(); x < point.x() + size(); x++) {
            auto distance = point.distance_from({ x, y });
            if (x < 0 || x >= bitmap.width() || y < 0 || y >= bitmap.height())
                continue;
            if (distance >= size())
                continue;

            auto falloff = (1.0 - double { distance / size() }) * (1.0 / (100 - hardness()));
            auto pixel_color = color;
            pixel_color.set_alpha(falloff * 255);
            bitmap.set_pixel(x, y, bitmap.get_pixel(x, y).blend(pixel_color));
        }
    }
}

void BrushTool::draw_line(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& start, Gfx::IntPoint const& end)
{
    int length_x = end.x() - start.x();
    int length_y = end.y() - start.y();
    float y_step = length_y == 0 ? 0 : (float)(length_y) / (float)(length_x);
    if (y_step > abs(length_y))
        y_step = abs(length_y);
    if (y_step < -abs(length_y))
        y_step = -abs(length_y);
    if (y_step == 0 && start.x() == end.x())
        return;

    int start_x = start.x();
    int end_x = end.x();
    int start_y = start.y();
    int end_y = end.y();
    if (start_x > end_x) {
        swap(start_x, end_x);
        swap(start_y, end_y);
    }

    float y = start_y;
    for (int x = start_x; x <= end_x; x++) {
        int start_step_y = y;
        int end_step_y = y + y_step;
        if (start_step_y > end_step_y)
            swap(start_step_y, end_step_y);
        for (int i = start_step_y; i <= end_step_y; i++)
            draw_point(bitmap, color, { x, i });
        y += y_step;
    }
}

GUI::Widget* BrushTool::get_properties_widget()
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
        size_slider.set_value(m_size);

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
        hardness_slider.set_value(m_hardness);

        hardness_slider.on_change = [&](int value) {
            set_hardness(value);
        };
        set_secondary_slider(&hardness_slider);
    }

    return m_properties_widget.ptr();
}

}
