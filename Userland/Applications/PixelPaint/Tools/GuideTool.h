/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Guide.h"
#include "Tool.h"
#include <AK/RefPtr.h>
#include <LibGUI/Menu.h>

namespace PixelPaint {

class GuideTool final : public Tool {
public:
    GuideTool() = default;

    virtual ~GuideTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_context_menu(Layer*, GUI::ContextMenuEvent&) override;

    virtual void on_tool_activation() override;

    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    virtual StringView tool_name() const override { return "Guide Tool"sv; }

    RefPtr<Guide> closest_guide(Gfx::IntPoint);

    RefPtr<GUI::Widget> m_properties_widget;

    RefPtr<Guide> m_selected_guide;
    RefPtr<Guide> m_context_menu_guide;
    Gfx::IntPoint m_event_origin;
    float m_guide_origin { 0 };
    RefPtr<GUI::Menu> m_context_menu;
    int m_snap_size { 10 };
};
}
