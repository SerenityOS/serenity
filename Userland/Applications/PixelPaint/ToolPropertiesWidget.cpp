/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
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

#include "ToolPropertiesWidget.h"
#include "Tool.h"
#include <LibGUI/BoxLayout.h>
#include <LibGUI/GroupBox.h>

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
