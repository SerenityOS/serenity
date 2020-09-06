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
#include <AK/Noncopyable.h>
#include <AK/SinglyLinkedList.h>
#include <LibAudio/Buffer.h>

typedef AK::SinglyLinkedListIterator<SinglyLinkedList<RollNote>, RollNote> RollIter;

class Track {
    AK_MAKE_NONCOPYABLE(Track);
    AK_MAKE_NONMOVABLE(Track);

public:
    explicit Track(const u32& time);
    ~Track();

    const Vector<Audio::Sample>& recorded_sample() const { return m_recorded_sample; }
    const SinglyLinkedList<RollNote>& roll_notes(int note) const { return m_roll_notes[note]; }
    int wave() const { return m_wave; }
    int attack() const { return m_attack; }
    int decay() const { return m_decay; }
    int sustain() const { return m_sustain; }
    int release() const { return m_release; }
    int delay() const { return m_delay; }

    void fill_sample(Sample& sample);
    void reset();
    String set_recorded_sample(const StringView& path);
    void set_note(int note, Switch);
    void set_roll_note(int note, u32 on_sample, u32 off_sample);
    void set_wave(int wave);
    void set_wave(Direction);
    void set_attack(int attack);
    void set_decay(int decay);
    void set_sustain(int sustain);
    void set_release(int release);
    void set_delay(int delay);

private:
    Audio::Sample sine(size_t note);
    Audio::Sample saw(size_t note);
    Audio::Sample square(size_t note);
    Audio::Sample triangle(size_t note);
    Audio::Sample noise() const;
    Audio::Sample recorded_sample(size_t note);

    void sync_roll(int note);
    void set_sustain_impl(int sustain);

    Vector<Sample> m_delay_buffer;

    Vector<Audio::Sample> m_recorded_sample;

    u8 m_note_on[note_count] { 0 };
    double m_power[note_count] { 0 };
    double m_pos[note_count]; // Initialized lazily.
    Envelope m_envelope[note_count] { Done };

    int m_wave { first_wave };
    int m_attack;
    double m_attack_step;
    int m_decay;
    double m_decay_step;
    int m_sustain;
    double m_sustain_level;
    int m_release;
    double m_release_step[note_count];
    int m_delay { 0 };
    size_t m_delay_samples { 0 };
    size_t m_delay_index { 0 };

    const u32& m_time;

    SinglyLinkedList<RollNote> m_roll_notes[note_count];
    RollIter m_roll_iters[note_count];
};
