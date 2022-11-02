/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView new_jail_name;
    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(new_jail_name, "New jail name", "jail name");
    args_parser.parse(arguments);

    TRY(Core::System::pledge("stdio jail"));

    if (!new_jail_name.is_null() && !new_jail_name.is_empty()) {
        TRY(Core::System::create_jail(new_jail_name));
        return 0;
    }

    return Error::from_string_view("Can't create a jail with empty name."sv);
}
