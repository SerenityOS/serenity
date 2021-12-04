/*
 * Copyright (c) 2021, kleines Filmröllchen <malu.bertsch@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibDSP/Transport.h>

constexpr int octave_min = 1;
constexpr int octave_max = 7;

namespace LibDSP {

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
        : m_pressed_notes(make_ref_counted<ReferencedRollNotes>())
        , m_transport(move(transport))
    {
    }

    u8 virtual_keyboard_octave() const { return m_virtual_keyboard_octave; }
    u8 virtual_keyboard_octave_base() const { return (m_virtual_keyboard_octave - octave_min) * 12; }
    void change_virtual_keyboard_octave(Direction);
    ErrorOr<void> set_virtual_keyboard_octave(u8 octave);

    void set_keyboard_note(u8 pitch, Switch note_switch);
    void set_keyboard_note_by_octave(i8 octave_offset, Switch note_switch);

    NonnullRefPtr<ReferencedRollNotes> notes() const { return m_pressed_notes; }

private:
    u8 m_virtual_keyboard_octave { 4 };
    NonnullRefPtr<ReferencedRollNotes> m_pressed_notes;

    NonnullRefPtr<Transport> m_transport;
};

}
