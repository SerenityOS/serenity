/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Guide.h"
#include "Tool.h"
#include <AK/RefPtr.h>

namespace PixelPaint {

class GuideTool final : public Tool {
public:
    GuideTool();

    virtual ~GuideTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_context_menu(Layer&, GUI::ContextMenuEvent&) override;

    virtual GUI::Widget* get_properties_widget() override;

private:
    RefPtr<Guide> closest_guide(Gfx::IntPoint const&);

    RefPtr<GUI::Widget> m_properties_widget;

    RefPtr<Guide> m_selected_guide;
    RefPtr<Guide> m_context_menu_guide;
    Gfx::IntPoint m_event_origin;
    float m_guide_origin { 0 };
    RefPtr<GUI::Menu> m_context_menu;
    int m_snap_size { 10 };
};
}
