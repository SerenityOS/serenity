/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GraphWidget.h"
#include <LibGUI/Application.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Font.h>
#include <LibGfx/Palette.h>
#include <LibGfx/Path.h>
#include <LibGfx/SystemTheme.h>

void GraphWidget::add_value(Vector<u64, 1>&& value)
{
    m_values.enqueue(move(value));
    update();
}

void GraphWidget::paint_event(GUI::PaintEvent& event)
{
    const auto& system_palette = GUI::Application::the()->palette();

    GUI::Frame::paint_event(event);
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());
    painter.fill_rect(event.rect(), palette().base());

    auto inner_rect = frame_inner_rect();
    float scale = (float)inner_rect.height() / (float)m_max;

    if (!m_values.is_empty()) {
        // Draw one set of values at a time
        for (size_t k = 0; k < m_value_format.size(); k++) {
            const auto& format = m_value_format[k];
            if (format.graph_color_role == ColorRole::Base) {
                continue;
            }
            const auto& line_color = system_palette.color(format.graph_color_role);
            const auto& background_color = line_color.with_alpha(0x7f);
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
            VERIFY(m_calculated_points.size() <= m_values.size());
            if (format.graph_color_role != ColorRole::Base) {
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
                        VERIFY(current_point);
                        VERIFY(first_point);
                        path.line_to({ current_point->x() - 1, inner_rect.bottom() + 1 });
                        path.line_to({ first_point->x() + 1, inner_rect.bottom() + 1 });
                        path.close();
                        painter.fill_path(path, background_color, Gfx::Painter::WindingRule::EvenOdd);
                    } else if (points_in_path == 1 && current_point) {
                        // Can't fill any area, we only have one data point.
                        // Just draw a vertical line as a "fill"...
                        painter.draw_line(*current_point, { current_point->x(), inner_rect.bottom() }, background_color);
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
            if (format.graph_color_role != ColorRole::Base) {
                // Draw the line for the data points we have
                const Gfx::IntPoint* previous_point = nullptr;
                for (size_t i = 0; i < m_calculated_points.size(); i++) {
                    const auto& current_point = m_calculated_points[i];
                    if (current_point.x() < 0) {
                        previous_point = nullptr;
                        continue;
                    }
                    if (previous_point)
                        painter.draw_line(*previous_point, current_point, line_color);
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
            const auto& graph_color = system_palette.color(format.graph_color_role);
            if (!format.text_formatter)
                continue;
            auto constrain_rect = inner_rect.shrunken(8, 8);
            auto text_rect = constrain_rect.translated(0, y).intersected(constrain_rect);
            text_rect.set_height(font().glyph_height());
            auto text = format.text_formatter(current_values[i]);
            if (format.text_shadow_color != Color::Transparent)
                painter.draw_text(text_rect.translated(1, 1), text.characters(), Gfx::TextAlignment::CenterRight, format.text_shadow_color);
            painter.draw_text(text_rect, text.characters(), Gfx::TextAlignment::CenterRight, graph_color);
            y += text_rect.height() + 4;
        }
    }
}
