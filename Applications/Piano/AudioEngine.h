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
#include <AK/FixedArray.h>
#include <AK/Noncopyable.h>
#include <AK/Queue.h>

class AudioEngine {
    AK_MAKE_NONCOPYABLE(AudioEngine)
    AK_MAKE_NONMOVABLE(AudioEngine)
public:
    AudioEngine();
    ~AudioEngine();

    const FixedArray<Sample>& buffer() const { return *m_front_buffer_ptr; }
    int octave() const { return m_octave; }
    int octave_base() const { return (m_octave - octave_min) * 12; }
    int wave() const { return m_wave; }
    int decay() const { return m_decay; }
    int delay() const { return m_delay; }
    int time() const { return m_time; }
    int tick() const { return m_tick; }

    void fill_buffer(FixedArray<Sample>& buffer);
    void set_note(int note, Switch);
    void set_note_current_octave(int note, Switch);
    void set_octave(Direction);
    void set_wave(int wave);
    void set_wave(Direction);
    void set_decay(int decay);
    void set_delay(int delay);

private:
    double sine(size_t note);
    double saw(size_t note);
    double square(size_t note);
    double triangle(size_t note);
    double noise() const;

    FixedArray<Sample> m_front_buffer { sample_count };
    FixedArray<Sample> m_back_buffer { sample_count };
    FixedArray<Sample>* m_front_buffer_ptr { &m_front_buffer };
    FixedArray<Sample>* m_back_buffer_ptr { &m_back_buffer };

    Queue<NonnullOwnPtr<FixedArray<Sample>>> m_delay_buffers;

    u8 m_note_on[note_count] { 0 };
    double m_power[note_count]; // Initialized lazily.
    double m_pos[note_count];   // Initialized lazily.

    int m_octave { 4 };
    int m_wave { first_wave };
    int m_decay;
    double m_decay_step;
    int m_delay { 0 };

    int m_time { 0 };
    int m_tick { 8 };
};
