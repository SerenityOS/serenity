/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/ARIA/Roles.h>
#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>

namespace Web::HTML {

class HTMLFormElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLFormElement, HTMLElement);

public:
    virtual ~HTMLFormElement() override;

    DeprecatedString action() const;
    DeprecatedString method() const { return attribute(HTML::AttributeNames::method); }

    ErrorOr<void> submit_form(JS::GCPtr<HTMLElement> submitter, bool from_submit_binding = false);

    void reset_form();

    // NOTE: This is for the JS bindings. Use submit_form instead.
    WebIDL::ExceptionOr<void> submit();

    // NOTE: This is for the JS bindings. Use submit_form instead.
    void reset();

    void add_associated_element(Badge<FormAssociatedElement>, HTMLElement&);
    void remove_associated_element(Badge<FormAssociatedElement>, HTMLElement&);

    ErrorOr<Vector<JS::NonnullGCPtr<DOM::Element>>> get_submittable_elements();

    JS::NonnullGCPtr<DOM::HTMLCollection> elements() const;
    unsigned length() const;

    // https://www.w3.org/TR/html-aria/#el-form
    virtual Optional<ARIA::Role> default_role() const override { return ARIA::Role::form; }

    // https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#constructing-entry-list
    bool constructing_entry_list() const { return m_constructing_entry_list; }
    void set_constructing_entry_list(bool value) { m_constructing_entry_list = value; }

private:
    HTMLFormElement(DOM::Document&, DOM::QualifiedName);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    ErrorOr<void> populate_vector_with_submittable_elements_in_tree_order(JS::NonnullGCPtr<DOM::Element> element, Vector<JS::NonnullGCPtr<DOM::Element>>& elements);

    bool m_firing_submission_events { false };

    // https://html.spec.whatwg.org/multipage/forms.html#locked-for-reset
    bool m_locked_for_reset { false };

    Vector<JS::GCPtr<HTMLElement>> m_associated_elements;

    JS::GCPtr<DOM::HTMLCollection> mutable m_elements;

    bool m_constructing_entry_list { false };
};

}
