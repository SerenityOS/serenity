/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class CSSGroupingRule : public CSSRule {
    AK_MAKE_NONCOPYABLE(CSSGroupingRule);
    AK_MAKE_NONMOVABLE(CSSGroupingRule);

public:
    ~CSSGroupingRule();

    CSSRuleList const& css_rules() const { return m_rules; }
    size_t insert_rule(StringView const& rule, size_t index = 0);
    void delete_rule(size_t index);

    virtual String serialized() const;

protected:
    explicit CSSGroupingRule(NonnullRefPtrVector<CSSRule>&&);

private:
    NonnullRefPtr<CSSRuleList> m_rules;
};

}
