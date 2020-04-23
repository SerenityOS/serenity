#include "CharacterMapFile.h"
#include <LibCore/File.h>

namespace LibKeyboard {

Optional<CharacterMapData> CharacterMapFile::load_from_file(const String& file_name)
{
    auto path = file_name;
    if (!path.ends_with(".json")) {
        StringBuilder full_path;
        full_path.append("/res/keymaps/");
        full_path.append(file_name);
        full_path.append(".json");
        path = full_path.to_string();
    }

    auto file = Core::File::construct(path);
    file->open(Core::IODevice::ReadOnly);
    if (!file->is_open()) {
        dbg() << "Failed to open " << file_name << ":" << file->error_string();
        return {};
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents).as_object();

    char* map = read_map(json, "map");
    char* shift_map = read_map(json, "shift_map");
    char* alt_map = read_map(json, "alt_map");
    char* altgr_map = read_map(json, "altgr_map");

    CharacterMapData character_map;
    for (int i = 0; i < 0x80; i++) {
        character_map.map[i] = map[i];
        character_map.shift_map[i] = shift_map[i];
        character_map.alt_map[i] = alt_map[i];
        if (altgr_map) {
            character_map.altgr_map[i] = altgr_map[i];
        } else {
            // AltGr map was not found, using Alt map as fallback.
            character_map.altgr_map[i] = alt_map[i];
        }
    }

    return character_map;
}

char* CharacterMapFile::read_map(const JsonObject& json, const String& name)
{
    if (!json.has(name))
        return nullptr;

    char* map = new char[0x80]();
    auto map_arr = json.get(name).as_array();

    for (int i = 0; i < map_arr.size(); i++) {
        auto key_value = map_arr.at(i).as_string();
        char character = 0;
        if (key_value.length() == 0) {
            ;
        } else if (key_value.length() == 1) {
            character = key_value.characters()[0];
        } else if (key_value.length() == 4) {
            // FIXME: Replace this workaround with "\u001B" in the keymap files
            //     after these kind of escape sequences are implemented in JsonParser.
            if (key_value == "0x1B") {
                character = 0x1B;
            }
        } else {
            dbg() << "Unknown character in " << name.characters() << "[" << i << "] = " << key_value.characters() << ".";
            ASSERT_NOT_REACHED();
        }

        map[i] = character;
    }

    return map;
}

}
