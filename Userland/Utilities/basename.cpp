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

    auto basename = LexicalPath(path).basename();
    if (!suffix.is_null() && basename.length() != suffix.length() && basename.ends_with(suffix))
        outln("{}", basename.substring_view(0, basename.length() - suffix.length()));
    else
        outln("{}", basename);
    return 0;
}
