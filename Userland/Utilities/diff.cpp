/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibDiff/Generator.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Core::ArgsParser parser;
    char const* filename1;
    char const* filename2;

    parser.add_positional_argument(filename1, "First file to compare", "file1", Core::ArgsParser::Required::Yes);
    parser.add_positional_argument(filename2, "Second file to compare", "file2", Core::ArgsParser::Required::Yes);
    parser.parse(argc, argv, Core::ArgsParser::FailureBehavior::PrintUsageAndExit);

    auto file1 = Core::File::construct(filename1);
    if (!file1->open(Core::OpenMode::ReadOnly)) {
        warnln("Error: Cannot open {}: {}", filename1, file1->error_string());
        return 1;
    }
    auto file2 = Core::File::construct(filename2);
    if (!file2->open(Core::OpenMode::ReadOnly)) {
        warnln("Error: Cannot open {}: {}", filename2, file2->error_string());
        return 1;
    }

    auto hunks = Diff::from_text(file1->read_all(), file2->read_all());
    for (const auto& hunk : hunks) {
        outln("Hunk: {}, {}", hunk.original_start_line, hunk.target_start_line);
        for (const auto& line : hunk.removed_lines) {
            outln("\033[31;1m< {}\033[0m", line);
        }
        if (hunk.added_lines.size() > 0 && hunk.removed_lines.size() > 0)
            outln("---");
        for (const auto& line : hunk.added_lines)
            outln("\033[32;1m> {}\033[0m", line);
    }

    return hunks.is_empty() ? 0 : 1;
}
