#include "CursorTool.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include <AK/LogStream.h>

void CursorTool::on_mousedown(GMouseEvent& event)
{
    dbg() << "CursorTool::on_mousedown";
    auto& form_widget = m_editor.form_widget();
    auto result = form_widget.hit_test(event.position(), GWidget::ShouldRespectGreediness::No);
    if (result.widget && result.widget != &form_widget) {
        if (event.modifiers() & Mod_Ctrl)
            m_editor.selection().toggle(*result.widget);
        else
            m_editor.selection().set(*result.widget);
        // FIXME: Do we need to update any part of the FormEditorWidget outside the FormWidget?
        form_widget.update();
    }
}

void CursorTool::on_mouseup(GMouseEvent& event)
{
    (void)event;
    dbg() << "CursorTool::on_mouseup";
}

void CursorTool::on_mousemove(GMouseEvent& event)
{
    (void)event;
    dbg() << "CursorTool::on_mousemove";
}
