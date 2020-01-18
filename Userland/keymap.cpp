/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/JsonObject.h>
#include <LibCore/CFile.h>
#include <stdio.h>

#include <Kernel/Syscall.h>
#include <AK/Optional.h>
#include <AK/StdLibExtras.h>
#include <AK/kmalloc.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

char* read_map(const JsonObject& json, const String& name)
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
            fprintf(stderr, "Unknown character in %s[%u] = %s.\n", name.characters(), i, key_value.characters());
            ASSERT_NOT_REACHED();
        }

        map[i] = character;
    }

    return map;
}

RefPtr<CFile> open_keymap_file(String& filename)
{
    auto file = CFile::construct(filename);
    if (file->open(CIODevice::ReadOnly))
        return file;

    if (!filename.ends_with(".json")) {
        StringBuilder full_path;
        full_path.append("/res/keymaps/");
        full_path.append(filename);
        full_path.append(".json");
        filename = full_path.to_string();
        file = CFile::construct(filename);
        if (file->open(CIODevice::ReadOnly))
            return file;
    }

    return file;
}

int read_map_from_file(String& filename)
{
    auto file = open_keymap_file(filename);
    if (!file->is_open()) {
        fprintf(stderr, "Failed to open %s: %s\n", filename.characters(), file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();
    auto json = JsonValue::from_string(file_contents).as_object();

    char* map = read_map(json, "map");
    char* shift_map = read_map(json, "shift_map");
    char* alt_map = read_map(json, "alt_map");
    char* altgr_map = read_map(json, "altgr_map");

    if (!altgr_map) {
        // AltGr map was not found, using Alt map as fallback.
        altgr_map = alt_map;
    }

    Syscall::SC_setkeymap_params params { map, shift_map, alt_map, altgr_map };
    return syscall(SC_setkeymap, &params);
}

int main(int argc, char** argv)
{
    if (argc != 2) {
        fprintf(stderr, "usage: keymap <name|file>\n");
        return 0;
    }

    String filename = argv[1];
    int ret_val = read_map_from_file(filename);

    if (ret_val == -EPERM)
        fprintf(stderr, "Permission denied.\n");

    if (ret_val == 0)
        fprintf(stderr, "New keymap loaded from \"%s\".\n", filename.characters());

    return ret_val;
}
