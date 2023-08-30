/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTextAreaElement final
    : public HTMLElement
    , public FormAssociatedElement
    , public DOM::EditableTextNodeOwner {
    WEB_PLATFORM_OBJECT(HTMLTextAreaElement, HTMLElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLTextAreaElement)

public:
    virtual ~HTMLTextAreaElement() override;

    virtual JS::GCPtr<Layout::Node> create_layout_node(NonnullRefPtr<CSS::StyleProperties>) override;

    DeprecatedString const& type() const
    {
        static DeprecatedString textarea = "textarea";
        return textarea;
    }

    // ^DOM::EditableTextNodeOwner
    virtual void did_edit_text_node(Badge<BrowsingContext>) override;

    // ^EventTarget
    // https://html.spec.whatwg.org/multipage/interaction.html#the-tabindex-attribute:the-textarea-element
    virtual bool is_focusable() const override { return true; }
    virtual void did_lose_focus() override;
    virtual void did_receive_focus() override;

    // ^FormAssociatedElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-listed
    virtual bool is_listed() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-submit
    virtual bool is_submittable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-reset
    virtual bool is_resettable() const override { return true; }

    // https://html.spec.whatwg.org/multipage/forms.html#category-autocapitalize
    virtual bool is_auto_capitalize_inheriting() const override { return true; }

    // ^HTMLElement
    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

    virtual void reset_algorithm() override;

    virtual void form_associated_element_was_inserted() override;

    virtual void children_changed() override;

    // https://www.w3.org/TR/html-aria/#el-textarea
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::textbox; }

private:
    HTMLTextAreaElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    // ^DOM::Element
    virtual i32 default_tab_index_value() const override;

    void create_shadow_tree_if_needed();

    JS::GCPtr<DOM::Element> m_inner_text_element;
    JS::GCPtr<DOM::Text> m_text_node;

    bool m_dirty { false };
    DeprecatedString m_raw_value;
};

}
