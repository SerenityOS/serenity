/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FormWidget.h"
#include "FormEditorWidget.h"
#include "Tool.h"
#include <LibGUI/Painter.h>

namespace HackStudio {

FormWidget::FormWidget()
{
    set_focus_policy(GUI::FocusPolicy::StrongFocus);
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

}
