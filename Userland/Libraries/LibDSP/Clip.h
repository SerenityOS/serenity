/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Music.h"
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <LibCore/Object.h>

namespace LibDSP {

class Clip : public Core::Object {
    C_OBJECT_ABSTRACT(Clip)
public:
    Clip(u32 start, u32 length)
        : m_start(start)
        , m_length(length)
    {
    }
    virtual ~Clip()
    {
    }

    u32 start() const { return m_start; }
    u32 length() const { return m_length; }
    u32 end() const { return m_start + m_length; }

protected:
    const u32 m_start;
    const u32 m_length;
};

class AudioClip final : public Clip {
public:
    Sample sample_at(u32 time);

    const Vector<Sample>& samples() const { return m_samples; }

private:
    Vector<Sample> m_samples {};
};

class NoteClip final : public Clip {
public:
    void set_note(RollNote note);
    SinglyLinkedList<RollNote> m_notes[note_count];
};

}
