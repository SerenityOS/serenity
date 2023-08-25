/*
 * Copyright (c) 2020-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath"));
    TRY(Core::System::unveil("/dev/beep", "rw"));
    TRY(Core::System::unveil(nullptr, nullptr));
    Optional<size_t> tone;
    Optional<size_t> milliseconds_duration;
    Core::ArgsParser args_parser;
    args_parser.add_option(tone, "Beep tone", "beep-tone", 'f', "Beep tone (frequency in Hz)");
    args_parser.add_option(milliseconds_duration, "Duration", "duration", 'n', "Duration (in milliseconds)");
    args_parser.parse(arguments);
    TRY(Core::System::beep(tone.value_or(440), milliseconds_duration.value_or(200)));
    return 0;
}
