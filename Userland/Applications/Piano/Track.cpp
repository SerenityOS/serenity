/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, William McPherson <willmcpherson2@gmail.com>
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Track.h"
#include <AK/Forward.h>
#include <AK/Math.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NumericLimits.h>
#include <LibAudio/Loader.h>
#include <LibDSP/Music.h>
#include <math.h>

Track::Track(NonnullRefPtr<DSP::Transport> transport, NonnullRefPtr<DSP::Keyboard> keyboard)
    : m_transport(move(transport))
    , m_delay(make_ref_counted<DSP::Effects::Delay>(m_transport))
    , m_synth(make_ref_counted<DSP::Synthesizers::Classic>(m_transport))
    , m_keyboard(move(keyboard))
{
    set_volume(volume_max);
}

void Track::fill_sample(Sample& sample)
{
    auto playing_notes = DSP::RollNotes {};

    for (size_t i = 0; i < note_count; ++i) {
        bool has_roll_notes = false;
        auto& notes_at_pitch = m_roll_notes[i];
        for (auto& note : notes_at_pitch) {
            if (note.is_playing(m_transport->time())) {
                has_roll_notes = true;
                playing_notes.set(i, note);
            }
        }
        if (m_is_active_track) {
            auto key_at_pitch = m_keyboard->note_at(i);
            if (key_at_pitch.has_value() && key_at_pitch.value().is_playing(m_transport->time()))
                playing_notes.set(i, key_at_pitch.release_value());
            // If there are roll notes playing, don't stop them when we lift a keyboard key.
            else if (!has_roll_notes)
                playing_notes.remove(i);
        }
    }

    auto synthesized_sample = DSP::Signal { FixedArray<Audio::Sample>::must_create_but_fixme_should_propagate_errors(1) };
    m_synth->process(playing_notes, synthesized_sample);
    auto delayed_signal = DSP::Signal { FixedArray<Audio::Sample>::must_create_but_fixme_should_propagate_errors(1) };
    m_delay->process(synthesized_sample, delayed_signal);
    auto delayed_sample = delayed_signal.get<FixedArray<Audio::Sample>>()[0];

    // HACK: Convert to old Piano range: 16-bit int
    delayed_sample *= NumericLimits<i16>::max();
    delayed_sample.left = clamp(delayed_sample.left, NumericLimits<i16>::min(), NumericLimits<i16>::max());
    delayed_sample.right = clamp(delayed_sample.right, NumericLimits<i16>::min(), NumericLimits<i16>::max());
    // TODO: Use the master processor
    delayed_sample *= static_cast<double>(m_volume) / static_cast<double>(volume_max) * volume_factor;

    sample.left += delayed_sample.left;
    sample.right += delayed_sample.right;
}

void Track::reset()
{
    for (size_t note = 0; note < note_count; ++note)
        m_roll_iterators[note] = m_roll_notes[note].begin();
}

void Track::sync_roll(int note)
{
    auto it = m_roll_notes[note].find_if([&](auto& roll_note) { return roll_note.off_sample > m_transport->time(); });
    if (it.is_end())
        m_roll_iterators[note] = m_roll_notes[note].begin();
    else
        m_roll_iterators[note] = it;
}

void Track::set_roll_note(int note, u32 on_sample, u32 off_sample)
{
    RollNote new_roll_note = { on_sample, off_sample, (u8)note, 0 };

    VERIFY(note >= 0 && note < note_count);
    VERIFY(new_roll_note.off_sample < roll_length);
    VERIFY(new_roll_note.length() >= 2);

    for (auto it = m_roll_notes[note].begin(); !it.is_end();) {
        if (it->on_sample > new_roll_note.off_sample) {
            m_roll_notes[note].insert_before(it, new_roll_note);
            sync_roll(note);
            return;
        }
        if (it->on_sample <= new_roll_note.on_sample && it->off_sample >= new_roll_note.on_sample) {
            it.remove(m_roll_notes[note]);
            sync_roll(note);
            return;
        }
        if ((new_roll_note.on_sample == 0 || it->on_sample >= new_roll_note.on_sample - 1) && it->on_sample <= new_roll_note.off_sample) {
            it.remove(m_roll_notes[note]);
            it = m_roll_notes[note].begin();
            continue;
        }
        ++it;
    }

    m_roll_notes[note].append(new_roll_note);
    sync_roll(note);
}

void Track::set_volume(int volume)
{
    VERIFY(volume >= 0);
    m_volume = volume;
}
