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

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::unveil("/dev/"sv, "rwc"sv));
    TRY(Core::System::unveil("/etc/group"sv, "rw"sv));
    TRY(Core::System::unveil(nullptr, nullptr));
    TRY(Core::System::pledge("stdio rpath dpath wpath cpath chown fattr"));

    auto file_or_error = Core::File::open("/dev/devctl"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        warnln("Failed to open /dev/devctl - {}", file_or_error.error());
        VERIFY_NOT_REACHED();
    }
    auto file = file_or_error.release_value();
    DeviceMapper::DeviceEventLoop device_event_loop(move(file));
    if (auto result = device_event_loop.drain_events_from_devctl(); result.is_error())
        dbgln("DeviceMapper: Fatal error: {}", result.release_error());
    // NOTE: If we return from drain_events_from_devctl, it must be an error
    // so we should always return 1!
    return 1;
}
