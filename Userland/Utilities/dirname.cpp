/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Tim Ledbetter <timledbetter@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    bool null_terminated = false;
    Vector<ByteString> paths;
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Return the directory portion of the given path(s).");
    args_parser.add_option(null_terminated, "End each output line with \\0, rather than \\n", "zero", 'z');
    args_parser.add_positional_argument(paths, "Path to get dirname from", "path");
    args_parser.parse(arguments);

    auto const delimiter = null_terminated ? '\0' : '\n';
    for (auto const& path : paths)
        out("{}{}", LexicalPath::dirname(path), delimiter);

    return 0;
}
