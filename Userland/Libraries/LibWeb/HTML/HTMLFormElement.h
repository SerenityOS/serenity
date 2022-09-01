/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/HTML/HTMLElement.h>
#include <LibWeb/HTML/HTMLInputElement.h>

namespace Web::HTML {

class HTMLFormElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLFormElement, HTMLElement);

public:
    virtual ~HTMLFormElement() override;

    String action() const;
    String method() const { return attribute(HTML::AttributeNames::method); }

    void submit_form(JS::GCPtr<HTMLElement> submitter, bool from_submit_binding = false);

    // NOTE: This is for the JS bindings. Use submit_form instead.
    void submit();

    void add_associated_element(Badge<FormAssociatedElement>, HTMLElement&);
    void remove_associated_element(Badge<FormAssociatedElement>, HTMLElement&);

    JS::NonnullGCPtr<DOM::HTMLCollection> elements() const;
    unsigned length() const;

private:
    HTMLFormElement(DOM::Document&, DOM::QualifiedName);

    virtual void visit_edges(Cell::Visitor&) override;

    bool m_firing_submission_events { false };

    Vector<JS::GCPtr<HTMLElement>> m_associated_elements;
};

}

WRAPPER_HACK(HTMLFormElement, Web::HTML)
