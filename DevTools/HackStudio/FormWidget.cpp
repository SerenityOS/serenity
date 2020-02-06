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

#include "FormWidget.h"
#include "FormEditorWidget.h"
#include "Tool.h"
#include <LibGUI/Painter.h>

FormWidget::FormWidget(FormEditorWidget& parent)
    : GUI::Widget(&parent)
{
    set_fill_with_background_color(true);
    set_relative_rect(5, 5, 400, 300);

    set_greedy_for_hits(true);
}

FormWidget::~FormWidget()
{
}

FormEditorWidget& FormWidget::editor()
{
    return static_cast<FormEditorWidget&>(*parent());
}

const FormEditorWidget& FormWidget::editor() const
{
    return static_cast<const FormEditorWidget&>(*parent());
}

void FormWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    for (int y = 0; y < height(); y += m_grid_size) {
        for (int x = 0; x < width(); x += m_grid_size) {
            painter.set_pixel({ x, y }, Color::from_rgb(0x404040));
        }
    }
}

void FormWidget::second_paint_event(GUI::PaintEvent& event)
{
    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    if (!editor().selection().is_empty()) {
        for_each_child_widget([&](auto& child) {
            if (editor().selection().contains(child)) {
                painter.draw_rect(child.relative_rect(), Color::Blue);
            }
            return IterationDecision::Continue;
        });
    }

    editor().tool().on_second_paint(painter, event);
}

void FormWidget::mousedown_event(GUI::MouseEvent& event)
{
    editor().tool().on_mousedown(event);
}

void FormWidget::mouseup_event(GUI::MouseEvent& event)
{
    editor().tool().on_mouseup(event);
}

void FormWidget::mousemove_event(GUI::MouseEvent& event)
{
    editor().tool().on_mousemove(event);
}

void FormWidget::keydown_event(GUI::KeyEvent& event)
{
    editor().tool().on_keydown(event);
}
