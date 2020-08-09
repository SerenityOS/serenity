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
#include <Kernel/API/Syscall.h>
#ifndef KERNEL
#    include <LibKeyboard/CharacterMapFile.h>
#endif

namespace Keyboard {

CharacterMap::CharacterMap(const String& file_name)
{
#ifdef KERNEL
    m_character_map_data = default_character_map;
#else
    auto result = CharacterMapFile::load_from_file(file_name);
    ASSERT(result.has_value());

    m_character_map_data = result.value();
#endif
    m_character_map_name = file_name;
}

#ifndef KERNEL

int CharacterMap::set_system_map()
{
    Syscall::SC_setkeymap_params params { m_character_map_data.map, m_character_map_data.shift_map, m_character_map_data.alt_map, m_character_map_data.altgr_map, { m_character_map_name.characters(), m_character_map_name.length() } };
    return syscall(SC_setkeymap, &params);
}

#endif

u32 CharacterMap::get_char(KeyEvent event)
{
    auto modifiers = event.modifiers();
    auto index = event.scancode & 0xFF; // Index is last byte of scan code.
    auto caps_lock_on = event.caps_lock_on;

    u32 code_point;
    if (modifiers & Mod_Shift)
        code_point = m_character_map_data.shift_map[index];
    else if (modifiers & Mod_Alt)
        code_point = m_character_map_data.alt_map[index];
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
