/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2024, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/Timer.h>
#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/FormAssociatedElement.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/WebIDL/Types.h>

namespace Web::HTML {

class HTMLTextAreaElement final
    : public HTMLElement
    , public FormAssociatedElement
    , public DOM::EditableTextNodeOwner {
    WEB_PLATFORM_OBJECT(HTMLTextAreaElement, HTMLElement);
    JS_DECLARE_ALLOCATOR(HTMLTextAreaElement);
    FORM_ASSOCIATED_ELEMENT(HTMLElement, HTMLTextAreaElement)

public:
    virtual ~HTMLTextAreaElement() override;

    virtual void adjust_computed_style(CSS::StyleProperties&) override;

    String const& type() const
    {
        static String const textarea = "textarea"_string;
        return textarea;
    }

    // ^DOM::EditableTextNodeOwner
    virtual void did_edit_text_node(Badge<Navigable>) override;

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

    // https://html.spec.whatwg.org/multipage/forms.html#category-label
    virtual bool is_labelable() const override { return true; }

    virtual void reset_algorithm() override;

    virtual void form_associated_element_was_inserted() override;
    virtual void form_associated_element_was_removed(DOM::Node*) override;
    virtual void form_associated_element_attribute_changed(FlyString const&, Optional<String> const&) override;

    virtual void children_changed() override;

    // https://www.w3.org/TR/html-aria/#el-textarea
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::textbox; }

    String default_value() const;
    void set_default_value(String const&);

    String value() const override;
    void set_value(String const&);

    u32 text_length() const;

    bool check_validity();
    bool report_validity();
    void set_custom_validity(String const& error);

    WebIDL::UnsignedLong selection_start() const;
    WebIDL::ExceptionOr<void> set_selection_start(WebIDL::UnsignedLong);

    WebIDL::UnsignedLong selection_end() const;
    WebIDL::ExceptionOr<void> set_selection_end(WebIDL::UnsignedLong);

    WebIDL::Long max_length() const;
    WebIDL::ExceptionOr<void> set_max_length(WebIDL::Long);

    WebIDL::Long min_length() const;
    WebIDL::ExceptionOr<void> set_min_length(WebIDL::Long);

    unsigned cols() const;
    WebIDL::ExceptionOr<void> set_cols(unsigned);

    unsigned rows() const;
    WebIDL::ExceptionOr<void> set_rows(unsigned);

private:
    HTMLTextAreaElement(DOM::Document&, DOM::QualifiedName);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    void set_raw_value(String);
    String api_value() const;

    // ^DOM::Element
    virtual i32 default_tab_index_value() const override;

    void create_shadow_tree_if_needed();

    void handle_readonly_attribute(Optional<String> const& value);
    void handle_maxlength_attribute();

    void queue_firing_input_event();

    void update_placeholder_visibility();
    JS::GCPtr<DOM::Element> m_placeholder_element;
    JS::GCPtr<DOM::Text> m_placeholder_text_node;

    JS::GCPtr<DOM::Element> m_inner_text_element;
    JS::GCPtr<DOM::Text> m_text_node;

    RefPtr<Core::Timer> m_input_event_timer;

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fe-dirty
    bool m_dirty_value { false };

    // https://html.spec.whatwg.org/multipage/form-elements.html#the-textarea-element:concept-fe-mutable
    bool m_is_mutable { true };

    // https://html.spec.whatwg.org/multipage/form-elements.html#concept-textarea-raw-value
    String m_raw_value;

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#concept-fe-api-value
    mutable Optional<String> m_api_value;
};

}
