/*
 * Copyright (c) 2022, Beckett Normington <beckett@b0ba.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Stream.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"sv));

    auto file_or_error = Core::Stream::File::open("/etc/nologin"sv, Core::Stream::OpenMode::Read);
    if (file_or_error.is_error()) {
        outln("This account is currently not available."sv);
    } else {
        auto message_from_file = TRY(file_or_error.value()->read_all());
        out(message_from_file);
    }

    return 1;
}
