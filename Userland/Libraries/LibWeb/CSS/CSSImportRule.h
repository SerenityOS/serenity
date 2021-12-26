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

#pragma once

#include <AK/URL.h>
#include <LibWeb/CSS/CSSRule.h>

namespace Web::CSS {

class CSSImportRule : public CSSRule {
    AK_MAKE_NONCOPYABLE(CSSImportRule);
    AK_MAKE_NONMOVABLE(CSSImportRule);

public:
    static NonnullRefPtr<CSSImportRule> create(URL url)
    {
        return adopt(*new CSSImportRule(move(url)));
    }

    ~CSSImportRule();

    const URL& url() const { return m_url; }

    bool has_import_result() const { return !m_style_sheet.is_null(); }
    RefPtr<CSSStyleSheet> loaded_style_sheet() { return m_style_sheet; }
    const RefPtr<CSSStyleSheet> loaded_style_sheet() const { return m_style_sheet; }
    void set_style_sheet(const RefPtr<StyleSheet>& style_sheet) { m_style_sheet = style_sheet; }

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
