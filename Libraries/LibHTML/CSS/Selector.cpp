#include <LibHTML/CSS/Selector.h>

Selector::Selector(Vector<Component>&& components)
    : m_components(move(components))
{
}

Selector::~Selector()
{
}

Specificity Selector::specificity() const
{
    unsigned ids = 0;
    unsigned tag_names = 0;
    unsigned classes = 0;

    for (auto& component : m_components) {
        switch (component.type) {
        case Component::Type::Id:
            ++ids;
            break;
        case Component::Type::Class:
            ++classes;
            break;
        case Component::Type::TagName:
            ++tag_names;
            break;
        default:
            break;
        }
    }

    return { ids, classes, tag_names };
}
