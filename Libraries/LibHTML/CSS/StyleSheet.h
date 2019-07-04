#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibHTML/CSS/StyleRule.h>

class StyleSheet : public RefCounted<StyleSheet> {
public:
    static NonnullRefPtr<StyleSheet> create(NonnullRefPtrVector<StyleRule>&& rules)
    {
        return adopt(*new StyleSheet(move(rules)));
    }

    ~StyleSheet();

    const NonnullRefPtrVector<StyleRule>& rules() const { return m_rules; }

private:
    explicit StyleSheet(NonnullRefPtrVector<StyleRule>&&);

    NonnullRefPtrVector<StyleRule> m_rules;
};
