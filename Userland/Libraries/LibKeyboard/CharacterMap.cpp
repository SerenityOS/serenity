/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "CharacterMap.h"
#include <AK/StringBuilder.h>
#include <LibKeyboard/CharacterMapFile.h>

#ifndef KERNEL
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

Result<CharacterMap, OSError> CharacterMap::fetch_system_map()
{
    CharacterMapData map_data;
    char keymap_name[50 + 1] = { 0 };

    if (getkeymap(keymap_name, sizeof(keymap_name), map_data.map, map_data.shift_map, map_data.alt_map, map_data.altgr_map, map_data.shift_altgr_map) < 0) {
        return OSError(errno);
    }

    return CharacterMap { keymap_name, map_data };
}

#endif

u32 CharacterMap::get_char(KeyEvent event)
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

const String CharacterMap::character_map_name()
{
    return m_character_map_name;
}
}
