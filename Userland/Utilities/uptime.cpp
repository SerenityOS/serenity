/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    auto file = TRY(Core::File::open("/sys/kernel/uptime"sv, Core::File::OpenMode::Read));

    TRY(Core::System::pledge("stdio"));

    Array<u8, BUFSIZ> buffer;
    auto read_buffer = TRY(file->read_some(buffer));
    auto maybe_seconds = AK::StringUtils::convert_to_uint(StringView(read_buffer));
    if (!maybe_seconds.has_value())
        return Error::from_string_literal("Couldn't convert to number");
    auto seconds = maybe_seconds.release_value();

    outln("Up {}", human_readable_time(seconds));
    return 0;
}
