/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/System.h>
#include <LibKeyboard/CharacterMap.h>
#include <LibMain/Main.h>
#include <stdio.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio setkeymap getkeymap rpath wpath cpath"));
    TRY(Core::System::unveil("/res/keymaps", "r"));
    TRY(Core::System::unveil("/etc/Keyboard.ini", "rwc"));

    const char* path = nullptr;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "The mapping file to be used", "file", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (path && path[0] == '/')
        TRY(Core::System::unveil(path, "r"));

    TRY(Core::System::unveil(nullptr, nullptr));

    if (!path) {
        auto keymap = TRY(Keyboard::CharacterMap::fetch_system_map());
        outln("{}", keymap.character_map_name());
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
