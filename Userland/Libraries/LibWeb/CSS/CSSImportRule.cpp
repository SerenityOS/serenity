/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/URL.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web::CSS {

CSSImportRule::CSSImportRule(AK::URL url, DOM::Document& document)
    : m_url(move(url))
    , m_document(document)
{
    dbgln_if(CSS_LOADER_DEBUG, "CSSImportRule: Loading import URL: {}", m_url);
    auto request = LoadRequest::create_for_url_on_page(m_url, document.page());

    // NOTE: Mark this rule as delaying the document load event *before* calling set_resource()
    //       as it may trigger a synchronous resource_did_load() callback.
    m_document_load_event_delayer.emplace(document);

    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
}

CSSImportRule::~CSSImportRule()
{
}

// https://www.w3.org/TR/cssom/#serialize-a-css-rule
String CSSImportRule::serialized() const
{
    StringBuilder builder;
    // The result of concatenating the following:

    // 1. The string "@import" followed by a single SPACE (U+0020).
    builder.append("@import "sv);

    // 2. The result of performing serialize a URL on the rule’s location.
    // FIXME: Look into the correctness of this serialization
    builder.append("url("sv);
    builder.append(m_url.to_string());
    builder.append(')');

    // FIXME: 3. If the rule’s associated media list is not empty, a single SPACE (U+0020) followed by the result of performing serialize a media query list on the media list.

    // 4. The string ";", i.e., SEMICOLON (U+003B).
    builder.append(';');

    return builder.to_string();
}

void CSSImportRule::resource_did_fail()
{
    dbgln_if(CSS_LOADER_DEBUG, "CSSImportRule: Resource did fail. URL: {}", resource()->url());

    m_document_load_event_delayer.clear();
}

void CSSImportRule::resource_did_load()
{
    VERIFY(resource());

    if (!m_document)
        return;

    m_document_load_event_delayer.clear();

    if (!resource()->has_encoded_data()) {
        dbgln_if(CSS_LOADER_DEBUG, "CSSImportRule: Resource did load, no encoded data. URL: {}", resource()->url());
    } else {
        dbgln_if(CSS_LOADER_DEBUG, "CSSImportRule: Resource did load, has encoded data. URL: {}", resource()->url());
    }

    auto sheet = parse_css(CSS::ParsingContext(*m_document), resource()->encoded_data());
    if (!sheet) {
        dbgln_if(CSS_LOADER_DEBUG, "CSSImportRule: Failed to parse stylesheet: {}", resource()->url());
        return;
    }

    m_style_sheet = move(sheet);
}

}
