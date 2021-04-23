/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
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

    size_t index = 0;
    for (auto it = m_notes[note.pitch].begin(); !it.is_end(); ++index) {
        if (it->on_sample > note.off_sample) {
            m_notes[note.pitch].insert_before(it, note);
            // synchronize the current note index
            if (m_current_notes_index[note.pitch] >= index)
                m_current_notes_index[note.pitch] = ++index;
            return;
        }
        if (it->on_sample <= note.on_sample && it->off_sample >= note.on_sample) {
            m_notes[note.pitch].remove(it);
            if (m_current_notes_index[note.pitch] >= index)
                m_current_notes_index[note.pitch] = --index;
            return;
        }
        if ((note.on_sample == 0 || it->on_sample >= note.on_sample - 1) && it->on_sample <= note.off_sample) {
            m_notes[note.pitch].remove(it);
            if (m_current_notes_index[note.pitch] >= index)
                m_current_notes_index[note.pitch] = --index;
            it = m_notes[note.pitch].begin();
            continue;
        }
        ++it;
    }

    m_notes[note.pitch].append(note);
}

}
