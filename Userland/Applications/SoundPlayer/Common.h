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
    void set_value(int value)
    {
        if (!knob_dragging())
            GUI::Slider::set_value(value);
    }

protected:
    AutoSlider(Orientation orientation)
        : GUI::Slider(orientation)
    {
    }

    virtual void mouseup_event(GUI::MouseEvent& event) override
    {
        if (on_knob_released && is_enabled())
            on_knob_released(value());

        GUI::Slider::mouseup_event(event);
    }
};
