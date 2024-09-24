/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 * Copyright (c) 2023, Karol Kosek <krkk@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/ScopeGuard.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>
#include <LibRegex/Regex.h>
#include <LibURL/URL.h>
#include <stdio.h>
#include <unistd.h>

enum class BinaryFileMode {
    Binary,
    Text,
    Skip,
};

enum class ExitStatus {
    SomethingMatched = 0,
    NoLinesMatched = 1,
    ErrorOccurred = 2, // NOTE: `-q` flag also silences errors.
};

template<typename... Ts>
void fail(StringView format, Ts... args)
{
    warn("\x1b[31m");
    warnln(format, forward<Ts>(args)...);
    warn("\x1b[0m");
    abort();
}

constexpr StringView ere_special_characters = ".^$*+?()[{\\|"sv;
constexpr StringView basic_special_characters = ".^$*[\\"sv;

static ByteString escape_characters(StringView string, StringView characters)
{
    StringBuilder builder;
    for (auto ch : string) {
        if (characters.contains(ch))
            builder.append('\\');

        builder.append(ch);
    }
    return builder.to_byte_string();
}

static ByteString& hostname()
{
    static ByteString s_hostname;
    if (s_hostname.is_empty()) {
        auto result = Core::System::gethostname();
        if (result.is_error())
            s_hostname = "localhost";
        else
            s_hostname = result.release_value();
    }
    return s_hostname;
}

enum class PrintType {
    Path = 1 << 0,
    LineNumbers = 1 << 1,
};
AK_ENUM_BITWISE_OPERATORS(PrintType)

static void append_formatted_path(StringBuilder& builder, StringView path, Optional<size_t> line_number, PrintType print_type, bool with_hyperlinks, bool with_color)
{
    if (path == '-') {
        path = "(standard input)"sv;
        with_hyperlinks = false;
    }

    if (with_hyperlinks) {
        auto full_path_or_error = FileSystem::real_path(path);
        if (!full_path_or_error.is_error()) {
            auto fullpath = full_path_or_error.release_value();
            auto url = URL::create_with_file_scheme(fullpath, {}, hostname());
            if (has_flag(print_type, PrintType::LineNumbers) && line_number.has_value())
                url.set_query(MUST(String::formatted("line_number={}", *line_number)));
            builder.appendff("\033]8;;{}\033\\", url.serialize());
        }
    }
    if (has_flag(print_type, PrintType::Path)) {
        if (with_color)
            builder.appendff("\033[34m{}\033[39m", path);
        else
            builder.append(path);
    }
    if (has_flag(print_type, PrintType::LineNumbers) && line_number.has_value()) {
        if (has_flag(print_type, PrintType::Path))
            builder.append(':');

        if (with_color)
            builder.appendff("\033[35m{}\033[39m", *line_number);
        else
            builder.appendff("{}", *line_number);
    }
    if (with_hyperlinks)
        builder.append("\033]8;;\033\\"sv);
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath"));

    ByteString program_name = AK::LexicalPath::basename(args.strings[0]);

    Vector<ByteString> files;

    bool recursive = (program_name == "rgrep"sv);
    bool use_ere = (program_name == "egrep"sv);
    bool fixed_strings = (program_name == "fgrep"sv);
    Vector<ByteString> patterns;
    StringView pattern_file;
    BinaryFileMode binary_mode { BinaryFileMode::Binary };
    bool case_insensitive = false;
    bool line_numbers = false;
    bool invert_match = false;
    bool quiet_mode = false;
    bool suppress_errors = false;
    bool is_a_tty = isatty(STDOUT_FILENO) == 1;
    bool colored_output = is_a_tty;
    bool disable_hyperlinks = !is_a_tty;
    bool count_lines = false;

    size_t matched_line_count = 0;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Recursively scan files", "recursive", 'r');
    args_parser.add_option(use_ere, "Extended regular expressions", "extended-regexp", 'E');
    args_parser.add_option(fixed_strings, "Treat pattern as a string, not a regexp", "fixed-strings", 'F');
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Pattern",
        .long_name = "regexp",
        .short_name = 'e',
        .value_name = "Pattern",
        .accept_value = [&](StringView str) {
            patterns.append(str);
            return true;
        },
    });
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Read patterns from a file",
        .long_name = "file",
        .short_name = 'f',
        .value_name = "File",
        .accept_value = [&](StringView str) {
            pattern_file = str;
            return true;
        },
    });
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", nullptr, 'i');
    args_parser.add_option(line_numbers, "Output line-numbers", "line-numbers", 'n');
    args_parser.add_option(invert_match, "Select non-matching lines", "invert-match", 'v');
    args_parser.add_option(quiet_mode, "Do not write anything to standard output", "quiet", 'q');
    args_parser.add_option(suppress_errors, "Suppress error messages for nonexistent or unreadable files", "no-messages", 's');
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "Action to take for binary files ([binary], text, skip)",
        .long_name = "binary-mode",
        .accept_value = [&](StringView str) {
            if ("text"sv == str)
                binary_mode = BinaryFileMode::Text;
            else if ("binary"sv == str)
                binary_mode = BinaryFileMode::Binary;
            else if ("skip"sv == str)
                binary_mode = BinaryFileMode::Skip;
            else
                return false;
            return true;
        },
    });
    args_parser.add_option(binary_mode, BinaryFileMode::Text, "Treat binary files as text (same as --binary-mode text)", "text", 'a');
    args_parser.add_option(binary_mode, BinaryFileMode::Skip, "Ignore binary files (same as --binary-mode skip)", nullptr, 'I');
    args_parser.add_option(Core::ArgsParser::Option {
        .argument_mode = Core::ArgsParser::OptionArgumentMode::Required,
        .help_string = "When to use colored output for the matching text ([auto], never, always)",
        .long_name = "color",
        .short_name = 0,
        .value_name = "WHEN",
        .accept_value = [&](StringView str) {
            if ("never"sv == str)
                colored_output = false;
            else if ("always"sv == str)
                colored_output = true;
            else if ("auto"sv != str)
                return false;
            return true;
        },
    });
    args_parser.add_option(disable_hyperlinks, "Disable hyperlinks", "no-hyperlinks");
    args_parser.add_option(count_lines, "Output line count instead of line contents", "count", 'c');
    args_parser.add_positional_argument(files, "File(s) to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(args);

    if (!pattern_file.is_empty()) {
        auto file = TRY(Core::File::open(pattern_file, Core::File::OpenMode::Read));
        auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));
        auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
        while (!buffered_file->is_eof()) {
            auto next_pattern = TRY(buffered_file->read_line_with_resize(buffer));
            // Empty lines represent a valid pattern, but the trailing newline
            // should be ignored.
            if (next_pattern.is_empty() && buffered_file->is_eof())
                break;
            patterns.append(next_pattern.to_byte_string());
        }
    }

    // mock grep behavior: if -e is omitted, use first positional argument as pattern
    if (patterns.size() == 0 && files.size())
        patterns.append(files.take_first());

    auto user_has_specified_files = !files.is_empty();

    PosixOptions options {};
    if (case_insensitive)
        options |= PosixFlags::Insensitive;

    auto grep_logic = [&](auto&& regular_expressions) {
        for (auto& re : regular_expressions) {
            if (re.parser_result.error != regex::Error::NoError) {
                warnln("regex parse error: {}", regex::get_error_string(re.parser_result.error));
                return ExitStatus::ErrorOccurred;
            }
        }

        auto matches = [&](StringView str, StringView filename, size_t line_number, bool print_filename, bool is_binary) {
            size_t last_printed_char_pos { 0 };
            if (is_binary && binary_mode == BinaryFileMode::Skip)
                return false;

            for (auto& re : regular_expressions) {
                auto result = re.match(str, PosixFlags::Global);
                if (!(result.success ^ invert_match))
                    continue;

                if (quiet_mode)
                    return true;

                if (count_lines) {
                    matched_line_count++;
                    return true;
                }

                if (is_binary && binary_mode == BinaryFileMode::Binary) {
                    StringBuilder filename_builder;
                    append_formatted_path(filename_builder, filename, {}, PrintType::Path, !disable_hyperlinks, colored_output);
                    outln("binary file {} matches"sv, filename_builder.string_view());
                } else {
                    PrintType print_type { 0 };
                    if (print_filename)
                        print_type |= PrintType::Path;
                    if (line_numbers)
                        print_type |= PrintType::LineNumbers;

                    if ((result.matches.size() || invert_match) && has_any_flag(print_type, PrintType::Path | PrintType::LineNumbers)) {
                        StringBuilder filename_builder;
                        append_formatted_path(filename_builder, filename, line_number, print_type, !disable_hyperlinks, colored_output);
                        out("{}:"sv, filename_builder.string_view());
                    }

                    for (auto& match : result.matches) {
                        auto pre_match_length = match.global_offset - last_printed_char_pos;
                        out(colored_output ? "{}\x1B[32m{}\x1B[0m"sv : "{}{}"sv,
                            pre_match_length > 0 ? StringView(&str[last_printed_char_pos], pre_match_length) : ""sv,
                            match.view.to_byte_string());
                        last_printed_char_pos = match.global_offset + match.view.length();
                    }
                    auto remaining_length = str.length() - last_printed_char_pos;
                    outln("{}", remaining_length > 0 ? StringView(&str[last_printed_char_pos], remaining_length) : ""sv);
                }

                return true;
            }

            return false;
        };

        auto exit_status = ExitStatus::NoLinesMatched;

        auto handle_file = [&matches, binary_mode, count_lines, quiet_mode, disable_hyperlinks, colored_output,
                               &matched_line_count, &exit_status](StringView filename, bool print_filename) -> ErrorOr<void> {
            auto file = TRY(Core::File::open_file_or_standard_stream(filename, Core::File::OpenMode::Read));
            auto buffered_file = TRY(Core::InputBufferedFile::create(move(file)));

            auto buffer = TRY(ByteBuffer::create_uninitialized(PAGE_SIZE));
            for (size_t line_number = 1; !buffered_file->is_eof(); ++line_number) {
                auto line = TRY(buffered_file->read_line_with_resize(buffer));
                if (line.is_empty() && buffered_file->is_eof())
                    break;

                auto is_binary = line.contains('\0');

                auto matched = matches(line, filename, line_number, print_filename, is_binary);
                if (matched) {
                    if (exit_status == ExitStatus::NoLinesMatched)
                        exit_status = ExitStatus::SomethingMatched;
                    if (is_binary && binary_mode == BinaryFileMode::Binary)
                        break;
                }
            }

            if (count_lines && !quiet_mode) {
                if (print_filename) {
                    StringBuilder filename_builder;
                    append_formatted_path(filename_builder, filename, {}, PrintType::Path, !disable_hyperlinks, colored_output);
                    out("{}:", filename_builder.string_view());
                }
                outln("{}", matched_line_count);
                matched_line_count = 0;
            }

            return {};
        };

        auto add_directory = [&handle_file, &exit_status, user_has_specified_files, suppress_errors](ByteString base, Optional<ByteString> recursive, auto handle_directory) -> void {
            Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
            while (it.has_next()) {
                auto path = it.next_full_path();
                if (!FileSystem::is_directory(path)) {
                    // Remove leading './' when `grep -r` was run without any specified paths.
                    auto key = user_has_specified_files ? path.view() : path.substring_view(base.length() + 1);
                    if (auto result = handle_file(key, true); result.is_error() && !suppress_errors) {
                        warnln("Failed with file {}: {}", key, result.release_error());
                        exit_status = ExitStatus::ErrorOccurred;
                    }

                } else {
                    handle_directory(base, path, handle_directory);
                }
            }
        };

        if (recursive) {
            if (!user_has_specified_files)
                files.append("."sv);

            for (auto& filename : files) {
                add_directory(filename, {}, add_directory);
            }
        } else {
            if (!user_has_specified_files)
                files.append("-"sv);

            bool print_filename { files.size() > 1 };
            for (auto& filename : files) {
                auto result = handle_file(filename, print_filename);
                if (result.is_error() && !suppress_errors) {
                    warnln("Failed with file {}: {}", filename, result.release_error());
                    exit_status = ExitStatus::ErrorOccurred;
                }
            }
        }

        return exit_status;
    };

    if (use_ere) {
        Vector<Regex<PosixExtended>> regular_expressions;
        for (auto pattern : patterns) {
            auto escaped_pattern = (fixed_strings) ? escape_characters(pattern, ere_special_characters) : pattern;
            regular_expressions.append(Regex<PosixExtended>(escaped_pattern, options));
        }
        return to_underlying(grep_logic(regular_expressions));
    }

    Vector<Regex<PosixBasic>> regular_expressions;
    for (auto pattern : patterns) {
        auto escaped_pattern = (fixed_strings) ? escape_characters(pattern, basic_special_characters) : pattern;
        regular_expressions.append(Regex<PosixBasic>(escaped_pattern, options));
    }
    return to_underlying(grep_logic(regular_expressions));
}
