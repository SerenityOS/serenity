/*
 * Copyright (c) 2022, Beckett Normington <beckett@b0ba.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments)
{
    TRY(Core::System::pledge("stdio rpath"sv));

    auto file_or_error = Core::File::open("/etc/nologin"sv, Core::File::OpenMode::Read);
    if (file_or_error.is_error()) {
        outln("This account is currently not available.");
    } else {
        auto message_from_file = TRY(file_or_error.value()->read_until_eof());
        out("{}", StringView { message_from_file });
    }

    return 1;
}
