/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
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

#include "RollWidget.h"
#include "AudioEngine.h"
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>

constexpr int note_height = 20;
constexpr int roll_height = note_count * note_height;

RollWidget::RollWidget(AudioEngine& audio_engine)
    : m_audio_engine(audio_engine)
{
    set_frame_thickness(2);
    set_frame_shadow(Gfx::FrameShadow::Sunken);
    set_frame_shape(Gfx::FrameShape::Container);

    set_should_hide_unnecessary_scrollbars(true);
    set_content_size({ 0, roll_height });
    vertical_scrollbar().set_value(roll_height / 2);
}

RollWidget::~RollWidget()
{
}

void RollWidget::paint_event(GUI::PaintEvent& event)
{
    int roll_width = widget_inner_rect().width();
    double note_width = static_cast<double>(roll_width) / horizontal_notes;

    set_content_size({ roll_width, roll_height });

    // This calculates the minimum number of rows needed. We account for a
    // partial row at the top and/or bottom.
    int y_offset = vertical_scrollbar().value();
    int note_offset = y_offset / note_height;
    int note_offset_remainder = y_offset % note_height;
    int paint_area = widget_inner_rect().height() + note_offset_remainder;
    if (paint_area % note_height != 0)
        paint_area += note_height;
    int notes_to_paint = paint_area / note_height;
    int key_pattern_index = (notes_per_octave - 1) - (note_offset % notes_per_octave);

    GUI::Painter painter(*this);
    painter.translate(frame_thickness(), frame_thickness());
    painter.translate(0, -note_offset_remainder);

    for (int y = 0; y < notes_to_paint; ++y) {
        int y_pos = y * note_height;
        for (int x = 0; x < horizontal_notes; ++x) {
            // This is needed to avoid rounding errors. You can't just use
            // note_width as the width.
            int x_pos = x * note_width;
            int next_x_pos = (x + 1) * note_width;
            int distance_to_next_x = next_x_pos - x_pos;
            Gfx::Rect rect(x_pos, y_pos, distance_to_next_x, note_height);

            if (m_audio_engine.roll_note(y + note_offset, x) == On)
                painter.fill_rect(rect, note_pressed_color);
            else if (x == m_audio_engine.current_column())
                painter.fill_rect(rect, column_playing_color);
            else if (key_pattern[key_pattern_index] == Black)
                painter.fill_rect(rect, Color::LightGray);
            else
                painter.fill_rect(rect, Color::White);

            painter.draw_line(rect.top_right(), rect.bottom_right(), Color::Black);
            painter.draw_line(rect.bottom_left(), rect.bottom_right(), Color::Black);
        }

        if (--key_pattern_index == -1)
            key_pattern_index = notes_per_octave - 1;
    }

    GUI::Frame::paint_event(event);
}

void RollWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!widget_inner_rect().contains(event.x(), event.y()))
        return;

    int roll_width = widget_inner_rect().width();
    double note_width = static_cast<double>(roll_width) / horizontal_notes;

    int y = (event.y() + vertical_scrollbar().value()) - frame_thickness();
    y /= note_height;

    // There's a case where we can't just use x / note_width. For example, if
    // your note_width is 3.1 you will have a rect starting at 3. When that
    // leftmost pixel of the rect is clicked you will do 3 / 3.1 which is 0
    // and not 1. We can avoid that case by shifting x by 1 if note_width is
    // fractional, being careful not to shift out of bounds.
    int x = event.x() - frame_thickness();
    bool note_width_is_fractional = note_width - static_cast<int>(note_width) != 0;
    bool x_is_not_last = x != widget_inner_rect().width() - 1;
    if (note_width_is_fractional && x_is_not_last)
        ++x;
    x /= note_width;

    m_audio_engine.set_roll_note(y, x, m_audio_engine.roll_note(y, x) == On ? Off : On);

    update();
}
