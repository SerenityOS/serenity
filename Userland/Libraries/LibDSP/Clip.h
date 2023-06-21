/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/RefCounted.h>
#include <AK/SinglyLinkedList.h>
#include <AK/Types.h>
#include <LibDSP/Music.h>

namespace DSP {

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
    Sample sample_at(u32 time);

    Vector<Sample> const& samples() const { return m_samples; }

private:
    Vector<Sample> m_samples;
};

class NoteClip final : public Clip {
public:
    NoteClip(u32 start, u32 length)
        : Clip(start, length)
    {
    }

    Optional<RollNote> note_at(u32 time, u8 pitch) const;
    void set_note(RollNote note);
    // May do nothing; that's fine.
    void remove_note(RollNote note);

    ReadonlySpan<RollNote> notes() const { return m_notes.span(); }

    RollNote operator[](size_t index) const { return m_notes[index]; }
    RollNote operator[](size_t index) { return m_notes[index]; }
    bool is_empty() const { return m_notes.is_empty(); }

private:
    // FIXME: Better datastructures to think about here: B-Trees or good ol' RBTrees (not very cache friendly)
    Vector<RollNote> m_notes;
};

}
