/*
 * Copyright (c) 2022, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>

namespace GUI {

class AbstractZoomPanWidget : public GUI::Frame {
    C_OBJECT(AbstractZoomPanWidget);

public:
    void set_scale(float scale);
    float scale() const { return m_scale; }
    void set_scale_bounds(float min_scale, float max_scale);

    void scale_by(float amount);
    void scale_centered(float new_scale, Gfx::IntPoint center);

    bool is_panning() const { return m_is_panning; }
    void start_panning(Gfx::IntPoint position);
    void stop_panning();

    void pan_to(Gfx::IntPoint position);

    // Should be overridden by derived classes if they want updates.
    virtual void handle_relayout(Gfx::IntRect const&) { update(); }
    void relayout();

    Gfx::FloatPoint frame_to_content_position(Gfx::IntPoint frame_position) const;
    Gfx::FloatRect frame_to_content_rect(Gfx::IntRect const& frame_rect) const;
    Gfx::FloatPoint content_to_frame_position(Gfx::IntPoint content_position) const;
    Gfx::FloatRect content_to_frame_rect(Gfx::IntRect const& content_rect) const;

    virtual void mousewheel_event(GUI::MouseEvent& event) override;
    virtual void mousedown_event(GUI::MouseEvent& event) override;
    virtual void resize_event(GUI::ResizeEvent& event) override;
    virtual void mousemove_event(GUI::MouseEvent& event) override;
    virtual void mouseup_event(GUI::MouseEvent& event) override;

    void set_original_rect(Gfx::IntRect const& rect) { m_original_rect = rect; }
    void set_content_rect(Gfx::IntRect const& content_rect);
    void set_origin(Gfx::FloatPoint origin) { m_origin = origin; }

    void reset_view();

    Gfx::IntRect content_rect() const { return m_content_rect; }

    Function<void(float)> on_scale_change;

    enum class FitType {
        Width,
        Height,
        Both
    };
    void fit_content_to_rect(Gfx::IntRect const& rect, FitType = FitType::Both);
    void fit_content_to_view(FitType fit_type = FitType::Both)
    {
        fit_content_to_rect(rect(), fit_type);
    }

private:
    Gfx::IntRect m_original_rect;
    Gfx::IntRect m_content_rect;

    Gfx::IntPoint m_pan_mouse_pos;
    Gfx::FloatPoint m_origin;
    Gfx::FloatPoint m_pan_start;
    bool m_is_panning { false };

    float m_min_scale { 0.1f };
    float m_max_scale { 10.0f };
    float m_scale { 1.0f };

    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> m_saved_cursor { Gfx::StandardCursor::None };
};

}
