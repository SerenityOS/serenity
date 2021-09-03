/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/URL.h>
#include <LibWeb/CSS/CSSRule.h>

namespace Web::CSS {

class CSSImportRule : public CSSRule {
    YAK_MAKE_NONCOPYABLE(CSSImportRule);
    YAK_MAKE_NONMOVABLE(CSSImportRule);

public:
    static NonnullRefPtr<CSSImportRule> create(URL url)
    {
        return adopt_ref(*new CSSImportRule(move(url)));
    }

    ~CSSImportRule();

    const URL& url() const { return m_url; }

    bool has_import_result() const { return !m_style_sheet.is_null(); }
    RefPtr<CSSStyleSheet> loaded_style_sheet() { return m_style_sheet; }
    const RefPtr<CSSStyleSheet> loaded_style_sheet() const { return m_style_sheet; }
    void set_style_sheet(const RefPtr<CSSStyleSheet>& style_sheet) { m_style_sheet = style_sheet; }

    virtual StringView class_name() const { return "CSSImportRule"; };
    virtual Type type() const { return Type::Import; };

private:
    explicit CSSImportRule(URL);

    URL m_url;
    RefPtr<CSSStyleSheet> m_style_sheet;
};

template<>
inline bool CSSRule::fast_is<CSSImportRule>() const { return type() == CSSRule::Type::Import; }

}
