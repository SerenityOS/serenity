#include <LibHTML/CSS/Selector.h>

Selector::Selector(Vector<Component>&& components)
    : m_components(move(components))
{
}

Selector::~Selector()
{
}
