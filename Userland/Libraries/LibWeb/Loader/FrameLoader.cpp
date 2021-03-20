/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Debug.h>
#include <AK/LexicalPath.h>
#include <LibGemini/Document.h>
#include <LibGfx/ImageDecoder.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/Parser/HTMLDocumentParser.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/Page/Page.h>

namespace Web {

FrameLoader::FrameLoader(Frame& frame)
    : m_frame(frame)
{
}

FrameLoader::~FrameLoader()
{
}

static bool build_markdown_document(DOM::Document& document, const ByteBuffer& data)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return false;

    HTML::HTMLDocumentParser parser(document, markdown_document->render_to_html(), "utf-8");
    parser.run(document.url());
    return true;
}

static bool build_text_document(DOM::Document& document, const ByteBuffer& data)
{
    auto html_element = document.create_element("html");
    document.append_child(html_element);

    auto head_element = document.create_element("head");
    html_element->append_child(head_element);
    auto title_element = document.create_element("title");
    head_element->append_child(title_element);

    auto title_text = document.create_text_node(document.url().basename());
    title_element->append_child(title_text);

    auto body_element = document.create_element("body");
    html_element->append_child(body_element);

    auto pre_element = document.create_element("pre");
    body_element->append_child(pre_element);

    pre_element->append_child(document.create_text_node(String::copy(data)));
    return true;
}

static bool build_image_document(DOM::Document& document, const ByteBuffer& data)
{
    auto image_decoder = Gfx::ImageDecoder::create(data.data(), data.size());
    auto bitmap = image_decoder->bitmap();
    if (!bitmap)
        return false;

    auto html_element = document.create_element("html");
    document.append_child(html_element);

    auto head_element = document.create_element("head");
    html_element->append_child(head_element);
    auto title_element = document.create_element("title");
    head_element->append_child(title_element);

    auto basename = LexicalPath(document.url().path()).basename();
    auto title_text = adopt(*new DOM::Text(document, String::formatted("{} [{}x{}]", basename, bitmap->width(), bitmap->height())));
    title_element->append_child(title_text);

    auto body_element = document.create_element("body");
    html_element->append_child(body_element);

    auto image_element = document.create_element("img");
    image_element->set_attribute(HTML::AttributeNames::src, document.url().to_string());
    body_element->append_child(image_element);

    return true;
}

static bool build_gemini_document(DOM::Document& document, const ByteBuffer& data)
{
    StringView gemini_data { data };
    auto gemini_document = Gemini::Document::parse(gemini_data, document.url());
    String html_data = gemini_document->render_to_html();

#if GEMINI_DEBUG
    dbgln("Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln("Converted to HTML:\n\"\"\"{}\"\"\"", html_data);
#endif

    HTML::HTMLDocumentParser parser(document, html_data, "utf-8");
    parser.run(document.url());
    return true;
}

bool FrameLoader::parse_document(DOM::Document& document, const ByteBuffer& data)
{
    auto& mime_type = document.content_type();
    if (mime_type == "text/html" || mime_type == "image/svg+xml") {
        HTML::HTMLDocumentParser parser(document, data, document.encoding());
        parser.run(document.url());
        return true;
    }
    if (mime_type.starts_with("image/"))
        return build_image_document(document, data);
    if (mime_type == "text/plain" || mime_type == "application/json")
        return build_text_document(document, data);
    if (mime_type == "text/markdown")
        return build_markdown_document(document, data);
    if (mime_type == "text/gemini")
        return build_gemini_document(document, data);

    return false;
}

bool FrameLoader::load(const LoadRequest& request, Type type)
{
    if (!request.is_valid()) {
        load_error_page(request.url(), "Invalid request");
        return false;
    }

    auto& url = request.url();

    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));

    if (type == Type::Navigation) {
        if (auto* page = frame().page())
            page->client().page_did_start_loading(url);
    }

    if (type == Type::IFrame)
        return true;

    if (url.protocol() == "http" || url.protocol() == "https") {
        URL favicon_url;
        favicon_url.set_protocol(url.protocol());
        favicon_url.set_host(url.host());
        favicon_url.set_port(url.port());
        favicon_url.set_path("/favicon.ico");

        ResourceLoader::the().load(
            favicon_url,
            [this, favicon_url](auto data, auto&) {
                dbgln("Favicon downloaded, {} bytes from {}", data.size(), favicon_url);
                auto decoder = Gfx::ImageDecoder::create(data.data(), data.size());
                auto bitmap = decoder->bitmap();
                if (!bitmap) {
                    dbgln("Could not decode favicon {}", favicon_url);
                    return;
                }
                dbgln("Decoded favicon, {}", bitmap->size());
                if (auto* page = frame().page())
                    page->client().page_did_change_favicon(*bitmap);
            });
    }

    return true;
}

bool FrameLoader::load(const URL& url, Type type)
{
    dbgln("FrameLoader::load: {}", url);

    if (!url.is_valid()) {
        load_error_page(url, "Invalid URL");
        return false;
    }

    LoadRequest request;
    request.set_url(url);

    return load(request, type);
}

void FrameLoader::load_html(const StringView& html, const URL& url)
{
    auto document = DOM::Document::create(url);
    HTML::HTMLDocumentParser parser(document, html, "utf-8");
    parser.run(url);
    frame().set_document(&parser.document());
}

// FIXME: Use an actual templating engine (our own one when it's built, preferably
// with a way to check these usages at compile time)

void FrameLoader::load_error_page(const URL& failed_url, const String& error)
{
    auto error_page_url = "file:///res/html/error.html";
    ResourceLoader::the().load(
        error_page_url,
        [this, failed_url, error](auto data, auto&) {
            VERIFY(!data.is_null());
#pragma GCC diagnostic ignored "-Wformat-nonliteral"
            auto html = String::format(
                String::copy(data).characters(),
                escape_html_entities(failed_url.to_string()).characters(),
                escape_html_entities(error).characters());
#pragma GCC diagnostic pop
            auto document = HTML::parse_html_document(html, failed_url, "utf-8");
            VERIFY(document);
            frame().set_document(document);
        },
        [](auto error) {
            dbgln("Failed to load error page: {}", error);
            VERIFY_NOT_REACHED();
        });
}

void FrameLoader::resource_did_load()
{
    auto url = resource()->url();

    if (!resource()->has_encoded_data()) {
        load_error_page(url, "No data");
        return;
    }

    // FIXME: Also check HTTP status code before redirecting
    auto location = resource()->response_headers().get("Location");
    if (location.has_value()) {
        load(url.complete_url(location.value()), FrameLoader::Type::Navigation);
        return;
    }

    dbgln("I believe this content has MIME type '{}', , encoding '{}'", resource()->mime_type(), resource()->encoding());

    auto document = DOM::Document::create();
    document->set_url(url);
    document->set_encoding(resource()->encoding());
    document->set_content_type(resource()->mime_type());

    frame().set_document(document);

    if (!parse_document(*document, resource()->encoded_data())) {
        load_error_page(url, "Failed to parse content.");
        return;
    }

    if (!url.fragment().is_empty())
        frame().scroll_to_anchor(url.fragment());

    if (auto* host_element = frame().host_element()) {
        // FIXME: Perhaps in the future we'll have a better common base class for <frame> and <iframe>
        VERIFY(is<HTML::HTMLIFrameElement>(*host_element));
        downcast<HTML::HTMLIFrameElement>(*host_element).content_frame_did_load({});
    }

    if (auto* page = frame().page())
        page->client().page_did_finish_loading(url);
}

void FrameLoader::resource_did_fail()
{
    load_error_page(resource()->url(), resource()->error());
}

}
