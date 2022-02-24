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

class HTMLInputElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLInputElementWrapper;

    HTMLInputElement(DOM::Document&, DOM::QualifiedName);
    virtual ~HTMLInputElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    enum TypeAttributeState {
#define __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(_, state) state,
        ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES
#undef __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE
    };

    String type() const;
    TypeAttributeState type_state() const;
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
    enum class ShouldRunActivationBehavior {
        No,
        Yes,
    };
    void set_checked(bool, ChangeSource = ChangeSource::Programmatic, ShouldRunActivationBehavior = ShouldRunActivationBehavior::Yes);

    bool enabled() const;

    void did_click_button(Badge<Layout::ButtonBox>);
    void did_click_checkbox(Badge<Layout::CheckBox>);

    void did_edit_text_node(Badge<BrowsingContext>);

    virtual bool is_focusable() const override;

    virtual void parse_attribute(FlyString const&, String const&) override;
    virtual void did_remove_attribute(FlyString const&) override;

private:
    // ^DOM::Node
    virtual void inserted() override;
    virtual void removed_from(Node*) override;

    // ^DOM::EventTarget
    virtual void did_receive_focus() override;
    virtual void run_activation_behavior() override;

    void create_shadow_tree_if_needed();
    void run_input_activation_behavior();

    // https://html.spec.whatwg.org/multipage/input.html#value-sanitization-algorithm
    String value_sanitization_algorithm(String) const;

    RefPtr<DOM::Text> m_text_node;
    bool m_checked { false };

    // https://html.spec.whatwg.org/multipage/input.html#concept-input-checked-dirty-flag
    bool m_dirty_checkedness { false };
};

}
