/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/GMLFormatter.h>
#include <unistd.h>

bool format_file(StringView, bool);

bool format_file(StringView path, bool inplace)
{
    auto read_from_stdin = path == "-";
    RefPtr<Core::File> file;
    if (read_from_stdin) {
        file = Core::File::standard_input();
    } else {
        auto open_mode = inplace ? Core::OpenMode::ReadWrite : Core::OpenMode::ReadOnly;
        auto file_or_error = Core::File::open(path, open_mode);
        if (file_or_error.is_error()) {
            warnln("Could not open {}: {}", path, file_or_error.error());
            return false;
        }
        file = file_or_error.value();
    }
    auto formatted_gml = GUI::format_gml(file->read_all());
    if (formatted_gml.is_null()) {
        warnln("Failed to parse GML!");
        return false;
    }
    if (inplace && !read_from_stdin) {
        if (!file->seek(0) || !file->truncate(0)) {
            warnln("Could not truncate {}: {}", path, file->error_string());
            return false;
        }
        if (!file->write(formatted_gml)) {
            warnln("Could not write to {}: {}", path, file->error_string());
            return false;
        }
    } else {
        out("{}", formatted_gml);
    }
    return true;
}

int main(int argc, char** argv)
{
#ifdef __serenity__
    if (pledge("stdio rpath wpath cpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }
#endif

    bool inplace = false;
    Vector<const char*> files;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Format GML files.");
    args_parser.add_option(inplace, "Write formatted contents back to file rather than standard output", "inplace", 'i');
    args_parser.add_positional_argument(files, "File(s) to process", "path", Core::ArgsParser::Required::No);
    args_parser.parse(argc, argv);

#ifdef __serenity__
    if (!inplace) {
        if (pledge("stdio rpath", nullptr) < 0) {
            perror("pledge");
            return 1;
        }
    }
#endif

    unsigned exit_code = 0;

    if (files.is_empty())
        files.append("-");
    for (auto& file : files) {
        if (!format_file(file, inplace))
            exit_code = 1;
    }

    return exit_code;
}
