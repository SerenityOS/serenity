/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibGfx/ImageFormats/PNGWriter.h>
#include <LibPDF/CommonNames.h>
#include <LibPDF/Document.h>
#include <LibPDF/Renderer.h>

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

static PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_page(PDF::Document& document, int page_index)
{
    auto page = TRY(document.get_page(page_index));

    auto page_size = Gfx::IntSize { 800, round_to<int>(800 * page.media_box.height() / page.media_box.width()) };

    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, page_size));

    auto errors = PDF::Renderer::render(document, page, bitmap, PDF::RenderingPreferences {});
    if (errors.is_error()) {
        for (auto const& error : errors.error().errors())
            warnln("warning: {}", error.message());
    }
    return bitmap;
}

static PDF::PDFErrorOr<void> save_rendered_page(PDF::Document& document, int page_index, StringView out_path)
{
    auto bitmap = TRY(render_page(document, page_index));

    if (!out_path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive))
        return Error::from_string_view("can only save to .png files"sv);

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_stream = TRY(Core::OutputBufferedFile::create(move(output_stream)));
    ByteBuffer bytes = TRY(Gfx::PNGWriter::encode(*bitmap));
    TRY(buffered_stream->write_until_depleted(bytes));

    return {};
}

static PDF::PDFErrorOr<int> pdf_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView password;
    args_parser.add_option(password, "Password for decrypting PDF, if needed", "password", {}, "PASS");

    StringView in_path;
    args_parser.add_positional_argument(in_path, "Path to input image file", "FILE");

    bool dump_contents = false;
    args_parser.add_option(dump_contents, "Dump page contents", "dump-contents", {});

    u32 page_number = 1;
    args_parser.add_option(page_number, "Page number (1-based)", "page", {}, "PAGE");

    StringView render_path;
    args_parser.add_option(render_path, "Path to render a PDF page to", "render", {}, "PNG FILE");

    args_parser.parse(arguments);

    auto file = TRY(Core::MappedFile::map(in_path));

    auto document = TRY(PDF::Document::create(file->bytes()));

    if (auto handler = document->security_handler(); handler && !handler->has_user_password()) {
        if (password.is_empty()) {
            warnln("PDF requires password, pass in using --password");
            return 1;
        }
        if (!document->security_handler()->try_provide_user_password(password)) {
            warnln("invalid password '{}'", password);
            return 1;
        }
    }

    TRY(document->initialize());

    if (page_number < 1 || page_number > document->get_page_count()) {
        warnln("--page {} out of bounds, must be between 1 and {}", page_number, document->get_page_count());
        return 1;
    }
    int page_index = page_number - 1;

    if (dump_contents) {
        auto page = TRY(document->get_page(page_index));
        auto contents = TRY(page.page_contents(*document));
        for (u8 c : contents.bytes()) {
            if (c < 128)
                out("{:c}", c);
            else
                out("\\{:03o}", c);
        }

        return 0;
    }

    if (!render_path.is_empty()) {
#if !defined(AK_OS_SERENITY)
        // Get from Build/lagom/bin/pdf to Base/res/fonts.
        auto source_root = LexicalPath(arguments.argv[0]).parent().parent().parent().parent().string();
        Gfx::FontDatabase::set_default_fonts_lookup_path(DeprecatedString::formatted("{}/Base/res/fonts", source_root));
#endif

        TRY(save_rendered_page(document, page_index, render_path));
        return 0;
    }

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
