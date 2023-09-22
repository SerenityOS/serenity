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
#include <LibGUI/TextBox.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class EllipseTool final : public Tool {
public:
    EllipseTool() = default;
    virtual ~EllipseTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    virtual StringView tool_name() const override { return "Ellipse Tool"sv; }

    enum class FillMode {
        Outline,
        Fill
    };

    enum class DrawMode {
        FromCenter,
        FromCorner,
    };

    void draw_using(GUI::Painter&, Gfx::IntPoint start_position, Gfx::IntPoint end_position, int thickness);

    RefPtr<GUI::Widget> m_properties_widget;
    RefPtr<GUI::TextBox> m_aspect_w_textbox;
    RefPtr<GUI::TextBox> m_aspect_h_textbox;

    GUI::MouseButton m_drawing_button { GUI::MouseButton::None };
    Gfx::IntPoint m_ellipse_start_position;
    Gfx::IntPoint m_ellipse_end_position;
    int m_thickness { 1 };
    FillMode m_fill_mode { FillMode::Outline };
    DrawMode m_draw_mode { DrawMode::FromCorner };
    Optional<float> m_aspect_ratio;
    bool m_antialias_enabled { false };
};

}
