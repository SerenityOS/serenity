/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
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
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/Cookie/ParsedCookie.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>
#include <LibWeb/XML/XMLDocumentBuilder.h>

namespace Web {

static DeprecatedString s_default_favicon_path = "/res/icons/16x16/app-browser.png";
static RefPtr<Gfx::Bitmap> s_default_favicon_bitmap;

void FrameLoader::set_default_favicon_path(DeprecatedString path)
{
    s_default_favicon_path = move(path);
}

FrameLoader::FrameLoader(HTML::BrowsingContext& browsing_context)
    : m_browsing_context(browsing_context)
{
    if (!s_default_favicon_bitmap) {
        s_default_favicon_bitmap = Gfx::Bitmap::try_load_from_file(s_default_favicon_path).release_value_but_fixme_should_propagate_errors();
        VERIFY(s_default_favicon_bitmap);
    }
}

FrameLoader::~FrameLoader() = default;

static ErrorOr<void> build_html_document(DOM::Document& document, ByteBuffer const& data)
{
    document.set_document_type(DOM::Document::Type::HTML);
    document.set_content_type("text/html");

    auto parser = HTML::HTMLParser::create_with_uncertain_encoding(document, data);
    parser->run(document.url());
    return {};
}

static ErrorOr<void> build_xml_document(DOM::Document& document, ByteBuffer const& data)
{
    document.set_document_type(DOM::Document::Type::XML);

    XML::Parser parser(data, { .resolve_external_resource = resolve_xml_resource });
    XMLDocumentBuilder builder { document };
    auto result = parser.parse_with_listener(builder);

    if (result.is_error())
        return Error::from_string_view(result.error().error);
    if (builder.has_error())
        return Error::from_string_literal("Error while building XML document");
    return {};
}

static ErrorOr<void> build_text_document(DOM::Document& document, ByteBuffer const& data)
{
    document.set_document_type(DOM::Document::Type::HTML);
    document.set_quirks_mode(DOM::QuirksMode::No);

    auto html_element = document.create_element("html").release_value();
    MUST(document.append_child(html_element));

    auto head_element = document.create_element("head").release_value();
    MUST(html_element->append_child(head_element));
    auto title_element = document.create_element("title").release_value();
    MUST(head_element->append_child(title_element));

    auto title_text = document.create_text_node(document.url().basename());
    MUST(title_element->append_child(title_text));

    auto body_element = document.create_element("body").release_value();
    MUST(html_element->append_child(body_element));

    auto pre_element = document.create_element("pre").release_value();
    MUST(body_element->append_child(pre_element));
    MUST(pre_element->append_child(document.create_text_node(DeprecatedString::copy(data))));

    return {};
}

static ErrorOr<AK::DeprecatedString> get_title_for_metdia_document(DOM::Document const& document, ByteBuffer const& data)
{
    auto const& type = document.content_type();
    auto const& basename = document.url().basename();

    if (type.starts_with("image/"sv)) {
        auto image = Platform::ImageCodecPlugin::the().decode_image(data);
        if (!image.has_value() || image->frames.is_empty())
            return Error::from_string_literal("Failed to decode image");

        auto const& frame = image->frames[0];
        auto const& bitmap = frame.bitmap;
        if (!bitmap)
            return Error::from_string_literal("Failed to decode image");

        return DeprecatedString::formatted("{} [{}x{}]", basename, bitmap->width(), bitmap->height());
    }

    return document.url().basename();
}

static AK::StringView const get_media_element_tag(AK::DeprecatedString const& type)
{
    if (type.starts_with("image/"sv))
        return "img"sv;
    if (type.starts_with("video/"sv))
        return "video"sv;
    if (type.starts_with("audio/"sv))
        return "audio"sv;

    // FIXME: Handle other media types.
    return "img"sv;
}

static ErrorOr<void> build_media_document(DOM::Document& document, ByteBuffer const& data)
{
    document.set_document_type(DOM::Document::Type::HTML);
    document.set_quirks_mode(DOM::QuirksMode::No);

    auto title = TRY(get_title_for_metdia_document(document, data));

    auto html_element = document.create_element("html").release_value();
    MUST(document.append_child(html_element));

    auto head_element = document.create_element("head").release_value();
    MUST(html_element->append_child(head_element));
    auto title_element = document.create_element("title").release_value();
    MUST(head_element->append_child(title_element));

    auto basename = LexicalPath::basename(document.url().path());
    auto title_text = document.create_text_node(title);
    MUST(title_element->append_child(*title_text));

    auto body_element = document.create_element("body").release_value();
    MUST(html_element->append_child(body_element));

    auto media_element = document.create_element(get_media_element_tag(document.content_type())).release_value();
    MUST(media_element->set_attribute(HTML::AttributeNames::src, document.url().to_deprecated_string()));
    MUST(body_element->append_child(media_element));

    return {};
}

static ErrorOr<void> build_markdown_document(DOM::Document& document, ByteBuffer const& data)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return Error::from_string_literal("Failed to parse Markdown document");

    auto extra_head_contents = R"~~~(
<style>
    .zoomable {
        cursor: zoom-in;
        max-width: 100%;
    }
    .zoomable.zoomed-in {
        cursor: zoom-out;
        max-width: none;
    }
</style>
<script>
    function imageClickEventListener(event) {
        let image = event.target;
        if (image.classList.contains("zoomable")) {
            image.classList.toggle("zoomed-in");
        }
    }
    function processImages() {
        let images = document.querySelectorAll("img");
        let windowWidth = window.innerWidth;
        images.forEach((image) => {
            if (image.naturalWidth > windowWidth) {
                image.classList.add("zoomable");
            } else {
                image.classList.remove("zoomable");
                image.classList.remove("zoomed-in");
            }

            image.addEventListener("click", imageClickEventListener);
        });
    }

    document.addEventListener("load", () => {
        processImages();
    });

    window.addEventListener("resize", () => {
        processImages();
    });
</script>
)~~~"sv;

    auto parser = HTML::HTMLParser::create(document, markdown_document->render_to_html(extra_head_contents), "utf-8");
    parser->run(document.url());

    return {};
}

static ErrorOr<void> build_gemini_document(DOM::Document& document, ByteBuffer const& data)
{
    StringView gemini_data { data };
    auto gemini_document = Gemini::Document::parse(gemini_data, document.url());
    DeprecatedString html_data = gemini_document->render_to_html();

    dbgln_if(GEMINI_DEBUG, "Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln_if(GEMINI_DEBUG, "Converted to HTML:\n\"\"\"{}\"\"\"", html_data);

    auto parser = HTML::HTMLParser::create(document, html_data, "utf-8");
    parser->run(document.url());

    return {};
}

// https://html.spec.whatwg.org/multipage/browsing-the-web.html#loading-a-document
ErrorOr<void> FrameLoader::load_document(DOM::Document& document, ByteBuffer const& data)
{
    auto mime_type = MimeSniff::MimeType::from_string(document.content_type());
    if (!mime_type.has_value())
        return Error::from_string_literal("Failed to parse MIME type");

    auto const& type = mime_type->essence();

    if (mime_type->is_html() || type == "image/svg+xml") {
        return build_html_document(document, data);
    }
    if (mime_type->is_xml())
        return build_xml_document(document, data);
    if (mime_type->is_javascript() || mime_type->is_json() || type.is_one_of("text/css"sv, "text/plain"sv, "text/vtt"sv))
        return build_text_document(document, data);
    if (type == "multipart/x-mixed-replace") {
        // FIXME: Implement multipart/x-mixed-replace support.
    }
    if (mime_type->is_image() || mime_type->is_audio_or_video())
        return build_media_document(document, data);
    if (type == "application/pdf" || type == "text/pdf") {
        // FIXME: Implement PDF support.
    }

    if (type == "text/markdown")
        return build_markdown_document(document, data);
    if (type == "text/gemini")
        return build_gemini_document(document, data);

    // FIXME: If the MIME type is not supported, we should either download it or pass it on to an external program

    return Error::from_string_view("Unsupported document type!"sv);
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

    if (type == Type::Navigation || type == Type::Reload || type == Type::Redirect) {
        if (auto* page = browsing_context().page()) {
            if (&page->top_level_browsing_context() == &m_browsing_context)
                page->client().page_did_start_loading(url, type == Type::Redirect);
        }
    }

    // https://fetch.spec.whatwg.org/#concept-fetch
    // Step 12: If request’s header list does not contain `Accept`, then:
    //          1. Let value be `*/*`. (NOTE: Not necessary as we're about to override it)
    //          2. A user agent should set value to the first matching statement, if any, switching on request’s destination:
    //              -> "document"
    //              -> "frame"
    //              -> "iframe"
    //                   `text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8`
    if (!request.headers().contains("Accept"))
        request.set_header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));

    if (type == Type::IFrame)
        return true;

    auto* document = browsing_context().active_document();
    if (document && document->has_active_favicon())
        return true;

    if (url.scheme() == "http" || url.scheme() == "https") {
        AK::URL favicon_url;
        favicon_url.set_scheme(url.scheme());
        favicon_url.set_host(url.host());
        favicon_url.set_port(url.port_or_default());
        favicon_url.set_paths({ "favicon.ico" });

        ResourceLoader::the().load(
            favicon_url,
            [this, favicon_url](auto data, auto&, auto) {
                // Always fetch the current document
                auto* document = this->browsing_context().active_document();
                if (document && document->has_active_favicon())
                    return;
                dbgln_if(SPAM_DEBUG, "Favicon downloaded, {} bytes from {}", data.size(), favicon_url);
                if (data.is_empty())
                    return;
                RefPtr<Gfx::Bitmap> favicon_bitmap;
                auto decoded_image = Platform::ImageCodecPlugin::the().decode_image(data);
                if (!decoded_image.has_value() || decoded_image->frames.is_empty()) {
                    dbgln("Could not decode favicon {}", favicon_url);
                } else {
                    favicon_bitmap = decoded_image->frames[0].bitmap;
                    dbgln_if(IMAGE_DECODER_DEBUG, "Decoded favicon, {}", favicon_bitmap->size());
                }
                load_favicon(favicon_bitmap);
            },
            [this](auto&, auto) {
                // Always fetch the current document
                auto* document = this->browsing_context().active_document();
                if (document && document->has_active_favicon())
                    return;

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
    auto& vm = Bindings::main_thread_vm();
    auto response = Fetch::Infrastructure::Response::create(vm);
    response->url_list().append(url);
    HTML::NavigationParams navigation_params {
        .id = {},
        .request = nullptr,
        .response = response,
        .origin = HTML::Origin {},
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = HTML::SandboxingFlagSet {},
        .cross_origin_opener_policy = HTML::CrossOriginOpenerPolicy {},
        .coop_enforcement_result = HTML::CrossOriginOpenerPolicyEnforcementResult {},
        .reserved_environment = {},
        .browsing_context = browsing_context(),
    };
    auto document = DOM::Document::create_and_initialize(
        DOM::Document::Type::HTML,
        "text/html",
        move(navigation_params));
    browsing_context().set_active_document(document);

    auto parser = HTML::HTMLParser::create(document, html, "utf-8");
    parser->run(url);
}

static DeprecatedString s_error_page_url = "file:///res/html/error.html";

void FrameLoader::set_error_page_url(DeprecatedString error_page_url)
{
    s_error_page_url = error_page_url;
}

// FIXME: Use an actual templating engine (our own one when it's built, preferably
// with a way to check these usages at compile time)

void FrameLoader::load_error_page(const AK::URL& failed_url, DeprecatedString const& error)
{
    ResourceLoader::the().load(
        s_error_page_url,
        [this, failed_url, error](auto data, auto&, auto) {
            VERIFY(!data.is_null());
            StringBuilder builder;
            SourceGenerator generator { builder };
            generator.set("failed_url", escape_html_entities(failed_url.to_deprecated_string()));
            generator.set("error", escape_html_entities(error));
            generator.append(data);
            load_html(generator.as_string_view(), failed_url);
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
        else if (s_default_favicon_bitmap)
            page->client().page_did_change_favicon(*s_default_favicon_bitmap);
    }
}

void FrameLoader::store_response_cookies(AK::URL const& url, DeprecatedString const& cookies)
{
    auto* page = browsing_context().page();
    if (!page)
        return;

    auto set_cookie_json_value = MUST(JsonValue::from_string(cookies));
    VERIFY(set_cookie_json_value.type() == JsonValue::Type::Array);

    for (auto const& set_cookie_entry : set_cookie_json_value.as_array().values()) {
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
            load(url.complete_url(location.value()), Type::Redirect);
            return;
        }
    }
    m_redirects_count = 0;

    if (resource()->has_encoding()) {
        dbgln_if(RESOURCE_DEBUG, "This content has MIME type '{}', encoding '{}'", resource()->mime_type(), resource()->encoding().value());
    } else {
        dbgln_if(RESOURCE_DEBUG, "This content has MIME type '{}', encoding unknown", resource()->mime_type());
    }

    auto final_sandboxing_flag_set = HTML::SandboxingFlagSet {};

    // (Part of https://html.spec.whatwg.org/#navigating-across-documents)
    // 3. Let responseOrigin be the result of determining the origin given browsingContext, resource's url, finalSandboxFlags, and incumbentNavigationOrigin.
    // FIXME: Pass incumbentNavigationOrigin
    auto response_origin = HTML::determine_the_origin(browsing_context(), url, final_sandboxing_flag_set, {});

    auto& vm = Bindings::main_thread_vm();
    auto response = Fetch::Infrastructure::Response::create(vm);
    response->url_list().append(url);
    HTML::NavigationParams navigation_params {
        .id = {},
        .request = nullptr,
        .response = response,
        .origin = move(response_origin),
        .policy_container = HTML::PolicyContainer {},
        .final_sandboxing_flag_set = final_sandboxing_flag_set,
        .cross_origin_opener_policy = HTML::CrossOriginOpenerPolicy {},
        .coop_enforcement_result = HTML::CrossOriginOpenerPolicyEnforcementResult {},
        .reserved_environment = {},
        .browsing_context = browsing_context(),
    };
    auto document = DOM::Document::create_and_initialize(
        DOM::Document::Type::HTML,
        resource()->mime_type(),
        move(navigation_params));

    document->set_url(url);
    document->set_encoding(resource()->encoding());
    document->set_content_type(resource()->mime_type());

    browsing_context().set_active_document(document);
    if (auto* page = browsing_context().page())
        page->client().page_did_create_main_document();

    auto error = load_document(document, resource()->encoded_data());
    if (error.is_error()) {
        load_error_page(url, error.error().string_literal());
        return;
    }

    if (!url.fragment().is_empty())
        browsing_context().scroll_to_anchor(url.fragment());
    else
        browsing_context().scroll_to({ 0, 0 });

    if (auto* page = browsing_context().page())
        page->client().page_did_finish_loading(url);
}

void FrameLoader::resource_did_fail()
{
    load_error_page(resource()->url(), resource()->error());
}

}
