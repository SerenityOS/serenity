/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>

namespace LibDSP {

// A clip is a self-contained snippet of notes or audio that can freely move inside and in between tracks.
class Clip : public RefCounted<Clip> {
public:
    virtual ~Clip() = default;

    u32 start() const { return m_start; }
    u32 length() const { return m_length; }
    u32 end() const { return m_start + m_length; }

protected:
    Clip(u32 start, u32 length)
        : m_start(start)
        , m_length(length)
    {
    }

    u32 m_start;
    u32 m_length;
};

class AudioClip final : public Clip {
public:
    Sample sample_at(u32 time);

    Vector<Sample> const& samples() const { return m_samples; }

private:
    Vector<Sample> m_samples;
};

class NoteClip final : public Clip {
public:
    void set_note(RollNote note);

    Array<SinglyLinkedList<RollNote>, note_frequencies.size()> const& notes() const { return m_notes; }

private:
    Array<SinglyLinkedList<RollNote>, note_frequencies.size()> m_notes;
};

}
