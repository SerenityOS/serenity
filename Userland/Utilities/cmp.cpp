/*
 * Copyright (c) 2022-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>

static ErrorOr<NonnullOwnPtr<Core::InputBufferedFile>> open_file_or_stdin(StringView filename)
{
    auto file = TRY(Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read));
    return TRY(Core::InputBufferedFile::create(move(file)));
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    Main::set_return_code_for_errors(2);
    TRY(Core::System::pledge("stdio rpath"));

    Core::ArgsParser parser;
    StringView filename1;
    StringView filename2;
    bool verbose = false;
    bool silent = false;

    parser.set_general_help("Compare two files, and report the first byte that does not match. Returns 0 if files are identical, or 1 if they differ.");
    parser.add_positional_argument(filename1, "First file to compare", "file1", Core::ArgsParser::Required::Yes);
    parser.add_positional_argument(filename2, "Second file to compare", "file2", Core::ArgsParser::Required::Yes);
    parser.add_option(verbose, "Output every byte mismatch, not just the first", "verbose", 'l');
    parser.add_option(silent, "Disable all output", "silent", 's');
    parser.parse(arguments);

    // When opening STDIN as both files, the results are undefined.
    // Let's just report that it matches.
    if (filename1 == "-" && filename2 == "-")
        return 0;

    auto file1 = TRY(open_file_or_stdin(filename1));
    auto file2 = TRY(open_file_or_stdin(filename2));
    TRY(Core::System::unveil(nullptr, nullptr));

    int line_number = 1;
    int byte_number = 1;
    Array<u8, 1> buffer1;
    Array<u8, 1> buffer2;
    bool files_match = true;

    auto report_mismatch = [&]() {
        files_match = false;
        if (silent)
            return;
        if (verbose)
            outln("{} {:o} {:o}", byte_number, buffer1[0], buffer2[0]);
        else
            outln("{} {} differ: char {}, line {}", filename1, filename2, byte_number, line_number);
    };

    auto report_eof = [&](auto& shorter_file_name) -> ErrorOr<void> {
        files_match = false;
        if (silent)
            return {};
        auto additional_info = verbose
            ? TRY(String::formatted(" after byte {}", byte_number))
            : TRY(String::formatted(" after byte {}, line {}", byte_number, line_number));
        warnln("cmp: EOF on {}{}", shorter_file_name, additional_info);
        return {};
    };

    while (true) {
        TRY(file1->read_some(buffer1));
        TRY(file2->read_some(buffer2));

        if (file1->is_eof() && file2->is_eof())
            break;

        if (file1->is_eof() || file2->is_eof()) {
            TRY(report_eof(file1->is_eof() ? filename1 : filename2));
            break;
        }

        if (buffer1[0] != buffer2[0]) {
            report_mismatch();
            if (!verbose)
                break;
        }

        if (buffer1[0] == '\n')
            ++line_number;
        ++byte_number;
    }

    return files_match ? 0 : 1;
}
