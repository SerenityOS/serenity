/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Selection.h"
#include "Tool.h"
#include <AK/Vector.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class PolygonalSelectTool final : public Tool {
public:
    PolygonalSelectTool() = default;
    virtual ~PolygonalSelectTool() = default;
    virtual void on_doubleclick(Layer*, MouseEvent& event) override;
    virtual void on_mousedown(Layer*, MouseEvent& event) override;
    virtual void on_mousemove(Layer*, MouseEvent& event) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }
    virtual Gfx::IntPoint point_position_to_preferred_cell(Gfx::FloatPoint position) const override;

private:
    virtual void flood_polygon_selection(Gfx::Bitmap&, Gfx::IntPoint polygon_delta);
    virtual void process_polygon();
    virtual StringView tool_name() const override { return "Polygonal Select Tool"sv; }

    RefPtr<GUI::Widget> m_properties_widget;
    Selection::MergeMode m_merge_mode { Selection::MergeMode::Set };
    bool m_selecting { false };
    Gfx::IntPoint m_last_selecting_cursor_position;
    Vector<Gfx::IntPoint> m_polygon_points {};
};

}
