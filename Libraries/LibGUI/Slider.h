/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <LibGUI/Widget.h>

namespace GUI {

class Slider : public Widget {
    C_OBJECT(Slider)
public:
    enum class KnobSizeMode {
        Fixed,
        Proportional,
    };

    virtual ~Slider() override;

    Orientation orientation() const { return m_orientation; }

    int value() const { return m_value; }
    int min() const { return m_min; }
    int max() const { return m_max; }
    int step() const { return m_step; }

    void set_range(int min, int max);
    void set_value(int);

    void set_min(int min) { set_range(min, max()); }
    void set_max(int max) { set_range(min(), max); }
    void set_step(int step) { m_step = step; }

    void set_knob_size_mode(KnobSizeMode mode) { m_knob_size_mode = mode; }
    KnobSizeMode knob_size_mode() const { return m_knob_size_mode; }

    int track_size() const { return 2; }
    int knob_fixed_primary_size() const { return 8; }
    int knob_secondary_size() const { return 20; }

    bool knob_dragging() const { return m_dragging; }
    Gfx::Rect knob_rect() const;

    Gfx::Rect inner_rect() const
    {
        if (orientation() == Orientation::Horizontal)
            return rect().shrunken(20, 0);
        return rect().shrunken(0, 20);
    }

    Function<void(int)> on_value_changed;

protected:
    explicit Slider(Widget* = nullptr);
    explicit Slider(Orientation, Widget* = nullptr);

    virtual void paint_event(PaintEvent&) override;
    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void mousewheel_event(MouseEvent&) override;
    virtual void leave_event(Core::Event&) override;
    virtual void change_event(Event&) override;

private:
    void set_knob_hovered(bool);

    int m_value { 0 };
    int m_min { 0 };
    int m_max { 100 };
    int m_step { 1 };
    bool m_knob_hovered { false };
    bool m_dragging { false };
    int m_drag_origin_value { 0 };
    Gfx::Point m_drag_origin;
    KnobSizeMode m_knob_size_mode { KnobSizeMode::Fixed };
    Orientation m_orientation { Orientation::Horizontal };
};

class VerticalSlider final : public Slider {
    C_OBJECT(VerticalSlider)
public:
    virtual ~VerticalSlider() override {}

private:
    explicit VerticalSlider(Widget* parent = nullptr)
        : Slider(Orientation::Vertical, parent)
    {
    }
};

class HorizontalSlider final : public Slider {
    C_OBJECT(HorizontalSlider)
public:
    virtual ~HorizontalSlider() override {}

private:
    explicit HorizontalSlider(Widget* parent = nullptr)
        : Slider(Orientation::Horizontal, parent)
    {
    }
};

}
