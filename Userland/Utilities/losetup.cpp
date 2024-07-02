/*
 * Copyright (c) 2024, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <AK/StringUtils.h>
#include <AK/StringView.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath"));
    StringView path;
    bool flag_delete_device = false;
    bool flag_add_new_device = false;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Manage loop devices.");
    args_parser.add_option(flag_delete_device, "Delete a loop device", "delete", 'd');
    args_parser.add_option(flag_add_new_device, "Add new device", "create", 'c');
    args_parser.add_positional_argument(path, "Path", "path", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (!(flag_delete_device || flag_add_new_device))
        return Error::from_string_literal("No specified option was requested.");

    if (path.is_null())
        return Error::from_string_literal("No specified path to handle.");

    auto devctl_device = TRY(Core::File::open("/dev/devctl"sv, Core::File::OpenMode::Read));
    if (flag_delete_device) {
        constexpr StringView base_dev_loop_path = "/dev/loop/"sv;
        if (!path.starts_with(base_dev_loop_path))
            return Error::from_string_literal("Invalid loop device path.");
        auto number = path.substring_view(base_dev_loop_path.length());
        auto possible_number = number.to_number<u64>();
        if (!possible_number.has_value())
            return Error::from_string_literal("Invalid loop device number.");
        auto loop_device_index = possible_number.release_value();
        TRY(Core::System::ioctl(devctl_device->fd(), DEVCTL_DESTROY_LOOP_DEVICE, &loop_device_index));
        return 0;
    }

    VERIFY(flag_add_new_device);
    auto value = TRY(Core::System::open(path, O_RDWR));
    TRY(Core::System::ioctl(devctl_device->fd(), DEVCTL_CREATE_LOOP_DEVICE, &value));
    int loop_device_index = value;

    auto loop_device_path = TRY(String::formatted("/dev/loop/{}", loop_device_index));
    outln("Created new device at {}", loop_device_path);
    return 0;
}
