/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/URL.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Loader/CSSLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {

CSSLoader::CSSLoader(DOM::Element& owner_element)
    : m_owner_element(owner_element)
{
}

void CSSLoader::load_from_text(const String& text)
{
    m_style_sheet = parse_css(CSS::ParsingContext(m_owner_element.document()), text);
    if (!m_style_sheet) {
        m_style_sheet = CSS::CSSStyleSheet::create({});
        m_style_sheet->set_owner_node(&m_owner_element);
    }
}

void CSSLoader::load_from_url(const AK::URL& url)
{
    m_style_sheet = CSS::CSSStyleSheet::create({});
    m_style_sheet->set_owner_node(&m_owner_element);

    auto request = LoadRequest::create_for_url_on_page(url, m_owner_element.document().page());
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));

    m_document_load_event_delayer.emplace(m_owner_element.document());
}

void CSSLoader::resource_did_load()
{
    VERIFY(resource());

    m_document_load_event_delayer.clear();

    if (!resource()->has_encoded_data()) {
        dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Resource did load, no encoded data. URL: {}", resource()->url());
    } else {
        dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Resource did load, has encoded data. URL: {}", resource()->url());
    }

    auto sheet = parse_css(CSS::ParsingContext(m_owner_element.document()), resource()->encoded_data());
    if (!sheet) {
        dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Failed to parse stylesheet: {}", resource()->url());
        return;
    }

    // Transfer the rules from the successfully parsed sheet into the sheet we've already inserted.
    m_style_sheet->set_rules(sheet->rules());

    if (on_load)
        on_load();
}

void CSSLoader::resource_did_fail()
{
    dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Resource did fail. URL: {}", resource()->url());

    m_document_load_event_delayer.clear();
}

}
