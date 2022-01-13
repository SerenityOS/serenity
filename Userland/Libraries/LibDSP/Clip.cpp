/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Clip.h"

namespace LibDSP {

Sample AudioClip::sample_at(u32 time)
{
    VERIFY(time < m_length);
    return m_samples[time];
}

void NoteClip::set_note(RollNote note)
{
    VERIFY(note.pitch >= 0 && note.pitch < note_count);
    VERIFY(note.off_sample < m_length);
    VERIFY(note.length() >= 2);

    auto& notes = m_notes[note.pitch];
    for (auto it = notes.begin(); !it.is_end();) {
        auto iterated_note = *it;
        if (iterated_note.on_sample > note.off_sample) {
            notes.insert_before(it, note);
            return;
        }
        if (iterated_note.on_sample <= note.on_sample && iterated_note.off_sample >= note.on_sample) {
            notes.remove(it);
            return;
        }
        if ((note.on_sample == 0 || iterated_note.on_sample >= note.on_sample - 1) && iterated_note.on_sample <= note.off_sample) {
            notes.remove(it);
            it = notes.begin();
            continue;
        }
        ++it;
    }

    notes.append(note);
}

}
