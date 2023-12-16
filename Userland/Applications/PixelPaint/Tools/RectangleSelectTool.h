/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
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

class RectangleSelectTool final : public Tool {
public:
    RectangleSelectTool() = default;
    virtual ~RectangleSelectTool() = default;

    virtual void on_mousedown(Layer*, MouseEvent& event) override;
    virtual void on_mousemove(Layer*, MouseEvent& event) override;
    virtual void on_mouseup(Layer*, MouseEvent& event) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override { return Gfx::StandardCursor::Crosshair; }
    virtual Gfx::IntPoint point_position_to_preferred_cell(Gfx::FloatPoint position) const override;

private:
    virtual StringView tool_name() const override { return "Rectangle Select Tool"sv; }

    enum class MovingMode {
        MovingOrigin,
        AroundCenter,
        None,
    };

    RefPtr<GUI::Widget> m_properties_widget;
    Vector<ByteString> m_merge_mode_names {};
    Selection::MergeMode m_merge_mode { Selection::MergeMode::Set };
    float m_edge_feathering { 0.0f };
    bool m_selecting { false };
    MovingMode m_moving_mode { MovingMode::None };
    Gfx::IntPoint m_selection_start;
    Gfx::IntPoint m_selection_end;

    Gfx::IntRect selection_rect() const;
};

}
