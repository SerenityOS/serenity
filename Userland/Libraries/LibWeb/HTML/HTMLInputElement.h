/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLInputElement final : public FormAssociatedElement {
public:
    using WrapperType = Bindings::HTMLInputElementWrapper;

    HTMLInputElement(DOM::Document&, QualifiedName);
    virtual ~HTMLInputElement() override;

    virtual RefPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    String type() const { return attribute(HTML::AttributeNames::type); }
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
