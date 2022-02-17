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
#define ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTES                \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(hidden)           \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(text)             \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(search)           \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(tel)              \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(url)              \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(email)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(password)         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(date)             \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(month)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(week)             \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(time)             \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE("datetime-local") \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(number)           \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(range)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(color)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(checkbox)         \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(radio)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(file)             \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(submit)           \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(image)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(reset)            \
    __ENUMERATE_HTML_INPUT_TYPE_ATTRIBUTE(button)

class HTMLInputElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLInputElementWrapper;

    HTMLInputElement(DOM::Document&, QualifiedName);
    virtual ~HTMLInputElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    String type() const;
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

    RefPtr<DOM::Text> m_text_node;
    bool m_checked { false };

    // https://html.spec.whatwg.org/multipage/input.html#concept-input-checked-dirty-flag
    bool m_dirty_checkedness { false };
};

}
