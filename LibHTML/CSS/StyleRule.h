#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <LibHTML/CSS/Selector.h>
#include <LibHTML/CSS/StyleDeclaration.h>

class StyleRule : public RefCounted<StyleRule> {
public:
    static NonnullRefPtr<StyleRule> create(Vector<Selector>&& selectors, NonnullRefPtrVector<StyleDeclaration>&& declarations)
    {
        return adopt(*new StyleRule(move(selectors), move(declarations)));
    }

    ~StyleRule();

    const Vector<Selector>& selectors() const { return m_selectors; }
    const NonnullRefPtrVector<StyleDeclaration>& declarations() const { return m_declarations; }

    template<typename C>
    void for_each_declaration(C callback) const
    {
        for (auto& declaration : m_declarations)
            callback(declaration);
    }

private:
    StyleRule(Vector<Selector>&&, NonnullRefPtrVector<StyleDeclaration>&&);

    Vector<Selector> m_selectors;
    NonnullRefPtrVector<StyleDeclaration> m_declarations;
};
