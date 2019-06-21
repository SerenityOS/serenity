#pragma once

#include <AK/Vector.h>
#include <LibHTML/CSS/Selector.h>
#include <LibHTML/CSS/StyleDeclaration.h>

class StyleRule : public RefCounted<StyleRule> {
public:
    NonnullRefPtr<StyleRule> create(Vector<Selector>&& selectors, Vector<NonnullRefPtr<StyleDeclaration>>&& declarations)
    {
        return adopt(*new StyleRule(move(selectors), move(declarations)));
    }

    ~StyleRule();

private:
    StyleRule(Vector<Selector>&&, Vector<NonnullRefPtr<StyleDeclaration>>&&);

    Vector<Selector> m_selectors;
    Vector<NonnullRefPtr<StyleDeclaration>> m_declarations;
};
