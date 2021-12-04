/*
 * Copyright (c) 2018-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Array.h>
#include <AK/Atomic.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibDSP/Keyboard.h>
#include <LibDSP/Music.h>
#include <LibDSP/Track.h>
#include <LibThreading/ConditionVariable.h>

namespace LibDSP {

class TrackManager : public RefCounted<TrackManager> {
    AK_MAKE_NONCOPYABLE(TrackManager);
    AK_MAKE_NONMOVABLE(TrackManager);

public:
    TrackManager(Transport transport, size_t buffer_size)
        : m_transport(transport)
        , m_keyboard(make_ref_counted<Keyboard>(m_transport))
        , m_front_buffer_ready_condition(m_front_buffer_ready_mutex)
        , m_front_buffer(buffer_size)
        , m_back_buffer(buffer_size)
        , m_temporary_track_buffer(buffer_size)
    {
    }
    ~TrackManager() { }

    // Called from the audio thread. After computing, signals buffer availability.
    void fill_one_buffer();
    // Called from the IPC thread.
    Span<Sample> wait_for_front_buffer();

    Span<Sample> front_buffer() { return m_front_buffer.span(); }

    void add_track(Track&&);
    Track const& track_at(size_t index) const
    {
        VERIFY(index < m_tracks.size());
        return m_tracks[index];
    }
    Track& track_at(size_t index)
    {
        VERIFY(index < m_tracks.size());
        return m_tracks[index];
    }
    void reset();

    size_t buffer_size() const { return m_front_buffer.size(); }
    void set_buffer_size(size_t size);

    NonnullRefPtr<Keyboard> keyboard() { return m_keyboard; }

private:
    void compute_samples();

    NonnullRefPtr<Transport> m_transport;
    NonnullRefPtr<Keyboard> m_keyboard;
    NonnullOwnPtrVector<Track> m_tracks;

    // This is accessed from the IPC thread and the audio thread.
    Threading::Mutex m_front_buffer_ready_mutex;
    Threading::ConditionVariable m_front_buffer_ready_condition;
    FixedArray<Sample> m_front_buffer;
    FixedArray<Sample> m_back_buffer;

    FixedArray<Sample> m_temporary_track_buffer;
};
}
