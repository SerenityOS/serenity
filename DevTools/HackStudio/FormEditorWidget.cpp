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

#include "FormEditorWidget.h"
#include "CursorTool.h"
#include "FormWidget.h"
#include "WidgetTreeModel.h"
#include <LibGUI/GPainter.h>

FormEditorWidget::FormEditorWidget(GUI::Widget* parent)
    : ScrollableWidget(parent)
    , m_tool(make<CursorTool>(*this))
{
    set_fill_with_background_color(true);
    set_background_color(Color::MidGray);

    set_frame_shape(Gfx::FrameShape::Container);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_thickness(2);

    m_form_widget = FormWidget::construct(*this);
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
