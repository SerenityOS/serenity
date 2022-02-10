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
#include <LibGUI/Painter.h>
#include <LibGUI/Scrollbar.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>

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
        m_background = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRx8888, Gfx::IntSize(m_roll_width, paint_area)).release_value_but_fixme_should_propagate_errors();
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

            if (keys_widget() && keys_widget()->note_is_set(note))
                painter.fill_rect(rect, note_pressed_color.with_alpha(128));
        }
    }

    painter.translate(-x_offset, -y_offset);
    painter.translate(horizontal_note_offset_remainder, note_offset_remainder);

    for (int note = note_count - (note_offset + notes_to_paint); note <= (note_count - 1) - note_offset; ++note) {
        int y = ((note_count - 1) - note) * note_height;
        for (auto roll_note : m_track_manager.current_track().roll_notes(note)) {
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

        Gfx::IntRect note_name_rect(3, y, 1, note_height);
        const char* note_name = note_names[note % notes_per_octave];

        painter.draw_text(note_name_rect, note_name, Gfx::TextAlignment::CenterLeft);
        note_name_rect.translate_by(Gfx::FontDatabase::default_font().width(note_name) + 2, 0);
        if (note % notes_per_octave == 0)
            painter.draw_text(note_name_rect, String::formatted("{}", note / notes_per_octave + 1), Gfx::TextAlignment::CenterLeft);
    }

    int x = m_roll_width * (static_cast<double>(m_track_manager.time()) / roll_length);
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

    m_note_drag_start = event.position();

    int y = (m_note_drag_start.value().y() + vertical_scrollbar().value()) - frame_thickness();
    y /= note_height;
    m_drag_note = (note_count - 1) - y;

    mousemove_event(event);
}

void RollWidget::mousemove_event(GUI::MouseEvent& event)
{
    if (!m_note_drag_start.has_value())
        return;

    if (m_note_drag_location.has_value()) {
        // Clear previous note
        m_track_manager.current_track().set_roll_note(m_drag_note, m_note_drag_location.value().on_sample, m_note_drag_location.value().off_sample);
    }

    auto get_note_x = [&](int x0) {
        // There's a case where we can't just use x / m_note_width. For example, if
        // your m_note_width is 3.1 you will have a rect starting at 3. When that
        // leftmost pixel of the rect is clicked you will do 3 / 3.1 which is 0
        // and not 1. We can avoid that case by shifting x by 1 if m_note_width is
        // fractional, being careful not to shift out of bounds.
        int x = (x0 + horizontal_scrollbar().value()) - frame_thickness();
        bool note_width_is_fractional = m_note_width - static_cast<int>(m_note_width) != 0;
        bool x_is_not_last = x != widget_inner_rect().width() - 1;
        if (note_width_is_fractional && x_is_not_last)
            ++x;
        x /= m_note_width;
        return clamp(x, 0, m_num_notes - 1);
    };

    int x0 = get_note_x(m_note_drag_start.value().x());
    int x1 = get_note_x(event.x());

    u32 on_sample = roll_length * (static_cast<double>(min(x0, x1)) / m_num_notes);
    u32 off_sample = (roll_length * (static_cast<double>(max(x0, x1) + 1) / m_num_notes)) - 1;
    m_track_manager.current_track().set_roll_note(m_drag_note, on_sample, off_sample);
    m_note_drag_location = RollNote { on_sample, off_sample, (u8)m_drag_note, 0 };

    update();
}

void RollWidget::mouseup_event([[maybe_unused]] GUI::MouseEvent& event)
{
    m_note_drag_start = {};
    m_note_drag_location = {};
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
