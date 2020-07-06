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
#include "TrackManager.h"
#include <LibGUI/Painter.h>
#include <LibGUI/ScrollBar.h>
#include <math.h>

constexpr int note_height = 20;
constexpr int max_note_width = note_height * 2;
constexpr int roll_height = note_count * note_height;
constexpr int horizontal_scroll_sensitivity = 20;
constexpr int max_zoom = 1 << 8;

RollWidget::RollWidget(TrackManager& track_manager)
    : m_track_manager(track_manager)
{
    set_should_hide_unnecessary_scrollbars(true);
    set_content_size({ 0, roll_height });
    vertical_scrollbar().set_value(roll_height / 2);
}

RollWidget::~RollWidget()
{
}

void RollWidget::paint_event(GUI::PaintEvent& event)
{
    m_roll_width = widget_inner_rect().width() * m_zoom_level;
    set_content_size({ m_roll_width, roll_height });

    // Divide the roll by the maximum note width. If we get fewer notes than
    // our time signature requires, round up. Otherwise, round down to the
    // nearest x*(2^y), where x is the base number of notes of our time
    // signature. In other words, find a number that is a double of our time
    // signature. For 4/4 that would be 16, 32, 64, 128 ...
    m_num_notes = m_roll_width / max_note_width;
    int time_signature_notes = beats_per_bar * notes_per_beat;
    if (m_num_notes < time_signature_notes)
        m_num_notes = time_signature_notes;
    else
        m_num_notes = time_signature_notes * pow(2, static_cast<int>(log2(m_num_notes / time_signature_notes)));
    m_note_width = static_cast<double>(m_roll_width) / m_num_notes;

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

    int x_offset = horizontal_scrollbar().value();
    int horizontal_note_offset_remainder = fmod(x_offset, m_note_width);
    int horizontal_paint_area = widget_inner_rect().width() + horizontal_note_offset_remainder;
    if (fmod(horizontal_paint_area, m_note_width) != 0)
        horizontal_paint_area += m_note_width;
    int horizontal_notes_to_paint = horizontal_paint_area / m_note_width;

    GUI::Painter painter(*this);
    painter.translate(frame_thickness(), frame_thickness());
    painter.add_clip_rect(event.rect());
    painter.translate(-horizontal_note_offset_remainder, -note_offset_remainder);

    for (int y = 0; y < notes_to_paint; ++y) {
        int y_pos = y * note_height;
        for (int x = 0; x < horizontal_notes_to_paint; ++x) {
            // This is needed to avoid rounding errors. You can't just use
            // m_note_width as the width.
            int x_pos = x * m_note_width;
            int next_x_pos = (x + 1) * m_note_width;
            int distance_to_next_x = next_x_pos - x_pos;
            Gfx::IntRect rect(x_pos, y_pos, distance_to_next_x, note_height);

            if (key_pattern[key_pattern_index] == Black)
                painter.fill_rect(rect, Color::LightGray);
            else
                painter.fill_rect(rect, Color::White);

            painter.draw_line(rect.top_right(), rect.bottom_right(), Color::Black);
            painter.draw_line(rect.bottom_left(), rect.bottom_right(), Color::Black);
        }

        if (--key_pattern_index == -1)
            key_pattern_index = notes_per_octave - 1;
    }

    painter.translate(-x_offset, -y_offset);
    painter.translate(horizontal_note_offset_remainder, note_offset_remainder);

    for (int note = note_count - (note_offset + notes_to_paint); note <= (note_count - 1) - note_offset; ++note) {
        for (auto roll_note : m_track_manager.current_track().roll_notes(note)) {
            int x = m_roll_width * (static_cast<double>(roll_note.on_sample) / roll_length);
            int width = m_roll_width * (static_cast<double>(roll_note.length()) / roll_length);
            if (x + width < x_offset || x > x_offset + widget_inner_rect().width())
                continue;
            if (width < 2)
                width = 2;

            int y = ((note_count - 1) - note) * note_height;
            int height = note_height;

            Gfx::IntRect rect(x, y, width, height);
            painter.fill_rect(rect, note_pressed_color);
            painter.draw_rect(rect, Color::Black);
        }
    }

    int x = m_roll_width * (static_cast<double>(m_track_manager.time()) / roll_length);
    if (x > x_offset && x <= x_offset + widget_inner_rect().width())
        painter.draw_line({ x, 0 }, { x, roll_height }, Gfx::Color::Black);

    GUI::Frame::paint_event(event);
}

void RollWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!widget_inner_rect().contains(event.x(), event.y()))
        return;

    int y = (event.y() + vertical_scrollbar().value()) - frame_thickness();
    y /= note_height;

    // There's a case where we can't just use x / m_note_width. For example, if
    // your m_note_width is 3.1 you will have a rect starting at 3. When that
    // leftmost pixel of the rect is clicked you will do 3 / 3.1 which is 0
    // and not 1. We can avoid that case by shifting x by 1 if m_note_width is
    // fractional, being careful not to shift out of bounds.
    int x = (event.x() + horizontal_scrollbar().value()) - frame_thickness();
    bool note_width_is_fractional = m_note_width - static_cast<int>(m_note_width) != 0;
    bool x_is_not_last = x != widget_inner_rect().width() - 1;
    if (note_width_is_fractional && x_is_not_last)
        ++x;
    x /= m_note_width;

    int note = (note_count - 1) - y;
    u32 on_sample = roll_length * (static_cast<double>(x) / m_num_notes);
    u32 off_sample = (roll_length * (static_cast<double>(x + 1) / m_num_notes)) - 1;
    m_track_manager.current_track().set_roll_note(note, on_sample, off_sample);

    update();
}

// FIXME: Implement zoom and horizontal scroll events in LibGUI, not here.
void RollWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.modifiers() & KeyModifier::Mod_Shift) {
        horizontal_scrollbar().set_value(horizontal_scrollbar().value() + (event.wheel_delta() * horizontal_scroll_sensitivity));
        return;
    }

    if (!(event.modifiers() & KeyModifier::Mod_Ctrl)) {
        GUI::ScrollableWidget::mousewheel_event(event);
        return;
    }

    double multiplier = event.wheel_delta() >= 0 ? 0.5 : 2;

    if (m_zoom_level * multiplier > max_zoom)
        return;

    if (m_zoom_level * multiplier < 1) {
        if (m_zoom_level == 1)
            return;
        m_zoom_level = 1;
    } else {
        m_zoom_level *= multiplier;
    }

    int absolute_x_of_pixel_at_cursor = horizontal_scrollbar().value() + event.position().x();
    int absolute_x_of_pixel_at_cursor_after_resize = absolute_x_of_pixel_at_cursor * multiplier;
    int new_scrollbar = absolute_x_of_pixel_at_cursor_after_resize - event.position().x();

    m_roll_width = widget_inner_rect().width() * m_zoom_level;
    set_content_size({ m_roll_width, roll_height });

    horizontal_scrollbar().set_value(new_scrollbar);
}
