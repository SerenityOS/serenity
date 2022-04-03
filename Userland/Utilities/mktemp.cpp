/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

constexpr StringView default_template = "tmp.XXXXXXXXXX";

static String generate_random_filename(String const& pattern)
{
    StringBuilder new_filename { pattern.length() };

    constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";
    for (size_t i = 0; i < pattern.length(); i++) {
        if (pattern[i] == 'X')
            new_filename.append(random_characters[(get_random<u32>() % (sizeof(random_characters) - 1))]);
        else
            new_filename.append(pattern[i]);
    }

    return new_filename.to_string();
}

static ErrorOr<String> make_temp(String const& pattern, bool directory, bool dry_run)
{
    for (int i = 0; i < 100; ++i) {
        auto path = generate_random_filename(pattern);
        if (dry_run) {
            auto stat_or_error = Core::System::lstat(path.view());
            if (stat_or_error.is_error() && stat_or_error.error().code() == ENOENT)
                return path;
        } else if (directory) {
            TRY(Core::System::mkdir(path.view(), 0700));
            return path;
        } else {
            auto fd_or_error = Core::System::open(path.view(), O_RDWR | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR);
            if (!fd_or_error.is_error()) {
                TRY(Core::System::close(fd_or_error.value()));
                return path;
            }
        }
    }
    return String {};
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    StringView file_template;
    bool create_directory = false;
    bool dry_run = false;
    bool quiet = false;
    StringView target_directory;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Create a temporary file or directory, safely, and print its name.");
    args_parser.add_positional_argument(file_template, "The template must contain at least 3 consecutive 'X's", "template", Core::ArgsParser::Required::No);
    args_parser.add_option(create_directory, "Create a temporary directory instead of a file", "directory", 'd');
    args_parser.add_option(dry_run, "Do not create anything, just print a unique name", "dry-run", 'u');
    args_parser.add_option(quiet, "Do not print diagnostics about file/directory creation failure", "quiet", 'q');
    args_parser.add_option(target_directory, "Create TEMPLATE relative to DIR", "tmpdir", 'p', "DIR");
    args_parser.parse(arguments);

    if (target_directory.is_empty()) {
        if (!file_template.is_empty()) { // If a custom template is specified we assume the target directory is the current directory
            target_directory = getcwd(nullptr, 0);
        } else {
            LexicalPath template_path(file_template);
            char const* env_directory = getenv("TMPDIR");
            target_directory = env_directory && *env_directory ? env_directory : "/tmp";
        }
    }

    if (file_template.is_empty()) {
        file_template = default_template;
    }

    if (!file_template.find("XXX").has_value()) {
        if (!quiet)
            warnln("Too few X's in template {}", file_template);
        return 1;
    }

    auto target_path = LexicalPath::join(target_directory, file_template).string();

    auto final_path = TRY(make_temp(target_path, create_directory, dry_run));
    if (final_path.is_null()) {
        if (!quiet) {
            if (create_directory)
                warnln("Failed to create directory via template {}", target_path.characters());
            else
                warnln("Failed to create file via template {}", target_path.characters());
        }
        return 1;
    }

    outln("{}", final_path);

    return 0;
}
