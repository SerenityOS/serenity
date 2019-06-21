#include <LibHTML/CSS/StyleRule.h>

StyleRule::StyleRule(Vector<Selector>&& selectors, Vector<NonnullRefPtr<StyleDeclaration>>&& declarations)
    : m_selectors(move(selectors))
    , m_declarations(move(declarations))
{
}

StyleRule::~StyleRule()
{
}
