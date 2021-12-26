/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class EraseTool final : public Tool {
public:
    EraseTool();
    virtual ~EraseTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_tool_button_contextmenu(GUI::ContextMenuEvent&) override;

private:
    Gfx::Color get_color() const;
    Gfx::IntRect build_rect(Gfx::IntPoint const& pos, Gfx::IntRect const& widget_rect);
    RefPtr<GUI::Menu> m_context_menu;

    bool m_use_secondary_color { false };
    int m_thickness { 1 };
    GUI::ActionGroup m_thickness_actions;
};

}
