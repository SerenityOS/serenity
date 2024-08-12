/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/JsonObject.h>
#include <AK/LexicalPath.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibCore/MappedFile.h>
#include <LibCore/ResourceImplementationFile.h>
#include <LibCore/System.h>
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

static PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_page(PDF::Document& document, PDF::Page const& page)
{
    auto page_size = Gfx::IntSize { 800, round_to<int>(800 * page.media_box.height() / page.media_box.width()) };
    if (int rotation_count = (page.rotate / 90) % 4; rotation_count % 2 == 1)
        page_size = Gfx::IntSize { round_to<int>(800 * page.media_box.width() / page.media_box.height()), 800 };

    auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, page_size));

    auto errors = PDF::Renderer::render(document, page, bitmap, Color::White, PDF::RenderingPreferences {});
    if (errors.is_error()) {
        for (auto const& error : errors.error().errors())
            warnln("warning: {}", error.message());
    }
    return TRY(PDF::Renderer::apply_page_rotation(bitmap, page));
}

static PDF::PDFErrorOr<NonnullRefPtr<Gfx::Bitmap>> render_page_to_memory(PDF::Document& document, PDF::Page const& page, int repeats)
{
    auto bitmap = TRY(render_page(document, page));
    for (int i = 0; i < repeats - 1; ++i)
        (void)TRY(render_page(document, page));
    return bitmap;
}

static PDF::PDFErrorOr<void> save_rendered_page(NonnullRefPtr<Gfx::Bitmap> bitmap, StringView out_path)
{
    if (!out_path.ends_with(".png"sv, CaseSensitivity::CaseInsensitive))
        return Error::from_string_view("can only save to .png files"sv);

    auto output_stream = TRY(Core::File::open(out_path, Core::File::OpenMode::Write));
    auto buffered_stream = TRY(Core::OutputBufferedFile::create(move(output_stream)));
    TRY(Gfx::PNGWriter::encode(*buffered_stream, *bitmap));

    return {};
}

// Takes a sorted non-empty vector of ints like `1 1 3 4 5 5 5` and returns a RLE vector with alternating elements and counts like `1 2 3 1 4 1 5 3`.
static Vector<int> rle_vector(Vector<int> const& pages)
{
    Vector<int> rle;
    int last_page = 0;
    int page_count = 0;
    for (int page : pages) {
        if (page == last_page) {
            ++page_count;
            continue;
        }

        if (last_page != 0) {
            rle.append(last_page);
            rle.append(page_count);
        }
        last_page = page;
        page_count = 1;
    }
    rle.append(last_page);
    rle.append(page_count);
    return rle;
}

// Takes a sorted non-empty vector of ints like `1 1 3 4 5 5 5` and returns a RLE-y summary string like " 1 (2x) 3 4 5 (3x)" (with a leading space).
static ErrorOr<String> summary_string(Vector<int> const& pages)
{
    StringBuilder builder;
    auto rle = rle_vector(pages);
    for (size_t i = 0; i < rle.size(); i += 2) {
        builder.appendff(" {}", rle[i]);
        if (rle[i + 1] > 1)
            builder.appendff(" ({}x)", rle[i + 1]);
    }
    return builder.to_string();
}

static PDF::PDFErrorOr<void> print_debugging_stats(PDF::Document& document, bool json)
{
    HashMap<ByteString, Vector<int>> diags_to_pages;
    for (u32 page_number = 1; page_number <= document.get_page_count(); ++page_number) {
        if (!json) {
            out("page number {} / {}", page_number, document.get_page_count());
            fflush(stdout);
        }
        auto page = TRY(document.get_page(page_number - 1));
        auto page_size = Gfx::IntSize { 200, round_to<int>(200 * page.media_box.height() / page.media_box.width()) };
        auto bitmap = TRY(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, page_size));
        auto errors = PDF::Renderer::render(document, page, bitmap, Color::White, PDF::RenderingPreferences {});
        if (errors.is_error()) {
            for (auto const& error : errors.error().errors())
                diags_to_pages.ensure(error.message()).append(page_number);
        }
        if (!json)
            out("\r");
    }
    if (!json)
        outln();

    JsonObject json_output;
    json_output.set("num_pages", document.get_page_count());

    if (diags_to_pages.is_empty() && !json) {
        outln("no issues found");
        return {};
    }

    JsonObject issues;
    auto keys = diags_to_pages.keys();
    quick_sort(keys, [&](auto& k1, auto& k2) { return diags_to_pages.get(k1)->size() < diags_to_pages.get(k2)->size(); });
    for (auto const& key : keys.in_reverse()) {
        auto const& value = diags_to_pages.get(key).value();
        if (json) {
            JsonArray page_counts;
            auto rle = rle_vector(value);
            for (size_t i = 0; i < rle.size(); i += 2)
                page_counts.must_append(JsonArray { Vector { rle[i], rle[i + 1] } });
            issues.set(key, page_counts);
        } else {
            outln("{} times: {}", value.size(), key);
            outln("    on pages:{}", TRY(summary_string(value)));
        }
    }
    if (json) {
        json_output.set("issues", issues);
        outln("{}", json_output.to_byte_string());
    }
    return {};
}

static PDF::PDFErrorOr<int> pdf_main(Main::Arguments arguments)
{
    Core::ArgsParser args_parser;

    StringView password;
    args_parser.add_option(password, "Password for decrypting PDF, if needed", "password", {}, "PASS");

    StringView in_path;
    args_parser.add_positional_argument(in_path, "Path to input image file", "FILE");

    bool debugging_stats = false;
    args_parser.add_option(debugging_stats, "Print stats for debugging", "debugging-stats", {});

    bool dump_contents = false;
    args_parser.add_option(dump_contents, "Dump page contents", "dump-contents", {});

    bool dump_outline = false;
    args_parser.add_option(dump_outline, "Dump document outline", "dump-outline", {});

    // FIXME: Currently only honored for --debugging-stats, should be honored for no-arg output too.
    bool json = false;
    args_parser.add_option(json, "Print output as json", "json", {});

    u32 page_number = 1;
    args_parser.add_option(page_number, "Page number (1-based)", "page", {}, "PAGE");

    StringView render_path;
    args_parser.add_option(render_path, "Path to render PDF page to", "render", {}, "FILE.png");

    bool render_bench = false;
    args_parser.add_option(render_bench, "Render to memory, then throw away result (for profiling)", "render-bench", {});

    u32 render_repeats = 1;
    args_parser.add_option(render_repeats, "Number of times to render page (for profiling)", "render-repeats", {}, "N");

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

#if !defined(AK_OS_SERENITY)
    if (debugging_stats || !render_path.is_empty() || render_bench) {
        // Get from Build/lagom/bin/pdf to Build/lagom/Root/res.
        auto source_root = LexicalPath(MUST(Core::System::current_executable_path())).parent().parent().string();
        Core::ResourceImplementation::install(make<Core::ResourceImplementationFile>(TRY(String::formatted("{}/Root/res", source_root))));
    }
#endif

    if (debugging_stats) {
        TRY(print_debugging_stats(*document, json));
        return 0;
    }

    if (page_number < 1 || page_number > document->get_page_count()) {
        warnln("--page {} out of bounds, must be between 1 and {}", page_number, document->get_page_count());
        return 1;
    }
    int page_index = page_number - 1;

    if (dump_outline) {
        if (auto outline = document->outline(); outline)
            outln("{}", *outline);
        else
            outln("(no outline)");
        return 0;
    }

    if (dump_contents) {
        TRY(document->dump_page(page_index));
        return 0;
    }

    if (!render_path.is_empty() || render_bench) {
        auto page = TRY(document->get_page(page_index));
        auto bitmap = TRY(render_page_to_memory(document, page, render_repeats));
        if (!render_path.is_empty())
            TRY(save_rendered_page(move(bitmap), render_path));
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
