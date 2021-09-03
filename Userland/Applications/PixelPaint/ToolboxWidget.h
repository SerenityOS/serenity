/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/NonnullOwnPtrVector.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class Tool;

class ToolboxWidget final : public GUI::Widget {
    C_OBJECT(ToolboxWidget);

public:
    virtual ~ToolboxWidget() override;

    Function<void(Tool*)> on_tool_selection;

    template<typename Callback>
    void for_each_tool(Callback callback)
    {
        for (auto& tool : m_tools)
            callback(tool);
    }

private:
    friend class ToolButton;

    void setup_tools();

    explicit ToolboxWidget();
    RefPtr<GUI::Toolbar> m_toolbar;
    GUI::ActionGroup m_action_group;
    NonnullOwnPtrVector<Tool> m_tools;
};

}
