/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMap.h"
#include <AK/StringBuilder.h>
#include <LibKeyboard/CharacterMapFile.h>
#include <errno.h>
#include <serenity.h>

namespace Keyboard {

ErrorOr<CharacterMap> CharacterMap::load_from_file(const String& map_name)
{
    auto result = TRY(CharacterMapFile::load_from_file(map_name));

    return CharacterMap(map_name, result);
}

CharacterMap::CharacterMap(const String& map_name, const CharacterMapData& map_data)
    : m_character_map_data(map_data)
    , m_character_map_name(map_name)
{
}

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

const String& CharacterMap::character_map_name() const
{
    return m_character_map_name;
}
}
