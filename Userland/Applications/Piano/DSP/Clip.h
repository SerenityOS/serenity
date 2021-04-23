/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/Types.h>
#include <AK/SinglyLinkedList.h>
#include <LibCore/Object.h>

namespace LibDSP {

class Clip : public Core::Object {
    C_OBJECT_ABSTRACT(Clip)
public:
    Clip(u32 length)
    : m_length(length)
    {
    }
    virtual ~Clip()
    {
    }

    u32 length() const { return m_length; }
protected:
    const u32 m_length;
};

class AudioClip final : public Clip {
public:
    Sample sample_at(u32 time);

private:
    Vector<Sample> m_samples { };
};

class NoteClip final : public Clip {
public:
    void set_note(RollNote note);

private:
    SinglyLinkedList<RollNote> m_notes[note_count];
    // Keeps track of the current note of each pitch that currently plays
    size_t m_current_notes_index[note_count];
};

}

