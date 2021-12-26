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

#include "GraphWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Path.h>

GraphWidget::GraphWidget()
{
}

GraphWidget::~GraphWidget()
{
}

void GraphWidget::add_value(Vector<int, 1>&& value)
{
    m_values.enqueue(move(value));
    update();
}

void GraphWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());
    painter.fill_rect(event.rect(), m_background_color);

    auto inner_rect = frame_inner_rect();
    float scale = (float)inner_rect.height() / (float)m_max;

    if (!m_values.is_empty()) {
        // Draw one set of values at a time
        for (size_t k = 0; k < m_value_format.size(); k++) {
            const auto& format = m_value_format[k];
            if (format.line_color == Color::Transparent && format.background_color == Color::Transparent)
                continue;
            m_calculated_points.clear_with_capacity();
            for (size_t i = 0; i < m_values.size(); i++) {
                int x = inner_rect.right() - (i * 2) + 1;
                if (x < 0)
                    break;
                const auto& current_values = m_values.at(m_values.size() - i - 1);
                if (current_values.size() <= k) {
                    // Don't have a data point
                    m_calculated_points.append({ -1, -1 });
                    continue;
                }
                float value = current_values[k];
                if (m_stack_values) {
                    for (size_t l = k + 1; l < current_values.size(); l++)
                        value += current_values[l];
                }
                float scaled_value = value * scale;
                Gfx::IntPoint current_point { x, inner_rect.bottom() - (int)scaled_value };
                m_calculated_points.append(current_point);
            }
            ASSERT(m_calculated_points.size() <= m_values.size());
            if (format.background_color != Color::Transparent) {
                // Fill the background for the area we have values for
                Gfx::Path path;
                size_t points_in_path = 0;
                bool started_path = false;
                const Gfx::IntPoint* current_point = nullptr;
                const Gfx::IntPoint* first_point = nullptr;
                auto check_fill_area = [&]() {
                    if (!started_path)
                        return;
                    if (points_in_path > 1) {
                        ASSERT(current_point);
                        ASSERT(first_point);
                        path.line_to({ current_point->x() - 1, inner_rect.bottom() + 1 });
                        path.line_to({ first_point->x() + 1, inner_rect.bottom() + 1 });
                        path.close();
                        painter.fill_path(path, format.background_color, Gfx::Painter::WindingRule::EvenOdd);
                    } else if (points_in_path == 1 && current_point) {
                        // Can't fill any area, we only have one data point.
                        // Just draw a vertical line as a "fill"...
                        painter.draw_line(*current_point, { current_point->x(), inner_rect.bottom() }, format.background_color);
                    }
                    path = {};
                    points_in_path = 0;
                    first_point = nullptr;
                    started_path = false;
                };
                for (size_t i = 0; i < m_calculated_points.size(); i++) {
                    current_point = &m_calculated_points[i];
                    if (current_point->x() < 0) {
                        check_fill_area();
                        continue;
                    }
                    if (!started_path) {
                        path.move_to({ current_point->x() + 1, current_point->y() });
                        points_in_path = 1;
                        first_point = current_point;
                        started_path = true;
                    } else {
                        path.line_to({ current_point->x(), current_point->y() });
                        points_in_path++;
                    }
                }
                check_fill_area();
            }
            if (format.line_color != Color::Transparent) {
                // Draw the line for the data points we have
                const Gfx::IntPoint* previous_point = nullptr;
                for (size_t i = 0; i < m_calculated_points.size(); i++) {
                    const auto& current_point = m_calculated_points[i];
                    if (current_point.x() < 0) {
                        previous_point = nullptr;
                        continue;
                    }
                    if (previous_point)
                        painter.draw_line(*previous_point, current_point, format.line_color);
                    previous_point = &current_point;
                }
            }
        }
    }

    if (!m_values.is_empty() && !m_value_format.is_empty()) {
        const auto& current_values = m_values.last();
        int y = 0;
        for (size_t i = 0; i < min(m_value_format.size(), current_values.size()); i++) {
            const auto& format = m_value_format[i];
            if (!format.text_formatter)
                continue;
            auto constrain_rect = inner_rect.shrunken(8, 8);
            auto text_rect = constrain_rect.translated(0, y).intersected(constrain_rect);
            text_rect.set_height(font().glyph_height());
            auto text = format.text_formatter(current_values[i]);
            if (format.text_shadow_color != Color::Transparent)
                painter.draw_text(text_rect.translated(1, 1), text.characters(), Gfx::TextAlignment::CenterRight, format.text_shadow_color);
            painter.draw_text(text_rect, text.characters(), Gfx::TextAlignment::CenterRight, format.line_color);
            y += text_rect.height() + 4;
        }
    }
}
