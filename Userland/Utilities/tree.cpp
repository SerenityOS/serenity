/*
 * Copyright (c) 2020, Stijn De Ridder <stijn.deridder@hotmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

static bool flag_show_hidden_files = false;
static bool flag_show_only_directories = false;
static int max_depth = INT_MAX;

static int g_directories_seen = 0;
static int g_files_seen = 0;

static void print_directory_tree(ByteString const& root_path, int depth, ByteString const& indent_string)
{
    if (depth > 0) {
        ByteString root_indent_string;
        if (depth > 1) {
            root_indent_string = indent_string.substring(0, (depth - 1) * 4);
        } else {
            root_indent_string = "";
        }
        out("{}|-- ", root_indent_string);
    }

    ByteString root_dir_name = LexicalPath::basename(root_path);
    out("\033[34;1m{}\033[0m\n", root_dir_name);

    if (depth >= max_depth) {
        return;
    }

    Core::DirIterator di(root_path, flag_show_hidden_files ? Core::DirIterator::SkipParentAndBaseDir : Core::DirIterator::SkipDots);
    if (di.has_error()) {
        warnln("{}: {}", root_path, di.error());
        return;
    }

    Vector<ByteString> names;
    while (di.has_next()) {
        ByteString name = di.next_path();
        if (di.has_error()) {
            warnln("{}: {}", root_path, di.error());
            continue;
        }
        names.append(name);
    }

    quick_sort(names);

    for (size_t i = 0; i < names.size(); i++) {
        ByteString name = names[i];

        StringBuilder builder;
        builder.append(root_path);
        if (!root_path.ends_with('/')) {
            builder.append('/');
        }
        builder.append(name);
        auto full_path = builder.to_byte_string();

        struct stat st;
        int rc = lstat(full_path.characters(), &st);
        if (rc == -1) {
            warnln("lstat({}) failed: {}", full_path, strerror(errno));
            continue;
        }

        if (S_ISDIR(st.st_mode)) {
            g_directories_seen++;

            bool at_last_entry = i == names.size() - 1;
            ByteString new_indent_string;
            if (at_last_entry) {
                new_indent_string = ByteString::formatted("{}    ", indent_string);
            } else {
                new_indent_string = ByteString::formatted("{}|   ", indent_string);
            }

            print_directory_tree(full_path.characters(), depth + 1, new_indent_string);
        } else if (!flag_show_only_directories) {
            g_files_seen++;

            outln("{}|-- {}", indent_string, name);
        }
    }
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath tty"));

    Vector<ByteString> directories;

    Core::ArgsParser args_parser;
    args_parser.add_option(flag_show_hidden_files, "Show hidden files", "all", 'a');
    args_parser.add_option(flag_show_only_directories, "Show only directories", "only-directories", 'd');
    args_parser.add_option(max_depth, "Maximum depth of the tree", "maximum-depth", 'L', "level");
    args_parser.add_positional_argument(directories, "Directories to print", "directories", Core::ArgsParser::Required::No);
    args_parser.parse(arguments);

    if (max_depth < 1) {
        warnln("{}: Invalid level, must be greater than 0.", arguments.argv[0]);
        return 1;
    }

    if (directories.is_empty()) {
        print_directory_tree(".", 0, "");
        puts("");
    } else {
        for (auto const& directory : directories) {
            print_directory_tree(directory, 0, "");
            puts("");
        }
    }

    outln("{} directories, {} files", g_directories_seen, g_files_seen);

    return 0;
}
