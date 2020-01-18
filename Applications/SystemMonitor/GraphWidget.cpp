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
#include <LibGUI/GPainter.h>

GraphWidget::GraphWidget(GWidget* parent)
    : GFrame(parent)
{
    set_frame_thickness(2);
    set_frame_shape(FrameShape::Container);
    set_frame_shadow(FrameShadow::Sunken);
}

GraphWidget::~GraphWidget()
{
}

void GraphWidget::add_value(int value)
{
    m_values.enqueue(value);
    update();
}

void GraphWidget::paint_event(GPaintEvent& event)
{
    GFrame::paint_event(event);
    GPainter painter(*this);
    painter.add_clip_rect(event.rect());
    painter.add_clip_rect(frame_inner_rect());
    painter.fill_rect(event.rect(), Color::Black);

    auto inner_rect = frame_inner_rect();
    float scale = (float)inner_rect.height() / (float)m_max;

    Point prev_point;
    for (int i = 0; i < m_values.size(); ++i) {
        int x = inner_rect.right() - (i * 2) + 1;
        if (x < 0)
            break;
        float scaled_value = (float)m_values.at(m_values.size() - i - 1) * scale;
        Point point = { x, inner_rect.bottom() - (int)scaled_value };
        if (i != 0)
            painter.draw_line(prev_point, point, m_graph_color);
        prev_point = point;
    }

    if (!m_values.is_empty() && text_formatter) {
        Rect text_rect = inner_rect.shrunken(8, 8);
        text_rect.set_height(font().glyph_height());
        auto text = text_formatter(m_values.last(), m_max);
        painter.draw_text(text_rect.translated(1, 1), text.characters(), TextAlignment::CenterRight, Color::Black);
        painter.draw_text(text_rect, text.characters(), TextAlignment::CenterRight, m_text_color);
    }
}
