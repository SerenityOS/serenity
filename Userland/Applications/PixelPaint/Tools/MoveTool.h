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
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override;

private:
    virtual StringView tool_name() const override { return "Move Tool"sv; }

    RefPtr<Layer> m_layer_being_moved;
    Gfx::IntPoint m_event_origin;
    Gfx::IntPoint m_layer_origin;
    Gfx::IntPoint m_new_scaled_layer_location;
    Gfx::IntSize m_new_layer_size;
    bool m_scaling { false };
    bool m_mouse_in_resize_corner { false };
    bool m_keep_ascept_ratio { false };
};

}
