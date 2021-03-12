/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
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

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/GMLFormatter.h>
#include <unistd.h>

bool format_file(const StringView&, bool);

bool format_file(const StringView& path, bool inplace)
{
    auto read_from_stdin = path == "-";
    RefPtr<Core::File> file;
    if (read_from_stdin) {
        file = Core::File::standard_input();
    } else {
        auto open_mode = inplace ? Core::File::ReadWrite : Core::File::ReadOnly;
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
