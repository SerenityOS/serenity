/*
 * Copyright (c) 2020, Ben Jilks <benjyjilks@gmail.com>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class BrushTool : public Tool {
public:
    BrushTool();
    virtual ~BrushTool() override;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override { return Gfx::StandardCursor::Crosshair; }

    void set_size(int size) { m_size = size; }
    int size() const { return m_size; }

    void set_hardness(int hardness) { m_hardness = hardness; }
    int hardness() const { return m_hardness; }

protected:
    virtual Color color_for(GUI::MouseEvent const& event);
    virtual void draw_point(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& point);
    virtual void draw_line(Gfx::Bitmap& bitmap, Gfx::Color const& color, Gfx::IntPoint const& start, Gfx::IntPoint const& end);

private:
    RefPtr<GUI::Widget> m_properties_widget;
    int m_size { 20 };
    int m_hardness { 80 };
    bool m_was_drawing { false };
    bool m_has_clicked { false };
    Gfx::IntPoint m_last_position;
};

}
