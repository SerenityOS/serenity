/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef KERNEL
#    include <AK/OSError.h>
#    include <AK/Result.h>
#endif
#include <AK/String.h>
#include <Kernel/API/KeyCode.h>
#include <LibKeyboard/CharacterMapData.h>

namespace Keyboard {

class CharacterMap {

public:
    CharacterMap(String const& map_name, CharacterMapData const& map_data);
    static Optional<CharacterMap> load_from_file(String const& filename);

#ifndef KERNEL
    int set_system_map();
    static Result<CharacterMap, OSError> fetch_system_map();
#endif

    u32 get_char(KeyEvent) const;
    void set_character_map_data(CharacterMapData character_map_data);
    void set_character_map_name(String const& character_map_name);

    CharacterMapData const& character_map_data() const { return m_character_map_data; };
    String const& character_map_name() const;

private:
    CharacterMapData m_character_map_data;
    String m_character_map_name;
};

}
