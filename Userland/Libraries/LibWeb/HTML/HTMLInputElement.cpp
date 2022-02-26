/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Adam Hodgen <ant1441@gmail.com>
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

HTMLInputElement::HTMLInputElement(DOM::Document& document, DOM::QualifiedName qualified_name)
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

    if (type() == "submit") {
        if (auto* form = first_ancestor_of_type<HTMLFormElement>()) {
            form->submit_form(this);
        }
        return;
    }
}

void HTMLInputElement::did_click_checkbox(Badge<Layout::CheckBox>)
{
    // FIXME: This should be a PointerEvent.
    auto click_event = DOM::Event::create(EventNames::click);
    click_event->set_bubbles(true);
    dispatch_event(move(click_event));
}

RefPtr<Layout::Node> HTMLInputElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (type() == "hidden")
        return nullptr;

    if (type() == "submit" || type() == "button" || type() == "reset")
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
    if (type() == "checkbox") {
        // 1. If the element is not connected, then return.
        if (!is_connected())
            return;

        // 2. Fire an event named input at the element with the bubbles and composed attributes initialized to true.
        auto input_event = DOM::Event::create(HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(move(input_event));

        // 3. Fire an event named change at the element with the bubbles attribute initialized to true.
        auto change_event = DOM::Event::create(HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(move(change_event));
    } else {
        dispatch_event(DOM::Event::create(EventNames::change));
    }
}

void HTMLInputElement::did_edit_text_node(Badge<BrowsingContext>)
{
    // NOTE: This is a bit ad-hoc, but basically implements part of "4.10.5.5 Common event behaviors"
    //       https://html.spec.whatwg.org/multipage/input.html#common-input-element-events
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto input_event = DOM::Event::create(HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(move(input_event));

        // FIXME: This should only fire when the input is "committed", whatever that means.
        auto change_event = DOM::Event::create(HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(move(change_event));
    });
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
    auto sanitised_value = value_sanitization_algorithm(move(value));

    if (m_text_node) {
        m_text_node->set_data(sanitised_value);
        return;
    }
    set_attribute(HTML::AttributeNames::value, sanitised_value);
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
    auto element = document().create_element(HTML::TagNames::div).release_value();
    element->set_attribute(HTML::AttributeNames::style, "white-space: pre; padding-top: 1px; padding-bottom: 1px; padding-left: 2px; padding-right: 2px");
    m_text_node = adopt_ref(*new DOM::Text(document(), initial_value));
    m_text_node->set_always_editable(true);
    m_text_node->set_owner_input_element({}, *this);
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

String HTMLInputElement::type() const
{
    auto value = attribute(HTML::AttributeNames::type);

#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, _) \
    if (value.equals_ignoring_case(#keyword))             \
        return #keyword;
    ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE

    // The missing value default and the invalid value default are the Text state.
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:missing-value-default
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:invalid-value-default
    return "text";
}

HTMLInputElement::TypeAttributeState HTMLInputElement::type_state() const
{
    auto value = attribute(HTML::AttributeNames::type);

#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    if (value.equals_ignoring_case(#keyword))                 \
        return HTMLInputElement::TypeAttributeState::state;
    ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE

    // The missing value default and the invalid value default are the Text state.
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:missing-value-default
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:invalid-value-default
    return HTMLInputElement::TypeAttributeState::Text;
}

void HTMLInputElement::set_type(String const& type)
{
    set_attribute(HTML::AttributeNames::type, type);
}

// https://html.spec.whatwg.org/multipage/input.html#value-sanitization-algorithm
String HTMLInputElement::value_sanitization_algorithm(String value) const
{
    if (type_state() == HTMLInputElement::TypeAttributeState::Text || type_state() == HTMLInputElement::TypeAttributeState::Search || type_state() == HTMLInputElement::TypeAttributeState::Telephone || type_state() == HTMLInputElement::TypeAttributeState::Password) {
        // Strip newlines from the value.
        if (value.contains('\r') || value.contains('\n')) {
            StringBuilder builder;
            for (auto c : value) {
                if (!(c == '\r' || c == '\n'))
                    builder.append(c);
            }
            return builder.to_string();
        }
    } else if (type_state() == HTMLInputElement::TypeAttributeState::URL) {
        // Strip newlines from the value, then strip leading and trailing ASCII whitespace from the value.
        if (value.contains('\r') || value.contains('\n')) {
            StringBuilder builder;
            for (auto c : value) {
                if (!(c == '\r' || c == '\n'))
                    builder.append(c);
            }
            return builder.to_string().trim_whitespace();
        }
    } else if (type_state() == HTMLInputElement::TypeAttributeState::Number) {
        // If the value of the element is not a valid floating-point number, then set it to the empty string instead.
        char* end_ptr;
        auto val = strtod(value.characters(), &end_ptr);
        if (!isfinite(val) || *end_ptr)
            return "";
    }

    // FIXME: Implement remaining value sanitation algorithms
    return value;
}

}
