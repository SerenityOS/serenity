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

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Slider.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

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

AbstractSlider::~AbstractSlider()
{
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

void AbstractSlider::set_value(int value)
{
    value = clamp(value, m_min, m_max);
    if (m_value == value)
        return;
    m_value = value;
    update();

    if (on_change)
        on_change(m_value);
}

}
