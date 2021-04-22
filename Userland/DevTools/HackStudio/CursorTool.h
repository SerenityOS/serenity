/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <AK/HashMap.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Point.h>

namespace HackStudio {

class CursorTool final : public Tool {
public:
    explicit CursorTool(FormEditorWidget& editor)
        : Tool(editor)
    {
    }
    virtual ~CursorTool() override { }

private:
    virtual const char* class_name() const override { return "CursorTool"; }
    virtual void on_mousedown(GUI::MouseEvent&) override;
    virtual void on_mouseup(GUI::MouseEvent&) override;
    virtual void on_mousemove(GUI::MouseEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual void on_second_paint(GUI::Painter&, GUI::PaintEvent&) override;

    void set_rubber_band_position(const Gfx::IntPoint&);
    Gfx::IntRect rubber_band_rect() const;

    Gfx::IntPoint m_drag_origin;
    HashMap<GUI::Widget*, Gfx::IntPoint> m_positions_before_drag;
    bool m_dragging { false };

    bool m_rubber_banding { false };
    Gfx::IntPoint m_rubber_band_origin;
    Gfx::IntPoint m_rubber_band_position;
};

}
