/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Selection.h"
#include "Tool.h"

#include <AK/Vector.h>
#include <LibGUI/Widget.h>

namespace PixelPaint {

class RectangleSelectTool final : public Tool {
public:
    RectangleSelectTool();
    virtual ~RectangleSelectTool();

    virtual void on_mousedown(Layer*, MouseEvent& event) override;
    virtual void on_mousemove(Layer*, MouseEvent& event) override;
    virtual void on_mouseup(Layer*, MouseEvent& event) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    enum class MovingMode {
        MovingOrigin,
        AroundCenter,
        None,
    };

    RefPtr<GUI::Widget> m_properties_widget;
    Vector<String> m_merge_mode_names {};
    Selection::MergeMode m_merge_mode { Selection::MergeMode::Set };
    float m_edge_feathering { 0.0f };
    bool m_selecting { false };
    MovingMode m_moving_mode { MovingMode::None };
    Gfx::IntPoint m_selection_start;
    Gfx::IntPoint m_selection_end;
};

}
