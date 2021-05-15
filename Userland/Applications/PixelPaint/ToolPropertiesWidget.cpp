/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ToolPropertiesWidget.h"
#include "Tool.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>

REGISTER_WIDGET(PixelPaint, ToolPropertiesWidget);

namespace PixelPaint {

ToolPropertiesWidget::ToolPropertiesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    m_group_box = add<GUI::GroupBox>("Tool properties");
    auto& layout = m_group_box->set_layout<GUI::VerticalBoxLayout>();
    layout.set_margins({ 10, 20, 10, 10 });
}

void ToolPropertiesWidget::set_active_tool(Tool* tool)
{
    if (tool == m_active_tool)
        return;

    if (m_active_tool_widget != nullptr)
        m_group_box->remove_child(*m_active_tool_widget);

    m_active_tool = tool;
    m_active_tool_widget = tool->get_properties_widget();
    if (m_active_tool_widget != nullptr)
        m_group_box->add_child(*m_active_tool_widget);
}

ToolPropertiesWidget::~ToolPropertiesWidget()
{
}

}
