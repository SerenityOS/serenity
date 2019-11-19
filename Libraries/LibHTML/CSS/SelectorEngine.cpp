#include <LibHTML/CSS/SelectorEngine.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>

namespace SelectorEngine {

static bool matches_hover_pseudo_class(const Element& element)
{
    auto* hovered_node = element.document().hovered_node();
    if (!hovered_node)
        return false;
    if (&element == hovered_node)
        return true;
    return element.is_ancestor_of(*hovered_node);
}

bool matches(const Selector::Component& component, const Element& element)
{
    switch (component.pseudo_class) {
    case Selector::Component::PseudoClass::None:
        break;
    case Selector::Component::PseudoClass::Link:
        if (!element.is_link())
            return false;
        break;
    case Selector::Component::PseudoClass::Hover:
        if (!matches_hover_pseudo_class(element))
            return false;
        break;
    }

    switch (component.type) {
    case Selector::Component::Type::Universal:
        return true;
    case Selector::Component::Type::Id:
        return component.value == element.attribute("id");
    case Selector::Component::Type::Class:
        return element.has_class(component.value);
    case Selector::Component::Type::TagName:
        return component.value == element.tag_name();
    default:
        ASSERT_NOT_REACHED();
    }
}

bool matches(const Selector& selector, int component_index, const Element& element)
{
    auto& component = selector.components()[component_index];
    if (!matches(component, element))
        return false;
    switch (component.relation) {
    case Selector::Component::Relation::None:
        return true;
    case Selector::Component::Relation::Descendant:
        ASSERT(component_index != 0);
        for (auto* ancestor = element.parent(); ancestor; ancestor = ancestor->parent()) {
            if (!is<Element>(*ancestor))
                continue;
            if (matches(selector, component_index - 1, to<Element>(*ancestor)))
                return true;
        }
        return false;
    case Selector::Component::Relation::ImmediateChild:
        ASSERT(component_index != 0);
        if (!element.parent() || !is<Element>(*element.parent()))
            return false;
        return matches(selector, component_index - 1, to<Element>(*element.parent()));
    case Selector::Component::Relation::AdjacentSibling:
        ASSERT(component_index != 0);
        if (auto* sibling = element.previous_element_sibling())
            return matches(selector, component_index - 1, *sibling);
        return false;
    case Selector::Component::Relation::GeneralSibling:
        ASSERT(component_index != 0);
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (matches(selector, component_index - 1, *sibling))
                return true;
        }
        return false;
    }
    ASSERT_NOT_REACHED();
}

bool matches(const Selector& selector, const Element& element)
{
    ASSERT(!selector.components().is_empty());
    return matches(selector, selector.components().size() - 1, element);
}

}
