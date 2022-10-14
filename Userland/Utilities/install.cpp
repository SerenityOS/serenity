/*
 * Copyright (c) 2022, Tim Schumacher <timschumi@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Directory.h>
#include <LibCore/File.h>
#include <LibCore/FilePermissionsMask.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath fattr"));

    bool create_leading_dest_components = false;
    StringView mode = "0755"sv;
    Vector<StringView> sources;
    StringView destination;

    Core::ArgsParser args_parser;
    args_parser.add_ignored(nullptr, 'c'); // "copy files" is the default, no contradicting options exist.
    args_parser.add_option(create_leading_dest_components, "Create leading components of the destination path", nullptr, 'D');
    args_parser.add_option(mode, "Permissions to set (instead of 0755)", "mode", 'm', "mode");
    args_parser.add_positional_argument(sources, "Source path", "source");
    args_parser.add_positional_argument(destination, "Destination path", "destination");
    args_parser.parse(arguments);

    auto permission_mask = TRY(Core::FilePermissionsMask::parse(mode));

    DeprecatedString destination_dir = (sources.size() > 1 ? DeprecatedString { destination } : LexicalPath::dirname(destination));

    if (create_leading_dest_components) {
        DeprecatedString destination_dir_absolute = Core::File::absolute_path(destination_dir);
        MUST(Core::Directory::create(destination_dir_absolute, Core::Directory::CreateDirectories::Yes));
    }

    for (auto const& source : sources) {
        DeprecatedString final_destination;
        if (sources.size() > 1) {
            final_destination = LexicalPath(destination).append(LexicalPath::basename(source)).string();
        } else {
            final_destination = destination;
        }

        TRY(Core::File::copy_file_or_directory(final_destination, source, Core::File::RecursionMode::Allowed,
            Core::File::LinkMode::Disallowed, Core::File::AddDuplicateFileMarker::No,
            Core::File::PreserveMode::Nothing));

        auto current_access = TRY(Core::System::stat(final_destination));
        TRY(Core::System::chmod(final_destination, permission_mask.apply(current_access.st_mode)));
    }

    return 0;
}
