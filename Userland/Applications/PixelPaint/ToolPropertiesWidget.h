/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGUI/Forward.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class Tool;

class ToolPropertiesWidget final : public GUI::Widget {
    C_OBJECT(ToolPropertiesWidget);

public:
    virtual ~ToolPropertiesWidget() override = default;

    void set_active_tool(Tool*);

private:
    ToolPropertiesWidget();

    RefPtr<GUI::GroupBox> m_group_box;

    Tool* m_active_tool { nullptr };
    RefPtr<GUI::StackWidget> m_tool_widget_stack;
    RefPtr<GUI::Widget> m_blank_widget;
    GUI::Widget* m_active_tool_widget { nullptr };
};

}
