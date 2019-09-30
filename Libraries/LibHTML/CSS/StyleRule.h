#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibHTML/CSS/Selector.h>
#include <LibHTML/CSS/StyleDeclaration.h>

class StyleRule : public RefCounted<StyleRule> {
public:
    static NonnullRefPtr<StyleRule> create(Vector<Selector>&& selectors, NonnullRefPtr<StyleDeclaration>&& declaration)
    {
        return adopt(*new StyleRule(move(selectors), move(declaration)));
    }

    ~StyleRule();

    const Vector<Selector>& selectors() const { return m_selectors; }
    const StyleDeclaration& declaration() const { return m_declaration; }

private:
    StyleRule(Vector<Selector>&&, NonnullRefPtr<StyleDeclaration>&&);

    Vector<Selector> m_selectors;
    NonnullRefPtr<StyleDeclaration> m_declaration;
};
