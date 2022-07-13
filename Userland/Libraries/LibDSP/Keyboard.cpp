/*
 * Copyright (c) 2021-2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Keyboard.h"
#include "Music.h"
#include <AK/Error.h>
#include <AK/NumericLimits.h>

namespace DSP {

void Keyboard::set_keyboard_note(u8 pitch, Keyboard::Switch note_switch)
{
    VERIFY(pitch < note_frequencies.size());

    if (note_switch == Switch::Off) {
        m_pressed_notes[pitch] = {};
        return;
    }

    auto fake_note = RollNote {
        .on_sample = m_transport->time(),
        .off_sample = NumericLimits<u32>::max(),
        .pitch = pitch,
        .velocity = NumericLimits<i8>::max(),
    };

    m_pressed_notes[pitch] = fake_note;
}
void Keyboard::set_keyboard_note_in_active_octave(i8 octave_offset, Switch note_switch)
{
    u8 real_note = octave_offset + (m_virtual_keyboard_octave - 1) * notes_per_octave;
    set_keyboard_note(real_note, note_switch);
}

void Keyboard::change_virtual_keyboard_octave(Direction direction)
{
    if (direction == Direction::Up) {
        if (m_virtual_keyboard_octave < octave_max)
            ++m_virtual_keyboard_octave;
    } else {
        if (m_virtual_keyboard_octave > octave_min)
            --m_virtual_keyboard_octave;
    }
}

ErrorOr<void> Keyboard::set_virtual_keyboard_octave(u8 octave)
{
    if (octave <= octave_max && octave >= octave_min) {
        m_virtual_keyboard_octave = octave;
        return {};
    }
    return Error::from_string_literal("Octave out of range");
}

bool Keyboard::is_pressed(u8 pitch) const
{
    return m_pressed_notes[pitch].has_value() && m_pressed_notes[pitch]->is_playing(m_transport->time());
}

bool Keyboard::is_pressed_in_active_octave(i8 octave_offset) const
{
    return is_pressed(octave_offset + virtual_keyboard_octave_base());
}

}
