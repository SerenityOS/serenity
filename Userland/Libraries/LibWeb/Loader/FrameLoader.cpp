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
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <LibMarkdown/Document.h>
#include <LibTextCodec/Decoder.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLIFrameElement.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Parser/HTMLEncodingDetection.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Namespace.h>
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
        s_default_favicon_bitmap = Gfx::Bitmap::load_from_file(s_default_favicon_path).release_value_but_fixme_should_propagate_errors();
        VERIFY(s_default_favicon_bitmap);
    }
}

FrameLoader::~FrameLoader() = default;

static bool build_markdown_document(DOM::Document& document, ByteBuffer const& data)
{
    auto markdown_document = Markdown::Document::parse(data);
    if (!markdown_document)
        return false;

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
    return true;
}

static bool build_text_document(DOM::Document& document, ByteBuffer const& data)
{
    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));
    auto title_element = DOM::create_element(document, HTML::TagNames::title, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(head_element->append_child(title_element));

    auto title_text = document.create_text_node(document.url().basename());
    MUST(title_element->append_child(title_text));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto pre_element = DOM::create_element(document, HTML::TagNames::pre, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(body_element->append_child(pre_element));

    MUST(pre_element->append_child(document.create_text_node(DeprecatedString::copy(data))));
    return true;
}

static bool build_image_document(DOM::Document& document, ByteBuffer const& data)
{
    auto image = Platform::ImageCodecPlugin::the().decode_image(data);
    if (!image.has_value() || image->frames.is_empty())
        return false;
    auto const& frame = image->frames[0];
    auto const& bitmap = frame.bitmap;
    if (!bitmap)
        return false;

    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));
    auto title_element = DOM::create_element(document, HTML::TagNames::title, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(head_element->append_child(title_element));

    auto basename = LexicalPath::basename(document.url().serialize_path());
    auto title_text = document.heap().allocate<DOM::Text>(document.realm(), document, DeprecatedString::formatted("{} [{}x{}]", basename, bitmap->width(), bitmap->height())).release_allocated_value_but_fixme_should_propagate_errors();
    MUST(title_element->append_child(*title_text));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto image_element = DOM::create_element(document, HTML::TagNames::img, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(image_element->set_attribute(HTML::AttributeNames::src, document.url().to_deprecated_string()));
    MUST(body_element->append_child(image_element));

    return true;
}

static bool build_gemini_document(DOM::Document& document, ByteBuffer const& data)
{
    StringView gemini_data { data };
    auto gemini_document = Gemini::Document::parse(gemini_data, document.url());
    DeprecatedString html_data = gemini_document->render_to_html();

    dbgln_if(GEMINI_DEBUG, "Gemini data:\n\"\"\"{}\"\"\"", gemini_data);
    dbgln_if(GEMINI_DEBUG, "Converted to HTML:\n\"\"\"{}\"\"\"", html_data);

    auto parser = HTML::HTMLParser::create(document, html_data, "utf-8");
    parser->run(document.url());
    return true;
}

static bool build_xml_document(DOM::Document& document, ByteBuffer const& data)
{
    auto encoding = HTML::run_encoding_sniffing_algorithm(document, data);
    auto decoder = TextCodec::decoder_for(encoding);
    VERIFY(decoder.has_value());
    auto source = decoder->to_utf8(data).release_value_but_fixme_should_propagate_errors();
    XML::Parser parser(source, { .resolve_external_resource = resolve_xml_resource });
    XMLDocumentBuilder builder { document };
    auto result = parser.parse_with_listener(builder);
    return !result.is_error() && !builder.has_error();
}

static bool build_video_document(DOM::Document& document)
{
    auto html_element = DOM::create_element(document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(document.append_child(html_element));

    auto head_element = DOM::create_element(document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));

    auto body_element = DOM::create_element(document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    auto video_element = DOM::create_element(document, HTML::TagNames::video, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(video_element->set_attribute(HTML::AttributeNames::src, document.url().to_deprecated_string()));
    MUST(video_element->set_attribute(HTML::AttributeNames::autoplay, DeprecatedString::empty()));
    MUST(video_element->set_attribute(HTML::AttributeNames::controls, DeprecatedString::empty()));
    MUST(body_element->append_child(video_element));

    return true;
}

bool FrameLoader::parse_document(DOM::Document& document, ByteBuffer const& data)
{
    auto& mime_type = document.content_type();
    if (mime_type == "text/html" || mime_type == "image/svg+xml") {
        auto parser = HTML::HTMLParser::create_with_uncertain_encoding(document, data);
        parser->run(document.url());
        return true;
    }
    if (mime_type.ends_with("+xml"sv) || mime_type.is_one_of("text/xml", "application/xml"))
        return build_xml_document(document, data);
    if (mime_type.starts_with("image/"sv))
        return build_image_document(document, data);
    if (mime_type.starts_with("video/"sv))
        return build_video_document(document);
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

    if (!m_browsing_context->is_frame_nesting_allowed(request.url())) {
        dbgln("No further recursion is allowed for the frame, abort load!");
        return false;
    }

    request.set_main_resource(true);

    auto& url = request.url();

    if (type == Type::Navigation || type == Type::Reload || type == Type::Redirect) {
        if (auto* page = browsing_context().page()) {
            if (&page->top_level_browsing_context() == m_browsing_context)
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
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", move(navigation_params)).release_value_but_fixme_should_propagate_errors();
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
    LoadRequest request = LoadRequest::create_for_url_on_page(s_error_page_url, browsing_context().page());

    ResourceLoader::the().load(
        request,
        [this, failed_url, error](auto data, auto&, auto) {
            VERIFY(!data.is_null());
            StringBuilder builder;
            SourceGenerator generator { builder };
            generator.set("failed_url", escape_html_entities(failed_url.to_deprecated_string()));
            generator.set("error", escape_html_entities(error));
            generator.append(data);
            load_html(generator.as_string_view(), s_error_page_url);
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

void FrameLoader::resource_did_load()
{
    // This prevents us setting up the document of a removed browsing context container (BCC, e.g. <iframe>), which will cause a crash
    // if the document contains a script that inserts another BCC as this will use the stale browsing context it previously set up,
    // even if it's reinserted.
    // Example:
    // index.html:
    // ```
    // <body><script>
    //     var i = document.createElement("iframe");
    //     i.src = "b.html";
    //     document.body.append(i);
    //     i.remove();
    // </script>
    // ```
    // b.html:
    // ```
    // <body><script>
    //     var i = document.createElement("iframe");
    //     document.body.append(i);
    // </script>
    // ```
    // Required by Prebid.js, which does this by inserting an <iframe> into a <div> in the active document via innerHTML,
    // then transfers it to the <html> element:
    // https://github.com/prebid/Prebid.js/blob/7b7389c5abdd05626f71c3df606a93713d1b9f85/src/utils.js#L597
    // This is done in the spec by removing all tasks and aborting all fetches when a document is destroyed:
    // https://html.spec.whatwg.org/multipage/document-lifecycle.html#destroy-a-document
    if (browsing_context().has_been_discarded())
        return;

    auto url = resource()->url();

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
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", move(navigation_params)).release_value_but_fixme_should_propagate_errors();
    document->set_url(url);
    document->set_encoding(resource()->encoding());
    document->set_content_type(resource()->mime_type());

    browsing_context().set_active_document(document);
    if (auto* page = browsing_context().page())
        page->client().page_did_create_main_document();

    if (!parse_document(*document, resource()->encoded_data())) {
        load_error_page(url, "Failed to parse content.");
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
    // See comment in resource_did_load() about why this is done.
    if (browsing_context().has_been_discarded())
        return;

    load_error_page(resource()->url(), resource()->error());
}

}
