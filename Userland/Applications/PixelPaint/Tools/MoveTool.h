/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Layer.h"
#include "Tool.h"

namespace PixelPaint {

class MoveTool final : public Tool {
public:
    MoveTool() = default;
    virtual ~MoveTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual void on_keydown(GUI::KeyEvent&) override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override { return Gfx::StandardCursor::Move; }

private:
    RefPtr<Layer> m_layer_being_moved;
    Gfx::IntPoint m_event_origin;
    Gfx::IntPoint m_layer_origin;
};

}
