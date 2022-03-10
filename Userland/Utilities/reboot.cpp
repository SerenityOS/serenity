/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/Stream.h>
#include <LibMain/Main.h>
#include <LibSession/Session.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;
    auto force = false;
    args_parser.add_option(force, "Force reboot even if it is inhibited", "force", 'f');
    args_parser.parse(arguments);

    auto& session = Session::Session::the();
    if (session.is_exit_inhibited() && !force) {
        warnln("Reboot is inhibited, use \"reboot -f\" to force");
        session.report_inhibited_exit_prevention();
        return 1;
    }

    auto file = TRY(Core::Stream::File::open("/sys/firmware/power_state", Core::Stream::OpenMode::Write));

    const String file_contents = "1";
    TRY(file->write(file_contents.bytes()));
    file->close();

    return 0;
}
