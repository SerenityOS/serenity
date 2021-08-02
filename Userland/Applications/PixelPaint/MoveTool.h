/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Tool.h"

namespace PixelPaint {

class MoveTool final : public Tool {
public:
    MoveTool();
    virtual ~MoveTool() override;

    virtual void on_mousedown(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mousemove(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_mouseup(Layer&, GUI::MouseEvent& layer_event, GUI::MouseEvent& image_event) override;
    virtual void on_keydown(GUI::KeyEvent&) override;

private:
    RefPtr<Layer> m_layer_being_moved;
    Gfx::IntPoint m_event_origin;
    Gfx::IntPoint m_layer_origin;
};

}
