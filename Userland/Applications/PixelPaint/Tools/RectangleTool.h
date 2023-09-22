/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/Forward.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class RectangleTool final : public Tool {
public:
    RectangleTool() = default;
    virtual ~RectangleTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    virtual StringView tool_name() const override { return "Rectangle Tool"sv; }

    enum class FillMode {
        Outline,
        Fill,
        Gradient,
        RoundedCorners
    };

    enum class DrawMode {
        FromCenter,
        FromCorner,
    };

    void draw_using(GUI::Painter&, Gfx::IntPoint start_position, Gfx::IntPoint end_position, int thickness, int corner_radius);

    RefPtr<GUI::Widget> m_properties_widget;
    RefPtr<GUI::TextBox> m_aspect_w_textbox;
    RefPtr<GUI::TextBox> m_aspect_h_textbox;

    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::IntPoint m_rectangle_start_position;
    Gfx::IntPoint m_rectangle_end_position;
    FillMode m_fill_mode { FillMode::Outline };
    DrawMode m_draw_mode { DrawMode::FromCorner };
    int m_thickness { 1 };
    Optional<float> m_aspect_ratio;
    bool m_antialias_enabled { false };
    int m_corner_radius = { 8 };
};

}
