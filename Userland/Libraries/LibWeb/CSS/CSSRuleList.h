/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Iterator.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class CSSRuleList {
public:
    explicit CSSRuleList(NonnullRefPtrVector<CSSRule>&&);
    virtual ~CSSRuleList();

    RefPtr<CSSRule> item(size_t index) const
    {
        if (index >= length())
            return nullptr;
        return m_rules[index];
    }
    size_t length() const { return m_rules.size(); }

    using ConstIterator = AK::SimpleIterator<AK::NonnullPtrVector<NonnullRefPtr<CSSRule>> const, CSSRule const>;
    using Iterator = AK::SimpleIterator<AK::NonnullPtrVector<NonnullRefPtr<CSSRule>>, CSSRule>;

    ConstIterator const begin() const { return m_rules.begin(); }
    Iterator begin() { return m_rules.begin(); }

    ConstIterator const end() const { return m_rules.end(); }
    Iterator end() { return m_rules.end(); }

private:
    NonnullRefPtrVector<CSSRule> m_rules;
};

}
