/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMap.h"
#include <AK/StringBuilder.h>

#ifndef KERNEL
#    include <LibKeyboard/CharacterMapFile.h>
#    include <serenity.h>
#endif

namespace Keyboard {

#ifndef KERNEL
// The Kernel explicitly and exclusively links only this file into it.
// Thus, we cannot even include a reference to the symbol `CharacterMapFile::load_from_file`.
Optional<CharacterMap> CharacterMap::load_from_file(const String& map_name)
{
    auto result = CharacterMapFile::load_from_file(map_name);
    if (!result.has_value())
        return {};

    return CharacterMap(map_name, result.value());
}
#endif

CharacterMap::CharacterMap(const String& map_name, const CharacterMapData& map_data)
    : m_character_map_data(map_data)
    , m_character_map_name(map_name)
{
}

#ifndef KERNEL

int CharacterMap::set_system_map()
{
    return setkeymap(m_character_map_name.characters(), m_character_map_data.map, m_character_map_data.shift_map, m_character_map_data.alt_map, m_character_map_data.altgr_map, m_character_map_data.shift_altgr_map);
}

ErrorOr<CharacterMap> CharacterMap::fetch_system_map()
{
    CharacterMapData map_data;
    char keymap_name[50 + 1] = { 0 };

    if (getkeymap(keymap_name, sizeof(keymap_name), map_data.map, map_data.shift_map, map_data.alt_map, map_data.altgr_map, map_data.shift_altgr_map) < 0)
        return Error::from_errno(errno);

    return CharacterMap { keymap_name, map_data };
}

#endif

u32 CharacterMap::get_char(KeyEvent event) const
{
    auto modifiers = event.modifiers();
    auto index = event.scancode & 0xFF; // Index is last byte of scan code.
    auto caps_lock_on = event.caps_lock_on;

    u32 code_point;
    if (modifiers & Mod_Alt)
        code_point = m_character_map_data.alt_map[index];
    else if ((modifiers & Mod_Shift) && (modifiers & Mod_AltGr))
        code_point = m_character_map_data.shift_altgr_map[index];
    else if (modifiers & Mod_Shift)
        code_point = m_character_map_data.shift_map[index];
    else if (modifiers & Mod_AltGr)
        code_point = m_character_map_data.altgr_map[index];
    else
        code_point = m_character_map_data.map[index];

    if (caps_lock_on && (modifiers == 0 || modifiers == Mod_Shift)) {
        if (code_point >= 'a' && code_point <= 'z')
            code_point &= ~0x20;
        else if (code_point >= 'A' && code_point <= 'Z')
            code_point |= 0x20;
    }

    if (event.e0_prefix && event.key == Key_Slash) {
        // If Key_Slash (scancode = 0x35) mapped to other form "/", we fix num pad key of "/" with this case.
        code_point = '/';
    } else if (event.e0_prefix && event.key != Key_Return) {
        // Except for `keypad-/` and 'keypad-return', all e0 scan codes are not actually characters. i.e., `keypad-0` and
        // `Insert` have the same scancode except for the prefix, but insert should not have a code_point.
        code_point = 0;
    }

    return code_point;
}

void CharacterMap::set_character_map_data(CharacterMapData character_map_data)
{
    m_character_map_data = character_map_data;
}

void CharacterMap::set_character_map_name(const String& character_map_name)
{
    m_character_map_name = character_map_name;
}

const String& CharacterMap::character_map_name() const
{
    return m_character_map_name;
}
}
