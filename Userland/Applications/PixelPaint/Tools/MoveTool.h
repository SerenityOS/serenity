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

enum class ResizeAnchorLocation {
    BottomLeft,
    BottomRight,
    TopLeft,
    TopRight
};
class MoveTool final : public Tool {
public:
    MoveTool() = default;
    virtual ~MoveTool() override = default;

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override;

private:
    virtual StringView tool_name() const override { return "Move Tool"sv; }
    ErrorOr<void> update_cached_preview_bitmap(Layer const* layer);
    Optional<ResizeAnchorLocation const> resize_anchor_location_from_cursor_position(Layer const*, MouseEvent&);

    RefPtr<Layer> m_layer_being_moved;
    Gfx::IntPoint m_event_origin;
    Gfx::IntPoint m_layer_origin;
    Gfx::IntRect m_new_layer_rect;
    bool m_scaling { false };
    Optional<ResizeAnchorLocation const> m_resize_anchor_location {};
    bool m_keep_ascept_ratio { false };

    RefPtr<Gfx::Bitmap> m_cached_preview_bitmap { nullptr };
};

}
