/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>

static PDF::PDFErrorOr<void> print_document_info_dict(PDF::Document& document)
{
    if (auto info_dict = TRY(document.info_dict()); info_dict.has_value()) {
        if (auto title = TRY(info_dict->title()); title.has_value())
            outln("Title: {}", title);
        if (auto author = TRY(info_dict->author()); author.has_value())
            outln("Author: {}", author);
        if (auto subject = TRY(info_dict->subject()); subject.has_value())
            outln("Subject: {}", subject);
        if (auto keywords = TRY(info_dict->keywords()); keywords.has_value())
            outln("Keywords: {}", keywords);
        if (auto creator = TRY(info_dict->creator()); creator.has_value())
            outln("Creator: {}", creator);
        if (auto producer = TRY(info_dict->producer()); producer.has_value())
            outln("Producer: {}", producer);
        if (auto creation_date = TRY(info_dict->creation_date()); creation_date.has_value())
            outln("Creation date: {}", creation_date);
        if (auto modification_date = TRY(info_dict->modification_date()); modification_date.has_value())
            outln("Modification date: {}", modification_date);
    }
    return {};
}

static PDF::PDFErrorOr<void> print_document_info(PDF::Document& document)
{
    outln("PDF Version: {}.{}", document.version().major, document.version().minor);
    outln("Number of pages: {}", document.get_page_count());
    TRY(print_document_info_dict(document));
    return {};
}

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

    TRY(print_document_info(*document));

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
