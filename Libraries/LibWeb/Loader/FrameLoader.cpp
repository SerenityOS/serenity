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
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/Frame.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Parser/HTMLDocumentParser.h>
#include <LibWeb/Parser/HTMLParser.h>

namespace Web {

FrameLoader::FrameLoader(Frame& frame)
    : m_frame(frame)
{
}

FrameLoader::~FrameLoader()
{
}

static RefPtr<Document> create_markdown_document(const ByteBuffer& data, const URL& url)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return nullptr;

    return parse_html_document(markdown_document->render_to_html(), url);
}

static RefPtr<Document> create_text_document(const ByteBuffer& data, const URL& url)
{
    auto document = adopt(*new Document(url));

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

static RefPtr<Document> create_image_document(const ByteBuffer& data, const URL& url)
{
    auto document = adopt(*new Document(url));

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
    auto title_text = adopt(*new Text(document, String::format("%s [%dx%d]", basename.characters(), bitmap->width(), bitmap->height())));
    title_element->append_child(title_text);

    auto body_element = create_element(document, "body");
    html_element->append_child(body_element);

    auto image_element = create_element(document, "img");
    image_element->set_attribute(HTML::AttributeNames::src, url.to_string());
    body_element->append_child(image_element);

    return document;
}

static RefPtr<Document> create_gemini_document(const ByteBuffer& data, const URL& url)
{
    auto markdown_document = Gemini::Document::parse({ (const char*)data.data(), data.size() }, url);

    return parse_html_document(markdown_document->render_to_html(), url);
}

String encoding_from_content_type(const String& content_type)
{
    auto offset = content_type.index_of("charset=");
    if (offset.has_value())
        return content_type.substring(offset.value() + 8, content_type.length() - offset.value() - 8).to_lowercase();

    return "utf-8";
}

String mime_type_from_content_type(const String& content_type)
{
    auto offset = content_type.index_of(";");
    if (offset.has_value())
        return content_type.substring(0, offset.value()).to_lowercase();

    return content_type;
}

static String guess_mime_type_based_on_filename(const URL& url)
{
    if (url.path().ends_with(".png"))
        return "image/png";
    if (url.path().ends_with(".gif"))
        return "image/gif";
    if (url.path().ends_with(".md"))
        return "text/markdown";
    if (url.path().ends_with(".html") || url.path().ends_with(".htm"))
        return "text/html";
    return "text/plain";
}

RefPtr<Document> FrameLoader::create_document_from_mime_type(const ByteBuffer& data, const URL& url, const String& mime_type, const String& encoding)
{
    if (mime_type.starts_with("image/"))
        return create_image_document(data, url);
    if (mime_type == "text/plain")
        return create_text_document(data, url);
    if (mime_type == "text/markdown")
        return create_markdown_document(data, url);
    if (mime_type == "text/gemini")
        return create_gemini_document(data, url);
    if (mime_type == "text/html") {
        if (m_use_old_parser)
            return parse_html_document(data, url, encoding);
        HTMLDocumentParser parser(data, encoding);
        parser.run(url);
        return parser.document();
    }
    return nullptr;
}

bool FrameLoader::load(const URL& url)
{
    dbg() << "FrameLoader::load: " << url;

    if (!url.is_valid()) {
        load_error_page(url, "Invalid URL");
        return false;
    }

    ResourceLoader::the().load(
        url,
        [this, url](auto data, auto& response_headers) {
            // FIXME: Also check HTTP status code before redirecting
            auto location = response_headers.get("Location");
            if (location.has_value()) {
                load(location.value());
                return;
            }

            if (data.is_null()) {
                load_error_page(url, "No data");
                return;
            }

            String encoding = "utf-8";
            String mime_type;

            auto content_type = response_headers.get("Content-Type");
            if (content_type.has_value()) {
                dbg() << "Content-Type header: _" << content_type.value() << "_";
                encoding = encoding_from_content_type(content_type.value());
                mime_type = mime_type_from_content_type(content_type.value());
            } else {
                dbg() << "No Content-Type header to go on! Guessing based on filename...";
                mime_type = guess_mime_type_based_on_filename(url);
            }

            dbg() << "I believe this content has MIME type '" << mime_type << "', encoding '" << encoding << "'";
            auto document = create_document_from_mime_type(data, url, mime_type, encoding);
            ASSERT(document);
            frame().set_document(document);

            if (!url.fragment().is_empty())
                frame().scroll_to_anchor(url.fragment());

            if (frame().on_title_change)
                frame().on_title_change(document->title());
        },
        [this, url](auto error) {
            load_error_page(url, error);
        });

    if (frame().on_load_start)
        frame().on_load_start(url);

    if (url.protocol() != "file" && url.protocol() != "about") {
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
                if (frame().on_favicon_change)
                    frame().on_favicon_change(*bitmap);
            });
    }

    return true;
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
            auto document = parse_html_document(html, failed_url);
            ASSERT(document);
            frame().set_document(document);
            if (frame().on_title_change)
                frame().on_title_change(document->title());
        },
        [](auto error) {
            dbg() << "Failed to load error page: " << error;
            ASSERT_NOT_REACHED();
        });
}

}
