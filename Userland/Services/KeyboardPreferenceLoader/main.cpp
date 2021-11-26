/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ConfigFile.h>
#include <LibCore/File.h>
#include <errno.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>

int main()
{
    if (pledge("stdio proc exec rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto keyboard_settings_config = Core::ConfigFile::open_for_app("KeyboardSettings");

    if (unveil("/bin/keymap", "x") < 0) {
        perror("unveil /bin/keymap");
        return 1;
    }

    if (unveil("/etc/Keyboard.ini", "r") < 0) {
        perror("unveil /etc/Keyboard.ini");
        return 1;
    }

    if (unveil("/dev/keyboard0", "r") < 0) {
        perror("unveil /dev/keyboard0");
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

    auto keyboard_device_or_error = Core::File::open("/dev/keyboard0", Core::OpenMode::ReadOnly);
    if (keyboard_device_or_error.is_error()) {
        warnln("Failed to open /dev/keyboard0: {}", keyboard_device_or_error.error());
        VERIFY_NOT_REACHED();
    }
    auto keyboard_device = keyboard_device_or_error.release_value();

    int rc = ioctl(keyboard_device->fd(), KEYBOARD_IOCTL_SET_NUM_LOCK, enable_num_lock);
    if (rc < 0) {
        perror("ioctl(KEYBOARD_IOCTL_SET_NUM_LOCK)");
        return 1;
    }
}
