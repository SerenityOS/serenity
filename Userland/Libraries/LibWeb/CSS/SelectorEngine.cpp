/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>

namespace Web::SelectorEngine {

// https://drafts.csswg.org/selectors-4/#the-lang-pseudo
static inline bool matches_lang_pseudo_class(DOM::Element const& element, Vector<FlyString> const& languages)
{
    FlyString element_language;
    for (auto const* e = &element; e; e = e->parent_element()) {
        auto lang = e->attribute(HTML::AttributeNames::lang);
        if (!lang.is_null()) {
            element_language = lang;
            break;
        }
    }
    if (element_language.is_null())
        return false;

    // FIXME: This is ad-hoc. Implement a proper language range matching algorithm as recommended by BCP47.
    for (auto const& language : languages) {
        if (language.is_empty())
            return false;
        if (language == "*"sv)
            return true;
        if (!element_language.view().contains('-'))
            return element_language.equals_ignoring_case(language);
        auto parts = element_language.view().split_view('-');
        return parts[0].equals_ignoring_case(language);
    }
    return false;
}

static inline bool matches_hover_pseudo_class(DOM::Element const& element)
{
    auto* hovered_node = element.document().hovered_node();
    if (!hovered_node)
        return false;
    if (&element == hovered_node)
        return true;
    return element.is_ancestor_of(*hovered_node);
}

// https://html.spec.whatwg.org/multipage/semantics-other.html#selector-checked
static inline bool matches_checked_pseudo_class(DOM::Element const& element)
{
    // The :checked pseudo-class must match any element falling into one of the following categories:
    // - input elements whose type attribute is in the Checkbox state and whose checkedness state is true
    // - input elements whose type attribute is in the Radio Button state and whose checkedness state is true
    if (is<HTML::HTMLInputElement>(element)) {
        auto const& input_element = static_cast<HTML::HTMLInputElement const&>(element);
        switch (input_element.type_state()) {
        case HTML::HTMLInputElement::TypeAttributeState::Checkbox:
        case HTML::HTMLInputElement::TypeAttributeState::RadioButton:
            return static_cast<HTML::HTMLInputElement const&>(element).checked();
        default:
            return false;
        }
    }

    // FIXME: - option elements whose selectedness is true

    return false;
}

static inline bool matches_attribute(CSS::Selector::SimpleSelector::Attribute const& attribute, DOM::Element const& element)
{
    if (attribute.match_type == CSS::Selector::SimpleSelector::Attribute::MatchType::HasAttribute) {
        // Early way out in case of an attribute existence selector.
        return element.has_attribute(attribute.name);
    }

    auto const case_insensitive_match = (attribute.case_type == CSS::Selector::SimpleSelector::Attribute::CaseType::CaseInsensitiveMatch);
    auto const case_sensitivity = case_insensitive_match
        ? CaseSensitivity::CaseInsensitive
        : CaseSensitivity::CaseSensitive;

    switch (attribute.match_type) {
    case CSS::Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch:
        return case_insensitive_match
            ? element.attribute(attribute.name).equals_ignoring_case(attribute.value)
            : element.attribute(attribute.name) == attribute.value;
    case CSS::Selector::SimpleSelector::Attribute::MatchType::ContainsWord: {
        if (attribute.value.is_empty()) {
            // This selector is always false is match value is empty.
            return false;
        }
        auto const view = element.attribute(attribute.name).split_view(' ');
        auto const size = view.size();
        for (size_t i = 0; i < size; ++i) {
            auto const value = view.at(i);
            if (case_insensitive_match
                    ? value.equals_ignoring_case(attribute.value)
                    : value == attribute.value) {
                return true;
            }
        }
        return false;
    }
    case CSS::Selector::SimpleSelector::Attribute::MatchType::ContainsString:
        return !attribute.value.is_empty()
            && element.attribute(attribute.name).contains(attribute.value, case_sensitivity);
    case CSS::Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment: {
        auto const element_attr_value = element.attribute(attribute.name);
        if (element_attr_value.is_empty()) {
            // If the attribute value on element is empty, the selector is true
            // if the match value is also empty and false otherwise.
            return attribute.value.is_empty();
        }
        if (attribute.value.is_empty()) {
            return false;
        }
        auto segments = element_attr_value.split_view('-');
        return case_insensitive_match
            ? segments.first().equals_ignoring_case(attribute.value)
            : segments.first() == attribute.value;
    }
    case CSS::Selector::SimpleSelector::Attribute::MatchType::StartsWithString:
        return !attribute.value.is_empty()
            && element.attribute(attribute.name).starts_with(attribute.value, case_sensitivity);
    case CSS::Selector::SimpleSelector::Attribute::MatchType::EndsWithString:
        return !attribute.value.is_empty()
            && element.attribute(attribute.name).ends_with(attribute.value, case_sensitivity);
    default:
        break;
    }

    return false;
}

static inline DOM::Element const* previous_sibling_with_same_tag_name(DOM::Element const& element)
{
    for (auto const* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
        if (sibling->tag_name() == element.tag_name())
            return sibling;
    }
    return nullptr;
}

static inline DOM::Element const* next_sibling_with_same_tag_name(DOM::Element const& element)
{
    for (auto const* sibling = element.next_element_sibling(); sibling; sibling = sibling->next_element_sibling()) {
        if (sibling->tag_name() == element.tag_name())
            return sibling;
    }
    return nullptr;
}

static inline bool matches_pseudo_class(CSS::Selector::SimpleSelector::PseudoClass const& pseudo_class, DOM::Element const& element)
{
    switch (pseudo_class.type) {
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Link:
        return element.is_link();
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Visited:
        // FIXME: Maybe match this selector sometimes?
        return false;
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Active:
        return element.is_active();
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Hover:
        return matches_hover_pseudo_class(element);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Focus:
        return element.is_focused();
    case CSS::Selector::SimpleSelector::PseudoClass::Type::FocusWithin: {
        auto* focused_element = element.document().focused_element();
        return focused_element && element.is_inclusive_ancestor_of(*focused_element);
    }
    case CSS::Selector::SimpleSelector::PseudoClass::Type::FirstChild:
        return !element.previous_element_sibling();
    case CSS::Selector::SimpleSelector::PseudoClass::Type::LastChild:
        return !element.next_element_sibling();
    case CSS::Selector::SimpleSelector::PseudoClass::Type::OnlyChild:
        return !(element.previous_element_sibling() || element.next_element_sibling());
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Empty: {
        if (!element.has_children())
            return true;
        if (element.first_child_of_type<DOM::Element>())
            return false;
        // NOTE: CSS Selectors level 4 changed ":empty" to also match whitespace-only text nodes.
        //       However, none of the major browser supports this yet, so let's just hang back until they do.
        bool has_nonempty_text_child = false;
        element.for_each_child_of_type<DOM::Text>([&](auto const& text_child) {
            if (!text_child.data().is_empty()) {
                has_nonempty_text_child = true;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return !has_nonempty_text_child;
    }
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Root:
        return is<HTML::HTMLHtmlElement>(element);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::FirstOfType:
        return !previous_sibling_with_same_tag_name(element);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::LastOfType:
        return !next_sibling_with_same_tag_name(element);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::OnlyOfType:
        return !previous_sibling_with_same_tag_name(element) && !next_sibling_with_same_tag_name(element);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Lang:
        return matches_lang_pseudo_class(element, pseudo_class.languages);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Disabled:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (!element.has_attribute("disabled"))
            return false;
        return true;
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Enabled:
        if (!element.tag_name().equals_ignoring_case(HTML::TagNames::input))
            return false;
        if (element.has_attribute("disabled"))
            return false;
        return true;
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Checked:
        return matches_checked_pseudo_class(element);
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Is:
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Where:
        for (auto& selector : pseudo_class.argument_selector_list) {
            if (matches(selector, element))
                return true;
        }
        return false;
    case CSS::Selector::SimpleSelector::PseudoClass::Type::Not:
        for (auto& selector : pseudo_class.argument_selector_list) {
            if (matches(selector, element))
                return false;
        }
        return true;
    case CSS::Selector::SimpleSelector::PseudoClass::Type::NthChild:
    case CSS::Selector::SimpleSelector::PseudoClass::Type::NthLastChild:
    case CSS::Selector::SimpleSelector::PseudoClass::Type::NthOfType:
    case CSS::Selector::SimpleSelector::PseudoClass::Type::NthLastOfType:
        auto const step_size = pseudo_class.nth_child_pattern.step_size;
        auto const offset = pseudo_class.nth_child_pattern.offset;
        if (step_size == 0 && offset == 0)
            return false; // "If both a and b are equal to zero, the pseudo-class represents no element in the document tree."

        auto const* parent = element.parent_element();
        if (!parent)
            return false;

        auto matches_selector_list = [](CSS::SelectorList const& list, DOM::Element const& element) {
            if (list.is_empty())
                return true;
            for (auto const& child_selector : list) {
                if (matches(child_selector, element)) {
                    return true;
                }
            }
            return false;
        };

        int index = 1;
        switch (pseudo_class.type) {
        case CSS::Selector::SimpleSelector::PseudoClass::Type::NthChild: {
            if (!matches_selector_list(pseudo_class.argument_selector_list, element))
                return false;
            for (auto* child = parent->first_child_of_type<DOM::Element>(); child && child != &element; child = child->next_element_sibling()) {
                if (matches_selector_list(pseudo_class.argument_selector_list, *child))
                    ++index;
            }
            break;
        }
        case CSS::Selector::SimpleSelector::PseudoClass::Type::NthLastChild: {
            if (!matches_selector_list(pseudo_class.argument_selector_list, element))
                return false;
            for (auto* child = parent->last_child_of_type<DOM::Element>(); child && child != &element; child = child->previous_element_sibling()) {
                if (matches_selector_list(pseudo_class.argument_selector_list, *child))
                    ++index;
            }
            break;
        }
        case CSS::Selector::SimpleSelector::PseudoClass::Type::NthOfType: {
            for (auto* child = previous_sibling_with_same_tag_name(element); child; child = previous_sibling_with_same_tag_name(*child))
                ++index;
            break;
        }
        case CSS::Selector::SimpleSelector::PseudoClass::Type::NthLastOfType: {
            for (auto* child = next_sibling_with_same_tag_name(element); child; child = next_sibling_with_same_tag_name(*child))
                ++index;
            break;
        }
        default:
            VERIFY_NOT_REACHED();
        }

        // When "step_size == -1", selector represents first "offset" elements in document tree.
        if (step_size == -1)
            return !(offset <= 0 || index > offset);

        // When "step_size == 1", selector represents last "offset" elements in document tree.
        if (step_size == 1)
            return !(offset < 0 || index < offset);

        // When "step_size == 0", selector picks only the "offset" element.
        if (step_size == 0)
            return index == offset;

        // If both are negative, nothing can match.
        if (step_size < 0 && offset < 0)
            return false;

        // Like "a % b", but handles negative integers correctly.
        auto const canonical_modulo = [](int a, int b) -> int {
            int c = a % b;
            if ((c < 0 && b > 0) || (c > 0 && b < 0)) {
                c += b;
            }
            return c;
        };

        // When "step_size < 0", we start at "offset" and count backwards.
        if (step_size < 0)
            return index <= offset && canonical_modulo(index - offset, -step_size) == 0;

        // Otherwise, we start at "offset" and count forwards.
        return index >= offset && canonical_modulo(index - offset, step_size) == 0;
    }

    return false;
}

static inline bool matches(CSS::Selector::SimpleSelector const& component, DOM::Element const& element)
{
    switch (component.type) {
    case CSS::Selector::SimpleSelector::Type::Universal:
        return true;
    case CSS::Selector::SimpleSelector::Type::Id:
        return component.name() == element.attribute(HTML::AttributeNames::id);
    case CSS::Selector::SimpleSelector::Type::Class:
        return element.has_class(component.name());
    case CSS::Selector::SimpleSelector::Type::TagName:
        return component.name() == element.local_name();
    case CSS::Selector::SimpleSelector::Type::Attribute:
        return matches_attribute(component.attribute(), element);
    case CSS::Selector::SimpleSelector::Type::PseudoClass:
        return matches_pseudo_class(component.pseudo_class(), element);
    case CSS::Selector::SimpleSelector::Type::PseudoElement:
        // Pseudo-element matching/not-matching is handled in the top level matches().
        return true;
    default:
        VERIFY_NOT_REACHED();
    }
}

static inline bool matches(CSS::Selector const& selector, int component_list_index, DOM::Element const& element)
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
            if (matches(selector, component_list_index - 1, static_cast<DOM::Element const&>(*ancestor)))
                return true;
        }
        return false;
    case CSS::Selector::Combinator::ImmediateChild:
        VERIFY(component_list_index != 0);
        if (!element.parent() || !is<DOM::Element>(*element.parent()))
            return false;
        return matches(selector, component_list_index - 1, static_cast<DOM::Element const&>(*element.parent()));
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

bool matches(CSS::Selector const& selector, DOM::Element const& element, Optional<CSS::Selector::PseudoElement> pseudo_element)
{
    VERIFY(!selector.compound_selectors().is_empty());
    if (pseudo_element.has_value() && selector.pseudo_element() != pseudo_element)
        return false;
    if (!pseudo_element.has_value() && selector.pseudo_element().has_value())
        return false;
    return matches(selector, selector.compound_selectors().size() - 1, element);
}

}
