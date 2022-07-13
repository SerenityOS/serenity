/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibDSP/Music.h>
#include <LibDSP/Transport.h>

namespace DSP {

class Keyboard : public RefCounted<Keyboard> {

public:
    enum class Direction : bool {
        Down,
        Up,
    };
    enum class Switch : bool {
        Off,
        On,
    };

    Keyboard(NonnullRefPtr<Transport> transport)
        : m_transport(move(transport))
    {
    }

    u8 virtual_keyboard_octave() const { return m_virtual_keyboard_octave; }
    u8 virtual_keyboard_octave_base() const { return (m_virtual_keyboard_octave - octave_min) * 12; }
    // Automatically clips the octave between the minimum and maximum.
    void change_virtual_keyboard_octave(Direction);
    // Errors out if the set octave is out of range.
    ErrorOr<void> set_virtual_keyboard_octave(u8 octave);

    void set_keyboard_note(u8 pitch, Switch note_switch);
    void set_keyboard_note_in_active_octave(i8 octave_offset, Switch note_switch);

    RollNotes const& notes() const { return m_pressed_notes; }
    Optional<RollNote> note_at(u8 pitch) const { return m_pressed_notes[pitch]; }
    bool is_pressed(u8 pitch) const;
    bool is_pressed_in_active_octave(i8 octave_offset) const;

private:
    u8 m_virtual_keyboard_octave { 4 };
    RollNotes m_pressed_notes;

    NonnullRefPtr<Transport> m_transport;

    static constexpr int const octave_min = 1;
    static constexpr int const octave_max = 7;
};

}
