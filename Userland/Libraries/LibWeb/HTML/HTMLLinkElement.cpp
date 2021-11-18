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
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLLinkElement.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::HTML {

HTMLLinkElement::HTMLLinkElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLLinkElement::~HTMLLinkElement()
{
}

void HTMLLinkElement::inserted()
{
    HTMLElement::inserted();

    if (m_relationship & Relationship::Stylesheet && !(m_relationship & Relationship::Alternate)) {
        auto url = document().parse_url(href());
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Loading import URL: {}", url);
        auto request = LoadRequest::create_for_url_on_page(url, document().page());
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
        m_document_load_event_delayer.emplace(document());
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
    }
}

void HTMLLinkElement::parse_attribute(const FlyString& name, const String& value)
{
    if (name == HTML::AttributeNames::rel) {
        m_relationship = 0;
        auto parts = value.split_view(' ');
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

    m_document_load_event_delayer.clear();

    if (!resource()->has_encoded_data()) {
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did load, no encoded data. URL: {}", resource()->url());
    } else {
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Resource did load, has encoded data. URL: {}", resource()->url());
    }

    auto sheet = parse_css(CSS::ParsingContext(document()), resource()->encoded_data());
    if (!sheet) {
        dbgln_if(CSS_LOADER_DEBUG, "HTMLLinkElement: Failed to parse stylesheet: {}", resource()->url());
        return;
    }

    sheet->set_owner_node(this);
    document().style_sheets().add_sheet(sheet.release_nonnull());
}

}
