/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
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

static bool matches_attribute(CSS::Selector::SimpleSelector::Attribute const& attribute, DOM::Element const& element)
{
    switch (attribute.match_type) {
        using enum CSS::Selector::SimpleSelector::Attribute::MatchType;
    case HasAttribute:
        return element.has_attribute(attribute.name);
        break;
    case ExactValueMatch:
        return element.attribute(attribute.name) == attribute.value;
        break;
    case ContainsWord:
        return element.attribute(attribute.name).split_view(' ').contains_slow(attribute.value);
        break;
    case ContainsString:
        return element.attribute(attribute.name).contains(attribute.value);
        break;
    case StartsWithSegment: {
        auto segments = element.attribute(attribute.name).split_view('-');
        return !segments.is_empty() && segments.first() == attribute.value;
        break;
    }
    case StartsWithString:
        return element.attribute(attribute.name).starts_with(attribute.value);
        break;
    case EndsWithString:
        return element.attribute(attribute.name).ends_with(attribute.value);
        break;
    case None:
        VERIFY_NOT_REACHED();
        break;
    }

    return false;
}

static bool matches_pseudo_class(CSS::Selector::SimpleSelector::PseudoClass const& pseudo_class, DOM::Element const& element)
{
    switch (pseudo_class.type) {
        using enum CSS::Selector::SimpleSelector::PseudoClass::Type;
    case None:
        break;
    case Link:
        return element.is_link();
    case Visited:
        // FIXME: Maybe match this selector sometimes?
        return false;
    case Active:
        return element.is_active();
    case Hover:
        return matches_hover_pseudo_class(element);
    case Focus:
        return element.is_focused();
    case FirstChild:
        return !element.previous_element_sibling();
    case LastChild:
        return !element.next_element_sibling();
    case OnlyChild:
        return !(element.previous_element_sibling() || element.next_element_sibling());
    case Empty:
        return !(element.first_child_of_type<DOM::Element>() || element.first_child_of_type<DOM::Text>());
    case Root:
        return is<HTML::HTMLElement>(element);
    case FirstOfType:
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (sibling->tag_name() == element.tag_name())
                return false;
        }
        return true;
    case LastOfType:
        for (auto* sibling = element.next_element_sibling(); sibling; sibling = sibling->next_element_sibling()) {
            if (sibling->tag_name() == element.tag_name())
                return false;
        }
        return true;
    case Disabled:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (!element.has_attribute("disabled"))
            return false;
        return true;
    case Enabled:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (element.has_attribute("disabled"))
            return false;
        return true;
    case Checked:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (!element.has_attribute("checked"))
            return false;
        return true;
    case Not:
        for (auto& selector : pseudo_class.not_selector) {
            if (matches(selector, element))
                return false;
        }
        return true;
    case NthChild:
    case NthLastChild:
        auto const step_size = pseudo_class.nth_child_pattern.step_size;
        auto const offset = pseudo_class.nth_child_pattern.offset;
        if (step_size == 0 && offset == 0)
            return false; // "If both a and b are equal to zero, the pseudo-class represents no element in the document tree."

        auto const* parent = element.parent_element();
        if (!parent)
            return false;

        int index = 1;
        if (pseudo_class.type == NthChild) {
            for (auto* child = parent->first_child_of_type<DOM::Element>(); child && child != &element; child = child->next_element_sibling())
                ++index;
        } else {
            for (auto* child = parent->last_child_of_type<DOM::Element>(); child && child != &element; child = child->previous_element_sibling())
                ++index;
        }

        if (step_size < 0) {
            // When "step_size" is negative, selector represents first "offset" elements in document tree.
            return !(offset <= 0 || index > offset);
        } else if (step_size == 1) {
            // When "step_size == 1", selector represents last "offset" elements in document tree.
            return !(offset < 0 || index < offset);
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
        return true;
    }

    return false;
}

static bool matches(CSS::Selector::SimpleSelector const& component, DOM::Element const& element)
{
    switch (component.type) {
        using enum CSS::Selector::SimpleSelector::Type;
    case Universal:
        return true;
    case Id:
        return component.value == element.attribute(HTML::AttributeNames::id);
    case Class:
        return element.has_class(component.value);
    case TagName:
        return component.value == element.local_name();
    case Attribute:
        return matches_attribute(component.attribute, element);
    case PseudoClass:
        return matches_pseudo_class(component.pseudo_class, element);
    case PseudoElement:
        // FIXME: Implement pseudo-elements.
        return false;
    default:
        VERIFY_NOT_REACHED();
    }
}

static bool matches(CSS::Selector const& selector, int component_list_index, DOM::Element const& element)
{
    auto& relative_selector = selector.compound_selectors()[component_list_index];
    for (auto& simple_selector : relative_selector.simple_selectors) {
        if (!matches(simple_selector, element))
            return false;
    }
    switch (relative_selector.combinator) {
    case CSS::Selector::Combinator::None:
        return true;
    case CSS::Selector::Combinator::Descendant:
        VERIFY(component_list_index != 0);
        for (auto* ancestor = element.parent(); ancestor; ancestor = ancestor->parent()) {
            if (!is<DOM::Element>(*ancestor))
                continue;
            if (matches(selector, component_list_index - 1, verify_cast<DOM::Element>(*ancestor)))
                return true;
        }
        return false;
    case CSS::Selector::Combinator::ImmediateChild:
        VERIFY(component_list_index != 0);
        if (!element.parent() || !is<DOM::Element>(*element.parent()))
            return false;
        return matches(selector, component_list_index - 1, verify_cast<DOM::Element>(*element.parent()));
    case CSS::Selector::Combinator::NextSibling:
        VERIFY(component_list_index != 0);
        if (auto* sibling = element.previous_element_sibling())
            return matches(selector, component_list_index - 1, *sibling);
        return false;
    case CSS::Selector::Combinator::SubsequentSibling:
        VERIFY(component_list_index != 0);
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (matches(selector, component_list_index - 1, *sibling))
                return true;
        }
        return false;
    case CSS::Selector::Combinator::Column:
        TODO();
    }
    VERIFY_NOT_REACHED();
}

bool matches(CSS::Selector const& selector, DOM::Element const& element)
{
    VERIFY(!selector.compound_selectors().is_empty());
    return matches(selector, selector.compound_selectors().size() - 1, element);
}

}
