/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/ArgsParser.h>
#include <LibKeyboard/CharacterMap.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio setkeymap rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res/keymaps", "r") < 0) {
        perror("unveil");
        return 1;
    }

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The mapping file to be used", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (path && path[0] == '/') {
        if (unveil(path, "r") < 0) {
            perror("unveil path");
            return 1;
        }
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    if (!path) {
        auto keymap = Keyboard::CharacterMap::fetch_system_map();
        if (keymap.is_error()) {
            warnln("getkeymap: {}", keymap.error());
            return 1;
        }

        outln("{}", keymap.value().character_map_name());
        return 0;
    }

    auto character_map = Keyboard::CharacterMap::load_from_file(path);
    if (!character_map.has_value()) {
        warnln("Cannot read keymap {}", path);
        warnln("Hint: Must be either a keymap name (e.g. 'en') or a full path.");
        return 1;
    }

    int rc = character_map.value().set_system_map();
    if (rc != 0) {
        perror("setkeymap");
    }

    return rc;
}
