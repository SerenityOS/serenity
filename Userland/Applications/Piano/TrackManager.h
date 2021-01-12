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
    int time() const { return m_time; }

    void fill_buffer(Span<Sample>);
    void reset();
    void set_should_loop(bool b) { m_should_loop = b; }
    void set_note_current_octave(int note, Switch);
    void set_octave(Direction);
    void set_octave(int octave);
    void add_track();
    void next_track();

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
