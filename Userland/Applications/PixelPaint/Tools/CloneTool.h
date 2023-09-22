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

    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override;

    virtual bool is_overriding_alt() override { return true; }

protected:
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint point) override;
    virtual void draw_line(Gfx::Bitmap& bitmap, Gfx::Color color, Gfx::IntPoint start, Gfx::IntPoint end) override;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;

private:
    virtual StringView tool_name() const override { return "Clone Tool"sv; }
    Optional<Gfx::IntRect> sample_marker_rect();
    void update_sample_marker(Optional<Gfx::IntRect> old_rect);

    RefPtr<GUI::Widget> m_properties_widget;

    Optional<Gfx::IntPoint> m_sample_location;
    Optional<Gfx::IntPoint> m_cursor_offset;
    bool m_is_selecting_location { false };

    Gfx::Color m_marker_color { Gfx::Color::Green };
};

}
