/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include <LibCore/DateTime.h>
#include <LibCore/Timer.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>

class AnalogClockWidget final : public GUI::Widget {
    C_OBJECT(AnalogClockWidget)
public:
    virtual ~AnalogClockWidget();
    void set_timezone_offset(int offset) { m_timezone_offset = offset; }
    void tick_clock(Core::DateTime time)
    {
        // FIXME: Ideally we should get the right timezone time from the MenuApplet/Clock.
        //        Once the timezone support is added, remove this
        auto current_time = Core::DateTime::from_timestamp(time.timestamp() + (m_timezone_offset * 60));
        hour = current_time.hour();
        minute = current_time.minute();
        second = current_time.second();
        update();
    }

private:
    AnalogClockWidget()
    {
        set_width(160);
        set_height(160);
    }
    virtual void paint_event(GUI::PaintEvent&) override;

    static const unsigned hour_hand_length = 50;
    static const unsigned minute_hand_length = 63;
    static const unsigned second_hand_length = 62;

    unsigned hour { 12 };
    unsigned minute { 0 };
    unsigned second { 0 };
    Gfx::IntPoint clock_center_point { 79, 79 };
    Gfx::IntRect center_circle { 75, 75, 10, 10 };
    Gfx::IntRect rect_twelve { 72, 3, 16, 16 };
    Gfx::IntRect rect_three { 142, 72, 16, 16 };
    Gfx::IntRect rect_six { 72, 142, 16, 16 };
    Gfx::IntRect rect_nine { 2, 72, 16, 16 };
    int m_timezone_offset { 0 };
};
