/*
 * Copyright (c) 2021, Idan Horowitz <idan.horowitz@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>

constexpr const char* default_template = "tmp.XXXXXXXXXX";

static char* generate_random_filename(const char* pattern)
{
    char* new_filename = strdup(pattern);
    int pattern_length = strlen(pattern);

    static constexpr char random_characters[] = "abcdefghijklmnopqrstuvwxyz0123456789";
    for (auto i = pattern_length - 1; i >= 0; --i) {
        if (pattern[i] != 'X')
            break;
        new_filename[i] = random_characters[(arc4random() % (sizeof(random_characters) - 1))];
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

    LexicalPath target_path(String::formatted("{}/{}", target_directory, file_template));
    if (!target_path.is_valid()) {
        if (!quiet)
            warnln("Invalid template path {}", target_path.string().characters());
        return 1;
    }

    char* final_path = make_temp(target_path.string().characters(), create_directory, dry_run);
    if (!final_path) {
        if (!quiet) {
            if (create_directory)
                warnln("Failed to create directory via template {}", target_path.string().characters());
            else
                warnln("Failed to create file via template {}", target_path.string().characters());
        }
        return 1;
    }

    outln(final_path);
    free(final_path);
    return 0;
}
