/*
 * Copyright (c) 2020, Stijn De Ridder <stijn.deridder@hotmail.com>
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
#include <AK/QuickSort.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <cstdio>
#include <sys/stat.h>

static bool flag_show_hidden_files = false;
static bool flag_show_only_directories = false;
static int max_depth = INT_MAX;

static int g_directories_seen = 0;
static int g_files_seen = 0;

static void print_directory_tree(const String& root_path, int depth, const String& indent_string)
{
    if (depth > 0) {
        String root_indent_string;
        if (depth > 1) {
            root_indent_string = indent_string.substring(0, (depth - 1) * 4);
        } else {
            root_indent_string = "";
        }
        printf("%s|-- ", root_indent_string.characters());
    }

    String root_dir_name = AK::LexicalPath(root_path).basename();
    printf("\033[34;1m%s\033[0m\n", root_dir_name.characters());

    if (depth >= max_depth) {
        return;
    }

    Core::DirIterator di(root_path, flag_show_hidden_files ? Core::DirIterator::SkipParentAndBaseDir : Core::DirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "%s: %s\n", root_path.characters(), di.error_string());
        return;
    }

    Vector<String> names;
    while (di.has_next()) {
        String name = di.next_path();
        if (di.has_error()) {
            fprintf(stderr, "%s: %s\n", root_path.characters(), di.error_string());
            continue;
        }
        names.append(name);
    }

    quick_sort(names);

    for (size_t i = 0; i < names.size(); i++) {
        String name = names[i];

        StringBuilder builder;
        builder.append(root_path);
        if (!root_path.ends_with('/')) {
            builder.append('/');
        }
        builder.append(name);
        String full_path = builder.to_string();

        struct stat st;
        int rc = lstat(full_path.characters(), &st);
        if (rc == -1) {
            fprintf(stderr, "lstat(%s) failed: %s\n", full_path.characters(), strerror(errno));
            continue;
        }

        if (S_ISDIR(st.st_mode)) {
            g_directories_seen++;

            bool at_last_entry = i == names.size() - 1;
            String new_indent_string;
            if (at_last_entry) {
                new_indent_string = String::format("%s    ", indent_string.characters());
            } else {
                new_indent_string = String::format("%s|   ", indent_string.characters());
            }

            print_directory_tree(full_path.characters(), depth + 1, new_indent_string);
        } else if (!flag_show_only_directories) {
            g_files_seen++;

            printf("%s|-- %s\n", indent_string.characters(), name.characters());
        }
    }
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath tty", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<const char*> directories;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_show_hidden_files, "Show hidden files", "all", 'a');
    args_parser.add_option(flag_show_only_directories, "Show only directories", "only-directories", 'd');
    args_parser.add_option(max_depth, "Maximum depth of the tree", "maximum-depth", 'L', "level");
    args_parser.add_positional_argument(directories, "Directories to print", "directories", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

    if (max_depth < 1) {
        fprintf(stderr, "%s: Invalid level, must be greater than 0.\n", argv[0]);
        return 1;
    }

    if (directories.is_empty()) {
        print_directory_tree(".", 0, "");
        printf("\n");
    } else {
        for (const char* directory : directories) {
            print_directory_tree(directory, 0, "");
            printf("\n");
        }
    }

    printf("%d directories, %d files\n", g_directories_seen, g_files_seen);

    return 0;
}
