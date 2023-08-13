/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>
#include <LibWeb/Loader/Resource.h>

namespace Web::CSS {

class CSSImportRule final
    : public CSSRule
    , public ResourceClient {
    WEB_PLATFORM_OBJECT(CSSImportRule, CSSRule);

public:
    [[nodiscard]] static JS::NonnullGCPtr<CSSImportRule> create(AK::URL, DOM::Document&);

    virtual ~CSSImportRule() = default;

    AK::URL const& url() const { return m_url; }
    // FIXME: This should return only the specified part of the url. eg, "stuff/foo.css", not "https://example.com/stuff/foo.css".
    DeprecatedString href() const { return m_url.to_deprecated_string(); }

    CSSStyleSheet* loaded_style_sheet() { return m_style_sheet; }
    CSSStyleSheet const* loaded_style_sheet() const { return m_style_sheet; }
    CSSStyleSheet* style_sheet_for_bindings() { return m_style_sheet; }
    void set_style_sheet(CSSStyleSheet* style_sheet) { m_style_sheet = style_sheet; }

    virtual Type type() const override { return Type::Import; }

private:
    CSSImportRule(AK::URL, DOM::Document&);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    virtual DeprecatedString serialized() const override;

    // ^ResourceClient
    virtual void resource_did_fail() override;
    virtual void resource_did_load() override;

    AK::URL m_url;
    JS::GCPtr<DOM::Document> m_document;
    JS::GCPtr<CSSStyleSheet> m_style_sheet;
    Optional<DOM::DocumentLoadEventDelayer> m_document_load_event_delayer;
};

template<>
inline bool CSSRule::fast_is<CSSImportRule>() const { return type() == CSSRule::Type::Import; }

}
