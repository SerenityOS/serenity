/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/NonnullRefPtrVector.h>
#include <AK/TypeCasts.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/Loader/Resource.h>

namespace Web::CSS {

class CSSStyleSheet final : public StyleSheet {
public:
    using WrapperType = Bindings::CSSStyleSheetWrapper;

    static NonnullRefPtr<CSSStyleSheet> create(NonnullRefPtrVector<CSSRule> rules)
    {
        return adopt(*new CSSStyleSheet(move(rules)));
    }

    virtual ~CSSStyleSheet() override;

    virtual String type() const override { return "text/css"; }

    const NonnullRefPtrVector<CSSRule>& rules() const { return m_rules; }
    NonnullRefPtrVector<CSSRule>& rules() { return m_rules; }

    template<typename Callback>
    void for_each_effective_style_rule(Callback callback) const
    {
        for (auto& rule : m_rules)
            if (rule.type() == CSSRule::Type::Style) {
                callback(downcast<CSSStyleRule>(rule));
            } else if (rule.type() == CSSRule::Type::Import) {
                const auto& import_rule = downcast<CSSImportRule>(rule);
                if (import_rule.has_import_result())
                    import_rule.loaded_style_sheet()->for_each_effective_style_rule(callback);
            }
    }

    template<typename Callback>
    bool for_first_not_loaded_import_rule(Callback callback)
    {
        for (auto& rule : m_rules)
            if (rule.type() == CSSRule::Type::Import) {
                auto& import_rule = downcast<CSSImportRule>(rule);
                if (!import_rule.has_import_result()) {
                    callback(import_rule);
                    return true;
                }

                if (import_rule.loaded_style_sheet()->for_first_not_loaded_import_rule(callback)) {
                    return true;
                }
            }

        return false;
    }

private:
    explicit CSSStyleSheet(NonnullRefPtrVector<CSSRule>);

    NonnullRefPtrVector<CSSRule> m_rules;
};

}

namespace Web::Bindings {

CSSStyleSheetWrapper* wrap(JS::GlobalObject&, CSS::CSSStyleSheet&);

}
