/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DeviceEventLoop.h"
#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/String.h>
#include <Kernel/API/DeviceEvent.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <stdio.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <sys/types.h>
#include <unistd.h>

using DeviceEventLoop = DeviceMapper::DeviceEventLoop;

static ErrorOr<Vector<DeviceEventLoop::DeviceNodeMatch>> fetch_device_node_matches_from_config_file(Core::ConfigFile const& config)
{
    Vector<DeviceEventLoop::DeviceNodeMatch> matches;
    for (auto const& name : config.groups()) {

        auto family_name = config.read_entry(name, "Name");
        auto devtmpfs_path = config.read_entry(name, "DevTmpFSPath");
        auto type = config.read_entry(name, "Type");

        auto major_number = config.read_entry(name, "MajorNumber").to_number<unsigned>();
        if (!major_number.has_value())
            return Error::from_string_literal("Invalid MajorNumber entry value");

        Optional<unsigned> possible_minor_number {};
        auto minor_number = config.read_entry_optional(name, "MinorNumber");
        if (minor_number.has_value()) {
            possible_minor_number = minor_number.value().to_number<unsigned>();
            if (!possible_minor_number.has_value())
                return Error::from_string_literal("Invalid MinorNumber entry value");
        }

        Optional<MinorNumber> specific_minor_number {};
        if (possible_minor_number.has_value())
            specific_minor_number = static_cast<MinorNumber>(possible_minor_number.value());

        auto group_permissions = config.read_entry(name, "GroupPermissions", "root");

        auto create_permissions = AK::StringUtils::convert_to_uint_from_octal<u16>(config.read_entry(name, "CreatePermissions"), TrimWhitespace::No);
        if (!create_permissions.has_value())
            return Error::from_string_literal("Invalid CreatePermissions entry value");

        DeviceNodeType node_type = (type == "CharacterDevice" ? DeviceNodeType::Character : DeviceNodeType::Block);
        auto match = DeviceEventLoop::DeviceNodeMatch {
            .permission_group = TRY(String::from_byte_string(group_permissions)),
            .family_type_literal = TRY(String::from_byte_string(family_name)),
            .path_pattern = TRY(String::from_byte_string(devtmpfs_path)),
            .device_node_type = node_type,
            .major_number = major_number.value(),
            .specific_minor_number = specific_minor_number,
            .create_mode = create_permissions.value(),
        };
        TRY(matches.try_append(match));
    }
    return matches;
}

ErrorOr<int> serenity_main(Main::Arguments)
{
    auto config = TRY(Core::ConfigFile::open_for_system("DeviceMapper"));
    auto matches = TRY(fetch_device_node_matches_from_config_file(config));

    TRY(Core::System::unveil("/dev/"sv, "rwc"sv));
    TRY(Core::System::unveil("/etc/group"sv, "rw"sv));
    TRY(Core::System::unveil("/tmp/system/devicemap/"sv, "rwc"sv));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio rpath dpath wpath cpath chown fattr"));

    auto file_or_error = Core::File::open("/dev/devctl"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        warnln("Failed to open /dev/devctl - {}", file_or_error.error());
        VERIFY_NOT_REACHED();
    }
    auto file = file_or_error.release_value();
    DeviceMapper::DeviceEventLoop device_event_loop(matches, move(file));
    if (auto result = device_event_loop.drain_events_from_devctl(); result.is_error())
        dbgln("DeviceMapper: Fatal error: {}", result.release_error());
    // NOTE: If we return from drain_events_from_devctl, it must be an error
    // so we should always return 1!
    return 1;
}
