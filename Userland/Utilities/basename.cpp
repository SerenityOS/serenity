/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio"));

    StringView path;
    StringView suffix;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Return the filename portion of the given path.");
    args_parser.add_positional_argument(path, "Path to get basename from", "path");
    args_parser.add_positional_argument(suffix, "Suffix to strip from name", "suffix", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto result = LexicalPath::basename(path);

    if (!suffix.is_null() && result.length() != suffix.length() && result.ends_with(suffix))
        result = result.substring_view(0, result.length() - suffix.length());

    outln("{}", result);
    return 0;
}
