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
    bool use_ere { true };
    const char* pattern = nullptr;
    BinaryFileMode binary_mode { BinaryFileMode::Binary };
    bool case_insensitive = false;
    bool invert_match = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(recursive, "Recursively scan files starting in working directory", "recursive", 'r');
    args_parser.add_option(use_ere, "Extended regular expressions (default)", "extended-regexp", 'E');
    args_parser.add_option(pattern, "Pattern", "regexp", 'e', "Pattern");
    args_parser.add_option(case_insensitive, "Make matches case-insensitive", nullptr, 'i');
    args_parser.add_option(invert_match, "Select non-matching lines", "invert-match", 'v');
    args_parser.add_option(Core::ArgsParser::Option {
        .requires_argument = true,
        .help_string = "Action to take for binary files ([binary], text, skip)",
        .long_name = "binary-mode",
        .accept_value = [&](auto* str) {
            if (StringView { "text" } == str)
                binary_mode = BinaryFileMode::Text;
            else if (StringView { "binary" } == str)
                binary_mode = BinaryFileMode::Binary;
            else if (StringView { "skip" } == str)
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
    args_parser.add_positional_argument(files, "File(s) to process", "file", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (!use_ere)
        return 0;

    // mock grep behaviour: if -e is omitted, use first positional argument as pattern
    if (pattern == nullptr && files.size())
        pattern = files.take_first();

    PosixOptions options {};
    if (case_insensitive)
        options |= PosixFlags::Insensitive;

    Regex<PosixExtended> re(pattern, options);
    if (re.parser_result.error != Error::NoError) {
        return 1;
    }

    auto matches = [&](StringView str, StringView filename = "", bool print_filename = false, bool is_binary = false) {
        size_t last_printed_char_pos { 0 };
        if (is_binary && binary_mode == BinaryFileMode::Skip)
            return false;

        auto result = re.match(str, PosixFlags::Global);
        if (result.success ^ invert_match) {
            if (is_binary && binary_mode == BinaryFileMode::Binary) {
                outln("binary file \x1B[34m{}\x1B[0m matches", filename);
            } else {
                if ((result.matches.size() || invert_match) && print_filename) {
                    out("\x1B[34m{}:\x1B[0m", filename);
                }

                for (auto& match : result.matches) {

                    out("{}\x1B[32m{}\x1B[0m",
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

    auto handle_file = [&matches, binary_mode](StringView filename, bool print_filename) -> bool {
        auto file = Core::File::construct(filename);
        if (!file->open(Core::OpenMode::ReadOnly)) {
            warnln("Failed to open {}: {}", filename, file->error_string());
            return false;
        }

        while (file->can_read_line()) {
            auto line = file->read_line();
            auto is_binary = memchr(line.characters(), 0, line.length()) != nullptr;

            if (matches(line, filename, print_filename, is_binary) && is_binary && binary_mode == BinaryFileMode::Binary)
                return true;
        }
        return true;
    };

    auto add_directory = [&handle_file](String base, Optional<String> recursive, auto handle_directory) -> void {
        Core::DirIterator it(recursive.value_or(base), Core::DirIterator::Flags::SkipDots);
        while (it.has_next()) {
            auto path = it.next_full_path();
            if (!Core::File::is_directory(path)) {
                auto key = path.substring_view(base.length() + 1, path.length() - base.length() - 1);
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
        while ((nread = getline(&line, &line_len, stdin)) != -1) {
            VERIFY(nread > 0);
            StringView line_view(line, nread);
            bool is_binary = line_view.contains(0);

            if (is_binary && binary_mode == BinaryFileMode::Skip)
                return 1;

            auto matched = matches(line_view, "stdin", false, is_binary);
            did_match_something = did_match_something || matched;
            if (matched && is_binary && binary_mode == BinaryFileMode::Binary)
                return 0;
        }
    } else {
        if (recursive) {
            add_directory(".", {}, add_directory);

        } else {
            bool print_filename { files.size() > 1 };
            for (auto& filename : files) {
                if (!handle_file(filename, print_filename))
                    return 1;
            }
        }
    }

    return did_match_something ? 0 : 1;
}
