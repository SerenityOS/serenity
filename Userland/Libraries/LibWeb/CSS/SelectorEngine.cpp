/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/CSS/ValueID.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Element.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/AttributeNames.h>
#include <LibWeb/HTML/HTMLAnchorElement.h>
#include <LibWeb/HTML/HTMLAreaElement.h>
#include <LibWeb/HTML/HTMLButtonElement.h>
#include <LibWeb/HTML/HTMLFieldSetElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/HTML/HTMLMediaElement.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLProgressElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/HTML/HTMLTextAreaElement.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::SelectorEngine {

// https://drafts.csswg.org/selectors-4/#the-lang-pseudo
static inline bool matches_lang_pseudo_class(DOM::Element const& element, Vector<FlyString> const& languages)
{
    FlyString element_language;
    for (auto const* e = &element; e; e = e->parent_element()) {
        auto lang = e->attribute(HTML::AttributeNames::lang);
        if (!lang.is_null()) {
            element_language = FlyString::from_deprecated_fly_string(lang).release_value_but_fixme_should_propagate_errors();
            break;
        }
    }
    if (element_language.is_empty())
        return false;

    // FIXME: This is ad-hoc. Implement a proper language range matching algorithm as recommended by BCP47.
    for (auto const& language : languages) {
        if (language.is_empty())
            continue;
        if (language == "*"sv)
            return true;
        if (!element_language.to_string().contains('-') && Infra::is_ascii_case_insensitive_match(element_language, language))
            return true;
        auto parts = element_language.to_string().split_limit('-', 2).release_value_but_fixme_should_propagate_errors();
        if (Infra::is_ascii_case_insensitive_match(parts[0], language))
            return true;
    }
    return false;
}

// https://html.spec.whatwg.org/multipage/semantics-other.html#selector-link
static inline bool matches_link_pseudo_class(DOM::Element const& element)
{
    // All a elements that have an href attribute, and all area elements that have an href attribute, must match one of :link and :visited.
    if (!is<HTML::HTMLAnchorElement>(element) && !is<HTML::HTMLAreaElement>(element))
        return false;
    return element.has_attribute(HTML::AttributeNames::href);
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

    // - option elements whose selectedness is true
    if (is<HTML::HTMLOptionElement>(element)) {
        return static_cast<HTML::HTMLOptionElement const&>(element).selected();
    }
    return false;
}

// https://html.spec.whatwg.org/multipage/semantics-other.html#selector-indeterminate
static inline bool matches_indeterminate_pseudo_class(DOM::Element const& element)
{
    // The :indeterminate pseudo-class must match any element falling into one of the following categories:
    // - input elements whose type attribute is in the Checkbox state and whose indeterminate IDL attribute is set to true
    // FIXME: - input elements whose type attribute is in the Radio Button state and whose radio button group contains no input elements whose checkedness state is true.
    if (is<HTML::HTMLInputElement>(element)) {
        auto const& input_element = static_cast<HTML::HTMLInputElement const&>(element);
        switch (input_element.type_state()) {
        case HTML::HTMLInputElement::TypeAttributeState::Checkbox:
            return input_element.indeterminate();
        default:
            return false;
        }
    }
    // - progress elements with no value content attribute
    if (is<HTML::HTMLProgressElement>(element)) {
        return !element.has_attribute(HTML::AttributeNames::value);
    }
    return false;
}

static inline bool matches_attribute(CSS::Selector::SimpleSelector::Attribute const& attribute, [[maybe_unused]] Optional<CSS::CSSStyleSheet const&> style_sheet_for_rule, DOM::Element const& element)
{
    // FIXME: Check the attribute's namespace, once we support that in DOM::Element!

    auto attribute_name = attribute.qualified_name.name.name.to_deprecated_fly_string();

    if (attribute.match_type == CSS::Selector::SimpleSelector::Attribute::MatchType::HasAttribute) {
        // Early way out in case of an attribute existence selector.
        return element.has_attribute(attribute_name);
    }

    auto const case_insensitive_match = (attribute.case_type == CSS::Selector::SimpleSelector::Attribute::CaseType::CaseInsensitiveMatch);
    auto const case_sensitivity = case_insensitive_match
        ? CaseSensitivity::CaseInsensitive
        : CaseSensitivity::CaseSensitive;

    switch (attribute.match_type) {
    case CSS::Selector::SimpleSelector::Attribute::MatchType::ExactValueMatch:
        return case_insensitive_match
            ? Infra::is_ascii_case_insensitive_match(element.attribute(attribute_name), attribute.value)
            : element.attribute(attribute_name) == attribute.value.to_deprecated_string();
    case CSS::Selector::SimpleSelector::Attribute::MatchType::ContainsWord: {
        if (attribute.value.is_empty()) {
            // This selector is always false is match value is empty.
            return false;
        }
        auto const view = element.attribute(attribute_name).split_view(' ');
        auto const size = view.size();
        for (size_t i = 0; i < size; ++i) {
            auto const value = view.at(i);
            if (case_insensitive_match
                    ? Infra::is_ascii_case_insensitive_match(value, attribute.value)
                    : value == attribute.value) {
                return true;
            }
        }
        return false;
    }
    case CSS::Selector::SimpleSelector::Attribute::MatchType::ContainsString:
        return !attribute.value.is_empty()
            && element.attribute(attribute_name).contains(attribute.value, case_sensitivity);
    case CSS::Selector::SimpleSelector::Attribute::MatchType::StartsWithSegment: {
        auto const element_attr_value = element.attribute(attribute_name);
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
            ? Infra::is_ascii_case_insensitive_match(segments.first(), attribute.value)
            : segments.first() == attribute.value;
    }
    case CSS::Selector::SimpleSelector::Attribute::MatchType::StartsWithString:
        return !attribute.value.is_empty()
            && element.attribute(attribute_name).starts_with(attribute.value, case_sensitivity);
    case CSS::Selector::SimpleSelector::Attribute::MatchType::EndsWithString:
        return !attribute.value.is_empty()
            && element.attribute(attribute_name).ends_with(attribute.value, case_sensitivity);
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

static inline bool matches_pseudo_class(CSS::Selector::SimpleSelector::PseudoClassSelector const& pseudo_class, Optional<CSS::CSSStyleSheet const&> style_sheet_for_rule, DOM::Element const& element, JS::GCPtr<DOM::ParentNode const> scope)
{
    switch (pseudo_class.type) {
    case CSS::PseudoClass::Link:
    case CSS::PseudoClass::AnyLink:
        // NOTE: AnyLink should match whether the link is visited or not, so if we ever start matching
        //       :visited, we'll need to handle these differently.
        return matches_link_pseudo_class(element);
    case CSS::PseudoClass::LocalLink: {
        // The :local-link pseudo-class allows authors to style hyperlinks based on the users current location
        // within a site. It represents an element that is the source anchor of a hyperlink whose target’s
        // absolute URL matches the element’s own document URL. If the hyperlink’s target includes a fragment
        // URL, then the fragment URL of the current URL must also match; if it does not, then the fragment
        // URL portion of the current URL is not taken into account in the comparison.
        if (!matches_link_pseudo_class(element))
            return false;
        auto document_url = element.document().url();
        AK::URL target_url = element.document().parse_url(element.attribute(HTML::AttributeNames::href));
        if (target_url.fragment().has_value())
            return document_url.equals(target_url, AK::URL::ExcludeFragment::No);
        return document_url.equals(target_url, AK::URL::ExcludeFragment::Yes);
    }
    case CSS::PseudoClass::Visited:
        // FIXME: Maybe match this selector sometimes?
        return false;
    case CSS::PseudoClass::Active:
        return element.is_active();
    case CSS::PseudoClass::Hover:
        return matches_hover_pseudo_class(element);
    case CSS::PseudoClass::Focus:
        return element.is_focused();
    case CSS::PseudoClass::FocusVisible:
        // FIXME: We should only apply this when a visible focus is useful. Decide when that is!
        return element.is_focused();
    case CSS::PseudoClass::FocusWithin: {
        auto* focused_element = element.document().focused_element();
        return focused_element && element.is_inclusive_ancestor_of(*focused_element);
    }
    case CSS::PseudoClass::FirstChild:
        return !element.previous_element_sibling();
    case CSS::PseudoClass::LastChild:
        return !element.next_element_sibling();
    case CSS::PseudoClass::OnlyChild:
        return !(element.previous_element_sibling() || element.next_element_sibling());
    case CSS::PseudoClass::Empty: {
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
    case CSS::PseudoClass::Root:
        return is<HTML::HTMLHtmlElement>(element);
    case CSS::PseudoClass::Host:
        // FIXME: Implement :host selector.
        return false;
    case CSS::PseudoClass::Scope:
        return scope ? &element == scope : is<HTML::HTMLHtmlElement>(element);
    case CSS::PseudoClass::FirstOfType:
        return !previous_sibling_with_same_tag_name(element);
    case CSS::PseudoClass::LastOfType:
        return !next_sibling_with_same_tag_name(element);
    case CSS::PseudoClass::OnlyOfType:
        return !previous_sibling_with_same_tag_name(element) && !next_sibling_with_same_tag_name(element);
    case CSS::PseudoClass::Lang:
        return matches_lang_pseudo_class(element, pseudo_class.languages);
    case CSS::PseudoClass::Disabled:
        // https://html.spec.whatwg.org/multipage/semantics-other.html#selector-disabled
        // The :disabled pseudo-class must match any element that is actually disabled.
        return element.is_actually_disabled();
    case CSS::PseudoClass::Enabled:
        // https://html.spec.whatwg.org/multipage/semantics-other.html#selector-enabled
        // The :enabled pseudo-class must match any button, input, select, textarea, optgroup, option, fieldset element, or form-associated custom element that is not actually disabled.
        return (is<HTML::HTMLButtonElement>(element) || is<HTML::HTMLInputElement>(element) || is<HTML::HTMLSelectElement>(element) || is<HTML::HTMLTextAreaElement>(element) || is<HTML::HTMLOptGroupElement>(element) || is<HTML::HTMLOptionElement>(element) || is<HTML::HTMLFieldSetElement>(element))
            && !element.is_actually_disabled();
    case CSS::PseudoClass::Checked:
        return matches_checked_pseudo_class(element);
    case CSS::PseudoClass::Indeterminate:
        return matches_indeterminate_pseudo_class(element);
    case CSS::PseudoClass::Defined:
        return element.is_defined();
    case CSS::PseudoClass::Is:
    case CSS::PseudoClass::Where:
        for (auto& selector : pseudo_class.argument_selector_list) {
            if (matches(selector, style_sheet_for_rule, element))
                return true;
        }
        return false;
    case CSS::PseudoClass::Not:
        for (auto& selector : pseudo_class.argument_selector_list) {
            if (matches(selector, style_sheet_for_rule, element))
                return false;
        }
        return true;
    case CSS::PseudoClass::NthChild:
    case CSS::PseudoClass::NthLastChild:
    case CSS::PseudoClass::NthOfType:
    case CSS::PseudoClass::NthLastOfType: {
        auto const step_size = pseudo_class.nth_child_pattern.step_size;
        auto const offset = pseudo_class.nth_child_pattern.offset;
        if (step_size == 0 && offset == 0)
            return false; // "If both a and b are equal to zero, the pseudo-class represents no element in the document tree."

        auto const* parent = element.parent_element();
        if (!parent)
            return false;

        auto matches_selector_list = [&style_sheet_for_rule](CSS::SelectorList const& list, DOM::Element const& element) {
            if (list.is_empty())
                return true;
            for (auto const& child_selector : list) {
                if (matches(child_selector, style_sheet_for_rule, element)) {
                    return true;
                }
            }
            return false;
        };

        int index = 1;
        switch (pseudo_class.type) {
        case CSS::PseudoClass::NthChild: {
            if (!matches_selector_list(pseudo_class.argument_selector_list, element))
                return false;
            for (auto* child = parent->first_child_of_type<DOM::Element>(); child && child != &element; child = child->next_element_sibling()) {
                if (matches_selector_list(pseudo_class.argument_selector_list, *child))
                    ++index;
            }
            break;
        }
        case CSS::PseudoClass::NthLastChild: {
            if (!matches_selector_list(pseudo_class.argument_selector_list, element))
                return false;
            for (auto* child = parent->last_child_of_type<DOM::Element>(); child && child != &element; child = child->previous_element_sibling()) {
                if (matches_selector_list(pseudo_class.argument_selector_list, *child))
                    ++index;
            }
            break;
        }
        case CSS::PseudoClass::NthOfType: {
            for (auto* child = previous_sibling_with_same_tag_name(element); child; child = previous_sibling_with_same_tag_name(*child))
                ++index;
            break;
        }
        case CSS::PseudoClass::NthLastOfType: {
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
    case CSS::PseudoClass::Playing: {
        if (!is<HTML::HTMLMediaElement>(element))
            return false;
        auto const& media_element = static_cast<HTML::HTMLMediaElement const&>(element);
        return !media_element.paused();
    }
    case CSS::PseudoClass::Paused: {
        if (!is<HTML::HTMLMediaElement>(element))
            return false;
        auto const& media_element = static_cast<HTML::HTMLMediaElement const&>(element);
        return media_element.paused();
    }
    case CSS::PseudoClass::Seeking: {
        if (!is<HTML::HTMLMediaElement>(element))
            return false;
        auto const& media_element = static_cast<HTML::HTMLMediaElement const&>(element);
        return media_element.seeking();
    }
    case CSS::PseudoClass::Muted: {
        if (!is<HTML::HTMLMediaElement>(element))
            return false;
        auto const& media_element = static_cast<HTML::HTMLMediaElement const&>(element);
        return media_element.muted();
    }
    case CSS::PseudoClass::VolumeLocked: {
        // FIXME: Currently we don't allow the user to specify an override volume, so this is always false.
        //        Once we do, implement this!
        return false;
    }
    case CSS::PseudoClass::Buffering: {
        if (!is<HTML::HTMLMediaElement>(element))
            return false;
        auto const& media_element = static_cast<HTML::HTMLMediaElement const&>(element);
        return media_element.blocked();
    }
    case CSS::PseudoClass::Stalled: {
        if (!is<HTML::HTMLMediaElement>(element))
            return false;
        auto const& media_element = static_cast<HTML::HTMLMediaElement const&>(element);
        return media_element.stalled();
    }
    case CSS::PseudoClass::Target:
        return element.is_target();
    case CSS::PseudoClass::Dir: {
        // "Values other than ltr and rtl are not invalid, but do not match anything."
        // - https://www.w3.org/TR/selectors-4/#the-dir-pseudo
        if (!first_is_one_of(pseudo_class.identifier, CSS::ValueID::Ltr, CSS::ValueID::Rtl))
            return false;
        switch (element.directionality()) {
        case DOM::Element::Directionality::Ltr:
            return pseudo_class.identifier == CSS::ValueID::Ltr;
        case DOM::Element::Directionality::Rtl:
            return pseudo_class.identifier == CSS::ValueID::Rtl;
        }
        VERIFY_NOT_REACHED();
    }
    }

    return false;
}

static inline bool matches(CSS::Selector::SimpleSelector const& component, Optional<CSS::CSSStyleSheet const&> style_sheet_for_rule, DOM::Element const& element, JS::GCPtr<DOM::ParentNode const> scope)
{
    switch (component.type) {
    case CSS::Selector::SimpleSelector::Type::Universal:
    case CSS::Selector::SimpleSelector::Type::TagName: {
        auto qualified_name = component.qualified_name();

        // Reject if the tag name doesn't match
        if (component.type == CSS::Selector::SimpleSelector::Type::TagName) {
            // See https://html.spec.whatwg.org/multipage/semantics-other.html#case-sensitivity-of-selectors
            if (element.document().document_type() == DOM::Document::Type::HTML) {
                if (qualified_name.name.lowercase_name != element.local_name().view())
                    return false;
            } else if (!Infra::is_ascii_case_insensitive_match(qualified_name.name.name, element.local_name())) {
                return false;
            }
        }

        // Match the namespace
        switch (qualified_name.namespace_type) {
        case CSS::Selector::SimpleSelector::QualifiedName::NamespaceType::Default:
            // "if no default namespace has been declared for selectors, this is equivalent to *|E."
            if (!style_sheet_for_rule.has_value() || !style_sheet_for_rule->default_namespace().has_value())
                return true;
            // "Otherwise it is equivalent to ns|E where ns is the default namespace."
            return element.namespace_() == style_sheet_for_rule->default_namespace();
        case CSS::Selector::SimpleSelector::QualifiedName::NamespaceType::None:
            // "elements with name E without a namespace"
            return element.namespace_().is_empty();
        case CSS::Selector::SimpleSelector::QualifiedName::NamespaceType::Any:
            // "elements with name E in any namespace, including those without a namespace"
            return true;
        case CSS::Selector::SimpleSelector::QualifiedName::NamespaceType::Named:
            // "elements with name E in namespace ns"
            // Unrecognized namespace prefixes are invalid, so don't match.
            // (We can't detect this at parse time, since a namespace rule may be inserted later.)
            // So, if we don't have a context to look up namespaces from, we fail to match.
            if (!style_sheet_for_rule.has_value())
                return false;

            auto selector_namespace = style_sheet_for_rule->namespace_uri(qualified_name.namespace_);
            return selector_namespace.has_value() && selector_namespace.value() == element.namespace_();
        }
        VERIFY_NOT_REACHED();
    }
    case CSS::Selector::SimpleSelector::Type::Id:
        return component.name() == element.attribute(HTML::AttributeNames::id).view();
    case CSS::Selector::SimpleSelector::Type::Class:
        return element.has_class(component.name());
    case CSS::Selector::SimpleSelector::Type::Attribute:
        return matches_attribute(component.attribute(), style_sheet_for_rule, element);
    case CSS::Selector::SimpleSelector::Type::PseudoClass:
        return matches_pseudo_class(component.pseudo_class(), style_sheet_for_rule, element, scope);
    case CSS::Selector::SimpleSelector::Type::PseudoElement:
        // Pseudo-element matching/not-matching is handled in the top level matches().
        return true;
    default:
        VERIFY_NOT_REACHED();
    }
}

static inline bool matches(CSS::Selector const& selector, Optional<CSS::CSSStyleSheet const&> style_sheet_for_rule, int component_list_index, DOM::Element const& element, JS::GCPtr<DOM::ParentNode const> scope)
{
    auto& relative_selector = selector.compound_selectors()[component_list_index];
    for (auto& simple_selector : relative_selector.simple_selectors) {
        if (!matches(simple_selector, style_sheet_for_rule, element, scope))
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
            if (matches(selector, style_sheet_for_rule, component_list_index - 1, static_cast<DOM::Element const&>(*ancestor), scope))
                return true;
        }
        return false;
    case CSS::Selector::Combinator::ImmediateChild:
        VERIFY(component_list_index != 0);
        if (!element.parent() || !is<DOM::Element>(*element.parent()))
            return false;
        return matches(selector, style_sheet_for_rule, component_list_index - 1, static_cast<DOM::Element const&>(*element.parent()), scope);
    case CSS::Selector::Combinator::NextSibling:
        VERIFY(component_list_index != 0);
        if (auto* sibling = element.previous_element_sibling())
            return matches(selector, style_sheet_for_rule, component_list_index - 1, *sibling, scope);
        return false;
    case CSS::Selector::Combinator::SubsequentSibling:
        VERIFY(component_list_index != 0);
        for (auto* sibling = element.previous_element_sibling(); sibling; sibling = sibling->previous_element_sibling()) {
            if (matches(selector, style_sheet_for_rule, component_list_index - 1, *sibling, scope))
                return true;
        }
        return false;
    case CSS::Selector::Combinator::Column:
        TODO();
    }
    VERIFY_NOT_REACHED();
}

bool matches(CSS::Selector const& selector, Optional<CSS::CSSStyleSheet const&> style_sheet_for_rule, DOM::Element const& element, Optional<CSS::Selector::PseudoElement> pseudo_element, JS::GCPtr<DOM::ParentNode const> scope)
{
    VERIFY(!selector.compound_selectors().is_empty());
    if (pseudo_element.has_value() && selector.pseudo_element() != pseudo_element)
        return false;
    if (!pseudo_element.has_value() && selector.pseudo_element().has_value())
        return false;
    return matches(selector, style_sheet_for_rule, selector.compound_selectors().size() - 1, element, scope);
}

}
