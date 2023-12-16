/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <LibKeyboard/CharacterMapData.h>

namespace Keyboard {

class CharacterMap {

public:
    CharacterMap(ByteString const& map_name, CharacterMapData const& map_data);
    static ErrorOr<CharacterMap> load_from_file(ByteString const& filename);

    int set_system_map();
    static ErrorOr<CharacterMap> fetch_system_map();

    CharacterMapData const& character_map_data() const { return m_character_map_data; }
    ByteString const& character_map_name() const;

private:
    CharacterMapData m_character_map_data;
    ByteString m_character_map_name;
};

}
