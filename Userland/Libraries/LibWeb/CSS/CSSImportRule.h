/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/URL.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/DOM/DocumentLoadEventDelayer.h>

namespace Web::CSS {

class CSSImportRule
    : public CSSRule
    , public ResourceClient {
    AK_MAKE_NONCOPYABLE(CSSImportRule);
    AK_MAKE_NONMOVABLE(CSSImportRule);

public:
    static NonnullRefPtr<CSSImportRule> create(AK::URL url, DOM::Document& document)
    {
        return adopt_ref(*new CSSImportRule(move(url), document));
    }

    ~CSSImportRule();

    const AK::URL& url() const { return m_url; }

    bool has_import_result() const { return !m_style_sheet.is_null(); }
    RefPtr<CSSStyleSheet> loaded_style_sheet() { return m_style_sheet; }
    const RefPtr<CSSStyleSheet> loaded_style_sheet() const { return m_style_sheet; }
    void set_style_sheet(const RefPtr<CSSStyleSheet>& style_sheet) { m_style_sheet = style_sheet; }

    virtual StringView class_name() const override { return "CSSImportRule"; };
    virtual Type type() const override { return Type::Import; };

private:
    explicit CSSImportRule(AK::URL, DOM::Document&);

    virtual String serialized() const override;

    // ^ResourceClient
    virtual void resource_did_fail() override;
    virtual void resource_did_load() override;

    AK::URL m_url;
    WeakPtr<DOM::Document> m_document;
    Optional<DOM::DocumentLoadEventDelayer> m_document_load_event_delayer;
    RefPtr<CSSStyleSheet> m_style_sheet;
};

template<>
inline bool CSSRule::fast_is<CSSImportRule>() const { return type() == CSSRule::Type::Import; }

}
