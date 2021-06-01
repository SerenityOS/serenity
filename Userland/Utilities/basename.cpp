/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    StringView path;
    StringView suffix;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to get basename from", "path");
    args_parser.add_positional_argument(suffix, "Suffix to strip from name", "suffix", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    auto result = LexicalPath(path).basename();

    if (!suffix.is_null() && result.length() != suffix.length() && result.ends_with(suffix))
        result = result.substring(0, result.length() - suffix.length());

    outln("{}", result);
    return 0;
}
