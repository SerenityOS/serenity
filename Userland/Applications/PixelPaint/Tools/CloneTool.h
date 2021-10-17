/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "BrushTool.h"

namespace PixelPaint {

class CloneTool : public BrushTool {
public:
    CloneTool() = default;
    virtual ~CloneTool() override = default;

    virtual GUI::Widget* get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override;

protected:
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point) override;
    virtual void draw_line(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& start, Gfx::IntPoint const& end) override;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;

private:
    RefPtr<GUI::Widget> m_properties_widget;

    Optional<Gfx::IntPoint> m_sample_location;
    Optional<Gfx::IntPoint> m_cursor_offset;
    bool m_is_selecting_location { false };

    Gfx::Color m_marker_color { Gfx::Color::Green };
};

}
