/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringBuilder.h>
#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/HTMLOptGroupElement.h>
#include <LibWeb/HTML/HTMLOptionElement.h>
#include <LibWeb/HTML/HTMLScriptElement.h>
#include <LibWeb/HTML/HTMLSelectElement.h>
#include <LibWeb/Infra/Strings.h>

namespace Web::HTML {

HTMLOptionElement::HTMLOptionElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
}

HTMLOptionElement::~HTMLOptionElement() = default;

void HTMLOptionElement::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::HTMLOptionElementPrototype>(realm, "HTMLOptionElement"));
}

void HTMLOptionElement::attribute_changed(DeprecatedFlyString const& name, DeprecatedString const& value)
{
    HTMLElement::attribute_changed(name, value);

    if (name == HTML::AttributeNames::selected) {
        if (value.is_null()) {
            // Whenever an option element's selected attribute is removed, if its dirtiness is false, its selectedness must be set to false.
            if (!m_dirty)
                m_selected = false;
        } else {
            // Except where otherwise specified, when the element is created, its selectedness must be set to true
            // if the element has a selected attribute. Whenever an option element's selected attribute is added,
            // if its dirtiness is false, its selectedness must be set to true.
            if (!m_dirty)
                m_selected = true;
        }
    }
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option-selected
void HTMLOptionElement::set_selected(bool selected)
{
    // On setting, it must set the element's selectedness to the new value, set its dirtiness to true, and then cause the element to ask for a reset.
    m_selected = selected;
    m_dirty = true;
    ask_for_a_reset();
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option-value
String HTMLOptionElement::value() const
{
    // The value of an option element is the value of the value content attribute, if there is one.
    // ...or, if there is not, the value of the element's text IDL attribute.
    return attribute(HTML::AttributeNames::value).value_or(text());
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option-value
WebIDL::ExceptionOr<void> HTMLOptionElement::set_value(String const& value)
{
    return set_attribute(HTML::AttributeNames::value, value);
}

static void concatenate_descendants_text_content(DOM::Node const* node, StringBuilder& builder)
{
    // FIXME: SVGScriptElement should also be skipped, but it doesn't exist yet.
    if (is<HTMLScriptElement>(node))
        return;
    if (is<DOM::Text>(node))
        builder.append(verify_cast<DOM::Text>(node)->data());
    node->for_each_child([&](auto const& node) {
        concatenate_descendants_text_content(&node, builder);
    });
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option-text
String HTMLOptionElement::text() const
{
    StringBuilder builder;

    // Concatenation of data of all the Text node descendants of the option element, in tree order,
    // excluding any that are descendants of descendants of the option element that are themselves
    // script or SVG script elements.
    for_each_child([&](auto const& node) {
        concatenate_descendants_text_content(&node, builder);
    });

    // Return the result of stripping and collapsing ASCII whitespace from the above concatenation.
    return MUST(Infra::strip_and_collapse_whitespace(builder.string_view()));
}

// https://html.spec.whatwg.org/multipage/form-elements.html#dom-option-text
void HTMLOptionElement::set_text(String const& text)
{
    string_replace_all(text.to_deprecated_string());
}

// https://html.spec.whatwg.org/multipage/form-elements.html#concept-option-index
int HTMLOptionElement::index() const
{
    // An option element's index is the number of option elements that are in the same list of options but that come before it in tree order.
    if (auto select_element = first_ancestor_of_type<HTMLSelectElement>()) {
        int index = 0;
        for (auto const& option_element : select_element->list_of_options()) {
            if (option_element.ptr() == this)
                return index;
            ++index;
        }
    }

    // If the option element is not in a list of options, then the option element's index is zero.
    return 0;
}

// https://html.spec.whatwg.org/multipage/form-elements.html#ask-for-a-reset
void HTMLOptionElement::ask_for_a_reset()
{
    // FIXME: Implement this operation.
}

// https://html.spec.whatwg.org/multipage/form-elements.html#concept-option-disabled
bool HTMLOptionElement::disabled() const
{
    // An option element is disabled if its disabled attribute is present or if it is a child of an optgroup element whose disabled attribute is present.
    return has_attribute(AttributeNames::disabled)
        || (parent() && is<HTMLOptGroupElement>(parent()) && static_cast<HTMLOptGroupElement const&>(*parent()).has_attribute(AttributeNames::disabled));
}

Optional<ARIA::Role> HTMLOptionElement::default_role() const
{
    // https://www.w3.org/TR/html-aria/#el-option
    // TODO: Only an option element that is in a list of options or that represents a suggestion in a datalist should return option
    return ARIA::Role::option;
}

}
