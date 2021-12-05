/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TrackManager.h"
#include "Applications/Piano/Music.h"

TrackManager::TrackManager()
{
    add_track();
}

TrackManager::~TrackManager()
{
}

void TrackManager::time_forward(int amount)
{
    int new_value = (static_cast<int>(m_time) + amount) % roll_length;

    if (new_value < 0) { // If the new time value is negative add roll_length to wrap around
        m_time = roll_length + new_value;
    } else {
        m_time = new_value;
    }
}

void TrackManager::fill_buffer(Span<Sample> buffer)
{
    memset(buffer.data(), 0, buffer_size);

    for (size_t i = 0; i < buffer.size(); ++i) {
        for (auto& track : m_tracks)
            track->fill_sample(buffer[i]);

        if (++m_time >= roll_length) {
            m_time = 0;
            if (!m_should_loop)
                break;
        }
    }

    memcpy(m_current_back_buffer.data(), buffer.data(), buffer_size);
    swap(m_current_front_buffer, m_current_back_buffer);
}

void TrackManager::reset()
{
    memset(m_front_buffer.data(), 0, buffer_size);
    memset(m_back_buffer.data(), 0, buffer_size);

    m_current_front_buffer = m_front_buffer.span();
    m_current_back_buffer = m_back_buffer.span();

    m_time = 0;

    for (auto& track : m_tracks)
        track->reset();
}

void TrackManager::set_keyboard_note(int note, Switch note_switch)
{
    m_tracks[m_current_track]->set_keyboard_note(note, note_switch);
}

void TrackManager::set_octave(Direction direction)
{
    if (direction == Up) {
        if (m_octave < octave_max)
            ++m_octave;
    } else {
        if (m_octave > octave_min)
            --m_octave;
    }
}

void TrackManager::set_octave(int octave)
{
    if (octave <= octave_max && octave >= octave_min) {
        m_octave = octave;
    }
}

void TrackManager::add_track()
{
    m_tracks.append(make<Track>(m_time));
}

int TrackManager::next_track_index()
{
    auto next_track_index = m_current_track + 1;
    if (next_track_index >= m_tracks.size())
        return 0;
    else
        return next_track_index;
}
