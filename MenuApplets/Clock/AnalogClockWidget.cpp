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

#include "AnalogClockWidget.h"
#include <LibGfx/Palette.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <math.h>

AnalogClockWidget::~AnalogClockWidget()
{
}

void AnalogClockWidget::paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    Gfx::IntRect bounds { 0, 0, width(), height() };
    painter.fill_ellipse(bounds, palette().base_text());
    auto width_thickness = width() / 15;
    auto height_thickness = height() / 15;
    bounds.shrink(width_thickness, height_thickness);
    painter.fill_ellipse(bounds, palette().button());

    painter.fill_ellipse(center_circle, palette().base_text());

    painter.draw_text(rect_twelve, StringView("12", 2), Gfx::TextAlignment::Center, palette().base_text());
    painter.draw_text(rect_three, StringView("3", 1), Gfx::TextAlignment::Center, palette().base_text());
    painter.draw_text(rect_six, StringView("6", 1), Gfx::TextAlignment::Center, palette().base_text());
    painter.draw_text(rect_nine, StringView("9", 1), Gfx::TextAlignment::Center, palette().base_text());

    auto second_degrees = second * 360.0 / 60;
    auto minute_degrees = (minute + second_degrees / 360.0) * 360.0 / 60;
    auto hour_degrees = (hour + minute_degrees / 360.0) * 360.0 / 12;

    auto radians = minute_degrees * M_PI / 180;
    Gfx::IntPoint minute_end_point {
        clock_center_point.x() + minute_hand_length * sin(radians),
        clock_center_point.y() - minute_hand_length * cos(radians),
    };
    painter.draw_line(clock_center_point, minute_end_point, palette().base_text(), 3); // Position Minutes Hand.

    radians = second_degrees * M_PI / 180;
    Gfx::IntPoint second_end_point {
        clock_center_point.x() + second_hand_length * sin(radians),
        clock_center_point.y() - second_hand_length * cos(radians)
    };
    painter.draw_line(clock_center_point, second_end_point, palette().base_text(), 3); // Position Seconds Hand.

    radians = hour_degrees * M_PI / 180;
    Gfx::IntPoint hour_end_point {
        clock_center_point.x() + hour_hand_length * sin(radians),
        clock_center_point.y() - hour_hand_length * cos(radians)
    };
    painter.draw_line(clock_center_point, hour_end_point, palette().base_text(), 3); // Position Hours Hand.
}
