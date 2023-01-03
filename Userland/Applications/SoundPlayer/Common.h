/*
 * Copyright (c) 2021, Cesar Torres <shortanemoia@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Slider.h>

class AutoSlider final : public GUI::Slider {
    C_OBJECT(AutoSlider)
public:
    ~AutoSlider() override = default;
    Function<void(int)> on_knob_released;
    virtual void set_value(int value, GUI::AllowCallback allow_callback = GUI::AllowCallback::Yes, DoClamp do_clamp = DoClamp::Yes) override
    {
        m_in_drag_value = value;
        if (!knob_dragging() && !mouse_is_down())
            GUI::Slider::set_value(value, allow_callback, do_clamp);
    }

    bool mouse_is_down() const { return m_mouse_is_down; }

private:
    AutoSlider(Orientation orientation)
        : GUI::Slider(orientation)
    {
    }

    virtual void mousedown_event(GUI::MouseEvent& event) override
    {
        m_mouse_is_down = true;
        GUI::Slider::mousedown_event(event);
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        m_mouse_is_down = false;
        set_value(m_in_drag_value);
        if (on_knob_released && is_enabled())
            on_knob_released(value());

        GUI::Slider::mouseup_event(event);
    }

    bool m_mouse_is_down { false };
    // Keeps track of the value while we're dragging
    int m_in_drag_value { 0 };
};
