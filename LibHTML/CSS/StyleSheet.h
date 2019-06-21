#pragma once

#include <AK/Vector.h>
#include <LibHTML/CSS/StyleRule.h>

class StyleSheet : public RefCounted<StyleSheet> {
public:
    static NonnullRefPtr<StyleSheet> create(Vector<NonnullRefPtr<StyleRule>>&& rules)
    {
        return adopt(*new StyleSheet(move(rules)));
    }

    ~StyleSheet();

    const Vector<NonnullRefPtr<StyleRule>>& rules() const { return m_rules; }

private:
    explicit StyleSheet(Vector<NonnullRefPtr<StyleRule>>&&);

    Vector<NonnullRefPtr<StyleRule>> m_rules;
};
