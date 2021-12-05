/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include "Track.h"
#include <AK/Array.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/Vector.h>

class TrackManager {
    AK_MAKE_NONCOPYABLE(TrackManager);
    AK_MAKE_NONMOVABLE(TrackManager);

public:
    TrackManager();
    ~TrackManager();

    Track& current_track() { return *m_tracks[m_current_track]; }
    Span<const Sample> buffer() const { return m_current_front_buffer; }
    int octave() const { return m_octave; }
    int octave_base() const { return (m_octave - octave_min) * 12; }
    int track_count() { return m_tracks.size(); };
    void set_current_track(size_t track_index)
    {
        VERIFY((int)track_index < track_count());
        m_current_track = track_index;
    }

    int time() const { return m_time; }
    void time_forward(int amount);

    void fill_buffer(Span<Sample>);
    void reset();
    void set_keyboard_note(int note, Switch note_switch);
    void set_should_loop(bool b) { m_should_loop = b; }
    void set_octave(Direction);
    void set_octave(int octave);
    void add_track();
    int next_track_index();

private:
    Vector<NonnullOwnPtr<Track>> m_tracks;
    size_t m_current_track { 0 };

    Array<Sample, sample_count> m_front_buffer;
    Array<Sample, sample_count> m_back_buffer;
    Span<Sample> m_current_front_buffer { m_front_buffer.span() };
    Span<Sample> m_current_back_buffer { m_back_buffer.span() };

    int m_octave { 4 };

    u32 m_time { 0 };

    bool m_should_loop { true };
};
