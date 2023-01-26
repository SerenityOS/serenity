/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "../ImageEditor.h"
#include "Tool.h"

namespace PixelPaint {

class GradientTool : public Tool {
public:
    GradientTool() = default;
    virtual ~GradientTool() override = default;
    virtual void on_mousedown(Layer*, MouseEvent&) override;
    virtual void on_mousemove(Layer*, MouseEvent&) override;
    virtual void on_mouseup(Layer*, MouseEvent&) override;
    virtual bool on_keydown(GUI::KeyEvent&) override;
    virtual void on_keyup(GUI::KeyEvent&) override;
    virtual void on_tool_activation() override;
    virtual GUI::Widget* get_properties_widget() override;

    virtual Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> cursor() override;
    virtual void on_second_paint(Layer const*, GUI::PaintEvent&) override;

protected:
    virtual StringView tool_name() const override { return "Gradient Tool"sv; }

private:
    RefPtr<GUI::Widget> m_properties_widget;
    Optional<Gfx::IntPoint> m_gradient_start;
    Optional<Gfx::IntPoint> m_gradient_center;
    Optional<Gfx::IntPoint> m_gradient_end;
    Gfx::IntPoint m_perpendicular_point;

    float m_gradient_half_length = 0;
    float m_physical_diagonal_layer_length = 0;
    bool m_button_pressed = false;
    bool m_shift_pressed = false;
    bool m_hover_over_drag_handle = false;
    bool m_hover_over_start_handle = false;
    bool m_hover_over_end_handle = false;
    int m_opacity = 100;
    Gfx::FloatLine m_gradient_begin_line;
    Gfx::FloatLine m_gradient_center_line;
    Gfx::FloatLine m_gradient_end_line;

    void reset();
    void draw_gradient(GUI::Painter&, bool with_guidelines = false, const Gfx::FloatPoint drawing_offset = { 0.0f, 0.0f }, float scale = 1, Optional<Gfx::IntRect const&> gradient_clip = {});
    void rasterize_gradient();
    void calculate_gradient_lines();
    void update_gradient_end_and_derive_start(Gfx::IntPoint const);
    void translate_gradient_start_end(Gfx::IntPoint const delta, bool update_start_counterwise = true);
    bool has_gradient_start_end() { return m_gradient_center.has_value() && m_gradient_end.has_value() && m_gradient_start.has_value(); }
};

}
