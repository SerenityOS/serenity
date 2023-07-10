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

static bool is_adding_file(Diff::Patch const& patch)
{
    return patch.hunks[0].location.old_range.start_line == 0;
}

static ErrorOr<ByteBuffer> read_content(StringView path_of_file_to_patch, Diff::Patch const& patch)
{
    auto file_to_patch_or_error = Core::File::open(path_of_file_to_patch, Core::File::OpenMode::Read);

    // Trivial case - no error reading the file.
    if (!file_to_patch_or_error.is_error())
        return TRY(file_to_patch_or_error.release_value()->read_until_eof());

    auto const& error = file_to_patch_or_error.error();

    // If the patch is adding a file then it is fine for opening the file to error out if it did not exist.
    if (!is_adding_file(patch) || !error.is_errno() || error.code() != ENOENT)
        return file_to_patch_or_error.release_error();

    return ByteBuffer {};
}

static ErrorOr<void> do_patch(StringView path_of_file_to_patch, Diff::Patch const& patch)
{
    ByteBuffer content = TRY(read_content(path_of_file_to_patch, patch));
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
    Optional<size_t> strip_count;

    Core::ArgsParser args_parser;
    args_parser.add_option(directory, "Change the working directory to <directory> before applying the patch file", "directory", 'd', "directory");
    args_parser.add_option(strip_count, "Strip given number of leading path components from file names (defaults as basename)", "strip", 'p', "count");
    args_parser.parse(arguments);

    if (!directory.is_null())
        TRY(Core::System::chdir(directory));

    auto input = TRY(Core::File::standard_input());

    auto patch_content = TRY(input->read_until_eof());

    // FIXME: Support multiple patches in the patch file.
    Diff::Parser parser(patch_content);
    Diff::Patch patch;
    patch.header = TRY(parser.parse_header(strip_count));
    patch.hunks = TRY(parser.parse_hunks());

    // FIXME: Support adding/removing a file, and asking for file to patch as fallback otherwise.
    StringView to_patch;
    if (FileSystem::is_regular_file(patch.header.old_file_path)) {
        to_patch = patch.header.old_file_path;
    } else if (is_adding_file(patch) || FileSystem::is_regular_file(patch.header.new_file_path)) {
        to_patch = patch.header.new_file_path;
    } else {
        warnln("Unable to determine file to patch");
        return 1;
    }

    outln("patching file {}", to_patch);
    TRY(do_patch(to_patch, patch));

    return 0;
}
