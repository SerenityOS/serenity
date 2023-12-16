/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/System.h>
#include <LibKeyboard/CharacterMap.h>
#include <LibMain/Main.h>
#include <stdio.h>

int set_keymap(ByteString const& keymap);

int set_keymap(ByteString const& keymap)
{
    auto character_map = Keyboard::CharacterMap::load_from_file(keymap);
    if (character_map.is_error()) {
        warnln("Cannot read keymap {}", keymap);
        warnln("Hint: Must be a keymap name (e.g. 'en-us')");
        return 1;
    }

    int rc = character_map.value().set_system_map();
    if (rc != 0) {
        perror("setkeymap");
    }

    return rc;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio setkeymap getkeymap rpath wpath cpath"));
    TRY(Core::System::unveil("/res/keymaps", "r"));
    TRY(Core::System::unveil("/etc/Keyboard.ini", "rwc"));

    ByteString mapping;
    ByteString mappings;
    Core::ArgsParser args_parser;
    args_parser.add_option(mapping, "The mapping to be used", "set-keymap", 'm', "keymap");
    args_parser.add_option(mappings, "Comma separated list of enabled mappings", "set-keymaps", 's', "keymaps");
    args_parser.parse(arguments);

    TRY(Core::System::unveil(nullptr, nullptr));

    if (mapping.is_empty() && mappings.is_empty()) {
        auto keymap = TRY(Keyboard::CharacterMap::fetch_system_map());
        outln("{}", keymap.character_map_name());
        return 0;
    }

    auto mapper_config = TRY(Core::ConfigFile::open("/etc/Keyboard.ini", Core::ConfigFile::AllowWriting::Yes));

    if (!mappings.is_empty()) {
        auto mappings_vector = mappings.split(',');

        if (mappings_vector.is_empty()) {
            warnln("Keymaps list should not be empty");
            return 1;
        }

        // Verify that all specified keymaps are loadable
        for (auto& keymap_name : mappings_vector) {
            if (auto keymap = Keyboard::CharacterMap::load_from_file(keymap_name); keymap.is_error()) {
                warnln("Cannot load keymap {}: {}({})", keymap_name, keymap.error().string_literal(), keymap.error().code());
                return keymap.release_error();
            }
        }

        auto keymaps = ByteString::join(',', mappings_vector);
        mapper_config->write_entry("Mapping", "Keymaps", keymaps);
        TRY(mapper_config->sync());
    }

    auto keymaps = mapper_config->read_entry("Mapping", "Keymaps");
    auto keymaps_vector = keymaps.split(',');

    if (!mapping.is_empty()) {
        if (keymaps_vector.is_empty()) {
            warnln("No keymaps configured - writing default configurations (en-us)");
            mapper_config->write_entry("Mapping", "Keymaps", "en-us");
            TRY(mapper_config->sync());
            keymaps_vector.append("en-us");
        }

        if (!keymaps_vector.find(mapping).is_end()) {
            int rc = set_keymap(mapping);
            if (rc == 0)
                return rc;
        } else {
            warnln("Keymap '{}' is not in list of configured keymaps ({})", mapping, keymaps);
        }
    }

    return set_keymap(keymaps_vector.first());
}
