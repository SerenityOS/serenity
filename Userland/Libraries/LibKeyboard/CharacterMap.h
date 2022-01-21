/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <LibKeyboard/CharacterMapData.h>

namespace Keyboard {

class CharacterMap {

public:
    CharacterMap(const String& map_name, const CharacterMapData& map_data);
    static ErrorOr<CharacterMap> load_from_file(const String& filename);

    int set_system_map();
    static ErrorOr<CharacterMap> fetch_system_map();

    const CharacterMapData& character_map_data() const { return m_character_map_data; };
    const String& character_map_name() const;

private:
    CharacterMapData m_character_map_data;
    String m_character_map_name;
};

}
