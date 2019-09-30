#include <LibHTML/CSS/StyleRule.h>

StyleRule::StyleRule(Vector<Selector>&& selectors, NonnullRefPtr<StyleDeclaration>&& declaration)
    : m_selectors(move(selectors))
    , m_declaration(move(declaration))
{
}

StyleRule::~StyleRule()
{
}
