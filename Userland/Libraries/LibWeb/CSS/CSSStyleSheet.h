/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/TypeCasts.h>
#include <LibWeb/CSS/CSSImportRule.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/Loader/Resource.h>

namespace Web::CSS {

class CSSStyleSheet final : public StyleSheet {
public:
    using WrapperType = Bindings::CSSStyleSheetWrapper;

    static NonnullRefPtr<CSSStyleSheet> create(NonnullRefPtrVector<CSSRule> rules)
    {
        return adopt_ref(*new CSSStyleSheet(move(rules)));
    }

    virtual ~CSSStyleSheet() override;

    virtual String type() const override { return "text/css"; }

    CSSRuleList const& rules() const { return m_rules; }
    CSSRuleList& rules() { return m_rules; }

    template<typename Callback>
    void for_each_effective_style_rule(Callback callback) const
    {
        for (auto& rule : m_rules)
            if (rule.type() == CSSRule::Type::Style) {
                callback(verify_cast<CSSStyleRule>(rule));
            } else if (rule.type() == CSSRule::Type::Import) {
                const auto& import_rule = verify_cast<CSSImportRule>(rule);
                if (import_rule.has_import_result())
                    import_rule.loaded_style_sheet()->for_each_effective_style_rule(callback);
            }
    }

    template<typename Callback>
    bool for_first_not_loaded_import_rule(Callback callback)
    {
        for (auto& rule : m_rules)
            if (rule.type() == CSSRule::Type::Import) {
                auto& import_rule = verify_cast<CSSImportRule>(rule);
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

    CSSRuleList m_rules;
};

}

namespace Web::Bindings {

CSSStyleSheetWrapper* wrap(JS::GlobalObject&, CSS::CSSStyleSheet&);

}
