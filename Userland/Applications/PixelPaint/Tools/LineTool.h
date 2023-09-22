/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class LineTool final : public Tool {
public:
    LineTool() = default;
    virtual ~LineTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

    void draw_using(GUI::Painter&, Gfx::IntPoint start_position, Gfx::IntPoint end_position, Color color, int thickness);

    virtual bool is_overriding_alt() override { return true; }

private:
    virtual StringView tool_name() const override { return "Line Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;

    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::IntPoint m_drag_start_position;
    Gfx::IntPoint m_line_start_position;
    Gfx::IntPoint m_line_end_position;
    int m_thickness { 1 };
    bool m_antialias_enabled { false };
};

}
