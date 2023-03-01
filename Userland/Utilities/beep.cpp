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
    Optional<size_t> tone;
    Core::ArgsParser args_parser;
    args_parser.add_option(tone, "Beep tone", "beep-tone", 'f', "Beep tone (frequency in Hz)");
    args_parser.parse(arguments);
    TRY(Core::System::beep(tone));
    return 0;
}
