#include "CharacterMap.h"
#include <LibKeyboard/CharacterMapFile.h>

namespace LibKeyboard {

CharacterMap::CharacterMap(String file_name)
{
    auto result = CharacterMapFile::load_from_file(file_name);
    if (!result.has_value()) {
        ASSERT_NOT_REACHED();
    }

    m_character_map = result.value();
}

char CharacterMap::get_char(KeyEvent event)
{
    auto modifiers = event.modifiers();
    auto scan_code = event.scan_code;
    auto caps_lock_on = event.caps_lock_on;

    char character;
    if (modifiers & Mod_Shift)
        character = m_character_map.shift_map[scan_code];
    else if (modifiers & Mod_Alt)
        character = m_character_map.alt_map[scan_code];
    else if (modifiers & Mod_AltGr)
        character = m_character_map.altgr_map[scan_code];
    else
        character = m_character_map.map[scan_code];

    if (caps_lock_on && (modifiers == 0 || modifiers == Mod_Shift)) {
        if (character >= 'a' && character <= 'z')
            character &= ~0x20;
        else if (character >= 'A' && character <= 'Z')
            character |= 0x20;
    }

    if (event.e0_prefix) {
        if (event.key == Key_Slash) {
            character = '/'; // On Turkish-QWERTY Keyboard Key_Slash mapped to '.' char, if e0 prefix is true remap to '/' char
        }
    }

    return character;
}

}
