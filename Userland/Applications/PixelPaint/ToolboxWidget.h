/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/ActionGroup.h>
#include <LibGUI/Frame.h>

namespace PixelPaint {

class Tool;

class ToolboxWidget final : public GUI::Frame {
    C_OBJECT(ToolboxWidget)
public:
    virtual ~ToolboxWidget() override;

    Function<void(Tool*)> on_tool_selection;

    template<typename Callback>
    void for_each_tool(Callback callback)
    {
        for (auto& tool : m_tools)
            callback(*tool);
    }

private:
    friend class ToolButton;

    void setup_tools();

    explicit ToolboxWidget();
    GUI::ActionGroup m_action_group;
    Vector<Tool*> m_tools;
};

}
