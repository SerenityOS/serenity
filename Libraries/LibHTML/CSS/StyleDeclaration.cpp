#include <LibHTML/CSS/StyleDeclaration.h>

StyleDeclaration::StyleDeclaration(Vector<StyleProperty>&& properties)
    : m_properties(move(properties))
{
}

StyleDeclaration::~StyleDeclaration()
{
}
