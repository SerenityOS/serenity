/*
 * Copyright (c) 2021, the SerenityOS developers.
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

#include <AK/Debug.h>
#include <AK/URL.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/CSS/StyleSheet.h>
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

void CSSLoader::load_from_url(const URL& url)
{
    m_style_sheet = CSS::CSSStyleSheet::create({});
    m_style_sheet->set_owner_node(&m_owner_element);

    LoadRequest request;
    request.set_url(url);
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Generic, request));
}

void CSSLoader::resource_did_load()
{
    VERIFY(resource());

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
    if (!was_imported) {
        m_style_sheet->rules() = sheet->rules();
    }

    if (on_load)
        on_load();

    load_next_import_if_needed();
}

void CSSLoader::resource_did_fail()
{
    dbgln_if(CSS_LOADER_DEBUG, "CSSLoader: Resource did fail. URL: {}", resource()->url());

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
