#include <LibHTML/CSS/StyleDeclaration.h>

StyleDeclaration::StyleDeclaration(const String& property_name, NonnullRefPtr<StyleValue>&& value)
    : m_property_name(property_name)
    , m_value(move(value))
{
}

StyleDeclaration::~StyleDeclaration()
{
}
