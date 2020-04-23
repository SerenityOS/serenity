#pragma once

#include <AK/String.h>
#include <Kernel/KeyCode.h>
#include <LibKeyboard/CharacterMapFile.h>

namespace LibKeyboard {

class CharacterMap {

public:
    CharacterMap(String file_name);

    char get_char(KeyEvent);

private:
    CharacterMapData m_character_map;
};

}
