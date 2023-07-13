/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, JJ Roberts-White <computerfido@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/FixedArray.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibDSP/Keyboard.h>
#include <LibDSP/Track.h>

class TrackManager {
    AK_MAKE_NONCOPYABLE(TrackManager);
    AK_MAKE_NONMOVABLE(TrackManager);

public:
    TrackManager();
    ~TrackManager() = default;

    NonnullRefPtr<DSP::NoteTrack> current_track() { return *m_tracks[m_current_track]; }
    size_t track_count() { return m_tracks.size(); }
    size_t current_track_index() const { return m_current_track; }
    void set_current_track(size_t track_index)
    {
        VERIFY(track_index < track_count());
        m_current_track = track_index;
    }
    Span<NonnullRefPtr<DSP::NoteTrack>> tracks() { return m_tracks.span(); }

    NonnullRefPtr<DSP::Transport> transport() const { return m_transport; }
    NonnullRefPtr<DSP::Keyboard> keyboard() const { return m_keyboard; }
    // Legacy API, do not add new users.
    void time_forward(int amount);

    void fill_buffer(FixedArray<DSP::Sample>&);
    void reset();
    void set_should_loop(bool b) { m_should_loop = b; }
    void add_track();
    int next_track_index() const;

private:
    Vector<NonnullRefPtr<DSP::NoteTrack>> m_tracks;
    NonnullRefPtr<DSP::Transport> m_transport;
    NonnullRefPtr<DSP::Keyboard> m_keyboard;
    size_t m_current_track { 0 };

    FixedArray<DSP::Sample> m_temporary_track_buffer;

    bool m_should_loop { true };
};
