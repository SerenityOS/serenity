/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteBuffer.h>
#include <AK/Debug.h>
#include <AK/URL.h>
#include <LibWeb/Bindings/HTMLLinkElementPrototype.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/ImageDecoding.h>
#include <LibWeb/Loader/ResourceLoader.h>
#include <LibWeb/Page/Page.h>

namespace Web::HTML {

HTMLLinkElement::HTMLLinkElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    set_prototype(&window().ensure_web_prototype<Bindings::HTMLLinkElementPrototype>("HTMLLinkElement"));
}

HTMLLinkElement::~HTMLLinkElement() = default;

void HTMLLinkElement::inserted()
{
    HTMLElement::inserted();

    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate)) {
        auto url = document().parse_url(href());
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Loading import URL: {}", url);
        auto request = LoadRequest::create_for_url_on_page(url, document().page());
        // NOTE: Mark this element as delaying the document load event *before* calling set_resource()
        //       as it may trigger a synchronous resource_did_load() callback.
        m_document_load_event_delayer.emplace(document());
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));

        // NOTE: If we ended up not loading a resource for whatever reason, don't delay the load event.
        if (!resource())
            m_document_load_event_delayer.clear();
    }

    if (m_relationship & Relationship::Preload) {
        // FIXME: Respect the "as" attribute.
        LoadRequest request;
        request.set_url(document().parse_url(attribute(HTML::AttributeNames::href)));
        m_preload_resource = ResourceLoader::the().load_resource(Resource::Type::Generic, request);
    } else if (m_relationship & Relationship::DNSPrefetch) {
        ResourceLoader::the().prefetch_dns(document().parse_url(attribute(HTML::AttributeNames::href)));
    } else if (m_relationship & Relationship::Preconnect) {
        ResourceLoader::the().preconnect(document().parse_url(attribute(HTML::AttributeNames::href)));
    } else if (m_relationship & Relationship::Icon) {
        auto favicon_url = document().parse_url(href());
        auto favicon_request = LoadRequest::create_for_url_on_page(favicon_url, document().page());
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, favicon_request));
    }
}

bool HTMLLinkElement::has_loaded_icon() const
{
    return m_relationship & Relationship::Icon && resource() && resource()->is_loaded() && resource()->has_encoded_data();
}

void HTMLLinkElement::parse_attribute(FlyString const& name, String const& value)
{
    // 4.6.7 Link types - https://html.spec.whatwg.org/multipage/links.html#linkTypes
    if (name == HTML::AttributeNames::rel) {
        m_relationship = 0;
        // Keywords are always ASCII case-insensitive, and must be compared as such.
        auto lowercased_value = value.to_lowercase();
        // To determine which link types apply to a link, a, area, or form element,
        // the element's rel attribute must be split on ASCII whitespace.
        // The resulting tokens are the keywords for the link types that apply to that element.
        auto parts = lowercased_value.split_view(' ');
        for (auto& part : parts) {
            if (part == "stylesheet"sv)
                m_relationship |= Relationship::Stylesheet;
            else if (part == "alternate"sv)
                m_relationship |= Relationship::Alternate;
            else if (part == "preload"sv)
                m_relationship |= Relationship::Preload;
            else if (part == "dns-prefetch"sv)
                m_relationship |= Relationship::DNSPrefetch;
            else if (part == "preconnect"sv)
                m_relationship |= Relationship::Preconnect;
            else if (part == "icon"sv)
                m_relationship |= Relationship::Icon;
        }
    }
}

void HTMLLinkElement::resource_did_fail()
{
    dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did fail. URL: {}", resource()->url());

    m_document_load_event_delayer.clear();
}

void HTMLLinkElement::resource_did_load()
{
    VERIFY(resource());
    VERIFY(m_relationship & (Relationship::Stylesheet | Relationship::Icon));

    if (m_relationship & Relationship::Stylesheet)
        resource_did_load_stylesheet();
    if (m_relationship & Relationship::Icon)
        resource_did_load_favicon();
}

void HTMLLinkElement::resource_did_load_stylesheet()
{
    VERIFY(m_relationship & Relationship::Stylesheet);
    m_document_load_event_delayer.clear();

    if (!resource()->has_encoded_data()) {
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did load, no encoded data. URL: {}", resource()->url());
    } else {
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did load, has encoded data. URL: {}", resource()->url());

        if (resource()->mime_type() != "text/css"sv) {
            dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did load, but MIME type was {} instead of text/css. URL: {}", resource()->mime_type(), resource()->url());
            return;
        }
    }

    auto* sheet = parse_css_stylesheet(CSS::Parser::ParsingContext(document(), resource()->url()), resource()->encoded_data());
    if (!sheet) {
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Failed to parse stylesheet: {}", resource()->url());
        return;
    }

    sheet->set_owner_node(this);
    document().style_sheets().add_sheet(*sheet);
}

void HTMLLinkElement::resource_did_load_favicon()
{
    VERIFY(m_relationship & (Relationship::Icon));
    if (!resource()->has_encoded_data()) {
        dbgln_if(SPAM_DEBUG, "Favicon downloaded, no encoded data");
        return;
    }

    dbgln_if(SPAM_DEBUG, "Favicon downloaded, {} bytes from {}", resource()->encoded_data().size(), resource()->url());

    document().check_favicon_after_loading_link_resource();
}

bool HTMLLinkElement::load_favicon_and_use_if_window_is_active()
{
    if (!has_loaded_icon())
        return false;

    RefPtr<Gfx::Bitmap> favicon_bitmap;
    auto decoded_image = Web::ImageDecoding::Decoder::the().decode_image(resource()->encoded_data());
    if (!decoded_image.has_value() || decoded_image->frames.is_empty()) {
        dbgln("Could not decode favicon {}", resource()->url());
        return false;
    }

    favicon_bitmap = decoded_image->frames[0].bitmap;
    dbgln_if(IMAGE_DECODER_DEBUG, "Decoded favicon, {}", favicon_bitmap->size());

    auto* page = document().page();
    if (!page)
        return favicon_bitmap;

    if (document().browsing_context() == &page->top_level_browsing_context())
        if (favicon_bitmap) {
            page->client().page_did_change_favicon(*favicon_bitmap);
            return true;
        }

    return false;
}

}
