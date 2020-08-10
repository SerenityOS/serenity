/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/AttributeNames.h>

namespace Web::SelectorEngine {

static bool matches_hover_pseudo_class(const DOM::Element& element)
{
    auto* hovered_node = element.document().hovered_node();
    if (!hovered_node)
        return false;
    if (&element == hovered_node)
        return true;
    return element.is_ancestor_of(*hovered_node);
}

static bool matches(const CSS::Selector::SimpleSelector& component, const DOM::Element& element)
{
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
    case CSS::Selector::SimpleSelector::PseudoClass::Hover:
        if (!matches_hover_pseudo_class(element))
            return false;
        break;
    case CSS::Selector::SimpleSelector::PseudoClass::Focus:
        // FIXME: Implement matches_focus_pseudo_class(element)
        return false;
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
        if (!element.is_html_element())
            return false;
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
    case CSS::Selector::SimpleSelector::AttributeMatchType::Contains:
        if (!element.attribute(component.attribute_name).split(' ').contains_slow(component.attribute_value))
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
        ASSERT_NOT_REACHED();
    }
}

static bool matches(const CSS::Selector& selector, int component_list_index, const DOM::Element& element)
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
        ASSERT(component_list_index != 0);
        for (auto* ancestor = element.parent(); ancestor; ancestor = ancestor->parent()) {
            if (!is<DOM::Element>(*ancestor))
                continue;
            if (matches(selector, component_list_index - 1, downcast<DOM::Element>(*ancestor)))
                return true;
        }
        return false;
    case CSS::Selector::ComplexSelector::Relation::ImmediateChild:
        ASSERT(component_list_index != 0);
        if (!element.parent() || !is<DOM::Element>(*element.parent()))
            return false;
        return matches(selector, component_list_index - 1, downcast<DOM::Element>(*element.parent()));
    case CSS::Selector::ComplexSelector::Relation::AdjacentSibling:
        ASSERT(component_list_index != 0);
        if (auto* sibling = element.previous_element_sibling())
            return matches(selector, component_list_index - 1, *sibling);
        return false;
    case CSS::Selector::ComplexSelector::Relation::GeneralSibling:
        ASSERT(component_list_index != 0);
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (matches(selector, component_list_index - 1, *sibling))
                return true;
        }
        return false;
    }
    ASSERT_NOT_REACHED();
}

bool matches(const CSS::Selector& selector, const DOM::Element& element)
{
    ASSERT(!selector.complex_selectors().is_empty());
    return matches(selector, selector.complex_selectors().size() - 1, element);
}

}
