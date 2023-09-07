/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/JsonArray.h>
#include <AK/LexicalPath.h>
#include <AK/SourceGenerator.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentLoading.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/NavigationParams.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Loader/FrameLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Namespace.h>
#include <LibWeb/Page/Page.h>
#include <LibWeb/Platform/ImageCodecPlugin.h>
#include <LibWeb/ReferrerPolicy/AbstractOperations.h>
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

    // HACK: We're crudely computing the referer value and shoving it into the
    //       request until fetch infrastructure is used here.
    auto referrer_url = ReferrerPolicy::strip_url_for_use_as_referrer(url);
    if (referrer_url.has_value() && !request.headers().contains("Referer"))
        request.set_header("Referer", referrer_url->serialize());

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
        .navigable = nullptr,
    };
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", move(navigation_params)).release_value_but_fixme_should_propagate_errors();
    browsing_context().set_active_document(document);

    auto parser = HTML::HTMLParser::create(document, html, "utf-8");
    parser->run(url);
}

static DeprecatedString s_resource_directory_url = "file:///res";

DeprecatedString FrameLoader::resource_directory_url()
{
    return s_resource_directory_url;
}

void FrameLoader::set_resource_directory_url(DeprecatedString resource_directory_url)
{
    s_resource_directory_url = resource_directory_url;
}

static DeprecatedString s_error_page_url = "file:///res/html/error.html";

DeprecatedString FrameLoader::error_page_url()
{
    return s_error_page_url;
}

void FrameLoader::set_error_page_url(DeprecatedString error_page_url)
{
    s_error_page_url = error_page_url;
}

static DeprecatedString s_directory_page_url = "file:///res/html/directory.html";

DeprecatedString FrameLoader::directory_page_url()
{
    return s_directory_page_url;
}

void FrameLoader::set_directory_page_url(DeprecatedString directory_page_url)
{
    s_directory_page_url = directory_page_url;
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
            generator.set("resource_directory_url", resource_directory_url());
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
        .navigable = nullptr,
    };
    auto document = DOM::Document::create_and_initialize(DOM::Document::Type::HTML, "text/html", move(navigation_params)).release_value_but_fixme_should_propagate_errors();
    document->set_url(url);
    document->set_encoding(resource()->encoding());
    document->set_content_type(resource()->mime_type());

    browsing_context().set_active_document(document);
    if (auto* page = browsing_context().page())
        page->client().page_did_create_new_document(*document);

    if (!parse_document(*document, resource()->encoded_data())) {
        load_error_page(url, "Failed to parse content.");
        return;
    }

    if (url.fragment().has_value() && !url.fragment()->is_empty())
        browsing_context().scroll_to_anchor(url.fragment()->to_deprecated_string());
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
