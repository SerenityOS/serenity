/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <LibKeyboard/CharacterMapData.h>

namespace Keyboard {

class CharacterMap {

public:
    CharacterMap(DeprecatedString const& map_name, CharacterMapData const& map_data);
    static ErrorOr<CharacterMap> load_from_file(DeprecatedString const& filename);

    int set_system_map();
    static ErrorOr<CharacterMap> fetch_system_map();

    CharacterMapData const& character_map_data() const { return m_character_map_data; };
    DeprecatedString const& character_map_name() const;

private:
    CharacterMapData m_character_map_data;
    DeprecatedString m_character_map_name;
};

}
