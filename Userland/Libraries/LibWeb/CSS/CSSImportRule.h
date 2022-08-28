/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>

namespace Web::CSS {

class CSSImportRule final
    : public CSSRule
    , public ResourceClient {
    WEB_PLATFORM_OBJECT(CSSImportRule, CSSRule);

public:
    static CSSImportRule* create(AK::URL, DOM::Document&);

    virtual ~CSSImportRule() = default;

    AK::URL const& url() const { return m_url; }
    // FIXME: This should return only the specified part of the url. eg, "stuff/foo.css", not "https://example.com/stuff/foo.css".
    String href() const { return m_url.to_string(); }

    bool has_import_result() const { return !m_style_sheet; }
    CSSStyleSheet* loaded_style_sheet() { return m_style_sheet; }
    CSSStyleSheet const* loaded_style_sheet() const { return m_style_sheet; }
    CSSStyleSheet* style_sheet_for_bindings() { return m_style_sheet; }
    void set_style_sheet(CSSStyleSheet* style_sheet) { m_style_sheet = style_sheet; }

    virtual Type type() const override { return Type::Import; };

private:
    CSSImportRule(AK::URL, DOM::Document&);

    virtual void visit_edges(Cell::Visitor&) override;

    virtual String serialized() const override;

    // ^ResourceClient
    virtual void resource_did_fail() override;
    virtual void resource_did_load() override;

    AK::URL m_url;
    WeakPtr<DOM::Document> m_document;
    Optional<DOM::DocumentLoadEventDelayer> m_document_load_event_delayer;
    CSSStyleSheet* m_style_sheet { nullptr };
};

template<>
inline bool CSSRule::fast_is<CSSImportRule>() const { return type() == CSSRule::Type::Import; }

}

WRAPPER_HACK(CSSImportRule, Web::CSS)
