/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RollWidget.h"
#include "TrackManager.h"
#include <AK/IntegralMath.h>
#include <LibDSP/Music.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>

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
        m_num_notes = time_signature_notes * AK::exp2(AK::log2(m_num_notes / time_signature_notes));
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
    int horizontal_note_offset_remainder = static_cast<int>(AK::fmod((double)x_offset, m_note_width));
    int horizontal_paint_area = widget_inner_rect().width() + horizontal_note_offset_remainder;
    if (AK::fmod((double)horizontal_paint_area, m_note_width) != 0.)
        horizontal_paint_area += m_note_width;
    int horizontal_notes_to_paint = horizontal_paint_area / m_note_width;

    GUI::Painter painter(*this);

    // Draw the background, if necessary.
    if (viewport_changed() || paint_area != m_background->height()) {
        m_background = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, Gfx::IntSize(m_roll_width, paint_area)).release_value_but_fixme_should_propagate_errors();
        Gfx::Painter background_painter(*m_background);

        background_painter.translate(frame_thickness(), frame_thickness());
        background_painter.translate(-horizontal_note_offset_remainder, -note_offset_remainder);

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
                    background_painter.fill_rect(rect, Color::LightGray);
                else
                    background_painter.fill_rect(rect, Color::White);

                rect.shrink(0, 1, 1, 0);
                background_painter.draw_line(rect.top_right(), rect.bottom_right(), Color::Black);
                background_painter.draw_line(rect.bottom_left(), rect.bottom_right(), Color::Black);
            }

            if (--key_pattern_index == -1)
                key_pattern_index = notes_per_octave - 1;
        }

        background_painter.translate(-x_offset, -y_offset);
        background_painter.translate(horizontal_note_offset_remainder, note_offset_remainder);

        m_prev_zoom_level = m_zoom_level;
        m_prev_scroll_x = horizontal_scrollbar().value();
        m_prev_scroll_y = vertical_scrollbar().value();
    }

    painter.blit(Gfx::IntPoint(0, 0), *m_background, m_background->rect());

    // Draw the notes, mouse interaction, and time position.
    painter.translate(frame_thickness(), frame_thickness());
    painter.add_clip_rect(event.rect());
    painter.translate(-horizontal_note_offset_remainder, -note_offset_remainder);

    for (int y = 0; y < notes_to_paint; ++y) {
        int y_pos = y * note_height;

        int note = (note_count - note_offset - 1) - y;
        for (int x = 0; x < horizontal_notes_to_paint; ++x) {
            // This is needed to avoid rounding errors. You can't just use
            // m_note_width as the width.
            int x_pos = x * m_note_width;
            int next_x_pos = (x + 1) * m_note_width;
            int distance_to_next_x = next_x_pos - x_pos;
            Gfx::IntRect rect(x_pos, y_pos, distance_to_next_x, note_height);

            if (static_cast<size_t>(note) < DSP::note_frequencies.size() && m_track_manager.keyboard()->is_pressed(note))
                painter.fill_rect(rect, note_pressed_color.with_alpha(128));
        }
    }

    painter.translate(-x_offset, -y_offset);
    painter.translate(horizontal_note_offset_remainder, note_offset_remainder);

    for (auto const& clip : m_track_manager.current_track()->notes()) {
        for (auto const& roll_note : clip->notes()) {
            int y = ((note_count - 1) - roll_note.pitch) * note_height;
            int x = m_roll_width * (static_cast<double>(roll_note.on_sample) / roll_length);
            int width = m_roll_width * (static_cast<double>(roll_note.length()) / roll_length);
            if (x + width < x_offset || x > x_offset + widget_inner_rect().width())
                continue;
            if (width < 2)
                width = 2;

            int height = note_height;

            Gfx::IntRect rect(x, y, width, height);
            painter.fill_rect(rect, note_pressed_color);
            painter.draw_rect(rect, Color::Black);
        }
    }

    for (int note = note_count - (note_offset + notes_to_paint); note <= (note_count - 1) - note_offset; ++note) {
        int y = ((note_count - 1) - note) * note_height;
        Gfx::IntRect note_name_rect(3, y, 1, note_height);
        auto note_name = note_names[note % notes_per_octave];

        painter.draw_text(note_name_rect, note_name, Gfx::TextAlignment::CenterLeft);
        note_name_rect.translate_by(Gfx::FontDatabase::default_font().width(note_name) + 2, 0);
        if (note % notes_per_octave == 0)
            painter.draw_text(note_name_rect, ByteString::formatted("{}", note / notes_per_octave + 1), Gfx::TextAlignment::CenterLeft);
    }

    int x = m_roll_width * (static_cast<double>(m_track_manager.transport()->time()) / roll_length);
    if (x > x_offset && x <= x_offset + widget_inner_rect().width())
        painter.draw_line({ x, 0 }, { x, roll_height }, Gfx::Color::Black);

    GUI::Frame::paint_event(event);
}

bool RollWidget::viewport_changed() const
{
    // height is complicated to check, will be done in paint_event
    return m_background.is_null()
        || m_roll_width != m_background->width()
        || m_prev_scroll_x != horizontal_scrollbar().value() || m_prev_scroll_y != vertical_scrollbar().value()
        || m_prev_zoom_level != m_zoom_level;
}

void RollWidget::mousedown_event(GUI::MouseEvent& event)
{
    if (!widget_inner_rect().contains(event.x(), event.y()))
        return;

    if (event.button() == GUI::MouseButton::Secondary) {
        auto const time = roll_length * (static_cast<double>(get_note_for_x(event.x())) / m_num_notes);
        auto const note = m_track_manager.current_track()->note_at(time, get_pitch_for_y(event.y()));

        if (note.has_value()) {
            m_track_manager.current_track()->remove_note(note.value());
            update();
        }
        return;
    }

    m_mousedown_event = event;
}

void RollWidget::mouseup_event(GUI::MouseEvent& event)
{
    mousemove_event(event);
    m_mousedown_event = {};
}

u8 RollWidget::get_pitch_for_y(int y) const
{
    return (note_count - 1) - ((y + vertical_scrollbar().value()) - frame_thickness()) / note_height;
}

int RollWidget::get_note_for_x(int x) const
{
    // There's a case where we can't just use x / m_note_width. For example, if
    // your m_note_width is 3.1 you will have a rect starting at 3. When that
    // leftmost pixel of the rect is clicked you will do 3 / 3.1 which is 0
    // and not 1. We can avoid that case by shifting x by 1 if m_note_width is
    // fractional, being careful not to shift out of bounds.
    x = (x + horizontal_scrollbar().value()) - frame_thickness();
    bool const note_width_is_fractional = m_note_width - static_cast<int>(m_note_width) != 0;
    bool const x_is_not_last = x != widget_inner_rect().width() - 1;
    if (note_width_is_fractional && x_is_not_last)
        ++x;
    x /= m_note_width;
    return clamp(x, 0, m_num_notes - 1);
}

void RollWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_mousedown_event.has_value())
        return;

    if (m_mousedown_event.value().button() == GUI::MouseButton::Primary) {
        int const x_start = get_note_for_x(m_mousedown_event.value().x());
        int const x_end = get_note_for_x(event.x());

        u32 const on_sample = round(roll_length * (static_cast<double>(min(x_start, x_end)) / m_num_notes));
        u32 const off_sample = round(roll_length * (static_cast<double>(max(x_start, x_end) + 1) / m_num_notes)) - 1;
        auto const note = RollNote { on_sample, off_sample, get_pitch_for_y(m_mousedown_event.value().y()), 127 };

        m_track_manager.current_track()->set_note(note);
        update();
    }
}

// FIXME: Implement zoom and horizontal scroll events in LibGUI, not here.
void RollWidget::mousewheel_event(GUI::MouseEvent& event)
{
    if (event.modifiers() & KeyModifier::Mod_Shift) {
        horizontal_scrollbar().increase_slider_by(event.wheel_delta_y() * horizontal_scroll_sensitivity);
        return;
    }

    if (event.wheel_delta_x() != 0) {
        horizontal_scrollbar().increase_slider_by(event.wheel_delta_x() * horizontal_scroll_sensitivity);
        return;
    }

    if (!(event.modifiers() & KeyModifier::Mod_Ctrl)) {
        GUI::AbstractScrollableWidget::mousewheel_event(event);
        return;
    }

    double multiplier = event.wheel_delta_y() >= 0 ? 0.5 : 2;

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
