/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibKeyboard/CharacterMap.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio setkeymap getkeymap rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/res/keymaps", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc/Keyboard.ini", "rwc") < 0) {
        perror("unveil /etc/Keyboard.ini");
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
        warnln("Hint: Must be either a keymap name (e.g. 'en-us') or a full path.");
        return 1;
    }

    int rc = character_map.value().set_system_map();
    if (rc != 0) {
        perror("setkeymap");
        return rc;
    }

    auto mapper_config(Core::ConfigFile::open("/etc/Keyboard.ini", Core::ConfigFile::AllowWriting::Yes));
    mapper_config->write_entry("Mapping", "Keymap", path);
    mapper_config->sync();

    return rc;
}
