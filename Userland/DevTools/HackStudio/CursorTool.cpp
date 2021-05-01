/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CursorTool.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include "WidgetTreeModel.h"
#include <AK/Debug.h>
#include <LibGfx/Palette.h>

namespace HackStudio {

void CursorTool::on_mousedown(GUI::MouseEvent& event)
{
    dbgln_if(CURSOR_TOOL_DEBUG, "CursorTool::on_mousedown");
    auto& form_widget = m_editor.form_widget();
    auto result = form_widget.hit_test(event.position(), GUI::Widget::ShouldRespectGreediness::No);

    if (event.button() == GUI::MouseButton::Left) {
        if (result.widget && result.widget != &form_widget) {
            if (event.modifiers() & Mod_Ctrl) {
                m_editor.selection().toggle(*result.widget);
            } else if (!event.modifiers()) {
                if (!m_editor.selection().contains(*result.widget)) {
                    dbgln_if(CURSOR_TOOL_DEBUG, "Selection didn't contain {}, making it the only selected one", *result.widget);
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

void CursorTool::on_mouseup(GUI::MouseEvent& event)
{
    dbgln_if(CURSOR_TOOL_DEBUG, "CursorTool::on_mouseup");
    if (event.button() == GUI::MouseButton::Left) {
        auto& form_widget = m_editor.form_widget();
        auto result = form_widget.hit_test(event.position(), GUI::Widget::ShouldRespectGreediness::No);
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

void CursorTool::on_mousemove(GUI::MouseEvent& event)
{
    dbgln_if(CURSOR_TOOL_DEBUG, "CursorTool::on_mousemove");
    auto& form_widget = m_editor.form_widget();

    if (m_rubber_banding) {
        set_rubber_band_position(event.position());
        return;
    }

    if (!m_dragging && event.buttons() & GUI::MouseButton::Left && event.position() != m_drag_origin) {
        auto result = form_widget.hit_test(event.position(), GUI::Widget::ShouldRespectGreediness::No);
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

void CursorTool::on_keydown(GUI::KeyEvent& event)
{
    dbgln_if(CURSOR_TOOL_DEBUG, "CursorTool::on_keydown");

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
        default:
            break;
        }
    }
}

void CursorTool::set_rubber_band_position(const Gfx::IntPoint& position)
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

Gfx::IntRect CursorTool::rubber_band_rect() const
{
    if (!m_rubber_banding)
        return {};
    return Gfx::IntRect::from_two_points(m_rubber_band_origin, m_rubber_band_position);
}

void CursorTool::on_second_paint(GUI::Painter& painter, GUI::PaintEvent&)
{
    if (!m_rubber_banding)
        return;
    auto rect = rubber_band_rect();
    painter.fill_rect(rect, m_editor.palette().rubber_band_fill());
    painter.draw_rect(rect, m_editor.palette().rubber_band_border());
}

}
