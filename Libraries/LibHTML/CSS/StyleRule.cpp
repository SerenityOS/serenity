#include <LibHTML/CSS/StyleRule.h>

StyleRule::StyleRule(Vector<Selector>&& selectors, NonnullRefPtrVector<StyleDeclaration>&& declarations)
    : m_selectors(move(selectors))
    , m_declarations(move(declarations))
{
}

StyleRule::~StyleRule()
{
}
