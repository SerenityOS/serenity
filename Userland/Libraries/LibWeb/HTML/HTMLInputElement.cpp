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
    : HTMLElement(document, move(qualified_name))
    , m_value(String::empty())
{
    set_prototype(&window().cached_web_prototype("HTMLInputElement"));

    activation_behavior = [this](auto&) {
        // The activation behavior for input elements are these steps:

        // FIXME: 1. If this element is not mutable and is not in the Checkbox state and is not in the Radio state, then return.

        // 2. Run this element's input activation behavior, if any, and do nothing otherwise.
        run_input_activation_behavior();
    };
}

HTMLInputElement::~HTMLInputElement() = default;

void HTMLInputElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_text_node.ptr());
    visitor.visit(m_legacy_pre_activation_behavior_checked_element_in_group.ptr());
}

RefPtr<Layout::Node> HTMLInputElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    if (type_state() == TypeAttributeState::Hidden)
        return nullptr;

    if (type_state() == TypeAttributeState::SubmitButton || type_state() == TypeAttributeState::Button || type_state() == TypeAttributeState::ResetButton)
        return adopt_ref(*new Layout::ButtonBox(document(), *this, move(style)));

    if (type_state() == TypeAttributeState::Checkbox)
        return adopt_ref(*new Layout::CheckBox(document(), *this, move(style)));

    if (type_state() == TypeAttributeState::RadioButton)
        return adopt_ref(*new Layout::RadioButton(document(), *this, move(style)));

    auto layout_node = adopt_ref(*new Layout::BlockContainer(document(), this, move(style)));
    layout_node->set_inline(true);
    return layout_node;
}

void HTMLInputElement::set_checked(bool checked, ChangeSource change_source)
{
    if (m_checked == checked)
        return;

    // The dirty checkedness flag must be initially set to false when the element is created,
    // and must be set to true whenever the user interacts with the control in a way that changes the checkedness.
    if (change_source == ChangeSource::User)
        m_dirty_checkedness = true;

    m_checked = checked;
    set_needs_style_update(true);
}

void HTMLInputElement::set_checked_binding(bool checked)
{
    if (type_state() == TypeAttributeState::RadioButton) {
        if (checked)
            set_checked_within_group();
        else
            set_checked(false, ChangeSource::Programmatic);
    } else {
        set_checked(checked, ChangeSource::Programmatic);
    }
}

// https://html.spec.whatwg.org/multipage/input.html#input-activation-behavior
void HTMLInputElement::run_input_activation_behavior()
{
    if (type_state() == TypeAttributeState::Checkbox || type_state() == TypeAttributeState::RadioButton) {
        // 1. If the element is not connected, then return.
        if (!is_connected())
            return;

        // 2. Fire an event named input at the element with the bubbles and composed attributes initialized to true.
        auto input_event = DOM::Event::create(document().window(), HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(*input_event);

        // 3. Fire an event named change at the element with the bubbles attribute initialized to true.
        auto change_event = DOM::Event::create(document().window(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(*change_event);
    } else if (type_state() == TypeAttributeState::SubmitButton) {
        JS::GCPtr<HTMLFormElement> form;
        // 1. If the element does not have a form owner, then return.
        if (!(form = this->form()))
            return;

        // 2. If the element's node document is not fully active, then return.
        if (!document().is_fully_active())
            return;

        // 3. Submit the form owner from the element.
        form->submit_form(this);
    } else {
        dispatch_event(*DOM::Event::create(document().window(), EventNames::change));
    }
}

void HTMLInputElement::did_edit_text_node(Badge<BrowsingContext>)
{
    // An input element's dirty value flag must be set to true whenever the user interacts with the control in a way that changes the value.
    m_value = value_sanitization_algorithm(m_text_node->data());
    m_dirty_value = true;

    // NOTE: This is a bit ad-hoc, but basically implements part of "4.10.5.5 Common event behaviors"
    //       https://html.spec.whatwg.org/multipage/input.html#common-input-element-events
    queue_an_element_task(HTML::Task::Source::UserInteraction, [this] {
        auto input_event = DOM::Event::create(document().window(), HTML::EventNames::input);
        input_event->set_bubbles(true);
        input_event->set_composed(true);
        dispatch_event(*input_event);

        // FIXME: This should only fire when the input is "committed", whatever that means.
        auto change_event = DOM::Event::create(document().window(), HTML::EventNames::change);
        change_event->set_bubbles(true);
        dispatch_event(*change_event);
    });
}

// https://html.spec.whatwg.org/multipage/input.html#dom-input-value-value
String HTMLInputElement::value() const
{
    // Return the current value of the element.
    return m_value;
}

// https://html.spec.whatwg.org/multipage/input.html#dom-input-value-value
void HTMLInputElement::set_value(String value)
{
    // 1. Let oldValue be the element's value.
    auto old_value = move(m_value);

    // 2. Set the element's value to the new value.
    // NOTE: This is done as part of step 4 below.

    // 3. Set the element's dirty value flag to true.
    m_dirty_value = true;

    // 4. Invoke the value sanitization algorithm, if the element's type attribute's current state defines one.
    m_value = value_sanitization_algorithm(move(value));

    // 5. If the element's value (after applying the value sanitization algorithm) is different from oldValue,
    //    and the element has a text entry cursor position, move the text entry cursor position to the end of the
    //    text control, unselecting any selected text and resetting the selection direction to "none".
    if (m_text_node && (m_value != old_value))
        m_text_node->set_data(m_value);
}

void HTMLInputElement::create_shadow_tree_if_needed()
{
    if (shadow_root())
        return;

    // FIXME: This could be better factored. Everything except the below types becomes a text input.
    switch (type_state()) {
    case TypeAttributeState::RadioButton:
    case TypeAttributeState::Checkbox:
    case TypeAttributeState::Button:
    case TypeAttributeState::SubmitButton:
    case TypeAttributeState::ResetButton:
    case TypeAttributeState::ImageButton:
        return;
    default:
        break;
    }

    auto* shadow_root = heap().allocate<DOM::ShadowRoot>(realm(), document(), *this);
    auto initial_value = m_value;
    if (initial_value.is_null())
        initial_value = String::empty();
    auto element = document().create_element(HTML::TagNames::div).release_value();
    element->set_attribute(HTML::AttributeNames::style, "white-space: pre; padding-top: 1px; padding-bottom: 1px; padding-left: 2px; padding-right: 2px");
    m_text_node = heap().allocate<DOM::Text>(realm(), document(), initial_value);
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

void HTMLInputElement::parse_attribute(FlyString const& name, String const& value)
{
    HTMLElement::parse_attribute(name, value);
    if (name == HTML::AttributeNames::checked) {
        // When the checked content attribute is added, if the control does not have dirty checkedness,
        // the user agent must set the checkedness of the element to true
        if (!m_dirty_checkedness)
            set_checked(true, ChangeSource::Programmatic);
    } else if (name == HTML::AttributeNames::type) {
        m_type = parse_type_attribute(value);
    } else if (name == HTML::AttributeNames::value) {
        if (!m_dirty_value)
            m_value = value_sanitization_algorithm(value);
    }
}

HTMLInputElement::TypeAttributeState HTMLInputElement::parse_type_attribute(StringView type)
{
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    if (type.equals_ignoring_case(#keyword##sv))              \
        return HTMLInputElement::TypeAttributeState::state;
    ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE

    // The missing value default and the invalid value default are the Text state.
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:missing-value-default
    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:invalid-value-default
    return HTMLInputElement::TypeAttributeState::Text;
}

void HTMLInputElement::did_remove_attribute(FlyString const& name)
{
    HTMLElement::did_remove_attribute(name);
    if (name == HTML::AttributeNames::checked) {
        // When the checked content attribute is removed, if the control does not have dirty checkedness,
        // the user agent must set the checkedness of the element to false.
        if (!m_dirty_checkedness)
            set_checked(false, ChangeSource::Programmatic);
    } else if (name == HTML::AttributeNames::value) {
        if (!m_dirty_value)
            m_value = String::empty();
    }
}

String HTMLInputElement::type() const
{
    switch (m_type) {
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(keyword, state) \
    case TypeAttributeState::state:                           \
        return #keyword##sv;
        ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE
    }

    VERIFY_NOT_REACHED();
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

void HTMLInputElement::form_associated_element_was_inserted()
{
    create_shadow_tree_if_needed();
}

void HTMLInputElement::set_checked_within_group()
{
    if (checked())
        return;

    set_checked(true, ChangeSource::User);
    String name = this->name();

    document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
        if (element.checked() && &element != this && element.name() == name)
            element.set_checked(false, ChangeSource::User);
        return IterationDecision::Continue;
    });
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-pre-activation-behavior
void HTMLInputElement::legacy_pre_activation_behavior()
{
    m_before_legacy_pre_activation_behavior_checked = checked();

    // 1. If this element's type attribute is in the Checkbox state, then set
    // this element's checkedness to its opposite value (i.e. true if it is
    // false, false if it is true) and set this element's indeterminate IDL
    // attribute to false.
    // FIXME: Set indeterminate to false when that exists.
    if (type_state() == TypeAttributeState::Checkbox) {
        set_checked(!checked(), ChangeSource::User);
    }

    // 2. If this element's type attribute is in the Radio Button state, then
    // get a reference to the element in this element's radio button group that
    // has its checkedness set to true, if any, and then set this element's
    // checkedness to true.
    if (type_state() == TypeAttributeState::RadioButton) {
        String name = this->name();

        document().for_each_in_inclusive_subtree_of_type<HTML::HTMLInputElement>([&](auto& element) {
            if (element.checked() && element.name() == name) {
                m_legacy_pre_activation_behavior_checked_element_in_group = &element;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });

        set_checked_within_group();
    }
}

// https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-canceled-activation-behavior
void HTMLInputElement::legacy_cancelled_activation_behavior()
{
    // 1. If the element's type attribute is in the Checkbox state, then set the
    // element's checkedness and the element's indeterminate IDL attribute back
    // to the values they had before the legacy-pre-activation behavior was run.
    if (type_state() == TypeAttributeState::Checkbox) {
        set_checked(m_before_legacy_pre_activation_behavior_checked, ChangeSource::Programmatic);
    }

    // 2. If this element 's type attribute is in the Radio Button state, then
    // if the element to which a reference was obtained in the
    // legacy-pre-activation behavior, if any, is still in what is now this
    // element' s radio button group, if it still has one, and if so, setting
    // that element 's checkedness to true; or else, if there was no such
    // element, or that element is no longer in this element' s radio button
    // group, or if this element no longer has a radio button group, setting
    // this element's checkedness to false.
    if (type_state() == TypeAttributeState::RadioButton) {
        String name = this->name();
        bool did_reselect_previous_element = false;
        if (m_legacy_pre_activation_behavior_checked_element_in_group) {
            auto& element_in_group = *m_legacy_pre_activation_behavior_checked_element_in_group;
            if (name == element_in_group.name()) {
                element_in_group.set_checked_within_group();
                did_reselect_previous_element = true;
            }

            m_legacy_pre_activation_behavior_checked_element_in_group = nullptr;
        }

        if (!did_reselect_previous_element)
            set_checked(false, ChangeSource::User);
    }
}

void HTMLInputElement::legacy_cancelled_activation_behavior_was_not_called()
{
    m_legacy_pre_activation_behavior_checked_element_in_group = nullptr;
}

}
