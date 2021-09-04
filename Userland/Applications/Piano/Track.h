/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/Noncopyable.h>
#include <AK/SinglyLinkedList.h>
#include <LibAudio/Buffer.h>
#include <LibDSP/Effects.h>

using RollIter = AK::SinglyLinkedListIterator<SinglyLinkedList<RollNote>, RollNote>;

class Track {
    AK_MAKE_NONCOPYABLE(Track);
    AK_MAKE_NONMOVABLE(Track);

public:
    explicit Track(u32 const& time);
    ~Track();

    const Vector<Audio::Frame>& recorded_sample() const { return m_recorded_sample; }
    const SinglyLinkedList<RollNote>& roll_notes(int note) const { return m_roll_notes[note]; }
    int wave() const { return m_wave; }
    int volume() const { return m_volume; }
    int attack() const { return m_attack; }
    int decay() const { return m_decay; }
    int sustain() const { return m_sustain; }
    int release() const { return m_release; }
    NonnullRefPtr<LibDSP::Effects::Delay> delay() { return m_delay; }

    void fill_sample(Sample& sample);
    void reset();
    String set_recorded_sample(StringView const& path);
    void set_note(int note, Switch);
    void set_roll_note(int note, u32 on_sample, u32 off_sample);
    void set_wave(int wave);
    void set_wave(Direction);
    void set_volume(int volume);
    void set_attack(int attack);
    void set_decay(int decay);
    void set_sustain(int sustain);
    void set_release(int release);

private:
    Audio::Frame sine(size_t note);
    Audio::Frame saw(size_t note);
    Audio::Frame square(size_t note);
    Audio::Frame triangle(size_t note);
    Audio::Frame noise(size_t note);
    Audio::Frame recorded_sample(size_t note);

    void sync_roll(int note);
    void set_sustain_impl(int sustain);

    Vector<Audio::Frame> m_recorded_sample;

    u8 m_note_on[note_count] { 0 };
    double m_power[note_count] { 0 };
    double m_pos[note_count]; // Initialized lazily.
    // Synths may use this to keep track of the last wave position
    double m_last_w[note_count] { 0 };
    Envelope m_envelope[note_count] { Done };

    int m_wave { first_wave };
    int m_volume;
    int m_attack;
    double m_attack_step;
    int m_decay;
    double m_decay_step;
    int m_sustain;
    double m_sustain_level;
    int m_release;
    double m_release_step[note_count];

    u32 const& m_time;

    NonnullRefPtr<LibDSP::Transport> m_temporary_transport;
    NonnullRefPtr<LibDSP::Effects::Delay> m_delay;

    SinglyLinkedList<RollNote> m_roll_notes[note_count];
    RollIter m_roll_iterators[note_count];
};
