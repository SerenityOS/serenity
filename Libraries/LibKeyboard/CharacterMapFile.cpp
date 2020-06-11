/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "CharacterMapFile.h"
#include <LibCore/File.h>

namespace Keyboard {

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
    auto json_result = JsonValue::from_string(file_contents);
    ASSERT(json_result.has_value());
    auto json = json_result.value().as_object();

    ByteBuffer map = read_map(json, "map");
    ByteBuffer shift_map = read_map(json, "shift_map");
    ByteBuffer alt_map = read_map(json, "alt_map");
    ByteBuffer altgr_map = read_map(json, "altgr_map");

    CharacterMapData character_map;
    for (int i = 0; i < CHAR_MAP_SIZE; i++) {
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

ByteBuffer CharacterMapFile::read_map(const JsonObject& json, const String& name)
{
    if (!json.has(name))
        return nullptr;

    ByteBuffer buffer;
    buffer.grow(CHAR_MAP_SIZE);

    auto map_arr = json.get(name).as_array();
    for (int i = 0; i < map_arr.size(); i++) {
        auto key_value = map_arr.at(i).as_string();
        if (key_value.length() == 0) {
            buffer[i] = 0;
        } else if (key_value.length() == 1) {
            buffer[i] = key_value.characters()[0];
        } else {
            dbg() << "Unknown character in " << name.characters() << "[" << i << "] = " << key_value.characters() << ".";
            ASSERT_NOT_REACHED();
        }
    }

    return buffer;
}

}
