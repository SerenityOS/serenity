/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibGUI/ActionGroup.h>
#include <LibGfx/Point.h>

namespace PixelPaint {

class PenTool final : public Tool {
public:
    PenTool();
    virtual ~PenTool() override;

    virtual void on_mousedown(Layer&, MouseEvent&) override;
    virtual void on_mousemove(Layer&, MouseEvent&) override;
    virtual void on_mouseup(Layer&, MouseEvent&) override;
    virtual GUI::Widget* get_properties_widget() override;
    virtual Gfx::StandardCursor cursor() override { return Gfx::StandardCursor::Crosshair; }

private:
    Gfx::IntPoint m_last_drawing_event_position { -1, -1 };
    RefPtr<GUI::Widget> m_properties_widget;
    int m_thickness { 1 };
};

}
