/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/URL.h>
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

    load_next_import_if_needed();
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

    bool was_imported = m_style_sheet->for_first_not_loaded_import_rule([&](auto& rule) {
        rule.set_style_sheet(sheet);
    });

    // Transfer the rules from the successfully parsed sheet into the sheet we've already inserted.
    // FIXME: @import rules need work.
    if (!was_imported) {
        m_style_sheet->set_rules(sheet->rules());
    }

    if (on_load)
        on_load();

    load_next_import_if_needed();
}

void CSSLoader::resource_did_fail()
{
    dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Resource did fail. URL: {}", resource()->url());

    m_document_load_event_delayer.clear();

    load_next_import_if_needed();
}

void CSSLoader::load_next_import_if_needed()
{
    // Create load request for the first import which isn't loaded.
    // TODO: We need to somehow handle infinite cycles in imports.
    m_style_sheet->for_first_not_loaded_import_rule([&](auto& rule) {
        dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Loading @import {}", rule.url());

        LoadRequest request;
        request.set_url(rule.url());
        set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
    });
}

}
