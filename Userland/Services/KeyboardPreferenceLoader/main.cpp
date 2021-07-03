/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ConfigFile.h>
#include <errno.h>
#include <serenity.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

int main()
{
    if (pledge("stdio proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto keyboard_settings_config = Core::ConfigFile::get_for_app("KeyboardSettings");

    if (unveil("/bin/keymap", "x") < 0) {
        perror("unveil /bin/keymap");
        return 1;
    }

    if (unveil("/etc/Keyboard.ini", "r") < 0) {
        perror("unveil /etc/Keyboard.ini");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto mapper_config(Core::ConfigFile::open("/etc/Keyboard.ini"));
    auto keymap = mapper_config->read_entry("Mapping", "Keymap", "");

    pid_t child_pid;
    const char* argv[] = { "/bin/keymap", keymap.characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }

    bool enable_num_lock = keyboard_settings_config->read_bool_entry("StartupEnable", "NumLock", true);
    set_num_lock(enable_num_lock);
}
