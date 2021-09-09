/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/Random.h>
#include <LibCore/ArgsParser.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

constexpr const char* default_template = "tmp.XXXXXXXXXX";

static char* generate_random_filename(const char* pattern)
{
    char* new_filename = strdup(pattern);
    int pattern_length = strlen(pattern);

    constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";
    for (auto i = pattern_length - 1; i >= 0; --i) {
        if (pattern[i] != 'X')
            break;
        new_filename[i] = random_characters[(get_random<u32>() % (sizeof(random_characters) - 1))];
    }

    return new_filename;
}

static char* make_temp(const char* pattern, bool directory, bool dry_run)
{
    for (int i = 0; i < 100; ++i) {
        char* path = generate_random_filename(pattern);
        if (dry_run) {
            struct stat stat;
            auto rc = lstat(path, &stat);
            if (rc < 0 && errno == ENOENT)
                return path;
        } else if (directory) {
            if (!mkdir(path, 0700))
                return path;
        } else {
            auto fd = open(path, O_RDWR | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR);
            if (fd >= 0) {
                close(fd);
                return path;
            }
        }
        free(path);
    }
    return nullptr;
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath wpath cpath", nullptr)) {
        perror("pledge");
        return 1;
    }

    const char* file_template = nullptr;
    bool create_directory = false;
    bool dry_run = false;
    bool quiet = false;
    const char* target_directory = nullptr;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Create a temporary file or directory, safely, and print its name.");
    args_parser.add_positional_argument(file_template, "The template must contain at least 3 consecutive 'X's", "template", Core::ArgsParser::Required::No);
    args_parser.add_option(create_directory, "Create a temporary directory instead of a file", "directory", 'd');
    args_parser.add_option(dry_run, "Do not create anything, just print a unique name", "dry-run", 'u');
    args_parser.add_option(quiet, "Do not print diagnostics about file/directory creation failure", "quiet", 'q');
    args_parser.add_option(target_directory, "Create TEMPLATE relative to DIR", "tmpdir", 'p', "DIR");
    args_parser.parse(argc, argv);

    if (!target_directory) {
        if (file_template) { // If a custom template is specified we assume the target directory is the current directory
            target_directory = getcwd(nullptr, 0);
        } else {
            LexicalPath template_path(file_template);
            const char* env_directory = getenv("TMPDIR");
            target_directory = env_directory && *env_directory ? env_directory : "/tmp";
        }
    }

    if (!file_template) {
        file_template = default_template;
    }

    if (!String(file_template).find("XXX").has_value()) {
        if (!quiet)
            warnln("Too few X's in template {}", file_template);
        return 1;
    }

    auto target_path = LexicalPath::join(target_directory, file_template).string();

    char* final_path = make_temp(target_path.characters(), create_directory, dry_run);
    if (!final_path) {
        if (!quiet) {
            if (create_directory)
                warnln("Failed to create directory via template {}", target_path.characters());
            else
                warnln("Failed to create file via template {}", target_path.characters());
        }
        return 1;
    }

    outln("{}", final_path);
    free(final_path);
    return 0;
}
