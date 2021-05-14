/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ConfigFile.h>
#include <errno.h>
#include <spawn.h>
#include <stdio.h>
#include <unistd.h>

int main()
{
    if (pledge("stdio proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

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
}
