#include <LibHTML/CSS/Selector.h>

Selector::Selector(Vector<ComplexSelector>&& component_lists)
    : m_complex_selectors(move(component_lists))
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

    for (auto& list : m_complex_selectors) {
        for (auto& simple_selector : list.compound_selector) {
            switch (simple_selector.type) {
            case SimpleSelector::Type::Id:
                ++ids;
                break;
            case SimpleSelector::Type::Class:
                ++classes;
                break;
            case SimpleSelector::Type::TagName:
                ++tag_names;
                break;
            default:
                break;
            }
        }
    }

    return { ids, classes, tag_names };
}
