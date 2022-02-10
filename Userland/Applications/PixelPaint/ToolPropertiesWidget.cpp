/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ToolPropertiesWidget.h"
#include "Tools/Tool.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>

REGISTER_WIDGET(PixelPaint, ToolPropertiesWidget);

namespace PixelPaint {

ToolPropertiesWidget::ToolPropertiesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    m_group_box = add<GUI::GroupBox>("Tool properties");
    auto& layout = m_group_box->set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 8 });
    m_tool_widget_stack = m_group_box->add<GUI::StackWidget>();
    m_blank_widget = m_tool_widget_stack->add<GUI::Widget>();
}

void ToolPropertiesWidget::set_active_tool(Tool* tool)
{
    if (tool == m_active_tool)
        return;

    m_active_tool = tool;
    m_active_tool_widget = tool->get_properties_widget();

    if (m_active_tool_widget == nullptr) {
        m_tool_widget_stack->set_active_widget(m_blank_widget);
        return;
    }

    if (!m_tool_widget_stack->is_ancestor_of(*m_active_tool_widget))
        m_tool_widget_stack->add_child(*m_active_tool_widget);

    m_tool_widget_stack->set_active_widget(m_active_tool_widget);
}
}
