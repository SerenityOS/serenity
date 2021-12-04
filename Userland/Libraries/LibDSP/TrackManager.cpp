/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TrackManager.h"
#include "AK/OwnPtr.h"
#include "AK/RefPtr.h"
#include "Track.h"
#include <AK/NonnullRefPtr.h>
#include <AK/Try.h>
#include <LibAudio/Sample.h>

namespace LibDSP {

Span<Sample> TrackManager::wait_for_front_buffer()
{
    m_front_buffer_ready_mutex.lock();
    m_front_buffer_ready_condition.wait();
    m_front_buffer_ready_mutex.unlock();

    return m_front_buffer.span();
}

void TrackManager::fill_one_buffer()
{
    m_back_buffer.clear_with_capacity();

    compute_samples();

    m_front_buffer.swap(m_back_buffer);

    // FIXME: Maybe a violation of the Rules of Audio Programming?
    m_front_buffer_ready_mutex.lock();
    m_front_buffer_ready_condition.signal();
    m_front_buffer_ready_mutex.unlock();
}

void TrackManager::compute_samples()
{
    m_back_buffer.clear_with_capacity();

    for (auto& track : m_tracks) {
        m_temporary_track_buffer.clear_with_capacity();
        track.compute_samples(m_temporary_track_buffer);
        //FIXME: Mixing
        Audio::samples_accumulative_sum(m_back_buffer, m_temporary_track_buffer);
    }
}

void TrackManager::reset()
{
    m_front_buffer.clear_with_capacity();
    m_back_buffer.clear_with_capacity();

    m_transport->time() = 0;

    // FIXME: Track reset?
}

void TrackManager::add_track(Track&& to_add)
{
    m_tracks.append(AK::adopt_own_if_nonnull<Track>(&to_add).release_nonnull());
}

void TrackManager::set_buffer_size(size_t size)
{
    // FIXME: The current APIs prevent OOM handling here.
    auto new_front_buffer = FixedArray<Sample>(size);
    auto new_back_buffer = FixedArray<Sample>(size);
    auto new_temporary_track_buffer = FixedArray<Sample>(size);
    m_front_buffer = new_front_buffer;
    m_back_buffer = new_back_buffer;
    m_temporary_track_buffer = new_temporary_track_buffer;
}

}
