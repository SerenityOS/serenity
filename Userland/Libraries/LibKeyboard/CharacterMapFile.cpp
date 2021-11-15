/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CharacterMapFile.h"
#include <AK/ByteBuffer.h>
#include <AK/Utf8View.h>
#include <LibCore/File.h>

namespace Keyboard {

Optional<CharacterMapData> CharacterMapFile::load_from_file(const String& filename)
{
    auto path = filename;
    if (!path.ends_with(".json")) {
        StringBuilder full_path;
        full_path.append("/res/keymaps/");
        full_path.append(filename);
        full_path.append(".json");
        path = full_path.to_string();
    }

    auto file = Core::File::construct(path);
    file->open(Core::OpenMode::ReadOnly);
    if (!file->is_open()) {
        dbgln("Failed to open {}: {}", path, file->error_string());
        return {};
    }

    auto file_contents = file->read_all();
    auto json_result = JsonValue::from_string(file_contents);
    if (json_result.is_error()) {
        dbgln("Failed to load character map from file {}", path);
        return {};
    }
    auto json = json_result.value().as_object();

    Vector<u32> map = read_map(json, "map");
    Vector<u32> shift_map = read_map(json, "shift_map");
    Vector<u32> alt_map = read_map(json, "alt_map");
    Vector<u32> altgr_map = read_map(json, "altgr_map");
    Vector<u32> shift_altgr_map = read_map(json, "shift_altgr_map");

    CharacterMapData character_map;
    for (int i = 0; i < CHAR_MAP_SIZE; i++) {
        character_map.map[i] = map.at(i);
        character_map.shift_map[i] = shift_map.at(i);
        character_map.alt_map[i] = alt_map.at(i);
        if (altgr_map.is_empty()) {
            // AltGr map was not found, using Alt map as fallback.
            character_map.altgr_map[i] = alt_map.at(i);
        } else {
            character_map.altgr_map[i] = altgr_map.at(i);
        }
        if (shift_altgr_map.is_empty()) {
            // Shift+AltGr map was not found, using Alt map as fallback.
            character_map.shift_altgr_map[i] = alt_map.at(i);
        } else {
            character_map.shift_altgr_map[i] = shift_altgr_map.at(i);
        }
    }

    return character_map;
}

Vector<u32> CharacterMapFile::read_map(const JsonObject& json, const String& name)
{
    if (!json.has(name))
        return {};

    Vector<u32> buffer;
    buffer.resize(CHAR_MAP_SIZE);

    auto map_arr = json.get(name).as_array();
    for (size_t i = 0; i < map_arr.size(); i++) {
        auto key_value = map_arr.at(i).as_string();
        if (key_value.length() == 0) {
            buffer[i] = 0;
        } else if (key_value.length() == 1) {
            buffer[i] = key_value.characters()[0];
        } else {
            Utf8View m_utf8_view(key_value);
            buffer[i] = *m_utf8_view.begin();
        }
    }

    return buffer;
}

}
