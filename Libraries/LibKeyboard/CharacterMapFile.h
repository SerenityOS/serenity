#pragma once

#include <AK/JsonObject.h>

namespace LibKeyboard {

struct CharacterMapData {
    char map[0x80];
    char shift_map[0x80];
    char alt_map[0x80];
    char altgr_map[0x80];
};

class CharacterMapFile {

public:
    static Optional<CharacterMapData> load_from_file(const String& file_name);

private:
    static char* read_map(const JsonObject& json, const String& name);
};

}
