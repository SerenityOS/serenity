/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class BrushTool final : public Tool {
public:
    BrushTool();
    virtual ~BrushTool() override;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;
    virtual Gfx::StandardCursor cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    RefPtr<GUI::Widget> m_properties_widget;
    int m_size { 20 };
    int m_hardness { 80 };
    bool m_was_drawing { false };
    bool m_has_clicked { false };
    Gfx::IntPoint m_last_position;

    void draw_line(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& start, Gfx::IntPoint const& end);
    void draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point);
};

}
