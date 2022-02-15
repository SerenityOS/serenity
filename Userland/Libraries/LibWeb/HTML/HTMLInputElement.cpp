/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/Event.h>
#include <LibWeb/DOM/ShadowRoot.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/EventNames.h>
#include <LibWeb/HTML/HTMLFormElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/CheckBox.h>
#include <LibWeb/Layout/RadioButton.h>

namespace Web::HTML {

HTMLInputElement::HTMLInputElement(DOM::Document& document, QualifiedName qualified_name)
    : FormAssociatedElement(document, move(qualified_name))
{
}

HTMLInputElement::~HTMLInputElement()
{
}

void HTMLInputElement::did_click_button(Badge<Layout::ButtonBox>)
{
    // FIXME: This should be a PointerEvent.
    dispatch_event(DOM::Event::create(EventNames::click));

    if (type().equals_ignoring_case("submit")) {
        if (auto* form = first_ancestor_of_type<HTMLFormElement>()) {
            form->submit_form(this);
        }
        return;
    }
}

RefPtr<Layout::Node> HTMLInputElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (type() == "hidden")
        return nullptr;

    if (type().equals_ignoring_case("submit") || type().equals_ignoring_case("button"))
        return adopt_ref(*new Layout::ButtonBox(document(), *this, move(style)));

    if (type() == "checkbox")
        return adopt_ref(*new Layout::CheckBox(document(), *this, move(style)));

    if (type() == "radio")
        return adopt_ref(*new Layout::RadioButton(document(), *this, move(style)));

    create_shadow_tree_if_needed();
    auto layout_node = adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));
    layout_node->set_inline(true);
    return layout_node;
}

void HTMLInputElement::set_checked(bool checked, ChangeSource change_source, ShouldRunActivationBehavior should_run_activation_behavior)
{
    if (m_checked == checked)
        return;

    // The dirty checkedness flag must be initially set to false when the element is created,
    // and must be set to true whenever the user interacts with the control in a way that changes the checkedness.
    if (change_source == ChangeSource::User)
        m_dirty_checkedness = true;

    m_checked = checked;
    if (layout_node())
        layout_node()->set_needs_display();

    if (should_run_activation_behavior == ShouldRunActivationBehavior::Yes)
        run_activation_behavior();
}

void HTMLInputElement::run_activation_behavior()
{
    // The activation behavior for input elements are these steps:

    // FIXME: 1. If this element is not mutable and is not in the Checkbox state and is not in the Radio state, then return.

    // 2. Run this element's input activation behavior, if any, and do nothing otherwise.
    run_input_activation_behavior();
}

// https://html.spec.whatwg.org/multipage/input.html#input-activation-behavior
void HTMLInputElement::run_input_activation_behavior()
{
    dispatch_event(DOM::Event::create(EventNames::change));
}

bool HTMLInputElement::enabled() const
{
    return !has_attribute(HTML::AttributeNames::disabled);
}

String HTMLInputElement::value() const
{
    if (m_text_node)
        return m_text_node->data();
    return default_value();
}

void HTMLInputElement::set_value(String value)
{
    if (m_text_node) {
        m_text_node->set_data(move(value));
        return;
    }
    set_attribute(HTML::AttributeNames::value, move(value));
}

void HTMLInputElement::create_shadow_tree_if_needed()
{
    if (shadow_root())
        return;

    // FIXME: This assumes that we want a text box. Is that always true?
    auto shadow_root = adopt_ref(*new DOM::ShadowRoot(document(), *this));
    auto initial_value = attribute(HTML::AttributeNames::value);
    if (initial_value.is_null())
        initial_value = String::empty();
    auto element = document().create_element(HTML::TagNames::div);
    element->set_attribute(HTML::AttributeNames::style, "white-space: pre");
    m_text_node = adopt_ref(*new DOM::Text(document(), initial_value));
    m_text_node->set_always_editable(true);
    element->append_child(*m_text_node);
    shadow_root->append_child(move(element));
    set_shadow_root(move(shadow_root));
}

void HTMLInputElement::did_receive_focus()
{
    auto* browsing_context = document().browsing_context();
    if (!browsing_context)
        return;
    if (!m_text_node)
        return;
    browsing_context->set_cursor_position(DOM::Position { *m_text_node, 0 });
}

bool HTMLInputElement::is_focusable() const
{
    return m_text_node;
}

void HTMLInputElement::inserted()
{
    HTMLElement::inserted();
    set_form(first_ancestor_of_type<HTMLFormElement>());
}

void HTMLInputElement::removed_from(DOM::Node* old_parent)
{
    HTMLElement::removed_from(old_parent);
    set_form(nullptr);
}

void HTMLInputElement::parse_attribute(FlyString const& name, String const& value)
{
    FormAssociatedElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::checked) {
        // When the checked content attribute is added, if the control does not have dirty checkedness,
        // the user agent must set the checkedness of the element to true
        if (!m_dirty_checkedness)
            set_checked(true, ChangeSource::Programmatic, ShouldRunActivationBehavior::No);
    }
}

void HTMLInputElement::did_remove_attribute(FlyString const& name)
{
    FormAssociatedElement::did_remove_attribute(name);
    if (name == HTML::AttributeNames::checked) {
        // When the checked content attribute is removed, if the control does not have dirty checkedness,
        // the user agent must set the checkedness of the element to false.
        if (!m_dirty_checkedness)
            set_checked(false, ChangeSource::Programmatic, ShouldRunActivationBehavior::No);
    }
}

}
