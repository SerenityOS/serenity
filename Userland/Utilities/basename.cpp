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
    args_parser.add_positional_argument(path, "Path to get basename from", "path");
    args_parser.add_positional_argument(suffix, "Suffix to strip from name", "suffix", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    auto result = TRY(LexicalPath::basename(path));

    if (!suffix.is_null() && result.bytes().size() != suffix.length() && result.ends_with_bytes(suffix))
        result = TRY(result.substring_from_byte_offset(0, result.bytes().size() - suffix.length()));

    outln("{}", result);
    return 0;
}
