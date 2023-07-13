/*
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDiff/Applier.h>
#include <LibDiff/Hunks.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>

static ErrorOr<void> do_patch(StringView path_of_file_to_patch, Diff::Patch const& patch)
{
    auto file_to_patch = TRY(Core::File::open(path_of_file_to_patch, Core::File::OpenMode::Read));
    auto content = TRY(file_to_patch->read_until_eof());
    auto lines = StringView(content).lines();

    // Apply patch to a temporary file in case one or more of the hunks fails.
    char tmp_output[] = "/tmp/patch.XXXXXX";
    auto tmp_file = TRY(Core::File::adopt_fd(TRY(Core::System::mkstemp(tmp_output)), Core::File::OpenMode::ReadWrite));

    TRY(Diff::apply_patch(*tmp_file, lines, patch));

    return FileSystem::move_file(path_of_file_to_patch, StringView { tmp_output, sizeof(tmp_output) });
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    StringView directory;

    Core::ArgsParser args_parser;
    args_parser.add_option(directory, "Change the working directory to <directory> before applying the patch file", "directory", 'd', "directory");
    args_parser.parse(arguments);

    if (!directory.is_null())
        TRY(Core::System::chdir(directory));

    auto input = TRY(Core::File::standard_input());

    auto patch_content = TRY(input->read_until_eof());

    // FIXME: Support multiple patches in the patch file.
    Diff::Parser parser(patch_content);
    Diff::Patch patch;
    patch.header = TRY(parser.parse_header());
    patch.hunks = TRY(parser.parse_hunks());

    // FIXME: Support adding/removing a file, and asking for file to patch as fallback otherwise.
    StringView to_patch;
    if (FileSystem::is_regular_file(patch.header.old_file_path)) {
        to_patch = patch.header.old_file_path;
    } else if (FileSystem::is_regular_file(patch.header.new_file_path)) {
        to_patch = patch.header.new_file_path;
    } else {
        warnln("Unable to determine file to patch");
        return 1;
    }

    outln("patching file {}", to_patch);
    TRY(do_patch(to_patch, patch));

    return 0;
}
