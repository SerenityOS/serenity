/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteBuffer.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Utf8View.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibRegex/Regex.h>
#include <stdio.h>
#include <unistd.h>

enum class BinaryFileMode {
    Binary,
    Text,
    Skip,
};

template<typename... Ts>
void fail(StringView format, Ts... args)
{
    warn("\x1b[31m");
    warnln(format, forward<Ts>(args)...);
    warn("\x1b[0m");
    abort();
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> files;

    bool recursive { false };
    bool use_ere { false };
    Vector<const char*> patterns;
    BinaryFileMode binary_mode { BinaryFileMode::Binary };
    bool case_insensitive = false;
    bool line_numbers = false;
    bool invert_match = false;
    bool quiet_mode = false;
    bool suppress_errors = false;
    bool colored_output = isatty(STDOUT_FILENO);
    bool count_lines = false;

    size_t matched_line_count = 0;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Recursively scan files", "recursive", 'r');
    args_parser.add_option(use_ere, "Extended regular expressions", "extended-regexp", 'E');
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Pattern",
        .long_name = "regexp",
        .short_name = 'e',
        .value_name = "Pattern",
        .accept_value = [&](auto* str) {
            patterns.append(str);
            return true;
        },
    });
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", nullptr, 'i');
    args_parser.add_option(line_numbers, "Output line-numbers", "line-numbers", 'n');
    args_parser.add_option(invert_match, "Select non-matching lines", "invert-match", 'v');
    args_parser.add_option(quiet_mode, "Do not write anything to standard output", "quiet", 'q');
    args_parser.add_option(suppress_errors, "Suppress error messages for nonexistent or unreadable files", "no-messages", 's');
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Action to take for binary files ([binary], text, skip)",
        .long_name = "binary-mode",
        .accept_value = [&](auto* str) {
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
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = false,
        .help_string = "Treat binary files as text (same as --binary-mode text)",
        .long_name = "text",
        .short_name = 'a',
        .accept_value = [&](auto) {
            binary_mode = BinaryFileMode::Text;
            return true;
        },
    });
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = false,
        .help_string = "Ignore binary files (same as --binary-mode skip)",
        .long_name = nullptr,
        .short_name = 'I',
        .accept_value = [&](auto) {
            binary_mode = BinaryFileMode::Skip;
            return true;
        },
    });
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "When to use colored output for the matching text ([auto], never, always)",
        .long_name = "color",
        .short_name = 0,
        .value_name = "WHEN",
        .accept_value = [&](auto* str) {
            if ("never"sv == str)
                colored_output = false;
            else if ("always"sv == str)
                colored_output = true;
            else if ("auto"sv != str)
                return false;
            return true;
        },
    });
    args_parser.add_option(count_lines, "Output line count instead of line contents", "count", 'c');
    args_parser.add_positional_argument(files, "File(s) to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    // mock grep behavior: if -e is omitted, use first positional argument as pattern
    if (patterns.size() == 0 && files.size())
        patterns.append(files.take_first());

    auto user_has_specified_files = !files.is_empty();
    auto user_specified_multiple_files = files.size() >= 2;

    PosixOptions options {};
    if (case_insensitive)
        options |= PosixFlags::Insensitive;

    auto grep_logic = [&](auto&& regular_expressions) {
        for (auto& re : regular_expressions) {
            if (re.parser_result.error != regex::Error::NoError) {
                return 1;
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
                    outln(colored_output ? "binary file \x1B[34m{}\x1B[0m matches" : "binary file {} matches", filename);
                } else {
                    if ((result.matches.size() || invert_match) && print_filename)
                        out(colored_output ? "\x1B[34m{}:\x1B[0m" : "{}:", filename);
                    if ((result.matches.size() || invert_match) && line_numbers)
                        out(colored_output ? "\x1B[35m{}:\x1B[0m" : "{}:", line_number);

                    for (auto& match : result.matches) {
                        out(colored_output ? "{}\x1B[32m{}\x1B[0m" : "{}{}",
                            StringView(&str[last_printed_char_pos], match.global_offset - last_printed_char_pos),
                            match.view.to_string());
                        last_printed_char_pos = match.global_offset + match.view.length();
                    }
                    outln("{}", StringView(&str[last_printed_char_pos], str.length() - last_printed_char_pos));
                }

                return true;
            }

            return false;
        };

        auto handle_file = [&matches, binary_mode, suppress_errors, count_lines, quiet_mode,
                               user_specified_multiple_files, &matched_line_count](StringView filename, bool print_filename) -> bool {
            auto file = Core::File::construct(filename);
            if (!file->open(Core::OpenMode::ReadOnly)) {
                if (!suppress_errors)
                    warnln("Failed to open {}: {}", filename, file->error_string());
                return false;
            }

            for (size_t line_number = 1; file->can_read_line(); ++line_number) {
                auto line = file->read_line();
                auto is_binary = memchr(line.characters(), 0, line.length()) != nullptr;

                if (matches(line, filename, line_number, print_filename, is_binary) && is_binary && binary_mode == BinaryFileMode::Binary)
                    break;
            }

            if (count_lines && !quiet_mode) {
                if (user_specified_multiple_files)
                    outln("{}:{}", filename, matched_line_count);
                else
                    outln("{}", matched_line_count);
                matched_line_count = 0;
            }

            return true;
        };

        auto add_directory = [&handle_file, user_has_specified_files](String base, Optional<String> recursive, auto handle_directory) -> void {
            Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
            while (it.has_next()) {
                auto path = it.next_full_path();
                if (!Core::File::is_directory(path)) {
                    auto key = user_has_specified_files ? path.view() : path.substring_view(base.length() + 1, path.length() - base.length() - 1);
                    handle_file(key, true);
                } else {
                    handle_directory(base, path, handle_directory);
                }
            }
        };

        bool did_match_something = false;
        if (!files.size() && !recursive) {
            char* line = nullptr;
            size_t line_len = 0;
            ssize_t nread = 0;
            ScopeGuard free_line = [line] { free(line); };
            size_t line_number = 0;
            while ((nread = getline(&line, &line_len, stdin)) != -1) {
                VERIFY(nread > 0);
                if (line[nread - 1] == '\n')
                    --nread;
                // Human-readable indexes start at 1, so it's fine to increment already.
                line_number += 1;
                StringView line_view(line, nread);
                bool is_binary = line_view.contains(0);

                if (is_binary && binary_mode == BinaryFileMode::Skip)
                    return 1;

                auto matched = matches(line_view, "stdin", line_number, false, is_binary);
                did_match_something = did_match_something || matched;
                if (matched && is_binary && binary_mode == BinaryFileMode::Binary)
                    break;
            }

            if (count_lines && !quiet_mode)
                outln("{}", matched_line_count);
        } else {
            if (recursive) {
                if (user_has_specified_files) {
                    for (auto& filename : files) {
                        add_directory(filename, {}, add_directory);
                    }
                } else {
                    add_directory(".", {}, add_directory);
                }

            } else {
                bool print_filename { files.size() > 1 };
                for (auto& filename : files) {
                    if (!handle_file(filename, print_filename))
                        return 1;
                }
            }
        }

        return did_match_something ? 0 : 1;
    };

    if (use_ere) {
        Vector<Regex<PosixExtended>> regular_expressions;
        for (auto pattern : patterns) {
            regular_expressions.append(Regex<PosixExtended>(pattern, options));
        }
        return grep_logic(regular_expressions);
    }

    Vector<Regex<PosixBasic>> regular_expressions;
    for (auto pattern : patterns) {
        regular_expressions.append(Regex<PosixBasic>(pattern, options));
    }
    return grep_logic(regular_expressions);
}
