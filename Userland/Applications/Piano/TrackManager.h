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
#include "Track.h"
#include <AK/Array.h>
#include <AK/Noncopyable.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibDSP/Keyboard.h>

class TrackManager {
    AK_MAKE_NONCOPYABLE(TrackManager);
    AK_MAKE_NONMOVABLE(TrackManager);

public:
    TrackManager();
    ~TrackManager() = default;

    Track& current_track() { return *m_tracks[m_current_track]; }
    Span<const Sample> buffer() const { return m_current_front_buffer; }
    int track_count() { return m_tracks.size(); };
    void set_current_track(size_t track_index)
    {
        VERIFY((int)track_index < track_count());
        auto old_track = m_current_track;
        m_current_track = track_index;
        m_tracks[old_track]->set_active(false);
        m_tracks[m_current_track]->set_active(true);
    }

    NonnullRefPtr<DSP::Transport> transport() const { return m_transport; }
    NonnullRefPtr<DSP::Keyboard> keyboard() const { return m_keyboard; }
    // Legacy API, do not add new users.
    void time_forward(int amount);

    void fill_buffer(Span<Sample>);
    void reset();
    void set_keyboard_note(int note, DSP::Keyboard::Switch note_switch);
    void set_should_loop(bool b) { m_should_loop = b; }
    void add_track();
    int next_track_index() const;

private:
    Vector<NonnullOwnPtr<Track>> m_tracks;
    NonnullRefPtr<DSP::Transport> m_transport;
    NonnullRefPtr<DSP::Keyboard> m_keyboard;
    size_t m_current_track { 0 };

    Array<Sample, sample_count> m_front_buffer;
    Array<Sample, sample_count> m_back_buffer;
    Span<Sample> m_current_front_buffer { m_front_buffer.span() };
    Span<Sample> m_current_back_buffer { m_back_buffer.span() };

    bool m_should_loop { true };
};
