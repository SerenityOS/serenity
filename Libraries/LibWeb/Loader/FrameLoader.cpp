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
#include <LibWeb/Page/Frame.h>
#include <LibWeb/Page/Page.h>

//#define GEMINI_DEBUG 1

namespace Web {

FrameLoader::FrameLoader(Frame& frame)
    : m_frame(frame)
{
}

FrameLoader::~FrameLoader()
{
}

static RefPtr<DOM::Document> create_markdown_document(const ByteBuffer& data, const URL& url)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return nullptr;

    return HTML::parse_html_document(markdown_document->render_to_html(), url, "utf-8");
}

static RefPtr<DOM::Document> create_text_document(const ByteBuffer& data, const URL& url)
{
    auto document = adopt(*new DOM::Document(url));

    auto html_element = document->create_element("html");
    document->append_child(html_element);

    auto head_element = document->create_element("head");
    html_element->append_child(head_element);
    auto title_element = document->create_element("title");
    head_element->append_child(title_element);

    auto title_text = document->create_text_node(url.basename());
    title_element->append_child(title_text);

    auto body_element = document->create_element("body");
    html_element->append_child(body_element);

    auto pre_element = create_element(document, "pre");
    body_element->append_child(pre_element);

    pre_element->append_child(document->create_text_node(String::copy(data)));
    return document;
}

static RefPtr<DOM::Document> create_image_document(const ByteBuffer& data, const URL& url)
{
    auto document = adopt(*new DOM::Document(url));

    auto image_decoder = Gfx::ImageDecoder::create(data.data(), data.size());
    auto bitmap = image_decoder->bitmap();
    ASSERT(bitmap);

    auto html_element = create_element(document, "html");
    document->append_child(html_element);

    auto head_element = create_element(document, "head");
    html_element->append_child(head_element);
    auto title_element = create_element(document, "title");
    head_element->append_child(title_element);

    auto basename = LexicalPath(url.path()).basename();
    auto title_text = adopt(*new DOM::Text(document, String::format("%s [%dx%d]", basename.characters(), bitmap->width(), bitmap->height())));
    title_element->append_child(title_text);

    auto body_element = create_element(document, "body");
    html_element->append_child(body_element);

    auto image_element = create_element(document, "img");
    image_element->set_attribute(HTML::AttributeNames::src, url.to_string());
    body_element->append_child(image_element);

    return document;
}

static RefPtr<DOM::Document> create_gemini_document(const ByteBuffer& data, const URL& url)
{
    StringView gemini_data { data };
    auto gemini_document = Gemini::Document::parse(gemini_data, url);
    String html_data = gemini_document->render_to_html();

#ifdef GEMINI_DEBUG
    dbgln("Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln("Converted to HTML:\n\"\"\"{}\"\"\"", html_data);
#endif

    return HTML::parse_html_document(move(html_data), url, "utf-8");
}

RefPtr<DOM::Document> FrameLoader::create_document_from_mime_type(const ByteBuffer& data, const URL& url, const String& mime_type, const String& encoding)
{
    if (mime_type == "text/html" || mime_type == "image/svg+xml") {
        HTML::HTMLDocumentParser parser(data, encoding);
        parser.run(url);
        return parser.document();
    }
    if (mime_type.starts_with("image/"))
        return create_image_document(data, url);
    if (mime_type == "text/plain")
        return create_text_document(data, url);
    if (mime_type == "text/markdown")
        return create_markdown_document(data, url);
    if (mime_type == "text/gemini")
        return create_gemini_document(data, url);

    return nullptr;
}

bool FrameLoader::load(const LoadRequest& request, Type type)
{
    if (!request.is_valid()) {
        load_error_page(request.url(), "Invalid request");
        return false;
    }

    auto& url = request.url();

    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));

    if (type == Type::Navigation)
        frame().page().client().page_did_start_loading(url);

    if (type != Type::IFrame && url.protocol() != "file" && url.protocol() != "about") {
        URL favicon_url;
        favicon_url.set_protocol(url.protocol());
        favicon_url.set_host(url.host());
        favicon_url.set_port(url.port());
        favicon_url.set_path("/favicon.ico");

        ResourceLoader::the().load(
            favicon_url,
            [this, favicon_url](auto data, auto&) {
                dbg() << "Favicon downloaded, " << data.size() << " bytes from " << favicon_url;
                auto decoder = Gfx::ImageDecoder::create(data.data(), data.size());
                auto bitmap = decoder->bitmap();
                if (!bitmap) {
                    dbg() << "Could not decode favicon " << favicon_url;
                    return;
                }
                dbg() << "Decoded favicon, " << bitmap->size();
                frame().page().client().page_did_change_favicon(*bitmap);
            });
    }

    return true;
}

bool FrameLoader::load(const URL& url, Type type)
{
    dbg() << "FrameLoader::load: " << url;

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
    HTML::HTMLDocumentParser parser(html, "utf-8");
    parser.run(url);
    frame().set_document(&parser.document());
}

void FrameLoader::load_error_page(const URL& failed_url, const String& error)
{
    auto error_page_url = "file:///res/html/error.html";
    ResourceLoader::the().load(
        error_page_url,
        [this, failed_url, error](auto data, auto&) {
            ASSERT(!data.is_null());
            auto html = String::format(
                String::copy(data).characters(),
                escape_html_entities(failed_url.to_string()).characters(),
                escape_html_entities(error).characters());
            auto document = HTML::parse_html_document(html, failed_url, "utf-8");
            ASSERT(document);
            frame().set_document(document);
        },
        [](auto error) {
            dbg() << "Failed to load error page: " << error;
            ASSERT_NOT_REACHED();
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

    dbg() << "I believe this content has MIME type '" << resource()->mime_type() << "', encoding '" << resource()->encoding() << "'";
    auto document = create_document_from_mime_type(resource()->encoded_data(), url, resource()->mime_type(), resource()->encoding());

    if (!document) {
        load_error_page(url, "Failed to parse content.");
        return;
    }

    frame().set_document(document);

    if (!url.fragment().is_empty())
        frame().scroll_to_anchor(url.fragment());

    if (auto* host_element = frame().host_element()) {
        // FIXME: Perhaps in the future we'll have a better common base class for <frame> and <iframe>
        ASSERT(is<HTML::HTMLIFrameElement>(*host_element));
        downcast<HTML::HTMLIFrameElement>(*host_element).content_frame_did_load({});
    }
}

void FrameLoader::resource_did_fail()
{
    load_error_page(resource()->url(), resource()->error());
}

}
