/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TrackManager.h"
#include "Music.h"
#include <AK/NoAllocationGuard.h>
#include <AK/NonnullRefPtr.h>
#include <AK/TypedTransfer.h>
#include <LibDSP/Effects.h>
#include <LibDSP/Synthesizers.h>

TrackManager::TrackManager()
    : m_transport(make_ref_counted<DSP::Transport>(120, 4))
    , m_keyboard(make_ref_counted<DSP::Keyboard>(m_transport))
    , m_temporary_track_buffer(FixedArray<DSP::Sample>::must_create_but_fixme_should_propagate_errors(sample_count))
{
    add_track();
}

void TrackManager::time_forward(int amount)
{
    int new_value = (static_cast<int>(m_transport->time()) + amount) % roll_length;

    if (new_value < 0) { // If the new time value is negative add roll_length to wrap around
        m_transport->set_time(roll_length + new_value);
    } else {
        m_transport->set_time(new_value);
    }
}

void TrackManager::fill_buffer(FixedArray<DSP::Sample>& buffer)
{
    NoAllocationGuard guard;
    VERIFY(buffer.size() == m_temporary_track_buffer.size());
    size_t sample_count = buffer.size();
    // No need to zero the temp buffer as the track overwrites it anyways.
    buffer.fill_with({});

    for (auto& track : m_tracks) {
        track->current_signal(m_temporary_track_buffer);
        for (size_t i = 0; i < sample_count; ++i)
            buffer[i] += m_temporary_track_buffer[i];
    }

    m_transport->set_time(m_transport->time() + sample_count);
    // FIXME: This should be handled automatically by Transport. It will also advance slightly past the loop point if we're unlucky.
    if (m_transport->time() >= roll_length)
        m_transport->set_time(0);
}

void TrackManager::reset()
{
    m_transport->set_time(0);
}

void TrackManager::add_track()
{
    auto new_track = make_ref_counted<DSP::NoteTrack>(m_transport, m_keyboard);
    MUST(new_track->resize_internal_buffers_to(m_temporary_track_buffer.size()));
    new_track->add_processor(make_ref_counted<DSP::Synthesizers::Classic>(m_transport));
    new_track->add_processor(make_ref_counted<DSP::Effects::Delay>(m_transport));
    new_track->add_clip(0, roll_length);
    m_tracks.append(move(new_track));
}

int TrackManager::next_track_index() const
{
    auto next_track_index = m_current_track + 1;
    if (next_track_index >= m_tracks.size())
        return 0;
    return static_cast<int>(next_track_index);
}
