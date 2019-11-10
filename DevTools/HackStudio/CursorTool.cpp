#include "CursorTool.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
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
    }
}

void CursorTool::on_mousemove(GMouseEvent& event)
{
    dbg() << "CursorTool::on_mousemove";

    if (!m_dragging && event.buttons() & GMouseButton::Left && event.position() != m_drag_origin) {
        auto& form_widget = m_editor.form_widget();
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
        return;
    }
}
