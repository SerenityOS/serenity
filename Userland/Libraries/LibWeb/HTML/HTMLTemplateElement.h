/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/DocumentFragment.h>
#include <LibWeb/HTML/HTMLElement.h>

namespace Web::HTML {

class HTMLTemplateElement final : public HTMLElement {
    WEB_PLATFORM_OBJECT(HTMLTemplateElement, HTMLElement);

public:
    virtual ~HTMLTemplateElement() override;

    JS::NonnullGCPtr<DOM::DocumentFragment> content() { return *m_content; }
    JS::NonnullGCPtr<DOM::DocumentFragment> const content() const { return *m_content; }

    virtual void adopted_from(DOM::Document&) override;
    virtual void cloned(Node& copy, bool clone_children) override;

private:
    HTMLTemplateElement(DOM::Document&, DOM::QualifiedName);

    virtual bool is_html_template_element() const final { return true; }

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    JS::GCPtr<DOM::DocumentFragment> m_content;
};

}

namespace Web::DOM {
template<>
inline bool Node::fast_is<HTML::HTMLTemplateElement>() const { return is_html_template_element(); }
}
