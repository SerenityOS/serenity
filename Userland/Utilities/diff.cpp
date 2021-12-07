/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDiff/Generator.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser parser;
    String filename1;
    String filename2;

    parser.add_positional_argument(filename1, "First file to compare", "file1", Core::ArgsParser::Required::Yes);
    parser.add_positional_argument(filename2, "Second file to compare", "file2", Core::ArgsParser::Required::Yes);
    parser.parse(arguments);

    auto file1 = TRY(Core::File::open(filename1, Core::OpenMode::ReadOnly));
    auto file2 = TRY(Core::File::open(filename2, Core::OpenMode::ReadOnly));

    auto hunks = Diff::from_text(file1->read_all(), file2->read_all());
    for (const auto& hunk : hunks) {
        auto original_start = hunk.original_start_line;
        auto target_start = hunk.target_start_line;
        auto num_added = hunk.added_lines.size();
        auto num_removed = hunk.removed_lines.size();

        StringBuilder sb;
        // Source line(s)
        sb.appendff("{}", original_start);
        if (num_removed > 1)
            sb.appendff(",{}", original_start + num_removed - 1);

        // Action
        if (num_added > 0 && num_removed > 0)
            sb.append("c");
        else if (num_added > 0)
            sb.append("a");
        else
            sb.append("d");

        // Target line(s)
        sb.appendff("{}", target_start);
        if (num_added > 1)
            sb.appendff(",{}", target_start + num_added - 1);

        bool color_output = isatty(STDOUT_FILENO);

        outln("Hunk: {}", sb.build());
        for (const auto& line : hunk.removed_lines) {
            if (color_output)
                outln("\033[31;1m< {}\033[0m", line);
            else
                outln("< {}", line);
        }
        if (num_added > 0 && num_removed > 0)
            outln("---");
        for (const auto& line : hunk.added_lines) {
            if (color_output)
                outln("\033[32;1m> {}\033[0m", line);
            else
                outln("> {}", line);
        }
    }

    return hunks.is_empty() ? 0 : 1;
}
