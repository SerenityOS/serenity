/*
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath fattr"));

    bool create_leading_dest_components = false;
    StringView source;
    StringView destination;

    Core::ArgsParser args_parser;
    args_parser.add_option(create_leading_dest_components, "Create leading components of the destination path", nullptr, 'D');
    args_parser.add_positional_argument(source, "Source path", "source");
    args_parser.add_positional_argument(destination, "Destination path", "destination");
    args_parser.parse(arguments);

    if (create_leading_dest_components) {
        String destination_absolute = Core::File::absolute_path(destination);
        Core::File::ensure_parent_directories(destination_absolute);
    }

    TRY(Core::File::copy_file_or_directory(destination, source, Core::File::RecursionMode::Allowed,
        Core::File::LinkMode::Disallowed, Core::File::AddDuplicateFileMarker::No,
        Core::File::PreserveMode::Nothing));

    return 0;
}
