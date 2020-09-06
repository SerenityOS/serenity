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

#include "TrackManager.h"

TrackManager::TrackManager()
{
    add_track();
}

TrackManager::~TrackManager()
{
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

void TrackManager::set_note_current_octave(int note, Switch switch_note)
{
    current_track().set_note(note + octave_base(), switch_note);
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

void TrackManager::next_track()
{
    if (++m_current_track >= m_tracks.size())
        m_current_track = 0;
}
