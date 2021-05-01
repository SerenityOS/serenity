/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FormEditorWidget.h"
#include "CursorTool.h"
#include "FormWidget.h"
#include "WidgetTreeModel.h"
#include <LibGUI/Painter.h>

namespace HackStudio {

FormEditorWidget::FormEditorWidget()
    : m_tool(make<CursorTool>(*this))
{
    set_fill_with_background_color(true);

    m_form_widget = add<FormWidget>();
    m_widget_tree_model = WidgetTreeModel::create(*m_form_widget);
}

FormEditorWidget::~FormEditorWidget()
{
}

void FormEditorWidget::paint_event(GUI::PaintEvent& event)
{
    GUI::Frame::paint_event(event);

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());
}

void FormEditorWidget::set_tool(NonnullOwnPtr<Tool> tool)
{
    m_tool->detach();
    m_tool = move(tool);
    m_tool->attach();
}

WidgetTreeModel& FormEditorWidget::model()
{
    return *m_widget_tree_model;
}

}
