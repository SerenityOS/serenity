/*
 * Copyright (c) 2022, Timothy Slater <tslater2006@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Selection.h"
#include "Tool.h"

#include <AK/Vector.h>
#include <LibGUI/Widget.h>
#include <LibGfx/Path.h>

namespace PixelPaint {

class LassoSelectTool final : public Tool {
public:
    LassoSelectTool() = default;
    virtual ~LassoSelectTool() = default;

    virtual void on_mousedown(Layer*, MouseEvent& event) override;
    virtual void on_mouseup(Layer*, MouseEvent& event) override;
    virtual void on_mousemove(Layer*, MouseEvent& event) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    virtual StringView tool_name() const override { return "Lasso Select Tool"sv; }
    void flood_lasso_selection(Gfx::Bitmap&);

    RefPtr<GUI::Widget> m_properties_widget;
    Selection::MergeMode m_merge_mode { Selection::MergeMode::Set };

    Gfx::IntPoint m_start_position;
    Gfx::IntPoint m_most_recent_position;
    Vector<Gfx::IntPoint> m_path_points;

    Gfx::IntPoint m_top_left;
    Gfx::IntPoint m_bottom_right;

    bool m_selecting { false };
};

}
