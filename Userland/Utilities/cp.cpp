/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <stdio.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath fattr chown"));

    bool link = false;
    auto preserve = Core::Stream::PreserveMode::Nothing;
    bool recursion_allowed = false;
    bool verbose = false;
    Vector<StringView> sources;
    DeprecatedString destination;

    Core::ArgsParser args_parser;
    args_parser.add_option(link, "Link files instead of copying", "link", 'l');
    args_parser.add_option({
        Core::ArgsParser::OptionArgumentMode::Optional,
        "Preserve a selection of mode, ownership and timestamps. Defaults to all three if the option is present but no list is given.",
        "preserve",
        'p',
        "attributes",
        [&preserve](char const* s) {
            if (!s) {
                preserve = Core::Stream::PreserveMode::Permissions | Core::Stream::PreserveMode::Ownership | Core::Stream::PreserveMode::Timestamps;
                return true;
            }

            bool values_ok = true;

            StringView { s, strlen(s) }.for_each_split_view(',', SplitBehavior::Nothing, [&](StringView value) {
                if (value == "mode"sv) {
                    preserve |= Core::Stream::PreserveMode::Permissions;
                } else if (value == "ownership"sv) {
                    preserve |= Core::Stream::PreserveMode::Ownership;
                } else if (value == "timestamps"sv) {
                    preserve |= Core::Stream::PreserveMode::Timestamps;
                } else {
                    warnln("cp: Unknown or unimplemented --preserve attribute: '{}'", value);
                    values_ok = false;
                }
            });

            return values_ok;
        },
        Core::ArgsParser::OptionHideMode::None,
    });
    args_parser.add_option(recursion_allowed, "Copy directories recursively", "recursive", 'R');
    args_parser.add_option(recursion_allowed, "Same as -R", nullptr, 'r');
    args_parser.add_option(verbose, "Verbose", "verbose", 'v');
    args_parser.add_positional_argument(sources, "Source file paths", "source");
    args_parser.add_positional_argument(destination, "Destination file path", "destination");
    args_parser.parse(arguments);

    if (has_flag(preserve, Core::Stream::PreserveMode::Permissions)) {
        umask(0);
    } else {
        TRY(Core::System::pledge("stdio rpath wpath cpath fattr"));
    }

    bool destination_is_existing_dir = Core::Stream::is_directory(destination);

    for (auto& source : sources) {
        auto destination_path = destination_is_existing_dir
            ? DeprecatedString::formatted("{}/{}", destination, LexicalPath::basename(source))
            : destination;

        auto result = Core::Stream::copy_file_or_directory(
            destination_path, source,
            recursion_allowed ? Core::Stream::RecursionMode::Allowed : Core::Stream::RecursionMode::Disallowed,
            link ? Core::Stream::LinkMode::Allowed : Core::Stream::LinkMode::Disallowed,
            Core::Stream::AddDuplicateFileMarker::No,
            preserve);

        if (result.is_error()) {
            if (result.error().tried_recursing)
                warnln("cp: -R not specified; omitting directory '{}'", source);
            else
                warnln("cp: unable to copy '{}' to '{}': {}", source, destination_path, strerror(result.error().code()));
            return 1;
        }

        if (verbose)
            outln("'{}' -> '{}'", source, destination_path);
    }
    return 0;
}
