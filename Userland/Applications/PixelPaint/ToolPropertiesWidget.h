/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class Tool;

class ToolPropertiesWidget final : public GUI::Widget {
    C_OBJECT(ToolPropertiesWidget);

public:
    virtual ~ToolPropertiesWidget() override;

    void set_active_tool(Tool*);

private:
    ToolPropertiesWidget();

    RefPtr<GUI::GroupBox> m_group_box;

    Tool* m_active_tool { nullptr };
    GUI::Widget* m_active_tool_widget { nullptr };
};

}
