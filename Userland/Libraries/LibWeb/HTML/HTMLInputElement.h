/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Adam Hodgen <ant1441@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

// https://html.spec.whatwg.org/multipage/input.html#attr-input-type
#define ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES                                  \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(hidden, Hidden)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(text, Text)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(search, Search)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(tel, Telephone)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(url, URL)                           \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(email, Email)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(password, Password)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(date, Date)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(month, Month)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(week, Week)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(time, Time)                         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE("datetime-local", LocalDateAndTime) \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(number, Number)                     \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(range, Range)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(color, Color)                       \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(checkbox, Checkbox)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(radio, RadioButton)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(file, FileUpload)                   \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(submit, SubmitButton)               \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(image, ImageButton)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(reset, ResetButton)                 \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(button, Button)

class HTMLInputElement final
    : public HTMLElement
    , public FormAssociatedElement {
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLInputElement)

public:
    using WrapperType = Bindings::HTMLInputElementWrapper;

    HTMLInputElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLInputElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    enum class TypeAttributeState {
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(_, state) state,
        ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE
    };

    String type() const;
    TypeAttributeState type_state() const { return m_type; }
    void set_type(String const&);

    String default_value() const { return attribute(HTML::AttributeNames::value); }
    String name() const { return attribute(HTML::AttributeNames::name); }

    String value() const;
    void set_value(String);

    bool checked() const { return m_checked; }
    enum class ChangeSource {
        Programmatic,
        User,
    };
    void set_checked(bool, ChangeSource = ChangeSource::Programmatic);

    bool checked_binding() const { return checked(); }
    void set_checked_binding(bool);

    void did_edit_text_node(Badge<BrowsingContext>);

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-input-element
    virtual bool is_focusable() const override { return m_type != TypeAttributeState::Hidden; }

    // ^HTMLElement
    virtual void parse_attribute(FlyString const&, String const&) override;
    virtual void did_remove_attribute(FlyString const&) override;

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-submit
    virtual bool is_submittable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-reset
    virtual bool is_resettable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    virtual void form_associated_element_was_inserted() override;

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return type_state() != TypeAttributeState::Hidden; }

private:
    // ^DOM::EventTarget
    virtual void did_receive_focus() override;
    virtual void legacy_pre_activation_behavior() override;
    virtual void legacy_cancelled_activation_behavior() override;
    virtual void legacy_cancelled_activation_behavior_was_not_called() override;

    static TypeAttributeState parse_type_attribute(StringView);
    void create_shadow_tree_if_needed();
    void run_input_activation_behavior();
    void set_checked_within_group();

    // https://html.spec.whatwg.org/multipage/input.html#value-sanitization-algorithm
    String value_sanitization_algorithm(String) const;

    RefPtr<DOM::Text> m_text_node;
    bool m_checked { false };

    // https://html.spec.whatwg.org/multipage/input.html#concept-input-checked-dirty-flag
    bool m_dirty_checkedness { false };

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fe-dirty
    bool m_dirty_value { false };

    // https://html.spec.whatwg.org/multipage/input.html#the-input-element:legacy-pre-activation-behavior
    bool m_before_legacy_pre_activation_behavior_checked { false };
    RefPtr<HTMLInputElement> m_legacy_pre_activation_behavior_checked_element_in_group;

    TypeAttributeState m_type { TypeAttributeState::Text };
    String m_value;
};

}
