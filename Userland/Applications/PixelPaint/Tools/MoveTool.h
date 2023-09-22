/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022-2023, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../Layer.h"
#include "Tool.h"
#include <LibGUI/RadioButton.h>

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

    enum class LayerSelectionMode {
        ForegroundLayer,
        ActiveLayer,
    };

    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;
    virtual NonnullRefPtr<GUI::Widget> get_properties_widget() override;
    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> cursor() override;
    virtual bool is_overriding_alt() override { return true; }
    LayerSelectionMode layer_selection_mode() const { return m_layer_selection_mode; }
    bool cursor_is_within_resize_anchor() { return m_resize_anchor_location.has_value(); }

private:
    static int resize_anchor_size(Gfx::IntRect layer_rect_in_frame_coordinates);
    static Gfx::IntRect resize_anchor_rect_from_position(Gfx::IntPoint, int resize_anchor_size);
    static Array<Gfx::IntRect, 4> resize_anchor_rects(Gfx::IntRect layer_rect_in_frame_coordinates, int resize_anchor_size);
    virtual StringView tool_name() const override { return "Move Tool"sv; }
    ErrorOr<void> update_cached_preview_bitmap(Layer const* layer);
    Optional<ResizeAnchorLocation const> resize_anchor_location_from_cursor_position(Layer const*, MouseEvent&);
    void toggle_selection_mode();

    LayerSelectionMode m_layer_selection_mode { LayerSelectionMode::ForegroundLayer };
    RefPtr<Layer> m_layer_being_moved;
    Gfx::IntPoint m_event_origin;
    Gfx::IntPoint m_layer_origin;
    Gfx::IntRect m_new_layer_rect;
    bool m_scaling { false };
    Optional<ResizeAnchorLocation const> m_resize_anchor_location {};
    bool m_keep_aspect_ratio { false };
    RefPtr<GUI::Widget> m_properties_widget;
    RefPtr<GUI::RadioButton> m_selection_mode_foreground;
    RefPtr<GUI::RadioButton> m_selection_mode_active;

    RefPtr<Gfx::Bitmap> m_cached_preview_bitmap { nullptr };
};

}
