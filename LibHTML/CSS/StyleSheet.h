#pragma once

#include <AK/Vector.h>
#include <LibHTML/CSS/StyleRule.h>

class StyleSheet {
public:
    StyleSheet();
    ~StyleSheet();

    const Vector<StyleRule>& rules() const { return m_rules; }

private:
    Vector<StyleRule> m_rules;
};
