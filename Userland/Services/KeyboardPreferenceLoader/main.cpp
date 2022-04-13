/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio proc exec rpath cpath"));
    auto keyboard_settings_config = TRY(Core::ConfigFile::open_for_app("KeyboardSettings"));

    TRY(Core::System::unveil("/bin/keymap", "x"));
    TRY(Core::System::unveil("/etc/Keyboard.ini", "r"));
    TRY(Core::System::unveil("/dev/keyboard0", "r"));
    TRY(Core::System::unveil(nullptr, nullptr));
    auto mapper_config = TRY(Core::ConfigFile::open("/etc/Keyboard.ini"));
    auto keymaps = mapper_config->read_entry("Mapping", "Keymaps", "");

    auto keymaps_vector = keymaps.split(',');
    if (keymaps_vector.size() == 0)
        exit(1);

    pid_t child_pid;
    char const* argv[] = { "/bin/keymap", "-m", keymaps_vector.first().characters(), nullptr };
    if ((errno = posix_spawn(&child_pid, "/bin/keymap", nullptr, nullptr, const_cast<char**>(argv), environ))) {
        perror("posix_spawn");
        exit(1);
    }

    bool enable_num_lock = keyboard_settings_config->read_bool_entry("StartupEnable", "NumLock", true);
    auto keyboard_device = TRY(Core::File::open("/dev/keyboard0", Core::OpenMode::ReadOnly));
    TRY(Core::System::ioctl(keyboard_device->fd(), KEYBOARD_IOCTL_SET_NUM_LOCK, enable_num_lock));

    return 0;
}
