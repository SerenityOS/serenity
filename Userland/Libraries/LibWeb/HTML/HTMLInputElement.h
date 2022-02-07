/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLInputElement final
    : public HTMLElement
    , public FormAssociatedElement {
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
    void set_checked(bool);

    bool enabled() const;

    void did_click_button(Badge<Layout::ButtonBox>);

    virtual bool is_focusable() const override;

private:
    // ^DOM::Node
    virtual void inserted() override;
    virtual void removed_from(Node*) override;

    // ^HTML::FormAssociatedElement
    virtual HTMLElement& form_associated_element_to_html_element() override { return *this; }

    // ^DOM::EventTarget
    virtual void did_receive_focus() override;

    void create_shadow_tree_if_needed();

    RefPtr<DOM::Text> m_text_node;
    bool m_checked { false };
};

}
