/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/Noncopyable.h>
#include <AK/NonnullRefPtr.h>
#include <AK/SinglyLinkedList.h>
#include <LibAudio/Buffer.h>
#include <LibDSP/Effects.h>
#include <LibDSP/Music.h>
#include <LibDSP/Synthesizers.h>

using LibDSP::RollNote;
using RollIter = AK::SinglyLinkedListIterator<SinglyLinkedList<RollNote>, RollNote>;

class Track {
    AK_MAKE_NONCOPYABLE(Track);
    AK_MAKE_NONMOVABLE(Track);

public:
    explicit Track(const u32& time);
    ~Track() = default;

    const Vector<Audio::Sample>& recorded_sample() const { return m_recorded_sample; }
    const SinglyLinkedList<RollNote>& roll_notes(int note) const { return m_roll_notes[note]; }
    int volume() const { return m_volume; }
    NonnullRefPtr<LibDSP::Synthesizers::Classic> synth() { return m_synth; }
    NonnullRefPtr<LibDSP::Effects::Delay> delay() { return m_delay; }

    void fill_sample(Sample& sample);
    void reset();
    String set_recorded_sample(StringView path);
    void set_roll_note(int note, u32 on_sample, u32 off_sample);
    void set_keyboard_note(int note, Switch state);
    void set_volume(int volume);

private:
    Audio::Sample recorded_sample(size_t note);

    void sync_roll(int note);

    Vector<Audio::Sample> m_recorded_sample;

    int m_volume;

    const u32& m_time;

    NonnullRefPtr<LibDSP::Transport> m_temporary_transport;
    NonnullRefPtr<LibDSP::Effects::Delay> m_delay;
    NonnullRefPtr<LibDSP::Synthesizers::Classic> m_synth;

    SinglyLinkedList<RollNote> m_roll_notes[note_count];
    RollIter m_roll_iterators[note_count];
    Array<Optional<RollNote>, note_count> m_keyboard_notes;
};
