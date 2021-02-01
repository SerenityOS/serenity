/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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

#include "BrushTool.h"
#include "ImageEditor.h"
#include "Layer.h"
#include <LibGUI/Action.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Label.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Color.h>
#include <LibGfx/Rect.h>

namespace PixelPaint {

BrushTool::BrushTool()
{
}

BrushTool::~BrushTool()
{
}

void BrushTool::on_mousedown(Layer&, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (event.button() != GUI::MouseButton::Left && event.button() != GUI::MouseButton::Right)
        return;

    m_last_position = event.position();
}

void BrushTool::on_mousemove(Layer& layer, GUI::MouseEvent& event, GUI::MouseEvent&)
{
    if (!(event.buttons() & GUI::MouseButton::Left || event.buttons() & GUI::MouseButton::Right))
        return;

    draw_line(layer.bitmap(), m_editor->color_for(event), m_last_position, event.position());
    layer.did_modify_bitmap(*m_editor->image());
    m_last_position = event.position();
    m_was_drawing = true;
}

void BrushTool::on_mouseup(Layer&, GUI::MouseEvent&, GUI::MouseEvent&)
{
    if (m_was_drawing) {
        m_editor->did_complete_action();
        m_was_drawing = false;
    }
}

void BrushTool::draw_point(Gfx::Bitmap& bitmap, const Gfx::Color& color, const Gfx::IntPoint& point)
{
    for (int y = point.y() - m_size; y < point.y() + m_size; y++) {
        for (int x = point.x() - m_size; x < point.x() + m_size; x++) {
            auto distance = point.distance_from({ x, y });
            if (x < 0 || x >= bitmap.width() || y < 0 || y >= bitmap.height())
                continue;
            if (distance >= m_size)
                continue;

            auto falloff = (1.0 - (distance / (float)m_size)) * (1.0f / (100 - m_hardness));
            auto pixel_color = color;
            pixel_color.set_alpha(falloff * 255);
            bitmap.set_pixel(x, y, bitmap.get_pixel(x, y).blend(pixel_color));
        }
    }
}

void BrushTool::draw_line(Gfx::Bitmap& bitmap, const Gfx::Color& color, const Gfx::IntPoint& start, const Gfx::IntPoint& end)
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

        auto& size_slider = size_container.add<GUI::HorizontalSlider>();
        size_slider.set_fixed_height(20);
        size_slider.set_range(1, 100);
        size_slider.set_value(m_size);
        size_slider.on_change = [this](int value) {
            m_size = value;
        };

        auto& hardness_container = m_properties_widget->add<GUI::Widget>();
        hardness_container.set_fixed_height(20);
        hardness_container.set_layout<GUI::HorizontalBoxLayout>();

        auto& hardness_label = hardness_container.add<GUI::Label>("Hardness:");
        hardness_label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        hardness_label.set_fixed_size(80, 20);

        auto& hardness_slider = hardness_container.add<GUI::HorizontalSlider>();
        hardness_slider.set_fixed_height(20);
        hardness_slider.set_range(1, 99);
        hardness_slider.set_value(m_hardness);
        hardness_slider.on_change = [this](int value) {
            m_hardness = value;
        };
    }

    return m_properties_widget.ptr();
}

}
