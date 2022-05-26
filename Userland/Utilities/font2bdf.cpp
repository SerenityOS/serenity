/*
 * Copyright (c) 2022, Marco Rebhan <me@dblsaiko.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibGfx/Font/BDFWriter.h>
#include <LibGfx/Font/BitmapFont.h>
#include <LibMain/Main.h>
#include <sysexits.h>

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath wpath cpath"));

    StringView path;
    StringView output_path;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(path, "Path to font file", "path");
    args_parser.add_option(output_path, "Path to output file", "output", 'o', "path");
    args_parser.set_general_help("Convert a SerenityOS font into BDF format.");
    args_parser.parse(arguments);

    if (!Core::File::exists(path)) {
        warnln("File does not exist: '{}'", path);
        return EX_NOINPUT;
    }

    auto font = Gfx::BitmapFont::load_from_file(path);

    if (!font) {
        warnln("Failed to load font file: '{}'", path);
        return EX_DATAERR;
    }

    OwnPtr<Core::Stream::File> output;

    if (output_path.is_null()) {
        output = TRY(Core::Stream::File::adopt_fd(fileno(stdout), Core::Stream::OpenMode::Write));
    } else {
        auto result = Core::Stream::File::open(output_path, Core::Stream::OpenMode::Write | Core::Stream::OpenMode::Truncate);

        if (result.is_error()) {
            warnln("Failed to create output file: '{}': {}", path, result.error());
            return EX_CANTCREAT;
        }

        output = result.release_value();
    }

    auto result = Gfx::write_bdf(*output, *font);

    if (result.is_error()) {
        warnln("Failed to write output file: '{}': {}", path, result.error());
        return EX_IOERR;
    }

    return EX_OK;
}
