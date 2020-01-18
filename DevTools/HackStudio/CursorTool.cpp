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

#include "CursorTool.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include "WidgetTreeModel.h"
#include <AK/LogStream.h>

void CursorTool::on_mousedown(GMouseEvent& event)
{
    dbg() << "CursorTool::on_mousedown";
    auto& form_widget = m_editor.form_widget();
    auto result = form_widget.hit_test(event.position(), GWidget::ShouldRespectGreediness::No);

    if (event.button() == GMouseButton::Left) {
        if (result.widget && result.widget != &form_widget) {
            if (event.modifiers() & Mod_Ctrl) {
                m_editor.selection().toggle(*result.widget);
            } else if (!event.modifiers()) {
                if (!m_editor.selection().contains(*result.widget)) {
                    dbg() << "Selection didn't contain " << *result.widget << ", making it the only selected one";
                    m_editor.selection().set(*result.widget);
                }

                m_drag_origin = event.position();
                m_positions_before_drag.clear();
                m_editor.selection().for_each([&](auto& widget) {
                    m_positions_before_drag.set(&widget, widget.relative_position());
                    return IterationDecision::Continue;
                });
            }
        } else {
            m_editor.selection().clear();
            m_rubber_banding = true;
            m_rubber_band_origin = event.position();
            m_rubber_band_position = event.position();
            form_widget.update();
        }
        // FIXME: Do we need to update any part of the FormEditorWidget outside the FormWidget?
        form_widget.update();
    }
}

void CursorTool::on_mouseup(GMouseEvent& event)
{
    dbg() << "CursorTool::on_mouseup";
    if (event.button() == GMouseButton::Left) {
        auto& form_widget = m_editor.form_widget();
        auto result = form_widget.hit_test(event.position(), GWidget::ShouldRespectGreediness::No);
        if (!m_dragging && !(event.modifiers() & Mod_Ctrl)) {
            if (result.widget && result.widget != &form_widget) {
                m_editor.selection().set(*result.widget);
                // FIXME: Do we need to update any part of the FormEditorWidget outside the FormWidget?
                form_widget.update();
            }
        }
        m_dragging = false;
        m_rubber_banding = false;
        form_widget.update();
    }
}

void CursorTool::on_mousemove(GMouseEvent& event)
{
    dbg() << "CursorTool::on_mousemove";
    auto& form_widget = m_editor.form_widget();

    if (m_rubber_banding) {
        set_rubber_band_position(event.position());
        return;
    }

    if (!m_dragging && event.buttons() & GMouseButton::Left && event.position() != m_drag_origin) {
        auto result = form_widget.hit_test(event.position(), GWidget::ShouldRespectGreediness::No);
        if (result.widget && result.widget != &form_widget) {
            if (!m_editor.selection().contains(*result.widget)) {
                m_editor.selection().set(*result.widget);
                // FIXME: Do we need to update any part of the FormEditorWidget outside the FormWidget?
                form_widget.update();
            }
        }
        m_dragging = true;
    }

    if (m_dragging) {
        auto movement_delta = event.position() - m_drag_origin;
        m_editor.selection().for_each([&](auto& widget) {
            auto new_rect = widget.relative_rect();
            new_rect.set_location(m_positions_before_drag.get(&widget).value_or({}).translated(movement_delta));
            new_rect.set_x(new_rect.x() - (new_rect.x() % m_editor.form_widget().grid_size()));
            new_rect.set_y(new_rect.y() - (new_rect.y() % m_editor.form_widget().grid_size()));
            widget.set_relative_rect(new_rect);
            return IterationDecision::Continue;
        });
        m_editor.model().update();
        return;
    }
}

void CursorTool::on_keydown(GKeyEvent& event)
{
    dbg() << "CursorTool::on_keydown";

    auto move_selected_widgets_by = [this](int x, int y) {
        m_editor.selection().for_each([&](auto& widget) {
            widget.move_by(x, y);
            return IterationDecision::Continue;
        });
    };

    if (event.modifiers() == 0) {
        switch (event.key()) {
        case Key_Down:
            move_selected_widgets_by(0, m_editor.form_widget().grid_size());
            break;
        case Key_Up:
            move_selected_widgets_by(0, -m_editor.form_widget().grid_size());
            break;
        case Key_Left:
            move_selected_widgets_by(-m_editor.form_widget().grid_size(), 0);
            break;
        case Key_Right:
            move_selected_widgets_by(m_editor.form_widget().grid_size(), 0);
            break;
        }
    }
}

void CursorTool::set_rubber_band_position(const Point& position)
{
    if (m_rubber_band_position == position)
        return;
    m_rubber_band_position = position;

    auto rubber_band_rect = this->rubber_band_rect();

    m_editor.selection().clear();
    m_editor.form_widget().for_each_child_widget([&](auto& child) {
        if (child.relative_rect().intersects(rubber_band_rect))
            m_editor.selection().add(child);
        return IterationDecision::Continue;
    });

    m_editor.form_widget().update();
}

Rect CursorTool::rubber_band_rect() const
{
    if (!m_rubber_banding)
        return {};
    return Rect::from_two_points(m_rubber_band_origin, m_rubber_band_position);
}

void CursorTool::on_second_paint(GPainter& painter, GPaintEvent&)
{
    if (!m_rubber_banding)
        return;
    auto rect = rubber_band_rect();
    painter.fill_rect(rect, m_editor.palette().rubber_band_fill());
    painter.draw_rect(rect, m_editor.palette().rubber_band_border());
}
