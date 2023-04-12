/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Palette.h>

namespace GUI {

AbstractSlider::AbstractSlider(Orientation orientation)
    : m_orientation(orientation)
{
    REGISTER_INT_PROPERTY("value", value, set_value);
    REGISTER_INT_PROPERTY("min", min, set_min);
    REGISTER_INT_PROPERTY("max", max, set_max);
    REGISTER_INT_PROPERTY("step", step, set_step);
    REGISTER_INT_PROPERTY("page_step", page_step, set_page_step);
    REGISTER_ENUM_PROPERTY("orientation", this->orientation, set_orientation, Orientation,
        { Orientation::Horizontal, "Horizontal" },
        { Orientation::Vertical, "Vertical" });
}

void AbstractSlider::set_orientation(Orientation value)
{
    if (m_orientation == value)
        return;
    m_orientation = value;
    update();
}

void AbstractSlider::set_page_step(int page_step)
{
    m_page_step = AK::max(0, page_step);
}

void AbstractSlider::set_range(int min, int max)
{
    VERIFY(min <= max);
    if (m_min == min && m_max == max)
        return;
    m_min = min;
    m_max = max;
    m_value = clamp(m_value, m_min, m_max);
    update();
}

void AbstractSlider::set_value(int value, AllowCallback allow_callback, DoClamp do_clamp)
{
    if (do_clamp == DoClamp::Yes)
        value = clamp(value, m_min, m_max);
    if (m_value == value)
        return;
    m_value = value;
    if (on_change && allow_callback == AllowCallback::Yes)
        on_change(m_value);
    update();
}

}
