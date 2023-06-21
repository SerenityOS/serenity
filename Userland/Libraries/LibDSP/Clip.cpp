/*
 * Copyright (c) 2021, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Clip.h"

namespace DSP {

Sample AudioClip::sample_at(u32 time)
{
    VERIFY(time < m_length);
    return m_samples[time];
}

Optional<RollNote> NoteClip::note_at(u32 time, u8 pitch) const
{
    for (auto& note : m_notes) {
        if (time >= note.on_sample && time <= note.off_sample && pitch == note.pitch)
            return note;
    }
    return {};
}

void NoteClip::set_note(RollNote note)
{
    m_notes.remove_all_matching([&](auto const& other) {
        return other.pitch == note.pitch && other.overlaps_with(note);
    });
    m_notes.append(note);
}

void NoteClip::remove_note(RollNote note)
{
    // FIXME: See header; this could be much faster with a better datastructure.
    m_notes.remove_first_matching([note](auto const& element) {
        return element.on_sample == note.on_sample && element.off_sample == note.off_sample && element.pitch == note.pitch;
    });
}

}
