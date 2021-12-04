/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/RefCounted.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <LibCore/Object.h>

namespace LibDSP {

// A clip is a self-contained snippet of notes or audio that can freely move inside and in between tracks.
class Clip : public RefCounted<Clip> {
public:
    Clip(u32 start, u32 length)
        : m_start(start)
        , m_length(length)
    {
    }
    virtual ~Clip() = default;

    u32 start() const { return m_start; }
    u32 length() const { return m_length; }
    u32 end() const { return m_start + m_length; }

protected:
    u32 m_start;
    u32 m_length;
};

class AudioClip final : public Clip {
public:
    Sample* sample_data_at(u32 time);
    Sample const* sample_data_at(u32 time) const;

    Vector<Sample> const& samples() const { return m_samples; }

private:
    Vector<Sample> m_samples;
};

class NoteClip final : public Clip {
public:
    void set_note(RollNote note);

    SinglyLinkedList<RollNote> notes_at_pitch(u8 pitch) const { return m_notes[pitch]; }
    Array<SinglyLinkedList<RollNote>, note_count>& notes() { return m_notes; }

private:
    Array<SinglyLinkedList<RollNote>, note_count> m_notes;
};

}
