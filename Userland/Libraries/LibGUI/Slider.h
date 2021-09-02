/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/AbstractSlider.h>

namespace GUI {

class Slider : public AbstractSlider {
    C_OBJECT(Slider);

public:
    enum class KnobSizeMode {
        Fixed,
        Proportional,
    };

    virtual ~Slider() override;

    void set_knob_size_mode(KnobSizeMode mode) { m_knob_size_mode = mode; }
    KnobSizeMode knob_size_mode() const { return m_knob_size_mode; }

    int track_size() const { return 2; }
    int track_margin() const { return 10; }
    int knob_fixed_primary_size() const { return 8; }
    int knob_secondary_size() const { return 20; }

    bool knob_dragging() const { return m_dragging; }
    Gfx::IntRect knob_rect() const;

    Gfx::IntRect inner_rect() const
    {
        if (orientation() == Orientation::Horizontal)
            return rect().shrunken(track_margin() * 2, 0);
        return rect().shrunken(0, track_margin() * 2);
    }

protected:
    explicit Slider(Orientation = Orientation::Vertical);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void change_event(Event&) override;

private:
    void set_knob_hovered(bool);

    bool m_knob_hovered { false };
    bool m_dragging { false };
    int m_drag_origin_value { 0 };
    Gfx::IntPoint m_drag_origin;
    KnobSizeMode m_knob_size_mode { KnobSizeMode::Fixed };
};

class VerticalSlider final : public Slider {
    C_OBJECT(VerticalSlider);

public:
    virtual ~VerticalSlider() override { }

private:
    VerticalSlider()
        : Slider(Orientation::Vertical)
    {
    }
};

class HorizontalSlider final : public Slider {
    C_OBJECT(HorizontalSlider);

public:
    virtual ~HorizontalSlider() override { }

private:
    HorizontalSlider()
        : Slider(Orientation::Horizontal)
    {
    }
};

}
