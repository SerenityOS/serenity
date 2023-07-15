/*
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibDiff/Format.h>
#include <LibDiff/Generator.h>
#include <LibMain/Main.h>
#include <unistd.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser parser;

    bool unified = false;
    bool context = false;

    Optional<size_t> unified_format_context;
    Optional<size_t> context_format_context;

    StringView filename1;
    StringView filename2;

    parser.add_positional_argument(filename1, "First file to compare", "file1", Core::ArgsParser::Required::Yes);
    parser.add_positional_argument(filename2, "Second file to compare", "file2", Core::ArgsParser::Required::Yes);
    parser.add_option(unified, "Write diff in unified format", nullptr, 'u');
    parser.add_option(context, "Write diff in context format", nullptr, 'c');
    parser.add_option(unified_format_context, "Write diff in unified format with the given number of context lines", "unified", 'U', "lines");
    parser.add_option(context_format_context, "Write diff in context format with the given number of context lines", "context", 'C', "lines");
    parser.parse(arguments);

    auto file1 = TRY(Core::File::open(filename1, Core::File::OpenMode::Read));
    auto file2 = TRY(Core::File::open(filename2, Core::File::OpenMode::Read));
    auto out = TRY(Core::File::standard_output());

    auto const color_output = TRY(Core::System::isatty(STDOUT_FILENO)) ? Diff::ColorOutput::Yes : Diff::ColorOutput::No;

    size_t number_context_lines = 0;

    if (unified_format_context.has_value()) {
        number_context_lines = *unified_format_context;
        unified = true;
    } else if (context_format_context.has_value()) {
        number_context_lines = *context_format_context;
        context = true;
    } else if (context || unified) {
        // Default for these formats is 3, but 0 in normal format.
        number_context_lines = 3;
    }

    auto hunks = TRY(Diff::from_text(TRY(file1->read_until_eof()), TRY(file2->read_until_eof()), number_context_lines));

    if (hunks.is_empty())
        return 0;

    if (unified) {
        TRY(Diff::write_unified_header(filename1, filename2, *out));
        for (auto const& hunk : hunks)
            TRY(Diff::write_unified(hunk, *out, color_output));
    } else if (context) {
        TRY(Diff::write_context_header(filename1, filename2, *out));
        for (auto const& hunk : hunks)
            TRY(Diff::write_context(hunk, *out, color_output));
    } else {
        for (auto const& hunk : hunks)
            TRY(Diff::write_normal(hunk, *out, color_output));
    }

    return 1;
}
