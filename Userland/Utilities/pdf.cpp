/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibPDF/Document.h>

static PDF::PDFErrorOr<int> pdf_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView in_path;
    args_parser.add_positional_argument(in_path, "Path to input image file", "FILE");

    args_parser.parse(arguments);

    auto file = TRY(Core::MappedFile::map(in_path));

    auto document = TRY(PDF::Document::create(file->bytes()));

    if (auto handler = document->security_handler(); handler && !handler->has_user_password()) {
        // FIXME: Add a flag for passing in a password and suggest it in this diag.
        warnln("PDF requires password");
        return 1;
    }

    TRY(document->initialize());

    outln("{} pages", document->get_page_count());

    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    auto maybe_error = pdf_main(move(arguments));
    if (maybe_error.is_error()) {
        auto error = maybe_error.release_error();
        warnln("{}", error.message());
        return 1;
    }
    return 0;
}
