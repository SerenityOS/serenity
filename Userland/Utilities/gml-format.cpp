/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGUI/GML/Formatter.h>
#include <LibMain/Main.h>

static ErrorOr<bool> format_file(StringView path, bool inplace)
{
    auto read_from_stdin = path == "-";
    auto open_mode = (inplace && !read_from_stdin) ? Core::File::OpenMode::ReadWrite : Core::File::OpenMode::Read;
    auto file = TRY(Core::File::open_file_or_standard_stream(path, open_mode));

    auto contents = TRY(file->read_until_eof());
    auto formatted_gml_or_error = GUI::GML::format_gml(contents);
    if (formatted_gml_or_error.is_error()) {
        warnln("Failed to parse GML: {}", formatted_gml_or_error.error());
        return false;
    }
    auto formatted_gml = formatted_gml_or_error.release_value();
    if (inplace && !read_from_stdin) {
        if (formatted_gml == contents)
            return true;
        TRY(file->seek(0, SeekMode::SetPosition));
        TRY(file->truncate(0));
        TRY(file->write_until_depleted(formatted_gml.bytes()));
    } else {
        out("{}", formatted_gml);
    }
    return formatted_gml == contents;
}

ErrorOr<int> serenity_main(Main::Arguments args)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    bool inplace = false;
    Vector<ByteString> files;

    Core::ArgsParser args_parser;
    args_parser.set_general_help("Format GML files.");
    args_parser.add_option(inplace, "Write formatted contents back to file rather than standard output", "inplace", 'i');
    args_parser.add_positional_argument(files, "File(s) to process", "path", Core::ArgsParser::Required::No);
    args_parser.parse(args);

    if (!inplace)
        TRY(Core::System::pledge("stdio rpath"));

    if (files.is_empty())
        files.append("-");

    auto formatting_changed = false;
    for (auto& file : files) {
        if (!TRY(format_file(file, inplace)))
            formatting_changed = true;
    }

    if (formatting_changed) {
        dbgln("Some GML formatting issues were encountered.");
        return 1;
    }

    return 0;
}
