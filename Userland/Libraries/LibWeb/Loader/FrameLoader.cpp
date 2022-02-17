/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonArray.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <LibGemini/Document.h>
#include <LibGfx/ImageDecoder.h>
#include <LibMarkdown/Document.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/ImageDecoding.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>

namespace Web {

static RefPtr<Gfx::Bitmap> s_default_favicon_bitmap;

FrameLoader::FrameLoader(HTML::BrowsingContext& browsing_context)
    : m_browsing_context(browsing_context)
{
    if (!s_default_favicon_bitmap) {
        s_default_favicon_bitmap = Gfx::Bitmap::try_load_from_file("/res/icons/16x16/app-browser.png").release_value_but_fixme_should_propagate_errors();
        VERIFY(s_default_favicon_bitmap);
    }
}

FrameLoader::~FrameLoader()
{
}

static bool build_markdown_document(DOM::Document& document, const ByteBuffer& data)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return false;

    HTML::HTMLParser parser(document, markdown_document->render_to_html(), "utf-8");
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

static bool build_image_document(DOM::Document& document, ByteBuffer const& data)
{
    NonnullRefPtr decoder = image_decoder_client();
    auto image = decoder->decode_image(data);
    if (!image.has_value() || image->frames.is_empty())
        return false;
    auto const& frame = image->frames[0];
    auto const& bitmap = frame.bitmap;
    if (!bitmap)
        return false;

    auto html_element = document.create_element("html");
    document.append_child(html_element);

    auto head_element = document.create_element("head");
    html_element->append_child(head_element);
    auto title_element = document.create_element("title");
    head_element->append_child(title_element);

    auto basename = LexicalPath::basename(document.url().path());
    auto title_text = adopt_ref(*new DOM::Text(document, String::formatted("{} [{}x{}]", basename, bitmap->width(), bitmap->height())));
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

    dbgln_if(GEMINI_DEBUG, "Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln_if(GEMINI_DEBUG, "Converted to HTML:\n\"\"\"{}\"\"\"", html_data);

    HTML::HTMLParser parser(document, html_data, "utf-8");
    parser.run(document.url());
    return true;
}

bool FrameLoader::parse_document(DOM::Document& document, const ByteBuffer& data)
{
    auto& mime_type = document.content_type();
    if (mime_type == "text/html" || mime_type == "image/svg+xml") {
        auto parser = HTML::HTMLParser::create_with_uncertain_encoding(document, data);
        parser->run(document.url());
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

bool FrameLoader::load(LoadRequest& request, Type type)
{
    if (!request.is_valid()) {
        load_error_page(request.url(), "Invalid request");
        return false;
    }

    if (!m_browsing_context.is_frame_nesting_allowed(request.url())) {
        dbgln("No further recursion is allowed for the frame, abort load!");
        return false;
    }

    auto& url = request.url();

    if (type == Type::Navigation || type == Type::Reload) {
        if (auto* page = browsing_context().page())
            page->client().page_did_start_loading(url);
    }

    // https://fetch.spec.whatwg.org/#concept-fetch
    // Step 12: If request’s header list does not contain `Accept`, then:
    //          1. Let value be `*/*`. (NOTE: Not necessary as we're about to override it)
    //          2. A user agent should set value to the first matching statement, if any, switching on request’s destination:
    //              -> "document"
    //              -> "frame"
    //              -> "iframe"
    //                   `text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8`
    // FIXME: This should be case-insensitive.
    if (!request.headers().contains("Accept"))
        request.set_header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));

    if (type == Type::IFrame)
        return true;

    if (url.protocol() == "http" || url.protocol() == "https") {
        AK::URL favicon_url;
        favicon_url.set_protocol(url.protocol());
        favicon_url.set_host(url.host());
        favicon_url.set_port(url.port_or_default());
        favicon_url.set_paths({ "favicon.ico" });

        ResourceLoader::the().load(
            favicon_url,
            [this, favicon_url](auto data, auto&, auto) {
                dbgln_if(SPAM_DEBUG, "Favicon downloaded, {} bytes from {}", data.size(), favicon_url);
                if (data.is_empty())
                    return;
                RefPtr<Gfx::Bitmap> favicon_bitmap;
                auto decoded_image = image_decoder_client().decode_image(data);
                if (!decoded_image.has_value() || decoded_image->frames.is_empty()) {
                    dbgln("Could not decode favicon {}", favicon_url);
                } else {
                    favicon_bitmap = decoded_image->frames[0].bitmap;
                    dbgln_if(IMAGE_DECODER_DEBUG, "Decoded favicon, {}", favicon_bitmap->size());
                }
                load_favicon(favicon_bitmap);
            },
            [this](auto&, auto) {
                load_favicon();
            });
    } else {
        load_favicon();
    }

    return true;
}

bool FrameLoader::load(const AK::URL& url, Type type)
{
    dbgln_if(SPAM_DEBUG, "FrameLoader::load: {}", url);

    if (!url.is_valid()) {
        load_error_page(url, "Invalid URL");
        return false;
    }

    auto request = LoadRequest::create_for_url_on_page(url, browsing_context().page());
    return load(request, type);
}

void FrameLoader::load_html(StringView html, const AK::URL& url)
{
    auto document = DOM::Document::create(url);
    HTML::HTMLParser parser(document, html, "utf-8");
    parser.run(url);
    browsing_context().set_active_document(&parser.document());
}

// FIXME: Use an actual templating engine (our own one when it's built, preferably
// with a way to check these usages at compile time)

void FrameLoader::load_error_page(const AK::URL& failed_url, const String& error)
{
    auto error_page_url = "file:///res/html/error.html";
    ResourceLoader::the().load(
        error_page_url,
        [this, failed_url, error](auto data, auto&, auto) {
            VERIFY(!data.is_null());
            StringBuilder builder;
            SourceGenerator generator { builder };
            generator.set("failed_url", escape_html_entities(failed_url.to_string()));
            generator.set("error", escape_html_entities(error));
            generator.append(data);
            auto document = HTML::parse_html_document(generator.as_string_view(), failed_url, "utf-8");
            VERIFY(document);
            browsing_context().set_active_document(document);
        },
        [](auto& error, auto) {
            dbgln("Failed to load error page: {}", error);
            VERIFY_NOT_REACHED();
        });
}

void FrameLoader::load_favicon(RefPtr<Gfx::Bitmap> bitmap)
{
    if (auto* page = browsing_context().page()) {
        if (bitmap)
            page->client().page_did_change_favicon(*bitmap);
        else
            page->client().page_did_change_favicon(*s_default_favicon_bitmap);
    }
}

void FrameLoader::store_response_cookies(AK::URL const& url, String const& cookies)
{
    auto* page = browsing_context().page();
    if (!page)
        return;

    auto set_cookie_json_value = MUST(JsonValue::from_string(cookies));
    VERIFY(set_cookie_json_value.type() == JsonValue::Type::Array);

    for (const auto& set_cookie_entry : set_cookie_json_value.as_array().values()) {
        VERIFY(set_cookie_entry.type() == JsonValue::Type::String);

        auto cookie = Cookie::parse_cookie(set_cookie_entry.as_string());
        if (!cookie.has_value())
            continue;

        page->client().page_did_set_cookie(url, cookie.value(), Cookie::Source::Http); // FIXME: Determine cookie source correctly
    }
}

void FrameLoader::resource_did_load()
{
    auto url = resource()->url();

    if (auto set_cookie = resource()->response_headers().get("Set-Cookie"); set_cookie.has_value())
        store_response_cookies(url, *set_cookie);

    // For 3xx (Redirection) responses, the Location value refers to the preferred target resource for automatically redirecting the request.
    auto status_code = resource()->status_code();
    if (status_code.has_value() && *status_code >= 300 && *status_code <= 399) {
        auto location = resource()->response_headers().get("Location");
        if (location.has_value()) {
            if (m_redirects_count > maximum_redirects_allowed) {
                m_redirects_count = 0;
                load_error_page(url, "Too many redirects");
                return;
            }
            m_redirects_count++;
            load(url.complete_url(location.value()), FrameLoader::Type::Navigation);
            return;
        }
    }
    m_redirects_count = 0;

    if (!resource()->has_encoded_data()) {
        load_error_page(url, "No data");
        return;
    }

    if (resource()->has_encoding()) {
        dbgln_if(RESOURCE_DEBUG, "This content has MIME type '{}', encoding '{}'", resource()->mime_type(), resource()->encoding().value());
    } else {
        dbgln_if(RESOURCE_DEBUG, "This content has MIME type '{}', encoding unknown", resource()->mime_type());
    }

    auto document = DOM::Document::create();
    document->set_url(url);
    document->set_encoding(resource()->encoding());
    document->set_content_type(resource()->mime_type());

    browsing_context().set_active_document(document);

    if (!parse_document(*document, resource()->encoded_data())) {
        load_error_page(url, "Failed to parse content.");
        return;
    }

    if (!url.fragment().is_empty())
        browsing_context().scroll_to_anchor(url.fragment());
    else
        browsing_context().set_viewport_scroll_offset({ 0, 0 });

    if (auto* page = browsing_context().page())
        page->client().page_did_finish_loading(url);
}

void FrameLoader::resource_did_fail()
{
    load_error_page(resource()->url(), resource()->error());
}

}
