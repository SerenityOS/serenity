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

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual GUI::Widget* get_properties_widget() override;

private:
    RefPtr<GUI::Widget> m_properties_widget;
    int m_size { 20 };
    int m_hardness { 80 };
    bool m_was_drawing { false };
    Gfx::IntPoint m_last_position;

    void draw_line(Gfx::Bitmap& bitmap, const Gfx::Color& color, const Gfx::IntPoint& start, const Gfx::IntPoint& end);
    void draw_point(Gfx::Bitmap& bitmap, const Gfx::Color& color, const Gfx::IntPoint& point);
};

}
