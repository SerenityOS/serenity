#pragma once

#include <AK/Vector.h>
#include <LibHTML/CSS/Selector.h>
#include <LibHTML/CSS/StyleDeclaration.h>

class StyleRule : public RefCounted<StyleRule> {
public:
    static NonnullRefPtr<StyleRule> create(Vector<Selector>&& selectors, Vector<NonnullRefPtr<StyleDeclaration>>&& declarations)
    {
        return adopt(*new StyleRule(move(selectors), move(declarations)));
    }

    ~StyleRule();

    const Vector<Selector>& selectors() const { return m_selectors; }
    const Vector<NonnullRefPtr<StyleDeclaration>>& declarations() const { return m_declarations; }

    template<typename C>
    void for_each_declaration(C callback) const
    {
        for (auto& declaration : m_declarations)
            callback(*declaration);
    }

private:
    StyleRule(Vector<Selector>&&, Vector<NonnullRefPtr<StyleDeclaration>>&&);

    Vector<Selector> m_selectors;
    Vector<NonnullRefPtr<StyleDeclaration>> m_declarations;
};
