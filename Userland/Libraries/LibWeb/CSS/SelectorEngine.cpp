/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/DeprecatedCSSParser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::SelectorEngine {

static bool matches_hover_pseudo_class(DOM::Element const& element)
{
    auto* hovered_node = element.document().hovered_node();
    if (!hovered_node)
        return false;
    if (&element == hovered_node)
        return true;
    return element.is_ancestor_of(*hovered_node);
}

static bool matches(CSS::Selector::SimpleSelector const& component, DOM::Element const& element)
{
    switch (component.pseudo_element) {
    case CSS::Selector::SimpleSelector::PseudoElement::None:
        break;
    default:
        // FIXME: Implement pseudo-elements.
        return false;
    }

    switch (component.pseudo_class) {
    case CSS::Selector::SimpleSelector::PseudoClass::None:
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Link:
        if (!element.is_link())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Visited:
        // FIXME: Maybe match this selector sometimes?
        return false;
    case CSS::Selector::SimpleSelector::PseudoClass::Active:
        if (!element.is_active())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Hover:
        if (!matches_hover_pseudo_class(element))
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Focus:
        if (!element.is_focused())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::FirstChild:
        if (element.previous_element_sibling())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::LastChild:
        if (element.next_element_sibling())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::OnlyChild:
        if (element.previous_element_sibling() || element.next_element_sibling())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Empty:
        if (element.first_child_of_type<DOM::Element>() || element.first_child_of_type<DOM::Text>())
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Root:
        if (!is<HTML::HTMLElement>(element))
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::FirstOfType:
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (sibling->tag_name() == element.tag_name())
                return false;
        }
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::LastOfType:
        for (auto* sibling = element.next_element_sibling(); sibling; sibling = sibling->next_element_sibling()) {
            if (sibling->tag_name() == element.tag_name())
                return false;
        }
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Disabled:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (!element.has_attribute("disabled"))
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Enabled:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (element.has_attribute("disabled"))
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Checked:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (!element.has_attribute("checked"))
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Not: {
        if (component.not_selector.is_empty())
            return false;
        auto not_selector = Web::parse_selector(CSS::DeprecatedParsingContext(element), component.not_selector);
        if (!not_selector.has_value())
            return false;
        auto not_matches = matches(not_selector.value(), element);
        if (not_matches)
            return false;
        break;
    }
    case CSS::Selector::SimpleSelector::PseudoClass::NthChild:
    case CSS::Selector::SimpleSelector::PseudoClass::NthLastChild:
        auto const step_size = component.nth_child_pattern.step_size;
        auto const offset = component.nth_child_pattern.offset;
        if (step_size == 0 && offset == 0)
            return false; // "If both a and b are equal to zero, the pseudo-class represents no element in the document tree."

        auto const* parent = element.parent_element();
        if (!parent)
            return false;

        int index = 1;
        if (component.pseudo_class == CSS::Selector::SimpleSelector::PseudoClass::NthChild) {
            for (auto* child = parent->first_child_of_type<DOM::Element>(); child && child != &element; child = child->next_element_sibling())
                ++index;
        } else {
            for (auto* child = parent->last_child_of_type<DOM::Element>(); child && child != &element; child = child->previous_element_sibling())
                ++index;
        }

        if (step_size < 0) {
            // When "step_size" is negative, selector represents first "offset" elements in document tree.
            if (offset <= 0 || index > offset)
                return false;
            else
                break;
        } else if (step_size == 1) {
            // When "step_size == 1", selector represents last "offset" elements in document tree.
            if (offset < 0 || index < offset)
                return false;
            else
                break;
        }

        // Like "a % b", but handles negative integers correctly.
        auto const canonical_modulo = [](int a, int b) -> int {
            int c = a % b;
            if ((c < 0 && b > 0) || (c > 0 && b < 0)) {
                c += b;
            }
            return c;
        };

        if (step_size == 0) {
            // Avoid divide by zero.
            if (index != offset) {
                return false;
            }
        } else if (canonical_modulo(index - offset, step_size) != 0) {
            return false;
        }
        break;
    }

    switch (component.attribute_match_type) {
    case CSS::Selector::SimpleSelector::AttributeMatchType::HasAttribute:
        if (!element.has_attribute(component.attribute_name))
            return false;
        break;
    case CSS::Selector::SimpleSelector::AttributeMatchType::ExactValueMatch:
        if (element.attribute(component.attribute_name) != component.attribute_value)
            return false;
        break;
    case CSS::Selector::SimpleSelector::AttributeMatchType::ContainsWord:
        if (!element.attribute(component.attribute_name).split(' ').contains_slow(component.attribute_value))
            return false;
        break;
    case CSS::Selector::SimpleSelector::AttributeMatchType::ContainsString:
        if (!element.attribute(component.attribute_name).contains(component.attribute_value))
            return false;
        break;
    case CSS::Selector::SimpleSelector::AttributeMatchType::StartsWithSegment:
        if (element.attribute(component.attribute_name).split('-').first() != component.attribute_value)
            return false;
        break;
    case CSS::Selector::SimpleSelector::AttributeMatchType::StartsWithString:
        if (!element.attribute(component.attribute_name).starts_with(component.attribute_value))
            return false;
        break;
    case CSS::Selector::SimpleSelector::AttributeMatchType::EndsWithString:
        if (!element.attribute(component.attribute_name).ends_with(component.attribute_value))
            return false;
        break;
    default:
        break;
    }

    switch (component.type) {
    case CSS::Selector::SimpleSelector::Type::Universal:
        return true;
    case CSS::Selector::SimpleSelector::Type::Id:
        return component.value == element.attribute(HTML::AttributeNames::id);
    case CSS::Selector::SimpleSelector::Type::Class:
        return element.has_class(component.value);
    case CSS::Selector::SimpleSelector::Type::TagName:
        return component.value == element.local_name();
    default:
        VERIFY_NOT_REACHED();
    }
}

static bool matches(CSS::Selector const& selector, int component_list_index, DOM::Element const& element)
{
    auto& component_list = selector.complex_selectors()[component_list_index];
    for (auto& component : component_list.compound_selector) {
        if (!matches(component, element))
            return false;
    }
    switch (component_list.relation) {
    case CSS::Selector::ComplexSelector::Relation::None:
        return true;
    case CSS::Selector::ComplexSelector::Relation::Descendant:
        VERIFY(component_list_index != 0);
        for (auto* ancestor = element.parent(); ancestor; ancestor = ancestor->parent()) {
            if (!is<DOM::Element>(*ancestor))
                continue;
            if (matches(selector, component_list_index - 1, verify_cast<DOM::Element>(*ancestor)))
                return true;
        }
        return false;
    case CSS::Selector::ComplexSelector::Relation::ImmediateChild:
        VERIFY(component_list_index != 0);
        if (!element.parent() || !is<DOM::Element>(*element.parent()))
            return false;
        return matches(selector, component_list_index - 1, verify_cast<DOM::Element>(*element.parent()));
    case CSS::Selector::ComplexSelector::Relation::AdjacentSibling:
        VERIFY(component_list_index != 0);
        if (auto* sibling = element.previous_element_sibling())
            return matches(selector, component_list_index - 1, *sibling);
        return false;
    case CSS::Selector::ComplexSelector::Relation::GeneralSibling:
        VERIFY(component_list_index != 0);
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (matches(selector, component_list_index - 1, *sibling))
                return true;
        }
        return false;
    case CSS::Selector::ComplexSelector::Relation::Column:
        TODO();
    }
    VERIFY_NOT_REACHED();
}

bool matches(CSS::Selector const& selector, DOM::Element const& element)
{
    VERIFY(!selector.complex_selectors().is_empty());
    return matches(selector, selector.complex_selectors().size() - 1, element);
}

}
