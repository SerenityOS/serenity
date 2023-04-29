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
#include <LibGUI/Label.h>

REGISTER_WIDGET(PixelPaint, ToolPropertiesWidget);

namespace PixelPaint {

ToolPropertiesWidget::ToolPropertiesWidget()
{
    set_layout<GUI::VerticalBoxLayout>();

    m_group_box = add<GUI::GroupBox>("Tool properties"sv);
    m_group_box->set_layout<GUI::VerticalBoxLayout>(8);
    m_tool_widget_stack = m_group_box->add<GUI::StackWidget>();
    m_blank_widget = m_tool_widget_stack->add<GUI::Widget>();
    m_error_label = m_tool_widget_stack->add<GUI::Label>();
    m_error_label->set_enabled(false);
}

void ToolPropertiesWidget::set_active_tool(Tool* tool)
{
    if (tool == m_active_tool)
        return;

    m_active_tool = tool;
    auto active_tool_widget_or_error = tool->get_properties_widget();
    if (active_tool_widget_or_error.is_error()) {
        m_active_tool_widget = nullptr;
        m_error_label->set_text(String::formatted("Error creating tool properties: {}", active_tool_widget_or_error.release_error()).release_value_but_fixme_should_propagate_errors());
        m_tool_widget_stack->set_active_widget(m_error_label);
        return;
    }
    m_active_tool_widget = active_tool_widget_or_error.release_value();
    if (m_active_tool_widget == nullptr) {
        m_tool_widget_stack->set_active_widget(m_blank_widget);
        return;
    }

    if (!m_tool_widget_stack->is_ancestor_of(*m_active_tool_widget))
        m_tool_widget_stack->add_child(*m_active_tool_widget);

    m_tool_widget_stack->set_active_widget(m_active_tool_widget);
}
}
