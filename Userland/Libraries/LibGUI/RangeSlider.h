/*
 * Copyright (c) 2023, Torsten Engelmann <engelTorsten@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractSlider.h>
#include <LibGfx/Gradients.h>

namespace GUI {

class RangeSlider : public AbstractSlider {
    C_OBJECT(RangeSlider);

public:
    virtual ~RangeSlider() override = default;

    void set_gradient_color(Gfx::Color, Gfx::Color);
    void set_gradient_colors(Vector<Gfx::ColorStop>);
    void set_show_label(bool);
    bool show_label();
    void set_lower_range(int value, AllowCallback allow_callback = AllowCallback::Yes);
    void set_upper_range(int value, AllowCallback allow_callback = AllowCallback::Yes);
    int lower_range();
    int upper_range();
    void set_range(int min, int max);
    Function<void(int, int)> on_range_change;

protected:
    explicit RangeSlider(Gfx::Orientation = Gfx::Orientation::Horizontal);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;

private:
    Gfx::IntRect frame_inner_rect() const;

    Vector<Gfx::ColorStop> m_background_gradient = Vector { Gfx::ColorStop { { 0, 0, 0, 0 }, 0 }, Gfx::ColorStop { { 0, 0, 0, 255 }, 1 } };

    virtual Optional<UISize> calculated_min_size() const override;
    virtual Optional<UISize> calculated_preferred_size() const override;

    int value_at(Gfx::IntPoint) const;
    Gfx::IntRect knob_rect_for_value(int value) const;

    bool m_show_label { true };
    bool m_dragging { false };
    bool m_hovered_lower_knob { false };
    bool m_hovered_upper_knob { false };
    int m_lower_range = 0;
    int m_upper_range = 0;
    int const c_knob_width = 7;
};

class HorizontalRangeSlider final : public RangeSlider {
    C_OBJECT(HorizontalRangeSlider);

public:
    virtual ~HorizontalRangeSlider() override = default;

private:
    HorizontalRangeSlider()
        : RangeSlider(Orientation::Horizontal)
    {
    }
};

}
