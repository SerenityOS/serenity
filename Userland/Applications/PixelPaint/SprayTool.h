/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"
#include <LibCore/Timer.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Painter.h>

namespace PixelPaint {

class SprayTool final : public Tool {
public:
    SprayTool();
    virtual ~SprayTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual GUI::Widget* get_properties_widget() override;

private:
    void paint_it();

    RefPtr<GUI::Widget> m_properties_widget;
    RefPtr<Core::Timer> m_timer;
    Gfx::IntPoint m_last_pos;
    Color m_color;
    int m_thickness { 10 };
    int m_density { 40 };
};

}
