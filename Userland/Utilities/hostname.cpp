/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <limits.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments args)
{
    StringView hostname {};

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(hostname, "Hostname to set", "hostname", Core::ArgsParser::Required::No);
    args_parser.parse(args);

    if (hostname.is_empty()) {
        outln("{}", TRY(Core::System::gethostname()));
    } else {
        if (hostname.length() >= HOST_NAME_MAX) {
            warnln("Hostname must be less than {} characters", HOST_NAME_MAX);
            return 1;
        }
        TRY(Core::System::sethostname(hostname));
    }
    return 0;
}
